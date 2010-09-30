warn "OpenSSL Netscape implementation unavailable"
warn "gem install bouncy-castle-java for full support."

module OpenSSL
  module Netscape
    class SPKI; end
  end
end
