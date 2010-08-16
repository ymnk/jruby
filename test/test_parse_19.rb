require 'test/unit'

if RUBY_VERSION >= "1.9"

class TestParse19 < Test::Unit::TestCase
  # run test/externals/ruby1.9/ruby/test_parse.rb for more comprehensive 1.9 parser compatibility.
  def test_block_variable
    o = Object.new
    def o.foo
      yield 42
    end

    a = b = 1
    eval <<-END # eval needed to avoid 1.8 parser crash.
      o.foo do |a;b|
        b = a
        assert_equal(42, a)
        assert_equal(42, b)
      end
    END
    assert_equal(1, a)
    assert_equal(1, b)
  end
end

end
