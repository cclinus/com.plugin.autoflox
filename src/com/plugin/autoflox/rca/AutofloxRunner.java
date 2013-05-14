package com.plugin.autoflox.rca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import com.plugin.autoflox.invarscope.DumpFileThread;
import com.plugin.autoflox.invarscope.aji.JSASTModifierWrapper;
import com.plugin.autoflox.invarscope.aji.executiontracer.AstInstrumenter;
import com.plugin.autoflox.service.FileManager;

public class AutofloxRunner {

	/**
	 * @param 1: js source folder where all js and html files are located:
	 *        sourceFolderPath(projectFolderPath) 2: proxyFolderPath
	 * 
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void main(String[] args) throws IOException, SAXException {

		String projectFolder = args[0];//"/home/cclinus/runtime-EclipseApplication/webTest/";//
		String proxyFolder = args[1];//"/home/cclinus/runtime-EclipseApplication/autoflox_proxy/";// args[1];

		/** For testing **/
//		String sbinFolder = "/home/cclinus/workspace/com.plugin.autoflox/sbin/";
//		FileManager.build(projectFolder, proxyFolder, sbinFolder);
//		FileManager.initFolderStruc();
		/** End of testing **/

		// Set up FileManager
		FileManager.build(projectFolder, proxyFolder, null);

		modifyJsToolFiles();
		File projectFolderFile = new File(FileManager.getProjectFolder());
		traverseAndInstrument(projectFolderFile,
				FileManager.getProjectFolder(),
				FileManager.getProxyInstrumentedFolder());

		// Start a thread that read and clean dump_file
		new DumpFileThread(FileManager.getDumpDataFile()).start();
	}

	public static void traverseAndInstrument(File node, String projectFolder,
			String proxyFolder) throws IOException, SAXException {

		if (node.isFile()) {
			if (node.getAbsolutePath().toLowerCase().contains(".js")
					|| node.getAbsolutePath().toLowerCase().contains(".html")) {
				// System.out.println("Instrumenting "+node.getAbsolutePath());
				initInstrumentation(node.getAbsolutePath(), projectFolder,
						proxyFolder);
			}
		}

		if (node.isDirectory()) {
			String[] subNote = node.list();
			for (String filename : subNote) {
				traverseAndInstrument(new File(node, filename), projectFolder,
						proxyFolder);
			}
		}

	}

	public static void initInstrumentation(String sourceFilePath,
			String projectFolder, String proxyFolder) throws IOException,
			SAXException {
		// Instrumentation
		JSASTModifierWrapper modifierWrapper = new JSASTModifierWrapper(
				new AstInstrumenter());
		modifierWrapper.setInstrumentAsyncs(false);

		String relativePath = FileManager.stringDiff(projectFolder,
				sourceFilePath);
		String destPath = proxyFolder + relativePath;

		// Make dirs
		File destFile = new File(destPath);
		destFile.mkdirs();

		modifierWrapper.startInstrumentation(sourceFilePath, destPath);
	}

	// Modify addVariable.js call path of receiveData.php
	public static void modifyJsToolFiles() throws IOException {
		// For addvariable.js
		String addVarFilePath = FileManager.getAddvariableScript();
		String addVarContent;
		File addVarFile = new File(addVarFilePath);
		if (addVarFile.exists()) {
			FileInputStream inputStream = new FileInputStream(addVarFilePath);
			try {
				addVarContent = IOUtils.toString(inputStream);
				// FIXME Need a better way to deal with constant
				addVarContent = addVarContent.replace("'receiveData.php'",
						"'/autoflox_proxy/bin/receiveData.php'");
			} finally {
				inputStream.close();
			}
			FileWriter fstream = new FileWriter(addVarFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(addVarContent);
			out.close();
		}

		// For addvariable_noAsync.js
		String addVarNoAsyFilePath = FileManager.getAddvariableNoAsyncScript();
		String addVarNoAsyContent;
		File addVarNoAsyFile = new File(addVarNoAsyFilePath);
		if (addVarNoAsyFile.exists()) {
			FileInputStream inputStream = new FileInputStream(
					addVarNoAsyFilePath);

			addVarNoAsyContent = IOUtils.toString(inputStream);
			// FIXME Need a better way to deal with constant
			addVarNoAsyContent = addVarNoAsyContent.replace(
					"'receiveData.php'",
					"'/autoflox_proxy/bin/receiveData.php'");
			inputStream.close();

			FileWriter fstream = new FileWriter(addVarNoAsyFilePath);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(addVarNoAsyContent);
			out.close();
		}

	}

}
