/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.util.io.ChannelDescriptor;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.jruby.anno.JRubyMethod;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Stream;
import org.jruby.util.Stream.InvalidValueException;
import org.jruby.util.Stream.PipeException;
import org.jruby.util.IOModes;
import org.jruby.util.ShellLauncher;
import org.jruby.util.TypeConverter;
import org.jruby.util.Stream.BadDescriptorException;
import org.jruby.util.ChannelStream;

/**
 * 
 * @author jpetersen
 */
public class RubyIO extends RubyObject {
    public enum STDIO {
        IN, OUT, ERR;
        
        public int fileno() {
            switch (this) {
            case IN: return 0;
            case OUT: return 1;
            case ERR: return 2;
            default: throw new RuntimeException();
            }
        }
        
        public static boolean isSTDIO(int fileno) {
            if (fileno >= 0 && fileno <= 2) return true;
            
            return false;
        }
    }
    
    public static class OpenFile {
        public static final int READABLE  = 1;
        public static final int WRITABLE  = 2;
        public static final int READWRITE = 3;
        public static final int APPEND    = 64;
        public static final int CREATE    = 128;
        public static final int BINMODE   = 4;
        public static final int SYNC      = 8;
        public static final int WBUF      = 16;
        public static final int RBUF      = 32;
        public static final int WSPLIT    = 0x200;
        public static final int WSPLIT_INITIALIZED = 0x400;
        public static final int SYNCWRITE = (SYNC|WRITABLE);
        
        public static interface Finalizer {
            public void finalize(Ruby runtime, boolean raise);
        }
        
        private Stream mainStream;
        private Stream pipeStream;
        private int mode;
        private Process process;
        private int lineNumber = 0;
        private String path;
        private Finalizer finalizer;

        public Stream getMainStream() {
            return mainStream;
        }

        public void setMainStream(Stream mainStream) {
            this.mainStream = mainStream;
        }

        public Stream getPipeStream() {
            return pipeStream;
        }

        public void setPipeStream(Stream pipeStream) {
            this.pipeStream = pipeStream;
        }
        
        public Stream getWriteStream() {
            return pipeStream == null ? mainStream : pipeStream;
        }

        public int getMode() {
            return mode;
        }
        
        public String getModeAsString(Ruby runtime) {
            String modeString = getStringFromMode(mode);
            
            if (modeString == null) {
                throw runtime.newArgumentError("Illegal access modenum " + Integer.toOctalString(mode));
            }
            
            return modeString;
        }
        
        public static String getStringFromMode(int mode) {
            if ((mode & APPEND) != 0) {
                if ((mode & READWRITE) != 0) {
                    return "ab+";
                }
                return "ab";
            }
            switch (mode & READWRITE) {
              case READABLE:
                return "rb";
              case WRITABLE:
                return "wb";
              case READWRITE:
                if ((mode & CREATE) != 0) {
                    return "wb+";
                }
                return "rb+";
            }
            return null;
        }
        
        public IOModes getModeAsIOModes(Ruby runtime) throws InvalidValueException {
            return getModeAsIOModes(runtime, mode);
        }
        
        public static IOModes getModeAsIOModes(Ruby runtime, int mode) throws InvalidValueException {
            return new IOModes(getIOModesIntFromString(runtime, getStringFromMode(mode)));
        }
        
        public void checkReadable(Ruby runtime) throws IOException, BadDescriptorException, PipeException, InvalidValueException {
            checkClosed(runtime);
            
            if ((mode & READABLE) == 0) {
                throw runtime.newIOError("not opened for reading");
            }
            
            if (
                    ((mode & WBUF) != 0 || (mode & (SYNCWRITE|RBUF)) == SYNCWRITE)
                    && !mainStream.feof()
                    && pipeStream == null) {
                seek(0, Stream.SEEK_CUR);
            }
            
            mode |= RBUF;
        }
        
        public void seek(long offset, int whence) throws IOException, InvalidValueException, PipeException, BadDescriptorException {
            flushBeforeSeek();
            
            getWriteStream().fseek(offset, whence);
        }
        
        private void flushBeforeSeek() throws BadDescriptorException, IOException {
            if ((mode & WBUF) != 0) {
                fflush(getWriteStream());
            }
        }
        
        private void fflush(Stream stream) throws IOException, BadDescriptorException {
            while (true) {
                int n = stream.fflush();
                if (n != -1) break;
                
                // TODO: Ruby waits until the target becomes writable again through io_wait_writable (a select) here
                //stream.getDescriptor().waitUntilWritable();
            }
            mode &= ~WBUF;
        }
        
        public void checkWritable(Ruby runtime) throws IOException, BadDescriptorException, InvalidValueException, PipeException {
            checkClosed(runtime);
            if ((mode & WRITABLE) == 0) {
                throw runtime.newIOError("not opened for writing");
            }
            if ((mode & RBUF) != 0 && !mainStream.feof() && pipeStream == null) {
                seek(0, Stream.SEEK_CUR);
            }
            if (pipeStream == null) {
                mode &= ~RBUF;
            }
        }
        
        public void checkClosed(Ruby runtime) {
            if (mainStream == null && pipeStream == null) {
                throw runtime.newIOError("closed stream");
            }
        }
        
        public boolean isOpen() {
            return mainStream != null || pipeStream != null;
        }
        
        public boolean isReadable() {
            return (mode & READABLE) != 0;
        }
        
        public boolean isWritable() {
            return (mode & WRITABLE) != 0;
        }
        
        public boolean isReadBuffered() {
            return (mode & RBUF) != 0;
        }
        
        public void setReadBuffered() {
            mode |= RBUF;
        }
        
        public boolean isWriteBuffered() {
            return (mode & WBUF) != 0;
        }
        
        public void setWriteBuffered() {
            mode |= WBUF;
        }
        
        public boolean isSync() {
            return (mode & SYNC) != 0;
        }
        
        public boolean areBothEOF() throws IOException, BadDescriptorException {
            return mainStream.feof() && (pipeStream != null ? pipeStream.feof() : true);
        }

        public void setMode(int modes) {
            this.mode = modes;
        }

        public Process getProcess() {
            return process;
        }

        public void setProcess(Process process) {
            this.process = process;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Finalizer getFinalizer() {
            return finalizer;
        }

        public void setFinalizer(Finalizer finalizer) {
            this.finalizer = finalizer;
        }
        
        public void cleanup(Ruby runtime, boolean raise) {
            if (finalizer != null) {
                finalizer.finalize(runtime, raise);
            } else {
                finalize(runtime, raise);
            }
        }
        
        public void finalize(Ruby runtime, boolean raise) {
            try {
                ChannelDescriptor main = null, pipe = null;
                
                if (pipeStream != null) {
                    pipe = pipeStream.getDescriptor();
                    
                    // TODO: Ruby logic is somewhat more complicated here, see comments after
                    try {
                        pipeStream.fflush();
                        pipeStream.fclose();
                    } finally {
                        // make sure the pipe stream is set to null
                        pipeStream = null;
                    }
    //                f2 = fileno(fptr->f2);
    //                while (n2 = 0, fflush(fptr->f2) < 0) {
    //                    n2 = errno;
    //                    if (!rb_io_wait_writable(f2)) {
    //                        break;
    //                    }
    //                    if (!fptr->f2) break;
    //                }
    //                if (fclose(fptr->f2) < 0 && n2 == 0) {
    //                    n2 = errno;
    //                }
    //                fptr->f2 = 0;
                }

                if (mainStream != null) {
                    // TODO: Ruby logic is somewhat more complicated here, see comments after
                    main = mainStream.getDescriptor();
                    
                    try {
                        if (pipe == null && isWriteBuffered()) {
                            mainStream.fflush();
                            // TODO: loop as shown here
        //                    while (n1 = 0, fflush(fptr->f) < 0) {
        //                        n1 = errno;
        //                        if (!rb_io_wait_writable(f1)) break;
        //                        if (!fptr->f) break;
        //                    }
                        }
                        mainStream.fclose();
                        // TODO: logic as shown here for errno? Maybe our propagated exceptions are enough
        //                if (fclose(fptr->f) < 0 && n1 == 0) {
        //                    n1 = errno;
        //                }
                    } catch (BadDescriptorException bde) {
                        if (main == pipe) {
                            // we ignore, since we've already closed it and we're happy
                        }
                    } finally {
                        // make sure the main stream is set to null
                        mainStream = null;
                    }
                }
                
                // TODO: handle cases where the streams can't be flushed/closed?
    //            if (!noraise && (n1 || n2)) {
    //                errno = (n1 ? n1 : n2);
    //                rb_sys_fail(fptr->path);
    //            }
            } catch (IOException ex) {
                if (raise) {
                    throw runtime.newIOErrorFromException(ex);
                }
            } catch (BadDescriptorException ex) {
                if (raise) {
                    throw runtime.newErrnoEBADFError();
                }
            } finally {}
        }
    }
    
    protected OpenFile openFile;
    
    // Does THIS IO object think it is still open
    // as opposed to the IO Handler which knows the
    // actual truth.  If two IO objects share the
    // same IO Handler, then it is possible for
    // one object to think that the handler is open
    // when it really isn't.  Keeping track of this yields
    // the right errors.
    private boolean atEOF = false;

