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
package consulo.fsharp.impl.lang.psi.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpStubElements;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.stub.StubElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Declaration structure of F# files: containers (namespaces and modules), declared types and their .NET names.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class FSharpDeclarationUtil {
    public static final TokenSet NAMESPACES = TokenSet.create(FSharpStubElements.NAMED_NAMESPACE_DECLARATION,
        FSharpStubElements.GLOBAL_NAMESPACE_DECLARATION);

    public static final TokenSet MODULES = TokenSet.create(FSharpStubElements.NAMED_MODULE_DECLARATION,
        FSharpStubElements.ANON_MODULE_DECLARATION,
        FSharpStubElements.NESTED_MODULE_DECLARATION);

    public static final TokenSet CONTAINERS = TokenSet.orSet(NAMESPACES, MODULES);

    /**
     * Declarations that are .NET types (or not, for abbreviations and measures).
     */
    public static final TokenSet TYPE_LIKE = TokenSet.orSet(MODULES, TokenSet.create(FSharpStubElements.FSHARP_TYPE_DECLARATION,
        FSharpStubElements.EXCEPTION_DECLARATION));

    private FSharpDeclarationUtil() {
    }

    public static boolean is(@Nullable PsiElement element, TokenSet set) {
        return element != null && set.contains(element.getNode().getElementType());
    }

    /**
     * Nearest enclosing namespace or module.
     */
    @RequiredReadAction
    public static @Nullable PsiElement getContainer(PsiElement declaration) {
        for (PsiElement parent = declaration.getParent(); parent != null && !(parent instanceof PsiFile); parent = parent.getParent()) {
            if (CONTAINERS.contains(parent.getNode().getElementType())) {
                return parent;
            }
        }
        return null;
    }

    /**
     * Types, modules and exceptions declared directly in a namespace or module.
     */
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static List<DotNetNamedElement> getDeclarations(PsiElement container) {
        List<DotNetNamedElement> result = new ArrayList<>();
        if (container instanceof StubBasedPsiElement<?> stubBased && stubBased.getStub() != null) {
            for (StubElement<?> child : ((StubElement<?>) stubBased.getStub()).getChildrenStubs()) {
                if (child.getPsi() instanceof DotNetNamedElement named && TYPE_LIKE.contains(child.getStubType())) {
                    result.add(named);
                }
            }
            return result;
        }
        collectDeclarations(container.getNode(), result);
        return result;
    }

    private static void collectDeclarations(ASTNode parent, List<DotNetNamedElement> result) {
        for (ASTNode child = parent.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (TYPE_LIKE.contains(type)) {
                if (child.getPsi() instanceof DotNetNamedElement named) {
                    result.add(named);
                }
            }
            else if (type == FSharpElementTypes.TYPE_DECLARATION_GROUP) {
                collectDeclarations(child, result);
            }
        }
    }

    /**
     * {@code [<Struct>]}, {@code [<StructAttribute>]}, {@code [<System.Struct>]}
     */
    @RequiredReadAction
    public static boolean hasAttribute(PsiElement declaration, String shortName) {
        for (ASTNode list = declaration.getNode().getFirstChildNode(); list != null; list = list.getTreeNext()) {
            if (list.getElementType() != FSharpElementTypes.ATTRIBUTE_LIST) {
                continue;
            }
            for (ASTNode attribute = list.getFirstChildNode(); attribute != null; attribute = attribute.getTreeNext()) {
                if (attribute.getElementType() != FSharpElementTypes.ATTRIBUTE) {
                    continue;
                }
                ASTNode name = attribute.findChildByType(FSharpElementTypes.TYPE_REFERENCE_NAME);
                if (name == null) {
                    continue;
                }
                String text = name.getText();
                int dot = text.lastIndexOf('.');
                String last = dot < 0 ? text : text.substring(dot + 1);
                if (last.equals(shortName) || last.equals(shortName + "Attribute")) {
                    return true;
                }
            }
        }
        return false;
    }

    @RequiredReadAction
    public static @Nullable String getAttributeArgumentText(PsiElement declaration, String shortName) {
        for (ASTNode list = declaration.getNode().getFirstChildNode(); list != null; list = list.getTreeNext()) {
            if (list.getElementType() != FSharpElementTypes.ATTRIBUTE_LIST) {
                continue;
            }
            for (ASTNode attribute = list.getFirstChildNode(); attribute != null; attribute = attribute.getTreeNext()) {
                if (attribute.getElementType() != FSharpElementTypes.ATTRIBUTE) {
                    continue;
                }
                ASTNode name = attribute.findChildByType(FSharpElementTypes.TYPE_REFERENCE_NAME);
                if (name != null && (name.getText().endsWith(shortName) || name.getText().endsWith(shortName + "Attribute"))) {
                    return attribute.getText();
                }
            }
        }
        return null;
    }
}
