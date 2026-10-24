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
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.resolve.DotNetNamespaceAsElement;
import consulo.dotnet.psi.resolve.DotNetPsiSearcher;
import consulo.dotnet.psi.resolve.DotNetTypeRef;
import consulo.fsharp.impl.lang.psi.resolve.FSharpTypeRefs;
import consulo.language.file.light.LightVirtualFile;
import consulo.project.DumbService;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code EXPRESSION_REFERENCE_NAME} and {@code TYPE_REFERENCE_NAME}: {@code A.B.C} as nested names.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpReferenceNameImpl extends FSharpElementImpl {
    public FSharpReferenceNameImpl(ASTNode node) {
        super(node);
    }

    public @Nullable FSharpReferenceNameImpl getQualifier() {
        PsiElement first = getFirstChild();
        return first instanceof FSharpReferenceNameImpl qualifier ? qualifier : null;
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
        // declared names are not references
        if (getParent() instanceof FSharpReferencePatImpl pattern && !pattern.isConstructorReference()) {
            return null;
        }
        return new FSharpReference(this, identifier, this::resolve, this::getVariants);
    }

    private FSharpResolver.Kind getKind() {
        PsiElement parent = getParent();
        IElementType parentType = parent == null ? null : parent.getNode().getElementType();
        if (getNode().getElementType() == FSharpElementTypes.TYPE_REFERENCE_NAME || parentType == getNode().getElementType()) {
            return FSharpResolver.Kind.TYPE;
        }
        if (parentType == FSharpElementTypes.RECORD_FIELD_BINDING || parentType == FSharpElementTypes.FIELD_PAT) {
            return FSharpResolver.Kind.FIELD;
        }
        if (parentType == FSharpElementTypes.PARAMETERS_OWNER_PAT || parentType == FSharpElementTypes.LOCAL_REFERENCE_PAT
            || parentType == FSharpElementTypes.TOP_REFERENCE_PAT) {
            return FSharpResolver.Kind.CONSTRUCTOR;
        }
        return FSharpResolver.Kind.VALUE;
    }

    @RequiredReadAction
    public @Nullable PsiElement resolve() {
        String name = getReferenceName();
        if (name == null) {
            return null;
        }
        FSharpResolver.Kind kind = getKind();
        FSharpReferenceNameImpl qualifier = getQualifier();
        if (qualifier != null) {
            for (PsiElement container : qualifier.resolveContainers()) {
                PsiElement element = container instanceof DotNetNamespaceAsElement || isForeignType(container)
                    ? resolveInDotNetContainer(container, name)
                    : FSharpResolver.resolveInContainer(container, name, kind);
                if (element != null) {
                    return element;
                }
            }
            return null;
        }
        PsiNamedElement element = FSharpResolver.resolveInScope(this, name, kind);
        if (element != null) {
            return element;
        }
        if (kind == FSharpResolver.Kind.TYPE) {
            List<PsiElement> containers = FSharpResolver.findContainers(this, name);
            return containers.isEmpty() ? resolveDotNet() : containers.get(0);
        }
        return null;
    }

    private static boolean isForeignType(PsiElement element) {
        return element instanceof DotNetTypeDeclaration && !(element instanceof FSharpTypeDeclarationImpl);
    }

    /**
     * .NET types and namespaces of other languages and libraries: {@code open System}, {@code inherit ShapeBase}.
     */
    @RequiredReadAction
    private @Nullable PsiElement resolveDotNet() {
        if (DumbService.isDumb(getProject())) {
            return null;
        }
        consulo.language.psi.PsiFile file = getContainingFile();
        if (file == null || file.getVirtualFile() == null || file.getVirtualFile() instanceof LightVirtualFile) {
            // no indexes for light files
            return null;
        }
        String text = getText().replaceAll("\\s", "");
        boolean namespacePosition = FSharpPsiUtil.is(getParent(), FSharpElementTypes.OPEN_STATEMENT)
            || FSharpPsiUtil.is(getParent(), FSharpElementTypes.TYPE_REFERENCE_NAME);
        if (!namespacePosition) {
            DotNetTypeRef typeRef = FSharpTypeRefs.fromReferenceName(this);
            if (typeRef.resolve().getElement() instanceof DotNetTypeDeclaration type) {
                return type;
            }
        }
        DotNetPsiSearcher searcher = DotNetPsiSearcher.getInstance(getProject());
        for (String candidate : FSharpTypeRefs.namespaceCandidates(this, text)) {
            DotNetNamespaceAsElement namespace = searcher.findNamespace(candidate, getResolveScope());
            if (namespace != null) {
                return namespace;
            }
        }
        if (namespacePosition) {
            DotNetTypeRef typeRef = FSharpTypeRefs.fromReferenceName(this);
            if (typeRef.resolve().getElement() instanceof DotNetTypeDeclaration type) {
                return type;
            }
        }
        return null;
    }

    @RequiredReadAction
    private @Nullable PsiElement resolveInDotNetContainer(PsiElement container, String name) {
        if (container instanceof DotNetNamespaceAsElement namespace) {
            DotNetPsiSearcher searcher = DotNetPsiSearcher.getInstance(getProject());
            String qName = namespace.getPresentableQName();
            String child = qName == null || qName.isEmpty() ? name : qName + "." + name;
            DotNetTypeDeclaration type = searcher.findType(child, getResolveScope());
            if (type != null) {
                return type;
            }
            return searcher.findNamespace(child, getResolveScope());
        }
        if (container instanceof DotNetTypeDeclaration type) {
            for (DotNetNamedElement member : type.getMembers()) {
                if (member instanceof DotNetTypeDeclaration nested && name.equals(nested.getName())) {
                    return nested;
                }
            }
        }
        return null;
    }

    /**
     * Modules, namespace fragments or types named by this name.
     */
    @RequiredReadAction
    public List<PsiElement> resolveContainers() {
        List<PsiElement> result = new ArrayList<>(FSharpResolver.findContainers(this, getText().replaceAll("\\s", "")));
        if (result.isEmpty()) {
            PsiElement element = resolve();
            if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    @RequiredReadAction
    private Object[] getVariants() {
        FSharpReferenceNameImpl qualifier = getQualifier();
        List<PsiNamedElement> variants = new ArrayList<>();
        if (qualifier != null) {
            for (PsiElement container : qualifier.resolveContainers()) {
                variants.addAll(FSharpResolver.collectContainerVariants(container, getKind()));
            }
        }
        else {
            variants.addAll(FSharpResolver.collectVariants(this, getKind()));
        }
        return variants.toArray();
    }
}
