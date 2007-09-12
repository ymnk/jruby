package org.jruby.runtime.component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.jruby.Ruby;
// FIXME: want interface here
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.Variable;
import org.jruby.runtime.builtin.Mutable;
import org.jruby.util.IdUtil;

public abstract class VariableStore<BaseObjectType> implements Serializable {

    protected static final String ERR_INSECURE_SET_INST_VAR  = "Insecure: can't modify instance variable";
    protected static final String ERR_INSECURE_SET_CLASS_VAR = "Insecure: can't modify class variable";
    protected static final String ERR_INSECURE_SET_CONSTANT  = "Insecure: can't modify constant";
    protected static final String ERR_FROZEN_CONST_TYPE = "class/module ";
    protected static final String ERR_FROZEN_CVAR_TYPE = "class/module ";

    
    // FIXME: will need to figure out how to restore this when
    // deserializing
    protected transient Ruby runtime;

    protected Mutable<?> mutableOwner;
    
    public VariableStore(final Ruby runtime, final BaseObjectType owner) {
        assert runtime != null;
        this.runtime = runtime;
        this.mutableOwner = owner instanceof Mutable ? (Mutable)owner : null;
    }
    
    public final void validateInstanceVariable(final String name) {
        if (!IdUtil.isValidInstanceVariableName(name)) {
            throw runtime.newNameError("`" + name + "' is not allowable as an instance variable name", name);
        }
    }

    public final void validateClassVariable(final String name) {
        if (!IdUtil.isValidClassVariableName(name)) {
            throw runtime.newNameError("`" + name + "' is not allowed as a class variable name", name);
        }
    }

    public final void validateConstant(final String name) {
        if (!IdUtil.isValidConstantName(name)) {
            throw runtime.newNameError("wrong constant name " + name, name);
        }
    }
    
    public static final void validateInstanceVariable(final Ruby runtime, final String name) {
        if (!IdUtil.isValidInstanceVariableName(name)) {
            throw runtime.newNameError("`" + name + "' is not allowable as an instance variable name", name);
        }
    }

    public static final void validateClassVariable(final Ruby runtime, final String name) {
        if (!IdUtil.isValidClassVariableName(name)) {
            throw runtime.newNameError("`" + name + "' is not allowed as a class variable name", name);
        }
    }

    public static final void validateConstant(final Ruby runtime, final String name) {
        if (!IdUtil.isValidConstantName(name)) {
            throw runtime.newNameError("wrong constant name " + name, name);
        }
    }
    
    public final void checkInstanceVariablesSettable() {
        Mutable<?> m;
        if ((m = mutableOwner) != null) {
            if (!m.isTaint() && runtime.getSafeLevel() >= 4) {
                throw runtime.newSecurityError(ERR_INSECURE_SET_INST_VAR);
            }
            if (m.isFrozen()) {
                // FIXME: want interface in instanceof
                if (m instanceof RubyModule) {
                    throw runtime.newFrozenError("class/module ");
                } else {
                    throw runtime.newFrozenError("");
                }
            }
        }
    }

    public final void checkClassVariablesSettable() {
        Mutable<?> m;
        if ((m = mutableOwner) != null) {
            if (!m.isTaint() && runtime.getSafeLevel() >= 4) {
                throw runtime.newSecurityError(ERR_INSECURE_SET_CLASS_VAR);
            }
            if (m.isFrozen()) {
                throw runtime.newFrozenError(ERR_FROZEN_CVAR_TYPE);
            }
        }
    }

    public final void checkConstantsSettable() {
        Mutable<?> m;
        if ((m = mutableOwner) != null) {
            if (!m.isTaint() && runtime.getSafeLevel() >= 4) {
                throw runtime.newSecurityError(ERR_INSECURE_SET_CONSTANT);
            }
            if (m.isFrozen()) {
                throw runtime.newFrozenError(ERR_FROZEN_CONST_TYPE);
            }
        }
    }


    public abstract int size();
    
    public abstract boolean isEmpty();

    //
    // PSEUDO/INTERNAL VARIABLES (i.e, not ivar/cvar/constant)
    // Don't use these methods for real attributes (ivar/cvar/constant),
    // as that may fail in implementations that store different attribute
    // types separately.
    //

