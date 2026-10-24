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

package consulo.fsharp.impl.lang.parser;

import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

import static consulo.fsharp.impl.lang.parser.FSharpLexFilterKind.*;
import static consulo.fsharp.impl.lang.psi.FSharpElementTypes.*;

/**
 * Expressions and bindings ({@code typedSequentialExpr}, {@code declExpr}, {@code appExpr}, {@code atomicExpr} of
 * {@code pars.fsy}).
 * <p>
 * Binary operator precedence, from lowest: {@code <-} and {@code :=}, {@code ,}, {@code ..}, {@code or ||},
 * {@code & &&}, {@code :> :?>}, comparison/pipe operators, {@code ^op}, {@code ::}, {@code :?}, {@code +op -op},
 * {@code *op /op %op}, {@code **op}; then prefix operators, application and atomic expressions.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public abstract class FSharpExpressionParsing extends FSharpPatternParsing {
    private static final int PREC_ASSIGN = 1;
    private static final int PREC_TUPLE = 2;
    private static final int PREC_RANGE = 3;
    private static final int PREC_OR = 4;
    private static final int PREC_AND = 5;
    private static final int PREC_CAST = 6;
    private static final int PREC_COMPARE = 7;
    private static final int PREC_HAT = 8;
    private static final int PREC_CONS = 9;
    private static final int PREC_TYPE_TEST = 10;
    private static final int PREC_ADD = 11;
    private static final int PREC_MUL = 12;
    private static final int PREC_POW = 13;

    private static final Set<FSharpLexFilterKind> KEYWORD_EXPRESSION_STARTS = EnumSet.of(
        FUN, OFUN, FUNCTION, OFUNCTION, MATCH, MATCH_BANG, TRY, IF, WHILE, WHILE_BANG, FOR, LET, OLET, BINDER, OBINDER, DO, ODO, DO_BANG,
        ODO_BANG, ASSERT, OASSERT, LAZY, OLAZY, YIELD, YIELD_BANG, FIXED
    );

    private static final Set<FSharpLexFilterKind> PREFIX_OPERATORS = EnumSet.of(
        MINUS, PLUS_MINUS_OP, ADJACENT_PREFIX_OP, PERCENT_OP, AMP, AMP_AMP, INFIX_AT_HAT_OP
    );

    private static final Set<FSharpLexFilterKind> INTERP_STRING_STARTS = EnumSet.of(INTERP_STRING_BEGIN_END, INTERP_STRING_BEGIN_PART);

    protected FSharpExpressionParsing(FSharpBuilder builder) {
        super(builder);
    }

    // ------------------------------------------------------------------------
    // blocks

    /**
     * Skips everything up to the token at {@code end} (an error) and consumes it.
     */
    protected void skipTo(int end) {
        if (end < 0 || myBuilder.position() > end) {
            return;
        }
        if (myBuilder.position() < end) {
            PsiBuilder.Marker error = mark();
            while (myBuilder.position() < end && !myBuilder.eof()) {
                advance();
            }
            error.error("Unexpected tokens");
        }
        advance();
    }

    /**
     * @return position of the token matching the current one, or -1
     */
    protected int matchEnd() {
        int distance = myBuilder.matchDistance();
        return distance < 0 ? -1 : myBuilder.position() + distance;
    }

    /**
     * {@code typedSequentialExprBlock}
     */
    protected PsiBuilder.@Nullable Marker parseTypedSequentialExprBlock() {
        if (is(OBLOCKBEGIN)) {
            int end = matchEnd();
            advance();
            PsiBuilder.Marker expr = parseTypedSequentialExpr();
            if (expr == null) {
                error("Expression expected");
            }
            if (end >= 0) {
                skipTo(end);
            }
            else {
                skipToBlockEnd(OBLOCKBEGIN, OBLOCKEND);
            }
            return expr;
        }
        return parseTypedSequentialExpr();
    }

    /**
     * {@code declExprBlock}
     */
    protected PsiBuilder.@Nullable Marker parseDeclExprBlock() {
        if (is(OBLOCKBEGIN)) {
            return parseTypedSequentialExprBlock();
        }
        return parseDeclExpr();
    }

    /**
     * {@code typedSequentialExprBlockR}: the body after {@code ->} or {@code try}, closed by {@code ORIGHT_BLOCK_END}.
     *
     * @param end position of the closing {@code ORIGHT_BLOCK_END} or -1
     */
    protected PsiBuilder.@Nullable Marker parseTypedSequentialExprBlockR(int end) {
        PsiBuilder.Marker expr = parseTypedSequentialExpr();
        if (is(BAR) || is(BAR_JUST_BEFORE_NULL)) {
            // next match clause on the same line: the block ends after the last clause
            return expr;
        }
        if (end >= 0) {
            skipTo(end);
        }
        else {
            consume(ORIGHT_BLOCK_END);
        }
        return expr;
    }

    /**
     * {@code typedSequentialExpr}
     */
    protected PsiBuilder.@Nullable Marker parseTypedSequentialExpr() {
        PsiBuilder.Marker expr = parseSequentialExpr();
        if (expr != null && is(COLON)) {
            PsiBuilder.Marker typed = expr.precede();
            advance();
            if (parseTypeUsage() == null) {
                error("Type expected");
            }
            typed.done(TYPED_EXPR);
            return typed;
        }
        return expr;
    }

    /**
     * {@code sequentialExpr}: expressions separated by {@code ;} or by virtual block separators.
     */
    protected PsiBuilder.@Nullable Marker parseSequentialExpr() {
        PsiBuilder.Marker first = parseDeclExpr();
        if (first == null) {
            return null;
        }
        PsiBuilder.Marker sequence = null;
        if (is(OTHEN) || is(THEN)) {
            // new(...) = expr then expr
            sequence = first.precede();
            advance();
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
            sequence.done(SEQUENTIAL_EXPR);
            return sequence;
        }
        while (is(SEMICOLON) || is(OBLOCKSEP)) {
            if (sequence == null) {
                sequence = first.precede();
            }
            consumeSeps();
            if (!canStartExpression()) {
                break;
            }
            if (parseDeclExpr() == null) {
                break;
            }
        }
        if (sequence != null) {
            sequence.done(SEQUENTIAL_EXPR);
            return sequence;
        }
        return first;
    }

    // ------------------------------------------------------------------------
    // bindings

    /**
     * {@code let}/{@code use}/{@code let!} bindings with the body: {@code LET_OR_USE_EXPR}.
     */
    private PsiBuilder.Marker parseLetExpression() {
        PsiBuilder.Marker letExpr = mark();
        boolean light = is(OLET) || is(OBINDER);
        parseLetBindings(LOCAL_BINDING, true);
        if (!light) {
            expect(IN, "'in' expected");
        }
        consume(OBLOCKSEP);
        if (canStartExpression()) {
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
        }
        else {
            // a 'let' must be followed by an expression
            PsiBuilder.Marker missing = mark();
            missing.done(FROM_ERROR_EXPR);
        }
        letExpr.done(LET_OR_USE_EXPR);
        return letExpr;
    }

    /**
     * {@code hardwhiteLetBindings} / {@code defnBindings}: the first binding with the keyword, then {@code and} bindings.
     * The closing {@code ODECLEND} is consumed.
     */
    protected void parseLetBindings(IElementType bindingType, boolean local) {
        PsiBuilder.Marker probe = mark();
        parseAttributeLists();
        consume(STATIC);
        int end = matchEnd();
        boolean bang = is(OBINDER) || is(BINDER);
        probe.rollbackTo();
        parseBinding(bindingType, local);
        if (bang) {
            // each 'and!' has its own context
            if (end >= 0) {
                skipTo(end);
            }
            while (is(OAND_BANG) || is(AND_BANG) || is(OBLOCKSEP) && (isAt(1, OAND_BANG) || isAt(1, AND_BANG))) {
                consume(OBLOCKSEP);
                int andEnd = matchEnd();
                parseBinding(bindingType, local);
                if (andEnd >= 0) {
                    skipTo(andEnd);
                }
            }
            return;
        }
        while (is(AND) || is(LBRACK_LESS) && isAttributedAnd()) {
            parseBinding(bindingType, local);
        }
        if (end >= 0) {
            skipTo(end);
        }
    }

    private boolean isAttributedAnd() {
        PsiBuilder.Marker probe = mark();
        parseAttributeLists();
        boolean and = is(AND);
        probe.rollbackTo();
        return and;
    }

    /**
     * {@code [attributes] let [rec] [inline] [mutable] [access] pat params [: type] = expr}
     */
    protected void parseBinding(IElementType bindingType, boolean local) {
        PsiBuilder.Marker binding = mark();
        parseAttributeLists();
        consume(STATIC);
        if (is(LET) || is(OLET) || is(AND) || is(BINDER) || is(OBINDER) || is(AND_BANG) || is(OAND_BANG)) {
            advance();
        }
        while (is(INLINE) || is(REC)) {
            advance();
        }
        parseAttributeLists();
        consume(MUTABLE);
        parseBindingHead(local);
        if (is(COLON)) {
            parseReturnTypeInfo();
        }
        if (expect(EQUALS, "'=' expected")) {
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
        }
        binding.done(bindingType);
    }

    private void parseBindingHead(boolean local) {
        if (isFunctionHead()) {
            PsiBuilder.Marker probe = mark();
            parseFunctionHead(local);
            if (!is(AS) && !is(COMMA) && !is(BAR) && !is(COLON_COLON)) {
                probe.drop();
                return;
            }
            // 'Case(args) as x, y': a pattern
            probe.rollbackTo();
        }
        if (parseBindingHeadPattern(local) == null) {
            error("Pattern expected");
        }
    }

    private void parseFunctionHead(boolean local) {
        {
            PsiBuilder.Marker reference = mark();
            consumeAccessModifier();
            if (is(LPAREN) && isAt(1, BAR)) {
                PsiBuilder.Marker name = mark();
                parseActivePatternId(true);
                name.done(EXPRESSION_REFERENCE_NAME);
            }
            else if (is(LPAREN_STAR_RPAREN)) {
                PsiBuilder.Marker name = mark();
                advance();
                name.done(EXPRESSION_REFERENCE_NAME);
            }
            else {
                parseExpressionReferenceName();
            }
            reference.done(local ? LOCAL_REFERENCE_PAT : TOP_REFERENCE_PAT);
            if (isPostfixTypeParameters()) {
                parsePostfixTypeParameterDeclarationList(TYPE_PARAMETER_OF_METHOD_DECLARATION);
            }
            parseParameterPatterns();
        }
    }

    protected void parseParameterPatterns() {
        while (canStartAtomicPattern() && !is(EQUALS)) {
            PsiBuilder.Marker parameter = mark();
            consumeHighPrecedenceApp();
            if (parseAtomicPattern(true) == null) {
                parameter.drop();
                break;
            }
            parameter.done(PARAMETERS_PATTERN_DECLARATION);
        }
    }

    /**
     * {@code f x y}, {@code (+) a b}, {@code (|A|B|) x}: a named head with parameters or type parameters.
     */
    private boolean isFunctionHead() {
        int steps = is(ACCESS) ? 1 : 0;
        FSharpLexFilterKind type = kind(steps);
        int next;
        if (type == IDENT || type == LPAREN_STAR_RPAREN) {
            next = steps + 1;
        }
        else if (type == LPAREN) {
            if (kind(steps + 1) == BAR) {
                return true;
            }
            if (OPERATOR_NAMES.contains(kind(steps + 1)) && kind(steps + 2) == RPAREN) {
                next = steps + 3;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
        FSharpLexFilterKind after = kind(next);
        if (after == HIGH_PRECEDENCE_PAREN_APP || after == HIGH_PRECEDENCE_BRACK_APP) {
            after = kind(next + 1);
        }
        if (after == HIGH_PRECEDENCE_TYAPP) {
            return true;
        }
        return after == IDENT || after == UNDERSCORE || after == LPAREN || after == LBRACK || after == LBRACK_BAR || after == LBRACE
            || after == NULL || after == QMARK || after == LESS || after == LBRACK_LESS || LITERALS.contains(after) || after == GLOBAL
            || after == LPAREN_STAR_RPAREN;
    }

    protected void parseReturnTypeInfo() {
        parseReturnTypeInfo(false);
    }

    /**
     * @param signature abstract members and signatures wrap type components into parameter signatures
     */
    protected void parseReturnTypeInfo(boolean signature) {
        PsiBuilder.Marker info = mark();
        advance();
        if (!signature) {
            // signature types keep attributes in the parameter type
            parseAttributeLists();
        }
        if ((signature ? parseSignatureTypeUsage() : parseTypeUsage()) == null) {
            error("Type expected");
        }
        info.done(RETURN_TYPE_INFO);
    }

    // ------------------------------------------------------------------------
    // declExpr

    protected boolean canStartExpression() {
        FSharpLexFilterKind kind = kind();
        return canStartAtomicExpression() || KEYWORD_EXPRESSION_STARTS.contains(kind) || PREFIX_OPERATORS.contains(kind) || kind == NEW
            || kind == UPCAST || kind == DOWNCAST || kind == DOT_DOT || kind == DOT_DOT_HAT || kind == STAR;
    }

    private boolean canStartAtomicExpression() {
        FSharpLexFilterKind kind = kind();
        return switch (kind) {
            case IDENT, GLOBAL, BASE, NULL, LPAREN, LBRACK, LBRACK_BAR, LBRACE, LBRACE_BAR, BEGIN, LQUOTE, UNDERSCORE, LPAREN_STAR_RPAREN,
                 QUOTE, PREFIX_OP, INTERP_STRING_BEGIN_END, INTERP_STRING_BEGIN_PART -> true;
            case QMARK -> isAt(1, IDENT);
            case STRUCT -> isAt(1, LPAREN) || isAt(1, LBRACE_BAR);
            default -> LITERALS.contains(kind);
        };
    }

    /**
     * {@code declExpr}: expression without sequencing.
     */
    protected PsiBuilder.@Nullable Marker parseDeclExpr() {
        return parseBinary(PREC_ASSIGN);
    }

    private PsiBuilder.@Nullable Marker parseBinary(int minPrecedence) {
        PsiBuilder.Marker left = parseOperand();
        if (left == null) {
            return null;
        }
        return parseBinaryTail(left, minPrecedence);
    }

    /**
     * Operand of binary operators: a keyword expression (extends to the right as far as possible) or a prefix expression.
     */
    private PsiBuilder.@Nullable Marker parseOperand() {
        if (KEYWORD_EXPRESSION_STARTS.contains(kind())) {
            return parseKeywordExpression();
        }
        if (is(DOT_DOT) || is(DOT_DOT_HAT)) {
            return parseBeginningSlice();
        }
        if (is(STAR) && (isAt(1, RBRACK) || isAt(1, COMMA) || isAt(1, RPAREN))) {
            PsiBuilder.Marker whole = mark();
            advance();
            whole.done(WHOLE_RANGE_EXPR);
            return whole;
        }
        return parseMinusExpr();
    }

    private PsiBuilder.Marker parseBeginningSlice() {
        PsiBuilder.Marker slice = mark();
        boolean fromEnd = is(DOT_DOT_HAT);
        advance();
        PsiBuilder.Marker inner = fromEnd ? mark() : null;
        if (parseBinary(PREC_RANGE + 1) == null) {
            error("Expression expected");
        }
        if (inner != null) {
            inner.done(END_SLICE_EXPR);
        }
        slice.done(BEGINNING_SLICE_EXPR);
        return slice;
    }

    private PsiBuilder.Marker parseBinaryTail(PsiBuilder.Marker leftExpr, int minPrecedence) {
        PsiBuilder.Marker left = leftExpr;
        while (true) {
            int precedence = binaryPrecedence();
            if (precedence < 0 || precedence < minPrecedence) {
                return left;
            }

            if (precedence == PREC_TUPLE) {
                left = parseTupleTail(left);
                continue;
            }

            if (precedence == PREC_RANGE) {
                FSharpLexFilterKind afterRange = kind(1);
                if (afterRange == RBRACK || afterRange == RPAREN || afterRange == COMMA || afterRange == BAR_RBRACK) {
                    PsiBuilder.Marker end = left.precede();
                    advance();
                    end.done(END_SLICE_EXPR);
                    left = end;
                    continue;
                }
                PsiBuilder.Marker range = left.precede();
                advance();
                if (parseBinary(PREC_RANGE + 1) == null) {
                    error("Expression expected");
                }
                if (is(DOT_DOT)) {
                    advance();
                    if (parseBinary(PREC_RANGE + 1) == null) {
                        error("Expression expected");
                    }
                }
                range.done(RANGE_EXPR);
                left = range;
                continue;
            }

            if (precedence == PREC_CAST || precedence == PREC_TYPE_TEST) {
                FSharpLexFilterKind operator = kind();
                PsiBuilder.Marker cast = left.precede();
                advance();
                if (parseTypeUsage() == null) {
                    error("Type expected");
                }
                cast.done(operator == COLON_GREATER ? UPCAST_EXPR : operator == COLON_QMARK_GREATER ? DOWNCAST_EXPR : TYPE_TEST_EXPR);
                left = cast;
                continue;
            }

            if (is(LARROW)) {
                PsiBuilder.Marker set = left.precede();
                advance();
                if (parseDeclExprBlock() == null) {
                    error("Expression expected");
                }
                set.done(SET_EXPR);
                left = set;
                continue;
            }

            if (is(JOIN_IN)) {
                PsiBuilder.Marker join = left.precede();
                advance();
                if (parseBinary(PREC_OR + 1) == null) {
                    error("Expression expected");
                }
                join.done(JOIN_IN_EXPR);
                left = join;
                continue;
            }

            boolean rightAssoc = precedence == PREC_ASSIGN || precedence == PREC_HAT || precedence == PREC_CONS || precedence == PREC_POW;
            PsiBuilder.Marker binary = left.precede();
            PsiBuilder.Marker operator = mark();
            advance();
            operator.done(REFERENCE_EXPR);
            if (parseBinary(rightAssoc ? precedence : precedence + 1) == null) {
                error("Expression expected");
            }
            binary.done(BINARY_APP_EXPR);
            left = binary;
        }
    }

    private PsiBuilder.Marker parseTupleTail(PsiBuilder.Marker first) {
        PsiBuilder.Marker tuple = first.precede();
        while (consume(COMMA)) {
            if (parseBinary(PREC_TUPLE + 1) == null) {
                error("Expression expected");
                break;
            }
        }
        tuple.done(TUPLE_EXPR);
        return tuple;
    }

    private int binaryPrecedence() {
        return switch (kind()) {
            case LARROW, COLON_EQUALS -> PREC_ASSIGN;
            case COMMA -> PREC_TUPLE;
            case DOT_DOT -> PREC_RANGE;
            case OR, BAR_BAR, JOIN_IN -> PREC_OR;
            case AMP, AMP_AMP -> PREC_AND;
            case COLON_GREATER, COLON_QMARK_GREATER -> PREC_CAST;
            case INFIX_COMPARE_OP, DOLLAR, LESS, GREATER, EQUALS, INFIX_BAR_OP, INFIX_AMP_OP -> PREC_COMPARE;
            case INFIX_AT_HAT_OP -> PREC_HAT;
            case COLON_COLON -> PREC_CONS;
            case COLON_QMARK -> PREC_TYPE_TEST;
            case PLUS_MINUS_OP, MINUS -> PREC_ADD;
            case INFIX_STAR_DIV_MOD_OP, STAR, PERCENT_OP -> PREC_MUL;
            case INFIX_STAR_STAR_OP -> PREC_POW;
            default -> -1;
        };
    }

    // ------------------------------------------------------------------------
    // minusExpr, appExpr

    /**
     * {@code minusExpr}: prefix operators, {@code new}, {@code upcast}, {@code downcast}, application.
     */
    private PsiBuilder.@Nullable Marker parseMinusExpr() {
        FSharpLexFilterKind kind = kind();
        if (isAddressOf()) {
            PsiBuilder.Marker address = mark();
            advance();
            if (parseMinusExpr() == null) {
                error("Expression expected");
            }
            address.done(ADDRESS_OF_EXPR);
            return address;
        }
        if (kind == INFIX_AT_HAT_OP) {
            // ^1: index from the end
            PsiBuilder.Marker fromEnd = mark();
            advance();
            if (parseMinusExpr() == null) {
                error("Expression expected");
            }
            fromEnd.done(END_SLICE_EXPR);
            return fromEnd;
        }
        if (PREFIX_OPERATORS.contains(kind)) {
            return parsePrefixApp(this::parseMinusExpr);
        }
        if (kind == NEW) {
            PsiBuilder.Marker newExpr = mark();
            advance();
            if (isTypar()) {
                PsiBuilder.Marker name = mark();
                parseTypeParameterId();
                name.done(TYPE_REFERENCE_NAME);
            }
            else {
                parseTypeReferenceName(true);
            }
            consumeHighPrecedenceApp();
            if (canStartAtomicExprAfterType()) {
                parseAtomicExpression();
            }
            newExpr.done(NEW_EXPR);
            return parsePostfixTail(newExpr);
        }
        if (kind == UPCAST || kind == DOWNCAST) {
            PsiBuilder.Marker cast = mark();
            advance();
            if (parseMinusExpr() == null) {
                error("Expression expected");
            }
            cast.done(kind == UPCAST ? INFERRED_UPCAST_EXPR : INFERRED_DOWNCAST_EXPR);
            return cast;
        }
        return parseApplication();
    }

    private boolean isAddressOf() {
        return is(AMP) || is(AMP_AMP) || is(ADJACENT_PREFIX_OP) && ("&".equals(tokenText()) || "&&".equals(tokenText()));
    }

    private interface ExpressionParser {
        PsiBuilder.@Nullable Marker parse();
    }

    private PsiBuilder.Marker parsePrefixApp(ExpressionParser operand) {
        PsiBuilder.Marker app = mark();
        PsiBuilder.Marker operator = mark();
        advance();
        operator.done(REFERENCE_EXPR);
        if (operand.parse() == null) {
            error("Expression expected");
        }
        app.done(PREFIX_APP_EXPR);
        return app;
    }

    /**
     * {@code appExpr}: an atomic expression applied to arguments.
     */
    private PsiBuilder.@Nullable Marker parseApplication() {
        PsiBuilder.Marker function = parseAtomicExpressionWithPostfix();
        if (function == null) {
            return null;
        }
        while (canStartArgument()) {
            PsiBuilder.Marker app = function.precede();
            if (parseArgument() == null) {
                app.drop();
                break;
            }
            app.done(PREFIX_APP_EXPR);
            function = app;
        }
        return function;
    }

    private boolean canStartArgument() {
        return is(ADJACENT_PREFIX_OP) || canStartAtomicExpression() && (!is(UNDERSCORE) || isAt(1, DOT));
    }

    /**
     * {@code argExpr}
     */
    private PsiBuilder.@Nullable Marker parseArgument() {
        if (isAddressOf()) {
            return parseMinusExpr();
        }
        if (is(ADJACENT_PREFIX_OP)) {
            return parsePrefixApp(this::parseAtomicExpressionWithPostfix);
        }
        return parseAtomicExpressionWithPostfix();
    }

    private PsiBuilder.@Nullable Marker parseAtomicExpressionWithPostfix() {
        if (is(PREFIX_OP)) {
            return parsePrefixApp(this::parseAtomicExpressionWithPostfix);
        }
        PsiBuilder.Marker expr = parseAtomicExpression();
        if (expr == null) {
            return null;
        }
        return parsePostfixTail(expr);
    }

    /**
     * Postfix part of {@code atomicExpr}: {@code .Name}, {@code .[i]}, {@code f(x)}, {@code f[x]}, {@code x?name}.
     */
    private PsiBuilder.Marker parsePostfixTail(PsiBuilder.Marker atom) {
        PsiBuilder.Marker expr = atom;
        boolean qualified = false;
        while (true) {
            if (is(DOT)) {
                if (isAt(1, LBRACK)) {
                    PsiBuilder.Marker indexer = expr.precede();
                    advance();
                    parseIndexerArgList();
                    indexer.done(ITEM_INDEXER_EXPR);
                    expr = indexer;
                    continue;
                }
                PsiBuilder.Marker reference = expr.precede();
                advance();
                if (isIdentOrOp() || is(BASE) || is(GLOBAL)) {
                    parseIdentOrOp(false);
                    if (is(HIGH_PRECEDENCE_TYAPP)) {
                        parseTypeArgumentList();
                    }
                }
                else {
                    error("Identifier expected");
                }
                reference.done(REFERENCE_EXPR);
                expr = reference;
                qualified = true;
                continue;
            }
            if (is(HIGH_PRECEDENCE_BRACK_APP) && isAt(1, LBRACK)) {
                // f[1] is an application to a list expression
                PsiBuilder.Marker app = expr.precede();
                advance();
                parseAtomicExpression();
                app.done(PREFIX_APP_EXPR);
                expr = app;
                continue;
            }
            if (is(HIGH_PRECEDENCE_PAREN_APP) && isAt(1, LPAREN)) {
                // high precedence application: f(x)
                PsiBuilder.Marker app = expr.precede();
                advance();
                parseAtomicExpression();
                app.done(qualified && is(LARROW) ? NAMED_INDEXER_EXPR : PREFIX_APP_EXPR);
                expr = app;
                qualified = false;
                continue;
            }
            if (is(HIGH_PRECEDENCE_TYAPP) && isAt(1, LESS)) {
                // type application of a non-reference expression
                parseTypeArgumentList();
                continue;
            }
            if (is(QMARK) && (isAt(1, IDENT) || isAt(1, LPAREN))) {
                PsiBuilder.Marker dynamic = expr.precede();
                advance();
                parseAtomicExpression();
                dynamic.done(DYNAMIC_EXPR);
                expr = dynamic;
                continue;
            }
            return expr;
        }
    }

    private void parseIndexerArgList() {
        PsiBuilder.Marker list = mark();
        advance();
        if (parseTypedSequentialExpr() == null) {
            error("Expression expected");
        }
        expect(RBRACK, "']' expected");
        list.done(INDEXER_ARG_LIST);
    }

    // ------------------------------------------------------------------------
    // atomic expressions

    @Override
    protected boolean canStartAtomicExprAfterType() {
        FSharpLexFilterKind kind = kind();
        return switch (kind) {
            case LPAREN, LBRACE, LBRACE_BAR, NULL, LQUOTE, LBRACK_BAR, BEGIN, INTERP_STRING_BEGIN_END, INTERP_STRING_BEGIN_PART -> true;
            case STRUCT -> isAt(1, LBRACE_BAR);
            default -> LITERALS.contains(kind);
        };
    }

    @Override
    protected void parseAttributeArgument() {
        parseAtomicExpression();
    }

    @Override
    protected void parseStaticConstantExpression() {
        if (is(NULL)) {
            // null as a static constant is a literal
            PsiBuilder.Marker literal = mark();
            advance();
            literal.done(LITERAL_EXPR);
            return;
        }
        parseAtomicExpression();
    }

    protected PsiBuilder.@Nullable Marker parseAtomicExpression() {
        FSharpLexFilterKind kind = kind();

        if (LITERALS.contains(kind)) {
            PsiBuilder.Marker literal = mark();
            advance();
            if (is(HIGH_PRECEDENCE_TYAPP) && isAt(1, LESS)) {
                parseUnitOfMeasureClause();
            }
            literal.done(LITERAL_EXPR);
            return literal;
        }

        if (kind == NULL) {
            PsiBuilder.Marker nullExpr = mark();
            advance();
            nullExpr.done(NULL_EXPR);
            return nullExpr;
        }

        if (INTERP_STRING_STARTS.contains(kind)) {
            return parseInterpolatedString();
        }

        if (kind == UNDERSCORE && isAt(1, DOT)) {
            PsiBuilder.Marker dotLambda = mark();
            PsiBuilder.Marker id = mark();
            advance();
            id.done(DOT_LAMBDA_ID);
            advance();
            if (is(LBRACK)) {
                parseAtomicExpression();
                dotLambda.done(DOT_LAMBDA_EXPR);
                return dotLambda;
            }
            PsiBuilder.Marker body = mark();
            if (isIdentOrOp()) {
                parseIdentOrOp(false);
            }
            else {
                error("Identifier expected");
            }
            body.done(REFERENCE_EXPR);
            parsePostfixTail(body);
            dotLambda.done(DOT_LAMBDA_EXPR);
            return dotLambda;
        }

        if (kind == LPAREN && isTraitCall()) {
            return parseTraitCall();
        }

        if (isIdentOrOp() || kind == GLOBAL || kind == BASE || kind == UNDERSCORE) {
            PsiBuilder.Marker reference = mark();
            if (kind == GLOBAL || kind == BASE || kind == UNDERSCORE) {
                advance();
            }
            else {
                parseIdentOrOp(false);
            }
            if (is(HIGH_PRECEDENCE_TYAPP) && isAt(1, LESS)) {
                parseTypeArgumentList();
            }
            reference.done(REFERENCE_EXPR);
            return reference;
        }

        if (kind == QMARK && isAt(1, IDENT)) {
            // ?arg = value: named optional argument
            PsiBuilder.Marker reference = mark();
            advance();
            advance();
            reference.done(REFERENCE_EXPR);
            return reference;
        }

        if (kind == QUOTE) {
            PsiBuilder.Marker reference = mark();
            parseTypeParameterId();
            reference.done(REFERENCE_EXPR);
            return reference;
        }

        if (kind == LPAREN) {
            return parseParenExpression();
        }

        if (kind == STRUCT && isAt(1, LPAREN)) {
            PsiBuilder.Marker tuple = mark();
            advance();
            advance();
            if (parseBinary(PREC_TUPLE + 1) == null) {
                error("Expression expected");
            }
            while (consume(COMMA)) {
                parseBinary(PREC_TUPLE + 1);
            }
            expect(RPAREN, "')' expected");
            tuple.done(TUPLE_EXPR);
            return tuple;
        }

        if (kind == BEGIN) {
            PsiBuilder.Marker beginEnd = mark();
            advance();
            parseTypedSequentialExpr();
            expect(END, "'end' expected");
            beginEnd.done(BEGIN_END_EXPR);
            return beginEnd;
        }

        if (kind == LBRACK || kind == LBRACK_BAR) {
            boolean array = kind == LBRACK_BAR;
            FSharpLexFilterKind end = array ? BAR_RBRACK : RBRACK;
            PsiBuilder.Marker list = mark();
            advance();
            if (!is(end)) {
                parseSequentialExpr();
            }
            expect(end, array ? "'|]' expected" : "']' expected");
            list.done(array ? ARRAY_EXPR : LIST_EXPR);
            return list;
        }

        if (kind == LBRACE) {
            return parseBraceExpression();
        }

        if (kind == LBRACE_BAR || kind == STRUCT && isAt(1, LBRACE_BAR)) {
            PsiBuilder.Marker record = mark();
            consume(STRUCT);
            advance();
            if (!isFieldBindingStart() && !is(BAR_RBRACE) && !is(DOT_DOT_DOT)) {
                parseMinusExpr();
                if (!consume(WITH)) {
                    expect(OWITH, "'with' expected");
                }
            }
            parseRecordMemberBindingList(BAR_RBRACE);
            consume(OEND);
            expect(BAR_RBRACE, "'|}' expected");
            record.done(ANON_RECORD_EXPR);
            return record;
        }

        if (kind == LQUOTE) {
            PsiBuilder.Marker quote = mark();
            parseQuotationExpression();
            quote.done(QUOTE_EXPR);
            return quote;
        }

        return null;
    }

    @Override
    protected void parseQuotationExpression() {
        advance();
        parseTypedSequentialExpr();
        if (!consume(RQUOTE)) {
            error("Quotation end expected");
        }
    }

    /**
     * {@code (^T : (member P : int) x)}
     */
    private boolean isTraitCall() {
        return isTyparAt(1) && isAt(3, COLON) && isAt(4, LPAREN) || isAt(1, LPAREN) && isTyparAt(2) && isAt(4, OR);
    }

    private PsiBuilder.Marker parseTraitCall() {
        PsiBuilder.Marker call = mark();
        advance();
        if (consume(LPAREN)) {
            while (!myBuilder.eof() && !is(RPAREN)) {
                advance();
            }
            expect(RPAREN, "')' expected");
        }
        else {
            // ^T as plain tokens
            advance();
            advance();
        }
        expect(COLON, "':' expected");
        expect(LPAREN, "'(' expected");
        consume(STATIC);
        consume(MEMBER);
        if (isIdentOrOp()) {
            parseIdentOrOp(false);
        }
        else {
            error("Member name expected");
        }
        if (expect(COLON, "':' expected")) {
            parseTypeUsage();
        }
        expect(RPAREN, "')' expected");
        parseDeclExpr();
        expect(RPAREN, "')' expected");
        call.done(TRAIT_CALL_EXPR);
        return call;
    }

    private PsiBuilder.Marker parseParenExpression() {
        if (isAt(1, RPAREN)) {
            PsiBuilder.Marker unit = mark();
            advance();
            advance();
            unit.done(UNIT_EXPR);
            return unit;
        }
        PsiBuilder.Marker paren = mark();
        advance();
        if (is(HASH)) {
            // inline IL: (# "ldnull" : 'T #)
            while (!myBuilder.eof() && !is(RPAREN)) {
                advance();
            }
            expect(RPAREN, "')' expected");
            paren.done(LIBRARY_ONLY_EXPR);
            return paren;
        }
        if (parseTypedSequentialExpr() == null) {
            error("Expression expected");
        }
        expect(RPAREN, "')' expected");
        paren.done(PAREN_EXPR);
        return paren;
    }

    // ------------------------------------------------------------------------
    // units of measure

    private void parseUnitOfMeasureClause() {
        consume(HIGH_PRECEDENCE_TYAPP);
        PsiBuilder.Marker clause = mark();
        advance();
        if (parseMeasure() == null) {
            error("Unit of measure expected");
        }
        expect(GREATER, "'>' expected");
        clause.done(UNIT_OF_MEASURE_CLAUSE);
    }

    private boolean isDivide() {
        return isOp(INFIX_STAR_DIV_MOD_OP, "/");
    }

    /**
     * {@code kg * m / s^2}: product and division, then juxtaposition, then power.
     */
    private PsiBuilder.@Nullable Marker parseMeasure() {
        PsiBuilder.Marker left;
        if (isDivide()) {
            left = mark();
            advance();
            if (parseSeqMeasure() == null) {
                error("Unit of measure expected");
            }
            left.done(DIVIDE_MEASURE);
        }
        else {
            left = parseSeqMeasure();
        }
        if (left == null) {
            return null;
        }
        while (is(STAR) || isDivide()) {
            boolean product = is(STAR);
            PsiBuilder.Marker binary = left.precede();
            advance();
            if (parseSeqMeasure() == null) {
                error("Unit of measure expected");
            }
            binary.done(product ? PRODUCT_MEASURE : DIVIDE_MEASURE);
            left = binary;
        }
        return left;
    }

    private PsiBuilder.@Nullable Marker parseSeqMeasure() {
        PsiBuilder.Marker seq = mark();
        PsiBuilder.Marker first = parsePowerMeasure();
        if (first == null || !canStartMeasure()) {
            seq.drop();
            return first;
        }
        // in a sequence numbers are plain tokens: reparse
        seq.rollbackTo();
        seq = mark();
        while (canStartMeasure()) {
            if (is(INT32)) {
                advance();
                continue;
            }
            if (parsePowerMeasure() == null) {
                break;
            }
        }
        seq.done(SEQ_MEASURE);
        return seq;
    }

    private boolean canStartMeasure() {
        return is(IDENT) || is(LPAREN) || is(UNDERSCORE) || is(QUOTE) || is(INT32);
    }

    private PsiBuilder.@Nullable Marker parsePowerMeasure() {
        PsiBuilder.Marker measure = parseAtomicMeasure();
        if (measure == null) {
            return null;
        }
        while (is(INFIX_AT_HAT_OP) && tokenText().startsWith("^")) {
            PsiBuilder.Marker power = measure.precede();
            String operator = tokenText();
            advance();
            parseRationalConst(operator.length() > 1);
            power.done(POWER_MEASURE);
            measure = power;
        }
        return measure;
    }

    private void parseRationalConst(boolean negatedByOperator) {
        PsiBuilder.Marker rational = mark();
        if (negatedByOperator) {
            PsiBuilder.Marker negate = mark();
            parseRationalConstValue();
            negate.done(NEGATE_RAT);
        }
        else if (is(MINUS) || is(ADJACENT_PREFIX_OP) || is(INT32) && tokenText().startsWith("-")) {
            PsiBuilder.Marker negate = mark();
            if (!is(INT32)) {
                advance();
                parseRationalConstValue();
            }
            else {
                PsiBuilder.Marker integer = mark();
                advance();
                integer.done(INTEGER_RAT);
            }
            negate.done(NEGATE_RAT);
        }
        else {
            parseRationalConstValue();
        }
        rational.done(RATIONAL_CONST);
    }

    private void parseRationalConstValue() {
        if (is(LPAREN)) {
            PsiBuilder.Marker paren = mark();
            advance();
            int steps = is(MINUS) || is(ADJACENT_PREFIX_OP) ? 1 : 0;
            if (kind(steps) == INT32 && kind(steps + 1) == INFIX_STAR_DIV_MOD_OP) {
                PsiBuilder.Marker rational = mark();
                if (steps > 0) {
                    advance();
                }
                advance();
                advance();
                expect(INT32, "Number expected");
                rational.done(RATIONAL_RAT);
            }
            else {
                parseRationalConst(false);
            }
            expect(RPAREN, "')' expected");
            paren.done(PAREN_RAT);
            return;
        }
        PsiBuilder.Marker integer = mark();
        if (!consume(INT32)) {
            error("Number expected");
        }
        integer.done(INTEGER_RAT);
    }

    private PsiBuilder.@Nullable Marker parseAtomicMeasure() {
        if (is(INT32)) {
            PsiBuilder.Marker one = mark();
            advance();
            one.done(ONE_MEASURE);
            return one;
        }
        if (is(UNDERSCORE)) {
            PsiBuilder.Marker anon = mark();
            advance();
            anon.done(ANON_MEASURE);
            return anon;
        }
        if (is(QUOTE)) {
            PsiBuilder.Marker variable = mark();
            PsiBuilder.Marker name = mark();
            parseTypeParameterId();
            name.done(TYPE_REFERENCE_NAME);
            variable.done(VAR_MEASURE);
            return variable;
        }
        if (is(LPAREN)) {
            PsiBuilder.Marker paren = mark();
            advance();
            if (parseMeasure() == null) {
                error("Unit of measure expected");
            }
            expect(RPAREN, "')' expected");
            paren.done(PAREN_MEASURE);
            return paren;
        }
        if (is(IDENT)) {
            PsiBuilder.Marker named = mark();
            PsiBuilder.Marker type = mark();
            parseTypeReferenceName(true);
            type.done(NAMED_TYPE_USAGE);
            named.done(NAMED_MEASURE);
            return named;
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // strings, braces

    private PsiBuilder.Marker parseInterpolatedString() {
        PsiBuilder.Marker string = mark();
        boolean parts = is(INTERP_STRING_BEGIN_PART);
        advance();
        if (parts) {
            while (!myBuilder.eof()) {
                if (!is(INTERP_STRING_PART) && !is(INTERP_STRING_END)) {
                    parseTypedSequentialExpr();
                }
                if (consume(INTERP_STRING_PART)) {
                    continue;
                }
                expect(INTERP_STRING_END, "Interpolated string end expected");
                break;
            }
        }
        string.done(INTERPOLATED_STRING_EXPR);
        return string;
    }

    /**
     * Record, record copy, object or computation expression.
     */
    private PsiBuilder.Marker parseBraceExpression() {
        PsiBuilder.Marker brace = mark();
        advance();
        if (is(RBRACE)) {
            advance();
            brace.done(RECORD_EXPR);
            return brace;
        }

        if (is(NEW)) {
            advance();
            parseTypeReferenceName(true);
            consumeHighPrecedenceApp();
            if (is(LPAREN)) {
                parseAtomicExpression();
            }
            if (is(AS)) {
                advance();
                if (!consume(IDENT) && !consume(BASE)) {
                    error("Identifier expected");
                }
            }
            if (is(WITH) || is(OWITH)) {
                int end = matchEnd();
                advance();
                parseObjectExpressionMembers();
                if (end >= 0) {
                    skipTo(end);
                }
            }
            consume(OBLOCKSEP);
            while (is(INTERFACE) || is(OINTERFACE_MEMBER)) {
                parseInterfaceImplementation();
                consume(OBLOCKSEP);
            }
            expect(RBRACE, "'}' expected");
            brace.done(OBJ_EXPR);
            return brace;
        }

        if (is(INHERIT)) {
            advance();
            parseTypeReferenceName(true);
            consumeHighPrecedenceApp();
            parseAtomicExpression();
            consumeSeps();
            parseRecordMemberBindingList(RBRACE);
            expect(RBRACE, "'}' expected");
            brace.done(RECORD_EXPR);
            return brace;
        }

        if (isFieldBindingStart() || is(DOT_DOT_DOT)) {
            parseRecordMemberBindingList(RBRACE);
            expect(RBRACE, "'}' expected");
            brace.done(RECORD_EXPR);
            return brace;
        }

        PsiBuilder.Marker probe = mark();
        PsiBuilder.Marker copied = parseMinusExpr();
        if (copied != null && (is(WITH) || is(OWITH))) {
            probe.drop();
            int end = matchEnd();
            advance();
            parseRecordMemberBindingList(RBRACE);
            if (end >= 0) {
                skipTo(end);
            }
            expect(RBRACE, "'}' expected");
            brace.done(RECORD_EXPR);
            return brace;
        }
        probe.rollbackTo();

        parseSequentialExpr();
        expect(RBRACE, "'}' expected");
        brace.done(COMPUTATION_EXPR);
        return brace;
    }

    private boolean isFieldBindingStart() {
        if (!is(IDENT) && !is(UNDERSCORE)) {
            return false;
        }
        int steps = 1;
        while (kind(steps) == DOT && kind(steps + 1) == IDENT) {
            steps += 2;
        }
        return kind(steps) == EQUALS;
    }

    private void parseRecordMemberBindingList(FSharpLexFilterKind end) {
        PsiBuilder.Marker list = mark();
        boolean anonymous = end == BAR_RBRACE;
        while (!myBuilder.eof() && !is(end) && !is(OEND)) {
            if (is(DOT_DOT_DOT)) {
                PsiBuilder.Marker spread = mark();
                advance();
                if (parseDeclExprBlock() == null) {
                    error("Expression expected");
                }
                spread.done(EXPR_TYPE_SPREAD);
                consumeSeps();
                continue;
            }
            if (!is(IDENT) && !is(UNDERSCORE)) {
                error("Field binding expected");
                break;
            }
            PsiBuilder.Marker field = mark();
            parseExpressionReferenceName();
            if (expect(EQUALS, "'=' expected")) {
                if (parseDeclExprBlock() == null) {
                    error("Expression expected");
                }
            }
            if (anonymous) {
                field.done(RECORD_FIELD_BINDING);
                consumeSeps();
            }
            else {
                consumeSeps();
                field.done(RECORD_FIELD_BINDING);
            }
        }
        list.done(RECORD_MEMBER_BINDING_LIST);
    }

    protected abstract void parseObjectExpressionMembers();

    protected abstract void parseInterfaceImplementation();

    // ------------------------------------------------------------------------
    // keyword expressions

    private PsiBuilder.@Nullable Marker parseKeywordExpression() {
        FSharpLexFilterKind kind = kind();
        switch (kind) {
            case OLET, LET, OBINDER, BINDER -> {
                return parseLetExpression();
            }
            case FUN, OFUN -> {
                int end = matchEnd();
                PsiBuilder.Marker lambda = mark();
                advance();
                PsiBuilder.Marker parameters = mark();
                while (canStartAtomicPattern() && !is(RARROW)) {
                    if (parseAtomicPattern(true) == null) {
                        break;
                    }
                }
                parameters.done(LAMBDA_PARAMETERS_LIST);
                if (is(RARROW)) {
                    int bodyEnd = matchEnd();
                    advance();
                    if (parseTypedSequentialExprBlockR(bodyEnd) == null) {
                        error("Expression expected");
                    }
                }
                else {
                    error("'->' expected");
                }
                lambda.done(LAMBDA_EXPR);
                if (end >= 0) {
                    skipTo(end);
                }
                return lambda;
            }
            case FUNCTION, OFUNCTION -> {
                int end = matchEnd();
                PsiBuilder.Marker function = mark();
                advance();
                parseMatchClauses();
                function.done(MATCH_LAMBDA_EXPR);
                if (end >= 0) {
                    skipTo(end);
                }
                return function;
            }
            case MATCH, MATCH_BANG -> {
                PsiBuilder.Marker match = mark();
                advance();
                if (parseTypedSequentialExpr() == null) {
                    error("Expression expected");
                }
                int end = parseWith();
                match.done(MATCH_EXPR);
                if (end >= 0) {
                    skipTo(end);
                }
                return match;
            }
            case TRY -> {
                PsiBuilder.Marker tryExpr = mark();
                int bodyEnd = matchEnd();
                advance();
                if (parseTypedSequentialExprBlockR(bodyEnd) == null) {
                    error("Expression expected");
                }
                if (is(FINALLY)) {
                    advance();
                    if (parseTypedSequentialExprBlock() == null) {
                        error("Expression expected");
                    }
                    tryExpr.done(TRY_FINALLY_EXPR);
                    return tryExpr;
                }
                if (is(WITH) || is(OWITH)) {
                    int end = parseWith();
                    tryExpr.done(TRY_WITH_EXPR);
                    if (end >= 0) {
                        skipTo(end);
                    }
                    return tryExpr;
                }
                error("'with' or 'finally' expected");
                tryExpr.done(TRY_WITH_EXPR);
                return tryExpr;
            }
            case IF -> {
                return parseIf();
            }
            case WHILE, WHILE_BANG -> {
                PsiBuilder.Marker whileExpr = mark();
                advance();
                if (parseDeclExpr() == null) {
                    error("Expression expected");
                }
                parseDoBlock();
                whileExpr.done(WHILE_EXPR);
                return whileExpr;
            }
            case FOR -> {
                return parseFor();
            }
            case DO, ODO, DO_BANG, ODO_BANG -> {
                int end = matchEnd();
                PsiBuilder.Marker doExpr = mark();
                advance();
                if (parseTypedSequentialExprBlock() == null) {
                    error("Expression expected");
                }
                doExpr.done(DO_EXPR);
                if (end >= 0) {
                    skipTo(end);
                }
                return doExpr;
            }
            case ASSERT, OASSERT, LAZY, OLAZY, FIXED -> {
                PsiBuilder.Marker expr = mark();
                advance();
                if (parseDeclExprBlock() == null) {
                    error("Expression expected");
                }
                expr.done(kind == ASSERT || kind == OASSERT ? ASSERT_EXPR : kind == FIXED ? FIXED_EXPR : LAZY_EXPR);
                return expr;
            }
            case YIELD, YIELD_BANG -> {
                PsiBuilder.Marker expr = mark();
                advance();
                if (!canStartExpression()) {
                    // implicit zero: 'yield' without a value
                    expr.rollbackTo();
                    return parseImplicitZeroYield();
                }
                if (parseDeclExpr() == null) {
                    error("Expression expected");
                }
                expr.done(YIELD_OR_RETURN_EXPR);
                return expr;
            }
            default -> {
                return null;
            }
        }
    }

    private PsiBuilder.Marker parseImplicitZeroYield() {
        PsiBuilder.Marker yield = mark();
        PsiBuilder.Marker error = mark();
        advance();
        error.done(FROM_ERROR_EXPR);
        yield.done(YIELD_OR_RETURN_EXPR);
        return yield;
    }

    /**
     * {@code withClauses}
     *
     * @return position of the closing {@code OEND} or -1
     */
    private int parseWith() {
        if (is(OWITH) || is(WITH)) {
            int end = is(OWITH) ? matchEnd() : -1;
            advance();
            parseMatchClauses();
            return end;
        }
        error("'with' expected");
        return -1;
    }

    private PsiBuilder.Marker parseIf() {
        PsiBuilder.Marker ifExpr = mark();
        advance();
        if (parseDeclExpr() == null) {
            error("Expression expected");
        }
        if (consume(OTHEN)) {
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
        }
        else if (consume(THEN)) {
            if (parseDeclExpr() == null) {
                error("Expression expected");
            }
        }
        else {
            error("'then' expected");
        }
        if (is(ELIF)) {
            parseElif();
        }
        else if (consume(OELSE)) {
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
        }
        else if (consume(ELSE)) {
            if (parseDeclExpr() == null) {
                error("Expression expected");
            }
        }
        ifExpr.done(IF_THEN_ELSE_EXPR);
        return ifExpr;
    }

    private void parseElif() {
        PsiBuilder.Marker elif = mark();
        advance();
        if (parseDeclExpr() == null) {
            error("Expression expected");
        }
        if (consume(OTHEN)) {
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
        }
        else if (consume(THEN)) {
            if (parseDeclExpr() == null) {
                error("Expression expected");
            }
        }
        else {
            error("'then' expected");
        }
        if (is(ELIF)) {
            parseElif();
        }
        else if (consume(OELSE)) {
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
        }
        else if (consume(ELSE)) {
            if (parseDeclExpr() == null) {
                error("Expression expected");
            }
        }
        elif.done(ELIF_EXPR);
    }

    /**
     * {@code doToken typedSequentialExprBlock doneDeclEnd}
     */
    private void parseDoBlock() {
        if (is(ODO) || is(DO)) {
            int end = matchEnd();
            advance();
            if (parseTypedSequentialExprBlock() == null) {
                error("Expression expected");
            }
            if (end >= 0) {
                skipTo(end);
            }
            else {
                consume(DONE);
            }
            return;
        }
        error("'do' expected");
    }

    private PsiBuilder.Marker parseFor() {
        PsiBuilder.Marker forExpr = mark();
        advance();
        boolean numeric = (is(IDENT) || is(UNDERSCORE)) && isAt(1, EQUALS);
        if (numeric) {
            PsiBuilder.Marker local = mark();
            advance();
            local.done(LOCAL_DECLARATION);
            advance();
            parseDeclExpr();
            if (!consume(TO) && !consume(DOWNTO)) {
                error("'to' expected");
            }
            parseDeclExpr();
        }
        else {
            if (parsePattern(true) == null) {
                error("Pattern expected");
            }
            expect(IN, "'in' expected");
            parseDeclExpr();
        }
        consume(OBLOCKSEP);
        if (is(RARROW)) {
            int end = matchEnd();
            advance();
            PsiBuilder.Marker yield = mark();
            parseTypedSequentialExprBlockR(end);
            yield.done(YIELD_OR_RETURN_EXPR);
        }
        else {
            parseDoBlock();
        }
        forExpr.done(numeric ? FOR_EXPR : FOR_EACH_EXPR);
        return forExpr;
    }

    /**
     * {@code withPatternClauses}
     */
    private void parseMatchClauses() {
        boolean first = true;
        while (!myBuilder.eof()) {
            if (!is(BAR) && !is(BAR_JUST_BEFORE_NULL) && !(first && canStartAtomicPattern())) {
                break;
            }
            parseMatchClause();
            first = false;
            while (is(ORIGHT_BLOCK_END)) {
                advance();
            }
        }
    }

    private void parseMatchClause() {
        PsiBuilder.Marker clause = mark();
        if (!consume(BAR)) {
            consume(BAR_JUST_BEFORE_NULL);
        }
        if (parsePattern(true) == null) {
            error("Pattern expected");
        }
        if (is(WHEN)) {
            PsiBuilder.Marker when = mark();
            advance();
            parseDeclExpr();
            when.done(WHEN_EXPR_CLAUSE);
        }
        if (is(RARROW)) {
            int end = matchEnd();
            advance();
            if (parseTypedSequentialExprBlockR(end) == null) {
                error("Expression expected");
            }
        }
        else {
            error("'->' expected");
        }
        clause.done(MATCH_CLAUSE);
    }
}
