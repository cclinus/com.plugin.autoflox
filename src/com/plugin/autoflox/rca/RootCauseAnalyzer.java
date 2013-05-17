package com.plugin.autoflox.rca;

//import daikon.tools.*;
//import daikon.chicory.*;
import java.io.*;
import java.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableItem;
//import com.google.javascript.rhino.*;
import org.mozilla.javascript.*;

import com.plugin.autoflox.service.FileManager;
import com.plugin.autoflox.views.AutofloxView;

public class RootCauseAnalyzer {
	private static String JS_SOURCE_FOLDER;

	public static void main(String[] args) {
		// DTraceReader d_trace = new DTraceReader();
		// File d_trace_file = new File("test.dtrace");

		// d_trace.read(d_trace_file);
		// d_trace.dump_data();

		// testFunctionSequenceParser();
		// extractSequences(ft);
		// testExtractSequence();
		// testStitchSequences();
		// testCompiler();
		// testJSExecute();
		// testGetGEBIDLine();
		// testGEBIDLineFinder();
		// testTaskFreak();
		// testTudu();
		// testWordPress();
		// testTumblr();
		// System.out.println(findErrorMsg("/new_jsassertions/executiontrace/jsexecutiontrace-index20111206185421.dtrace"));
	}

	public RootCauseAnalyzer(String jsSourceFolder) {
		this.JS_SOURCE_FOLDER = jsSourceFolder;
	}

	public static List<List<FunctionTrace>> extractSequences(
			List<FunctionTrace> ft) {
		List<List<FunctionTrace>> sequences = new ArrayList<List<FunctionTrace>>();
		List<FunctionTrace> sequence = new ArrayList<FunctionTrace>();
		Iterator it = ft.iterator();

		boolean first_function = true;
		String first_function_ppt = null;
		String first_function_trace_type = null;
		boolean finishedSequence = true;
		// TODO: Handle recursive functions
		while (it.hasNext()) {
			FunctionTrace next_ft = (FunctionTrace) it.next();
			if (first_function) {
				sequence = new ArrayList<FunctionTrace>();
				first_function = false;
				sequence.add(next_ft);

				int index_from_colons = next_ft.f_decl.ppt_decl.indexOf(":::");
				first_function_ppt = next_ft.f_decl.ppt_decl.substring(0,
						index_from_colons);
				first_function_trace_type = next_ft.f_decl.ppt_decl
						.substring(index_from_colons);

				finishedSequence = false;
			} else {
				sequence.add(next_ft);

				// Determine if this is the last trace in the sequence
				int index_from_colons = next_ft.f_decl.ppt_decl.indexOf(":::");
				String function_ppt = next_ft.f_decl.ppt_decl.substring(0,
						index_from_colons);
				String function_trace_type = next_ft.f_decl.ppt_decl
						.substring(index_from_colons);

				boolean function_ppt_match = function_ppt
						.equals(first_function_ppt);
				boolean function_trace_type_exit = function_trace_type
						.startsWith(":::EXIT");
				boolean function_trace_type_error = function_trace_type
						.startsWith(":::ERROR");

				if (function_ppt_match
						&& (function_trace_type_exit || function_trace_type_error)) {
					sequences.add(sequence);
					first_function = true;
					finishedSequence = true;
				}
			}
		}

		if (!finishedSequence) {
			sequences.add(sequence);
		}

		return sequences;
	}

