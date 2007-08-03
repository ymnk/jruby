package org.jruby.bnw.java;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;

public class JavaMetaClass extends DynamicClass {

    private final Class javaClass;
    private MethodNodeMap methodNodes;
    
    public JavaMetaClass(Registry registry, Class javaClass, MetaClass superclass) {
        super(registry, superclass);
        this.javaClass = javaClass;
    }
    
    public Class getJavaClass() {
        return javaClass;
    }
    
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

}
