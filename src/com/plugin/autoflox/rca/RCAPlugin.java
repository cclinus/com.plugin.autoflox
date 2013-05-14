package com.plugin.autoflox.rca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import com.plugin.autoflox.service.FileManager;

public class RCAPlugin {
	private static String outputFolder;
	private static String traceFolder;
	private static String jsSourceFolder;

	public RCAPlugin(String trace_folder) {
		// trace_folder should be set to the folder where the traces are stored
		this.traceFolder = trace_folder;
	}

	public RCAPlugin(String trace_folder, String jsFolder) {
		// trace_folder should be set to the folder where the traces are stored
		this.traceFolder = trace_folder;
		this.jsSourceFolder = jsFolder;
	}

	public void rcaStart(int errorIdCounter) throws IOException {
		System.out.println("\n\n*****AutoFLox Traceback*******");
		// Note: Attach a variable to the error trace containing the
		// error message

		// Note: Create a function in RootCauseAnalyzer that finds the
		// error trace. You can do this by invoking TraceParser on the
		// file first, and then looking through each trace to see if any
		// of them is of type ERROR. The return value should be the error
		// message. Call this findErrorMsg().

		// Note: Create a function in RootCauseAnalyzer that takes the
		// trace to be parsed and the initial null_var as its parameters. This
		// function would return true if it found a GEBID line; false
		// otherwise. Suppose this function is called findGEBID()

		// STEPS:
		// Get outputFolder
		String of = getOutputFolder();

		// For each output trace file in the outputFolder, do the following
		// 1. Determine the initial null_var (should be "" if it's of the form
		// document.getElementById(...)).
		// You can do this by calling findErrorMsg() and getting the error
		// message.
		// Once you have the error message, determine if it is of the form
		// "<something> is null".
		// If so, we will ASSUME this is the corresponding error message.
		// If there is no error message, move on to the next one
		// 2. Call findGEBID()
		// 3a. If findGEBID() returns true, an output must have been printed
		// showing the GEBID line.
		// In this case, halt execution since the output has been found
		// 3b. If findGEBID() returns false, move on to the next one
		boolean lineFound = false;
		File dir = new File(traceFolder);
		RootCauseAnalyzer rca = new RootCauseAnalyzer(jsSourceFolder);
		File[] traceDirFiles = dir.listFiles();
		Arrays.sort(traceDirFiles);
		for (File child : traceDirFiles) {
			// Ignore the self and parent aliases, and files beginning with "."
			if (".".equals(child.getName()) || "..".equals(child.getName())
					|| child.getName().indexOf(".") == 0) {
				continue;
			}

			String fullPath = traceFolder + "/" + child.getName();
			
			System.out.println("Trace File for Error: " + fullPath);

			String errorMessage = rca.findErrorMsg(fullPath);

			// Create entry error item
			String executionTrace = String.valueOf(errorIdCounter) + ":::"
					+ "1:::" + errorMessage + ":::";
			
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(FileManager.getTableResultFile(), true)));
			out.println(executionTrace);
			out.close();

			System.err.println("error msg:"+errorMessage);

			if (errorMessage != null) {
				// Determine if it is of the form "<something> is null"
				if (errorMessage.matches(".* is null")) {
					// Get the initial null_var
					String initNullVar = errorMessage.substring(0,
							errorMessage.lastIndexOf(" is null"));
					if (initNullVar.startsWith("document.getElementById(")
							|| initNullVar.startsWith("$(")
							|| initNullVar.contains("getAttribute")
							|| initNullVar.contains("getComputedStyle")) {
						initNullVar = "";
					}

					// Find the direct DOM access
					boolean GEBIDfound = rca.findGEBID(fullPath, initNullVar,
							errorIdCounter);
					
					// Clean up the trace as the error is analysed already.
					new File(fullPath).delete();

					if (GEBIDfound) {
						System.out.println("Direct DOM access found!\n");
						return;
					}
				}
			}
		}

		// If GEBID line was not found, print a message saying so
		System.out.println("Direct DOM access NOT found\n");

		// Note: See first note in "Improvements to Tool" in your iTouch
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String absolutePath) {
		outputFolder = absolutePath;
	}
}