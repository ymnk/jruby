require 'java'
require File.expand_path('../../../build/jruby-test-classes.jar', __FILE__)
require 'rspec'

RSpec.configure do |config|
  config.before :suite do
    require File.expand_path('../../../test/test_helper', __FILE__)
    include TestHelper
  end

  # config.after :each do
  # end
end