    /*
     * Random notes:
     *  
     * 1. When a second IO object is created with the same fileno odd
     * concurrency issues happen when the underlying implementation
     * commits data.   So:
     * 
     * f = File.new("some file", "w")
     * f.puts("heh")
     * g = IO.new(f.fileno)
     * g.puts("hoh")
     * ... more operations of g and f ...
     * 
     * Will generate a mess in "some file".  The problem is that most
     * operations are buffered.  When those buffers flush and get
     * written to the physical file depends on the implementation
     * (semantically I would think that it should be last op wins -- but 
     * it isn't).  I doubt java could mimic ruby in this way.  I also 
     * doubt many people are taking advantage of this.  How about 
     * syswrite/sysread though?  I think the fact that sysread/syswrite 
     * are defined to be a low-level system calls, allows implementations 
     * to be somewhat different?
     * 
     * 2. In the case of:
     * f = File.new("some file", "w")
     * f.puts("heh")
     * print f.pos
     * g = IO.new(f.fileno)
     * print g.pos
     * Both printed positions will be the same.  But:
     * f = File.new("some file", "w")
     * f.puts("heh")
     * g = IO.new(f.fileno)
     * print f.pos, g.pos
     * won't be the same position.  Seem peculiar enough not to touch
     * (this involves pos() actually causing a seek?)
     * 
     * 3. All IO objects reference a IOHandler.  If multiple IO objects
     * have the same fileno, then they also share the same IOHandler.
     * It is possible that some IO objects may share the same IOHandler
     * but not have the same permissions.  However, all subsequent IO
     * objects created after the first must be a subset of the original
     * IO Object (see below for an example). 
     *
     * The idea that two or more IO objects can have different access
     * modes means that IO objects must keep track of their own
     * permissions.  In addition the IOHandler itself must know what
     * access modes it has.
     * 
     * The above sharing situation only occurs in a situation like:
     * f = File.new("some file", "r+")
     * g = IO.new(f.fileno, "r")
     * Where g has reduced (subset) permissions.
     * 
     * On reopen, the fileno's IOHandler gets replaced by a new handler. 
     */
    
    /*
     * I considered making all callers of this be moved into IOHandlers
     * constructors (since it would be less error prone to forget there).
     * However, reopen() makes doing this a little funky. 
     */
    public void registerDescriptor(ChannelDescriptor descriptor) {
        getRuntime().getDescriptors().put(new Integer(descriptor.getFileno()), new WeakReference<ChannelDescriptor>(descriptor));
    }
    
    public void unregisterDescriptor(int aFileno) {
        getRuntime().getDescriptors().remove(new Integer(aFileno));
    }
    
    public ChannelDescriptor getDescriptorByFileno(int aFileno) {
        Reference<ChannelDescriptor> reference = getRuntime().getDescriptors().get(new Integer(aFileno));
        if (reference == null) {
            return null;
        }
        return (ChannelDescriptor) reference.get();
    }
    
    // FIXME can't use static; would interfere with other runtimes in the same JVM
    protected static int filenoIndex = 2;
    
    public static int getNewFileno() {
        filenoIndex++;
        
        return filenoIndex;
    }

    // This should only be called by this and RubyFile.
    // It allows this object to be created without a IOHandler.
    public RubyIO(Ruby runtime, RubyClass type) {
        super(runtime, type);
        
        openFile = new OpenFile();
    }

    public RubyIO(Ruby runtime, OutputStream outputStream) {
        super(runtime, runtime.getIO());
        
        // We only want IO objects with valid streams (better to error now). 
        if (outputStream == null) {
            throw runtime.newIOError("Opening invalid stream");
        }
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(new ChannelStream(runtime, new ChannelDescriptor(Channels.newChannel(outputStream), getNewFileno(), new FileDescriptor())));
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(OpenFile.WRITABLE | OpenFile.APPEND);
        
        registerDescriptor(openFile.getMainStream().getDescriptor());
    }
    
    public RubyIO(Ruby runtime, InputStream inputStream) {
        super(runtime, runtime.getIO());
        
        if (inputStream == null) {
            throw runtime.newIOError("Opening invalid stream");
        }
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(new ChannelStream(runtime, new ChannelDescriptor(Channels.newChannel(inputStream), getNewFileno(), new FileDescriptor())));
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(OpenFile.READABLE);
        
        registerDescriptor(openFile.getMainStream().getDescriptor());
    }
    
    public RubyIO(Ruby runtime, Channel channel) {
        super(runtime, runtime.getIO());
        
        // We only want IO objects with valid streams (better to error now). 
        if (channel == null) {
            throw runtime.newIOError("Opening invalid stream");
        }
        
        openFile = new OpenFile();
        
        try {
            openFile.setMainStream(new ChannelStream(runtime, new ChannelDescriptor(channel, getNewFileno(), new FileDescriptor())));
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(openFile.getMainStream().getModes().getOpenFileFlags());
        
        registerDescriptor(openFile.getMainStream().getDescriptor());
    }

    public RubyIO(Ruby runtime, Process process, IOModes modes) {
    	super(runtime, runtime.getIO());
        
        openFile = new OpenFile();
        
        openFile.setMode(modes.getOpenFileFlags() | OpenFile.SYNC);
        openFile.setProcess(process);

        try {
            InputStream pipeIn = process.getInputStream();
            ChannelDescriptor main = new ChannelDescriptor(
                    Channels.newChannel(pipeIn),
                    getNewFileno(),
                    new FileDescriptor());
            
            OutputStream pipeOut = process.getOutputStream();
            ChannelDescriptor pipe = new ChannelDescriptor(
                    Channels.newChannel(pipeOut),
                    getNewFileno(),
                    new FileDescriptor());
            
            if (!openFile.isReadable()) {
                main.close();
                pipeIn.close();
            } else {
                openFile.setMainStream(new ChannelStream(getRuntime(), main));
            }
            
            if (!openFile.isWritable()) {
                pipe.close();
                pipeOut.close();
            } else {
                if (openFile.getMainStream() != null) {
                    openFile.setPipeStream(new ChannelStream(getRuntime(), pipe));
                } else {
                    openFile.setMainStream(new ChannelStream(getRuntime(), pipe));
                }
            }
            
            registerDescriptor(main);
            registerDescriptor(pipe);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
    }
    
    public RubyIO(Ruby runtime, STDIO stdio) {
        super(runtime, runtime.getIO());
        
        openFile = new OpenFile();

        try {
            switch (stdio) {
            case IN:
                openFile.setMainStream(
                        new ChannelStream(
                            runtime, 
                            new ChannelDescriptor(Channels.newChannel(runtime.getIn()), 0, new IOModes(IOModes.RDONLY), FileDescriptor.in),
                            FileDescriptor.in));
                break;
            case OUT:
                openFile.setMainStream(
                        new ChannelStream(
                            runtime, 
                            new ChannelDescriptor(Channels.newChannel(runtime.getOut()), 1, new IOModes(IOModes.WRONLY | IOModes.APPEND), FileDescriptor.out),
                            FileDescriptor.out));
                openFile.getMainStream().setSync(true);
                break;
            case ERR:
                openFile.setMainStream(
                        new ChannelStream(
                            runtime, 
                            new ChannelDescriptor(Channels.newChannel(runtime.getErr()), 2, new IOModes(IOModes.WRONLY | IOModes.APPEND), FileDescriptor.out), 
                            FileDescriptor.err));
                openFile.getMainStream().setSync(true);
                break;
            }
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        openFile.setMode(openFile.getMainStream().getModes().getOpenFileFlags());
        
        registerDescriptor(openFile.getMainStream().getDescriptor());        
    }
    
    private static ObjectAllocator IO_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyIO(runtime, klass);
        }
    };

    public static RubyClass createIOClass(Ruby runtime) {
        RubyClass ioClass = runtime.defineClass("IO", runtime.getObject(), IO_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyIO.class);   
        ioClass.kindOf = new RubyModule.KindOf() {
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyIO;
                }
            };

        ioClass.includeModule(runtime.getEnumerable());
        
        // TODO: Implement tty? and isatty.  We have no real capability to
        // determine this from java, but if we could set tty status, then
        // we could invoke jruby differently to allow stdin to return true
        // on this.  This would allow things like cgi.rb to work properly.
        
        ioClass.defineAnnotatedMethods(RubyIO.class);

        // Constants for seek
        ioClass.fastSetConstant("SEEK_SET", runtime.newFixnum(Stream.SEEK_SET));
        ioClass.fastSetConstant("SEEK_CUR", runtime.newFixnum(Stream.SEEK_CUR));
        ioClass.fastSetConstant("SEEK_END", runtime.newFixnum(Stream.SEEK_END));
        
        ioClass.dispatcher = callbackFactory.createDispatcher(ioClass);

        return ioClass;
    }

    public OutputStream getOutStream() {
        return openFile.getMainStream().newOutputStream();
    }

    public InputStream getInStream() {
        return openFile.getMainStream().newInputStream();
    }

    public Channel getChannel() {
        if (openFile.getMainStream() instanceof ChannelStream) {
            return ((ChannelStream) openFile.getMainStream()).getDescriptor().getChannel();
        } else {
            return null;
        }
    }
    
    public Stream getHandler() {
        return openFile.getMainStream();
    }

