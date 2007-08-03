package org.jruby.bnw.java;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MethodNode implements Cloneable {

    private static final int PUBLIC_ABS = Modifier.PUBLIC | Modifier.ABSTRACT;
    private static final int PROTECTED_ABS = Modifier.PROTECTED | Modifier.ABSTRACT;
    
    private final String name;
    private Method[] publicMethods;
    private Method[] protectedMethods;
    
    public MethodNode(final String name) {
        this.name = name;
    }
    
    public MethodNode(final Method method) {
        this.name = method.getName();
        addMethod(method);
    }
    
    public final String getName() {
        return name;
    }
    
    public void addMethod(Method method) {
        if (name != method.getName()) {
            throw new IllegalArgumentException("method name " + method.getName() + " does not match node name " + name);
        }
        int modifiers;
        if (((modifiers = method.getModifiers()) & PUBLIC_ABS) == Modifier.PUBLIC) {
            publicMethods = append(publicMethods, method);
        } else if ((modifiers & PROTECTED_ABS) == Modifier.PROTECTED) {
            protectedMethods = append(protectedMethods, method);
        }
        
    }
    
    private static Method[] append(Method[] oldArray, Method method) {
        if (oldArray == null) {
            return new Method[] { method };
        } else {
            int length;
            Method[] newArray = new Method[(length = oldArray.length) + 1];
            System.arraycopy(oldArray, 0, newArray, 0, length);
            newArray[length] = method;
            return newArray;
        }
    }
    
    public boolean hasPublicMethods() {
        return publicMethods != null && publicMethods.length > 0;
    }
    
    public Method[] getPublicMethods() {
        return publicMethods;
    }
    
    public boolean hasProtectedMethods() {
        return protectedMethods != null && protectedMethods.length > 0;
    }
    
    public Method[] getProtectedMethods() {
        return protectedMethods;
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
