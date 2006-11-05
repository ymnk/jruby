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

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Extensions {
    public static void createX509Ext(IRuby runtime, RubyModule mX509) {
        RubyClass cX509ExtFactory = mX509.defineClassUnder("ExtensionFactory",runtime.getObject());
        mX509.defineClassUnder("ExtensionError",runtime.getModule("OpenSSL").getClass("OpenSSLError"));
        
        CallbackFactory extfcb = runtime.callbackFactory(ExtensionFactory.class);
        cX509ExtFactory.defineSingletonMethod("new",extfcb.getOptSingletonMethod("newInstance"));
        cX509ExtFactory.defineMethod("initialize",extfcb.getOptMethod("initialize"));

        cX509ExtFactory.attr_reader(new IRubyObject[]{runtime.newString("issuer_certificate"),runtime.newString("subject_certificate"),
                                            runtime.newString("subject_request"),runtime.newString("crl"),
                                            runtime.newString("config")});
        cX509ExtFactory.defineMethod("issuer_certificate=",extfcb.getMethod("set_issuer_cert",IRubyObject.class));
        cX509ExtFactory.defineMethod("subject_certificate=",extfcb.getMethod("set_subject_cert",IRubyObject.class));
        cX509ExtFactory.defineMethod("subject_request=",extfcb.getMethod("set_subject_req",IRubyObject.class));
        cX509ExtFactory.defineMethod("crl=",extfcb.getMethod("set_crl",IRubyObject.class));
        cX509ExtFactory.defineMethod("config=",extfcb.getMethod("set_config",IRubyObject.class));
        cX509ExtFactory.defineMethod("create_ext",extfcb.getOptMethod("create_ext"));

        RubyClass cX509Ext = mX509.defineClassUnder("Extension",runtime.getObject());
        CallbackFactory extcb = runtime.callbackFactory(Extension.class);
        cX509Ext.defineSingletonMethod("new",extcb.getOptSingletonMethod("newInstance"));
        cX509Ext.defineMethod("initialize",extcb.getOptMethod("_initialize"));
        cX509Ext.defineMethod("oid=",extcb.getMethod("set_oid",IRubyObject.class));
        cX509Ext.defineMethod("value=",extcb.getMethod("set_value",IRubyObject.class));
        cX509Ext.defineMethod("critical=",extcb.getMethod("set_critical",IRubyObject.class));
        cX509Ext.defineMethod("oid",extcb.getMethod("oid"));
        cX509Ext.defineMethod("value",extcb.getMethod("value"));
        cX509Ext.defineMethod("critical?",extcb.getMethod("critical_p"));
        cX509Ext.defineMethod("to_der",extcb.getMethod("to_der"));
    }

    public static class ExtensionFactory extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            ExtensionFactory result = new ExtensionFactory(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public ExtensionFactory(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        public IRubyObject initialize(IRubyObject[] args) {
            checkArgumentCount(args,0,4);
            if(args.length > 0 && !args[0].isNil()) {
                set_issuer_cert(args[0]);
            }
            if(args.length > 1 && !args[1].isNil()) {
                set_subject_cert(args[1]);
            }
            if(args.length > 2 && !args[2].isNil()) {
                set_subject_req(args[2]);
            }
            if(args.length > 3 && !args[3].isNil()) {
                set_crl(args[3]);
            }
            return this;
        }

        public IRubyObject set_issuer_cert(IRubyObject arg) {
            setInstanceVariable("@issuer_certificate",arg);
            return arg;
        }

        public IRubyObject set_subject_cert(IRubyObject arg) {
            setInstanceVariable("@subject_certificate",arg);
            return arg;
        }

        public IRubyObject set_subject_req(IRubyObject arg) {
            setInstanceVariable("@subject_request",arg);
            return arg;
        }

        public IRubyObject set_crl(IRubyObject arg) {
            setInstanceVariable("@crl",arg);
            return arg;
        }

        public IRubyObject set_config(IRubyObject arg) {
            setInstanceVariable("@config",arg);
            return arg;
        }

        private DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
            Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
            if(null != val1) {
                return (DERObjectIdentifier)val1;
            }
            DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
            return val2;
        }

        private static boolean isHexDigit(char c) {
            return ('0'<=c && c<='9') || ('A'<= c && c <= 'F') || ('a'<= c && c <= 'f');
        }

        public IRubyObject create_ext(IRubyObject[] args) throws Exception {
            IRubyObject critical = getRuntime().getFalse();
            if(checkArgumentCount(args,2,3) == 3 && !args[2].isNil()) {
                critical = args[2];
            }
            String oid = args[0].toString();
            String value = args[1].toString();

            DERObjectIdentifier r_oid = null;

            try {
                r_oid = getObjectIdentifier(oid);
            } catch(IllegalArgumentException e) {
                r_oid = null;
            }
            if(null == r_oid) {
                throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), "unknown OID `" + oid + "'", true);
            }

            Extension ext = (Extension)(((RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("Extension"))).callMethod("new"));

            if(value.startsWith("critical,")) {
                critical = getRuntime().getTrue();
                value = value.substring(9).trim();
            }

            if(r_oid.equals(new DERObjectIdentifier("2.5.29.14"))) { //subjectKeyIdentifier
                if("hash".equalsIgnoreCase(value)) {
                    IRubyObject pkey = getInstanceVariable("@subject_certificate").callMethod("public_key");
                    IRubyObject val = null;
                    if(pkey instanceof PKeyRSA) {
                        val = pkey.callMethod("to_der");
                    } else {
                        val = ASN1.decode(getRuntime().getModule("OpenSSL").getConstant("ASN1"),pkey.callMethod("to_der")).callMethod("value").callMethod("[]",getRuntime().newFixnum(1)).callMethod("value");
                    }
                    byte[] b = MessageDigest.getInstance("SHA-1").digest(val.toString().getBytes("PLAIN"));
                    value = new String(new DEROctetString(b).getDEREncoded(),"ISO8859_1");
                } else {
                    StringBuffer nstr = new StringBuffer();
                    for(int i = 0; i < value.length(); i+=2) {
                        if(i+1 >= value.length()) {
                            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), oid + " = " + value + ": odd number of digits", true);
                        }

                        char c1 = value.charAt(i);
                        char c2 = value.charAt(i+1);
                        if(isHexDigit(c1) && isHexDigit(c2)) {
                            nstr.append(Character.toUpperCase(c1)).append(Character.toUpperCase(c2));
                        } else {
                            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), oid + " = " + value + ": illegal hex digit", true);
                        }
                        while((i+2) < value.length() && value.charAt(i+2) == ':') {
                            i++;
                        }
                    }
                    String v = nstr.toString();
                    byte[] arr = new byte[v.length()/2];
                    String cur = null;
                    for(int i=0;i<v.length();i+=2) {
                        arr[i/2] = (byte)Integer.parseInt(v.substring(i,i+2),16);
                    }
                    value = new String(new DEROctetString(arr).getDEREncoded(),"ISO8859_1");
                }
            } else if(r_oid.equals(new DERObjectIdentifier("2.5.29.19"))) { //basicConstraints
                String[] spl = value.split(",");
                for(int i=0;i<spl.length;i++) {
                    spl[i] = spl[i].trim();
                }
                ASN1EncodableVector asnv = new ASN1EncodableVector();
                for(int i=0;i<spl.length;i++) {
                    if(spl[i].length() > 3 && spl[i].substring(0,3).equalsIgnoreCase("CA:")) {
                        asnv.add(new DERBoolean("TRUE".equalsIgnoreCase(spl[i].substring(3).trim())));
                    }
                }
                for(int i=0;i<spl.length;i++) {
                    if(spl[i].length() > 8 && spl[i].substring(0,8).equalsIgnoreCase("pathlen:")) {
                        asnv.add(new DERInteger(Integer.parseInt(spl[i].substring(8).trim())));
                    }
                }
                value = new String(new DERSequence(asnv).getDEREncoded(),"ISO8859_1");
            } else if(r_oid.equals(new DERObjectIdentifier("2.5.29.15"))) { //keyUsage
                byte v1 = 0;
                byte v2 = 0;
                String[] spl = value.split(",");
                for(int i=0;i<spl.length;i++) {
                    spl[i] = spl[i].trim();
                }
                for(int i=0;i<spl.length;i++) {
                    if("decipherOnly".equals(spl[i].trim()) || "Decipher Only".equals(spl[i].trim())) {
                        v2 |= (byte)128;
                    } else if("digitalSignature".equals(spl[i].trim()) || "Digital Signature".equals(spl[i].trim())) {
                        v1 |= (byte)128;
                    } else if("nonRepudiation".equals(spl[i].trim()) || "Non Repudiation".equals(spl[i].trim())) {
                        v1 |= (byte)64;
                    } else if("keyEncipherment".equals(spl[i].trim()) || "Key Encipherment".equals(spl[i].trim())) {
                        v1 |= (byte)32;
                    } else if("dataEncipherment".equals(spl[i].trim()) || "Data Encipherment".equals(spl[i].trim())) {
                        v1 |= (byte)16;
                    } else if("keyAgreement".equals(spl[i].trim()) || "Key Agreement".equals(spl[i].trim())) {
                        v1 |= (byte)8;
                    } else if("keyCertSign".equals(spl[i].trim()) || "Key Cert Sign".equals(spl[i].trim())) {
                        v1 |= (byte)4;
                    } else if("cRLSign".equals(spl[i].trim())) {
                        v1 |= (byte)2;
                    } else if("encipherOnly".equals(spl[i].trim()) || "Encipher Only".equals(spl[i].trim())) {
                        v1 |= (byte)1;
                    } else {
                        throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getConstant("ExtensionError")), oid + " = " + value + ": unknown bit string argument", true);
                    }
                }
                byte[] inp;
                if(v2 != 0) {
                    inp = new byte[]{v1,v2};
                } else {
                    inp = new byte[]{v1};
                }
                int unused = 0;
                for(int i = (inp.length-1); i>-1; i--) {
                    if(inp[i] == 0) {
                        unused += 8;
                    } else {
                        byte a2 = inp[i];
                        int x = 8;
                        while(a2 != 0) {
                            a2 <<= 1;
                            x--;
                        }
                        unused += x;
                        break;
                    }
                }
                
                value = new String(new DERBitString(inp,unused).getDEREncoded(),"ISO8859_1");
            }

            ext.setRealOid(r_oid);
            ext.setRealValue(value);
            ext.setRealCritical(critical.isTrue());

            return ext;
        }
    }

    public static class Extension extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            Extension result = new Extension(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public Extension(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        private DERObjectIdentifier oid;
        private String value;
        private boolean critical;

        void setRealOid(DERObjectIdentifier oid) {
            this.oid = oid;
        }

        void setRealValue(String value) {
            this.value = value;
        }

        void setRealCritical(boolean critical) {
            this.critical = critical;
        }
        
        DERObjectIdentifier getRealOid() {
            return oid;
        }

        String getRealValue() {
            return value;
        }

        boolean getRealCritical() {
            return critical;
        }

        private DERObjectIdentifier getObjectIdentifier(String nameOrOid) {
            Object val1 = ASN1.getOIDLookup(getRuntime()).get(nameOrOid.toLowerCase());
            if(null != val1) {
                return (DERObjectIdentifier)val1;
            }
            DERObjectIdentifier val2 = new DERObjectIdentifier(nameOrOid);
            return val2;
        }

        public IRubyObject _initialize(IRubyObject[] args) throws Exception {
            byte[] octets = null;
            if(args.length == 1) {
                ASN1InputStream is = new ASN1InputStream(OpenSSLImpl.to_der_if_possible(args[0]).toString().getBytes("PLAIN"));
                Object obj = is.readObject();
                ASN1Sequence seq = (ASN1Sequence)obj;
                setRealOid((DERObjectIdentifier)(seq.getObjectAt(0)));
                setRealCritical(((DERBoolean)(seq.getObjectAt(1))).isTrue());
                octets = ((DEROctetString)(seq.getObjectAt(2))).getOctets();
            } else if(args.length > 1) {
                setRealOid(getObjectIdentifier(args[0].toString()));
                octets = args[1].toString().getBytes("PLAIN");
            }
            if(args.length > 2) {
                setRealCritical(args[2].isTrue());
            }
            if(args.length > 0) {
                setRealValue(new String(octets,"ISO8859_1"));
            }

            return this;
        }

        public IRubyObject set_oid(IRubyObject arg) {
            System.err.println("WARNING: calling ext#oid=");
            return getRuntime().getNil();
        }

        public IRubyObject set_value(IRubyObject arg) {
            System.err.println("WARNING: calling ext#value=");
            return getRuntime().getNil();
        }

        public IRubyObject set_critical(IRubyObject arg) {
            System.err.println("WARNING: calling ext#critical=");
            return getRuntime().getNil();
        }

        public IRubyObject oid() {
            return getRuntime().newString((String)(ASN1.getSymLookup(getRuntime()).get(oid)));
        }

        public IRubyObject value() throws Exception {
            if(getRealOid().equals(new DERObjectIdentifier("2.5.29.19"))) { //basicConstraints
                ASN1Sequence seq2 = (ASN1Sequence)(new ASN1InputStream(value.getBytes("PLAIN")).readObject());
                String c = "";
                String path = "";
                if(seq2.size()>0) {
                    c = "CA:" + (((DERBoolean)(seq2.getObjectAt(0))).isTrue() ? "TRUE" : "FALSE");
                }
                if(seq2.size()>1) {
                    path = ", pathlen:" + seq2.getObjectAt(1).toString();
                }
                return getRuntime().newString(c+path);
            } else if(getRealOid().equals(new DERObjectIdentifier("2.5.29.15"))) { //keyUsage
                byte[] bs = value.substring(2).getBytes("PLAIN");
                byte b1 = 0;
                byte b2 = bs[0];
                if(bs.length>1) {
                    b1 = bs[1];
                }
                StringBuffer sbe = new StringBuffer();
                String sep = "";
                if((b2 & (byte)128) != 0) {
                    sbe.append(sep).append("Decipher Only");
                    sep = ", ";
                }
                if((b1 & (byte)128) != 0) {
                    sbe.append(sep).append("Digital Signature");
                    sep = ", ";
                }
                if((b1 & (byte)64) != 0) {
                    sbe.append(sep).append("Non Repudiation");
                    sep = ", ";
                }
                if((b1 & (byte)32) != 0) {
                    sbe.append(sep).append("Key Encipherment");
                    sep = ", ";
                }
                if((b1 & (byte)16) != 0) {
                    sbe.append(sep).append("Data Encipherment");
                    sep = ", ";
                }
                if((b1 & (byte)8) != 0) {
                    sbe.append(sep).append("Key Agreement");
                    sep = ", ";
                }
                if((b1 & (byte)4) != 0) {
                    sbe.append(sep).append("Key Cert Sign");
                    sep = ", ";
                }
                if((b1 & (byte)2) != 0) {
                    sbe.append(sep).append("cRLSign");
                    sep = ", ";
                }
                if((b1 & (byte)1) != 0) {
                    sbe.append(sep).append("Encipher Only");
                }
                return getRuntime().newString(sbe.toString());
            } else if(getRealOid().equals(new DERObjectIdentifier("2.5.29.17"))) { //subjectAltName
                return getRuntime().newString(value);
            } else {
                return getRuntime().newString(Utils.toHex(value.substring(2).getBytes("PLAIN"),':'));
            }
        }

        public IRubyObject critical_p() {
            return critical ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject to_der() throws Exception {
            ASN1EncodableVector all = new ASN1EncodableVector();
            all.add(getRealOid());
            all.add(getRealCritical() ? DERBoolean.TRUE : DERBoolean.FALSE);
            all.add(new DEROctetString(value.getBytes("PLAIN")));
            return getRuntime().newString(new String(new DERSequence(all).getDEREncoded(),"ISO8859_1"));
        }
    }
}// X509Extensions
