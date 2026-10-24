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
import consulo.language.ast.TokenSet;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Evaluates {@code #if}/{@code #elif} conditions ({@code A && !(B || C)}) over lexer tokens.
 * <p>
 * Ported from JetBrains resharper-fsharp (Apache 2.0).
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public final class FSharpPreprocessor {
    /**
     * Read-only view of lexed tokens, positioned at the first token of the directive.
     */
    public interface TokenCursor {
        @Nullable IElementType getTokenType();

        CharSequence getTokenText();

        void advance();
    }

    private static final TokenSet SKIPPED_TOKENS = TokenSet.create(
        FSharpTokenType.WHITE_SPACE,
        FSharpTokenType.PP_IF_SECTION,
        FSharpTokenType.PP_ELIF_SECTION
    );

    private final TokenCursor myCursor;
    private final Set<String> myDefinedConstants;

    private FSharpPreprocessor(TokenCursor cursor, Set<String> definedConstants) {
        myCursor = cursor;
        myDefinedConstants = definedConstants;
    }

    public static boolean evaluate(TokenCursor cursor, Set<String> definedConstants) {
        FSharpPreprocessor preprocessor = new FSharpPreprocessor(cursor, definedConstants);
        return preprocessor.parseExpression().myValue;
    }

    /**
     * Parsed condition value. An error node evaluates to {@code false} and stops the
     * surrounding binary operator loop, but combined expressions are evaluated normally.
     */
    private static final class Expr {
        static final Expr ERROR = new Expr(false, true);

        final boolean myValue;
        final boolean myError;

        Expr(boolean value, boolean error) {
            myValue = value;
            myError = error;
        }

        static Expr of(boolean value) {
            return new Expr(value, false);
        }
    }

    private @Nullable IElementType tokenType() {
        while (true) {
            IElementType tokenType = myCursor.getTokenType();
            if (tokenType == null || !SKIPPED_TOKENS.contains(tokenType)) {
                return tokenType;
            }
            myCursor.advance();
        }
    }

    private Expr parseExpression() {
        Expr expr = parseNotExpression();
        Expr andExpr = parseAndExpression(expr);
        return parseOrExpression(andExpr);
    }

    private Expr parseOrExpression(Expr left) {
        Expr expr = left;
        while (!expr.myError && tokenType() == FSharpTokenType.PP_OR) {
            myCursor.advance();
            Expr notExpr = parseNotExpression();
            expr = parseAndExpression(Expr.of(expr.myValue || notExpr.myValue));
        }
        return expr;
    }

    private Expr parseAndExpression(Expr left) {
        Expr expr = left;
        while (!expr.myError && tokenType() == FSharpTokenType.PP_AND) {
            myCursor.advance();
            Expr notExpr = parseNotExpression();
            expr = Expr.of(expr.myValue && notExpr.myValue);
        }
        return expr;
    }

    private Expr parseNotExpression() {
        IElementType tokenType = tokenType();
        if (tokenType == null) {
            return Expr.ERROR;
        }

        if (tokenType == FSharpTokenType.PP_NOT) {
            myCursor.advance();
            return Expr.of(!parseNotExpression().myValue);
        }
        return parseAtomExpression();
    }

    private Expr parseAtomExpression() {
        IElementType tokenType = tokenType();
        if (tokenType == FSharpTokenType.PP_CONDITIONAL_SYMBOL) {
            boolean defined = myDefinedConstants.contains(myCursor.getTokenText().toString());
            myCursor.advance();
            return Expr.of(defined);
        }

        if (tokenType == FSharpTokenType.PP_LPAR) {
            myCursor.advance();
            Expr expr = parseExpression();
            if (tokenType() != FSharpTokenType.PP_RPAR) {
                return Expr.ERROR;
            }
            myCursor.advance();
            return expr;
        }

        myCursor.advance();
        return Expr.ERROR;
    }
}
