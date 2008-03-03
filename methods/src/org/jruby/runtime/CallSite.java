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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

package org.jruby.runtime;

import java.util.concurrent.locks.Lock;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.JumpException.BreakJump;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public abstract class CallSite {
    public final int methodID;
    public final String methodName;
    protected final CallType callType;
    private static int callCount = 0;
    
    
    public CallSite(int methodID, String methodName, CallType callType) {
        this.methodID = methodID;
        this.methodName = methodName;
        this.callType = callType;
    }
    
    // no block
    public abstract IRubyObject call(ThreadContext context, IRubyObject self);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args);
    // with block
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);

    private static class ImmutableMethodAndType {
        // note that final is required for correct synchronization
        final DynamicMethod method;
        final RubyClass type;

        ImmutableMethodAndType(DynamicMethod method, RubyClass type) {
            this.method = method;
            this.type = type;
        }
    }
    
    private static class WrappedMethod {
        DynamicMethod method;
        boolean valid;
        
        WrappedMethod(DynamicMethod method, boolean valid) {
            this.method = method;
            this.valid = valid;
        }
    }
    
    public static class InlineCachingCallSite extends CallSite implements CacheMap.CacheSite {
        // note that volatile is required for correct synchronization
        volatile ImmutableMethodAndType cachedMethodAndType;
        
        public InlineCachingCallSite(String methodName, CallType callType) {
            super(MethodIndex.getIndex(methodName), methodName, callType);
        }
        
        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, args, block);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, args, context.getFrameSelf(), callType, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, args);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, args, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, IRubyObject.NULL_ARRAY, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, block);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, IRubyObject.NULL_ARRAY, context.getFrameSelf(), callType, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, arg);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, new IRubyObject[] {arg}, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, arg, block);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, new IRubyObject[] {arg}, context.getFrameSelf(), callType, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, arg1, arg2);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, new IRubyObject[] {arg1,arg2}, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, arg1, arg2, block);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, new IRubyObject[] {arg1,arg2}, context.getFrameSelf(), callType, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, arg1, arg2, arg3);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, new IRubyObject[] {arg1,arg2,arg3}, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            WrappedMethod wrapped = searchAndCache(selfType, context);

            if (wrapped.valid) {
                return wrapped.method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
            }

            return RuntimeHelpers.callMethodMissing(context, self, wrapped.method, methodName, new IRubyObject[] {arg1,arg2,arg3}, context.getFrameSelf(), callType, block);
        }
        
        private WrappedMethod searchAndCache(RubyClass selfType, ThreadContext context) {
            Ruby runtime = context.getRuntime();
            Lock methodWriteLock = runtime.getGlobalMethodWriteLock();
            methodWriteLock.lock();
            try {
                DynamicMethod method = selfType.searchMethod(methodName);
                if (method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(context.getFrameSelf(), callType))) {
                    cachedMethodAndType = null; // write-volatile
                    return new WrappedMethod(method, false);
                }
                cachedMethodAndType = new ImmutableMethodAndType(method, selfType); // write-volatile
                runtime.getCacheMap().add(method, this);
                return new WrappedMethod(method, true);
            } finally {
                methodWriteLock.unlock();
            }
        }
        
        public void removeCachedMethod() {
            cachedMethodAndType = null;
        }
        
        private void pollThreadEvents(ThreadContext context) {
            if ((callCount++ & 0xFF) == 0) context.pollThreadEvents();
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            pollThreadEvents(context);

            RubyClass selfType = self.getMetaClass();
            ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile
            
            if (cached != null && cached.type == selfType) {
                return cached.method.call(context, self, selfType, methodName, args);
            }

            return cacheAndCall(selfType, args, context, self);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            pollThreadEvents(context);

            try {
                RubyClass selfType = self.getMetaClass();
                ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

                if (cached != null && cached.type == selfType) {
                    return cached.method.call(context, self, selfType, methodName, args, block);
                }

                return cacheAndCall(selfType, block, args, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(bj, block);
            } catch (JumpException.RetryJump rj) {
                throw context.getRuntime().newLocalJumpError("retry", context.getRuntime().getNil(), "retry outside of rescue not yet supported");
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self) {
            pollThreadEvents(context);

            RubyClass selfType = self.getMetaClass();
            ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

            if (cached != null && cached.type == selfType) {
                return cached.method.call(context, self, selfType, methodName);
            }

            return cacheAndCall(selfType, context, self);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, Block block) {
            pollThreadEvents(context);

            try {
                RubyClass selfType = self.getMetaClass();
                ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

                if (cached != null && cached.type == selfType) {
                    return cached.method.call(context, self, selfType, methodName, block);
                }

                return cacheAndCall(selfType, block, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(bj, block);
            } catch (JumpException.RetryJump rj) {
                throw context.getRuntime().newLocalJumpError("retry", context.getRuntime().getNil(), "retry outside of rescue not yet supported");
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1) {
            pollThreadEvents(context);

            RubyClass selfType = self.getMetaClass();
            ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

            if (cached != null && cached.type == selfType) {
                return cached.method.call(context, self, selfType, methodName, arg1);
            }

            return cacheAndCall(selfType, context, self, arg1);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block) {
            pollThreadEvents(context);

            try {
                RubyClass selfType = self.getMetaClass();
                ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

                if (cached != null && cached.type == selfType) {
                    return cached.method.call(context, self, selfType, methodName, arg1, block);
                }

                return cacheAndCall(selfType, block, context, self, arg1);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(bj, block);
            } catch (JumpException.RetryJump rj) {
                throw context.getRuntime().newLocalJumpError("retry", context.getRuntime().getNil(), "retry outside of rescue not yet supported");
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            pollThreadEvents(context);

            RubyClass selfType = self.getMetaClass();
            ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

            if (cached != null && cached.type == selfType) {
                return cached.method.call(context, self, selfType, methodName, arg1, arg2);
            }

            return cacheAndCall(selfType, context, self, arg1, arg2);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
            pollThreadEvents(context);

            try {
                RubyClass selfType = self.getMetaClass();
                ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

                if (cached != null && cached.type == selfType) {
                    return cached.method.call(context, self, selfType, methodName, arg1, arg2, block);
                }

                return cacheAndCall(selfType, block, context, self, arg1, arg2);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(bj, block);
            } catch (JumpException.RetryJump rj) {
                throw context.getRuntime().newLocalJumpError("retry", context.getRuntime().getNil(), "retry outside of rescue not yet supported");
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            pollThreadEvents(context);

            RubyClass selfType = self.getMetaClass();
            ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

            if (cached != null && cached.type == selfType) {
                return cached.method.call(context, self, selfType, methodName, arg1, arg2, arg3);
            }

            return cacheAndCall(selfType, context, self, arg1, arg2, arg3);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            pollThreadEvents(context);

            try {
                RubyClass selfType = self.getMetaClass();
                ImmutableMethodAndType cached = cachedMethodAndType; // read-volatile

                if (cached != null && cached.type == selfType) {
                    return cached.method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
                }

                return cacheAndCall(selfType, block, context, self, arg1, arg2, arg3);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(bj, block);
            } catch (JumpException.RetryJump rj) {
                throw context.getRuntime().newLocalJumpError("retry", context.getRuntime().getNil(), "retry outside of rescue not yet supported");
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }

        private IRubyObject handleBreakJump(BreakJump bj, Block block) throws BreakJump {
            // JRUBY-530, Kernel#loop case:
            if (bj.isBreakInKernelLoop()) {
                // consume and rethrow or just keep rethrowing?
                if (block.getBody() == bj.getTarget()) {
                    bj.setBreakInKernelLoop(false);
                }
                throw bj;
            }

            return (IRubyObject) bj.getValue();
        }
    }
    
    public static class PlusCallSite extends InlineCachingCallSite {
        public PlusCallSite() {
            super("+", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_plus(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class MinusCallSite extends InlineCachingCallSite {
        public MinusCallSite() {
            super("-", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_minus(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class MulCallSite extends InlineCachingCallSite {
        public MulCallSite() {
            super("*", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_mul(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class DivCallSite extends InlineCachingCallSite {
        public DivCallSite() {
            super("/", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_div(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class LtCallSite extends InlineCachingCallSite {
        public LtCallSite() {
            super("<", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_lt(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class LeCallSite extends InlineCachingCallSite {
        public LeCallSite() {
            super("<=", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_le(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class GtCallSite extends InlineCachingCallSite {
        public GtCallSite() {
            super(">", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_gt(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class GeCallSite extends InlineCachingCallSite {
        public GeCallSite() {
            super(">=", CallType.NORMAL);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_ge(arg);
            }
            
            return super.call(context, self, arg);
        }
    }
}
