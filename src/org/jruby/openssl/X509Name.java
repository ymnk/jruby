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

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.bouncycastle.asn1.DERTags;

import org.jruby.IRuby;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509Name extends RubyObject {
    public static void createX509Name(IRuby runtime, RubyModule mX509) {
        RubyClass cX509Name = mX509.defineClassUnder("Name",runtime.getObject());
        mX509.defineClassUnder("NameError",runtime.getModule("OpenSSL").getClass("OpenSSLError"));

        CallbackFactory namecb = runtime.callbackFactory(X509Name.class);

        cX509Name.defineSingletonMethod("new",namecb.getOptSingletonMethod("newInstance"));
        cX509Name.defineMethod("initialize",namecb.getOptMethod("initialize"));
        cX509Name.defineMethod("add_entry",namecb.getOptMethod("add_entry"));
        cX509Name.defineMethod("to_s",namecb.getOptMethod("_to_s"));
        cX509Name.defineMethod("to_a",namecb.getMethod("to_a"));
        cX509Name.defineMethod("cmp",namecb.getMethod("cmp",IRubyObject.class));
        cX509Name.defineMethod("<=>",namecb.getMethod("cmp",IRubyObject.class));
        cX509Name.defineMethod("eql?",namecb.getMethod("eql_p",IRubyObject.class));
        cX509Name.defineMethod("hash",namecb.getMethod("hash"));
        cX509Name.defineMethod("to_der",namecb.getMethod("to_der"));
        
        cX509Name.setConstant("COMPAT",runtime.newFixnum(0));
        cX509Name.setConstant("RFC2253",runtime.newFixnum(17892119));
        cX509Name.setConstant("ONELINE",runtime.newFixnum(8520479));
        cX509Name.setConstant("MULTILINE",runtime.newFixnum(44302342));
        cX509Name.setConstant("DEFAULT_OBJECT_TYPE",runtime.newFixnum(DERTags.UTF8_STRING));

        Map val = new HashMap();
        val.put(runtime.newString("C"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("countryName"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("serialNumber"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("dnQualifier"),runtime.newFixnum(DERTags.PRINTABLE_STRING));
        val.put(runtime.newString("DC"),runtime.newFixnum(DERTags.IA5_STRING));
        val.put(runtime.newString("domainComponent"),runtime.newFixnum(DERTags.IA5_STRING));
        val.put(runtime.newString("emailAddress"),runtime.newFixnum(DERTags.IA5_STRING));
        cX509Name.setConstant("OBJECT_TYPE_TEMPLATE",new RubyHash(runtime,val,runtime.newFixnum(DERTags.UTF8_STRING)));
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        X509Name result = new X509Name(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public X509Name(IRuby runtime, RubyClass type) {
        super(runtime,type);
    }

    public IRubyObject initialize(IRubyObject[] args) {
        if(checkArgumentCount(args,0,2) == 0) {
            return this;
        }
        IRubyObject arg = args[0];
        IRubyObject template = getRuntime().getNil();
        if(args.length > 1) {
            template = args[1];
        }
        IRubyObject tmp = (arg instanceof RubyArray) ? arg : getRuntime().getNil();
        if(!tmp.isNil()) {
            if(template.isNil()) {
                template = ((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("X509"))).getClass("Name").getConstant("OBJECT_TYPE_TEMPLATE");
            }
            for(Iterator iter = ((RubyArray)tmp).getList().iterator();iter.hasNext();) {
                // ossl_x509name_init_i
            }
        } else {

        }


        /*
        
	VALUE tmp = rb_check_array_type(arg);
	if (!NIL_P(tmp)) {
	}
	else{
	    unsigned char *p;
	    VALUE str = ossl_to_der_if_possible(arg);
	    StringValue(str);
	    p = RSTRING(str)->ptr;
	    if(!d2i_X509_NAME((X509_NAME**)&DATA_PTR(self), &p, RSTRING(str)->len)){
		ossl_raise(eX509NameError, NULL);
	    }
            }*/

        return this;
    }

    public IRubyObject add_entry(IRubyObject[] args) {
        return this;
    }

    public IRubyObject _to_s(IRubyObject[] args) {
        return this;
    }

    public IRubyObject to_a() {
        return this;
    }

    public IRubyObject cmp(IRubyObject other) {
        return this;
    }

    public IRubyObject eql_p(IRubyObject other) {
        return this;
    }

    public RubyFixnum hash() {
        return null;
    }

    public IRubyObject to_der() {
        return this;
    }
}// X509Name
