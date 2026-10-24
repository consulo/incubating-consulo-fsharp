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

/**
 * Ported from JetBrains resharper-fsharp (Apache 2.0).
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public final class FSharpLexerContext {
    private final int myLexerState;
    private final int myParenLevel;
    private final int myBrackLevel;
    private final int myNestedCommentLevel;

    public FSharpLexerContext(int lexerState, int parenLevel, int brackLevel, int nestedCommentLevel) {
        myLexerState = lexerState;
        myParenLevel = parenLevel;
        myBrackLevel = brackLevel;
        myNestedCommentLevel = nestedCommentLevel;
    }

    public int getLexerState() {
        return myLexerState;
    }

    public int getParenLevel() {
        return myParenLevel;
    }

    public int getBrackLevel() {
        return myBrackLevel;
    }

    public int getNestedCommentLevel() {
        return myNestedCommentLevel;
    }
}
