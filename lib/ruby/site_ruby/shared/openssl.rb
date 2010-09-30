=begin
= $RCSfile$ -- Loader for all OpenSSL C-space and Ruby-space definitions

= Info
  'OpenSSL for Ruby 2' project
  Copyright (C) 2002  Michal Rokos <m.rokos@sh.cvut.cz>
  All rights reserved.

= Licence
  This program is licenced under the same licence as Ruby.
  (See the file 'LICENCE'.)

= Version
  $Id: openssl.rb 12496 2007-06-08 15:02:04Z technorama $
=end

require 'openssl.so'

begin
  # try to add BC jars from gem
  require 'bouncy-castle-java'
rescue LoadError
  # runs under restricted mode.
end

require 'openssl/jruby-ossl-ext.jar'
require 'opensslext'

require 'openssl/bn'
require 'openssl/cipher'
require 'openssl/config'
require 'openssl/digest'
unless OpenSSL.autoload? :PKCS7
  require 'openssl/pkcs7'
end
unless OpenSSL.autoload? :X509
  require 'openssl/x509-internal'
  unless OpenSSL.autoload? :SSL
    require 'openssl/ssl-internal'
  end
end
