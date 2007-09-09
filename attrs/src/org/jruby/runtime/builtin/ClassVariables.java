package org.jruby.runtime.builtin;

import java.util.List;

public interface ClassVariables<BaseObjectType> {

    boolean hasClassVariable(String name);
    boolean fastHasClassVariable(String internedName);
    
    BaseObjectType getClassVariable(String name);
    BaseObjectType fastGetClassVariable(String internedName);
    
    BaseObjectType setClassVariable(String name, BaseObjectType value);
    BaseObjectType fastSetClassVariable(String internedName, BaseObjectType value);

    BaseObjectType removeClassVariable(String name);

    List<Variable<BaseObjectType>> getClassVariableList();

    List<String> getClassVariableNameList();

}
