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
 * Copyright (C) 2006 Thomas Corbat <tcorbat@hsr.ch>
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
package org.jruby.lexer.yacc;

import org.jruby.common.IRubyWarnings;
import org.jruby.parser.ParserSupport;

public class LexerContext implements Cloneable{

	private boolean inString;
	private boolean inRegExp;
	
    // Last token read via yylex().
    private int token;
    
    // Value of last token which had a value associated with it.
    private Token yaccValue;

    // Stream of data that yylex() examines.
    private LexerSource src;
    
    // Used for tiny smidgen of grammar in lexer (see setParserSupport())
    private ParserSupport parserSupport = null;

    // The current location of the lexer immediately after a call to yylex()
    private ISourcePosition currentPos;

    // What handles warnings
    private IRubyWarnings warnings;

    // Additional context surrounding tokens that both the lexer and
    // grammar use.
    private LexState lex_state;
    
    // Tempory buffer to build up a potential token.  Consumer takes responsibility to reset 
    // this before use.
    private StringBuffer tokenBuffer = new StringBuffer(60);

    private StackState conditionState = new StackState();
    private StackState cmdArgumentState = new StackState();
    private StrTerm lex_strterm;
    private boolean commandStart;

    private boolean lastWasNewLine = false;
	private boolean currentIsNewLine = true;
	
	
	public final Object clone()
	{
		LexerContext clone = new LexerContext();
		
		clone.setToken(token);
		clone.setYaccValue(yaccValue);
		clone.setSrc(src);
		clone.setParserSupportEncap(parserSupport);
		clone.setCurrentPos(currentPos);
		clone.setWarningsEncap(warnings);
		clone.setLex_state(lex_state);
		clone.setTokenBuffer(new StringBuffer(tokenBuffer.toString()));
		clone.setConditionStateEncap(conditionState.getClone());
		clone.setCmdArgumentStateEncap(cmdArgumentState.getClone());
		if(lex_strterm != null) {
			clone.setLex_strterm((StrTerm)lex_strterm.clone());
		}
		clone.setCommandStart(commandStart);
		clone.setLastWasNewLine(lastWasNewLine);
		clone.setCurrentIsNewLine(currentIsNewLine);
		clone.setInString(inString);
		clone.setInRegExp(inRegExp);
		
		return clone;
	}
	
	
	public int setToken(int token) {
		return this.token = token;
	}

	public int getToken() {
		return token;
	}

	public void setYaccValue(Token yaccValue) {
		this.yaccValue = yaccValue;
	}

	public Token getYaccValue() {
		return yaccValue;
	}

	public void setSrc(LexerSource src) {
		this.src = src;
	}

	public LexerSource getSrc() {
		return src;
	}

	public void setParserSupportEncap(ParserSupport parserSupport) {
		this.parserSupport = parserSupport;
	}

	public ParserSupport getParserSupportEncap() {
		return parserSupport;
	}

	public void setCurrentPos(ISourcePosition currentPos) {
		this.currentPos = currentPos;
	}

	public ISourcePosition getCurrentPos() {
		return currentPos;
	}

	public void setWarningsEncap(IRubyWarnings warnings) {
		this.warnings = warnings;
	}

	public IRubyWarnings getWarningsEncap() {
		return warnings;
	}

	public void setLex_state(LexState lex_state) {
		this.lex_state = lex_state;
	}

	public LexState getLex_state() {
		return lex_state;
	}

	public void setTokenBuffer(StringBuffer tokenBuffer) {
		this.tokenBuffer = tokenBuffer;
	}

	public StringBuffer getTokenBuffer() {
		return tokenBuffer;
	}

	public void setConditionStateEncap(StackState conditionState) {
		this.conditionState = conditionState;
	}

	public StackState getConditionStateEncap() {
		return conditionState;
	}

	public void setCmdArgumentStateEncap(StackState cmdArgumentState) {
		this.cmdArgumentState = cmdArgumentState;
	}

	public StackState getCmdArgumentStateEncap() {
		return cmdArgumentState;
	}

	public void setLex_strterm(StrTerm lex_strterm) {
		this.lex_strterm = lex_strterm;
	}

	public StrTerm getLex_strterm() {
		return lex_strterm;
	}

	public void setCommandStart(boolean commandStart) {
		this.commandStart = commandStart;
	}

	public boolean isCommandStart() {
		return commandStart;
	}

	public void setLastWasNewLine(boolean lastWasNewLine) {
		this.lastWasNewLine = lastWasNewLine;
	}

	public boolean isLastWasNewLine() {
		return lastWasNewLine;
	}

	public void setCurrentIsNewLine(boolean currentIsNewLine) {
		this.currentIsNewLine = currentIsNewLine;
	}

	public boolean isCurrentIsNewLine() {
		return currentIsNewLine;
	}


	public boolean isInRegExp() {
		return inRegExp;
	}


	public void setInRegExp(boolean inRegExp) {
		this.inRegExp = inRegExp;
	}


	public boolean isInString() {
		return inString;
	}


	public void setInString(boolean inString) {
		this.inString = inString;
	}
}
