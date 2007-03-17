require 'test/unit'

class TestLine
  attr_accessor :pattern, :error, :fail, :icase, :error_name, 
                :match_against, :matching_substring, :subexpressions
  def succeed
    !@error && !@fail
  end
  
  def self.create str
    parts = str.split /\t+/
    return nil if parts.length < 3

    tl = TestLine.new
    
    tl.pattern = transform parts[0]

    parts[1].each_byte do |ch|
      ch = ch.chr
      case ch
      when 'C': tl.error = true; tl.error_name = parts[2]
      when 'i': tl.icase = true
      when 'n':
      when '-': 
      else return nil # $stderr.puts "Can't handle flag #{ch} for definition \"#{str}\""; return nil
      end
    end

    return tl if tl.error
    tl.match_against = transform parts[2]
    if parts.length == 3
      tl.fail = true
      return tl
    end

    tl.matching_substring = mtransform parts[3]

    if parts.length > 4
      tl.subexpressions = parts[4].split(/,/).map{|n| mtransform(n)}
    end
    tl
  end
  
  def self.transform str
    return '' if str == '""'
    str.tr('NSTZ',"\n \t\0")
  end
  
  def self.mtransform str
    return nil if str == '-'
    return '' if /^@/ =~ str
    str.tr('NSTZ',"\n \t\0")
  end
end

def read_file
  lines = open("test/regexp_test_data"){|f| f.to_a }
  lines.select{|s| s.strip != '' && /^#/ !~ s }.map{|s| TestLine.create(s.strip)}.compact
end

$CONFIGURATION = read_file

class RubyRegexpTest < Test::Unit::TestCase
  $CONFIGURATION.each_with_index do |tl,ix|
    define_method :"test_regexp_#{ix}" do 
      dotest(tl)
    end
  end

  def dotest(tl)
    flags = 0
    flags |= Regexp::IGNORECASE if tl.icase

    if tl.error
      assert_raise(RegexpError, "Shouldn't be able to compile pattern: \"#{tl.pattern}\" Error: #{tl.error_name}") do 
        Regexp.compile tl.pattern, flags
      end
    else
      p = Regexp.compile tl.pattern, flags
      if tl.fail
        assert_no_match p, tl.match_against
      else
        assert p =~ tl.match_against, "#{p} should match #{tl.match_against}"
        m = $~
        assert_equal tl.matching_substring, m[0], "#{p} should match #{tl.match_against}"
        if tl.subexpressions
          assert_equal tl.subexpressions, m[1..-1], "#{p} should match #{tl.match_against}"
        end
      end
    end
  end
end
