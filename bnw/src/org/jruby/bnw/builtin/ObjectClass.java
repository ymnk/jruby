package org.jruby.bnw.builtin;

import org.jruby.bnw.DynamicClass;
import org.jruby.bnw.MetaClass;
import org.jruby.bnw.Registry;
import org.jruby.runtime.ClassIndex;

public final class ObjectClass extends DynamicClass implements MetaClass {

    public ObjectClass(final Registry registry) {
        super(registry);
    }
    
    public String getName() {
        return "Object";
    }
    
    public int getClassIndex() {
        return ClassIndex.OBJECT;
    }
    
    // Assumption: self and any arguments are unwrapped prior to call. 
    
    // ==
    public static Object _eq(Object self, Object other) {
        return self == other ? Boolean.TRUE : Boolean.FALSE;
    }
    
    // ===
    public static Object _eqq(Object self, Object other) {
        return self == other ? Boolean.TRUE : Boolean.FALSE;
    }
    
    // =~
    public static Object _match(Object self, Object other) {
        return Boolean.FALSE;
    }

    // __id__
    public Object __id__(Object self) {
        return null;
    }
    
    // id
    public Object id(Object self) {
        return null;
    }
    
    // eql?
    public static Object eql__p(Object self, Object other) {
        return self == other ? Boolean.TRUE : Boolean.FALSE;
    }

    // equal?
    public static Object equal__p(Object self, Object other) {
        return self == other ? Boolean.TRUE : Boolean.FALSE;
    }
    
    // TODO: including these here in case we decide on java.lang.Object == Ruby Object
    
    public Object toString(Object self) {
        return "";
    }
    
    public static Object equals(Object self, Object other) {
        if (self == null) {
            return Boolean.valueOf(other == null);
        } else {
            return Boolean.valueOf(self.equals(other));
        }
    }
    
    
}
