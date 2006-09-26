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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SourcePosition;

/**
 * Decorates a Node with a <code>CommentNode</code>. It is currently not used anymore. 
 * If anyone can solve the problems in the concept of decorating node with comment this class
 * might become relevant again.
 * 
 * @author tcorbat
 * 
 */
public class CommentDecoratorNode extends Node {

	private static final long serialVersionUID = -5068698702736909498L;

	private Node decoratedNode;

	private ArrayList comments = new ArrayList();
	
	public Node unwrap() {
		return decoratedNode;
	}

	public CommentDecoratorNode(ISourcePosition position, Node decoratedNode) {
		super(position);
		this.decoratedNode = decoratedNode;
		updatePosition();

	}
	
	public Collection getComments(){
		return comments;
	}
	
	public void addComment(CommentNode comment){
		comments.add(comment);
		updatePosition();
	}
	
	public void addComments(ArrayList comments)
	{
		this.comments.addAll(comments);
		updatePosition();
	}

	
	public Node getDecoratedNode() {
		
		return decoratedNode;
	}
	
	public void setDecoratedNode(Node decoratedNode){
		this.decoratedNode = decoratedNode;
	}

	public Instruction accept(NodeVisitor visitor) {
		
		return null;
	}

	public List childNodes() {
		return Node.createList(decoratedNode);
	}
	
	private void updatePosition(){
		ISourcePosition pos = decoratedNode.getPosition();
		
		Iterator commentItr = comments.iterator();
		while(commentItr.hasNext())
		{
			ISourcePosition currentPos = ((CommentNode)commentItr.next()).getPosition();
			pos = SourcePosition.combinePosition(pos, currentPos);
		}		
		
		setPosition(pos);
	}
}
