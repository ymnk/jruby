package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.runtime.ClassIndex;

public final class HashClass extends DynamicClass {

    public HashClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Hash";
    }
    
    public int getClassIndex() {
        return ClassIndex.HASH;
    }

}