    @JRubyMethod(name = "reopen", required = 1, optional = 1)
    public IRubyObject reopen(IRubyObject[] args) throws InvalidValueException {
    	if (args.length < 1) {
            throw getRuntime().newArgumentError("wrong number of arguments");
    	}
    	
    	IRubyObject tmp = TypeConverter.convertToTypeWithCheck(args[0],
    	        getRuntime().getIO(), MethodIndex.getIndex("to_io"), "to_io");
    	if (!tmp.isNil()) {
            try {
                RubyIO ios = (RubyIO) tmp;

                if (ios.openFile == this.openFile) {
                    return this;
                }

                OpenFile originalFile = ios.openFile;
                OpenFile selfFile = openFile;

                long pos = 0;
                if (originalFile.isReadable()) {
                    pos = originalFile.getMainStream().fgetpos();
                }

                if (originalFile.getPipeStream() != null) {
                    originalFile.getPipeStream().fflush();
                } else if (originalFile.isWritable()) {
                    originalFile.getMainStream().fflush();
                }

                if (selfFile.isWritable()) {
                    if (selfFile.getPipeStream() != null) {
                        selfFile.getPipeStream().fflush();
                    } else {
                        selfFile.getMainStream().fflush();
                    }
                }

                selfFile.setMode(originalFile.getMode());
                selfFile.setProcess(originalFile.getProcess());
                selfFile.setLineNumber(originalFile.getLineNumber());
                selfFile.setPath(originalFile.getPath());
                selfFile.setFinalizer(originalFile.getFinalizer());

                ChannelDescriptor selfDescriptor = selfFile.getMainStream().getDescriptor();
                ChannelDescriptor originalDescriptor = originalFile.getMainStream().getDescriptor();

                // confirm we're not reopening self's channel
                if (selfDescriptor.getChannel() != originalDescriptor.getChannel()) {
                    // check if we're a stdio IO, and ensure we're not badly mutilated
                    if (selfDescriptor.getFileno() >=0 && selfDescriptor.getFileno() <= 2) {
                        // dup2 new fd into self to preserve fileno and references to it
                        originalDescriptor.dup2(selfDescriptor);
                        
                        // re-register, since fileno points at something new now
                        registerDescriptor(selfFile.getMainStream().getDescriptor());
                    } else {
                        Stream pipeFile = selfFile.getPipeStream();
                        int mode = selfFile.getMode();
                        selfFile.getMainStream().fclose();
                        selfFile.setPipeStream(null);

                        // TODO: turn off readable? am I reading this right?
                        // This only seems to be used while duping below, since modes gets
                        // reset to actual modes afterward
                        //fptr->mode &= (m & FMODE_READABLE) ? ~FMODE_READABLE : ~FMODE_WRITABLE;

                        if (pipeFile != null) {
                            selfFile.setMainStream(ChannelStream.fdopen(getRuntime(), originalDescriptor, new IOModes()));
                            selfFile.setPipeStream(pipeFile);
                        } else {
                            selfFile.setMainStream(
                                    new ChannelStream(
                                        getRuntime(),
                                        originalDescriptor.dup2(selfDescriptor.getFileno())));
                            
                            // re-register the descriptor
                            registerDescriptor(selfFile.getMainStream().getDescriptor());
                            
                            // since we're not actually duping the incoming channel into our handler, we need to
                            // copy the original sync behavior from the other handler
                            selfFile.getMainStream().setSync(selfFile.getMainStream().isSync());
                        }
                        selfFile.setMode(mode);
                    }
                    // TODO: anything threads attached to original fd are notified of the close...
                    // see rb_thread_fd_close
                    
                    if (originalFile.isReadable() && pos >= 0) {
                        selfFile.seek(pos, Stream.SEEK_SET);
                        originalFile.seek(pos, Stream.SEEK_SET);
                    }
                }

                // TODO: more pipe logic
    //            if (fptr->f2 && fd != fileno(fptr->f2)) {
    //                fd = fileno(fptr->f2);
    //                if (!orig->f2) {
    //                    fclose(fptr->f2);
    //                    rb_thread_fd_close(fd);
    //                    fptr->f2 = 0;
    //                }
    //                else if (fd != (fd2 = fileno(orig->f2))) {
    //                    fclose(fptr->f2);
    //                    rb_thread_fd_close(fd);
    //                    if (dup2(fd2, fd) < 0)
    //                        rb_sys_fail(orig->path);
    //                    fptr->f2 = rb_fdopen(fd, "w");
    //                }
    //            }
                
                // TODO: restore binary mode
    //            if (fptr->mode & FMODE_BINMODE) {
    //                rb_io_binmode(io);
    //            }
                
                // TODO: set our metaclass to target's class (i.e. scary!)

            } catch (IOException ex) { // TODO: better error handling
                throw getRuntime().newIOError("could not reopen: " + ex.getMessage());
            } catch (BadDescriptorException ex) {
                throw getRuntime().newIOError("could not reopen: " + ex.getMessage());
            } catch (PipeException ex) {
                throw getRuntime().newIOError("could not reopen: " + ex.getMessage());
            }
        } else {
            IRubyObject pathString = args[0].convertToString();
            
            // TODO: check safe, taint on incoming string
            
            if (openFile == null) {
                openFile = new OpenFile();
            }
            
            try {
                IOModes modes;
                if (args.length > 1) {
                    IRubyObject modeString = args[1].convertToString();
                    modes = getIOModes(getRuntime(), modeString.toString());

                    openFile.setMode(modes.getOpenFileFlags());
                } else {
                    modes = getIOModes(getRuntime(), "r");
                }

                String path = pathString.toString();
                
                // Ruby code frequently uses a platform check to choose "NUL:" on windows
                // but since that check doesn't work well on JRuby, we help it out
                if ("/dev/null".equals(path) || System.getProperty("os.name").contains("Windows")) {
                    path = "NUL:";
                }
                
                openFile.setPath(path);
            
                if (openFile.getMainStream() == null) {
                    openFile.setMainStream(ChannelStream.fopen(getRuntime(), path, modes));
                    
                    registerDescriptor(openFile.getMainStream().getDescriptor());
                    if (openFile.getPipeStream() != null) {
                        openFile.getPipeStream().fclose();
                        unregisterDescriptor(openFile.getPipeStream().getDescriptor().getFileno());
                        openFile.setPipeStream(null);
                    }
                    return this;
                } else {
                    // TODO: This is an freopen in MRI, this is close, but not quite the same
                    openFile.getMainStream().freopen(path, getIOModes(getRuntime(), openFile.getModeAsString(getRuntime())));

                    // re-register
                    registerDescriptor(openFile.getMainStream().getDescriptor());

                    if (openFile.getPipeStream() != null) {
                        // TODO: pipe handler to be reopened with path and "w" mode
                    }
                }
            } catch (PipeException pe) {
                throw getRuntime().newErrnoEPIPEError();
            } catch (IOException ex) {
                throw getRuntime().newIOErrorFromException(ex);
            } catch (BadDescriptorException ex) {
                throw getRuntime().newErrnoEBADFError();
            } catch (Stream.InvalidValueException e) {
            	throw getRuntime().newErrnoEINVALError();
            }
        }
        
        // A potentially previously close IO is being 'reopened'.
        return this;
    }
    
    public static IOModes getIOModes(Ruby runtime, String modesString) throws InvalidValueException {
        return new IOModes(getIOModesIntFromString(runtime, modesString));
    }
        
    public static int getIOModesIntFromString(Ruby runtime, String modesString) {
        int modes = 0;
        int length = modesString.length();

        if (length == 0) {
            throw runtime.newArgumentError("illegal access mode");
        }

        switch (modesString.charAt(0)) {
        case 'r' :
            modes |= IOModes.RDONLY;
            break;
        case 'a' :
            modes |= IOModes.APPEND | IOModes.WRONLY | IOModes.CREAT;
            break;
        case 'w' :
            modes |= IOModes.WRONLY | IOModes.TRUNC | IOModes.CREAT;
            break;
        default :
            throw runtime.newArgumentError("illegal access mode " + modes);
        }

        for (int n = 1; n < length; n++) {
            switch (modesString.charAt(n)) {
            case 'b':
                modes |= IOModes.BINARY;
                break;
            case '+':
                modes = (modes & ~IOModes.ACCMODE) | IOModes.RDWR;
                break;
            default:
                throw runtime.newArgumentError("illegal access mode " + modes);
            }
        }

        return modes;
    }
    
    private ByteList getSeparatorForGets(IRubyObject[] args) {
        IRubyObject sepVal;

        if (args.length > 0) {
            sepVal = args[0];
        } else {
            sepVal = getRuntime().getRecordSeparatorVar().get();
        }
        
        ByteList separator = sepVal.isNil() ? null : ((RubyString) sepVal).getByteList();

        if (separator != null && separator.realSize == 0) {
            separator = Stream.PARAGRAPH_DELIMETER;
        }
        
        return separator;
    }

    /** Read a line.
     * 
     */
    // TODO: Most things loop over this and always pass it the same arguments
    // meaning they are an invariant of the loop.  Think about fixing this.
    public IRubyObject getline(IRubyObject[] args) {
        return getline(getSeparatorForGets(args));
    }

