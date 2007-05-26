package org.jruby.debug;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyThread;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

public class RubyDebugBaseLibrary implements Library {
    private static final String VERSION = "0.9.3";
    
    public static final int CTX_FL_SUSPEND = (1<<1);
    public static final int CTX_FL_TRACING = (1<<2);
    public static final int CTX_FL_SKIPPED = (1<<3);
    public static final int CTX_FL_IGNORE = (1<<4);
    public static final int CTX_FL_DEAD = (1<<5);
    public static final int CTX_FL_WAS_RUNNING = (1<<6);
    public static final int CTX_FL_ENABLE_BKPT = (1<<7);
    public static final int CTX_FL_STEPPED = (1<<8);
    public static final int CTX_FL_FORCE_MOVE = (1<<9);

    public static final int CTX_STOP_NONE = 0;
    public static final int CTX_STOP_STEP = 1;
    public static final int CTX_STOP_BREAKPOINT = 2;
    public static final int CTX_STOP_CATCHPOINT = 3;
    
    public static final int BP_POS_TYPE = 0;
    public static final int BP_METHOD_TYPE = 1;
    
    public static final int HIT_COND_NONE = 0;
    public static final int HIT_COND_GE = 1;
    public static final int HIT_COND_EQ = 2;
    public static final int HIT_COND_MOD = 3;

