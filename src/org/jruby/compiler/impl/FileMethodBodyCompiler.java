package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;

/**
 * FileMethodBodyCompiler is the base compiler for all top-level bodies
 */
public class FileMethodBodyCompiler extends MethodBodyCompiler {
    protected boolean specificArity;

    public FileMethodBodyCompiler(StandardASMCompiler scriptCompiler, String rubyName, String javaName, ASTInspector inspector, StaticScope scope) {
        // scope is cached in-body, rather than beforehand
        super(scriptCompiler, javaName, rubyName, inspector, scope, -1);
    }

    @Override
    public void beginMethod(CompilerCallback args, StaticScope scope) {
        method.start();

        variableCompiler.beginMethod(args, scope);
        
        // cache scope and store index
        scopeIndex = script.getCacheCompiler().cacheStaticScope(this, scope);
        method.pop();

        // visit a label to start scoping for local vars in this method
        method.label(scopeStart);
    }

    public boolean isSimpleRoot() {
        return !inNestedMethod;
    }
}
