/***** BEGIN LICENSE BLOCK *****
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
package org.jruby.bnw;


//import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.WeakHashMap;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.bnw.util.AttributesMap;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;
import org.jruby.util.WeakIdentityHashMap;


/**
 * The Registry stores metaclasses, attributes (instance variables, class variables,
 * constants, etc.), data structs, and possibly other values.
 * 
 * Highly experimental!  Nothing here is writ in stone; mostly chalk...
 * 
 * @author Bill Dortch
 *
 */
public final class Registry {
/* TODO: much to do/try here:
 * 
 *  - should object-specific (i.e., singleton) metaclasses be stored in a
 *    separate map?
 *  
 *  - need to experiment with (backported) ReentrantReadWriteLock instead of
 *    conventional synchronization for metaclass map(s), as writes will be
 *    infrequent compared to reads. need to compare performance.
 *    
 *  - same for attributes map.
 *  
 *  - may want to create a "Weak" version of ConcurrentHashMap, compare performance.
 *    
 *  - need to play with values for initial map sizes.
 *  
 *  - should we keep separate maps for different attribute types? 
 *  
 *    - instance variables
 *    
 *    - class variables
 *    
 *    - constants
 *    
 *    - other values used by marshalling, etc.
 *  
 *  - should access to individual variables/constants be controlled through
 *    the Registry? (I think so.) Or should code be given access to the individual
 *    variable/constant maps, as is presently the case?
 *  
 *  - should the Registry provide builtin support for dup/clone operations?
 *    (I think so.)
 *    
 *  - should new-style metaclasses be defined separately from the current RubyClass?
 *    (I think so.) RubyClass descends from RubyObject, which makes less sense in the
 *    Registry world.
 *    
 *        
 *  
 *
 *
 *
 */
    private static final String ERR_INSECURE_SET_INST_VAR  = "Insecure: can't modify instance variable";;
    private static final String ERR_INSECURE_SET_CLASS_VAR = "Insecure: can't modify class variable";;
    private static final String ERR_INSECURE_SET_CONSTANT  = "Insecure: can't modify constant";
    private static final String ERR_FROZEN_CONST_TYPE = "class/module";
    
    private static final String[] EMPTY_NAME_ARRAY = new String[0];
    
    private static final int INITIAL_META_CLASS_MAP_SIZE = 1024;
    private static final int INITIAL_ATTRIBUTES_MAP_SIZE = 1024;
    private static final int INITIAL_DATA_STRUCT_MAP_SIZE = 16;
    
    private final Ruby runtime;

    private final Map metaClassMap =
        Collections.synchronizedMap(new WeakIdentityHashMap(INITIAL_META_CLASS_MAP_SIZE));
    
    private final Map attributesMap =
        Collections.synchronizedMap(new WeakIdentityHashMap(INITIAL_ATTRIBUTES_MAP_SIZE));
    
    private final Map dataStructMap =
        Collections.synchronizedMap(new WeakIdentityHashMap(INITIAL_DATA_STRUCT_MAP_SIZE));
    
    
    public Registry(Ruby runtime) {
        this.runtime = runtime;
    }
    
    public Ruby getRuntime() {
        return runtime;
    }
    
    // metaclass methods
    
    // TODO: metaclass should be interface
    public void setMetaClass(Object object, RubyClass metaClass) {
        
    }
    

    // transitional version of getMetaClass
    public RubyClass getMetaClass(IRubyObject self) {
        return self.getMetaClass();
    }
    
    public CommonMetaClass getMetaClass(Object self) {
        if (self instanceof IRubyObject) {
            return ((IRubyObject)self).getMetaClass();
        }
        MetaClass metaClass;
        self = unwrap(self);
        synchronized(metaClassMap) {
            if ((metaClass = (MetaClass)metaClassMap.get(self)) == null) {
                metaClass = (MetaClass)metaClassMap.get(self.getClass());
            }
        }
        // TODO: build out JavaClass hierarchy if metaClass == null, as in
        // org.jruby.javasupport.Java#get_proxy_class.
        return metaClass;
    }


