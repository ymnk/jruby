package org.jruby.runtime.builtin;

import org.jruby.internal.runtime.methods.DynamicMethod;

public interface NamedMethod {

    String getName();
    
    DynamicMethod getMethod();
}
