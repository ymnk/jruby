package org.jruby.runtime.builtin;

public interface Mutable<BaseObjectType> {

    boolean isTaint();
    
    void setTaint(boolean b);
    
    /**
     * Infect this object using the taint of another object
     */
    BaseObjectType infectBy(BaseObjectType obj);
    
    boolean isFrozen();

    void setFrozen(boolean b);
    
}
