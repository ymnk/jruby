package org.jruby.ext.zlib;

import static org.jruby.ext.zlib.Zlib.*;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.joda.time.DateTime;
import org.jruby.Ruby;
import org.jruby.RubyZlib;
import org.jruby.exceptions.RaiseException;

public class ZlibCustomInflaterInputStream extends InflaterInputStream {

    private final static int DEFAULT_BUFFER_SIZE = 512;
    // same as InputStream#in

    private String origName = null;
    private String comment = null;
    private int level = Z_DEFAULT_COMPRESSION;
    private byte osCode = OS_UNKNOWN;
    private DateTime modifiedTime = null;

    private CountingIOInputStream countingStream;
    private Callback callback;
    private CRC32 checksum = new CRC32();
    private boolean eof = false;
    
    public interface Callback {
        void onReadHeaderComplete(ZlibCustomInflaterInputStream stream);
    }

    /**
     * Offers header property in addition to GZIPInputStream.
     */
    public ZlibCustomInflaterInputStream(CountingIOInputStream io, Callback callback) {
        super(new BufferedInputStream(io), new Inflater(true), DEFAULT_BUFFER_SIZE);
        this.countingStream = io;
        this.callback = callback;
        readHeader();
        eof = false;
        checksum.reset();
    }

    public String getOrigName() {
        return origName;
    }

    public String getComment() {
        return comment;
    }

    public int getLevel() {
        return level;
    }