	public static List<FunctionTrace> extractRelevantSequence(
			List<List<FunctionTrace>> llft) {
		List<FunctionTrace> relevantSequence = null;

		Iterator it = llft.iterator();
		List<FunctionTrace> lft = null;
		List<FunctionTrace> finalFunctionTrace = new ArrayList<FunctionTrace>();
		boolean foundErrorTrace = false;
		while (it.hasNext()) {
			lft = (List<FunctionTrace>) it.next();
			Iterator it_trace = lft.iterator();
			while (it_trace.hasNext()) {
				FunctionTrace ft = (FunctionTrace) it_trace.next();

				int index_from_colons = ft.f_decl.ppt_decl.indexOf(":::");
				if (ft.f_decl.ppt_decl.substring(index_from_colons).equals(
						":::ERROR")) {
					foundErrorTrace = true;
					break;
				}
			}
			if (foundErrorTrace) {
				break;
			}
		}

		// Return last sequence if no error trace is found
		relevantSequence = lft;
		if (lft == null) {
			System.out.println("Error: Empty list of sequences");
			System.exit(-1);
		} else if (foundErrorTrace) {
			// Remove the "ERROR" trace(s)
			Iterator relevantSequence_it = lft.iterator();
			while (relevantSequence_it.hasNext()) {
				FunctionTrace ft = (FunctionTrace) relevantSequence_it.next();

				int index_from_colons = ft.f_decl.ppt_decl.indexOf(":::");
				if (ft.f_decl.ppt_decl.substring(index_from_colons).equals(
						":::ERROR")) {
					break;
				} else {
					finalFunctionTrace.add(ft);
				}
			}
		} else {
			finalFunctionTrace = lft;
		}

		if (finalFunctionTrace.isEmpty()) {
			System.out.println("Error: Empty list of sequences");
			System.exit(-1);
		}

		return finalFunctionTrace;
	}

	public static boolean sequenceIsAsync(List<FunctionTrace> ft) {
		if (ft == null) {
			return false;
		}

		Iterator it = ft.iterator();

		if (!it.hasNext()) {
			return false;
		} else {
			FunctionTrace firstTrace = (FunctionTrace) it.next();
			int index_from_colons = firstTrace.f_decl.ppt_decl.indexOf(":::");
			if (firstTrace.f_decl.ppt_decl.substring(index_from_colons).equals(
					":::ASYNC")) {
				return true;
			}
		}

		return false;
	}

	public static List<FunctionTrace> stitchSequences(
			List<List<FunctionTrace>> sequences,
			List<FunctionTrace> relevantSequence) {
		if (sequences.isEmpty() || relevantSequence.isEmpty()) {
			System.out.println("Error: Empty list of sequences");
			System.exit(-1);
		}

		List<FunctionTrace> stitchedSeq = new ArrayList<FunctionTrace>();

		List<FunctionTrace> curr_sequence = relevantSequence;
		ListIterator rs_it = curr_sequence.listIterator(curr_sequence.size());
		// Iterator seq_it = sequences.iterator();

		while (rs_it.hasPrevious()) {
			FunctionTrace ft = (FunctionTrace) rs_it.previous();
			int index_from_colons = ft.f_decl.ppt_decl.indexOf(":::");
			String postfix = ft.f_decl.ppt_decl.substring(index_from_colons);

			if (!postfix.equals(":::ASYNC")) {
				stitchedSeq.add(ft);
			} else {
				stitchedSeq.add(ft);

				// Record ID
				// First, determine where RCA_timerID is found
				int rca_timerID_index = -1;
				Iterator vars = ft.f_decl.var_descs.iterator();
				int var_counter = 0;
				while (vars.hasNext()) {
					VariableDesc varDesc = (VariableDesc) vars.next();
					if (varDesc.getVarName().equals("RCA_timerID")) {
						rca_timerID_index = var_counter;
						break;
					}
					var_counter++;
				}

				if (rca_timerID_index == -1) {
					System.out.println("Error: Async ID missing");
					System.exit(-1);
				}
				int asyncID = Integer.parseInt(ft.var_values
						.get(rca_timerID_index));

				// Look through all the sequences and find corresponding async
				// call
				List<FunctionTrace> callingSeq = null;
				Iterator seq_it = sequences.iterator();
				List<FunctionTrace> trace_seq = null;
				int async_call_index = -1;

				boolean async_call_found = false;

				while (seq_it.hasNext() && !async_call_found) {
					trace_seq = (List<FunctionTrace>) seq_it.next();
					Iterator trace_seq_it = trace_seq.iterator();

					int counter = 0;
					while (trace_seq_it.hasNext() && !async_call_found) {
						FunctionTrace trace = (FunctionTrace) trace_seq_it
								.next();
						index_from_colons = trace.f_decl.ppt_decl
								.indexOf(":::");
						if (trace.f_decl.ppt_decl.substring(index_from_colons)
								.equals(":::ASYNC_CALL")) {
							// Get value of RCA_timerID, which is always at
							// index 0 for ASYNC_CALL-type traces
							int asyncCall_ID = Integer
									.parseInt(trace.var_values.get(0));
							if (asyncID == asyncCall_ID) {
								async_call_found = true;
								async_call_index = counter;
								callingSeq = trace_seq;
							}
						}
						counter++;
					}
				}

				if (async_call_found) {
					rs_it = callingSeq.listIterator(async_call_index + 1);
				}
			}
		}

		Collections.reverse(stitchedSeq);

		return stitchedSeq;
	}

