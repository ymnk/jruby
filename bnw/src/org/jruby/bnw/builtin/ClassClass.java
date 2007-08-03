package org.jruby.bnw.builtin;

import org.jruby.Ruby;
import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class ClassClass extends DynamicClass implements MetaClass {

    public ClassClass(final ModuleClass superclass) {
        super(superclass);
    }
    
    public String getName() {
        return "Class";
    }

    public int getClassIndex() {
        return ClassIndex.CLASS;
    }

}