    public byte getOsCode() {
        return osCode;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    @Override
    public int read() throws IOException {
        if (eof) {
            return -1;
        }
        int ret = super.read();
        if (ret == -1) {
            readTrailer();
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
            readTrailer();
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
            readTrailer();
        } else {
            checksum.update(b, off, ret);
        }
        return ret;
    }

    /**
     * We call internal IO#close directly, not via IOInputStream#close.
     * IOInputStream#close directly invoke IO.getOutputStream().close() for IO
     * object instead of just calling IO#cloase of Ruby. It causes EBADF at
     * OpenFile#finalize. CAUTION: CountingIOInputStream#close does not called
     * even if it exists. TODO: implement this without IOInputStream? Not so
     * hard.
     */
    @Override
    public void close() throws IOException {
        // Do not invoke DeflaterOutputStream#close here.
        // super.close();
        // call IO#close instead.
        countingStream.closeInternal();
        eof = true;
    }

    public int pos() {
        return countingStream.pos();
    }

    public long crc() {
        return checksum.getValue();
    }

    private void readHeader() {
        checksum.reset();
        try {
            if ((byte) readUByte() != GZ_MAGIC_ID_1) {
                throw newGzipFileError(countingStream.getRuntime(), "not in gzip format");
            }
            if ((byte) readUByte() != GZ_MAGIC_ID_2) {
                throw newGzipFileError(countingStream.getRuntime(), "not in gzip format");
            }
            byte b = (byte) readUByte();
            if ((byte) b != GZ_METHOD_DEFLATE) {
                throw newGzipFileError(countingStream.getRuntime(),
                        "unsupported compression method " + b);
            }
            int flags = readUByte();
            if ((flags & GZ_FLAG_MULTIPART) != 0) {
                throw newGzipFileError(countingStream.getRuntime(),
                        "multi-part gzip file is not supported");
            } else if ((flags & GZ_FLAG_ENCRYPT) != 0) {
                throw newGzipFileError(countingStream.getRuntime(),
                        "encrypted gzip file is not supported");
            } else if ((flags & GZ_FLAG_UNKNOWN_MASK) != 0) {
                throw newGzipFileError(countingStream.getRuntime(), "unknown flags " + flags);
            }
            modifiedTime = new DateTime(readUInt() * 1000);
            int extraflags = readUByte();
            if ((extraflags & GZ_EXTRAFLAG_FAST) != 0) {
                level = Z_BEST_SPEED;
            } else if ((extraflags & GZ_EXTRAFLAG_SLOW) != 0) {
                level = Z_BEST_COMPRESSION;
            } else {
                level = Z_DEFAULT_COMPRESSION;
            }
            osCode = (byte) readUByte();
            if ((flags & GZ_FLAG_EXTRA) != 0) {
                int size = readUShort();
                byte[] extra = new byte[2 + size];
                // just discard it
                readBytes(extra);
            }
            if ((flags & GZ_FLAG_ORIG_NAME) != 0) {
                origName = readNullTerminateString();
            }
            if ((flags & GZ_FLAG_COMMENT) != 0) {
                comment = readNullTerminateString();
            }
        } catch (IOException ioe) {
            throw newGzipFileError(countingStream.getRuntime(), ioe.getMessage());
        }
        if (callback != null) {
            callback.onReadHeaderComplete(this);
        }
        // TODO: should check header CRC (cruby-zlib doesn't do for now)
    }

    private int readUByte() throws IOException {
        int ret = in.read();
        if (ret == -1) {
            throw new EOFException();
        } else {
            checksum.update((byte) (ret & 0xff));
        }
        return ret & 0xff;
    }

    private int readUShort() throws IOException {
        return (readUByte() | (readUByte() << 8)) & 0xffff;
    }

    private long readUInt() throws IOException {
        return (readUShort() | (readUShort() << 16)) & 0xffffffffL;
    }

    private void readBytes(byte[] bytes) throws IOException {
        readBytes(bytes, 0, bytes.length, true);
    }

    private void readBytes(byte[] bytes, int pos, int len, boolean updateChecksum)
            throws IOException {
        if (bytes.length < pos + len) {
            throw new IllegalArgumentException();
        }
        while (len > 0) {
            int ret = in.read(bytes, pos, len);
            if (ret == -1) {
                throw new EOFException();
            } else {
                if (updateChecksum) {
                    checksum.update(bytes, pos, ret);
                }
            }
            pos += ret;
            len -= ret;
        }
    }

    private String readNullTerminateString() throws IOException {
        StringBuilder builder = new StringBuilder();
        int c;
        while ((c = readUByte()) != '\0') {
            builder.append((char) c);
        }
        return builder.toString();
    }

    private void readTrailer() throws IOException {
        try {
            eof = true;
            int rest = 8;
            byte[] trailer = new byte[8];
            int remaining = super.inf.getRemaining();
            if (remaining > 0) {
                System.arraycopy(super.buf, super.len - remaining, trailer, 0, (remaining > 8) ? 8
                        : remaining);
                rest -= remaining;
            }
            if (rest > 0) {
                // Do not update checksum for trailer
                readBytes(trailer, 8 - rest, rest, false);
            }
            long uint = bytesToUInt(trailer, 0);
            if (uint != checksum.getValue()) {
                throw newCRCError(countingStream.getRuntime(),
                        "invalid compressed data -- crc error");
            }
            uint = bytesToUInt(trailer, 4);
            if (uint != (super.inf.getBytesWritten() & 0xffffffffL)) {
                throw newLengthError(countingStream.getRuntime(),
                        "invalid compressed data -- length error");
            }
        } catch (IOException ignored) {
            throw newNoFooter(countingStream.getRuntime(), "footer is not found");
        }
    }

    private long bytesToUInt(byte[] bytes, int pos) {
        if (bytes.length < pos + 4) {
            throw new IllegalArgumentException();
        }
        return (bytes[pos++] & 0xff | (bytes[pos++] & 0xff) << 8 | (bytes[pos++] & 0xff) << 16 | (bytes[pos++] & 0xff) << 24) & 0xffffffffL;
    }

    private static RaiseException newGzipFileError(Ruby runtime, String klass, String message) {
        return RubyZlib.RubyGzipFile.newGzipFileError(runtime, klass, message);
    }

    private static RaiseException newGzipFileError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "Error", message);
    }

    private static RaiseException newCRCError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "CRCError", message);
    }

    private static RaiseException newNoFooter(Ruby runtime, String message) {
        return newGzipFileError(runtime, "NoFooter", message);
    }

    private static RaiseException newLengthError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "LengthError", message);
    }
}