	public static String findErrorMsg(String filepath) {
		TraceParser tp = new TraceParser(filepath);

		String errMsg = null;

		for (FunctionTrace ft : tp.function_traces) {

			if (ft.f_decl.f_name.substring(ft.f_decl.f_name.lastIndexOf(":::"))
					.equals(":::ERROR")) {
				// Get the error message
				int varCounter = 0;
				for (VariableDesc v : ft.f_decl.var_descs) {
					if (v.getVarName().equals("RCA_errorMsg")) {

						errMsg = ft.var_values.get(varCounter);
						errMsg = errMsg.substring(1, errMsg.length() - 1);
						return errMsg;
					}
					varCounter++;
				}
			}
		}

		return errMsg;
	}

	public static boolean findGEBID(String filepath, String initNullVar,
			int errorId) throws IOException {
		TraceParser tp = new TraceParser(filepath);

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> lft = null;
		lft = stitchSequences(sequences, relevantSequence);

		// We're assuming the last trace is always the one corresponding to
		// erroneous line
		GEBIDLineFinder gebid_line = new GEBIDLineFinder(initNullVar, lft);
		gebid_line.setErrorId(errorId);
		gebid_line.setJsSourceFolder(JS_SOURCE_FOLDER);

		gebid_line.findGEBIDLine();

		if (gebid_line.getGEBIDLineFt() != null) {

			FunctionTrace gebidLineFt = gebid_line.getGEBIDLineFt();
			// System.err.println(gebidLineFt.f_decl.ppt_decl);
			// System.err.println(gebidLineFt.f_decl.f_name);

			ArrayList<String> traceTable = gebid_line.getTraceTable();
			
			// Add type for each line
			String firstLine = traceTable.get(0);
			firstLine = firstLine + "Error Thrown:::";
			traceTable.set(0, firstLine);
			
			String lastLine = traceTable.get(traceTable.size()-1);
			lastLine = lastLine + "Direct DOM Access:::";
			traceTable.set(traceTable.size()-1, lastLine);

			// String funcName = gebidLineFt.f_decl.f_name;
			// String pptDecl = gebidLineFt.f_decl.ppt_decl;
			// String combinedOutput = pptDecl.replace("."+funcName,
			// ":::"+funcName+":::");

			outputTableItem(traceTable);

			return true;

		} else {
			return false;
		}
	}

