package com.plugin.autoflox.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

import com.plugin.autoflox.action.AutofloxRunAction;

public class AutofloxViewUpdateThread extends Thread {

	private static String RESULT_FILE_PATH = AutofloxRunAction.workspacePath + "/autoflox_proxy/bin/tableResult";
	public static boolean running = true;
	
	public static void terminate(){
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
					}

					// Clean the file
					file = new File(RESULT_FILE_PATH);
					file.delete();

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
		
		//itemContent is the format: 
		// PathToFile:::FunctionName:::Type+LineNo
		final String[] itemColums = itemContent.split(":::");
		
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Add new item to table");
				TableItem item = new TableItem(AutofloxView.resultTable,
						SWT.NONE);
				item.setText(0, itemColums[2]);
				item.setText(1, itemColums[1]);
				item.setText(2, itemColums[0]);
			}
		});
	}
}