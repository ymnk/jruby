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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004-2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Zach Dennis <zdennis@mktec.com>
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

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;

import org.jruby.ast.BackRefNode;
import org.jruby.ast.CommentNode;
import org.jruby.ast.NthRefNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.parser.BlockNamesElement;
import org.jruby.parser.LocalNamesElement;
import org.jruby.parser.ParserSupport;
import org.jruby.parser.Tokens;
import org.jruby.util.IdUtil;
import org.jruby.util.PrintfFormat;

/** This is a port of the MRI lexer to Java it is compatible to Ruby 1.8.1.
 */
public class RubyYaccLexer {
	private LexerContext context = new LexerContext();

    // Give a name to a value.  Enebo: This should be used more.
    static final int EOF = 0;

    // ruby constants for strings (should this be moved somewhere else?)
    static final int STR_FUNC_ESCAPE=0x01;
    static final int STR_FUNC_EXPAND=0x02;
    static final int STR_FUNC_REGEXP=0x04;
    static final int STR_FUNC_QWORDS=0x08;
    static final int STR_FUNC_SYMBOL=0x10;
    static final int STR_FUNC_INDENT=0x20;

    private static final int str_squote = 0;
    private static final int str_dquote = STR_FUNC_EXPAND;
    private static final int str_xquote = STR_FUNC_EXPAND;
    private static final int str_regexp = 
        STR_FUNC_REGEXP|STR_FUNC_ESCAPE|STR_FUNC_EXPAND;
    //private final int str_sword  = STR_FUNC_QWORDS;
    //private final int str_dword  = STR_FUNC_QWORDS|STR_FUNC_EXPAND;
    private static final int str_ssym   = STR_FUNC_SYMBOL;
    private static final int str_dsym   = STR_FUNC_SYMBOL|STR_FUNC_EXPAND;


	private LexerContext savedContext;
    
    public RubyYaccLexer() {
    	reset();
    }
    
    public void reset() {
		context.setToken(0);
		context.setYaccValue(null);
		context.setSrc(null);
		context.setLex_state(null);
		resetStacks();
		context.setLex_strterm(null);
		context.setCommandStart(true);
		context.setLastWasNewLine(false);
		context.setCurrentIsNewLine(true);
	}
    
    /**
	 * How the parser advances to the next token.
	 * 
	 * @return true if not at end of file (EOF).
	 */
    public boolean advance() {

		context.setToken(yylex());
    	
		saveContext();
		int nextToken = context.setToken(yylex());

		if (isTailComment(nextToken)) {
			CommentNode comment = new CommentNode(context.getYaccValue().getPosition(), context.getYaccValue().getValue().toString());
			context.setToken(savedContext.getToken());
			context.setYaccValue(savedContext.getYaccValue());
			context.getYaccValue().addComment(comment);
		} else if (savedContext.getToken() == Tokens.tSOLOCOMMENT) {
			ArrayList comments = new ArrayList();

			CommentNode comment = new CommentNode(savedContext.getYaccValue().getPosition(), savedContext.getYaccValue().getValue().toString());
			comments.add(comment);

			while (nextToken == Tokens.tSOLOCOMMENT) {
				comment = new CommentNode(context.getYaccValue().getPosition(),	context.getYaccValue().getValue().toString());
				comments.add(comment);
				nextToken = context.setToken(yylex());
			}

			context.getYaccValue().addComments(comments);
			if (nextToken == EOF) {
				context.setToken(Tokens.tEOF_COMMENT);
				return true;
			}

			saveContext();
			nextToken = context.setToken(yylex());
			if (nextToken == Tokens.tTAILCOMMENT) {
				comment = new CommentNode(context.getYaccValue().getPosition(),	context.getYaccValue().getValue().toString());
				savedContext.getYaccValue().addComment(comment);

				context.setYaccValue(savedContext.getYaccValue());
				context.setToken(savedContext.getToken());

			} else {
				restoreContext();
			}

		} else {
			restoreContext();
		}

		setInStringFlag();
		setInRegExpFlag();

		return (context.getToken()) != EOF;
	}

	private boolean isTailComment(int nextToken) {
		return (!context.isInString() || savedContext.getToken() == Tokens.tSTRING_END)
				&& (!context.isInRegExp() || savedContext.getToken() == Tokens.tREGEXP_END)
				&& nextToken == Tokens.tTAILCOMMENT;
	}

	private void setInRegExpFlag() {
		if(context.getToken() == Tokens.tREGEXP_BEG){ 
			context.setInRegExp(true);
		}
		else if(context.getToken() == Tokens.tREGEXP_END) {
			context.setInRegExp(false);
		}
	}

	private void setInStringFlag() {
		if(context.getToken() == Tokens.tSTRING_BEG) {
			context.setInString(true);
		}
		else if(context.getToken() == Tokens.tSTRING_END) {
			context.setInString(false);
		}
	}

