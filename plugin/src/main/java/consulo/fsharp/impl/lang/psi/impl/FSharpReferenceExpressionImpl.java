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
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code REFERENCE_EXPR}: {@code x}, {@code M.f}, {@code r.Field}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpReferenceExpressionImpl extends FSharpElementImpl {
    public FSharpReferenceExpressionImpl(ASTNode node) {
        super(node);
    }

    public @Nullable PsiElement getQualifier() {
        if (getNode().findChildByType(FSharpTokenType.DOT) == null) {
            return null;
        }
        PsiElement first = getFirstChild();
        return first != null && first.getNode().getFirstChildNode() != null ? first : null;
    }

    public @Nullable PsiElement getIdentifier() {
        ASTNode identifier = getNode().findChildByType(FSharpTokenType.IDENTIFIER);
        return identifier == null ? null : identifier.getPsi();
    }

    @RequiredReadAction
    public @Nullable String getReferenceName() {
        PsiElement identifier = getIdentifier();
        return identifier == null ? null : FSharpPsiUtil.unquoteName(identifier.getText());
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiReference getReference() {
        PsiElement identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        return new FSharpReference(this, identifier, this::resolve, this::getVariants);
    }

    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        String name = getReferenceName();
        if (name == null) {
            return null;
        }
        PsiElement qualifier = getQualifier();
        if (qualifier == null) {
            return FSharpResolver.resolveInScope(this, name, FSharpResolver.Kind.VALUE);
        }
        for (PsiElement container : resolveQualifierContainers(qualifier)) {
            PsiNamedElement element = FSharpResolver.resolveInContainer(container, name, FSharpResolver.Kind.VALUE);
            if (element == null) {
                element = FSharpResolver.resolveInContainer(container, name, FSharpResolver.Kind.FIELD);
            }
            if (element != null) {
                return element;
            }
        }
        return null;
    }

    /**
     * Modules, namespaces and types a qualifier stands for; for values, the type from their annotation.
     */
    @RequiredReadAction
    private static List<PsiElement> resolveQualifierContainers(PsiElement qualifier) {
        List<PsiElement> result = new ArrayList<>();
        if (!(qualifier instanceof FSharpReferenceExpressionImpl reference)) {
            return result;
        }
        String text = qualifier.getText().replaceAll("\\s", "");
        result.addAll(FSharpResolver.findContainers(qualifier, text));
        if (!result.isEmpty()) {
            return result;
        }
        PsiElement target = reference.resolve();
        if (target == null) {
            return result;
        }
        IElementType type = target.getNode().getElementType();
        if (type == FSharpElementTypes.NESTED_MODULE_DECLARATION || type == FSharpElementTypes.NAMED_MODULE_DECLARATION
            || type == FSharpElementTypes.NAMED_NAMESPACE_DECLARATION || type == FSharpElementTypes.FSHARP_TYPE_DECLARATION) {
            result.add(target);
            return result;
        }
        if (type == FSharpElementTypes.NAMED_SELF_ID || type == FSharpElementTypes.CTOR_SELF_ID) {
            PsiElement typeDeclaration = target.getParent();
            while (typeDeclaration != null && typeDeclaration.getNode().getElementType() != FSharpElementTypes.FSHARP_TYPE_DECLARATION) {
                typeDeclaration = typeDeclaration.getParent();
            }
            if (typeDeclaration != null) {
                result.add(typeDeclaration);
            }
            return result;
        }
        PsiElement annotated = FSharpTypeOfUtil.resolveAnnotatedType(target);
        if (annotated != null) {
            result.add(annotated);
        }
        return result;
    }

    @RequiredReadAction
    private Object[] getVariants() {
        PsiElement qualifier = getQualifier();
        List<PsiNamedElement> variants = new ArrayList<>();
        if (qualifier != null) {
            for (PsiElement container : resolveQualifierContainers(qualifier)) {
                variants.addAll(FSharpResolver.collectContainerVariants(container, FSharpResolver.Kind.VALUE));
                variants.addAll(FSharpResolver.collectContainerVariants(container, FSharpResolver.Kind.FIELD));
            }
        }
        else {
            variants.addAll(FSharpResolver.collectVariants(this, FSharpResolver.Kind.VALUE));
        }
        return variants.toArray();
    }
}
