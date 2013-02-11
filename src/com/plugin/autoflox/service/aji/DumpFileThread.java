package com.plugin.autoflox.service.aji;

import java.io.IOException;

import com.plugin.autoflox.rca.AutoFloxRunner;
import com.plugin.autoflox.rca.RCAPlugin;
import com.plugin.autoflox.service.aji.executiontracer.JSExecutionTracer;

public class DumpFileThread extends Thread {

	private DumpFileReader dReader;
	private int stateCounter = 0;
	public String executionTraceFolder = AutoFloxRunner.proxyFolderPath + "/trace/";
	public static String jsSourceFolder = AutoFloxRunner.proxyFolderPath + "/jsSource/";

	public DumpFileThread(String dumpFilePath) {
		DumpFileReader dReader = new DumpFileReader(dumpFilePath);
		this.dReader = dReader;
	}

	public void run() {
		while (true) {
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
								stateCounter).toString(), executionTraceFolder);
						
						// rca analysis
						RCAPlugin rca = new RCAPlugin(executionTraceFolder+"executiontrace", jsSourceFolder);
						rca.rcaStart();
					}
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}