package org.jruby.bnw;

import org.jruby.runtime.builtin.IRubyObject;

public final class MetaUtil {
    
    private MetaUtil() {}

    public static boolean isNil(Object obj) {
        return obj == null || (obj instanceof IRubyObject && ((IRubyObject)obj).isNil());
    }
    
    public static boolean isTrue(Object obj) {
        return obj instanceof IRubyObject ? ((IRubyObject)obj).isTrue() :
            !(obj == null || obj == Boolean.FALSE);
    }

    public static boolean isFalse(Object obj) {
        return obj == null || obj == Boolean.FALSE || 
            (obj instanceof IRubyObject && !((IRubyObject)obj).isTrue());
    }

}
