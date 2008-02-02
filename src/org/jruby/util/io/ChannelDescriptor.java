package org.jruby.util.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import static java.util.logging.Logger.getLogger;
import java.util.zip.ZipEntry;
import org.jruby.RubyIO;
import org.jruby.util.ByteList;
import org.jruby.util.DirectoryAsFileException;
import org.jruby.util.IOModes;
import org.jruby.util.JRubyFile;
import org.jruby.util.Stream.BadDescriptorException;
import org.jruby.util.Stream.InvalidValueException;

public class ChannelDescriptor {
    private static final boolean DEBUG = false;
    
    public static final int RDONLY = 0x0000;
    public static final int WRONLY = 0x0001;
    public static final int RDWR = 0x0002;
    public static final int CREAT = 0x0100;
    public static final int EXCL = 0x0400;
    public static final int NOCTTY = 256; // what is this?
    public static final int TRUNC = 0x0200;
    public static final int APPEND = 0x0008;
    public static final int NONBLOCK = 0x0004;
    public static final int BINARY = 0x8000;
    public static final int ACCMODE = 0x10000;
    
    private Channel channel;
    private final int fileno;
    private final FileDescriptor fileDescriptor;
    private final IOModes originalModes;
    private AtomicInteger refCounter;
    
    private ChannelDescriptor(Channel channel, int fileno, IOModes originalModes, FileDescriptor fileDescriptor, AtomicInteger refCounter) {
        this.refCounter = refCounter;
        this.channel = channel;
        this.fileno = fileno;
        this.originalModes = originalModes;
        this.fileDescriptor = fileDescriptor;
    }

    public ChannelDescriptor(Channel channel, int fileno, IOModes originalModes, FileDescriptor fileDescriptor) {
        this(channel, fileno, originalModes, fileDescriptor, new AtomicInteger(1));
    }
    
    public ChannelDescriptor(Channel channel, int fileno, FileDescriptor fileDescriptor) throws InvalidValueException {
        this(channel, fileno, getModesFromChannel(channel), fileDescriptor);
    }
    
    /**
     * Mimics the libs dup(2) function, returning a new descriptor that references
     * the same open channel.
     * 
     * @return A duplicate ChannelDescriptor based on this one
     */
    public ChannelDescriptor dup() {
        synchronized (refCounter) {
            refCounter.incrementAndGet();

            int newFileno = RubyIO.getNewFileno();
            
            if (DEBUG) getLogger("ChannelDescriptor").info("Reopen fileno " + newFileno + ", refs now: " + refCounter.get());

            return new ChannelDescriptor(channel, newFileno, originalModes, fileDescriptor, refCounter);
        }
    }
    
    /**
     * Mimics the libc dup2(2) function for a "file descriptor" that's already
     * open.
     * 
     * @param other The descriptor into which we should dup this one
     */
    public void dup2(ChannelDescriptor other) {
        other.channel = channel;
    }
    
    /**
     * Mimics the libc dup2(2) function, returning a new descriptor that references
     * the same open channel but with a specified fileno.
     * 
     * @param fileno The fileno to use for the new descriptor
     * @return A duplicate ChannelDescriptor based on this one
     */
    public ChannelDescriptor dup2(int fileno) {
        synchronized (refCounter) {
            refCounter.incrementAndGet();

            if (DEBUG) getLogger("ChannelDescriptor").info("Reopen fileno " + fileno + ", refs now: " + refCounter.get());

            return new ChannelDescriptor(channel, fileno, originalModes, fileDescriptor, refCounter);
        }
    }
    
    public static IOModes getModesFromChannel(Channel channel) throws InvalidValueException {
        IOModes modes;
        if (channel instanceof ReadableByteChannel) {
            if (channel instanceof WritableByteChannel) {
                modes = new IOModes(RDWR);
            }
            modes = new IOModes(RDONLY);
        } else if (channel instanceof WritableByteChannel) {
            modes = new IOModes(WRONLY);
        } else {
            // FIXME: I don't like this
            modes = new IOModes(RDWR);
        }
        
        return modes;
    }

    public int getFileno() {
        return fileno;
    }
    
