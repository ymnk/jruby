package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class IntegerClass extends DynamicClass implements MetaClass {

    public IntegerClass(final NumericClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Integer";
    }
    
    public int getClassIndex() {
        return ClassIndex.INTEGER;
    }
}
