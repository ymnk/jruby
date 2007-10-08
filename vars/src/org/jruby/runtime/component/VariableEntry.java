package org.jruby.runtime.component;

import org.jruby.runtime.builtin.Variable;
import org.jruby.util.IdUtil;

public class VariableEntry<BaseObjectType> implements Variable<BaseObjectType> {
    public final String name;
    public final BaseObjectType value;
    
    public VariableEntry(final String name, final BaseObjectType value) {
        this.name = name;
        this.value = value;
    }

    public final String getName() {
        return name;
    }
    
    public final BaseObjectType getValue() {
        return value;
    }
    
    public final boolean isClassVariable() {
        return IdUtil.isClassVariable(name);
    }
    
    public final boolean isConstant() {
        return IdUtil.isConstant(name);
    }
    
    public final boolean isInstanceVariable() {
        return IdUtil.isInstanceVariable(name);
    }

    public final boolean isRubyVariable() {
        char c;
        return name.length() > 0 && ((c = name.charAt(0)) == '@' || (c <= 'Z' && c >= 'A'));
    }
}
