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
import consulo.annotation.access.RequiredWriteAction;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubElement;
import consulo.language.util.IncorrectOperationException;
import org.jspecify.annotations.Nullable;

/**
 * Stub based declaration named by its direct {@code IDENTIFIER} child.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public abstract class FSharpStubElementImpl<S extends StubElement> extends StubBasedPsiElementBase<S>
    implements StubBasedPsiElement<S>, PsiNameIdentifierOwner {
    protected FSharpStubElementImpl(ASTNode node) {
        super(node);
    }

    protected FSharpStubElementImpl(S stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @RequiredReadAction
    @Override
    public @Nullable PsiElement getNameIdentifier() {
        ASTNode identifier = getNode().findChildByType(FSharpTokenType.IDENTIFIER);
        return identifier == null ? null : identifier.getPsi();
    }

    @RequiredReadAction
    @Override
    public @Nullable String getName() {
        PsiElement identifier = getNameIdentifier();
        return identifier == null ? null : FSharpPsiUtil.unquoteName(identifier.getText());
    }

    @RequiredWriteAction
    @Override
    public PsiElement setName(String name) throws IncorrectOperationException {
        PsiElement identifier = getNameIdentifier();
        if (identifier == null) {
            throw new IncorrectOperationException("No name identifier");
        }
        identifier.replace(FSharpPsiUtil.createIdentifier(getProject(), name));
        return this;
    }

    @RequiredReadAction
    @Override
    public int getTextOffset() {
        PsiElement identifier = getNameIdentifier();
        return identifier == null ? super.getTextOffset() : identifier.getTextOffset();
    }

    @Override
    public String toString() {
        return getElementType().toString();
    }
}
