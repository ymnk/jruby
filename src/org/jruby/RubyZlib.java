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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Dave Brosius <dbrosius@mebigfatguy.com>
 * Copyright (C) 2006 Peter K Chan <peter@oaktop.com>
 * Copyright (C) 2009 Aurelian Oancea <aurelian@locknet.ro>
 * Copyright (C) 2009 Vladimir Sizikov <vsizikov@gmail.com>
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.joda.time.DateTime;

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyModule;

import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;

import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ext.zlib.Zlib.*;

import org.jruby.util.Adler32Ext;
import org.jruby.util.ByteList;
import org.jruby.util.CRC32Ext;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.io.Stream;

import static org.jruby.CompatVersion.*;
import org.jruby.ext.zlib.Util;

@JRubyModule(name="Zlib")
public class RubyZlib {
    // version
    public final static String ZLIB_VERSION = "1.2.3.3";
    public final static String VERSION = "0.6.0";

    
    /** Create the Zlib module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createZlibModule(Ruby runtime) {
        RubyModule mZlib = runtime.defineModule("Zlib");
        mZlib.defineAnnotatedMethods(RubyZlib.class);

        RubyClass cStandardError = runtime.getStandardError();
        RubyClass cZlibError = mZlib.defineClassUnder("Error", cStandardError, cStandardError.getAllocator());
        mZlib.defineClassUnder("StreamEnd", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("StreamError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("BufError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("NeedDict", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("MemError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("VersionError", cZlibError, cZlibError.getAllocator());
        mZlib.defineClassUnder("DataError", cZlibError, cZlibError.getAllocator());

        RubyClass cGzFile = mZlib.defineClassUnder("GzipFile", runtime.getObject(), RubyGzipFile.GZIPFILE_ALLOCATOR);
        cGzFile.defineAnnotatedMethods(RubyGzipFile.class);

        cGzFile.defineClassUnder("Error", cZlibError, cZlibError.getAllocator());
        RubyClass cGzError = cGzFile.defineClassUnder("Error", cZlibError, cZlibError.getAllocator());
        cGzFile.defineClassUnder("CRCError", cGzError, cGzError.getAllocator());
        cGzFile.defineClassUnder("NoFooter", cGzError, cGzError.getAllocator());
        cGzFile.defineClassUnder("LengthError", cGzError, cGzError.getAllocator());

        RubyClass cGzReader = mZlib.defineClassUnder("GzipReader", cGzFile, RubyGzipReader.GZIPREADER_ALLOCATOR);
        cGzReader.includeModule(runtime.getEnumerable());
        cGzReader.defineAnnotatedMethods(RubyGzipReader.class);

        RubyClass cGzWriter = mZlib.defineClassUnder("GzipWriter", cGzFile, RubyGzipWriter.GZIPWRITER_ALLOCATOR);
        cGzWriter.defineAnnotatedMethods(RubyGzipWriter.class);

        mZlib.defineConstant("ZLIB_VERSION", runtime.newString(ZLIB_VERSION));
        mZlib.defineConstant("VERSION", runtime.newString(VERSION));

        mZlib.defineConstant("BINARY", runtime.newFixnum(Z_BINARY));
        mZlib.defineConstant("ASCII", runtime.newFixnum(Z_ASCII));
        mZlib.defineConstant("UNKNOWN", runtime.newFixnum(Z_UNKNOWN));

        mZlib.defineConstant("DEF_MEM_LEVEL", runtime.newFixnum(8));
        mZlib.defineConstant("MAX_MEM_LEVEL", runtime.newFixnum(9));

        mZlib.defineConstant("OS_UNIX", runtime.newFixnum(OS_UNIX));
        mZlib.defineConstant("OS_UNKNOWN", runtime.newFixnum(OS_UNKNOWN));
        mZlib.defineConstant("OS_CODE", runtime.newFixnum(OS_CODE));
        mZlib.defineConstant("OS_ZSYSTEM", runtime.newFixnum(OS_ZSYSTEM));
        mZlib.defineConstant("OS_VMCMS", runtime.newFixnum(OS_VMCMS));
        mZlib.defineConstant("OS_VMS", runtime.newFixnum(OS_VMS));
        mZlib.defineConstant("OS_RISCOS", runtime.newFixnum(OS_RISCOS));
        mZlib.defineConstant("OS_MACOS", runtime.newFixnum(OS_MACOS));
        mZlib.defineConstant("OS_OS2", runtime.newFixnum(OS_OS2));
        mZlib.defineConstant("OS_AMIGA", runtime.newFixnum(OS_AMIGA));
        mZlib.defineConstant("OS_QDOS", runtime.newFixnum(OS_QDOS));
        mZlib.defineConstant("OS_WIN32", runtime.newFixnum(OS_WIN32));
        mZlib.defineConstant("OS_ATARI", runtime.newFixnum(OS_ATARI));
        mZlib.defineConstant("OS_MSDOS", runtime.newFixnum(OS_MSDOS));
        mZlib.defineConstant("OS_CPM", runtime.newFixnum(OS_CPM));
        mZlib.defineConstant("OS_TOPS20", runtime.newFixnum(OS_TOPS20));

        mZlib.defineConstant("DEFAULT_STRATEGY", runtime.newFixnum(Z_DEFAULT_STRATEGY));
        mZlib.defineConstant("FILTERED", runtime.newFixnum(Z_FILTERED));
        mZlib.defineConstant("HUFFMAN_ONLY", runtime.newFixnum(Z_HUFFMAN_ONLY));

        mZlib.defineConstant("NO_FLUSH", runtime.newFixnum(Z_NO_FLUSH));
        mZlib.defineConstant("SYNC_FLUSH", runtime.newFixnum(Z_SYNC_FLUSH));
        mZlib.defineConstant("FULL_FLUSH", runtime.newFixnum(Z_FULL_FLUSH));
        mZlib.defineConstant("FINISH", runtime.newFixnum(Z_FINISH));

        mZlib.defineConstant("NO_COMPRESSION", runtime.newFixnum(Z_NO_COMPRESSION));
        mZlib.defineConstant("BEST_SPEED", runtime.newFixnum(Z_BEST_SPEED));
        mZlib.defineConstant("DEFAULT_COMPRESSION", runtime.newFixnum(Z_DEFAULT_COMPRESSION));
        mZlib.defineConstant("BEST_COMPRESSION", runtime.newFixnum(Z_BEST_COMPRESSION));

        mZlib.defineConstant("MAX_WBITS", runtime.newFixnum(MAX_WBITS));

        // ZStream actually *isn't* allocatable
        RubyClass cZStream = mZlib.defineClassUnder("ZStream", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        cZStream.defineAnnotatedMethods(ZStream.class);
        cZStream.undefineMethod("new");

        RubyClass cInflate = mZlib.defineClassUnder("Inflate", cZStream, Inflate.INFLATE_ALLOCATOR);
        cInflate.defineAnnotatedMethods(Inflate.class);

        RubyClass cDeflate = mZlib.defineClassUnder("Deflate", cZStream, Deflate.DEFLATE_ALLOCATOR);
        cDeflate.defineAnnotatedMethods(Deflate.class);

        runtime.getKernel().callMethod(runtime.getCurrentContext(), "require", runtime.newString("stringio"));

        return mZlib;
    }

    @JRubyClass(name="Zlib::Error", parent="StandardError")
    public static class Error {}
    @JRubyClass(name="Zlib::StreamEnd", parent="Zlib::Error")
    public static class StreamEnd extends Error {}
    @JRubyClass(name="Zlib::StreamError", parent="Zlib::Error")
    public static class StreamError extends Error {}
    @JRubyClass(name="Zlib::BufError", parent="Zlib::Error")
    public static class BufError extends Error {}
    @JRubyClass(name="Zlib::NeedDict", parent="Zlib::Error")
    public static class NeedDict extends Error {}
    @JRubyClass(name="Zlib::MemError", parent="Zlib::Error")
    public static class MemError extends Error {}
    @JRubyClass(name="Zlib::VersionError", parent="Zlib::Error")
    public static class VersionError extends Error {}
    @JRubyClass(name="Zlib::DataError", parent="Zlib::Error")
    public static class DataError extends Error {}

    @JRubyMethod(name = "zlib_version", module = true, visibility = PRIVATE)
    public static IRubyObject zlib_version(IRubyObject recv) {
        RubyBasicObject res = (RubyBasicObject) ((RubyModule)recv).getConstant("ZLIB_VERSION");
        // MRI behavior, enforced by tests
        res.taint(recv.getRuntime());
        return res;
    }

    @JRubyMethod(name = "crc32", optional = 2, module = true, visibility = PRIVATE)
    public static IRubyObject crc32(IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(recv.getRuntime(),args,0,2);
        long crc = 0;
        ByteList bytes = null;
        
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) crc = RubyNumeric.num2long(args[1]);

        CRC32Ext ext = new CRC32Ext((int)crc);
        if (bytes != null) {
            ext.update(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        }
        
        return recv.getRuntime().newFixnum(ext.getValue());
    }

    @JRubyMethod(name = "adler32", optional = 2, module = true, visibility = PRIVATE)
    public static IRubyObject adler32(IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(recv.getRuntime(),args,0,2);
        int adler = 1;
        ByteList bytes = null;
        if (!args[0].isNil()) bytes = args[0].convertToString().getByteList();
        if (!args[1].isNil()) adler = RubyNumeric.fix2int(args[1]);

        Adler32Ext ext = new Adler32Ext(adler);
        if (bytes != null) {
            ext.update(bytes.getUnsafeBytes(), bytes.begin(), bytes.length()); // it's safe since adler.update doesn't modify the array
        }
        return recv.getRuntime().newFixnum(ext.getValue());
    }

    private final static long[] crctab = new long[]{
        0L, 1996959894L, 3993919788L, 2567524794L, 124634137L, 1886057615L, 3915621685L, 2657392035L, 249268274L, 2044508324L, 3772115230L, 2547177864L, 162941995L, 
        2125561021L, 3887607047L, 2428444049L, 498536548L, 1789927666L, 4089016648L, 2227061214L, 450548861L, 1843258603L, 4107580753L, 2211677639L, 325883990L, 
        1684777152L, 4251122042L, 2321926636L, 335633487L, 1661365465L, 4195302755L, 2366115317L, 997073096L, 1281953886L, 3579855332L, 2724688242L, 1006888145L, 
        1258607687L, 3524101629L, 2768942443L, 901097722L, 1119000684L, 3686517206L, 2898065728L, 853044451L, 1172266101L, 3705015759L, 2882616665L, 651767980L, 
        1373503546L, 3369554304L, 3218104598L, 565507253L, 1454621731L, 3485111705L, 3099436303L, 671266974L, 1594198024L, 3322730930L, 2970347812L, 795835527L, 
        1483230225L, 3244367275L, 3060149565L, 1994146192L, 31158534L, 2563907772L, 4023717930L, 1907459465L, 112637215L, 2680153253L, 3904427059L, 2013776290L, 
        251722036L, 2517215374L, 3775830040L, 2137656763L, 141376813L, 2439277719L, 3865271297L, 1802195444L, 476864866L, 2238001368L, 4066508878L, 1812370925L, 
        453092731L, 2181625025L, 4111451223L, 1706088902L, 314042704L, 2344532202L, 4240017532L, 1658658271L, 366619977L, 2362670323L, 4224994405L, 1303535960L, 
        984961486L, 2747007092L, 3569037538L, 1256170817L, 1037604311L, 2765210733L, 3554079995L, 1131014506L, 879679996L, 2909243462L, 3663771856L, 1141124467L, 
        855842277L, 2852801631L, 3708648649L, 1342533948L, 654459306L, 3188396048L, 3373015174L, 1466479909L, 544179635L, 3110523913L, 3462522015L, 1591671054L, 
        702138776L, 2966460450L, 3352799412L, 1504918807L, 783551873L, 3082640443L, 3233442989L, 3988292384L, 2596254646L, 62317068L, 1957810842L, 3939845945L, 
        2647816111L, 81470997L, 1943803523L, 3814918930L, 2489596804L, 225274430L, 2053790376L, 3826175755L, 2466906013L, 167816743L, 2097651377L, 4027552580L, 
        2265490386L, 503444072L, 1762050814L, 4150417245L, 2154129355L, 426522225L, 1852507879L, 4275313526L, 2312317920L, 282753626L, 1742555852L, 4189708143L, 
        2394877945L, 397917763L, 1622183637L, 3604390888L, 2714866558L, 953729732L, 1340076626L, 3518719985L, 2797360999L, 1068828381L, 1219638859L, 3624741850L, 
        2936675148L, 906185462L, 1090812512L, 3747672003L, 2825379669L, 829329135L, 1181335161L, 3412177804L, 3160834842L, 628085408L, 1382605366L, 3423369109L, 
        3138078467L, 570562233L, 1426400815L, 3317316542L, 2998733608L, 733239954L, 1555261956L, 3268935591L, 3050360625L, 752459403L, 1541320221L, 2607071920L, 
        3965973030L, 1969922972L, 40735498L, 2617837225L, 3943577151L, 1913087877L, 83908371L, 2512341634L, 3803740692L, 2075208622L, 213261112L, 2463272603L, 
        3855990285L, 2094854071L, 198958881L, 2262029012L, 4057260610L, 1759359992L, 534414190L, 2176718541L, 4139329115L, 1873836001L, 414664567L, 2282248934L, 
        4279200368L, 1711684554L, 285281116L, 2405801727L, 4167216745L, 1634467795L, 376229701L, 2685067896L, 3608007406L, 1308918612L, 956543938L, 2808555105L, 
        3495958263L, 1231636301L, 1047427035L, 2932959818L, 3654703836L, 1088359270L, 936918000L, 2847714899L, 3736837829L, 1202900863L, 817233897L, 3183342108L, 
        3401237130L, 1404277552L, 615818150L, 3134207493L, 3453421203L, 1423857449L, 601450431L, 3009837614L, 3294710456L, 1567103746L, 711928724L, 3020668471L, 
        3272380065L, 1510334235L, 755167117};

    @JRubyMethod(name = "crc_table", module = true, visibility = PRIVATE)
    public static IRubyObject crc_table(IRubyObject recv) {
        List<IRubyObject> ll = new ArrayList<IRubyObject>(crctab.length);
        for(int i=0;i<crctab.length;i++) {
            ll.add(recv.getRuntime().newFixnum(crctab[i]));
        }
        return recv.getRuntime().newArray(ll);
    }

    @JRubyClass(name="Zlib::ZStream")
    public static abstract class ZStream extends RubyObject {
        protected boolean closed = false;

        protected abstract int internalTotalIn();
        protected abstract int internalTotalOut();
        protected abstract boolean internalStreamEndP();
        protected abstract void internalReset();
        protected abstract boolean internalFinished();
        protected abstract long internalAdler();

        // TODO: eliminate?
        protected abstract IRubyObject internalFinish();
        protected abstract void internalClose();

        public ZStream(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(visibility = PRIVATE)
        public IRubyObject initialize(Block unusedBlock) {
            return this;
        }

        @JRubyMethod
        public IRubyObject flush_next_out(ThreadContext context) {
            return RubyString.newEmptyString(context.getRuntime());
        }

        @JRubyMethod
        public IRubyObject total_out() {
            checkClosed();
            return getRuntime().newFixnum(internalTotalOut());
        }

        @JRubyMethod(name = "stream_end?")
        public IRubyObject stream_end_p() {
            return internalStreamEndP() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod(name = "data_type")
        public IRubyObject data_type() {
            checkClosed();
            return getRuntime().getModule("Zlib").getConstant("UNKNOWN");
        }

        @JRubyMethod(name = { "closed?", "ended?"})
        public IRubyObject closed_p() {
            return closed ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        @JRubyMethod(name = "reset")
        public IRubyObject reset() {
            checkClosed();
            internalReset();
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "avail_out")
        public IRubyObject avail_out() {
            return RubyFixnum.zero(getRuntime());
        }

        @JRubyMethod(name = "avail_out=", required = 1)
        public IRubyObject set_avail_out(IRubyObject p1) {
            checkClosed();
            return p1;
        }

        @JRubyMethod(name = "adler")
        public IRubyObject adler() {
            checkClosed();
            return getRuntime().newFixnum(internalAdler());
        }

        @JRubyMethod(name = "finish", backtrace = true)
        public IRubyObject finish(ThreadContext context) {
            checkClosed();
            IRubyObject result = internalFinish();
            return result;
        }

        @JRubyMethod(name = "avail_in")
        public IRubyObject avail_in() {
            return RubyFixnum.zero(getRuntime());
        }

        @JRubyMethod(name = "flush_next_in")
        public IRubyObject flush_next_in(ThreadContext context) {
            return RubyString.newEmptyString(context.getRuntime());
        }

        @JRubyMethod(name = "total_in")
        public IRubyObject total_in() {
            checkClosed();
            return getRuntime().newFixnum(internalTotalIn());
        }

        @JRubyMethod(name = "finished?")
        public IRubyObject finished_p(ThreadContext context) {
            checkClosed();
            Ruby runtime = context.getRuntime();
            return internalFinished() ? runtime.getTrue() : runtime.getFalse();
        }

        @JRubyMethod(name = {"close", "end"})
        public IRubyObject close() {
            checkClosed();
            internalClose();
            closed = true;
            return getRuntime().getNil();
        }

        void checkClosed() {
            if (closed) {
                throw Util.newZlibError(getRuntime(), "stream is not ready");
            }
        }

        static void checkLevel(Ruby runtime, int level) {
            if ((level < 0 || level > 9) && level != Deflater.DEFAULT_COMPRESSION) {
                throw Util.newStreamError(runtime, "stream error: invalid level");
            }
        }

        /**
         * We only do windowBits=15(32K buffer, LZ77 algorithm) since java.util.zip only allows it.
         * NOTE: deflateInit2 of zlib.c also accepts MAX_WBITS + 16(gzip compression).
         * inflateInit2 also accepts MAX_WBITS + 16(gzip decompression) and MAX_WBITS + 32(automatic detection of gzip and LZ77).
         */
        static void checkWindowBits(Ruby runtime, int wbits, boolean forInflate) {
            wbits = Math.abs(wbits);
            if ((wbits & 0xf) < MIN_WBITS) {
                throw Util.newStreamError(runtime, "stream error: invalid window bits");
            }
            if ((wbits & 0xf) != 0xf) {
                // windowBits < 15 for reducing memory is meaningless on Java platform. 
                runtime.getWarnings().warn("windowBits < 15 is ignored on this platform");
                // continue
            }
            if (forInflate && wbits > MAX_WBITS + 32) {
                throw Util.newStreamError(runtime, "stream error: invalid window bits");
            } else if (!forInflate && wbits > MAX_WBITS + 16) {
                throw Util.newStreamError(runtime, "stream error: invalid window bits");
            }
        }

