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
    private boolean kcode_default = true;
    private KCode kcode;
    private Pattern ptr;
    private byte[] str;
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
        regexpClass.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("initialize_copy",RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));
        regexpClass.defineFastMethod("eql?", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("==", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("=~", callbackFactory.getFastMethod("match", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("match", callbackFactory.getFastMethod("match_m", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("===", callbackFactory.getFastMethod("eqq", RubyKernel.IRUBY_OBJECT));
        regexpClass.defineFastMethod("~", callbackFactory.getFastMethod("match2"));
        regexpClass.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        regexpClass.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        regexpClass.defineFastMethod("source", callbackFactory.getFastMethod("source"));
        regexpClass.defineFastMethod("casefold?", callbackFactory.getFastMethod("casefold_p"));
        regexpClass.defineFastMethod("kcode", callbackFactory.getFastMethod("kcode"));
        regexpClass.getMetaClass().defineAlias("compile","new");
        regexpClass.getMetaClass().defineFastMethod("quote", callbackFactory.getFastOptSingletonMethod("quote"));
        regexpClass.getMetaClass().defineFastMethod("escape", callbackFactory.getFastOptSingletonMethod("quote"));
        regexpClass.getMetaClass().defineFastMethod("last_match", callbackFactory.getFastOptSingletonMethod("last_match_s"));
        regexpClass.getMetaClass().defineFastMethod("union", callbackFactory.getFastOptSingletonMethod("union"));
        return regexpClass;
    }

    public static RubyRegexp newRegexp(Ruby runtime, String pattern, int options, String kcode) {
        return newRegexp(runtime, ByteList.plain(pattern), options, kcode);
    }

    public static RubyRegexp newRegexp(IRubyObject ptr, int options, String kcode) {
        return newRegexp(ptr.getRuntime(), ptr.convertToString().getByteList(), options, kcode);
    }

    public static RubyRegexp newRegexp(Ruby runtime, ByteList pattern, int options, String kcode) {
        return newRegexp(runtime, pattern.bytes, pattern.realSize, options, kcode);
    }

    public static RubyRegexp newRegexp(Ruby runtime, byte[] pattern, int options, String kcode) {
        return newRegexp(runtime, pattern, pattern.length, options, kcode);
    }

    public static RubyRegexp newRegexp(Ruby runtime, byte[] pattern, int len, int options, String kcode) {
        RubyRegexp rr = new RubyRegexp(runtime);
        rr.initialize(pattern,len,options);
        return rr;
    }

    public IRubyObject kcode() {
        if(!kcode_default) {
            return getRuntime().newString(kcode.name());
        }
        return getRuntime().getNil();
    }

    public int getNativeTypeIndex() {
        return ClassIndex.REGEXP;
    }

    public Pattern getPattern() {
        return ptr;
    }

    public RubyFixnum hash() {
        rb_reg_check(this);
        int hashval = (int)ptr.options;
        int len = this.len;
        int p = 0;
        while(len-->0) {
            hashval = hashval * 33 + str[p++];
        }
        hashval = hashval + (hashval>>5);
        return getRuntime().newFixnum(hashval);
    }
    
    private final static boolean memcmp(byte[] s1, byte[] s2, int len) {
        int x = 0;
        while(len-->0) {
            if(s1[x] != s2[x]) {
                return false;
            }
            x++;
        }
        return true;
    }

    public IRubyObject equal(IRubyObject other) {
        if(this == other) {
            return getRuntime().getTrue();
        }
        if(!(other instanceof RubyRegexp)) {
            return getRuntime().getFalse();
        }
        rb_reg_check(this);
        rb_reg_check((RubyRegexp)other);
        if(len != ((RubyRegexp)other).len) {
            return getRuntime().getFalse();
        }
        if(memcmp(str,((RubyRegexp)other).str, len) &&
           kcode == ((RubyRegexp)other).kcode &&
           ptr.options == ((RubyRegexp)other).ptr.options) {
            return getRuntime().getTrue();
        }
        return getRuntime().getFalse();
    }

    public IRubyObject match2() {
        IRubyObject line = getRuntime().getCurrentContext().getLastline();
        if(!(line instanceof RubyString)) {
            getRuntime().getCurrentContext().setBackref(getRuntime().getNil());
            return getRuntime().getNil();
        }
        int start = search((RubyString)line,0,false);
        if(start < 0) {
            return getRuntime().getNil();
        } else {
            return getRuntime().newFixnum(start);
        }
    }

    /** rb_reg_eqq
     * 
     */
    public IRubyObject eqq(IRubyObject target) {
        int start;
        IRubyObject str = target;
        if(!(str instanceof RubyString)) {
            str = target.checkStringType();
        }
        if(str.isNil()) {
            getRuntime().getCurrentContext().setBackref(getRuntime().getNil());
            return getRuntime().getFalse();
        }
        start = search((RubyString)str,0,false);
        return (start < 0) ? getRuntime().getFalse() : getRuntime().getTrue();
    }

    private void rb_reg_check(IRubyObject re) {
        if(((RubyRegexp)re).ptr == null || ((RubyRegexp)re).str == null) {
            throw getRuntime().newTypeError("uninitialized Regexp");
        }
    }
    
    /** rb_reg_initialize
     */
    private void initialize(byte[] s, int len, int options) {
        if(isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify regexp");
        }
        checkFrozen();
        if(literal) {
            throw getRuntime().newSecurityError("can't modify literal regexp");
        }

        kcode_default = false;
        switch(options & ~0xf) {
        case 0:
        default:
            kcode_default = true;
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
        str = new byte[len];
        System.arraycopy(s,0,str,0,len);
        this.len = len;
    }

    private final Pattern make_regexp(byte[] s, int len, int flags, Pattern.CompileContext ctx) {
        Pattern rp = new Pattern(new byte[16],16,new byte[256],flags);
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

    private final StringBuffer rb_reg_desc(byte[] s, int len) {
        StringBuffer sb = new StringBuffer("/");
        rb_reg_expr_str(sb, s, len);
        sb.append("/");
        return sb;
    }

    private final void rb_reg_expr_str(StringBuffer sb, byte[] s, int len) {
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
            sb.append(ByteList.createString(s,0,len));
        } else {
            p = 0;
            while(p < pend) {
                if(s[p] == '\\') {
                    int n = Pattern.mbclen(s[p+1],ctx) + 1;
                    sb.append(ByteList.createString(s,p,n));
                    p += n;
                    continue;
                } else if(s[p] == '/') {
                    sb.append("\\/");
                } else if(Pattern.ismbchar(s[p],ctx)) {
                    sb.append(ByteList.createString(s,p,Pattern.mbclen(s[p],ctx)));
                    p += Pattern.mbclen(s[p],ctx);
                    continue;
                } else if((' ' == s[p] || (!Character.isWhitespace(s[p]) && 
                                           !Character.isISOControl(s[p])))) {
                    sb.append((char)(s[p]&0xFF));
                } else if(!Character.isSpace((char)(s[p]&0xFF))) {
                    sb.append('\\');
                    sb.append(Integer.toString((int)(s[p]&0377),8));
                } else {
                    sb.append((char)(s[p]&0xFF));
                }
                p++;
            }
        }
    }    

    private final void rb_reg_raise(byte[] s, int len, String err) {
        throw getRuntime().newRegexpError(err + ": " + rb_reg_desc(s,len));
    }

    /** rb_reg_init_copy
     */
    public IRubyObject initialize_copy(IRubyObject re) {
        if(this == re) {
            return this;
        }
        checkFrozen();

        if(getMetaClass().getRealClass() != re.getMetaClass().getRealClass()) {
            throw getRuntime().newTypeError("wrong argument type");
	    }

        rb_reg_check(re);

        initialize(((RubyRegexp)re).str, ((RubyRegexp)re).len, ((RubyRegexp)re).rb_reg_options());
        return this;
    }

    private int rb_reg_get_kcode() {
        if(kcode == KCode.NONE) {
            return 16;
        } else if(kcode == KCode.EUC) {
            return 32;
        } else if(kcode == KCode.SJIS) {
            return 48;
        } else if(kcode == KCode.UTF8) {
            return 64;
        }
        return 0;
    }

    /** rb_reg_options
     */
    private int rb_reg_options() {
        rb_reg_check(this);
        int options = (int)(ptr.options & (RE_OPTION_IGNORECASE|RE_OPTION_MULTILINE|RE_OPTION_EXTENDED));
        if(!kcode_default) {
            options |= rb_reg_get_kcode();
        }
        return options;
    }

    /** rb_reg_initialize_m
     */
    public IRubyObject initialize_m(IRubyObject[] args) {
        byte[] s;
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
            if(!r.kcode_default && r.kcode != null && r.kcode != KCode.NIL) {
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
            ByteList bl = args[0].convertToString().getByteList();
            s = bl.bytes;
            len = bl.realSize;
        }
        initialize(s, len, flags);
        return this;
    }

    /** rb_reg_search
     */
    public int search(RubyString str, int pos, boolean reverse) {
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
        ByteList bl = str.getByteList();
        byte[] cstr = bl.bytes;
        result = ptr.search(cstr,bl.realSize,pos,range,regs);

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
        ((RubyMatchData)match).str = (byte[])cstr.clone();
        ((RubyMatchData)match).len = bl.realSize;
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

    /** rb_reg_match_m
     * 
     */
    public IRubyObject match_m(IRubyObject str) {
        if(match(str).isNil()) {
            return getRuntime().getNil();
        }
        return getRuntime().getCurrentContext().getBackref();
    }

    public RubyString regsub(RubyString str, RubyString src, Registers regs) {
        int p,s,e;
        char c;
        p = s = 0;
        int no = -1;
        ByteList bs = str.getByteList();
        ByteList srcbs = src.getByteList();
        e = bs.length();
        RubyString val = null;
        Pattern.CompileContext ctx = kcode.getContext();

        while(s < e) {
            int ss = s;
            c = bs.charAt(s++);
            if(Pattern.ismbchar(c,ctx)) {
                s += Pattern.mbclen(c,ctx) - 1;
                continue;
            }
            if(c != '\\' || s == e) {
                continue;
            }
            if(val == null) {
                val = RubyString.newString(getRuntime(),new ByteList(ss-p));
            }
            val.cat(bs,p,ss-p);
            c = bs.charAt(s++);
            p = s;
            switch(c) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                no = c - '0';
                break;
            case '&':
                no = 0;
                break;
            case '`':
                val.cat(srcbs,0,regs.beg[0]);
                continue;

            case '\'':
                val.cat(srcbs,regs.end[0],src.getByteList().realSize-regs.end[0]);
                continue;

            case '+':
                no = regs.num_regs-1;
                while(regs.beg[no] == -1 && no > 0) {
                    no--;
                }
                if(no == 0) {
                    continue;
                }
                break;
            case '\\':
                val.cat(bs,s-1,1);
                continue;
            default:
                val.cat(bs,s-2,2);
                continue;
            }
            if (no >= 0) {
                if(no >= regs.num_regs) {
                    continue;
                }
                if(regs.beg[no] == -1) {
                    continue;
                }
                val.cat(srcbs,regs.beg[no],regs.end[no]-regs.beg[no]);
            }
        }

        if(p < e) {
            if(val == null) {
                val = RubyString.newString(getRuntime(),new ByteList(bs,p,e-p));
            } else {
                val.cat(bs,p,e-p);
            }
        }
        if(val == null) {
            return str;
        }

        return val;
    }

    public int adjust_startpos(IRubyObject str, int pos, boolean reverse) {
        int range;
        rb_reg_check(this);
        if(reverse) {
            range = -pos;
        } else {
            range = ((RubyString)str).getByteList().length() - pos;
        }

        return ptr.adjust_startpos(((RubyString)str).getByteList().bytes, ((RubyString)str).getByteList().realSize, pos, range);
    }

    public IRubyObject casefold_p() {
        rb_reg_check(this);
        if((ptr.options & Pattern.RE_OPTION_IGNORECASE) != 0) {
            return getRuntime().getTrue();
        }
        return getRuntime().getFalse();
    }

    public IRubyObject source() {
        rb_reg_check(this);
        RubyString str = RubyString.newString(getRuntime(), new String(this.str,0,this.len));
        if(isTaint()) {
            str.taint();
        }
        return str;
    }

    public IRubyObject inspect() {
        rb_reg_check(this);
        return getRuntime().newString(rb_reg_desc(str,len).toString());
    }

    private final static int EMBEDDABLE = Pattern.RE_OPTION_MULTILINE|Pattern.RE_OPTION_IGNORECASE|Pattern.RE_OPTION_EXTENDED;

    public IRubyObject to_s() {
        int options;
        int l;
        int p;
        RubyString ss = getRuntime().newString("(?");
        rb_reg_check(this);
        options = (int)ptr.options;
        p = 0;
        l = len;

        again: do {
            if(l >= 4 && str[p] == '(' && str[p+1] == '?') {
                boolean err = true;
                p += 2;
                if((l -= 2) > 0) {
                    do {
                        if(str[p] == 'm') {
                            options |= Pattern.RE_OPTION_MULTILINE;
                        } else if(str[p] == 'i') {
                            options |= Pattern.RE_OPTION_IGNORECASE;
                        } else if(str[p] == 'x') {
                            options |= Pattern.RE_OPTION_EXTENDED;
                        } else {
                            break;
                        }
                        p++;
                    } while(--l > 0);
                }
                if(l > 1 && str[p] == '-') {
                    ++p;
                    --l;
                    do {
                        if(str[p] == 'm') {
                            options &= ~Pattern.RE_OPTION_MULTILINE;
                        } else if(str[p] == 'i') {
                            options &= ~Pattern.RE_OPTION_IGNORECASE;
                        } else if(str[p] == 'x') {
                            options &= ~Pattern.RE_OPTION_EXTENDED;
                        } else {
                            break;
                        }
                        p++;
                    } while(--l > 0);
                }
                if(str[p] == ')') {
                    --l;
                    ++p;
                    continue again;
                }
                if(str[p] == ':' && str[p+l-1] == ')') {
                    try {
                        Pattern.compile_s(str,++p,l-=2,kcode.getContext());
                        err = false;
                    } catch(Exception e) {
                        err = true;
                    }
                }
                if(err) {
                    options = (int)ptr.options;
                    p = 0;
                    l = len;
                }
            }

            if((options & RE_OPTION_MULTILINE)!=0) ss.cat("m");
            if((options & RE_OPTION_IGNORECASE)!=0) ss.cat("i");
            if((options & RE_OPTION_EXTENDED)!=0) ss.cat("x");

            if((options&EMBEDDABLE) != EMBEDDABLE) {
                ss.cat("-");
                if((options & RE_OPTION_MULTILINE)==0) ss.cat("m");
                if((options & RE_OPTION_IGNORECASE)==0) ss.cat("i");
                if((options & RE_OPTION_EXTENDED)==0) ss.cat("x");
            }
            ss.cat(":");
            rb_reg_expr_str(ss,p,l);
            ss.cat(")");
            ss.infectBy(this);
            return ss;
        } while(true);
    }

    private final boolean ISPRINT(byte c) {
        return ISPRINT((char)(c&0xFF));
    }

    private final boolean ISPRINT(char c) {
        return (' ' == c || (!Character.isWhitespace(c) && !Character.isISOControl(c)));
    }

    private final void rb_reg_expr_str(RubyString ss, int s, int l) {
        int p = s;
        int pend = l;
        boolean need_escape = false;
        while(p<pend) {
            if(str[p] == '/' || (!ISPRINT(str[p]) && !Pattern.ismbchar(str[p],kcode.getContext()))) {
                need_escape = true;
                break;
            }
            p += Pattern.mbclen(str[p],kcode.getContext());
        }
        if(!need_escape) {
            ss.cat(str,s,l);
        } else {
            p = s; 
            while(p<pend) {
                if(str[p] == '\\') {
                    int n = Pattern.mbclen(str[p+1],kcode.getContext()) + 1;
                    ss.cat(str,p,n);
                    p += n;
                    continue;
                } else if(str[p] == '/') {
                    char c = '\\';
                    ss.cat((byte)c);
                    ss.cat(str,p,1);
                } else if(Pattern.ismbchar(str[p],kcode.getContext())) {
                    ss.cat(str,p,Pattern.mbclen(str[p],kcode.getContext()));
                    p += Pattern.mbclen(str[p],kcode.getContext());
                    continue;
                } else if(ISPRINT(str[p])) {
                    ss.cat(str,p,1);
                } else if(!Character.isWhitespace(str[p])) {
                    ss.cat(Integer.toString(str[p]&0377,8));
                } else {
                    ss.cat(str,p,1);
                }
                p++;
            }
        }
    }

    public static RubyRegexp regexpValue(IRubyObject obj) {
        if(obj instanceof RubyRegexp) {
            return (RubyRegexp)obj;
        } else if (obj instanceof RubyString) {
            return newRegexp(obj.getRuntime(), obj.toString(), 0, null);
        } else {
            throw obj.getRuntime().newArgumentError("can't convert arg to Regexp");
        }
    }

    /** rb_reg_s_quote
     * 
     */
    public static RubyString quote(IRubyObject recv, IRubyObject[] args) {
        IRubyObject str;
        IRubyObject kcode = null;
        if(recv.checkArgumentCount(args,1,2) == 2) {
            kcode = args[1];
        }
        str = args[0];
        KCode code = recv.getRuntime().getKCode();
        if(kcode != null && !kcode.isNil()) {
            code = KCode.create(recv.getRuntime(),kcode.toString());
        }
        return quote(str,code);
    }

    /** rb_reg_quote
     *
     */
    public static RubyString quote(IRubyObject _str, KCode kcode) {
        RubyString str = _str.convertToString();
        if(null == kcode) {
            kcode = str.getRuntime().getKCode();
        }
        ByteList bs = str.getByteList();
        int tix = 0;
        int s = 0;
        char c;
        int send = str.getByteList().length();
        Pattern.CompileContext ctx = kcode.getContext();
        meta_found: do {
            for(; s<send; s++) {
                c = bs.charAt(s);
                if(Pattern.ismbchar(c,ctx)) {
                    int n = Pattern.mbclen(c,ctx);
                    while(n-- > 0 && s < send) {
                        s++;
                    }
                    s--;
                    continue;
                }
                switch (c) {
                case '[': case ']': case '{': case '}':
                case '(': case ')': case '|': case '-':
                case '*': case '.': case '\\':
                case '?': case '+': case '^': case '$':
                case ' ': case '#':
                case '\t': case '\f': case '\n': case '\r':
                    break meta_found;
                }
            }
            return str;
        } while(false);
        ByteList b1 = new ByteList(send*2);
        System.arraycopy(bs.bytes,0,b1.bytes,0,s);
        tix += s;

        for(; s<send; s++) {
            c = bs.charAt(s);
            if(Pattern.ismbchar(c,ctx)) {
                int n = Pattern.mbclen(c,ctx);
                while(n-- > 0 && s < send) {
                    b1.bytes[tix++] = bs.bytes[s++];
                }
                s--;
                continue;
            }

            switch(c) {
            case '[': case ']': case '{': case '}':
            case '(': case ')': case '|': case '-':
            case '*': case '.': case '\\':
            case '?': case '+': case '^': case '$':
            case '#':
                b1.bytes[tix++] = '\\';
                break;
            case ' ':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = ' ';
                continue;
            case '\t':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 't';
                 continue;
            case '\n':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 'n';
                continue;
            case '\r':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 'r';
                continue;
            case '\f':
                b1.bytes[tix++] = '\\';
                b1.bytes[tix++] = 'f';
                continue;
            }
            b1.bytes[tix++] = (byte)c;
        }
        b1.realSize = tix;
        RubyString tmp = RubyString.newString(str.getRuntime(),b1);
        tmp.infectBy(str);
        return tmp;
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
        RubyString str = RubyString.newString(match.getRuntime(), new ByteList(m.str,start,end-start,false));
        str.infectBy(match);
        return str;
    }

    /** rb_reg_last_match
     *
     */
    public static IRubyObject last_match(IRubyObject match) {
        return nth_match(0,match);
    }

    /** rb_reg_s_last_match
     *
     */
    public static IRubyObject last_match_s(IRubyObject recv, IRubyObject[] args) {
        if(recv.checkArgumentCount(args,0,1) == 1) {
            return nth_match(RubyNumeric.fix2int(args[0]), recv.getRuntime().getCurrentContext().getBackref());
        }
        return recv.getRuntime().getCurrentContext().getBackref();
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
        RubyString str = RubyString.newString(match.getRuntime(), new ByteList(m.str,0,m.regs.beg[0]));
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
        RubyString str = RubyString.newString(match.getRuntime(), new ByteList(m.str,m.regs.end[0],m.str.length-m.regs.end[0]));
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

    /** rb_reg_s_union
     *
     */
    public static IRubyObject union(IRubyObject recv, IRubyObject[] args) {
        if(args.length == 0) {
            return newRegexp(recv.getRuntime(),"(?!)",0,null);
        } else if(args.length == 1) {
            IRubyObject v = args[0].convertToTypeWithCheck("Regexp","to_regexp");
            if(!v.isNil()) {
                return v;
            } else {
                return newRegexp(quote(recv,args),0,null);
            }
        } else {
            KCode kcode = null;
            IRubyObject kcode_re = recv.getRuntime().getNil();
            RubyString source = recv.getRuntime().newString("");
            IRubyObject[] _args = new IRubyObject[3];

            for(int i = 0; i < args.length; i++) {
                if(0 < i) {
                    source.cat("|");
                }
                IRubyObject v = args[i].convertToTypeWithCheck("Regexp","to_regexp");
                if(!v.isNil()) {
                    if(!((RubyRegexp)v).kcode_default) {
                        if(kcode == null) {
                            kcode_re = v;
                            kcode = ((RubyRegexp)v).kcode;
                        } else if(((RubyRegexp)v).kcode != kcode) {
                            IRubyObject str1 = kcode_re.inspect();
                            IRubyObject str2 = v.inspect();
                            throw recv.getRuntime().newArgumentError("mixed kcode " + str1 + " and " + str2);
                        }
                    }
                    v = ((RubyRegexp)v).to_s();
                } else {
                    v = quote(recv, new IRubyObject[]{args[i]});
                }
                source.append(v);
            }

            _args[0] = source;
            _args[1] = recv.getRuntime().getNil();
            if(kcode == null) {
                _args[2] = recv.getRuntime().getNil();
            } else if(kcode == KCode.NONE) {
                _args[2] = recv.getRuntime().newString("n");
            } else if(kcode == KCode.EUC) {
                _args[2] = recv.getRuntime().newString("e");
            } else if(kcode == KCode.SJIS) {
                _args[2] = recv.getRuntime().newString("s");
            } else if(kcode == KCode.UTF8) {
                _args[2] = recv.getRuntime().newString("u");
            }
            return recv.callMethod(recv.getRuntime().getCurrentContext(),"new",_args);
        }
    }

    public static RubyRegexp unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyRegexp result = newRegexp(input.getRuntime(), 
                                      RubyString.byteListToString(input.unmarshalString()), input.unmarshalInt(), null);
        input.registerLinkTarget(result);
        return result;
    }

    public static void marshalTo(RubyRegexp regexp, MarshalStream output) throws java.io.IOException {
        output.writeString(new String(regexp.str,0,regexp.len));
        output.writeInt(((int)regexp.ptr.options) & EMBEDDABLE);
    }
}
