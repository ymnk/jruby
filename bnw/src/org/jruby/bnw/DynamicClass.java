package org.jruby.bnw;

import org.jruby.Ruby;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Base MetaClass to provide convenience/default method implementations, data
 * structures, etc.
 * 
 * @author Bill Dortch
 *
 */
public abstract class DynamicClass implements MetaClass {

    private final Registry registry;
    
    private final MetaClass superclass;
    
    
    protected Module[] includedModules = new Module[0];
    
    protected DynamicClass(final Registry registry, final MetaClass superclass) {
        this.registry = registry;
        this.superclass = superclass;
    }
    
    protected DynamicClass(final Registry registry) {
        this.registry = registry;
        this.superclass = null;
    }
    
    protected DynamicClass(final DynamicClass superclass) {
        this.registry = superclass.getRegistry();
        this.superclass = superclass;
    }
    
    public final Registry getRegistry() {
        return registry;
    }

    public final Ruby getRuntime() {
        return registry.getRuntime();
    }
    
    public final MetaClass getSuperClass() {
        return superclass;
    }
    
    public int getClassIndex() {
        return ClassIndex.NO_INDEX;
    }
    
    public boolean isSingleton() {
        return false;
    }

    public boolean isImmediate() {
        return false;
    }

    public boolean respondsTo(Object self, String name) {
        return false;
    }

    public Module[] getIncludedModules() {
        return includedModules;
    }
    
    public boolean isModuleIncluded(Module module) {
        Module[] modules = includedModules;
        for (int i = modules.length; --i >= 0; ) {
            if (module == modules[i]) return true;
        }
        MetaClass sc;
        if ((sc = getSuperClass()) != null) {
            return sc.isModuleIncluded(module);
        }
        return false;
    }
    
    // TODO: implement
    public synchronized boolean includeModule(Module module) {
        throw new UnsupportedOperationException();
    }
    
    public Object callMethod(ThreadContext context, Object self, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, String name, Block block) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, String name, Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, String name, Object[] args) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, String name, Object[] args,
            Block block) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, String name, Object[] args,
            CallType callType) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, String name, Object[] args,
            CallType callType, Block block) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, int methodIndex, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, int methodIndex, String name,
            Object arg) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object callMethod(ThreadContext context, Object self, int methodIndex, String name,
            Object[] args) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
