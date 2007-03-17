/*
 * Copyright (c) 2007 Ola Bini
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of 
 * this software and associated documentation files (the "Software"), to deal in 
 * the Software without restriction, including without limitation the rights to 
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies 
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 */
package org.rej;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 * @mri re_pattern_buffer
 */
public class Pattern {
    private final static int INIT_BUF_SIZE = 23;

    /** match will be done case insensetively */
    public final static int RE_OPTION_IGNORECASE  =1;
    /** perl-style extended pattern available */
    public final static int RE_OPTION_EXTENDED    =1<<1;
    /** newline will be included for . */
    public final static int RE_OPTION_MULTILINE   =1<<2;
    /** ^ and $ ignore newline */
    public final static int RE_OPTION_SINGLELINE  =1<<3;
    /** search for longest match, in accord with POSIX regexp */
    public final static int RE_OPTION_LONGEST     =1<<4;

    public final static int RE_MAY_IGNORECASE  = (RE_OPTION_LONGEST<<1);
    public final static int RE_OPTIMIZE_ANCHOR = (RE_MAY_IGNORECASE<<1);
    public final static int RE_OPTIMIZE_EXACTN = (RE_OPTIMIZE_ANCHOR<<1);
    public final static int RE_OPTIMIZE_NO_BM  = (RE_OPTIMIZE_EXACTN<<1);
    public final static int RE_OPTIMIZE_BMATCH = (RE_OPTIMIZE_NO_BM<<1);

    public final static int RE_DUP_MAX            =((1 << 15) - 1);

    public final static int MBCTYPE_ASCII=0;
    public final static int MBCTYPE_EUC=1;
    public final static int MBCTYPE_SJIS=2;
    public final static int MBCTYPE_UTF8=3;
    
    private static final char Sword = 1;
    private static final char Sword2 = 2;

    private static final char[] re_syntax_table = new char[256];

    static {
        char c;
        for(c=0; c<=0x7f; c++) {
            if(Character.isLetterOrDigit(c)) {
                re_syntax_table[c] = Sword;
            }
        }
        re_syntax_table['_'] = Sword;
        for(c=0x80; c<=0xff; c++) {
            if(Character.isLetterOrDigit(c)) {
                re_syntax_table[c] = Sword2;
            }
        }
    }

