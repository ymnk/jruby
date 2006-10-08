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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyOpenSSL {
    public static void checkBouncyCastle() {
        try {
            Class v = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            java.security.Security.addProvider((java.security.Provider)(v.newInstance()));
        } catch(Exception e) {
            // No BouncyCastle available...
        }
    }

    public static void createOpenSSL(IRuby runtime) {
        checkBouncyCastle();
        RubyModule ossl = runtime.defineModule("OpenSSL");
        RubyClass eOSSLError = ossl.defineClassUnder("OpenSSLError",runtime.getClass("StandardError"));

        RubyModule rand = ossl.defineModuleUnder("Random");
        RubyModule mCipher = ossl.defineModuleUnder("Cipher");
        RubyModule mDigest = ossl.defineModuleUnder("Digest");
        RubyClass cDigest = mDigest.defineClassUnder("Digest",runtime.getObject());
        mCipher.defineClassUnder("CipherError",eOSSLError);
        mDigest.defineClassUnder("DigestError",eOSSLError);

        RubyClass cCipher = mCipher.defineClassUnder("Cipher",runtime.getObject());
        CallbackFactory ciphercb = runtime.callbackFactory(Cipher.class);

        cCipher.defineSingletonMethod("new",ciphercb.getOptSingletonMethod("newInstance"));
        cCipher.defineMethod("initialize",ciphercb.getMethod("initialize",IRubyObject.class));
        cCipher.defineMethod("initialize_copy",ciphercb.getMethod("initialize_copy",IRubyObject.class));
        cCipher.defineMethod("clone",ciphercb.getMethod("rbClone"));
        cCipher.defineMethod("name",ciphercb.getMethod("name"));
        cCipher.defineMethod("key_len",ciphercb.getMethod("key_len"));
        cCipher.defineMethod("key_len=",ciphercb.getMethod("set_key_len",IRubyObject.class));
        cCipher.defineMethod("iv_len",ciphercb.getMethod("iv_len"));
        cCipher.defineMethod("block_size",ciphercb.getMethod("block_size"));
        cCipher.defineMethod("encrypt",ciphercb.getOptMethod("encrypt"));
        cCipher.defineMethod("decrypt",ciphercb.getOptMethod("decrypt"));

        CallbackFactory digestcb = runtime.callbackFactory(Digest.class);
        cDigest.defineSingletonMethod("new",digestcb.getOptSingletonMethod("newInstance"));
        cDigest.defineSingletonMethod("digest",digestcb.getSingletonMethod("s_digest",IRubyObject.class,IRubyObject.class));
        cDigest.defineSingletonMethod("hexdigest",digestcb.getSingletonMethod("s_hexdigest",IRubyObject.class,IRubyObject.class));
        cDigest.defineMethod("initialize",digestcb.getOptMethod("initialize"));
        cDigest.defineMethod("initialize_copy",digestcb.getMethod("initialize_copy",IRubyObject.class));
        cDigest.defineMethod("clone",digestcb.getMethod("rbClone"));
        cDigest.defineMethod("update",digestcb.getMethod("update",IRubyObject.class));
        cDigest.defineMethod("<<",digestcb.getMethod("update",IRubyObject.class));
        cDigest.defineMethod("digest",digestcb.getMethod("digest"));
        cDigest.defineMethod("hexdigest",digestcb.getMethod("hexdigest"));
        cDigest.defineMethod("inspect",digestcb.getMethod("hexdigest"));
        cDigest.defineMethod("to_s",digestcb.getMethod("hexdigest"));
        cDigest.defineMethod("==",digestcb.getMethod("eq",IRubyObject.class));
        cDigest.defineMethod("reset",digestcb.getMethod("reset"));
        cDigest.defineMethod("name",digestcb.getMethod("name"));
        cDigest.defineMethod("size",digestcb.getMethod("size"));

        ossl.setConstant("VERSION",runtime.newString("1.0.0"));
        ossl.setConstant("OPENSSL_VERSION",runtime.newString("OpenSSL 0.9.8b 04 May 2006 (Java fake)"));
        
        try {
            MessageDigest.getInstance("SHA224");
            ossl.setConstant("OPENSSL_VERSION_NUMBER",runtime.newFixnum(9469999));
        } catch(NoSuchAlgorithmException e) {
            ossl.setConstant("OPENSSL_VERSION_NUMBER",runtime.newFixnum(9469952));
        }

        CallbackFactory randcb = runtime.callbackFactory(Random.class);
        rand.defineSingletonMethod("seed",randcb.getOptSingletonMethod("seed"));
        rand.defineSingletonMethod("load_random_file",randcb.getOptSingletonMethod("load_random_file"));
        rand.defineSingletonMethod("write_random_file",randcb.getOptSingletonMethod("write_random_file"));
        rand.defineSingletonMethod("random_bytes",randcb.getOptSingletonMethod("random_bytes"));
        rand.defineSingletonMethod("pseudo_bytes",randcb.getOptSingletonMethod("pseudo_bytes"));
        rand.defineSingletonMethod("egd",randcb.getOptSingletonMethod("egd"));
        rand.defineSingletonMethod("egd_bytes",randcb.getOptSingletonMethod("egd_bytes"));
    }
    
    public static class Digest extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            Digest result = new Digest(recv.getRuntime(), (RubyClass)recv);
            if(!(recv.toString().equals("OpenSSL::Digest::Digest"))) {
                try {
                    result.name = recv.toString();
                    result.md = MessageDigest.getInstance(recv.toString());
                } catch(NoSuchAlgorithmException e) {
                    throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + recv.toString() + ")");
                }
            }
            result.callInit(args);
            return result;
        }
        public static IRubyObject s_digest(IRubyObject recv, IRubyObject str, IRubyObject data) {
            String name = str.toString();
            try {
                MessageDigest md = MessageDigest.getInstance(name);
                return recv.getRuntime().newString(new String(md.digest(data.toString().getBytes("PLAIN")),"PLAIN"));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            } catch(java.io.UnsupportedEncodingException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }
        public static IRubyObject s_hexdigest(IRubyObject recv, IRubyObject str, IRubyObject data) {
            String name = str.toString();
            try {
                MessageDigest md = MessageDigest.getInstance(name);
                return recv.getRuntime().newString(toHex(md.digest(data.toString().getBytes("PLAIN"))));
            } catch(NoSuchAlgorithmException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            } catch(java.io.UnsupportedEncodingException e) {
                throw recv.getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
        }

        public Digest(IRuby runtime, RubyClass type) {
            super(runtime,type);
            data = new StringBuffer();
        }

        private MessageDigest md;
        private StringBuffer data;
        private String name;

        public IRubyObject initialize(IRubyObject[] args) {
            IRubyObject type;
            IRubyObject data = getRuntime().getNil();
            if(checkArgumentCount(args,1,2) == 2) {
                data = args[1];
            }
            type = args[0];

            name = type.toString();
            try {
                md = MessageDigest.getInstance(name);
            } catch(NoSuchAlgorithmException e) {
                throw getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }
            if(!data.isNil()) {
                update(data);
            }
            return this;
        }

        public IRubyObject initialize_copy(IRubyObject obj) {
            if(this == obj) {
                return this;
            }
            ((RubyObject)obj).checkFrozen();
            data = new StringBuffer(((Digest)obj).data.toString());
            name = ((Digest)obj).md.getAlgorithm();
            try {
                md = MessageDigest.getInstance(name);
            } catch(NoSuchAlgorithmException e) {
                throw getRuntime().newNotImplementedError("Unsupported digest algorithm (" + name + ")");
            }

            return this;
        }

        public IRubyObject update(IRubyObject obj) {
            try {
                data.append(obj);
                md.update(obj.toString().getBytes("PLAIN"));
            } catch(java.io.UnsupportedEncodingException e) {}
            return this;
        }

        public IRubyObject reset() {
            md.reset();
            data = new StringBuffer();
            return this;
        }

        public IRubyObject digest() {
            try {
                md.reset();
                return getRuntime().newString(new String(md.digest(data.toString().getBytes("PLAIN")),"PLAIN"));
            } catch(java.io.UnsupportedEncodingException e) {
                return getRuntime().getNil();
            }
        }

        public IRubyObject name() {
            return getRuntime().newString(name);
        }

        public IRubyObject size() {
            return getRuntime().newFixnum(md.getDigestLength());
        }

        public IRubyObject hexdigest() {
            try {
                md.reset();
                return getRuntime().newString(toHex(md.digest(data.toString().getBytes("PLAIN"))));
            } catch(java.io.UnsupportedEncodingException e) {
                return getRuntime().getNil();
            }
        }

        public IRubyObject eq(IRubyObject oth) {
            boolean ret = this == oth;
            if(!ret && oth instanceof Digest) {
                Digest b = (Digest)oth;
                ret = this.md.getAlgorithm().equals(b.md.getAlgorithm()) &&
                    this.digest().equals(b.digest());
            }

            return ret ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject rbClone() {
            IRubyObject clone = new Digest(getRuntime(),getMetaClass().getRealClass());
            clone.setMetaClass(getMetaClass().getSingletonClassClone());
            clone.setTaint(this.isTaint());
            clone.initCopy(this);
            clone.setFrozen(isFrozen());
            return clone;
        }

        private static String toHex(byte[] val) {
            StringBuffer out = new StringBuffer();
            for(int i=0,j=val.length;i<j;i++) {
                String ve = Integer.toString((((int)((char)val[i])) & 0xFF),16);
                if(ve.length() == 1) {
                    ve = "0" + ve;
                }
                out.append(ve);
            }
            return out.toString();
        }
    }

    public static class Cipher extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            System.err.println("creating new instance of us: " + recv);
            Cipher result = new Cipher(recv.getRuntime(), (RubyClass)recv);
            System.err.println("we have an instance");
            result.callMethod("initialize",args);
            System.err.println("after calling init");
            return result;
        }

        public Cipher(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        private javax.crypto.Cipher ciph;
        private String name;
        private String cryptoBase;
        private String cryptoVersion;
        private String cryptoMode;
        private String padding = "PKCS5Padding";
        private String realName;
        private int keyLen;
        private boolean encryptMode = true;
        private IRubyObject[] modeParams;
        private boolean ciphInited = false;

        public IRubyObject initialize(IRubyObject str) {
            name = str.toString();
            System.err.println("We want cipher with name: " + name + " for class: " + this);
            String[] split = name.split("-");
            cryptoBase = split[0];
            if(split.length == 3) {
                cryptoVersion = split[1];
                cryptoMode = split[2];
            } else {
                if(split.length == 2) {
                    cryptoMode = split[1];
                } else {
                    cryptoMode = "ECB";
                }
            }

            if(cryptoBase.equalsIgnoreCase("DES") && "EDE3".equalsIgnoreCase(cryptoVersion)) {
                realName = "DESede";
            } else {
                realName = cryptoBase;
            }
            realName = realName + "/" + cryptoMode + "/" + padding;

            System.err.println("Trying " + realName);

            try {
                ciph = javax.crypto.Cipher.getInstance(realName);
            } catch(NoSuchAlgorithmException e) {
                throw getRuntime().newLoadError("unsupported cipher algorithm (" + realName + ")");
            } catch(javax.crypto.NoSuchPaddingException e) {
                throw getRuntime().newLoadError("unsupported cipher padding (" + realName + ")");
            }

            if("AES".equalsIgnoreCase(cryptoBase) && null != cryptoVersion) {
                try {
                    keyLen = Integer.parseInt(cryptoVersion);
                } catch(NumberFormatException e) {
                    keyLen = 256;
                }
            } else {
                keyLen = 128;
            }

            return this;
        }

        public IRubyObject initialize_copy(IRubyObject obj) {
            if(this == obj) {
                return this;
            }
            ((RubyObject)obj).checkFrozen();

            cryptoBase = ((Cipher)obj).cryptoBase;
            cryptoVersion = ((Cipher)obj).cryptoVersion;
            cryptoMode = ((Cipher)obj).cryptoMode;
            padding = ((Cipher)obj).padding;
            realName = ((Cipher)obj).realName;
            name = ((Cipher)obj).name;
            keyLen = ((Cipher)obj).keyLen;
            // TODO: fix rest

            try {
                ciph = javax.crypto.Cipher.getInstance(realName);
            } catch(NoSuchAlgorithmException e) {
                throw getRuntime().newLoadError("unsupported cipher algorithm (" + realName + ")");
            } catch(javax.crypto.NoSuchPaddingException e) {
                throw getRuntime().newLoadError("unsupported cipher padding (" + realName + ")");
            }

            return this;
        }

        public IRubyObject name() {
            return getRuntime().newString(name);
        }

        public IRubyObject key_len() {
            return getRuntime().newFixnum(keyLen);
        }

        public IRubyObject iv_len() {
            if(ciph.getIV() != null) {
                return getRuntime().newFixnum(ciph.getIV().length);
            }
            return RubyFixnum.zero(getRuntime());
        }

        public IRubyObject set_key_len(IRubyObject len) {
            this.keyLen = RubyNumeric.fix2int(len);
            return len;
        }

        public IRubyObject block_size() {
            return getRuntime().newFixnum(ciph.getBlockSize());
        }

        public IRubyObject encrypt(IRubyObject[] args) {
            //TODO: implement backwards compat
            checkArgumentCount(args,0,2);
            encryptMode = true;
            modeParams = args;
            ciphInited = false;
            return this;
        }

        public IRubyObject decrypt(IRubyObject[] args) {
            //TODO: implement backwards compat
            checkArgumentCount(args,0,2);
            encryptMode = false;
            modeParams = args;
            ciphInited = false;
            return this;
        }

        public IRubyObject rbClone() {
            IRubyObject clone = new Cipher(getRuntime(),getMetaClass().getRealClass());
            clone.setMetaClass(getMetaClass().getSingletonClassClone());
            clone.setTaint(this.isTaint());
            clone.initCopy(this);
            clone.setFrozen(isFrozen());
            return clone;
        }
    }

    public static class Random {
        public static IRubyObject seed(IRubyObject recv, IRubyObject[] args) {
            return recv.getRuntime().getNil();
        }
        public static IRubyObject load_random_file(IRubyObject recv, IRubyObject[] args) {
            return recv.getRuntime().getNil();

        }
        public static IRubyObject write_random_file(IRubyObject recv, IRubyObject[] args) {
            return recv.getRuntime().getNil();
        }
        public static IRubyObject random_bytes(IRubyObject recv, IRubyObject[] args) {
            return recv.getRuntime().getNil();
        }
        public static IRubyObject pseudo_bytes(IRubyObject recv, IRubyObject[] args) {
            return recv.getRuntime().getNil();
        }
        public static IRubyObject egd(IRubyObject recv, IRubyObject[] args) {
            return recv.getRuntime().getNil();
        }
        public static IRubyObject egd_bytes(IRubyObject recv, IRubyObject[] args) {
            return recv.getRuntime().getNil();
        }
    }
}// RubyOpenSSL
