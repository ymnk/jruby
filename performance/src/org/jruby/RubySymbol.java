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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Joey Gibson <joey@joeygibson.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Derek Berner <derek.berner@state.nm.us>
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

import java.util.Map;
import java.util.Set;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.collections.NamedIRubyObjectMap;

/**
 *
 * @author  jpetersen
 */
public class RubySymbol extends RubyObject {
    private final String symbol;
    private final int id;
    
    private RubySymbol(Ruby runtime, String symbol) {
        super(runtime, runtime.getSymbol());
        this.symbol = symbol;

        runtime.symbolLastId++;
        this.id = runtime.symbolLastId;
    }
    
    public static RubyClass createSymbolClass(Ruby runtime) {
        RubyClass symbolClass = runtime.defineClass("Symbol", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubySymbol.class);   
        RubyClass symbolMetaClass = symbolClass.getMetaClass();
        symbolClass.index = ClassIndex.SYMBOL;

        
        symbolClass.defineFastMethod("==", callbackFactory.getFastMethod("equal", RubyKernel.IRUBY_OBJECT));
        symbolClass.defineFastMethod("freeze", callbackFactory.getFastMethod("freeze"));
        symbolClass.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));
        symbolClass.defineFastMethod("inspect", callbackFactory.getFastMethod("inspect"));
        symbolClass.defineFastMethod("taint", callbackFactory.getFastMethod("taint"));
        symbolClass.defineFastMethod("to_i", callbackFactory.getFastMethod("to_i"));
        symbolClass.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        symbolClass.defineFastMethod("to_sym", callbackFactory.getFastMethod("to_sym"));
        symbolClass.defineAlias("id2name", "to_s");
        symbolClass.defineAlias("to_int", "to_i");

        symbolMetaClass.defineFastMethod("all_symbols", callbackFactory.getFastSingletonMethod("all_symbols"));

        symbolMetaClass.undefineMethod("new");
        
        return symbolClass;
    }
    
    public int getNativeTypeIndex() {
        return ClassIndex.SYMBOL;
    }

    public static final byte NIL_P_SWITCHVALUE = 1;
    public static final byte EQUALEQUAL_SWITCHVALUE = 2;
    public static final byte TO_S_SWITCHVALUE = 3;
    public static final byte TO_I_SWITCHVALUE = 4;
    public static final byte TO_SYM_SWITCHVALUE = 5;
    public static final byte HASH_SWITCHVALUE = 6;
    public static final byte INSPECT_SWITCHVALUE = 7;
    
    public IRubyObject callMethod(ThreadContext context, RubyModule rubyclass, int methodIndex, String name,
            IRubyObject[] args, CallType callType, Block block) {
        // If tracing is on, don't do STI dispatch
        if (context.getRuntime().hasEventHooks()) return super.callMethod(context, rubyclass, name, args, callType, block);
        
        switch (getRuntime().getSelectorTable().table[rubyclass.index][methodIndex]) {
        case NIL_P_SWITCHVALUE:
            if (args.length != 0) throw context.getRuntime().newArgumentError("wrong number of arguments(" + args.length + " for " + 0 + ")");
            return nil_p();
        case EQUALEQUAL_SWITCHVALUE:
            if (args.length != 1) throw context.getRuntime().newArgumentError("wrong number of arguments(" + args.length + " for " + 1 + ")");
            return equal(args[0]);
        case TO_S_SWITCHVALUE:
            if (args.length != 0) throw context.getRuntime().newArgumentError("wrong number of arguments(" + args.length + " for " + 0 + ")");
            return to_s();
        case TO_I_SWITCHVALUE:
            if (args.length != 0) throw context.getRuntime().newArgumentError("wrong number of arguments(" + args.length + " for " + 0 + ")");
            return to_i();
        case TO_SYM_SWITCHVALUE:
            if (args.length != 0) throw context.getRuntime().newArgumentError("wrong number of arguments(" + args.length + " for " + 0 + ")");
            return to_sym();
        case HASH_SWITCHVALUE:
            if (args.length != 0) throw context.getRuntime().newArgumentError("wrong number of arguments(" + args.length + " for " + 0 + ")");
            return hash();
        case INSPECT_SWITCHVALUE:
            if (args.length != 0) throw context.getRuntime().newArgumentError("wrong number of arguments(" + args.length + " for " + 0 + ")");
            return inspect();
        case 0:
        default:
            return super.callMethod(context, rubyclass, name, args, callType, block);
        }
    }
    

    /** rb_to_id
     * 
     * @return a String representation of the symbol 
     */
    public String asSymbol() {
        return symbol;
    }
    
    /** short circuit for Symbol key comparison
     * 
     */
    public final boolean eql(IRubyObject other) {
        return other == this;
    }

    public boolean isImmediate() {
    	return true;
    }
    
    public static String getSymbol(Ruby runtime, long id) {
        return runtime.getSymbolTable().getSymbol(id);
    }

    /* Symbol class methods.
     * 
     */

    public static RubySymbol newSymbol(Ruby runtime, String name) {
        return runtime.getSymbolTable().newSymbol(name);
    }
    
    public static RubySymbol newSymbol(RubyString name) {
        return name.getRuntime().getSymbolTable().newSymbol(name);
    }

    /**
     * Call this <i>only</i> if you are certain the name has already been intern-ed.
     * Be very, <i>very</i> certain.
     * 
     * @param runtime
     * @param name - the intern-ed name
     * @return
     */
    public static RubySymbol newSymbolNoIntern(Ruby runtime, String name) {
        return runtime.getSymbolTable().newSymbolNoIntern(name);
    }

    public IRubyObject equal(IRubyObject other) {
        // Symbol table ensures only one instance for every name,
        // so object identity is enough to compare symbols.
        return RubyBoolean.newBoolean(getRuntime(), this == other);
    }

    public RubyFixnum to_i() {
        return getRuntime().newFixnum(id);
    }

    public IRubyObject inspect() {
        return getRuntime().newString(":" + 
            (isSymbolName(symbol) ? symbol : getRuntime().newString(symbol).dump().toString())); 
    }

    public IRubyObject to_s() {
        return getRuntime().newString(symbol);
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }
    
    public int hashCode() {
        return id;
    }
    
    public boolean equals(Object other) {
        return other == this;
    }
    
    public IRubyObject to_sym() {
        return this;
    }

    public IRubyObject freeze() {
        return this;
    }

    public IRubyObject taint() {
        return this;
    }
    
    private static boolean isIdentStart(char c) {
        return ((c >= 'a' && c <= 'z')|| (c >= 'A' && c <= 'Z')
                || c == '_');
    }
    private static boolean isIdentChar(char c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
                || c == '_');
    }
    
    private static boolean isIdentifier(String s) {
        if (s == null || s.length() <= 0) {
            return false;
        } 
        
        if (!isIdentStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!isIdentChar(s.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * is_special_global_name from parse.c.  
     * @param s
     * @return
     */
    private static boolean isSpecialGlobalName(String s) {
        if (s == null || s.length() <= 0) {
            return false;
        }

        int length = s.length();
           
        switch (s.charAt(0)) {        
        case '~': case '*': case '$': case '?': case '!': case '@': case '/': case '\\':        
        case ';': case ',': case '.': case '=': case ':': case '<': case '>': case '\"':        
        case '&': case '`': case '\'': case '+': case '0':
            return length == 1;            
        case '-':
            return (length == 1 || (length == 2 && isIdentChar(s.charAt(1))));
            
        default:
            // we already confirmed above that length > 0
            for (int i = 0; i < length; i++) {
                if (!Character.isDigit(s.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private static boolean isSymbolName(String s) {
        int length;
        if (s == null || (length = s.length()) == 0) {
            return false;
        }

        char c = s.charAt(0);
        switch (c) {
        case '$':
            return length > 1 && isSpecialGlobalName(s.substring(1));
        case '@':
            int offset = 1;
            if (length >= 2 && s.charAt(1) == '@') {
                offset++;
            }

            return isIdentifier(s.substring(offset));
        case '<':
            return (length == 1 || (length == 2 && (s.equals("<<") || s.equals("<="))) || 
                    (length == 3 && s.equals("<=>")));
        case '>':
            return (length == 1) || (length == 2 && (s.equals(">>") || s.equals(">=")));
        case '=':
            return ((length == 2 && (s.equals("==") || s.equals("=~"))) || 
                    (length == 3 && s.equals("===")));
        case '*':
            return (length == 1 || (length == 2 && s.equals("**")));
        case '+':
            return (length == 1 || (length == 2 && s.equals("+@")));
        case '-':
            return (length == 1 || (length == 2 && s.equals("-@")));
        case '|': case '^': case '&': case '/': case '%': case '~': case '`':
            return length == 1;
        case '[':
            return s.equals("[]") || s.equals("[]=");
        }
        
        if (!isIdentStart(c)) {
            return false;
        }

        boolean localID = (c >= 'a' && c <= 'z');
        int last = 1;
        
        for (; last < length; last++) {
            char d = s.charAt(last);
            
            if (!isIdentChar(d)) {
                break;
            }
        }
                    
        if (last == length) {
            return true;
        } else if (localID && last == length - 1) {
            char d = s.charAt(last);
            
            return d == '!' || d == '?' || d == '=';
        }
        
        return false;
    }
    
    public static IRubyObject all_symbols(IRubyObject recv) {
        return recv.getRuntime().getSymbolTable().allSymbols();
    }

    public static RubySymbol unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubySymbol result = RubySymbol.newSymbol(input.getRuntime(), RubyString.byteListToString(input.unmarshalString()));
        input.registerLinkTarget(result);
        return result;
    }

    public static class SymbolTable extends NamedIRubyObjectMap {
        private static final int INITIAL_SYMBOL_TABLE_SIZE = 1024;

        private Ruby runtime;

        public SymbolTable(Ruby runtime) {
            super(INITIAL_SYMBOL_TABLE_SIZE);
            this.runtime = runtime;
        }
        
        public SymbolTable(Ruby runtime, int initialTableSize) {
            super(initialTableSize);
            this.runtime = runtime;
        }
        
        public synchronized RubySymbol newSymbol(String name) {
            RubySymbol symbol;
            Entry entry = getOrAddEntry(name = name.intern());
            if ((symbol = (RubySymbol)entry.getObject()) == null) {
                entry.setObject(symbol = new RubySymbol(runtime, name));
            }
            return symbol;
        }
        
        public RubySymbol newSymbol(RubyString name) {
            return newSymbolNoIntern(name.asSymbol());
        }
        
        /**
         * Call this <i>only</i> if you are certain the name has already been intern-ed.
         * @param name
         * @return
         */
        public synchronized RubySymbol newSymbolNoIntern(String name) {
            RubySymbol symbol;
            Entry entry = getOrAddEntry(name);
            if ((symbol = (RubySymbol)entry.getObject()) == null) {
                entry.setObject(symbol = new RubySymbol(runtime, name));
            }
            return symbol;
        }
        
        public synchronized void store(RubySymbol symbol) {
            Entry entry = getOrAddEntry(symbol.asSymbol());
            if (entry.getObject() == null) {
                entry.setObject(symbol);
            }
        }
        
        public synchronized RubySymbol lookup(String name) {
            Entry entry;
            if ((entry = getEntry(name.intern())) != null) {
                return (RubySymbol)entry.getValue();
            }
            return null;
        }
        
        public String getSymbol(long id) {
            // getValueArray is synchronized
            final IRubyObject[] symbols = getValueArray();
            for (int i = symbols.length; --i >= 0; ) {
                if (id == ((RubySymbol)symbols[i]).id) {
                    return ((RubySymbol)symbols[i]).symbol;
                }
            }
            return null;
        }

        public RubySymbol lookup(long id) {
            // getValueArray is synchronized
            final IRubyObject[] symbols = getValueArray();
            for (int i = symbols.length; --i >= 0; ) {
                if (id == ((RubySymbol)symbols[i]).id) {
                    return (RubySymbol)symbols[i];
                }
            }
            return null;
        }

        public IRubyObject allSymbols() {
            // getValueArray is synchronized
            return runtime.newArrayNoCopy(getValueArray());            
        }

        public IRubyObject[] all_symbols() {
            // getValueArray is synchronized
            return getValueArray();
        }
        

        // disable the following NIROM methods:
        // TODO: stripped-down version (superclass?) of NIROM?
        public Set entrySet() {
            throw new UnsupportedOperationException();
        }
        public IRubyObject put(String name, IRubyObject value) {
            throw new UnsupportedOperationException();
        }
        public IRubyObject putNoIntern(String name, IRubyObject value) {
            throw new UnsupportedOperationException();
        }
        public synchronized void putAll(final Map map) {
            throw new UnsupportedOperationException();
        }
        public synchronized void putAll(final NamedIRubyObjectMap map) {
            throw new UnsupportedOperationException();
        }
        public void clear() {
            throw new UnsupportedOperationException();
        }
        protected Entry removeMapping(final Object o) {
            throw new UnsupportedOperationException();
        }
        protected Entry removeEntryForKey(final String key) {
            throw new UnsupportedOperationException();
        }
    }
    
}
