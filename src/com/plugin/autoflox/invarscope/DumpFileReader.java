package com.plugin.autoflox.invarscope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class DumpFileReader {

	private String dumpFilePath;

	public static void main(String[] args) {
		DumpFileReader dReader = new DumpFileReader(
				"/home/cclinus/runtime-EclipseApplication/autoflox_proxy/bin/dump_data");
		try {
			System.out.println(dReader.readDumpFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public DumpFileReader(String dumpFilePath) {
		this.dumpFilePath = dumpFilePath;
	}

	public String readDumpFile() throws IOException {
		String dumpFileData;
		File file = new File(this.dumpFilePath);
		if (file.exists()) {
			FileInputStream inputStream = new FileInputStream(this.dumpFilePath);
			dumpFileData = IOUtils.toString(inputStream);
			inputStream.close();
			return dumpFileData;
		}
		file = null;
		return null;
	}

	public void cleanDumpFile() throws FileNotFoundException {
		File file = new File(this.dumpFilePath);

		if (file.exists()) {
			file.delete();
		}
	}

}
