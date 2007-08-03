package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class NumericClass extends DynamicClass implements MetaClass {

    public NumericClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Numeric";
    }
    
    public int getClassIndex() {
        return ClassIndex.NUMERIC;
    }

}