    /* These are the command codes that appear in compiled regular
       expressions, one per byte.  Some command codes are followed by
       argument bytes.  A command code can specify any interpretation
       whatsoever for its arguments.  Zero-bytes may appear in the compiled
       regular expression.*/
    private final static char unused = 0;
    private final static char exactn = 1; /* Followed by one byte giving n, then by n literal bytes.  */
    private final static char begline = 2;  /* Fail unless at beginning of line.  */
    private final static char endline = 3;  /* Fail unless at end of line.  */
    private final static char begbuf = 4;   /* Succeeds if at beginning of buffer (if emacs) or at beginning
                                               of string to be matched (if not).  */
    private final static char endbuf = 5;   /* Analogously, for end of buffer/string.  */
    private final static char endbuf2 = 6;  /* End of buffer/string, or newline just before it.  */
    private final static char begpos = 7;   /* Matches where last scan//gsub left off.  */
    private final static char jump = 8;     /* Followed by two bytes giving relative address to jump to.  */
    private final static char jump_past_alt = 9;/* Same as jump, but marks the end of an alternative.  */
    private final static char on_failure_jump = 10;	 /* Followed by two bytes giving relative address of 
                                                        place to resume at in case of failure.  */
    private final static char finalize_jump = 11;	 /* Throw away latest failure point and then jump to 
                                                        address.  */
    private final static char maybe_finalize_jump = 12; /* Like jump but finalize if safe to do so.
                                                           This is used to jump back to the beginning
                                                           of a repeat.  If the command that follows
                                                           this jump is clearly incompatible with the
                                                           one at the beginning of the repeat, such that
                                                           we can be sure that there is no use backtracking
                                                           out of repetitions already completed,
                                                           then we finalize.  */
    private final static char dummy_failure_jump = 13;  /* Jump, and push a dummy failure point. This 
                                                           failure point will be thrown away if an attempt 
                                                           is made to use it for a failure. A + construct 
                                                           makes this before the first repeat.  Also
                                                           use it as an intermediary kind of jump when
                                                           compiling an or construct.  */
    private final static char push_dummy_failure = 14; /* Push a dummy failure point and continue.  Used at the end of
                                                          alternatives.  */
    private final static char succeed_n = 15;	 /* Used like on_failure_jump except has to succeed n times;
                                                    then gets turned into an on_failure_jump. The relative
                                                    address following it is useless until then.  The
                                                    address is followed by two bytes containing n.  */
    private final static char jump_n = 16;	 /* Similar to jump, but jump n times only; also the relative
                                                address following is in turn followed by yet two more bytes
                                                containing n.  */
    private final static char try_next = 17;    /* Jump to next pattern for the first time,
                                                   leaving this pattern on the failure stack. */
    private final static char finalize_push = 18;	/* Finalize stack and push the beginning of the pattern
                                                       on the stack to retry (used for non-greedy match) */
    private final static char finalize_push_n = 19;	/* Similar to finalize_push, buf finalize n time only */
    private final static char set_number_at = 20;	/* Set the following relative location to the
                                                       subsequent number.  */
    private final static char anychar = 21;	 /* Matches any (more or less) one character excluding newlines.  */
    private final static char anychar_repeat = 22;	 /* Matches sequence of characters excluding newlines.  */
    private final static char charset = 23;     /* Matches any one char belonging to specified set.
                                                   First following byte is number of bitmap bytes.
                                                   Then come bytes for a bitmap saying which chars are in.
                                                   Bits in each byte are ordered low-bit-first.
                                                   A character is in the set if its bit is 1.
                                                   A character too large to have a bit in the map
                                                   is automatically not in the set.  */
    private final static char charset_not = 24; /* Same parameters as charset, but match any character
                                                   that is not one of those specified.  */
    private final static char start_memory = 25; /* Start remembering the text that is matched, for
                                                    storing in a memory register.  Followed by one
                                                    byte containing the register number.  Register numbers
                                                    must be in the range 0 through RE_NREGS.  */
    private final static char stop_memory = 26; /* Stop remembering the text that is matched
                                                   and store it in a memory register.  Followed by
                                                   one byte containing the register number. Register
                                                   numbers must be in the range 0 through RE_NREGS.  */
    private final static char start_paren = 27;    /* Place holder at the start of (?:..). */
    private final static char stop_paren = 28;    /* Place holder at the end of (?:..). */
    private final static char casefold_on = 29;   /* Turn on casefold flag. */
    private final static char casefold_off = 30;  /* Turn off casefold flag. */
    private final static char option_set = 31;	   /* Turn on multi line match (match with newlines). */
    private final static char start_nowidth = 32; /* Save string point to the stack. */
    private final static char stop_nowidth = 33;  /* Restore string place at the point start_nowidth. */
    private final static char pop_and_fail = 34;  /* Fail after popping nowidth entry from stack. */
    private final static char stop_backtrack = 35;  /* Restore backtrack stack at the point start_nowidth. */
    private final static char duplicate = 36;   /* Match a duplicate of something remembered.
                                                   Followed by one byte containing the index of the memory 
                                                   register.  */
    private final static char wordchar = 37;    /* Matches any word-constituent character.  */
    private final static char notwordchar = 38; /* Matches any char that is not a word-constituent.  */
    private final static char wordbeg = 39;	 /* Succeeds if at word beginning.  */
    private final static char wordend = 40;	 /* Succeeds if at word end.  */
    private final static char wordbound = 41;   /* Succeeds if at a word boundary.  */
    private final static char notwordbound = 42; /* Succeeds if not at a word boundary.  */

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length(),emptyPattern(0),ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length(),emptyPattern(0),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern, int flags) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length(),emptyPattern(flags),ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern, int flags, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length(),emptyPattern(flags),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern, Pattern bufp) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length(),bufp, ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern, Pattern bufp, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length(),bufp,ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern, int start, int length, Pattern bufp) throws PatternSyntaxException {
        return compile(pattern.toCharArray(),start,length,bufp,ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(String pattern, int start, int length, Pattern bufp, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern.toCharArray(),start,length,bufp,ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(0),ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(0),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern, int flags) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(flags),ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern, int flags, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,emptyPattern(flags),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern, Pattern bufp) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,bufp,ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern, Pattern bufp, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,0,pattern.length,bufp,ctx);
    }

    public final static char[] ASCII_TRANSLATE_TABLE = {
        '\000', '\001', '\002', '\003', '\004', '\005', '\006', '\007',
        '\010', '\011', '\012', '\013', '\014', '\015', '\016', '\017',
        '\020', '\021', '\022', '\023', '\024', '\025', '\026', '\027',
        '\030', '\031', '\032', '\033', '\034', '\035', '\036', '\037',
        /* ' '     '!'     '"'     '#'     '$'     '%'     '&'     ''' */
        '\040', '\041', '\042', '\043', '\044', '\045', '\046', '\047',
        /* '('     ')'     '*'     '+'     ','     '-'     '.'     '/' */
        '\050', '\051', '\052', '\053', '\054', '\055', '\056', '\057',
        /* '0'     '1'     '2'     '3'     '4'     '5'     '6'     '7' */
        '\060', '\061', '\062', '\063', '\064', '\065', '\066', '\067',
        /* '8'     '9'     ':'     ';'     '<'     '='     '>'     '?' */
        '\070', '\071', '\072', '\073', '\074', '\075', '\076', '\077',
        /* '@'     'A'     'B'     'C'     'D'     'E'     'F'     'G' */
        '\100', '\141', '\142', '\143', '\144', '\145', '\146', '\147',
        /* 'H'     'I'     'J'     'K'     'L'     'M'     'N'     'O' */
        '\150', '\151', '\152', '\153', '\154', '\155', '\156', '\157',
        /* 'P'     'Q'     'R'     'S'     'T'     'U'     'V'     'W' */
        '\160', '\161', '\162', '\163', '\164', '\165', '\166', '\167',
        /* 'X'     'Y'     'Z'     '['     '\'     ']'     '^'     '_' */
        '\170', '\171', '\172', '\133', '\134', '\135', '\136', '\137',
        /* '`'     'a'     'b'     'c'     'd'     'e'     'f'     'g' */
        '\140', '\141', '\142', '\143', '\144', '\145', '\146', '\147',
        /* 'h'     'i'     'j'     'k'     'l'     'm'     'n'     'o' */
        '\150', '\151', '\152', '\153', '\154', '\155', '\156', '\157',
        /* 'p'     'q'     'r'     's'     't'     'u'     'v'     'w' */
        '\160', '\161', '\162', '\163', '\164', '\165', '\166', '\167',
        /* 'x'     'y'     'z'     '{'     '|'     '}'     '~' */
        '\170', '\171', '\172', '\173', '\174', '\175', '\176', '\177',
        '\200', '\201', '\202', '\203', '\204', '\205', '\206', '\207',
        '\210', '\211', '\212', '\213', '\214', '\215', '\216', '\217',
        '\220', '\221', '\222', '\223', '\224', '\225', '\226', '\227',
        '\230', '\231', '\232', '\233', '\234', '\235', '\236', '\237',
        '\240', '\241', '\242', '\243', '\244', '\245', '\246', '\247',
        '\250', '\251', '\252', '\253', '\254', '\255', '\256', '\257',
        '\260', '\261', '\262', '\263', '\264', '\265', '\266', '\267',
        '\270', '\271', '\272', '\273', '\274', '\275', '\276', '\277',
        '\300', '\301', '\302', '\303', '\304', '\305', '\306', '\307',
        '\310', '\311', '\312', '\313', '\314', '\315', '\316', '\317',
        '\320', '\321', '\322', '\323', '\324', '\325', '\326', '\327',
        '\330', '\331', '\332', '\333', '\334', '\335', '\336', '\337',
        '\340', '\341', '\342', '\343', '\344', '\345', '\346', '\347',
        '\350', '\351', '\352', '\353', '\354', '\355', '\356', '\357',
        '\360', '\361', '\362', '\363', '\364', '\365', '\366', '\367',
        '\370', '\371', '\372', '\373', '\374', '\375', '\376', '\377',
};
    
    public final static class CompileContext {
        public final char[] translate;
        public final int current_mbctype;
        public final char[] re_mbctab;
        public CompileContext() {
            this(null,MBCTYPE_ASCII,mbctab_ascii);
        }
        public CompileContext(char[] t) {
            this(t,MBCTYPE_ASCII,mbctab_ascii);
        }
        public CompileContext(char[] t, int mbc, char[] mbctab) {
            this.translate = t;
            this.current_mbctype = mbc;
            this.re_mbctab = mbctab;
        }
    }

    private CompileContext ctx;

    private static class CompilationEnvironment {
        public CompileContext ctx;
        public char[] b;
        public int bix;
        public char[] p;
        public int pix;
        public int pend;
        public char c, c1;
        public int p0;
        public long optz;
        public int[] numlen = new int[1];
        public int nextp;

        /* Address of the count-byte of the most recently inserted `exactn'
           command.  This makes it possible to tell whether a new exact-match
           character can be added to that command or requires a new `exactn'
           command.  */

        public int pending_exact = -1;

        /* Address of the place where a forward-jump should go to the end of
           the containing expression.  Each alternative of an `or', except the
           last, ends with a forward-jump of this sort.  */

        public int fixup_alt_jump = -1;
        
        /* Address of start of the most recently finished expression.
           This tells postfix * where to find the start of its operand.  */

        public int laststart = -1;

        /* In processing a repeat, 1 means zero matches is allowed.  */

        public boolean zero_times_ok = false;

        /* In processing a repeat, 1 means many matches is allowed.  */

        public boolean many_times_ok = false;

        public boolean greedy = false;

        /* Address of beginning of regexp, or inside of last (.  */

        public int begalt = 0;

        /* Place in the uncompiled pattern (i.e., the {) to
           which to go back if the interval is invalid.  */
        public int beg_interval;

        /* In processing an interval, at least this many matches must be made.  */
        public int lower_bound;

        /* In processing an interval, at most this many matches can be made.  */
        public int upper_bound;

        /* Stack of information saved by ( and restored by ).
           Five stack elements are pushed by each (:
           First, the value of b.
           Second, the value of fixup_alt_jump.
           Third, the value of begalt.
           Fourth, the value of regnum.
           Fifth, the type of the paren. */

        public int[] stacka = new int[40];
        public int[] stackb = stacka;
        public int stackp = 0;
        public int stacke = 40;

        /* Counts ('s as they are encountered.  Remembered for the matching ),
           where it becomes the register number to put in the stop_memory
           command.  */

        public int regnum = 1;

        public int range = 0;
        public int had_mbchar = 0;
        public int had_num_literal = 0;
        public int had_char_class = 0;

        public boolean gotoRepeat=false;
        public boolean gotoNormalChar=false;
        public boolean gotoNumericChar=false;

        public Pattern self;

        public final void PATFETCH() {
            if(pix == pend) {
                err("premature end of regular expression");
            }
            c = p[pix++];
            if(TRANSLATE_P()) {
                c = ctx.translate[c];
            }
        }

        public final boolean TRANSLATE_P() {
            return ((optz&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
        }

        public final boolean MAY_TRANSLATE() {
            return ((self.options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0 && ctx.translate!=null);
        }

        public final void BUFPUSH(char ch) {
            GET_BUFFER_SPACE(1);
            b[bix++] = ch;
        }

        public final void PATFETCH_RAW() {
            if(pix == pend) {
                err("premature end of regular expression");
            }
            c = p[pix++];
        }

        public final void PATFETCH_RAW_c1() {
            if(pix == pend) {
                err("premature end of regular expression");
            }
            c1 = p[pix++];
        }

        public final void dollar() {
            if((optz & RE_OPTION_SINGLELINE) != 0) {
                BUFPUSH(endbuf);
            } else {
                p0 = pix;
                /* When testing what follows the $,
                   look past the \-constructs that don't consume anything.  */
                while(p0 != pend) {
                    if(p[p0] == '\\' && p0 + 1 != pend && (p[p0+1] == 'b' || p[p0+1] == 'B')) {
                        p0 += 2;
                    } else {
                        break;
                    }
                }
                BUFPUSH(endline);
            }
        }

        public final void caret() {
            if((optz & RE_OPTION_SINGLELINE) != 0) {
                BUFPUSH(begbuf);
            } else {
                BUFPUSH(begline);
            }
        }

        public final void prepareRepeat() {
            if(!gotoRepeat) {
                /* If there is no previous pattern, char not special. */
                if(laststart==-1) {
                    err("invalid regular expression; there's no previous pattern, to which '"+
                        (char)c
                        +"' would define cardinality at " + pix);
                }
                /* If there is a sequence of repetition chars,
                   collapse it down to just one.  */
                zero_times_ok = c != '+';
                many_times_ok = c != '?';
                greedy = true;
                
                if(pix != pend) {
                    PATFETCH();
                    switch (c) {
                    case '?':
                        greedy = false;
                        break;
                    case '*':
                    case '+':
                        err("nested *?+ in regexp");
                    default:
                        pix--;
                        break;
                    }
                }
            } else {
                gotoRepeat = false;
            }
        }

        public final void mainRepeat() {
            /* Now we know whether or not zero matches is allowed
               and also whether or not two or more matches is allowed.  */
            if(many_times_ok) {
                /* If more than one repetition is allowed, put in at the
                   end a backward relative jump from b to before the next
                   jump we're going to put in below (which jumps from
                   laststart to after this jump).  */
                GET_BUFFER_SPACE(3);
                store_jump(b,bix,greedy?maybe_finalize_jump:finalize_push,laststart-3);
                bix += 3;  	/* Because store_jump put stuff here.  */
            }

            /* On failure, jump from laststart to next pattern, which will be the
               end of the buffer after this jump is inserted.  */
            GET_BUFFER_SPACE(3);
            insert_jump(on_failure_jump, b, laststart, bix + 3, bix);
            bix += 3;

            if(zero_times_ok) {
                if(!greedy) {
                    GET_BUFFER_SPACE(3);
                    insert_jump(try_next, b, laststart, bix + 3, bix);
                    bix += 3;
                }
            } else {
                /* At least one repetition is required, so insert a
                   `dummy_failure_jump' before the initial
                   `on_failure_jump' instruction of the loop. This
                   effects a skip over that instruction the first time
                   we hit that loop.  */
                GET_BUFFER_SPACE(3);
                insert_jump(dummy_failure_jump, b, laststart, laststart + 6, bix);
                bix += 3;
            }
        }

        public final void dot() { 
            laststart = bix;
            BUFPUSH(anychar);
        }

        public final void prepareCharset() {
            if(pix == pend) {
                err("invalid regular expression; '[' can't be the last character ie. can't start range at the end of pattern");
            }

            while((bix + 9 + 32) > self.allocated) {
                EXTEND_BUFFER();
            }

            laststart = bix;
            if(p[pix] == '^') {
                BUFPUSH(charset_not);
                pix++;
            } else {
                BUFPUSH(charset);
            }
            p0 = pix;
            
            BUFPUSH((char)32);
            Arrays.fill(b,bix,bix + 32 + 2,(char)0);
                    
            had_mbchar = 0;
            had_num_literal = 0;
            had_char_class = 0;
        }

        public int last = -1;

        public final void charset_w() {
            for(c = 0; c < 256; c++) {
                if(re_syntax_table[c] == Sword || (ctx.current_mbctype==0 && re_syntax_table[c] == Sword2)) {
                    SET_LIST_BIT(c);
                }
            }
            if(ctx.current_mbctype != 0) {
                set_list_bits(0x80, 0xffffffff, b, bix);
            }
            had_char_class = 1;
            last = -1;
        }

        public final void charset_W() {
            for(c = 0; c < 256; c++) {
                if(re_syntax_table[c] != Sword &&
                   ((ctx.current_mbctype>0 && ctx.re_mbctab[c] == 0) ||
                    (ctx.current_mbctype==0 && re_syntax_table[c] != Sword2))) {
                    SET_LIST_BIT(c);
                }
            }
            had_char_class = 1;
            last = -1;
        }

        public final void charset_s() {
            for(c = 0; c < 256; c++) {
                if(Character.isWhitespace(c)) {
                    SET_LIST_BIT(c);
                }
            }
            had_char_class = 1;
            last = -1;
        }

        public final void charset_S() {
            for(c = 0; c < 256; c++) {
                if(!Character.isWhitespace(c)) {
                    SET_LIST_BIT(c);
                }
            }
            if(ctx.current_mbctype>0) {
                set_list_bits(0x80, 0xffffffff, b, bix);
            }
            had_char_class = 1;
            last = -1;
        }

        public final void charset_d() {
            for(c = '0'; c <= '9'; c++) {
                SET_LIST_BIT(c);
            }
            had_char_class = 1;
            last = -1;
        }

        public final void charset_D() {
            for(c = 0; c < 256; c++) {
                if(!Character.isDigit(c)) {
                    SET_LIST_BIT(c);
                }
            }
            if(ctx.current_mbctype>0) {
                set_list_bits(0x80, 0xffffffff, b, bix);
            }
            had_char_class = 1;
            last = -1;
        }

        public final void charset_x() {
            c = (char)scan_hex(p, pix, 2, numlen);
            if(numlen[0] == 0) {
                err("Invalid escape character syntax");
            }
            pix += numlen[0];
            had_num_literal = 1;
        }

        public final void charset_digit() {
            pix--;
            c = (char)scan_oct(p, pix, 3, numlen);
            pix += numlen[0];
            had_num_literal = 1;
        }

        public final void charset_special() {
            --pix;
            c = (char)read_special(p, pix, pend, numlen);
            if(c > 255) {
                err("Invalid escape character syntax");
            }
            pix = numlen[0];
            had_num_literal = 1;
        }

        public final void charset_default() {
            c = read_backslash(c);
            if(ismbchar(c,ctx)) {
                if(pix + mbclen(c,ctx) - 1 >= pend) {
                    err("premature end of regular expression");
                }
                c = self.MBC2WC(c, p, pix);
                pix += mbclen(c,ctx) - 1;
                had_mbchar++;
            }
        }

        public final void charset_posixclass(String _str) {
            int ch;
            boolean is_alnum = _str.equals("alnum");
            boolean is_alpha = _str.equals("alpha");
            boolean is_blank = _str.equals("blank");
            boolean is_cntrl = _str.equals("cntrl");
            boolean is_digit = _str.equals("digit");
            boolean is_graph = _str.equals("graph");
            boolean is_lower = _str.equals("lower");
            boolean is_print = _str.equals("print");
            boolean is_punct = _str.equals("punct");
            boolean is_space = _str.equals("space");
            boolean is_upper = _str.equals("upper");
            boolean is_xdigit= _str.equals("xdigit");

            if (!(is_alnum || is_alpha || is_blank || is_cntrl ||
                  is_digit || is_graph || is_lower || is_print ||
                  is_punct || is_space || is_upper || is_xdigit)){
                err("invalid regular expression; [:"+_str+":] is not a character class");
            }
                                
            /* Throw away the ] at the end of the character class.  */
                                    
            PATFETCH();

            if(pix == pend)  {
                err("invalid regular expression; range doesn't have ending ']' after a character class");
            }

            for(ch = 0; ch < 256; ch++) {
                if(      (is_alnum  && Character.isLetterOrDigit(ch))
                         || (is_alpha  && Character.isLetter(ch))
                         || (is_blank  && (ch == ' ' || ch == '\t'))
                         || (is_cntrl  && Character.isISOControl(ch))
                         || (is_digit  && Character.isDigit(ch))
                         || (is_graph  && (!Character.isWhitespace(ch) && !Character.isISOControl(ch)))
                         || (is_lower  && Character.isLowerCase(ch))
                         || (is_print  && (' ' == ch || (!Character.isWhitespace(ch) && !Character.isISOControl(ch))))
                         || (is_punct  && (!Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch) && !Character.isISOControl(ch)))
                         || (is_space  && Character.isWhitespace(ch))
                         || (is_upper  && Character.isUpperCase(ch))
                         || (is_xdigit && HEXDIGIT.indexOf(ch) != -1)) {
                    SET_LIST_BIT((char)ch);
                }
            }
            had_char_class = 1;
        }

        public final void charset_range() {
            if(last > c) {
                err("invalid regular expression");
            }
            range = 0;
            if(had_mbchar == 0) {
                if((optz & RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null) {
                    for (;last<=c;last++) {
                        char cx = ctx.translate[last];
                        SET_LIST_BIT(cx);
                    }
                } else {
                    for(;last<=c;last++) {
                        SET_LIST_BIT((char)last);
                    }
                }
            } else if (had_mbchar == 2) {
                set_list_bits(last, c, b, bix);
            } else {
                /* restriction: range between sbc and mbc */
                err("invalid regular expression");
            }
        }

        public final void charset_range_trans() {
            if(((optz & RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null) && c < 0x100) {
                c = ctx.translate[c];
            }
            if(had_mbchar == 0 && (ctx.current_mbctype == 0 || had_num_literal == 0)) {
                SET_LIST_BIT(c);
                had_num_literal = 0;
            } else {
                set_list_bits(c, c, b, bix);
            }
        }

        public final void main_charset() {
            boolean gotoRangeRepeat=false;
            int size;
            last = -1;

            /* Read in characters and ranges, setting map bits.  */
            charsetLoop: for (;;) {
                if(!gotoRangeRepeat) {
                    size = -1;
                    last = -1;
                    if((size = EXTRACT_UNSIGNED(b,bix+32))!=0 || ctx.current_mbctype!=0) {
                        /* Ensure the space is enough to hold another interval
                           of multi-byte chars in charset(_not)?.  */
                        size = 32 + 2 + size*8 + 8;
                        while(bix + size + 1 > self.allocated) {
                            EXTEND_BUFFER();
                        }
                    }
                } else {
                    gotoRangeRepeat = false;
                }

                if(range>0 && had_char_class>0) {
                    err("invalid regular expression; can't use character class as an end value of range");
                }
                PATFETCH_RAW();

                if(c == ']') {
                    if(pix == p0 + 1) {
                        if(pix == pend) {
                            err("invalid regular expression; empty character class");
                        }
                        self.re_warning("character class has `]' without escape");
                    } else {
                        /* Stop if this isn't merely a ] inside a bracket
                           expression, but rather the end of a bracket
                           expression.  */
                        break charsetLoop;
                    }
                }
                /* Look ahead to see if it's a range when the last thing
                   was a character class.  */
                if(had_char_class > 0 && c == '-' && p[pix] != ']') {
                    err("invalid regular expression; can't use character class as a start value of range");
                }
                if(ismbchar(c,ctx)) {
                    if(pix + mbclen(c,ctx) - 1 >= pend) {
                        err("premature end of regular expression");
                    }
                    c = self.MBC2WC(c, p, pix);
                    pix += mbclen(c,ctx) - 1;
                    had_mbchar++;
                }
                had_char_class = 0;

                if(c == '-' && ((pix != p0 + 1 && p[pix] != ']') ||
                                  (p[pix] == '-' && p[pix+1] != ']') ||
                                  range>0)) {
                    self.re_warning("character class has `-' without escape");
                }
                if(c == '[' && p[pix] != ':') {
                    self.re_warning("character class has `[' without escape");
                }


                /* \ escapes characters when inside [...].  */
                if(c == '\\') {
                    PATFETCH_RAW();
                    switch(c) {
                    case 'w':
                        charset_w();
                        continue charsetLoop;
                    case 'W':
                        charset_W();
                        continue charsetLoop;
                    case 's':
                        charset_s();
                        continue charsetLoop;
                    case 'S':
                        charset_S();
                        continue charsetLoop;
                    case 'd':
                        charset_d();
                        continue charsetLoop;
                    case 'D':
                        charset_D();
                        continue charsetLoop;
                    case 'x':
                        charset_x();
                        break;
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        charset_digit();
                        break;
                    case 'M':
                    case 'C':
                    case 'c':
                        charset_special();
                        break;
                    default:
                        charset_default();
                        break;
                    }
                } else if(c == '[' && p[pix] == ':') { /* [:...:] */
                    /* Leave room for the null.  */
                    char[] str = new char[7];
                    PATFETCH_RAW();
                    c1 = 0;

                    /* If pattern is `[[:'.  */
                    if(pix == pend) {
                        err("invalid regular expression; re can't end '[[:'");
                    }

                    for(;;) {
                        PATFETCH_RAW();
                        if(c == ':' || c == ']' || pix == pend || c1 == 6) {
                            break;
                        }
                        str[c1++] = c;
                    }
                    String _str = new String(str,0,c1);

                    /* If isn't a word bracketed by `[:' and `:]':
                       undo the ending character, the letters, and
                       the leading `:' and `['.  */
                                
                    if(c == ':' && p[pix] == ']') {
                        charset_posixclass(_str);
                        continue charsetLoop;
                    } else {
                        c1 += 2;
                        pix -= c1;
                        self.re_warning("character class has `[' without escape");
                        c = '[';
                    }
                }

                /* Get a range.  */
                if(range > 0) {
                    charset_range();
                } else if(p[pix] == '-' && p[pix+1] != ']') {
                    last = c;
                    PATFETCH_RAW_c1();
                    range = 1;
                    gotoRangeRepeat = true;
                    continue charsetLoop;
                } else {
                    charset_range_trans();
                }
                had_mbchar = 0;
            }
        }

        public final void compact_charset() {
            /* Discard any character set/class bitmap bytes that are all
               0 at the end of the map. Decrement the map-length byte too.  */
            while(b[bix-1] > 0 && b[bix+b[bix-1]-1] == 0) {
                b[bix-1]--; 
            }
            if(b[bix-1] != 32) {
                System.arraycopy(b,bix+32,b,bix+b[bix-1],2+EXTRACT_UNSIGNED(b,bix+32)*8);
            }
            bix += b[bix-1] + 2 + EXTRACT_UNSIGNED(b,bix+b[bix-1])*8;
            had_num_literal = 0;
        }

        public int old_options;
        public int push_option = 0;
        public int casefold = 0;

        public final void group_settings() {
            boolean negative = false;

            if(pix == pend) {
                err("premature end of regular expression");
            }
                        
            c = p[pix++];

            switch (c) {
            case 'x': case 'm': case 'i': case '-':
                for(;;) {
                    switch (c) {
                    case '-':
                        negative = true;
                        break;
                    case ':':
                    case ')':
                        break;
                    case 'x':
                        if(negative) {
                            optz &= ~RE_OPTION_EXTENDED;
                        } else {
                            optz |= RE_OPTION_EXTENDED;
                        }
                        break;
                    case 'm':
                        if(negative) {
                            if((optz&RE_OPTION_MULTILINE) != 0) {
                                optz &= ~RE_OPTION_MULTILINE;
                            }
                        }else if ((optz&RE_OPTION_MULTILINE) == 0) {
                            optz |= RE_OPTION_MULTILINE;
                        }
                        push_option = 1;
                        break;
                    case 'i':
                        if(negative) {
                            if((optz&RE_OPTION_IGNORECASE) != 0) {
                                optz &= ~RE_OPTION_IGNORECASE;
                            }
                        } else if ((optz&RE_OPTION_IGNORECASE) == 0) {
                            optz |= RE_OPTION_IGNORECASE;
                        }
                        casefold = 1;
                        break;
                    default:
                        err("undefined (?...) inline option");
                    }
                    if(c == ')') {
                        c = '#';	/* read whole in-line options */
                        break;
                    }
                    if(c == ':') {
                        break;
                    }
                                
                    if(pix == pend) {
                        err("premature end of regular expression");
                    }
                    c = p[pix++];
                }
                break;

            case '#':
                for(;;) {
                    if(pix == pend) {
                        err("premature end of regular expression");
                    }
                    c = p[pix++];
                    if((optz & RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null) {
                        c = ctx.translate[c];
                    }
                    if(c == ')') {
                        break;
                    }
                }
                c = '#';
                break;

            case ':':
            case '=':
            case '!':
            case '>':
                break;
            default:
                err("undefined (?...) sequence");
            }
        }

        public final void group_start() {
            old_options = (int)optz;
            push_option = 0;
            casefold = 0;
            PATFETCH();
            
            if(c == '?') {
                group_settings();
            } else {
                pix--;
                c = '(';
            }
        }

        public final void DOUBLE_STACK() {
            int[] stackx;
            int xlen = stacke;
            stackx = new int[2*xlen];
            System.arraycopy(stackb,0,stackx,0,xlen);
            stackb = stackx;
            stacke = 2*xlen;
        }

        public final void group_start_end() {
            if(stackp+8 >= stacke) {
                DOUBLE_STACK();
            }

            /* Laststart should point to the start_memory that we are about
               to push (unless the pattern has RE_NREGS or more ('s).  */
            /* obsolete: now RE_NREGS is just a default register size. */
            stackb[stackp++] = bix;    
            stackb[stackp++] = fixup_alt_jump != -1 ? fixup_alt_jump - 0 + 1 : 0;
            stackb[stackp++] = begalt;
            switch(c) {
            case '(':
                BUFPUSH(start_memory);
                BUFPUSH((char)regnum);
                stackb[stackp++] = regnum++;
                stackb[stackp++] = bix;
                BUFPUSH((char)0);
                /* too many ()'s to fit in a byte. (max 254) */
                if(regnum >= 255) {
                    err("regular expression too big");
                }
                break;
            case '=':
            case '!':
            case '>':
                BUFPUSH(start_nowidth);
                stackb[stackp++] = bix;
                BUFPUSH((char)0);
                BUFPUSH((char)0);
                if(c != '!') {
                    break;
                }
                BUFPUSH(on_failure_jump);
                stackb[stackp++] = bix;
                BUFPUSH((char)0);
                BUFPUSH((char)0);
                break;
            case ':':
                BUFPUSH(start_paren);
                pending_exact = -1;
            default:
                break;
            }
            if(push_option != 0) {
                BUFPUSH(option_set);
                BUFPUSH((char)optz);
            }
            if(casefold != 0) {
                if((optz & RE_OPTION_IGNORECASE)!=0) {
                    BUFPUSH(casefold_on);
                } else {
                    BUFPUSH(casefold_off);
                }
            }
            stackb[stackp++] = c;
            stackb[stackp++] = old_options;
            fixup_alt_jump = -1;
            laststart = -1;
            begalt = bix;
        }

        public final void group_end() {
            if(stackp == 0) { 
                err("unmatched )");
            }

            pending_exact = -1;
            if(fixup_alt_jump != -1) {
                /* Push a dummy failure point at the end of the
                   alternative for a possible future
                   `finalize_jump' to pop.  See comments at
                   `push_dummy_failure' in `re_match'.  */
                BUFPUSH(push_dummy_failure);
                        
                /* We allocated space for this jump when we assigned
                   to `fixup_alt_jump', in the `handle_alt' case below.  */
                store_jump(b, fixup_alt_jump, jump, bix);
            }
            if(optz != stackb[stackp-1]) {
                if (((optz ^ stackb[stackp-1]) & RE_OPTION_IGNORECASE) != 0) {
                    BUFPUSH((optz&RE_OPTION_IGNORECASE) != 0 ? casefold_off:casefold_on);
                }
                if ((optz ^ stackb[stackp-1]) != RE_OPTION_IGNORECASE) {
                    BUFPUSH(option_set);
                    BUFPUSH((char)stackb[stackp-1]);
                }
            }
            p0 = bix;
            optz = stackb[--stackp];
            switch(c = (char)stackb[--stackp]) {
            case '(': {
                int v1 = stackb[--stackp];
                self.buffer[v1] = (char)(regnum - stackb[stackp-1]);
                GET_BUFFER_SPACE(3);
                b[bix++] = stop_memory;
                b[bix++] = (char)stackb[stackp-1];
                b[bix++] = (char)(regnum - stackb[stackp-1]);
                stackp--;
            }
                break;

            case '!':
                BUFPUSH(pop_and_fail);
                /* back patch */

                STORE_NUMBER(self.buffer,stackb[stackp-1], bix - stackb[stackp-1] - 2);
                stackp--;
                /* fall through */
            case '=':
                BUFPUSH(stop_nowidth);
                /* tell stack-pos place to start_nowidth */
                STORE_NUMBER(self.buffer,stackb[stackp-1], bix - stackb[stackp-1] - 2);
                BUFPUSH((char)0); /* space to hold stack pos */
                BUFPUSH((char)0);
                stackp--;
                break;
            case '>':
                BUFPUSH(stop_backtrack);
                /* tell stack-pos place to start_nowidth */
                STORE_NUMBER(self.buffer,stackb[stackp-1], bix - stackb[stackp-1] - 2);
                BUFPUSH((char)0); /* space to hold stack pos */
                BUFPUSH((char)0);
                stackp--;
                break;
            case ':':
                BUFPUSH(stop_paren);
                break;
            default:
                break;
            }
            begalt = stackb[--stackp];
            stackp--;
            fixup_alt_jump = stackb[stackp] != 0 ? stackb[stackp]  - 1 : -1;
            laststart = stackb[--stackp];
            if(c == '!' || c == '=') {
                laststart = bix;
            }
        }

        public final void alt() {
            /* Insert before the previous alternative a jump which
               jumps to this alternative if the former fails.  */
            GET_BUFFER_SPACE(3);
            insert_jump(on_failure_jump, b, begalt, bix + 6, bix);
            pending_exact = -1;
            bix += 3;
            /* The alternative before this one has a jump after it
               which gets executed if it gets matched.  Adjust that
               jump so it will jump to this alternative's analogous
               jump (put in below, which in turn will jump to the next
               (if any) alternative's such jump, etc.).  The last such
               jump jumps to the correct final destination.  A picture:
               _____ _____ 
               |   | |   |   
               |   v |   v 
               a | b   | c   
                       
               If we are at `b', then fixup_alt_jump right now points to a
               three-byte space after `a'.  We'll put in the jump, set
               fixup_alt_jump to right after `b', and leave behind three
               bytes which we'll fill in when we get to after `c'.  */

            if(fixup_alt_jump != -1) {
                store_jump(b, fixup_alt_jump, jump_past_alt, bix);
            }

            /* Mark and leave space for a jump after this alternative,
               to be filled in later either by next alternative or
               when know we're at the end of a series of alternatives.  */
            fixup_alt_jump = bix;
            GET_BUFFER_SPACE(3);
            bix += 3;
            laststart = -1;
            begalt = bix;
        }

        public final void start_bounded_repeat() {
            /* If there is no previous pattern, this is an invalid pattern.  */
            if(laststart == -1) {
                err("invalid regular expression; there's no previous pattern, to which '{' would define cardinality at " + pix);
            }
            if(pix == pend) {
                err("invalid regular expression; '{' can't be last character");
            }

            beg_interval = pix - 1;

            lower_bound = -1;			/* So can see if are set.  */
            upper_bound = -1;

            if(pix != pend) {
                PATFETCH();
                while(Character.isDigit(c)) {
                    if(lower_bound < 0) {
                        lower_bound = 0;
                    }
                    lower_bound = lower_bound * 10 + c - '0';
                    if(pix == pend) {
                        break;
                    }
                    PATFETCH();
                }
            } 	

            if(c == ',') {
                if(pix != pend) {
                    PATFETCH();
                    while(Character.isDigit(c)) {
                        if(upper_bound < 0) {
                            upper_bound = 0;
                        }
                        upper_bound = lower_bound * 10 + c - '0';
                        if(pix == pend) {
                            break;
                        }
                        PATFETCH();
                    }
                } 	
            } else {
                /* Interval such as `{1}' => match exactly once. */
                upper_bound = lower_bound;
            }
        }

        public final int continue_bounded_repeat() {
            if(lower_bound >= RE_DUP_MAX || upper_bound >= RE_DUP_MAX) {
                err("too big quantifier in {,}");
            }
            if(upper_bound < 0) {
                upper_bound = RE_DUP_MAX;
            }
            if(lower_bound > upper_bound) {
                err("can't do {n,m} with n > m");
            }

            beg_interval = 0;
            pending_exact = 0;
                    
            greedy = true;

            if(pix != pend) {
                PATFETCH();
                if(c == '?') {
                    greedy = false;
                } else {
                    pix--;
                }
            }

            if(lower_bound == 0) {
                zero_times_ok = true;
                if(upper_bound == RE_DUP_MAX) {
                    many_times_ok = true;
                    gotoRepeat = true;
                    c = '*';
                    return 1;
                }
                if(upper_bound == 1) {
                    many_times_ok = false;
                    gotoRepeat = true;
                    c = '*';
                    return 1;
                }
            }
            if(lower_bound == 1) {
                if(upper_bound == 1) {
                    /* No need to repeat */
                    return 2;
                }
                if(upper_bound == RE_DUP_MAX) {
                    many_times_ok = true;
                    zero_times_ok = false;
                    gotoRepeat = true;
                    c = '*';
                    return 1;
                }
            }

            /* If upper_bound is zero, don't want to succeed at all; 
               jump from laststart to b + 3, which will be the end of
               the buffer after this jump is inserted.  */

            if(upper_bound == 0) {
                GET_BUFFER_SPACE(3);
                insert_jump(jump, b, laststart, bix + 3, bix);
                bix += 3;
                return 2;
            }

            /* If lower_bound == upper_bound, repeat count can be removed */
            if(lower_bound == upper_bound) {
                int mcnt;
                int skip_stop_paren = 0;

                if(b[bix-1] == stop_paren) {
                    skip_stop_paren = 1;
                    bix--;
                }

                if (b[laststart] == exactn && b[laststart+1]+2 == bix - laststart && b[laststart+1]*lower_bound < 256) {
                    mcnt = b[laststart+1];
                    GET_BUFFER_SPACE((lower_bound-1)*mcnt);
                    b[laststart+1] = (char)(lower_bound*mcnt);
                    while(--lower_bound > 0) {
                        System.arraycopy(b,laststart+2,b,bix,mcnt);
                        bix+=mcnt;
                    }
                    if(skip_stop_paren != 0) {
                        BUFPUSH(stop_paren);
                    }
                    return 2;
                }

                if(lower_bound < 5 && bix - laststart < 10) {
                    /* 5 and 10 are the magic numbers */
                    mcnt = bix - laststart;
                    GET_BUFFER_SPACE((lower_bound-1)*mcnt);
                    while(--lower_bound > 0) {
                        System.arraycopy(b, laststart, b, bix, mcnt);
                        bix+=mcnt;
                    }
                    if(skip_stop_paren!=0) {
                        BUFPUSH(stop_paren);
                    }
                    return 2;
                }
                if(skip_stop_paren!=0) {
                    bix++; /* push back stop_paren */
                }
            }
            return 0;
        }

        public final void bounded_nontrivial() {
            /* Otherwise, we have a nontrivial interval.  When
               we're all done, the pattern will look like:
               set_number_at <jump count> <upper bound>
               set_number_at <succeed_n count> <lower bound>
               succeed_n <after jump addr> <succed_n count>
               <body of loop>
               jump_n <succeed_n addr> <jump count>
               (The upper bound and `jump_n' are omitted if
               `upper_bound' is 1, though.)  */
            /* If the upper bound is > 1, we need to insert
               more at the end of the loop.  */
            int nbytes = upper_bound == 1 ? 10 : 20;
            GET_BUFFER_SPACE(nbytes);

            /* Initialize lower bound of the `succeed_n', even
               though it will be set during matching by its
               attendant `set_number_at' (inserted next),
               because `re_compile_fastmap' needs to know.
               Jump to the `jump_n' we might insert below.  */
            insert_jump_n(succeed_n, b, laststart, bix + (nbytes/2), bix, lower_bound);
            bix += 5; 	/* Just increment for the succeed_n here.  */
                        
            /* Code to initialize the lower bound.  Insert 
               before the `succeed_n'.  The `5' is the last two
               bytes of this `set_number_at', plus 3 bytes of
               the following `succeed_n'.  */
            insert_op_2(set_number_at, b, laststart, bix, 5, lower_bound);
            bix += 5;

            if(upper_bound > 1) {
                /* More than one repetition is allowed, so
                   append a backward jump to the `succeed_n'
                   that starts this interval.
                               
                   When we've reached this during matching,
                   we'll have matched the interval once, so
                   jump back only `upper_bound - 1' times.  */
                GET_BUFFER_SPACE(5);
                store_jump_n(b, bix, greedy?jump_n:finalize_push_n, laststart + 5, upper_bound - 1);
                bix += 5;

                /* The location we want to set is the second
                   parameter of the `jump_n'; that is `b-2' as
                   an absolute address.  `laststart' will be
                   the `set_number_at' we're about to insert;
                   `laststart+3' the number to set, the source
                   for the relative address.  But we are
                   inserting into the middle of the pattern --
                   so everything is getting moved up by 5.
                   Conclusion: (b - 2) - (laststart + 3) + 5,
                   i.e., b - laststart.

                   We insert this at the beginning of the loop
                   so that if we fail during matching, we'll
                   reinitialize the bounds.  */
                insert_op_2(set_number_at, b, laststart, bix, bix - laststart, upper_bound - 1);
                bix += 5;
            }
        }
        
        public final void unfetch_interval() {
            // unfetch_interval:
            /* If an invalid interval, match the characters as literals.  */
            self.re_warning("regexp has invalid interval");
            pix = beg_interval;
            beg_interval = 0;
            /* normal_char and normal_backslash need `c'.  */
            PATFETCH();
            gotoNormalChar = true;
        }

        public final void escape() {
            if(pix == pend) {
                err("invalid regular expression; '\\' can't be last character");
            }
            /* Do not translate the character after the \, so that we can
               distinguish, e.g., \B from \b, even if we normally would
               translate, e.g., B to b.  */
            c = p[pix++];
            switch (c) {
            case 's':
            case 'S':
            case 'd':
            case 'D':
                while(bix + 9 + 32 > self.allocated) {
                    EXTEND_BUFFER();
                }

                laststart = bix;
                if(c == 's' || c == 'd') {
                    b[bix++] = charset;
                } else {
                    b[bix++] = charset_not;
                }
                b[bix++] = 32;
                Arrays.fill(b,bix,bix+34,(char)0);
                if(c == 's' || c == 'S') {
                    SET_LIST_BIT(' ');
                    SET_LIST_BIT('\t');
                    SET_LIST_BIT('\n');
                    SET_LIST_BIT('\r');
                    SET_LIST_BIT('\f');
                } else {
                    char cc;
                    for (cc = '0'; cc <= '9'; cc++) {
                        SET_LIST_BIT(cc);
                    }
                }

                while(b[bix-1] > 0 && b[bix+b[bix-1]-1] == 0) { 
                    b[bix-1]--;
                }
                if(b[bix-1] != 32) {
                    System.arraycopy(b,bix+32,b,bix+b[bix-1],  2 + EXTRACT_UNSIGNED(b,bix+32)*8);
                }
                bix += b[bix-1] + 2 + EXTRACT_UNSIGNED(b, bix+b[bix-1])*8;
                break;
            case 'w':
                laststart = bix;
                BUFPUSH(wordchar);
                break;
            case 'W':
                laststart = bix;
                BUFPUSH(notwordchar);
                break;

                /******************* NOT IN RUBY
                    case '<':
                        SH(wordbeg);
                        break;

                    case '>':
                        SH(wordend);
                        break;
                ********************/

            case 'b':
                BUFPUSH(wordbound);
                break;

            case 'B':
                BUFPUSH(notwordbound);
                break;

            case 'A':
                BUFPUSH(begbuf);
                break;

            case 'Z':
                if((self.options & RE_OPTION_SINGLELINE) == 0) {
                    BUFPUSH(endbuf2);
                    break;
                }
                /* fall through */
            case 'z':
                BUFPUSH(endbuf);
                break;

            case 'G':
                BUFPUSH(begpos);
                break;

                /* hex */
            case 'x':
                had_mbchar = 0;
                c = (char)scan_hex(p, pix, 2, numlen);
                if(numlen[0] == 0) {
                    err("Invalid escape character syntax");
                }
                pix += numlen[0];
                had_num_literal = 1;
                gotoNumericChar = true;
                return;

                /* octal */
            case '0':
                had_mbchar = 0;
                c = (char)scan_oct(p, pix, 2, numlen);
                pix += numlen[0];
                had_num_literal = 1;
                gotoNumericChar = true;
                return;

                /* back-ref or octal */
            case '1': case '2': case '3':
            case '4': case '5': case '6':
            case '7': case '8': case '9':
                pix--;
                p0 = pix;
                had_mbchar = 0;
                c1 = 0;

                if(pix != pend) {
                    PATFETCH();
                    while(Character.isDigit(c)) {
                        if(c1 < 0) {
                            c1 = 0;
                        }
                        c1 = (char)(c1 * 10 + c - '0');
                        if(pix == pend) {
                            break;
                        }
                        PATFETCH();
                    }
                } 	

                if(!Character.isDigit(c)) {
                    pix--;
                }
                        
                if(9 < c1 && c1 >= regnum) {
                    /* need to get octal */
                    c = (char)(scan_oct(p, p0, 3, numlen) & 0xff);
                    pix = p0 + numlen[0];
                    c1 = 0;
                    had_num_literal = 1;
                    gotoNumericChar = true;
                    return;
                }

                laststart = bix;
                BUFPUSH(duplicate);
                BUFPUSH((char)c1);
                break;

            case 'M':
            case 'C':
            case 'c':
                p0 = --pix;
                int[] p00 = new int[]{p0};
                c = (char)read_special(p, pix, pend, p00);
                p0 = p00[0];
                if(c > 255) {
                    err("Invalid escape character syntax");
                }
                pix = p0;
                had_num_literal = 1;
                gotoNumericChar = true;
                return;
            default:
                c = read_backslash(c);
                gotoNormalChar = true;
            }
        }

        public final void handle_num_or_normal() {
            if(gotoNormalChar) {
                /* Expects the character in `c'.  */
                had_mbchar = 0;
                if(ismbchar(c,ctx)) {
                    had_mbchar = 1;
                    c1 = (char)pix;
                }
            }
            if(gotoNormalChar || gotoNumericChar) {
                nextp = pix + mbclen(c,ctx) - 1;

                if(pending_exact==-1 || pending_exact + b[pending_exact] + 1 != bix || 
                   b[pending_exact] >= (c1!=0 ? 0176 : 0177) || (nextp < pend &&
                                                              (p[nextp] == '+' || p[nextp] == '?'
                                                               || p[nextp] == '*' || p[nextp] == '^'
                                                               || p[nextp] == '{'))) {
                    laststart = bix;
                    BUFPUSH(exactn);
                    pending_exact = bix;
                    BUFPUSH((char)0);
                }
                if(had_num_literal!=0 || c == 0xff) {
                    BUFPUSH((char)0xFF);
                    b[pending_exact]++;
                    had_num_literal = 0;
                }
                BUFPUSH(c);
                b[pending_exact]++;
                if(had_mbchar!=0) {
                    int len = mbclen(c,ctx) - 1;
                    while(len-- > 0) {
                        if(pix == pend) {
                            err("premature end of regular expression");
                        }
                        c = p[pix++];
                        BUFPUSH(c);
                        b[pending_exact]++;
                    }
                }

                gotoNormalChar = false;
                gotoNumericChar = false;
            }
        }

        public final void finalize_compilation() {
            if(fixup_alt_jump!=-1) {
                store_jump(b, fixup_alt_jump, jump, bix);
            }
            if(stackp > 0) {
                err("unmatched (");
            }

            /* set optimize flags */
            laststart = 0;
            if(laststart != bix) {
                if(b[laststart] == dummy_failure_jump) {
                    laststart += 3;
                } else if(b[laststart] == try_next) {
                    laststart += 3;
                }
                if(b[laststart] == anychar_repeat) {
                    self.options |= RE_OPTIMIZE_ANCHOR;
                }
            }
            self.used = bix;
            self.re_nsub = regnum;
            laststart = 0;
            if(laststart != bix) {
                if(b[laststart] == start_memory) {
                    laststart += 3;
                }
                if(b[laststart] == exactn) {
                    self.options |= RE_OPTIMIZE_EXACTN;
                    self.must = laststart+1;
                }
            }
            if(self.must==-1) {
                self.must = calculate_must_string(self.buffer, bix);
            }

            if(ctx.current_mbctype == MBCTYPE_SJIS) {
                self.options |= RE_OPTIMIZE_NO_BM;
            } else if(self.must != -1) {
                int i;
                int len = self.buffer[self.must];

                for(i=1; i<len; i++) {
                    if(self.buffer[self.must+i] == 0xff ||
                       (ctx.current_mbctype!=0 && ismbchar(self.buffer[self.must+i],ctx))) {
                        self.options |= RE_OPTIMIZE_NO_BM;
                        break;
                    }
                }
                if((self.options & RE_OPTIMIZE_NO_BM) == 0) {
                    self.must_skip = new int[256];
                    bm_init_skip(self.must_skip, self.buffer, self.must+1,
                                 self.buffer[self.must],
                                 (((self.options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0) && ctx.translate!=null)?ctx.translate:null);
                }
            }

            self.regstart = new int[regnum];
            self.regend = new int[regnum];
            self.old_regstart = new int[regnum];
            self.old_regend = new int[regnum];
            self.reg_info = new RegisterInfoType[regnum];
            for(int x=0;x<self.reg_info.length;x++) {
                self.reg_info[x] = new RegisterInfoType();
            }
            self.best_regstart = new int[regnum];
            self.best_regend = new int[regnum];
            /*
              System.err.println("compiled into pattern of length: " + self.used);
              for(int i=0;i<self.used;i++) {
              System.err.print(" "+(int)self.buffer[i]);
              }
              System.err.println();
            */
        }


        private final void SET_LIST_BIT(char c) {
            b[bix+c/8] |= 1 << (c%8);
        }

        private final void GET_BUFFER_SPACE(int n) {
            while(bix+n >= self.allocated) {
                EXTEND_BUFFER();
            }
        }

        private final void EXTEND_BUFFER() {
            char[] old_buffer = self.buffer;
            self.allocated *= 2;
            self.buffer = new char[self.allocated];
            b = self.buffer;
            System.arraycopy(old_buffer,0,self.buffer,0,old_buffer.length);
        }

    }

    private final static char read_backslash(char c) {
        switch(c) {
        case 'n':
            return '\n';
        case 't':
            return '\t';
        case 'r':
            return '\r';
        case 'f':
            return '\f';
        case 'v':
            return 11;
        case 'a':
            return '\007';
        case 'b':
            return '\010';
        case 'e':
            return '\033';
        }
        return c;
    }

    private final static int read_special(char[] p, int pix, int pend, int[] pp) {
        int c;
        if(pix == pend) {
            pp[0] = pix;
            return ~0;
        }
        c = p[pix++];
        switch(c) {
        case 'M':
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++];
            if(c != '-') {
                return -1;
            }
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++];
            pp[0] = pix;
            if(c == '\\') {
                return read_special(p, --pix, pend, pp) | 0x80;
            } else if(c == -1) { 
                return ~0;
            } else {
                return ((c & 0xff) | 0x80);
            }
        case 'C':
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++];
            if(c != '-') {
                return -1;
            }
        case 'c':
            if(pix == pend) {
                return ~0;
            }
            c = p[pix++];
            pp[0] = pix;
            if(c == '\\') {
                c = read_special(p, --pix, pend, pp);
            } else if(c == '?') {
                return 0177;
            } else if(c == -1) {
                return ~0;
            }
            return c & 0x9f;
        default:
            pp[0] = pix+1;
            return read_backslash((char)c);
        }
    }

    private final static String HEXDIGIT = "0123456789abcdef0123456789ABCDEF";
    private final static long scan_hex(char[] p, int start, int len, int[] retlen) {
        int s = start;
        long retval = 0;
        int tmp;
        while(len-- > 0 && s<p.length && (tmp = HEXDIGIT.indexOf(p[s])) != -1) {
            retval <<= 4;
            retval |= (tmp & 15);
            s++;
        }
        retlen[0] = s-start;
        return retval;
    }

    private final static long scan_oct(char[] p, int start, int len, int[] retlen) {
        int s = start;
        long retval = 0;

        while(len-- > 0 && s<p.length && p[s] >= '0' && p[s] <= '7') {
            retval <<= 3;
            retval |= (p[s++] - '0');
        }
        retlen[0] = s-start;
        return retval;

    }


    private final int WC2MBC1ST(long c) {
        if(ctx.current_mbctype != MBCTYPE_UTF8) {
            return (int)((c<0x100) ? c : ((c>>8)&0xff));
        } else {
            return (int)utf8_firstbyte(c);
        }
    }

    private void compile(char[] pattern, int start, int length, CompileContext ctx) throws PatternSyntaxException {
        this.ctx = ctx;
        CompilationEnvironment w = new CompilationEnvironment();
        w.ctx = ctx;
        w.b = buffer;
        w.bix = 0;
        w.p = pattern;
        w.pix = start;
        w.pend = start+length;
        w.c1 = 0;
        w.optz = options;
        w.self = this;

        fastmap_accurate = 0;
        must = -1;
        must_skip = null;

        if(allocated == 0) {
            allocated = INIT_BUF_SIZE;
            /* EXTEND_BUFFER loses when allocated is 0.  */
            buffer = new char[INIT_BUF_SIZE];
            w.b = buffer;
        }

        mainParse: while(w.pix != w.pend) {
            w.PATFETCH();

            mainSwitch: do {
                switch(w.c) {
                case '$':
                    w.dollar();
                    break;

                case '^':
                    w.caret();
                    break;

                case '+':
                case '?':
                case '*':
                    w.prepareRepeat();

                    /* Star, etc. applied to an empty pattern is equivalent
                       to an empty pattern.  */
                    if(w.laststart==-1) {
                        break;
                    }
                    
                    if(w.greedy && w.many_times_ok && w.b[w.laststart] == anychar && w.bix-w.laststart <= 2) {
                        if(w.b[w.bix - 1] == stop_paren) {
                            w.bix--;
                        }
                        if(w.zero_times_ok) {
                            w.b[w.laststart] = anychar_repeat;
                        } else {
                            w.BUFPUSH(anychar_repeat);
                        }
                        break;
                    }

                    w.mainRepeat();
                    break;

                case '.':
                    w.dot();
                    break;

                case '[':
                    w.prepareCharset();
                    w.main_charset();
                    w.compact_charset();
                    break;

                case '(':
                    w.group_start();

                    if(w.c == '#') {
                        if(w.push_option!=0) {
                            w.BUFPUSH(option_set);
                            w.BUFPUSH((char)w.optz);
                        }
                        if(w.casefold!=0) {
                            if((w.optz & RE_OPTION_IGNORECASE) != 0) {
                                w.BUFPUSH(casefold_on);
                            } else {
                                w.BUFPUSH(casefold_off);
                            }
                        }
                        break;
                    }

                    w.group_start_end();
                    break;
                case ')':
                    w.group_end();
                    break;
                case '|':
                    w.alt();
                    break;
                case '{':
                unfetch_interval: do {
                    w.start_bounded_repeat();
                    
                    if(w.lower_bound < 0 || w.c != '}') {
                        break unfetch_interval;
                    }
                    switch(w.continue_bounded_repeat()) {
                    case 0:
                        break;
                    case 1:
                        continue mainSwitch;
                    case 2:
                        break mainSwitch;
                    }
                    w.bounded_nontrivial();
                    break mainSwitch;
                } while(false);
                w.unfetch_interval();
                break mainSwitch;

                case '\\':
                    w.escape();
                    break mainSwitch;
                case '#':
                    if((w.optz & RE_OPTION_EXTENDED) != 0) {
                        while(w.pix != w.pend) {
                            w.PATFETCH();
                            if(w.c == '\n') {
                                break;
                            }
                        }
                        break mainSwitch;
                    }
                    w.gotoNormalChar = true;
                    break mainSwitch;

                case ' ':
                case '\t':
                case '\f':
                case '\r':
                case '\n':
                    if((w.optz & RE_OPTION_EXTENDED) != 0) {
                        break mainSwitch;
                    }
                default:
                    if(w.c == ']') {
                        re_warning("regexp has `]' without escape");
                    } else if(w.c == '}') {
                        re_warning("regexp has `}' without escape");
                    }
                    w.gotoNormalChar = true;
                }
            } while(w.gotoRepeat);

            w.handle_num_or_normal();
        }

        w.finalize_compilation();
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern, int start, int length, Pattern bufp) throws PatternSyntaxException {
        return compile(pattern,start,length,bufp,ASCII);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile_s(char[] pattern, int start, int length, CompileContext ctx) throws PatternSyntaxException {
        return compile(pattern,start,length,emptyPattern(0),ctx);
    }

    /**
     * @mri re_compile_pattern
     */
    public static Pattern compile(char[] pattern, int start, int length, Pattern bufp, CompileContext ctx) throws PatternSyntaxException {
        bufp.compile(pattern,start,length,ctx);
        return bufp;
    }

    private final static void bm_init_skip(int[] skip, char[] patb, int patix, int m, char[] trans) {
        int j, c;
        
        for(c=0; c<256; c++) {
            skip[c] = m;
        }
        if(trans != null) {
            for (j=0; j<m-1; j++) {
                skip[trans[patb[patix+j]]] = m-1-j;
            }
        } else {
            for(j=0; j<m-1; j++) {
                skip[patb[patix+j]] = m-1-j;
            }
        }
    }

    private final static int calculate_must_string(char[] start, int end) {
        int mcnt;
        int max = 0;
        int p = 0;
        int pend = end;
        int must = -1;
        if(null == start || start.length == 0) {
            return -1;
        }

        while(p<pend) {
            switch(start[p++]) {
            case unused:
                break;
            case exactn:
                mcnt = start[p];
                if(mcnt > max) {
                    must = p;
                    max = mcnt;
                }
                p += mcnt+1;
                break;
            case start_memory:
            case stop_memory:
                p += 2;
                break;
            case duplicate:
            case option_set:
                p++;
                break;
            case casefold_on:
            case casefold_off:
                return 0;		/* should not check must_string */
            case pop_and_fail:
            case anychar:
            case anychar_repeat:
            case begline:
            case endline:
            case wordbound:
            case notwordbound:
            case wordbeg:
            case wordend:
            case wordchar:
            case notwordchar:
            case begbuf:
            case endbuf:
            case endbuf2:
            case begpos:
            case push_dummy_failure:
            case start_paren:
            case stop_paren:
                break;
            case charset:
            case charset_not:
                mcnt = start[p++];
                p += mcnt;
                mcnt = EXTRACT_UNSIGNED(start, p);
                p+=2;
                while(mcnt-- > 0) {
                    p += 8;
                }
                break;
            case on_failure_jump:
                mcnt = EXTRACT_NUMBER(start, p);
                p+=2;
                if(mcnt > 0) {
                    p += mcnt;
                }
                if(start[p-3] == jump) {
                    p -= 2;
                    mcnt = EXTRACT_NUMBER(start, p);
                    p+=2;
                    if(mcnt > 0) {
                        p += mcnt;
                    }
                }
                break;
            case dummy_failure_jump:
            case succeed_n: 
            case try_next:
            case jump:
                mcnt = EXTRACT_NUMBER(start, p);
                p+=2;
                if(mcnt > 0) {
                    p += mcnt;
                }
                break;
            case start_nowidth:
            case stop_nowidth:
            case stop_backtrack:
            case finalize_jump:
            case maybe_finalize_jump:
            case finalize_push:
                p += 2;
                break;
            case jump_n: 
            case set_number_at: 
            case finalize_push_n:
                p += 4;
                break;
            default:
                break;
            }
        }

        return must;
    }


    private final static void insert_op_2(char op, char[] b, int there, int current_end, int num_1, int num_2) {
        int pfrom = current_end;
        int pto = current_end+5;

        System.arraycopy(b,there,b,there+5,pfrom-there);

        b[there] = op;
        STORE_NUMBER(b, there + 1, num_1);
        STORE_NUMBER(b, there + 3, num_2);
    }

    private final static void store_jump_n(char[] b, int from, char opcode, int to, int n) {
        b[from] = opcode;
        STORE_NUMBER(b, from + 1, to - (from + 3));
        STORE_NUMBER(b, from + 3, n);
    }

    private final static void store_jump(char[] b, int from, char opcode, int to) {
        b[from] = opcode;
        STORE_NUMBER(b, from+1, to-(from+3));
    }

    private final static void insert_jump_n(char op, char[] b, int from, int to, int current_end, int n) {
        int pfrom = current_end;
        int pto = current_end+5;

        System.arraycopy(b,from,b,from+5,pfrom-from);
        store_jump_n(b, from, op, to, n);
    }


    private final static void insert_jump(char op, char[] b, int from, int to, int current_end) {
        int pfrom = current_end;
        int pto = current_end+3;
        System.arraycopy(b,from,b,from+3,pfrom-from);
        store_jump(b, from, op, to);
    }

    private final static void STORE_NUMBER(char[] d, int dix, int number) {
        d[dix] = (char)(number&0xFF);
        d[dix+1] = (char)((number >> 8)&0xFF);
        int vv = ((d[dix] & 0377) + (d[dix+1] << 8));
        if((vv & 0x8000) != 0) {
            vv |= 0xFFFF0000;
        }
    }

    private final static void STORE_NUMBER(int[] d, int dix, int number) {
        d[dix] = (number&0xFF);
        d[dix+1] = ((number >> 8)&0xFF);
        int vv = ((d[dix] & 0377) + (d[dix+1] << 8));
        if((vv & 0x8000) != 0) {
            vv |= 0xFFFF0000;
        }
    }

    private final static void STORE_MBC(char[] d, int dix, long c) {
        d[dix  ] = (char)(((c)>>>24) & 0xff);
        d[dix+1] = (char)(((c)>>>16) & 0xff);
        d[dix+2] = (char)(((c)>>> 8) & 0xff);
        d[dix+3] = (char)(((c)>>> 0) & 0xff);
    }

    private final static int EXTRACT_MBC(char[] p, int pix) {
        return p[pix]<<24 |
            p[pix+1] <<16 |
            p[pix+2] <<8 |
            p[pix+3];
    }

    private final static int EXTRACT_NUMBER(char[] b, int p) {
        int vv = (b[p] & 0377) | (b[p+1] << 8);
        if((vv & 0x8000) != 0) {
            vv |= 0xFFFF0000;
        }
        return vv;
    }

    private final static int EXTRACT_UNSIGNED(char[] b, int p) {
        return (b[p] & 0377) | (b[p+1] << 8);
    }

    private final char MBC2WC(char c, char[] p, int pix) {
        if(ctx.current_mbctype == MBCTYPE_UTF8) {
            int n = mbclen(c,ctx) - 1;
            c &= (1<<(6-n)) - 1;
            while(n-- > 0) {
                c = (char)(c << 6 | (p[pix++] & ((1<<6)-1)));
            }
        } else {
            c <<= 8;
            c |= p[pix];
        }
        return c;
    }

    private final static long utf8_firstbyte(long c) {
        if(c < 0x80) return c;
        if(c <= 0x7ff) return ((c>>6)&0xff)|0xc0;
        if(c <= 0xffff) return ((c>>12)&0xff)|0xe0;
        if(c <= 0x1fffff) return ((c>>18)&0xff)|0xf0;
        if(c <= 0x3ffffff) return ((c>>24)&0xff)|0xf8;
        if(c <= 0x7fffffff) return ((c>>30)&0xff)|0xfc;
        return 0xfe;
    }
    


    public final static boolean ismbchar(int c, CompileContext ctx) {
        return ctx.re_mbctab[c] != 0;
    }

    public final static int mbclen(char c, CompileContext ctx) {
        return ctx.re_mbctab[c] + 1;
    }

    private final static void set_list_bits(long c1, long c2, char[] b, int bix) {
        char sbc_size = b[bix-1];
        int mbc_size = EXTRACT_UNSIGNED(b,bix+sbc_size);
        int beg,end,upb;
        if(c1 > c2) {
            return;
        }
        bix+=(sbc_size+2);
        for(beg=0,upb=mbc_size;beg<upb;) {
            int mid = (beg+upb)>>>1;
            if((c1-1) > EXTRACT_MBC(b,bix+(mid*8+4))) {
                beg = mid+1;
            } else {
                upb = mid;
            }
        }
        for(end=beg,upb=mbc_size; end<upb; ) {
            int mid = (end+upb)>>>1;
            if(c2 >= (EXTRACT_MBC(b,bix+(mid*8))-1)) {
                end = mid+1;
            } else {
                upb = mid;
            }
        }
        if(beg != end) {
            if(c1 > EXTRACT_MBC(b,bix+(beg*8))) {
                c1 = EXTRACT_MBC(b,bix+(beg*8));
            }
            if(c2 < EXTRACT_MBC(b,bix+((end - 1)*8+4))) {
                c2 = EXTRACT_MBC(b,bix+((end - 1)*8+4));
            }
        }
        if(end < mbc_size && end != beg + 1) {
            System.arraycopy(b,bix+(end*8),b,bix+((beg+1)*8),(mbc_size - end)*8);
        }
        STORE_MBC(b,bix+(beg*8 + 0),c1);
        STORE_MBC(b,bix+(beg*8 + 4),c2);
        mbc_size += beg - end + 1;
        STORE_NUMBER(b,bix-2, mbc_size);
    }

    private List warnings = new ArrayList();

    private void re_warning(String msg) {
        warnings.add(msg);
    }

    public List getWarnings() {
        return warnings;
    }

    public void clearWarnings() {
        warnings.clear();
    }
    
    private static interface StartPos {
        int startpos(char[] string, int pos);
    }

    private static class ASC_StartPos implements StartPos {
        public int startpos(char[] string, int pos) {
            return pos;
        }
    }

    private static class SJIS_StartPos implements StartPos {
        private boolean isfirst(char c) {
            return mbctab_sjis[c] == 0;
        }

        private boolean istrail(char c) {
            return mbctab_sjis_trail[c] != 0;
        }

        private int mbclen(char c) {
            return mbctab_sjis[c] + 1;
        }

        public int startpos(char[] string, int pos) {
            int i = pos, w;
            if(i > 0 && istrail(string[i])) {
                do {
                    if(!isfirst(string[--i])) {
                        ++i;
                        break;
                    }
                } while (i > 0);
            }
            if(i == pos || i + (w = mbclen(string[i])) > pos) {
                return i;
            }
            i += w;
            return i + ((pos - i) & ~1);
        }
    }

    private static class EUC_StartPos implements StartPos {
        private boolean islead(char c) {
            return ((c) - 0xA1) > 0xFE - 0xa1;
        }

        private int mbclen(char c) {
            return mbctab_euc[c] + 1;
        }

        public int startpos(char[] string, int pos) {
            int i = pos, w;
            while(i > 0 && !islead(string[i])) {
                --i;
            }
            if(i == pos || i + (w = mbclen(string[i])) > pos) {
                return i;
            }
            i += w;
            return i + ((pos - i) & ~1);
        }
    }

    private static class UTF8_StartPos implements StartPos {
        private boolean islead(char c) {
            return (c & 0xC0) != 0x80;
        }

        private int mbclen(char c) {
            return mbctab_utf8[c] + 1;
        }

        public int startpos(char[] string, int pos) {
            int i = pos, w;

            while(i > 0 && !islead(string[i])) {
                --i;
            }
            if(i == pos || i + (w = mbclen(string[i])) > pos) {
                return i;
            }
            return i + w;
        }
    }

    private final static StartPos[] startpositions = {
        new ASC_StartPos(),
        new EUC_StartPos(),
        new SJIS_StartPos(),
        new UTF8_StartPos()};

    public final static int mbc_startpos(char[] string, int startpos, CompileContext ctx) {
        return startpositions[ctx.current_mbctype].startpos(string,startpos);
    }
    
    public int adjust_startpos(char[] string, int size, int startpos, int range) {
        /* Update the fastmap now if not correct already.  */
        if(fastmap_accurate==0) {
            compile_fastmap();
        }

        /* Adjust startpos for mbc string */
        if(ctx.current_mbctype != 0 && startpos>0 && (options&RE_OPTIMIZE_BMATCH) == 0) {
            int i = mbc_startpos(string, startpos, ctx);

            if(i < startpos) {
                if(range > 0) {
                    startpos = i + mbclen(string[i],ctx);
                } else {
                    int len = mbclen(string[i],ctx);
                    if(i + len <= startpos) {
                        startpos = i + len;
                    } else {
                        startpos = i;
                    }
                }
            }
        }
        return startpos;
    }


    private static void err(String msg) throws PatternSyntaxException {
        throw new PatternSyntaxException(msg);
    }

    private static Pattern emptyPattern(int flags) {
        Pattern p = new Pattern();
        p.options = flags;
        return p;
    }

    private Pattern() {}
    public Pattern(char[] b, int all, char[] fmap, int flags) {
        buffer = b;
        allocated = all;
        fastmap = fmap;
        options = flags;
    }

    private char[] buffer;	  /* Space holding the compiled pattern commands.  */
    private int allocated;	  /* Size of space that `buffer' points to. */
    private int used;		  /* Length of portion of buffer actually occupied  */
    private char[] fastmap;	  /* Pointer to fastmap, if any, or nul if none.  */
                              /* re_search uses the fastmap, if there is one,
                                 to skip over totally implausible characters.  */
    private int must;         /* Pointer to exact pattern which strings should have
			                     to be matched.  */
    private int[] must_skip;  /* Pointer to exact pattern skip table for bm_search */
    public  long options;  	  /* Flags for options such as extended_pattern. */
    private int re_nsub;	  /* Number of subexpressions found by the compiler. */
    private char fastmap_accurate;
			                  /* Set to zero when a new pattern is stored,
			                     set to one when the fastmap is updated from it.  */
    private char can_be_null; /* Set to one by compiling fastmap
			                     if this pattern might match the null string.
			                     It does not necessarily match the null string
                                 in that case, but if this is zero, it cannot.
                                 2 as value means can match null string
                                 but at end of range or before a character
                                 listed in the fastmap.  */

    /* stack & working area for re_match() */
    private int[] regstart;
    private int[] regend;
    private int[] old_regstart;
    private int[] old_regend;

    private static class RegisterInfoType {
        public char word;
        public boolean is_active;
        public boolean matched_something;
    }

    private RegisterInfoType[] reg_info;
    private int[] best_regstart;
    private int[] best_regend;
    
    public final boolean MAY_TRANSLATE() {
        return ((options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0 && ctx.translate!=null);
    }


    /**
     * @mri re_search
     */
    public int search(char[] string, int size, int startpos, int range, Registers regs) {
        int val=-1, anchor = 0, initpos = startpos;
        boolean doBegbuf = false;
        int pix;
        char c;
        /* Check for out-of-range starting position.  */
        if(startpos < 0 || startpos > size) {
            return -1;
        }

        /* Update the fastmap now if not correct already.  */
        if(fastmap!=null && fastmap_accurate==0) {
            compile_fastmap();
        }

        /* If the search isn't to be a backwards one, don't waste time in a
           search for a pattern that must be anchored.  */
        if(used > 0) {
            switch(buffer[0]) {
            case begbuf:
                doBegbuf = true;
                break;
            case begline:
                anchor = 1;
                break;
            case begpos:
                //                                System.err.println("doing match1");
                val = match(string, size, startpos, regs);
                if (val >= 0) {
                    return startpos;
                }
                return val;
            default:
                break;
            }
        }
        begbuf_match: do {
            if(doBegbuf) {
                if(range > 0) {
                    if(startpos > 0) {
                        return -1;
                    } else {
                        //                                                System.err.println("doing match2");
                        val = match(string, size, 0, regs);
                        if(val >= 0) {
                            return 0;
                        }
                        return val;
                    }
                }
                doBegbuf = false;
            }

            if((options&RE_OPTIMIZE_ANCHOR)!=0) {
                if((options&RE_OPTION_MULTILINE)!=0 && range > 0) {
                    doBegbuf = true;
                    continue begbuf_match;
                }
                anchor = 1;
            }

            if(must != -1) {
                int len = buffer[must];
                int pos=-1, pbeg, pend;

                pbeg = startpos;
                pend = startpos + range;
                if(pbeg > pend) {		/* swap pbeg,pend */
                    pos = pend; 
                    pend = pbeg; 
                    pbeg = pos;
                }
                pend = size;
                if((options&RE_OPTIMIZE_NO_BM) != 0) {
                    //                                                            System.err.println("doing slow_search");
                    pos = slow_search(buffer, must+1, len, string, pbeg, pend-pbeg, MAY_TRANSLATE()?ctx.translate:null);
                    //                                        System.err.println("slow_search=" + pos);
                } else {
                    //                                        System.err.println("doing bm_search (" + (must+1) + "," + len + "," + pbeg + "," +(pend-pbeg));
                    pos = bm_search(buffer, must+1, len, string, pbeg, pend-pbeg, must_skip, MAY_TRANSLATE()?ctx.translate:null);
                    //                                        System.err.println("bm_search=" + pos);
                }
                if(pos == -1) {
                    return -1;
                }
                if(range > 0 && (options&RE_OPTIMIZE_EXACTN) != 0) {
                    startpos += pos;
                    range -= pos;
                    if(range < 0) {
                        return -1;
                    }
                }
            }

            for (;;) {
                advance: do {
                    /* If a fastmap is supplied, skip quickly over characters that
                       cannot possibly be the start of a match.  Note, however, that
                       if the pattern can possibly match the null string, we must
                       test it at each starting point so that we take the first null
                       string we get.  */
                    if(fastmap!=null && startpos < size && can_be_null != 1 && !(anchor != 0 && startpos == 0)) {
                        if(range > 0) {	/* Searching forwards.  */
                            int irange = range;
                            pix = startpos;

                            startpos_adjust: while(range > 0) {
                                c = string[pix++];
                                if(ismbchar(c,ctx)) {
                                    int len;
                                    if(fastmap[c] != 0) {
                                        break;
                                    }
                                    len = mbclen(c,ctx) - 1;
                                    while(len-- > 0) {
                                        c = string[pix++];
                                        range--;
                                        if(fastmap[c] == 2) {
                                            break startpos_adjust;
                                        }
                                    }
                                } else {
                                    if(fastmap[MAY_TRANSLATE() ? ctx.translate[c] : c] != 0) {
                                        break;
                                    }
                                }
                                range--;
                            }
                            startpos += irange - range;
                        } else { /* Searching backwards.  */
                            c = string[startpos];
                            c &= 0xff;
                            if(MAY_TRANSLATE() ? fastmap[ctx.translate[c]]==0 : fastmap[c]==0) {
                                break advance;
                            }
                        }
 
                    }

                    if(startpos > size) {
                        return -1;
                    }
                    if((anchor!=0 || can_be_null==0) && range > 0 && size > 0 && startpos == size) {
                        return -1;
                    }

                    val = match_exec(string, size, startpos, initpos, regs);
                    if(val >= 0) {
                        return startpos;
                    }
                    if(val == -2) {
                        return -2;
                    }

                    if(range > 0) {
                        if(anchor!=0 && startpos < size && (startpos < 1 || string[startpos-1] != '\n')) {
                            while(range > 0 && string[startpos] != '\n') {
                                range--;
                                startpos++;
                            }
                        }
                    }

                } while(false);

                //advance:

                if(range==0) { 
                    break;
                } else if(range > 0) {
                    int d = startpos;
                    if(ismbchar(string[d],ctx)) {
                        int len = mbclen(string[d],ctx) - 1;
                        range-=len;
                        startpos+=len;
                        if(range==0) {
                            break;
                        }
                    }
                    range--;
                    startpos++;
                } else {
                    range++;
                    startpos--;
                    {
                        int s = 0;
                        int d = startpos;
                        for(pix = d; pix-- > s && ismbchar(string[pix],ctx); );
                        if(((d - pix)&1) == 0) {
                            if(range == 0) {
                                break;
                            }
                            range++;
                            startpos--;
                        }
                    }
                }
            }
        } while(false);

        return -1;
    }

    private final boolean IS_A_LETTER(char[] d, int dix, int dend) {
        return re_syntax_table[d[dix]] == Sword ||
            (ctx.current_mbctype != 0 ? 
             (ctx.re_mbctab[d[dix]] != 0 && d[dix+mbclen(d[dix],ctx)]<=dend):
             re_syntax_table[d[dix]] == Sword2);
    }

    private final boolean PREV_IS_A_LETTER(char[] d, int dix, int dend) {
        return ((ctx.current_mbctype == MBCTYPE_SJIS)?
                IS_A_LETTER(d,dix-(((dix-1)!=0&&ismbchar(d[dix-2],ctx))?2:1),dend):
                ((ctx.current_mbctype!=0 && (d[dix-1] >= 0x80)) ||
                 IS_A_LETTER(d,dix-1,dend)));
    }

    public final static int memcmp(byte[] s, int s1, byte[] ss, int s2, int len) {
        while(len > 0) {
            if(s[s1++] != ss[s2++]) {
                return 1;
            }
        }
        return 0;
    }

    public final static int memcmp(byte[] s, int s1, int s2, int len) {
        return memcmp(s,s1,s,s2,len);
    }

    public final static int memcmp(char[] s, int s1, char[] ss, int s2, int len) {
        while(len > 0) {
            if(s[s1++] != ss[s2++]) {
                return 1;
            }
        }
        return 0;
    }

    public final static int memcmp(char[] s, int s1, int s2, int len) {
        return memcmp(s,s1,s,s2,len);
    }

    private final int memcmp_translate(char[] s, int s1, int s2, int len) {
        int p1 = s1;
        int p2 = s2;
        char cc;
        while(len>0) {
            cc = s[p1++];
            if(ismbchar(cc,ctx)) {
                int n;
                if(cc != s[p2++]) {
                    return 1;
                }
                for(n=mbclen(cc,ctx)-1; n>0; n--) {
                    if(--len == 0 || s[p1++] != s[p2++]) {
                        return 1;
                    }
                }
            } else {
                if(ctx.translate[cc] != ctx.translate[s[p2++]]) {
                    return 1;
                }
            }
            len--;
        }
        return 0;
    }


    /**
     * @mri re_match
     */
    public int match(char[] string_arg, int size, int pos, Registers regs) {
        return match_exec(string_arg, size, pos, pos, regs);
    }

    private final static int NON_GREEDY = 1;
    private final static int REG_UNSET_VALUE = -1;

    private final static int NUM_NONREG_ITEMS = 4;
    private final static int NUM_REG_ITEMS = 3;
    private final static int NUM_COUNT_ITEMS = 2;

    private static class MatchEnvironment {
        public char[] p;
        public char c;
        public int p1;
        public int pix;
        public int pend;
        public int optz;
        public int num_regs;
        public char[] string;
        public int mcnt;
        public int d;
        public int dend;
        public int pos;

        public CompileContext ctx;

        /* Failure point stack.  Each place that can handle a failure further
           down the line pushes a failure point on this stack.  It consists of
           restart, regend, and reg_info for all registers corresponding to the
           subexpressions we're currently inside, plus the number of such
           registers, and, finally, two char *'s.  The first char * is where to
           resume scanning the pattern; the second one is where to resume
           scanning the strings.  If the latter is zero, the failure point is a
           ``dummy''; if a failure happens and the failure point is a dummy, it
           gets discarded and the next next one is tried.  */
        public int[] stacka;
        public int[] stackb;
        public int stackp;
        public int stacke;

        public boolean best_regs_set = false;
        public int num_failure_counts = 0;

        public Pattern self;

        public final boolean TRANSLATE_P() {
            return ((optz&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
        }

        public final boolean MAY_TRANSLATE() {
            return ((self.options&(RE_OPTION_IGNORECASE|RE_MAY_IGNORECASE))!=0 && ctx.translate!=null);
        }

        public final void init_stack() {
            /* Initialize the stack. */
            stacka = new int[(num_regs*3 + 4)*160];
            stackb = stacka;
            stackp = 0;
            stacke = stackb.length;
        }

        public final void init_registers() {
            /* Initialize subexpression text positions to -1 to mark ones that no
               ( or ( and ) or ) has been seen for. Also set all registers to
               inactive and mark them as not having matched anything or ever
               failed. */
            for(mcnt = 0; mcnt < num_regs; mcnt++) {
                self.regstart[mcnt] = self.regend[mcnt]
                    = self.old_regstart[mcnt] = self.old_regend[mcnt]
                    = self.best_regstart[mcnt] = self.best_regend[mcnt] = REG_UNSET_VALUE;
                self.reg_info[mcnt].is_active = false;
                self.reg_info[mcnt].matched_something = false;
            }
        }

        public final void POP_FAILURE_COUNT() {
            int ptr = stackb[--stackp];
            int count = stackb[--stackp];
            STORE_NUMBER(p, ptr, count);
        }

        public final void POP_FAILURE_POINT() {
            long temp;
            stackp -= NUM_NONREG_ITEMS;	/* Remove failure points (and flag). */
            temp = stackb[--stackp];	/* How many regs pushed.  */
            temp *= NUM_REG_ITEMS;	/* How much to take off the stack.  */
            stackp -= temp; 		/* Remove the register info.  */
            temp = stackb[--stackp];	/* How many counters pushed.  */
            while(temp-- > 0) {
                POP_FAILURE_COUNT();
            }
            num_failure_counts = 0;	/* Reset num_failure_counts.  */
        }

        public final void SET_REGS_MATCHED() {
            for(int this_reg = 0; this_reg < num_regs; this_reg++) {
                self.reg_info[this_reg].matched_something = self.reg_info[this_reg].is_active;
            }
        }

        public final void PUSH_FAILURE_POINT(int pattern_place, int string_place) {
            int last_used_reg, this_reg;

            /* Find out how many registers are active or have been matched.
               (Aside from register zero, which is only set at the end.) */
            for(last_used_reg = num_regs-1; last_used_reg > 0; last_used_reg--) {
                if(self.regstart[last_used_reg]!=REG_UNSET_VALUE) {
                    break;
                }
            }
                        
            if(stacke - stackp <= (last_used_reg * NUM_REG_ITEMS + NUM_NONREG_ITEMS + 1)) {
                int[] stackx;
                int xlen = stacke;
                stackx = new int[2*xlen];
                System.arraycopy(stackb,0,stackx,0,xlen);
                stackb = stackx;
                stacke = 2*xlen;
            }
            stackb[stackp++] = num_failure_counts;
            num_failure_counts = 0;

            /* Now push the info for each of those registers.  */
            for(this_reg = 1; this_reg <= last_used_reg; this_reg++) {
                stackb[stackp++] = self.regstart[this_reg];
                stackb[stackp++] = self.regend[this_reg];
                stackb[stackp++] = self.reg_info[this_reg].word;
            }

            /* Push how many registers we saved.  */
            stackb[stackp++] = last_used_reg;
                        
            stackb[stackp++] = pattern_place;
            stackb[stackp++] = string_place;
            stackb[stackp++] = (int)optz; /* current option status */
            stackb[stackp++] = 0; /* non-greedy flag */
        }

        public final boolean duplicate() {
            int regno = p[pix++];   /* Get which register to match against */
            int d2, dend2;

            /* Check if there's corresponding group */
            if(regno >= num_regs) {
                return true;
            }
            /* Check if corresponding group is still open */
            if(self.reg_info[regno].is_active) {
                return true;
            }

            /* Where in input to try to start matching.  */
            d2 = self.regstart[regno];
            if(d2 == REG_UNSET_VALUE) {
                return true;
            }

            /* Where to stop matching; if both the place to start and
               the place to stop matching are in the same string, then
               set to the place to stop, otherwise, for now have to use
               the end of the first string.  */

            dend2 = self.regend[regno];
            if(dend2 == REG_UNSET_VALUE) {
                return true;
            }

            for(;;) {
                /* At end of register contents => success */
                if(d2 == dend2) {
                    break;
                }

                /* If necessary, advance to next segment in data.  */
                if(d == dend) {return true;}

                /* How many characters left in this segment to match.  */
                mcnt = dend - d;

                /* Want how many consecutive characters we can match in
                   one shot, so, if necessary, adjust the count.  */
                if(mcnt > dend2 - d2) {
                    mcnt = dend2 - d2;
                }

                /* Compare that many; failure if mismatch, else move
                   past them.  */
                if(((self.options & RE_OPTION_IGNORECASE) != 0) ? self.memcmp_translate(string, d, d2, mcnt)!=0 : self.memcmp(string, d, d2, mcnt)!=0) {
                    return true;
                }
                d += mcnt;
                d2 += mcnt;
            }
            return false;
        }

        public final boolean is_in_list_sbc(int cx, char[] b, int bix) {
            int size = b[bix++];
            return cx/8 < size && (b[bix + cx/8]&(1<<cx%8)) != 0;
        }
  
        public final boolean is_in_list_mbc(int cx, char[] b, int bix) {
            int size = b[bix++];
            bix+=size+2;
            size = EXTRACT_UNSIGNED(b,bix-2);
            if(size == 0) {
                return false;
            }
            int i,j;
            for(i=0,j=size;i<j;) {
                int k = (i+j)>>1;
                if(cx > EXTRACT_MBC(b,bix+k*8+4)) {
                    i = k+1;
                } else {
                    j = k;
                }
            }
            return i<size && EXTRACT_MBC(b,bix+i*8) <= cx;
        }        

        public final boolean is_in_list(int cx, char[] b, int bix) {
            return is_in_list_sbc(cx, b, bix) || (ctx.current_mbctype!=0 ? is_in_list_mbc(cx, b, bix) : false);
        }
    }

    /**
     * @mri re_match_exec
     */
    public int match_exec(char[] string_arg, int size, int pos, int beg, Registers regs) {
        MatchEnvironment w = new MatchEnvironment();
        w.p = buffer;
        w.p1=-1;
        w.pend = used;
        w.num_regs = re_nsub;
        w.string = string_arg;
        w.optz = (int)options;
        w.self = this;
        w.ctx = this.ctx;
        w.pos = pos;

        if(regs != null) {
            regs.init_regs(w.num_regs);
        }

        w.init_stack();
        w.init_registers();


        /* Set up pointers to ends of strings.
           Don't allow the second string to be empty unless both are empty.  */

        /* `p' scans through the pattern as `d' scans through the data. `dend'
           is the end of the input string that `d' points within. `d' is
           advanced into the following input string whenever necessary, but
           this happens before fetching; therefore, at the beginning of the
           loop, `d' can be pointing at the end of a string, but it cannot
           equal string2.  */

        w.d = pos; w.dend = size;

        /* This loops over pattern commands.  It exits by returning from the
           function if match is complete, or it drops through if match fails
           at this starting point in the input data.  */
        boolean gotoRestoreBestRegs = false;
        restore_best_regs2: do {
            mainLoop: for(;;) {
                fail1: do {
                    /* End of pattern means we might have succeeded.  */
                    if(w.pix == w.pend || gotoRestoreBestRegs) {
                        if(!gotoRestoreBestRegs) {
                            restore_best_regs: do {
                                /* If not end of string, try backtracking.  Otherwise done.  */
                                if((options&RE_OPTION_LONGEST)!=0 && w.d != w.dend) {
                                    if(w.best_regs_set) {/* non-greedy, no need to backtrack */
                                        /* Restore best match.  */
                                        w.d = best_regend[0];

                                        for(w.mcnt = 0; w.mcnt < w.num_regs; w.mcnt++) {
                                            regstart[w.mcnt] = best_regstart[w.mcnt];
                                            regend[w.mcnt] = best_regend[w.mcnt];
                                        }
                                        break restore_best_regs;
                                    }
                                    while(w.stackp != 0 && w.stackb[w.stackp-1] == NON_GREEDY) {
                                        if(w.best_regs_set) {/* non-greedy, no need to backtrack */
                                            w.d = best_regend[0];

                                            for(w.mcnt = 0; w.mcnt < w.num_regs; w.mcnt++) {
                                                regstart[w.mcnt] = best_regstart[w.mcnt];
                                                regend[w.mcnt] = best_regend[w.mcnt];
                                            }
                                            break restore_best_regs;
                                        }
                                        w.POP_FAILURE_POINT();
                                    }
                                    if(w.stackp != 0) {
                                        /* More failure points to try.  */

                                        /* If exceeds best match so far, save it.  */
                                        if(!w.best_regs_set || (w.d > best_regend[0])) {
                                            w.best_regs_set = true;
                                            best_regend[0] = w.d;	/* Never use regstart[0].  */

                                            for(w.mcnt = 1; w.mcnt < w.num_regs; w.mcnt++) {
                                                best_regstart[w.mcnt] = regstart[w.mcnt];
                                                best_regend[w.mcnt] = regend[w.mcnt];
                                            }
                                        }
                                        break fail1;	       
                                    } /* If no failure points, don't restore garbage.  */
                                    else if(w.best_regs_set) {
                                        /* Restore best match.  */
                                        w.d = best_regend[0];

                                        for(w.mcnt = 0; w.mcnt < w.num_regs; w.mcnt++) {
                                            regstart[w.mcnt] = best_regstart[w.mcnt];
                                            regend[w.mcnt] = best_regend[w.mcnt];
                                        }
                                    }
                                }
                            } while(false);
                        } else {
                            gotoRestoreBestRegs = false;
                        }

                        /* If caller wants register contents data back, convert it 
                           to indices.  */
                        if(regs != null) {
                            regs.beg[0] = pos;
                            regs.end[0] = w.d;
                            for(w.mcnt = 1; w.mcnt < w.num_regs; w.mcnt++) {
                                if(regend[w.mcnt] == REG_UNSET_VALUE) {
                                    regs.beg[w.mcnt] = -1;
                                    regs.end[w.mcnt] = -1;
                                    continue;
                                }
                                regs.beg[w.mcnt] = regstart[w.mcnt];
                                regs.end[w.mcnt] = regend[w.mcnt];
                            }
                        }
                        return w.d - w.pos;
                    }

                    //                    System.err.println("--executing " + (int)p[pix] + " at " + pix);
                    switch(w.p[w.pix++]) {
                        /* ( [or `(', as appropriate] is represented by start_memory,
                           ) by stop_memory.  Both of those commands are followed by
                           a register number in the next byte.  The text matched
                           within the ( and ) is recorded under that number.  */
                    case start_memory:
                        old_regstart[w.p[w.pix]] = regstart[w.p[w.pix]];
                        regstart[w.p[w.pix]] = w.d;
                        reg_info[w.p[w.pix]].is_active = true;
                        reg_info[w.p[w.pix]].matched_something = false;
                        w.pix += 2;
                        continue mainLoop;
                    case stop_memory:
                        old_regend[w.p[w.pix]] = regend[w.p[w.pix]];
                        regend[w.p[w.pix]] = w.d;
                        reg_info[w.p[w.pix]].is_active = false;
                        w.pix += 2;
                        continue mainLoop;
                    case start_paren:
                    case stop_paren:
                        break;
                        /* \<digit> has been turned into a `duplicate' command which is
                           followed by the numeric value of <digit> as the register number.  */
                    case duplicate:
                        if(w.duplicate()) {
                            break fail1;
                        }
                        break;
                    case start_nowidth:
                        w.PUSH_FAILURE_POINT(-1,w.d);
                        if(w.stackp > RE_DUP_MAX) {
                            return -2;
                        }
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix+=2;
                        STORE_NUMBER(w.p, w.pix+w.mcnt, w.stackp);
                        continue mainLoop;
                    case stop_nowidth:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix+=2;
                        w.stackp = w.mcnt;
                        w.d = w.stackb[w.stackp-3];
                        w.POP_FAILURE_POINT();
                        continue mainLoop;
                    case stop_backtrack:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix+=2;
                        w.stackp = w.mcnt;
                        w.POP_FAILURE_POINT();
                        continue mainLoop;
                    case pop_and_fail:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix+1);
                        w.stackp = w.mcnt;
                        w.POP_FAILURE_POINT();
                        break fail1;
                    case anychar:
                        if(w.d == w.dend) {break fail1;}

                        if(ismbchar(w.string[w.d],w.ctx)) {
                            if(w.d + mbclen(w.string[w.d],w.ctx) > w.dend) {
                                break fail1;
                            }
                            w.SET_REGS_MATCHED();
                            w.d += mbclen(w.string[w.d],w.ctx);
                            break;
                        }
                        if((w.optz&RE_OPTION_MULTILINE)==0
                           && (w.TRANSLATE_P() ? w.ctx.translate[w.string[w.d]] : w.string[w.d]) == '\n') {
                            break fail1;
                        }
                        w.SET_REGS_MATCHED();
                        w.d++;
                        break;
                    case anychar_repeat:
                        for (;;) {
                            w.PUSH_FAILURE_POINT(w.pix,w.d);
                            if(w.d == w.dend) {break fail1;}
                            if(ismbchar(w.string[w.d],w.ctx)) {
                                if(w.d + mbclen(w.string[w.d],w.ctx) > w.dend) {
                                    break fail1;
                                }
                                w.SET_REGS_MATCHED();
                                w.d += mbclen(w.string[w.d],w.ctx);
                                continue;
                            }
                            if((w.optz&RE_OPTION_MULTILINE)==0 &&
                               (w.TRANSLATE_P() ? w.ctx.translate[w.string[w.d]] : w.string[w.d]) == '\n') {
                                break fail1;
                            }
                            w.SET_REGS_MATCHED();
                            w.d++;
                        }

                    case charset:
                    case charset_not: {
                        boolean not;	    /* Nonzero for charset_not.  */
                        boolean part = false;	    /* true if matched part of mbc */
                        int dsave = w.d + 1;
                        int cc;
                    
                        if(w.d == w.dend) {break fail1;}

                        w.c = w.string[w.d++];
                        if(ismbchar(w.c,w.ctx)) {
                            if(w.d + mbclen(w.c,w.ctx) - 1 <= w.dend) {
                                cc = w.c;
                                w.c = MBC2WC(w.c, w.string, w.d);
                                not = w.is_in_list_mbc(w.c, w.p, w.pix);
                                if(!not) {
                                    part = not = w.is_in_list_sbc(cc, w.p, w.pix);
                                }
                            } else {
                                not = w.is_in_list(w.c, w.p, w.pix);
                            }
                        } else {
                            if(w.TRANSLATE_P()) {
                                w.c = w.ctx.translate[w.c];
                            }
                            not = w.is_in_list(w.c, w.p, w.pix);
                        }
                        if(w.p[w.pix-1] == charset_not) {
                            not = !not;
                        }

                        if(!not) {break fail1;}
                    
                        w.pix += 1 + w.p[w.pix] + 2 + EXTRACT_UNSIGNED(w.p, w.pix + 1 + w.p[w.pix])*8;
                        w.SET_REGS_MATCHED();
                    
                        if(part) {
                            w.d = dsave;
                        }
                        continue mainLoop;
                    }

                    case begline:
                        if(size == 0 || w.d == 0) {
                            continue mainLoop;
                        }
                        if(w.string[w.d-1] == '\n' && w.d != w.dend) {
                            continue mainLoop;
                        }
                        break fail1;
                    case endline:
                        if(w.d == w.dend) {
                            continue mainLoop;
                        } else if(w.string[w.d] == '\n') {
                            continue mainLoop;
                        }
                        break fail1;
                        /* Match at the very beginning of the string. */
                    case begbuf:
                        if(w.d==0) {
                            continue mainLoop;
                        }
                        break fail1;
                        /* Match at the very end of the data. */
                    case endbuf:
                        if(w.d == w.dend) {
                            continue mainLoop;
                        }
                        break fail1;
                        /* Match at the very end of the data. */
                    case endbuf2:
                        if(w.d == w.dend) {
                            continue mainLoop;
                        }
                        /* .. or newline just before the end of the data. */
                        if(w.string[w.d] == '\n' && w.d+1 == w.dend) {
                            continue mainLoop;
                        }
                        break fail1;
                        /* `or' constructs are handled by starting each alternative with
                           an on_failure_jump that points to the start of the next
                           alternative.  Each alternative except the last ends with a
                           jump to the joining point.  (Actually, each jump except for
                           the last one really jumps to the following jump, because
                           tensioning the jumps is a hassle.)  */
                    
                        /* The start of a stupid repeat has an on_failure_jump that points
                           past the end of the repeat text. This makes a failure point so 
                           that on failure to match a repetition, matching restarts past
                           as many repetitions have been found with no way to fail and
                           look for another one.  */
                    
                        /* A smart repeat is similar but loops back to the on_failure_jump
                           so that each repetition makes another failure point.  */
                    
                        /* Match at the starting position. */
                    case begpos:
                        if(w.d == beg) {
                            continue mainLoop;
                        }
                        break fail1;

                    case on_failure_jump:
                        //                on_failure:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix+=2;
                        w.PUSH_FAILURE_POINT(w.pix+w.mcnt,w.d);
                        continue mainLoop;

                        /* The end of a smart repeat has a maybe_finalize_jump back.
                           Change it either to a finalize_jump or an ordinary jump.  */
                    case maybe_finalize_jump:
                        w.mcnt = EXTRACT_NUMBER(w.p,w.pix);
                        w.pix+=2;
                        w.p1 = w.pix;

                        /* Compare the beginning of the repeat with what in the
                           pattern follows its end. If we can establish that there
                           is nothing that they would both match, i.e., that we
                           would have to backtrack because of (as in, e.g., `a*a')
                           then we can change to finalize_jump, because we'll
                           never have to backtrack.

                           This is not true in the case of alternatives: in
                           `(a|ab)*' we do need to backtrack to the `ab' alternative
                           (e.g., if the string was `ab').  But instead of trying to
                           detect that here, the alternative has put on a dummy
                           failure point which is what we will end up popping.  */

                        /* Skip over open/close-group commands.  */
                        while(w.p1 + 2 < w.pend) {
                            if(w.p[w.p1] == stop_memory ||
                               w.p[w.p1] == start_memory) {
                                w.p1 += 3;	/* Skip over args, too.  */
                            } else if(w.p[w.p1] == stop_paren) {
                                w.p1 += 1;
                            } else {
                                break;
                            }
                        }
                        if(w.p1 == w.pend) {
                            w.p[w.pix-3] = finalize_jump;
                        } else if(w.p[w.p1] == exactn || w.p[w.p1] == endline) {
                            w.c = w.p[w.p1] == endline ? '\n' : w.p[w.p1+2];
                            int p2 = w.pix+w.mcnt;
                            /* p2[0] ... p2[2] are an on_failure_jump.
                               Examine what follows that.  */
                            if(w.p[p2+3] == exactn && w.p[p2+5] != w.c) {
                                w.p[w.pix-3] = finalize_jump;
                            } else if(w.p[p2+3] == charset ||
                                      w.p[p2+3] == charset_not) {
                                boolean not;
                                if(ismbchar(w.c,w.ctx)) {
                                    int pp = w.p1+3;
                                    w.c = MBC2WC(w.c, w.p, pp);
                                }
                                /* `is_in_list()' is TRUE if c would match */
                                /* That means it is not safe to finalize.  */
                                not = w.is_in_list(w.c, w.p, p2 + 4);
                                if(w.p[p2+3] == charset_not) {
                                    not = !not;
                                }
                                if(!not) {
                                    w.p[w.pix-3] = finalize_jump;
                                }
                            }
                        }
                        w.pix -= 2;		/* Point at relative address again.  */
                        if(w.p[w.pix-1] != finalize_jump) {
                            w.p[w.pix-1] = jump;	
                            w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                            w.pix += 2;
                            if(w.mcnt < 0 && w.stackp > 2 && w.stackb[w.stackp-3] == w.d) {/* avoid infinite loop */
                                break fail1;
                            }
                            w.pix += w.mcnt;
                            continue mainLoop;
                        }
                        /* Note fall through.  */

                        /* The end of a stupid repeat has a finalize_jump back to the
                           start, where another failure point will be made which will
                           point to after all the repetitions found so far.  */
                    
                        /* Take off failure points put on by matching on_failure_jump 
                           because didn't fail.  Also remove the register information
                           put on by the on_failure_jump.  */

                    case finalize_jump:
                        if(w.stackp > 2 && w.stackb[w.stackp-3] == w.d) {
                            w.pix = w.stackb[w.stackp-4];
                            w.POP_FAILURE_POINT();
                            continue mainLoop;
                        }
                        w.POP_FAILURE_POINT();
                        /* Note fall through.  */

                        /* We need this opcode so we can detect where alternatives end
                           in `group_match_null_string_p' et al.  */
                    case jump_past_alt:
                        /* fall through */
                        /* Jump without taking off any failure points.  */
                    case jump:
                        //      nofinalize:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix += 2;
                        if(w.mcnt < 0 && w.stackp > 2 && w.stackb[w.stackp-3] == w.d) {/* avoid infinite loop */
                            break fail1;
                        }
                        w.pix += w.mcnt;
                        continue mainLoop;
                    case dummy_failure_jump:
                        /* Normally, the on_failure_jump pushes a failure point, which
                           then gets popped at finalize_jump.  We will end up at
                           finalize_jump, also, and with a pattern of, say, `a+', we
                           are skipping over the on_failure_jump, so we have to push
                           something meaningless for finalize_jump to pop.  */
                        w.PUSH_FAILURE_POINT(-1,0);
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix += 2;
                        if(w.mcnt < 0 && w.stackp > 2 && w.stackb[w.stackp-3] == w.d) {/* avoid infinite loop */
                            break fail1;
                        }
                        w.pix += w.mcnt;
                        continue mainLoop;

                        /* At the end of an alternative, we need to push a dummy failure
                           point in case we are followed by a `finalize_jump', because
                           we don't want the failure point for the alternative to be
                           popped.  For example, matching `(a|ab)*' against `aab'
                           requires that we match the `ab' alternative.  */
                    case push_dummy_failure:
                        /* See comments just above at `dummy_failure_jump' about the
                           two zeroes.  */
                        w.p1 = w.pix;
                        /* Skip over open/close-group commands.  */
                        while(w.p1 + 2 < w.pend) {
                            if(w.p[w.p1] == stop_memory ||
                               w.p[w.p1] == start_memory) {
                                w.p1 += 3;	/* Skip over args, too.  */
                            } else if(w.p[w.p1] == stop_paren) {
                                w.p1 += 1;
                            } else {
                                break;
                            }
                        }
                        if(w.p1 < w.pend && w.p[w.p1] == jump) {
                            w.p[w.pix-1] = unused;
                        } else {
                            w.PUSH_FAILURE_POINT(-1,0);
                        }
                        continue mainLoop;

                        /* Have to succeed matching what follows at least n times.  Then
                           just handle like an on_failure_jump.  */
                    case succeed_n: 
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix + 2);
                        /* Originally, this is how many times we HAVE to succeed.  */
                        if(w.mcnt != 0) {
                            w.mcnt--;
                            w.pix += 2;

                            w.c = (char)EXTRACT_NUMBER(w.p, w.pix);
                            if(w.stacke - w.stackp <= NUM_COUNT_ITEMS) {
                                int[] stackx;
                                int xlen = w.stacke;
                                stackx = new int[2*xlen];
                                System.arraycopy(w.stackb,0,stackx,0,xlen);
                                w.stackb = stackx;
                                w.stacke = 2*xlen;
                            }
                            w.stackb[w.stackp++] = w.c;
                            w.stackb[w.stackp++] = w.pix;
                            w.num_failure_counts++;

                            STORE_NUMBER(w.p, w.pix, w.mcnt);
                            w.pix+=2;

                            w.PUSH_FAILURE_POINT(-1,0);
                        } else  {
                            w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                            w.pix+=2;
                            w.PUSH_FAILURE_POINT(w.pix+w.mcnt,w.d);
                        }
                        continue mainLoop;

                    case jump_n:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix + 2);
                        /* Originally, this is how many times we CAN jump.  */
                        if(w.mcnt!=0) {
                            w.mcnt--;

                            w.c = (char)EXTRACT_NUMBER(w.p, w.pix+2);
                            if(w.stacke - w.stackp <= NUM_COUNT_ITEMS) {
                                int[] stackx;
                                int xlen = w.stacke;
                                stackx = new int[2*xlen];
                                System.arraycopy(w.stackb,0,stackx,0,xlen);
                                w.stackb = stackx;
                                w.stacke = 2*xlen;
                            }
                            w.stackb[w.stackp++] = w.c;
                            w.stackb[w.stackp++] = w.pix+2;
                            w.num_failure_counts++;
                            STORE_NUMBER(w.p, w.pix + 2, w.mcnt);
                            w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                            //                            System.err.println("jump_n ix: " + mcnt);
                            w.pix += 2;
                            if(w.mcnt < 0 && w.stackp > 2 && w.stackb[w.stackp-3] == w.d) {/* avoid infinite loop */
                                break fail1;
                            }
                            w.pix += w.mcnt;
                            continue mainLoop;
                        }
                        /* If don't have to jump any more, skip over the rest of command.  */
                        else {
                            w.pix += 4;
                        }
                        continue mainLoop;
                    case set_number_at:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix+=2;
                        w.p1 = w.pix + w.mcnt;
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix+=2;
                        STORE_NUMBER(w.p, w.p1, w.mcnt);
                        continue mainLoop;
                    case try_next:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix += 2;
                        if(w.pix + w.mcnt < w.pend) {
                            w.PUSH_FAILURE_POINT(w.pix,w.d);
                            w.stackb[w.stackp-1] = NON_GREEDY;
                        }
                        w.pix += w.mcnt;
                        continue mainLoop;
                    case finalize_push:
                        w.POP_FAILURE_POINT();
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                        w.pix+=2;
                        if(w.mcnt < 0 && w.stackp > 2 && w.stackb[w.stackp-3] == w.d) { /* avoid infinite loop */
                            break fail1;
                        }
                        w.PUSH_FAILURE_POINT(w.pix+w.mcnt,w.d);
                        w.stackb[w.stackp-1] = NON_GREEDY;
                        continue mainLoop;
                    case finalize_push_n:
                        w.mcnt = EXTRACT_NUMBER(w.p, w.pix + 2); 
                        /* Originally, this is how many times we CAN jump.  */
                        if(w.mcnt>0) {
                            int posx, i;
                            w.mcnt--;
                            STORE_NUMBER(w.p, w.pix + 2, w.mcnt);
                            posx = EXTRACT_NUMBER(w.p, w.pix);
                            i = EXTRACT_NUMBER(w.p,w.pix+posx+5);
                            if(i > 0) {
                                w.mcnt = EXTRACT_NUMBER(w.p, w.pix);
                                w.pix += 2;
                                if(w.mcnt < 0 && w.stackp > 2 && w.stackb[w.stackp-3] == w.d) {/* avoid infinite loop */
                                    break fail1;
                                }
                                w.pix += w.mcnt;
                                continue mainLoop;
                            }
                            w.POP_FAILURE_POINT();
                            w.mcnt = EXTRACT_NUMBER(w.p,w.pix);
                            w.pix+=2;
                            w.PUSH_FAILURE_POINT(w.pix+w.mcnt,w.d);
                            w.stackb[w.stackp-1] = NON_GREEDY;
                            w.pix += 2;		/* skip n */
                        }
                        /* If don't have to push any more, skip over the rest of command.  */
                        else {
                            w.pix += 4;
                        }
                        continue mainLoop;
                        /* Ignore these.  Used to ignore the n of succeed_n's which
                           currently have n == 0.  */
                    case unused:
                        continue mainLoop;
                    case casefold_on:
                        w.optz |= RE_OPTION_IGNORECASE;
                        continue mainLoop;
                    case casefold_off:
                        w.optz &= ~RE_OPTION_IGNORECASE;
                        continue mainLoop;
                    case option_set:
                        w.optz = w.p[w.pix++];
                        continue mainLoop;
                    case wordbound:
                        if(w.d == 0) {
                            if(w.d == w.dend) {break fail1;}
                            if(IS_A_LETTER(w.string,w.d,w.dend)) {
                                continue mainLoop;
                            } else {
                                break fail1;
                            }
                        }
                        if(w.d == w.dend) {
                            if(PREV_IS_A_LETTER(w.string,w.d,w.dend)) {
                                continue mainLoop;
                            } else {
                                break fail1;
                            }
                        }
                        if(PREV_IS_A_LETTER(w.string,w.d,w.dend) != IS_A_LETTER(w.string,w.d,w.dend)) {
                            continue mainLoop;
                        }
                        break fail1;
                    case notwordbound:
                        if(w.d==0) {
                            if(IS_A_LETTER(w.string, w.d, w.dend)) {
                                break fail1;
                            } else {
                                continue mainLoop;
                            }
                        }
                        if(w.d == w.dend) {
                            if(PREV_IS_A_LETTER(w.string, w.d, w.dend)) {
                                break fail1;
                            } else {
                                continue mainLoop;
                            }
                        }
                        if(PREV_IS_A_LETTER(w.string, w.d, w.dend) != IS_A_LETTER(w.string, w.d, w.dend)) {
                            break fail1;
                        }
                        continue mainLoop;
                    case wordbeg:
                        if(IS_A_LETTER(w.string, w.d, w.dend) && (w.d==0 || !PREV_IS_A_LETTER(w.string,w.d,w.dend))) {
                            continue mainLoop;
                        }
                        break fail1;
                    case wordend:
                        if(w.d!=0 && PREV_IS_A_LETTER(w.string, w.d, w.dend)
                           && (!IS_A_LETTER(w.string, w.d, w.dend) || w.d == w.dend)) {
                            continue mainLoop;
                        }
                        break fail1;
                    case wordchar:
                        if(w.d == w.dend) {break fail1;}
                        if(!IS_A_LETTER(w.string,w.d,w.dend)) {
                            break fail1;
                        }
                        if(ismbchar(w.string[w.d],w.ctx) && w.d + mbclen(w.string[w.d],w.ctx) - 1 < w.dend) {
                            w.d += mbclen(w.string[w.d],w.ctx) - 1;
                        }
                        w.d++;
                        w.SET_REGS_MATCHED();
                        continue mainLoop;
                    case notwordchar:
                        if(w.d == w.dend) {break fail1;}
                        if(IS_A_LETTER(w.string, w.d, w.dend)) {
                            break fail1;
                        }
                        if(ismbchar(w.string[w.d],w.ctx) && w.d + mbclen(w.string[w.d],w.ctx) - 1 < w.dend) {
                            w.d += mbclen(w.string[w.d],w.ctx) - 1;
                        }
                        w.d++;
                        w.SET_REGS_MATCHED();
                        continue mainLoop;
                    case exactn:
                        /* Match the next few pattern characters exactly.
                           mcnt is how many characters to match.  */
                        w.mcnt = w.p[w.pix++];
                        /* This is written out as an if-else so we don't waste time
                           testing `translate' inside the loop.  */
                        if(w.TRANSLATE_P()) {
                            do {
                                if(w.d == w.dend) {break fail1;}
                                if(w.p[w.pix] == 0xff) {
                                    w.pix++;  
                                    if(--w.mcnt==0
                                       || w.d == w.dend
                                       || w.string[w.d++] != w.p[w.pix++]) {
                                        break fail1;
                                    }
                                    continue;
                                }
                                w.c = w.string[w.d++];
                                if(ismbchar(w.c,w.ctx)) {
                                    int n;
                                    if(w.c != w.p[w.pix++]) {
                                        break fail1;
                                    }
                                    for(n = mbclen(w.c,w.ctx) - 1; n > 0; n--) {
                                        if(--w.mcnt==0
                                           || w.d == w.dend
                                           || w.string[w.d++] != w.p[w.pix++]) {
                                            break fail1;
                                        }
                                        continue;
                                    }
                                    /* compiled code translation needed for ruby */
                                    if(w.ctx.translate[w.c] != w.ctx.translate[w.p[w.pix++]]) {
                                        break fail1;
                                    }
                                }
                            } while(--w.mcnt > 0);
                        } else {
                            do {
                                if(w.d == w.dend) {break fail1;}
                                if(w.p[w.pix] == 0xff) {
                                    w.pix++; w.mcnt--;
                                }
                                if(w.string[w.d++] != w.p[w.pix++]) {
                                    break fail1;
                                }
                            } while(--w.mcnt > 0);
                        }
                        w.SET_REGS_MATCHED();
                        continue mainLoop;
                    }

                    continue mainLoop;
                } while(false);
                //fail:
                fail2: do {
                    if(w.stackp != 0) {
                        /* A restart point is known.  Restart there and pop it. */
                        int last_used_reg, this_reg;

                        /* If this failure point is from a dummy_failure_point, just
                           skip it.  */
                        if(w.stackb[w.stackp-4] == -1 || (w.best_regs_set && w.stackb[w.stackp-1] == NON_GREEDY)) {
                            w.POP_FAILURE_POINT();
                            continue fail2;
                        }
                        w.stackp--;		/* discard greedy flag */
                        w.optz = w.stackb[--w.stackp];
                        w.d = w.stackb[--w.stackp];
                        w.pix = w.stackb[--w.stackp];
                        /* Restore register info.  */
                        last_used_reg = w.stackb[--w.stackp];

                        /* Make the ones that weren't saved -1 or 0 again. */
                        for(this_reg = w.num_regs - 1; this_reg > last_used_reg; this_reg--) {
                            regend[this_reg] = REG_UNSET_VALUE;
                            regstart[this_reg] = REG_UNSET_VALUE;
                            reg_info[this_reg].is_active = false;
                            reg_info[this_reg].matched_something = false;
                        }

                        /* And restore the rest from the stack.  */
                        for( ; this_reg > 0; this_reg--) {
                            reg_info[this_reg].word = (char)w.stackb[--w.stackp];
                            regend[this_reg] = w.stackb[--w.stackp];
                            regstart[this_reg] = w.stackb[--w.stackp];
                        }
                        w.mcnt = w.stackb[--w.stackp];
                        while(w.mcnt-->0) {
                            int ptr = w.stackb[--w.stackp];
                            int count = w.stackb[--w.stackp];
                            STORE_NUMBER(w.p, ptr, count);
                        }
                        if(w.pix < w.pend) {
                            int is_a_jump_n = 0;
                            int failed_paren = 0;

                            w.p1 = w.pix;
                            /* If failed to a backwards jump that's part of a repetition
                               loop, need to pop this failure point and use the next one.  */
                            switch(w.p[w.p1]) {
                            case jump_n:
                            case finalize_push_n:
                                is_a_jump_n = 1;
                            case maybe_finalize_jump:
                            case finalize_jump:
                            case finalize_push:
                            case jump:
                                w.p1++;
                                w.mcnt = EXTRACT_NUMBER(w.p,w.p1);
                                w.p1+=2;
                                if(w.mcnt >= 0) {
                                    break;	/* should be backward jump */
                                }
                                w.p1 += w.mcnt;
                                if((is_a_jump_n!=0 && w.p[w.p1] == succeed_n) ||
                                   (is_a_jump_n==0 && w.p[w.p1] == on_failure_jump)) {
                                    if(failed_paren!=0) {
                                        w.p1++;
                                        w.mcnt = EXTRACT_NUMBER(w.p, w.p1);
                                        w.p1+=2;

                                        w.PUSH_FAILURE_POINT(w.p1+w.mcnt,w.d);
                                        w.stackb[w.stackp-1] = NON_GREEDY;
                                    }
                                    continue fail2;
                                }
                                continue mainLoop;
                            default:
                                /* do nothing */;
                                continue mainLoop;
                            }
                        }
                        continue mainLoop;
                    } else {
                        break mainLoop;   /* Matching at this starting point really fails.  */
                    }
                } while(true);
            }        

            if(w.best_regs_set) {
                gotoRestoreBestRegs=true;
                w.d = best_regend[0];

                for(w.mcnt = 0; w.mcnt < w.num_regs; w.mcnt++) {
                    regstart[w.mcnt] = best_regstart[w.mcnt];
                    regend[w.mcnt] = best_regend[w.mcnt];
                }
                continue restore_best_regs2;
            }
        } while(false);

        return -1;
    }


    /**
     * @mri slow_match
     */
    public boolean slow_match(char[] little, int littleix, int lend, char[] big, int bigix, int bend, char[] translate) {
        while(littleix < lend && bigix < bend) {
            char c = little[littleix++];
            if(c == 0xff) {
                c = little[littleix++];
            }
            if(!(translate != null ? translate[big[bigix++]]==translate[c] : big[bigix++]==c)) { 
                break;
            }
        }
        return littleix == lend;
    }

    /**
     * @mri slow_search
     */
    public int slow_search(char[] little, int littleix, int llen, char[] big, int bigix, int blen, char[] translate) {
        int bsave = bigix;
        int bend = bigix+blen;
        boolean fescape = false;

        char c = little[littleix];

        if(c == 0xff) {
            c = little[littleix+1];
            fescape = true;
        } else if(translate!=null && !ismbchar(c,ctx)) {
            c = translate[c];
        }

        while(bigix < bend) {
            /* look for first character */
            if(fescape) {
                while(bigix < bend) {
                    if(big[bigix] == c) {
                        break;
                    }
                    bigix++;
                }
            } else if(translate!=null && !ismbchar(c,ctx)) {
                while(bigix < bend) {
                    if(ismbchar(big[bigix],ctx)) {
                        bigix+=mbclen(big[bigix],ctx)-1;
                    } else if(translate[big[bigix]] == c) {
                        break;
                    }
                    bigix++;
                }
            } else {
                while(bigix < bend) {
                    if(big[bigix] == c) {
                        break;
                    }
                    if(ismbchar(big[bigix],ctx)) {
                        bigix+=mbclen(big[bigix],ctx)-1;
                    }
                    bigix++;
                }
            }

            if(slow_match(little, littleix, littleix+llen, big, bigix, bend, translate)) {
                return bigix - bsave;
            }

            if(bigix<bend) {
                bigix+=mbclen(big[bigix],ctx);
            }
        }
        return -1;
    }

    /**
     * @mri bm_search
     */
    public int bm_search(char[] little, int littleix, int llen, char[] big, int bigix, int blen, int[] skip, char[] translate) {
        int i, j, k;
        i = llen-1;
        if(translate != null) {
            while(i < blen) {
                k = i;
                j = llen-1;
                while(j >= 0 && translate[big[bigix+k]] == translate[little[littleix+j]]) {
                    k--;
                    j--;
                }
                if(j < 0) {
                    return k+1;
                }
                i += skip[translate[big[bigix+i]]];
            }
            return -1;
        }
        while(i < blen) {
            k = i;
            j = llen-1;
            while(j >= 0 && big[bigix+k] == little[littleix+j]) {
                k--;
                j--;
            }
            if(j < 0) {
                return k+1;
            }
            i += skip[big[bigix+i]];
        }
        return -1;
    }

    public final boolean TRANSLATE_P(long optz) {
        return ((optz&RE_OPTION_IGNORECASE)!=0 && ctx.translate!=null);
    }

    /**
     * @mri re_compile_fastmap
     */
    private final void compile_fastmap() {
        int size = used;
        char[] p = buffer;
        int pix = 0;
        int pend = size;
        int j,k;
        int is_a_succeed_n;
        
        int[] stacka = new int[160];
        int[] stackb = stacka;
        int stackp = 0;
        int stacke = 160;
        long optz = options;

        Arrays.fill(fastmap, 0, 256, (char)0);

        fastmap_accurate = 1;
        can_be_null = 0;

        while(p[pix] != 0) {
            is_a_succeed_n = 0;
            if(pix == pend) {
                can_be_null = 1;
                break;
            }
            switch(p[pix++]) {
            case exactn:
                if(p[pix+1] == 0xff) {
                    if(TRANSLATE_P(optz)) {
                        fastmap[ctx.translate[p[pix+2]]] = 2;
                    } else {
                        fastmap[p[pix+2]] = 2;
                    }
                    options |= RE_OPTIMIZE_BMATCH;
                } else if(TRANSLATE_P(optz)) {
                    fastmap[ctx.translate[p[pix+1]]] = 1;
                } else {
                    fastmap[p[pix+1]] = 1;
                }
                break;
            case begline:
            case begbuf:
            case begpos:
            case endbuf:
            case endbuf2:
            case wordbound:
            case notwordbound:
            case wordbeg:
            case wordend:
            case pop_and_fail:
            case push_dummy_failure:
            case start_paren:
            case stop_paren:
                continue;
            case casefold_on:
                options |= RE_MAY_IGNORECASE;
                optz |= RE_OPTION_IGNORECASE;
                continue;
            case casefold_off:
                optz &= ~RE_OPTION_IGNORECASE;
                continue;
            case option_set:
                options = p[pix++];
                continue;
            case endline:
                if(TRANSLATE_P(optz)) {
                    fastmap[ctx.translate['\n']] = 1;
                } else {
                    fastmap['\n'] = 1;
                }
                if((optz & RE_OPTION_SINGLELINE) == 0 && can_be_null == 0) {
                    can_be_null = 2;
                }
                break;
            case jump_n:
            case finalize_jump:
            case maybe_finalize_jump:
            case jump:
            case jump_past_alt:
            case dummy_failure_jump:
            case finalize_push:
            case finalize_push_n:
                j = EXTRACT_NUMBER(p,pix);
                pix += 2;
                pix += j;	
                if(j > 0) {
                    continue;
                }
                /* Jump backward reached implies we just went through
                   the body of a loop and matched nothing.
                   Opcode jumped to should be an on_failure_jump.
                   Just treat it like an ordinary jump.
                   For a * loop, it has pushed its failure point already;
                   If so, discard that as redundant.  */
                if(p[pix] != on_failure_jump && p[pix] != try_next && p[pix] != succeed_n) {
                    continue;
                }
                pix++;
                j = EXTRACT_NUMBER(p,pix);
                pix+=2;
                pix += j;
                if(stackp != 0 && stackb[stackp] == pix) {
                    stackp--;		/* pop */
                }
                continue;
            case try_next:
            case start_nowidth:
            case stop_nowidth:
            case stop_backtrack:
                pix += 2;
                continue;
            case succeed_n:
                is_a_succeed_n = 1;
                /* Get to the number of times to succeed.  */
                k = EXTRACT_NUMBER(p, pix + 2);
                /* Increment p past the n for when k != 0.  */
                if(k != 0) {
                    pix += 4;
                    continue;
                }
                /* fall through */
            case on_failure_jump:
                j = EXTRACT_NUMBER(p,pix);
                pix += 2;
                if(pix + j < pend) {
                    if(stackp == stacke) {
                        int[] stackx;
                        int xlen = stacke;
                        stackx = new int[2*xlen];
                        System.arraycopy(stackb,0,stackx,0,xlen);
                        stackb = stackx;
                        stacke = 2*xlen;
                    }
                    stackb[++stackp] = pix + j;	/* push */
                } else {
                    can_be_null = 1;
                }
                if(is_a_succeed_n>0) {
                    k = EXTRACT_NUMBER(p,pix);	/* Skip the n.  */
                    pix += 2;
                }
                continue;
            case set_number_at:
                pix += 4;
                continue;
            case start_memory:
            case stop_memory:
                pix += 2;
                continue;
            case duplicate:
                can_be_null = 1;
                if(p[pix] >= re_nsub) {
                    break;
                }
                fastmap['\n'] = 1;
            case anychar_repeat:
            case anychar:
                for(j = 0; j < 256; j++) {
                    if(j != '\n' || ((optz & RE_OPTION_MULTILINE)) != 0) {
                        fastmap[j] = 1;
                    }
                }
                if(can_be_null!=0) {
                    return;
                }
                /* Don't return; check the alternative paths
                   so we can set can_be_null if appropriate.  */
                if(p[pix-1] == anychar_repeat) {
                    continue;
                }
                break;
      case wordchar:
          for(j = 0; j < 0x80; j++) {
              if(re_syntax_table[j] == Sword) {
                  fastmap[j] = 1;
              }
          }
          switch(ctx.current_mbctype) {
          case MBCTYPE_ASCII:
              for(j = 0x80; j < 256; j++) {
                  if(re_syntax_table[j] == Sword2) {
                      fastmap[j] = 1;
                  }
              }
              break;
          case MBCTYPE_EUC:
          case MBCTYPE_SJIS:
          case MBCTYPE_UTF8:
              for(j = 0x80; j < 256; j++) {
                  if(ctx.re_mbctab[j] != 0) {
                      fastmap[j] = 1;
                  }
              }
              break;
          }
          break;
      case notwordchar:
          for(j = 0; j < 0x80; j++) {
              if(re_syntax_table[j] != Sword) {
                  fastmap[j] = 1;
              }
          }
          switch(ctx.current_mbctype) {
          case MBCTYPE_ASCII:
              for(j = 0x80; j < 256; j++) {
                  if(re_syntax_table[j] != Sword2) {
                      fastmap[j] = 1;
                  }
              }
              break;
          case MBCTYPE_EUC:
          case MBCTYPE_SJIS:
          case MBCTYPE_UTF8:
              for(j = 0x80; j < 256; j++) {
                  if(ctx.re_mbctab[j] == 0) {
                      fastmap[j] = 1;
                  }
              }
              break;
          }
          break;
      case charset:
          /* NOTE: Charset for single-byte chars never contain
             multi-byte char.  See set_list_bits().  */
          for(j = p[pix++] * 8 - 1; j >= 0; j--) {
              if((p[pix + j / 8] & (1 << (j % 8))) != 0) {
                  int tmp = TRANSLATE_P(optz)?ctx.translate[j]:j;
                  fastmap[tmp] = 1;
              }
          }
          {
              int c, beg, end;

              pix += p[pix-1] + 2;
              size = EXTRACT_UNSIGNED(p,pix-2);
              for(j = 0; j < size; j++) {
                  c = EXTRACT_MBC(p,pix+j*8);
                  beg = WC2MBC1ST(c);
                  c = EXTRACT_MBC(p,pix+j*8+4);
                  end = WC2MBC1ST(c);
                  /* set bits for 1st bytes of multi-byte chars.  */
                  while(beg <= end) {
                      /* NOTE: Charset for multi-byte chars might contain
                         single-byte chars.  We must reject them. */
                      if(c < 0x100) {
                          fastmap[beg] = 2;
                          options |= RE_OPTIMIZE_BMATCH;
                      } else if(ismbchar((char)beg,ctx)) {
                          fastmap[beg] = 1;
                      }
                      beg++;
                  }
              }
          }
          break;
            case charset_not:
                /* S: set of all single-byte chars.
                   M: set of all first bytes that can start multi-byte chars.
                   s: any set of single-byte chars.
                   m: any set of first bytes that can start multi-byte chars.
                   
                   We assume S+M = U.
                   ___      _   _
                   s+m = (S*s+M*m).  */
                /* Chars beyond end of map must be allowed */
                /* NOTE: Charset_not for single-byte chars might contain
                   multi-byte chars.  See set_list_bits(). */
                for(j = p[pix] * 8; j < 256; j++) {
                    if(!ismbchar(j,ctx)) {
                        fastmap[j] = 1;
                    }
                }

                for(j = p[pix++] * 8 - 1; j >= 0; j--) {
                    if((p[pix + j / 8] & (1 << (j % 8))) == 0) {
                        if(!ismbchar(j,ctx)) {
                            fastmap[j] = 1;
                        }
                    }
                }
                {
                    long c, beg;
                    int num_literal = 0;
                    pix += p[pix-1] + 2;
                    size = EXTRACT_UNSIGNED(p,pix-2);
                    if(size == 0) {
                        for(j = 0x80; j < 256; j++) {
                            if(ismbchar(j,ctx)) {
                                fastmap[j] = 1;
                            }
                        }
                        break;
                    }
                    for(j = 0,c = 0;j < size; j++) {
                        int cc = EXTRACT_MBC(p,pix+j*8);
                        beg = WC2MBC1ST(cc);
                        while(c <= beg) {
                            if(ismbchar((int)c, ctx)) {
                                fastmap[(int)c] = 1;
                            }
                            c++;
                        }
                        cc = EXTRACT_MBC(p,pix+j*8+4);
                        if(cc < 0xff) {
                            num_literal = 1;
                            while(c <= cc) {
                                if(ismbchar((int)c, ctx)) {
                                    fastmap[(int)c] = 1;
                                }
                                c++;
                            }
                        }
                        c = WC2MBC1ST(cc);
                    }

                    for(j = (int)c; j < 256; j++) {
                        if(num_literal != 0) {
                            fastmap[j] = 1;
                        }
                        if(ismbchar(j,ctx)) {
                            fastmap[j] = 1;
                        }
                    }
                }
                break;
            }
            /* Get here means we have successfully found the possible starting
               characters of one path of the pattern.  We need not follow this
               path any farther.  Instead, look at the next alternative
               remembered in the stack.  */
            if(stackp != 0) {
                pix = stackb[stackp--];		/* pop */
            } else {
                break;
            }
        }
    }


















    /* Functions for multi-byte support.
       Created for grep multi-byte extension Jul., 1993 by t^2 (Takahiro Tanimoto)
       Last change: Jul. 9, 1993 by t^2  */
    private static final char[] mbctab_ascii = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    private static final char[] mbctab_euc = { /* 0xA1-0xFE */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
    };

    private static final char[] mbctab_sjis = { /* 0x81-0x9F,0xE0-0xFC */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0
    };

    private static final char[] mbctab_sjis_trail = { /* 0x40-0x7E,0x80-0xFC */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0
    };

    private static final char[] mbctab_utf8 = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 0, 0,
    };

    public static void tmain4(String[] args) throws Exception {
        char[] ccc = args[1].toCharArray();
        Registers reg = new Registers();
        System.out.println(Pattern.compile(args[0]).search(ccc,ccc.length,0,ccc.length,reg));
        for(int i=0;i<reg.num_regs;i++) {
            System.err.println("[" + i + "]" + reg.beg[i] + ":" + reg.end[i] + "=" + new String(ccc,reg.beg[i],reg.end[i]-reg.beg[i]));
        }
    }

    public static void tmain1(String[] args) throws Exception {
        Pattern.compile(args[0]);
    }

    public static void main(String[] args) throws Exception {
        System.err.println("Speed test");
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(args[0]));
        java.util.List strings = new java.util.ArrayList();
        String s;
        while((s = reader.readLine()) != null) {
            strings.add(s);
        }
        reader.close();
        String[] sss = (String[])strings.toArray(new String[0]);
        int times = 1000;

        long b1 = System.currentTimeMillis();

        for(int j=0;j<times;j++) {
            for(int i=0;i<sss.length;i++) {
                java.util.regex.Pattern.compile(sss[i]);
            }
        }
        long a1 = System.currentTimeMillis();

        long b2 = System.currentTimeMillis();
        for(int j=0;j<times;j++) {
            for(int i=0;i<sss.length;i++) {
                Pattern.compile(sss[i]);
            }
        }
        long a2 = System.currentTimeMillis();

        System.out.println("Compiling " + (times*sss.length) + " regexps took java.util.regex: " + ((a1-b1)) + "ms");
        System.out.println("Compiling " + (times*sss.length) + " regexps took REJ: " + ((a2-b2)) + "ms");
    }

    public static void tmain3(String[] args) throws Exception {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(args[0]));
        java.util.List strings = new java.util.ArrayList();
        String s;
        while((s = reader.readLine()) != null) {
            strings.add(s);
        }
        reader.close();
        String[] sss = (String[])strings.toArray(new String[0]);

        int times = 13;

        java.util.Map tes = new java.util.TreeMap();

        for(int i=0;i<sss.length;i++) {
            long a1 = System.currentTimeMillis();
            for(int j=0;j<times;j++) {
                Pattern.compile(sss[i]);
            }
            long b1 = System.currentTimeMillis();
            tes.put(new Long(b1-a1),sss[i]);
        }

        for(java.util.Iterator iter = tes.entrySet().iterator();iter.hasNext();) {
            System.err.println(iter.next());
        }
    }

    public final static CompileContext ASCII = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_ASCII, mbctab_ascii);
    public final static CompileContext UTF8 = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_UTF8, mbctab_utf8);
    public final static CompileContext SJIS = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_SJIS, mbctab_sjis);
    public final static CompileContext EUC = new CompileContext(ASCII_TRANSLATE_TABLE, MBCTYPE_EUC, mbctab_euc);
}// Pattern
