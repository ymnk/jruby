package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class NilClass extends DynamicClass implements MetaClass {

    public NilClass(ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "NilClass";
    }
    
    public int getClassIndex() {
        return ClassIndex.NIL;
    }

    public boolean isImmediate() {
        return true;
    }
}
