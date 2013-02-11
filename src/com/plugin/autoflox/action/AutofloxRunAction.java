package com.plugin.autoflox.action;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.plugin.autoflox.service.AutofloxService;
import com.plugin.autoflox.views.AutofloxView;
import com.plugin.autoflox.views.AutofloxViewUpdateThread;

public class AutofloxRunAction implements IWorkbenchWindowActionDelegate {
	private static IWorkbenchWindow window;
	public static String workspacePath;
	public static String currentOpenedFilePath;
	public static String projectPath;
	public static String proxyFolderPath;
	public static String sbinFolderPath;
	public static String instrumentedFolderPath;

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
		System.out.println("Autoflox runs ");
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
			String parseProjectString = AutofloxService.stringDiff(
					workspacePath, currentOpenedFilePath);

			// Parse the project name
			String[] temp;
			temp = parseProjectString.split("/");
			String projectName = temp[1];

			projectPath = workspacePath + "/" + projectName + "/";
			System.out.println("Project path:" + projectPath);

			File checkPath = new File(this.getClass().getProtectionDomain()
					.getCodeSource().getLocation().getPath());
			sbinFolderPath = checkPath + "/sbin/";
			
			proxyFolderPath = workspacePath	+ "/autoflox_proxy/";
			instrumentedFolderPath = proxyFolderPath + "/instrumented/";

			//openDialog("AutoFlox", "AutoFlox runs. Please navigate your browser to the AutoFlox proxy at "+instrumentedFolderPath);
			
			try {
				AutofloxService.initFolderStruc(projectPath, proxyFolderPath, sbinFolderPath);
				Runtime.getRuntime().exec(
						"java -jar " + sbinFolderPath + "autoflox-cmd.jar "
								+ projectPath + " " + proxyFolderPath); // /home/cclinus/workspace/autoflox-cmd.jar
			} catch (IOException e) {
				e.printStackTrace();
			}

			new AutofloxViewUpdateThread().start();
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