    public IRubyObject getline(ByteList separator) {
        try {
            openFile.checkReadable(getRuntime());
        
            ByteList newLine = openFile.getMainStream().fgets(separator);

            if (newLine != null) {
                openFile.setLineNumber(openFile.getLineNumber() + 1);
                getRuntime().getGlobalVariables().set("$.", getRuntime().newFixnum(openFile.getLineNumber()));
                RubyString result = RubyString.newString(getRuntime(), newLine);
                result.taint();

                return result;
            }
		    
            return getRuntime().getNil();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (EOFException e) {
            return getRuntime().getNil();
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }
    // IO class methods.

    @JRubyMethod(name = {"new"}, rest = true, frame = true, meta = true)
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyClass klass = (RubyClass)recv;
        
        if (block.isGiven()) {
            String className = klass.getName();
            recv.getRuntime().getWarnings().warn(className + "::new() does not take block; use " + className + "::open() instead");
        }
        
        return klass.newInstance(args, block);
    }

    @JRubyMethod(name = "initialize", required = 1, optional = 1, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        int argCount = args.length;
        IOModes modes;
        
        int fileno = RubyNumeric.fix2int(args[0]);
        
        try {
            ChannelDescriptor descriptor = getDescriptorByFileno(fileno);
            
            if (descriptor == null) {
                throw getRuntime().newErrnoEBADFError();
            }
            
            descriptor.checkOpen();
            
            if (argCount == 2) {
                if (args[1] instanceof RubyFixnum) {
                    modes = new IOModes(RubyFixnum.fix2long(args[1]));
                } else {
                    modes = getIOModes(getRuntime(), (args[1].convertToString().toString()));
                }
            } else {
                // use original modes
                modes = descriptor.getOriginalModes();
            }

            openFile.setMode(modes.getOpenFileFlags());
        
            openFile.setMainStream(fdopen(descriptor, modes));
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException ive) {
            throw getRuntime().newErrnoEINVALError();
        }
        
        return this;
    }
    
    protected Stream fdopen(ChannelDescriptor existingDescriptor, IOModes modes) throws InvalidValueException {
        // See if we already have this descriptor open.
        // If so then we can mostly share the handler (keep open
        // file, but possibly change the mode).
        
        if (existingDescriptor == null) {
            // redundant, done above as well
            
            // this seems unlikely to happen unless it's a totally bogus fileno
            // ...so do we even need to bother trying to create one?
            
            // IN FACT, we should probably raise an error, yes?
            throw getRuntime().newErrnoEBADFError();
            
//            if (mode == null) {
//                mode = "r";
//            }
//            
//            try {
//                openFile.setMainStream(streamForFileno(getRuntime(), fileno));
//            } catch (BadDescriptorException e) {
//                throw getRuntime().newErrnoEBADFError();
//            } catch (IOException e) {
//                throw getRuntime().newErrnoEBADFError();
//            }
//            //modes = new IOModes(getRuntime(), mode);
//            
//            registerStream(openFile.getMainStream());
        } else {
            // We are creating a new IO object that shares the same
            // IOHandler (and fileno).
            return ChannelStream.fdopen(getRuntime(), existingDescriptor, modes);
        }
    }

    @JRubyMethod(name = "open", required = 1, optional = 1, frame = true, meta = true)
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyClass klass = (RubyClass)recv;
        
        RubyIO io = (RubyIO)klass.newInstance(args, block);

        if (block.isGiven()) {
            try {
                return block.yield(runtime.getCurrentContext(), io);
            } finally {
                if (io.openFile.isOpen()) {
                    io.close();
                }
            }
        }

        return io;
    }

    // This appears to be some windows-only mode.  On a java platform this is a no-op
    @JRubyMethod(name = "binmode")
    public IRubyObject binmode() {
            return this;
    }
    
    protected void checkInitialized() {
        if (openFile == null) {
            throw getRuntime().newIOError("uninitialized stream");
        }
    }
    
    protected void checkClosed() {
        if (openFile.getMainStream() == null && openFile.getPipeStream() == null) {
            throw getRuntime().newIOError("closed stream");
        }
    }
    
    @JRubyMethod(name = "syswrite", required = 1)
    public IRubyObject syswrite(IRubyObject obj) {
        try {
            RubyString string = obj.convertToString();
            
            openFile.checkWritable(getRuntime());
            
            Stream writeStream = openFile.getWriteStream();
            
            if (openFile.isWriteBuffered()) {
                getRuntime().getWarnings().warn("syswrite for buffered IO");
            }
            
            if (!writeStream.getDescriptor().isWritable()) {
                openFile.checkClosed(getRuntime());
            }
            
            int read = writeStream.getDescriptor().write(string.getByteList());
            
            if (read == -1) {
                // TODO? I think this ends up propagating from normal Java exceptions
                // sys_fail(openFile.getPath())
            }
            
            return getRuntime().newFixnum(read);
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            e.printStackTrace();
            throw getRuntime().newSystemCallError(e.getMessage());
        }
    }
    
    /** io_write
     * 
     */
    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(IRubyObject obj) {
        getRuntime().secure(4);
        
        RubyString str;
        if (obj instanceof RubyString) {
            str = (RubyString)obj;
        } else {
            str = (RubyString)obj.asString();
        }
        
        // TODO: Ruby reuses this logic for other "write" behavior by checking if it's an IO and calling write again
        
        if (str.getByteList().length() == 0) {
            return getRuntime().newFixnum(0);
        }

        try {
            openFile.checkWritable(getRuntime());

            int written = fwrite(str.getByteList());

            if (written == -1) {
                // TODO: sys fail
            }

            // if not sync, we switch to write buffered mode
            if (!openFile.isSync()) {
                openFile.setWriteBuffered();
            }

            return getRuntime().newFixnum(written);
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        }
    }
    
    protected int fwrite(ByteList buffer) {
        int n, r, l, offset = 0;
        Stream writeStream = openFile.getWriteStream();

        int len = buffer.length();
        
//        if ((n = len) <= 0) return n;
        if (len == 0) return 0;
        
        try {
            if (openFile.isSync()) {
                openFile.fflush(writeStream);

                // TODO: why is this guarded?
    //            if (!rb_thread_fd_writable(fileno(f))) {
    //                rb_io_check_closed(fptr);
    //            }
                // TODO: loop until it's all written
                //while (offset < len) {
                    writeStream.getDescriptor().write(buffer);
                //}
                return len;

                // TODO: all this stuff...some pipe logic, some async thread stuff
    //          retry:
    //            l = n;
    //            if (PIPE_BUF < l &&
    //                !rb_thread_critical &&
    //                !rb_thread_alone() &&
    //                wsplit_p(fptr)) {
    //                l = PIPE_BUF;
    //            }
    //            TRAP_BEG;
    //            r = write(fileno(f), RSTRING(str)->ptr+offset, l);
    //            TRAP_END;
    //            if (r == n) return len;
    //            if (0 <= r) {
    //                offset += r;
    //                n -= r;
    //                errno = EAGAIN;
    //            }
    //            if (rb_io_wait_writable(fileno(f))) {
    //                rb_io_check_closed(fptr);
    //                if (offset < RSTRING(str)->len)
    //                    goto retry;
    //            }
    //            return -1L;
            }

            // TODO: handle errors in buffered write by retrying until finished or file is closed
            return writeStream.fwrite(buffer);
    //        while (errno = 0, offset += (r = fwrite(RSTRING(str)->ptr+offset, 1, n, f)), (n -= r) > 0) {
    //            if (ferror(f)
    //            ) {
    //                if (rb_io_wait_writable(fileno(f))) {
    //                    rb_io_check_closed(fptr);
    //                    clearerr(f);
    //                    if (offset < RSTRING(str)->len)
    //                        continue;
    //                }
    //                return -1L;
    //            }
    //        }

//            return len - n;
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        }
    }

