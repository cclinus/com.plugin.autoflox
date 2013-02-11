package com.plugin.autoflox.service.aji;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class DumpFileReader {

	private String dumpFilePath;

	public static void main(String[] args) {
		DumpFileReader dReader = new DumpFileReader(
				"/home/cclinus/workspace/proxy/instrumented/dump_data");
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
			try {
				dumpFileData = IOUtils.toString(inputStream);
			} finally {
				inputStream.close();
			}
			return dumpFileData;
		}
		return null;
	}

	public void cleanDumpFile() throws FileNotFoundException {
		File file = new File(this.dumpFilePath);

		if (file.exists()) {
			file.delete();
		}
	}

}
