package org.jruby.bnw;

//TODO: may combine this with Taintable, other wrapper interfaces?
public interface Freezable {

    boolean isFrozen();
}
