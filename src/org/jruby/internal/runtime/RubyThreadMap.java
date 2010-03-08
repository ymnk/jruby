package org.jruby.internal.runtime;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import org.jruby.RubyThread;
import org.jruby.runtime.ThreadContext;

public class RubyThreadMap {

    private final Map<RubyThreadWeakReference<Object>, RubyThread> map = new Hashtable();
    private final ReferenceQueue<Object> queue = new ReferenceQueue();
    private final Map<RubyThread, ThreadContext> mapToClean;

    public RubyThreadMap(Map<RubyThread, ThreadContext> mapToClean) {
        this.mapToClean = mapToClean;
    }

    public static class RubyThreadWeakReference<T> extends WeakReference<T> {
        private final RubyThread thread;
        public int hashCode;
        public RubyThreadWeakReference(T referrent, RubyThread thread) {
            super(referrent);
            hashCode = referrent.hashCode();
            this.thread = thread;
        }
        public RubyThreadWeakReference(T referrent, ReferenceQueue<? super T> queue, RubyThread thread) {
            super(referrent, queue);
            hashCode = referrent.hashCode();
            this.thread = thread;
        }
        public RubyThread getThread() {
            return thread;
        }
        @Override
        public int hashCode() {
            return hashCode;
        }
        @Override
        public boolean equals(Object other) {
            Object myKey = get();
            if (other instanceof RubyThreadWeakReference) {
                Object otherKey = ((RubyThreadWeakReference)other).get();
                if (myKey != otherKey) return false;
                return true;
            } else if (other instanceof Thread) {
                return myKey == other;
            } else {
                return false;
            }
        }
    }

    private void cleanup() {
        RubyThreadWeakReference<Object> ref;
        while ((ref = (RubyThreadWeakReference<Object>) queue.poll()) != null) {
            map.remove(ref);
            mapToClean.remove(ref.getThread());
        }
    }

    public int size() {
        cleanup();
        return map.size();
    }

    public Set<Map.Entry<RubyThreadWeakReference<Object>, RubyThread>> entrySet() {
        return map.entrySet();
    }

    public RubyThread get(Object key) {
        cleanup();
        return map.get(key);
    }

    public RubyThread put(Object key, RubyThread value) {
        cleanup();
        return map.put(new RubyThreadWeakReference(key, value), value);
    }

    public RubyThread remove(Object key) {
        cleanup();
        RubyThread t = map.remove(key);
        return t;
    }
}
