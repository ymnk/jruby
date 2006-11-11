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
package org.jruby.openssl.x509store;

import java.security.cert.Certificate;
import java.security.cert.CRL;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_STORE_CTX {
    public X509_STORE ctx;
    public int current_method;

    public Certificate cert;
    public List untrusted; //List<Certificate>
    public List crls; //List<CRL>

    public X509_VERIFY_PARAM param;

    public Object other_ctx;

    public Function1 verify;
    public Function2 verify_cb;
    public Function3 get_issuer;
    public Function3 check_issued;
    public Function1 check_revocation;
    public Function3 get_crl;
    public Function2 check_crl;
    public Function3 cert_crl;
    public Function1 check_policy;
    public Function1 cleanup;

    public boolean valid;
    public int last_untrusted;
    
    public List chain; //List<Certificate>
    //    public X509_POLICY_TREE tree; //TODO: implement

    public int explicit_policy;

    public int error_depth;
    public int error;
    public Certificate current_cert;
    public Certificate current_issuer;
    public CRL current_crl;

    public List ex_data;

    public void set_depth(int depth) { //TODO: implement
    }

    public void set_app_data(Object data) {
        set_ex_data(0,data);
    }

    public Object get_app_data() {
        return get_ex_data(0);
    }

    public X509_STORE_CTX() { //TODO: implement
    }

    public int get1_issuer(Certificate[] issuer, Certificate x) { return -1; } //TODO: implement

    public void free() {} //TODO: implement
    public int init(X509_STORE store, Certificate x509, List chain) { return -1; } //TODO: implement
    public void trusted_stack(List sk) {} //TODO: implement
    public void cleanup() {} //TODO: implement
    public int set_ex_data(int idx,Object data) { return -1; } //TODO: implement
    public Object get_ex_data(int idx) { return null; } //TODO: implement
    public int get_error() { return -1; } //TODO: implement
    public void set_error(int s) {} //TODO: implement
    public int get_error_depth() { return -1; } //TODO: implement
    public Certificate get_current_cert() { return null; } //TODO: implement
    public List get_chain() { return null; } //TODO: implement
    public List get1_chain() { return null; } //TODO: implement
    public void set_cert(Certificate x) {} //TODO: implement
    public void set_chain(List sk) {} //TODO: implement
    public void set0_crls(List sk) {} //TODO: implement
    public int set_purpose(int purpose) { return -1; } //TODO: implement
    public int set_trust(int trust) { return -1; } //TODO: implement
    public int purpose_inherit(int def_purpose,int purpose, int trust) { return -1; } //TODO: implement
    public void set_flags(long flags) {} //TODO: implement
    public void set_time(long flags,Date t) {} //TODO: implement
    public void set_verify_cb(Function2 verify_cb) {} //TODO: implement
    //X509_POLICY_TREE get0_policy_tree(); //TODO: implement
    public int get_explicit_policy() { return -1; } //TODO: implement

    public X509_VERIFY_PARAM get0_param() { return null; } //TODO: implement
    public void set0_param(X509_VERIFY_PARAM param) {} //TODO: implement
    public int set_default(String name) { return -1; } //TODO: implement

    public int get_by_subject(int type,X509_NAME name,X509_OBJECT ret) { return -1; } //TODO: implement
}// X509_STORE_CTX
