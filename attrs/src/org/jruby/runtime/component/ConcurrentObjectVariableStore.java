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
package org.jruby.runtime.component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.Variable;
import org.jruby.util.IdUtil;

/**
 * 
 * @author Bill Dortch.
 * 
 * Hash functionality adapted from java.util.concurrent.ConcurrentHashMap, (C) Sun Microsystems, Inc.
 *
 */
public class ConcurrentObjectVariableStore<BaseObjectType>
extends VariableStore<BaseObjectType>
implements Serializable {
    
    //
    // STATICS
    //
    
    protected static final class Entry<BaseObjectType> {
        final int hash;
        final String name;
        volatile BaseObjectType value;
        final Entry<BaseObjectType> next;
        
        // note that volatile value is not passed to the constructor. the ConcurrentHashMap
        // approach is to set the value here, but then the value must be checked for null
        // on every read on the off-to-nonexistent chance a compiler might reorder the
        // initialization with respect to insertion of the entry into the table. the approach
        // here is to set the value externally after the entry is instantiated, and before
        // it is inserted into the table. it should not be permissible for a compiler to 
        // reorder those operations (TODO: confirm!).  this will be a little slower, but it
        // only applies the first time a variable is written (though also on rehashes), while
        // it allows every read to be a little faster.
        Entry(final int hash, final String name, final Entry<BaseObjectType> next) {
            this.hash = hash;
            this.name = name;
            this.next = next;
        }
    }
    
    // array allocator prevents warning messages, as generics
    // don't support parameterized array creation
    @SuppressWarnings("unchecked")
    private static final <BaseObjectType> Entry<BaseObjectType>[] newArray(final int i) {
        return new Entry[i];
    }
    
    protected static final int DEFAULT_INITIAL_CAPACITY = 8; // must be a power of 2!
    protected static final int MAXIMUM_CAPACITY = 1 << 30;
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;

    //
    // INSTANCE FIELDS
    //
    
    protected final ReentrantLock tableLock = new ReentrantLock();
    protected transient volatile Entry<BaseObjectType>[] table;
    protected transient int size;
    protected transient int threshold;
    protected final float loadFactor;
    
    //
    // SERIALIZATION METHODS
    //
    
    private static final long serialVersionUID = -1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            out.defaultWriteObject();
            Entry<BaseObjectType>[] tab = table;
            final int len;
            out.writeInt(len = tab.length);
            for (int i = len; --i >= 0; ) {
                for (Entry<BaseObjectType> e = tab[i]; e != null; e = e.next) {
                    out.writeObject(e.name);
                    out.writeObject(e.value);
                }
            }
            // null key signals end of data
            out.writeObject(null);
        } finally {
            lock.unlock();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // FIXME: need to initialize runtime (Ruby) reference here
        final int len = in.readInt();
        threshold = (int)(len * loadFactor);
        table = newArray(len);
        for (String name = (String)in.readObject(); name != null; name = (String)in.readObject()) {
            fastStoreValue(name.intern(), (BaseObjectType)in.readObject());
        }
    }
    
    //
    // CONSTRUCTORS
    //
    
    public ConcurrentObjectVariableStore(final Ruby runtime, final BaseObjectType owner) {
        super(runtime, owner);
        loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = newArray(DEFAULT_INITIAL_CAPACITY);
    }

    public ConcurrentObjectVariableStore(
            final Ruby runtime,
            final BaseObjectType owner,
            int initialCapacity,
            final float loadFactor) {
        super(runtime, owner);
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);
        final int capacity = nextCapacity(initialCapacity);
        table = newArray(capacity);
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
    }
    
    // returns the next valid (power of two) capacity
    protected static final int nextCapacity(final int requestedCapacity) {
        int capacity = 1;
        while (capacity < requestedCapacity)
            capacity <<= 1;
        return capacity;
    }
    
    public ConcurrentObjectVariableStore(
            final Ruby runtime,
            final BaseObjectType owner,
            final int initialCapacity) {
        this(runtime, owner, initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    
    public ConcurrentObjectVariableStore(
            final Ruby runtime,
            final BaseObjectType owner,
            final List<Variable<BaseObjectType>> varList) {
        this(runtime, owner, (int)(varList.size()/DEFAULT_LOAD_FACTOR) + 1, DEFAULT_LOAD_FACTOR);
        ReentrantLock lock = tableLock;
        lock.lock();
        try {
            for (Variable<BaseObjectType> var : varList) {
                String name = var.getName().intern();
                BaseObjectType value = var.getValue();
                switch(IdUtil.getVarType(name)) {
                case IdUtil.CONSTANT:
                    fastStoreConstant(name, value);
                    break;
                case IdUtil.INSTANCE_VAR:
                    fastStoreInstanceVariable(name, value);
                    break;
                case IdUtil.CLASS_VAR:
                    fastStoreClassVariable(name, value);
                    break;
                default:
                    fastStoreInternalVariable(name, value);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Constructor provided to ease transition to new variables mechanism. May
     * be deprecated in the near future.
     */
    @SuppressWarnings("unchecked")
    public ConcurrentObjectVariableStore(
            final Ruby runtime,
            final BaseObjectType owner,
            final Map varMap) {
        this(runtime, owner, (int)(varMap.size()/DEFAULT_LOAD_FACTOR) + 1, DEFAULT_LOAD_FACTOR);
        ReentrantLock lock = tableLock;
        lock.lock();
        try {
            synchronized(varMap) {
                for (Iterator iter = varMap.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry)iter.next();
                    String name = ((String)entry.getKey()).intern();
                    BaseObjectType value = (BaseObjectType)entry.getValue();
                    switch(IdUtil.getVarType(name)) {
                    case IdUtil.CONSTANT:
                        fastStoreConstant(name, value);
                        break;
                    case IdUtil.INSTANCE_VAR:
                        fastStoreInstanceVariable(name, value);
                        break;
                    case IdUtil.CLASS_VAR:
                        fastStoreClassVariable(name, value);
                        break;
                    default:
                        fastStoreInternalVariable(name, value);
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // 
    // INTERNAL METHODS
    //
    
    protected final BaseObjectType fetchValue(final String name) {
        Entry<BaseObjectType> e;
        final int hash = name.hashCode();
        final Entry<BaseObjectType>[] table = this.table;
        for (e = table[hash & (table.length - 1)]; e != null; e = e.next) {
            if (hash == e.hash && name.equals(e.name)) {
                return e.value;
            }
        }
        return null;
    }

    protected final BaseObjectType fastFetchValue(final String internedName) {
        //assert internedName == internedName.intern() : internedName + " not interned";
        Entry<BaseObjectType> e;
        final Entry<BaseObjectType>[] table = this.table;
        for (e = table[internedName.hashCode() & (table.length - 1)]; e != null; e = e.next) {
            if (internedName == e.name) {
                return e.value;
            }
        }
        return null;
    }
    
    protected final void storeValue(final String name, final BaseObjectType value) {
        final int hash = name.hashCode();
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            Entry<BaseObjectType>[] table = this.table; // read-volatile
            final int s;
            if ((s = size + 1) > threshold) {
                table = rehash();
            }
            Entry<BaseObjectType> e;
            final int index;
            for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
                if (hash == e.hash && name.equals(e.name)) {
                    e.value = value;
                    return;
                }
            }
            // external volatile value initialization intended to obviate the need for
            // readValueUnderLock technique used in ConcurrentHashMap. may be a little
            // slower, but better to pay a price on first write rather than all reads.
            e = new Entry<BaseObjectType>(hash, name.intern(), table[index]);
            e.value = value;
            table[index] = e;
            size = s;
            this.table = table; // write-volatile

        } finally {
            lock.unlock();
        }
    }
    
    protected final void fastStoreValue(final String internedName, final BaseObjectType value) {
        assert internedName == internedName.intern() : internedName + " not interned";
        final int hash = internedName.hashCode();
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            Entry<BaseObjectType>[] table = this.table; // read-volatile
            final int s;
            if ((s = size + 1) > threshold) {
                table = rehash();
            }
            Entry<BaseObjectType> e;
            final int index;
            for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
                if (internedName == e.name) {
                    e.value = value;
                    return;
                }
            }
            // external volatile value initialization intended to obviate the need for
            // readValueUnderLock technique used in ConcurrentHashMap. may be a little
            // slower, but better to pay a price on first write rather than all reads.
            e = new Entry<BaseObjectType>(hash, internedName, table[index]);
            e.value = value;
            table[index] = e;
            size = s;
            this.table = table; // write-volatile

        } finally {
            lock.unlock();
        }
    }
    
    protected final BaseObjectType delete(final String name) {
        final int hash = name.hashCode();
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            final Entry<BaseObjectType>[] tab = this.table;
            final int s = size - 1;
            final int index = hash & (tab.length - 1);
            Entry<BaseObjectType> first = tab[index];
            Entry<BaseObjectType> e;
            for (e = first; e != null; e = e.next) {
                if (hash == e.hash && name.equals(e.name)) {
                    BaseObjectType oldValue = e.value;
                    // All entries following removed node can stay
                    // in list, but all preceding ones need to be
                    // cloned.
                    Entry<BaseObjectType> newFirst = e.next;
                    for (Entry<BaseObjectType> p = first; p != e; p = p.next) {
                        newFirst = new Entry<BaseObjectType>(p.hash, p.name, newFirst);
                        newFirst.value = p.value;
                    }
                    tab[index] = newFirst;
                    size = s;
                    this.table = tab; // write-volatile 
                    return oldValue;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }
    
    protected final boolean containsName(final String name) {
        Entry<BaseObjectType> e;
        final int hash = name.hashCode();
        final Entry<BaseObjectType>[] table = this.table;
        for (e = table[hash & (table.length - 1)]; e != null; e = e.next) {
            if (hash == e.hash && name.equals(e.name)) {
                return true;
            }
        }
        return false;
    }
    
    protected final boolean fastContainsName(final String internedName) {
        //assert internedName == internedName.intern() : internedName + " not interned";
        Entry<BaseObjectType> e;
        final Entry<BaseObjectType>[] table = this.table;
        for (e = table[internedName.hashCode() & (table.length - 1)]; e != null; e = e.next) {
            if (internedName == e.name) {
                return true;
            }
        }
        return false;
    }

    // TODO: not sure we have a use case for this; if so, need to look
    // at implications of calling value.equals(v)
    protected final boolean containsValue(final BaseObjectType value) {
        Entry<BaseObjectType> e;
        final Entry<BaseObjectType>[] table = this.table;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (value.equals(e.value)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected final Entry<BaseObjectType>[] rehash() {
        final Entry<BaseObjectType>[] oldTable = table;
        final int oldCapacity;
        if ((oldCapacity = oldTable.length) >= MAXIMUM_CAPACITY) {
            return oldTable;
        }
        
        final int newCapacity = oldCapacity << 1;
        final Entry<BaseObjectType>[] newTable = newArray(newCapacity);
        threshold = (int)(newCapacity * loadFactor);
        final int sizeMask = newCapacity - 1;
        Entry<BaseObjectType> e;
        for (int i = oldCapacity; --i >= 0; ) {
            // We need to guarantee that any existing reads of old Map can
            //  proceed. So we cannot yet null out each bin.
            e = oldTable[i];

            if (e != null) {
                Entry<BaseObjectType> next = e.next;
                int idx = e.hash & sizeMask;

                //  Single node on list
                if (next == null)
                    newTable[idx] = e;

                else {
                    // Reuse trailing consecutive sequence at same slot
                    Entry<BaseObjectType> lastRun = e;
                    int lastIdx = idx;
                    for (Entry<BaseObjectType> last = next;
                         last != null;
                         last = last.next) {
                        int k = last.hash & sizeMask;
                        if (k != lastIdx) {
                            lastIdx = k;
                            lastRun = last;
                        }
                    }
                    newTable[lastIdx] = lastRun;

                    // Clone all remaining nodes
                    for (Entry<BaseObjectType> p = e; p != lastRun; p = p.next) {
                        int k = p.hash & sizeMask;
                        Entry<BaseObjectType> m = new Entry<BaseObjectType>(p.hash, p.name, newTable[k]);
                        m.value = p.value;
                        newTable[k] = m;
                    }
                }
            }
        }
        table = newTable;
        return newTable;
    }
    
    public int size() {
        if (table != null) {
            return size;
        }
        return 0;
    }
    
    public boolean isEmpty() {
        if (table != null) {
            return size == 0;
        }
        return true;
    }
    
    public void syncVariables(final List<Variable<BaseObjectType>> varList) {
        // similar to doing a clear() followed by a putAll() in a ConcurrentHashMap.
        
        // TODO: in theory, a reader could see this store in a transitional state
        // (after old table is cleared and before all new values are set).  In practice
        // (the way this method is actually used), that won't happen, since this
        // is always called before the owner object has actually been used (as
        // part of a dup or clone operation, for example). Still, it might (someday)
        // be useful if this method transitioned cleanly.
        
        final int capacity = nextCapacity(Math.max(
                ((int)(varList.size()/DEFAULT_LOAD_FACTOR) + 1),
                DEFAULT_INITIAL_CAPACITY));
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            size = 0;
            threshold = (int)(capacity * loadFactor);
            table = newArray(capacity);
            
            for (Variable<BaseObjectType> var : varList) {
                String name = var.getName().intern();
                BaseObjectType value = var.getValue();
                switch(IdUtil.getVarType(name)) {
                case IdUtil.CONSTANT:
                    fastStoreConstant(name, value);
                    break;
                case IdUtil.INSTANCE_VAR:
                    fastStoreInstanceVariable(name, value);
                    break;
                case IdUtil.CLASS_VAR:
                    fastStoreClassVariable(name, value);
                    break;
                default:
                    fastStoreInternalVariable(name, value);
                    break;
                }
            }
            
        } finally {
            lock.unlock();
        }
    }
    
    //
    // PSEUDO/INTERNAL VARIABLES (i.e, not ivar/cvar/constant)
    // Don't use these methods for real variables (ivar/cvar/constant),
    // as that may fail in implementations that store different variable
    // types separately.
    //

    public boolean hasInternalVariable(final String name) {
        assert !isRubyVariable(name);
        return containsName(name);
    }
    
    public boolean fastHasInternalVariable(final String internedName) {
        assert !isRubyVariable(internedName);
        return fastContainsName(internedName);
    }
    
    public BaseObjectType fetchInternalVariable(final String name) {
        assert !isRubyVariable(name);
        return fetchValue(name);
    }

    public BaseObjectType fastFetchInternalVariable(final String internedName) {
        assert !isRubyVariable(internedName);
        return fastFetchValue(internedName);
    }
    
    public void storeInternalVariable(final String name, final BaseObjectType value) {
        assert !isRubyVariable(name);
        storeValue(name, value);
    }
    
    public void fastStoreInternalVariable(final String internedName, final BaseObjectType value) {
        assert !isRubyVariable(internedName);
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType deleteInternalVariable(final String name) {
        assert !isRubyVariable(name);
        return delete(name);
    }
    
    //
    // INSTANCE VARIABLES
    // Don't use these methods for non-ivars
    //
    
    public boolean hasInstanceVariable(final String name) {
        return containsName(name);
    }
    
    public boolean fastHasInstanceVariable(final String internedName) {
        return fastContainsName(internedName);
    }
    
    public boolean validatedHasInstanceVariable(final String name) {
        validateInstanceVariable(name);
        return containsName(name);
    }
    
    public boolean fastValidatedHasInstanceVariable(final String internedName) {
        validateInstanceVariable(internedName);
        return fastContainsName(internedName);
    }
    
    public BaseObjectType fetchInstanceVariable(final String name) {
        Entry<BaseObjectType> e;
        final int hash = name.hashCode();
        final Entry<BaseObjectType>[] table = this.table;
        for (e = table[hash & (table.length - 1)]; e != null; e = e.next) {
            if (hash == e.hash && name.equals(e.name)) {
                return e.value;
            }
        }
        return null;
    }
    
    public BaseObjectType fastFetchInstanceVariable(final String internedName) {
        Entry<BaseObjectType> e;
        final Entry<BaseObjectType>[] table = this.table;
        for (e = table[internedName.hashCode() & (table.length - 1)]; e != null; e = e.next) {
            if (internedName == e.name) {
                return e.value;
            }
        }
        return null;
    }

    public BaseObjectType validatedFetchInstanceVariable(final String name) {
        validateInstanceVariable(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastValidatedFetchInstanceVariable(final String internedName) {
        validateInstanceVariable(internedName);
        return fastFetchValue(internedName);
    }
    
    public void storeInstanceVariable(final String name, final BaseObjectType value) {
        assert IdUtil.isInstanceVariable(name) && value != null : 
            "invalid instance variable, name = " + name + ", value = " + value;
        checkInstanceVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastStoreInstanceVariable(final String internedName, final BaseObjectType value) {
        assert IdUtil.isInstanceVariable(internedName) && value != null : 
            "invalid instance variable, name = " + internedName + ", value = " + value;
        checkInstanceVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public void validatedStoreInstanceVariable(final String name, final BaseObjectType value) {
        assert value != null : name + " is null";
        validateInstanceVariable(name);
        checkInstanceVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastValidatedStoreInstanceVariable(final String internedName, final BaseObjectType value) {
        assert value != null : internedName + " is null";
        validateInstanceVariable(internedName);
        checkInstanceVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType deleteInstanceVariable(final String name) {
        assert IdUtil.isInstanceVariable(name);
        checkInstanceVariablesSettable();
        return delete(name);
    }
    
    public BaseObjectType validatedDeleteInstanceVariable(final String name) {
        validateInstanceVariable(name);
        checkInstanceVariablesSettable();
        return delete(name);
    }
    

    //
    // CLASS VARIABLES
    // Don't use these methods for non-cvars
    //
    
    public boolean hasClassVariable(final String name) {
        assert IdUtil.isClassVariable(name);
        return containsName(name);
    }
    
    public boolean fastHasClassVariable(final String internedName) {
        assert IdUtil.isClassVariable(internedName);
        return fastContainsName(internedName);
    }
    
    public boolean validatedHasClassVariable(final String name) {
        validateClassVariable(name);
        return containsName(name);
    }
    
    public boolean fastValidatedHasClassVariable(final String internedName) {
        validateClassVariable(internedName);
        return fastContainsName(internedName);
    }
    
    public BaseObjectType fetchClassVariable(final String name) {
        return fetchValue(name);
    }
    
    public BaseObjectType fastFetchClassVariable(final String internedName) {
        return fastFetchValue(internedName);
    }

    public BaseObjectType validatedFetchClassVariable(final String name) {
        validateClassVariable(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastValidatedFetchClassVariable(final String internedName) {
        validateClassVariable(internedName);
        return fastFetchValue(internedName);
    }
    
    public void storeClassVariable(final String name, final BaseObjectType value) {
        assert IdUtil.isClassVariable(name);
        assert value != null : name + " is null";
        checkClassVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastStoreClassVariable(final String internedName, final BaseObjectType value) {
        assert IdUtil.isClassVariable(internedName);
        assert value != null : internedName + " is null";
        checkClassVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public void validatedStoreClassVariable(final String name, final BaseObjectType value) {
        assert value != null : name + " is null";
        validateClassVariable(name);
        checkClassVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastValidatedStoreClassVariable(final String internedName, final BaseObjectType value) {
        assert value != null : internedName + " is null";
        validateClassVariable(internedName);
        checkClassVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType deleteClassVariable(final String name) {
        assert IdUtil.isClassVariable(name);
        checkClassVariablesSettable();
        return delete(name);
    }
    
    public BaseObjectType validatedDeleteClassVariable(final String name) {
        validateClassVariable(name);
        checkClassVariablesSettable();
        return delete(name);
    }
    
    //
    // CONSTANTS
    // Don't use these methods for non-constants
    //
    
    public boolean hasConstant(final String name) {
        //assert IdUtil.isConstant(name);
        return containsName(name);
    }
    
    public boolean fastHasConstant(final String internedName) {
        //assert IdUtil.isConstant(internedName);
        return fastContainsName(internedName);
    }
    
    public boolean validatedHasConstant(final String name) {
        validateConstant(name);
        return containsName(name);
    }
    
    public boolean fastValidatedHasConstant(final String internedName) {
        validateConstant(internedName);
        return fastContainsName(internedName);
    }
    
    public BaseObjectType fetchConstant(final String name) {
        //assert IdUtil.isConstant(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastFetchConstant(final String internedName) {
        //assert IdUtil.isConstant(internedName);
        return fastFetchValue(internedName);
    }

    public BaseObjectType validatedFetchConstant(final String name) {
        validateConstant(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastValidatedFetchConstant(final String internedName) {
        validateConstant(internedName);
        return fastFetchValue(internedName);
    }
    
    public void storeConstant(final String name, final BaseObjectType value) {
        assert IdUtil.isConstant(name);
        assert value != null : name + " is null";
        checkConstantsSettable();
        storeValue(name, value);
    }
    
    public void fastStoreConstant(final String internedName, final BaseObjectType value) {
        assert IdUtil.isConstant(internedName);
        assert value != null : internedName + " is null";
        checkConstantsSettable();
        fastStoreValue(internedName, value);
    }
    
    public void validatedStoreConstant(final String name, final BaseObjectType value) {
        assert value != null : name + " is null";
        validateConstant(name);
        checkConstantsSettable();
        storeValue(name, value);
    }
    
    public void fastValidatedStoreConstant(final String internedName, final BaseObjectType value) {
        assert value != null : internedName + " is null";
        validateConstant(internedName);
        checkConstantsSettable();
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType deleteConstant(final String name) {
        assert IdUtil.isConstant(name);
        checkConstantsSettable();
        return delete(name);
    }
    
    public BaseObjectType validatedDeleteConstant(final String name) {
        validateConstant(name);
        checkConstantsSettable();
        return delete(name);
    }
    
    //
    // LIST METHODS
    //
    
    /**
     * Returns all variables - instance variables, class variables, constants,
     * and any internal "variables".  Use this in place of the old
     * RubyObject#getInstanceVariablesSnapshot method.
     */
    public List<Variable<BaseObjectType>> getStoredVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>(this.size);
        Entry<BaseObjectType> e;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                list.add(new VariableEntry<BaseObjectType>(e.name, e.value));
            }
        }
        return list;
    }
    
    /**
     * Returns only "internal" variables - those that are NOT instance
     * variables, class variables, or constants.
     */
    public List<Variable<BaseObjectType>> getStoredInternalVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>();
        Entry<BaseObjectType> e;
        String name;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (!isRubyVariable(name = e.name)) {
                    list.add(new VariableEntry<BaseObjectType>(name, e.value));
                }
            }
        }
        return list;
    }
    
    public List<String> getStoredVariableNameList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<String> list = new ArrayList<String>(this.size);
        Entry<BaseObjectType> e;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                list.add(e.name);
            }
        }
        return list;
    }
    
    public List<Variable<BaseObjectType>> getStoredInstanceVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>(this.size);
        Entry<BaseObjectType> e;
        String name;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isInstanceVariable(name = e.name)) {
                    list.add(new VariableEntry<BaseObjectType>(name, e.value));
                }
            }
        }
        return list;
    }
    
    public List<String> getStoredInstanceVariableNameList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<String> list = new ArrayList<String>(this.size);
        Entry<BaseObjectType> e;
        String name;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isInstanceVariable(name = e.name)) {
                    list.add(name);
                }
            }
        }
        return list;
    }
    
    public List<Variable<BaseObjectType>> getStoredClassVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list = new ArrayList<Variable<BaseObjectType>>();
        Entry<BaseObjectType> e;
        String name;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isClassVariable(name = e.name)) {
                    list.add(new VariableEntry<BaseObjectType>(name, e.value));
                }
            }
        }
        return list;
    }
    
    public List<String> getStoredClassVariableNameList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<String> list = new ArrayList<String>();
        Entry<BaseObjectType> e;
        String name;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isClassVariable(name = e.name)) {
                    list.add(name);
                }
            }
        }
        return list;
    }
    
    public List<Variable<BaseObjectType>> getStoredConstantList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>(this.size);
        Entry<BaseObjectType> e;
        String name;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isConstant(name = e.name)) {
                    list.add(new VariableEntry<BaseObjectType>(name, e.value));
                }
            }
        }
        return list;
    }
    
    public List<String> getStoredConstantNameList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<String> list = new ArrayList<String>(this.size);
        Entry<BaseObjectType> e;
        String name;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isConstant(name = e.name)) {
                    list.add(name);
                }
            }
        }
        return list;
    }
    
    /**
     * May be deprecated in the near future, see note at VariableStore.
     */
    @SuppressWarnings("unchecked")
    public Map getStoredVariableMap() {
        final Entry<BaseObjectType>[] table = this.table;
        final HashMap map = new HashMap(((int)(this.size / this.loadFactor)) + 2); 
        Entry<BaseObjectType> e;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                map.put(e.name, e.value);
            }
        }
        return map;
    }
 
    
}
