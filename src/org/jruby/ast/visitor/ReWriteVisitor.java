/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Mirko Stocker <me@misto.ch>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ast.visitor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.ast.AliasNode;
import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.CaseNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.Colon2Node;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.CommentDecoratorNode;
import org.jruby.ast.CommentNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefinedNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.rewriteutils.DRegxReWriteVisitor;
import org.jruby.ast.visitor.rewriteutils.HereDocReWriteVisitor;
import org.jruby.ast.visitor.rewriteutils.IgnoreCommentsReWriteVisitor;
import org.jruby.ast.visitor.rewriteutils.Indentor;
import org.jruby.ast.visitor.rewriteutils.MultipleAssignmentReWriteVisitor;
import org.jruby.ast.visitor.rewriteutils.Operators;
import org.jruby.ast.visitor.rewriteutils.ReWriterContext;
import org.jruby.ast.visitor.rewriteutils.ShortIfNodeReWriteVisitor;
import org.jruby.common.NullWarnings;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.parser.RubyParserPool;

/**
 * Visits each node and outputs the corresponding Ruby sourcecode for the nodes. 
 * 
 * @author Mirko Stocker
 * 
 */
public class ReWriteVisitor implements NodeVisitor {
	
	protected final ReWriterContext config;
	

	public ReWriteVisitor(Writer out, int indentationSteps,
			char indentationChar, String source) {
		this(new ReWriterContext(new PrintWriter(out), new Indentor(indentationSteps, indentationChar), source));
	}

	public ReWriteVisitor(OutputStream out, int indentationSteps,
			char indentationChar, String source) {
		this(new ReWriterContext(new PrintWriter(out, true), new Indentor(indentationSteps, indentationChar), source));
	}

	public ReWriteVisitor(ReWriterContext config) {
		this.config = config;
	}
	
