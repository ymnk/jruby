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

import java.io.StringReader;

import java.security.KeyPair;
import java.security.KeyFactory;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.jruby.IRuby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyObject;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class PKeyDSA extends PKey {
    public static void createPKeyDSA(IRuby runtime, RubyModule mPKey) {
        RubyClass cDSA = mPKey.defineClassUnder("DSA",mPKey.getClass("PKey"));
        mPKey.defineClassUnder("DSAError",mPKey.getClass("PKeyError"));
        
        CallbackFactory rsacb = runtime.callbackFactory(PKeyDSA.class);

        cDSA.defineSingletonMethod("new",rsacb.getOptSingletonMethod("newInstance"));
        cDSA.defineMethod("initialize",rsacb.getOptMethod("initialize"));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        PKeyDSA result = new PKeyDSA(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public PKeyDSA(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    private DSAPrivateKey privKey;
    private DSAPublicKey pubKey;

    public IRubyObject initialize(IRubyObject[] args) {
        Object rsa;
        IRubyObject arg;
        IRubyObject pass = null;
        String passwd = null;
        if(checkArgumentCount(args,0,2) == 0) {
            rsa = null; //DSA.new
        } else {
            arg = args[0];
            if(args.length > 1) {
                pass = args[1];
            }
            if(arg instanceof RubyFixnum) {
                //	rsa = rsa_generate(FIX2INT(arg), NIL_P(pass) ? DSA_F4 : NUM2INT(pass));
                //	if (!rsa) ossl_raise(eDSAError, NULL);
            } else {
                if(pass != null && !pass.isNil()) {
                    passwd = pass.toString();
                }
                String input = arg.toString();

                Object val = null;
                KeyFactory fact = null;
                try {
                    fact = KeyFactory.getInstance("DSA");
                } catch(Exception e) {
                    throw getRuntime().newLoadError("unsupported key algorithm (DSA)");
                }

                if(null == val) {
                    try {
                        val = fact.generatePublic(new X509EncodedKeySpec(input.getBytes("PLAIN")));
                    } catch(Exception e) {
                        val = null;
                    }
                }
                if(null == val) {
                    try {
                        val = fact.generatePrivate(new PKCS8EncodedKeySpec(input.getBytes("PLAIN")));
                    } catch(Exception e) {
                        val = null;
                    }
                }
                if(null == val) {
                    try {
                        val = OpenSSLImpl.getPEMHandler().readPEM(new StringReader(input),passwd);
                    } catch(Exception e3) {
                        val = null;
                    }
                }
                if(null == val) {
                    throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("PKey"))).getConstant("DSAError")), "Neither PUB key nor PRIV key:", true);
                }

                if(val instanceof KeyPair) {
                    privKey = (DSAPrivateKey)(((KeyPair)val).getPrivate());
                    pubKey = (DSAPublicKey)(((KeyPair)val).getPublic());
                } else if(val instanceof DSAPrivateKey) {
                    privKey = (DSAPrivateKey)val;
                } else if(val instanceof DSAPublicKey) {
                    pubKey = (DSAPublicKey)val;
                    privKey = null;
                } else {
                    throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("PKey"))).getConstant("DSAError")), "Neither PUB key nor PRIV key:", true);
                }
            }
        }

        return this;
    }
}// PKeyDSA
