package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.List;
import java.util.Map;

import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyArray;
import org.jruby.runtime.ThreadContext;

// This represents an array that is built at runtime from its constituents.
//   * Array + Splat ([1,2,3], *[5,6,7])
//   * Array + Value ([1,2,3], 5)
// Note that in either case, the first operand array is not modified, 
// i.e. this is a side-effect free operation, hence an operand
//
// NOTE: This operand is only used in the initial stages of optimization.
// Further down the line, this will get built into an actual array object
public class CompoundArray extends Operand {
    Operand a1;
    Operand a2;
    boolean argsPushNode;

    public CompoundArray(Operand a1, Operand a2) { 
        this.a1 = a1;
        this.a2 = a2; 
        argsPushNode = false;
    }

    public CompoundArray(Operand a1, Operand a2, boolean argsPushNode) { 
        this.a1 = a1;
        this.a2 = a2;
        this.argsPushNode = argsPushNode;
    }

    public boolean isConstant() { return false; /*return a1.isConstant() && a2.isConstant();*/ }

    public String toString() { return a1 + ", *" + a2; }

    public Operand getSimplifiedOperand(Map<Operand, Operand> valueMap) {
/*
 * SSS FIXME:  Cannot convert this to an Array operand!
 *
        a1 = a1.getSimplifiedOperand(valueMap);
        a2 = a2.getSimplifiedOperand(valueMap);

        // For simplification, get the target value, even if compound
        Operand p1 = a1;
        if (p1 instanceof Variable)
            p1 = ((Variable)p1).getValue(valueMap);

        // For simplification, get the target value, even if compound
        Operand p2 = a2;
        if (p2 instanceof Variable)
            p2 = ((Variable)p2).getValue(valueMap);

        if ((p1 instanceof Array) && (p2 instanceof Array)) {
            // SSS FIXME: Move this code to some utils area .. or probably there is already a method for this in some jruby utils class
            // Holy cow!  Just to append two darned arrays!
            Operand[] p1Elts = ((Array)p1).elts;
            Operand[] p2Elts = ((Array)p2).elts;
            Operand[] newElts = new Operand[p1Elts.length + p2Elts.length];
            System.arraycopy(p1Elts, 0, newElts, 0, p1Elts.length);
            System.arraycopy(p2Elts, 0, newElts, p1Elts.length, p2Elts.length);
            return new Array(newElts);
        }
        else {
            return this;
        }
*/
        return this;
    }

    @Override
    public boolean isNonAtomicValue() { return true; }

    /** Append the list of variables used in this operand to the input list */
    @Override
    public void addUsedVariables(List<Variable> l) {
        a1.addUsedVariables(l);
        a2.addUsedVariables(l);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) { 
        return isConstant() ? this : new CompoundArray(a1.cloneForInlining(ii), a2.cloneForInlining(ii));
    }

    @Override
    public Object retrieve(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject v1 = (IRubyObject)a1.retrieve(interp, context, self);
        IRubyObject v2 = (IRubyObject)a2.retrieve(interp, context, self);
        return argsPushNode ? ((RubyArray)v1.dup()).append(v2) : RuntimeHelpers.argsCat(v1, v2);
    }
}
