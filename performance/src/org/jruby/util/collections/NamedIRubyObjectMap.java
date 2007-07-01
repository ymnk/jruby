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

/**
 * Hash code adapted from java.util.HashMap.
 * Copyright (C) Sun Microsystems, Inc.
 */
package org.jruby.util.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The NamedIRubyObjectMap maps <em>interned</em> names to IRubyObjects. It provides
 * superior performance compared to java.util.HashMap (from which much of the code was
 * derived), because:
 * <ul>
 * <li> All comparisons are made using ==
 * <li> Casts are not required when accessing keys (names) or values (objects)
 * <li> Fast methods are provided for retrieving values as arrays or lists
 * </ul>
 * Note that while name interning is ensured for <code>put/set</code> methods,
 * <b>it is up to the caller to ensure that names for <code>get</code> methods are interned</b>.
 * This avoids unnecessary calls to String#intern, as in many (even most) cases, the
 * name will already have been interned (if, for example, it was obtained from
 * <code>RubySymbol#asSymbol</code> or <code>RubyString#asSymbol</code>).
 * <p>
 * NamedIRubyObjectMap implements java.util.Map, and its hash entries implement
 * java.util.Map.Entry, so instances may be passed to code that expect a Map.
 * However, for best performance results, instances should be passed as
 * NamedIRubyObjectMap (or its subclasses, such as InstanceVariables and SymbolTable).
 * 
 * @author Bill Dortch
 *
 */
public class NamedIRubyObjectMap implements Map {

    protected static final int DEFAULT_INITIAL_CAPACITY = 16;
    protected static final int MAXIMUM_CAPACITY = 1 << 30;
    protected static final float DEFAULT_LOAD_FACTOR = 0.75f;

    protected final EntryFactory entryFactory;
    protected Entry[] table;
    protected int size;
    protected int threshold;
    protected final float loadFactor;
    protected int modCount;
    protected KeySet keySet = null;
    protected Collection values = null;
    protected EntrySet entrySet = null;
    
    protected static int hash(int h) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
    
    protected static int indexFor(final int h, final int length) {
        return h & (length-1);
    }

    // override to supply alternative EntryFactory
    protected EntryFactory newEntryFactory() {
        return new EntryFactory();
    }
    
    public NamedIRubyObjectMap() {
        loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
        entryFactory = newEntryFactory();
    }

    public NamedIRubyObjectMap(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public NamedIRubyObjectMap(final Map map) {        
        this(Math.max((int) (map.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry e = (Map.Entry)iter.next();
            putForCreate((String)e.getKey(), (IRubyObject)e.getValue());
        }
    }

    public NamedIRubyObjectMap(final NamedIRubyObjectMap vars) {
        this(Math.max((int) (vars.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);

        for (Iterator iter = vars.entrySet().iterator(); iter.hasNext(); ) {
            Entry e = (Entry)iter.next();
            putForCreateTrusted(e.key, e.value);
        }
    }

    public NamedIRubyObjectMap(int initialCapacity, final float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        this.loadFactor = loadFactor;
        this.threshold = (int)(capacity * loadFactor);
        this.table = new Entry[capacity];
        this.entryFactory = newEntryFactory();
    }
    
    public Object get(final Object name) {
        return get((String)name);
    }
    
    public synchronized IRubyObject get(final String name) {
        if (name == null)
            throw new NullPointerException("null variable name not allowed");
        final String key = name.intern();
        final int hash = hash(key.hashCode());
        for (Entry e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
            if (e.hash == hash && e.key == key) return e.value;
        }
        return null;
    }
    
    /**
     * Call this <i>only</i> if you are certain the name has already been intern-ed.
     * 
     * @param name - the intern-ed name
     * @return
     */
    public synchronized IRubyObject getNoIntern(final String name) {
        if (name == null)
            throw new NullPointerException("null variable name not allowed");
        final int hash = hash(name.hashCode());
        for (Entry e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
            if (e.hash == hash && e.key == name) return e.value;
        }
        return null;
    }
    
    public Object put(final Object name, final Object value) {
        return put((String)name,(IRubyObject)value);
    }
    
    public synchronized IRubyObject put(final String name, final IRubyObject value) {
        if (name == null)
            throw new NullPointerException("null variable name not allowed");
        final String key = name.intern();
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
             if (e.hash == hash && e.key == key) {
                IRubyObject oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        modCount++;
        addEntry(hash, key, value, i);
        return null;
    }
    
    /**
     * Call this <i>only</i> if you are certain the name has already been intern-ed.
     * 
     * @param name - the intern-ed name
     * @param value
     * @return
     */
    public synchronized IRubyObject putNoIntern(final String name, final IRubyObject value) {
        if (name == null)
            throw new NullPointerException("null variable name not allowed");
        final int hash = hash(name.hashCode());
        final int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
             if (e.hash == hash && e.key == name) {
                IRubyObject oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        modCount++;
        addEntry(hash, name, value, i);
        return null;
    }
    
    protected IRubyObject putInternal(final String key, final IRubyObject value) {
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
             if (e.hash == hash && e.key == key) {
                IRubyObject oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        modCount++;
        addEntry(hash, key, value, i);
        return null;
    }
    
    protected void putForCreate(final String name, final IRubyObject value) {
        if (name == null) return;
        final String key = name.intern();
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);

        for (Entry e = table[i]; e != null; e = e.next) {
            if (e.hash == hash && e.key == key) {
                e.value = value;
                return;
            }
        }

        createEntry(hash, key, value, i);
    }

    protected void putForCreateTrusted(final String key, final IRubyObject value) {
        final int hash = hash(key.hashCode());
        createEntry(hash, key, value, indexFor(hash, table.length));
    }

    public synchronized void putAll(final Map map) {
        if (map instanceof NamedIRubyObjectMap) {
            putAll((NamedIRubyObjectMap)map);
            return;
        }
        final int numKeysToBeAdded = map.size();
        if (numKeysToBeAdded == 0)
            return;
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry e = (Map.Entry)iter.next();
            put((String)e.getKey(), (IRubyObject)e.getValue());
        }
    }
    
    public synchronized void putAll(final NamedIRubyObjectMap map) {
        final int numKeysToBeAdded = map.size();
        if (numKeysToBeAdded == 0)
            return;
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
            Entry e = (Entry)iter.next();
            putInternal(e.key, e.value);
        }
    }
    
    public Object remove(final Object name) {
        return remove((String)name);
    }
    
    public synchronized IRubyObject remove(final String name) {
        Entry e = removeEntryForKey(name);
        return (e == null ? null : e.value);
    }
    
    public synchronized void clear () {
        modCount++;
        Entry[] tab = table;
        for (int i = tab.length; --i >= 0; ) {
            tab[i] = null;
        }
        size = 0;
    }
    
    public boolean containsKey(final Object name) {
        return containsKey((String)name);
    }
    
    public synchronized boolean containsKey(final String name) {
        return getEntry(name) != null;
    }
    
    public boolean containsValue(final Object value) {
        return containsValue((IRubyObject)value);
    }
    
    public synchronized boolean containsValue(final IRubyObject value) {
        Entry[] tab = table;
        for (int i = tab.length; --i >= 0; )
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (value == e.value)
                    return true;
        return false;
    }
    
    public synchronized Collection values() {
        Collection vs = values;
        return (vs != null ? vs : (values = new Values()));
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }

    public synchronized Set keySet() {
        Set ks = keySet;
        return ks != null ? ks : (keySet = new KeySet());
    }

    public synchronized Set entrySet() {
        Set es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    public synchronized List getEntryList() {
        final NamedIRubyObjectMap.Entry[] src = table;
        ArrayList list = new ArrayList(size);
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                list.add(entryFactory.newUnmodifiableEntry(e));
                e = e.next;
            }
        }
        return list;
    }
    
    public synchronized NamedIRubyObjectMap.Entry[] getEntryArray() {
        final Entry[] src = table;
        NamedIRubyObjectMap.Entry[] list =  new NamedIRubyObjectMap.Entry[size];
        int j = 0;
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                list[j++] = entryFactory.newUnmodifiableEntry(e);
                e = e.next;
            }
        }
        return list;
    }

    public synchronized IRubyObject[] getValueArray() {
        final Entry[] src = table;
        final IRubyObject[] values = new IRubyObject[size];
        int j = 0;
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                values[j++] = e.value;
                e = e.next;
            }
        }
        return values;
    }
    
    protected Iterator newKeyIterator()   {
        return new KeyIterator();
    }
    protected Iterator newValueIterator()   {
        return new ValueIterator();
    }
    protected Iterator newEntryIterator()   {
        return new EntryIterator();
    }

    protected Entry getEntry(final String key) {
        final int hash = hash(key.hashCode());
        for (Entry e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
            if (e.hash == hash && e.key == key) return e;
        }
        return null;
    }
    
    protected void addEntry(final int hash, final String key, final IRubyObject value, final int bucketIndex) {
        final Entry e = table[bucketIndex];
        table[bucketIndex] = entryFactory.newEntry(hash, key, value, e);
        if (size++ >= threshold)
            resize(2 * table.length);
    }
    
    protected Entry getOrAddEntry(final String key) {
        final int hash = hash(key.hashCode());
        final int index = indexFor(hash, table.length);
        Entry e;
        for (e = table[index]; e != null; e = e.next) {
            if (e.hash == hash && e.key == key) return e;
        }
        table[index] = e = entryFactory.newEntry(hash, key, null, table[index]);
        if (size++ >= threshold)
            resize(2 * table.length);
        return e;
    }
    
    protected void createEntry(final int hash, final String key, final IRubyObject value, final int bucketIndex) {
        final Entry e = table[bucketIndex];
        table[bucketIndex] = entryFactory.newEntry(hash, key, value, e);
        size++;
    }
    
    protected Entry removeEntryForKey(final String key) {
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);
        Entry prev = table[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;
            if (e.hash == hash && e.key == key) {
                modCount++;
                size--;
                if (prev == e)
                    table[i] = next;
                else
                    prev.next = next;
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }

    protected Entry removeMapping(final Object o) {
        if (!(o instanceof Entry))
            return null;

        final Entry entry = (Entry) o;
        final String key = entry.key;
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);
        Entry prev = table[i];
        Entry e = prev;

        while (e != null) {
            Entry next = e.next;
            if (e.hash == hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e)
                    table[i] = next;
                else
                    prev.next = next;
                return e;
            }
            prev = e;
            e = next;
        }

        return e;
    }

    protected void resize(final int newCapacity) {
        final Entry[] oldTable = table;
        //int oldCapacity = oldTable.length;
        if (oldTable.length == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        final Entry[] newTable = new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)(newCapacity * loadFactor);
    }
    
    protected void transfer(final Entry[] newTable) {
        final Entry[] src = table;
        final int newCapacity = newTable.length;
        Entry e;
        for (int j = src.length; --j >= 0; ) {
            if ((e = src[j]) != null) {
                src[j] = null;
                do {
                    Entry next = e.next;
                    int i = indexFor(e.hash, newCapacity);
                    e.next = newTable[i];
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }


    public static class EntryFactory {
        public Entry newEntry(
                final int hash,
                final String key,
                final IRubyObject value,
                final Entry next
                ) {
            return new Entry(hash, key, value, next);
        }
        public UnmodifiableEntry newUnmodifiableEntry(final Entry entry) {
            return new UnmodifiableEntry(entry);
        }
    }
    public static class Entry implements Map.Entry {
        protected final String key;
        protected IRubyObject value;
        protected final int hash;
        protected Entry next;
        
        public Entry(final int hash, final String key, final IRubyObject value, final Entry next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
        public final Object getKey() {
            return key;
        }
        public final String getName () {
            return key;
        }
        public final Object getValue() {
            return value;
        }
        public final IRubyObject getObject() {
            return value;
        }
        public Object setValue(final Object value) {
            IRubyObject oldValue = this.value;
            this.value = (IRubyObject)value;
            return oldValue;
        }
        public IRubyObject setValue(final IRubyObject value) {
            IRubyObject oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        public IRubyObject setObject(final IRubyObject value) {
            IRubyObject oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        public boolean equals(final Object other) {
            if (!(other instanceof Entry)) return false;
            final Entry otherEntry;
            return (otherEntry = (Entry)other).key == key && otherEntry.value == value;
        }
        public final int hashCode() {
            return hash;
        }
    }

    public static class UnmodifiableEntry extends Entry  {
        public UnmodifiableEntry(Entry entry) {
            super(entry.hash, entry.key, entry.value, (Entry)null);
        }
        public Object setValue(final Object value) {
            throw new UnsupportedOperationException();
        }
        public IRubyObject setValue(final IRubyObject value) {
            throw new UnsupportedOperationException();
        }
        public boolean equals(final Object other) {
            if (!(other instanceof Map.Entry)) return false;
            final Map.Entry otherEntry = (Map.Entry)other;
            return key.equals(otherEntry.getKey()) && value.equals(otherEntry.getValue());
        }
    }

    protected final class KeySet extends AbstractSet {
        public Iterator iterator() {
            return newKeyIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(final Object o) {
            return containsKey((String)o);
        }
        public boolean remove(final Object o) {
            return NamedIRubyObjectMap.this.removeEntryForKey((String)o) != null;
        }
        public boolean remove(final String s) {
            return NamedIRubyObjectMap.this.removeEntryForKey(s) != null;
        }
        public void clear() {
            NamedIRubyObjectMap.this.clear();
        }
    }

    protected final class EntrySet extends AbstractSet {
        public Iterator iterator() {
            return newEntryIterator();
        }
        public boolean contains(final Object o) {
            if (!(o instanceof Entry))
                return false;
            final Entry e = (Entry) o;
            final Entry candidate = getEntry(e.key);
            return candidate != null && candidate.equals(e);
        }
        public boolean remove(final Object o) {
            return removeMapping(o) != null;
        }
        public int size() {
            return size;
        }
        public void clear() {
            NamedIRubyObjectMap.this.clear();
        }
    }

    protected final class Values extends AbstractCollection {
        public Iterator iterator() {
            return newValueIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            NamedIRubyObjectMap.this.clear();
        }
    }

    protected abstract class HashIterator implements Iterator {
        Entry next;    // next entry to return
        int expectedModCount;   // For fast-fail
        int index;      // current slot
        Entry current; // current entry

        HashIterator() {
            expectedModCount = modCount;
            if (size > 0) { // advance to first entry
                Entry[] t = table;
                while (index < t.length && (next = t[index++]) == null)
                    ;
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Entry e = current = next;
            if (e == null)
                throw new NoSuchElementException();

            if ((next = e.next) == null) {
                Entry[] t = table;
                while (index < t.length && (next = t[index++]) == null)
                    ;
            }
            return e;
        }

        public final void remove() {
            if (current == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            String k = current.key;
            current = null;
            NamedIRubyObjectMap.this.removeEntryForKey(k);
            expectedModCount = modCount;
        }

    }

    protected final class ValueIterator extends HashIterator {
        public Object next() {
            return nextEntry().value;
        }
    }

    protected final class KeyIterator extends HashIterator {
        public Object next() {
            return nextEntry().key;
        }
    }

    protected final class EntryIterator extends HashIterator {
        public Object next() {
            return nextEntry();
        }
    }
}