    public abstract boolean hasInternalVariable(final String name);
    public abstract boolean fastHasInternalVariable(final String internedName);
    public abstract BaseObjectType getInternalVariable(final String name);
    public abstract BaseObjectType fastGetInternalVariable(final String internedName);
    public abstract void setInternalVariable(final String name, final BaseObjectType value);
    public abstract void fastSetInternalVariable(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType removeInternalVariable(final String name);

    /**
     * @return all attributes (ivar/cvar/constant/internal)
     */
    public abstract List<Variable<BaseObjectType>> getVariableList();

    /**
     * @return only "internal" attributes (NOT ivar/cvar/constant)
     */
    public abstract List<Variable<BaseObjectType>> getInternalVariableList();
    
    /**
     * @return all attribute names (ivar/cvar/constant/internal)
     */
    public abstract List<String> getVariableNameList();
    
    /**
     * @return all attributes (ivar/cvar/constant/internal) as a HashMap.
     *         This is a snapshot, not the store itself.  Provided mostly
     *         to ease transition to new attributes mechanism. May be 
     *         deprecated in the near future -- call the appropriate 
     *         getXxxList method for future compatiblity.
     */
    public abstract Map getVariableMap();

    
    //
    // INSTANCE VARIABLES
    // Don't use these methods for non-ivars
    //
    
    public abstract boolean hasInstanceVariable(final String name);
    public abstract boolean fastHasInstanceVariable(final String internedName);
    public abstract boolean validatedHasInstanceVariable(final String name);
    public abstract boolean fastValidatedHasInstanceVariable(final String internedName);
    public abstract BaseObjectType getInstanceVariable(final String name);
    public abstract BaseObjectType fastGetInstanceVariable(final String internedName);
    public abstract BaseObjectType validatedGetInstanceVariable(final String name);
    public abstract BaseObjectType fastValidatedGetInstanceVariable(final String internedName);
    public abstract void setInstanceVariable(final String name, final BaseObjectType value);
    public abstract void fastSetInstanceVariable(final String internedName, final BaseObjectType value);
    public abstract void validatedSetInstanceVariable(final String name, final BaseObjectType value);
    public abstract void fastValidatedSetInstanceVariable(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType removeInstanceVariable(final String name);
    public abstract BaseObjectType validatedRemoveInstanceVariable(final String name);
    public abstract List<Variable<BaseObjectType>> getInstanceVariableList();
    public abstract List<String> getInstanceVariableNameList();

    //
    // CLASS VARIABLES
    // Don't use these methods for non-cvars
    //
    
    public abstract boolean hasClassVariable(final String name);
    public abstract boolean fastHasClassVariable(final String internedName);
    public abstract boolean validatedHasClassVariable(final String name);
    public abstract boolean fastValidatedHasClassVariable(final String internedName);
    public abstract BaseObjectType getClassVariable(final String name);
    public abstract BaseObjectType fastGetClassVariable(final String internedName);
    public abstract BaseObjectType validatedGetClassVariable(final String name);
    public abstract BaseObjectType fastValidatedGetClassVariable(final String internedName);
    public abstract void setClassVariable(final String name, final BaseObjectType value);
    public abstract void fastSetClassVariable(final String internedName, final BaseObjectType value);
    public abstract void validatedSetClassVariable(final String name, final BaseObjectType value);
    public abstract void fastValidatedSetClassVariable(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType removeClassVariable(final String name);
    public abstract BaseObjectType validatedRemoveClassVariable(final String name);
    public abstract List<Variable<BaseObjectType>> getClassVariableList();
    public abstract List<String> getClassVariableNameList();
    
    //
    // CONSTANTS
    // Don't use these methods for non-constants
    //
    
    public abstract boolean hasConstant(final String name);
    public abstract boolean fastHasConstant(final String internedName);
    public abstract boolean validatedHasConstant(final String name);
    public abstract boolean fastValidatedHasConstant(final String internedName);
    public abstract BaseObjectType getConstant(final String name);
    public abstract BaseObjectType fastGetConstant(final String internedName);
    public abstract BaseObjectType validatedGetConstant(final String name);
    public abstract BaseObjectType fastValidatedGetConstant(final String internedName);
    public abstract void setConstant(final String name, final BaseObjectType value);
    public abstract void fastSetConstant(final String internedName, final BaseObjectType value);
    public abstract void validatedSetConstant(final String name, final BaseObjectType value);
    public abstract void fastValidatedSetConstant(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType removeConstant(final String name);
    public abstract BaseObjectType validatedRemoveConstant(final String name);
    public abstract List<Variable<BaseObjectType>> getConstantList();
    public abstract List<String> getConstantNameList();
 
    // FIXME: this should go somewhere more generic -- maybe IdUtil
    public static final boolean isRubyVariable(final String name) {
        char c;
        return name.length() > 0 && ((c = name.charAt(0)) == '@' || (c <= 'Z' && c >= 'A'));
    }
    
    
}
