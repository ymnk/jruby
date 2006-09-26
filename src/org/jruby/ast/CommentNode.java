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

package org.jruby.ast;

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SourcePosition;

/**
 * Represents a comment in the ruby code.
 * 
 * @author tcorbat
 */
public class CommentNode extends Node {

	private static final long serialVersionUID = -8304070370230933044L;

	private String commentValue;

	public CommentNode(ISourcePosition position, String commentValue) {
		super(position);

		this.commentValue = commentValue;
	}

	public Instruction accept(NodeVisitor visitor) {

		return visitor.visitCommentNode(this);
	}

	public List childNodes() {

		return EMPTY_LIST;
	}

	public String getCommentValue() {

		return commentValue;
	}

	public void add(CommentNode comment) {
		StringBuffer commentString = new StringBuffer(commentValue);
		commentString.append("\n");
		commentString.append(comment.getCommentValue());
		
		commentValue = commentString.toString();
		expandPosition(comment);				
	}

	private void expandPosition(CommentNode comment) {
		ISourcePosition currentPos = getPosition();
		ISourcePosition newCommentPos = comment.getPosition();
		
		String filename = currentPos.getFile();
		int startLine = currentPos.getStartLine();
		int startOffset = currentPos.getStartOffset();
		int endLine = newCommentPos.getEndLine();
		int endOffset = newCommentPos.getEndOffset();
		
		ISourcePosition combinedPos =  new SourcePosition(filename, startLine, startOffset, endLine, endOffset);
		setPosition(combinedPos);
	}

    public String toString() {
        return "CommentNode [" + commentValue + "]";
    }
}
