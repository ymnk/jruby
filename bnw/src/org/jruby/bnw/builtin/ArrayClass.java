package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.runtime.ClassIndex;

public final class ArrayClass extends DynamicClass {

    public ArrayClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Array";
    }
    
    public int getClassIndex() {
        return ClassIndex.ARRAY;
    }


}
