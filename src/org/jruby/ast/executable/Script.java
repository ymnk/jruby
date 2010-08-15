package org.jruby.ast.executable;

import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public interface Script {
    public IRubyObject __file__(ThreadContext context, IRubyObject self, RubyModule cls, String name, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, RubyModule cls, String name, IRubyObject arg, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, RubyModule cls, String name, IRubyObject arg1, IRubyObject arg2, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, RubyModule cls, String name, IRubyObject arg, IRubyObject arg2, IRubyObject arg3, Block block);
    public IRubyObject __file__(ThreadContext context, IRubyObject self, RubyModule cls, String name, IRubyObject[] args, Block block);
    
    public IRubyObject run(ThreadContext context, IRubyObject self, RubyModule cls, String name, IRubyObject[] args, Block block);
    public IRubyObject load(ThreadContext context, IRubyObject self, RubyModule cls, String name, IRubyObject[] args, Block block);
    public void setFilename(String filename);
}
