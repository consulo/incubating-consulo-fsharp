/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.fsharp.impl.lang.psi.resolve;

import consulo.annotation.access.RequiredReadAction;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.fsharp.impl.lang.psi.impl.FSharpDeclarationUtil;
import consulo.fsharp.impl.lang.psi.impl.FSharpNamespaceDeclarationImpl;
import consulo.fsharp.impl.lang.psi.impl.FSharpPsiUtil;
import consulo.fsharp.impl.lang.psi.impl.FSharpTypeDeclarationImpl;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * .NET types of F# type usages: abbreviations ({@code int}, {@code list}), function and tuple types, names relative to
 * opens and enclosing namespaces and modules.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class FSharpTypeRefs {
    public static final String UNIT = "Microsoft.FSharp.Core.Unit";
    public static final String FUNCTION = "Microsoft.FSharp.Core.FSharpFunc`2";

    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
        Map.entry("int", "System.Int32"),
        Map.entry("int32", "System.Int32"),
        Map.entry("uint", "System.UInt32"),
        Map.entry("uint32", "System.UInt32"),
        Map.entry("int8", "System.SByte"),
        Map.entry("sbyte", "System.SByte"),
        Map.entry("uint8", "System.Byte"),
        Map.entry("byte", "System.Byte"),
        Map.entry("int16", "System.Int16"),
        Map.entry("uint16", "System.UInt16"),
        Map.entry("int64", "System.Int64"),
        Map.entry("uint64", "System.UInt64"),
        Map.entry("nativeint", "System.IntPtr"),
        Map.entry("unativeint", "System.UIntPtr"),
        Map.entry("float", "System.Double"),
        Map.entry("double", "System.Double"),
        Map.entry("float32", "System.Single"),
        Map.entry("single", "System.Single"),
        Map.entry("decimal", "System.Decimal"),
        Map.entry("bool", "System.Boolean"),
        Map.entry("char", "System.Char"),
        Map.entry("string", "System.String"),
        Map.entry("obj", "System.Object"),
        Map.entry("exn", "System.Exception"),
        Map.entry("unit", UNIT),
        Map.entry("bigint", "System.Numerics.BigInteger"),
        Map.entry("list", "Microsoft.FSharp.Collections.FSharpList`1"),
        Map.entry("option", "Microsoft.FSharp.Core.FSharpOption`1"),
        Map.entry("voption", "Microsoft.FSharp.Core.FSharpValueOption`1"),
        Map.entry("seq", "System.Collections.Generic.IEnumerable`1"),
        Map.entry("ResizeArray", "System.Collections.Generic.List`1"),
        Map.entry("Map", "Microsoft.FSharp.Collections.FSharpMap`2"),
        Map.entry("Set", "Microsoft.FSharp.Collections.FSharpSet`1"),
        Map.entry("Async", "Microsoft.FSharp.Control.FSharpAsync`1"),
        Map.entry("Result", "Microsoft.FSharp.Core.FSharpResult`2"),
        Map.entry("ref", "Microsoft.FSharp.Core.FSharpRef`1"),
        Map.entry("lazy", "System.Lazy`1"),
        Map.entry("Lazy", "System.Lazy`1")
    );

    /**
     * {@code int}, {@code string}, {@code unit}, {@code list}...: lower case abbreviations of FSharp.Core and {@code array},
     * shown like keywords
     */
    public static boolean isPredefinedTypeName(String name) {
        return name.equals("array") || (ABBREVIATIONS.containsKey(name) && Character.isLowerCase(name.charAt(0)));
    }

    private static final List<String> AUTO_OPEN = List.of("Microsoft.FSharp.Core", "Microsoft.FSharp.Collections", "Microsoft.FSharp.Control");

    private static final TokenSet TYPE_USAGES = TokenSet.create(
        FSharpElementTypes.NAMED_TYPE_USAGE, FSharpElementTypes.TUPLE_TYPE_USAGE, FSharpElementTypes.ANON_RECORD_TYPE_USAGE,
        FSharpElementTypes.ARRAY_TYPE_USAGE, FSharpElementTypes.FUNCTION_TYPE_USAGE, FSharpElementTypes.ANON_TYPE_USAGE,
        FSharpElementTypes.PAREN_TYPE_USAGE, FSharpElementTypes.PARAMETER_SIGNATURE_TYPE_USAGE,
        FSharpElementTypes.EXPR_STATIC_CONSTANT_TYPE_USAGE, FSharpElementTypes.NAMED_STATIC_CONSTANT_TYPE_USAGE,
        FSharpElementTypes.CONSTRAINED_TYPE_USAGE, FSharpElementTypes.HASH_TYPE_USAGE, FSharpElementTypes.INTERSECTION_TYPE_USAGE,
        FSharpElementTypes.WITH_NULL_TYPE_USAGE
    );

    private FSharpTypeRefs() {
    }

    public static boolean isTypeUsage(@Nullable PsiElement element) {
        return element != null && TYPE_USAGES.contains(element.getNode().getElementType());
    }

    public static @Nullable PsiElement findTypeUsage(@Nullable PsiElement parent) {
        if (parent == null) {
            return null;
        }
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isTypeUsage(child)) {
                return child;
            }
        }
        return null;
    }

    public static List<PsiElement> findTypeUsages(PsiElement parent) {
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isTypeUsage(child)) {
                result.add(child);
            }
        }
        return result;
    }

    @RequiredReadAction
    public static DotNetTypeRef fromTypeUsage(@Nullable PsiElement usage) {
        if (usage == null) {
            return DotNetTypeRef.UNKNOWN_TYPE;
        }
        IElementType type = usage.getNode().getElementType();
        if (type == FSharpElementTypes.NAMED_TYPE_USAGE) {
            PsiElement name = FSharpPsiUtil.findChild(usage, FSharpElementTypes.TYPE_REFERENCE_NAME);
            return name == null ? DotNetTypeRef.UNKNOWN_TYPE : fromReferenceName(name);
        }
        if (type == FSharpElementTypes.PAREN_TYPE_USAGE || type == FSharpElementTypes.PARAMETER_SIGNATURE_TYPE_USAGE
            || type == FSharpElementTypes.WITH_NULL_TYPE_USAGE || type == FSharpElementTypes.CONSTRAINED_TYPE_USAGE) {
            return fromTypeUsage(findTypeUsage(usage));
        }
        if (type == FSharpElementTypes.FUNCTION_TYPE_USAGE) {
            return new FSharpTypeRefByQName(usage, FUNCTION);
        }
        if (type == FSharpElementTypes.TUPLE_TYPE_USAGE) {
            int count = findTypeUsages(usage).size();
            boolean struct = usage.getNode().findChildByType(FSharpTokenType.STRUCT) != null;
            return new FSharpTypeRefByQName(usage, (struct ? "System.ValueTuple`" : "System.Tuple`") + count);
        }
        if (type == FSharpElementTypes.ARRAY_TYPE_USAGE) {
            int dimensions = 1;
            for (ASTNode child = usage.getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
                if (child.getElementType() == FSharpTokenType.COMMA) {
                    dimensions++;
                }
            }
            return new FSharpArrayTypeRef(usage, fromTypeUsage(findTypeUsage(usage)), dimensions);
        }
        return DotNetTypeRef.UNKNOWN_TYPE;
    }

    /**
     * Type of a {@code TYPE_REFERENCE_NAME}: {@code A.B.C}, {@code List<int>}, {@code int list}.
     */
    @RequiredReadAction
    public static DotNetTypeRef fromReferenceName(PsiElement name) {
        if (name.getNode().findChildByType(FSharpElementTypes.TYPE_PARAMETER_ID) != null) {
            // generic parameter
            return DotNetTypeRef.UNKNOWN_TYPE;
        }
        List<String> segments = new ArrayList<>();
        collectSegments(name, segments);
        if (segments.isEmpty()) {
            return DotNetTypeRef.UNKNOWN_TYPE;
        }
        int typeArguments = countTypeArguments(name);
        String written = String.join(".", segments);
        if (segments.size() == 1) {
            String last = segments.get(0);
            if (last.equals("array")) {
                PsiElement arguments = FSharpPsiUtil.findChild(name, FSharpElementTypes.POSTFIX_APP_TYPE_ARGUMENT_LIST);
                if (arguments == null) {
                    arguments = FSharpPsiUtil.findChild(name, FSharpElementTypes.PREFIX_APP_TYPE_ARGUMENT_LIST);
                }
                return new FSharpArrayTypeRef(name, fromTypeUsage(findTypeUsage(arguments)), 1);
            }
            String abbreviation = ABBREVIATIONS.get(last);
            if (abbreviation != null) {
                return new FSharpTypeRefByQName(name, abbreviation);
            }
        }
        String vmName = typeArguments > 0 ? written + "`" + typeArguments : written;
        return new FSharpTypeRefByQName(name, vmName, candidates(name, vmName));
    }

    private static void collectSegments(PsiElement name, List<String> segments) {
        PsiElement qualifier = FSharpPsiUtil.findChild(name, FSharpElementTypes.TYPE_REFERENCE_NAME);
        if (qualifier != null) {
            collectSegments(qualifier, segments);
        }
        ASTNode identifier = null;
        for (ASTNode child = name.getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (child.getElementType() == FSharpTokenType.IDENTIFIER) {
                identifier = child;
            }
        }
        if (identifier != null) {
            segments.add(FSharpPsiUtil.unquoteName(identifier.getText()));
        }
    }

    private static int countTypeArguments(PsiElement name) {
        PsiElement prefix = FSharpPsiUtil.findChild(name, FSharpElementTypes.PREFIX_APP_TYPE_ARGUMENT_LIST);
        if (prefix != null) {
            return findTypeUsages(prefix).size();
        }
        PsiElement postfix = FSharpPsiUtil.findChild(name, FSharpElementTypes.POSTFIX_APP_TYPE_ARGUMENT_LIST);
        if (postfix != null) {
            return Math.max(1, findTypeUsages(postfix).size());
        }
        return 0;
    }

    /**
     * VM names a written name may mean, the most local first.
     */
    @RequiredReadAction
    public static List<String> candidates(PsiElement context, String written) {
        Set<String> result = new LinkedHashSet<>();
        for (PsiElement container = FSharpDeclarationUtil.getContainer(context); container != null;
             container = FSharpDeclarationUtil.getContainer(container)) {
            if (container instanceof FSharpTypeDeclarationImpl module) {
                addCandidates(result, module.getVmQName(), true, written);
            }
            else if (container instanceof FSharpNamespaceDeclarationImpl namespace) {
                String qName = namespace.getPresentableQName();
                // names are relative to the namespace and to its parents
                for (String prefix = qName; !prefix.isEmpty(); prefix = parentOf(prefix)) {
                    addCandidates(result, prefix, false, written);
                }
            }
        }
        PsiFile file = context.getContainingFile();
        if (file != null) {
            List<String> opens = new ArrayList<>();
            for (PsiElement open : PsiTreeUtil.findChildrenOfType(file, PsiElement.class)) {
                if (!FSharpPsiUtil.is(open, FSharpElementTypes.OPEN_STATEMENT)) {
                    continue;
                }
                PsiElement target = FSharpPsiUtil.findChild(open, FSharpElementTypes.TYPE_REFERENCE_NAME);
                if (target != null) {
                    opens.add(target.getText().replaceAll("\\s", ""));
                }
            }
            // a later open shadows an earlier one
            for (int i = opens.size() - 1; i >= 0; i--) {
                addCandidates(result, opens.get(i), false, written);
                addCandidates(result, opens.get(i), true, written);
            }
        }
        for (String open : AUTO_OPEN) {
            addCandidates(result, open, false, written);
        }
        result.add(written);
        int dot = written.lastIndexOf('.');
        if (dot > 0) {
            // A.M.T: T nested in module A.M
            result.add(written.substring(0, dot) + "/" + written.substring(dot + 1));
        }
        return List.copyOf(result);
    }

    /**
     * Namespaces a written name may mean: absolute, relative to enclosing namespaces and to opens.
     */
    @RequiredReadAction
    public static List<String> namespaceCandidates(PsiElement context, String written) {
        Set<String> result = new LinkedHashSet<>();
        result.add(written);
        for (PsiElement container = FSharpDeclarationUtil.getContainer(context); container != null;
             container = FSharpDeclarationUtil.getContainer(container)) {
            if (container instanceof FSharpNamespaceDeclarationImpl namespace) {
                for (String prefix = namespace.getPresentableQName(); !prefix.isEmpty(); prefix = parentOf(prefix)) {
                    result.add(prefix + "." + written);
                }
            }
        }
        return List.copyOf(result);
    }

    private static void addCandidates(Set<String> result, @Nullable String prefix, boolean module, String written) {
        if (prefix == null || prefix.isEmpty()) {
            return;
        }
        result.add(prefix + (module ? "/" : ".") + written);
        int dot = written.lastIndexOf('.');
        if (dot > 0) {
            result.add(prefix + (module ? "/" : ".") + written.substring(0, dot) + "/" + written.substring(dot + 1));
        }
    }

    private static String parentOf(String qName) {
        int dot = qName.lastIndexOf('.');
        return dot < 0 ? "" : qName.substring(0, dot);
    }
}