        static void checkStrategy(Ruby runtime, int strategy) {
            switch (strategy) {
                case Deflater.DEFAULT_STRATEGY:
                case Deflater.FILTERED:
                case Deflater.HUFFMAN_ONLY:
                    break;
                default:
                    throw Util.newStreamError(runtime, "stream error: invalid strategy");
            }
        }
    }

    @JRubyClass(name = "Zlib::Inflate", parent = "Zlib::ZStream")
    public static class Inflate extends ZStream {

        public static final int BASE_SIZE = 100;
        private Inflater flater;
        private int windowBits;
        private boolean readHeaderNeeded = false;
        private boolean readTrailerNeeded = false;
        private CRC32 checksum;
        private ByteList collected;
        private ByteList input;

        private boolean jzlib = true;
        private com.jcraft.jzlib.ZStream flater2 = null;
        private boolean finished = false;

        protected static final ObjectAllocator INFLATE_ALLOCATOR = new ObjectAllocator() {

            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Inflate(runtime, klass);
            }
        };

        public Inflate(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(name = "inflate", required = 1, meta = true, backtrace = true)
        public static IRubyObject s_inflate(ThreadContext context, IRubyObject recv, IRubyObject string) {
            RubyClass klass = (RubyClass) recv;
            Inflate inflate = (Inflate) klass.allocate();
            inflate.init(MAX_WBITS);

            IRubyObject result;
            try {
                inflate.append(string.convertToString().getByteList());
            } finally {
                result = inflate.finish(context);
                inflate.close();
            }
            return result;
        }

        @JRubyMethod(name = "initialize", optional = 1, visibility = PRIVATE)
        public IRubyObject _initialize(IRubyObject[] args) {
            windowBits = MAX_WBITS;

            if (args.length > 0 && !args[0].isNil()) {
                windowBits = RubyNumeric.fix2int(args[0]);
                checkWindowBits(getRuntime(), windowBits, true);
            }

            init(windowBits);
            return this;
        }

        private void init(int windowBits) {
            finished = false;
            flater2 = null;

            if(jzlib){
                flater2 = new com.jcraft.jzlib.ZStream();
                flater2.inflateInit(windowBits);
            }
            else{
            boolean nowrap = false;
            if (windowBits < 0) {
                nowrap = true;
            } else if ((windowBits & 0x10) != 0) {
                nowrap = true; // gzip wrapper
                readHeaderNeeded = true;
                checksum = new CRC32();
            } else if ((windowBits & 0x20) != 0) {
                nowrap = true; // automatic detection
                readHeaderNeeded = true;
                checksum = new CRC32();
            }
            flater = new Inflater(nowrap);
            }
            collected = new ByteList(BASE_SIZE);
            input = new ByteList();
        }

        @Override
        @JRubyMethod(name = "flush_next_out")
        public IRubyObject flush_next_out(ThreadContext context) {
            return flushOutput(context.getRuntime());
        }

        private RubyString flushOutput(Ruby runtime) {
            if (collected.getRealSize() > 0) {
                if (checksum != null) {
                    checksum.update(collected.getUnsafeBytes(), collected.getBegin(),
                            collected.getRealSize());
                }
                RubyString res = RubyString.newString(runtime, collected.getUnsafeBytes(),
                        collected.getBegin(), collected.getRealSize());
                Util.resetBuffer(collected);
                return res;
            }
            return RubyString.newEmptyString(runtime);
        }

        @JRubyMethod(name = "<<", required = 1)
        public IRubyObject append(ThreadContext context, IRubyObject arg) {
            checkClosed();
            if (arg.isNil()) {
                run(true);
            } else {
                append(arg.convertToString().getByteList());
            }
            return this;
        }

        public void append(ByteList obj) {
            if (!internalFinished()) {
                if (readHeaderNeeded) {
                    input.append(obj);
                    byte[] bytes = input.bytes();
                    int size = parseHeader(bytes);
                    switch (size) {
                    case -1:
                        // not in gzip format; reinitialize Inflater
                        init(windowBits & 0xf);
                        flater.setInput(obj.bytes());
                        input = new ByteList(bytes, false);
                        break;
                    case 0:
                        // buffer is short
                        return;
                    default:
                        flater.setInput(bytes, size, bytes.length - size);
                        input = new ByteList(bytes, size, bytes.length - size, false);
                        break;
                    }
                } else {
                    byte[] bytes = obj.bytes();

                    if(jzlib){
                        flater2.next_in=bytes;
                        flater2.next_in_index=0;
                        flater2.avail_in=bytes.length;
                    }
                    else{
                    flater.setInput(bytes);
                    input = new ByteList(bytes, false);
                    }
                }
                run(false);
            } else {
                input.append(obj);
            }
        }

        @JRubyMethod(name = "sync_point?")
        public IRubyObject sync_point_p() {
            return sync_point();
        }

        public IRubyObject sync_point() {
            if(jzlib){
                int ret = flater2.inflateSyncPoint();
                switch(ret){
                    case com.jcraft.jzlib.JZlib.Z_STREAM_END:
                        return getRuntime().getTrue();
                    case com.jcraft.jzlib.JZlib.Z_OK:
                        return getRuntime().getFalse();
                    default:
                        throw Util.newStreamError(getRuntime(), "stream error");
                }
            }
            else
            return getRuntime().getFalse();
        }

        @JRubyMethod(name = "set_dictionary", required = 1, backtrace = true)
        public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) {
            try {
                return set_dictionary(arg);
            } catch (IllegalArgumentException iae) {
                throw Util.newStreamError(context.getRuntime(), "stream error: " + iae.getMessage());
            }
        }

        private IRubyObject set_dictionary(IRubyObject str) {
            if(jzlib){
                byte [] tmp = str.convertToString().getBytes();
                int ret =  flater2.inflateSetDictionary(tmp, tmp.length);
                switch(ret){
                    case com.jcraft.jzlib.JZlib.Z_STREAM_ERROR:
                        throw Util.newStreamError(getRuntime(), "stream error");
                    case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                        throw Util.newDataError(getRuntime(), "wrong dictionary");
                    default:
                }
            }
            else
            flater.setDictionary(str.convertToString().getBytes());
            run(false);
            return str;
        }

        @JRubyMethod(name = "inflate", required = 1, backtrace = true)
        public IRubyObject inflate(ThreadContext context, IRubyObject string) {
            ByteList data = null;
            if (!string.isNil()) {
                data = string.convertToString().getByteList();
            }
            return inflate(context, data);
        }

        public IRubyObject inflate(ThreadContext context, ByteList str) {
            if (null == str) {
                return internalFinish();
            } else {
                append(str);
                return flushOutput(context.getRuntime());
            }
        }

        @JRubyMethod(name = "sync", required = 1)
        public IRubyObject sync(ThreadContext context, IRubyObject string) {
            if(jzlib){
                if(flater2.avail_in>0){
                    switch(flater2.inflateSync()){
                        case com.jcraft.jzlib.JZlib.Z_OK:
                            return getRuntime().getTrue();
                        case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                            break;
                        default:
                            throw Util.newStreamError(getRuntime(), "stream error");
                    }
                }
                if(string.convertToString().getByteList().length()<=0)
                    return getRuntime().getFalse();
                append(context, string);
                switch(flater2.inflateSync()){
                    case com.jcraft.jzlib.JZlib.Z_OK:
                        return getRuntime().getTrue();
                    case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                        return getRuntime().getFalse();
                    default:
                        throw Util.newStreamError(getRuntime(), "stream error");
                }
            }
            else{
            try {
                append(context, string);
            } catch (RaiseException re) {
                if (!re.getException().getMetaClass().getRealClass().getName().equals("Zlib::DataError")) {
                    throw re;
                }
            }
            return context.getRuntime().getFalse();
            }
        }

        private void run(boolean finish) {
            byte[] outp = new byte[1024];
            int resultLength = -1;

            while (!internalFinished() && resultLength != 0) {
                Ruby runtime = getRuntime();

                // MRI behavior
                boolean  needsInput = jzlib ? flater2.avail_in<0 : flater.needsInput();
                if (finish && needsInput) {
                    throw Util.newBufError(runtime, "buffer error");
                }

                try {
                    if(jzlib){
                      flater2.next_out = outp;
                      flater2.next_out_index = 0;
                      flater2.avail_out = outp.length;
                      int ret = flater2.inflate(com.jcraft.jzlib.JZlib.Z_NO_FLUSH);
                      switch(ret){
                      case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                           throw Util.newDataError(runtime, flater2.msg);
                      case com.jcraft.jzlib.JZlib.Z_NEED_DICT:
                           throw Util.newDictError(runtime, "need dictionary");
                      case com.jcraft.jzlib.JZlib.Z_OK:
                      case com.jcraft.jzlib.JZlib.Z_STREAM_END:
                          if(ret == com.jcraft.jzlib.JZlib.Z_STREAM_END)
                              finished=true;
                          resultLength = flater2.next_out_index;
                          break;
                      default:
                          resultLength=0;
                      }
                    }
                    else 
                        resultLength = flater.inflate(outp);
                    if (!jzlib && flater.needsDictionary()) {
                        throw Util.newDictError(runtime, "need dictionary");
                    } else {
                        if (input.getRealSize() > 0) {
                            int remaining = flater.getRemaining();
                            if (remaining > 0) {
                                input.view(input.getRealSize() - remaining, remaining);
                            } else {
                                Util.resetBuffer(input);
                            }
                        }
                    }
                } catch (DataFormatException ex) {
                    throw Util.newDataError(runtime, "data error: " + ex.getMessage());
                }

                collected.append(outp, 0, resultLength);
                if (resultLength == outp.length) {
                    outp = new byte[outp.length * 2];
                }
            }
        }

        @Override
        protected int internalTotalIn() {
            if(jzlib)
                return (int)flater2.total_in;
            else
            return flater.getTotalIn();
        }

        @Override
        protected int internalTotalOut() {
            if(jzlib)
                return (int)flater2.total_out;
            else
            return flater.getTotalOut();
        }

        @Override
        protected boolean internalStreamEndP() {
            if(jzlib)
                return finished;
            else
            return flater.finished();
        }

        @Override
        protected void internalReset() {
            init(windowBits);
        }

        @Override
        protected boolean internalFinished() {
            if(jzlib)
                return finished;
            else
            return flater.finished();
        }

        @Override
        protected long internalAdler() {
            if(jzlib)
                return flater2.getAdler();
            else
            return (checksum != null) ? checksum.getValue() : flater.getAdler() & 0xffffffffL;
        }

        @Override
        protected IRubyObject internalFinish() {
            run(true);
            Ruby runtime = getRuntime();
            // Need to process buffer first for calculating checksum
            RubyString str = flushOutput(runtime);
            // process trailer if needed
            if (internalFinished() && readTrailerNeeded) {
                if (input.getRealSize() < 8) {
                    throw Util.newBufError(runtime, "buffer error");
                }
                readTrailer(input.bytes(), (flater.getBytesWritten() & 0xffffffffL),
                        checksum.getValue());
                input.view(8, input.getRealSize() - 8);
            }
            if(jzlib){
                if(!finished){
                    int err=flater2.inflate(com.jcraft.jzlib.JZlib.Z_FINISH);
                    if(err!=com.jcraft.jzlib.JZlib.Z_OK){
                      throw Util.newBufError(runtime, "buffer error");
                    }
                    finished =true;
                }
                flater2.inflateEnd();
            }
            else
            flater.end();
            // MRI behavior: in finished mode, we work as pass-through
            if (internalFinished()) {
                if (input.getRealSize() > 0) {
                    collected.append(input);
                    Util.resetBuffer(input);
                    str.append(flushOutput(runtime));
                }
            }
            return str;
        }

        @Override
        protected void internalClose() {
            if(jzlib){
                if(!finished){
                    int err=flater2.inflate(com.jcraft.jzlib.JZlib.Z_FINISH);
                    if(err!=com.jcraft.jzlib.JZlib.Z_OK){
                        Ruby runtime = getRuntime();
                        throw Util.newBufError(runtime, "buffer error");
                    }
                    finished =true;
                }
                flater2.inflateEnd();
            }
            else
            flater.end();
        }

        private int parseHeader(byte[] bytes) {
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            try {
                // parsed Gzip header is not used
                Util.GzipHeader header = Util.readHeader(getRuntime(), is);
                if (header == null) {
                    // Not a gzip format
                    return -1;
                }
                readHeaderNeeded = false;
                readTrailerNeeded = true;
                return header.length;
            } catch (RaiseException re) {
                return 0;
            }
        }

        private void readTrailer(byte[] trailer, long bytesWritten, long checksum) {
            Util.checkTrailer(getRuntime(), trailer, bytesWritten, checksum);
            readTrailerNeeded = false;
        }
    }

    @JRubyClass(name = "Zlib::Deflate", parent = "Zlib::ZStream")
    public static class Deflate extends ZStream {

        public static final int BASE_SIZE = 100;
        private Deflater flater;
        private int level;
        private int windowBits;
        private int strategy;
        private boolean dumpHeaderNeeded = false;
        private boolean dumpTrailerNeeded = false;
        private CRC32 checksum;
        private ByteList collected;
        protected static final ObjectAllocator DEFLATE_ALLOCATOR = new ObjectAllocator() {

            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Deflate(runtime, klass);
            }
        };

        private boolean jzlib = true;
        private com.jcraft.jzlib.ZStream flater2 = null;
        private boolean finished = false;
        private int flush = Z_NO_FLUSH;

        @JRubyMethod(name = "deflate", required = 1, optional = 1, meta = true, backtrace = true)
        public static IRubyObject s_deflate(IRubyObject recv, IRubyObject[] args) {
            Ruby runtime = recv.getRuntime();
            args = Arity.scanArgs(runtime, args, 1, 1);
            int level = Deflater.DEFAULT_COMPRESSION;
            if (!args[1].isNil()) {
                level = RubyNumeric.fix2int(args[1]);
                checkLevel(runtime, level);
            }

            RubyClass klass = (RubyClass) recv;
            Deflate deflate = (Deflate) klass.allocate();
            deflate.init(level, MAX_WBITS, 8, Deflater.DEFAULT_STRATEGY);

            try {
                IRubyObject result = deflate.deflate(args[0].convertToString().getByteList(), Z_FINISH);
                deflate.close();
                return result;
            } catch (IOException ioe) {
                throw runtime.newIOErrorFromException(ioe);
            }
        }

        public Deflate(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(name = "initialize", optional = 4, visibility = PRIVATE, backtrace = true)
        public IRubyObject _initialize(IRubyObject[] args) {
            args = Arity.scanArgs(getRuntime(), args, 0, 4);
            level = -1;
            windowBits = MAX_WBITS;
            int memlevel = 8;
            strategy = 0;
            if (!args[0].isNil()) {
                level = RubyNumeric.fix2int(args[0]);
                checkLevel(getRuntime(), level);
            }
            if (!args[1].isNil()) {
                windowBits = RubyNumeric.fix2int(args[1]);
                checkWindowBits(getRuntime(), windowBits, false);
            }
            if (!args[2].isNil()) {
                memlevel = RubyNumeric.fix2int(args[2]);
                // We accepts any memlevel and ignores it. Memory setting means nothing on Java platform.
            }
            if (!args[3].isNil()) {
                strategy = RubyNumeric.fix2int(args[3]);
            }
            init(level, windowBits, memlevel, strategy);
            return this;
        }

        private void init(int level, int windowBits, int memlevel, int strategy) {
            // Zlib behavior: negative win_bits means no header and no checksum.
            if(jzlib){
                finished = false;
                flush = Z_NO_FLUSH;
                flater2 = new com.jcraft.jzlib.ZStream();

                int err =  flater2.deflateInit(level, windowBits, memlevel); 
                // TODO: checking err
                err = flater2.deflateParams(level, strategy);
                // TODO: checking err
            }
            else{
            boolean nowrap = false;
            if (windowBits < 0) {
                nowrap = true;
            } else if ((windowBits & 0x10) != 0) {
                nowrap = true; // gzip wrapper
                dumpHeaderNeeded = true;
                checksum = new CRC32();
            }
            flater = new Deflater(level, nowrap);
            flater.setStrategy(strategy);
            }
            collected = new ByteList(BASE_SIZE);
        }

        @Override
        @JRubyMethod(visibility = PRIVATE)
        public IRubyObject initialize_copy(IRubyObject other) {
            if (this == other) {
                return this;
            }
            // TODO: we cannot implement Deflate#dup as long as we use java.util.zip.Deflater...
            throw getRuntime().newNotImplementedError("Zlib::Deflate#dup is not supported");
        }

        @JRubyMethod(name = "<<", required = 1)
        public IRubyObject append(IRubyObject arg) {
            checkClosed();
            try {
                append(arg.convertToString().getByteList());
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
            return this;
        }

        @JRubyMethod(name = "params", required = 2)
        public IRubyObject params(ThreadContext context, IRubyObject level, IRubyObject strategy) {
            int l = RubyNumeric.fix2int(level);
            checkLevel(getRuntime(), l);
            int s = RubyNumeric.fix2int(strategy);
            checkStrategy(getRuntime(), s);
            if(jzlib){
                if(flater2.next_out==null)
                    flater2.next_out=new byte[0];
                flater2.avail_out = flater2.next_out.length;
                flater2.next_out_index = 0;
                int err = flater2.deflateParams(l, s);
                // TODO: checking err
                if(flater2.next_out_index>0)
                    collected.append(flater2.next_out, 0, flater2.next_out_index);
            }
            else{
            flater.setLevel(l);
            flater.setStrategy(s);
            }
            run();
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "set_dictionary", required = 1, backtrace = true)
        public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) {
            try {
                if(jzlib){
                    byte [] tmp = arg.convertToString().getBytes();
		    flater2.deflateSetDictionary(tmp, tmp.length);
                }
                else{
                flater.setDictionary(arg.convertToString().getBytes());
                }
                run();
                return arg;
            } catch (IllegalArgumentException iae) {
                throw Util.newStreamError(context.getRuntime(), "stream error: " + iae.getMessage());
            }
        }

        @JRubyMethod(name = "flush", optional = 1)
        public IRubyObject flush(IRubyObject[] args) {
            int flush = 2; // SYNC_FLUSH
            if (args.length == 1) {
                if (!args[0].isNil()) {
                    flush = RubyNumeric.fix2int(args[0]);
                }
            }
            return flush(flush);
        }

        @JRubyMethod(name = "deflate", required = 1, optional = 1)
        public IRubyObject deflate(IRubyObject[] args) {
            args = Arity.scanArgs(getRuntime(), args, 1, 1);
            if (internalFinished()) {
                throw Util.newStreamError(getRuntime(), "stream error");
            }
            ByteList data = null;
            if (!args[0].isNil()) {
                data = args[0].convertToString().getByteList();
            }
            int flush = Z_NO_FLUSH;
            if (!args[1].isNil()) {
                flush = RubyNumeric.fix2int(args[1]);
            }
            try {
                return deflate(data, flush);
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }

        @Override
        protected int internalTotalIn() {
            if(jzlib){
                return (int)flater2.total_in;
            }
            else
            return flater.getTotalIn();
        }

        @Override
        protected int internalTotalOut() {
            if(jzlib){
                return (int)flater2.total_out;
            }
            else
            return flater.getTotalOut();
        }

        @Override
        protected boolean internalStreamEndP() {
            if(jzlib)
                return finished;
            else 
            return flater.finished();
        }

        @Override
        protected void internalReset() {
            init(level, windowBits, 8, strategy);
        }

        @Override
        public boolean internalFinished() {
            if(jzlib)
                return finished;
            else
            return flater.finished();
        }

        @Override
        protected long internalAdler() {
            if(jzlib){
                return flater2.getAdler();
            }
            else
            return (checksum != null) ? checksum.getValue() : flater.getAdler() & 0xffffffffL;
        }

        @Override
        protected IRubyObject internalFinish() {
            return finish();
        }

        @Override
        protected void internalClose() {
            if(jzlib){
                flater2.deflateEnd();
            }
            else
            flater.end();
        }

        private void append(ByteList obj) throws IOException {
            if(jzlib){
                flater2.next_in=obj.getUnsafeBytes();
                flater2.next_in_index=obj.getBegin();
                flater2.avail_in=obj.getRealSize();
            }
            else{
            if (checksum != null) {
                if (dumpHeaderNeeded) {
                    writeHeader();
                }
                checksum.update(obj.getUnsafeBytes(), obj.getBegin(), obj.getRealSize());
            }
            flater.setInput(obj.getUnsafeBytes(), obj.getBegin(), obj.getRealSize());
            }
            run();
        }

        private IRubyObject flush(int flush) {
            if(jzlib){
                this.flush=flush;
                if (flush == Z_NO_FLUSH) { // TODO: ???
                    return RubyString.newEmptyString(getRuntime());
                }
                run();
	    }
            else{
            if (flush == Z_NO_FLUSH) {
                return RubyString.newEmptyString(getRuntime());
            }
            if (flush == Z_FINISH) {
                flater.finish();
                run();
                if (dumpTrailerNeeded) {
                    writeTrailer();
                }
            } else {
                run();
            }
            }
            IRubyObject obj = RubyString.newString(getRuntime(), collected);
            collected = new ByteList(BASE_SIZE);
            return obj;
        }

        private IRubyObject deflate(ByteList str, int flush) throws IOException {
            if (null != str) {
                append(str);
            }
            return flush(flush);
        }

        private IRubyObject finish() {
            return flush(Z_FINISH);
        }

        private void run() {
            if(jzlib){
                if(finished)
                    return;
                byte[] outp = new byte[1024];
                while (!finished){
                    flater2.next_out = outp;
                    flater2.next_out_index = 0;
                    flater2.avail_out = flater2.next_out.length;
                    int err = flater2.deflate(flush);
                    if(err == com.jcraft.jzlib.JZlib.Z_STREAM_END){
                        finished=true;
                    }
                    int resultLength = flater2.next_out_index;
                    if(resultLength == 0)
                        break;
                    collected.append(flater2.next_out, 0, resultLength);
                    if (resultLength == flater2.next_out.length && !finished) {
                        outp = new byte[flater2.next_out.length * 2];
                    }
                }
            }
            else{
            if (flater.finished()) {
                return;
            }
            byte[] outp = new byte[1024];
            while (!flater.finished()) {
                int resultLength = flater.deflate(outp);
                if (resultLength == 0) {
                    break;
                }
                collected.append(outp, 0, resultLength);
                if (resultLength == outp.length) {
                    outp = new byte[outp.length * 2];
                }
            }
            }
        }

        private void writeHeader() throws IOException {
            collected.append(Util.dumpHeader(null, null, Z_DEFAULT_COMPRESSION, OS_CODE,
                    System.currentTimeMillis()));
            dumpHeaderNeeded = false;
            dumpTrailerNeeded = true;
        }

        private void writeTrailer() {
            collected.append(Util.dumpTrailer(flater.getTotalIn(), (int) checksum.getValue()));
            dumpTrailerNeeded = false;
        }
    }

    @JRubyClass(name="Zlib::GzipFile")
    public static class RubyGzipFile extends RubyObject {
        @JRubyClass(name="Zlib::GzipFile::Error", parent="Zlib::Error")
        public static class Error {}
        @JRubyClass(name="Zlib::GzipFile::CRCError", parent="Zlib::GzipFile::Error")
        public static class CRCError extends Error {}
        @JRubyClass(name="Zlib::GzipFile::NoFooter", parent="Zlib::GzipFile::Error")
        public static class NoFooter extends Error {}
        @JRubyClass(name="Zlib::GzipFile::LengthError", parent="Zlib::GzipFile::Error")
        public static class LengthError extends Error {}

        static IRubyObject wrapBlock(ThreadContext context, RubyGzipFile instance, Block block) {
            if (block.isGiven()) {
                try {
                    return block.yield(context, instance);
                } finally {
                    if (!instance.isClosed()) {
                        instance.close();
                    }
                }
            }
            return instance;
        }
        
        @JRubyMethod(meta = true)
        public static IRubyObject wrap(ThreadContext context, IRubyObject recv, IRubyObject io, Block block) {
            Ruby runtime = recv.getRuntime();
            RubyGzipFile instance;
            
            // TODO: People extending GzipWriter/reader will break.  Find better way here.
            if (recv == runtime.getModule("Zlib").getClass("GzipWriter")) {
                instance = RubyGzipWriter.newGzipWriter(recv, new IRubyObject[] { io }, block);
            } else {
                instance = RubyGzipReader.newInstance(recv, new IRubyObject[] { io }, block);
            }

            return wrapBlock(context, instance, block);
        }
        
        protected static final ObjectAllocator GZIPFILE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyGzipFile(runtime, klass);
            }
        };

        @JRubyMethod(name = "new", meta = true)
        public static RubyGzipFile newInstance(IRubyObject recv, Block block) {
            RubyClass klass = (RubyClass)recv;
            
            RubyGzipFile result = (RubyGzipFile) klass.allocate();
            
            result.callInit(new IRubyObject[0], block);
            
            return result;
        }

        protected boolean closed = false;
        protected boolean finished = false;
        protected byte osCode = OS_UNKNOWN;
        protected int level = -1;
        protected RubyString nullFreeOrigName;
        protected RubyString nullFreeComment;
        protected IRubyObject realIo;
        protected RubyTime mtime;

        public RubyGzipFile(Ruby runtime, RubyClass type) {
            super(runtime, type);
            mtime = RubyTime.newTime(runtime, new DateTime());
        }
        
        @JRubyMethod(name = "os_code")
        public IRubyObject os_code() {
            return getRuntime().newFixnum(osCode & 0xff);
        }
        
        @JRubyMethod(name = "closed?")
        public IRubyObject closed_p() {
            return closed ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        
        protected boolean isClosed() {
            return closed;
        }
        
        @JRubyMethod(name = "orig_name")
        public IRubyObject orig_name() {
            if(closed) {
                throw Util.newGzipFileError(getRuntime(), "closed gzip stream");
            }
            return nullFreeOrigName == null ? getRuntime().getNil() : nullFreeOrigName;
        }
        
        @JRubyMethod(name = "to_io")
        public IRubyObject to_io() {
            return realIo;
        }

        @JRubyMethod(name = "comment")
        public IRubyObject comment() {
            if(closed) {
                throw Util.newGzipFileError(getRuntime(), "closed gzip stream");
            }
            return nullFreeComment == null ? getRuntime().getNil() : nullFreeComment;
        }

        @JRubyMethod(name = "crc")
        public IRubyObject crc() {
            return getRuntime().newFixnum(0);
        }
        
        @JRubyMethod(name = "mtime")
        public IRubyObject mtime() {
            return mtime;
        }
        
        @JRubyMethod(name = "sync")
        public IRubyObject sync() {
            return getRuntime().getNil();
        }
        
        @JRubyMethod(name = "finish")
        public IRubyObject finish() {
            if (!finished) {
                //io.finish();
            }
            finished = true;
            return realIo;
        }

        @JRubyMethod(name = "close")
        public IRubyObject close() {
            return null;
        }
        
        @JRubyMethod(name = "level")
        public IRubyObject level() {
            return getRuntime().newFixnum(level);
        }
        
        @JRubyMethod(name = "sync=", required = 1)
        public IRubyObject set_sync(IRubyObject ignored) {
            return getRuntime().getNil();
        }
    }

    @JRubyClass(name="Zlib::GzipReader", parent="Zlib::GzipFile", include="Enumerable")
    public static class RubyGzipReader extends RubyGzipFile {

        @JRubyClass(name="Zlib::GzipReader::Error", parent="Zlib::GzipReader")
        public static class Error {}

        protected static final ObjectAllocator GZIPREADER_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyGzipReader(runtime, klass);
            }
        };
        
        @JRubyMethod(name = "new", rest = true, meta = true)
        public static RubyGzipReader newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            RubyClass klass = (RubyClass)recv;
            RubyGzipReader result = (RubyGzipReader)klass.allocate();
            result.callInit(args, block);
            return result;
        }

        @JRubyMethod(meta = true)
        public static IRubyObject open(final ThreadContext context, IRubyObject recv, IRubyObject filename, Block block) {
            Ruby runtime = recv.getRuntime();
            IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", filename, runtime.newString("rb"));
            RubyGzipReader gzio = newInstance(recv, new IRubyObject[]{io}, block);
            return RubyGzipFile.wrapBlock(context, gzio, block);
        }

        public RubyGzipReader(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        private int line;
        private long position;
        private HeaderReadableGZIPInputStream io;
        private InputStream bufferedStream;

        /**
         * IOInputStream wrapper for counting and keeping reading position.
         */
        private static class CountingIOInputStream extends IOInputStream {

            private int position;
            IRubyObject io;

            public CountingIOInputStream(IRubyObject io) {
                super(io);
                this.io = io;
                position = 0;
            }

            @Override
            public int read() throws IOException {
                int ret = super.read();
                if (ret != -1) {
                    position++;
                }
                return ret;
            }

            @Override
            public int read(byte[] b) throws IOException {
                int ret = super.read(b);
                if (ret != -1) {
                    position += ret;
                }
                return ret;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int ret = super.read(b, off, len);
                if (ret != -1) {
                    position += ret;
                }
                return ret;
            }

            int pos() {
                return position;
            }

            Ruby getRuntime() {
                return io.getRuntime();
            }
        }

        private class HeaderReadableGZIPInputStream extends InflaterInputStream {

            private final static int DEFAULT_BUFFER_SIZE = 512;
            // saame as InputStream#in
            private CountingIOInputStream countingStream;
            private CRC32 checksum = new CRC32();
            private boolean eof = false;

            /**
             * Offers header property in addition to GZIPInputStream.
             */
            public HeaderReadableGZIPInputStream(CountingIOInputStream io) {
                super(new BufferedInputStream(io), new Inflater(true), DEFAULT_BUFFER_SIZE);
                this.countingStream = io;
                parseHeader(io);
                eof = false;
                checksum.reset();
            }

            @Override
            public int read() throws IOException {
                if (eof) {
                    return -1;
                }
                int ret = super.read();
                if (ret == -1) {
                    parseTrailer();
                } else {
                    checksum.update((byte) (ret & 0xff));
                }
                return ret;
            }

            @Override
            public int read(byte[] b) throws IOException {
                if (eof) {
                    return -1;
                }
                int ret = super.read(b);
                if (ret == -1) {
                    parseTrailer();
                } else {
                    checksum.update(b, 0, ret);
                }
                return ret;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (eof) {
                    return -1;
                }
                int ret = super.read(b, off, len);
                if (ret == -1) {
                    parseTrailer();
                } else {
                    checksum.update(b, off, ret);
                }
                return ret;
            }

            /**
             * We call internal IO#close directly, not via IOInputStream#close.
             * IOInputStream#close directly invoke IO.getOutputStream().close()
             * for IO object instead of just calling IO#cloase of Ruby.
             * It causes EBADF at OpenFile#finalize.
             *
             * CAUTION: CountingIOInputStream#close does not called even if it exists.
             *
             * TODO: implement this without IOInputStream? Not so hard.
             */
            @Override
            public void close() throws IOException {
                // Do not invoke DeflaterOutputStream#close here.
                // following works as same as DeflaterOutputStream#close.
                //super.close();
                if (!closed) {
                    // we don't want to invoke IOInputStream#close for now.
                    // in.close();
                    closed = true;
                }
                // call IO#close instead.
                if (countingStream.io.respondsTo("close")) {
                    countingStream.io.callMethod(countingStream.getRuntime().getCurrentContext(), "close");
                }
                eof = true;
            }

            public int pos() {
                return countingStream.pos();
            }

            public long crc() {
                return checksum.getValue();
            }

            private void parseHeader(CountingIOInputStream io) {
                Util.GzipHeader header = Util.readHeader(io.getRuntime(), in);
                if (header == null) {
                    throw Util.newGzipFileError(io.getRuntime(), "not in gzip format");
                }
                mtime.setDateTime(header.mtime);
                level = header.level;
                osCode = header.osCode;
                if (header.origName != null) {
                    nullFreeOrigName = io.getRuntime().newString(header.origName);
                    nullFreeOrigName.setTaint(true);
                }
                if (header.comment != null) {
                    nullFreeComment = io.getRuntime().newString(header.comment);
                    nullFreeComment.setTaint(true);
                }
            }
            
            private void parseTrailer() {
                try {
                    eof = true;
                    int rest = 8;
                    byte[] trailer = new byte[8];
                    int remaining = super.inf.getRemaining();
                    if (remaining > 0) {
                        System.arraycopy(super.buf, super.len - remaining, trailer, 0,
                                (remaining > 8) ? 8 : remaining);
                        rest -= remaining;
                    }
                    while (rest > 0) {
                        int ret = in.read(trailer, 8 - rest, rest);
                        if (ret == -1) {
                            throw new EOFException();
                        }
                        rest -= ret;
                    }
                    Util.checkTrailer(countingStream.getRuntime(), trailer,
                            (super.inf.getBytesWritten() & 0xffffffffL), checksum.getValue());
                } catch (IOException ignored) {
                    throw Util.newNoFooter(countingStream.getRuntime(),
                            "footer is not found");
                }
            }
        }

        @JRubyMethod(visibility = PRIVATE)
        public IRubyObject initialize(IRubyObject stream) {
            realIo = stream;
            line = 0;
            position = 0;
            io = new HeaderReadableGZIPInputStream(new CountingIOInputStream(realIo));
            bufferedStream = new BufferedInputStream(io);
            return this;
        }

        @JRubyMethod(visibility = PRIVATE, compat = RUBY1_9)
        public IRubyObject initialize19(IRubyObject stream) {
            return initialize(stream);
        }

        @JRubyMethod(visibility = PRIVATE, compat = RUBY1_9)
        public IRubyObject initialize19(IRubyObject stream, IRubyObject options) {
            initialize(stream);
            return this;
        }

        @JRubyMethod
        public IRubyObject rewind() {
            Ruby rt = getRuntime();
            // should invoke seek on realIo...
            realIo.callMethod(rt.getCurrentContext(), "seek",
                    new IRubyObject[]{rt.newFixnum(-io.pos()), rt.newFixnum(Stream.SEEK_CUR)});
            // ... and then reinitialize
            initialize(realIo);
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "lineno")
        public IRubyObject lineno() {
            return getRuntime().newFixnum(line);
        }

        @JRubyMethod(name = "readline", writes = FrameField.LASTLINE)
        public IRubyObject readline(ThreadContext context) {
            IRubyObject dst = gets(context, new IRubyObject[0]);
            if (dst.isNil()) {
                throw getRuntime().newEOFError();
            }
            return dst;
        }

        private IRubyObject internalGets(IRubyObject[] args) throws IOException {
            ByteList sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getByteList();
            if (args.length > 0) {
                sep = args[0].convertToString().getByteList();
            }
            return internalSepGets(sep);
        }

        private IRubyObject internalSepGets(ByteList sep) throws IOException {
            ByteList result = new ByteList();
            int ce = bufferedStream.read();
            while (ce != -1 && sep.indexOf(ce) == -1) {
                result.append((byte)ce);
                ce = bufferedStream.read();
            }
            // io.available() only returns 0 after EOF is encountered
            // so we need to differentiate between the empty string and EOF
            if (0 == result.length() && -1 == ce) {
              return getRuntime().getNil();
            }
            line++;
            this.position = result.length();
            result.append(sep);
            return RubyString.newString(getRuntime(),result);
        }

        @JRubyMethod(name = "gets", optional = 1, writes = FrameField.LASTLINE)
        public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
            try {
                IRubyObject result = internalGets(args);
                if (!result.isNil()) {
                    context.getCurrentScope().setLastLine(result);
                }
                return result;
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }

        private final static int BUFF_SIZE = 4096;
        
        @JRubyMethod(name = "read", optional = 1)
        public IRubyObject read(IRubyObject[] args) {
            try {
                if (args.length == 0 || args[0].isNil()) {
                    ByteList val = new ByteList(10);
                    byte[] buffer = new byte[BUFF_SIZE];
                    int read = bufferedStream.read(buffer);
                    while (read != -1) {
                        val.append(buffer, 0, read);
                        read = bufferedStream.read(buffer);
                    }
                    this.position += val.length();
                    return RubyString.newString(getRuntime(), val);
                }

                int len = RubyNumeric.fix2int(args[0]);
                if (len < 0) {
                    throw getRuntime().newArgumentError("negative length " + len + " given");
                } else if (len > 0) {
                    byte[] buffer = new byte[len];
                    int toRead = len;
                    int offset = 0;
                    int read = 0;
                    while (toRead > 0) {
                        read = bufferedStream.read(buffer, offset, toRead);
                        if (read == -1) {
                            if (offset == 0) {
                                // we're at EOF right away
                                return getRuntime().getNil();
                            }
                            break;
                        }
                        toRead -= read;
                        offset += read;
                    } // hmm...
                    this.position += buffer.length;
                    return RubyString.newString(getRuntime(),
                            new ByteList(buffer, 0, len - toRead, false));
                }
                return RubyString.newEmptyString(getRuntime());
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }

        @JRubyMethod(name = "lineno=", required = 1)
        public IRubyObject set_lineno(IRubyObject lineArg) {
            line = RubyNumeric.fix2int(lineArg);
            return lineArg;
        }

        @JRubyMethod(name = {"pos", "tell"})
        public IRubyObject pos() {
            return RubyNumeric.int2fix(getRuntime(), position);
        }

        @JRubyMethod(name = "readchar")
        public IRubyObject readchar() {
            try {
                int value = bufferedStream.read();
                if (value == -1) {
                    throw getRuntime().newEOFError();
                }
                position++;
                return getRuntime().newFixnum(value);
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }

        @JRubyMethod(name = "getc")
        public IRubyObject getc() {
            try {
                int value = bufferedStream.read();
                if (value == -1) {
                    return getRuntime().getNil();
                }
                position++;
                return getRuntime().newFixnum(value);
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }

        private boolean isEof() throws IOException {
            if (bufferedStream.available() == 0) {
                return true;
            } else {
                // Java's GZIPInputStream behavior is such
                // that it says that more bytes available even
                // when we are right before the EOF, but not yet
                // encountered the actual EOF during the reading.
                // So, we compensate for that to provide MRI
                // compatible behavior.
                bufferedStream.mark(16);
                bufferedStream.read();
                bufferedStream.reset();
            }
            return bufferedStream.available() == 0;
        }

        @Override
        @JRubyMethod(name = "close")
        public IRubyObject close() {
            if (!closed) {
                try {
                    bufferedStream.close();
                } catch (IOException ioe) {
                    throw getRuntime().newIOErrorFromException(ioe);
                }
            }
            this.closed = true;
            return getRuntime().getNil();
        }
        
        @JRubyMethod(name = "eof")
        public IRubyObject eof() {
            try {
                return isEof() ? getRuntime().getTrue() : getRuntime().getFalse();
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }

        @JRubyMethod(name = "eof?")
        public IRubyObject eof_p() {
            return eof();
        }

        @JRubyMethod
        public IRubyObject unused() {
            // TODO: implement
            return getRuntime().getNil();
        }

        @Override
        @JRubyMethod
        public IRubyObject crc() {
            return getRuntime().newFixnum(io.crc());
        }

        @JRubyMethod(optional = 1)
        public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
            ByteList sep = ((RubyString) getRuntime().getGlobalVariables().get("$/")).getByteList();
            if (args.length > 0 && !args[0].isNil()) {
                sep = args[0].convertToString().getByteList();
            }
            try {
                for (IRubyObject result = internalSepGets(sep); !result.isNil(); result = internalSepGets(sep)) {
                    block.yield(context, result);
                }
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
            return getRuntime().getNil();
        }

        @JRubyMethod(optional = 1)
        public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
          return each(context, args, block);
        }
    
        @JRubyMethod
        public IRubyObject ungetc(IRubyObject arg) {
            return getRuntime().getNil();
        }

        @JRubyMethod(optional = 1)
        public IRubyObject readlines(IRubyObject[] args) {
            List<IRubyObject> array = new ArrayList<IRubyObject>();
            if (args.length != 0 && args[0].isNil()) {
                array.add(read(new IRubyObject[0]));
            } else {
                ByteList sep = ((RubyString) getRuntime().getGlobalVariables().get("$/")).getByteList();
                if (args.length > 0) {
                    sep = args[0].convertToString().getByteList();
                }
                try {
                    for (IRubyObject result = internalSepGets(sep); !result.isNil(); result = internalSepGets(sep)) {
                        array.add(result);
                    }
                } catch (IOException ioe) {
                    throw getRuntime().newIOErrorFromException(ioe);
                }
            }
            return getRuntime().newArray(array);
        }

        @JRubyMethod
        public IRubyObject each_byte(ThreadContext context, Block block) {
            try {
                int value = bufferedStream.read();
                while (value != -1) {
                    position++;
                    block.yield(context, getRuntime().newFixnum(value));
                    value = bufferedStream.read();
                }
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
            return getRuntime().getNil();
        }
    }

    @JRubyClass(name="Zlib::GzipWriter", parent="Zlib::GzipFile")
    public static class RubyGzipWriter extends RubyGzipFile {
        protected static final ObjectAllocator GZIPWRITER_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyGzipWriter(runtime, klass);
            }
        };
        
        @JRubyMethod(name = "new", rest = true, meta = true)
        public static RubyGzipWriter newGzipWriter(IRubyObject recv, IRubyObject[] args, Block block) {
            RubyClass klass = (RubyClass)recv;
            
            RubyGzipWriter result = (RubyGzipWriter)klass.allocate();
            result.callInit(args, block);
            return result;
        }

        @JRubyMethod(required = 1, optional = 2, meta = true)
        public static IRubyObject open(final ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
            Ruby runtime = recv.getRuntime();
            IRubyObject level = runtime.getNil();
            IRubyObject strategy = runtime.getNil();

            if (args.length > 1) {
                level = args[1];
                checkLevel(context.getRuntime(), RubyNumeric.fix2int(level));
                if (args.length > 2) strategy = args[2];
            }

            IRubyObject io = RuntimeHelpers.invoke(context, runtime.getFile(), "open", args[0], runtime.newString("wb"));
            RubyGzipWriter gzio = newGzipWriter(recv, new IRubyObject[]{io, level, strategy}, block);
            return RubyGzipFile.wrapBlock(context, gzio, block);
        }
        
        private static void checkLevel(Ruby runtime, int level) {
            if (level < 0 || level > 9) {
                throw Util.newStreamError(runtime, "stream error: invalid level");
            }
        }

        public RubyGzipWriter(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        public class HeaderModifyableGZIPOutputStream extends DeflaterOutputStream {

            private IRubyObject io;
            private long position;
            private CRC32 checksum = new CRC32();
            private boolean headerIsWritten = false;
            private long modifiedTime = System.currentTimeMillis();
            private final static int DEFAULT_BUFFER_SIZE = 512;

            public HeaderModifyableGZIPOutputStream(IRubyObject io) throws IOException {
                super(new IOOutputStream(io, false, false), new Deflater(Deflater.DEFAULT_COMPRESSION, true), DEFAULT_BUFFER_SIZE);
                this.io = io;
                position = 0;
            }

            /**
             * We call internal IO#close directly, not via IOOutputStream#close.
             * IOInputStream#close directly invoke IO.getOutputStream().close()
             * for IO object instead of just calling IO#cloase of Ruby.
             * It causes EBADF at OpenFile#finalize.
             * TODO: implement this without IOOutputStream? Not so hard.
             */
            @Override
            public void close() throws IOException {
                // Do not invoke DeflaterOutputStream#close here.
                // following works as same as DeflaterOutputStream#close.
                //super.close();
                if (!closed) {
                    finish();
                    // out.close(); // we don't want to invoke IOInputStream#close for now.
                    closed = true;
                }
                // call IO#close instead.
                if (io.respondsTo("close")) {
                    io.callMethod(io.getRuntime().getCurrentContext(), "close");
                }
            }

            @Override
            public synchronized void write(byte bytes[], int offset, int length) throws IOException {
                writeHeaderIfNeeded();
                super.write(bytes, offset, length);
                checksum.update(bytes, offset, length);
                position += length;
            }

            @Override
            public void finish() throws IOException {
                writeHeaderIfNeeded();
                super.finish();
                out.write(Util.dumpTrailer(def.getTotalIn(), (int) checksum.getValue()));
            }

            public void setModifiedTime(long newModifiedTime) {
                modifiedTime = newModifiedTime;
            }

            public boolean headerIsWritten() {
                return headerIsWritten;
            }

            public long crc() {
                return checksum.getValue();
            }

            public long pos() {
                return position;
            }

            // Called before any write to make sure the
            // header is always written before the first bytes
            private void writeHeaderIfNeeded() throws IOException {
                if (headerIsWritten == false) {
                    out.write(Util.dumpHeader((nullFreeOrigName != null ? nullFreeOrigName.toString()
                            : null), (nullFreeComment != null ? nullFreeComment.toString() : null),
                            level, OS_CODE, modifiedTime));
                    headerIsWritten = true;
                }
            }
        }

        private HeaderModifyableGZIPOutputStream io;
        
        @JRubyMethod(required = 1, rest = true, visibility = PRIVATE)
        public IRubyObject initialize(IRubyObject[] args) {
            return initialize(args[0]);
        }
        
        @JRubyMethod(visibility = PRIVATE)
        public IRubyObject initialize(IRubyObject arg) {
            realIo = (RubyObject) arg;
            try {
                io = new HeaderModifyableGZIPOutputStream(realIo);
                return this;
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }

        @JRubyMethod(visibility = PRIVATE, compat = RUBY1_9)
        public IRubyObject initialize19(IRubyObject stream) {
            return initialize(stream);
        }

        @JRubyMethod(visibility = PRIVATE, compat = RUBY1_9)
        public IRubyObject initialize19(IRubyObject stream, IRubyObject options) {
            initialize(stream);
            return this;
        }

        @Override
        @JRubyMethod(name = "close")
        public IRubyObject close() {
            if (!closed) {
                try {
                    io.close();
                } catch (IOException ioe) {
                    throw getRuntime().newIOErrorFromException(ioe);
                }
            }
            this.closed = true;
            return getRuntime().getNil();
        }

        @JRubyMethod(name = {"append", "<<"}, required = 1)
        public IRubyObject append(IRubyObject p1) {
            this.write(p1);
            return this;
        }

        @JRubyMethod(name = "printf", required = 1, rest = true)
        public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
            write(RubyKernel.sprintf(context, this, args));
            return context.getRuntime().getNil();
        }

        @JRubyMethod(name = "print", rest = true)
        public IRubyObject print(IRubyObject[] args) {
            if (args.length != 0) {
                for (int i = 0, j = args.length; i < j; i++) {
                    write(args[i]);
                }
            }
            
            IRubyObject sep = getRuntime().getGlobalVariables().get("$\\");
            if (!sep.isNil()) {
                write(sep);
            }
            
            return getRuntime().getNil();
        }

        @JRubyMethod(name = {"pos", "tell"})
        public IRubyObject pos() {
            return RubyNumeric.int2fix(getRuntime(), io.pos());
        }

        @JRubyMethod(name = "orig_name=", required = 1)
        public IRubyObject set_orig_name(IRubyObject obj) {
            if (io.headerIsWritten()) {
                throw Util.newGzipFileError(getRuntime(), "header is already written");
            }
            nullFreeOrigName = obj.convertToString();
            ensureNonNull(nullFreeOrigName);
            return obj;
        }

        @JRubyMethod(name = "comment=", required = 1)
        public IRubyObject set_comment(IRubyObject obj) {
            if (io.headerIsWritten()) {
                throw Util.newGzipFileError(getRuntime(), "header is already written");
            }
            nullFreeComment = obj.convertToString();
            ensureNonNull(nullFreeComment);
            return obj;
        }

        private void ensureNonNull(RubyString obj) {
            String str = obj.toString();
            if (str.indexOf('\0') >= 0) {
                String trim = str.substring(0, str.toString().indexOf('\0'));
                obj.setValue(new ByteList(trim.getBytes()));
            }
        }

        @JRubyMethod(name = "putc", required = 1)
        public IRubyObject putc(IRubyObject p1) {
            try {
                io.write(RubyNumeric.fix2int(p1));
                return p1;
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }
        
        @JRubyMethod(name = "puts", rest = true)
        public IRubyObject puts(ThreadContext context, IRubyObject[] args) {
            RubyStringIO sio = (RubyStringIO)getRuntime().getClass("StringIO").newInstance(context, new IRubyObject[0], Block.NULL_BLOCK);
            sio.puts(context, args);
            write(sio.string());
            
            return getRuntime().getNil();
        }

        @Override
        public IRubyObject finish() {
            if (!finished) {
                try {
                    io.finish();
                } catch (IOException ioe) {
                    throw getRuntime().newIOErrorFromException(ioe);
                }
            }
            finished = true;
            return realIo;
        }

        @JRubyMethod(name = "flush", optional = 1)
        public IRubyObject flush(IRubyObject[] args) {
            if (args.length == 0 || args[0].isNil() || RubyNumeric.fix2int(args[0]) != 0) { // Zlib::NO_FLUSH
                try {
                    io.flush();
                } catch (IOException ioe) {
                    throw getRuntime().newIOErrorFromException(ioe);
                }
            }
            return getRuntime().getNil();
        }

        @JRubyMethod(name = "mtime=", required = 1)
        public IRubyObject set_mtime(IRubyObject arg) {
            if (io.headerIsWritten()) {
                throw Util.newGzipFileError(getRuntime(), "header is already written");
            }
            if (arg instanceof RubyTime) {
                this.mtime = ((RubyTime) arg);
            } else if (arg.isNil()) {
                // ...nothing
            } else {
                this.mtime.setDateTime(new DateTime(RubyNumeric.fix2long(arg) * 1000));
            }
            io.setModifiedTime(this.mtime.to_i().getLongValue());
            return getRuntime().getNil();
        }

        @Override
        @JRubyMethod(name = "crc")
        public IRubyObject crc() {
            return getRuntime().newFixnum(io.crc());
        }

        @JRubyMethod(name = "write", required = 1)
        public IRubyObject write(IRubyObject p1) {
            ByteList bytes = p1.asString().getByteList();
            try {
                io.write(bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
                return getRuntime().newFixnum(bytes.length());
            } catch (IOException ioe) {
                throw getRuntime().newIOErrorFromException(ioe);
            }
        }
    }
}
