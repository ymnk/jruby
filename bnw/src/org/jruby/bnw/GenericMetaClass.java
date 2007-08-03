package org.jruby.bnw;

public class GenericMetaClass extends DynamicClass implements MetaClass {

    private String name;
    
    public GenericMetaClass(Registry registry, String name, MetaClass superclass) {
        super(registry, superclass);
        this.name = name;
    }
    
    // base name, may be null for anonymous
    // TODO: incorporate logic from RubyModule/RubyClass?
    public String getName() {
        return name;
    }
    
}
