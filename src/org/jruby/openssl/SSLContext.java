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

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.SSLEngine;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.RubyObject;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class SSLContext extends RubyObject {
    private final static String[] ctx_attrs = {
    "cert", "key", "client_ca", "ca_file", "ca_path",
    "timeout", "verify_mode", "verify_depth",
    "verify_callback", "options", "cert_store", "extra_chain_cert",
    "client_cert_cb", "tmp_dh_callback", "session_id_context"};

    public static void createSSLContext(IRuby runtime, RubyModule mSSL) {
        RubyClass cSSLContext = mSSL.defineClassUnder("SSLContext",runtime.getObject());
        for(int i=0;i<ctx_attrs.length;i++) {
            cSSLContext.attr_accessor(new IRubyObject[]{runtime.newSymbol(ctx_attrs[i])});
        }

        CallbackFactory ctxcb = runtime.callbackFactory(SSLContext.class);
        cSSLContext.defineSingletonMethod("new",ctxcb.getOptSingletonMethod("newInstance"));
        cSSLContext.defineMethod("initialize",ctxcb.getOptMethod("initialize"));
        cSSLContext.defineMethod("ciphers",ctxcb.getMethod("ciphers"));
        cSSLContext.defineMethod("ciphers=",ctxcb.getMethod("set_ciphers",IRubyObject.class));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        SSLContext result = new SSLContext(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public SSLContext(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    private IRubyObject ciphers;

    public IRubyObject initialize(IRubyObject[] args) {
        ciphers = getRuntime().getNil();
        return this;
    }

    public IRubyObject ciphers() {
        return this.ciphers;
    }

    public IRubyObject set_ciphers(IRubyObject val) {
        this.ciphers = val;
        return val;
    }

    String[] getCipherSuites(SSLEngine engine) {
        List<String> ciphs = new ArrayList<String>();
        if(this.ciphers instanceof RubyArray) {
            for(Iterator iter = ((RubyArray)this.ciphers).getList().iterator();iter.hasNext();) {
                addCipher(ciphs, iter.next().toString(),engine);
            }
        } else {
            addCipher(ciphs,this.ciphers.toString(),engine);
        }
        return ciphs.toArray(new String[ciphs.size()]);
    }

    private void addCipher(List<String> lst, String cipher, SSLEngine engine) {
        String[] supported = engine.getSupportedCipherSuites();
        if("ADH".equals(cipher)) {
            for(int i=0;i<supported.length;i++) {
                if(supported[i].indexOf("DH_anon") != -1) {
                    lst.add(supported[i]);
                }
            }
        } else {
            for(int i=0;i<supported.length;i++) {
                if(supported[i].indexOf(cipher) != -1) {
                    lst.add(supported[i]);
                }
            }
        }
    }
}// SSLContext
