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

package consulo.fsharp.impl.lang.lexer;

import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.lexer.FlexAdapter;
import consulo.language.lexer.RestartableLexer;
import consulo.language.lexer.TokenIterator;

/**
 * F# lexer without preprocessing: {@code #if} branches are lexed as regular code.
 * <p>
 * Ported from JetBrains resharper-fsharp (Apache 2.0).
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public class FSharpLexer extends FlexAdapter implements RestartableLexer {
    public FSharpLexer() {
        super(new _FSharpLexer());
    }

    @Override
    public int getStartState() {
        return _FSharpLexer.YYINITIAL;
    }

    @Override
    public boolean isRestartableState(int state) {
        return state == _FSharpLexer.YYINITIAL || state == _FSharpLexer.LINE;
    }

    @Override
    public int getState() {
        int state = super.getState();
        _FSharpLexer flex = (_FSharpLexer) getFlex();
        if (!flex.isRestartableState() || FSharpTokenType.INTERPOLATED_STRING_ENDS.contains(getTokenType())) {
            return -1;
        }
        return state;
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        _FSharpLexer flex = (_FSharpLexer) getFlex();
        flex.myInterpolatedStringStates.clear();
        flex.myNestedCommentLevel = 0;
        flex.myParenLevel = 0;
        flex.myTokenLength = 0;
        flex.myBrackLevel = 0;
        super.start(buffer, startOffset, endOffset, initialState);
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState, TokenIterator tokenIterator) {
        start(buffer, startOffset, endOffset, initialState);
    }
}
