package com.plugin.autoflox.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TreeItem;

import com.plugin.autoflox.action.AutofloxRunAction;
import com.plugin.autoflox.service.FileManager;

public class AutofloxViewUpdateThread extends Thread {

	public static boolean runFlag = true;

	public static void terminate() {
		runFlag = false;
	}

	public void run() {
		runFlag = true;
		while (runFlag) {
			// Check if the gui table result file exists
			File dir = new File(FileManager.getTableResultFolder());
			File[] tableResultItems = dir.listFiles();
			if (tableResultItems != null && tableResultItems.length > 0) {
				Arrays.sort(tableResultItems);
				InputStream fis;
				BufferedReader br;
				String line;

				try {
					fis = new FileInputStream(tableResultItems[0]);

					br = new BufferedReader(new InputStreamReader(fis,
							Charset.forName("UTF-8")));
					while ((line = br.readLine()) != null) {
						updateViewTable(line);
					}

					// Clean the file
					tableResultItems[0].delete();

					fis.close();
					br.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		System.out.println("AutofloxViewUpdateThread terminated");

	}

	private static void updateViewTable(final String itemContent) {

		/**
		 *  itemContent is the format:
		 *	PathToFile:::FunctionName:::Type+LineNo
		 */ 
		System.out.println("Adding from tableResult: " + itemContent);
		final String[] itemColums = itemContent.split(":::");

		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				synchronized (this) {

					final String itemId = itemColums[0];
					String itemType = itemColums[1];
					String itemExpression = itemColums[2];

					if (Integer.parseInt(itemType) == 0) {
						// Error Detail
						String itemFuncName = itemColums[3];
						String itemPath = itemColums[4];
						String itemLine = itemColums[5];
						String itemErrorType = "";

						// Check if the line has an error type
						if (itemColums.length == 7) {
							itemErrorType = itemColums[6];
						}

						if (AutofloxView.consoleTableMap.containsKey(itemId)) {
							TreeItem errorDetailItem = new TreeItem(
									AutofloxView.consoleTableMap.get(itemId),
									SWT.NONE);
							errorDetailItem.setText(new String[] {
									itemExpression.trim(), itemPath,
									"line " + itemLine, itemErrorType });

							// Add click to line func
							AutofloxView.PanelTree.addListener(SWT.Selection,
									new Listener() {
										public void handleEvent(Event e) {
											TreeItem[] selection = AutofloxView.PanelTree
													.getSelection();
											if (selection.length > 0) {
												String lineNoText = selection[0]
														.getText(2);
												lineNoText = lineNoText
														.replace("line ", "");
												if (lineNoText.trim() != "") {
													int lineNo = Integer
															.parseInt(lineNoText
																	.trim());
													String filePath = selection[0]
															.getText(1);
													AutofloxView
															.openFileInEditor(filePath);

													try {
														AutofloxView
																.goToLine(lineNo);
													} catch (BadLocationException e1) {
														e1.printStackTrace();
													}
												}
											}else{
												System.err.println("Nothing is selected.");
											}
										}
									});
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
									"Code-terminating DOM-related Error: "
											+ itemExpression, "", "", "" });
						}
					}

				}
			}
		});
	}
}