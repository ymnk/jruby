package org.jruby.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;

import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.ICommentable;

public class ParserSupportForComments extends ParserSupport {
	
	public ParserSupportForComments() {
		super();
	}

	public Node introduceComment(Node node, Object[] yaccValues){
		
        if (node == null) {
            return null;
        }
        
		ArrayList yaccValueList = new ArrayList(Arrays.asList(yaccValues));
		Iterator valueItr = yaccValueList.iterator();
		
		while(valueItr.hasNext()){
			
			Object currentValue = valueItr.next();
			if((currentValue instanceof ICommentable) && ((ICommentable)currentValue).hasComments()){
				continue;
			}else{
				valueItr.remove();
			}
		}
		
		if(yaccValueList.isEmpty()){
			return node;
		}else{
			Iterator commentItr = yaccValueList.iterator();
			
			while(commentItr.hasNext()){
				ICommentable currentCommentable = (ICommentable)commentItr.next();
				if(currentCommentable.hasComments()){
					node.addComments(currentCommentable.getComments());
				}			
			}
			return node;
		}
	}
	
	public ListNode commentLastElement(ListNode node, Object[] yaccValues) {
		
		ListIterator revItr = node.reverseIterator();
		Node commentedNode = null;
		
		if(revItr.hasPrevious())
		{
			commentedNode = (Node)revItr.previous();
			revItr.remove();
		}
		else{
			return node;
		}
		
		commentedNode = introduceComment(commentedNode, yaccValues);
		
		node.add(commentedNode);
		
		return node;
	}
	
}
