package com.plugin.autoflox.service.aji;

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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.WhileLoop;

import com.plugin.autoflox.service.aji.executiontracer.ProgramPoint;

//import com.crawljax.core.CrawljaxController;
//import com.crawljax.plugins.aji.executiontracer.ProgramPoint;

/**
 * Abstract class that is used to define the interface and some functionality for the NodeVisitors
 * that modify JavaScript.
 * 
 * @author Frank Groeneveld
 * @version $Id: JSASTModifier.java 6161 2009-12-16 13:47:15Z frank $
 */
public abstract class JSASTModifier implements NodeVisitor {

	private final Map<String, String> mapper = new HashMap<String, String>();

	//protected static final Logger LOGGER = Logger.getLogger(CrawljaxController.class.getName());
	
	protected boolean instrumentAsyncs = true;

	/**
	 * This is used by the JavaScript node creation functions that follow.
	 */
	private CompilerEnvirons compilerEnvirons = new CompilerEnvirons();

	/**
	 * Contains the scopename of the AST we are visiting. Generally this will be the filename
	 */
	private String scopeName = null;
	
	/**
	 * @param scopeName
	 *            the scopeName to set
	 */
	public void setScopeName(String scopeName) {
		this.scopeName = scopeName;
	}

	/**
	 * @return the scopeName
	 */
	public String getScopeName() {
		return scopeName;
	}

	/**
	 * Abstract constructor to initialize the mapper variable.
	 */
	protected JSASTModifier() {
		/* add -<number of arguments> to also make sure number of arguments is the same */
		mapper.put("addClass", "attr('class')");
		mapper.put("removeClass", "attr('class')");

		mapper.put("css-2", "css(%0)");
		mapper.put("attr-2", "attr(%0)");
		mapper.put("append", "html()");
	}
	
	public void setInstrumentAsyncs(boolean val) {
		instrumentAsyncs = val;
	}

	/**
	 * Parse some JavaScript to a simple AST.
	 * 
	 * @param code
	 *            The JavaScript source code to parse.
	 * @return The AST node.
	 */
	public AstNode parse(String code) {
		Parser p = new Parser(compilerEnvirons, null);
		//compilerEnvirons.setErrorReporter(new ConsoleErrorReporter());
		//Parser p = new Parser(compilerEnvirons, new ConsoleErrorReporter());
		return p.parse(code, null, 0);
	}

	/**
	 * Find out the function name of a certain node and return "anonymous" if it's an anonymous
	 * function.
	 * 
	 * @param f
	 *            The function node.
	 * @return The function name.
	 */
	protected String getFunctionName(FunctionNode f) {
		Name functionName = f.getFunctionName();

		if (functionName == null) {
			return "anonymous" + f.getLineno();
		} else {
			return functionName.toSource();
		}
	}

	/**
	 * Creates a node that can be inserted at a certain point in function.
	 * 
	 * @param function
	 *            The function that will enclose the node.
	 * @param postfix
	 *            The postfix function name (enter/exit).
	 * @param lineNo
	 *            Linenumber where the node will be inserted.
	 * @return The new node.
	 */
	protected abstract AstNode createNode(FunctionNode function, String postfix, int lineNo);
	
	/**
	 * Creates a node that can be inserted at a certain point in the AST root.
	 * @param root
	 * 			The AST root that will enclose the node.
	 * @param postfix
	 * 			The postfix name.
	 * @param lineNo
	 * 			Linenumber where the node will be inserted.
	 * @param rootCount
	 * 			Unique integer that identifies the AstRoot
	 * @return The new node
	 */
	protected abstract AstNode createNode(AstRoot root, String postfix, int lineNo, int rootCount);

	/**
	 * Creates a node that can be inserted before and after a DOM modification statement (such as
	 * jQuery('#test').addClass('bla');).
	 * 
	 * @param shouldLog
	 *            The variable that should be logged (for example jQuery('#test').attr('style'))
	 * @param lineNo
	 *            The line number where this will be inserted.
	 * @return The new node.
	 */
	protected abstract AstNode createPointNode(String shouldLog, int lineNo);