	private void restoreContext() {
		context = savedContext;
		try {
			context.getSrc().resetSource();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveContext() {
		
		try {
			context.getSrc().markSource();
		} catch (IOException e) {
			e.printStackTrace();
		}
		savedContext =(LexerContext) context.clone();
	}
    
    /**
     * Last token read from the lexer at the end of a call to yylex()
     * 
     * @return last token read
     */
    public int token() {
        return context.getToken();
    }

    /**
     * Value of last token (if it is a token which has a value).
     * 
     * @return value of last value-laden token
     */
    public Object value() {
        return context.getYaccValue();
    }

    /**
     * Position from where last token came from.
     * 
     * @return the last tokens position from the source
     */
    public ISourcePosition getPosition() {
        return context.getCurrentPos();
    }
    
    public ISourcePosition getDummyPosition() {
        return context.getSrc().getDummyPosition();
    }
    
    public ISourcePositionFactory getPositionFactory() {
        return context.getSrc().getPositionFactory();
    }
    
    /**
     * Get position information for Token/Node that follows node represented by startPosition 
     * and current lexer location.
     * 
     * @param startPosition previous node/token
     * @param inclusive include previous node into position information of current node
     * @return a new position
     */
    public ISourcePosition getPosition(ISourcePosition startPosition, boolean inclusive) {
    	return context.getSrc().getPosition(startPosition, inclusive);
    }
    
    protected ISourcePosition getPositionMinusOne() {
    		context.getSrc().unread(' ');
        ISourcePosition position = getPosition(null, false);
        context.getSrc().read();
        
        return position;
    }
    
    protected ISourcePosition getPositionMinusOne(ISourcePosition startIndex) {
		context.getSrc().unread(' ');
    ISourcePosition position = getPosition(startIndex, false);
    context.getSrc().read();
    
    return position;
}

    /**
     * Parse must pass its support object for some check at bottom of
     * yylex().  Ruby does it this way as well (i.e. a little parsing
     * logic in the lexer).
     * 
     * @param parserSupport
     */
    public void setParserSupport(ParserSupport parserSupport) {
        context.setParserSupportEncap(parserSupport);
    }

    /**
     * Allow the parser to set the source for its lexer.
     * 
     * @param source where the lexer gets raw data
     */
    public void setSource(LexerSource source) {
    	context.setSrc(source);
    	context.setCurrentPos(context.getSrc().getDummyPosition());
    }

    public StrTerm getStrTerm() {
        return context.getLex_strterm();
    }
    
    public void setStrTerm(StrTerm strterm) {
    	context.setLex_strterm(strterm);
    }

    public void resetStacks() {
    	context.getConditionStateEncap().reset();
    	context.getCmdArgumentStateEncap().reset();
    }
    
    public void setWarnings(IRubyWarnings warnings) {
    	context.setWarningsEncap(warnings);
    }


    public void setState(LexState state) {
    	context.setLex_state(state);
    }

    public StackState getCmdArgumentState() {
        return context.getCmdArgumentStateEncap();
    }

    public StackState getConditionState() {
        return context.getConditionStateEncap();
    }

    private boolean isNext_identchar() {
        char c = context.getSrc().read();
        context.getSrc().unread(c);

        return c != EOF && (Character.isLetterOrDigit(c) || c == '-');
    }
    
    private static final Object getInteger(String value, int radix) {
        try {
            return Long.valueOf(value, radix);
        } catch (NumberFormatException e) {
            return new BigInteger(value, radix);
        }
    }

    private boolean ISUPPER(char c) {
        return Character.isUpperCase(c);
    }

    /**
	 * Do the next characters from the source match provided String in a case
	 * insensitive manner.  If so, then consume those characters and return 
	 * true.  Otherwise, consume none of them.
	 * 
	 * @param s to be matched against
     * @return true if string matches
     */ 
    private boolean isNextNoCase(String s) {
    	StringBuffer buf = new StringBuffer();
    	
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char r = context.getSrc().read();
            buf.append(r);
            
            if (Character.toLowerCase(c) != r &&
                Character.toUpperCase(c) != r) {
            	context.	getSrc().unreadMany(buf);
                return false;
            }
        }

        return true;
    }

	/**
	 * @param c the character to test
	 * @return true if character is a hex value (0-9a-f)
	 */
    static final boolean isHexChar(char c) {
        return Character.isDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

    /**
	 * @param c the character to test
     * @return true if character is an octal value (0-7)
	 */
    static final boolean isOctChar(char c) {
        return '0' <= c && c <= '7';
    }
    
    /**
     * @param c is character to be compared
     * @return whether c is an identifier or not
     */
    private static final boolean isIdentifierChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
    
    private int parseQuote(char c) {
        char term;
        char paren;
        
        if (!Character.isLetterOrDigit(c)) {
            term = c;
            c = 'Q';
        } else {
            term = context.getSrc().read();
            if (Character.isLetterOrDigit(term) /* no mb || ismbchar(term)*/) {
                throw new SyntaxException(context.getSrc().getPosition(), "unknown type of %string");
            }
        }
        if (c == EOF || term == EOF) {
            throw new SyntaxException(context.getSrc().getPosition(), "unterminated quoted string meets end of file");
        }
        paren = term;
        if (term == '(') term = ')';
        else if (term == '[') term = ']';
        else if (term == '{') term = '}';
        else if (term == '<') term = '>';
        else paren = '\0';

        switch (c) {
        case 'Q':
        		context.setLex_strterm(new StringTerm(str_dquote, term, paren));
        		context.setYaccValue(new Token(""+c, getPositionMinusOne()));
            return Tokens.tSTRING_BEG;

        case 'q':
	        	context.setLex_strterm(new StringTerm(str_squote, term, paren));
	        	context.setYaccValue(new Token(""+c, getPositionMinusOne()));
            return Tokens.tSTRING_BEG;

        case 'W':
        	context.setLex_strterm(new StringTerm(str_dquote | STR_FUNC_QWORDS, term, paren));
            do {c = context.getSrc().read();} while (Character.isWhitespace(c));
            context.getSrc().unread(c);
            context.setYaccValue(new Token(""+c, getPositionMinusOne()));
            return Tokens.tWORDS_BEG;

        case 'w':
        		context.setLex_strterm(new StringTerm(str_squote | STR_FUNC_QWORDS, term, paren));
            do {c = context.getSrc().read();} while (Character.isWhitespace(c));
            context.getSrc().unread(c);
            context.setYaccValue(new Token(""+c, getPositionMinusOne()));
            return Tokens.tQWORDS_BEG;

        case 'x':
	        	context.setLex_strterm(new StringTerm(str_xquote, term, paren));
	        	context.setYaccValue(new Token(""+c, getPositionMinusOne()));
            return Tokens.tXSTRING_BEG;

        case 'r':
	        	context.setLex_strterm(new StringTerm(str_regexp, term, paren));
	        	context.setYaccValue(new Token(""+c, getPositionMinusOne()));
            return Tokens.tREGEXP_BEG;

        case 's':
        		context.setLex_strterm(new StringTerm(str_ssym, term, paren));
            context.setLex_state(LexState.EXPR_FNAME);
            context.setYaccValue(new Token(""+c, getPositionMinusOne()));
            return Tokens.tSYMBEG;

        default:
            throw new SyntaxException(context.getSrc().getPosition(), "Unknown type of %string. Expected 'Q', 'q', 'w', 'x', 'r' or any non letter character, but found '" + c + "'.");
        }
    }
    
    private int hereDocumentIdentifier() {
        char c = context.getSrc().read(); 
        int term;

        int func = 0;
        if (c == '-') {
            c = context.getSrc().read();
            func = STR_FUNC_INDENT;
        }
        
        if (c == '\'' || c == '"' || c == '`') {
            if (c == '\'') {
                func |= str_squote;
            } else if (c == '"') {
                func |= str_dquote;
            } else {
                func |= str_xquote; 
            }

            context.getTokenBuffer().setLength(0);
            term = c;
            while ((c = context.getSrc().read()) != EOF && c != term) {
            	context.getTokenBuffer().append(c);
            }
            if (c == EOF) {
                throw new SyntaxException(context.getSrc().getPosition(), "unterminated here document identifier");
            }	
        } else {
            if (!isIdentifierChar(c)) {
            		context.getSrc().unread(c);
                if ((func & STR_FUNC_INDENT) != 0) {
                		context.getSrc().unread(c);
                }
                return 0;
            }
            context.getTokenBuffer().setLength(0);
            term = '"';
            func |= str_dquote;
            do {
            		context.getTokenBuffer().append(c);
            } while ((c = context.getSrc().read()) != EOF && isIdentifierChar(c));
            context.getSrc().unread(c);
        }

        String line = context.getSrc().readLine() + '\n';
        String tok = context.getTokenBuffer().toString();
        context.setLex_strterm(new HeredocTerm(tok, func, line));

        return term == '`' ? Tokens.tXSTRING_BEG : Tokens.tSTRING_BEG;
    }
    
    private void arg_ambiguous() {
    		context.getWarningsEncap().warning(context.getSrc().getPosition(), "Ambiguous first argument; make sure.");
    }


    /*
     * Not normally used, but is left in here since it can be useful in debugging
     * grammar and lexing problems.
    private void printToken(int token) {
        //System.out.print("LOC: " + support.getPosition() + " ~ ");
        
        switch (token) {
        	case Tokens.yyErrorCode: System.err.print("yyErrorCode,"); break;
        	case Tokens.kCLASS: System.err.print("kClass,"); break;
        	case Tokens.kMODULE: System.err.print("kModule,"); break;
        	case Tokens.kDEF: System.err.print("kDEF,"); break;
        	case Tokens.kUNDEF: System.err.print("kUNDEF,"); break;
        	case Tokens.kBEGIN: System.err.print("kBEGIN,"); break;
        	case Tokens.kRESCUE: System.err.print("kRESCUE,"); break;
        	case Tokens.kENSURE: System.err.print("kENSURE,"); break;
        	case Tokens.kEND: System.err.print("kEND,"); break;
        	case Tokens.kIF: System.err.print("kIF,"); break;
        	case Tokens.kUNLESS: System.err.print("kUNLESS,"); break;
        	case Tokens.kTHEN: System.err.print("kTHEN,"); break;
        	case Tokens.kELSIF: System.err.print("kELSIF,"); break;
        	case Tokens.kELSE: System.err.print("kELSE,"); break;
        	case Tokens.kCASE: System.err.print("kCASE,"); break;
        	case Tokens.kWHEN: System.err.print("kWHEN,"); break;
        	case Tokens.kWHILE: System.err.print("kWHILE,"); break;
        	case Tokens.kUNTIL: System.err.print("kUNTIL,"); break;
        	case Tokens.kFOR: System.err.print("kFOR,"); break;
        	case Tokens.kBREAK: System.err.print("kBREAK,"); break;
        	case Tokens.kNEXT: System.err.print("kNEXT,"); break;
        	case Tokens.kREDO: System.err.print("kREDO,"); break;
        	case Tokens.kRETRY: System.err.print("kRETRY,"); break;
        	case Tokens.kIN: System.err.print("kIN,"); break;
        	case Tokens.kDO: System.err.print("kDO,"); break;
        	case Tokens.kDO_COND: System.err.print("kDO_COND,"); break;
        	case Tokens.kDO_BLOCK: System.err.print("kDO_BLOCK,"); break;
        	case Tokens.kRETURN: System.err.print("kRETURN,"); break;
        	case Tokens.kYIELD: System.err.print("kYIELD,"); break;
        	case Tokens.kSUPER: System.err.print("kSUPER,"); break;
        	case Tokens.kSELF: System.err.print("kSELF,"); break;
        	case Tokens.kNIL: System.err.print("kNIL,"); break;
        	case Tokens.kTRUE: System.err.print("kTRUE,"); break;
        	case Tokens.kFALSE: System.err.print("kFALSE,"); break;
        	case Tokens.kAND: System.err.print("kAND,"); break;
        	case Tokens.kOR: System.err.print("kOR,"); break;
        	case Tokens.kNOT: System.err.print("kNOT,"); break;
        	case Tokens.kIF_MOD: System.err.print("kIF_MOD,"); break;
        	case Tokens.kUNLESS_MOD: System.err.print("kUNLESS_MOD,"); break;
        	case Tokens.kWHILE_MOD: System.err.print("kWHILE_MOD,"); break;
        	case Tokens.kUNTIL_MOD: System.err.print("kUNTIL_MOD,"); break;
        	case Tokens.kRESCUE_MOD: System.err.print("kRESCUE_MOD,"); break;
        	case Tokens.kALIAS: System.err.print("kALIAS,"); break;
        	case Tokens.kDEFINED: System.err.print("kDEFINED,"); break;
        	case Tokens.klBEGIN: System.err.print("klBEGIN,"); break;
        	case Tokens.klEND: System.err.print("klEND,"); break;
        	case Tokens.k__LINE__: System.err.print("k__LINE__,"); break;
        	case Tokens.k__FILE__: System.err.print("k__FILE__,"); break;
        	case Tokens.tIDENTIFIER: System.err.print("tIDENTIFIER["+ value() + "],"); break;
        	case Tokens.tFID: System.err.print("tFID[" + value() + "],"); break;
        	case Tokens.tGVAR: System.err.print("tGVAR[" + value() + "],"); break;
        	case Tokens.tIVAR: System.err.print("tIVAR[" + value() +"],"); break;
        	case Tokens.tCONSTANT: System.err.print("tCONSTANT["+ value() +"],"); break;
        	case Tokens.tCVAR: System.err.print("tCVAR,"); break;
        	case Tokens.tINTEGER: System.err.print("tINTEGER,"); break;
        	case Tokens.tFLOAT: System.err.print("tFLOAT,"); break;
            case Tokens.tSTRING_CONTENT: System.err.print("tSTRING_CONTENT[" + yaccValue + "],"); break;
            case Tokens.tSTRING_BEG: System.err.print("tSTRING_BEG,"); break;
            case Tokens.tSTRING_END: System.err.print("tSTRING_END,"); break;
            case Tokens.tSTRING_DBEG: System.err.print("STRING_DBEG,"); break;
            case Tokens.tSTRING_DVAR: System.err.print("tSTRING_DVAR,"); break;
            case Tokens.tXSTRING_BEG: System.err.print("tXSTRING_BEG,"); break;
            case Tokens.tREGEXP_BEG: System.err.print("tREGEXP_BEG,"); break;
            case Tokens.tREGEXP_END: System.err.print("tREGEXP_END,"); break;
            case Tokens.tWORDS_BEG: System.err.print("tWORDS_BEG,"); break;
            case Tokens.tQWORDS_BEG: System.err.print("tQWORDS_BEG,"); break;
        	case Tokens.tBACK_REF: System.err.print("tBACK_REF,"); break;
        	case Tokens.tNTH_REF: System.err.print("tNTH_REF,"); break;
        	case Tokens.tUPLUS: System.err.print("tUPLUS"); break;
        	case Tokens.tUMINUS: System.err.print("tUMINUS,"); break;
        	case Tokens.tPOW: System.err.print("tPOW,"); break;
        	case Tokens.tCMP: System.err.print("tCMP,"); break;
        	case Tokens.tEQ: System.err.print("tEQ,"); break;
        	case Tokens.tEQQ: System.err.print("tEQQ,"); break;
        	case Tokens.tNEQ: System.err.print("tNEQ,"); break;
        	case Tokens.tGEQ: System.err.print("tGEQ,"); break;
        	case Tokens.tLEQ: System.err.print("tLEQ,"); break;
        	case Tokens.tANDOP: System.err.print("tANDOP,"); break;
        	case Tokens.tOROP: System.err.print("tOROP,"); break;
        	case Tokens.tMATCH: System.err.print("tMATCH,"); break;
        	case Tokens.tNMATCH: System.err.print("tNMATCH,"); break;
        	case Tokens.tDOT2: System.err.print("tDOT2,"); break;
        	case Tokens.tDOT3: System.err.print("tDOT3,"); break;
        	case Tokens.tAREF: System.err.print("tAREF,"); break;
        	case Tokens.tASET: System.err.print("tASET,"); break;
        	case Tokens.tLSHFT: System.err.print("tLSHFT,"); break;
        	case Tokens.tRSHFT: System.err.print("tRSHFT,"); break;
        	case Tokens.tCOLON2: System.err.print("tCOLON2,"); break;
        	case Tokens.tCOLON3: System.err.print("tCOLON3,"); break;
        	case Tokens.tOP_ASGN: System.err.print("tOP_ASGN,"); break;
        	case Tokens.tASSOC: System.err.print("tASSOC,"); break;
        	case Tokens.tLPAREN: System.err.print("tLPAREN,"); break;
        	case Tokens.tLPAREN_ARG: System.err.print("tLPAREN_ARG,"); break;
        	case Tokens.tLBRACK: System.err.print("tLBRACK,"); break;
        	case Tokens.tLBRACE: System.err.print("tLBRACE,"); break;
        	case Tokens.tSTAR: System.err.print("tSTAR,"); break;
        	case Tokens.tAMPER: System.err.print("tAMPER,"); break;
        	case Tokens.tSYMBEG: System.err.print("tSYMBEG,"); break;
        	case '\n': System.err.println("NL"); break;
        	default: System.err.print("'" + (int)token + "',"); break;
        }
    }

    // DEBUGGING HELP 
    private int yylex() {
        int token = yylex2();
        
        printToken(token);
        
        return token;
    }
    */
    
    
    
    /**
     *  Returns the next token. Also sets yyVal is needed.
     *
     *@return    Description of the Returned Value
     */
    private int yylex() {
        char c;
        boolean spaceSeen = false;
        boolean commandState;
        
        if (context.getLex_strterm() != null) {
			int tok = context.getLex_strterm().parseString(this, context.getSrc());
			if (tok == Tokens.tSTRING_END || tok == Tokens.tREGEXP_END) {
				context.setLex_strterm(null);
				context.setLex_state(LexState.EXPR_END);
			}
			return tok;
        }

        context.setCurrentPos(context.getSrc().getPosition());
        
        commandState = context.isCommandStart();
        context.setCommandStart(false);
        
        //tcorbat
        context.setLastWasNewLine(context.isCurrentIsNewLine());
        context.setCurrentIsNewLine(false);	
        
        retry: for(;;) {
        	

        	
            c = context.getSrc().read();
            switch(c) {
            case '\004':		/* ^D */
            case '\032':		/* ^Z */
            case 0:			/* end of script. */
                return 0;

                /* white spaces */
            case ' ': case '\t': case '\f': case '\r':
            case '\13': /* '\v' */
                getPosition(null, false);
                spaceSeen = true;
                continue retry;
                
            // tcorbat: Changed for handling comments
            case '#': /* it's a comment */
                    ISourcePosition startPos = getPositionMinusOne();
                    StringBuffer commentText = new StringBuffer();
                    
                    commentText.append(c);
                    c = readRestOfLine(commentText);
                    context.getSrc().unread(c);
                    
                    context.setYaccValue(new Token(commentText.toString(), getPositionMinusOne(startPos)));
                    
                    if(context.isLastWasNewLine())
                            return Tokens.tSOLOCOMMENT;
                    return Tokens.tTAILCOMMENT;
            
            /* fall through -> tcorbat: not anymore */
            case '\n':
            	// Replace a string of newlines with a single one

            		context.setCurrentIsNewLine(true);
                do {
                		context.setCurrentPos(context.getSrc().getPosition());
                } while((c = context.getSrc().read()) == '\n'); 
                context.getSrc().unread( c );

                if (context.getLex_state() == LexState.EXPR_BEG ||
                		context.getLex_state() == LexState.EXPR_FNAME ||
                		context.getLex_state() == LexState.EXPR_DOT ||
                		context.getLex_state() == LexState.EXPR_CLASS) {
                		context.setLastWasNewLine(context.isCurrentIsNewLine());
                		context.setCurrentIsNewLine(false);
                    continue retry;
                } 

                context.setCommandStart(true);
                context.setLex_state(LexState.EXPR_BEG);
                
                //tcorbat: Resetting the yaccValue for not dublicating comments
                context.setYaccValue(new Token("\n", getPosition(null, false)));
                
                return '\n';
                
            case '*':
                if ((c = context.getSrc().read()) == '*') {
                    if ((c = context.getSrc().read()) == '=') {
                    		context.setYaccValue(new Token("**", getPosition(null, false)));
                    		context.setLex_state(LexState.EXPR_BEG);
                        return Tokens.tOP_ASGN;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("**", getPosition(null, false)));
                    c = Tokens.tPOW;
                } else {
                    if (c == '=') {
                    		context.setYaccValue(new Token("*", getPosition(null, false)));
                    		context.setLex_state(LexState.EXPR_BEG);
                        return Tokens.tOP_ASGN;
                    }
                    context.getSrc().unread(c);
                    if (context.getLex_state().isArgument() && spaceSeen && !Character.isWhitespace(c)) {
                    		context.getWarningsEncap().warning(context.getSrc().getPosition(), "`*' interpreted as argument prefix");
                        c = Tokens.tSTAR;
                    } else if (context.getLex_state() == LexState.EXPR_BEG || 
                    			context.getLex_state() == LexState.EXPR_MID) {
                        c = Tokens.tSTAR;
                    } else {
                        c = Tokens.tSTAR2;
                    }
                }
                if (context.getLex_state() == LexState.EXPR_FNAME ||
                		context.getLex_state() == LexState.EXPR_DOT) {
                		context.setLex_state(LexState.EXPR_ARG);
                } else {
                		context.setLex_state(LexState.EXPR_BEG);
                }
                		context.setYaccValue(new Token("*", getPosition(null, false)));
                return c;

            case '!':
            		context.setLex_state(LexState.EXPR_BEG);
                if ((c = context.getSrc().read()) == '=') {
                		//tcorbat
                		context.setYaccValue(new Token("!=",getPosition(null, true)));
                    return Tokens.tNEQ;
                }
                if (c == '~') {
            			//tcorbat
            			context.setYaccValue(new Token("!~",getPosition(null, true)));
                    return Tokens.tNMATCH;
                }
                context.getSrc().unread(c);
	    			//tcorbat
	    			context.setYaccValue(new Token("!",getPosition(null, false)));
                return Tokens.tBANG;

            case '=':
                // Skip documentation nodes
                if (context.getSrc().wasBeginOfLine()) {
                    /* skip embedded rd document */
                    if (isNextNoCase("begin")) {
                    		ISourcePosition startPosition = getPosition();
                        StringBuffer documentationText = new StringBuffer();
                        documentationText.append("=begin");
                    		
                        c = context.getSrc().read();
                        
                        if (Character.isWhitespace(c)) {
                            // In case last next was the newline.
                        		context.getSrc().unread(c); 
                            for (;;) {
                                c = context.getSrc().read();
                                documentationText.append(c);

                                // If a line is followed by a blank line put
                                // it back.
                                while (c == '\n') {
                                    c = context.getSrc().read();
                                    documentationText.append(c);
                                }
                                if (c == EOF) {
                                    throw new SyntaxException(context.getSrc().getPosition(), "embedded document meets end of file");
                                }
                                if (c != '=') continue;
                                if (context.getSrc().wasBeginOfLine() && isNextNoCase("end")) {
                                    //if (src.peek('\n')) {
                                    //    break;
                                    //} 
                                    
                                    //c = src.read();
                                    
                                    //if (Character.isWhitespace(c)) {
                                			documentationText.append("end");
                                			documentationText.append(context.getSrc().readLine());
                                			getPosition();
                                        break;
                                    //}
									//src.unread(c);
                                }
                            }
                            context.setYaccValue(new Token(documentationText.toString(), getPositionMinusOne(startPosition)));
                            return Tokens.tSOLOCOMMENT;
                            //continue retry;
                        }
                        context.getSrc().unread(c);
                    }
                }

                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                		context.setLex_state(LexState.EXPR_ARG);
                } else { 
                			context.setLex_state(LexState.EXPR_BEG);
                }

                c = context.getSrc().read();
                if (c == '=') {
                    c = context.getSrc().read();
                    if (c == '=') {
                    	context.setYaccValue(new Token("===", getPosition(null, false)));
                        return Tokens.tEQQ;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("==", getPosition(null, false)));
                    return Tokens.tEQ;
                }
                if (c == '~') {
                		context.setYaccValue(new Token("=~", getPosition(null, false)));
                    return Tokens.tMATCH;
                } else if (c == '>') {
                		context.setYaccValue(new Token("=>", getPosition(null, false)));
                    return Tokens.tASSOC;
                }
                context.getSrc().unread(c);
                context.setYaccValue(new Token("=", getPosition(null, false)));
                return '=';
                
            case '<':
                c = context.getSrc().read();
                if (c == '<' &&
                			context.getLex_state() != LexState.EXPR_END &&
                			context.getLex_state() != LexState.EXPR_DOT &&
                			context.getLex_state() != LexState.EXPR_ENDARG && 
                			context.getLex_state() != LexState.EXPR_CLASS &&
                        (!context.getLex_state().isArgument() || spaceSeen)) {
                    int tok = hereDocumentIdentifier();
                    if (tok != 0) return tok;
                }
                if (context.getLex_state() == LexState.EXPR_FNAME ||
                		context.getLex_state() == LexState.EXPR_DOT) {
                		context.setLex_state(LexState.EXPR_ARG);
                } else {
                		context.setLex_state(LexState.EXPR_BEG);
                }
                if (c == '=') {
                    if ((c = context.getSrc().read()) == '>') {
                    		context.setYaccValue(new Token("<=>", getPosition(null, false)));
                        return Tokens.tCMP;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("<=", getPosition(null, false)));
                    return Tokens.tLEQ;
                }
                if (c == '<') {
                    if ((c = context.getSrc().read()) == '=') {
                    		context.setLex_state(LexState.EXPR_BEG);
                    		context.setYaccValue(new Token("<<", getPosition(null, false)));
                        return Tokens.tOP_ASGN;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("<<", getPosition(null, false)));
                    return Tokens.tLSHFT;
                }
                context.setYaccValue(new Token("<", getPosition(null, false)));
                context.getSrc().unread(c);
                return Tokens.tLT;
                
            case '>':
                if (context.getLex_state() == LexState.EXPR_FNAME ||
                		context.getLex_state() == LexState.EXPR_DOT) {
                		context.setLex_state(LexState.EXPR_ARG);
                } else {
                		context.setLex_state(LexState.EXPR_BEG);
                }

                if ((c = context.getSrc().read()) == '=') {
                		context.setYaccValue(new Token(">=", getPosition(null, false)));
                    return Tokens.tGEQ;
                }
                if (c == '>') {
                    if ((c = context.getSrc().read()) == '=') {
	                    	context.setLex_state(LexState.EXPR_BEG);
	                    	context.setYaccValue(new Token(">>", getPosition(null, false)));
                        return Tokens.tOP_ASGN;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token(">>", getPosition(null, false)));
                    return Tokens.tRSHFT;
                }
                context.getSrc().unread(c);
                context.setYaccValue(new Token(">", getPosition(null, false)));
                return Tokens.tGT;

            case '"':
            		context.setLex_strterm(new StringTerm(str_dquote, '"', '\0'));
                return Tokens.tSTRING_BEG;

            case '`':
            		context.setYaccValue(new Token("`", getPosition(null, false)));
                if (context.getLex_state() == LexState.EXPR_FNAME) {
                		context.setLex_state(LexState.EXPR_END);
                    return Tokens.tBACK_REF2;
                }
                if (context.getLex_state() == LexState.EXPR_DOT) {
                    if (commandState) {
                    		context.setLex_state(LexState.EXPR_CMDARG);
                    } else {
                    		context.setLex_state(LexState.EXPR_ARG);
                    }
                    return Tokens.tBACK_REF2;
                }
                context.setLex_strterm(new StringTerm(str_xquote, '`', '\0'));
                return Tokens.tXSTRING_BEG;

            case '\'':
	            	context.setLex_strterm(new StringTerm(str_squote, '\'', '\0'));
	    			return Tokens.tSTRING_BEG;

            case '?':
                if (context.getLex_state() == LexState.EXPR_END || 
                		context.getLex_state() == LexState.EXPR_ENDARG) {
                		context.setLex_state(LexState.EXPR_BEG);
            			//tcorbat
            			context.setYaccValue(new Token("?",getPosition(null, false)));
                    return '?';
                }
                c = context.getSrc().read();
                if (c == EOF) {
                    throw new SyntaxException(context.getSrc().getPosition(), "incomplete character syntax");
                }
                if (Character.isWhitespace(c)){
                    if (!context.getLex_state().isArgument()){
                        int c2 = 0;
                        switch (c) {
                        case ' ':
                            c2 = 's';
                            break;
                        case '\n':
                            c2 = 'n';
                            break;
                        case '\t':
                            c2 = 't';
                            break;
                            /* What is \v in C?
                        case '\v':
                            c2 = 'v';
                            break;
                            */
                        case '\r':
                            c2 = 'r';
                            break;
                        case '\f':
                            c2 = 'f';
                            break;
                        }
                        if (c2 != 0) {
                        	context.getWarningsEncap().warn(context.getSrc().getPosition(), "invalid character syntax; use ?\\" + c2);
                        }
                    }
                    context.getSrc().unread(c);
                    context.setLex_state(LexState.EXPR_BEG);
                    context.setYaccValue(new Token("?", getPosition(null, false)));
                    return '?';
                /*} else if (ismbchar(c)) { // ruby - we don't support them either?
                    rb_warn("multibyte character literal not supported yet; use ?\\" + c);
                    support.unread(c);
                    lexState = LexState.EXPR_BEG;
                    return '?';*/
                } else if ((Character.isLetterOrDigit(c) || c == '_') &&
                        !context.getSrc().peek('\n') && isNext_identchar()) {
                	context.getSrc().unread(c);
                	context.setLex_state(LexState.EXPR_BEG);
                	context.setYaccValue(new Token("?", getPosition(null, false)));
                    return '?';
                } else if (c == '\\') {
                    c = context.getSrc().readEscape();
                }
                c &= 0xff;
                context.setLex_state(LexState.EXPR_END);
                context.setYaccValue(new Token(new Long(c), getPosition(null, false)));
                
                return Tokens.tINTEGER;

            case '&':
                if ((c = context.getSrc().read()) == '&') {
                	context.setLex_state(LexState.EXPR_BEG);
                    if ((c = context.getSrc().read()) == '=') {
	                    	context.setYaccValue(new Token("&&", getPosition(null, false)));
	                    	context.setLex_state(LexState.EXPR_BEG);
                        return Tokens.tOP_ASGN;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("&&", getPositionMinusOne()));
                    return Tokens.tANDOP;
                }
                else if (c == '=') {
	                	context.setYaccValue(new Token("&", getPosition(null, false)));
	                	context.setLex_state(LexState.EXPR_BEG);
                    return Tokens.tOP_ASGN;
                }
                context.getSrc().unread(c);
                if (context.getLex_state().isArgument() && spaceSeen && !Character.isWhitespace(c)){
                	context.getWarningsEncap().warning(context.getSrc().getPosition(), "`&' interpreted as argument prefix");
                    c = Tokens.tAMPER;
                } else if (context.getLex_state() == LexState.EXPR_BEG || 
                		context.getLex_state() == LexState.EXPR_MID) {
                    c = Tokens.tAMPER;
                } else {
                    c = Tokens.tAMPER2;
                }
                
                if (context.getLex_state() == LexState.EXPR_FNAME ||
                		context.getLex_state() == LexState.EXPR_DOT) {
                		context.setLex_state(LexState.EXPR_ARG);
                } else {
                		context.setLex_state(LexState.EXPR_BEG);
                }
                context.setYaccValue(new Token("&", getPosition(null, false)));
                return c;
                
            case '|':
                if ((c = context.getSrc().read()) == '|') {
                	context.setLex_state(LexState.EXPR_BEG);
                    if ((c = context.getSrc().read()) == '=') {
	                    	context.setYaccValue(new Token("||", getPosition(null, false)));
	                    	context.setLex_state(LexState.EXPR_BEG);
                        return Tokens.tOP_ASGN;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("||", getPositionMinusOne()));
                    return Tokens.tOROP;
                }
                if (c == '=') {
	                	context.setYaccValue(new Token("|", getPosition(null, false)));
	                	context.setLex_state(LexState.EXPR_BEG);
                    return Tokens.tOP_ASGN;
                }
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                		context.setLex_state(LexState.EXPR_ARG);
                } else {
                		context.setLex_state(LexState.EXPR_BEG);
                }
                context.getSrc().unread(c);
                context.setYaccValue(new Token("|", getPosition(null, false)));
                return Tokens.tPIPE;

            case '+':
                c = context.getSrc().read();
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                		context.setLex_state(LexState.EXPR_ARG);
                    if (c == '@') {
                    		context.setYaccValue(new Token("@+", getPosition(null, false)));
                        return Tokens.tUPLUS;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("+", getPosition(null, false)));
                    return Tokens.tPLUS;
                }
                if (c == '=') {
	                	context.setYaccValue(new Token("+", getPosition(null, false)));
	                	context.setLex_state(LexState.EXPR_BEG);
                    return Tokens.tOP_ASGN;
                }
                if (context.getLex_state() == LexState.EXPR_BEG ||
                		context.getLex_state() == LexState.EXPR_MID ||
                        (context.getLex_state().isArgument() && spaceSeen && !Character.isWhitespace(c))) {
                    if (context.getLex_state().isArgument()) arg_ambiguous();
                    context.setLex_state(LexState.EXPR_BEG);
                    context.getSrc().unread(c);
                    if (Character.isDigit(c)) {
                        c = '+';
                        return parseNumber(c);
                    }
                    context.setYaccValue(new Token("+", getPosition(null, false)));
                    return Tokens.tUPLUS;
                }
                context.setLex_state(LexState.EXPR_BEG);
                context.getSrc().unread(c);
                context.setYaccValue(new Token("+", getPosition(null, false)));
                return Tokens.tPLUS;

            case '-':
                c = context.getSrc().read();
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                	context.setLex_state(LexState.EXPR_ARG);
                    if (c == '@') {
                    		context.setYaccValue(new Token("@-", getPosition(null, false)));
                        return Tokens.tUMINUS;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("-", getPosition(null, false)));
                    return Tokens.tMINUS;
                }
                if (c == '=') {
	                	context.setYaccValue(new Token("-", getPosition(null, false)));
	                	context.setLex_state(LexState.EXPR_BEG);
                    return Tokens.tOP_ASGN;
                }
                if (context.getLex_state() == LexState.EXPR_BEG || 
                		context.getLex_state() == LexState.EXPR_MID ||
                        (context.getLex_state().isArgument() && spaceSeen && !Character.isWhitespace(c))) {
                    if (context.getLex_state().isArgument()) arg_ambiguous();
                    context.setLex_state(LexState.EXPR_BEG);
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("-", getPosition(null, false)));
                    if (Character.isDigit(c)) {
                        return Tokens.tUMINUS_NUM;
                    }
                    return Tokens.tUMINUS;
                }
                context.setLex_state(LexState.EXPR_BEG);
                context.getSrc().unread(c);
                context.setYaccValue(new Token("-", getPosition(null, false)));
                return Tokens.tMINUS;
                
