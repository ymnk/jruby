package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class ModuleClass extends DynamicClass implements MetaClass {

    public ModuleClass(final ObjectClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Module";
    }

    public int getClassIndex() {
        return ClassIndex.MODULE;
    }

}
