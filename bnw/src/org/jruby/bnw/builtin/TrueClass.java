package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.runtime.ClassIndex;

public final class TrueClass extends DynamicClass implements MetaClass {

    public TrueClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "TrueClass";
    }

    public int getClassIndex() {
        return ClassIndex.TRUE;
    }

    public boolean isImmediate() {
        return true;
    }

}
