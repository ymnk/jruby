/*
 * Created on Sep 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.jruby.evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import org.jruby.IRuby;
import org.jruby.MetaClass;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.ast.AliasNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BignumNode;
import org.jruby.ast.BinaryOperatorNode;
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
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.NotNode;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OpAsgnNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OpElementAsgnNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.ScopeNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.SymbolNode;
import org.jruby.ast.ToAryNode;
import org.jruby.ast.UndefNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.WhenNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.JumpException.JumpType;
import org.jruby.internal.runtime.methods.DefaultMethod;
import org.jruby.internal.runtime.methods.WrapperCallable;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class EvaluationState {    
	public final IRuby runtime;
    
    public EvaluationState(IRuby runtime, IRubyObject self) {
        this(runtime);
    }
    
    public EvaluationState(IRuby runtime) {
        this.runtime = runtime;
    }
    
    public IRubyObject begin(Node node) {
        return eval(node, getThreadContext().getFrameSelf());
    }
    
    public IRubyObject eval(Node node, IRubyObject self) {
        if (node == null) return runtime.getNil();
        
        switch (node.nodeId) {
        case NodeTypes.ALIASNODE: {
            AliasNode iVisited = (AliasNode) node;
            ThreadContext tc = this.getThreadContext();

            if (tc.getRubyClass() == null) {
                throw this.runtime.newTypeError("no class to make alias");
            }

            tc.getRubyClass().defineAlias(iVisited.getNewName(), iVisited.getOldName());
            tc.getRubyClass().callMethod("method_added",
            runtime.newSymbol(iVisited.getNewName()));

            return runtime.getNil();
        }
        case NodeTypes.ANDNODE: {
            BinaryOperatorNode iVisited = (BinaryOperatorNode) node;

            // add in reverse order
            IRubyObject result = eval(iVisited.getFirstNode(), self);
            if (!result.isTrue()) return result;
            return eval(iVisited.getSecondNode(), self);
        }
        case NodeTypes.ARGSCATNODE: {
            ArgsCatNode iVisited = (ArgsCatNode) node;

            IRubyObject args = eval(iVisited.getFirstNode(), self);
            IRubyObject secondArgs = splatValue(eval(iVisited.getSecondNode(), self));
            RubyArray list = args instanceof RubyArray ? (RubyArray) args : runtime
                    .newArray(args);

            return list.concat(secondArgs); 
        }
            //                case NodeTypes.ARGSNODE:
            //                EvaluateVisitor.argsNodeVisitor.execute(this, node);
            //                break;
            //                case NodeTypes.ARGUMENTNODE:
            //                EvaluateVisitor.argumentNodeVisitor.execute(this, node);
            //                break;
        case NodeTypes.ARRAYNODE: {
            ArrayNode iVisited = (ArrayNode) node;
            IRubyObject[] array = new IRubyObject[iVisited.size()];
            int i = 0;
            for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                Node next = (Node) iterator.next();

                array[i++] = eval(next, self);
            }
            
            return runtime.newArray(array);
        }
            //                case NodeTypes.ASSIGNABLENODE:
            //                EvaluateVisitor.assignableNodeVisitor.execute(this, node);
            //                break;
        case NodeTypes.BACKREFNODE: {
            BackRefNode iVisited = (BackRefNode) node;
            IRubyObject backref = this.getThreadContext().getBackref();
            switch (iVisited.getType()) {
            case '~':
                return backref;
            case '&':
                return RubyRegexp.last_match(backref);
            case '`':
                return RubyRegexp.match_pre(backref);
            case '\'':
                return RubyRegexp.match_post(backref);
            case '+':
                return RubyRegexp.match_last(backref);
            }
            break;
        }
        case NodeTypes.BEGINNODE: {
            BeginNode iVisited = (BeginNode) node;

            return eval(iVisited.getBodyNode(), self);
        }
        case NodeTypes.BIGNUMNODE: {
            BignumNode iVisited = (BignumNode) node;
            return RubyBignum.newBignum(this.runtime, iVisited.getValue());
        }
            //                case NodeTypes.BINARYOPERATORNODE:
            //                EvaluateVisitor.binaryOperatorNodeVisitor.execute(this, node);
            //                break;
            //                case NodeTypes.BLOCKARGNODE:
            //                EvaluateVisitor.blockArgNodeVisitor.execute(this, node);
            //                break;
        case NodeTypes.BLOCKNODE: {
            BlockNode iVisited = (BlockNode) node;

            IRubyObject result = runtime.getNil();
            for (Iterator iter = iVisited.iterator(); iter.hasNext();) {
                result = eval((Node) iter.next(), self);
            }

            return result;
        }
        case NodeTypes.BLOCKPASSNODE: {
            BlockPassNode iVisited = (BlockPassNode) node;
            IRubyObject proc = eval(iVisited.getBodyNode(), self);
            ThreadContext tc = getThreadContext();

            if (proc.isNil()) {
                tc.setNoBlock();
                try {
                    return eval(iVisited.getIterNode(), self);
                } finally {
                    tc.clearNoBlock();
                }
            }

            // If not already a proc then we should try and make it one.
            if (!(proc instanceof RubyProc)) {
                proc = proc.convertToType("Proc", "to_proc", false);

                if (!(proc instanceof RubyProc)) {
                    throw runtime.newTypeError("wrong argument type "
                            + proc.getMetaClass().getName() + " (expected Proc)");
                }
            }

            // TODO: Add safety check for taintedness

            Block block = (Block) tc.getCurrentBlock();
            if (block != null) {
                IRubyObject blockObject = block.getBlockObject();
                // The current block is already associated with the proc.  No need to create new
                // block for it.  Just eval!
                if (blockObject != null && blockObject == proc) {
                    try {
                        tc.setBlockAvailable();
                        return eval(iVisited.getIterNode(), self);
                    } finally {
                        tc.clearBlockAvailable();
                    }
                }
            }

            tc.preBlockPassEval(((RubyProc) proc).getBlock());

            try {
                return eval(iVisited.getIterNode(), self);
            } finally {
                tc.postBlockPassEval();
            }
        }
        case NodeTypes.BREAKNODE: {
            BreakNode iVisited = (BreakNode) node;

            IRubyObject result = eval(iVisited.getValueNode(), self);

            JumpException je = new JumpException(JumpException.JumpType.BreakJump);

            je.setPrimaryData(result);
            je.setSecondaryData(node);

            throw je;
        }
        case NodeTypes.CALLNODE: {
            CallNode iVisited = (CallNode) node;
            ThreadContext tc = this.getThreadContext();

            tc.beginCallArgs();
            IRubyObject receiver = null;
            IRubyObject[] args = null;
            try {
                receiver = this.eval(iVisited.getReceiverNode(), self);
                args = setupArgs2(runtime, tc, iVisited.getArgsNode(), self);
            } finally {
                tc.endCallArgs();
            }
            assert receiver.getMetaClass() != null : receiver.getClass().getName();
            // If reciever is self then we do the call the same way as vcall
            CallType callType = (receiver == self ? CallType.VARIABLE : CallType.NORMAL);

            return receiver.callMethod(iVisited.getName(), args, callType);
        }
        case NodeTypes.CASENODE: {
            CaseNode iVisited = (CaseNode) node;
            IRubyObject expression = null;
            if (iVisited.getCaseNode() != null) {
                expression = eval(iVisited.getCaseNode(), self);
            }

            getThreadContext().pollThreadEvents();
            
            IRubyObject result = runtime.getNil();

            Node firstWhenNode = iVisited.getFirstWhenNode();
            while (firstWhenNode != null) {
                if (!(firstWhenNode instanceof WhenNode)) {
                    return eval(firstWhenNode, self);
                }

                WhenNode whenNode = (WhenNode) firstWhenNode;

                if (whenNode.getExpressionNodes() instanceof ArrayNode) {
                    for (Iterator iter = ((ArrayNode) whenNode.getExpressionNodes()).iterator(); iter
                            .hasNext();) {
                        Node tag = (Node) iter.next();

                        getThreadContext().setPosition(tag.getPosition());
                        if (isTrace()) {
                            callTraceFunction("line", self);
                        }

                        // Ruby grammar has nested whens in a case body because of
                        // productions case_body and when_args.
                        if (tag instanceof WhenNode) {
                            RubyArray expressions = (RubyArray) eval(((WhenNode) tag)
                                    .getExpressionNodes(), self);

                            for (int j = 0; j < expressions.getLength(); j++) {
                                IRubyObject condition = expressions.entry(j);

                                if ((expression != null && condition.callMethod("===",
                                        expression).isTrue())
                                        || (expression == null && condition.isTrue())) {
                                    return eval(((WhenNode) firstWhenNode).getBodyNode(), self);
                                }
                            }
                            continue;
                        }

                        result = eval(tag, self);

                        if ((expression != null && result.callMethod("===", expression)
                                .isTrue())
                                || (expression == null && result.isTrue())) {
                            return eval(whenNode.getBodyNode(), self);
                        }
                    }
                } else {
                    result = eval(whenNode.getExpressionNodes(), self);

                    if ((expression != null && result.callMethod("===", expression).isTrue())
                            || (expression == null && result.isTrue())) {
                        return eval(((WhenNode) firstWhenNode).getBodyNode(), self);
                    }
                }

                getThreadContext().pollThreadEvents();

                firstWhenNode = whenNode.getNextCase();
            }

            return result;
        }
        case NodeTypes.CLASSNODE: {
            ClassNode iVisited = (ClassNode) node;
            RubyClass superClass = getSuperClassFromNode(iVisited.getSuperNode(), self);
            Node classNameNode = iVisited.getCPath();
            String name = ((INameNode) classNameNode).getName();
            RubyModule enclosingClass = getEnclosingModule(classNameNode, self);
            RubyClass rubyClass = enclosingClass.defineOrGetClassUnder(name, superClass);
            ThreadContext tc = this.getThreadContext();

            if (tc.getWrapper() != null) {
                rubyClass.extendObject(tc.getWrapper());
                rubyClass.includeModule(tc.getWrapper());
            }
            return evalClassDefinitionBody(iVisited.getBodyNode(), rubyClass, self);
        }
        case NodeTypes.CLASSVARASGNNODE: {
            ClassVarAsgnNode iVisited = (ClassVarAsgnNode) node;
            IRubyObject result = eval(iVisited.getValueNode(), self);
            RubyModule rubyClass = (RubyModule) getThreadContext().peekCRef().getValue();

            if (rubyClass == null) {
                rubyClass = self.getMetaClass();
            } else if (rubyClass.isSingleton()) {
                rubyClass = (RubyModule) rubyClass.getInstanceVariable("__attached__");
            }

            rubyClass.setClassVar(iVisited.getName(), result);

            return result;
        }
        case NodeTypes.CLASSVARDECLNODE: {

            ClassVarDeclNode iVisited = (ClassVarDeclNode) node;

            // FIXME: shouldn't we use cref here?
            if (this.getThreadContext().getRubyClass() == null) {
                throw runtime.newTypeError("no class/module to define class variable");
            }
            IRubyObject result = eval(iVisited.getValueNode(), self);
            ((RubyModule) getThreadContext().peekCRef().getValue()).setClassVar(iVisited
                    .getName(), result);

            return runtime.getNil();
        }
        case NodeTypes.CLASSVARNODE: {
            ClassVarNode iVisited = (ClassVarNode) node;
            RubyModule rubyClass = this.getThreadContext().getRubyClass();

            if (rubyClass == null) {
                return self.getMetaClass().getClassVar(iVisited.getName());
            } else if (!rubyClass.isSingleton()) {
                return rubyClass.getClassVar(iVisited.getName());
            } else {
                RubyModule module = (RubyModule) rubyClass.getInstanceVariable("__attached__");

                if (module != null) {
                    return module.getClassVar(iVisited.getName());
                }
            }
            break;
        }
        case NodeTypes.COLON2NODE: {
            Colon2Node iVisited = (Colon2Node) node;
            Node leftNode = iVisited.getLeftNode();

            // TODO: Made this more colon3 friendly because of cpath production
            // rule in grammar (it is convenient to think of them as the same thing
            // at a grammar level even though evaluation is).
            if (leftNode == null) {
                return runtime.getObject().getConstantFrom(iVisited.getName());
            } else {
                IRubyObject result = eval(iVisited.getLeftNode(), self);
                if (result instanceof RubyModule) {
                    return ((RubyModule) result).getConstantFrom(iVisited.getName());
                } else {
                    return result.callMethod(iVisited.getName());
                }
            }
        }
        case NodeTypes.COLON3NODE: {
            Colon3Node iVisited = (Colon3Node) node;
            return runtime.getObject().getConstantFrom(iVisited.getName());
        }
        case NodeTypes.CONSTDECLNODE: {
            ConstDeclNode iVisited = (ConstDeclNode) node;

            IRubyObject result = eval(iVisited.getValueNode(), self);
            IRubyObject module;

            if (iVisited.getPathNode() != null) {
                module = eval(iVisited.getPathNode(), self);
            } else {
                ThreadContext tc = getThreadContext();

                // FIXME: why do we check RubyClass and then use CRef?
                if (tc.getRubyClass() == null) {
                    // TODO: wire into new exception handling mechanism
                    throw runtime.newTypeError("no class/module to define constant");
                }
                module = (RubyModule) tc.peekCRef().getValue();
            }

            // FIXME: shouldn't we use the result of this set in setResult?
            ((RubyModule) module).setConstant(iVisited.getName(), result);

            return result;
        }
        case NodeTypes.CONSTNODE: {
            ConstNode iVisited = (ConstNode) node;
            return getThreadContext().getConstant(iVisited.getName());
        }
        case NodeTypes.DASGNNODE: {
            DAsgnNode iVisited = (DAsgnNode) node;

            IRubyObject result = eval(iVisited.getValueNode(), self);
            getThreadContext().getCurrentDynamicVars().set(iVisited.getName(), result);

            return result;
        }
        case NodeTypes.DEFINEDNODE: {
            DefinedNode iVisited = (DefinedNode) node;
            String def = new DefinedVisitor(this).getDefinition(iVisited.getExpressionNode());
            if (def != null) {
                return runtime.newString(def);
            } else {
                return runtime.getNil();
            }
        }
        case NodeTypes.DEFNNODE: {
            DefnNode iVisited = (DefnNode) node;
            ThreadContext tc = getThreadContext();
            RubyModule containingClass = tc.getRubyClass();

            if (containingClass == null) {
                throw runtime.newTypeError("No class to add method.");
            }

            String name = iVisited.getName();
            if (containingClass == runtime.getObject() && name.equals("initialize")) {
                runtime.getWarnings().warn(
                        "redefining Object#initialize may cause infinite loop");
            }

            Visibility visibility = tc.getCurrentVisibility();
            if (name.equals("initialize") || visibility.isModuleFunction()) {
                visibility = Visibility.PRIVATE;
            }

            DefaultMethod newMethod = new DefaultMethod(containingClass,
                    iVisited.getBodyNode(), (ArgsNode) iVisited.getArgsNode(), visibility, tc
                            .peekCRef());

            iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));

            containingClass.addMethod(name, newMethod);

            if (tc.getCurrentVisibility().isModuleFunction()) {
                containingClass.getSingletonClass().addMethod(
                        name,
                        new WrapperCallable(containingClass.getSingletonClass(), newMethod,
                                Visibility.PUBLIC));
                containingClass.callMethod("singleton_method_added", runtime.newSymbol(name));
            }

            // 'class << state.self' and 'class << obj' uses defn as opposed to defs
            if (containingClass.isSingleton()) {
                ((MetaClass) containingClass).getAttachedObject().callMethod(
                        "singleton_method_added", runtime.newSymbol(iVisited.getName()));
            } else {
                containingClass.callMethod("method_added", runtime.newSymbol(name));
            }

            return runtime.getNil();
        }
        case NodeTypes.DEFSNODE: {
            DefsNode iVisited = (DefsNode) node;
            IRubyObject receiver = eval(iVisited.getReceiverNode(), self);

            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw runtime.newSecurityError("Insecure; can't define singleton method.");
            }
            if (receiver.isFrozen()) {
                throw runtime.newFrozenError("object");
            }
            if (!receiver.singletonMethodsAllowed()) {
                throw runtime.newTypeError("can't define singleton method \""
                        + iVisited.getName() + "\" for " + receiver.getType());
            }

            RubyClass rubyClass = receiver.getSingletonClass();

            if (runtime.getSafeLevel() >= 4) {
                ICallable method = (ICallable) rubyClass.getMethods().get(iVisited.getName());
                if (method != null) {
                    throw runtime.newSecurityError("Redefining method prohibited.");
                }
            }

            DefaultMethod newMethod = new DefaultMethod(rubyClass, iVisited.getBodyNode(),
                    (ArgsNode) iVisited.getArgsNode(), Visibility.PUBLIC, getThreadContext()
                            .peekCRef());

            iVisited.getBodyNode().accept(new CreateJumpTargetVisitor(newMethod));

            rubyClass.addMethod(iVisited.getName(), newMethod);
            receiver
                    .callMethod("singleton_method_added", runtime.newSymbol(iVisited.getName()));

            return runtime.getNil();
        }
        case NodeTypes.DOTNODE: {
            DotNode iVisited = (DotNode) node;
            return RubyRange.newRange(runtime, eval(iVisited.getBeginNode(), self), eval(
                    iVisited.getEndNode(), self), iVisited.isExclusive());
        }
        case NodeTypes.DREGEXPNODE: {
            DRegexpNode iVisited = (DRegexpNode) node;

            StringBuffer sb = new StringBuffer();
            for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                Node iterNode = (Node) iterator.next();

                sb.append(eval(iterNode, self).toString());
            }

            return RubyRegexp.newRegexp(runtime, sb.toString(), iVisited.getOptions(), null);
        }
        case NodeTypes.DSTRNODE: {
            DStrNode iVisited = (DStrNode) node;

            StringBuffer sb = new StringBuffer();
            for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                Node iterNode = (Node) iterator.next();

                sb.append(eval(iterNode, self).toString());
            }

            return runtime.newString(sb.toString());
        }
        case NodeTypes.DSYMBOLNODE: {
            DSymbolNode iVisited = (DSymbolNode) node;

            StringBuffer sb = new StringBuffer();
            for (Iterator iterator = iVisited.getNode().iterator(); iterator.hasNext();) {
                Node iterNode = (Node) iterator.next();

                sb.append(eval(iterNode, self).toString());
            }

            return runtime.newSymbol(sb.toString());
        }
        case NodeTypes.DVARNODE: {
            DVarNode iVisited = (DVarNode) node;
            return getThreadContext().getDynamicValue(iVisited.getName());
        }
        case NodeTypes.DXSTRNODE: {
            DXStrNode iVisited = (DXStrNode) node;

            StringBuffer sb = new StringBuffer();
            for (Iterator iterator = iVisited.iterator(); iterator.hasNext();) {
                Node iterNode = (Node) iterator.next();

                sb.append(eval(iterNode, self).toString());
            }

            return self.callMethod("`", runtime.newString(sb.toString()));
        }
        case NodeTypes.ENSURENODE: {
            EnsureNode iVisited = (EnsureNode) node;

            // save entering the try if there's nothing to ensure
            if (iVisited.getEnsureNode() != null) {
                try {
                    return eval(iVisited.getBodyNode(), self);
                } finally {
                    return eval(iVisited.getEnsureNode(), self);
                }
            }

            return eval(iVisited.getBodyNode(), self);
        }
        case NodeTypes.EVSTRNODE: {
            EvStrNode iVisited = (EvStrNode) node;

            return eval(iVisited.getBody(), self);
        }
        case NodeTypes.FALSENODE: {
            getThreadContext().pollThreadEvents();
            return runtime.getFalse();
        }
        case NodeTypes.FCALLNODE: {
            FCallNode iVisited = (FCallNode) node;
            ThreadContext tc = getThreadContext();

            tc.beginCallArgs();
            IRubyObject[] args = null;
            try {
                args = setupArgs2(runtime, tc, iVisited.getArgsNode(), self);
            } finally {
                tc.endCallArgs();
            }

            return self.callMethod(iVisited.getName(), args, CallType.FUNCTIONAL);
        }
        case NodeTypes.FIXNUMNODE: {
            FixnumNode iVisited = (FixnumNode) node;
            return runtime.newFixnum(iVisited.getValue());
        }
        case NodeTypes.FLIPNODE: {
            FlipNode iVisited = (FlipNode) node;
            ThreadContext tc = runtime.getCurrentContext();
            if (iVisited.isExclusive()) {
                if (!tc.getFrameScope().getValue(iVisited.getCount()).isTrue()) {
                    //Benoit: I don't understand why the result is inversed
                    IRubyObject result = eval(iVisited.getBeginNode(), self).isTrue() ? runtime
                            .getFalse() : runtime.getTrue();
                    tc.getFrameScope().setValue(iVisited.getCount(), result);
                } else {
                    if (eval(iVisited.getEndNode(), self).isTrue()) {
                        tc.getFrameScope().setValue(iVisited.getCount(), runtime.getFalse());
                        return runtime.getFalse();
                    }
                    return runtime.getTrue();
                }
            } else {
                if (!tc.getFrameScope().getValue(iVisited.getCount()).isTrue()) {
                    if (eval(iVisited.getBeginNode(), self).isTrue()) {
                        //Benoit: I don't understand why the result is inversed
                        tc.getFrameScope().setValue(
                                iVisited.getCount(),
                                eval(iVisited.getEndNode(), self).isTrue() ? runtime.getFalse()
                                        : runtime.getTrue());
                        return runtime.getTrue();
                    } else {
                        return runtime.getFalse();
                    }
                } else {
                    if (eval(iVisited.getEndNode(), self).isTrue()) {
                        tc.getFrameScope().setValue(iVisited.getCount(), runtime.getFalse());
                        return runtime.getFalse();
                    }
                    return runtime.getTrue();
                }
            }
            break;
        }
        case NodeTypes.FLOATNODE: {
            FloatNode iVisited = (FloatNode) node;
            return RubyFloat.newFloat(runtime, iVisited.getValue());
        }
        case NodeTypes.FORNODE: {
            ForNode iVisited = (ForNode) node;
            ThreadContext tc = getThreadContext();

            tc.preForLoopEval(Block.createBlock(iVisited.getVarNode(), iVisited.getCallable(),
                    self));

            try {
                while (true) {
                    try {
                        ISourcePosition position = tc.getPosition();
                        tc.beginCallArgs();

                        IRubyObject recv = null;
                        try {
                            recv = eval(iVisited.getIterNode(), self);
                        } finally {
                            tc.setPosition(position);
                            tc.endCallArgs();
                        }

                        return recv.callMethod("each", IRubyObject.NULL_ARRAY, CallType.NORMAL);
                    } catch (JumpException je) {
                        if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                            // do nothing, allow loop to retry
                        } else {
                            throw je;
                        }
                    }
                }
            } catch (JumpException je) {
                if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                    IRubyObject breakValue = (IRubyObject) je.getPrimaryData();

                    return breakValue == null ? runtime.getNil() : breakValue;
                } else {
                    throw je;
                }
            } finally {
                tc.postForLoopEval();
            }
        }
        case NodeTypes.GLOBALASGNNODE: {
            GlobalAsgnNode iVisited = (GlobalAsgnNode) node;
            runtime.getGlobalVariables().set(iVisited.getName(),
                    eval(iVisited.getValueNode(), self));
            break;
        }
        case NodeTypes.GLOBALVARNODE: {
            GlobalVarNode iVisited = (GlobalVarNode) node;
            return runtime.getGlobalVariables().get(iVisited.getName());
        }
        case NodeTypes.HASHNODE: {
            HashNode iVisited = (HashNode) node;

            Map hash = null;
            if (iVisited.getListNode() != null) {
                hash = new HashMap(iVisited.getListNode().size() / 2);
                
                for (Iterator iterator = iVisited.getListNode().iterator(); iterator.hasNext();) {
                    // insert all nodes in sequence, hash them in the final instruction
                    // KEY
                    IRubyObject key = eval((Node) iterator.next(), self);
                    IRubyObject value = eval((Node) iterator.next(), self);

                    hash.put(key, value);
                }
            }

            if (hash == null) {
                return RubyHash.newHash(runtime);
            }
        
            return RubyHash.newHash(runtime, hash, runtime.getNil());
        }
        case NodeTypes.IFNODE: {
            IfNode iVisited = (IfNode) node;
            IRubyObject result = eval(iVisited.getCondition(), self);

            if (result.isTrue()) {
                if (iVisited.getThenBody() != null) {
                    return eval(iVisited.getThenBody(), self);
                }
            } else {
                if (iVisited.getElseBody() != null) {
                    return eval(iVisited.getElseBody(), self);
                }
            }
            return runtime.getNil();
        }
        case NodeTypes.INSTASGNNODE: {
            InstAsgnNode iVisited = (InstAsgnNode) node;

            IRubyObject result = eval(iVisited.getValueNode(), self);
            self.setInstanceVariable(iVisited.getName(), result);

            return result;
        }
        case NodeTypes.INSTVARNODE: {
            InstVarNode iVisited = (InstVarNode) node;
            IRubyObject variable = self.getInstanceVariable(iVisited.getName());

            return variable == null ? runtime.getNil() : variable;
        }
            //                case NodeTypes.ISCOPINGNODE:
            //                EvaluateVisitor.iScopingNodeVisitor.execute(this, node);
            //                break;
        case NodeTypes.ITERNODE: {
            IterNode iVisited = (IterNode) node;
            ThreadContext tc = getThreadContext();

            tc.preIterEval(Block.createBlock(iVisited.getVarNode(), iVisited.getCallable(),
                    self));
            try {
                while (true) {
                    try {
                        tc.setBlockAvailable();
                        return eval(iVisited.getIterNode(), self);
                    } catch (JumpException je) {
                        if (je.getJumpType() == JumpException.JumpType.RetryJump) {
                            // allow loop to retry
                        } else {
                            throw je;
                        }
                    } finally {
                        tc.clearBlockAvailable();
                    }
                }
            } catch (JumpException je) {
                if (je.getJumpType() == JumpException.JumpType.BreakJump) {
                    IRubyObject breakValue = (IRubyObject) je.getPrimaryData();

                    return breakValue == null ? runtime.getNil() : breakValue;
                } else {
                    throw je;
                }
            } finally {
                tc.postIterEval();
            }
        }
            //                case NodeTypes.LISTNODE:
            //                EvaluateVisitor.listNodeVisitor.execute(this, node);
            //                break;
        case NodeTypes.LOCALASGNNODE: {
            LocalAsgnNode iVisited = (LocalAsgnNode) node;
            IRubyObject result = eval(iVisited.getValueNode(), self);
            getThreadContext().getFrameScope().setValue(iVisited.getCount(), result);
            return result;
        }
        case NodeTypes.LOCALVARNODE: {
            LocalVarNode iVisited = (LocalVarNode) node;
            return runtime.getCurrentContext().getFrameScope().getValue(iVisited.getCount());
        }
        case NodeTypes.MATCH2NODE: {
            Match2Node iVisited = (Match2Node) node;
            IRubyObject recv = eval(iVisited.getReceiverNode(), self);
            IRubyObject value = eval(iVisited.getValueNode(), self);

            return ((RubyRegexp) recv).match(value);
        }
        case NodeTypes.MATCH3NODE: {
            Match3Node iVisited = (Match3Node) node;
            IRubyObject recv = eval(iVisited.getReceiverNode(), self);
            IRubyObject value = eval(iVisited.getValueNode(), self);

            if (value instanceof RubyString) {
                return ((RubyRegexp) recv).match(value);
            } else {
                return value.callMethod("=~", recv);
            }
        }
        case NodeTypes.MATCHNODE: {
            MatchNode iVisited = (MatchNode) node;
            return ((RubyRegexp) eval(iVisited.getRegexpNode(), self)).match2();
        }
        case NodeTypes.MODULENODE: {
            ModuleNode iVisited = (ModuleNode) node;
            Node classNameNode = iVisited.getCPath();
            String name = ((INameNode) classNameNode).getName();
            RubyModule enclosingModule = getEnclosingModule(classNameNode, self);

            if (enclosingModule == null) {
                throw runtime.newTypeError("no outer class/module");
            }

            RubyModule module;
            if (enclosingModule == runtime.getObject()) {
                module = runtime.getOrCreateModule(name);
            } else {
                module = enclosingModule.defineModuleUnder(name);
            }
            return evalClassDefinitionBody(iVisited.getBodyNode(), module, self);
        }
            case NodeTypes.MULTIPLEASGNNODE: {
                MultipleAsgnNode iVisited = (MultipleAsgnNode)node;
                return new AssignmentVisitor(this, self).assign(iVisited, eval(iVisited.getValueNode(), self), false);
            }
            case NodeTypes.NEWLINENODE: {
                NewlineNode iVisited = (NewlineNode)node;
                
                // something in here is used to build up ruby stack trace...
                getThreadContext().setPosition(iVisited.getPosition());
                
                if (isTrace()) {
                   callTraceFunction("line", self);
                }
                
                // TODO: do above but not below for additional newline nodes
                return eval(iVisited.getNextNode(), self);
            }
            case NodeTypes.NEXTNODE: {
                NextNode iVisited = (NextNode)node;

                getThreadContext().pollThreadEvents();
                
                IRubyObject result = eval(iVisited.getValueNode(), self);

                // now used as an interpreter event
                JumpException je = new JumpException(JumpException.JumpType.NextJump);
                
                je.setPrimaryData(result);
                je.setSecondaryData(iVisited);

                //state.setCurrentException(je);
                throw je;
            }
            case NodeTypes.NILNODE:
                return runtime.getNil();
//                case NodeTypes.NODETYPES:
//                EvaluateVisitor.nodeTypesVisitor.execute(this, node);
//                break;
            case NodeTypes.NOTNODE: {
                NotNode iVisited = (NotNode)node;
                
                IRubyObject result = eval(iVisited.getConditionNode(), self);
                return result.isTrue() ? runtime.getFalse() : runtime.getTrue();
            }
            case NodeTypes.NTHREFNODE: {
                NthRefNode iVisited = (NthRefNode)node;
                return RubyRegexp.nth_match(iVisited.getMatchNumber(), getThreadContext().getBackref());
            }
            case NodeTypes.OPASGNANDNODE: {
                BinaryOperatorNode iVisited = (BinaryOperatorNode) node;

                // add in reverse order
                IRubyObject result = eval(iVisited.getFirstNode(), self);
                if (!result.isTrue()) return result;
                return eval(iVisited.getSecondNode(), self);
            }
            case NodeTypes.OPASGNNODE: {
                OpAsgnNode iVisited = (OpAsgnNode)node;
                IRubyObject receiver = eval(iVisited.getReceiverNode(), self);
                IRubyObject value = receiver.callMethod(iVisited.getVariableName());

                if (iVisited.getOperatorName().equals("||")) {
                    if (value.isTrue()) {
                        return value;
                    }
                    value = eval(iVisited.getValueNode(), self);
                } else if (iVisited.getOperatorName().equals("&&")) {
                    if (!value.isTrue()) {
                        return value;
                    }
                    value = eval(iVisited.getValueNode(), self);
                } else {
                    value = value.callMethod(iVisited.getOperatorName(), eval(iVisited.getValueNode(), self));
                }

                receiver.callMethod(iVisited.getVariableName() + "=", value);

                getThreadContext().pollThreadEvents();
                
                return value;
            }
            case NodeTypes.OPASGNORNODE: {
                OpAsgnOrNode iVisited = (OpAsgnOrNode) node;
                String def = new DefinedVisitor(this).getDefinition(iVisited.getFirstNode());
                
                IRubyObject result = runtime.getNil();
                if (def != null) {
                    result = eval(iVisited.getFirstNode(), self);
                }
                if (!result.isTrue()) {
                    result = eval(iVisited.getSecondNode(), self);
                }
                
                return result;
            }
            case NodeTypes.OPELEMENTASGNNODE: {
                OpElementAsgnNode iVisited = (OpElementAsgnNode)node;
                IRubyObject receiver = eval(iVisited.getReceiverNode(), self);

                IRubyObject[] args = setupArgs2(runtime, getThreadContext(), iVisited.getArgsNode(), self);

                IRubyObject firstValue = receiver.callMethod("[]", args);

                if (iVisited.getOperatorName().equals("||")) {
                    if (firstValue.isTrue()) {
                        return firstValue;
                    }
                    firstValue = eval(iVisited.getValueNode(), self);
                } else if (iVisited.getOperatorName().equals("&&")) {
                    if (!firstValue.isTrue()) {
                        return firstValue;
                    }
                    firstValue = eval(iVisited.getValueNode(), self);
                } else {
                    firstValue = firstValue.callMethod(iVisited.getOperatorName(), eval(iVisited.getValueNode(), self));
                }

                IRubyObject[] expandedArgs = new IRubyObject[args.length + 1];
                System.arraycopy(args, 0, expandedArgs, 0, args.length);
                expandedArgs[expandedArgs.length - 1] = firstValue;
                return receiver.callMethod("[]=", expandedArgs);
            }
            case NodeTypes.OPTNNODE: {
                OptNNode iVisited = (OptNNode)node;

                IRubyObject result = runtime.getNil();
                while (RubyKernel.gets(runtime.getTopSelf(), IRubyObject.NULL_ARRAY).isTrue()) {
                    loop: while (true) { // Used for the 'redo' command
                        try {
                            result = eval(iVisited.getBodyNode(), self);
                            break;
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.REDO:
                                // do nothing, this iteration restarts
                                break;
                            case JumpType.NEXT:
                                // recheck condition
                                break loop;
                            case JumpType.BREAK:
                                // end loop
                                return (IRubyObject)je.getPrimaryData();
                            default:
                                throw je;
                            }
                        }
                    }
                }
            }
            case NodeTypes.ORNODE: {
                OrNode iVisited = (OrNode) node;
                
                IRubyObject result = eval(iVisited.getFirstNode(), self);
                
                if (!result.isTrue()) {
                    result = eval(iVisited.getSecondNode(), self);
                }
                
                return result;
            }
//                case NodeTypes.POSTEXENODE:
//                EvaluateVisitor.postExeNodeVisitor.execute(this, node);
//                break;
            case NodeTypes.REDONODE: {
                getThreadContext().pollThreadEvents();
                
                // now used as an interpreter event
                JumpException je = new JumpException(JumpException.JumpType.RedoJump);
                
                je.setSecondaryData(node);
                
                throw je;
            }
            case NodeTypes.REGEXPNODE: {
                RegexpNode iVisited = (RegexpNode)node;
                
                // FIXME: don't pass null
                return RubyRegexp.newRegexp(runtime, iVisited.getPattern(), null);
            }
            case NodeTypes.RESCUEBODYNODE: {
                RescueBodyNode iVisited = (RescueBodyNode)node;
                if (iVisited.getBodyNode() != null) {
                    return eval(iVisited.getBodyNode(), self);
                }
            }
            case NodeTypes.RESCUENODE: {
                RescueNode iVisited = (RescueNode)node;

                IRubyObject result = runtime.getNil();
                RescuedBlock : while (true) {
                    try {
                        // Execute rescue block
                        result = eval(iVisited.getBodyNode(), self);

                        // If no exception is thrown execute else block
                        if (iVisited.getElseNode() != null) {
                            result = eval(iVisited.getElseNode(), self);
                        }

                        return result;
                    } catch (RaiseException raiseJump) {
                        // TODO: Rubicon TestKernel dies without this line.  A cursory glance implies we
                        // falsely set $! to nil and this sets it back to something valid.  This should 
                        // get fixed at the same time we address bug #1296484.
                        runtime.getGlobalVariables().set("$!", raiseJump.getException());

                        RescueBodyNode rescueNode = iVisited.getRescueNode();

                        while (rescueNode != null) {
                            Node  exceptionNodes = rescueNode.getExceptionNodes();
                            ListNode exceptionNodesList;
                            
                            if (exceptionNodes instanceof SplatNode) {                    
                                exceptionNodesList = (ListNode) eval(exceptionNodes, self);
                            } else {
                                exceptionNodesList = (ListNode) exceptionNodes;
                            }
                            
                            if (isRescueHandled(raiseJump.getException(), exceptionNodesList, self)) {
                                try {
                                    return eval(rescueNode, self);
                                } catch (JumpException je) {
                                    if (je.getJumpType().getTypeId() == JumpType.RETRY) {
                                        runtime.getGlobalVariables().set("$!", runtime.getNil());
                                        continue RescuedBlock;
                                    } else {
                                        throw je;
                                    }
                                }
                            }
                            
                            rescueNode = rescueNode.getOptRescueNode();
                        }

                        throw raiseJump;
                    } finally {
                        runtime.getGlobalVariables().set("$!", runtime.getNil());
                    }
                }
            }
            case NodeTypes.RETRYNODE: {
                getThreadContext().pollThreadEvents();
                
                JumpException je = new JumpException(JumpException.JumpType.RetryJump);
                
                throw je;
            }
            case NodeTypes.RETURNNODE: {
                ReturnNode iVisited = (ReturnNode)node;
                
                IRubyObject result = eval(iVisited.getValueNode(), self);
                
                JumpException je = new JumpException(JumpException.JumpType.ReturnJump);
                    
                je.setPrimaryData(iVisited.getTarget());
                je.setSecondaryData(result);
                je.setTertiaryData(iVisited);

                throw je;
            }
            case NodeTypes.SCLASSNODE: {
                SClassNode iVisited = (SClassNode)node;
                IRubyObject receiver = eval(iVisited.getReceiverNode(), self);

                RubyClass singletonClass;

                if (receiver.isNil()) {
                    singletonClass = runtime.getNilClass();
                } else if (receiver == runtime.getTrue()) {
                    singletonClass = runtime.getClass("True");
                } else if (receiver == runtime.getFalse()) {
                    singletonClass = runtime.getClass("False");
                } else {
                    if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                        throw runtime.newSecurityError("Insecure: can't extend object.");
                    }

                    singletonClass = receiver.getSingletonClass();
                }
                
                ThreadContext tc = getThreadContext();
                
                if (tc.getWrapper() != null) {
                    singletonClass.extendObject(tc.getWrapper());
                    singletonClass.includeModule(tc.getWrapper());
                }

                return evalClassDefinitionBody(iVisited.getBodyNode(), singletonClass, self);
            }
            case NodeTypes.SCOPENODE: {
                ScopeNode iVisited = (ScopeNode)node;
                ThreadContext tc = getThreadContext();
                
                tc.preScopedBody(iVisited.getLocalNames());
                try {
                    return eval(iVisited.getBodyNode(), self);
                } finally {
                    tc.postScopedBody();
                }
            }
            case NodeTypes.SELFNODE:
                return self;
            case NodeTypes.SPLATNODE: {
                SplatNode iVisited = (SplatNode)node;
                return splatValue(eval(iVisited.getValue(), self));
            }
////                case NodeTypes.STARNODE:
////                EvaluateVisitor.starNodeVisitor.execute(this, node);
////                break;
            case NodeTypes.STRNODE: {
                StrNode iVisited = (StrNode)node;
                return runtime.newString(iVisited.getValue());
            }
            case NodeTypes.SUPERNODE: {
                SuperNode iVisited = (SuperNode)node;
                ThreadContext tc = getThreadContext();
                
                if (tc.getFrameLastClass() == null) {
                    throw runtime.newNameError("Superclass method '" + tc.getFrameLastFunc() + "' disabled.");
                }

                tc.beginCallArgs();

                IRubyObject[] args = null;
                try {
                    args = setupArgs(runtime, tc, iVisited.getArgsNode(), self);
                } finally {
                    tc.endCallArgs();
                }
                return tc.callSuper(args);
            }
            case NodeTypes.SVALUENODE: {
                SValueNode iVisited = (SValueNode)node;
                return aValueSplat(eval(iVisited.getValue(), self));
            }
            case NodeTypes.SYMBOLNODE: {
                SymbolNode iVisited = (SymbolNode)node;
                return runtime.newSymbol(iVisited.getName());
            }
            case NodeTypes.TOARYNODE: {
                ToAryNode iVisited = (ToAryNode)node;
                return aryToAry(eval(iVisited.getValue(), self));
            }
            case NodeTypes.TRUENODE: {
                return runtime.getTrue();
            }
            case NodeTypes.UNDEFNODE: {
                UndefNode iVisited = (UndefNode)node;
                ThreadContext tc = getThreadContext();
                
                if (tc.getRubyClass() == null) {
                    throw runtime.newTypeError("No class to undef method '" + iVisited.getName() + "'.");
                }
                tc.getRubyClass().undef(iVisited.getName());
                
                return runtime.getNil();
            }
            case NodeTypes.UNTILNODE: {
                UntilNode iVisited = (UntilNode)node;
                
                while (!eval(iVisited.getConditionNode(), self).isTrue()) {
                    loop: while (true) { // Used for the 'redo' command
                        try {
                            return eval(iVisited.getBodyNode(), self);
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.REDO:
                                continue;
                            case JumpType.NEXT:
                                break loop;
                            case JumpType.BREAK:
                                return (IRubyObject)je.getPrimaryData();
                            default:
                                throw je;
                            }
                        }
                    }
                }
            }
            case NodeTypes.VALIASNODE: {
                VAliasNode iVisited = (VAliasNode)node;
                runtime.getGlobalVariables().alias(iVisited.getNewName(), iVisited.getOldName());
                
                return runtime.getNil();
            }
            case NodeTypes.VCALLNODE: {
                VCallNode iVisited = (VCallNode)node;
                return self.callMethod(iVisited.getMethodName(), IRubyObject.NULL_ARRAY, CallType.VARIABLE);
            }
            case NodeTypes.WHENNODE:
                assert false;
                return null;
            case NodeTypes.WHILENODE: {
                WhileNode iVisited = (WhileNode)node;
                
                while (eval(iVisited.getConditionNode(), self).isTrue()) {
                    loop: while (true) { // Used for the 'redo' command
                        try {
                            return eval(iVisited.getBodyNode(), self);
                        } catch (JumpException je) {
                            switch (je.getJumpType().getTypeId()) {
                            case JumpType.REDO:
                                continue;
                            case JumpType.NEXT:
                                break loop;
                            case JumpType.BREAK:
                                return (IRubyObject)je.getPrimaryData();
                            default:
                                throw je;
                            }
                        }
                    }
                }
            }
            case NodeTypes.XSTRNODE: {
                XStrNode iVisited = (XStrNode)node;
                return self.callMethod("`", runtime.newString(iVisited.getValue()));
            }
            case NodeTypes.YIELDNODE: {
                YieldNode iVisited = (YieldNode)node;
                
                IRubyObject result = eval(iVisited.getArgsNode(), self);
                if (iVisited.getArgsNode() != null) {
                    result = null;
                }
                    
                result = getThreadContext().yieldCurrentBlock(result, null, null, iVisited.getCheckState());
            }
            case NodeTypes.ZARRAYNODE: {
                return runtime.newArray();
            }
            case NodeTypes.ZSUPERNODE: {
                ThreadContext tc = getThreadContext();
                
                if (tc.getFrameLastClass() == null) {
                    throw runtime.newNameError("superclass method '" + tc.getFrameLastFunc() + "' disabled");
                }

                return tc.callSuper(tc.getFrameArgs());
            }
        }
    
        return runtime.getNil();
    }

    private IRubyObject aryToAry(IRubyObject value) {
        if (value instanceof RubyArray) {
            return value;
        }
        
        if (value.respondsTo("to_ary")) {
            return value.convertToType("Array", "to_ary", false);
        }
        
        return runtime.newArray(value);
    }

    /** Evaluates the body in a class or module definition statement.
     *
     */
    private IRubyObject evalClassDefinitionBody(ScopeNode iVisited, RubyModule type, IRubyObject self) {
        ThreadContext tc = getThreadContext();
        
        tc.preClassEval(iVisited.getLocalNames(), type);

        try {
            if (isTrace()) {
                callTraceFunction("class", type);
            }

            return eval(iVisited.getBodyNode(), type);
        } finally {
            tc.postClassEval();

            if (isTrace()) {
                callTraceFunction("end", null);
            }
        }
    }
    
    private RubyClass getSuperClassFromNode(Node superNode, IRubyObject self) {
        if (superNode == null) {
            return null;
        }
        RubyClass superClazz;
        try {
            superClazz = (RubyClass) eval(superNode, self);
        } catch (Exception e) {
            if (superNode instanceof INameNode) {
                String name = ((INameNode) superNode).getName();
                throw runtime.newTypeError("undefined superclass '" + name + "'");
            }
            throw runtime.newTypeError("superclass undefined");
        }
        if (superClazz instanceof MetaClass) {
            throw runtime.newTypeError("can't make subclass of virtual class");
        }
        return superClazz;
    }

    /**
     * Helper method.
     *
     * test if a trace function is avaiable.
     *
     */
    private boolean isTrace() {
        return runtime.getTraceFunction() != null;
    }

    private void callTraceFunction(String event, IRubyObject zelf) {
        ThreadContext tc = getThreadContext();
        String name = tc.getFrameLastFunc();
        RubyModule type = tc.getFrameLastClass();
        runtime.callTraceFunction(event, tc.getPosition(), zelf, name, type);
    }
    
    private IRubyObject splatValue(IRubyObject value) {
        if (value.isNil()) {
            return runtime.newArray(value);
        }
        
        return arrayValue(value);
    }

    private IRubyObject aValueSplat(IRubyObject value) {
        if (!(value instanceof RubyArray) ||
            ((RubyArray) value).length().getLongValue() == 0) {
            return runtime.getNil();
        }
        
        RubyArray array = (RubyArray) value;
        
        return array.getLength() == 1 ? array.first(IRubyObject.NULL_ARRAY) : array;
    }

    private RubyArray arrayValue(IRubyObject value) {
        IRubyObject newValue = value.convertToType("Array", "to_ary", false);

        if (newValue.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can 
            // remove this hack too.
            if (value.getType().searchMethod("to_a").getImplementationClass() != runtime.getKernel()) {
                newValue = value.convertToType("Array", "to_a", false);
                if(newValue.getType() != runtime.getClass("Array")) {
                    throw runtime.newTypeError("`to_a' did not return Array");
                }
            } else {
                newValue = runtime.newArray(value);
            }
        }
        
        return (RubyArray) newValue;
    }
    
    private IRubyObject[] setupArgs(IRuby runtime, ThreadContext context, Node node, IRubyObject self) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }

        if (node instanceof ArrayNode) {
            ISourcePosition position = context.getPosition();
            ArrayList list = new ArrayList(((ArrayNode) node).size());
            
            for (Iterator iter=((ArrayNode)node).iterator(); iter.hasNext();){
                final Node next = (Node) iter.next();
                if (next instanceof SplatNode) {
                    list.addAll(((RubyArray) eval(next, self)).getList());
                } else {
                    list.add(eval(next, self));
                }
            }

            context.setPosition(position);

            return (IRubyObject[]) list.toArray(new IRubyObject[list.size()]);
        }

        return ArgsUtil.arrayify(eval(node, self));
    }

    private RubyModule getEnclosingModule(Node node, IRubyObject self) {
        RubyModule enclosingModule = null;
        
        if (node instanceof Colon2Node) {
            IRubyObject result = eval(((Colon2Node) node).getLeftNode(), self);
            
            if (result != null && !result.isNil()) {
                enclosingModule = (RubyModule) result;
            }
        } else if (node instanceof Colon3Node) {
            enclosingModule = runtime.getObject(); 
        }
        
        if (enclosingModule == null) {
            enclosingModule = (RubyModule)getThreadContext().peekCRef().getValue();
        }

        return enclosingModule;
    }
    
    private boolean isRescueHandled(RubyException currentException, ListNode exceptionNodes, IRubyObject self) {
        if (exceptionNodes == null) {
            return currentException.isKindOf(runtime.getClass("StandardError"));
        }
        
        ThreadContext tc = getThreadContext();

        tc.beginCallArgs();

        IRubyObject[] args = null;
        try {
            args = setupArgs2(runtime, tc, exceptionNodes, self);
        } finally {
            tc.endCallArgs();
        }

        for (int i = 0; i < args.length; i++) {
            if (! args[i].isKindOf(runtime.getClass("Module"))) {
                throw runtime.newTypeError("class or module required for rescue clause");
            }
            if (args[i].callMethod("===", currentException).isTrue())
                return true;
        }
        return false;
    }

    private IRubyObject[] setupArgs2(IRuby runtime, ThreadContext context, Node node, IRubyObject self) {
        if (node == null) {
            return IRubyObject.NULL_ARRAY;
        }

        if (node instanceof ArrayNode) {
            ISourcePosition position = context.getPosition();
            ArrayList list = new ArrayList(((ArrayNode) node).size());
            
            for (Iterator iter=((ArrayNode)node).iterator(); iter.hasNext();){
                final Node next = (Node) iter.next();
                if (next instanceof SplatNode) {
                    list.addAll(((RubyArray) eval(next, self)).getList());
                } else {
                    list.add(eval(next, self));
                }
            }

            context.setPosition(position);

            return (IRubyObject[]) list.toArray(new IRubyObject[list.size()]);
        }

        return ArgsUtil.arrayify(eval(node, self));
    }

    // Had to make it work this way because eval states are sometimes created in one thread for use in another...
    // For example, block creation for a new Thread; block, frame, and evalstate for that Thread are created in the caller
    // but used in the new Thread.
    public ThreadContext getThreadContext() {
        return runtime.getCurrentContext();
    }
}