	/**
	 * Create a new block node with two children.
	 * 
	 * @param node
	 *            The child.
	 * @return The new block.
	 */
	private Block createBlockWithNode(AstNode node) {
		Block b = new Block();

		b.addChild(node);

		return b;
	}

	/**
	 * @param node
	 *            The node we want to have wrapped.
	 * @return The (new) node parent (the block probably)
	 */
	private AstNode makeSureBlockExistsAround(AstNode node) {
		AstNode parent = node.getParent();

		if (parent instanceof IfStatement) {
			/* the parent is an if and there are no braces, so we should make a new block */
			IfStatement i = (IfStatement) parent;

			/* replace the if or the then, depending on what the current node is */
			if (i.getThenPart().equals(node)) {
				i.setThenPart(createBlockWithNode(node));
			} else {
				i.setElsePart(createBlockWithNode(node));
			}
		} else if (parent instanceof WhileLoop) {
			/* the parent is a while and there are no braces, so we should make a new block */
			/* I don't think you can find this in the real world, but just to be sure */
			WhileLoop w = (WhileLoop) parent;
			w.setBody(createBlockWithNode(node));
		} else if (parent instanceof ForLoop) {
			/* the parent is a for and there are no braces, so we should make a new block */
			/* I don't think you can find this in the real world, but just to be sure */
			ForLoop f = (ForLoop) parent;
			f.setBody(createBlockWithNode(node));
		}
		// else if (parent instanceof SwitchCase) {
		// SwitchCase s = (SwitchCase) parent;
		// List<AstNode> statements = new TreeList(s.getStatements());

		// for (int i = 0; i < statements.size(); i++) {
		// if (statements.get(i).equals(node)) {
		// statements.add(i, newNode);

		/**
		 * TODO: Frank, find a way to do this without concurrent modification exceptions.
		 */
		// s.setStatements(statements);
		// break;
		// }
		// }

		// }
		return node.getParent();
	}

