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

package consulo.fsharp.impl.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiComment;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class FSharpCommenter implements CodeDocumentationAwareCommenter {
    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

    @Override
    public String getBlockCommentPrefix() {
        return "(*";
    }

    @Override
    public String getBlockCommentSuffix() {
        return "*)";
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return null;
    }

    @Override
    public IElementType getLineCommentTokenType() {
        return FSharpTokenType.LINE_COMMENT;
    }

    @Override
    public IElementType getBlockCommentTokenType() {
        return FSharpTokenType.BLOCK_COMMENT;
    }

    @Override
    public IElementType getDocumentationCommentTokenType() {
        return FSharpTokenType.LINE_COMMENT;
    }

    @Override
    public @Nullable String getDocumentationCommentPrefix() {
        return null;
    }

    @Override
    public String getDocumentationCommentLinePrefix() {
        return "///";
    }

    @Override
    public @Nullable String getDocumentationCommentSuffix() {
        return null;
    }

    @Override
    public boolean isDocumentationComment(PsiComment comment) {
        return comment.getText().startsWith("///") && !comment.getText().startsWith("////");
    }

    @Override
    public Language getLanguage() {
        return FSharpLanguage.INSTANCE;
    }
}
