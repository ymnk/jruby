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
package org.jruby.ext.openssl;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateFactory;
import javax.crypto.SecretKeyFactory;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.ext.openssl.impl.ASN1FormatHandler;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class OpenSSLReal {
    private static java.security.Provider BC_PROVIDER = null;   // const-ish
    private static ASN1FormatHandler FORMAT_HANDLER = null;     // const-ish

    static {
        try {
            BC_PROVIDER = (java.security.Provider) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance();
        } catch (Throwable ignored) {
            // no bouncy castle available
        }
        try {
            FORMAT_HANDLER = (ASN1FormatHandler) Class.forName("org.jruby.ext.openssl.x509store.PEMInputOutput").newInstance();
        } catch (Throwable ignored) {
            FORMAT_HANDLER = null;
        }
    }

    public interface Runnable {
        public void run() throws GeneralSecurityException;
    }
    
    public interface Callable {
        public Object call() throws GeneralSecurityException;
    }

    public static void doWithBCProvider(final Runnable toRun) throws GeneralSecurityException {
        getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                toRun.run();
                return null;
            }
        });
    }
    
    public static synchronized void setBCProvider(java.security.Provider newProvider) {
        BC_PROVIDER = newProvider;
    }
    
    public static synchronized ASN1FormatHandler getFormatHandler() {
        return FORMAT_HANDLER;
    }
    
    public static synchronized void setFormatHandler(ASN1FormatHandler newHandler) {
        FORMAT_HANDLER = newHandler;
    }

    // This method just adds BouncyCastleProvider if it's allowed.  Removing
    // "BC" can remove pre-installed or runtime-added BC provider by elsewhere
    // and it causes unknown runtime error anywhere.  We avoid this. To use
    // part of jruby-openssl feature (X.509 and PKCS), users must be aware of
    // dynamic BC provider adding.
    public static Object getWithBCProvider(Callable toCall) throws GeneralSecurityException {
        try {
            if (BC_PROVIDER != null && java.security.Security.getProvider("BC") == null) {
                java.security.Security.addProvider(BC_PROVIDER);
            }
            return toCall.call();
        } catch (NoSuchProviderException nspe) {
            throw new GeneralSecurityException(bcExceptionMessage(nspe), nspe);
        } catch (Exception e) {
            throw new GeneralSecurityException(e.getMessage(), e);
        }
    }

    public static String bcExceptionMessage(NoSuchProviderException nspe) {
        return "You need to configure JVM/classpath to enable BouncyCastle Security Provider: " + nspe.getMessage();
    }

    public static String bcExceptionMessage(NoClassDefFoundError ncdfe) {
        return "You need to configure JVM/classpath to enable BouncyCastle Security Provider: NoClassDefFoundError: " + ncdfe.getMessage();
    }

    public static void createOpenSSL(Ruby runtime) {
        RubyModule ossl = runtime.getOrCreateModule("OpenSSL");
        RubyClass standardError = runtime.getClass("StandardError");
        ossl.defineClassUnder("OpenSSLError", standardError, standardError.getAllocator());

        PKey.createPKey(runtime, ossl);
        BN.createBN(runtime, ossl);
        Digest.createDigest(runtime, ossl);
        Cipher.createCipher(runtime, ossl);
        Random.createRandom(runtime, ossl);
        HMAC.createHMAC(runtime, ossl);
        Config.createConfig(runtime, ossl);

        runtime.getLoadService().require("opensslext");
        
        ossl.setConstant("VERSION", runtime.newString("1.0.0"));
        ossl.setConstant("OPENSSL_VERSION", runtime.newString("jruby-ossl 1.0.0"));
        ossl.setConstant("OPENSSL_VERSION_NUMBER", runtime.newFixnum(9469999));
    }

    public static javax.crypto.Cipher getCipherBC(final String algorithm) throws GeneralSecurityException {
        return (javax.crypto.Cipher) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return javax.crypto.Cipher.getInstance(algorithm, "BC");
            }
        });
    }

    public static SecretKeyFactory getSecretKeyFactoryBC(final String algorithm) throws GeneralSecurityException {
        return (SecretKeyFactory) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return SecretKeyFactory.getInstance(algorithm, "BC");
            }
        });
    }

    public static MessageDigest getMessageDigestBC(final String algorithm) throws GeneralSecurityException {
        return (MessageDigest) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return MessageDigest.getInstance(algorithm, "BC");
            }
        });
    }

    public static CertificateFactory getX509CertificateFactoryBC() throws GeneralSecurityException {
        return (CertificateFactory) getWithBCProvider(new Callable() {

            public Object call() throws GeneralSecurityException {
                return CertificateFactory.getInstance("X.509", "BC");
            }
        });
    }
}// OpenSSLReal

