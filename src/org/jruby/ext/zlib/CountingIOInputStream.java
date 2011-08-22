package org.jruby.ext.zlib;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IOInputStream;

/**
 * IOInputStream wrapper for counting and keeping reading position.
 */
public class CountingIOInputStream extends IOInputStream {

    private int position;
    private IRubyObject io;

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
    
    void closeInternal() {
        if (io.respondsTo("close")) {
            io.callMethod(getRuntime().getCurrentContext(), "close");
        }
    }
}