    /** rb_io_addstr
     * 
     */
    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject op_concat(IRubyObject anObject) {
        // Claims conversion is done via 'to_s' in docs.
        IRubyObject strObject = anObject.callMethod(getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s");

        write(strObject);
        
        return this; 
    }

    @JRubyMethod(name = "fileno", alias = "to_i")
    public RubyFixnum fileno() {
        return getRuntime().newFixnum(openFile.getMainStream().getDescriptor().getFileno());
    }
    
    /** Returns the current line number.
     * 
     * @return the current line number.
     */
    @JRubyMethod(name = "lineno")
    public RubyFixnum lineno() {
        return getRuntime().newFixnum(openFile.getLineNumber());
    }

    /** Sets the current line number.
     * 
     * @param newLineNumber The new line number.
     */
    @JRubyMethod(name = "lineno=", required = 1)
    public RubyFixnum lineno_set(IRubyObject newLineNumber) {
        openFile.setLineNumber(RubyNumeric.fix2int(newLineNumber));

        return (RubyFixnum) newLineNumber;
    }

    /** Returns the current sync mode.
     * 
     * @return the current sync mode.
     */
    @JRubyMethod(name = "sync")
    public RubyBoolean sync() {
        return getRuntime().newBoolean(openFile.getMainStream().isSync());
    }
    
    /**
     * <p>Return the process id (pid) of the process this IO object
     * spawned.  If no process exists (popen was not called), then
     * nil is returned.  This is not how it appears to be defined
     * but ruby 1.8 works this way.</p>
     * 
     * @return the pid or nil
     */
    @JRubyMethod(name = "pid")
    public IRubyObject pid() {
        if (openFile.getProcess() == null) {
            return getRuntime().getNil();
        }
        
        // Of course this isn't particularly useful.
        int pid = openFile.getProcess().hashCode();
        
        return getRuntime().newFixnum(pid); 
    }
    
    /**
     * @deprecated
     * @return
     */
    public boolean writeDataBuffered() {
        return openFile.getMainStream().writeDataBuffered();
    }
    
    @JRubyMethod(name = {"pos", "tell"})
    public RubyFixnum pos() {
        try {
            return getRuntime().newFixnum(openFile.getMainStream().fgetpos());
        } catch (BadDescriptorException bde) {
            throw getRuntime().newErrnoEBADFError();
        } catch (Stream.PipeException e) {
            throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }
    
    @JRubyMethod(name = "pos=", required = 1)
    public RubyFixnum pos_set(IRubyObject newPosition) {
        long offset = RubyNumeric.num2long(newPosition);

        if (offset < 0) {
            throw getRuntime().newSystemCallError("Negative seek offset");
        }
        
        try {
            openFile.getMainStream().fseek(offset, Stream.SEEK_SET);
        } catch (Stream.InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (Stream.PipeException e) {
            throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        
        return (RubyFixnum) newPosition;
    }
    
    /** Print some objects to the stream.
     * 
     */
    @JRubyMethod(name = "print", rest = true)
    public IRubyObject print(IRubyObject[] args) {
        if (args.length == 0) {
            args = new IRubyObject[] { getRuntime().getCurrentContext().getCurrentFrame().getLastLine() };
        }

        IRubyObject fs = getRuntime().getGlobalVariables().get("$,");
        IRubyObject rs = getRuntime().getGlobalVariables().get("$\\");
        ThreadContext context = getRuntime().getCurrentContext();
        
        for (int i = 0; i < args.length; i++) {
            if (i > 0 && !fs.isNil()) {
                callMethod(context, "write", fs);
            }
            if (args[i].isNil()) {
                callMethod(context, "write", getRuntime().newString("nil"));
            } else {
                callMethod(context, "write", args[i]);
            }
        }
        if (!rs.isNil()) {
            callMethod(context, "write", rs);
        }

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "printf", required = 1, rest = true)
    public IRubyObject printf(IRubyObject[] args) {
        callMethod(getRuntime().getCurrentContext(), "write", RubyKernel.sprintf(this, args));
        return getRuntime().getNil();
    }
    
    @JRubyMethod(name = "putc", required = 1)
    public IRubyObject putc(IRubyObject object) {
        int c;
        
        if (getRuntime().getString().isInstance(object)) {
            String value = ((RubyString) object).toString();
            
            if (value.length() > 0) {
                c = value.charAt(0);
            } else {
                throw getRuntime().newTypeError("Cannot convert String to Integer");
            }
        } else if (getRuntime().getFixnum().isInstance(object)){
            c = RubyNumeric.fix2int(object);
        } else { // What case will this work for?
            c = RubyNumeric.fix2int(object.callMethod(getRuntime().getCurrentContext(), MethodIndex.TO_I, "to_i"));
        }

        try {
            openFile.getMainStream().fputc(c);
        } catch (Stream.BadDescriptorException e) {
            return RubyFixnum.zero(getRuntime());
        } catch (IOException e) {
            return RubyFixnum.zero(getRuntime());
        }
        
        return object;
    }
    
    // This was a getOpt with one mandatory arg, but it did not work
    // so I am parsing it for now.
    @JRubyMethod(name = "seek", required = 1, optional = 1)
    public RubyFixnum seek(IRubyObject[] args) {
        long offset = RubyNumeric.num2long(args[0]);
        int whence = Stream.SEEK_SET;
        
        if (args.length > 1) {
            whence = RubyNumeric.fix2int(args[1].convertToInteger());
        }
        
        try {
            openFile.seek(offset, whence);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        } catch (Stream.InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (Stream.PipeException e) {
            throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
        
        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = "rewind")
    public RubyFixnum rewind() {
        try {
            openFile.getMainStream().rewind();
        } catch (BadDescriptorException bde) {
            throw getRuntime().newErrnoEBADFError();
        } catch (Stream.InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (Stream.PipeException e) {
            throw getRuntime().newErrnoESPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }

        // Must be back on first line on rewind.
        openFile.setLineNumber(0);
        
        return RubyFixnum.zero(getRuntime());
    }
    
    @JRubyMethod(name = "fsync")
    public RubyFixnum fsync() {
        try {
            openFile.checkWritable(getRuntime());
        
            openFile.getMainStream().sync();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        }

        return RubyFixnum.zero(getRuntime());
    }

    /** Sets the current sync mode.
     * 
     * @param newSync The new sync mode.
     */
    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject sync_set(IRubyObject newSync) {
        openFile.getMainStream().setSync(newSync.isTrue());

        return this;
    }

    @JRubyMethod(name = {"eof?", "eof"})
    public RubyBoolean eof_p() {
        try {
            boolean isEOF = openFile.getMainStream().feof(); 
            return isEOF ? getRuntime().getTrue() : getRuntime().getFalse();
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }

    @JRubyMethod(name = {"tty?", "isatty"})
    public RubyBoolean tty_p() {
        return getRuntime().newBoolean(getRuntime().getPosix().isatty(openFile.getMainStream().getFD()));
    }
    
    @JRubyMethod(name = "initialize_copy", required = 1)
    public IRubyObject initialize_copy(IRubyObject original){
        if (this == original) return this;

        RubyIO originalIO = (RubyIO) TypeConverter.convertToTypeWithCheck(original, getRuntime().getIO(), MethodIndex.TO_IO, "to_io");
        
        OpenFile originalFile = originalIO.openFile;
        OpenFile newFile = openFile;
        
        try {
            if (originalFile.getPipeStream() != null) {
                originalFile.getPipeStream().fflush();
                originalFile.getMainStream().fseek(0, Stream.SEEK_CUR);
            } else if (originalFile.isWritable()) {
                originalFile.getMainStream().fflush();
            } else {
                originalFile.getMainStream().fseek(0, Stream.SEEK_CUR);
            }

            newFile.setMode(originalFile.getMode());
            newFile.setProcess(originalFile.getProcess());
            newFile.setLineNumber(originalFile.getLineNumber());
            newFile.setPath(originalFile.getPath());
            newFile.setFinalizer(originalFile.getFinalizer());
            
            IOModes modes;
            if (newFile.isReadable()) {
                if (newFile.isWritable()) {
                    if (newFile.getPipeStream() != null) {
                        modes = new IOModes(IOModes.RDONLY);
                    } else {
                        modes = new IOModes(IOModes.RDWR);
                    }
                } else {
                    modes = new IOModes(IOModes.RDONLY);
                }
            } else {
                if (newFile.isWritable()) {
                    modes = new IOModes(IOModes.WRONLY);
                } else {
                    modes = originalFile.getModeAsIOModes(getRuntime());
                }
            }
            
            ChannelDescriptor descriptor = originalFile.getMainStream().getDescriptor().dup();

            openFile.setMainStream(ChannelStream.fdopen(getRuntime(), descriptor, modes));
            
            // TODO: the rest of this...seeking to same position is unnecessary since we share a channel
            // but some of this may be needed?
            
//    fseeko(fptr->f, ftello(orig->f), SEEK_SET);
//    if (orig->f2) {
//	if (fileno(orig->f) != fileno(orig->f2)) {
//	    fd = ruby_dup(fileno(orig->f2));
//	}
//	fptr->f2 = rb_fdopen(fd, "w");
//	fseeko(fptr->f2, ftello(orig->f2), SEEK_SET);
//    }
//    if (fptr->mode & FMODE_BINMODE) {
//	rb_io_binmode(dest);
//    }
            
            // Register the new descriptor
            registerDescriptor(openFile.getMainStream().getDescriptor());
        } catch (IOException ex) {
            throw getRuntime().newIOError("could not init copy: " + ex);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newIOError("could not init copy: " + ex);
        } catch (PipeException ex) {
            throw getRuntime().newIOError("could not init copy: " + ex);
        } catch (InvalidValueException ex) {
            throw getRuntime().newIOError("could not init copy: " + ex);
        }
        
        return this;
    }
    
    /** Closes the IO.
     * 
     * @return The IO.
     */
    @JRubyMethod(name = "closed?")
    public RubyBoolean closed_p() {
        if (openFile.getMainStream() == null && openFile.getPipeStream() == null) {
            return getRuntime().getTrue();
        } else {
            return getRuntime().getFalse();
        }
    }

    /** 
     * <p>Closes all open resources for the IO.  It also removes
     * it from our magical all open file descriptor pool.</p>
     * 
     * @return The IO.
     */
    @JRubyMethod(name = "close")
    public IRubyObject close() {
        if (getRuntime().getSafeLevel() >= 4 && isTaint()) {
            throw getRuntime().newSecurityError("Insecure: can't close");
        }
        
        openFile.checkClosed(getRuntime());
        return close2();
    }
        
    protected IRubyObject close2() {
        if (openFile == null) {
            return getRuntime().getNil();
        }
        
        // These would be used when we notify threads...if we notify threads
        ChannelDescriptor main, pipe;
        if (openFile.getPipeStream() != null) {
            pipe = openFile.getPipeStream().getDescriptor();
        } else {
            if (openFile.getMainStream() == null) {
                return getRuntime().getNil();
            }
            pipe = null;
        }
        
        main = openFile.getMainStream().getDescriptor();
        
        // cleanup, raising errors if any
        openFile.cleanup(getRuntime(), true);
        
        // TODO: notify threads waiting on descriptors/IO? probably not...
        
        if (openFile.getProcess() != null) {
            try {
                IRubyObject processResult = RubyProcess.RubyStatus.newProcessStatus(getRuntime(), openFile.getProcess().waitFor());
                getRuntime().getGlobalVariables().set("$?", processResult);
            } catch (InterruptedException ie) {
                // TODO: do something here?
            }
        }
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "close_write")
    public IRubyObject close_write() throws BadDescriptorException {
        try {
            if (getRuntime().getSafeLevel() >= 4 && isTaint()) {
                throw getRuntime().newSecurityError("Insecure: can't close");
            }
            
            if (openFile.getPipeStream() == null && openFile.isReadable()) {
                throw getRuntime().newIOError("closing non-duplex IO for writing");
            }
            
            if (openFile.getPipeStream() == null) {
                close();
            } else{
                openFile.getPipeStream().fclose();
                openFile.setPipeStream(null);
                openFile.setMode(openFile.getMode() & ~OpenFile.WRITABLE);
                // TODO
                // n is result of fclose; but perhaps having a SysError below is enough?
                // if (n != 0) rb_sys_fail(fptr->path);
            }
        } catch (IOException ioe) {
            // hmmmm
        }
        return this;
    }

    @JRubyMethod(name = "close_read")
    public IRubyObject close_read() throws BadDescriptorException {
        try {
            if (getRuntime().getSafeLevel() >= 4 && isTaint()) {
                throw getRuntime().newSecurityError("Insecure: can't close");
            }
            
            if (openFile.getPipeStream() == null && openFile.isWritable()) {
                throw getRuntime().newIOError("closing non-duplex IO for reading");
            }
            
            if (openFile.getPipeStream() == null) {
                close();
            } else{
                openFile.getMainStream().fclose();
                openFile.setMode(openFile.getMode() & ~OpenFile.READABLE);
                openFile.setMainStream(openFile.getPipeStream());
                openFile.setPipeStream(null);
                // TODO
                // n is result of fclose; but perhaps having a SysError below is enough?
                // if (n != 0) rb_sys_fail(fptr->path);
            }
        } catch (IOException ioe) {
            // I believe Ruby bails out with a "bug" if closing fails
            throw getRuntime().newIOErrorFromException(ioe);
        }
        return this;
    }

    /** Flushes the IO output stream.
     * 
     * @return The IO.
     */
    @JRubyMethod(name = "flush")
    public RubyIO flush() {
        try { 
            openFile.getMainStream().fflush();
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }

        return this;
    }

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "gets", optional = 1)
    public IRubyObject gets(IRubyObject[] args) {
        IRubyObject result = getline(args);

        if (!result.isNil()) getRuntime().getCurrentContext().getCurrentFrame().setLastLine(result);

        return result;
    }

    public boolean getBlocking() {
        return ((ChannelStream) openFile.getMainStream()).isBlocking();
     }

    @JRubyMethod(name = "fcntl", required = 2)
    public IRubyObject fcntl(IRubyObject cmd, IRubyObject arg) throws IOException {
        long realCmd = cmd.convertToInteger().getLongValue();
        
        // FIXME: Arg may also be true, false, and nil and still be valid.  Strangely enough, 
        // protocol conversion is not happening in Ruby on this arg?
        if (!(arg instanceof RubyNumeric)) return getRuntime().newFixnum(0);
        
        long realArg = ((RubyNumeric)arg).getLongValue();

        // Fixme: Only F_SETFL is current supported
        if (realCmd == 1L) {  // cmd is F_SETFL
            boolean block = true;
            
            if ((realArg & IOModes.NONBLOCK) == IOModes.NONBLOCK) {
                block = false;
            }

            try {
                openFile.getMainStream().setBlocking(block);
            } catch (IOException e) {
                throw getRuntime().newIOError(e.getMessage());
            }
        }
        
        return getRuntime().newFixnum(0);
    }

    @JRubyMethod(name = "puts", rest = true)
    public IRubyObject puts(IRubyObject[] args) {
    	ThreadContext context = getRuntime().getCurrentContext();
        
        if (args.length == 0) {
            callMethod(context, "write", getRuntime().newString("\n"));
            return getRuntime().getNil();
        }

        for (int i = 0; i < args.length; i++) {
            String line;
            
            if (args[i].isNil()) {
                line = "nil";
            } else if (getRuntime().isInspecting(args[i])) {
                line = "[...]";
            } else if (args[i] instanceof RubyArray) {
                inspectPuts((RubyArray) args[i]);
                continue;
            } else {
                line = args[i].toString();
            }
            
            callMethod(context, "write", getRuntime().newString(line));
            
            if (!line.endsWith("\n")) {
                callMethod(context, "write", getRuntime().newString("\n"));
            }
        }
        return getRuntime().getNil();
    }
    
    private IRubyObject inspectPuts(RubyArray array) {
        try {
            getRuntime().registerInspecting(array);
            return puts(array.toJavaArray());
        } finally {
            getRuntime().unregisterInspecting(array);
        }
    }

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "readline", optional = 1)
    public IRubyObject readline(IRubyObject[] args) {
        IRubyObject line = gets(args);

        if (line.isNil()) {
            throw getRuntime().newEOFError();
        }
        
        return line;
    }

    /** Read a byte. On EOF returns nil.
     * 
     */
    @JRubyMethod(name = "getc")
    public IRubyObject getc() {
        try {
            openFile.checkReadable(getRuntime());

            Stream stream = openFile.getMainStream();

            if (!stream.readDataBuffered()) {
                openFile.checkClosed(getRuntime());
            }
        
            int c = openFile.getMainStream().fgetc();
        
            return c == -1 ? getRuntime().getNil() : getRuntime().newFixnum(c);
            
            // TODO: if EOF, clear error, try to wait until file is readable again
            // if that also fails, raise appropriate system errorno
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }
    
    /** 
     * <p>Pushes char represented by int back onto IOS.</p>
     * 
     * @param number to push back
     */
    @JRubyMethod(name = "ungetc", required = 1)
    public IRubyObject ungetc(IRubyObject number) {
        openFile.getMainStream().ungetc(RubyNumeric.fix2int(number));

        return getRuntime().getNil();
    }
    
    @JRubyMethod(name = "readpartial", required = 1, optional = 1)
    public IRubyObject readpartial(IRubyObject[] args) {
        if(!(openFile.mainStream instanceof ChannelStream)) {
            // cryptic for the uninitiated...
            throw getRuntime().newNotImplementedError("readpartial only works with Nio based handlers");
        }
    	try {
            ByteList buf = ((ChannelStream)openFile.getMainStream()).readpartial(RubyNumeric.fix2int(args[0]));
            IRubyObject strbuf = RubyString.newString(getRuntime(), buf == null ? new ByteList(ByteList.NULL_ARRAY) : buf);
            if(args.length > 1) {
                args[1].callMethod(getRuntime().getCurrentContext(),MethodIndex.OP_LSHIFT, "<<", strbuf);
                return args[1];
            } 

            return strbuf;
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            return getRuntime().getNil();
        } catch (IOException e) {
            throw getRuntime().newIOError(e.getMessage());
        }
    }

    @JRubyMethod(name = "sysread", required = 1, optional = 1)
    public IRubyObject sysread(IRubyObject[] args) {
        int len = (int)RubyNumeric.num2long(args[0]);
        if (len < 0) throw getRuntime().newArgumentError("Negative size");

        try {
            RubyString str;
            ByteList buffer;
            if (args.length == 1 || args[1].isNil()) {
                if (len == 0) {
                    return RubyString.newStringShared(getRuntime(), ByteList.EMPTY_BYTELIST);
                }
                
                buffer = new ByteList(len);
                str = RubyString.newString(getRuntime(), buffer);
            } else {
                str = args[1].convertToString();
                str.modify(len);
                
                if (len == 0) {
                    return str;
                }
                
                buffer = str.getByteList();
            }
            
            openFile.checkReadable(getRuntime());
            
            if (openFile.getMainStream().readDataBuffered()) {
                throw getRuntime().newIOError("sysread for buffered IO");
            }
            
            // TODO: Ruby locks the string here
            
            getRuntime().getCurrentContext().getThread().beforeBlockingCall();
            
            int bytesRead = openFile.getMainStream().getDescriptor().read(len, str.getByteList());
            
            // TODO: Ruby unlocks the string here
            
            // TODO: Ruby fails with rb_sys_fail if number of bytes read is -1
            
            // TODO: Ruby truncates string to specific size here, but our bytelist should handle this already?
            
            if (bytesRead == 0 && len > 0) {
                throw getRuntime().newEOFError();
            }
            
            str.setTaint(true);
            
            return str;
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (Stream.InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        } catch (Stream.PipeException e) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (EOFException e) {
            throw getRuntime().newEOFError();
    	} catch (IOException e) {
            // All errors to sysread should be SystemCallErrors, but on a closed stream
            // Ruby returns an IOError.  Java throws same exception for all errors so
            // we resort to this hack...
            if ("File not open".equals(e.getMessage())) {
                    throw getRuntime().newIOError(e.getMessage());
            }
    	    throw getRuntime().newSystemCallError(e.getMessage());
    	} finally {
            getRuntime().getCurrentContext().getThread().afterBlockingCall();
        }
    }
    
    @JRubyMethod(name = "read", optional = 2)
    public IRubyObject read(IRubyObject[] args) {
        int argCount = args.length;
        
        if (argCount == 0 || args[0].isNil()) {
            try {
                openFile.checkReadable(getRuntime());

                if (args.length == 2) {
                    return readAll(args[1]);
                } else {
                    return readAll(getRuntime().getNil());
                }
            } catch (PipeException ex) {
                throw getRuntime().newErrnoEPIPEError();
            } catch (InvalidValueException ex) {
                throw getRuntime().newErrnoEINVALError();
            } catch (EOFException ex) {
                throw getRuntime().newEOFError();
            } catch (IOException ex) {
                throw getRuntime().newIOErrorFromException(ex);
            } catch (BadDescriptorException ex) {
                throw getRuntime().newErrnoEBADFError();
            }
        }
        
        int length = RubyNumeric.num2int(args[0]);
        
        if (length < 0) {
            throw getRuntime().newArgumentError("negative length " + length + " given");
        }
        
        RubyString str = null;
//        ByteList buffer = null;
        if (args.length == 1 || args[1].isNil()) {
            if (length == 0) {
                return RubyString.newStringShared(getRuntime(), ByteList.EMPTY_BYTELIST);
            }

//            buffer = new ByteList(length);
//            str = RubyString.newString(getRuntime(), buffer);
        } else {
            str = args[1].convertToString();
            str.modify(length);

            if (length == 0) {
                return str;
            }

//            buffer = str.getByteList();
        }

        try {
            openFile.checkReadable(getRuntime());

            if (openFile.getMainStream().feof()) {
                return getRuntime().getNil();
            }

            // TODO: Ruby locks the string here

            // READ_CHECK from MRI io.c
            if (openFile.getMainStream().readDataBuffered()) {
                // TODO: thread wait fd stuff
                // TODO: checkClosed? see rb_io_check_closed in io.c
            }

            // TODO: check buffer length again?
    //        if (RSTRING(str)->len != len) {
    //            rb_raise(rb_eRuntimeError, "buffer string modified");
    //        }

            // TODO: read into buffer using all the fread logic
    //        int read = openFile.getMainStream().fread(buffer);
            ByteList newBuffer = openFile.getMainStream().fread(length);

            // TODO: Ruby unlocks the string here

            // TODO: change this to check number read into buffer once that's working
    //        if (read == 0) {
            if (newBuffer.length() == 0) {
                if (openFile.getMainStream() == null) {
                    return getRuntime().getNil();
                }

                if (openFile.getMainStream().feof()) {
                    // TODO: resize the buffer string to zero
                    return getRuntime().getNil();
                }

                if (length > 0) {
                    // I think this is only partly correct; sys fail based on errno in Ruby
                    if (newBuffer.length() == 0 && length > 0) {
                        throw getRuntime().newEOFError();
                    }
                }
            }


            // TODO: Ruby truncates string to specific size here, but our bytelist should handle this already?

            if (str == null) {
                str = RubyString.newString(getRuntime(), newBuffer);
            } else {
                str.setValue(newBuffer);
            }
            str.setTaint(true);

            return str;
        } catch (EOFException ex) {
            throw getRuntime().newEOFError();
        } catch (PipeException ex) {
            throw getRuntime().newErrnoEPIPEError();
        } catch (InvalidValueException ex) {
            throw getRuntime().newErrnoEINVALError();
        } catch (IOException ex) {
            throw getRuntime().newIOErrorFromException(ex);
        } catch (BadDescriptorException ex) {
            throw getRuntime().newErrnoEBADFError();
        }
    }
    
    protected IRubyObject readAll(IRubyObject buffer) throws BadDescriptorException, EOFException, IOException {
        // TODO: handle writing into original buffer better
        
        RubyString str = null;
        if (buffer instanceof RubyString) {
            str = (RubyString)str;
        }
        
        // TODO: ruby locks the string here
        
        // READ_CHECK from MRI io.c
        if (openFile.getMainStream().readDataBuffered()) {
            // TODO: thread wait fd stuff
            // TODO: checkClosed? see rb_io_check_closed in io.c
        }
        
        ByteList newBuffer = openFile.getMainStream().readall();

        // TODO same zero-length checks as file above

        if (str == null) {
            if (newBuffer == null) {
                str = RubyString.newStringShared(getRuntime(), ByteList.EMPTY_BYTELIST);
            } else {
                str = RubyString.newString(getRuntime(), newBuffer);
            }
        } else {
            if (newBuffer == null) {
                str.setValue(ByteList.EMPTY_BYTELIST.dup());
            } else {
                str.setValue(newBuffer);
            }
        }

        str.taint();

        return str;
//        long bytes = 0;
//        long n;
//
//        if (siz == 0) siz = BUFSIZ;
//        if (NIL_P(str)) {
//            str = rb_str_new(0, siz);
//        }
//        else {
//            rb_str_resize(str, siz);
//        }
//        for (;;) {
//            rb_str_locktmp(str);
//            READ_CHECK(fptr->f);
//            n = io_fread(RSTRING(str)->ptr+bytes, siz-bytes, fptr);
//            rb_str_unlocktmp(str);
//            if (n == 0 && bytes == 0) {
//                if (!fptr->f) break;
//                if (feof(fptr->f)) break;
//                if (!ferror(fptr->f)) break;
//                rb_sys_fail(fptr->path);
//            }
//            bytes += n;
//            if (bytes < siz) break;
//            siz += BUFSIZ;
//            rb_str_resize(str, siz);
//        }
//        if (bytes != siz) rb_str_resize(str, bytes);
//        OBJ_TAINT(str);
//
//        return str;
    }
    
    // TODO: There's a lot of complexity here due to error handling and
    // nonblocking IO; much of this goes away, but for now I'm just
    // having read call ChannelStream.fread directly.
//    protected int fread(int len, ByteList buffer) {
//        long n = len;
//        int c;
//        int saved_errno;
//
//        while (n > 0) {
//            c = read_buffered_data(ptr, n, fptr->f);
//            if (c < 0) goto eof;
//            if (c > 0) {
//                ptr += c;
//                if ((n -= c) <= 0) break;
//            }
//            rb_thread_wait_fd(fileno(fptr->f));
//            rb_io_check_closed(fptr);
//            clearerr(fptr->f);
//            TRAP_BEG;
//            c = getc(fptr->f);
//            TRAP_END;
//            if (c == EOF) {
//              eof:
//                if (ferror(fptr->f)) {
//                    switch (errno) {
//                      case EINTR:
//    #if defined(ERESTART)
//                      case ERESTART:
//    #endif
//                        clearerr(fptr->f);
//                        continue;
//                      case EAGAIN:
//    #if defined(EWOULDBLOCK) && EWOULDBLOCK != EAGAIN
//                      case EWOULDBLOCK:
//    #endif
//                        if (len > n) {
//                            clearerr(fptr->f);
//                        }
//                        saved_errno = errno;
//                        rb_warning("nonblocking IO#read is obsolete; use IO#readpartial or IO#sysread");
//                        errno = saved_errno;
//                    }
//                    if (len == n) return 0;
//                }
//                break;
//            }
//            *ptr++ = c;
//            n--;
//        }
//        return len - n;
//        
//    }

    /** Read a byte. On EOF throw EOFError.
     * 
     */
    @JRubyMethod(name = "readchar")
    public IRubyObject readchar() {
        IRubyObject c = getc();
        
        if (c.isNil()) throw getRuntime().newEOFError();
        
        return c;
    }
    
    @JRubyMethod
    public IRubyObject stat() {
        return getRuntime().newFileStat(openFile.getMainStream().getFD());
    }

    /** 
     * <p>Invoke a block for each byte.</p>
     */
    @JRubyMethod(name = "each_byte", frame = true)
    public IRubyObject each_byte(Block block) {
    	try {
            ThreadContext context = getRuntime().getCurrentContext();
            for (int c = openFile.getMainStream().fgetc(); c != -1; c = openFile.getMainStream().fgetc()) {
                assert c < 256;
                block.yield(context, getRuntime().newFixnum(c));
            }

            return getRuntime().getNil();
        } catch (Stream.BadDescriptorException e) {
            throw getRuntime().newErrnoEBADFError();
        } catch (EOFException e) {
            return getRuntime().getNil();
    	} catch (IOException e) {
    	    throw getRuntime().newIOError(e.getMessage());
        }
    }

    /** 
     * <p>Invoke a block for each line.</p>
     */
    @JRubyMethod(name = {"each_line", "each"}, optional = 1, frame = true)
    public RubyIO each_line(IRubyObject[] args, Block block) {
        ThreadContext context = getRuntime().getCurrentContext(); 
        ByteList separator = getSeparatorForGets(args);
        
        for (IRubyObject line = getline(separator); !line.isNil(); 
        	line = getline(separator)) {
            block.yield(context, line);
        }
        
        return this;
    }


    @JRubyMethod(name = "readlines", optional = 1)
    public RubyArray readlines(IRubyObject[] args) {
        ByteList separator;
        if (args.length > 0) {
            if (!getRuntime().getNilClass().isInstance(args[0]) &&
                !getRuntime().getString().isInstance(args[0])) {
                throw getRuntime().newTypeError(args[0], 
                        getRuntime().getString());
            } 
            separator = getSeparatorForGets(new IRubyObject[] { args[0] });
        } else {
            separator = getSeparatorForGets(IRubyObject.NULL_ARRAY);
        }

        RubyArray result = getRuntime().newArray();
        IRubyObject line;
        while (! (line = getline(separator)).isNil()) {
            result.append(line);
        }
        return result;
    }
    
    @JRubyMethod(name = "to_io")
    public RubyIO to_io() {
    	return this;
    }

    public String toString() {
        return "RubyIO(" + openFile.getMode() + ", " + openFile.getMainStream().getDescriptor().getFileno() + ")";
    }
    
    /* class methods for IO */
    
    /** rb_io_s_foreach
    *
    */
    @JRubyMethod(name = "foreach", required = 1, optional = 1, frame = true, meta = true)
    public static IRubyObject foreach(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        int count = args.length;
        IRubyObject filename = args[0].convertToString();
        runtime.checkSafeString(filename);
       
        RubyString separator;
        if (count == 2) {
            separator = args[1].convertToString();
        } else {
            separator = runtime.getGlobalVariables().get("$/").convertToString();
        }
        
        RubyIO io = (RubyIO)RubyFile.open(runtime.getFile(), new IRubyObject[] { filename }, Block.NULL_BLOCK);
        
        if (!io.isNil()) {
            try {
                ByteList sep = separator.getByteList();
                IRubyObject str = io.getline(sep);
                ThreadContext context = runtime.getCurrentContext();
                while (!str.isNil()) {
                    block.yield(context, str);
                    str = io.getline(sep);
                }
            } finally {
                io.close();
            }
        }
       
        return runtime.getNil();
    }
   
    private static RubyIO registerSelect(Selector selector, IRubyObject obj, int ops) throws IOException {
       RubyIO ioObj;
       
       if (!(obj instanceof RubyIO)) {
           // invoke to_io
           if (!obj.respondsTo("to_io")) return null;

           ioObj = (RubyIO) obj.callMethod(obj.getRuntime().getCurrentContext(), "to_io");
       } else {
           ioObj = (RubyIO) obj;
       }
       
       Channel channel = ioObj.getChannel();
       if (channel == null || !(channel instanceof SelectableChannel)) {
           return null;
       }
       
       ((SelectableChannel) channel).configureBlocking(false);
       int real_ops = ((SelectableChannel) channel).validOps() & ops;
       SelectionKey key = ((SelectableChannel) channel).keyFor(selector);
       
       if (key == null) {
           ((SelectableChannel) channel).register(selector, real_ops, obj);
       } else {
           key.interestOps(key.interestOps()|real_ops);
       }
       
       return ioObj;
   }
   
    @JRubyMethod(name = "select", required = 1, optional = 3, meta = true)
    public static IRubyObject select(IRubyObject recv, IRubyObject[] args) {
        return select_static(recv.getRuntime(), args);
    }
   
    public static IRubyObject select_static(Ruby runtime, IRubyObject[] args) {
       try {
           boolean atLeastOneDescriptor = false;
           
           Set pending = new HashSet();
           Selector selector = Selector.open();
           if (!args[0].isNil()) {
               atLeastOneDescriptor = true;
               
               // read
               for (Iterator i = ((RubyArray) args[0]).getList().iterator(); i.hasNext(); ) {
                   IRubyObject obj = (IRubyObject) i.next();
                   RubyIO ioObj = registerSelect(selector, obj, 
                           SelectionKey.OP_READ | SelectionKey.OP_ACCEPT);
                   
                   if (ioObj!=null && ioObj.writeDataBuffered()) pending.add(obj);
               }
           }
           if (args.length > 1 && !args[1].isNil()) {
               atLeastOneDescriptor = true;
               // write
               for (Iterator i = ((RubyArray) args[1]).getList().iterator(); i.hasNext(); ) {
                   IRubyObject obj = (IRubyObject) i.next();
                   registerSelect(selector, obj, SelectionKey.OP_WRITE);
               }
           }
           if (args.length > 2 && !args[2].isNil()) {
               atLeastOneDescriptor = true;
               // Java's select doesn't do anything about this, so we leave it be.
           }
           
           long timeout = 0;
           if(args.length > 3 && !args[3].isNil()) {
               if (args[3] instanceof RubyFloat) {
                   timeout = Math.round(((RubyFloat) args[3]).getDoubleValue() * 1000);
               } else {
                   timeout = Math.round(((RubyFixnum) args[3]).getDoubleValue() * 1000);
               }
               
               if (timeout < 0) {
                   throw runtime.newArgumentError("negative timeout given");
               }
           }
           
           if (!atLeastOneDescriptor) {
               return runtime.getNil();
           }
           
           if (pending.isEmpty()) {
               if (args.length > 3) {
                   if (timeout==0) {
                       selector.selectNow();
                   } else {
                       selector.select(timeout);                       
                   }
               } else {
                   selector.select();
               }
           } else {
               selector.selectNow();               
           }
           
           List r = new ArrayList();
           List w = new ArrayList();
           List e = new ArrayList();
           for (Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
               SelectionKey key = (SelectionKey) i.next();
               if ((key.interestOps() & key.readyOps()
                       & (SelectionKey.OP_READ|SelectionKey.OP_ACCEPT|SelectionKey.OP_CONNECT)) != 0) {
                   r.add(key.attachment());
                   pending.remove(key.attachment());
               }
               if ((key.interestOps() & key.readyOps() & (SelectionKey.OP_WRITE)) != 0) {
                   w.add(key.attachment());
               }
           }
           r.addAll(pending);
           
           // make all sockets blocking as configured again
           for (Iterator i = selector.keys().iterator(); i.hasNext(); ) {
               SelectionKey key = (SelectionKey) i.next();
               SelectableChannel channel = key.channel();
               synchronized(channel.blockingLock()) {
                   boolean blocking = ((RubyIO) key.attachment()).getBlocking();
                   key.cancel();
                   channel.configureBlocking(blocking);
               }
           }
           selector.close();
           
           if (r.size() == 0 && w.size() == 0 && e.size() == 0) {
               return runtime.getNil();
           }
           
           List ret = new ArrayList();
           
           ret.add(RubyArray.newArray(runtime, r));
           ret.add(RubyArray.newArray(runtime, w));
           ret.add(RubyArray.newArray(runtime, e));
           
           return RubyArray.newArray(runtime, ret);
       } catch(IOException e) {
           throw runtime.newIOError(e.getMessage());
       }
   }
   
    @JRubyMethod(name = "read", required = 1, optional = 2, meta = true)
    public static IRubyObject read(IRubyObject recv, IRubyObject[] args, Block block) {
       IRubyObject[] fileArguments = new IRubyObject[] {args[0]};
       RubyIO file = (RubyIO) RubyKernel.open(recv, fileArguments, block);
       IRubyObject[] readArguments;
       
       if (args.length >= 2) {
           readArguments = new IRubyObject[] {args[1].convertToInteger()};
       } else {
           readArguments = new IRubyObject[] {};
       }
       
       try {
           
           if (args.length == 3) {
               file.seek(new IRubyObject[] {args[2].convertToInteger()});
           }
           
           return file.read(readArguments);
       } finally {
           file.close();
       }
   }
   
    @JRubyMethod(name = "readlines", required = 1, optional = 1, meta = true)
    public static RubyArray readlines(IRubyObject recv, IRubyObject[] args, Block block) {
        int count = args.length;

        IRubyObject[] fileArguments = new IRubyObject[]{args[0]};
        IRubyObject[] separatorArguments = count >= 2 ? new IRubyObject[]{args[1]} : IRubyObject.NULL_ARRAY;
        RubyIO file = (RubyIO) RubyKernel.open(recv, fileArguments, block);
        try {
            return file.readlines(separatorArguments);
        } finally {
            file.close();
        }
    }
   
    @JRubyMethod(name = "popen", required = 1, optional = 1, meta = true)
    public static IRubyObject popen(IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        int mode;

        try {
            if (args.length == 1) {
                mode = IOModes.RDONLY;
            } else if (args[1] instanceof RubyFixnum) {
                mode = RubyFixnum.num2int(args[1]);
            } else {
                mode = getIOModesIntFromString(runtime, args[1].convertToString().toString());
            }

            IOModes modes = new IOModes(mode);
            IRubyObject cmdObj = args[0].convertToString();
            runtime.checkSafeString(cmdObj);
        
            Process process = new ShellLauncher(runtime).run(cmdObj);
            RubyIO io = new RubyIO(runtime, process, modes);

            if (block.isGiven()) {
                try {
                    return block.yield(runtime.getCurrentContext(), io);
                } finally {
                    if (io.openFile.isOpen()) {
                        io.close();
                    }
                    runtime.getGlobalVariables().set("$?", RubyProcess.RubyStatus.newProcessStatus(runtime, (process.waitFor() * 256)));
                }
            }
            return io;
        } catch (InvalidValueException ex) {
            throw runtime.newErrnoEINVALError();
        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } catch (InterruptedException e) {
            throw runtime.newThreadError("unexpected interrupt");
        }
    }

    // NIO based pipe
    @JRubyMethod(name = "pipe", meta = true)
    public static IRubyObject pipe(IRubyObject recv) throws Exception {
        // TODO: This isn't an exact port of MRI's pipe behavior, so revisit
       Ruby runtime = recv.getRuntime();
       Pipe pipe = Pipe.open();
       
       RubyIO source = new RubyIO(runtime, pipe.source());
       RubyIO sink = new RubyIO(runtime, pipe.sink());
       
       sink.openFile.mainStream.setSync(true);
       return runtime.newArrayNoCopy(new IRubyObject[]{
           source,
           sink
       });
   }
   
    /**
     * returns non-nil if input available without blocking, false if EOF or not open/readable, otherwise nil.
     */
    public IRubyObject ready() {
       try {
           if (!openFile.mainStream.isOpen() || !openFile.mainStream.isReadable() || openFile.getMainStream().feof()) {
               return getRuntime().getFalse();
           }

           int avail = openFile.getMainStream().ready();
           if (avail > 0) {
               return getRuntime().newFixnum(avail);
           } 
       } catch (Exception anyEx) {
           return getRuntime().getFalse();
       }
       return getRuntime().getNil();
   }
   
    /**
     * waits until input available or timed out and returns self, or nil when EOF reached.
     */
    public IRubyObject io_wait() {
       try {
           if (openFile.getMainStream().feof()) {
               return getRuntime().getNil();
           }
            openFile.getMainStream().waitUntilReady();
       } catch (Exception anyEx) {
           return getRuntime().getNil();
       }
       return this;
   }
}
