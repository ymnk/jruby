package org.jruby.lexer.yacc;

import java.io.IOException;
import java.util.ArrayList;

import org.jruby.ast.CommentNode;
import org.jruby.parser.Tokens;

public class RubyYaccLexerForComments extends RubyYaccLexer {
	
	private LexerContext savedContext;
	
	public RubyYaccLexerForComments() {
		super();
	}
	
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
	
	protected int readComment(char c) {
        ISourcePosition startPos = getPositionMinusOne();
        StringBuffer commentText = new StringBuffer();
        
        commentText.append(c);
        c = readRestOfLine(commentText);
        context.getSrc().unread(c);
        
        context.setYaccValue(new Token(commentText.toString(), getPositionMinusOne(startPos)));
        
        if(context.isLastWasNewLine())
                return Tokens.tSOLOCOMMENT;
        return Tokens.tTAILCOMMENT;
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
}
