/*
    Automatic JavaScript Invariants is a plugin for Crawljax that can be
    used to derive JavaScript invariants automatically and use them for
    regressions testing.
    Copyright (C) 2010  crawljax.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */
package com.plugin.autoflox.service.aji.executiontracer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;

import daikon.Daikon;

/**
 * Crawljax Plugin that reads an instrumentation array from the webbrowser and
 * saves the contents in a Daikon trace file.
 * 
 * @author Frank Groeneveld
 * @version $Id: JSExecutionTracer.java 6162 2009-12-16 13:56:21Z frank $
 */
public class JSExecutionTracer{

	private static final int ONE_SEC = 1000;

	private static String outputFolder;
	private static String assertionFilename;

	public static JSONArray points = new JSONArray();

	public static final String EXECUTIONTRACEDIRECTORY = "executiontrace/";
	public static String currentState;
	
	// traceVector keeps track of all trace files generated, ordered by date
	public static Vector traceVector = new Vector();

	/**
	 * @param filename
	 *            How to name the file that will contain the assertions after
	 *            execution.
	 */
	public JSExecutionTracer(String filename) {
		assertionFilename = filename;
	}

	/**
	 * Retrieves the JavaScript instrumentation array from the webbrowser and
	 * writes its contents in Daikon format to a file.
	 * 
	 * @param session
	 *            The crawling session.
	 * @param candidateElements
	 *            The candidate clickable elements.
	 */
	public static void generateTrace(String state, String outputFolder) {
		
		currentState = state;

		String filename = outputFolder + EXECUTIONTRACEDIRECTORY
				+ "jsexecutiontrace-";
		filename += state;

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = new Date();
		filename += dateFormat.format(date) + ".dtrace";
		
		traceVector.addElement(filename);
		System.out.println("traceVector size: " + traceVector.size());

		try {

			/*
			 * FIXME: Frank, hack to send last buffer items and wait for them to
			 * arrive
			 */
			//session.getBrowser().executeJavaScript("sendReally();");
			Thread.sleep(ONE_SEC);

			Trace trace = Trace.parse(points);

			PrintWriter file = new PrintWriter(filename);
			file.write(trace.getDeclaration());
			file.write('\n');
			file.write(trace.getData(points));
			file.close();

			points = new JSONArray();

		} catch (Exception e) {
			e.printStackTrace();
		}

		/* FROLIN - TRY TO RETRIEVE DOM */
		/*
		try {
			NodeList nlist = session.getCurrentState().getDocument()
					.getElementsByTagName("*");
			System.out.println("State IDs");
			for (int i = 0; i < nlist.getLength(); i++) {
				Element e = (Element) nlist.item(i);
				if (e.hasAttribute("id")) {
					System.out.println(e.getAttribute("id"));
				}
			}
			System.out.println("\n");
		} catch (Exception ee) {
			System.out.println("Error: Exception when retrieving document");
			System.exit(-1);
		}
		*/
		/* END TRY TO RETRIEVE DOM */
	}

	/**
	 * Get a list with all trace files in the executiontracedirectory.
	 * 
	 * @return The list.
	 */
	public List<String> allTraceFiles() {
		ArrayList<String> result = new ArrayList<String>();

		/* find all trace files in the trace directory */
		File dir = new File(getOutputFolder() + EXECUTIONTRACEDIRECTORY);

		String[] files = dir.list();
		if (files == null) {
			return result;
		}
		for (String file : files) {
			if (file.endsWith(".dtrace")) {
				result.add(getOutputFolder() + EXECUTIONTRACEDIRECTORY + file);
			}
		}

		return result;
	}


	/**
	 * @return Name of the assertion file.
	 */
	public String getAssertionFilename() {
		return assertionFilename;
	}

	public static String getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(String absolutePath) {
		outputFolder = absolutePath;
	}

	/**
	 * Dirty way to save program points from the proxy request threads. TODO:
	 * Frank, find cleaner way.
	 * 
	 * @param string
	 *            The JSON-text to save.
	 */
	public static void addPoint(String string) {
		JSONArray buffer = null;
		try {
			buffer = new JSONArray(string);
			for (int i = 0; i < buffer.length(); i++) {
				points.put(buffer.get(i));
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
}