            case '.':
            	context.setLex_state(LexState.EXPR_BEG);
                if ((c = context.getSrc().read()) == '.') {
                    if ((c = context.getSrc().read()) == '.') {
                    		context.setYaccValue(new Token("...", getPosition(null, false)));
                        return Tokens.tDOT3;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("..", getPosition(null, false)));
                    return Tokens.tDOT2;
                }
                context.getSrc().unread(c);
                if (Character.isDigit(c)) {
                    throw new SyntaxException(context.getSrc().getPosition(), "no .<digit> floating literal anymore; put 0 before dot"); 
                }
                context.setLex_state(LexState.EXPR_DOT);
                context.setYaccValue(new Token(".", getPosition(null, false)));
                return Tokens.tDOT;
            case '0' : case '1' : case '2' : case '3' : case '4' :
            case '5' : case '6' : case '7' : case '8' : case '9' :
                return parseNumber(c);
                
            case ']':
            case '}':
            case ')':
	            	context.getConditionStateEncap().restart();
	            	context.getCmdArgumentStateEncap().restart();
	            	context.setLex_state(LexState.EXPR_END);
        			//tcorbat
        			context.setYaccValue(new Token(Character.toString(c),getPosition(null, false)));
                return c;

            case ':':
                c = context.getSrc().read();
                if (c == ':') {
                    if (context.getLex_state() == LexState.EXPR_BEG ||
                    		context.getLex_state() == LexState.EXPR_MID ||
                    		context.getLex_state() == LexState.EXPR_CLASS || 
                        (context.getLex_state().isArgument() && spaceSeen)) {
	                    	context.setLex_state(LexState.EXPR_BEG);
	                    	context.setYaccValue(new Token("::", getPosition(null, false)));
                        return Tokens.tCOLON3;
                    }
                    context.setLex_state(LexState.EXPR_DOT);
	        			//tcorbat
	        			context.setYaccValue(new Token(":",getPosition(null, false)));
                    return Tokens.tCOLON2;
                }
                if (context.getLex_state() == LexState.EXPR_END || 
                		context.getLex_state() == LexState.EXPR_ENDARG || Character.isWhitespace(c)) {
	                	context.getSrc().unread(c);
	                	context.setLex_state(LexState.EXPR_BEG);
            			//tcorbat
            			context.setYaccValue(new Token(":",getPosition(null, false)));
                    return ':';
                }
                switch (c) {
                case '\'':
                	context.setLex_strterm(new StringTerm(str_ssym, c, '\0'));
                    break;
                case '"':
                	context.setLex_strterm(new StringTerm(str_dsym, c, '\0'));
                    break;
                default:
                	context.getSrc().unread(c);
                    break;
                }
                context.setLex_state(LexState.EXPR_FNAME);
                context.setYaccValue(new Token(":", getPositionMinusOne()));
                return Tokens.tSYMBEG;

