/**
 * 
 */
package org.jruby.bnw;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;

/**
 * 
 *
 */
public interface MetaClass extends ModuleIncluder, CommonMetaClass {

    String getName();
    
    int getClassIndex();

    MetaClass getSuperClass();
    
    boolean isSingleton();
    
    boolean isImmediate();
    
    boolean respondsTo(Object self, String name);

    Object callMethod(ThreadContext context, Object self, String name);

    Object callMethod(ThreadContext context, Object self, String name, Block block);

    Object callMethod(ThreadContext context, Object self, String name, Object arg);

    Object callMethod(ThreadContext context, Object self, String name, Object[] args);

    Object callMethod(ThreadContext context, Object self, String name, Object[] args, Block block);

    Object callMethod(ThreadContext context, Object self, String name, Object[] args, CallType callType);

    Object callMethod(ThreadContext context, Object self, String name, Object[] args, CallType callType, Block block);

    Object callMethod(ThreadContext context, Object self, int methodIndex, String name);

    Object callMethod(ThreadContext context, Object self, int methodIndex, String name, Object arg);

    Object callMethod(ThreadContext context, Object self, int methodIndex, String name, Object[] args);


    // TODO: need Charlie's input on these
//    IRubyObject compilerCallMethodWithIndex(ThreadContext context, int methodIndex, String name, IRubyObject[] args, IRubyObject self, CallType callType, Block block);
//    IRubyObject compilerCallMethod(ThreadContext context, String name,
//            IRubyObject[] args, IRubyObject self, CallType callType, Block block);
}
