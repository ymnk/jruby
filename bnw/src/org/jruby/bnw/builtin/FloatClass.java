package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class FloatClass extends DynamicClass implements MetaClass {

    public FloatClass(final NumericClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Float";
    }
    
    public int getClassIndex() {
        return ClassIndex.FLOAT;
    }

}
