package com.plugin.autoflox.action;

import java.util.HashMap;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.plugin.autoflox.service.ProcessManager;
import com.plugin.autoflox.views.AutofloxView;
import com.plugin.autoflox.views.AutofloxViewUpdateThread;

public class AutofloxStopAction implements IWorkbenchWindowActionDelegate {
	private static IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public AutofloxStopAction() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		// Clean the consoleTableMap
		AutofloxView.consoleTableMap = new HashMap<String, TreeItem>();
		
		System.out.println("Autoflox stops ");
		if (ProcessManager.isAutofloxRun()) {
			// Terminate cmd process
			ProcessManager.stopAutoflox();
			// Terminate AutofloxViewUpdateThread
			AutofloxViewUpdateThread.terminate();
			// Clean console
			AutofloxView.cleanConsole();
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