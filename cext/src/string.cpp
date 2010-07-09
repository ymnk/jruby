/*
 * Copyright (C) 2008-2010 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <sys/param.h>
#include <jni.h>

#include "JLocalEnv.h"
#include "jruby.h"
#include "JUtil.h"
#include "ruby.h"
#include "Handle.h"

using namespace jruby;


static VALUE
newString(const char* ptr, int len, int capacity = 0, bool tainted = false)
{
    JLocalEnv env;

    jbyteArray bytes = env->NewByteArray(capacity > len ? capacity : len);
    checkExceptions(env);

    if (len > 0 && ptr != NULL) {
        env->SetByteArrayRegion(bytes, 0, len, (jbyte *) ptr);
        checkExceptions(env);
    }

    jlong result = env->CallStaticLongMethod(JRuby_class, JRuby_newString, jruby::getRuntime(), bytes, (jboolean) tainted);
    checkExceptions(env);

    return (VALUE) result;
}

extern "C" VALUE
rb_str_new(const char *ptr, long len)
{
    return newString(ptr, len);
}

extern "C" VALUE
rb_str_new_cstr(const char *ptr)
{
    return newString(ptr, ptr ? strlen(ptr) : 0);
}

#define STR_BUF_MIN_SIZE 128
extern "C" VALUE
rb_str_buf_new(long capacity)
{
    capacity = MAX(capacity, STR_BUF_MIN_SIZE);

    return newString(NULL, 0, MAX(capacity, STR_BUF_MIN_SIZE), false);
}

extern "C" VALUE
rb_str_dup(VALUE str)
{
    return callMethod(str, "dup", 0, NULL);
}

extern "C" VALUE
rb_str_append(VALUE str, VALUE str2)
{
    return callMethod(str, "append", 1, str2);
}

extern "C" VALUE
rb_str_cat(VALUE str, const char *ptr, long len)
{
    return callMethod(str, "concat", 1, rb_str_new(ptr, len));
}

extern "C" VALUE
rb_str_cat2(VALUE self_handle, const char* other) {
    return rb_str_cat(self_handle, other, std::strlen(other));
}

extern "C" VALUE
rb_str_plus(VALUE str1, VALUE str2)
{
    return callMethod(str1, "+", 1, str2);
}

extern "C" VALUE
rb_str_buf_cat(VALUE str, const char *ptr, long len)
{
    return callMethod(str, "concat", 1, rb_str_new(ptr, len));
}

extern "C" int
rb_str_cmp(VALUE str1, VALUE str2)
{
    return NUM2INT(callMethod(str1, "<=>", 1, str2));
}

extern "C" VALUE
rb_str_split(VALUE str, const char *sep)
{
    return callMethod(str, "split", 1, rb_str_new2(sep));
}

extern "C" VALUE
rb_str2inum(VALUE str, int base)
{
    return callMethod(str, "to_i", 1, INT2NUM(base));
}

extern "C" VALUE
rb_cstr2inum(const char* str, int base)
{
    return callMethod(rb_str_new2(str), "to_i", 1, INT2NUM(base));
}

extern "C" VALUE
rb_str_substr(VALUE str, long beg, long len)
{
    return callMethod(str, "substr", 2, LONG2NUM(beg), LONG2NUM(len));
}

extern "C" VALUE
rb_tainted_str_cstr(const char *ptr)
{
    int len = strlen(ptr);

    return newString(ptr, len, len, true);
}

extern "C" VALUE
rb_tainted_str_new(const char* ptr, long len)
{
    return newString(ptr, len, len, true);
}

extern "C" VALUE
rb_obj_as_string(VALUE obj)
{
    return callMethod(obj, "to_s", 0);
}

extern "C" VALUE
rb_str_to_str(VALUE obj)
{
    return callMethod(obj, "to_str", 0);
}

extern "C" VALUE
rb_string_value(VALUE* ptr)
{
    return *ptr = callMethod(*ptr, "to_s", 0);
}

extern "C" char* 
rb_string_value_ptr(VALUE* object_variable) {
    VALUE str = rb_string_value(object_variable);    
    return (char*)(RSTRING_PTR(str));
}

extern "C" char* 
rb_string_value_cstr(VALUE* object_variable) {
    VALUE str = rb_string_value(object_variable);
    long long str_size = NUM2LL(callMethod(str, "length", 0));
    char* cstr = (char*)(RSTRING_PTR(str));

    if(str_size != strlen(cstr)) {
      rb_raise(rb_eArgError, "string contains NULL byte");
    }

    return cstr;
}

extern "C" VALUE 
rb_str_freeze(VALUE str) {
    return callMethodA(str, "freeze", 0, NULL);
}

extern "C" size_t
rb_str_len(VALUE str) {
    return NUM2INT(callMethod(str, "length", 0, NULL));
}

static RubyString*
jruby_str(VALUE v)
{
    if (TYPE(v) != T_STRING) {
        rb_raise(rb_eTypeError, "wrong type (expected String)");
    }

    return (RubyString *) v;
}

extern "C" char*
rb_str_ptr_readonly(VALUE v)
{
    return jruby_str(v)->toRString(true)->as.heap.ptr;    
}

extern "C" char*
rb_str2cstr(VALUE str, long* len) {
    char* cstr = RSTRING_PTR(str);
    if (len) {
        *len = RSTRING_LEN(str);
    } else if(*len != strlen(cstr)) {
        rb_warn("string contains \\0 character");
    }
    return cstr;
}

extern "C" char*
jruby_str_ptr(VALUE v)
{
    return jruby_rstring(v)->as.heap.ptr;
}

extern "C" int
jruby_str_length(VALUE v)
{
    return jruby_str(v)->length();
}

extern "C" struct RString*
jruby_rstring(VALUE v)
{
    return jruby_str(v)->toRString(false);
}