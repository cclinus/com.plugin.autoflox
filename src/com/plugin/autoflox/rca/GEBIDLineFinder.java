package com.plugin.autoflox.rca;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.mozilla.javascript.*;

import com.plugin.autoflox.service.FileManager;

public class GEBIDLineFinder {
	private Stack<String> funcStack; // The function stack
	private String last_func; // Set to the last function popped before the
								// stack becomes empty
	private String null_var; // The current variable being tracked
	private FunctionTrace null_var_ft; // The FunctionTrace corresponding to the
										// current null_var
	private boolean wasReturned; // Indicates if the previous return statement's
									// return value is a function call

	private List<FunctionTrace> relevantSeq; // List of function traces in the
												// relevant sequence
	private int indexToChange; // index to change the iterator's starting
								// position in relevantSeq
	private boolean indexToChangeModified; // set to true if indexToChange has
											// just been modified

	private String JS_FILES_FOLDER; // = "js_files2";

	private FunctionTrace gebidLineFt; // The function trace corresponding to
										// the GEBID line

	private boolean calledFinder; // true if findGEBIDLine has already been
									// calleds

	public ArrayList<String> traceTable;
	public int errorId;

	/**
	 * The GEBIDLineFinder constructor
	 * 
	 * @param nullVar
	 *            The variable that became null
	 * @param seq
	 *            A list of function traces representing the relevant sequence
	 */
	public GEBIDLineFinder(String nullVar, List<FunctionTrace> seq) {
		funcStack = new Stack<String>();
		last_func = "";
		wasReturned = false;

		null_var = nullVar;
		relevantSeq = seq;
		indexToChangeModified = false;

		gebidLineFt = null;

		ListIterator<FunctionTrace> seqIt = relevantSeq
				.listIterator(relevantSeq.size());
		if (seqIt.hasPrevious()) {
			null_var_ft = seqIt.previous();
		} else {
			System.out.println("Error: Empty relevant sequence");
			System.exit(-1);
		}
	}

	public void setErrorId(int id) {
		this.errorId = id;
	}

	public ArrayList<String> getTraceTable() {
		return this.traceTable;
	}

	/**
	 * Set the JS source folder
	 * 
	 * @param folder
	 *            The JS source folder
	 */
	public void setJsSourceFolder(String folder) {
		JS_FILES_FOLDER = folder;
	}

