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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.jruby;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import org.jruby.parser.ReOptions;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.Sprintf;

import org.rej.Pattern;

/**
 *
 * @author  amoore
 */
public class RubyRegexp extends RubyObject implements ReOptions {
    private KCode code;

    KCode getCode() {
        return code;
    }

    public RubyRegexp(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    private RubyRegexp(Ruby runtime) {
        super(runtime, runtime.getClass("Regexp"));
    }
    
    private static ObjectAllocator REGEXP_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyRegexp(runtime, klass);
        }
    };

    public static RubyClass createRegexpClass(Ruby runtime) {
        RubyClass regexpClass = runtime.defineClass("Regexp", runtime.getObject(), REGEXP_ALLOCATOR);
        regexpClass.index = ClassIndex.REGEXP;
        
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyRegexp.class);
        
        regexpClass.defineConstant("IGNORECASE", runtime.newFixnum(Pattern.RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", runtime.newFixnum(Pattern.RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", runtime.newFixnum(Pattern.RE_OPTION_MULTILINE));

        regexpClass.defineFastMethod("initialize", callbackFactory.getFastOptMethod("initialize"));
        regexpClass.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("initialize_copy",RubyKernel.IRUBY_OBJECT));        
        regexpClass.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));
        regexpClass.defineFastMethod("eql?", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("==", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("=~", callbackFactory.getFastMethod("match", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("===", callbackFactory.getFastMethod("eqq", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("~", callbackFactory.getFastMethod("match2"));
        regexpClass.defineFastMethod("match", callbackFactory.getFastMethod("match_m", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        regexpClass.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        regexpClass.defineFastMethod("source", callbackFactory.getFastMethod("source"));
        regexpClass.defineFastMethod("casefold?", callbackFactory.getFastMethod("casefold_p"));
        regexpClass.defineFastMethod("kcode", callbackFactory.getFastMethod("kcode"));

        regexpClass.getMetaClass().defineFastMethod("new", callbackFactory.getFastOptSingletonMethod("newInstance"));
        regexpClass.getMetaClass().defineFastMethod("compile", callbackFactory.getFastOptSingletonMethod("newInstance"));
        regexpClass.getMetaClass().defineFastMethod("quote", callbackFactory.getFastOptSingletonMethod("quote"));
        regexpClass.getMetaClass().defineFastMethod("escape", callbackFactory.getFastOptSingletonMethod("quote"));
        regexpClass.getMetaClass().defineFastMethod("last_match", callbackFactory.getFastSingletonMethod("last_match_s"));
        regexpClass.getMetaClass().defineFastMethod("union", callbackFactory.getFastOptSingletonMethod("union"));

        return regexpClass;
    }

    public static RubyRegexp newRegexp(Ruby runtime, String pattern, int options, String kcode) {
        return newRegexp(runtime, pattern.toCharArray(), options, kcode);
    }

    public static RubyRegexp newRegexp(Ruby runtime, char[] pattern, int options, String kcode) {
        return null;
    }

    public static RubyRegexp newRegexp(Ruby runtime, Pattern pattern, String kcode) {
        return null;
    }
    
    public int getNativeTypeIndex() {
        return ClassIndex.REGEXP;
    }

    public Pattern getPattern() {
        return null;
    }

    public IRubyObject match2() {
        return null;
    }
    































    /** rb_reg_nth_match
     *
     */
    public static IRubyObject nth_match(int n, IRubyObject match) {
        /*
        IRubyObject nil = match.getRuntime().getNil();
        if (match.isNil()) {
            return nil;
        }
        
        RubyMatchData rmd = (RubyMatchData) match;
        
        if (n > rmd.getSize()) {
            return nil;
        }
        
        if (n < 0) {
            n += rmd.getSize();
            if (n <= 0) {
                return nil;
            }
        }
        return rmd.group(n);
        */
        return null;
    }

    /** rb_reg_last_match
     *
     */
    public static IRubyObject last_match(IRubyObject match) {
        //        return match.isNil() ? match : ((RubyMatchData) match).group(0);
        return null;
    }

    /** rb_reg_match_pre
     *
     */
    public static IRubyObject match_pre(IRubyObject match) {
        //        return match.isNil() ? match : ((RubyMatchData) match).pre_match();
        return null;
    }

    /** rb_reg_match_post
     *
     */
    public static IRubyObject match_post(IRubyObject match) {
        //        return match.isNil() ? match : ((RubyMatchData) match).post_match();
        return null;
    }

    /** rb_reg_match_last
     *
     */
    public static IRubyObject match_last(IRubyObject match) {
        /*
        if (match.isNil()) {
            return match;
        }
        RubyMatchData md = (RubyMatchData) match;
        for (long i = md.getSize() - 1; i > 0; i--) {
            if (!md.group(i).isNil()) {
                return md.group(i);
            }
        }
        return md.getRuntime().getNil();
        */
        return null;
    }
}
