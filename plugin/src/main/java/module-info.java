import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 17/01/2023
 */
@NullMarked
module consulo.fsharp {
    requires consulo.fsharp.api;
    requires consulo.dotnet.api;
    requires consulo.dotnet.psi.api;
    requires consulo.dotnet.psi.impl;

    requires consulo.code.editor.api;
    requires consulo.color.scheme.api;
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.impl;
    requires consulo.virtual.file.system.api;

    exports consulo.fsharp.impl;
    exports consulo.fsharp.impl.editor;
    exports consulo.fsharp.impl.editor.findUsage;
    exports consulo.fsharp.impl.editor.folding;
    exports consulo.fsharp.impl.editor.highlight;
    exports consulo.fsharp.impl.lang;
    exports consulo.fsharp.impl.lang.lexer;
    exports consulo.fsharp.impl.lang.parser;
    exports consulo.fsharp.impl.lang.psi;
    exports consulo.fsharp.impl.lang.psi.impl;
    exports consulo.fsharp.impl.lang.psi.light;
    exports consulo.fsharp.impl.lang.psi.resolve;
    exports consulo.fsharp.impl.lang.psi.stub;
    exports consulo.fsharp.impl.lang.psi.stub.elementTypes;
    exports consulo.fsharp.impl.lang.psi.stub.index;
}
