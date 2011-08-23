package org.jruby.compiler.impl;

import org.jruby.compiler.ASTInspector;
import org.jruby.compiler.CompilerCallback;
import org.jruby.parser.StaticScope;

/**
 * FileMethodBodyCompiler is the base compiler for all top-level bodies
 */
public class FileMethodBodyCompiler extends MethodBodyCompiler {
    protected boolean specificArity;

    public FileMethodBodyCompiler(StandardASMCompiler script, String rubyName, String javaName, ASTInspector inspector, StaticScope scope) {
        // scope is cached by caller of __file__, so we just reserve it here
        super(script, javaName, rubyName, inspector, scope, script.getCacheCompiler().reserveStaticScope());
    }

    public boolean isSimpleRoot() {
        return !inNestedMethod;
    }
}
