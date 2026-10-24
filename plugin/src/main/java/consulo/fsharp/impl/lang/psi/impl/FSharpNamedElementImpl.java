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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.util.IncorrectOperationException;
import org.jspecify.annotations.Nullable;

/**
 * Declaration named by its direct {@code IDENTIFIER} child: types, modules, union cases, fields, members.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpNamedElementImpl extends FSharpElementImpl implements PsiNameIdentifierOwner {
    public FSharpNamedElementImpl(ASTNode node) {
        super(node);
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
}