    public void load(Ruby runtime) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public static class Debugger {
        public static RubyModule createDebuggerModule(Ruby runtime) {
            RubyModule debugger = runtime.defineModule("Debugger");
            
            CallbackFactory callbackFactory = runtime.callbackFactory(Debugger.class);
            
            debugger.defineConstant("VERSION", RubyString.newString(runtime, VERSION));
            debugger.defineModuleFunction("start", callbackFactory.getSingletonMethod("start"));
            debugger.defineModuleFunction("stop", callbackFactory.getSingletonMethod("stop"));
            debugger.defineModuleFunction("started?", callbackFactory.getSingletonMethod("started_p"));
            debugger.defineModuleFunction("breakpoints", callbackFactory.getSingletonMethod("breakpoints"));
            debugger.defineModuleFunction("add_breakpoint", callbackFactory.getOptSingletonMethod("add_breakpoint"));
            debugger.defineModuleFunction("remove_breakpoint", callbackFactory.getSingletonMethod("remove_breakpoint", IRubyObject.class));
            debugger.defineModuleFunction("catchpoint", callbackFactory.getSingletonMethod("catchpoint"));
            debugger.defineModuleFunction("catchpoint=", callbackFactory.getSingletonMethod("catchpoint_set", IRubyObject.class));
            debugger.defineModuleFunction("last_context", callbackFactory.getSingletonMethod("last_context"));
            debugger.defineModuleFunction("contexts", callbackFactory.getSingletonMethod("contexts"));
            debugger.defineModuleFunction("thread_context", callbackFactory.getSingletonMethod("thread_context", IRubyObject.class));
            debugger.defineModuleFunction("suspend", callbackFactory.getSingletonMethod("suspend"));
            debugger.defineModuleFunction("resume", callbackFactory.getSingletonMethod("resume"));
            debugger.defineModuleFunction("tracing", callbackFactory.getSingletonMethod("tracing"));
            debugger.defineModuleFunction("tracing=", callbackFactory.getSingletonMethod("tracing_set", IRubyObject.class));
            debugger.defineModuleFunction("debug_load", callbackFactory.getOptSingletonMethod("debug_load"));
            debugger.defineModuleFunction("skip", callbackFactory.getSingletonMethod("skip"));
            debugger.defineModuleFunction("debug_at_exit", callbackFactory.getSingletonMethod("debug_at_exit"));
            debugger.defineModuleFunction("post_mortem?", callbackFactory.getSingletonMethod("post_mortem_p"));
            debugger.defineModuleFunction("post_mortem=", callbackFactory.getSingletonMethod("post_mortem_set", IRubyObject.class));
            debugger.defineModuleFunction("keep_frame_binding?", callbackFactory.getSingletonMethod("keep_frame_binding_p"));
            debugger.defineModuleFunction("keep_frame_binding=", callbackFactory.getSingletonMethod("keep_frame_binding_p", IRubyObject.class));
            debugger.defineModuleFunction("debug", callbackFactory.getSingletonMethod("debug"));
            debugger.defineModuleFunction("debug=", callbackFactory.getSingletonMethod("debug_set", IRubyObject.class));
            
            RubyClass threadsTable = debugger.defineClassUnder("ThreadsTable", runtime.getObject(), runtime.getObject().getAllocator());
            
            CallbackFactory dtCallbackFactory = runtime.callbackFactory(DebugThread.class);
            
            RubyClass debugThread = debugger.defineClassUnder("DebugThread", runtime.getClass("Thread"), runtime.getClass("Thread").getAllocator());
            
            debugThread.getSingletonClass().defineMethod("inherited", dtCallbackFactory.getSingletonMethod("inherited", IRubyObject.class));
            
            CallbackFactory bCallbackFactory = runtime.callbackFactory(Breakpoint.class);
            
            RubyClass breakpoint = debugger.defineClassUnder("Breakpoint", runtime.getObject(), runtime.getObject().getAllocator());
            
            breakpoint.defineMethod("id", bCallbackFactory.getMethod("id"));
            breakpoint.defineMethod("source", bCallbackFactory.getMethod("source"));
            breakpoint.defineMethod("source=", bCallbackFactory.getMethod("source_set", IRubyObject.class));
            breakpoint.defineMethod("pos", bCallbackFactory.getMethod("pos"));
            breakpoint.defineMethod("pos=", bCallbackFactory.getMethod("pos_set", IRubyObject.class));
            breakpoint.defineMethod("expr", bCallbackFactory.getMethod("expr"));
            breakpoint.defineMethod("expr=", bCallbackFactory.getMethod("expr_set", IRubyObject.class));
            breakpoint.defineMethod("hit_count", bCallbackFactory.getMethod("hit_count"));
            breakpoint.defineMethod("hit_value", bCallbackFactory.getMethod("hit_value"));
            breakpoint.defineMethod("hit_value=", bCallbackFactory.getMethod("hit_value_set", IRubyObject.class));
            breakpoint.defineMethod("hit_condition", bCallbackFactory.getMethod("hit_condition"));
            breakpoint.defineMethod("hit_condition=", bCallbackFactory.getMethod("hit_condition_set", IRubyObject.class));
            
            CallbackFactory cCallbackFactory = runtime.callbackFactory(Context.class);
            
            RubyClass context = debugger.defineClassUnder("Context", runtime.getObject(), runtime.getObject().getAllocator());
            
            context.defineMethod("stop_next=", cCallbackFactory.getMethod("stop_next_set", IRubyObject.class));
            context.defineMethod("step", cCallbackFactory.getMethod("step"));
            context.defineMethod("step_over", cCallbackFactory.getOptMethod("step_over"));
            context.defineMethod("stop_frame=", cCallbackFactory.getMethod("stop_frame_set", IRubyObject.class));
            context.defineMethod("thread", cCallbackFactory.getMethod("thread"));
            context.defineMethod("thnum", cCallbackFactory.getMethod("thnum"));
            context.defineMethod("stop_reason", cCallbackFactory.getMethod("stop_reason"));
            context.defineMethod("suspend", cCallbackFactory.getMethod("suspend"));
            context.defineMethod("suspended?", cCallbackFactory.getMethod("suspended_p"));
            context.defineMethod("resume", cCallbackFactory.getMethod("resume"));
            context.defineMethod("tracing", cCallbackFactory.getMethod("tracing"));
            context.defineMethod("tracing=", cCallbackFactory.getMethod("tracing_set", IRubyObject.class));
            context.defineMethod("ignored?", cCallbackFactory.getMethod("ignored_p"));
            context.defineMethod("frame_binding", cCallbackFactory.getMethod("frame_binding", IRubyObject.class));
            context.defineMethod("frame_id", cCallbackFactory.getMethod("frame_id", IRubyObject.class));
            context.defineMethod("frame_method", cCallbackFactory.getMethod("frame_method", IRubyObject.class));
            context.defineMethod("frame_line", cCallbackFactory.getMethod("frame_line", IRubyObject.class));
            context.defineMethod("frame_file", cCallbackFactory.getMethod("frame_file", IRubyObject.class));
            context.defineMethod("frame_locals", cCallbackFactory.getMethod("frame_locals", IRubyObject.class));
            context.defineMethod("frame_self", cCallbackFactory.getMethod("frame_self", IRubyObject.class));
            context.defineMethod("frame_class", cCallbackFactory.getMethod("frame_class", IRubyObject.class));
            context.defineMethod("stack_size", cCallbackFactory.getMethod("stack_size"));
            context.defineMethod("dead?", cCallbackFactory.getMethod("dead_p"));
            context.defineMethod("breakpoint", cCallbackFactory.getMethod("breakpoint"));
            context.defineMethod("set_breakpoint", cCallbackFactory.getOptMethod("set_breakpoint"));
            
            // FIXME: some constants and global vars missing here...
            
            return debugger;
        }
        
        public static IRubyObject start(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject stop(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject started_p(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject breakpoints(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject add_breakpoint(IRubyObject recv, IRubyObject[] args) {
            return null;
        }
        
        public static IRubyObject remove_breapoint(IRubyObject recv, IRubyObject breakpoint) {
            return null;
        }
        
        public static IRubyObject catchpoint(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject catchpoint_set(IRubyObject recv, IRubyObject catchpoint) {
            return null;
        }
        
        public static IRubyObject last_context(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject contexts(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject thread_context(IRubyObject recv, IRubyObject context) {
            return null;
        }
        
        public static IRubyObject suspend(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject resume(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject tracing(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject tracing_set(IRubyObject recv, IRubyObject tracing) {
            return null;
        }
        
        public static IRubyObject debug_load(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject skip(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject debug_at_exit(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject post_mortem_p(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject post_mortem_set(IRubyObject recv, IRubyObject post_mortem) {
            return null;
        }
        
        public static IRubyObject keep_frame_binding_p(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject keep_frame_binding_set(IRubyObject recv, IRubyObject keep_frame_binding) {
            return null;
        }
        
        public static IRubyObject debug(IRubyObject recv) {
            return null;
        }
        
        public static IRubyObject debug_set(IRubyObject recv, IRubyObject debug) {
            return null;
        }
    }
    
    public static class DebugThread extends RubyThread {
        protected DebugThread(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }
        
        public static IRubyObject inherited(IRubyObject recv, IRubyObject clazz) {
            return null;
        }
    }
    
    public static class Breakpoint extends RubyObject {
        protected Breakpoint(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }
        
        public RubyFixnum id() {
            return super.id();
        }
        
        public IRubyObject source() {
            return getInstanceVariable("source");
        }
        
        public IRubyObject source_set(IRubyObject source) {
            return setInstanceVariable("source", source);
        }
        
        public IRubyObject pos() {
            return getInstanceVariable("pos");
        }
        
        public IRubyObject pos_set(IRubyObject pos) {
            return setInstanceVariable("pos", pos);
        }
        
        public IRubyObject expr() {
            return getInstanceVariable("expr");
        }
        
        public IRubyObject expr_set(IRubyObject expr) {
            return setInstanceVariable("expr", expr);
        }
        
        public IRubyObject hit_count() {
            return getInstanceVariable("hit_count");
        }
        
        public IRubyObject hit_value() {
            return getInstanceVariable("hit_value");
        }
        
        public IRubyObject hit_value_set(IRubyObject hit_value) {
            return setInstanceVariable("hit_value", hit_value);
        }
        
        public IRubyObject hit_condition() {
            return getInstanceVariable("hit_condition");
        }
        
        public IRubyObject hit_condition_set(IRubyObject hit_condition) {
            return setInstanceVariable("hit_condition", hit_condition);
        }
    }
    
    public static class Context extends RubyObject {        
        protected Context(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }
        
        public IRubyObject stop_next_set(IRubyObject stop_next) {
            return null;
        }
        
        public IRubyObject step() {
            return null;
        }
        
        public IRubyObject step_over() {
            return null;
        }
        
        public IRubyObject stop_frame_set(IRubyObject frame) {
            return null;
        }
        
        public IRubyObject thread() {
            return null;
        }
        
        public IRubyObject thnum() {
            return null;
        }
        
        public IRubyObject stop_reason() {
            return null;
        }
        
        public IRubyObject suspend() {
            return null;
        }
        
        public IRubyObject suspended_p() {
            return null;
        }
        
        public IRubyObject resume() {
            return null;
        }
        
        public IRubyObject tracing() {
            return null;
        }
        
        public IRubyObject tracing_set(IRubyObject tracing) {
            return null;
        }
        
        public IRubyObject ignored_p() {
            return null;
        }
        
        public IRubyObject frame_binding(IRubyObject binding) {
            return null;
        }
        
        public IRubyObject frame_id(IRubyObject id) {
            return null;
        }
        
        public IRubyObject frame_method(IRubyObject method) {
            return null;
        }
        
        public IRubyObject frame_line(IRubyObject line) {
            return null;
        }
        
        public IRubyObject frame_file(IRubyObject file) {
            return null;
        }
        
        public IRubyObject frame_locals(IRubyObject locals) {
            return null;
        }
        
        public IRubyObject frame_self(IRubyObject self) {
            return null;
        }
        
        public IRubyObject frame_class(IRubyObject klass) {
            return null;
        }
        
        public IRubyObject stack_size() {
            return null;
        }
        
        public IRubyObject dead_p() {
            return null;
        }
        
        public IRubyObject breakpoint() {
            return null;
        }
        
        public IRubyObject set_breakpoint(IRubyObject[] args) {
            return null;
        }
    }
}