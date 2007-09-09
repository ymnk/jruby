/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.builtin;

import java.util.List;
import java.util.Map;

import org.jruby.runtime.builtin.Variable;

/**
 * 
 * @author Bill Dortch
 */
public interface Variables<BaseObjectType> {

    //
    // GENERAL VARIABLES METHODS
    //

    /**
     * Returns true if object has any variables, defined as:
     * <ul>
     * <li> instance variables
     * <li> class variables
     * <li> constants
     * <li> internal variables, such as those used when marshalling Ranges and Exceptions
     * </ul>
     * @return true if object has any variables, else false
     */
    boolean hasVariables();

    /**
     * @return the count of all variables (ivar/cvar/constant/internal)
     */
    int getVariableCount();
    
    /**
     * Returns true if object has the named internal variable.  Use only
     * for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of an internal variable
     * @return true if object has the named internal variable.
     */
    boolean hasInternalVariable(String name);
    
    /**
     * Returns true if object has the named internal variable.  Use only
     * for internal variables (not ivar/cvar/constant). The supplied
     * name <em>must</em> have been previously interned.
     * 
     * @param internedName the interned name of an internal variable
     * @return true if object has the named internal variable, else false
     */
    boolean fastHasInternalVariable(String internedName);

    /**
     * Returns the named internal variable if present, else null.  Use only
     * for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of an internal variable
     * @return the named internal variable if present, else null
     */
    BaseObjectType getInternalVariable(String name);
    
    /**
     * Returns the named internal variable if present, else null.  Use only
     * for internal variables (not ivar/cvar/constant). The supplied
     * name <em>must</em> have been previously interned.
     * 
     * @param internedName the interned name of an internal variable
     * @return he named internal variable if present, else null
     */
    BaseObjectType fastGetInternalVariable(String internedName);

    /**
     * Sets the named internal variable to the specified value.  Use only
     * for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of an internal variable
     * @param value the value to be set
     */
    void setInternalVariable(String name, BaseObjectType value);
    
    /**
     * Sets the named internal variable to the specified value.  Use only
     * for internal variables (not ivar/cvar/constant). The supplied
     * name <em>must</em> have been previously interned.
     * 
     * @param internedName the interned name of an internal variable
     * @param value the value to be set
     */
    void fastSetInternalVariable(String internedName, BaseObjectType value);

    /**
     * Removes the named internal variable, if present, returning its
     * value.  Use only for internal variables (not ivar/cvar/constant).
     * 
     * @param name the name of the variable to remove
     * @return the value of the remove variable, if present; else null
     */
    BaseObjectType removeInternalVariable(String name);

    /**
     * Sets object's variables to those in the supplied list,
     * removing/replacing any previously defined variables.  Applies
     * to all variable types (ivar/cvar/constant/internal).
     * 
     * @param variables the variables to be set for object 
     */
    void syncVariables(List<Variable<BaseObjectType>> variables);
    
    /**
     * @return a list of all variables (ivar/cvar/constant/internal)
     */
    List<Variable<BaseObjectType>> getVariableList();

    /**
     * @return only internal variables (NOT ivar/cvar/constant)
     */
    List<Variable<BaseObjectType>> getInternalVariableList();

    /**
     * @return a list of all variable names (ivar/cvar/constant/internal)
     */
    List<String> getVariableNameList();
    
    /**
     * @return all variables (ivar/cvar/constant/internal) as a HashMap.
     *         This is a snapshot, not the store itself.  Provided mostly
     *         to ease transition to new variables mechanism. May be 
     *         deprecated in the near future -- call the appropriate 
     *         getXxxList method for future compatiblity.
     */
    Map getVariableMap();

}
