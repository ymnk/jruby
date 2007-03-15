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

import java.util.Iterator;

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
import org.rej.PatternSyntaxException;
import org.rej.Registers;

/**
 *
 * @author  amoore
 */
public class RubyRegexp extends RubyObject implements ReOptions {
    private KCode kcode;
    private Pattern ptr;
    private char[] str;
    private int len;
    private boolean literal;

    public void setLiteral() {
        literal = true;
    }

    public KCode getKCode() {
        return kcode;
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

        regexpClass.defineFastMethod("initialize", callbackFactory.getFastOptMethod("initialize_m"));
        /*
        regexpClass.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("initialize_copy",RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));
        regexpClass.defineFastMethod("eql?", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("==", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        */
        regexpClass.defineFastMethod("=~", callbackFactory.getFastMethod("match", RubyKernel.IRUBY_OBJECT));
        /*
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
        */
        return regexpClass;
    }

    public static RubyRegexp newRegexp(Ruby runtime, String pattern, int options, String kcode) {
        return newRegexp(runtime, pattern.toCharArray(), options, kcode);
    }

    public static RubyRegexp newRegexp(Ruby runtime, char[] pattern, int options, String kcode) {
        RubyRegexp rr = new RubyRegexp(runtime);
        rr.initialize(pattern,pattern.length,options);
        return rr;
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

    private void rb_reg_check(IRubyObject re) {
        if(((RubyRegexp)re).ptr == null || ((RubyRegexp)re).str == null) {
            throw getRuntime().newTypeError("uninitialized Regexp");
        }
    }
    
    /** rb_reg_initialize
     */
    private void initialize(char[] s, int len, int options) {
        if(isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify regexp");
        }
        checkFrozen();
        if(literal) {
            throw getRuntime().newSecurityError("can't modify literal regexp");
        }

        switch(options & ~0xf) {
        case 0:
        default:
            kcode = getRuntime().getKCode();
            break;
        case 16:
            kcode = KCode.NONE;
            break;
        case 32:
            kcode = KCode.EUC;
            break;
        case 48:
            kcode = KCode.SJIS;
            break;
        case 64:
            kcode = KCode.UTF8;
            break;
        }
        ptr = make_regexp(s, len, options & 0xf, kcode.getContext());
        str = new char[len];
        System.arraycopy(s,0,str,0,len);
        this.len = len;
    }

    private final Pattern make_regexp(char[] s, int len, int flags, Pattern.CompileContext ctx) {
        Pattern rp = new Pattern(new char[16],16,new char[256],flags);
        try {
            Pattern.compile(s,0,len,rp,ctx);
        } catch(PatternSyntaxException e) {
            rb_reg_raise(s,len,e.getMessage());
        }
        for(Iterator iter = rp.getWarnings().iterator();iter.hasNext();) {
            getRuntime().getWarnings().warn(iter.next().toString());
        }
        rp.clearWarnings();
        return rp;
    }

    private final StringBuffer rb_reg_desc(char[] s, int len) {
        StringBuffer sb = new StringBuffer("/");
        rb_reg_expr_str(sb, s, len);
        sb.append("/");
        return sb;
    }

    private final void rb_reg_expr_str(StringBuffer sb, char[] s, int len) {
        int p,pend;
        boolean need_escape = false;
        p = 0;
        pend = len;
        Pattern.CompileContext ctx = kcode.getContext();
        while(p<pend) {
            if(s[p] == '/' || (!(' ' == s[p] || (!Character.isWhitespace(s[p]) && 
                                                 !Character.isISOControl(s[p]))) && 
                               !Pattern.ismbchar(s[p],ctx))) {
                need_escape = true;
                break;
            }
            p += Pattern.mbclen(s[p],ctx);
        }
        if(!need_escape) {
            sb.append(s,0,len);
        } else {
            p = 0;
            while(p < pend) {
                if(s[p] == '\\') {
                    int n = Pattern.mbclen(s[p+1],ctx) + 1;
                    sb.append(s,p,n);
                    p += n;
                    continue;
                } else if(s[p] == '/') {
                    sb.append("\\/");
                } else if(Pattern.ismbchar(s[p],ctx)) {
                    sb.append(s,p,Pattern.mbclen(s[p],ctx));
                    p += Pattern.mbclen(s[p],ctx);
                    continue;
                } else if((' ' == s[p] || (!Character.isWhitespace(s[p]) && 
                                           !Character.isISOControl(s[p])))) {
                    sb.append(s[p]);
                } else if(!Character.isSpace(s[p])) {
                    sb.append('\\');
                    sb.append(Integer.toString((int)(s[p]&0377),8));
                } else {
                    sb.append(s[p]);
                }
                p++;
            }
        }
    }    

    private final void rb_reg_raise(char[] s, int len, String err) {
        throw getRuntime().newRegexpError(err + ": " + rb_reg_desc(s,len));
    }

    /** rb_reg_initialize_m
     */
    public IRubyObject initialize_m(IRubyObject[] args) {
        System.err.println("initialize_m");
        char[] s;
        int len;
        int flags = 0;

        if(args.length == 0 || args.length > 3) {
            throw getRuntime().newArgumentError("wrong number of arguments");
        }

        if(args[0] instanceof RubyRegexp) {
            if(args.length > 1) {
                getRuntime().getWarnings().warn("flags" +((args.length == 3)?" and encoding":"")+ " ignored");
            }
            rb_reg_check(args[0]);
            RubyRegexp r = (RubyRegexp)args[0];
            flags = (int)r.ptr.options & 0xF;
            if(r.kcode != null && r.kcode != KCode.NIL) {
                if(r.kcode == KCode.NONE) {
                    flags |= 16;
                } else if(r.kcode == KCode.EUC) {
                    flags |= 32;
                } else if(r.kcode == KCode.SJIS) {
                    flags |= 48;
                } else if(r.kcode == KCode.UTF8) {
                    flags |= 64;
                }
            }
            s = r.str;
            len = r.len;
        } else {
            if(args.length >= 2) {
                if(args[1] instanceof RubyFixnum) {
                    flags = RubyNumeric.fix2int(args[1]);
                } else if(args[1].isTrue()) {
                    flags = Pattern.RE_OPTION_IGNORECASE;
                }
            }
            if(args.length == 3 && !args[2].isNil()) {
                char first = args[2].convertToString().toString().charAt(0);
                flags &= ~0x70;
                switch(first) {
                case 'n': case 'N':
                    flags |= 16;
                    break;
                case 'e': case 'E':
                    flags |= 32;
                    break;
                case 's': case 'S':
                    flags |= 48;
                    break;
                case 'u': case 'U':
                    flags |= 64;
                    break;
                default:
                    break;
                }
            }
            s = args[0].convertToString().getByteList().toCharArray();
            len = s.length;
        }
        initialize(s, len, flags);
        return this;
    }

    /** rb_reg_search
     */
    private int search(RubyString str, int pos, boolean reverse) {
        int result;
        IRubyObject match;
        Registers regs = new Registers();
        int range;
        
        if(pos > str.getByteList().length() || pos < 0) {
            getRuntime().getCurrentContext().setBackref(getRuntime().getNil());
            return -1;
        }
        rb_reg_check(this);
        
        if(reverse) {
            range = -pos;
        } else {
            range = str.getByteList().length() - pos;
        }
        char[] cstr = str.getByteList().toCharArray();
        result = ptr.search(cstr,cstr.length,pos,range,regs);

        if(result == -2) {
            rb_reg_raise(cstr,len,"Stack overflow in regexp matcher");
        }
        if(result < 0) {
            getRuntime().getCurrentContext().setBackref(getRuntime().getNil());
            return result;
        }
        match = getRuntime().getCurrentContext().getBackref();
        if(match.isNil()) {
            match = new RubyMatchData(getRuntime());
        } else {
            if(getRuntime().getSafeLevel() >= 3) {
                match.taint();
            } else {
                match.untaint();
            }
        }

        ((RubyMatchData)match).regs = regs.copy();
        ((RubyMatchData)match).str = (char[])cstr.clone();
        getRuntime().getCurrentContext().setBackref(match);
            
        match.infectBy(this);
        match.infectBy(str);
        return result;
    }

    /** rb_reg_match
     * 
     */
    public IRubyObject match(IRubyObject str) {
        int start;
        if(str.isNil()) {
            getRuntime().getCurrentContext().setBackref(getRuntime().getNil());
            return str;
        }
        
        RubyString s = str.convertToString();
        start = search(s,0,false);
        if(start < 0) {
            return getRuntime().getNil();
        }
        return RubyFixnum.newFixnum(getRuntime(),start);
    }

    /** rb_reg_nth_match
     *
     */
    public static IRubyObject nth_match(int nth, IRubyObject match) {
        int start, end;
        IRubyObject nil = match.getRuntime().getNil();
        if(match.isNil()) {
            return nil;
        }
        RubyMatchData m = (RubyMatchData)match;
        if(nth >= m.regs.num_regs) {
            return nil;
        }
        if(nth < 0) {
            nth += m.regs.num_regs;
            if(nth <= 0) {
                return nil;
            }
        }
        start = m.regs.beg[nth];
        if(start == -1) {
            return nil;
        }
        end = m.regs.end[nth];
        RubyString str = RubyString.newString(match.getRuntime(), new ByteList(ByteList.plain(m.str),start,end-start,false));
        str.infectBy(match);
        return str;
    }

    /** rb_reg_last_match
     *
     */
    public static IRubyObject last_match(IRubyObject match) {
        return nth_match(0, match);
    }

    /** rb_reg_match_pre
     *
     */
    public static IRubyObject match_pre(IRubyObject match) {
        IRubyObject nil = match.getRuntime().getNil();
        if(match.isNil()) {
            return nil;
        }
        RubyMatchData m = (RubyMatchData)match;
        if(m.regs.beg[0] == -1) {
            return nil;
        }
        RubyString str = RubyString.newString(match.getRuntime(), new ByteList(ByteList.plain(m.str),0,m.regs.beg[0],false));
        str.infectBy(match);
        return str;
    }

    /** rb_reg_match_post
     *
     */
    public static IRubyObject match_post(IRubyObject match) {
        IRubyObject nil = match.getRuntime().getNil();
        if(match.isNil()) {
            return nil;
        }
        RubyMatchData m = (RubyMatchData)match;
        if(m.regs.beg[0] == -1) {
            return nil;
        }
        RubyString str = RubyString.newString(match.getRuntime(), new ByteList(ByteList.plain(m.str),m.regs.end[0],m.str.length-m.regs.end[0],false));
        str.infectBy(match);
        return str;
    }

    /** rb_reg_match_last
     *
     */
    public static IRubyObject match_last(IRubyObject match) {
        IRubyObject nil = match.getRuntime().getNil();
        if(match.isNil()) {
            return nil;
        }
        RubyMatchData m = (RubyMatchData)match;
        if(m.regs.beg[0] == -1) {
            return nil;
        }
        int i=0;
        for(i=m.regs.num_regs-1; m.regs.beg[i] == -1 && i>0; i--);
        if(i == 0) {
            return nil;
        }
        return nth_match(i,match);
    }
}