	/**
	 * Actual visiting method.
	 * 
	 * @param node
	 *            The node that is currently visited.
	 * @return Whether to visit the children.
	 */
	@Override
	public boolean visit(AstNode node) {
		FunctionNode func;
		
		if (!((node instanceof FunctionNode || node instanceof ReturnStatement || node instanceof SwitchCase || node instanceof AstRoot || node instanceof ExpressionStatement || node instanceof BreakStatement || node instanceof ContinueStatement || node instanceof ThrowStatement || node instanceof VariableDeclaration))) {// || node instanceof ExpressionStatement || node instanceof BreakStatement || node instanceof ContinueStatement || node instanceof ThrowStatement || node instanceof VariableDeclaration || node instanceof ReturnStatement || node instanceof SwitchCase)) {
			return true;
		}

		if (node instanceof FunctionNode) {
			func = (FunctionNode) node;
			
			/*Output the source code to a file*/
			/*
			if (func.getName().equals("change_promo")) {
				System.out.println(func.toString());
			}
			*/

			/* this is function enter */
			AstNode newNode = createNode(func, ProgramPoint.ENTERPOSTFIX, func.getLineno());

			func.getBody().addChildToFront(newNode);
			
			/*START FROLIN'S CODE*/
			/*
			node = (AstNode) func.getBody().getFirstChild();
			node = (AstNode) node.getNext(); //The first node is the node just added in front, so get next node
			int firstLine = 0;
			if (node != null) {
				firstLine = node.getLineno();
			}
			while (node != null) {
				//Check if this child is the last line
				AstNode testNode = (AstNode) node.getNext();
				if (testNode == null) {
					newNode = createNode(func, ProgramPoint.EXITPOSTFIX, node.getLineno()-firstLine+1);
					func.getBody().addChildrenToBack(newNode);
					break;
				}
				
				if (node instanceof ReturnStatement || node instanceof IfStatement || node instanceof Block || node instanceof WhileLoop || node instanceof ForLoop || ) {
					node = (AstNode) node.getNext();
					continue;
				}
				
				newNode = createNode(func, ":::INTERMEDIATE", node.getLineno()-firstLine+1);
				func.getBody().addChildAfter(newNode, node);
				
				//Get the node that's two positions after (since the next node will always be the newly added node)
				node = (AstNode) node.getNext();
				node = (AstNode) node.getNext();
			}
			*/
			/*END FROLIN'S CODE*/
			
			node = (AstNode) func.getBody().getFirstChild();
			node = (AstNode) node.getNext(); //The first node is the node just added in front, so get next node
			int firstLine = 0;
			if (node != null) {
				firstLine = node.getLineno();
			}

			/* get last line of the function */
			node = (AstNode) func.getBody().getLastChild();
			/* if this is not a return statement, we need to add logging here also */
			if (!(node instanceof ReturnStatement)) {
				AstNode newNode_end = createNode(func, ProgramPoint.EXITPOSTFIX, node.getLineno()-firstLine+1);
				/* add as last statement */
				func.getBody().addChildToBack(newNode_end);
			}
			
			/* enclose function with try...catch */
			Block b = new Block();
			AstNode curr_node = (AstNode) func.getBody().getFirstChild();
			while (curr_node != null) {
				AstNode peeked_node = (AstNode) curr_node.getNext(); //curr_node's parent will change, so getNext() will also change. Thus, peek the next node first
				b.addStatement(curr_node);
				curr_node = peeked_node;
			}
			TryStatement ts = new TryStatement();
			ts.setTryBlock(b);
			
			CatchClause cc = new CatchClause();
			Name catchVar = new Name(0,"err");
			cc.setVarName(catchVar);
			Block catch_b = new Block();
			AstNode newNode_try = createNode(func,":::ERROR",func.getLineno());
			Name errorObj = new Name(0,"Error");
			StringLiteral errStr = new StringLiteral();
			errStr.setValue("Root Cause Analyzer: Error detected");
			errStr.setQuoteCharacter('\"');
			NewExpression newExpr = new NewExpression();
			newExpr.setTarget(errorObj);
			newExpr.addArgument(errStr);
			ThrowStatement throw_st = new ThrowStatement();
			throw_st.setExpression(newExpr);
			catch_b.addStatement(newNode_try);
			catch_b.addStatement(throw_st);
			cc.setBody(catch_b);
			ts.addCatchClause(cc);
			
			Block func_block = new Block();
			func_block.addStatement(ts);
			func.setBody(func_block);
			
			//System.out.println(func.toSource());
			
			if (!instrumentAsyncs)
				return true;
			
			/*add RCA_timerID parameter*/
			Name rca_timer_id = new Name(0,"RCA_timerID");
			func.addParam(rca_timer_id);
			
			/*add an if statement prior to the catch block in case this is async call*/
			//IfStatement async_if = new IfStatement();
			String async_if_code = "if (typeof RCA_timerID != 'undefined') { }";
			AstNode async_if_tmp = parse(async_if_code);
			async_if_tmp = (AstNode)async_if_tmp.getFirstChild();
			if (!(async_if_tmp instanceof IfStatement)) {
				System.out.println("Error instrumenting function");
				System.exit(-1);
			}
			else {
				//Add "then" part of if statement
				//The marker's line
				IfStatement async_if = (IfStatement)async_if_tmp;
				AstNode async_marker = createNode(func,":::ASYNC",func.getLineno());
				async_if.setThenPart(async_marker);
				makeSureBlockExistsAround(async_marker);
				func.getBody().addChildToFront(async_if);
			}
		}
		else if (node instanceof AstRoot) {
			AstRoot rt = (AstRoot) node;
			
			if (rt.getSourceName() == null) { //make sure this is an actual AstRoot, not one we created
				return true;
			}
			
			//this is the entry point of the AST root
			m_rootCount++;
			AstNode newNode = createNode(rt, ProgramPoint.ENTERPOSTFIX, rt.getLineno(), m_rootCount);

			rt.addChildToFront(newNode);
			
			node = (AstNode) rt.getFirstChild();
			node = (AstNode) node.getNext(); //The first node is the node just added in front, so get next node
			int firstLine = 0;
			if (node != null) {
				firstLine = node.getLineno();
			}
			
			// get last line of the function
			node = (AstNode) rt.getLastChild();
			//if this is not a return statement, we need to add logging here also
			if (!(node instanceof ReturnStatement)) {
				AstNode newNode_end = createNode(rt, ProgramPoint.EXITPOSTFIX, node.getLineno()-firstLine+1, m_rootCount);
				//add as last statement
				rt.addChildToBack(newNode_end);
			}
			
			//enclose with try...catch
			Block b = new Block();
			AstNode curr_node = (AstNode) rt.getFirstChild();
			while (curr_node != null) {
				AstNode peeked_node = (AstNode) curr_node.getNext(); //curr_node's parent will change, so getNext() will also change. Thus, peek the next node first
				b.addStatement(curr_node);
				curr_node = peeked_node;
			}
			TryStatement ts = new TryStatement();
			ts.setTryBlock(b);
			
			CatchClause cc = new CatchClause();
			Name catchVar = new Name(0,"err");
			cc.setVarName(catchVar);
			Block catch_b = new Block();
			AstNode newNode_try = createNode(rt,":::ERROR",rt.getLineno(),m_rootCount);
			Name errorObj = new Name(0,"Error");
			StringLiteral errStr = new StringLiteral();
			errStr.setValue("Root Cause Analyzer: Error detected");
			errStr.setQuoteCharacter('\"');
			NewExpression newExpr = new NewExpression();
			newExpr.setTarget(errorObj);
			newExpr.addArgument(errStr);
			ThrowStatement throw_st = new ThrowStatement();
			throw_st.setExpression(newExpr);
			catch_b.addStatement(newNode_try);
			catch_b.addStatement(throw_st);
			cc.setBody(catch_b);
			ts.addCatchClause(cc);
			
			Block func_block = new Block();
			func_block.addStatement(ts);
			rt.removeChildren();
			rt.addChild(func_block);
		}
		//else if (node instanceof BreakStatement || node instanceof ConditionalExpression || node instanceof ContinueStatement || node instanceof ExpressionStatement || node instanceof FunctionCall || node instanceof Assignment || node instanceof InfixExpression || node instanceof ThrowStatement || node instanceof UnaryExpression || node instanceof VariableDeclaration || node instanceof VariableInitializer || node instanceof XmlDotQuery || node instanceof XmlMemberGet || node instanceof XmlPropRef || node instanceof Yield) {
		else if (node instanceof ExpressionStatement || node instanceof BreakStatement || node instanceof ContinueStatement || node instanceof ThrowStatement || node instanceof VariableDeclaration) {
			if (node instanceof VariableDeclaration) {
				//Make sure this variable declaration is not part of a for loop
				if (node.getParent() instanceof ForLoop) {
					return true;
				}
			}
			/* check if node is the ThrowStatement added in the instrumentation */
			if (node instanceof ThrowStatement) {
				ThrowStatement throw_s = (ThrowStatement) node;
				if (throw_s.getExpression() instanceof NewExpression) {
					NewExpression n_ex = (NewExpression) throw_s.getExpression();
					if (n_ex.getTarget() instanceof Name) {
						Name target = (Name)n_ex.getTarget();
						if (target.getIdentifier().equals("Error")) {
							List<AstNode> list_ast = n_ex.getArguments();
							if (list_ast.iterator().hasNext()) {
								AstNode strLit_node = list_ast.iterator().next();
								if (strLit_node instanceof StringLiteral) {
									StringLiteral strLit = (StringLiteral)strLit_node;
									if (strLit.getValue().equals("Root Cause Analyzer: Error detected")) {
										return true;
									}
								}
							}
							
						}
					}
				}
			}
			
			//Make sure additional try statement is not instrumented
			if (node instanceof TryStatement) {
				return true; //no need to add instrumentation before try statement anyway since we only instrument what's inside the blocks
			}
			
			func = node.getEnclosingFunction();
			
			if (func != null) {
				AstNode firstLine_node = (AstNode) func.getBody().getFirstChild();
				if (func instanceof FunctionNode && firstLine_node instanceof IfStatement) { //Perform extra check due to addition if statement
					firstLine_node = (AstNode) firstLine_node.getNext();
				}
				if (func instanceof FunctionNode && firstLine_node instanceof TryStatement) {
					TryStatement firstLine_node_try = (TryStatement) firstLine_node;
					firstLine_node = (AstNode) firstLine_node_try.getTryBlock().getFirstChild();
				}
				firstLine_node = (AstNode) firstLine_node.getNext();
				int firstLine = 0;
				if (firstLine_node != null) {
					//If first child is an ExpressionStatement or VariableDeclaration, then there might be multiple instances of the instrumented node at the beginning of the FunctionNode's list of children
					while (firstLine_node != null) {
						firstLine = firstLine_node.getLineno();
						if (firstLine > 0) {
							break;
						}
						else {
							firstLine_node = (AstNode) firstLine_node.getNext();
						}
					}
				}
				
				if (node.getLineno() >= firstLine) {
					AstNode newNode = createNode(func, ":::INTERMEDIATE", node.getLineno()-firstLine+1);
					//AstNode parent = node.getParent();
					
					AstNode parent = makeSureBlockExistsAround(node);
					
					//parent.addChildAfter(newNode, node);
					try {
						parent.addChildBefore(newNode, node);
					}
					catch (NullPointerException npe) {
						System.out.println(npe.getMessage());
					}
				}
			}
			else { //The expression must be outside a function
				AstRoot rt = node.getAstRoot();
				if (rt == null || rt.getSourceName() == null) {
					return true;
				}
				AstNode firstLine_node = (AstNode) rt.getFirstChild();
				//if (firstLine_node instanceof IfStatement) { //Perform extra check due to addition if statement
				//	firstLine_node = (AstNode) firstLine_node.getNext();
				//}
				if (firstLine_node instanceof Block) {
					firstLine_node = (AstNode)firstLine_node.getFirstChild(); //Try statement
				}
				if (firstLine_node instanceof TryStatement) {
					TryStatement firstLine_node_try = (TryStatement) firstLine_node;
					firstLine_node = (AstNode) firstLine_node_try.getTryBlock().getFirstChild();
				}
				firstLine_node = (AstNode) firstLine_node.getNext();
				int firstLine = 0;
				if (firstLine_node != null) {
					//If first child is an ExpressionStatement or VariableDeclaration, then there might be multiple instances of the instrumented node at the beginning of the FunctionNode's list of children
					while (firstLine_node != null) {
						firstLine = firstLine_node.getLineno();
						if (firstLine > 0) {
							break;
						}
						else {
							firstLine_node = (AstNode) firstLine_node.getNext();
						}
					}
				}
				
				if (node.getLineno() >= firstLine) {
					AstNode newNode = createNode(rt, ":::INTERMEDIATE", node.getLineno()-firstLine+1, m_rootCount);
					//AstNode parent = node.getParent();
					
					AstNode parent = makeSureBlockExistsAround(node);
					
					//parent.addChildAfter(newNode, node);
					try {
						parent.addChildBefore(newNode, node);
					}
					catch (NullPointerException npe) {
						System.out.println(npe.getMessage());
					}
				}
			}
		}
		/*
		else if (node instanceof IfStatement) {
			func = node.getEnclosingFunction();
			AstNode firstLine_node = (AstNode) func.getBody().getFirstChild();
			firstLine_node = (AstNode) firstLine_node.getNext();
			int firstLine = 0;
			if (node != null) {
				firstLine = firstLine_node.getLineno();
			}
			
			IfStatement if_node = (IfStatement) node;
			
			//Instrument condition node
			AstNode cond_node = (AstNode) if_node.getCondition();
			AstNode newNode = createNode(func, ":::INTERMEDIATE", cond_node.getLineno()-firstLine+1);
			if_node.addChildAfter(newNode, cond_node);
			
			//Instrument thenPart
			AstNode then_node = (AstNode) if_node.getThenPart();
			if (!(then_node instanceof ReturnStatement)) {
				if (then_node instanceof Scope) {
					AstNode scopeChild = (AstNode) then_node.getFirstChild();
					while (scopeChild != null) {
						if (!(scopeChild instanceof ReturnStatement)) {
							newNode = createNode(func, ":::INTERMEDIATE", scopeChild.getLineno()-firstLine+1);
							
							//AstNode parent = makeSureBlockExistsAround(scopeChild);
							//parent.addChildAfter(newNode, scopeChild);
							
							then_node.addChildAfter(newNode, scopeChild);
							
							scopeChild = (AstNode) scopeChild.getNext();
							scopeChild = (AstNode) scopeChild.getNext();
						}
						else {
							scopeChild = (AstNode) scopeChild.getNext();
						}
					}
				}
				else {
					newNode = createNode(func, ":::INTERMEDIATE", then_node.getLineno()-firstLine+1);
					if_node.addChildAfter(newNode, then_node);
				}
			}
			
			//Instrument elsePart
			AstNode else_node = (AstNode) if_node.getElsePart();
			if (else_node != null) {
				if (!(else_node instanceof ReturnStatement)) {
					if (else_node instanceof Scope) {
						AstNode scopeChild = (AstNode) else_node.getFirstChild();
						while (scopeChild != null) {
							if (!(scopeChild instanceof ReturnStatement)) {
								newNode = createNode(func, ":::INTERMEDIATE", scopeChild.getLineno()-firstLine+1);
								
								//AstNode parent = makeSureBlockExistsAround(scopeChild);
								//parent.addChildAfter(newNode, scopeChild);
								
								else_node.addChildAfter(newNode, scopeChild);
								
								scopeChild = (AstNode) scopeChild.getNext();
								scopeChild = (AstNode) scopeChild.getNext();
							}
							else {
								scopeChild = (AstNode) scopeChild.getNext();
							}
						}
					}
					else {
						newNode = createNode(func, ":::INTERMEDIATE", else_node.getLineno()-firstLine+1);
						if_node.addChildAfter(newNode, else_node);
					}
				}
			}
		}
		*/
		//else if (node instanceof IfStatement || node instanceof Block || node instanceof ForLoop || node instanceof WhileLoop) {
		//	func = node.getEnclosingFunction();
			
		//}
		
		else if (node instanceof ReturnStatement) {
			func = node.getEnclosingFunction();
			AstNode firstLine_node = (AstNode) func.getBody().getFirstChild();
			if (func instanceof FunctionNode && firstLine_node instanceof IfStatement) { //Perform extra check due to addition if statement
				firstLine_node = (AstNode) firstLine_node.getNext();
			}
			if (func instanceof FunctionNode && firstLine_node instanceof TryStatement) {
				TryStatement firstLine_node_try = (TryStatement) firstLine_node;
				firstLine_node = (AstNode) firstLine_node_try.getTryBlock().getFirstChild();
			}
			firstLine_node = (AstNode) firstLine_node.getNext();
			int firstLine = 0;
			if (firstLine_node != null) {
				//If first child is an ExpressionStatement or VariableDeclaration, then there might be multiple instances of the instrumented node at the beginning of the FunctionNode's list of children
				while (firstLine_node != null) {
					firstLine = firstLine_node.getLineno();
					if (firstLine > 0) {
						break;
					}
					else {
						firstLine_node = (AstNode) firstLine_node.getNext();
					}
				}
			}
			
			AstNode parent = makeSureBlockExistsAround(node);
			
			/*PARTITION NODE - FROLIN*/
			//Create Name node
			ReturnStatement r_node = (ReturnStatement)node;
			//if (r_node.getReturnValue() instanceof FunctionCall) {
			Name name_node = new Name(0,"invarscope_reserved_var_name");
			AstNode right_node = r_node.getReturnValue();
			if (right_node != null) {
				Assignment assn_node = new Assignment(90,name_node,right_node,0);
				ExpressionStatement expr_node = new ExpressionStatement(assn_node,true);
			
				//AstNode parent = makeSureBlockExistsAround(node);
				/*
				if (parent instanceof SwitchCase) {
					SwitchCase sc = (SwitchCase)parent;
					List<AstNode> statements = sc.getStatements();
					List<AstNode> newStatements = new ArrayList<AstNode>();
					
					Iterator<AstNode> it = statements.iterator();
					while (it.hasNext()) {
						AstNode stmnt = it.next();
						if (stmnt.equals(node)) {
							newStatements.add(expr_node);
						}
						newStatements.add(stmnt);
					}
					sc.setStatements(newStatements);
				}
				*/
				//else {
				parent.addChildBefore(expr_node, node);
				//}
			
				//Remove change return value
				r_node.setReturnValue(name_node);
			}
			/*END PARTITION NODE*/

			AstNode newNode = createNode(func, ProgramPoint.EXITPOSTFIX, node.getLineno()-firstLine+1);

			//Need to wrap return call in block because otherwise, the added node won't be executed (since the function has returned)
			//AstNode parent = makeSureBlockExistsAround(node);
			
			//Special scenario: parent is a SwitchCase (which has first = null)
			/*
			if (parent instanceof SwitchCase) {
				SwitchCase sc = (SwitchCase)parent;
				List<AstNode> statements = sc.getStatements();
				List<AstNode> newStatements = new ArrayList<AstNode>();
				
				Iterator<AstNode> it = statements.iterator();
				while (it.hasNext()) {
					AstNode stmnt = it.next();
					if (stmnt.equals(node)) {
						newStatements.add(newNode);
					}
					newStatements.add(stmnt);
				}
				sc.setStatements(newStatements);
				
				return true;
			}
			*/

			/* the parent is something we can prepend to */
			parent.addChildBefore(newNode, node);

		}
		else if (node instanceof SwitchCase) {
			//Add block around all statements in the switch case
			SwitchCase sc = (SwitchCase)node;
			List<AstNode> statements = sc.getStatements();
			List<AstNode> blockStatement = new ArrayList<AstNode>();
			Block b = new Block();
			
			if (statements != null) {
				Iterator<AstNode> it = statements.iterator();
				while (it.hasNext()) {
					AstNode stmnt = it.next();
					b.addChild(stmnt);
				}
				
				blockStatement.add(b);
				sc.setStatements(blockStatement);
			}
		}
		//else if (node instanceof Name) {

			/* lets detect function calls like .addClass, .css, .attr etc */
		//	if (node.getParent() instanceof PropertyGet
		//	        && node.getParent().getParent() instanceof FunctionCall) {

		//		List<AstNode> arguments =
		//		        ((FunctionCall) node.getParent().getParent()).getArguments();

		//		if (mapper.get(node.toSource()) != null
		//		        || mapper.get(node.toSource() + "-" + arguments.size()) != null) {

		//			/* this seems to be one! */
		//			PropertyGet g = (PropertyGet) node.getParent();

		//			String objectAndFunction = mapper.get(node.toSource());
		//			if (objectAndFunction == null) {
		//				objectAndFunction = mapper.get(node.toSource() + "-" + arguments.size());
		//			}

		//			objectAndFunction = g.getLeft().toSource() + "." + objectAndFunction;

					/* fill in parameters in the "getter" */
		//			for (int i = 0; i < arguments.size(); i++) {
		//				objectAndFunction =
		//				        objectAndFunction.replace("%" + i, arguments.get(i).toSource());
		//			}

		//			AstNode parent = makeSureBlockExistsAround(getLineNode(node));
					/*
					 * TODO: lineno will be off by one. otherwise we might have overlapping problems
					 */

		//			 parent.addChildBefore(createPointNode(objectAndFunction, node.getLineno()),
		//			 getLineNode(node));
		//			 parent.addChildAfter(
		//			 createPointNode(objectAndFunction, node.getLineno() + 1),
		//			 getLineNode(node));

		//		}
		//	}
		//}
		/* have a look at the children of this node */
		return true;
	}

	private AstNode getLineNode(AstNode node) {
		while ((!(node instanceof ExpressionStatement) && !(node instanceof Assignment))
		        || node.getParent() instanceof ReturnStatement) {
			node = node.getParent();
		}
		return node;
	}

	/**
	 * This method is called when the complete AST has been traversed.
	 * 
	 * @param node
	 *            The AST root node.
	 */
	public abstract void finish(AstRoot node);

	/**
	 * This method is called before the AST is going to be traversed.
	 */
	public abstract void start();
	
	private int m_rootCount = 0;
}