	/**
	 * Finds the GEBID line and, once found, sets the value of gebidLineFt to
	 * the corresponding FunctionTrace
	 * <p>
	 * Can only be called once
	 * @throws IOException 
	 */
	public void findGEBIDLine() throws IOException {

		if (calledFinder) {
			System.out.println("Error: findGEBIDLine called multiple times");
			System.exit(-1);
		}

		// Set up iterator for the relevant sequence and determine the name of
		// the current function
		ListIterator<FunctionTrace> seqIterator = relevantSeq
				.listIterator(relevantSeq.size()); // start at end of the list
		if (relevantSeq.isEmpty()) {
			System.out.println("Error: Relevant sequence is empty");
			System.exit(-1);
		} else {
			FunctionTrace lastFt = seqIterator.previous();
			String firstFunctionName = this.getFunctionName(lastFt);
			pushFunc(firstFunctionName);

			// System.err.println(firstFunctionName);
		}

		// Reset iterator
		seqIterator = relevantSeq.listIterator(relevantSeq.size());
		int currIndex = relevantSeq.size();
		int paramNum = -1;

		// Variables to handle asynchronous calls
		boolean foundAsync = false; // true if a trace of type ASYNC has just
									// been encountered
		boolean foundAsyncCall = false; // true if a trace of type ASYNC_CALL
										// has just been encountered
		String callbackFunctionCall = null; // the call to the asynchronous
											// function made by the caller
											// function

		boolean firstLine = true;

		String executionTraceLine = null;
		traceTable = new ArrayList();

		while (seqIterator.hasPrevious()) {

			executionTraceLine = String.valueOf(this.errorId) + ":::" + "0"
					+ ":::"; // 0: item trace; 1: error item

			// Get next FunctionTrace in list
			FunctionTrace ft = seqIterator.previous();
			currIndex--;

			String currType = getLineType(ft);
			String currLine = "";
			List<String> lineTokens = new ArrayList<String>();
			if (!(currType.equals("ASYNC_CALL") || currType.equals("ASYNC"))) {
				currLine = getLine(ft);
				lineTokens = getTokenStream(currLine);

				// System.err.println(currLine);
				executionTraceLine += currLine + ":::";

				// Check the first line (i.e., the erroneous line) as this line
				// itself might be the one containing the GEBID line
				// No need to check if it's of the form "return <expr>" or an
				// assignment - subsequent checks will take care of that
				if (firstLine) {
					if (isNullSource(lineTokens)) {
						setGEBIDLineFt(ft);
						return;
					}
				}
			}
			String currFunction = getFunctionName(ft);

			// System.err.println(currFunction);
			executionTraceLine += currFunction + ":::";

			firstLine = false; // so that next line doesn't get recognized as
								// the first line

			/* ASYNC START */
			// Check if the current line is of type ASYNC
			if (currType.equals("ASYNC")) {
				foundAsync = true;
				continue;
			}

			// Check if an ASYNC has just been encountered
			if (foundAsync) {
				// First, assure that the current line is an ASYNC_CALL
				if (!currType.equals("ASYNC_CALL")) {
					System.out
							.println("Error: ASYNC not preceded by an ASYNC_CALL");
					System.exit(-1);
				} else {
					foundAsync = false;
					foundAsyncCall = true;
					callbackFunctionCall = this.getCallbackFunctionCall(ft);
				}
				continue;
			}

			// Check if an ASYNC_CALL has just been encountered
			if (foundAsyncCall) {
				// First, assure that the stack is empty
				assert (funcStackIsEmpty());

				// Also, assert that callbackFunctionCall is not null
				assert (callbackFunctionCall != null);

				// Change the current line to the corresponding callback
				// function call
				currLine = callbackFunctionCall;
				lineTokens = getTokenStream(currLine);

				// Reset foundAsyncCall
				foundAsyncCall = false;
				callbackFunctionCall = null;
			}

			if (!currType.equals("ENTER")) {
				// Get path
				String[] errorTraceInfo = ft.f_decl.ppt_decl.split(":::");
				String filePath = null;
				if (errorTraceInfo.length >= 1) {
					filePath = errorTraceInfo[0];
					filePath = filePath.replace("." + currFunction, "");
					filePath = filePath.replace(FileManager.getProxyInstrumentedFolder(), FileManager.getProjectFolder());
					executionTraceLine += filePath + ":::";
				}

				// Calculate line NO. regarding on the entire file
				int lineNoInFile = getLineNOInFile(filePath, getJSFuncDecl(ft), getFunctionLineno(ft));
				executionTraceLine += lineNoInFile + ":::";

				System.err.println("ExecutionTrace: " + executionTraceLine);

				// Update traceTable
				this.traceTable.add(executionTraceLine);
				System.err.println("Trace table size: "
						+ this.traceTable.size());

				// System.err.println("Line:"+getFunctionLineno(ft));
				// System.err.println("Func Declaration:"+getJSFuncDecl(ft));
				// System.err.println("File title: " + ft.f_decl.ppt_decl);
			}

			/* ASYNC END */

			// Check if the stack is empty
			if (funcStackIsEmpty()) {
				// The primary function will change. First, verify if the
				// current line is indeed a function call
				if (!isJSFunctionCall(lineTokens, last_func)) {
					System.out
							.println("Error: Primary function changed, but no corresponding function call");
					System.exit(-1);
				} else {
					pushFunc(currFunction);

					// If the current null_var is local, check the argument the
					// current
					// null_var corresponds to in the function call
					if (isVarLocal(null_var_ft, null_var)) {
						assert (paramNum != -1);
						String newNullVar = getArgument(lineTokens, paramNum,
								last_func);
						setNullVar(ft, newNullVar);
					}
				}
				continue;
			}

			// Check if the current line is an ENTER
			if (currType.equals("ENTER")) {
				// We must've reached the point where the current function was
				// called, so pop this function from the stack
				// First, check if the top of the stack equals the function name
				// of the current line
				if (!currFunction.equals(funcStack.peek())) {
					System.out.println("Error: Function name mismatch");
					System.exit(-1);
				} else {
					popFunc();
					if (isVarLocal(null_var_ft, null_var)) {
						String funcDecl = getJSFuncDecl(ft);
						List<String> funcDeclTokens = getTokenStream(funcDecl);
						paramNum = getParamNumber(funcDeclTokens, currFunction);
					}
				}
				continue;
			}

			// If this point is reached, the stack should be non-empty
			// Assumption: No recursive functions

			// Check if the current function does not match the function at the
			// top of the stack
			if (!currFunction.equals(funcStack.peek())) {
				// There must have been a function call in the function at the
				// top of the stack calling the current function
				// Thus, push the current function at the top of the stack
				assert (currType.equals("EXIT"));
				String callingFunction = funcStack.peek();
				pushFunc(currFunction);

				// Peek ahead to see if the corresponding function call is of
				// the form <null var> = <function call>
				if (isFuncCallAfterPeek(relevantSeq, currIndex, currFunction,
						callingFunction)) {
					// First, verify that the current expression is a return
					// expression
					if (!isReturnExpr(lineTokens)) {
						System.out
								.println("Error: Function from which return value is expected does not end with return expression");
						System.exit(-1);
					} else {
						// Get the return expression
						List<String> retExpr = returnExpr(lineTokens);

						// Check if the return expression is one of the GEBID
						// functions
						if (isNullSource(retExpr)) {
							setGEBIDLineFt(ft);
							return;
						} else if (isFuncCall(retExpr)) {
							wasReturned = true;
							continue;
						} else if (isVariable(retExpr)) {
							String newNullVar = getVariableName(retExpr);
							setNullVar(ft, newNullVar);
							continue;
						} else {
							System.out
									.println("Error: Return value is not a GEBID, a function call, or a variable name");
							System.exit(-1);
						}
					}
				} else {
					if (wasReturned) {
						wasReturned = false;

						// First, verify that the current expression is a return
						// expression
						if (!isReturnExpr(lineTokens)) {
							System.out
									.println("Error: Function from which return value is expected does not end with return expression");
							System.exit(-1);
						} else {
							// Get the return expression
							List<String> retExpr = returnExpr(lineTokens);

							// Check if the return expression is one of the
							// GEBID functions
							if (isNullSource(retExpr)) {
								setGEBIDLineFt(ft);
								return;
							} else if (isFuncCall(retExpr)) {
								wasReturned = true;
								continue;
							} else if (isVariable(retExpr)) {
								String newNullVar = getVariableName(retExpr);
								setNullVar(ft, newNullVar);
								continue;
							} else {
								System.out
										.println("Error: Return value is not a GEBID, a function call, or a variable name");
								System.exit(-1);
							}
						}
					} else if (isVarLocal(null_var_ft, null_var)) {
						// Move iterator to next line of the calling function
						if (!indexToChangeModified) {
							System.out
									.println("Error: Invalid indexToChange value about to be used");
							System.exit(-1);
						}
						seqIterator = relevantSeq.listIterator(indexToChange);
						currIndex = indexToChange;
						indexToChangeModified = false;

						// Pop the current function off the stack
						popFunc();
						continue;
					} else { // If null_var is global
						continue;
					}
				}
			}

			// At this point, the current function must have matched the top of
			// the stack

			// Check if the current line is of the form <null var> = expression
			if (isNullVarAssn(lineTokens)) {
				List<String> expr = this.nullVarAssn(lineTokens);
				if (isNullSource(expr)) {
					setGEBIDLineFt(ft);
					return;
				} else if (isVariable(expr)) {
					String newNullVar = getVariableName(expr);
					setNullVar(ft, newNullVar);
					continue;
				} else {
					System.out
							.println("Error: Expression is neither a GEBID nor a variable name");
					System.exit(-1);
				}
			} else {
				continue;
			}
		}
	}

