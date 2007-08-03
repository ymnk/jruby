package org.jruby.bnw.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jruby.util.IdUtil;


/**
 * AttributesMap is a synchronized Map implementation, with specialized functionality
 * for handling Ruby attribute types:
 * <ul>
 * <li> instance variables
 * <li> class variables
 * <li> constants
 * <li> other attributes, such as those used when marshaling Range and Exception objects
 * </ul> 
 *  
 * The AttributesMap maps <em>interned</em> names (Strings) to Objects. It provides
 * improved performance compared to java.util.HashMap (from which much of the code was
 * derived), because:
 * <ul>
 * <li> all comparisons are made using ==
 * <li> casts are not required when accessing keys
 * <li> fast methods are provided for retrieving names and/or values as arrays or lists
 * </ul>
 * 
 * The normal <code>put/get</code> methods <code>intern</code> the supplied name string
 * (key) before using it.  Use the non-interning methods if you know the name string has
 * already been interned. (Strings returned from the lexer/parser and from
 * RubyString/RubySymbol #asSymbol have already been interned.)  Remember that keys are
 * compared using ==, so <b>passing non-interned strings to the get/putNoIntern methods will
 * most likely break the map!</b>  
 * 
 * 
 * @author Bill Dortch
 *
 */
public class AttributesMap implements Map {

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
    
 
    // TODO: this is probably overkill (and unnecessarily expensive) for String keys.
    // simplify.
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
    
    public AttributesMap() {
        loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
        entryFactory = newEntryFactory();
    }

