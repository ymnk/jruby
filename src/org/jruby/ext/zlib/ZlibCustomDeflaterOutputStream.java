package org.jruby.ext.zlib;

import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOOutputStream;

import static org.jruby.ext.zlib.Zlib.*;

public class ZlibCustomDeflaterOutputStream extends DeflaterOutputStream {
    private String origName;
    private String comment;
    private int level = Z_DEFAULT_COMPRESSION;
    private long modifiedTime = System.currentTimeMillis();

    private IRubyObject io;

    private long position;
    private CRC32 checksum = new CRC32();
    private boolean headerIsWritten = false;
    private final static int DEFAULT_BUFFER_SIZE = 512;

    public ZlibCustomDeflaterOutputStream(IRubyObject io) throws IOException {
        super(new IOOutputStream(io, false, false),
                new Deflater(Deflater.DEFAULT_COMPRESSION, true), DEFAULT_BUFFER_SIZE);
        this.io = io;
        position = 0;
    }

    /**
     * We call internal IO#close directly, not via IOOutputStream#close.
     * IOInputStream#close directly invoke IO.getOutputStream().close() for IO
     * object instead of just calling IO#cloase of Ruby. It causes EBADF at
     * OpenFile#finalize. TODO: implement this without IOOutputStream? Not so
     * hard.
     */
    @Override
    public void close() throws IOException {
        // Do not invoke DeflaterOutputStream#close here.
        // super.close();
        // following should work as same as DeflaterOutputStream#close.
        finish();
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
        writeTrailer();
    }
    
    public void setOrigName(String origName) {
        this.origName = origName;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public void setLevel(int level) {
        this.level = level;
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
            writeHeader();
            headerIsWritten = true;
        }
    }

    private void writeHeader() throws IOException {
        // See http://www.gzip.org/zlib/rfc-gzip.html
        byte flags = 0, extraflags = 0;
        if (origName != null) {
            flags |= GZ_FLAG_ORIG_NAME;
        }
        if (comment != null) {
            flags |= GZ_FLAG_COMMENT;
        }
        if (level == Z_BEST_SPEED) {
            extraflags |= GZ_EXTRAFLAG_FAST;
        } else if (level == Z_BEST_COMPRESSION) {
            extraflags |= GZ_EXTRAFLAG_SLOW;
        }
        final byte header[] = { GZ_MAGIC_ID_1, GZ_MAGIC_ID_2, GZ_METHOD_DEFLATE, flags,
                // 4 bytes of modified time
                (byte) (modifiedTime), (byte) (modifiedTime >> 8), (byte) (modifiedTime >> 16),
                (byte) (modifiedTime >> 24), extraflags, OS_CODE };
        out.write(header);
        if (origName != null) {
            out.write(origName.getBytes());
            out.write('\0');
        }
        if (comment != null) {
            out.write(comment.getBytes());
            out.write('\0');
        }
    }

    private void writeTrailer() throws IOException {
        final int originalDataSize = def.getTotalIn();
        final int checksumInt = (int) checksum.getValue();

        final byte[] trailer = { (byte) (checksumInt), (byte) (checksumInt >> 8),
                (byte) (checksumInt >> 16), (byte) (checksumInt >> 24),

                (byte) (originalDataSize), (byte) (originalDataSize >> 8),
                (byte) (originalDataSize >> 16), (byte) (originalDataSize >> 24) };

        out.write(trailer);
    }
}
