package com.plugin.autoflox.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;

import com.plugin.autoflox.action.AutofloxRunAction;

public class AutofloxViewUpdateThread extends Thread {

	private static String RESULT_FILE_PATH = AutofloxRunAction.workspacePath
			+ "/autoflox_proxy/bin/tableResult";
	public static boolean running = true;

	public static void terminate() {
		running = false;
	}

	public void run() {
		running = true;
		while (running) {
			// Check if the file exists
			File file = new File(RESULT_FILE_PATH);
			if (file.exists()) {

				InputStream fis;
				BufferedReader br;
				String line;

				try {
					fis = new FileInputStream(RESULT_FILE_PATH);

					br = new BufferedReader(new InputStreamReader(fis,
							Charset.forName("UTF-8")));
					while ((line = br.readLine()) != null) {
						updateViewTable(line);
						// System.err.println(line);
					}

					// Clean the file
					file = new File(RESULT_FILE_PATH);
					file.delete();
					
					fis.close();
					br.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		System.out.println("AutofloxViewUpdateThread terminated");

	}

	private static void updateViewTable(final String itemContent) {

		// itemContent is the format:
		// PathToFile:::FunctionName:::Type+LineNo
		final String[] itemColums = itemContent.split(":::");

		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				synchronized (this) {
					System.out.println("Add new item to table");
					// TableItem item = new TableItem(AutofloxView.resultTable,
					// SWT.NONE);
					// item.setText(0, itemColums[2]);
					// item.setText(1, itemColums[1]);
					// item.setText(2, itemColums[0]);

					String itemId = itemColums[0];
					String itemType = itemColums[1];
					String itemExpression = itemColums[2];

					if (Integer.parseInt(itemType) == 0) {
						// Error Detail
						String itemFuncName = itemColums[3];
						String itemPath = itemColums[4];
						String itemLine = itemColums[5];
						String itemErrorType = "";
						
						// Check if the line has an error type
						if(itemColums.length == 7){
							itemErrorType = itemColums[6];
						}
						
						if (AutofloxView.consoleTableMap.containsKey(itemId)) {
							TreeItem errorDetailItem = new TreeItem(
									AutofloxView.consoleTableMap.get(itemId),
									SWT.NONE);
							errorDetailItem.setText(new String[] {
									itemExpression.trim(), itemPath, "line " + itemLine,
									itemErrorType });
						}
					} else {
						// Error Entry
						if (!AutofloxView.consoleTableMap.containsKey(itemId)) {
							// Create the new error entry
							TreeItem newErrorTreeItem = new TreeItem(
									AutofloxView.PanelTree, SWT.NONE);
							AutofloxView.consoleTableMap.put(itemId,
									newErrorTreeItem);

							// Add the error entry messages to console
							newErrorTreeItem.setText(new String[] {
									"Code-terminating DOM-related Error: " + itemExpression, "", "", "" });
						}
					}

					// System.err.println(itemId + " " + itemType + " "
					// + itemExpression + " " + itemFuncName + " " + itemPath
					// + " " + itemLine);
				}
			}
		});
	}
}