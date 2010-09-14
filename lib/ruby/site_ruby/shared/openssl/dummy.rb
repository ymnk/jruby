module OpenSSL
  %w[
    ASN1
    Netscape
    PKCS7
    X509
  ].each do |c|
    autoload c, "openssl/autoloads/#{c.downcase}"
  end
end
