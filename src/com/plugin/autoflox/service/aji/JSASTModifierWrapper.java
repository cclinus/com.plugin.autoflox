package com.plugin.autoflox.service.aji;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ast.AstRoot;

import com.plugin.autoflox.rca.AutofloxRunner;

public class JSASTModifierWrapper {

	private JSASTModifier modifier;
	private int rootCounter = 0;
	private boolean htmlFound = false;
	private boolean instrumentAsyncs = true;
	public static String jsSourceOutputFolder = AutofloxRunner.proxyJsOutputFolderPath;

	public JSASTModifierWrapper(JSASTModifier modify) {
		// excludeFilenamePatterns = new ArrayList<String>();
		modifier = modify;
	}

	public void setInstrumentAsyncs(boolean val) {
		instrumentAsyncs = val;
		modifier.setInstrumentAsyncs(val);
	}

	/*
	 *  Take sourceContent which is the actual code, instrument it and save to scopeName(file path)
	 */
	public void startInstrumentation(String sourceContent, String scopeName)
			throws FileNotFoundException {
		
		// TODO: Handle html case later
		String instrumentedCode = modifyJS(sourceContent, scopeName);
		//System.out.println(instrumentedCode);

		// Generate the real instrumented code, scopeName is the js file name
		writeTextFile(scopeName, instrumentedCode);

		// Deal with html code
		// htmlFound = true;
		// try {
		// DocumentBuilderFactory factory = DocumentBuilderFactory
		// .newInstance();
		// DocumentBuilder builder = factory.newDocumentBuilder();
		// Document dom = builder.parse(new InputSource(new StringReader(
		// sourceContent)));
		//
		// /* find script nodes in the html */
		// NodeList nodes = dom.getElementsByTagName("script");
		//
		// for (int i = 0; i < nodes.getLength(); i++) {
		// Node nType = nodes.item(i).getAttributes().getNamedItem("type");
		// /* instrument if this is a JavaScript node */
		// if ((nType != null && nType.getTextContent() != null && nType
		// .getTextContent().toLowerCase().contains("javascript"))) {
		// String content = nodes.item(i).getTextContent();
		// System.out.println(content.length());
		// if (content.length() > 0) {
		// String js = modifyJS(content, scopeName + "script" + i);
		// nodes.item(i).setTextContent(js);
		// System.out.println(js);
		// continue;
		// }
		// }
		//
		// /* also check for the less used language="javascript" type tag */
		// nType = nodes.item(i).getAttributes().getNamedItem("language");
		// if ((nType != null && nType.getTextContent() != null && nType
		// .getTextContent().toLowerCase().contains("javascript"))) {
		//
		// System.out.println("javascript tag found");
		//
		// String content = nodes.item(i).getTextContent();
		// if (content.length() > 0) {
		// String js = modifyJS(content, scopeName + "script" + i);
		// nodes.item(i).setTextContent(js);
		// }
		//
		// }
		// }
		// /* only modify content when we did modify anything */
		// if (nodes.getLength() > 0) {
		// /* set the new content */
		//
		// // Convert dom to string
		// DOMSource domSource = new DOMSource(dom);
		// StringWriter writer = new StringWriter();
		// StreamResult result = new StreamResult(writer);
		// TransformerFactory tf = TransformerFactory.newInstance();
		// Transformer transformer = tf.newTransformer();
		// transformer.transform(domSource, result);
		//
		// System.out.println(writer.toString());
		// }
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

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
			// cx.setErrorReporter(new ConsoleErrorReporter());

			/* create a new parser */
			// CompilerEnvirons ce = new CompilerEnvirons();
			// ce.setErrorReporter(new ConsoleErrorReporter());
			Parser rhinoParser = new Parser(new CompilerEnvirons(),
					cx.getErrorReporter());
			// Parser rhinoParser = new Parser(ce, cx.getErrorReporter());

			/* parse some script and save it in AST */
			ast = rhinoParser.parse(new String(input), scopename, 0);

			/* Print out AST root to file */
			/* START */
			rootCounter++;
			try {
				File file = new File(jsSourceOutputFolder + "/root"
						+ rootCounter + ".js");
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
				// System.out.println(code);

				try {
					funcName = funcName.trim();
					File file = new File(jsSourceOutputFolder + "/" + funcName
							+ ".js");
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
			
			System.out.println("Instrumented to "+ fileName);
			
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

	}
}
