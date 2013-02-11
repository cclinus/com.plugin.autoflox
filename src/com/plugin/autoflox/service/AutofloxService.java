package com.plugin.autoflox.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AutofloxService extends Thread {

	public static int INDEX_NOT_FOUND = -1;

	public static void initFolderStruc(String srcFolder, String proxyFolder,
			String sbinFolder) throws IOException {
		// Clean AutofloxProxy folder under workspace
		File proxyFolderFile = new File(proxyFolder);
		if (proxyFolderFile.exists()) {
			delete(proxyFolderFile);
		}
		proxyFolderFile.mkdirs();
		// Create file structure of the proxy
		File instrumentFolderFile = new File(proxyFolder + "/instrumented");
		instrumentFolderFile.mkdirs();
		instrumentFolderFile.setWritable(true, false);
		File jsSourceFolderFile = new File(proxyFolder + "/jsSource");
		jsSourceFolderFile.mkdirs();
		jsSourceFolderFile.setWritable(true, false);
		File traceFolderFile = new File(proxyFolder + "/trace/executiontrace");
		traceFolderFile.mkdirs();
		traceFolderFile.setWritable(true, false);
		File binFolderFile = new File(proxyFolder + "/bin");
		binFolderFile.mkdirs();
		binFolderFile.setWritable(true, false);

		// Copy receiveData.php to workspace/autoflox_proxy/instrumented
		File receiveDataSource = new File(sbinFolder + "/receiveData.php");
		File receiveDataDest = new File(binFolderFile + "/receiveData.php");
		copy(receiveDataSource, receiveDataDest);

		
		File addVarSource = new File(sbinFolder + "/addvariable.js");
		File addVarDest = new File(binFolderFile + "/addvariable.js");
		copy(addVarSource, addVarDest);
		
		File addVarNoSynSource = new File(sbinFolder + "/addvariable_noAsync.js");
		File addVarNoSynDest = new File(binFolderFile + "/addvariable_noAsync.js");
		copy(addVarNoSynSource, addVarNoSynDest);

	}

	public static void copy(File in, File out) throws IOException {
		InputStream inStream = null;
		OutputStream outStream = null;
		inStream = new FileInputStream(in);
		outStream = new FileOutputStream(out);

		byte[] buffer = new byte[1024];

		int length;
		// copy the file content in bytes
		while ((length = inStream.read(buffer)) > 0) {

			outStream.write(buffer, 0, length);

		}

		inStream.close();
		outStream.close();
	}

	public static void delete(File file) throws IOException {

		if (file.isDirectory()) {

			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();
				System.out.println("Directory is deleted : "
						+ file.getAbsolutePath());

			} else {

				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					delete(fileDelete);
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
					System.out.println("Directory is deleted : "
							+ file.getAbsolutePath());
				}
			}

		} else {
			// if file, then delete it
			file.delete();
			System.out.println("File is deleted : " + file.getAbsolutePath());
		}
	}

	public static String stringDiff(String str1, String str2) {
		if (str1 == null) {
			return str2;
		}
		if (str2 == null) {
			return str1;
		}
		int at = indexOfDifference(str1, str2);
		if (at == INDEX_NOT_FOUND) {
			return null;
		}
		return str2.substring(at);
	}

	public static int indexOfDifference(CharSequence cs1, CharSequence cs2) {
		if (cs1 == cs2) {
			return INDEX_NOT_FOUND;
		}
		if (cs1 == null || cs2 == null) {
			return 0;
		}
		int i;
		for (i = 0; i < cs1.length() && i < cs2.length(); ++i) {
			if (cs1.charAt(i) != cs2.charAt(i)) {
				break;
			}
		}
		if (i < cs2.length() || i < cs1.length()) {
			return i;
		}
		return INDEX_NOT_FOUND;
	}

}
