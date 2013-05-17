package com.plugin.autoflox.invarscope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import com.plugin.autoflox.rca.RootCauseAnalyzer;

public class DumpFileReader {

	private String binTraceFolder;

	public static void main(String[] args) {
		DumpFileReader dReader = new DumpFileReader(
				"/home/cclinus/runtime-EclipseApplication/autoflox_proxy/bin/dump_data");
		try {
			System.out.println(dReader.readDumpFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DumpFileReader(String binTraceFolder) {
		this.binTraceFolder = binTraceFolder;
	}

	public String readDumpFile() throws IOException {
		String dumpFileData;
		File dir = new File(this.binTraceFolder);
		File[] dumpTraceDirFiles = dir.listFiles();
		if (dumpTraceDirFiles.length > 0) {
			Arrays.sort(dumpTraceDirFiles);
			FileInputStream inputStream = new FileInputStream(
					dumpTraceDirFiles[0]);
			dumpFileData = IOUtils.toString(inputStream);
			inputStream.close();
			
			cleanDumpFile(dumpTraceDirFiles[0].getAbsolutePath());
			System.gc();
			
			return dumpFileData;
		}
		System.gc();
		return null;
	}

	private void cleanDumpFile(String currentDumpFilePath) throws FileNotFoundException {
		File file = new File(currentDumpFilePath);

		if (file.exists()) {
			file.delete();
		}
	}

}
