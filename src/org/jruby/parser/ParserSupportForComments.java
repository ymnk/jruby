package org.jruby.parser;

import org.jruby.ast.ListNode;
import org.jruby.ast.Node;
import org.jruby.lexer.yacc.ICommentable;

public class ParserSupportForComments extends ParserSupport {
	public Node introduceComment(Node node, Object[] values) {
        // Enebo: Bummer that this is true
        if (node == null) {
            return null;
        }

        for (int i = 0; i < values.length; i++) {
            if (!(values[i] instanceof ICommentable)) {
                continue;
            }
            
            ICommentable commentable = (ICommentable) values[i];
            
            if (commentable.hasComments()) {
                node.addComments(commentable.getComments());
            }
        }
		
        return node;
	}
	
	public ListNode commentLastElement(ListNode node, Object[] yaccValues) {
        Node commentedNode = node.getLast();
        
        // Enebo: If no children nodes where should the comments go?
        if (commentedNode != null) {
            introduceComment(commentedNode, yaccValues);
        }
		
		return node;
	}
}
