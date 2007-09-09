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
public final class ConcurrentObjectVariableStore<BaseObjectType>
extends VariableStore<BaseObjectType>
implements Serializable {
    
    static final int DEFAULT_INITIAL_CAPACITY = 8;
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    final ReentrantLock tableLock = new ReentrantLock();
    transient volatile Entry<BaseObjectType>[] table;
    // FIXME: does +size+ still need to be volatile?  I'm writing back to
    // +table+ after structural updates in the belief that doing so establishes
    // the necessary happens-before ordering, so reading a separate volatile
    // (size) before gets/fetches shouldn't be necessary... I think... -bd
    transient volatile int size;
    transient int threshold;
    final float loadFactor;
    
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
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        table = newArray(capacity);
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
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
            final List<Variable<BaseObjectType>> attrList) {
        this(runtime, owner, (int)(attrList.size()/DEFAULT_LOAD_FACTOR) + 1, DEFAULT_LOAD_FACTOR);
        ReentrantLock lock = tableLock;
        lock.lock();
        try {
            for (Variable<BaseObjectType> attr : attrList) {
                String name = attr.getName().intern();
                BaseObjectType value = attr.getValue();
                switch(IdUtil.getVarType(name)) {
                case IdUtil.CONSTANT:
                    fastSetConstant(name, value);
                    break;
                case IdUtil.INSTANCE_VAR:
                    fastSetInstanceVariable(name, value);
                    break;
                case IdUtil.CLASS_VAR:
                    fastSetClassVariable(name, value);
                    break;
                default:
                    fastSetInternalVariable(name, value);
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Constructor provided to ease transition to new attributes mechanism. May
     * be deprecated in the near future.
     */
    @SuppressWarnings("unchecked")
    public ConcurrentObjectVariableStore(
            final Ruby runtime,
            final BaseObjectType owner,
            final Map attrMap) {
        this(runtime, owner, (int)(attrMap.size()/DEFAULT_LOAD_FACTOR) + 1, DEFAULT_LOAD_FACTOR);
        ReentrantLock lock = tableLock;
        lock.lock();
        try {
            synchronized(attrMap) {
                for (Iterator iter = attrMap.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry)iter.next();
                    String name = ((String)entry.getKey()).intern();
                    BaseObjectType value = (BaseObjectType)entry.getValue();
                    switch(IdUtil.getVarType(name)) {
                    case IdUtil.CONSTANT:
                        fastSetConstant(name, value);
                        break;
                    case IdUtil.INSTANCE_VAR:
                        fastSetInstanceVariable(name, value);
                        break;
                    case IdUtil.CLASS_VAR:
                        fastSetClassVariable(name, value);
                        break;
                    default:
                        fastSetInternalVariable(name, value);
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
    
    private final BaseObjectType readValueUnderLock(final Entry<BaseObjectType> e) {
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            return e.value;
        } finally {
            lock.unlock();
        }
    }
    
    private final BaseObjectType fetchValue(final String name) {
//        if (size > 0) { // read-volatile
            Entry<BaseObjectType> e;
            final BaseObjectType value;
            final int hash = name.hashCode();
            final Entry<BaseObjectType>[] table = this.table;
            for (e = table[hash & (table.length - 1)]; e != null; e = e.next) {
                if (hash == e.hash && name.equals(e.name)) {
                    //if ((value = e.value) != null) {
                    //    return value;
                    //}
                    //return readValueUnderLock(e);
                    return e.value;
                }
            }
//        }
        return null;
    }

    private final BaseObjectType fastFetchValue(final String internedName) {
        assert internedName == internedName.intern() : internedName + " not interned";
//        if (size > 0) { // read-volatile
            Entry<BaseObjectType> e;
            final BaseObjectType value;
            final Entry<BaseObjectType>[] table = this.table;
            for (e = table[internedName.hashCode() & (table.length - 1)]; e != null; e = e.next) {
                if (internedName == e.name) {
                    //if ((value = e.value) != null) {
                    ///    return value;
                    //}
                    //return readValueUnderLock(e);
                    return e.value;
                }
            }
//        }
        return null;
    }
    
    private final void storeValue(final String name, final BaseObjectType value) {
        // TODO: null value may be legal someday (if lightweight null => nil)
        assert value != null;
        final int hash = name.hashCode();
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            final int s;
            if ((s = size + 1) > threshold) {
                rehash();
            }
            Entry<BaseObjectType> e;
            final Entry<BaseObjectType>[] table = this.table;
            final int index;
            for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
                if (hash == e.hash && name.equals(e.name)) {
                    e.value = value;
                    return;
                }
            }
            
            table[index] = (e = new Entry<BaseObjectType>(hash, name.intern(), value, table[index]));
            size = s; // write-volatile
            // this is also write-volatile - since we have to read this anyway,
            // I think we should be able to forego the read of +size+ for getXxx
            // methods (or at least that's how I read the JMM)
            this.table = table;

        } finally {
            lock.unlock();
        }
    }
    
    private final void fastStoreValue(final String internedName, final BaseObjectType value) {
        // TODO: null value may be legal someday (if lightweight null => nil)
        assert value != null;
        assert internedName == internedName.intern() : internedName + " not interned";
        final int hash = internedName.hashCode();
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            final int s;
            if ((s = size + 1) > threshold) {
                rehash();
            }
            Entry<BaseObjectType> e;
            final Entry<BaseObjectType>[] table = this.table;
            final int index;
            for (e = table[index = hash & (table.length - 1)]; e != null; e = e.next) {
                if (internedName == e.name) {
                    e.value = value;
                    return;
                }
            }
            
            table[index] = (e = new Entry<BaseObjectType>(hash, internedName, value, table[index]));
            size = s; // write-volatile
            // this is also write-volatile - since we have to read this anyway,
            // I think we should be able to forego the read of +size+ for getXxx
            // methods (or at least that's how I read the JMM)
            this.table = table;

        } finally {
            lock.unlock();
        }
    }
    
    private final BaseObjectType remove(final String name) {
        final int hash = name.hashCode();
        final ReentrantLock lock;
        (lock = tableLock).lock();
        try {
            final int s = size - 1;
            final Entry<BaseObjectType>[] tab = this.table;
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
                    for (Entry<BaseObjectType> p = first; p != e; p = p.next)
                        newFirst = new Entry<BaseObjectType>(p.hash, p.name, p.value, newFirst);
                    tab[index] = newFirst;
                    size = s; // write-volatile
                    // this is also write-volatile - since we have to read this anyway,
                    // I think we should be able to forego the read of +size+ for getXxx
                    // methods (or at least that's how I read the JMM)
                    this.table = tab; 
                    return oldValue;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }
    
    private final boolean containsName(final String name) {
//        if (size > 0) { // read-volatile
            Entry<BaseObjectType> e;
            final int hash = name.hashCode();
            final Entry<BaseObjectType>[] table = this.table;
            for (e = table[hash & (table.length - 1)]; e != null; e = e.next) {
                if (hash == e.hash && name.equals(e.name)) {
                    return true;
                }
            }
//        }
        return false;
    }
    
    private final boolean fastContainsName(final String internedName) {
        assert internedName == internedName.intern() : internedName + " not interned";
        Entry<BaseObjectType> e;
//        if (size > 0) { // read-volatile
            final Entry<BaseObjectType>[] table = this.table;
            for (e = table[internedName.hashCode() & (table.length - 1)]; e != null; e = e.next) {
                if (internedName == e.name) {
                    return true;
                }
            }
//        }
        return false;
    }

    // TODO: not sure we have a use case for this; if so, need to look
    // at implications of calling value.equals(v)
    private final boolean containsValue(final BaseObjectType value) {
        if (size != 0) {
            Entry<BaseObjectType> e;
            BaseObjectType v;
            final Entry<BaseObjectType>[] table = this.table;
            for (int i = table.length; --i >= 0; ) {
                for (e = table[i]; e != null; e = e.next) {
                    if ((v = e.value) == null) {
                        v = readValueUnderLock(e);
                    }
                    if (value.equals(v)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private final void rehash() {
        final Entry<BaseObjectType>[] oldTable = table;
        final int oldCapacity;
        if ((oldCapacity = oldTable.length) >= MAXIMUM_CAPACITY) {
            return;
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
                        Entry<BaseObjectType> n = newTable[k];
                        newTable[k] = new Entry<BaseObjectType>(p.hash, p.name, p.value, n);
                    }
                }
            }
        }
        table = newTable;       
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
    
    //
    // PSEUDO/INTERNAL ATTRIBUTES (i.e, not ivar/cvar/constant)
    // Don't use these methods for real attributes (ivar/cvar/constant),
    // as that may fail in implementations that store different attribute
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
    
    public BaseObjectType getInternalVariable(final String name) {
        assert !isRubyVariable(name);
        return fetchValue(name);
    }

    public BaseObjectType fastGetInternalVariable(final String internedName) {
        assert !isRubyVariable(internedName);
        return fastFetchValue(internedName);
    }
    
    public void setInternalVariable(final String name, final BaseObjectType value) {
        assert !isRubyVariable(name);
        storeValue(name, value);
    }
    
    public void fastSetInternalVariable(final String internedName, final BaseObjectType value) {
        assert !isRubyVariable(internedName);
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType removeInternalVariable(final String name) {
        assert !isRubyVariable(name);
        return remove(name);
    }
    
    //
    // INSTANCE VARIABLES
    // Don't use these methods for non-ivars
    //
    
    public boolean hasInstanceVariable(final String name) {
        assert IdUtil.isInstanceVariable(name);
        return containsName(name);
    }
    
    public boolean fastHasInstanceVariable(final String internedName) {
        assert IdUtil.isInstanceVariable(internedName);
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
    
    public BaseObjectType getInstanceVariable(final String name) {
        assert IdUtil.isInstanceVariable(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastGetInstanceVariable(final String internedName) {
        assert IdUtil.isInstanceVariable(internedName);
        return fastFetchValue(internedName);
    }

    public BaseObjectType validatedGetInstanceVariable(final String name) {
        validateInstanceVariable(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastValidatedGetInstanceVariable(final String internedName) {
        validateInstanceVariable(internedName);
        return fastFetchValue(internedName);
    }
    
    public void setInstanceVariable(final String name, final BaseObjectType value) {
        assert IdUtil.isInstanceVariable(name);
        checkInstanceVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastSetInstanceVariable(final String internedName, final BaseObjectType value) {
        assert IdUtil.isInstanceVariable(internedName);
        checkInstanceVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public void validatedSetInstanceVariable(final String name, final BaseObjectType value) {
        validateInstanceVariable(name);
        checkInstanceVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastValidatedSetInstanceVariable(final String internedName, final BaseObjectType value) {
        validateInstanceVariable(internedName);
        checkInstanceVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType removeInstanceVariable(final String name) {
        assert IdUtil.isInstanceVariable(name);
        checkInstanceVariablesSettable();
        return remove(name);
    }
    
    public BaseObjectType validatedRemoveInstanceVariable(final String name) {
        validateInstanceVariable(name);
        checkInstanceVariablesSettable();
        return remove(name);
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
    
    public BaseObjectType getClassVariable(final String name) {
        assert IdUtil.isClassVariable(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastGetClassVariable(final String internedName) {
        assert IdUtil.isClassVariable(internedName);
        return fastFetchValue(internedName);
    }

    public BaseObjectType validatedGetClassVariable(final String name) {
        validateClassVariable(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastValidatedGetClassVariable(final String internedName) {
        validateClassVariable(internedName);
        return fastFetchValue(internedName);
    }
    
    public void setClassVariable(final String name, final BaseObjectType value) {
        assert IdUtil.isClassVariable(name);
        checkClassVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastSetClassVariable(final String internedName, final BaseObjectType value) {
        assert IdUtil.isClassVariable(internedName);
        checkClassVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public void validatedSetClassVariable(final String name, final BaseObjectType value) {
        validateClassVariable(name);
        checkClassVariablesSettable();
        storeValue(name, value);
    }
    
    public void fastValidatedSetClassVariable(final String internedName, final BaseObjectType value) {
        validateClassVariable(internedName);
        checkClassVariablesSettable();
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType removeClassVariable(final String name) {
        assert IdUtil.isClassVariable(name);
        checkClassVariablesSettable();
        return remove(name);
    }
    
    public BaseObjectType validatedRemoveClassVariable(final String name) {
        validateClassVariable(name);
        checkClassVariablesSettable();
        return remove(name);
    }
    
    //
    // CONSTANTS
    // Don't use these methods for non-constants
    //
    
    public boolean hasConstant(final String name) {
        assert IdUtil.isConstant(name);
        return containsName(name);
    }
    
    public boolean fastHasConstant(final String internedName) {
        assert IdUtil.isConstant(internedName);
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
    
    public BaseObjectType getConstant(final String name) {
        assert IdUtil.isConstant(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastGetConstant(final String internedName) {
        assert IdUtil.isConstant(internedName);
        return fastFetchValue(internedName);
    }

    public BaseObjectType validatedGetConstant(final String name) {
        validateConstant(name);
        return fetchValue(name);
    }
    
    public BaseObjectType fastValidatedGetConstant(final String internedName) {
        validateConstant(internedName);
        return fastFetchValue(internedName);
    }
    
    public void setConstant(final String name, final BaseObjectType value) {
        assert IdUtil.isConstant(name);
        checkConstantsSettable();
        storeValue(name, value);
    }
    
    public void fastSetConstant(final String internedName, final BaseObjectType value) {
        assert IdUtil.isConstant(internedName);
        checkConstantsSettable();
        fastStoreValue(internedName, value);
    }
    
    public void validatedSetConstant(final String name, final BaseObjectType value) {
        validateConstant(name);
        checkConstantsSettable();
        storeValue(name, value);
    }
    
    public void fastValidatedSetConstant(final String internedName, final BaseObjectType value) {
        validateConstant(internedName);
        checkConstantsSettable();
        fastStoreValue(internedName, value);
    }
    
    public BaseObjectType removeConstant(final String name) {
        assert IdUtil.isConstant(name);
        checkConstantsSettable();
        return remove(name);
    }
    
    public BaseObjectType validatedRemoveConstant(final String name) {
        validateConstant(name);
        checkConstantsSettable();
        return remove(name);
    }
    
    //
    // LIST METHODS
    //
    
    /**
     * Returns all attributes - instance variables, class variables, constants,
     * and any "special" attributes.  Use this in place of the old
     * RubyObject#getInstanceVariablesSnapshot method.
     */
    public List<Variable<BaseObjectType>> getVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>(this.size);
        Entry<BaseObjectType> e;
        BaseObjectType value;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if ((value = e.value) == null) {
                    value = readValueUnderLock(e);
                }
                list.add(new VariableEntry<BaseObjectType>(e.name, value));
            }
        }
        return list;
    }
    
    /**
     * Returns only "internal" attributes - those that are NOT instance
     * variables, class variables, or constants.
     */
    public List<Variable<BaseObjectType>> getInternalVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>();
        Entry<BaseObjectType> e;
        String name;
        BaseObjectType value;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (!isRubyVariable(name = e.name)) {
                    if ((value = e.value) == null) {
                        value = readValueUnderLock(e);
                    }
                    list.add(new VariableEntry<BaseObjectType>(name, value));
                }
            }
        }
        return list;
    }
    
    public List<String> getVariableNameList() {
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
    
    public List<Variable<BaseObjectType>> getInstanceVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>(this.size);
        Entry<BaseObjectType> e;
        String name;
        BaseObjectType value;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isInstanceVariable(name = e.name)) {
                    if ((value = e.value) == null) {
                        value = readValueUnderLock(e);
                    }
                    list.add(new VariableEntry<BaseObjectType>(name, value));
                }
            }
        }
        return list;
    }
    
    public List<String> getInstanceVariableNameList() {
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
    
    public List<Variable<BaseObjectType>> getClassVariableList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list = new ArrayList<Variable<BaseObjectType>>();
        Entry<BaseObjectType> e;
        String name;
        BaseObjectType value;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isClassVariable(name = e.name)) {
                    if ((value = e.value) == null) {
                        value = readValueUnderLock(e);
                    }
                    list.add(new VariableEntry<BaseObjectType>(name, value));
                }
            }
        }
        return list;
    }
    
    public List<String> getClassVariableNameList() {
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
    
    public List<Variable<BaseObjectType>> getConstantList() {
        final Entry<BaseObjectType>[] table = this.table;
        final ArrayList<Variable<BaseObjectType>> list =
            new ArrayList<Variable<BaseObjectType>>(this.size);
        Entry<BaseObjectType> e;
        String name;
        BaseObjectType value;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if (IdUtil.isConstant(name = e.name)) {
                    if ((value = e.value) == null) {
                        value = readValueUnderLock(e);
                    }
                    list.add(new VariableEntry<BaseObjectType>(name, value));
                }
            }
        }
        return list;
    }
    
    public List<String> getConstantNameList() {
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
    public Map getVariableMap() {
        final Entry<BaseObjectType>[] table = this.table;
        final HashMap map = new HashMap(((int)(this.size / this.loadFactor)) + 2); 
        Entry<BaseObjectType> e;
        BaseObjectType value;
        for (int i = table.length; --i >= 0; ) {
            for (e = table[i]; e != null; e = e.next) {
                if ((value = e.value) == null) {
                    value = readValueUnderLock(e);
                }
                map.put(e.name, value);
            }
        }
        return map;
    }
 
    
    static final class Entry<BaseObjectType> {
        final int hash;
        final String name;
        volatile BaseObjectType value;
        final Entry<BaseObjectType> next;
        
        Entry(final int hash, final String name, final BaseObjectType value, final Entry<BaseObjectType> next) {
            this.hash = hash;
            this.name = name;
            this.value = value;
            this.next = next;
        }
    }
    
    // array allocator prevents warning messages, as generics
    // don't support parameterized array creation
    @SuppressWarnings("unchecked")
    private static final <BaseObjectType> Entry<BaseObjectType>[] newArray(final int i) {
        return new Entry[i];
    }
    
}
