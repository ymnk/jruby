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
package org.jruby.runtime.builtin;

import org.jruby.RubyModule;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;

public interface Callable<BaseObjectType> {
/*
 * This would be nice to do soon, as another step towards breaking the RubyObject/IRubyObject
 * stranglehold, and ultimately allowing lightweights and/or other alternative class/object
 * implementations.
 * 
 * Right now, though, this breaks the JRuby compiler, since the effective type for
 * parameterized types reduces to Object; for example:  
 * 
 *   public Object callMethod(ThreadContext context, String name, Object arg);
 *   
 * Works OK in interpreted mode, though!
 * 
 * 
    public BaseObjectType callSuper(ThreadContext context, BaseObjectType[] args, Block block);
    public BaseObjectType callMethod(ThreadContext context, String name);
    public BaseObjectType callMethod(ThreadContext context, String name, Block block);
    public BaseObjectType callMethod(ThreadContext context, String name, BaseObjectType arg);
    public BaseObjectType callMethod(ThreadContext context, String name, BaseObjectType[] args);
    public BaseObjectType callMethod(ThreadContext context, String name, BaseObjectType[] args, Block block);
    public BaseObjectType callMethod(ThreadContext context, String name, BaseObjectType[] args, CallType callType);
    public BaseObjectType callMethod(ThreadContext context, String name, BaseObjectType[] args, CallType callType, Block block);
    public BaseObjectType callMethod(ThreadContext context, int methodIndex, String name);
    public BaseObjectType callMethod(ThreadContext context, int methodIndex, String name, BaseObjectType arg);
    public BaseObjectType callMethod(ThreadContext context, int methodIndex, String name, BaseObjectType[] args);
    public BaseObjectType callMethod(ThreadContext context, int methodIndex, String name, BaseObjectType[] args, CallType callType);
    public BaseObjectType callMethod(ThreadContext context, RubyModule rubyclass, String name, BaseObjectType[] args, CallType callType, Block block);
    public BaseObjectType callMethod(ThreadContext context, RubyModule rubyclass, int methodIndex, String name, BaseObjectType[] args, CallType callType, Block block);
    public BaseObjectType compilerCallMethodWithIndex(ThreadContext context, int methodIndex, String name, BaseObjectType[] args, BaseObjectType self, CallType callType, Block block);
    public BaseObjectType compilerCallMethod(ThreadContext context, String name,
            BaseObjectType[] args, BaseObjectType self, CallType callType, Block block);
*/


    public IRubyObject callSuper(ThreadContext context, IRubyObject[] args, Block block);
    public IRubyObject callMethod(ThreadContext context, String name);
    public IRubyObject callMethod(ThreadContext context, String name, Block block);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject arg);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, Block block);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, CallType callType);
    public IRubyObject callMethod(ThreadContext context, String name, IRubyObject[] args, CallType callType, Block block);
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name);
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject arg);
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject[] args);
    public IRubyObject callMethod(ThreadContext context, int methodIndex, String name, IRubyObject[] args, CallType callType);
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, String name, IRubyObject[] args, CallType callType, Block block);
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, int methodIndex, String name, IRubyObject[] args, CallType callType, Block block);
    public IRubyObject compilerCallMethodWithIndex(ThreadContext context, int methodIndex, String name, IRubyObject[] args, IRubyObject self, CallType callType, Block block);
    public IRubyObject compilerCallMethod(ThreadContext context, String name,
            IRubyObject[] args, IRubyObject self, CallType callType, Block block);

}
