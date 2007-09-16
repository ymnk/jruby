package org.jruby.runtime.component;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.NamedMethod;

public class MethodEntry implements NamedMethod {
    public final String name;
    public final DynamicMethod method;
    
    public MethodEntry(final String name, final DynamicMethod method) {
        this.name = name;
        this.method = method;
    }

    public final String getName() {
        return name;
    }
    public final DynamicMethod getMethod() {
        return method;
    }

}
