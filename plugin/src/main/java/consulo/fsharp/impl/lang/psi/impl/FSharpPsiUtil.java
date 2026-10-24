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
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
public final class FSharpPsiUtil {
    private FSharpPsiUtil() {
    }

    /**
     * {@code ``my name``} -> {@code my name}
     */
    public static String unquoteName(String text) {
        if (text.length() >= 4 && text.startsWith("``") && text.endsWith("``")) {
            return text.substring(2, text.length() - 2);
        }
        return text;
    }

    public static PsiElement createIdentifier(Project project, String name) {
        String text = isPlainIdentifier(name) ? name : "``" + name + "``";
        PsiFile file = PsiFileFactory.getInstance(project).createFileFromText("dummy.fs", FSharpLanguage.INSTANCE, "let " + text + " = ()");
        for (PsiElement element = file.getFirstChild(); element != null; element = PsiTreeUtil.nextLeaf(element)) {
            if (element.getNode().getElementType() == FSharpTokenType.IDENTIFIER) {
                return element;
            }
        }
        throw new IncorrectOperationException("Can not create identifier '" + name + "'");
    }

    private static boolean isPlainIdentifier(String name) {
        if (name.isEmpty() || !(Character.isLetter(name.charAt(0)) || name.charAt(0) == '_')) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '\'') {
                return false;
            }
        }
        return true;
    }

    public static boolean is(@Nullable PsiElement element, IElementType type) {
        return element != null && element.getNode().getElementType() == type;
    }

    public static List<PsiElement> children(PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        for (ASTNode node = element.getNode().getFirstChildNode(); node != null; node = node.getTreeNext()) {
            PsiElement psi = node.getPsi();
            if (psi != null) {
                children.add(psi);
            }
        }
        return children;
    }

    public static @Nullable PsiElement findChild(PsiElement element, IElementType type) {
        ASTNode node = element.getNode().findChildByType(type);
        return node == null ? null : node.getPsi();
    }

    /**
     * A file without a module or namespace declaration is a module named after the file: {@code utils.fs} is {@code Utils}.
     */
    public static @Nullable String getImplicitModuleName(@Nullable PsiFile file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        int dot = name.indexOf('.');
        String stem = dot < 0 ? name : name.substring(0, dot);
        if (stem.isEmpty()) {
            return null;
        }
        return Character.toUpperCase(stem.charAt(0)) + stem.substring(1);
    }

    private static boolean isScript(PsiElement element) {
        PsiFile file = element.getContainingFile();
        return file != null && file.getName().endsWith(".fsx");
    }

    /**
     * Qualified name of a namespace or module declaration: {@code A.B.C}.
     */
    @RequiredReadAction
    public static @Nullable String getQualifiedName(PsiElement declaration) {
        IElementType type = declaration.getNode().getElementType();
        if (type == FSharpElementTypes.ANON_MODULE_DECLARATION) {
            return getImplicitModuleName(declaration.getContainingFile());
        }
        if (type != FSharpElementTypes.NAMED_NAMESPACE_DECLARATION && type != FSharpElementTypes.NAMED_MODULE_DECLARATION
            && type != FSharpElementTypes.NESTED_MODULE_DECLARATION) {
            return null;
        }
        if (!(declaration instanceof consulo.language.psi.PsiNamedElement named) || named.getName() == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        if (type == FSharpElementTypes.NESTED_MODULE_DECLARATION) {
            PsiElement parent = declaration.getParent();
            String parentName = parent == null || is(parent, FSharpElementTypes.ANON_MODULE_DECLARATION) && !isScript(parent)
                ? null : getQualifiedName(parent);
            if (parentName != null) {
                builder.append(parentName).append('.');
            }
        }
        else {
            PsiElement qualifier = findChild(declaration, FSharpElementTypes.TYPE_REFERENCE_NAME);
            if (qualifier != null) {
                builder.append(qualifier.getText().replaceAll("\\s", "")).append('.');
            }
        }
        builder.append(named.getName());
        return builder.toString();
    }
}