	public int getLineNOInFile(String path, String funcName, int lineInFunc)
			throws IOException {
		int lineCounter = 1;
		File file = new File(path);
		if (file.exists()) {

			InputStream fis = null;
			BufferedReader br = null;
			String line;

			try {
				fis = new FileInputStream(path);

				br = new BufferedReader(new InputStreamReader(fis,
						Charset.forName("UTF-8")));
				while ((line = br.readLine()) != null) {
					if (line.contains(funcName)) {
						fis.close();
						br.close();
						return lineCounter + lineInFunc;
					}
					lineCounter++;
				}
			} finally {
				br.close();
				fis.close();
			}
		}

		return 0;
	}

	/**
	 * Returns a function trace representing the line determined to contain the
	 * GEBID call
	 * 
	 * @return The value of gebidLineFt
	 */
	public FunctionTrace getGEBIDLineFt() {
		return this.gebidLineFt;
	}

	/**
	 * Sets gebidLineFt to some function trace
	 * 
	 * @param ft
	 *            The function trace to set gebidLineFt to
	 */
	private void setGEBIDLineFt(FunctionTrace ft) {
		this.gebidLineFt = ft;
	}

	/**
	 * Fetches the line corresponding to the function trace
	 * <p>
	 * The JS code for a function must be saved in a file called func.js where
	 * func is the name of the JS function
	 * 
	 * @param ft
	 *            The function trace
	 * @return The string containing the JS line
	 */
	private String getLine(FunctionTrace ft) {
		String current_function = getFunctionName(ft);
		int lineno = getFunctionLineno(ft);
		String strLine = null;
		try {
			System.out.println("Current function name:" + current_function);
			FileInputStream fstream = new FileInputStream(JS_FILES_FOLDER + "/"
					+ current_function + ".js");
			DataInputStream din = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(din));

			String functionDeclaration = null;
			List<String> functionDeclarationTokens = null;
			int linesToReadAhead = lineno - 1; // subtract 1 because this next
												// check counts as one read
			if ((strLine = br.readLine()) != null) {
				List<String> token_stream = getTokenStream(strLine);
				Iterator token_stream_it = token_stream.iterator();
				if (token_stream_it.hasNext()) {
					String firstToken = (String) token_stream_it.next();
					if (firstToken.equals("FUNCTION")) {
						// read next line - considered line no. 1
						// if ((strLine = br.readLine()) == null) {
						// System.out.println("Error: Empty function");
						// System.exit(-1);
						// }
						linesToReadAhead++;
						functionDeclaration = strLine;
						functionDeclarationTokens = token_stream;
					}
				}
			} else {
				System.out.println("Error: Empty function");
				System.exit(-1);
			}

			for (int i = 0; i < linesToReadAhead; i++) {
				if ((strLine = br.readLine()) == null) {
					System.out
							.println("Error: Function does not match with FunctionTrace line number");
					System.exit(-1);
				}
			}
		} catch (Exception e) {
			System.out.println("!Error reading function file" + e.toString());
			System.exit(-1);
		}

