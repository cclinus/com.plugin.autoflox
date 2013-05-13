package com.plugin.autoflox.action;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.plugin.autoflox.service.ProcessManager;
import com.plugin.autoflox.service.FileManager;
import com.plugin.autoflox.views.AutofloxView;
import com.plugin.autoflox.views.AutofloxViewUpdateThread;

public class AutofloxRunAction implements IWorkbenchWindowActionDelegate {
	private static IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public AutofloxRunAction() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {

		if (!ProcessManager.isAutofloxRun()) {

			System.out.println("Autoflox runs ");
			String workspacePath, currentOpenedFilePath;
			boolean pathCheckFlag = false;
			try {
				// Get workspace path
				workspacePath = AutofloxView.getWorkspacePath();
				// Get current opened file path
				currentOpenedFilePath = AutofloxView.getOpenedFilePath();
				// Check if the opened file is in the workspace
				pathCheckFlag = currentOpenedFilePath.contains(workspacePath);
			} catch (NullPointerException e) {
				System.out.println("No file is found.");
				return;
			}

			if (pathCheckFlag) {

				// Get project folder
				String parseProjectString = FileManager.stringDiff(
						workspacePath, currentOpenedFilePath);

				// Set up FileManager
				String[] temp;
				temp = parseProjectString.split("/");
				String projectName = temp[1];
				String projectFolder = workspacePath + "/" + projectName + "/";
				File currentClassPath = new File(this.getClass()
						.getProtectionDomain().getCodeSource().getLocation()
						.getPath());
				String sbinFolder = currentClassPath + "/sbin/";
				String proxyFolder = workspacePath + "/autoflox_proxy/";
				FileManager.build(projectFolder, proxyFolder, sbinFolder);

				try {
					// Start autoflox process
					FileManager.initFolderStruc();
					String runCmd = "java -jar " + FileManager.getSbinFolder()
							+ "autoflox-cmd.jar "
							+ FileManager.getProjectFolder() + " "
							+ FileManager.getProxyFolder();
					System.out.println("Run Cmd: " + runCmd);
					ProcessManager.autofloxProcess = Runtime.getRuntime()
							.exec(runCmd);
				} catch (IOException e) {
					e.printStackTrace();
				}

				new AutofloxViewUpdateThread().start();
			}
		} else {
			System.err.println("rca is running already.");
		}

	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}