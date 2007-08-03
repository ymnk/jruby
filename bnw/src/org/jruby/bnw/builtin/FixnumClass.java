package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class FixnumClass extends DynamicClass implements MetaClass {

    public FixnumClass(final IntegerClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Fixnum";
    }
    
    public int getClassIndex() {
        return ClassIndex.FIXNUM;
    }
    
    public boolean isImmediate() {
        return true;
    }

}
