Dir.glob(File.expand_path("test_*.rb", File.dirname(__FILE__))).sort.reject { |t|
  t =~ /test_all/
}.each { |t|
  require t
}