    public FileDescriptor getFileDescriptor() {
        return fileDescriptor;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isSeekable() {
        return channel instanceof FileChannel;
    }

    public boolean isWritable() {
        return channel instanceof WritableByteChannel;
    }
    
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    public void checkOpen() throws BadDescriptorException {
        if (!isOpen()) {
            throw new BadDescriptorException();
        }
    }

    public int read(int number, ByteList byteList) throws IOException, BadDescriptorException {
        checkOpen();
        
        ByteBuffer buffer = ByteBuffer.allocate(number);
        int bytesRead = read(buffer);

        byte[] ret;
        if (buffer.hasRemaining()) {
            buffer.flip();
            ret = new byte[buffer.remaining()];
            buffer.get(ret);
        } else {
            ret = buffer.array();
        }
        byteList.append(ret);

        return bytesRead;
    }

    public int read(ByteBuffer buffer) throws IOException, BadDescriptorException {
        checkOpen();
        
        ReadableByteChannel readChannel = (ReadableByteChannel) channel;
        int bytesRead = 0;
        bytesRead = readChannel.read(buffer);

        return bytesRead;
    }
    
    public int write(ByteList buf) throws IOException, BadDescriptorException {
        checkOpen();
        
        WritableByteChannel writeChannel = (WritableByteChannel)channel;
        
        return writeChannel.write(ByteBuffer.wrap(buf.unsafeBytes(), buf.begin(), buf.length()));
    }
    
    public int write(int c) throws IOException, BadDescriptorException {
        checkOpen();
        
        WritableByteChannel writeChannel = (WritableByteChannel)channel;
        
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(0, (byte)c);
        
        return writeChannel.write(buf);
    }
    
    public static ChannelDescriptor open(String cwd, String path, IOModes modes, int perm) throws FileNotFoundException, DirectoryAsFileException, FileExistsException, IOException {
        // FIXME: Do something with permissions
        
        if (path.equals("/dev/null")) {
            Channel nullChannel = new NullWritableChannel();
            // FIXME: don't use RubyIO for this
            return new ChannelDescriptor(nullChannel, RubyIO.getNewFileno(), modes, new FileDescriptor());
        } else if(path.startsWith("file:")) {
            String filePath = path.substring(5, path.indexOf("!"));
            String internalPath = path.substring(path.indexOf("!") + 2);

            if (!new File(filePath).exists()) {
                throw new FileNotFoundException(path);
            }
            
            JarFile jf = new JarFile(filePath);
            ZipEntry zf = jf.getEntry(internalPath);

            if(zf == null) {
                throw new FileNotFoundException(path);
            }

            InputStream is = jf.getInputStream(zf);
            // FIXME: don't use RubyIO for this
            return new ChannelDescriptor(Channels.newChannel(is), RubyIO.getNewFileno(), modes, new FileDescriptor());
        } else {
            JRubyFile theFile = JRubyFile.create(cwd,path);

            if (theFile.isDirectory() && modes.isWritable()) {
                throw new DirectoryAsFileException();
            }

            if (modes.isCreate()) {
                if (theFile.exists() && modes.isExclusive()) {
                    throw new FileExistsException(path);
                }
                theFile.createNewFile();
            } else {
                if (!theFile.exists()) {
                    throw new FileNotFoundException(path);
                }
            }

            // We always open this rw since we can only open it r or rw.
            RandomAccessFile file = new RandomAccessFile(theFile, modes.javaMode());

            if (modes.isTruncate()) file.setLength(0L);

            // TODO: append should set the FD to end, no? But there is no seek(int) in libc!
            //if (modes.isAppendable()) seek(0, Stream.SEEK_END);

            return new ChannelDescriptor(file.getChannel(), RubyIO.getNewFileno(), modes, file.getFD());
        }
    }
    
    public void close() throws BadDescriptorException, IOException {
        synchronized (refCounter) {
            // if refcount is at or below zero, we're no longer valid
            if (refCounter.get() <= 0) {
                throw new BadDescriptorException();
            }

            // if channel is already closed, we're no longer valid
            if (!channel.isOpen()) {
                throw new BadDescriptorException();
            }

            // otherwise decrement and possibly close as normal
            int count = refCounter.decrementAndGet();

            if (DEBUG) getLogger("ChannelDescriptor").info("Descriptor for fileno " + fileno + " refs: " + count);

            if (count <= 0) {
                channel.close();
            }
        }
    }
    
    public IOModes getOriginalModes() {
        return originalModes;
    }
    
    public void checkNewModes(IOModes newModes) throws InvalidValueException {
        if (!newModes.isSubsetOf(originalModes)) {
            throw new InvalidValueException();
        }
    }
    
    public static class FileExistsException extends IOException {
        public FileExistsException(String path) {
            super("file exists: " + path);
        }
    }
}
