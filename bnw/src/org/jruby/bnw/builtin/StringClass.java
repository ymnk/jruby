package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.runtime.ClassIndex;

public final class StringClass extends DynamicClass {

    public StringClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "String";
    }
    
    public int getClassIndex() {
        return ClassIndex.STRING;
    }

}
