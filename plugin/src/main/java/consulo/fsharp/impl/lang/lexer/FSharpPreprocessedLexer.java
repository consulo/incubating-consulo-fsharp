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
import consulo.language.ast.IElementType;
import consulo.language.lexer.LexerBase;
import consulo.language.lexer.RestartableLexer;
import consulo.language.lexer.TokenIterator;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * F# lexer with {@code #if}/{@code #elif}/{@code #else}/{@code #endif} support: every line inside an inactive
 * branch becomes a single {@link FSharpTokenType#DEAD_CODE} token followed by its {@link FSharpTokenType#NEW_LINE}.
 * <p>
 * The whole range is lexed on {@link #start}; restart is allowed only outside of {@code #if} sections.
 * <p>
 * Ported from JetBrains resharper-fsharp (Apache 2.0), {@code FSharpPreprocessedLexer}.
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public class FSharpPreprocessedLexer extends LexerBase implements RestartableLexer {
    private static final int NOT_RESTARTABLE_STATE = -1;

    private record Token(IElementType type, int start, int end, int state) {
    }

    private final Set<String> myDefinedConstants;

    private final List<Token> myBaseTokens = new ArrayList<>();
    private final List<Token> myTokens = new ArrayList<>();
    private int myIndex;

    private CharSequence myBuffer = "";
    private int myBufferEnd;

    public FSharpPreprocessedLexer(Set<String> definedConstants) {
        myDefinedConstants = definedConstants;
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myBuffer = buffer;
        myBufferEnd = endOffset;
        myIndex = 0;

        myBaseTokens.clear();
        FSharpLexer lexer = new FSharpLexer();
        lexer.start(buffer, startOffset, endOffset, initialState);
        IElementType tokenType;
        while ((tokenType = lexer.getTokenType()) != null) {
            myBaseTokens.add(new Token(tokenType, lexer.getTokenStart(), lexer.getTokenEnd(), lexer.getState()));
            lexer.advance();
        }

        myTokens.clear();
        new Preprocessor().run();
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState, TokenIterator tokenIterator) {
        start(buffer, startOffset, endOffset, initialState);
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
        return myIndex < myTokens.size() ? myTokens.get(myIndex).state() : _FSharpLexer.YYINITIAL;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return myIndex < myTokens.size() ? myTokens.get(myIndex).type() : null;
    }

    @Override
    public int getTokenStart() {
        return myIndex < myTokens.size() ? myTokens.get(myIndex).start() : myBufferEnd;
    }

    @Override
    public int getTokenEnd() {
        return myIndex < myTokens.size() ? myTokens.get(myIndex).end() : myBufferEnd;
    }

    @Override
    public void advance() {
        myIndex++;
    }

    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getBufferEnd() {
        return myBufferEnd;
    }

    private final class Preprocessor {
        private final BlockStack myStack = new BlockStack();
        private int myPosition;

        void run() {
            while (myPosition < myBaseTokens.size()) {
                if (myStack.condition()) {
                    preprocessActiveBranch();
                }
                else {
                    preprocessInactiveBranch();
                }
            }
        }

        private void preprocessActiveBranch() {
            Token token = myBaseTokens.get(myPosition);
            Boolean lineIsActive = tryApplyDirective(token.type());
            if (lineIsActive == null) {
                emit(token);
                myPosition++;
            }
            else if (lineIsActive) {
                preprocessLine();
            }
            else {
                preprocessInactiveLine();
            }
        }

        private void preprocessInactiveBranch() {
            int position = myPosition;
            while (position < myBaseTokens.size() && myBaseTokens.get(position).type() == FSharpTokenType.WHITE_SPACE) {
                position++;
            }

            IElementType tokenType = position < myBaseTokens.size() ? myBaseTokens.get(position).type() : null;
            Boolean lineIsActive = tokenType == null ? null : tryApplyDirective(tokenType);
            if (lineIsActive != null && lineIsActive) {
                preprocessLine();
            }
            else {
                preprocessInactiveLine();
            }
        }

        /**
         * @return {@code null} if the token is not a conditional directive, otherwise whether the directive line is active
         */
        private @Nullable Boolean tryApplyDirective(IElementType tokenType) {
            if (tokenType == FSharpTokenType.PP_IF_SECTION) {
                return myStack.startIfSection(this::evaluateBranchCondition);
            }
            if (tokenType == FSharpTokenType.PP_ELIF_SECTION) {
                return myStack.switchBranch(this::evaluateBranchCondition, false);
            }
            if (tokenType == FSharpTokenType.PP_ELSE_SECTION) {
                return myStack.switchBranch(() -> true, true);
            }
            if (tokenType == FSharpTokenType.PP_ENDIF) {
                return myStack.endIfSection();
            }
            return null;
        }

        private boolean evaluateBranchCondition() {
            int[] position = {myPosition};
            FSharpPreprocessor.TokenCursor cursor = new FSharpPreprocessor.TokenCursor() {
                @Override
                public @Nullable IElementType getTokenType() {
                    return position[0] < myBaseTokens.size() ? myBaseTokens.get(position[0]).type() : null;
                }

                @Override
                public CharSequence getTokenText() {
                    Token token = myBaseTokens.get(position[0]);
                    return myBuffer.subSequence(token.start(), token.end());
                }

                @Override
                public void advance() {
                    if (position[0] < myBaseTokens.size()) {
                        position[0]++;
                    }
                }
            };
            return FSharpPreprocessor.evaluate(cursor, myDefinedConstants);
        }

        private void preprocessLine() {
            while (myPosition < myBaseTokens.size()) {
                Token token = myBaseTokens.get(myPosition++);
                emit(token);
                if (token.type() == FSharpTokenType.NEW_LINE) {
                    return;
                }
            }
        }

        private void preprocessInactiveLine() {
            int deadStart = myBaseTokens.get(myPosition).start();
            while (myPosition < myBaseTokens.size()) {
                Token token = myBaseTokens.get(myPosition++);
                if (token.type() == FSharpTokenType.NEW_LINE) {
                    emitDeadCode(deadStart, token.start());
                    emit(token);
                    return;
                }
            }
            // last line without a line break: ReSharper drops it, but a lexer must cover the whole text
            emitDeadCode(deadStart, myBaseTokens.get(myBaseTokens.size() - 1).end());
        }

        private void emitDeadCode(int start, int end) {
            if (start < end) {
                myTokens.add(new Token(FSharpTokenType.DEAD_CODE, start, end, NOT_RESTARTABLE_STATE));
            }
        }

        private void emit(Token token) {
            int state = myStack.isEmpty() ? token.state() : NOT_RESTARTABLE_STATE;
            myTokens.add(new Token(token.type(), token.start(), token.end(), state));
        }
    }

    private interface Condition {
        boolean evaluate();
    }

    private static final class BlockStack {
        private static final class BlockState {
            private final boolean myOuterCondition;
            private boolean myCondition;
            private boolean myAnyBranchTaken;
            private boolean myAfterElse;

            BlockState(Condition condition, boolean outerCondition) {
                myOuterCondition = outerCondition;
                myCondition = outerCondition && condition.evaluate();
                myAnyBranchTaken = myCondition;
            }

            boolean switchBranch(Condition condition, boolean isElse) {
                if (myAfterElse) {
                    return false;
                }

                myCondition = myOuterCondition && !myAnyBranchTaken && condition.evaluate();
                myAnyBranchTaken |= myCondition;
                myAfterElse = isElse;
                return true;
            }
        }

        private final Deque<BlockState> myStack = new ArrayDeque<>();

        boolean isEmpty() {
            return myStack.isEmpty();
        }

        boolean condition() {
            BlockState state = myStack.peek();
            return state == null || state.myCondition;
        }

        boolean startIfSection(Condition condition) {
            boolean outerCondition = condition();
            myStack.push(new BlockState(condition, outerCondition));
            return outerCondition;
        }

        boolean endIfSection() {
            if (!myStack.isEmpty()) {
                myStack.pop();
            }
            return condition();
        }

        boolean switchBranch(Condition condition, boolean isElse) {
            BlockState state = myStack.peek();
            if (state == null) {
                return true;
            }

            if (!state.switchBranch(condition, isElse)) {
                myStack.clear();
                return true;
            }
            return state.myOuterCondition;
        }
    }
}
