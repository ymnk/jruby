package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class FalseClass extends DynamicClass implements MetaClass {

    public FalseClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "FalseClass";
    }

    public int getClassIndex() {
        return ClassIndex.FALSE;
    }

    public boolean isImmediate() {
        return true;
    }

}
