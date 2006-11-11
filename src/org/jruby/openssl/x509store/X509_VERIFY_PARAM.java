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

import java.util.Date;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class X509_VERIFY_PARAM {
    public String name;
    public Date check_time;
    public long inh_flags;
    public long flags;
    public int purpose;
    public int trust;
    public int depth;
    public List policies;

    public X509_VERIFY_PARAM() { 
        zero();
    }

    private void zero() {
        name = null;
        purpose = 0;
        trust = 0;
        inh_flags = X509.X509_VP_FLAG_DEFAULT;
        flags = 0;
        depth = -1;
        policies = null;
    }

    public void free() {
        zero();
    }
    
    public int inherit(X509_VERIFY_PARAM from) { 
        return -1; 
    } //TODO: implement
    
    public int set1(X509_VERIFY_PARAM from) { 
	inh_flags |= X509.X509_VP_FLAG_DEFAULT;
	return inherit(from);
    } 
    
    public int set1_name(String name) { 
        this.name = name;
        return 1;
    }
    
    public int set_flags(long flags) { 
	this.flags |= flags;
	if((flags & X509.V_FLAG_POLICY_MASK) == X509.V_FLAG_POLICY_MASK) {
            this.flags |= X509.V_FLAG_POLICY_CHECK;
        }
        return 1;
    } 
    
    public int clear_flags(long flags) { 
	this.flags &= ~flags;
	return 1;
    } 
    
    public long get_flags() { 
	return flags;
    } 
    
    public int set_purpose(int purpose) { 
        int[] arg = new int[]{this.purpose};
        int v = X509_PURPOSE.set(arg,purpose);
        this.purpose = arg[0];
        return v;
    } 
    
    public int set_trust(int trust) { 
        int[] arg = new int[]{this.trust};
        int v = X509_TRUST.set(arg,trust);
        this.trust = arg[0];
        return v;
    }
    
    public void set_depth(int depth) {
	this.depth = depth;
    }
    
    public void set_time(Date t) {
	this.check_time = t;
	this.flags |= X509.V_FLAG_USE_CHECK_TIME;
    } 
    
    public int add0_policy(Object policy) { 
        if(policies == null) {
            policies = new ArrayList();
        }
        policies.add(policy);
        return 1;
    }
    
    public int set1_policies(List policies) { 
        return -1; 
    } //TODO: implement
    
    public int get_depth() { 
	return depth;
    }
    
    public int add0_table() { 
        return -1; 
    } //TODO: implement

    public static X509_VERIFY_PARAM lookup(String name) { 
        return null; 
    } //TODO: implement
    
    public static void table_cleanup() {
    } //TODO: implement
}// X509_VERIFY_PARAM
