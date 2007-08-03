package org.jruby.bnw;

public interface ModuleIncluder {

    boolean includeModule(Module module);
    
    boolean isModuleIncluded(Module module);
    
    Module[] getIncludedModules();
    
}
