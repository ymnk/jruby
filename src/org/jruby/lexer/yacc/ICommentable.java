package org.jruby.lexer.yacc;

import java.util.ArrayList;

import org.jruby.ast.CommentNode;

//tcorbat
public interface ICommentable {
	public boolean hasComments();
	public void addComment(CommentNode comment);
	public ArrayList getComments();
}
