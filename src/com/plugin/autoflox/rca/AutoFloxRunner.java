package com.plugin.autoflox.rca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.plugin.autoflox.service.AutofloxService;
import com.plugin.autoflox.service.aji.DumpFileThread;
import com.plugin.autoflox.service.aji.JSASTModifierWrapper;
import com.plugin.autoflox.service.aji.executiontracer.AstInstrumenter;

public class AutoFloxRunner {

	public static String sourceFolderPath;
	public static String dumpFilePath;
	public static String proxyInstrumentedFolderPath;
	public static String proxyFolderPath;
	public static String proxyBinFolderPath;
	public static String proxyJsOutputFolderPath;

	/**
	 * @param 1: js source folder where all js and html files are located:
	 *        sourceFolderPath(projectFolderPath) 
	 *        2: proxyFolderPath
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		sourceFolderPath = args[0]; //"/home/cclinus/runtime-EclipseApplication/webTest/";
		proxyFolderPath = args[1]; //"/home/cclinus/runtime-EclipseApplication/autoflox_proxy/";
		
		proxyInstrumentedFolderPath = proxyFolderPath + "/instrumented/";	
		proxyBinFolderPath = proxyFolderPath + "/bin/";
		dumpFilePath = proxyBinFolderPath + "/dump_data";
		proxyJsOutputFolderPath = proxyFolderPath + "/jsSource/";

		modifyJsToolFiles();
		
		File sourceFolderFile = new File(sourceFolderPath);
		traverseAndInstrument(sourceFolderFile, sourceFolderPath,
				proxyInstrumentedFolderPath);

		// Start a thread that read and clean dump_file
		new DumpFileThread(dumpFilePath).start();

	}

	public static void traverseAndInstrument(File node,
			String sourceFolderPath, String proxyFolderPath) throws IOException {

		if (node.isFile()) {
			System.out.println(node.getAbsolutePath());
			initInstrumentation(node.getAbsolutePath(), sourceFolderPath,
					proxyFolderPath);
		}
		// System.out.println(node.getAbsoluteFile());

		if (node.isDirectory()) {
			String[] subNote = node.list();
			for (String filename : subNote) {
				traverseAndInstrument(new File(node, filename),
						sourceFolderPath, proxyFolderPath);
			}
		}

	}

	public static void initInstrumentation(String sourceFilePath,
			String sourceFolderPath, String proxyFolderPath) throws IOException {
		// Instrumentation
		JSASTModifierWrapper modifierWrapper = new JSASTModifierWrapper(
				new AstInstrumenter());
		modifierWrapper.setInstrumentAsyncs(false);

		String jsCode;
		FileInputStream inputStream = new FileInputStream(sourceFilePath);
		try {
			jsCode = IOUtils.toString(inputStream);
		} finally {
			inputStream.close();
		}

		String relativePath = AutofloxService.stringDiff(sourceFolderPath,
				sourceFilePath);
		String destPath = proxyFolderPath + relativePath;
		
		// Make dirs
		File destFile = new File(destPath);
		destFile.mkdirs();

		modifierWrapper.startInstrumentation(jsCode, destPath);
	}
	
	// Modify addVariable.js call path of receiveData.php
	public static void modifyJsToolFiles() throws IOException{
		// For addvariable.js
		String addVarFilePath = proxyBinFolderPath + "/addvariable.js";
		String addVarContent;
		File addVarFile = new File(addVarFilePath);
		if (addVarFile.exists()) {
			FileInputStream inputStream = new FileInputStream(addVarFilePath);
			try {
				addVarContent = IOUtils.toString(inputStream);
				addVarContent = addVarContent.replace("receiveData.php", "/autoflox_proxy/bin/receiveData.php");
			} finally {
				inputStream.close();
			}
			FileWriter fstream = new FileWriter(addVarFilePath);
			  BufferedWriter out = new BufferedWriter(fstream);
			  out.write(addVarContent);
			  //Close the output stream
			  out.close();  
		}
		
		// For addvariable_noAsync.js
		String addVarNoAsyFilePath = proxyBinFolderPath + "/addvariable_noAsync.js";
		String addVarNoAsyContent;
		File addVarNoAsyFile = new File(addVarNoAsyFilePath);
		if (addVarNoAsyFile.exists()) {
			FileInputStream inputStream = new FileInputStream(addVarNoAsyFilePath);
			try {
				addVarNoAsyContent = IOUtils.toString(inputStream);
				addVarNoAsyContent = addVarNoAsyContent.replace("receiveData.php", "/autoflox_proxy/bin/receiveData.php");
			} finally {
				inputStream.close();
			}
			FileWriter fstream = new FileWriter(addVarNoAsyFilePath);
			  BufferedWriter out = new BufferedWriter(fstream);
			  out.write(addVarNoAsyContent);
			  //Close the output stream
			  out.close();  
		}
		
	}

}