            case '/':
                if (context.getLex_state() == LexState.EXPR_BEG || 
                		context.getLex_state() == LexState.EXPR_MID) {
                		context.setLex_strterm(new StringTerm(str_regexp, '/', '\0'));
            			//tcorbat
            			context.setYaccValue(new Token("/",getPosition(null, false)));
                    return Tokens.tREGEXP_BEG;
                }
                
                if ((c = context.getSrc().read()) == '=') {
                	context.setYaccValue(new Token("/", getPositionMinusOne()));
                	context.setLex_state(LexState.EXPR_BEG);
                    return Tokens.tOP_ASGN;
                }
                context.getSrc().unread(c);
                if (context.getLex_state().isArgument() && spaceSeen) {
                    if (!Character.isWhitespace(c)) {
                        arg_ambiguous();
                        context.setLex_strterm(new StringTerm(str_regexp, '/', '\0'));
	            			//tcorbat
	            			context.setYaccValue(new Token(Character.toString(c),getPosition(null, true)));
                        return Tokens.tREGEXP_BEG;
                    }
                }
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                	context.setLex_state(LexState.EXPR_ARG);
                } else {
                	context.setLex_state(LexState.EXPR_BEG);
                }
                context.setYaccValue(new Token("/", getPosition(null, false)));
                return Tokens.tDIVIDE;

            case '^':
                if ((c = context.getSrc().read()) == '=') {
	                	context.setLex_state(LexState.EXPR_BEG);
	                	context.setYaccValue(new Token("^", getPosition(null, false)));
                    return Tokens.tOP_ASGN;
                }
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                	context.setLex_state(LexState.EXPR_ARG);
                } else {
                	context.setLex_state(LexState.EXPR_BEG);
                }
                context.getSrc().unread(c);
                context.setYaccValue(new Token("^", getPosition(null, false)));
                return Tokens.tCARET;

            case ';':
            		context.setCommandStart(true);
            case ',':
            		context.setLex_state(LexState.EXPR_BEG);
        			//tcorbat
        			context.setYaccValue(new Token(Character.toString(c),getPosition(null, true)));
                return c;

            case '~':
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                    if ((c = context.getSrc().read()) != '@') {
                    	context.getSrc().unread(c);
                    }
                }
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                	context.setLex_state(LexState.EXPR_ARG);
                } else {
                	context.setLex_state(LexState.EXPR_BEG);
                }
                context.setYaccValue(new Token("~", getPosition(null, false)));
                return Tokens.tTILDE;
            case '(':
            	c = Tokens.tLPAREN2;
            	context.setCommandStart(true);
                if (context.getLex_state() == LexState.EXPR_BEG || 
                		context.getLex_state() == LexState.EXPR_MID) {
                    c = Tokens.tLPAREN;
                } else if (spaceSeen) {
                    if (context.getLex_state() == LexState.EXPR_CMDARG) {
                        c = Tokens.tLPAREN_ARG;
                    } else if (context.getLex_state() == LexState.EXPR_ARG) {
                    	context.getWarningsEncap().warn(context.getSrc().getPosition(), "don't put space before argument parentheses");
                        c = Tokens.tLPAREN2;
                    }
                }
                context.getConditionStateEncap().stop();
                context.getCmdArgumentStateEncap().stop();
                context.setLex_state(LexState.EXPR_BEG);
                context.setYaccValue(new Token("(", getPosition(null, false)));
                return c;

            case '[':
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                	context.setLex_state(LexState.EXPR_ARG);
                    if ((c = context.getSrc().read()) == ']') {
                        if ((c = context.getSrc().read()) == '=') {
                        		context.setYaccValue(new Token("[]=", getPosition(null, false)));
                            return Tokens.tASET;
                        }
                        context.setYaccValue(new Token("[]", getPosition(null, false)));
                        context.getSrc().unread(c);
                        return Tokens.tAREF;
                    }
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token("[", getPosition(null, false)));
                    return '[';
                } else if (context.getLex_state() == LexState.EXPR_BEG || 
                		context.getLex_state() == LexState.EXPR_MID) {
                    c = Tokens.tLBRACK;
                } else if (context.getLex_state().isArgument() && spaceSeen) {
                    c = Tokens.tLBRACK;
                }
                context.setLex_state(LexState.EXPR_BEG);
                context.getConditionStateEncap().stop();
                context.getCmdArgumentStateEncap().stop();
                context.setYaccValue(new Token("[", getPosition(null, false)));
                return c;
                
            case '{':
            	c = Tokens.tLCURLY;
            	
                if (context.getLex_state().isArgument() || context.getLex_state() == LexState.EXPR_END) {
                    c = Tokens.tLCURLY;          /* block (primary) */
                } else if (context.getLex_state() == LexState.EXPR_ENDARG) {
                    c = Tokens.tLBRACE_ARG;  /* block (expr) */
                } else {
                    c = Tokens.tLBRACE;      /* hash */
                }
                context.getConditionStateEncap().stop();
                context.getCmdArgumentStateEncap().stop();
                context.setLex_state(LexState.EXPR_BEG);
                context.setYaccValue(new Token("{", getPosition(null, false)));
                return c;

            case '\\':
                c = context.getSrc().read();
                if (c == '\n') {
                    spaceSeen = true;
                    continue retry; /* skip \\n */
                }
                context.getSrc().unread(c);
                context.setYaccValue(new Token("\\", getPosition(null, false)));
                return '\\';

            case '%':
                if (context.getLex_state() == LexState.EXPR_BEG || 
                		context.getLex_state() == LexState.EXPR_MID) {
	        			//tcorbat
	        			context.setYaccValue(new Token("%",getPosition(null, false)));
                    return parseQuote(context.getSrc().read());
                }
                if ((c = context.getSrc().read()) == '=') {
	                	context.setYaccValue(new Token("%", getPosition(null, false)));
	                	context.setLex_state(LexState.EXPR_BEG);
                    return Tokens.tOP_ASGN;
                }
                if (context.getLex_state().isArgument() && spaceSeen && !Character.isWhitespace(c)) {
                    return parseQuote(c);
                }
                if (context.getLex_state() == LexState.EXPR_FNAME || 
                		context.getLex_state() == LexState.EXPR_DOT) {
                	context.setLex_state(LexState.EXPR_ARG);
                } else {
                	context.setLex_state(LexState.EXPR_BEG);
                }
                context.getSrc().unread(c);
                context.setYaccValue(new Token("%", getPosition(null, false)));
                return Tokens.tPERCENT;

            case '$':
            	context.setLex_state(LexState.EXPR_END);
            	context.getTokenBuffer().setLength(0);
                c = context.getSrc().read();
                switch (c) {
                case '_':		/* $_: last read line string */
                    c = context.getSrc().read();
                    if (isIdentifierChar(c)) {
                    	context.getTokenBuffer().append('$');
                    	context.getTokenBuffer().append('_');
                        break;
                    }
                    context.getSrc().unread(c);
                    c = '_';
                    /* fall through */
                case '*':		/* $*: argv */
                case '$':		/* $$: pid */
                case '?':		/* $?: last status */
                case '!':		/* $!: error string */
                case '@':		/* $@: error position */
                case '/':		/* $/: input record separator */
                case '\\':		/* $\: output record separator */
                case ';':		/* $;: field separator */
                case ',':		/* $,: output field separator */
                case '.':		/* $.: last read line number */
                case '=':		/* $=: ignorecase */
                case ':':		/* $:: load path */
                case '<':		/* $<: reading filename */
                case '>':		/* $>: default output handle */
                case '\"':		/* $": already loaded files */
	                	context.getTokenBuffer().append('$');
	                	context.getTokenBuffer().append(c);
	                	context.setYaccValue(new Token(context.getTokenBuffer().toString(), getPosition(null, false)));
                    return Tokens.tGVAR;

                case '-':
	                	context.getTokenBuffer().append('$');
	                	context.getTokenBuffer().append(c);
                    c = context.getSrc().read();
                    context.getTokenBuffer().append(c);
                    context.setYaccValue(new Token(context.getTokenBuffer().toString(), getPosition(null, false)));
                    /* xxx shouldn't check if valid option variable */
                    return Tokens.tGVAR;

                case '~':		/* $~: match-data */
                case '&':		/* $&: last match */
                case '`':		/* $`: string before last match */
                case '\'':		/* $': string after last match */
                case '+':		/* $+: string matches last paren. */
                		context.setYaccValue(new Token(new BackRefNode(context.getSrc().getPosition(), c),context.getSrc().getPosition()));
                    return Tokens.tBACK_REF;

                case '1': case '2': case '3':
                case '4': case '5': case '6':
                case '7': case '8': case '9':
                	context.getTokenBuffer().append('$');
                    do {
                    	context.getTokenBuffer().append(c);
                        c = context.getSrc().read();
                    } while (Character.isDigit(c));
                    context.getSrc().unread(c);
                    context.setYaccValue(new Token(new NthRefNode(context.getSrc().getPosition(), Integer.parseInt(context.getTokenBuffer().substring(1))),context.getSrc().getPosition()));
                    
                    return Tokens.tNTH_REF;

                default:
                    if (!isIdentifierChar(c)) {
	                    	context.getSrc().unread(c);
	                    	context.setYaccValue(new Token("$", getPosition(null, false)));
                        return '$';
                    }
                case '0':
                	context.getTokenBuffer().append('$');
                }
                break;

            case '@':
                c = context.getSrc().read();
                context.getTokenBuffer().setLength(0);
                context.getTokenBuffer().append('@');
                if (c == '@') {
                	context.getTokenBuffer().append('@');
                    c = context.getSrc().read();
                }
                if (Character.isDigit(c)) {
                    if (context.getTokenBuffer().length() == 1) {
                        throw new SyntaxException(context.getSrc().getPosition(), "`@" + c + "' is not allowed as an instance variable name");
                    }
                    throw new SyntaxException(context.getSrc().getPosition(), "`@@" + c + "' is not allowed as a class variable name");
                }
                if (!isIdentifierChar(c)) {
                	context.getSrc().unread(c);
                	context.setYaccValue(new Token("@", getPosition(null, false)));
                    return '@';
                }
                break;

            case '_':
                if (context.getSrc().wasBeginOfLine() && context.getSrc().matchString("_END__\n", false)) {
                		context.getParserSupportEncap().getResult().setEndSeen(true);
                    return 0;
                }
                context.getTokenBuffer().setLength(0);
                break;

            default:
                if (!isIdentifierChar(c)) {
                    throw new SyntaxException(context.getSrc().getPosition(), "Invalid char `\\" + new PrintfFormat("%.3o").sprintf(c) + "' in expression");
                }
            
            context.getTokenBuffer().setLength(0);
                break;
            }
    
            do {
            	context.getTokenBuffer().append(c);
                /* no special multibyte character handling is needed in Java
                 * if (ismbchar(c)) {
                    int i, len = mbclen(c)-1;

                    for (i = 0; i < len; i++) {
                        c = src.read();
                        tokenBuffer.append(c);
                    }
                }*/
                c = context.getSrc().read();
            } while (isIdentifierChar(c));
            
            char peek = context.getSrc().read();
            if ((c == '!' || c == '?') && 
                isIdentifierChar(context.getTokenBuffer().charAt(0)) && peek != '=') {
            	context.getSrc().unread(peek);
            	context.getTokenBuffer().append(c);
            } else {
            	context.getSrc().unread(peek);
            	context.getSrc().unread(c);
            }
            
            int result = 0;

            switch (context.getTokenBuffer().charAt(0)) {
                case '$':
                	context.setLex_state(LexState.EXPR_END);
                    result = Tokens.tGVAR;
                    break;
                case '@':
                	context.setLex_state(LexState.EXPR_END);
                    if (context.getTokenBuffer().charAt(1) == '@') {
                        result = Tokens.tCVAR;
                    } else {
                        result = Tokens.tIVAR;
                    }
                    break;

                default:
                	char last = context.getTokenBuffer().charAt(context.getTokenBuffer().length() - 1);
                    if (last == '!' || last == '?') {
                        result = Tokens.tFID;
                    } else {
                        if (context.getLex_state() == LexState.EXPR_FNAME) {
                        	/*
                        	// Enebo: This should be equivalent to below without
                        	// so much read/unread action.
                            if ((c = src.read()) == '=') { 
                            	char c2 = src.read();
                        	
                            	if (c2 != '~' && c2 != '>' &&
                            		(c2 != '=' || (c2 == '\n' && src.peek('>')))) {
                            		result = Token.tIDENTIFIER;
                            		tokenBuffer.append(c);
                            	} else { 
                            		src.unread(c2);
                            		src.unread(c);
                            	}
                        	} else {
                            	src.unread(c);
                            }
                            */
                            
                            if ((c = context.getSrc().read()) == '=' && 
                            	 !context.getSrc().peek('~') && 
								 !context.getSrc().peek('>') &&
                                 (!context.getSrc().peek('=') || 
                                 		(context.getSrc().peek('\n') && 
                                 				context.getSrc().getCharAt(1) == '>'))) {
                                result = Tokens.tIDENTIFIER;
                                context.getTokenBuffer().append(c);
                            } else {
                            	context.getSrc().unread(c);
                            }
                        }
                        if (result == 0 && ISUPPER(context.getTokenBuffer().charAt(0))) {
                            result = Tokens.tCONSTANT;
                        } else {
                            result = Tokens.tIDENTIFIER;
                        }
                    }

                    if (context.getLex_state() != LexState.EXPR_DOT) {
                        /* See if it is a reserved word.  */
                        Keyword keyword = Keyword.getKeyword(context.getTokenBuffer().toString(), context.getTokenBuffer().length());
                        if (keyword != null) {
                            // enum lex_state
                            LexState state = context.getLex_state();

                            context.setLex_state(keyword.state);
                            if (state.isExprFName()) {
                            	context.setYaccValue(new Token(keyword.name, getPositionMinusOne()));
                            } else {
                            	context.setYaccValue(new Token(context.getTokenBuffer().toString(), getPositionMinusOne()));
                            }
                            if (keyword.id0 == Tokens.kDO) {
                                if (context.getConditionStateEncap().isInState()) {
                                    return Tokens.kDO_COND;
                                }
                                if (context.getCmdArgumentStateEncap().isInState() && state != LexState.EXPR_CMDARG) {
                                    return Tokens.kDO_BLOCK;
                                }
                                if (state == LexState.EXPR_ENDARG) {
                                    return Tokens.kDO_BLOCK;
                                }
                                return Tokens.kDO;
                            }

                            if (state == LexState.EXPR_BEG) {
                                return keyword.id0;
                            }
							if (keyword.id0 != keyword.id1) {
								context.setLex_state(LexState.EXPR_BEG);
							}
							return keyword.id1;
                        }
                    }

                    if (context.getLex_state() == LexState.EXPR_BEG ||
                    		context.getLex_state() == LexState.EXPR_MID ||
                    		context.getLex_state() == LexState.EXPR_DOT ||
                    		context.getLex_state() == LexState.EXPR_ARG ||
                    		context.getLex_state() == LexState.EXPR_CMDARG) {
                        if (commandState) {
                        	context.setLex_state(LexState.EXPR_CMDARG);
                        } else {
                        	context.setLex_state(LexState.EXPR_ARG);
                        }
                    } else {
                    	context.setLex_state(LexState.EXPR_END);
                    }
            }
            
            //tcorbat: Removed ugly abuse of yaccValue as a temporary variable
            String tempVal = context.getTokenBuffer().toString(); 

            // Lame: parsing logic made it into lexer in ruby...So we
            // are emulating
            if (IdUtil.isLocal(tempVal) &&
                ((((LocalNamesElement) context.getParserSupportEncap().getLocalNames().peek()).isInBlock() && 
                ((BlockNamesElement) context.getParserSupportEncap().getBlockNames().peek()).isDefined(tempVal)) ||
				((LocalNamesElement) context.getParserSupportEncap().getLocalNames().peek()).isLocalRegistered(tempVal))) {
            	context.setLex_state(LexState.EXPR_END);
            }

            context.setYaccValue(new Token(tempVal, getPositionMinusOne()));

            return result;
        }
    }

	/**
	 * Reads the remaining characters in the current line and appends them to the StringBuffer <code>lineText</code>.
	 * @author tcorbat
	 * @param A <code>StingBuffer</code> which receives the charaters read
	 * @return The last chracter read - usually \\n or EOF
	 */
	private char readRestOfLine(StringBuffer lineText) {
		char c;
		while ((c = context.getSrc().read()) != '\n') {
			lineText.append(c);
			context.setCurrentPos(context.getSrc().getPosition());
			if (c == EOF) {
				break;
			}
		}
		return c;
	}

    /**
     *  Parse a number from the input stream.
     *
     *@param c The first character of the number.
     *@return A int constant wich represents a token.
     */
    private int parseNumber(char c) {
    	context.setLex_state(LexState.EXPR_END);

    	context.getTokenBuffer().setLength(0);

        if (c == '-') {
        	context.getTokenBuffer().append(c);
            c = context.getSrc().read();
        } else if (c == '+') {
        	// We don't append '+' since Java number parser gets confused
            c = context.getSrc().read();
        }
        
        char nondigit = '\0';

        if (c == '0') {
            int startLen = context.getTokenBuffer().length();

            switch (c = context.getSrc().read()) {
                case 'x' :
                case 'X' : //  hexadecimal
                    c = context.getSrc().read();
                    if (isHexChar(c)) {
                        for (;; c = context.getSrc().read()) {
                            if (c == '_') {
                                if (nondigit != '\0') {
                                    break;
                                }
								nondigit = c;
                            } else if (isHexChar(c)) {
                                nondigit = '\0';
                                context.getTokenBuffer().append(c);
                            } else {
                                break;
                            }
                        }
                    }
                    context.getSrc().unread(c);

                    if (context.getTokenBuffer().length() == startLen) {
                        throw new SyntaxException(context.getSrc().getPosition(), "Hexadecimal number without hex-digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
                    }
                    context.setYaccValue(new Token(getInteger(context.getTokenBuffer().toString(), 16), getPositionMinusOne()));
                    return Tokens.tINTEGER;
                case 'b' :
                case 'B' : // binary
                    c = context.getSrc().read();
                    if (c == '0' || c == '1') {
                        for (;; c = context.getSrc().read()) {
                            if (c == '_') {
                                if (nondigit != '\0') {
                                    break;
                                }
								nondigit = c;
                            } else if (c == '0' || c == '1') {
                                nondigit = '\0';
                                context.getTokenBuffer().append(c);
                            } else {
                                break;
                            }
                        }
                    }
                    context.getSrc().unread(c);

                    if (context.getTokenBuffer().length() == startLen) {
                        throw new SyntaxException(context.getSrc().getPosition(), "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
                    }
                    context.setYaccValue(new Token(getInteger(context.getTokenBuffer().toString(), 2), getPositionMinusOne()));
                    return Tokens.tINTEGER;
                case 'd' :
                case 'D' : // decimal
                    c = context.getSrc().read();
                    if (Character.isDigit(c)) {
                        for (;; c = context.getSrc().read()) {
                            if (c == '_') {
                                if (nondigit != '\0') {
                                    break;
                                }
								nondigit = c;
                            } else if (Character.isDigit(c)) {
                                nondigit = '\0';
                                context.getTokenBuffer().append(c);
                            } else {
                                break;
                            }
                        }
                    }
                    context.getSrc().unread(c);

                    if (context.getTokenBuffer().length() == startLen) {
                        throw new SyntaxException(context.getSrc().getPosition(), "Binary number without digits.");
                    } else if (nondigit != '\0') {
                        throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
                    }
                    context.setYaccValue(new Token(getInteger(context.getTokenBuffer().toString(), 2), getPositionMinusOne()));
                    return Tokens.tINTEGER;
                case '0' : case '1' : case '2' : case '3' : case '4' : //Octal
                case '5' : case '6' : case '7' : case '_' : 
                    for (;; c = context.getSrc().read()) {
                        if (c == '_') {
                            if (nondigit != '\0') {
                                break;
                            }
							nondigit = c;
                        } else if (c >= '0' && c <= '7') {
                            nondigit = '\0';
                            context.getTokenBuffer().append(c);
                        } else {
                            break;
                        }
                    }
                    if (context.getTokenBuffer().length() > startLen) {
                    	context.getSrc().unread(c);

                        if (nondigit != '\0') {
                            throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
                        }

                        context.setYaccValue(new Token(getInteger(context.getTokenBuffer().toString(), 8), getPositionMinusOne()));
                        return Tokens.tINTEGER;
                    }
                case '8' :
                case '9' :
                    throw new SyntaxException(context.getSrc().getPosition(), "Illegal octal digit.");
                case '.' :
                case 'e' :
                case 'E' :
                	context.	getTokenBuffer().append('0');
                    break;
                default :
                	context.getSrc().unread(c);
                context.setYaccValue(new Token(new Long(0), getPositionMinusOne()));
                    return Tokens.tINTEGER;
            }
        }

        boolean seen_point = false;
        boolean seen_e = false;

        for (;; c = context.getSrc().read()) {
            switch (c) {
                case '0' :
                case '1' :
                case '2' :
                case '3' :
                case '4' :
                case '5' :
                case '6' :
                case '7' :
                case '8' :
                case '9' :
                    nondigit = '\0';
                    context.getTokenBuffer().append(c);
                    break;
                case '.' :
                    if (nondigit != '\0') {
                    	context.getSrc().unread(c);
                        throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
                    } else if (seen_point || seen_e) {
                    	context.getSrc().unread(c);
                        return getNumberToken(context.getTokenBuffer().toString(), true, nondigit);
                    } else {
                    	char c2;
                        if (!Character.isDigit(c2 = context.getSrc().read())) {
                        	context.getSrc().unread(c2);
                        	context.	getSrc().unread('.');
                            if (c == '_') { 
                            		// Enebo:  c can never be antrhign but '.'
                            		// Why did I put this here?
                            } else {
                            	context.setYaccValue(new Token(getInteger(context.getTokenBuffer().toString(), 10), getPositionMinusOne()));
                                return Tokens.tINTEGER;
                            }
                        } else {
                        	context.getTokenBuffer().append('.');
                        	context.getTokenBuffer().append(c2);
                            seen_point = true;
                            nondigit = '\0';
                        }
                    }
                    break;
                case 'e' :
                case 'E' :
                    if (nondigit != '\0') {
                        throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
                    } else if (seen_e) {
                    	context.getSrc().unread(c);
                        return getNumberToken(context.getTokenBuffer().toString(), true, nondigit);
                    } else {
                    	context.getTokenBuffer().append(c);
                        seen_e = true;
                        nondigit = c;
                        c = context.getSrc().read();
                        if (c == '-' || c == '+') {
                        	context.getTokenBuffer().append(c);
                            nondigit = c;
                        } else {
                        	context.getSrc().unread(c);
                        }
                    }
                    break;
                case '_' : //  '_' in number just ignored
                    if (nondigit != '\0') {
                        throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
                    }
                    nondigit = c;
                    break;
                default :
                	context.getSrc().unread(c);
                return getNumberToken(context.getTokenBuffer().toString(), seen_e || seen_point, nondigit);
            }
        }
    }

    private int getNumberToken(String number, boolean isFloat, char nondigit) {
        if (nondigit != '\0') {
            throw new SyntaxException(context.getSrc().getPosition(), "Trailing '_' in number.");
        }
        if (isFloat) {
            Double d = new Double(0.0);
            try {
                d = Double.valueOf(number);
            } catch (NumberFormatException e) {
            	context.getWarningsEncap().warn(context.getSrc().getPosition(), "Float " + number + " out of range.");
                if (number.startsWith("-")) {
                    d = new Double(Double.NEGATIVE_INFINITY);
                } else {
                    d = new Double(Double.POSITIVE_INFINITY);
                }
            }
            context.setYaccValue(new Token(d, getPositionMinusOne()));
            return Tokens.tFLOAT;
        }
        context.setYaccValue(new Token(getInteger(number, 10), getPositionMinusOne()));
		return Tokens.tINTEGER;
    }

	public void setYaccValue(Token token) {
		context.setYaccValue(token);
		
	}
}
