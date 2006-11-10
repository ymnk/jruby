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

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public interface X509 {
    Object CRYPTO_LOCK_X509_STORE = new Object();

    int X509_FILETYPE_PEM = 1;
    int X509_FILETYPE_ASN1 = 2;
    int X509_FILETYPE_DEFAULT = 3;

    int V_OK = 0;
    int	V_ERR_UNABLE_TO_GET_ISSUER_CERT = 2;
    int	V_ERR_UNABLE_TO_GET_CRL = 3;
    int	V_ERR_UNABLE_TO_DECRYPT_CERT_SIGNATURE = 4;
    int	V_ERR_UNABLE_TO_DECRYPT_CRL_SIGNATURE = 5;
    int	V_ERR_UNABLE_TO_DECODE_ISSUER_PUBLIC_KEY = 6;
    int	V_ERR_CERT_SIGNATURE_FAILURE = 7;
    int	V_ERR_CRL_SIGNATURE_FAILURE = 8;
    int	V_ERR_CERT_NOT_YET_VALID = 9;
    int	V_ERR_CERT_HAS_EXPIRED = 10;
    int	V_ERR_CRL_NOT_YET_VALID = 11;
    int	V_ERR_CRL_HAS_EXPIRED = 12;
    int	V_ERR_ERROR_IN_CERT_NOT_BEFORE_FIELD = 13;
    int	V_ERR_ERROR_IN_CERT_NOT_AFTER_FIELD = 14;
    int	V_ERR_ERROR_IN_CRL_LAST_UPDATE_FIELD = 15;
    int	V_ERR_ERROR_IN_CRL_NEXT_UPDATE_FIELD = 16;
    int	V_ERR_OUT_OF_MEM = 17;
    int	V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT = 18;
    int	V_ERR_SELF_SIGNED_CERT_IN_CHAIN = 19;
    int	V_ERR_UNABLE_TO_GET_ISSUER_CERT_LOCALLY = 20;
    int	V_ERR_UNABLE_TO_VERIFY_LEAF_SIGNATURE = 21;
    int	V_ERR_CERT_CHAIN_TOO_LONG = 22;
    int	V_ERR_CERT_REVOKED = 23;
    int	V_ERR_INVALID_CA = 24;
    int	V_ERR_PATH_LENGTH_EXCEEDED = 25;
    int	V_ERR_INVALID_PURPOSE = 26;
    int	V_ERR_CERT_UNTRUSTED = 27;
    int	V_ERR_CERT_REJECTED = 28;

    int	V_ERR_SUBJECT_ISSUER_MISMATCH = 29;
    int	V_ERR_AKID_SKID_MISMATCH = 30;
    int	V_ERR_AKID_ISSUER_SERIAL_MISMATCH = 31;
    int	V_ERR_KEYUSAGE_NO_CERTSIGN = 32;

    int	V_ERR_UNABLE_TO_GET_CRL_ISSUER = 33;
    int	V_ERR_UNHANDLED_CRITICAL_EXTENSION = 34;
    int	V_ERR_KEYUSAGE_NO_CRL_SIGN = 35;
    int	V_ERR_UNHANDLED_CRITICAL_CRL_EXTENSION = 36;
    int	V_ERR_INVALID_NON_CA = 37;
    int	V_ERR_PROXY_PATH_LENGTH_EXCEEDED = 38;
    int	V_ERR_KEYUSAGE_NO_DIGITAL_SIGNATURE = 39;
    int	V_ERR_PROXY_CERTIFICATES_NOT_ALLOWED = 40;

    int	V_ERR_INVALID_EXTENSION = 41;
    int	V_ERR_INVALID_POLICY_EXTENSION = 42;
    int	V_ERR_NO_EXPLICIT_POLICY = 43;

    int	V_ERR_APPLICATION_VERIFICATION = 50;

    int	V_FLAG_CB_ISSUER_CHECK = 0x1;
    int	V_FLAG_USE_CHECK_TIME = 0x2;
    int	V_FLAG_CRL_CHECK = 0x4;
    int	V_FLAG_CRL_CHECK_ALL = 0x8;
    int	V_FLAG_IGNORE_CRITICAL = 0x10;
    int	V_FLAG_STRICT = 0x20;
    int	V_FLAG_ALLOW_PROXY_CERTS = 0x40;
    int V_FLAG_POLICY_CHECK = 0x80;
    int V_FLAG_EXPLICIT_POLICY = 0x100;
    int	V_FLAG_INHIBIT_ANY = 0x200;
    int V_FLAG_INHIBIT_MAP = 0x400;
    int V_FLAG_NOTIFY_POLICY = 0x800;

    int VP_FLAG_DEFAULT = 0x1;
    int VP_FLAG_OVERWRITE = 0x2;
    int VP_FLAG_RESET_FLAGS = 0x4;
    int VP_FLAG_LOCKED = 0x8;
    int VP_FLAG_ONCE = 0x10;

    /* Internal use: mask of policy related options */
    int V_FLAG_POLICY_MASK = (V_FLAG_POLICY_CHECK | 
                              V_FLAG_EXPLICIT_POLICY | 
                              V_FLAG_INHIBIT_ANY | 
                              V_FLAG_INHIBIT_MAP);

    int X509_R_BAD_X509_FILETYPE = 100;
    int X509_R_BASE64_DECODE_ERROR = 118;
    int X509_R_CANT_CHECK_DH_KEY = 114;
    int X509_R_CERT_ALREADY_IN_HASH_TABLE = 101;
    int X509_R_ERR_ASN1_LIB = 102;
    int X509_R_INVALID_DIRECTORY = 113;
    int X509_R_INVALID_FIELD_NAME = 119;
    int X509_R_INVALID_TRUST = 123;
    int X509_R_KEY_TYPE_MISMATCH = 115;
    int X509_R_KEY_VALUES_MISMATCH = 116;
    int X509_R_LOADING_CERT_DIR = 103;
    int X509_R_LOADING_DEFAULTS = 104;
    int X509_R_NO_CERT_SET_FOR_US_TO_VERIFY = 105;
    int X509_R_SHOULD_RETRY = 106;
    int X509_R_UNABLE_TO_FIND_PARAMETERS_IN_CHAIN = 107;
    int X509_R_UNABLE_TO_GET_CERTS_PUBLIC_KEY = 108;
    int X509_R_UNKNOWN_KEY_TYPE = 117;
    int X509_R_UNKNOWN_NID = 109;
    int X509_R_UNKNOWN_PURPOSE_ID = 121;
    int X509_R_UNKNOWN_TRUST_ID = 120;
    int X509_R_UNSUPPORTED_ALGORITHM = 111;
    int X509_R_WRONG_LOOKUP_TYPE = 112;
    int X509_R_WRONG_TYPE = 122;
}// X509
