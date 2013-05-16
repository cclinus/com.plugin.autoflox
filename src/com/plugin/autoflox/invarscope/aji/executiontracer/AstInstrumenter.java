package com.plugin.autoflox.invarscope.aji.executiontracer;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.Symbol;

import com.plugin.autoflox.invarscope.aji.JSASTModifier;
import com.plugin.autoflox.service.FileManager;

/**
 * This class is used to visit all JS nodes. When a node matches a certain
 * condition, this class will add instrumentation code near this code.
 * 
 * @author Frank Groeneveld
 * @version $Id: AstInstrumenter.java 6162 2009-12-16 13:56:21Z frank $
 */
public class AstInstrumenter extends JSASTModifier {

	public static final String JSINSTRUMENTLOGNAME = "window.jsExecutionTrace";

	/**
	 * List with regular expressions of variables that should not be
	 * instrumented.
	 */
	private List<String> excludeVariableNamesList = new ArrayList<String>();

	private boolean domModifications = false;

	/**
	 * Construct without patterns.
	 */
	public AstInstrumenter() {
		super();
		excludeVariableNamesList = new ArrayList<String>();
	}

	/**
	 * Constructor with patterns.
	 * 
	 * @param excludes
	 *            List with variable patterns to exclude.
	 */
	public AstInstrumenter(List<String> excludes) {
		excludeVariableNamesList = excludes;
	}

	/**
	 * Return an AST of the variable logging functions.
	 * 
	 * @return The AstNode which contains functions.
	 * @throws IOException
	 */
	private AstNode jsLoggingFunctions() throws IOException {
		String code;

		/* FROLIN: Diff. versions of addvariable.js */
		String instrumentationCode = FileManager.getAddvariableScript();
		if (!this.instrumentAsyncs) {
			instrumentationCode = FileManager.getAddvariableNoAsyncScript();
		}

		File js = new File(instrumentationCode);
		// System.out.println(this.getClass().getResource(instrumentationCode).getFile());
		// code = Helper.getContent(js);
		code = getContent(js);
		// code = Files.toString(js, Charsets.UTF_8);

		return parse(code);
	}

