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

import java.math.BigInteger;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_LOOKUP {
    public boolean init;
    public boolean skip;
    public X509_LOOKUP_METHOD method;
    public String method_data;
    public X509_STORE store_ctx;

    public final static int X509_L_FILE_LOAD = 1;
    public final static int X509_L_ADD_DIR = 2;

    public X509_LOOKUP(X509_LOOKUP_METHOD method) {
    }

    public int load_file(X509_CERT_FILE_CTX.Path file) {
        return ctrl(X509_L_FILE_LOAD,file.name,file.type,null);
    }

    public int add_dir(X509_HASH_DIR_CTX.Dir dir) {
        return ctrl(X509_L_ADD_DIR,dir.name,dir.type,null);
    }

    public static X509_LOOKUP_METHOD hash_dir() { return null; }
    public static X509_LOOKUP_METHOD file() { return null; }

    public int ctrl(int cmd, String argc, long argl, String[] ret) { return -1; }
    public int load_cert_file(String file, int type) { return -1; }
    public int load_crl_file(String file, int type) { return -1; }
    public int load_cert_crl_file(String file, int type) { return -1; }
    public void free() {}
    public int init() { return -1; }
    public int by_subject(int type, X509_NAME name,X509_OBJECT ret) { return -1; }
    public int by_issuer_serial(int type, X509_NAME name,BigInteger serial, X509_OBJECT ret) { return -1; }
    public int by_fingerprint(int type,String bytes, int len, X509_OBJECT ret) { return -1; }
    public int by_alias(int type, String str,int len,X509_OBJECT ret) { return -1; }
    public int shutdown() { return -1; }
}// X509_LOOKUP
