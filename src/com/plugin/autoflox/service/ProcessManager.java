package com.plugin.autoflox.service;

public class ProcessManager {

	public static Process autofloxProcess = null;

	public static boolean isAutofloxRun() {
		if (autofloxProcess != null)
			return true;
		else
			return false;
	}
	
	public static void stopAutoflox(){
		ProcessManager.autofloxProcess.destroy();
		ProcessManager.autofloxProcess = null;
	}

}