	@Override
	protected AstNode createNode(FunctionNode function, String postfix,
			int lineNo) {
		String name;
		String code;
		String[] variables = getVariablesNamesInScope(function);

		name = getFunctionName(function);

		if (name == null) {
			// Detect anonymous function
			if (!AnonymousFunctionTracer.isAnonymousFunctionAdded(
					AnonymousFunctionTracer.fileName,
					AnonymousFunctionTracer.scriptTagNo, function.getLineno())) {
				// This is a new anonymous function, use counter as anonymous
				// function id
				AnonymousFunctionTracer.anonymousFunctionCouter++;
				// Create entity for this new anonymous function
				AnonymousFunctionEntity newAnonymousFunctionEntity = new AnonymousFunctionEntity(
						AnonymousFunctionTracer.fileName,
						AnonymousFunctionTracer.scriptTagNo,
						function.getLineno(),
						AnonymousFunctionTracer.anonymousFunctionCouter);
				AnonymousFunctionTracer
						.addToAnonymousFunctionList(newAnonymousFunctionEntity);
				name = "anonymous-"
						+ AnonymousFunctionTracer.anonymousFunctionCouter;

				// Dump this newfile to js folder as well
				String afFilePath = FileManager.getProxyJsSourceFolder() + name + ".js";

				File file = new File(afFilePath);
				// if file doesnt exists, then create it
				try {
					if (!file.exists()) {
						file.createNewFile();

					}
					FileWriter fw;
					fw = new FileWriter(file.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write(function.toSource());
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {
				AnonymousFunctionEntity oldAnonymousFunctionEntity = AnonymousFunctionTracer
						.getAnonymousFunctionEntity(
								AnonymousFunctionTracer.fileName,
								AnonymousFunctionTracer.scriptTagNo,
								function.getLineno());
				name = "anonymous-" + oldAnonymousFunctionEntity.getId();
			}
		}

		// Frolin: Added '|| postfix == ":::INTERMEDIATE"'
		if (postfix == ProgramPoint.EXITPOSTFIX || postfix == ":::INTERMEDIATE") {
			postfix += lineNo;
		}

		/* only add instrumentation code if there are variables to log */
		if (variables.length == 0) {
			code = "/* empty */";
		} else {
			/*
			 * TODO: this uses JSON.stringify which only works in Firefox? make
			 * browser indep.
			 */
			/* post to the proxy server */
			code = "send(new Array('" + getScopeName() + "." + name + "', '"
					+ postfix + "', new Array(";

			String vars = "";
			for (int i = 0; i < variables.length; i++) {
				/* only instrument variables that should not be excluded */
				if (shouldInstrument(variables[i])) {
					int colonIndex = variables[i].indexOf(":");
					String varNameNoSuffix = variables[i].substring(0,
							colonIndex);
					/* FROLIN: RCA_errorMsg variable already handled */
					if (variables[i].equals("RCA_errorMsg:local")) {
						continue;
					}
					vars += "addVariable('" + variables[i] + "', "
							+ varNameNoSuffix + "),";
				}
			}

			/* FROLIN: If error, add variable for error message */
			if (postfix.equals(":::ERROR")) {
				vars += "addVariable('RCA_errorMsg:local', err.message),";
			}

			/* FROLIN: if async, add variable RCA_timerID */
			if (postfix.equals(":::ASYNC")) {
				vars += "addVariable('RCA_timerID', RCA_timerID),";
			}
			if (vars.length() > 0) {
				/* remove last comma */
				vars = vars.substring(0, vars.length() - 1);
				code += vars + ")));";
			} else {
				/* no variables to instrument here, so just return an empty node */
				code = "/* empty */";
			}
		}
		return parse(code);
	}

	@Override
	protected AstNode createNode(AstRoot root, String postfix, int lineNo,
			int rootCount) {
		String name;
		String code;
		String[] variables = getVariablesNamesInScope(root);

		name = "root" + rootCount;
		// Frolin: Added '|| postfix == ":::INTERMEDIATE"'
		if (postfix == ProgramPoint.EXITPOSTFIX || postfix == ":::INTERMEDIATE") {
			postfix += lineNo;
		}

		/* only add instrumentation code if there are variables to log */
		if (variables.length == 0) {
			code = "/* empty */";
		} else {
			/*
			 * TODO: this uses JSON.stringify which only works in Firefox? make
			 * browser indep.
			 */
			/* post to the proxy server */
			code = "send(new Array('" + getScopeName() + "." + name + "', '"
					+ postfix + "', new Array(";

			String vars = "";
			for (int i = 0; i < variables.length; i++) {
				/* only instrument variables that should not be excluded */
				if (shouldInstrument(variables[i])) {
					int colonIndex = variables[i].indexOf(":");
					String varNameNoSuffix = variables[i].substring(0,
							colonIndex);
					/* FROLIN: RCA_errorMsg variable already handled */
					if (variables[i].equals("RCA_errorMsg:local"))
						continue;
					vars += "addVariable('" + variables[i] + "', "
							+ varNameNoSuffix + "),";
				}
			}

			/* FROLIN: If error, add variable for error message */
			if (postfix.equals(":::ERROR")) {
				vars += "addVariable('RCA_errorMsg:local', err.message),";
			}

			/* FROLIN: if async, add variable RCA_timerID */
			if (postfix.equals(":::ASYNC")) {
				vars += "addVariable('RCA_timerID', RCA_timerID),";
			}
			if (vars.length() > 0) {
				/* remove last comma */
				vars = vars.substring(0, vars.length() - 1);
				code += vars + ")));";
			} else {
				/* no variables to instrument here, so just return an empty node */
				code = "/* empty */";
			}
		}
		return parse(code);
	}

	/**
	 * Check if we should instrument this variable by matching it against the
	 * exclude variable regexps.
	 * 
	 * @param name
	 *            Name of the variable.
	 * @return True if we should add instrumentation code.
	 */
	private boolean shouldInstrument(String name) {
		if (name == null) {
			return false;
		}

		/* is this an excluded variable? */
		for (String regex : excludeVariableNamesList) {
			if (name.matches(regex)) {
				// LOGGER.debug("Not instrumenting variable " + name);
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns all variables in scope.
	 * 
	 * @param func
	 *            The function.
	 * @return All variables in scope.
	 */
	private String[] getVariablesNamesInScope(Scope scope) {
		TreeSet<String> result = new TreeSet<String>();

		/*
		 * TODO: Frank, what to do with object/classes? For example test.test?
		 */

		boolean doneFirst = false;
		String suffix = "local";

		do {
			if (!doneFirst && scope instanceof AstRoot) {
				suffix = "global";
			}
			/* get the symboltable for the current scope */
			Map<String, Symbol> t = scope.getSymbolTable();
			if (t != null) {
				for (String key : t.keySet()) {
					/* read the symbol */
					Symbol symbol = t.get(key);
					/* only add variables and function parameters */
					if (symbol.getDeclType() == Token.LP
							|| symbol.getDeclType() == Token.VAR) {
						result.add(symbol.getName() + ":" + suffix);
					}
				}
			}

			doneFirst = true;
			suffix = "global";
			/* get next scope (upwards) */
			scope = scope.getEnclosingScope();
		} while (scope != null);

		/* return the result as a String array */
		return result.toArray(new String[0]);
	}

	@Override
	public void finish(AstRoot node) {
		/* add initialization code for the function and logging array */
		try {
			node.addChildToFront(jsLoggingFunctions());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void start() {
		/* nothing to do here */
	}

	@Override
	protected AstNode createPointNode(String objectAndFunction, int lineNo) {
		/* TODO: Frank, save + "." + objectAndFunction also */
		String code = "send(new Array('" + getScopeName() + "line" + lineNo
				+ "', '" + ProgramPoint.POINTPOSTFIX + lineNo
				+ "', new Array(addVariable('"
				+ objectAndFunction.replaceAll("\\\'", "\\\\\'") + "', "
				+ objectAndFunction + "))));";

		if (shouldInstrumentDOMModifications()) {
			/*
			 * FIXME: doesn't work on Mac OS, some problem with strings being
			 * parsed as int?
			 */
			return parse(code);
		} else {
			return parse("/* empty */");
		}
	}

	private boolean shouldInstrumentDOMModifications() {
		return domModifications;
	}

	/**
	 * Add instrumentation to dynamic DOM modifications. (Still buggy)
	 */
	public void instrumentDOMModifications() {
		domModifications = true;
	}

	/**
	 * Returns the file contents without stripping line-endings.
	 * 
	 * @param file
	 *            File to read out.
	 * @return Contents including line-endings.
	 */
	public static String getContent(File file) {
		StringBuilder contents = new StringBuilder();

		try {
			BufferedReader input = new BufferedReader(new FileReader(file));
			try {
				String line = null; // not declared within while loop
				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append("\n");
				}
			} finally {
				input.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return contents.toString();
	}
}
