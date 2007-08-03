package org.jruby.bnw.java;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jruby.bnw.util.AttributesMap;


public class MethodNodeMap {

    private static final int MODIFIER_MASK = Modifier.PUBLIC | Modifier.STATIC | Modifier.ABSTRACT;
    private static final int PUBLIC_STATIC = Modifier.PUBLIC | Modifier.STATIC;
    
    private final Class javaClass;
    private final MethodNodeMap superClassMap;
    private AttributesMap staticMethods;
    private AttributesMap instanceMethods;
    
    public MethodNodeMap(final Class javaClass, final MethodNodeMap superClassMap) {
        this.javaClass = javaClass;
        this.superClassMap = superClassMap;
        initialize(javaClass);
    }
    
    public final Class getJavaClass() {
        return javaClass;
    }
    
    public final MethodNodeMap getSuperClassMap() {
        return superClassMap;
    }
    
    public AttributesMap getStaticMethods() {
        return staticMethods;
    }
    
    public AttributesMap getInstanceMethods() {
        return instanceMethods;
    }
    
    private void initialize(final Class javaClass) {
        Method[] methods;
        Method method;
        int mods;
        try {
            methods = javaClass.getDeclaredMethods();
        } catch (SecurityException e) {
            methods = javaClass.getMethods();
        }
        for (int i = methods.length; --i >= 0; ) {
            if (javaClass == (method = methods[i]).getDeclaringClass()) {
                if (Modifier.PUBLIC == (mods = MODIFIER_MASK & method.getModifiers())) {
                    addInstanceMethod(method);
                } else if (PUBLIC_STATIC == mods) {
                    addStaticMethod(method);
                }
            }
        }
    }

    private void addInstanceMethod(final Method method) {
        final String name = method.getName();
        MethodNode node;
        if (instanceMethods == null) {
            instanceMethods = new AttributesMap();
            node = null;
        } else {
            node = (MethodNode)instanceMethods.get(name);
        }
        if (node == null) {
            if ((node = findInstanceMethodNode(name)) == null) {
                node = new MethodNode(method);
            } else {
                node = (MethodNode)node.clone();
                node.addMethod(method);
            }
            instanceMethods.put(name, node);
        } else {
            node.addMethod(method);
        }
    }

    private void addStaticMethod(final Method method) {
        final String name = method.getName();
        MethodNode node;
        if (staticMethods == null) {
            staticMethods = new AttributesMap();
            node = null;
        } else {
            node = (MethodNode)staticMethods.get(name);
        }
        if (node == null) {
            if ((node = findStaticMethodNode(name)) == null) {
                node = new MethodNode(method);
            } else {
                node = (MethodNode)node.clone();
                node.addMethod(method);
            }
            staticMethods.put(name, node);
        } else {
            node.addMethod(method);
        }
    }

    public MethodNode getInstanceMethodNode(final String name) {
        return instanceMethods == null ? null : (MethodNode)instanceMethods.get(name);
    }

    public MethodNode findInstanceMethodNode(final String name) {
        MethodNode node;
        for (MethodNodeMap map = this; map != null; map = map.getSuperClassMap()) {
            if ((node = map.getInstanceMethodNode(name)) != null) return node;
        }
        return null;
    }

    public MethodNode getStaticMethodNode(final String name) {
        return staticMethods == null ? null : (MethodNode)staticMethods.get(name);
    }
    
    public MethodNode findStaticMethodNode(final String name) {
        MethodNode node;
        for (MethodNodeMap map = this; map != null; map = map.getSuperClassMap()) {
            if ((node = map.getStaticMethodNode(name)) != null) return node;
        }
        return null;
    }
    

}
