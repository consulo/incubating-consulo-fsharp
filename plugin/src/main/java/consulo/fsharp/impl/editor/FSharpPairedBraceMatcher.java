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
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class FSharpPairedBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = {
        new BracePair(FSharpTokenType.LPAREN, FSharpTokenType.RPAREN, false),
        new BracePair(FSharpTokenType.LBRACK, FSharpTokenType.RBRACK, false),
        new BracePair(FSharpTokenType.LBRACE, FSharpTokenType.RBRACE, true),
        new BracePair(FSharpTokenType.LBRACK_BAR, FSharpTokenType.BAR_RBRACK, false),
        new BracePair(FSharpTokenType.LBRACE_BAR, FSharpTokenType.BAR_RBRACE, false),
        new BracePair(FSharpTokenType.LBRACK_LESS, FSharpTokenType.GREATER_RBRACK, false),
        new BracePair(FSharpTokenType.LQUOTE_TYPED, FSharpTokenType.RQUOTE_TYPED, false),
        new BracePair(FSharpTokenType.LQUOTE_UNTYPED, FSharpTokenType.RQUOTE_UNTYPED, false),
        new BracePair(FSharpTokenType.BEGIN, FSharpTokenType.END, true)
    };

    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(IElementType type, @Nullable IElementType contextType) {
        return contextType == null || FSharpTokenType.WHITESPACES.contains(contextType) || FSharpTokenType.COMMENTS.contains(contextType)
            || FSharpTokenType.RIGHT_BRACES.contains(contextType) || contextType == FSharpTokenType.COMMA
            || contextType == FSharpTokenType.SEMICOLON;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }

    @Override
    public Language getLanguage() {
        return FSharpLanguage.INSTANCE;
    }
}