    // attributes methods
    
    /**
     * Returns true if self has any attributes defined (instance variables,
     * class variables, constants, or other attributes).
     * 
     * @param self
     * @return true if attributes defined, else false
     */
    public boolean hasAttributes(final Object self) {
        return attributesMap.get(unwrap(self)) != null;
    }
    
    public boolean hasInstanceVariables(final Object self) {
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsInstanceVariables();
    }

    public boolean hasClassVariables(final Object self) {
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsClassVariables();
    }

    public boolean hasConstants(final Object self) {
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsConstants();
    }

    public ArrayList getAttributeList(final Object self) {
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) == null) {
            return new ArrayList(0);
        } else {
            return ((AttributesMap)attrs).getEntryList();
        }
    }

    public String[] getAttributeNames(final Object self) {
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) == null) {
            return EMPTY_NAME_ARRAY;
        } else {
            return((AttributesMap)attrs).getNameArray();
        }
    }

    public Object getAttribute(final Object self, final String name) {
        //System.out.println("attr get  " + name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).get(name);
        }
        return null;
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public Object fastGetAttribute(final Object self, final String name) {
        //System.out.println("attr fget " + name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getNoIntern(name);
        }
        return null;
    }
    
    public boolean hasAttribute(final Object self, final String name) {
        //System.out.println("attr has  " + name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKey(name);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public boolean fastHasAttribute(final Object self, final String name) {
        //System.out.println("attr fhas " + name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKeyNoIntern(name);
    }
    
    public void setAttribute(Object self, final String name, final Object value) {
        //System.out.println("attr set  " + name);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name.intern(), value));
                return;
            }
        }
        ((AttributesMap)attrs).put(name, value);
    }
    
    public void setAttribute(
            final Object self,
            final String name,
            final Object value,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        setAttribute(self, name, value);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @param value
     * @return
     */
    public void fastSetAttribute(Object self, final String name, final Object value) {
        //System.out.println("attr fset " + name);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name, value));
                return;
            }
        }
        ((AttributesMap)attrs).putNoIntern(name, value);
    }
    
    public Object removeAttribute(final Object self, final String name) {
        //System.out.println("attr rmov " + name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).remove(name);
        }
        return null;
    }
   
    /**
     * 
     * @param self
     * @return a copy of self's attributes 
     */
    public AttributesMap getAttributesSnapshot(final Object self) {
        //System.out.println("attr snap");
        AttributesMap attrs;
        if ((attrs = (AttributesMap)attributesMap.get(unwrap(self))) != null) {
            AttributesMap snapshot;
            synchronized(attrs) {
                snapshot = new AttributesMap(attrs);
            }
            return snapshot;
        }
        return new AttributesMap();
    }
    
    public AttributesMap getAttributesSnapshotOrNull(final Object self) {
        //System.out.println("attr snapnull");
        AttributesMap attrs;
        if ((attrs = (AttributesMap)attributesMap.get(unwrap(self))) != null &&
                !attrs.isEmpty()) {
            AttributesMap snapshot;
            synchronized(attrs) {
                snapshot = new AttributesMap(attrs);
            }
            return snapshot;
        }
        return null;
    }
    
    public void importAttributes(Object self, final Map externallyCreatedAttributes) {
        //System.out.println("attr impo");
        self = unwrap(self);
        AttributesMap attrs = new AttributesMap(externallyCreatedAttributes);
        synchronized(attributesMap) {
            // this may be acceptable, want to flag for now
            if (attributesMap.get(self) != null) {
                throw new IllegalStateException("attributes already exist for " + self);
            }
            attributesMap.put(self, attrs);
        }
    }
    
    public void syncAttributes(Object self, final Object source) {
        //System.out.println("attr sync");
        self = unwrap(self);
        AttributesMap sourceAttrs;
        if ((sourceAttrs = (AttributesMap)attributesMap.get(unwrap(source))) != null) {
            AttributesMap attrs;
            synchronized(sourceAttrs) {
                attrs = new AttributesMap(sourceAttrs);
            }
//            synchronized(attributesMap) {
                // this may be acceptable, want to flag for now
                // FIXME: disabling exception for now due to duplication
                // in RubyModule/RubyObject.  Warning for now until this is fixed....
                if (attributesMap.get(self) != null) {
                    runtime.getWarnings().warn("FIXME: duplication of attributes copy");
                    //throw new IllegalStateException("attributes already exist for " + self);
                }
                attributesMap.put(self, attrs);
//            }
        } else {
            // source has no attrs, so we shall have none
            attributesMap.remove(self);
        }
    }

    // instance variable methods
    
    public Object getInstanceVariable(final Object self, final String name) {
        //System.out.println("ivar get  " + name);
        assert IdUtil.isInstanceVariable(name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).get(name);
        }
        return null;
    }

    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public Object fastGetInstanceVariable(final Object self, final String name) {
        //System.out.println("ivar fget " + name);
        assert IdUtil.isInstanceVariable(name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getNoIntern(name);
        }
        return null;
    }

    public boolean hasInstanceVariable(final Object self, final String name) {
        //System.out.println("ivar has  " + name);
        assert IdUtil.isInstanceVariable(name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKey(name);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public boolean fastHasInstanceVariable(final Object self, final String name) {
        //System.out.println("ivar fhas " + name);
        assert IdUtil.isInstanceVariable(name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKeyNoIntern(name);
    }
    
    public void setInstanceVariable(Object self, final String name, final Object value) {
        //System.out.println("ivar set  " + name);
        assert IdUtil.isInstanceVariable(name);
        checkFrozen(self, null);
        checkTaint(self, ERR_INSECURE_SET_INST_VAR);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name.intern(), value));
                return;
            }
        }
        ((AttributesMap)attrs).put(name, value);
    }
    
    public void setInstanceVariable(
            Object self,
            final String name,
            final Object value,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("ivar set  " + name);
        assert IdUtil.isInstanceVariable(name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name.intern(), value));
                return;
            }
        }
        ((AttributesMap)attrs).put(name, value);
    }
    
    public void fastSetInstanceVariable(Object self, final String name, final Object value) {
        //System.out.println("ivar fset " + name);
        assert IdUtil.isInstanceVariable(name);
        checkFrozen(self, null);
        checkTaint(self, ERR_INSECURE_SET_INST_VAR);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name, value));
                return;
            }
        }
        ((AttributesMap)attrs).putNoIntern(name, value);
    }
    
    public void fastSetInstanceVariable(
            Object self,
            final String name,
            final Object value,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("ivar fset " + name);
        assert IdUtil.isInstanceVariable(name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name, value));
                return;
            }
        }
        ((AttributesMap)attrs).putNoIntern(name, value);
    }
    
    
    public Object removeInstanceVariable(final Object self, final String name) {
        //System.out.println("ivar rmov " + name);
        assert IdUtil.isInstanceVariable(name);
        checkFrozen(self, null);
        checkTaint(self, ERR_INSECURE_SET_INST_VAR);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).remove(name);
        }
        return null;
    }
    
    public Object removeInstanceVariable(
            final Object self,
            final String name,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("ivar rmov " + name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).remove(name);
        }
        return null;
    }
    
    public ArrayList getInstanceVariableNameList(final Object self) {
        //System.out.println("ivar getnlist  ");
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getInstanceVariableNameList();
        }
        return emptyArrayList();
    }
    
    public ArrayList getInstanceVariableEntryList(final Object self) {
        //System.out.println("ivar getelist");
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getInstanceVariableEntryList();
        }
        return emptyArrayList();
    }
    
    
    // class variable methods
    
    public Object getClassVariable(final Object self, final String name) {
        //System.out.println("cvar get  " + name);
        assert IdUtil.isClassVariable(name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).get(name);
        }
        return null;
    }

    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public Object fastGetClassVariable(final Object self, final String name) {
        //System.out.println("cvar fget " + name);
        assert IdUtil.isClassVariable(name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getNoIntern(name);
        }
        return null;
    }

    public boolean hasClassVariable(final Object self, final String name) {
        //System.out.println("cvar has  " + name);
        assert IdUtil.isClassVariable(name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKey(name);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public boolean fastHasClassVariable(final Object self, final String name) {
        //System.out.println("cvar fhas " + name);
        assert IdUtil.isClassVariable(name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKeyNoIntern(name);
    }
    
    public void setClassVariable(Object self, final String name, final Object value) {
        //System.out.println("cvar set  " + name);
        assert IdUtil.isClassVariable(name);
        checkFrozen(self, null);
        checkTaint(self, ERR_INSECURE_SET_CLASS_VAR);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name.intern(), value));
                return;
            }
        }
        ((AttributesMap)attrs).put(name, value);
    }
    
    // TODO: FIXME: don't call other version
    
    public void setClassVariable(
            Object self,
            final String name,
            final Object value,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("cvar set  " + name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name.intern(), value));
                return;
            }
        }
        ((AttributesMap)attrs).put(name, value);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @param value
     */
    public void fastSetClassVariable(Object self, final String name, final Object value) {
        //System.out.println("cvar fset " + name);
        assert IdUtil.isClassVariable(name);
        checkFrozen(self, null);
        checkTaint(self, ERR_INSECURE_SET_CLASS_VAR);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name, value));
                return;
            }
        }
        ((AttributesMap)attrs).putNoIntern(name, value);
    }

    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @param value
     * @param taintErrorMessage
     * @param frozenObjectType
     */
    public void fastSetClassVariable(
            Object self,
            final String name,
            final Object value,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("cvar fset " + name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name, value));
                return;
            }
        }
        ((AttributesMap)attrs).putNoIntern(name, value);
    }
    
    public Object removeClassVariable(final Object self, final String name) {
        //System.out.println("cvar rmov " + name);
        assert IdUtil.isClassVariable(name);
        checkFrozen(self, null);
        checkTaint(self, ERR_INSECURE_SET_CLASS_VAR);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).remove(name);
        }
        return null;
    }
    
    public Object removeClassVariable(
            final Object self,
            final String name,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("cvar get  " + name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).remove(name);
        }
        return null;
    }
    
    public ArrayList getClassVariableNameList(final Object self) {
        //System.out.println("cvar getnlist");
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getClassVariableNameList();
        }
        return emptyArrayList();
    }
    
    public ArrayList getClassVariableEntryList(final Object self) {
        //System.out.println("cvar getelist");
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getClassVariableEntryList();
        }
        return emptyArrayList();
    }
    

    // constant methods
    
    public Object getConstant(final Object self, final String name) {
        //System.out.println("cons get  " + name);
        assert IdUtil.isConstant(name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).get(name);
        }
        return null;
    }

    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public Object fastGetConstant(final Object self, final String name) {
        //System.out.println("cons fget " + name);
        assert IdUtil.isConstant(name);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getNoIntern(name);
        }
        return null;
    }

    public boolean hasConstant(final Object self, final String name) {
        //System.out.println("cons has  " + name);
        assert IdUtil.isConstant(name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKey(name);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @return
     */
    public boolean fastHasConstant(final Object self, final String name) {
        //System.out.println("cons fhas " + name);
        assert IdUtil.isConstant(name);
        Object attrs;
        return (attrs = attributesMap.get(unwrap(self))) != null &&
            ((AttributesMap)attrs).containsKeyNoIntern(name);
    }
    
    public void setConstant(Object self, final String name, final Object value) {
        //System.out.println("cons set  " + name);
        assert IdUtil.isConstant(name);
        checkFrozen(self, ERR_FROZEN_CONST_TYPE);
        checkTaint(self, ERR_INSECURE_SET_CONSTANT);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name.intern(), value));
                return;
            }
        }
        ((AttributesMap)attrs).put(name, value);
    }
    
    public void setConstant(
            Object self,
            final String name,
            final Object value,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("cons set  " + name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name.intern(), value));
                return;
            }
        }
        ((AttributesMap)attrs).put(name, value);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @param value
     */
    public void fastSetConstant(Object self, final String name, final Object value) {
        //System.out.println("cons fset " + name);
        assert IdUtil.isConstant(name);
        checkFrozen(self, ERR_FROZEN_CONST_TYPE);
        checkTaint(self, ERR_INSECURE_SET_CONSTANT);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name, value));
                return;
            }
        }
        ((AttributesMap)attrs).putNoIntern(name, value);
    }
    
    /**
     * Use only if <code>name</code> has been interned.
     * @param self
     * @param name
     * @param value
     * @param taintErrorMessage
     * @param frozenObjectType
     */
    public void fastSetConstant(
            Object self,
            final String name,
            final Object value,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("cons set  " + name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        self = unwrap(self);
        synchronized(attributesMap) {
            if ((attrs = attributesMap.get(self)) ==  null) {
                attributesMap.put(self, new AttributesMap(name, value));
                return;
            }
        }
        ((AttributesMap)attrs).putNoIntern(name, value);
    }
    
    public Object removeConstant(final Object self, final String name) {
        //System.out.println("cons rmov " + name);
        assert IdUtil.isConstant(name);
        checkFrozen(self, null);
        checkTaint(self, ERR_INSECURE_SET_CONSTANT);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).remove(name);
        }
        return null;
    }
    
    public Object removeConstant(
            final Object self,
            final String name,
            final String taintErrorMessage,
            final String frozenObjectType
            ) {
        //System.out.println("cons rmov " + name);
        checkFrozen(self, frozenObjectType);
        checkTaint(self, taintErrorMessage);
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).remove(name);
        }
        return null;
    }
    
    public ArrayList getConstantNameList(final Object self) {
        //System.out.println("cons getnlist");
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getConstantNameList();
        }
        return emptyArrayList();
    }
    
    public ArrayList getConstantEntryList(final Object self) {
        //System.out.println("cons getelist");
        Object attrs;
        if ((attrs = attributesMap.get(unwrap(self))) != null) {
            return ((AttributesMap)attrs).getConstantEntryList();
        }
        return emptyArrayList();
    }
    

    // data struct methods
    
        
    public Object getDataStruct(final Object self) {
        //System.out.println("dstr get  cls: " + self.getClass());
        return dataStructMap.get(unwrap(self));
    }
    
    public Object setDataStruct(final Object self, final Object value) {
        //System.out.println("dstr set  cls: " + self.getClass());
        dataStructMap.put(unwrap(self), value);
        return value;
    }
    

    // utility methods
    
    private void checkTaint(final Object self, final String taintErrorMessage) {
        if (runtime.getSafeLevel() >= 4 &&
                self instanceof Taintable &&
                ((Taintable)self).isTaint()) {
            throw runtime.newSecurityError(taintErrorMessage);
        }
    }
    
    private void checkFrozen(final Object self, String frozenObjectType) {
        if (self instanceof Freezable && ((Freezable)self).isFrozen()) {
            if (frozenObjectType == null || frozenObjectType.trim().length() == 0) {
                frozenObjectType = ((CommonMetaClass)getMetaClass(self)).getName();
            }
            throw runtime.newFrozenError(frozenObjectType);
        }
    }
    
    
    public static Object unwrap(final Object maybeWrapped) {
        if (!(maybeWrapped instanceof ObjectWrapper)) {
            return maybeWrapped;
        } else {
            return ((ObjectWrapper)maybeWrapped).getObject();
        }
    }
    
    private static ArrayList emptyArrayList() {
        return new ArrayList(0);
    }


}
