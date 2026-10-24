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
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetModifierList;
import consulo.dotnet.psi.DotNetNamedElement;
import consulo.dotnet.psi.DotNetNamespaceDeclaration;
import consulo.fsharp.impl.lang.psi.stub.FSharpNamespaceStub;
import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.IStubElementType;
import org.jspecify.annotations.Nullable;

/**
 * {@code namespace A.B} or {@code namespace global}.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpNamespaceDeclarationImpl extends FSharpStubElementImpl<FSharpNamespaceStub> implements DotNetNamespaceDeclaration {
    public FSharpNamespaceDeclarationImpl(ASTNode node) {
        super(node);
    }

    public FSharpNamespaceDeclarationImpl(FSharpNamespaceStub stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @RequiredReadAction
    @Override
    public String getPresentableQName() {
        FSharpNamespaceStub stub = getGreenStub();
        if (stub != null) {
            return stub.getQName();
        }
        String qName = FSharpPsiUtil.getQualifiedName(this);
        return qName == null ? "" : qName;
    }

    @RequiredReadAction
    @Override
    public @Nullable String getPresentableParentQName() {
        String qName = getPresentableQName();
        int dot = qName.lastIndexOf('.');
        return dot < 0 ? "" : qName.substring(0, dot);
    }

    @RequiredReadAction
    @Override
    public DotNetNamedElement[] getMembers() {
        return FSharpDeclarationUtil.getDeclarations(this).toArray(DotNetNamedElement[]::new);
    }

    @RequiredReadAction
    @Override
    public boolean hasModifier(DotNetModifier modifier) {
        return false;
    }

    @RequiredReadAction
    @Override
    public @Nullable DotNetModifierList getModifierList() {
        return null;
    }
}