		return strLine;
	}

	/**
	 * Converts a string of JS code into tokens
	 * 
	 * @param str_to_parse
	 *            The JS code to convert into tokens
	 * @return A list of tokens
	 */
	private List<String> getTokenStream(String str_to_parse) {
		Parser ps = new Parser(new CompilerEnvirons());
		TokenStream ts = ps.initForUnitTest(new StringReader(str_to_parse), "",
				1, false);
		List<String> tokens_list = new ArrayList<String>();
		try {
			String t_name = null;
			int token;

			token = ts.getToken();
			while (Token.typeToName(token) != "EOF") {
				t_name = Token.typeToName(token);
				tokens_list.add(t_name);
				if (t_name.equals("NAME") || t_name.equals("STRING")) {
					tokens_list.add(ts.getString());
				}
				token = ts.getToken();
			}
		} catch (IOException ie) {
			System.out.println("Error");
		}
		return tokens_list;
	}

	/**
	 * Fetches the function name corresponding to the function trace
	 * 
	 * @param ft
	 *            The function trace
	 * @return The function name corresponding to ft
	 */
	private String getFunctionName(FunctionTrace ft) {
		FunctionDecl fd = ft.f_decl;
		String name = fd.f_name.substring(0, fd.f_name.indexOf(":::"));
		return name;
	}

	/**
	 * Fetches the line number of the current function trace relative to the
	 * corresponding function
	 * 
	 * @param ft
	 *            The function trace
	 * @return The line number of the current function
	 */
	private int getFunctionLineno(FunctionTrace ft) {
		String name = ft.f_decl.f_name;
		String suffix = name.substring(name.indexOf(":::"));
		String lineno_str = "";
		if (suffix.startsWith(":::EXIT")) {
			lineno_str = suffix.substring(7);
		} else if (suffix.startsWith(":::INTERMEDIATE")) {
			lineno_str = suffix.substring(15);
		} else if (suffix.startsWith(":::ENTER")) {
			lineno_str = "1";
		} else if (suffix.startsWith(":::ASYNC")
				|| suffix.startsWith(":::ASYNC_CALL")) {
			lineno_str = "0"; // we won't need the line numbers for these anyway
		} else {
			System.out.println("Error: FunctionTrace suffix undefined");
			System.exit(-1);
		}
		int lineno = Integer.parseInt(lineno_str);
		return lineno;
	}

	/**
	 * Tests if the function stack is empty
	 * 
	 * @return true if the function stack is empty; false otherwise
	 */
	public boolean funcStackIsEmpty() {
		return funcStack.empty();
	}

	/**
	 * Parses a line of JavaScript code based on its tokens and determines if
	 * this line contains a function call to the specified function
	 * 
	 * @param tokenList
	 *            The tokens to parse
	 * @param funcName
	 *            The name of the function expected to be called in the JS line
	 *            tested
	 * @return true if the JavaScript line contains a function call to funcName;
	 *         false otherwise
	 */
	private boolean isJSFunctionCall(List<String> tokenList, String funcName) {
		// Set up states
		final int St_START = 1, St_VAR_DECL = 2, St_LP_START = 3, St_FALSE = 4, St_NAME_FOUND = 5, St_ERROR = 6, St_FUNC_NAME_FOUND = 7, St_NAME_STR = 8, St_TRUE = 9, St_ASSN = 10, St_NAME_FOUND_AFTER_ASSN = 11, St_NAME_STR_AFTER_ASSN = 12, St_CONT_LOOKING = 13;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("VAR")) {
					currentState = St_VAR_DECL;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_VAR_DECL:
				if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else if (nextToken.equals("VAR")) {
					currentState = St_VAR_DECL;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				if (nextToken.equals(funcName)) {
					currentState = St_FUNC_NAME_FOUND;
				} else {
					currentState = St_NAME_STR;
				}
				break;
			case St_FUNC_NAME_FOUND:
				if (nextToken.equals("LP")) {
					return true;
				} else {
					currentState = St_CONT_LOOKING;
				}
				break;
			case St_NAME_STR:
				if (nextToken.equals("ASSIGN")) {
					currentState = St_ASSN;
				} else {
					currentState = St_CONT_LOOKING;
				}
				break;
			case St_ASSN:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND_AFTER_ASSN;
				} else {
					currentState = St_CONT_LOOKING;
				}
				break;
			case St_NAME_FOUND_AFTER_ASSN:
				if (nextToken.equals(funcName)) {
					currentState = St_FUNC_NAME_FOUND;
				} else {
					currentState = St_NAME_STR_AFTER_ASSN;
				}
				break;
			case St_NAME_STR_AFTER_ASSN:
				currentState = St_CONT_LOOKING;
				break;
			case St_CONT_LOOKING:
				if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND_AFTER_ASSN;
				} else {
					// state stays the same
				}
				break;
			default:
				System.out
						.println("Error: Incorrect state in isJSFunctionCall");
				System.exit(-1);
				break;
			}
		}

		return false;
	}

	/**
	 * Pushes a JavaScript function name onto the stack
	 * 
	 * @param func
	 *            The JavaScript function name
	 */
	private void pushFunc(String func) {
		funcStack.push(func);
	}

	/**
	 * Pops a function name from the top of the function stack If the function
	 * name popped is the only one remaining in the stack, set last_func to this
	 * function name.
	 */
	private void popFunc() {
		String funcToPop = funcStack.pop();
		if (this.funcStackIsEmpty()) {
			last_func = funcToPop;
		}
	}

	/**
	 * Tests if a variable is local in the function containing the specified
	 * function trace
	 * 
	 * @param ft
	 *            The function trace
	 * @param varName
	 *            The variable to test
	 * @return true if varName is local in the function corresponding to ft;
	 *         false otherwise
	 */
	private boolean isVarLocal(FunctionTrace ft, String varName) {
		if (ft == null || varName == null || varName == "") {
			System.out
					.println("Error: Empty variable or trace passed to isVarLocal");
			System.exit(-1);
		}

		// Find varName in ft's variable list
		List<VariableDesc> vDesc_list = ft.f_decl.var_descs;
		Iterator<VariableDesc> vDesc_it = vDesc_list.iterator();
		VariableDesc vDesc = null;
		boolean varFound = false;
		while (vDesc_it.hasNext()) {
			vDesc = vDesc_it.next();
			// System.err.println(vDesc.getVarName());
			if (vDesc.getVarName().equals(varName)) {
				varFound = true;
				break;
			} else {
				// System.out.println(varName);
			}
		}

		if (!varFound) {
			System.out
					.println("Error: variable name not found in function declaration => varName:"
							+ varName);
			System.exit(-1);
		}

		// If this point is reached, the variable must have been found
		// and vDesc should contain the correct VariableDesc
		boolean isLocal = false;
		if (!vDesc.isGlobal()) {
			isLocal = true;
		}

		return isLocal;
	}

	/**
	 * Determines the parameter name at a certain position in the function call
	 * <p>
	 * The name of the function must match the name in the function call tokens
	 * 
	 * @param tokenList
	 *            The tokens for the function call (may contain extra tokens at
	 *            the end, which could be neglected)
	 * @param paramNum
	 *            The index of the argument to fetch (starting from 1)
	 * @param funcName
	 *            The name of the function called
	 * @return A string representing the parameter name
	 */
	private String getArgument(List<String> tokenList, int paramNum,
			String funcName) {
		// Set up states
		final int St_START = 1, St_VAR_DECL = 2, St_LP_START = 3, St_FALSE = 4, St_NAME_FOUND = 5, St_ERROR = 6, St_FUNC_NAME_FOUND = 7, St_NAME_STR = 8, St_TRUE = 9, St_ASSN = 10, St_NAME_FOUND_AFTER_ASSN = 11, St_NAME_STR_AFTER_ASSN = 12, St_CONT_LOOKING = 13, St_FUNC_FOUND = 14, St_FIND_ARG = 15, St_FOUND_ARG_NAME_TOKEN = 16;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		int paramCounter = 1;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("VAR")) {
					currentState = St_VAR_DECL;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					System.out.println("Error: Invalid function call tokens");
					System.exit(-1);
				}
				break;
			case St_VAR_DECL:
				if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					System.out.println("Error: Invalid function call tokens");
					System.exit(-1);
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else if (nextToken.equals("VAR")) {
					currentState = St_VAR_DECL;
				} else {
					System.out.println("Error: Invalid function call tokens");
					System.exit(-1);
				}
				break;
			case St_NAME_FOUND:
				if (nextToken.equals(funcName)) {
					currentState = St_FUNC_NAME_FOUND;
				} else {
					currentState = St_NAME_STR;
				}
				break;
			case St_FUNC_NAME_FOUND:
				if (nextToken.equals("LP")) {
					if (paramCounter == paramNum) {
						currentState = St_FIND_ARG;
					} else {
						currentState = St_FUNC_FOUND;
					}
				} else {
					currentState = St_CONT_LOOKING;
				}
				break;
			case St_NAME_STR:
				if (nextToken.equals("ASSIGN")) {
					currentState = St_ASSN;
				} else {
					currentState = St_CONT_LOOKING;
				}
				break;
			case St_ASSN:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND_AFTER_ASSN;
				} else {
					currentState = St_CONT_LOOKING;
				}
				break;
			case St_NAME_FOUND_AFTER_ASSN:
				if (nextToken.equals(funcName)) {
					currentState = St_FUNC_NAME_FOUND;
				} else {
					currentState = St_NAME_STR_AFTER_ASSN;
				}
				break;
			case St_NAME_STR_AFTER_ASSN:
				currentState = St_CONT_LOOKING;
				break;
			case St_CONT_LOOKING:
				if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND_AFTER_ASSN;
				} else {
					// state stays the same
				}
				break;
			case St_FUNC_FOUND:
				if (nextToken.equals("COMMA")) {
					paramCounter++;
					if (paramCounter == paramNum) {
						currentState = St_FIND_ARG;
					}
				} else {
					// state stays the same
				}
				break;
			case St_FIND_ARG:
				if (nextToken.equals("NAME")) {
					currentState = St_FOUND_ARG_NAME_TOKEN;
				} else {
					System.out.println("Error: Invalid function call tokens");
					System.exit(-1);
				}
				break;
			case St_FOUND_ARG_NAME_TOKEN:
				return nextToken;
			default:
				System.out.println("Error: Incorrect state in getArgument");
				System.exit(-1);
				break;
			}
		}

		// If this point is reached, we must've run out of tokens
		System.out.println("Error: Invalid function call tokens");
		System.exit(-1);

		return ""; // This won't be reached
	}

	/**
	 * Set null_var to a new value
	 * 
	 * @param newNullVar
	 *            The value the new null_var will be set to
	 * @param newNullVarFt
	 *            The FunctionTrace corresponding to the new null_var
	 */
	private void setNullVar(FunctionTrace newNullVarFt, String newNullVar) {
		this.null_var_ft = newNullVarFt;
		this.null_var = newNullVar;
	}

	/**
	 * Gets the string representation of the function trace's line type
	 * 
	 * @param ft
	 *            The function trace
	 * @return The line type (either ENTER, EXIT, INTERMEDIATE, ASYNC_CALL, or
	 *         ASYNC)
	 */
	private String getLineType(FunctionTrace ft) {
		String name = ft.f_decl.f_name;
		String suffix = name.substring(name.indexOf(":::"));
		String linetype = "";
		if (suffix.startsWith(":::EXIT")) {
			linetype = "EXIT";
		} else if (suffix.startsWith(":::INTERMEDIATE")) {
			linetype = "INTERMEDIATE";
		} else if (suffix.startsWith(":::ENTER")) {
			linetype = "ENTER";
		} else if (suffix.startsWith(":::ASYNC_CALL")) { // must be checked
															// before :::ASYNC
															// !!
			linetype = "ASYNC_CALL";
		} else if (suffix.startsWith(":::ASYNC")) {
			linetype = "ASYNC";
		} else {
			System.out.println("Error: Incorrect Function Line Type");
			System.exit(-1);
		}

		return linetype;
	}

	/**
	 * Determines the position of the null_var in the function declaration of
	 * the current function
	 * 
	 * @param tokenList
	 *            The tokens representing the function declaration
	 * @param funcName
	 *            The name of the current function
	 * @return The index (starting from 1) representing the position of the
	 *         current null_var in the function declaration
	 */
	private int getParamNumber(List<String> tokenList, String funcName) {
		final int St_START = 1, St_FUNC_PREFIX = 2, St_FUNC_NAME_TOKEN = 3, St_FUNC_NAME = 4, St_IN_PARAMS = 5, St_CHECK_PARAM = 6, St_FOUND_COMMA = 7;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		int paramCount = 0;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("FUNCTION")) {
					currentState = St_FUNC_PREFIX;
				} else {
					System.out
							.println("Error: Invalid function declaration tokens");
					System.exit(-1);
				}
				break;
			case St_FUNC_PREFIX:
				if (nextToken.equals("NAME")) {
					currentState = St_FUNC_NAME_TOKEN;
				} else {
					System.out
							.println("Error: Invalid function declaration tokens");
					System.exit(-1);
				}
				break;
			case St_FUNC_NAME_TOKEN:
				if (nextToken.equals(funcName)) {
					currentState = St_FUNC_NAME;
				} else {
					System.out
							.println("Error: Invalid function declaration tokens");
					System.exit(-1);
				}
				break;
			case St_FUNC_NAME:
				if (nextToken.equals("LP")) {
					currentState = St_IN_PARAMS;
					paramCount++;
				} else {
					System.out
							.println("Error: Invalid function declaration tokens");
					System.exit(-1);
				}
				break;
			case St_IN_PARAMS:
				if (nextToken.equals("NAME")) {
					currentState = St_CHECK_PARAM;
				} else if (nextToken.equals("COMMA")) {
					currentState = St_FOUND_COMMA;
				} else {
					// state stays the same
				}
				break;
			case St_CHECK_PARAM:
				if (nextToken.equals(null_var)) {
					return paramCount;
				} else {
					currentState = St_IN_PARAMS;
				}
				break;
			case St_FOUND_COMMA:
				paramCount++;
				if (nextToken.equals("NAME")) {
					currentState = St_CHECK_PARAM;
				} else {
					currentState = St_IN_PARAMS;
				}
				break;
			default:
				System.out.println("Error: Incorrect state in getParamNumber");
				System.exit(-1);
				break;
			}
		}

		System.out.println("Error: Invalid function declaration tokens");
		System.exit(-1);

		return -1; // this won't be reached
	}

	/**
	 * Gets the function declaration string from the corresponding JS function
	 * 
	 * @param ft
	 *            The function trace
	 * @return A string representing the declaration of the function
	 *         corresponding to ft
	 */
	private String getJSFuncDecl(FunctionTrace ft) {
		String current_function = getFunctionName(ft);
		int lineno = 1;
		String strLine = null;
		try {
			FileInputStream fstream = new FileInputStream(JS_FILES_FOLDER + "/"
					+ current_function + ".js");
			DataInputStream din = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(din));

			String functionDeclaration = null;
			List<String> functionDeclarationTokens = null;
			if ((strLine = br.readLine()) != null) {
				List<String> token_stream = getTokenStream(strLine);
				Iterator token_stream_it = token_stream.iterator();
				if (token_stream_it.hasNext()) {
					String firstToken = (String) token_stream_it.next();
					if (!firstToken.equals("FUNCTION")) {
						System.out
								.println("Error: First line is not a function declaration");
						System.exit(-1);
					}
				} else {
					System.out
							.println("Error: No tokens in function declaration");
					System.exit(-1);
				}
			} else {
				System.out.println("Error: Empty function");
				System.exit(-1);
			}
		} catch (Exception e) {
			System.out.println("Error reading function file");
			System.exit(-1);
		}

		return strLine;
	}

	/**
	 * Starting from a specified index, trace back through the trace sequence
	 * until the corresponding ENTER of the current function is found. If found,
	 * go back another trace and check if it corresponds to the calling
	 * function. If not, throw an error
	 * 
	 * @param trace_list
	 *            The list of JS traces
	 * @param curr_index
	 *            The index in the trace list currently being analyzed
	 * @param currFunction
	 *            The function to which the current trace belongs
	 * @param callingFunc
	 *            The function that called the line in the current execution
	 *            trace
	 * @return true if the call to the current function is of the form
	 *         "<null var> = <function call>"; false otherwise
	 */
	private boolean isFuncCallAfterPeek(List<FunctionTrace> trace_list,
			int curr_index, String currFunction, String callingFunc) {
		ListIterator<FunctionTrace> l_itr = trace_list.listIterator(curr_index);
		boolean enterFound = false;
		int newIndex = curr_index;
		FunctionTrace theTrace;
		String theLine;
		while (l_itr.hasPrevious()) {
			FunctionTrace ft = l_itr.previous();
			newIndex--;
			String functionName = getFunctionName(ft);
			String traceType = getLineType(ft);

			if (functionName.equals(currFunction) && traceType.equals("ENTER")) {
				enterFound = true;
				theTrace = ft;
				indexToChange = newIndex; // index just "after" the function
											// call ("before" in our order of
											// analysis)
				this.indexToChangeModified = true;
				break;
			}
		}

		if (!enterFound) {
			System.out.println("Error: No corresponding ENTER");
			System.exit(-1);
		} else {
			if (!l_itr.hasPrevious()) {
				System.out.println("Error: No corresponding function call");
				System.exit(-1);
			} else {
				FunctionTrace ft = l_itr.previous();
				theLine = getLine(ft);
				List<String> lineTokens = getTokenStream(theLine);
				return isNullVarEqualsFuncCallAssn(lineTokens, currFunction);
			}
		}
		return false;
	}

	/**
	 * Checks if the current line is of the form <null var> = <function call>
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @param funcName
	 *            The name of the function that is being called
	 * @return true if the corresponding list of tokens corresponds to a
	 *         function call to funcName; false otherwise
	 */
	private boolean isNullVarEqualsFuncCallAssn(List<String> tokenList,
			String funcName) {
		final int St_START = 1, St_VAR_DECL = 2, St_LP_START = 3, St_NAME_FOUND = 4, St_NULL_VAR_FOUND = 5, St_ASSN = 6, St_NAME_FOUND_AFTER_ASSN = 7, St_FUNC_NAME_FOUND = 8;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("VAR")) {
					currentState = St_VAR_DECL;
				} else if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_VAR_DECL:
				if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				if (nextToken.equals(null_var)) {
					currentState = St_NULL_VAR_FOUND;
				} else {
					return false;
				}
				break;
			case St_NULL_VAR_FOUND:
				if (nextToken.equals("ASSIGN")) {
					currentState = St_ASSN;
				} else {
					return false;
				}
				break;
			case St_ASSN:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND_AFTER_ASSN;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND_AFTER_ASSN:
				if (nextToken.equals(funcName)) {
					currentState = St_FUNC_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_FUNC_NAME_FOUND:
				if (nextToken.equals("LP")) {
					return true;
				} else {
					return false;
				}
			default:
				System.out
						.println("Error: Incorrect state in isNullVarEqualsFuncCallAssn");
				System.exit(-1);
				break;
			}
		}

		return false;
	}

	/**
	 * Checks if the expression represented by the tokens is a function call
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if the expression is a function call; false otherwise
	 */
	private boolean isFuncCall(List<String> tokenList) {
		final int St_START = 1, St_LP_START = 2, St_NAME_FOUND = 3, St_NAME_STR = 4;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				currentState = St_NAME_STR;
				break;
			case St_NAME_STR:
				if (nextToken.equals("LP")) {
					return true;
				} else {
					return false;
				}
			default:
				System.out.println("Error: Incorrect state in isFuncCall");
				System.exit(-1);
				break;
			}
		}

		return false;
	}

	/**
	 * Checks if the current line is of the form <null var> = expression
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if the corresponding list of tokens corresponds to a
	 *         function call; false otherwise
	 */
	private boolean isNullVarAssn(List<String> tokenList) {
		final int St_START = 1, St_VAR_DECL = 2, St_LP_START = 3, St_NAME_FOUND = 4, St_NULL_VAR_FOUND = 5;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("VAR")) {
					currentState = St_VAR_DECL;
				} else if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_VAR_DECL:
				if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				if (nextToken.equals(null_var)) {
					currentState = St_NULL_VAR_FOUND;
				} else {
					return false;
				}
				break;
			case St_NULL_VAR_FOUND:
				if (nextToken.equals("ASSIGN")) {
					return true;
				} else {
					return false;
				}
			default:
				System.out.println("Error: Incorrect state in isNullVarAssn");
				System.exit(-1);
				break;
			}
		}

		return false;
	}

	/**
	 * Determines if the current line is of the form <null var> = expression. If
	 * so, it returns the tokens representing the right hand side of this
	 * assignment. If not, returns an empty list
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return A list of tokens representing the expression on the right hand
	 *         side if the provided list of tokens is of the form <null var> =
	 *         expression; otherwise, returns an empty list of strings
	 */
	private List<String> nullVarAssn(List<String> tokenList) {
		final int St_START = 1, St_VAR_DECL = 2, St_LP_START = 3, St_NAME_FOUND = 4, St_NULL_VAR_FOUND = 5;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("VAR")) {
					currentState = St_VAR_DECL;
				} else if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return new ArrayList<String>();
				}
				break;
			case St_VAR_DECL:
				if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return new ArrayList<String>();
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return new ArrayList<String>();
				}
				break;
			case St_NAME_FOUND:
				if (nextToken.equals(null_var)) {
					currentState = St_NULL_VAR_FOUND;
				} else {
					return new ArrayList<String>();
				}
				break;
			case St_NULL_VAR_FOUND:
				if (nextToken.equals("ASSIGN")) {
					List<String> exprList = new ArrayList<String>();
					while (tokenIt.hasNext()) {
						exprList.add((String) tokenIt.next());
					}
					return exprList;
				} else {
					return new ArrayList<String>();
				}
			default:
				System.out.println("Error: Incorrect state in nullVarAssn");
				System.exit(-1);
				break;
			}
		}

		return new ArrayList<String>();
	}

	/**
	 * Checks if the list of tokens represents a return expression
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if the line represented by the passed tokens is a return
	 *         statement; false otherwise
	 */
	private boolean isReturnExpr(List<String> tokenList) {
		final int St_START = 1, St_LP_START = 2;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("RETURN")) {
					return true;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("RETURN")) {
					return true;
				}
				break;
			default:
				System.out.println("Error: Incorrect state in isReturnExpr");
				System.exit(-1);
				break;
			}
		}

		return false;
	}

	/**
	 * Determines if the current line represents a return expression. If so,
	 * returns a list of tokens of the returned expression. Otherwise, returns
	 * an empty list of tokens.
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return A list of tokens of the returned expression if the current line
	 *         is of the form "return expression"; otherwise, returns an empty
	 *         list of strings.
	 */
	private List<String> returnExpr(List<String> tokenList) {
		final int St_START = 1, St_LP_START = 2;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("RETURN")) {
					List<String> exprList = new ArrayList<String>();
					while (tokenIt.hasNext()) {
						exprList.add((String) tokenIt.next());
					}
					return exprList;
				} else {
					return new ArrayList<String>();
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("RETURN")) {
					List<String> exprList = new ArrayList<String>();
					while (tokenIt.hasNext()) {
						exprList.add((String) tokenIt.next());
					}
					return exprList;
				}
				break;
			default:
				System.out.println("Error: Incorrect state in isReturnExpr");
				System.exit(-1);
				break;
			}
		}

		return new ArrayList<String>();
	}

	/**
	 * Checks if the list of strings represents a variable
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if the list of tokens represents a variable; false otherwise
	 */
	private boolean isVariable(List<String> tokenList) {
		final int St_START = 1, St_LP_START = 2, St_NAME_FOUND = 3, St_CHECK_IF_VAR = 4;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				currentState = St_CHECK_IF_VAR;
				break;
			case St_CHECK_IF_VAR:
				if (nextToken.equals("DOT")) {
					return false;
				} else if (nextToken.equals("LP")) {
					return false;
				} else {
					return true;
				}
			default:
				System.out.println("Error: Incorrect state in isVariable");
				System.exit(-1);
				break;
			}
		}

		if (currentState == St_CHECK_IF_VAR) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Fetches the variable name assuming that tokenList represents a variable
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return The name of the JavaScript variable represented by the tokens
	 */
	private String getVariableName(List<String> tokenList) {
		if (!isVariable(tokenList)) {
			System.out
					.println("Error: Attempting to extract variable name from tokens not representing a variable");
			System.exit(-1);
		}

		Iterator<String> token_it = tokenList.iterator();

		String token = token_it.next();
		while (token.equals("LP")) {
			token = token_it.next();
		}

		// At this point, the last fetched token must have been NAME
		// Fetch the next token to get the variable name
		String varName = token_it.next();

		return varName;
	}

	/**
	 * Checks if the list of tokens represents a call to
	 * document.getElementById() or $()
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if tokenList represents a call to document.getElementById()
	 *         or $(); false otherwise
	 */
	private boolean isDocumentGEBID(List<String> tokenList) {
		final int St_START = 1, St_LP_START = 2, St_NAME_FOUND = 3, St_DOCUMENT_FOUND = 4, St_DOLLAR_FOUND = 5, St_DOT_FOUND = 6, St_GEBID_NAME_FOUND = 7, St_GEBID_FOUND = 8;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				if (nextToken.equals("document")) {
					currentState = St_DOCUMENT_FOUND;
				} else if (nextToken.equals("$")) {
					currentState = St_DOLLAR_FOUND;
				} else {
					return false;
				}
				break;
			case St_DOCUMENT_FOUND:
				if (nextToken.equals("DOT")) {
					currentState = St_DOT_FOUND;
				} else {
					return false;
				}
				break;
			case St_DOLLAR_FOUND:
				if (nextToken.equals("LP")) {
					return true;
				} else {
					return false;
				}
			case St_DOT_FOUND:
				if (nextToken.equals("NAME")) {
					currentState = St_GEBID_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_GEBID_NAME_FOUND:
				if (nextToken.equals("getElementById")) {
					currentState = St_GEBID_FOUND;
				} else {
					return false;
				}
				break;
			case St_GEBID_FOUND:
				if (nextToken.equals("LP")) {
					return true;
				} else {
					return false;
				}
			default:
				System.out.println("Error: Incorrect state in isDocumentGEBID");
				System.exit(-1);
				break;
			}
		}

		return false;
	}

	/**
	 * Checks if the list of tokens represents a call to getAttribute()
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if tokenList represents a call to getAttribute; false
	 *         otherwise
	 */
	private boolean isGetAttr(List<String> tokenList) {
		final int St_START = 1, St_LP_START = 2, St_NAME_FOUND = 3, St_ELEM_FOUND = 4, St_DOT_FOUND = 5, St_GETATTR_NAME_FOUND = 6, St_GETATTR_FOUND = 7;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				currentState = St_ELEM_FOUND;
				break;
			case St_ELEM_FOUND:
				if (nextToken.equals("DOT")) {
					currentState = St_DOT_FOUND;
				} else {
					// state stays the same
				}
				break;
			case St_DOT_FOUND:
				if (nextToken.equals("NAME")) {
					currentState = St_GETATTR_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_GETATTR_NAME_FOUND:
				if (nextToken.equals("getAttribute")) {
					currentState = St_GETATTR_FOUND;
				} else {
					currentState = St_ELEM_FOUND;
				}
				break;
			case St_GETATTR_FOUND:
				if (nextToken.equals("LP")) {
					return true;
				} else {
					currentState = St_ELEM_FOUND;
				}
				break;
			default:
				System.out.println("Error: Incorrect state in isGetAttr");
				System.exit(-1);
				break;
			}
		}

		return false;
	}

	/**
	 * Checks if the list of tokens represents a call to getComputedStyle()
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if tokenList represents a call to getComputedStyle; false
	 *         otherwise
	 */
	private boolean isGCS(List<String> tokenList) {
		final int St_START = 1, St_LP_START = 2, St_NAME_FOUND = 3, St_WINDOW_FOUND = 4, St_DOT_FOUND = 5, St_GCS_NAME_FOUND = 6, St_GCS_FOUND = 7, St_DOCUMENT_FOUND = 8, St_FIRST_DOT_FOUND = 9, St_DEF_VIEW_NAME_FOUND = 10, St_DEF_VIEW_FOUND = 11;

		Iterator tokenIt = tokenList.iterator();
		int currentState = St_START;

		while (tokenIt.hasNext()) {
			String nextToken = (String) tokenIt.next();
			switch (currentState) {
			case St_START:
				if (nextToken.equals("LP")) {
					currentState = St_LP_START;
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_LP_START:
				if (nextToken.equals("LP")) {
					// state stays the same
				} else if (nextToken.equals("NAME")) {
					currentState = St_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_NAME_FOUND:
				if (nextToken.equals("window")) {
					currentState = St_WINDOW_FOUND;
				} else if (nextToken.equals("document")) {
					currentState = St_DOCUMENT_FOUND;
				} else {
					return false;
				}
				break;
			case St_WINDOW_FOUND:
				if (nextToken.equals("DOT")) {
					currentState = St_DOT_FOUND;
				} else {
					return false;
				}
				break;
			case St_DOT_FOUND:
				if (nextToken.equals("NAME")) {
					currentState = St_GCS_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_GCS_NAME_FOUND:
				if (nextToken.equals("getComputedStyle")) {
					currentState = St_GCS_FOUND;
				} else {
					return false;
				}
				break;
			case St_GCS_FOUND:
				if (nextToken.equals("LP")) {
					return true;
				} else {
					return false;
				}
			case St_DOCUMENT_FOUND:
				if (nextToken.equals("DOT")) {
					currentState = St_FIRST_DOT_FOUND;
				} else {
					return false;
				}
				break;
			case St_FIRST_DOT_FOUND:
				if (nextToken.equals("NAME")) {
					currentState = St_DEF_VIEW_NAME_FOUND;
				} else {
					return false;
				}
				break;
			case St_DEF_VIEW_NAME_FOUND:
				if (nextToken.equals("defaultView")) {
					currentState = St_DEF_VIEW_FOUND;
				} else {
					return false;
				}
				break;
			case St_DEF_VIEW_FOUND:
				if (nextToken.equals("DOT")) {
					currentState = St_DOT_FOUND;
				} else {
					return false;
				}
				break;
			default:
				System.out.println("Error: Incorrect state in isGCS");
				System.exit(-1);
				break;
			}
		}
		return false;
	}

	/**
	 * Checks if the list of tokens represents an expression that could have
	 * started the propagation of null
	 * 
	 * @param tokenList
	 *            The list of tokens
	 * @return true if the tokenList expression could have started null
	 *         propagation; false otherwise
	 */
	private boolean isNullSource(List<String> tokenList) {
		boolean retVal = false;

		// Add whatever checks need to be done here
		retVal = retVal || isDocumentGEBID(tokenList);
		retVal = retVal || isGetAttr(tokenList);
		retVal = retVal || isGCS(tokenList);

		return retVal;
	}

	String getCallbackFunctionCall(FunctionTrace ft) {
		// Ensure ft is of type ASYNC_CALL
		String type = getLineType(ft);
		if (!type.equals("ASYNC_CALL")) {
			System.out
					.println("Error: Cannot get callback function call from non-ASYNC_CALL trace");
			System.exit(-1);
		}

		// Find which index contains the variable FuncCall
		List<VariableDesc> varDescList = ft.f_decl.var_descs;
		Iterator varDescIt = varDescList.iterator();
		int indexOfFuncCall = -1;
		boolean foundIndex = false;
		int counter = 0;
		while (varDescIt.hasNext() && !foundIndex) {
			VariableDesc varDesc = (VariableDesc) varDescIt.next();
			if (varDesc.getVarName().equals("FuncCall")) {
				indexOfFuncCall = counter;
				foundIndex = true;
			}
			counter++;
		}

		if (!foundIndex) {
			System.out
					.println("Error: Callback function call not stored in ASYNC_CALL");
			System.exit(-1);
		}

		String theCall = ft.var_values.get(indexOfFuncCall);

		// Strip the leading and trailing quotation marks (note that the dtrace
		// always uses double quotes)
		if (theCall.startsWith("\"") && theCall.endsWith("\"")) {
			theCall = theCall.substring(1, theCall.length() - 1);
		}

		return theCall;
	}

	private void testGEBIDLineFinderMethods() {
		int token;

		Parser ps = new Parser(new CompilerEnvirons());
		TokenStream ts = ps.initForUnitTest(new StringReader(
				"(x = hello(why,hello));"), "", 1, false);
		List<String> tokenList = new ArrayList<String>();

		try {
			token = ts.getToken();
			while (!Token.typeToName(token).equals("EOF")) {
				String t_name = Token.typeToName(token);
				tokenList.add(t_name);

				if (t_name.equals("NAME") || t_name.equals("STRING")) {
					tokenList.add(ts.getString());
				}

				token = ts.getToken();
			}
		} catch (IOException ioe) {
		}

		System.out.println(isJSFunctionCall(tokenList, "hello"));
	}
}