	public static void main(String[] args) {

		if (args.length < 1) {
			System.err.println("Please specify a sourcefile.");
			return;
		}

		DefaultRubyParser parser = RubyParserPool.getInstance().borrowParser();
		parser.setWarnings(new NullWarnings());
		parser.init(new RubyParserConfiguration());

		LexerSource lexerSource = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(args[0])));
			lexerSource = new LexerSource(args[0], reader);
		} catch (FileNotFoundException e) {
			System.err.println("Could not find the file " + args[0]);
			return;
		}

		Node rootNode = parser.parse(lexerSource).getAST();
		if (rootNode == null) {
			System.err.println("Source File seems to be empty.");
			return;
		}

		StringBuffer buffer = new StringBuffer();
		String line;
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(args[0])));
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append('\n');
			}
		} catch (IOException e) {
			System.err.println("Could not read the Sourcefile.");
		}
		ReWriteVisitor visitor = new ReWriteVisitor(System.out, 2, ' ', buffer.toString());
		rootNode.accept(visitor);
		visitor.flushStream();

		System.out.println("\n");
	}

	public void flushStream() {
		config.getOutput().flush();
	}
	
	private ShortIfNodeReWriteVisitor createShortIfNodeReWriteVisitor() {
		return new ShortIfNodeReWriteVisitor(config);
	}
	
	private MultipleAssignmentReWriteVisitor createMultipleAssignmentReWriteVisitor() {
		return new MultipleAssignmentReWriteVisitor(config);
	}
	
	private DRegxReWriteVisitor createDRegxReWriteVisitor() {
		return new DRegxReWriteVisitor(config);
	}
	
	private HereDocReWriteVisitor createHereDocReWriteVisitor() {
		return new HereDocReWriteVisitor(config);
	}
	
	private IgnoreCommentsReWriteVisitor createIgnoreCommentsReWriteVisitor() {
		return new IgnoreCommentsReWriteVisitor(config);
	}
	
	private ReWriteVisitor createReWriteVisitor() {
		return new ReWriteVisitor(config);
	}
	
	protected void print(String s) {
		config.getOutput().print(s);
	}
	
	protected void print(char c) {
		config.getOutput().print(c);
	}
	
	protected void print(BigInteger i) {
		config.getOutput().print(i);
	}
	
	protected void print(int i) {
		config.getOutput().print(i);
	}
	
	protected void print(long l) {
		config.getOutput().print(l);
	}
	
	protected void print(double d) {
		config.getOutput().print(d);
	}
	
	private void enterCall() {
		config.getCallDepth().enterCall();
	}

	private void leaveCall() {
		config.getCallDepth().leaveCall();
	}

	private boolean inCall() {
		return config.getCallDepth().inCall();
	}

	protected void printHereDocument() {
		if(!config.hasHereDocument())
			return;
		config.setSkipNextNewline(false);
		print('\n');
		print(config.fetchHereDocument().getContent());
		config.getIndentor().printIndentation(config.getOutput());
		print("EOF");
	}

	protected void printNewlineAndIndentation() {
		printHereDocument();
		print('\n');
		config.getIndentor().printIndentation(config.getOutput());
	}

	private boolean isReceiverACallNode(Node n) {
		if (n instanceof CallNode) {
			CallNode callNode = (CallNode) n;
			return (callNode.getReceiverNode() instanceof CallNode || callNode
					.getReceiverNode() instanceof FCallNode);
		}
		return false;
	}

	private void printCommentsBefore(Node iVisited) {
		Iterator it = iVisited.getComments().iterator();
		while(it.hasNext()) {
			CommentNode n = (CommentNode) it.next();
			if(getStartLine(n) < getStartLine(iVisited)) {
				visitNode(n);
				printNewlineAndIndentation();
			}
		}
	}

	protected boolean printCommentsAfter(Node iVisited) {
		boolean hasComment = false;
		Iterator it = iVisited.getComments().iterator();
		while(it.hasNext()) {
			CommentNode n = (CommentNode) it.next();
			if(getStartLine(n) >= getEndLine(iVisited)) {
				print(' ');
				visitNode(n);
				hasComment = true;
				if(it.hasNext())
					printNewlineAndIndentation();
			}
		}
		return hasComment;
	}
	
	public void visitNode(Node iVisited) {
		if (iVisited != null) {
			printCommentsBefore(iVisited);
			iVisited.accept(this);
			printCommentsAfter(iVisited);
			config.setLastPosition(iVisited.getPosition());
		}
	}

	public void visitIter(Iterator iterator) {
		while (iterator.hasNext()) {
			visitNode((Node) iterator.next());
		}
	}

	private void visitIterAndSkipFirst(Iterator iterator) {
		iterator.next();
		visitIter(iterator);
	}

	private void notImplemented(Node iVisited) {
		print("!" + iVisited + "!");
	}

	private boolean isStartOnNewLine(Node first, Node second) {
		if (first == null || second == null)
			return false;
		return (getStartLine(first) < getStartLine(second));
	}

	private boolean needsParentheses(Node n) {
		if (n == null)
			return false;
		return ((n.childNodes().size() > 1 || inCall() || firstChild(n) instanceof HashNode)
				|| firstChild(n) instanceof NewlineNode || firstChild(n) instanceof IfNode);
	}

	private void printCallArguments(Node argsNode) {

		if (argsNode == null || !hasArguments(argsNode))
			return;

		if (argsNode.childNodes().size() == 1
				&& firstChild(argsNode) instanceof HashNode) {
			HashNode hashNode = (HashNode) firstChild(argsNode);
			print("({");
			printHashNodeContent(hashNode);
			print("})");
			return;
		}

		boolean paranthesesPrinted = needsParentheses(argsNode);

		if (paranthesesPrinted)
			print('(');
		else
			print(' ');

		if (firstChild(argsNode) instanceof NewlineNode)
			config.setSkipNextNewline(true);

		enterCall();

		if (argsNode instanceof SplatNode || argsNode instanceof BlockPassNode)
			visitNode(argsNode);
		else
			visitAndPrintWithSeparator(argsNode.childNodes().iterator());

		if (paranthesesPrinted && !config.getArgsNodeWithBlock().contains(argsNode))
			print(')');

		leaveCall();
	}

	public void visitAndPrintWithSeparator(Iterator it) {
		while (it.hasNext()) {
			Node n = (Node) it.next();
			createIgnoreCommentsReWriteVisitor().visitNode(n);
			if (it.hasNext())
				print(", ");
			if(n.hasComments()) {
				createReWriteVisitor().visitIter(n.getComments().iterator());
				printNewlineAndIndentation();
			}
		}
	}

	public Instruction visitAliasNode(AliasNode iVisited) {
		print("alias ");
		print(iVisited.getNewName());
		print(' ');
		print(iVisited.getOldName());
		printCommentsAtEnd(iVisited);
		return null;
	}

	private int nodeSourceLength(Node n) {
		return (getEndOffset(n) - getStartOffset(n)) + 1;
	}

	private boolean sourceRangeEquals(int start, int stop, String compare) {
		return (stop <= config.getSource().length() && sourceSubStringEquals(start, stop - start, compare));
	}

	private boolean sourceRangeEquals(ISourcePosition pos, String compare) {
		return sourceRangeEquals(pos.getStartOffset(), pos.getEndOffset() + 1, compare);
	}

	public Instruction visitAndNode(AndNode iVisited) {
		enterCall();
		visitNode(iVisited.getFirstNode());
		if (nodeSourceLength(iVisited) == 2)
			print(" && ");
		else
			print(" and ");
		visitNode(iVisited.getSecondNode());
		leaveCall();
		return null;
	}
	
	private ArrayList collectAllArguments(ArgsNode iVisited) {
		ArrayList arguments = new ArrayList();
		if(iVisited.getArgs() != null)
			arguments.addAll(iVisited.getArgs().childNodes());
		if(iVisited.getOptArgs() != null)
			arguments.addAll(iVisited.getOptArgs().childNodes());
		if (iVisited.getRestArg() > 0)
			arguments.add(new ConstNode(null, "*" + config.getLocalVariables().getLocalVariable(iVisited.getRestArg())));
		if (iVisited.getBlockArgNode() != null)
			arguments.add(iVisited.getBlockArgNode());
		return arguments;
	}
	
	private boolean hasNodeCommentsAtEnd(Node n) {
		if(n.getComments() == null)
			return false;
		Iterator it = n.getComments().iterator();
		while(it.hasNext()) {
			Node comment = (Node) it.next();
			if(getStartLine(comment) == getStartLine(n))
				return true;
		}
		return false;
	}
	
	private void printCommentsInArgs(Node n, boolean hasNext) {
		if(hasNodeCommentsAtEnd(n) && hasNext)
			print(",");
		if (printCommentsAfter(n) && hasNext) {
			printNewlineAndIndentation();
		} else 	if (hasNext)
			print(", ");
	}
	
	public Instruction visitArgsNode(ArgsNode iVisited) {

		if (hasArguments(iVisited))
			print(' ');

		Iterator it = collectAllArguments(iVisited).iterator();

		while(it.hasNext()) {
			Node n = (Node) it.next();
			if (n instanceof ArgumentNode) {
				print(((ArgumentNode) n).getName());
				printCommentsInArgs(n, it.hasNext());
			} else if (n.getComments() != null) {
				visitNode(n);
				printCommentsInArgs(n, it.hasNext());
			} else {
				visitNode(n);

				if (it.hasNext())
					print(", ");
			}
		}
		return null;
	}

	public Instruction visitArgsCatNode(ArgsCatNode iVisited) {
		print("[");
		visitAndPrintWithSeparator(iVisited.getFirstNode().childNodes()
				.iterator());
		print(", *");
		visitNode(iVisited.getSecondNode());
		print("]");
		return null;
	}

	public Instruction visitArrayNode(ArrayNode iVisited) {
		print('[');
		enterCall();
		visitAndPrintWithSeparator(iVisited.iterator());
		leaveCall();
		print(']');
		return null;
	}

	public Instruction visitBackRefNode(BackRefNode iVisited) {
		print('$');
		print(iVisited.getType());
		return null;
	}

	public Instruction visitBeginNode(BeginNode iVisited) {
		print("begin");
		visitNodeInIndentation(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	public Instruction visitBignumNode(BignumNode iVisited) {
		print(iVisited.getValue());
		return null;
	}

	public Instruction visitBlockArgNode(BlockArgNode iVisited) {
		print(config.getLocalVariables().getLocalVariable(iVisited.getCount()));
		return null;
	}

	public Instruction visitBlockNode(BlockNode iVisited) {
		visitIter(iVisited.iterator());
		return null;
	}

	private int getLocalVarIndex(Node n) {
		if (n instanceof LocalVarNode) {
			LocalVarNode localVar = (LocalVarNode) n;
			return localVar.getCount();
		}
		return -1;
	}

	public Instruction visitBlockPassNode(BlockPassNode iVisited) {
		config.getLocalVariables().addCharToVariable('&', getLocalVarIndex(iVisited.getBodyNode()));

		//if there are other arguments, we can just add our block to them
		if (iVisited.getArgsNode() instanceof ListNode) {
			((ListNode) iVisited.getArgsNode()).add(
					new LocalVarNode(iVisited.getPosition(), getLocalVarIndex(iVisited.getBodyNode())));
			visitNode(iVisited.getIterNode());
		} else if (iVisited.getArgsNode() == null) {
			visitNode(iVisited.getIterNode());
			if (needsParentheses(iVisited))
				print('(');
			else
				print(" ");
			visitNode(iVisited.getBodyNode());
			if (needsParentheses(iVisited))
				print(')');
		} else {
			//we need to save the argsnode here so we can check later if we need to print a closing parentheses
			config.getArgsNodeWithBlock().add(iVisited.getArgsNode());
			visitNode(iVisited.getIterNode());
			config.getArgsNodeWithBlock().remove(iVisited.getArgsNode());
			print(", ");
			visitNode(iVisited.getBodyNode());
			if (needsParentheses(iVisited.getArgsNode()))
				print(')');
		}

		config.getLocalVariables().removeCharFromVariable('&', getLocalVarIndex(iVisited.getBodyNode()));
		return null;
	}

	public Instruction visitBreakNode(BreakNode iVisited) {
		print("break");
		return null;
	}

	public Instruction visitConstDeclNode(ConstDeclNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Instruction visitClassVarAsgnNode(ClassVarAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Instruction visitClassVarDeclNode(ClassVarDeclNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Instruction visitClassVarNode(ClassVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	private boolean isNumericNode(Node n) {
		return (n != null && (n instanceof FixnumNode || n instanceof BignumNode));
	}

	private boolean isNameAnOperator(String name) {
		return Operators.contain(name);
	}

	private boolean isBlockCalled(CallNode n) {
		return (n.getName().equals("call") && n.getReceiverNode() instanceof LocalVarNode);
	}

	private boolean printSpaceInsteadOfDot(CallNode n) {
		return (isNameAnOperator(n.getName()) && !(n.getArgsNode().childNodes()
				.size() > 1));
	}
	
	protected void printAssignmentOperator(){
		print(" = ");
	}

	private Instruction printIndexAssignment(CallNode iVisited) {
		enterCall();
		visitNode(iVisited.getReceiverNode());
		leaveCall();
		print('[');
		visitNode(firstChild(iVisited.getArgsNode()));
		print("]");
		printAssignmentOperator();
		if (iVisited.getArgsNode().childNodes().size() > 1)
			visitNode((Node) iVisited.getArgsNode().childNodes().get(1));
		return null;
	}

	private Instruction printIndexAccess(CallNode iVisited) {
		enterCall();
		visitNode(iVisited.getReceiverNode());
		leaveCall();
		print('[');
		if (iVisited.getArgsNode() != null)
			visitAndPrintWithSeparator(iVisited.getArgsNode().childNodes().iterator());
		print("]");
		return null;
	}
	
	private Instruction printNegativNumericNode(CallNode iVisited) {
		print('-');
		visitNode(iVisited.getReceiverNode());
		return null;
	}
	
	private boolean isNegativeNumericNode(CallNode iVisited) {
		return isNumericNode(iVisited.getReceiverNode()) && iVisited.getName().equals("-@");
	}
	
	private void printCallReceiverNode(CallNode iVisited) {
		
		if (isBlockCalled(iVisited))
			config.getLocalVariables().removeCharFromVariable('&', ((LocalVarNode) iVisited.getReceiverNode()).getCount());

		if (iVisited.getReceiverNode() instanceof HashNode)
			print('(');

		if (isReceiverACallNode(iVisited) && !printSpaceInsteadOfDot(iVisited)) {
			enterCall();
			visitNewlineInParentheses(iVisited.getReceiverNode());
			leaveCall();
		} else
			visitNewlineInParentheses(iVisited.getReceiverNode());

		if (iVisited.getReceiverNode() instanceof HashNode)
			print(')');
		
		if (isBlockCalled(iVisited))
			config.getLocalVariables().addCharToVariable('&', ((LocalVarNode) iVisited.getReceiverNode()).getCount());
	}
	
	protected boolean inMultipleAssignment() {
		return false;
	}

	public Instruction visitCallNode(CallNode iVisited) {

		if (iVisited.getName().equals("[]=")) {
			return printIndexAssignment(iVisited);
		} else if (iVisited.getName().equals("[]")) {
			return printIndexAccess(iVisited);
		} else if (isNegativeNumericNode(iVisited)) {
			return printNegativNumericNode(iVisited);
		}

		printCallReceiverNode(iVisited);

		if (printSpaceInsteadOfDot(iVisited))
			print(' ');
		else
			print('.');

		if (inMultipleAssignment() && iVisited.getName().endsWith("="))
			print(iVisited.getName().substring(0, iVisited.getName().length() - 1));
		else
			print(iVisited.getName());

		if (isNameAnOperator(iVisited.getName())) {
			if (firstChild(iVisited.getArgsNode()) instanceof NewlineNode)
				print(' ');
			config.getCallDepth().disableCallDepth();
		}
		printCallArguments(iVisited.getArgsNode());

		if (isNameAnOperator(iVisited.getName()))
			config.getCallDepth().enableCallDepth();

		return null;
	}

	public Instruction visitCaseNode(CaseNode iVisited) {
		print("case ");
		visitNode(iVisited.getCaseNode());
		visitNode(iVisited.getFirstWhenNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	private boolean printCommentsIn(Node iVisited) {
		boolean hadComment = false;
		Iterator it = iVisited.getComments().iterator();
		while(it.hasNext()) {
			CommentNode n = (CommentNode) it.next();
			if(getStartLine(n) > getStartLine(iVisited) && getEndLine(n) < getEndLine(iVisited)) {
				hadComment = true;
				visitNode(n);
				printNewlineAndIndentation();
			}
		}
		
		return hadComment;
	}

	public Instruction visitClassNode(ClassNode iVisited) {

		print("class ");
		visitNode(iVisited.getCPath());
		if (iVisited.getSuperNode() != null) {
			print(" < ");
			visitNode(iVisited.getSuperNode());
		}
		visitNode(iVisited.getBodyNode());
		printNewlineAndIndentation();
		
		printCommentsIn(iVisited);
		
		print("end");
		return null;
	}
	
	private boolean printIfComment(Node n) {
		if(n instanceof CommentDecoratorNode) {
			visitIter(((CommentDecoratorNode)n).getComments().iterator());
			return true;
		}
		return false;
	}
	
	private void printIfCommentWithNewline(Node n) {
		if (printIfComment(n))
			printNewlineAndIndentation();
	}

	public Instruction visitColon2Node(Colon2Node iVisited) {
		if (iVisited.getLeftNode() != null) { 
			visitNode(iVisited.getLeftNode());
			print("::");
			printIfCommentWithNewline(iVisited.getLeftNode());
		}
		print(iVisited.getName());
		return null;
	}

	public Instruction visitColon3Node(Colon3Node iVisited) {
		print("::");
		print(iVisited.getName());
		return null;
	}

	public Instruction visitConstNode(ConstNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	public Instruction visitDAsgnNode(DAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Instruction visitDRegxNode(DRegexpNode iVisited) {
		config.getPrintQuotesInString().set(false);
		print(getFirstRegexpEnclosure(iVisited));
		createDRegxReWriteVisitor().visitIter(iVisited.childNodes().iterator());
		print(getSecondRegexpEnclosure(iVisited));
		printRegexpOptions(iVisited.getOptions());
		config.getPrintQuotesInString().revert();
		return null;
	}
	
	private Instruction createHereDocument(DStrNode iVisited) {
		config.getPrintQuotesInString().set(false);
		print("<<-EOF");
		StringWriter writer = new StringWriter();
		PrintWriter oldOut = config.getOutput();
		config.setOutput(new PrintWriter(writer));

		Iterator it = iVisited.childNodes().iterator();
		while (it.hasNext()) {
			createHereDocReWriteVisitor().visitNode((Node) it.next());
			if (it.hasNext())
				config.setSkipNextNewline(true);
		}
		config.setOutput(oldOut);
		config.depositHereDocument(writer.getBuffer().toString());
		config.getPrintQuotesInString().revert();

		return null;
	}

	public Instruction visitDStrNode(DStrNode iVisited) {

		if (firstChild(iVisited) instanceof StrNode	&& stringIsHereDocument((StrNode) firstChild(iVisited))) {
			return createHereDocument(iVisited);
		}

		if (config.getPrintQuotesInString().isTrue())
			print(getSeparatorForStr(iVisited));
		config.getPrintQuotesInString().set(false);
		leaveCall();
		Iterator it = iVisited.childNodes().iterator();
		while (it.hasNext()) {
			Node n = (Node) it.next();
			if (n instanceof ArrayNode)
				visitIter(((ArrayNode) n).childNodes().iterator());
			else
				visitNode(n);
		}
		enterCall();
		config.getPrintQuotesInString().revert();
		if (config.getPrintQuotesInString().isTrue())
			print(getSeparatorForStr(iVisited));
		return null;
	}

	public Instruction visitDSymbolNode(DSymbolNode iVisited) {
		print(':');
		visitNode(iVisited.getNode());
		return null;
	}

	public Instruction visitDVarNode(DVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	public Instruction visitDXStrNode(DXStrNode iVisited) {
		config.getPrintQuotesInString().set(false);
		print("%x{");
		visitIter(iVisited.childNodes().iterator());
		print('}');
		config.getPrintQuotesInString().revert();
		return null;
	}

	public Instruction visitDefinedNode(DefinedNode iVisited) {
		print("defined? ");
		enterCall();
		visitNode(iVisited.getExpressionNode());
		leaveCall();
		return null;
	}

	private boolean hasArguments(Node n) {
		if (n instanceof ArgsNode) {
			ArgsNode args = (ArgsNode) n;
			return (args.getArgs() != null || args.getOptArgs() != null
					|| args.getBlockArgNode() != null || args.getRestArg() > 0);
		}
		return true;
	}

	private int getVarIndexOfBlockArgumentFromArgs(Node n) {
		if (n != null && n instanceof ArgsNode
				&& ((ArgsNode) n).getBlockArgNode() != null) {
			return ((ArgsNode) n).getBlockArgNode().getCount();
		}
		return -1;
	}
	
	protected void printCommentsAtEnd(Node n) {
		Iterator it = n.getComments().iterator();
		while(it.hasNext()) {
			Node comment = (Node) it.next();
			if(getStartLine(n) == getStartLine(comment)) {
				print(' ');
				visitNode(comment);
			}
		}
	}
	
	private void printDefNode(Node parent, String name, Node args, ScopeNode body) {
		print(name);
		printCommentsAtEnd(parent);
		config.getLocalVariables().addLocalVariable(body);
		config.getLocalVariables().addCharToVariable('&', getVarIndexOfBlockArgumentFromArgs(args));
		visitNode(args);
		config.getLocalVariables().removeCharFromVariable('&', getVarIndexOfBlockArgumentFromArgs(args));
		visitNode(body);
		printNewlineAndIndentation();
		printCommentsIn(parent);
		print("end");
	}

	public Instruction visitDefnNode(DefnNode iVisited) {
		
		print("def ");
		printDefNode(iVisited, iVisited.getName(), iVisited.getArgsNode(), iVisited.getBodyNode());
		return null;
	}

	public Instruction visitDefsNode(DefsNode iVisited) {
		print("def ");
		visitNode(iVisited.getReceiverNode());
		print('.');
		printDefNode(iVisited, iVisited.getName(), iVisited.getArgsNode(), iVisited.getBodyNode());
		return null;
	}

	public Instruction visitDotNode(DotNode iVisited) {
		enterCall();
		visitNode(iVisited.getBeginNode());
		print("..");
		if (iVisited.isExclusive())
			print('.');
		visitNode(iVisited.getEndNode());
		leaveCall();
		return null;
	}

	public Instruction visitEnsureNode(EnsureNode iVisited) {
		visitNode(iVisited.getBodyNode());
		config.getIndentor().outdent();
		printNewlineAndIndentation();
		print("ensure");
		visitNodeInIndentation(iVisited.getEnsureNode());
		config.getIndentor().indent();
		return null;
	}

	public Instruction visitEvStrNode(EvStrNode iVisited) {
		print('#');
		if (!(iVisited.getBody() instanceof NthRefNode))
			print('{');
		config.getPrintQuotesInString().set(true);
		visitNode(iVisited.getBody());
		config.getPrintQuotesInString().revert();
		if (!(iVisited.getBody() instanceof NthRefNode))
			print('}');
		return null;
	}

	public Instruction visitFCallNode(FCallNode iVisited) {
		print(iVisited.getName());
		printCallArguments(iVisited.getArgsNode());
		return null;
	}

	public Instruction visitFalseNode(FalseNode iVisited) {
		print("false");
		return null;
	}

	public Instruction visitFixnumNode(FixnumNode iVisited) {
		print(iVisited.getValue());
		return null;
	}

	public Instruction visitFlipNode(FlipNode iVisited) {
		enterCall();
		visitNode(iVisited.getBeginNode());
		print(" ..");
		if (iVisited.isExclusive())
			print('.');
		print(' ');
		visitNode(iVisited.getEndNode());
		leaveCall();
		return null;
	}

	public Instruction visitFloatNode(FloatNode iVisited) {
		print(iVisited.getValue());
		return null;
	}

	public Instruction visitForNode(ForNode iVisited) {
		print("for ");
		visitNode(iVisited.getVarNode());
		print(" in ");
		visitNode(iVisited.getIterNode());
		visitNodeInIndentation(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	public Instruction visitGlobalAsgnNode(GlobalAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Instruction visitGlobalVarNode(GlobalVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	private void printHashNodeContent(HashNode iVisited) {
		if (iVisited.getListNode() != null) {
			Iterator it = iVisited.getListNode().childNodes().iterator();
			while (it.hasNext()) {
				visitNode((Node) it.next());
				print(" => ");
				visitNode((Node) it.next());
				if (it.hasNext())
					print(", ");
			}
		}
	}

	public Instruction visitHashNode(HashNode iVisited) {
		print('{');
		printHashNodeContent(iVisited);
		print('}');
		return null;
	}

	private void printAsgnNode(AssignableNode n) {
		print(((INameNode) n).getName());
		if (n.getValueNode() == null)
			return;
		print(" = ");
		visitNewlineInParentheses(n.getValueNode());
	}

	public Instruction visitInstAsgnNode(InstAsgnNode iVisited) {
		printAsgnNode(iVisited);
		return null;
	}

	public Instruction visitInstVarNode(InstVarNode iVisited) {
		print(iVisited.getName());
		return null;
	}

	/**
	 * Elsif-conditions in the AST are represented by multiple nested if / else
	 * combinations. This method takes a node and checks if the node is an
	 * elsif-statement or a normal else node.
	 * 
	 * @param iVisited
	 * @return Returns the last ElseNode or null.
	 */
	private Node printElsIfNodes(Node iVisited) {
		if (iVisited != null && iVisited instanceof IfNode) {
			IfNode n = (IfNode) iVisited;
			printNewlineAndIndentation();
			print("elsif ");
			visitNode(n.getCondition());
			visitNodeInIndentation(n.getThenBody());
			return printElsIfNodes(n.getElseBody());
		} else if (iVisited != null)
			return iVisited;
		else
			return null;
	}

	private Instruction printShortIfStatement(IfNode n) {
		if (n.getThenBody() == null) {
			visitNode(n.getElseBody());
			print(" unless ");
			visitNode(n.getCondition());
		} else {
			enterCall();
			createShortIfNodeReWriteVisitor().visitNode(n.getCondition());
			print(" ? ");
			createShortIfNodeReWriteVisitor().visitNode(n.getThenBody());
			print(" : ");
			createShortIfNodeReWriteVisitor().visitNewlineInParentheses(n.getElseBody());
			leaveCall();
		}
		return null;
	}

	private boolean isAssignment(Node n) {
		return (n instanceof DAsgnNode || n instanceof GlobalAsgnNode
				|| n instanceof InstAsgnNode || n instanceof LocalAsgnNode || n instanceof ClassVarAsgnNode);
	}

	private boolean sourceSubStringEquals(int offset, int length, String str) {
		return config.getSource().substring(offset, offset + length).equals(str);
	}
	
	private boolean isShortIfStatement(IfNode iVisited) {
		return (isOnSingleLine(iVisited.getCondition(), iVisited.getElseBody())
				&& !(iVisited.getElseBody() instanceof IfNode)
				&& !sourceSubStringEquals(getStartOffset(iVisited), 2, "if"));
	}

	public Instruction visitIfNode(IfNode iVisited) {

		if (isShortIfStatement(iVisited)) {
			return printShortIfStatement(iVisited);
		}

		print("if ");

		if (isAssignment(iVisited.getCondition()))
			enterCall();

		// We have to skip a possible Newline here:
		visitNewlineInParentheses(iVisited.getCondition());
		if (isAssignment(iVisited.getCondition()))
			leaveCall();

		config.getIndentor().indent();
		// we have to check this to generate valid code for this style: "return
		// if true", because there is no newline
		if (!isStartOnNewLine(iVisited.getCondition(), iVisited.getThenBody())
				&& iVisited.getThenBody() != null)
			printNewlineAndIndentation();
		else if (!(iVisited.getThenBody() instanceof NewlineNode)
				&& iVisited.getThenBody() != null
				&& !(iVisited.getThenBody() instanceof BlockNode))
			printNewlineAndIndentation();

		visitNode(iVisited.getThenBody());
		config.getIndentor().outdent();
		Node elseNode = printElsIfNodes(iVisited.getElseBody());

		if (elseNode != null) {
			printNewlineAndIndentation();
			print("else");
			config.getIndentor().indent();
			if (!(elseNode instanceof NewlineNode))
				printNewlineAndIndentation();
			visitNode(elseNode);
			config.getIndentor().outdent();
		}
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	private boolean isOnSingleLine(Node n) {
		return isOnSingleLine(n, n);
	}

	private boolean isOnSingleLine(Node n1, Node n2) {
		if (n1 == null || n2 == null)
			return false;
		return (getStartLine(n1) == getEndLine(n2));
	}

	private void printIterVarNode(IterNode n) {
		if (n.getVarNode() != null) {
			print('|');
			visitNode(n.getVarNode());
			print("|");
		}
	}

	public Instruction visitIterNode(IterNode iVisited) {
		enterCall();
		visitNode(iVisited.getIterNode());
		leaveCall();
		if (isOnSingleLine(iVisited)) {
			print(" {");
			printIterVarNode(iVisited);
			print(' ');

			if (iVisited.getBodyNode() instanceof NewlineNode)
				visitNode(((NewlineNode) iVisited.getBodyNode()).getNextNode());
			else
				visitNode(iVisited.getBodyNode());
			print('}');
		} else {
			print(" do ");
			printIterVarNode(iVisited);
			visitNodeInIndentation(iVisited.getBodyNode());
			printNewlineAndIndentation();
			print("end");
		}
		return null;
	}

	public Instruction visitLocalAsgnNode(LocalAsgnNode iVisited) {
		config.getLocalVariables().addLocalVariable(iVisited.getCount(), iVisited.getName());
		printAsgnNode(iVisited);
		return null;
	}

	public Instruction visitLocalVarNode(LocalVarNode iVisited) {
		print(config.getLocalVariables().getLocalVariable(iVisited.getCount()));
		return null;
	}

	public Instruction visitMultipleAsgnNode(MultipleAsgnNode iVisited) {
		if (iVisited.getHeadNode() != null)
			createMultipleAssignmentReWriteVisitor().visitAndPrintWithSeparator(iVisited.getHeadNode().childNodes().iterator());
		if (iVisited.getValueNode() == null) {
			visitNode(iVisited.getArgsNode());
			return null;
		}
		print(" = ");
		enterCall();
		if (iVisited.getValueNode() instanceof ArrayNode)
			visitAndPrintWithSeparator(iVisited.getValueNode().childNodes().iterator());
		else
			visitNode(iVisited.getValueNode());
		leaveCall();
		return null;
	}

	public Instruction visitMatch2Node(Match2Node iVisited) {
		visitNode(iVisited.getReceiverNode());
		print(" =~ ");
		enterCall();
		visitNode(iVisited.getValueNode());
		leaveCall();
		return null;
	}

	public Instruction visitMatch3Node(Match3Node iVisited) {
		visitNode(iVisited.getValueNode());
		print(" =~ ");
		visitNode(iVisited.getReceiverNode());
		return null;
	}

	public Instruction visitMatchNode(MatchNode iVisited) {
		visitNode(iVisited.getRegexpNode());
		return null;
	}

	public Instruction visitModuleNode(ModuleNode iVisited) {
		print("module ");
		visitNode(iVisited.getCPath());
		visitNode(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}
	
	public Instruction visitNewlineNode(NewlineNode iVisited) {

		boolean skippedLastNewLine = config.isSkipNextNewline();
		if (skippedLastNewLine)
			config.setSkipNextNewline(false);

		if (iVisited.getNextNode() instanceof SplatNode) {
			print("[");
			visitNode(iVisited.getNextNode());
			print("]");
		} else if (config.hasHereDocument()) {
			printHereDocument();
			printNewlineAndIndentation();
			visitNode(iVisited.getNextNode());
		} else if (!skippedLastNewLine) {
			printNewlineAndIndentation();
			visitNode(iVisited.getNextNode());
		} else
			visitNode(iVisited.getNextNode());
		return null;
	}

	public Instruction visitNextNode(NextNode iVisited) {
		print("next");
		return null;
	}

	public Instruction visitNilNode(NilNode iVisited) {
		print("nil");
		return null;
	}

	public Instruction visitNotNode(NotNode iVisited) {
		if (iVisited.getConditionNode() instanceof CallNode)
			enterCall();
		if (nodeSourceLength(iVisited) == 3)
			print("not ");
		else
			print("!");

		visitNewlineInParentheses(iVisited.getConditionNode());

		if (iVisited.getConditionNode() instanceof CallNode)
			leaveCall();
		return null;
	}

	public Instruction visitNthRefNode(NthRefNode iVisited) {
		print('$');
		print(iVisited.getMatchNumber());
		return null;
	}

	private boolean isSimpleNode(Node n) {
		return (n instanceof LocalVarNode || n instanceof AssignableNode
				|| n instanceof InstVarNode || n instanceof ClassVarNode
				|| n instanceof GlobalVarNode || n instanceof ConstDeclNode
				|| n instanceof VCallNode || isNumericNode(n));
	}

	public Instruction visitOpElementAsgnNode(OpElementAsgnNode iVisited) {

		if (!isSimpleNode(iVisited.getReceiverNode()))
			visitNewlineInParentheses(iVisited.getReceiverNode());
		else
			visitNode(iVisited.getReceiverNode());

		visitNode(iVisited.getArgsNode());
		print(' ');
		print(iVisited.getOperatorName());
		print("= ");
		visitNode(iVisited.getValueNode());
		return null;
	}

	public Instruction visitOpAsgnNode(OpAsgnNode iVisited) {
		visitNode(iVisited.getReceiverNode());
		print('.');
		print(iVisited.getVariableName());
		print(' ');
		print(iVisited.getOperatorName());
		print("= ");
		visitNode(iVisited.getValueNode());
		return null;
	}

	private void printOpAsgnNode(Node n, String operator) {
		enterCall();
		if (n instanceof LocalAsgnNode) {
			LocalAsgnNode asgnNode = (LocalAsgnNode) n;
			print(asgnNode.getName());
			print(operator);
			visitNode(asgnNode.getValueNode());
		} else if (n instanceof InstAsgnNode) {
			InstAsgnNode asgnNode = (InstAsgnNode) n;
			print(asgnNode.getName());
			print(operator);
			visitNode(asgnNode.getValueNode());
		}
		leaveCall();
	}

	public Instruction visitOpAsgnAndNode(OpAsgnAndNode iVisited) {
		printOpAsgnNode(iVisited.getSecondNode(), " &&= ");
		return null;
	}

	public Instruction visitOpAsgnOrNode(OpAsgnOrNode iVisited) {
		printOpAsgnNode(iVisited.getSecondNode(), " ||= ");
		return null;
	}

	public Instruction visitOptNNode(OptNNode iVisited) {
		// this node is never used in the parser, only here:
		// org.jruby/src/org/jruby/Main.java
		notImplemented(iVisited);
		return null;
	}

	public Instruction visitOrNode(OrNode iVisited) {
		enterCall();
		visitNode(iVisited.getFirstNode());
		leaveCall();
		if (sourceRangeEquals(iVisited.getPosition(), "||"))
			print(" || ");
		else
			print(" or ");
		enterCall();
		visitNewlineInParentheses(iVisited.getSecondNode());
		leaveCall();
		return null;
	}

	public Instruction visitPostExeNode(PostExeNode iVisited) {
		// this node contains nothing but an empty list, so we don't have to
		// process anything
		notImplemented(iVisited);
		return null;
	}

	public Instruction visitRedoNode(RedoNode iVisited) {
		print("redo");
		return null;
	}

	private String getFirstRegexpEnclosure(Node n) {
		if (config.getSource().length() < getStartOffset(n))
			return "/";
		else if (getStartOffset(n) >= 2
				&& config.getSource().charAt(getStartOffset(n) - 2) == '%')
			return "%r(";
		else
			return "/";
	}

	private String getSecondRegexpEnclosure(Node n) {
		if (config.getSource().length() < getStartOffset(n))
			return "/";
		else if (getStartOffset(n) >= 2
				&& config.getSource().charAt(getStartOffset(n) - 2) == '%')
			return ")";
		else
			return "/";
	}

	private void printRegexpOptions(int o) {
		if ((o & 1) == 1)
			print('i');
		if ((o & 2) == 2)
			print('x');
		if ((o & 4) == 4)
			print('m');
	}

	public Instruction visitRegexpNode(RegexpNode iVisited) {

		print(getFirstRegexpEnclosure(iVisited));
		print(iVisited.getValue());
		print(getSecondRegexpEnclosure(iVisited));

		printRegexpOptions(iVisited.getOptions());

		return null;
	}

	private Node firstChild(Node n) {
		if (n.childNodes() == null)
			return null;
		else if (n.childNodes().size() <= 0)
			return null;
		return (Node) n.childNodes().get(0);
	}

	public Instruction visitRescueBodyNode(RescueBodyNode iVisited) {
		if (config.getLastPosition().getStartLine() == getEndLine(iVisited.getBodyNode())) {
			print(" rescue ");
		} else {
			print("rescue");
		}

		if (iVisited.getExceptionNodes() != null) {
			printExceptionNode(iVisited);
		} else {
			visitNodeInIndentation(iVisited.getBodyNode());
		}
		if (iVisited.getOptRescueNode() != null)
			printNewlineAndIndentation();
		visitNode(iVisited.getOptRescueNode());
		return null;
	}

	private void printExceptionNode(RescueBodyNode n) {
		if (n.getExceptionNodes() == null)
			return;

		print(' ');
		visitNode(firstChild(n.getExceptionNodes()));

		Node firstBodyNode = n.getBodyNode();
		if (n.getBodyNode() instanceof BlockNode)
			firstBodyNode = firstChild(n.getBodyNode());

		// if the exception is assigned to a variable, we have to skip the first
		// node in the body
		if (firstBodyNode instanceof AssignableNode) {
			print(" => ");
			print(((INameNode) firstBodyNode).getName());
			if (firstBodyNode instanceof LocalAsgnNode)
				config.getLocalVariables().addLocalVariable(((LocalAsgnNode) firstBodyNode).getCount(),
						((LocalAsgnNode) firstBodyNode).getName());

			config.getIndentor().indent();
			visitIterAndSkipFirst(n.getBodyNode().childNodes().iterator());
			config.getIndentor().outdent();
		} else
			visitNodeInIndentation(n.getBodyNode());
	}

	public Instruction visitRescueNode(RescueNode iVisited) {
		visitNode(iVisited.getBodyNode());
		config.getIndentor().outdent();

		if (iVisited.getRescueNode().getBodyNode() != null
				&& getStartLine(iVisited) != getEndLine(iVisited
						.getRescueNode().getBodyNode()))
			printNewlineAndIndentation();

		if (iVisited.getRescueNode().getBodyNode() == null) {
			printNewlineAndIndentation();
			print("rescue");
			printExceptionNode(iVisited.getRescueNode());
		} else {
			visitNode(iVisited.getRescueNode());
		}
		if (iVisited.getElseNode() != null) {
			printNewlineAndIndentation();
			print("else");
			visitNodeInIndentation(iVisited.getElseNode());
		}
		config.getIndentor().indent();
		return null;
	}

	public Instruction visitRetryNode(RetryNode iVisited) {
		print("retry");
		return null;
	}

	private Node unwrapSingleArrayNode(Node n) {
		if (!(n instanceof ArrayNode))
			return n;
		else if (((ArrayNode) n).childNodes().size() > 1)
			return n;
		return firstChild((ArrayNode) n);
	}

	public Instruction visitReturnNode(ReturnNode iVisited) {
		print("return");
		enterCall();
		if (iVisited.getValueNode() != null) {
			print(' ');
			visitNode(unwrapSingleArrayNode(iVisited.getValueNode()));
		}
		leaveCall();
		return null;
	}

	public Instruction visitSClassNode(SClassNode iVisited) {
		print("class << ");
		visitNode(iVisited.getReceiverNode());
		visitNode(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	public Instruction visitScopeNode(ScopeNode iVisited) {
		visitNodeInIndentation(iVisited.getBodyNode());
		return null;
	}

	public Instruction visitSelfNode(SelfNode iVisited) {
		print("self");
		return null;
	}

	public Instruction visitSplatNode(SplatNode iVisited) {
		print("*");
		visitNode(iVisited.getValue());
		return null;
	}

	private boolean stringIsHereDocument(StrNode n) {
		return sourceRangeEquals(getStartOffset(n), getStartOffset(n) + 2, "<<");
	}

	protected char getSeparatorForStr(Node n) {
		char separator = '"';
		if (config.getSource().length() >= getStartOffset(n)
				&& config.getSource().charAt(getStartOffset(n)) == '\'')
			separator = '\'';
		return separator;
	}
	
	protected boolean inDRegxNode() {
		return false;
	}

	public Instruction visitStrNode(StrNode iVisited) {
		// look for a here-document:
		if (stringIsHereDocument(iVisited)) {
			print("<<-EOF");
			config.depositHereDocument(iVisited.getValue());
			return null;
		}

		// don't print quotes if we are a subpart of an other here-document
		if (config.getPrintQuotesInString().isTrue())
			print(getSeparatorForStr(iVisited));

		if (inDRegxNode())
			print(iVisited.getValue());
		else {
			Matcher matcher = Pattern.compile("([\\\\\\n\\f\\r\\t\\\"\\\'])").matcher(iVisited.getValue());

			if (matcher.find()) {
				String unescChar = unescapeChar(matcher.group(1).charAt(0));
				print(matcher.replaceAll("\\\\" + unescChar));
			} else {
				print(iVisited.getValue());
			}
		}
		if (config.getPrintQuotesInString().isTrue())
			print(getSeparatorForStr(iVisited));

		return null;
	}

	protected String unescapeChar(char escapedChar) {
		switch (escapedChar) {
		case '\n':
			return "n";
		case '\f':
			return "f";
		case '\r':
			return "r";
		case '\t':
			return "t";
		case '\"':
			return "\"";
		case '\'':
			return "'";
		case '\\':
			return "\\\\";
		default:
			return null;
		}
	}

	private boolean needsSuperNodeParentheses(SuperNode n) {
		return n.getArgsNode() == null
				&& config.getSource().charAt(getEndOffset(n) + 1) == '(';
	}

	public Instruction visitSuperNode(SuperNode iVisited) {
		print("super");
		if (needsSuperNodeParentheses(iVisited))
			print('(');
		printCallArguments(iVisited.getArgsNode());
		if (needsSuperNodeParentheses(iVisited))
			print(')');
		return null;
	}

	public Instruction visitSValueNode(SValueNode iVisited) {
		visitNode(iVisited.getValue());
		return null;
	}

	public Instruction visitSymbolNode(SymbolNode iVisited) {
		print(':');
		print(iVisited.getName());
		return null;
	}

	public Instruction visitToAryNode(ToAryNode iVisited) {
		visitNode(iVisited.getValue());
		return null;
	}

	public Instruction visitTrueNode(TrueNode iVisited) {
		print("true");
		return null;
	}

	public Instruction visitUndefNode(UndefNode iVisited) {
		print("undef ");
		print(iVisited.getName());
		return null;
	}

	public Instruction visitUntilNode(UntilNode iVisited) {
		print("until ");
		visitNode(iVisited.getConditionNode());
		visitNodeInIndentation(iVisited.getBodyNode());
		printNewlineAndIndentation();
		print("end");
		return null;
	}

	public Instruction visitVAliasNode(VAliasNode iVisited) {
		print("alias ");
		print(iVisited.getNewName());
		print(' ');
		print(iVisited.getOldName());
		return null;
	}

	public Instruction visitVCallNode(VCallNode iVisited) {
		print(iVisited.getMethodName());
		return null;
	}

	private void visitNodeInIndentation(Node n) {
		config.getIndentor().indent();
		visitNode(n);
		config.getIndentor().outdent();
	}

	public Instruction visitWhenNode(WhenNode iVisited) {
		printNewlineAndIndentation();
		print("when ");
		enterCall();
		visitAndPrintWithSeparator(iVisited.getExpressionNodes().childNodes()
				.iterator());
		leaveCall();
		visitNodeInIndentation(iVisited.getBodyNode());
		if (!(iVisited.getNextCase() instanceof WhenNode || iVisited
				.getNextCase() == null)) {
			printNewlineAndIndentation();
			print("else");
			visitNodeInIndentation(iVisited.getNextCase());
		} else
			visitNode(iVisited.getNextCase());
		return null;
	}

	protected void visitNewlineInParentheses(Node n) {
		if (n instanceof NewlineNode) {
			if (((NewlineNode) n).getNextNode() instanceof SplatNode) {
				print('[');
				visitNode(((NewlineNode) n).getNextNode());
				print(']');
			} else {
				print('(');
				visitNode(((NewlineNode) n).getNextNode());
				print(')');
			}
		} else if (n instanceof BlockNode) {
			print('(');
			config.setSkipNextNewline(true);
			visitIter(n.childNodes().iterator());
			print(')');
		} else
			visitNode(n);
	}

	public Instruction visitWhileNode(WhileNode iVisited) {
		if (iVisited.evaluateAtStart()) {
			print("while ");
			if (isAssignment(iVisited.getConditionNode()))
				enterCall();
			visitNewlineInParentheses(iVisited.getConditionNode());
			if (isAssignment(iVisited.getConditionNode()))
				leaveCall();
			config.getIndentor().indent();
			if (!(iVisited.getBodyNode() instanceof NewlineNode || iVisited
					.getBodyNode() instanceof BlockNode))
				printNewlineAndIndentation();
			visitNode(iVisited.getBodyNode());
			config.getIndentor().outdent();
			printNewlineAndIndentation();
			print("end");
		} else {
			print("begin");
			visitNodeInIndentation(iVisited.getBodyNode());
			printNewlineAndIndentation();
			print("end while ");
			visitNode(iVisited.getConditionNode());
		}
		return null;
	}

	public Instruction visitXStrNode(XStrNode iVisited) {
		print('`');
		print(iVisited.getValue());
		print('`');
		return null;
	}

	public Instruction visitYieldNode(YieldNode iVisited) {
		print("yield");
		
		if (iVisited.getArgsNode() != null) {
			if (needsParentheses(iVisited.getArgsNode()))
				print('(');
			else
				print(' ');

			enterCall();

			if (iVisited.getArgsNode() instanceof ArrayNode)
				visitAndPrintWithSeparator(iVisited.getArgsNode().childNodes().iterator());
			else
				visitNode(iVisited.getArgsNode());

			leaveCall();

			if (needsParentheses(iVisited.getArgsNode()))
				print(')');
		}
		return null;
	}

	public Instruction visitZArrayNode(ZArrayNode iVisited) {
		print("[]");
		return null;
	}

	public Instruction visitZSuperNode(ZSuperNode iVisited) {
		print("super");
		return null;
	}

	public Instruction visitCommentNode(CommentNode iVisited) {
		print(iVisited.getCommentValue());
		return null;
	}

	private int getStartLine(Node n) {
		return n.getPosition().getStartLine();
	}

	private int getStartOffset(Node n) {
		return n.getPosition().getStartOffset();
	}

	private int getEndLine(Node n) {
		return n.getPosition().getEndLine();
	}

	protected int getEndOffset(Node n) {
		return n.getPosition().getEndOffset();
	}
}
