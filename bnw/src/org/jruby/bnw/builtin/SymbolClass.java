package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.runtime.ClassIndex;

public final class SymbolClass extends DynamicClass implements MetaClass {

    public SymbolClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Symbol";
    }
    
    public int getClassIndex() {
        return ClassIndex.SYMBOL;
    }

    public boolean isImmediate() {
        return true;
    }

}
