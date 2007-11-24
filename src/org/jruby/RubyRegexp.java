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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import org.jruby.anno.JRubyMethod;
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
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;

import org.joni.Regex;
import org.joni.Syntax;
import org.joni.encoding.Encoding;

/**
 *
 */
public class RubyRegexp extends RubyObject implements ReOptions {
    private boolean kcode_default = true;
    private KCode kcode;
    private Regex ptr;
    private boolean literal;
    private ByteList str;

    private final static byte[] PIPE = new byte[]{'|'};
    private final static byte[] DASH = new byte[]{'-'};
    private final static byte[] R_PAREN = new byte[]{')'};
    private final static byte[] COLON = new byte[]{':'};
    private final static byte[] M_CHAR = new byte[]{'m'};
    private final static byte[] I_CHAR = new byte[]{'i'};
    private final static byte[] X_CHAR = new byte[]{'x'};

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
        super(runtime, runtime.getRegexp());
    }
    
    private static ObjectAllocator REGEXP_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyRegexp instance = new RubyRegexp(runtime, klass);
            return instance;
        }
    };

    public static RubyClass createRegexpClass(Ruby runtime) {
        RubyClass regexpClass = runtime.defineClass("Regexp", runtime.getObject(), REGEXP_ALLOCATOR);
        runtime.setRegexp(regexpClass);
        regexpClass.index = ClassIndex.REGEXP;
        regexpClass.kindOf = new RubyModule.KindOf() {
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyRegexp;
                }
            };
        
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyRegexp.class);
        
        regexpClass.defineConstant("IGNORECASE", runtime.newFixnum(RE_OPTION_IGNORECASE));
        regexpClass.defineConstant("EXTENDED", runtime.newFixnum(RE_OPTION_EXTENDED));
        regexpClass.defineConstant("MULTILINE", runtime.newFixnum(RE_OPTION_MULTILINE));
        
        regexpClass.defineAnnotatedMethods(RubyRegexp.class);
        regexpClass.dispatcher = callbackFactory.createDispatcher(regexpClass);

        return regexpClass;
    }
    
    @JRubyMethod(name = "kcode")
    public IRubyObject kcode() {
        if(!kcode_default) {
            return getRuntime().newString(kcode.name());
        }
        return getRuntime().getNil();
    }

    public int getNativeTypeIndex() {
        return ClassIndex.REGEXP;
    }
    
    public Regex getPattern() {
        return ptr;
    }

    private void rb_reg_check(IRubyObject re) {
        if(((RubyRegexp)re).ptr == null || ((RubyRegexp)re).str == null) {
            throw getRuntime().newTypeError("uninitialized Regexp");
        }
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        rb_reg_check(this);
        int hashval = (int)ptr.getOptions();
        int len = this.str.realSize;
        int p = this.str.begin;
        while(len-->0) {
            hashval = hashval * 33 + str.bytes[p++];
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

    @JRubyMethod(name = {"==", "eql?"}, required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        if(this == other) {
            return getRuntime().getTrue();
        }
        if(!(other instanceof RubyRegexp)) {
            return getRuntime().getFalse();
        }
        rb_reg_check(this);
        rb_reg_check((RubyRegexp)other);
        if(str.equals(((RubyRegexp)other).str) &&
           kcode == ((RubyRegexp)other).kcode &&
           ptr.getOptions() == ((RubyRegexp)other).ptr.getOptions()) {
            return getRuntime().getTrue();
        }
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = "~")
    public IRubyObject op_match2() {
        IRubyObject line = getRuntime().getCurrentContext().getCurrentFrame().getLastLine();
        if(!(line instanceof RubyString)) {
            getRuntime().getCurrentContext().getCurrentFrame().setBackRef(getRuntime().getNil());
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
    @JRubyMethod(name = "===", required = 1)
    public IRubyObject eqq(IRubyObject target) {
        int start;
        IRubyObject str = target;
        if(!(str instanceof RubyString)) {
            str = target.checkStringType();
        }
        if(str.isNil()) {
            getRuntime().getCurrentContext().getCurrentFrame().setBackRef(getRuntime().getNil());
            return getRuntime().getFalse();
        }
        start = search((RubyString)str,0,false);
        return (start < 0) ? getRuntime().getFalse() : getRuntime().getTrue();
    }
    
    public void initialize(ByteList regex, int options) {
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

        int extra = getRuntime().getGlobalVariables().get("$=").isTrue() ? ReOptions.RE_OPTION_IGNORECASE : 0;
        ptr = make_regexp(regex, regex.begin, regex.realSize, (options|extra) & 0xf, kcode.getEncoding());
        str = regex.makeShared(0, regex.realSize);
    }

    private final Regex make_regexp(ByteList s, int start, int len, int flags, Encoding enc) {
        try {
            return new Regex(s.bytes,start,len,flags,enc,Syntax.DEFAULT);
        } catch(Exception e) {
            rb_reg_raise(s.bytes,start,len,e.getMessage());
        }
        //TODO: handle joni warnings correctly
    }

    private final void rb_reg_raise(byte[] s, int start, int len, String err) {
        throw getRuntime().newRegexpError(err + ": " + rb_reg_desc(s,start, len));
    }

    private final StringBuffer rb_reg_desc(byte[] s, int start, int len) {
        StringBuffer sb = new StringBuffer("/");
        rb_reg_expr_str(sb, s, start, len);
        sb.append("/");

        if((ptr.getOptions() & ReOptions.RE_OPTION_MULTILINE) != 0) {
            sb.append("m");
        }
        if((ptr.getOptions() & ReOptions.RE_OPTION_IGNORECASE) != 0) {
            sb.append("i");
        }
        if((ptr.getOptions() & ReOptions.RE_OPTION_EXTENDED) != 0) {
            sb.append("x");
        }

        if(kcode != null && !kcode_default) {
            sb.append(kcode.name().charAt(0));
        }
        return sb;
    }

    private final void rb_reg_expr_str(StringBuffer sb, byte[] s, int start, int len) {
        int p,pend;
        boolean need_escape = false;
        p = start;
        pend = start+len;
        Encoding enc = kcode.getEncoding();
        while(p<pend) {
            if(s[p] == '/' || (!(' ' == s[p] || (!Character.isWhitespace(s[p]) && 
                                                 !Character.isISOControl(s[p]))) && 
                               enc.length(s[p])==1)) {
                need_escape = true;
                break;
            }
            p += enc.length(s[p]);
        }
        if(!need_escape) {
            sb.append(new ByteList(s,start,len,false).toString());
        } else {
            p = 0;
            while(p < pend) {
                if(s[p] == '\\') {
                    int n = enc.length(s[p+1]) + 1;
                    sb.append(new ByteList(s,p,n,false).toString());
                    p += n;
                    continue;
                } else if(s[p] == '/') {
                    sb.append("\\/");
                } else if(enc.length(s[p])!=1) {
                    sb.append(new ByteList(s,p,enc.length(s[p]),false).toString());
                    p += enc.length(s[p]);
                    continue;
                } else if((' ' == s[p] || (!Character.isWhitespace(s[p]) && 
                                           !Character.isISOControl(s[p])))) {
                    sb.append((char)(s[p]&0xFF));
                } else if(!Character.isWhitespace((char)(s[p]&0xFF))) {
                    sb.append('\\');
                    sb.append(Integer.toString((int)(s[p]&0377),8));
                } else {
                    sb.append((char)(s[p]&0xFF));
                }
                p++;
            }
        }
    }    

    /** rb_reg_init_copy
     */
    @JRubyMethod(name = "initialize_copy", required = 1)
    public IRubyObject initialize_copy(IRubyObject re) {
        if(this == re) {
            return this;
        }
        checkFrozen();

        if(getMetaClass().getRealClass() != re.getMetaClass().getRealClass()) {
            throw getRuntime().newTypeError("wrong argument type");
	    }

        rb_reg_check(re);

        initialize(((RubyRegexp)re).str, ((RubyRegexp)re).rb_reg_options());

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
        int options = (int)(ptr.getOptions() & (RE_OPTION_IGNORECASE|RE_OPTION_MULTILINE|RE_OPTION_EXTENDED));
        if(!kcode_default) {
            options |= rb_reg_get_kcode();
        }
        return options;
    }

    /** rb_reg_initialize_m
     */
    @JRubyMethod(name = "initialize", optional = 3, visibility = Visibility.PRIVATE)
    public IRubyObject initialize_m(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 1, 3);

        ByteList s;
        int flags = 0;

        if(args[0] instanceof RubyRegexp) {
            if(args.length > 1) {
                getRuntime().getWarnings().warn("flags" +((args.length == 3)?" and encoding":"")+ " ignored");
            }
            rb_reg_check(args[0]);
            RubyRegexp r = (RubyRegexp)args[0];
            flags = (int)r.ptr.getOptions() & 0xF;
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
        } else {
            if(args.length >= 2) {
                if(args[1] instanceof RubyFixnum) {
                    flags = RubyNumeric.fix2int(args[1]);
                } else if(args[1].isTrue()) {
                    flags = RE_OPTION_IGNORECASE;
                }
            }
            if(args.length == 3 && !args[2].isNil()) {
                char first = args[2].convertToString().getByteList().charAt(0);
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
            s = bl;
        }

        initialize(s, flags);

        return this;
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
        if(nth >= m.regs.numRegs) {
            return nil;
        }
        if(nth < 0) {
            nth += m.regs.numRegs;
            if(nth <= 0) {
                return nil;
            }
        }
        start = m.regs.beg[nth];
        if(start == -1) {
            return nil;
        }
        end = m.regs.end[nth];
        RubyString str = m.str.makeShared(start, end-start);
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
    @JRubyMethod(name = "last_match", optional = 1, meta = true)
    public static IRubyObject last_match_s(IRubyObject recv, IRubyObject[] args) {
        if(Arity.checkArgumentCount(recv.getRuntime(), args,0,1) == 1) {
            return nth_match(RubyNumeric.fix2int(args[0]), recv.getRuntime().getCurrentContext().getCurrentFrame().getBackRef());
        }

        IRubyObject result = recv.getRuntime().getCurrentContext().getCurrentFrame().getBackRef();

        if(result instanceof RubyMatchData) {
            ((RubyMatchData)result).use();
        }
        return result;
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
        RubyString str = m.str.makeShared(0,m.regs.beg[0]);
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
        RubyString str = m.str.makeShared(m.regs.end[0], m.str.getByteList().realSize-m.regs.end[0]);
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
        for(i=m.regs.numRegs-1; m.regs.beg[i] == -1 && i>0; i--);
        if(i == 0) {
            return nil;
        }
        return nth_match(i,match);
    }
}
