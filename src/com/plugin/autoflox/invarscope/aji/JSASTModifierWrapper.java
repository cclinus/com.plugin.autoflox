package com.plugin.autoflox.invarscope.aji;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ast.AstRoot;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.plugin.autoflox.invarscope.aji.executiontracer.AnonymousFunctionTracer;
import com.plugin.autoflox.rca.AutofloxRunner;
import com.plugin.autoflox.service.FileManager;

public class JSASTModifierWrapper {

	private JSASTModifier modifier;
	private int rootCounter = 0;
	private boolean htmlFound = false;
	private boolean instrumentAsyncs = true;

	public JSASTModifierWrapper(JSASTModifier modify) {
		modifier = modify;
	}

	public void setInstrumentAsyncs(boolean val) {
		instrumentAsyncs = val;
		modifier.setInstrumentAsyncs(val);
	}

	/**
	 * Take sourceContent which is the actual code, instrument it and save to
	 * scopeName(file path)
	 */
	public void startInstrumentation(String sourceFilePath, String scopeName)
			throws IOException, SAXException {

		String instrumentedCode = null;

		String jsCode;
		
		// Log path for potential anonymous functions
		AnonymousFunctionTracer.fileName = sourceFilePath;

		// FIXME Find a better way to detect html and js files
		if (scopeName.contains(".html")) {

			htmlFound = true;
			// Parse html node and get script node
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = null;

			try {
				builder = builderFactory.newDocumentBuilder();
				FileInputStream inputStream = new FileInputStream(
						sourceFilePath);
				
				Document document = builder.parse(inputStream);
				inputStream.close();
				NodeList rootElement = document.getElementsByTagName("script");

				// Traverse script tag list
				for (int i = 0; i < rootElement.getLength(); i++) {
					
					// Log for potential anonymous functions. 
					AnonymousFunctionTracer.scriptTagNo = i;

					Node jsNode = rootElement.item(i);
					String nodeString = jsNode.getTextContent();

					if (nodeString.length() > 0) {
						// Make some change on each content of script tag
						nodeString = modifyJS(nodeString, scopeName);
						jsNode.setTextContent(nodeString);
					}

					instrumentedCode = convertDomToString(document);
				}

			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}

		} else if (scopeName.contains(".js")) {
			
			// Log for potential anonymous functions: in js file, tag id is always 0. 
			AnonymousFunctionTracer.scriptTagNo = 0;

			FileInputStream inputStream = new FileInputStream(sourceFilePath);
			jsCode = IOUtils.toString(inputStream);
			inputStream.close();
			
			// If it is javascript file
			instrumentedCode = modifyJS(jsCode, scopeName);
		}

		// Generate the instrumented code, scopeName is the file name
		writeTextFile(scopeName, instrumentedCode);

	}

	/**
	 * This method tries to add instrumentation code to the input it receives.
	 * The original input is returned if we can't parse the input correctly
	 * (which might have to do with the fact that the input is no JavaScript
	 * because the server uses a wrong Content-Type header for JSON data)
	 * 
	 * @param input
	 *            The JavaScript to be modified
	 * @param scopename
	 *            Name of the current scope (filename mostly)
	 * @return The modified JavaScript
	 */
	public String modifyJS(String input, String scopename) {

		try {
			AstRoot ast = null;

			/* initialize JavaScript context */
			Context cx = Context.enter();

			/* create a new parser */
			Parser rhinoParser = new Parser(new CompilerEnvirons(),
					cx.getErrorReporter());

			/* parse some script and save it in AST */
			ast = rhinoParser.parse(new String(input), scopename, 0);

			/* Print out AST root to file */
			/* START */
			rootCounter++;
			try {
				File file = new File(FileManager.getProxyJsSourceFolder()
						+ "/root" + rootCounter + ".js");
				if (!file.exists()) {
					file.createNewFile();
				}

				FileOutputStream fop = new FileOutputStream(file);

				fop.write(input.getBytes());
				fop.flush();
				fop.close();
			} catch (IOException ioe) {
				System.out.println("IO Exception");
			}
			/* END */

			/*
			 * Look for instances of "function" in input then figure out where
			 * it ends
			 */
			/* START */
			String inputCopy = input;

			// FIXME Here also need to dump anonymous function

			int indexOfFuncString = inputCopy.indexOf("function ");
			while (indexOfFuncString != -1) {
				String sub = inputCopy.substring(indexOfFuncString);

				int nextOpenParen = sub.indexOf("(");
				String funcName = sub.substring(9, nextOpenParen); // "function "
																	// has 9
																	// characters

				int firstOpenBrace = sub.indexOf("{");
				int countOpenBraces = 1;
				int countCloseBraces = 0;

				int endIndex = firstOpenBrace;
				while (countOpenBraces != countCloseBraces) {
					endIndex++;
					if (sub.charAt(endIndex) == '{') {
						countOpenBraces++;
					} else if (sub.charAt(endIndex) == '}') {
						countCloseBraces++;
					}
				}

				String code = sub.substring(0, endIndex + 1);

				try {
					funcName = funcName.trim();
					File file = new File(FileManager.getProxyJsSourceFolder()
							+ "/" + funcName + ".js");
					if (!file.exists()) {
						file.createNewFile();
					}

					FileOutputStream fop = new FileOutputStream(file);

					fop.write(code.getBytes());
					fop.flush();
					fop.close();
				} catch (IOException ioe) {
					System.out.println("IO Exception");
				}

				inputCopy = sub.substring(endIndex + 1);
				indexOfFuncString = inputCopy.indexOf("function ");
			}
			/* END */

			modifier.setScopeName(scopename);

			modifier.start();

			/* recurse through AST */
			ast.visit(modifier);

			// if (htmlFound == true) {
			modifier.finish(ast);
			// htmlFound = false;
			// }

			/* clean up */
			Context.exit();

			return ast.toSource();
		} catch (RhinoException re) {
			System.err.println(re.getMessage());
			// LOGGER.warn("Unable to instrument. This might be a JSON response sent"
			// + " with the wrong Content-Type or a syntax error.");
		} catch (IllegalArgumentException iae) {
			// LOGGER.warn("Invalid operator exception catched. Not instrumenting code.");
		}
		// LOGGER.warn("Here is the corresponding buffer: \n" + input + "\n");

		return input;
	}

	public void writeTextFile(String fileName, String s) {
		try {
			File f = new File(fileName);
			f.delete();

			FileWriter fstream = new FileWriter(fileName);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(s);
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Instrumentation writeTextFile Error: "
					+ e.getMessage());
		}

	}

	public static String convertDomToString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			// Clean up junk chars in xml before turning to html code
			return "<!DOCTYPE html>"
					+ sw.toString().replace("&gt;", ">").replace("&lt;", "<")
							.replace("&amp;", "&");
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}

}
