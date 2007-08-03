package org.jruby.bnw;

/**
 * Transitional CommonMetaClass to bridge RubyClass and MetaClass (which may
 * eventually merge).
 * <p>
 * Not much in common just yet....
 */
public interface CommonMetaClass {

    String getName();
    
    // here as reminder this needs to be implemented in
    // MetaClass/DynamicMetaClass.
    String toString();
    
    // these are also module-y implementation things...
    
    //defineMethod(...)
    //defineFastMethod(...)
    //undefineMethod(...)

}
