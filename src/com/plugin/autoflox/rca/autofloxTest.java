package com.plugin.autoflox.rca;

import java.io.*;

import org.apache.commons.io.IOUtils;

import com.plugin.autoflox.service.aji.DumpFileThread;
import com.plugin.autoflox.service.aji.JSASTModifierWrapper;
import com.plugin.autoflox.service.aji.executiontracer.AstInstrumenter;

public class autofloxTest {

	public static String jsSourcePath = "/home/cclinus/workspace/webTest/WebContent/ex.js";
	public static String dumpFilePath = "/home/cclinus/workspace/proxy/instrumented/dump_data";
	public static String jsSourceName = "ex.js";

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		// Instrumentation
		JSASTModifierWrapper modifierWrapper = new JSASTModifierWrapper(
				new AstInstrumenter());
		modifierWrapper.setInstrumentAsyncs(false);

		String jsSource;
		FileInputStream inputStream = new FileInputStream(jsSourcePath);
		try {
			jsSource = IOUtils.toString(inputStream);
		} finally {
			inputStream.close();
		}
		modifierWrapper.startInstrumentation(jsSource, jsSourceName);

		// Start a thread that read and clean dump_file
		new DumpFileThread(dumpFilePath).start();
	}

}
