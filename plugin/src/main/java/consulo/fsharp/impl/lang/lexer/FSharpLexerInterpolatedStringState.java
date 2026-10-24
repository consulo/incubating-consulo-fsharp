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

import org.jspecify.annotations.Nullable;

import java.util.Deque;

/**
 * Ported from JetBrains resharper-fsharp (Apache 2.0).
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public final class FSharpLexerInterpolatedStringState {
    private final FSharpInterpolatedStringKind myKind;
    private final @Nullable Integer myDelimiterLength;
    private final FSharpLexerContext myPreviousLexerContext;
    private final Deque<InterpolatedStringStackItem> myInterpolatedStringStack;

    public FSharpLexerInterpolatedStringState(FSharpInterpolatedStringKind kind,
                                              @Nullable Integer delimiterLength,
                                              FSharpLexerContext previousLexerContext,
                                              Deque<InterpolatedStringStackItem> interpolatedStringStack) {
        myKind = kind;
        myDelimiterLength = delimiterLength;
        myPreviousLexerContext = previousLexerContext;
        myInterpolatedStringStack = interpolatedStringStack;
    }

    public FSharpInterpolatedStringKind getKind() {
        return myKind;
    }

    public @Nullable Integer getDelimiterLength() {
        return myDelimiterLength;
    }

    public FSharpLexerContext getPreviousLexerContext() {
        return myPreviousLexerContext;
    }

    public Deque<InterpolatedStringStackItem> getInterpolatedStringStack() {
        return myInterpolatedStringStack;
    }
}
