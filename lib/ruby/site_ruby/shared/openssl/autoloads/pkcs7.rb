warn "OpenSSL PKCS7 implementation unavailable"
warn "gem install bouncy-castle-java for full support."

module OpenSSL
  class PKCS7
    # this definition causes TypeError "superclass mismatch for class PKCS7"
    # MRI also crashes following definition;
    #   class Foo; class Foo < Foo; end; end
    #   class Foo; class Foo < Foo; end; end
    #
    # class PKCS7 < PKCS7; end
  end
end
