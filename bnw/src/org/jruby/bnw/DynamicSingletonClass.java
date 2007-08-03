package org.jruby.bnw;

public abstract class DynamicSingletonClass extends DynamicClass implements Singleton {

    private final Object attachedObject;
    
    protected DynamicSingletonClass(
            final Registry registry,
            final MetaClass superclass,
            final Object attachedObject
            ) {
        super(registry, superclass);
        this.attachedObject = attachedObject;
    }
    
    protected DynamicSingletonClass(
            final Registry registry,
            final Object attachedObject
            ) {
        super(registry);
        this.attachedObject = attachedObject;
    }
    
    protected DynamicSingletonClass(
            final DynamicClass superclass,
            final Object attachedObject
            ) {
        super(superclass);
        this.attachedObject = attachedObject;
    }
    
    public final boolean isSingleton() {
        return true;
    }
    
    public final Object getAttachedObject() {
        return attachedObject;
    }
    
}
