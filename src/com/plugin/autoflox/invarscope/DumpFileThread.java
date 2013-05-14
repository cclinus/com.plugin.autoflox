package com.plugin.autoflox.invarscope;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import com.plugin.autoflox.invarscope.aji.executiontracer.JSExecutionTracer;
import com.plugin.autoflox.rca.RCAPlugin;
import com.plugin.autoflox.service.FileManager;

public class DumpFileThread extends Thread {

	private DumpFileReader dReader;
	private int stateCounter = 0;

	public DumpFileThread(String dumpFilePath) {
		DumpFileReader dReader = new DumpFileReader(dumpFilePath);
		this.dReader = dReader;
	}

	public void run() {
		// Used to assign each error an id so an error tree structure can be
		// constructed.
		int errorIdCounter = 0;
		while (true) {
			// System.out.println("Reading dumpData");
			try {

				// Read from dump file to point array
				String dumpData = dReader.readDumpFile();
				if (dumpData != null) {
					JSExecutionTracer.addPoint(dumpData);

					System.out.println("Trace point array size:"
							+ JSExecutionTracer.points.length());

					// Clean dump file
					dReader.cleanDumpFile();

					// Check if ERROR occurs
					if (dumpData.contains(":::ERROR")) {
						stateCounter++;
						// Output the trace
						JSExecutionTracer.generateTrace(new Integer(
								stateCounter).toString(), FileManager
								.getProxyTraceFolder());
					}

					// Check if trace folder is empty
					// If not, we need analyse the error from traces
					if (!FileManager.isDirectoryEmpty(FileManager
							.getProxyTraceFolder())) {
						// rca analysis
						RCAPlugin rca = new RCAPlugin(
								FileManager.getProxyTraceExecutiontraceFolder(),
								FileManager.getProxyJsSourceFolder());
						rca.rcaStart(errorIdCounter);
						errorIdCounter++;
						// Clean the point array by removing the last finished error
						JSONArray temJSONArray = new JSONArray();
						for(int j = 0; j < JSExecutionTracer.points.length(); j++){
							if(!JSExecutionTracer.points.get(j).toString().contains(":::ERROR")){
								temJSONArray.put(JSExecutionTracer.points.get(j));
							}
						}
						JSExecutionTracer.points = temJSONArray;
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}