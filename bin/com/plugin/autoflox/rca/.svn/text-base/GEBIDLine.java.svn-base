package com.plugin.autoflox.rca;

import java.io.*;
import java.util.*;
import org.mozilla.javascript.*;

public class GEBIDLine {
	public List<String> GEBID_Id; //list of tokens
	public String GEBID_function;
	public int GEBID_line;
	public FunctionTrace GEBID_trace;
	
	String JS_FILES_FOLDER = "js_files";
	
	//Each function is stored in the file <name>.js
	//where <name> is the name of the function
	//Values of variables are stored in <name>_values.js
	//TODO: Handle case where same function name assigned
	//to different functions
	public GEBIDLine(List<FunctionTrace> ft, String err_function_name, String null_var) {
		ListIterator ft_litr = ft.listIterator(ft.size()); //start at end of the list
		getGEBIDLine(ft, err_function_name, null_var, false);
	}
	
	void getGEBIDLine(List<FunctionTrace> ft, String function_name, String null_variable, boolean startAtEnd) {
		String null_var = null_variable;
		ListIterator ft_litr = ft.listIterator(ft.size()); //start at end of the list
		GEBID_Id = new ArrayList<String>();
		try {
			String current_function = function_name;
			//String next_function = function_name;
			//FileInputStream fstream = new FileInputStream(JS_FILES_FOLDER + "/" + current_function + ".js");
			//DataInputStream din = new DataInputStream(fstream);
			//BufferedReader br = new BufferedReader(new InputStreamReader(din));
			boolean wasReturned = false;
			String previous_function = null;
			int previous_lineno = -1;
			String previous_linetype = "";
			
			boolean inCalledFunction = false;
			String current_function_in_stack = function_name;
			boolean change_stack_function = false;
			int corresponding_param = 0;
			String calledFunctionName = "";
			//boolean confirmIfCalled = false;
			
			Stack returningFunctions = new Stack();
			returningFunctions.push(function_name);
			boolean inReturningFunction = false;
			
			//Functions for async handling
			boolean foundAsync = false;
			boolean foundAsyncCall = false;
			String callbackFunctionCall = "";
			
			while (ft_litr.hasPrevious()) {
				FunctionTrace trace = (FunctionTrace)ft_litr.previous();
				
				int lineno = getFunctionLineno(trace);
				String line_type = getLineType(trace);
				
				if (foundAsync) {
					//Ensure current trace has lineType ASYNC_CALL
					if (!line_type.equals("ASYNC_CALL")) {
						System.out.println("ERROR: ASYNC not preceded by ASYNC_CALL");
						System.exit(-1);
					}
					else {
						foundAsync = false;
						foundAsyncCall = true;
						
						//Determine the callback function call - this is always the second variable trace (i.e., index 1) in an ASYNC_CALL, with variable name FuncCall
						callbackFunctionCall = getCallbackFunctionCall(trace);
						
						continue;
					}
				}
				if (line_type.equals("ASYNC")) {
					foundAsync = true;
					continue;
				}
				
				current_function = getFunctionName(trace);
				
				FileInputStream fstream = new FileInputStream(JS_FILES_FOLDER + "/" + current_function + ".js");
				DataInputStream din = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(din));
				
				if (change_stack_function) {
					//TODO: First, check if there is indeed a call to the previous function in the stack
					//If not, then this function might correspond to a foo() + bar() type of call
					current_function_in_stack = current_function;
					
					//Change bottom of returningFunctions stack as well
					assert(returningFunctions.size() == 1);
					returningFunctions.pop();
					returningFunctions.push(current_function);
					
					//change_stack_function = false; //should delay this until we already have the corresponding null_var
				}
				
				if (wasReturned && !(current_function.equals(returningFunctions.peek()))) {
					returningFunctions.push(current_function);
					inReturningFunction = true;
				}
				
				//If we've reached a called function, just keep on skipping those lines until we get back to the "primary" trace
				if (inCalledFunction && line_type.equals("ENTER") && current_function.equals(calledFunctionName)) {
					inCalledFunction = false;
					previous_function = current_function;
					previous_linetype = line_type;
					previous_lineno = lineno;
					continue;
				}
				if (inCalledFunction) {
					previous_function = current_function;
					previous_linetype = line_type;
					previous_lineno = lineno;
					continue;
				}
				if ((!(current_function.equals(previous_function)) && !wasReturned && !(current_function.equals(current_function_in_stack)) && !inReturningFunction) || (inReturningFunction && !(returningFunctions.peek().equals(current_function)))) {
					inCalledFunction = true;
					calledFunctionName = current_function;
					//confirmIfCalled = true;
					
					//Peek ahead until we get back to the calling function to see if the calling function retrieves a value from this function
					try {
						int thisIndex = ft_litr.previousIndex() + 1;
						String prev_line = null;
						FunctionTrace prev_ft = null;
						while (ft_litr.hasPrevious()) {
							prev_ft = (FunctionTrace)ft_litr.previous();
							String prev_ft_type = getLineType(prev_ft);
							String prev_function_name = getFunctionName(prev_ft);
							if (prev_ft_type.equals("ENTER") && prev_function_name.equals(calledFunctionName)) {
								break;
							}
						}
						prev_ft = (FunctionTrace)ft_litr.previous();
						
						prev_line = getLine(prev_ft);
						
						//At this point, we should be at the line in the calling function that does the actual call
						//If the line assigns the return value of the called function to null_var, then we shouldn't skip the lines in the called function
						//Otherwise, we'll skip the lines
						List<String> the_tokens = getTokenStream(prev_line);
						Iterator the_tokens_it = the_tokens.iterator();
						
						boolean inCalledFunctionDecided = false;
						
						if (the_tokens.size() < 5) {
							inCalledFunction = true;
							
							inCalledFunctionDecided = true;
						}
						
						if (!inCalledFunctionDecided) {
							String s1 = (String)the_tokens_it.next();
							String s2 = (String)the_tokens_it.next();
							String s3 = (String)the_tokens_it.next();
							String s4 = (String)the_tokens_it.next();
							String s5 = (String)the_tokens_it.next();
							if (s1.equals("NAME") && s2.equals(null_var) && s3.equals("ASSIGN") && s4.equals("NAME") && s5.equals(current_function)) {
								inCalledFunction = false;
								wasReturned = true;
								inReturningFunction = true;
								returningFunctions.push(current_function);
								
								inCalledFunctionDecided = true;
							}
						}
						
						if (!inCalledFunctionDecided) {
							if (the_tokens.size() < 6) {
								inCalledFunction = true;
								
								inCalledFunctionDecided = true;
							}
						}
						
						if (!inCalledFunctionDecided) {
							the_tokens_it = the_tokens.iterator();
							
							String s1 = (String)the_tokens_it.next();
							String s2 = (String)the_tokens_it.next();
							String s3 = (String)the_tokens_it.next();
							String s4 = (String)the_tokens_it.next();
							String s5 = (String)the_tokens_it.next();
							String s6 = (String)the_tokens_it.next();
							if (s1.equals("VAR") && s2.equals("NAME") && s3.equals(null_var) && s4.equals("ASSIGN") && s5.equals("NAME") && s6.equals(current_function)) {
								inCalledFunction = false;
								wasReturned = true;
								inReturningFunction = true;
								returningFunctions.push(current_function);
								
								inCalledFunctionDecided = true;
							}
						}
						
						//Reinstate old iterator
						ft_litr = ft.listIterator(thisIndex);
					}
					catch (NoSuchElementException nsee) {
						System.out.println("Error: No entry point for called function");
						System.exit(-1);
					}
					
					if (inCalledFunction) {
						previous_function = current_function;
						previous_linetype = line_type;
						previous_lineno = lineno;
						continue;
					}
				}
				
				//If previous lineno and function are the same as this one, and the previous one was an EXIT, it must be the same line (and same execution), so skip to next line
				//This check is necessary since not all EXIT's are duplicated (e.g., ReturnNodes?)
				if (previous_function != null && previous_lineno != -1 && !(previous_linetype.equals("")) && current_function.equals(previous_function) && lineno == previous_lineno && previous_linetype.equals("EXIT")) {
					previous_function = current_function;
					previous_linetype = line_type;
					previous_lineno = lineno;
					continue;
				}
				
				//Read first line and check if it's a function declaration
				String functionDeclaration = null;
				List<String> functionDeclarationTokens = null;
				String strLine = null;
				int linesToReadAhead = lineno-1; //subtract 1 because this next check counts as one read
				if ((strLine = br.readLine()) != null) {
					List<String> token_stream = getTokenStream(strLine);
					Iterator token_stream_it = token_stream.iterator();
					if (token_stream_it.hasNext()) {
						String firstToken = (String)token_stream_it.next();
						if (firstToken.equals("FUNCTION")) {
							//read next line - considered line no. 1
							//if ((strLine = br.readLine()) == null) {
							//	System.out.println("Error: Empty function");
							//	System.exit(-1);
							//}
							linesToReadAhead++;
							functionDeclaration = strLine;
							functionDeclarationTokens = token_stream;
						}
					}
				}
				else {
					System.out.println("Error: Empty function");
					System.exit(-1);
				}
				
				for (int i = 0; i < linesToReadAhead; i++) {
					if ((strLine = br.readLine()) == null) {
						System.out.println("Error: Function does not match with FunctionTrace line number");
						System.exit(-1);
					}
				}
				
				//If an ASYNC_CALL has just been encountered, we will consider the callback function call as the string line. This call is stored as a variable in the ASYNC_CALL trace.
				if (foundAsyncCall) {
					strLine = callbackFunctionCall;
					foundAsyncCall = false;
				}
				
				//By this point, strLine should contain the current line of code to parse
				List<String> tokens_to_analyze = getTokenStream(strLine);
				Iterator tokens_it = tokens_to_analyze.iterator();
				
				//Two ways for null_var to be assigned a value: assignment or function call (in which case the value comes from a ReturnNode)
				
				//Determine corresponding null_var if current_function_in_stack has just been changed
				if (change_stack_function) {
					Iterator new_it = tokens_to_analyze.iterator();
					try {
						String next_token = (String)new_it.next();
						while (!(next_token.equals(previous_function))) {
							next_token = (String)new_it.next();
						}
						
						//TODO: Here, we assume that the function parameters are simply variables, so each comma corresponds to a parameter. We can change this if/when we relax this assumption
						next_token = (String)new_it.next(); //LP
						int param_counter = 1;
						String the_var = null;
						while (new_it.hasNext()) {
							if (param_counter == corresponding_param) {
								next_token = (String)new_it.next(); //NAME
								null_var = (String)new_it.next();
								next_token = (String)new_it.next();
								if (!(next_token.equals("COMMA") || next_token.equals("RP"))) {
									System.out.println("Error: Invalid null variable");
									System.exit(-1);
								}
								break;
							}
							next_token = (String)new_it.next();
							if (next_token.equals("COMMA")) {
								param_counter++;
							}
						}
					}
					catch (NoSuchElementException nsee) {
						System.out.println("Error: Expected function call not found");
						System.exit(-1);
					}
					
					change_stack_function = false;
				}
				
				previous_function = current_function;
				previous_linetype = line_type;
				previous_lineno = lineno;
				
				//An ENTER is redundant with the first line executed in the function
				if (line_type.equals("ENTER")) {
					//If current line is an ENTER and we're in the "primary" string of execution trace (i.e., not in a function called by a function in the primary trace - i.e., stack trace)
					if (!inCalledFunction && current_function.equals(current_function_in_stack)) {
						change_stack_function = true;
						
						//Determine the parameter corresponding to the null_var. If it's not there, exit
						if (!(functionDeclarationTokens == null)) {
							Iterator fd_iterator = functionDeclarationTokens.iterator();
							int param_counter = 0;
							try {
								String next_token = (String)fd_iterator.next(); //FUNCTION
								next_token = (String)fd_iterator.next(); //NAME
								next_token = (String)fd_iterator.next(); //<name of function>
								next_token = (String)fd_iterator.next(); //LP
								while (!(next_token.equals("RP"))) {
									next_token = (String)fd_iterator.next();
									if (param_counter == 0 && next_token.equals("RP")) {
										System.out.println("Null value not propagated through parameter");
										System.exit(-1);
									}
									if (next_token.equals("COMMA")) { //|| next_token.equals("RP")) {
										param_counter++;
									}
									if (next_token.equals(null_var)) {
										param_counter++;
										corresponding_param = param_counter;
										break;
									}
								}
							}
							catch (NoSuchElementException nsee) {
								System.out.println("Error: Incorrect function declaration");
								System.exit(-1);
							}
						}
						else { //reached "script" function with no declaration
							System.out.println("Error: Can't find document.getElementById call");
							System.exit(-1);
						}
					}
					else if (inReturningFunction && current_function.equals(returningFunctions.peek())) {
						returningFunctions.pop();
						if (returningFunctions.size() == 1) {
							inReturningFunction = false;
						}
					}
					continue;
				}
				
				//Keeps track of LPs and RPs inside document.GEBID parameter
				int lp_counter = 0;
				
				//Check if the stream starts with the tokens "RETURN", "NAME", "<name of var>"
				if (tokens_to_analyze.size() < 3) {
					continue;
				}
				String s1 = (String)tokens_it.next();
				String s2 = (String)tokens_it.next();
				if (s1.equals("RETURN") && s2.equals("NAME") && wasReturned) {
					String next_token = (String)tokens_it.next();
					if (next_token.equals("document")) {
						if (tokens_it.hasNext()) {
							next_token = (String)tokens_it.next();
							if (next_token.equals("DOT")) {
								next_token = (String)tokens_it.next();
								next_token = (String)tokens_it.next();
								if (next_token.equals("getElementById")) {
									//This must be the line, so get the ID
									next_token = (String)tokens_it.next(); //LP
									lp_counter++;
									next_token = (String)tokens_it.next();
									if (next_token.equals("LP")) {
										lp_counter++;
									}
									else if (next_token.equals("RP")) {
										lp_counter--;
									}
									//while (!(next_token.equals("RP"))) {
									while (!(lp_counter == 0)) {
										this.GEBID_Id.add(next_token);
										next_token = (String)tokens_it.next();
										if (next_token.equals("LP")) {
											lp_counter++;
										}
										else if (next_token.equals("RP")) {
											lp_counter--;
										}
									}
									this.GEBID_function = current_function;
									this.GEBID_line = lineno;
									this.GEBID_trace = trace;
									return;
								}
							}
							else if (next_token.equals("LP")) { //must be a function call
								//do nothing
								continue;
							}
							else if (next_token.equals("SEMI")) {
								null_var = "document";
								wasReturned = false;
								continue;
							}
							else {
								System.out.println("Error: Returned value not a variable, function call, or document.getElementById call");
								System.exit(-1);
							}
						}
						else {
							null_var = next_token;
							wasReturned = false;
							continue;
						}
					}
					else if (next_token.equals("$")) {
						//This must be the line, so get the ID
						next_token = (String)tokens_it.next(); //LP
						lp_counter++;
						next_token = (String)tokens_it.next();
						if (next_token.equals("LP")) {
							lp_counter++;
						}
						else if (next_token.equals("RP")) {
							lp_counter--;
						}
						//while (!(next_token.equals("RP"))) {
						while (!(lp_counter == 0)) {
							this.GEBID_Id.add(next_token);
							next_token = (String)tokens_it.next();
							if (next_token.equals("LP")) {
								lp_counter++;
							}
							else if (next_token.equals("RP")) {
								lp_counter--;
							}
						}
						this.GEBID_function = current_function;
						this.GEBID_line = lineno;
						this.GEBID_trace = trace;
						return;
					}
					else {
						if (tokens_it.hasNext()) {
							String curr_token = next_token;
							next_token = (String)tokens_it.next();
							if (next_token.equals("LP")) { //must be a function call
								//do nothing
								continue;
							}
							else if (next_token.equals("SEMI")) {
								null_var = curr_token;
								wasReturned = false;
								continue;
							}
							else {
								System.out.println("Error: Returned value not a variable, function call, or document.getElementById call");
								System.exit(-1);
							}
						}
						else {
							null_var = next_token;
							wasReturned = false;
							continue;
						}
					}
				}
				
				//Check if the stream starts with the tokens "NAME", "<name of var>", "ASSIGN", "NAME"
				tokens_it = tokens_to_analyze.iterator();
				if (tokens_to_analyze.size() < 5) {
					continue;
				}
				s1 = (String)tokens_it.next();
				s2 = (String)tokens_it.next();
				String s3 = (String)tokens_it.next();
				String s4 = (String)tokens_it.next();
				if (s1.equals("NAME") && s2.equals(null_var) && s3.equals("ASSIGN") && s4.equals("NAME")) {
					String next_token = (String)tokens_it.next();
					if (next_token.equals("document")) {
						if (tokens_it.hasNext()) {
							next_token = (String)tokens_it.next();
							if (next_token.equals("DOT")) {
								next_token = (String)tokens_it.next();
								next_token = (String)tokens_it.next();
								if (next_token.equals("getElementById")) {
									//This must be the line, so get the ID
									next_token = (String)tokens_it.next(); //LP
									lp_counter++;
									next_token = (String)tokens_it.next();
									if (next_token.equals("LP")) {
										lp_counter++;
									}
									else if (next_token.equals("RP")) {
										lp_counter--;
									}
									//while (!(next_token.equals("RP"))) {
									while (!(lp_counter == 0)) {
										this.GEBID_Id.add(next_token);
										next_token = (String)tokens_it.next();
										if (next_token.equals("LP")) {
											lp_counter++;
										}
										else if (next_token.equals("RP")) {
											lp_counter--;
										}
									}
									this.GEBID_function = current_function;
									this.GEBID_line = lineno;
									this.GEBID_trace = trace;
									return;
								}
							}
							else if (next_token.equals("LP")) { //must be a function call
								//wasReturned = true;
								continue;
							}
							else if (next_token.equals("SEMI")) {
								null_var = "document";
								wasReturned = false;
								continue;
							}
							else {
								System.out.println("Error: Returned value not a variable, function call, or document.getElementById call");
								System.exit(-1);
							}
						}
						else {
							null_var = next_token;
							wasReturned = false;
							continue;
						}
					}
					else if (next_token.equals("$")) {
						//This must be the line, so get the ID
						next_token = (String)tokens_it.next(); //LP
						lp_counter++;
						next_token = (String)tokens_it.next();
						if (next_token.equals("LP")) {
							lp_counter++;
						}
						else if (next_token.equals("RP")) {
							lp_counter--;
						}
						//while (!(next_token.equals("RP"))) {
						while (!(lp_counter == 0)) {
							this.GEBID_Id.add(next_token);
							next_token = (String)tokens_it.next();
							if (next_token.equals("LP")) {
								lp_counter++;
							}
							else if (next_token.equals("RP")) {
								lp_counter--;
							}
						}
						this.GEBID_function = current_function;
						this.GEBID_line = lineno;
						this.GEBID_trace = trace;
						return;
					}
					else {
						if (tokens_it.hasNext()) {
							String curr_token = next_token;
							next_token = (String)tokens_it.next();
							if (next_token.equals("LP")) { //must be a function call
								//wasReturned = true;
								continue;
							}
							else if (next_token.equals("SEMI")) {
								null_var = curr_token;
								wasReturned = false;
								continue;
							}
							else {
								System.out.println("Error: Returned value not a variable, function call, or document.getElementById call");
								System.exit(-1);
							}
						}
						else {
							null_var = next_token;
							wasReturned = false;
							continue;
						}
					}
				}
				
				//Check if the stream starts with the tokens "VAR", "NAME", "<name of var>", "ASSIGN", "NAME"
				tokens_it = tokens_to_analyze.iterator();
				if (tokens_to_analyze.size() < 6) {
					continue;
				}
				s1 = (String)tokens_it.next();
				s2 = (String)tokens_it.next();
				s3 = (String)tokens_it.next();
				s4 = (String)tokens_it.next();
				String s5 = (String)tokens_it.next();
				if (s1.equals("VAR") && s2.equals("NAME") && s3.equals(null_var) && s4.equals("ASSIGN") && s5.equals("NAME")) {
					String next_token = (String)tokens_it.next();
					if (next_token.equals("document")) {
						if (tokens_it.hasNext()) {
							next_token = (String)tokens_it.next();
							if (next_token.equals("DOT")) {
								next_token = (String)tokens_it.next();
								next_token = (String)tokens_it.next();
								if (next_token.equals("getElementById")) {
									//This must be the line, so get the ID
									next_token = (String)tokens_it.next(); //LP
									lp_counter++;
									next_token = (String)tokens_it.next();
									if (next_token.equals("LP")) {
										lp_counter++;
									}
									else if (next_token.equals("RP")) {
										lp_counter--;
									}
									//while (!(next_token.equals("RP"))) {
									while (!(lp_counter == 0)) {
										this.GEBID_Id.add(next_token);
										next_token = (String)tokens_it.next();
										if (next_token.equals("LP")) {
											lp_counter++;
										}
										else if (next_token.equals("RP")) {
											lp_counter--;
										}
									}
									this.GEBID_function = current_function;
									this.GEBID_line = lineno;
									this.GEBID_trace = trace;
									return;
								}
							}
							else if (next_token.equals("LP")) { //must be a function call
								//wasReturned = true;
								continue;
							}
							else if (next_token.equals("SEMI")) {
								null_var = "document";
								wasReturned = false;
								continue;
							}
							else {
								System.out.println("Error: Returned value not a variable, function call, or document.getElementById call");
								System.exit(-1);
							}
						}
						else {
							null_var = next_token;
							wasReturned = false;
							continue;
						}
					}
					else if (next_token.equals("$")) {
						//This must be the line, so get the ID
						next_token = (String)tokens_it.next(); //LP
						lp_counter++;
						next_token = (String)tokens_it.next();
						if (next_token.equals("LP")) {
							lp_counter++;
						}
						else if (next_token.equals("RP")) {
							lp_counter--;
						}
						//while (!(next_token.equals("RP"))) {
						while (!(lp_counter == 0)) {
							this.GEBID_Id.add(next_token);
							next_token = (String)tokens_it.next();
							if (next_token.equals("LP")) {
								lp_counter++;
							}
							else if (next_token.equals("RP")) {
								lp_counter--;
							}
						}
						this.GEBID_function = current_function;
						this.GEBID_line = lineno;
						this.GEBID_trace = trace;
						return;
					}
					else {
						if (tokens_it.hasNext()) {
							String curr_token = next_token;
							next_token = (String)tokens_it.next();
							if (next_token.equals("LP")) { //must be a function call
								//wasReturned = true;
								continue;
							}
							else if (next_token.equals("SEMI")) {
								null_var = curr_token;
								wasReturned = false;
								continue;
							}
							else {
								System.out.println("Error: Returned value not a variable, function call, or document.getElementById call");
								System.exit(-1);
							}
						}
						else {
							null_var = next_token;
							wasReturned = false;
							continue;
						}
					}
				}
			}
		} catch (IOException ioe) {
			System.out.println("Error reading FunctionTrace file");
			System.exit(-1);
		}
	}
	
	String getFunctionName(FunctionTrace ft) {
		FunctionDecl fd = ft.f_decl;
		String name = fd.f_name.substring(0, fd.f_name.indexOf(":::"));
		return name;
	}
	
	int getFunctionLineno(FunctionTrace ft) {
		String name = ft.f_decl.f_name;
		String suffix = name.substring(name.indexOf(":::"));
		String lineno_str = "";
		if (suffix.startsWith(":::EXIT")) {
			lineno_str = suffix.substring(7);
		}
		else if (suffix.startsWith(":::INTERMEDIATE")) {
			lineno_str = suffix.substring(15);
		}
		else if (suffix.startsWith(":::ENTER")) {
			lineno_str = "1";
		}
		else if (suffix.startsWith(":::ASYNC") || suffix.startsWith(":::ASYNC_CALL")) {
			lineno_str = "0"; //we won't need the line numbers for these anyway
		}
		else {
			System.out.println("Error: FunctionTrace suffix undefined");
			System.exit(-1);
		}
		int lineno = Integer.parseInt(lineno_str);
		return lineno;
	}
	
	String getLineType(FunctionTrace ft) {
		String name = ft.f_decl.f_name;
		String suffix = name.substring(name.indexOf(":::"));
		String linetype = "";
		if (suffix.startsWith(":::EXIT")) {
			linetype = "EXIT";
		}
		else if (suffix.startsWith(":::INTERMEDIATE")) {
			linetype = "INTERMEDIATE";
		}
		else if (suffix.startsWith(":::ENTER")) {
			linetype = "ENTER";
		}
		else if (suffix.startsWith(":::ASYNC_CALL")) { //must be checked before :::ASYNC !!
			linetype = "ASYNC_CALL";
		}
		else if (suffix.startsWith(":::ASYNC")) {
			linetype = "ASYNC";
		}
		else {
			System.out.println("Error: Incorrect Function Line Type");
			System.exit(-1);
		}
		
		return linetype;
	}
	
	String getLine(FunctionTrace ft) {
		String current_function = getFunctionName(ft);
		int lineno = getFunctionLineno(ft);
		String strLine = null;
		try {
			FileInputStream fstream = new FileInputStream(JS_FILES_FOLDER + "/" + current_function + ".js");
			DataInputStream din = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(din));
			
			String functionDeclaration = null;
			List<String> functionDeclarationTokens = null;
			int linesToReadAhead = lineno-1; //subtract 1 because this next check counts as one read
			if ((strLine = br.readLine()) != null) {
				List<String> token_stream = getTokenStream(strLine);
				Iterator token_stream_it = token_stream.iterator();
				if (token_stream_it.hasNext()) {
					String firstToken = (String)token_stream_it.next();
					if (firstToken.equals("FUNCTION")) {
						//read next line - considered line no. 1
						//if ((strLine = br.readLine()) == null) {
						//	System.out.println("Error: Empty function");
						//	System.exit(-1);
						//}
						linesToReadAhead++;
						functionDeclaration = strLine;
						functionDeclarationTokens = token_stream;
					}
				}
			}
			else {
				System.out.println("Error: Empty function");
				System.exit(-1);
			}
			
			for (int i = 0; i < linesToReadAhead; i++) {
				if ((strLine = br.readLine()) == null) {
					System.out.println("Error: Function does not match with FunctionTrace line number");
					System.exit(-1);
				}
			}
		}
		catch (Exception e) {
			System.out.println("Error reading function file");
			System.exit(-1);
		}
		
		return strLine;
	}
	
	List<String> getTokenStream(String str_to_parse) {
		Parser ps = new Parser(new CompilerEnvirons());
		TokenStream ts = ps.initForUnitTest(new StringReader(str_to_parse), "", 1, false);
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
	
	String getCallbackFunctionCall(FunctionTrace ft) {
		//Ensure ft is of type ASYNC_CALL
		String type = getLineType(ft);
		if (!type.equals("ASYNC_CALL")) {
			System.out.println("Error: Cannot get callback function call from non-ASYNC_CALL trace");
			System.exit(-1);
		}
		
		//Find which index contains the variable FuncCall
		List<VariableDesc> varDescList = ft.f_decl.var_descs;
		Iterator varDescIt = varDescList.iterator();
		int indexOfFuncCall = -1;
		boolean foundIndex = false;
		int counter = 0;
		while (varDescIt.hasNext() && !foundIndex) {
			VariableDesc varDesc = (VariableDesc)varDescIt.next();
			if (varDesc.getVarName().equals("FuncCall")) {
				indexOfFuncCall = counter;
				foundIndex = true;
			}
			counter++;
		}
		
		if (!foundIndex) {
			System.out.println("Error: Callback function call not stored in ASYNC_CALL");
			System.exit(-1);
		}
		
		String theCall = ft.var_values.get(indexOfFuncCall);
		
		//Strip the leading and trailing quotation marks (note that the dtrace always uses double quotes)
		if (theCall.startsWith("\"") && theCall.endsWith("\"")) {
			theCall = theCall.substring(1,theCall.length()-1);
		}
		
		return theCall;
	}
}