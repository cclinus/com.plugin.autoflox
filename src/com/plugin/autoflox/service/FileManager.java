package com.plugin.autoflox.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileManager {

	public static int INDEX_NOT_FOUND = -1;

	public static String projectFolder;
	public static String proxyFolder;
	public static String sbinFolder;

	public static void build(String projectFolder, String proxyFolder,
			String sbinFolder) {
		FileManager.projectFolder = projectFolder;
		FileManager.proxyFolder = proxyFolder;
		FileManager.sbinFolder = sbinFolder;
	}

	public static String getProjectFolder() {
		return FileManager.projectFolder;
	}

	public static String getProxyFolder() {
		return FileManager.proxyFolder;
	}

	public static String getSbinFolder() {
		return FileManager.sbinFolder;
	}

	public static String getSbinReceiveData() {
		return FileManager.sbinFolder + "receiveData.php";
	}

	public static String getSbinAddvariableScript() {
		return FileManager.sbinFolder + "addvariable.js";
	}

	public static String getSbinAddvariableNoAsyncScript() {
		return FileManager.sbinFolder + "addvariable_noAsync.js";
	}

	public static String getProxyBinFolder() {
		return FileManager.proxyFolder + "bin/";
	}
	
	public static String getProxyBinTraceFolder(){
		return FileManager.proxyFolder + "bin/dumpTrace/";
	}

	public static String getProxyInstrumentedFolder() {
		return FileManager.proxyFolder + "instrumented/";
	}

	public static String getProxyJsSourceFolder() {
		return FileManager.proxyFolder + "jsSource/";
	}

	public static String getProxyTraceFolder() {
		return FileManager.proxyFolder + "trace/";
	}

	public static String getTableResultFolder() {
		return FileManager.proxyFolder + "bin/errorResult/";
	}
	
	public static String getProxyTraceExecutiontraceFolder() {
		return FileManager.proxyFolder + "trace/executiontrace/";
	}

	public static String getAddvariableScript() {
		return FileManager.proxyFolder + "bin/addvariable.js";
	}

	public static String getAddvariableNoAsyncScript() {
		return FileManager.proxyFolder + "bin/addvariable_noAsync.js";
	}

	public static String getReceiveDataScript() {
		return FileManager.proxyFolder + "bin/receiveData.php";
	}

	public static void initFolderStruc() throws IOException {
		// Clean AutofloxProxy folder under workspace
		File proxyFolderFile = new File(proxyFolder);
		if (proxyFolderFile.exists()) {
			delete(proxyFolderFile);
		}
		proxyFolderFile.mkdirs();
		// Create file structure of the proxy
		File instrumentFolderFile = new File(
				FileManager.getProxyInstrumentedFolder());
		instrumentFolderFile.mkdirs();
		instrumentFolderFile.setWritable(true, false);
		File jsSourceFolderFile = new File(FileManager.getProxyJsSourceFolder());
		jsSourceFolderFile.mkdirs();
		jsSourceFolderFile.setWritable(true, false);
		File traceFolderFile = new File(
				FileManager.getProxyTraceExecutiontraceFolder());
		traceFolderFile.mkdirs();
		traceFolderFile.setWritable(true, false);
		File binFolderFile = new File(FileManager.getProxyBinFolder());
		binFolderFile.mkdirs();
		binFolderFile.setWritable(true, false);
		File dumpTraceFolderFile = new File(FileManager.getProxyBinTraceFolder());
		dumpTraceFolderFile.mkdirs();
		dumpTraceFolderFile.setWritable(true, false);
		File tableResultFolder = new File(FileManager.getTableResultFolder());
		tableResultFolder.mkdirs();
		tableResultFolder.setWritable(true, false);

		// Copy receiveData.php to workspace/autoflox_proxy/instrumented
		File receiveDataSource = new File(FileManager.getSbinReceiveData());
		File receiveDataDest = new File(FileManager.getReceiveDataScript());
		FileManager.copy(receiveDataSource, receiveDataDest);

		File addVarSource = new File(FileManager.getSbinAddvariableScript());
		File addVarDest = new File(FileManager.getAddvariableScript());
		FileManager.copy(addVarSource, addVarDest);

		File addVarNoSynSource = new File(
				FileManager.getSbinAddvariableNoAsyncScript());
		File addVarNoSynDest = new File(
				FileManager.getAddvariableNoAsyncScript());
		FileManager.copy(addVarNoSynSource, addVarNoSynDest);

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
				}
			}
		} else {
			// if file, then delete it
			file.delete();
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

	public static boolean isDirectoryEmpty(String dirPath) {
		File file = new File(dirPath);
		if (file.isDirectory()) {
			if (file.list().length > 0) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

}
