package org.jruby.bnw;

//TODO: may combine this with Freezable, other wrapper interfaces?
public interface Taintable {
    boolean isTaint();
}
