/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2010 Charles O Nutter <headius@headius.com>
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
package org.jruby.ext.rubinius;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.TypeConverter;

public class RubiniusLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        ThreadContext context = runtime.getCurrentContext();

        // Go!
        runtime.setRubinius(true);

        RubyModule rubinius = runtime.getOrCreateModule("Rubinius");

        // some impls need the JRuby utils available
        runtime.getLoadService().lockAndRequire("jruby");

        RubyTuple.createTupleClass(runtime);

        String rbxHome = (String)((RubyHash)runtime.getObject().getConstant("ENV")).get("RBX_KERNEL");
        if (rbxHome == null) {
            throw runtime.newRuntimeError("set RBX_KERNEL to the location of the Rubinius kernel");
        }

        final IRubyObject undefined = new RubyObject(runtime, runtime.getObject());
        runtime.getKernel().addMethod("undefined", new JavaMethod.JavaMethodZero(runtime.getKernel(), PRIVATE) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return undefined;
            }
        });

        RubyModule rbxRuby = runtime.getOrCreateModule("Ruby");
        rbxRuby.defineAnnotatedMethods(RubiniusRuby.class);

        // Rubinius kernel expects allocate to actually get called, so we define a "new" that does so
        runtime.getClassClass().addMethod("new", new SpecificArityNew(runtime.getClassClass(), PUBLIC));

        // Type module
        RubyModule type = runtime.defineModule("Type");
        type.defineAnnotatedMethods(RubiniusType.class);
        runtime.getLoadService().lockAndRequire(rbxHome + "/common/type.rb");

        // Remove Hash and reload from rbx
        runtime.getObject().deleteConstant("Hash");
        runtime.getLoadService().lockAndRequire(rbxHome + "/common/hash.rb");
        RubyClass hash = (RubyClass)runtime.getClass("Hash");
        runtime.setHash(hash);

        // LookupTable is just Hash for now
        rubinius.setConstant("LookupTable", hash);

        // Remove ENV, define EnvironmentAccess, and reload from rbx
        IRubyObject oldEnv = runtime.getObject().deleteConstant("ENV");
        RubyModule envAccess = rubinius.defineModuleUnder("EnvironmentAccess");
        envAccess.defineAnnotatedMethods(RubiniusEnvironmentAccess.class);
        runtime.getLoadService().lockAndRequire(rbxHome + "/common/env.rb");

        // Rubinius sets ENV in loader.rb, so we do it here
        IRubyObject env = rubinius.getConstant("EnvironmentVariables").callMethod(context, "new");
        env.getInternalVariables().setInternalVariable("env", oldEnv);
        runtime.getObject().setConstant("ENV", env);

        // More Rubinius core additions
        runtime.getThread().module_eval(context, runtime.newString(DETECT_RECURSION), Block.NULL_BLOCK);
        runtime.getKernel().defineAnnotatedMethods(RubiniusKernel.class);
    }

    public static class RubiniusRuby {
        @JRubyMethod(meta = true)
        public static void check_frozen(ThreadContext context, IRubyObject self) {
            IRubyObject obj = context.getFrameSelf();
            if (obj.isFrozen()) {
                throw context.runtime.newFrozenError(obj.getMetaClass().getName());
            }
        }
    }

    public static class RubiniusKernel {
        @JRubyMethod(module = true)
        public static IRubyObject StringValue(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return arg.convertToString();
        }
    }

    public static class RubiniusType {
        @JRubyMethod(meta = true, name = "object_kind_of?")
        public static IRubyObject obj_kind_of_p(ThreadContext context, IRubyObject self, IRubyObject obj, IRubyObject cls) {
            return context.runtime.newBoolean(((RubyModule)cls).isInstance(obj));
        }
    }

    public static class RubiniusEnvironmentAccess {
        @JRubyMethod(module = true)
        public static IRubyObject getenv(ThreadContext context, IRubyObject self, IRubyObject name) {
            return getEnv(self).op_aref(context, name);
        }

        @JRubyMethod(module = true)
        public static IRubyObject setenv(ThreadContext context, IRubyObject self, IRubyObject name, IRubyObject value) {
            return getEnv(self).op_aset(context, name, value);
        }

        @JRubyMethod(module = true)
        public static IRubyObject environ_as_hash(ThreadContext context, IRubyObject self) {
            return getEnv(self).dup();
        }

        private static RubyHash getEnv(IRubyObject self) {
            return (RubyHash)self.getInternalVariables().getInternalVariable("env");
        }
    }

    // Largely copieed from RubyClass, just does an allocate dyncall as well
    public static class SpecificArityNew extends JavaMethod {
        public SpecificArityNew(RubyModule implClass, Visibility visibility) {
            super(implClass, visibility);
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.getBaseCallSites()[RubyClass.CS_IDX_ALLOCATE].call(context, self, self);
                cls.getBaseCallSites()[RubyClass.CS_IDX_INITIALIZE].call(context, self, obj, args, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.getBaseCallSites()[RubyClass.CS_IDX_ALLOCATE].call(context, self, self);
                cls.getBaseCallSites()[RubyClass.CS_IDX_INITIALIZE].call(context, self, obj, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.getBaseCallSites()[RubyClass.CS_IDX_ALLOCATE].call(context, self, self);
                cls.getBaseCallSites()[RubyClass.CS_IDX_INITIALIZE].call(context, self, obj, arg0, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.getBaseCallSites()[RubyClass.CS_IDX_ALLOCATE].call(context, self, self);
                cls.getBaseCallSites()[RubyClass.CS_IDX_INITIALIZE].call(context, self, obj, arg0, arg1, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
            preBacktraceOnly(context, name);
            try {
                RubyClass cls = (RubyClass)self;
                IRubyObject obj = cls.getBaseCallSites()[RubyClass.CS_IDX_ALLOCATE].call(context, self, self);
                cls.getBaseCallSites()[RubyClass.CS_IDX_INITIALIZE].call(context, self, obj, arg0, arg1, arg2, block);
                return obj;
            } finally {
                postBacktraceOnly(context);
            }
        }
    }

    // Yes, this is ugly. Hopefully we can pull these out of common/thread.rb.
    private static final String DETECT_RECURSION =
"  # detect_recursion will return if there's a recursion\n" +
"  # on obj (or the pair obj+paired_obj).\n" +
"  # If there is one, it returns true.\n" +
"  # Otherwise, it will yield once and return false.\n" +
"\n" +
"  def self.detect_recursion(obj, paired_obj=undefined)\n" +
"    id = obj.object_id\n" +
"    pair_id = paired_obj.object_id\n" +
"    objects = current.recursive_objects\n" +
"\n" +
"    case objects[id]\n" +
"\n" +
"      # Default case, we haven't seen +obj+ yet, so we add it and run the block.\n" +
"    when nil\n" +
"      objects[id] = pair_id\n" +
"      begin\n" +
"        yield\n" +
"      ensure\n" +
"        objects.delete id\n" +
"      end\n" +
"\n" +
"      # We've seen +obj+ before and it's got multiple paired objects associated\n" +
"      # with it, so check the pair and yield if there is no recursion.\n" +
"    when Rubinius::LookupTable\n" +
"      return true if objects[id][pair_id]\n" +
"      objects[id][pair_id] = true\n" +
"\n" +
"      begin\n" +
"        yield\n" +
"      ensure\n" +
"        objects[id].delete pair_id\n" +
"      end\n" +
"\n" +
"      # We've seen +obj+ with one paired object, so check the stored one for\n" +
"      # recursion.\n" +
"      #\n" +
"      # This promotes the value to a LookupTable since there is another new paired\n" +
"      # object.\n" +
"    else\n" +
"      previous = objects[id]\n" +
"      return true if previous == pair_id\n" +
"\n" +
"      objects[id] = Rubinius::LookupTable.new(previous => true, pair_id => true)\n" +
"\n" +
"      begin\n" +
"        yield\n" +
"      ensure\n" +
"        objects[id] = previous\n" +
"      end\n" +
"    end\n" +
"\n" +
"    false\n" +
"  end\n" +
"\n" +
"  def recursive_objects\n" +
"    @recursive_objects ||= {}\n" +
"  end\n" +
"\n" +
"  # Similar to detect_recursion, but will short circuit all inner recursion\n" +
"  # levels (using a throw)\n" +
"\n" +
"  class InnerRecursionDetected < Exception; end\n" +
"\n" +
"  def self.detect_outermost_recursion(obj, paired_obj=undefined, &block)\n" +
"    rec = current.recursive_objects\n" +
"\n" +
"    if rec[:__detect_outermost_recursion__]\n" +
"      if detect_recursion(obj, paired_obj, &block)\n" +
"        raise InnerRecursionDetected.new\n" +
"      end\n" +
"      false\n" +
"    else\n" +
"      begin\n" +
"        rec[:__detect_outermost_recursion__] = true\n" +
"\n" +
"        begin\n" +
"          detect_recursion(obj, paired_obj, &block)\n" +
"        rescue InnerRecursionDetected\n" +
"          return true\n" +
"        end\n" +
"\n" +
"        return nil\n" +
"      ensure\n" +
"        rec.delete :__detect_outermost_recursion__\n" +
"      end\n" +
"    end\n" +
"  end";
}