    public AttributesMap(final int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Use only if <code>name</code> has been intern-ed.
     * @param name
     * @param value
     */
    public AttributesMap(final String name, final Object value) {
        this();
        putForCreateTrusted(name, value);
    }

    public AttributesMap(final Map map) {        
        this(Math.max((int) (map.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry e = (Map.Entry)iter.next();
            putForCreate((String)e.getKey(), e.getValue());
        }
    }

    public AttributesMap(final AttributesMap vars) {
        this(Math.max((int) (vars.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);

        for (Iterator iter = vars.entrySet().iterator(); iter.hasNext(); ) {
            Entry e = (Entry)iter.next();
            putForCreateTrusted(e.key, e.value);
        }
    }

    public AttributesMap(int initialCapacity, final float loadFactor) {
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
    
    public synchronized Object get(final String name) {
        final String key = name.intern();
        final int hash = hash(key.hashCode());
        for (Entry e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
            if (e.key == key) return e.value;
        }
        return null;
    }
    
    /**
     * Call this <i>only</i> if you are certain the name has already been intern-ed.
     * 
     * @param name - the intern-ed name
     * @return
     */
    public synchronized Object getNoIntern(final String name) {
        final int hash = hash(name.hashCode());
        for (Entry e = table[indexFor(hash, table.length)]; e != null; e = e.next) {
            if (e.key == name) return e.value;
        }
        return null;
    }
    
    public Object put(final Object name, final Object value) {
        return put((String)name, value);
    }
    
    public synchronized Object put(final String name, final Object value) {
        if (name == null)
            throw new NullPointerException("null variable name not allowed");
        final String key = name.intern();
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
             if (e.key == key) {
                Object oldValue = e.value;
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
    public synchronized Object putNoIntern(final String name, final Object value) {
        if (name == null)
            throw new NullPointerException("null variable name not allowed");
        final int hash = hash(name.hashCode());
        final int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
             if (e.key == name) {
                Object oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        modCount++;
        addEntry(hash, name, value, i);
        return null;
    }
    
    protected Object putInternal(final String key, final Object value) {
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);
        for (Entry e = table[i]; e != null; e = e.next) {
             if (e.key == key) {
                Object oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        modCount++;
        addEntry(hash, key, value, i);
        return null;
    }
    
    protected void putForCreate(final String name, final Object value) {
        if (name == null) return;
        final String key = name.intern();
        final int hash = hash(key.hashCode());
        final int i = indexFor(hash, table.length);

        for (Entry e = table[i]; e != null; e = e.next) {
            if (e.key == key) {
                e.value = value;
                return;
            }
        }

        createEntry(hash, key, value, i);
    }

    protected void putForCreateTrusted(final String key, final Object value) {
        final int hash = hash(key.hashCode());
        createEntry(hash, key, value, indexFor(hash, table.length));
    }

    public synchronized void putAll(final Map map) {
        if (map instanceof AttributesMap) {
            putAll((AttributesMap)map);
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
            put((String)e.getKey(), e.getValue());
        }
    }
    
    public synchronized void putAll(final AttributesMap map) {
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
    
    public synchronized Object remove(final String name) {
        Entry e = removeEntryForKey(name.intern());
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
        return getEntry(name.intern()) != null;
    }
    
    public synchronized boolean containsKeyNoIntern(final String name) {
        return getEntry(name) != null;
    }
    
    public synchronized boolean containsValue(final Object value) {
        Entry[] tab = table;
        for (int i = tab.length; --i >= 0; )
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (value == e.value)
                    return true;
        return false;
    }
    
    public synchronized boolean containsInstanceVariables() {
        Entry[] tab = table;
        for (int i = tab.length; --i >= 0; )
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (IdUtil.isInstanceVariable(e.key))
                    return true;
        return false;
    }
    
    public synchronized boolean containsClassVariables() {
        Entry[] tab = table;
        for (int i = tab.length; --i >= 0; )
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (IdUtil.isClassVariable(e.key))
                    return true;
        return false;
    }
    
    public synchronized boolean containsConstants() {
        Entry[] tab = table;
        for (int i = tab.length; --i >= 0; )
            for (Entry e = tab[i] ; e != null ; e = e.next)
                if (IdUtil.isClassVariable(e.key))
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

    public synchronized ArrayList getEntryList() {
        final AttributesMap.Entry[] src = table;
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
    
    public synchronized AttributesMap.Entry[] getEntryArray() {
        final Entry[] src = table;
        AttributesMap.Entry[] list =  new AttributesMap.Entry[size];
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

    public synchronized Object[] getValueArray() {
        final Entry[] src = table;
        final Object[] values = new Object[size];
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
    
    public synchronized String[] getNameArray() {
        final Entry[] src = table;
        final String[] keys = new String[size];
        int j = 0;
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                keys[j++] = e.key;
                e = e.next;
            }
        }
        return keys;
    }
    
    public synchronized ArrayList getInstanceVariableEntryList() {
        final AttributesMap.Entry[] src = table;
        ArrayList list = new ArrayList(size);
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                if (IdUtil.isInstanceVariable(e.key)) {
                    list.add(entryFactory.newUnmodifiableEntry(e));
                }
                e = e.next;
            }
        }
        return list;
    }
    
    public synchronized ArrayList getClassVariableEntryList() {
        final AttributesMap.Entry[] src = table;
        ArrayList list = new ArrayList(size);
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                if (IdUtil.isClassVariable(e.key)) {
                    list.add(entryFactory.newUnmodifiableEntry(e));
                }
                e = e.next;
            }
        }
        return list;
    }
    
    public synchronized ArrayList getConstantEntryList() {
        final AttributesMap.Entry[] src = table;
        ArrayList list = new ArrayList(size);
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                if (IdUtil.isConstant(e.key)) {
                    list.add(entryFactory.newUnmodifiableEntry(e));
                }
                e = e.next;
            }
        }
        return list;
    }
    
    public synchronized ArrayList getInstanceVariableNameList() {
        final AttributesMap.Entry[] src = table;
        ArrayList list = new ArrayList(size);
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                if (IdUtil.isInstanceVariable(e.key)) {
                    list.add(e.key);
                }
                e = e.next;
            }
        }
        return list;
    }
    
    public synchronized ArrayList getClassVariableNameList() {
        final AttributesMap.Entry[] src = table;
        ArrayList list = new ArrayList(size);
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                if (IdUtil.isClassVariable(e.key)) {
                    list.add(e.key);
                }
                e = e.next;
            }
        }
        return list;
    }
    
    public synchronized ArrayList getConstantNameList() {
        final AttributesMap.Entry[] src = table;
        ArrayList list = new ArrayList(size);
        for (int i = src.length; --i >= 0; ) {
            Entry e = src[i];
            while (e != null) {
                if (IdUtil.isConstant(e.key)) {
                    list.add(e.key);
                }
                e = e.next;
            }
        }
        return list;
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
            if (e.key == key) return e;
        }
        return null;
    }
    
    protected void addEntry(final int hash, final String key, final Object value, final int bucketIndex) {
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
            if (e.key == key) return e;
        }
        table[index] = e = entryFactory.newEntry(hash, key, null, table[index]);
        if (size++ >= threshold)
            resize(2 * table.length);
        return e;
    }
    
    protected void createEntry(final int hash, final String key, final Object value, final int bucketIndex) {
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
            if (e.key == key) {
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
                final Object value,
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
        protected Object value;
        protected final int hash;
        protected Entry next;
        
        public Entry(final int hash, final String key, final Object value, final Entry next) {
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
        public Object setValue(final Object value) {
            Object oldValue = this.value;
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
            return AttributesMap.this.removeEntryForKey((String)o) != null;
        }
        public boolean remove(final String s) {
            return AttributesMap.this.removeEntryForKey(s) != null;
        }
        public void clear() {
            AttributesMap.this.clear();
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
            AttributesMap.this.clear();
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
            AttributesMap.this.clear();
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
            AttributesMap.this.removeEntryForKey(k);
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