	// Dump trace to file, in view table format
	// Append
	public static void outputTableItem(ArrayList<String> traceTable) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(FileManager.getTableResultFolder() + new Date().getTime(), true)));
			while (traceTable.size() > 0) {
				String item = traceTable.remove(traceTable.size() - 1);
				out.println(item);
			}
			out.close();
		} catch (IOException e) {
			// oh noes!
		}
	}

	public static void testFunctionSequenceParser() {
		// TraceParser tp = new TraceParser("test.dtrace");
		TraceParser tp = new TraceParser("test4.dtrace");

		// Iterate through list of FunctionDecl and print
		System.out.println("Test FunctionDecl list");
		Iterator it = tp.function_decls.iterator();
		while (it.hasNext()) {
			FunctionDecl fd = (FunctionDecl) it.next();
			System.out.println(fd.f_name);
		}

		System.out.println("\nTest FunctionDecl list access");

		Collections.sort(tp.function_decls, new FunctionDeclComparator());
		FunctionDecl fd_to_compare = new FunctionDecl();
		// fd_to_compare.ppt_decl =
		// "http://frolinsfilms.110mb.com:80/sample_page.htmlscript0.isOdd:::INTERMEDIATE6";
		fd_to_compare.ppt_decl = "http://frolinsfilms.110mb.com:80/sample_page.htmlscript0.z_func1:::INTERMEDIATE1";
		int index = Collections.binarySearch(tp.function_decls, fd_to_compare,
				new FunctionDeclComparator());
		System.out.println("Index: " + index);
		FunctionDecl new_fd = tp.function_decls.get(index);
		System.out.println(new_fd.f_name);
		System.out.println(new_fd.ppt_decl);
		System.out.println(new_fd.ppt_type);

		Iterator new_it = new_fd.var_descs.iterator();
		while (new_it.hasNext()) {
			VariableDesc vd = (VariableDesc) new_it.next();
			System.out.println(vd.getVarName());
			System.out.println(vd.getVarKind());
			System.out.println(vd.getDecType());
			System.out.println(vd.getRepType());
		}

		System.out.println("\nTest FunctionTrace list");

		it = tp.function_traces.iterator();
		while (it.hasNext()) {
			FunctionTrace ft = (FunctionTrace) it.next();
			System.out.println(ft.f_decl.ppt_decl);
			Iterator var_it = ft.var_values.iterator();
			while (var_it.hasNext()) {
				System.out.println(var_it.next());
			}
		}

		System.out.println("\nDone!");
	}

	public static void testExtractSequence() {
		// TraceParser tp = new TraceParser("test.dtrace");
		// TraceParser tp = new TraceParser("test4.dtrace");
		// TraceParser tp = new TraceParser("test5.dtrace");
		TraceParser tp = new TraceParser("test6.dtrace");

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		Iterator it = sequences.iterator();
		int counter = 0;
		while (it.hasNext()) {
			counter++;
			System.out.println("\nTrace " + counter);
			List<FunctionTrace> l_ft = (List<FunctionTrace>) it.next();
			Iterator l_ft_iter = l_ft.iterator();
			while (l_ft_iter.hasNext()) {
				FunctionTrace ft = (FunctionTrace) l_ft_iter.next();
				System.out.println(ft.f_decl.ppt_decl);
			}
		}
	}

	public static void testStitchSequences() {
		TraceParser tp = new TraceParser("test6.dtrace");

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> stitchedSequence = stitchSequences(sequences,
				relevantSequence);

		Iterator it = stitchedSequence.iterator();

		while (it.hasNext()) {
			FunctionTrace ft = (FunctionTrace) it.next();
			System.out.println(ft.f_decl.ppt_decl);
		}
	}

	public static void testCompiler() {
		Parser ps = new Parser(new CompilerEnvirons());
		TokenStream ts = ps
				.initForUnitTest(
						new StringReader(
								"var y = $('hello'); var x = document.getElementById('hello');"),
						"", 1, false);
		try {
			// Token t = new Token();
			String t_name = null;
			int token;

			token = ts.getToken();
			while (Token.typeToName(token) != "EOF") {
				t_name = Token.typeToName(token);
				System.out.println(t_name);
				if (t_name.equals("NAME") || t_name.equals("STRING")) {
					System.out.println(ts.getString());
				}
				token = ts.getToken();
			}
		} catch (IOException ie) {
			System.out.println("Error");
		}
	}

	public static void testJSExecute() {
		try {
			Context cx = Context.enter();
			Scriptable scope = cx.initStandardObjects();
			// Object result = cx.evaluateString(scope,
			// "x=null;x.innerHTML = 5;", "test_script.js", 1, null);
			Object result = cx.evaluateReader(scope, new FileReader(
					"test_script.js"), "test_script.js", 1, null);
			System.out.println(Context.toString(result));
		} catch (IOException ie) {
			System.out.println("Error: IO Exception");
		} finally {
			Context.exit();
		}
	}

	public static void testGetGEBIDLine() {
		// TraceParser tp = new TraceParser("test.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline2.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline3.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline4.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline5.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline6.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline7.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline8.dtrace");
		TraceParser tp = new TraceParser(
				"/home/cclinus/runtime-EclipseApplication/autoflox_proxy/jsSource");

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		// Iterator it = sequences.iterator();
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> lft = null;
		// while (it.hasNext()) {
		// lft = (List<FunctionTrace>)it.next();
		// }
		lft = stitchSequences(sequences, relevantSequence);

		// We're assuming the last trace is always the one corresponding to
		// erroneous line
		// GEBIDLine gebid_line = new GEBIDLine(lft,"find_isOdd","the_elem");
		// //normally, the null_var is deduced from the error message
		// GEBIDLine gebid_line = new GEBIDLine(lft,"dummy_func2","t");
		// GEBIDLine gebid_line = new GEBIDLine(lft,"test_func","t_id");
		// GEBIDLine gebid_line = new GEBIDLine(lft,"x_func","x_b");
		// GEBIDLine gebid_line = new GEBIDLine(lft,"y_func","y_b");
		// GEBIDLine gebid_line = new GEBIDLine(lft,"z_func4","z_id");
		// GEBIDLine gebid_line = new GEBIDLine(lft,"z_func4","z_id");
		// GEBIDLine gebid_line = new GEBIDLine(lft,"b_func_async","b_id4");
		// GEBIDLine gebid_line = new GEBIDLine(lft,"b_func_async","b_id4");
		GEBIDLine gebid_line = new GEBIDLine(lft, "b_func_async", "b_id4");
		System.out.println("Function: " + gebid_line.GEBID_function);
		System.out.println("Line Number: " + gebid_line.GEBID_line);
		System.out.println("Function (according to trace): "
				+ gebid_line.GEBID_trace.f_decl.f_name);

		System.out.println("\nID:");

		Iterator tokens_it = gebid_line.GEBID_Id.iterator();
		while (tokens_it.hasNext()) {
			String next_token = (String) tokens_it.next();
			System.out.println(next_token);
		}
		// System.out.println(gebid_line.GEBID)
	}

	public static void testGEBIDLineFinder() throws IOException {
		// TraceParser tp = new TraceParser("test.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline2.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline3.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline4.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline5.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline6.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline7.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline8.dtrace");
		// TraceParser tp = new TraceParser("test_gebidline9.dtrace");
		// TraceParser tp = new TraceParser("tumblr_execution_trace.dtrace");
		TraceParser tp = new TraceParser("taskfreak_execution_trace.dtrace");

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		// Iterator it = sequences.iterator();
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> lft = null;
		// while (it.hasNext()) {
		// lft = (List<FunctionTrace>)it.next();
		// }
		lft = stitchSequences(sequences, relevantSequence);

		// We're assuming the last trace is always the one corresponding to
		// erroneous line
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("the_elem", lft);
		// //normally, the null_var is deduced from the error message
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("t", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("t_id", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("x_b", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("y_b", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("z_id", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("z_id", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("b_id4", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("b_id4", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("b_id4", lft);
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft);
		GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft);

		gebid_line.findGEBIDLine();

		if (gebid_line.getGEBIDLineFt() != null) {
			System.out.println("Found it!");

			FunctionTrace gebidLineFt = gebid_line.getGEBIDLineFt();
			System.out.println(gebidLineFt.f_decl.ppt_decl);
		} else {
			System.out.println("Can't find GEBID Line!");
		}
	}

	public static void testTaskFreak() throws IOException {
		// TraceParser tp = new TraceParser("taskfreak_execution_trace.dtrace");
		// //1
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace2.dtrace"); //2
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace3.dtrace"); //3
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace4.dtrace"); //4
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace5.dtrace"); //5
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace6.dtrace"); //6
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace7.dtrace"); //7
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace8.dtrace"); //8
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace9.dtrace"); //9
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace10.dtrace"); //10
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace11.dtrace"); //11
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace12.dtrace"); //12
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace13.dtrace"); //13
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace14.dtrace"); //14
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace15.dtrace"); //15
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace16.dtrace"); //16
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace17.dtrace"); //17
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace18.dtrace"); //18
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace19.dtrace"); //19
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace20.dtrace"); //20
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace21.dtrace"); //21
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace22.dtrace"); //22
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace23.dtrace"); //23
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace24.dtrace"); //24
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace25.dtrace"); //25
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace26.dtrace"); //26
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace27.dtrace"); //27
		// TraceParser tp = new
		// TraceParser("taskfreak_execution_trace28.dtrace"); //28
		TraceParser tp = new TraceParser("taskfreak_execution_trace29.dtrace"); // 29

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> lft = null;
		lft = stitchSequences(sequences, relevantSequence);

		System.out.println("tfdsarr");

		// We're assuming the last trace is always the one corresponding to
		// erroneous line
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //1
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //2
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //3
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //4
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //5
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("f", lft); //6
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //7
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //8
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //9
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //10
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("x", lft); //11
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //12
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //13
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //14
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //15
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //16
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //17
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //18
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //19
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //20
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //21
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //22
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //23
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //24
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("e", lft); //25
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("fm", lft); //26
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("x", lft); //27
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("x", lft); //28
		GEBIDLineFinder gebid_line = new GEBIDLineFinder("x", lft); // 29

		gebid_line.findGEBIDLine();

		if (gebid_line.getGEBIDLineFt() != null) {
			System.out.println("Found it!");

			FunctionTrace gebidLineFt = gebid_line.getGEBIDLineFt();
			System.out.println(gebidLineFt.f_decl.ppt_decl);
		} else {
			System.out.println("Can't find GEBID Line!");
		}
	}

	public static void testTudu() throws IOException {
		// TraceParser tp = new TraceParser("tudu_execution_trace1.dtrace"); //1
		// TraceParser tp = new TraceParser("tudu_execution_trace2.dtrace"); //2
		// TraceParser tp = new TraceParser("tudu_execution_trace3.dtrace"); //3
		// TraceParser tp = new TraceParser("tudu_execution_trace4.dtrace"); //4
		// TraceParser tp = new TraceParser("tudu_execution_trace5.dtrace"); //5
		// TraceParser tp = new TraceParser("tudu_execution_trace6.dtrace"); //6
		// TraceParser tp = new TraceParser("tudu_execution_trace7.dtrace"); //7
		// TraceParser tp = new TraceParser("tudu_execution_trace8.dtrace"); //8
		// TraceParser tp = new TraceParser("tudu_execution_trace9.dtrace"); //9
		// TraceParser tp = new TraceParser("tudu_execution_trace10.dtrace");
		// //10
		// TraceParser tp = new TraceParser("tudu_execution_trace11.dtrace");
		// //11
		// TraceParser tp = new TraceParser("tudu_execution_trace12.dtrace");
		// //12
		// TraceParser tp = new TraceParser("tudu_execution_trace13.dtrace");
		// //13
		// TraceParser tp = new TraceParser("tudu_execution_trace14.dtrace");
		// //14
		// TraceParser tp = new TraceParser("tudu_execution_trace15.dtrace");
		// //15
		// TraceParser tp = new TraceParser("tudu_execution_trace16.dtrace");
		// //16
		// TraceParser tp = new TraceParser("tudu_execution_trace17.dtrace");
		// //17
		// TraceParser tp = new TraceParser("tudu_execution_trace18.dtrace");
		// //18
		// TraceParser tp = new TraceParser("tudu_execution_trace19.dtrace");
		// //19
		// TraceParser tp = new TraceParser("tudu_execution_trace20.dtrace");
		// //20
		// TraceParser tp = new TraceParser("tudu_execution_trace21.dtrace");
		// //21
		// TraceParser tp = new TraceParser("tudu_execution_trace22.dtrace");
		// //22
		// TraceParser tp = new TraceParser("tudu_execution_trace23.dtrace");
		// //23
		TraceParser tp = new TraceParser("tudu_execution_trace24.dtrace"); // 24

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> lft = null;
		lft = stitchSequences(sequences, relevantSequence);

		System.out.println("testing");

		// We're assuming the last trace is always the one corresponding to
		// erroneous line
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //1
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //2
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //3
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //4
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //5
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //6
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //7
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //8
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //9
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //10
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //11
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //12
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //13
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //14
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //15
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //16
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //17
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //18
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //19
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //20
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //21
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("y", lft); //22
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("url", lft); //23
		GEBIDLineFinder gebid_line = new GEBIDLineFinder("url", lft); // 24

		gebid_line.findGEBIDLine();

		if (gebid_line.getGEBIDLineFt() != null) {
			System.out.println("Found it!");

			FunctionTrace gebidLineFt = gebid_line.getGEBIDLineFt();
			System.out.println(gebidLineFt.f_decl.ppt_decl);
		} else {
			System.out.println("Can't find GEBID Line!");
		}
	}

	public static void testWordPress() throws IOException {
		// TraceParser tp = new TraceParser("wp_execution_trace1.dtrace"); //1
		// TraceParser tp = new TraceParser("wp_execution_trace2.dtrace"); //2
		// TraceParser tp = new TraceParser("wp_execution_trace3.dtrace"); //3
		// TraceParser tp = new TraceParser("wp_execution_trace4.dtrace"); //4
		// TraceParser tp = new TraceParser("wp_execution_trace5.dtrace"); //5
		// TraceParser tp = new TraceParser("wp_execution_trace6.dtrace"); //6
		// TraceParser tp = new TraceParser("wp_execution_trace7.dtrace"); //7
		// TraceParser tp = new TraceParser("wp_execution_trace8.dtrace"); //8
		// TraceParser tp = new TraceParser("wp_execution_trace9.dtrace"); //9
		// TraceParser tp = new TraceParser("wp_execution_trace10.dtrace"); //10
		// TraceParser tp = new TraceParser("wp_execution_trace11.dtrace"); //11
		// TraceParser tp = new TraceParser("wp_execution_trace12.dtrace"); //12
		TraceParser tp = new TraceParser("wp_execution_trace13.dtrace"); // 13

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> lft = null;
		lft = stitchSequences(sequences, relevantSequence);

		System.out.println("testing again");

		// We're assuming the last trace is always the one corresponding to
		// erroneous line
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //1
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //2
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //3
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //4
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("cont", lft); //5
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("t.Canvas", lft);
		// //6
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //7
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //8
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //9
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft); //10
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("f", lft); //11
		// GEBIDLineFinder gebid_line = new GEBIDLineFinder("h", lft); //12
		GEBIDLineFinder gebid_line = new GEBIDLineFinder("x", lft); // 13

		gebid_line.findGEBIDLine();

		if (gebid_line.getGEBIDLineFt() != null) {
			System.out.println("Found it!");

			FunctionTrace gebidLineFt = gebid_line.getGEBIDLineFt();
			System.out.println(gebidLineFt.f_decl.ppt_decl);
		} else {
			System.out.println("Can't find GEBID Line!");
		}
	}

	public static void testTumblr() throws IOException {
		long startTime = System.nanoTime();
		long endTime;

		TraceParser tp = new TraceParser("tumblr_execution_trace.dtrace");

		List<List<FunctionTrace>> sequences = extractSequences(tp.function_traces);
		List<FunctionTrace> relevantSequence = extractRelevantSequence(sequences);

		List<FunctionTrace> lft = null;
		lft = stitchSequences(sequences, relevantSequence);

		// We're assuming the last trace is always the one corresponding to
		// erroneous line
		GEBIDLineFinder gebid_line = new GEBIDLineFinder("", lft);

		gebid_line.findGEBIDLine();

		endTime = System.nanoTime();
		long duration = endTime - startTime;

		System.out.println("TOTAL TIME (ns): " + duration);

		if (gebid_line.getGEBIDLineFt() != null) {
			System.out.println("Found it!");

			FunctionTrace gebidLineFt = gebid_line.getGEBIDLineFt();
			System.out.println(gebidLineFt.f_decl.ppt_decl);
		} else {
			System.out.println("Can't find GEBID Line!");
		}
	}
}