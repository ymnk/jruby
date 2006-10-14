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
package org.jruby.openssl;

import java.security.MessageDigest;

import org.jruby.runtime.builtin.IRubyObject;

/**
 * Static class that holds various OpenSSL methods that aren't
 * really easy to do any other way.
 *
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class OpenSSLImpl {
    /**
     * No instantiating this class...
     */
    private OpenSSLImpl() {}

    public static IRubyObject to_der(IRubyObject obj) {
        return obj.callMethod("to_der");
    }

    public static IRubyObject to_der_if_possible(IRubyObject obj) {
        if(obj.respondsTo("to_der")) {
            return to_der(obj);
        } else {
            return obj;
        }
    }

    public static interface KeyAndIv {
        byte[] getKey();
        byte[] getIv();
    }

    private static class KeyAndIvImpl implements KeyAndIv {
        private final byte[] key;
        private final byte[] iv;
        public KeyAndIvImpl(byte[] key, byte[] iv) {
            this.key = key;
            this.iv = iv;
        }
        public byte[] getKey() {
            return key;
        }
        public byte[] getIv() {
            return iv;
        }
    }

    private static Class pemHandlerImpl;
    public static PEMHandler getPEMHandler() {
        if(null == pemHandlerImpl) {
            try {
                Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                pemHandlerImpl = Class.forName("org.jruby.openssl.BouncyCastlePEMHandler");
            } catch(Exception e) {
                pemHandlerImpl = DefaultPEMHandler.class;
            }
        }
        try {
            return (PEMHandler)pemHandlerImpl.newInstance();
        } catch(Exception e) {}
        return null;
    }

    public static KeyAndIv EVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] salt, byte[] data, int count) {
        byte[] key = new byte[key_len];
        byte[]  iv = new byte[iv_len];
        int key_ix = 0;
        int iv_ix = 0;
        byte[] md_buf = null;
        int nkey = key_len;
        int niv = iv_len;
        int i = 0;
        if(data == null) {
            return new KeyAndIvImpl(key,iv);
        }
        int addmd = 0;
        for(;;) {
            md.reset();
            if(addmd++ > 0) {
                md.update(md_buf);
            }
            md.update(data);
            if(null != salt) {
                md.update(salt,0,8);
            }
            md_buf = md.digest();
            for(i=1;i<count;i++) {
                md.reset();
                md.update(md_buf);
                md_buf = md.digest();
            }
            i=0;
            if(nkey > 0) {
                for(;;) {
                    if(nkey == 0) break;
                    if(i == md_buf.length) break;
                    key[key_ix++] = md_buf[i];
                    nkey--;
                    i++;
                }
            }
            if(niv > 0 && i != md_buf.length) {
                for(;;) {
                    if(niv == 0) break;
                    if(i == md_buf.length) break;
                    iv[iv_ix++] = md_buf[i];
                    niv--;
                    i++;
                }
            }
            if(nkey == 0 && niv == 0) {
                break;
            }
        }
        for(i=0;i<md_buf.length;i++) {
            md_buf[i] = 0;
        }
        return new KeyAndIvImpl(key,iv);
    }
}// OpenSSLImpl
