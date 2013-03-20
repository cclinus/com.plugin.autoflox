package com.plugin.autoflox.invarscope;

import java.io.IOException;

import com.plugin.autoflox.invarscope.aji.executiontracer.JSExecutionTracer;
import com.plugin.autoflox.rca.AutofloxRunner;
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
		int errorIdCounter = 0;
		while (true) {
			System.out.println("Reading dumpData");
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
								stateCounter).toString(), FileManager.getProxyTraceFolder());
						
						// rca analysis
						RCAPlugin rca = new RCAPlugin(FileManager.getProxyTraceExecutiontraceFolder(), FileManager.getProxyJsSourceFolder());
						rca.rcaStart(errorIdCounter);
						errorIdCounter++;
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}