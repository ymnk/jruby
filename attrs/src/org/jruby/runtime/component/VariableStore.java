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

    public abstract void syncVariables(final List<Variable<BaseObjectType>> varList);
    
    //
    // PSEUDO/INTERNAL VARIABLES (i.e, not ivar/cvar/constant)
    // Don't use these methods for real variables (ivar/cvar/constant),
    // as that may fail in implementations that store different variable
    // types separately.
    //

    public abstract boolean hasInternalVariable(final String name);
    public abstract boolean fastHasInternalVariable(final String internedName);
    public abstract BaseObjectType fetchInternalVariable(final String name);
    public abstract BaseObjectType fastFetchInternalVariable(final String internedName);
    public abstract void storeInternalVariable(final String name, final BaseObjectType value);
    public abstract void fastStoreInternalVariable(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType deleteInternalVariable(final String name);

    /**
     * @return all stored variables (ivar/cvar/constant/internal)
     */
    public abstract List<Variable<BaseObjectType>> getStoredVariableList();

    /**
     * @return only "internal" variables (NOT ivar/cvar/constant)
     */
    public abstract List<Variable<BaseObjectType>> getStoredInternalVariableList();
    
    /**
     * @return all variable names (ivar/cvar/constant/internal)
     */
    public abstract List<String> getStoredVariableNameList();
    
    /**
     * @return all variables (ivar/cvar/constant/internal) as a HashMap.
     *         This is a snapshot, not the store itself.  Provided mostly
     *         to ease transition to new variables mechanism. May be 
     *         deprecated in the near future -- call the appropriate 
     *         getXxxList method for future compatiblity.
     */
    public abstract Map getStoredVariableMap();

    
    //
    // INSTANCE VARIABLES
    // Don't use these methods for non-ivars
    //
    
    public abstract boolean hasInstanceVariable(final String name);
    public abstract boolean fastHasInstanceVariable(final String internedName);
    public abstract boolean validatedHasInstanceVariable(final String name);
    public abstract boolean fastValidatedHasInstanceVariable(final String internedName);
    public abstract BaseObjectType fetchInstanceVariable(final String name);
    public abstract BaseObjectType fastFetchInstanceVariable(final String internedName);
    public abstract BaseObjectType validatedFetchInstanceVariable(final String name);
    public abstract BaseObjectType fastValidatedFetchInstanceVariable(final String internedName);
    public abstract void storeInstanceVariable(final String name, final BaseObjectType value);
    public abstract void fastStoreInstanceVariable(final String internedName, final BaseObjectType value);
    public abstract void validatedStoreInstanceVariable(final String name, final BaseObjectType value);
    public abstract void fastValidatedStoreInstanceVariable(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType deleteInstanceVariable(final String name);
    public abstract BaseObjectType validatedDeleteInstanceVariable(final String name);
    public abstract List<Variable<BaseObjectType>> getStoredInstanceVariableList();
    public abstract List<String> getStoredInstanceVariableNameList();

    //
    // CLASS VARIABLES
    // Don't use these methods for non-cvars
    //
    
    public abstract boolean hasClassVariable(final String name);
    public abstract boolean fastHasClassVariable(final String internedName);
    public abstract boolean validatedHasClassVariable(final String name);
    public abstract boolean fastValidatedHasClassVariable(final String internedName);
    public abstract BaseObjectType fetchClassVariable(final String name);
    public abstract BaseObjectType fastFetchClassVariable(final String internedName);
    public abstract BaseObjectType validatedFetchClassVariable(final String name);
    public abstract BaseObjectType fastValidatedFetchClassVariable(final String internedName);
    public abstract void storeClassVariable(final String name, final BaseObjectType value);
    public abstract void fastStoreClassVariable(final String internedName, final BaseObjectType value);
    public abstract void validatedStoreClassVariable(final String name, final BaseObjectType value);
    public abstract void fastValidatedStoreClassVariable(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType deleteClassVariable(final String name);
    public abstract BaseObjectType validatedDeleteClassVariable(final String name);
    public abstract List<Variable<BaseObjectType>> getStoredClassVariableList();
    public abstract List<String> getStoredClassVariableNameList();
    
    //
    // CONSTANTS
    // Don't use these methods for non-constants
    //
    
    public abstract boolean hasConstant(final String name);
    public abstract boolean fastHasConstant(final String internedName);
    public abstract boolean validatedHasConstant(final String name);
    public abstract boolean fastValidatedHasConstant(final String internedName);
    public abstract BaseObjectType fetchConstant(final String name);
    public abstract BaseObjectType fastFetchConstant(final String internedName);
    public abstract BaseObjectType validatedFetchConstant(final String name);
    public abstract BaseObjectType fastValidatedFetchConstant(final String internedName);
    public abstract void storeConstant(final String name, final BaseObjectType value);
    public abstract void fastStoreConstant(final String internedName, final BaseObjectType value);
    public abstract void validatedStoreConstant(final String name, final BaseObjectType value);
    public abstract void fastValidatedStoreConstant(final String internedName, final BaseObjectType value);
    public abstract BaseObjectType deleteConstant(final String name);
    public abstract BaseObjectType validatedDeleteConstant(final String name);
    public abstract List<Variable<BaseObjectType>> getStoredConstantList();
    public abstract List<String> getStoredConstantNameList();
 
    // FIXME: this should go somewhere more generic -- maybe IdUtil
    public static final boolean isRubyVariable(final String name) {
        char c;
        return name.length() > 0 && ((c = name.charAt(0)) == '@' || (c <= 'Z' && c >= 'A'));
    }
    
    
}
