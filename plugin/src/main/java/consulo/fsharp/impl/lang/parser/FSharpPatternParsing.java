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

import consulo.language.parser.PsiBuilder;
import org.jspecify.annotations.Nullable;

import static consulo.fsharp.impl.lang.parser.FSharpLexFilterKind.*;
import static consulo.fsharp.impl.lang.psi.FSharpElementTypes.*;

/**
 * Patterns ({@code headBindingPattern}, {@code parenPattern}, {@code constrPattern}, {@code atomicPattern}), from lowest
 * to highest precedence: {@code as}, {@code |}, {@code ,}, {@code &}, {@code ::}, {@code :} (type annotation),
 * application ({@code Some x}), atomic patterns.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public abstract class FSharpPatternParsing extends FSharpTypeParsing {
    private boolean myTypeAnnotationAllowed = true;
    private int myParenDepth;

    protected FSharpPatternParsing(FSharpBuilder builder) {
        super(builder);
    }

    protected boolean canStartAtomicPattern() {
        return canStartAtomicPatternAt(0);
    }

    private boolean canStartAtomicPatternAt(int lookAhead) {
        FSharpLexFilterKind kind = kind(lookAhead);
        return switch (kind) {
            case IDENT, GLOBAL, UNDERSCORE, LPAREN, LBRACK, LBRACK_BAR, LBRACE, NULL, QMARK, LBRACK_LESS, COLON_QMARK, LPAREN_STAR_RPAREN,
                 LQUOTE -> true;
            case STRUCT -> kind(lookAhead + 1) == LPAREN;
            case HIGH_PRECEDENCE_PAREN_APP, HIGH_PRECEDENCE_BRACK_APP -> canStartAtomicPatternAt(lookAhead + 1);
            default -> LITERALS.contains(kind);
        };
    }

    /**
     * @param local whether named patterns are {@code LOCAL_REFERENCE_PAT} or {@code TOP_REFERENCE_PAT}
     */
    protected PsiBuilder.@Nullable Marker parsePattern(boolean local) {
        PsiBuilder.Marker pattern = parseOrPattern(local);
        if (pattern == null) {
            return null;
        }
        while (is(AS)) {
            PsiBuilder.Marker as = pattern.precede();
            advance();
            // the right side is a constructor pattern: 'p as x, y' is a tuple
            if (parseConsPattern(local) == null) {
                error("Pattern expected");
            }
            as.done(AS_PAT);
            pattern = as;
            while (is(COMMA) || is(BAR) || is(BAR_JUST_BEFORE_NULL)) {
                if (is(COMMA)) {
                    PsiBuilder.Marker tuple = pattern.precede();
                    while (consume(COMMA)) {
                        if (parseAndsPattern(local) == null) {
                            error("Pattern expected");
                            break;
                        }
                    }
                    tuple.done(TUPLE_PAT);
                    pattern = tuple;
                }
                else {
                    PsiBuilder.Marker or = pattern.precede();
                    advance();
                    if (parseTuplePattern(local) == null) {
                        error("Pattern expected");
                    }
                    or.done(OR_PAT);
                    pattern = or;
                }
            }
        }
        return pattern;
    }

    private PsiBuilder.@Nullable Marker parseOrPattern(boolean local) {
        PsiBuilder.Marker pattern = parseTuplePattern(local);
        if (pattern == null) {
            return null;
        }
        while ((is(BAR) || is(BAR_JUST_BEFORE_NULL)) && !isOffsideBar()) {
            PsiBuilder.Marker or = pattern.precede();
            advance();
            if (parseTuplePattern(local) == null) {
                error("Pattern expected");
            }
            or.done(OR_PAT);
            pattern = or;
        }
        return pattern;
    }

    /**
     * Whether {@code |} starts the next match clause instead of an or-pattern.
     */
    protected boolean isOffsideBar() {
        return false;
    }

    private PsiBuilder.@Nullable Marker parseTuplePattern(boolean local) {
        PsiBuilder.Marker pattern = parseAndsPattern(local);
        if (pattern == null || !is(COMMA)) {
            return pattern;
        }
        PsiBuilder.Marker tuple = pattern.precede();
        while (consume(COMMA)) {
            if (parseAndsPattern(local) == null) {
                error("Pattern expected");
                break;
            }
        }
        tuple.done(TUPLE_PAT);
        return tuple;
    }

    private PsiBuilder.@Nullable Marker parseAndsPattern(boolean local) {
        PsiBuilder.Marker pattern = parseConsPattern(local);
        if (pattern == null || !is(AMP)) {
            return pattern;
        }
        PsiBuilder.Marker ands = pattern.precede();
        while (consume(AMP)) {
            if (parseConsPattern(local) == null) {
                error("Pattern expected");
                break;
            }
        }
        ands.done(ANDS_PAT);
        return ands;
    }

    private PsiBuilder.@Nullable Marker parseConsPattern(boolean local) {
        PsiBuilder.Marker pattern = parseTypedPattern(local);
        if (pattern == null || !is(COLON_COLON)) {
            return pattern;
        }
        PsiBuilder.Marker cons = pattern.precede();
        advance();
        if (parseConsPattern(local) == null) {
            error("Pattern expected");
        }
        cons.done(LIST_CONS_PAT);
        return cons;
    }

    private PsiBuilder.@Nullable Marker parseTypedPattern(boolean local) {
        if (is(LBRACK_LESS)) {
            PsiBuilder.Marker attributed = mark();
            parseAttributeLists();
            if (parseTypedPattern(local) == null) {
                error("Pattern expected");
            }
            attributed.done(ATTRIB_PAT);
            return attributed;
        }

        boolean annotationAllowed = myTypeAnnotationAllowed;
        myTypeAnnotationAllowed = true;
        PsiBuilder.Marker pattern = parseAppPattern(local);
        myTypeAnnotationAllowed = annotationAllowed;
        if (pattern == null || !is(COLON) || !annotationAllowed) {
            return pattern;
        }
        PsiBuilder.Marker typed = pattern.precede();
        advance();
        if ((myParenDepth > 0 ? parseTypeUsage() : parseTupleTypeUsage()) == null) {
            error("Type expected");
        }
        typed.done(TYPED_PAT);
        return typed;
    }

    /**
     * Pattern of a binding head: a top level type annotation belongs to the binding ({@code let x: int = 1}).
     */
    protected PsiBuilder.@Nullable Marker parseBindingHeadPattern(boolean local) {
        boolean annotationAllowed = myTypeAnnotationAllowed;
        myTypeAnnotationAllowed = false;
        try {
            return parsePattern(local);
        }
        finally {
            myTypeAnnotationAllowed = annotationAllowed;
        }
    }

    /**
     * {@code constrPattern}
     */
    private PsiBuilder.@Nullable Marker parseAppPattern(boolean local) {
        if (is(COLON_QMARK)) {
            PsiBuilder.Marker isInst = mark();
            advance();
            if (parseTupleTypeUsage() == null) {
                error("Type expected");
            }
            isInst.done(IS_INST_PAT);
            return isInst;
        }

        if (isIdentOrOp() && !isLowerCaseVariable()) {
            PsiBuilder.Marker owner = mark();
            parseExpressionReferenceName();
            if (isPostfixTypeParameters()) {
                parseTypeArgumentList();
            }
            int afterApp = is(HIGH_PRECEDENCE_PAREN_APP) ? 1 : 0;
            if (kind(afterApp) == LPAREN && kind(afterApp + 1) == IDENT && kind(afterApp + 2) == EQUALS) {
                consume(HIGH_PRECEDENCE_PAREN_APP);
                parseNamedUnionCaseFields(local);
                owner.done(PARAMETERS_OWNER_PAT);
                return owner;
            }
            boolean any = false;
            while (canStartAtomicPattern() && !is(LBRACK_LESS)) {
                consumeHighPrecedenceApp();
                if (parseAtomicPattern(true) == null) {
                    break;
                }
                any = true;
            }
            if (any) {
                owner.done(PARAMETERS_OWNER_PAT);
            }
            else {
                owner.rollbackTo();
                return parseAtomicPattern(local);
            }
            return owner;
        }
        return parseAtomicPattern(local);
    }

    /**
     * Lower case single identifiers are variables, others may be union cases or active patterns with arguments.
     */
    private boolean isLowerCaseVariable() {
        if (!is(IDENT) || isAt(1, DOT) || isAt(1, HIGH_PRECEDENCE_PAREN_APP) || isAt(1, LPAREN)) {
            return false;
        }
        String text = tokenText();
        return !text.isEmpty() && !Character.isUpperCase(text.charAt(0));
    }

    private void parseNamedUnionCaseFields(boolean local) {
        PsiBuilder.Marker fields = mark();
        advance();
        while (is(IDENT)) {
            parseFieldPattern(local);
            if (!consumeSeps() && !consume(COMMA)) {
                break;
            }
        }
        expect(RPAREN, "')' expected");
        fields.done(NAMED_UNION_CASE_FIELDS_PAT);
    }

    private void parseFieldPattern(boolean local) {
        PsiBuilder.Marker field = mark();
        parseExpressionReferenceName();
        expect(EQUALS, "'=' expected");
        if (parseParenPattern(local) == null) {
            error("Pattern expected");
        }
        field.done(FIELD_PAT);
    }

    /**
     * {@code parenPattern}: inside brackets a type annotation may be a function type.
     */
    private PsiBuilder.@Nullable Marker parseParenPattern(boolean local) {
        myParenDepth++;
        boolean annotationAllowed = myTypeAnnotationAllowed;
        myTypeAnnotationAllowed = true;
        try {
            return parsePattern(local);
        }
        finally {
            myTypeAnnotationAllowed = annotationAllowed;
            myParenDepth--;
        }
    }

    protected PsiBuilder.@Nullable Marker parseAtomicPattern(boolean local) {
        if (is(UNDERSCORE)) {
            PsiBuilder.Marker wild = mark();
            advance();
            wild.done(WILD_PAT);
            return wild;
        }

        if (is(NULL)) {
            PsiBuilder.Marker nullPat = mark();
            advance();
            nullPat.done(NULL_PAT);
            return nullPat;
        }

        if (is(QMARK)) {
            PsiBuilder.Marker optional = mark();
            advance();
            if (parseAtomicPattern(true) == null) {
                error("Pattern expected");
            }
            optional.done(OPTIONAL_VAL_PAT);
            return optional;
        }

        if (is(CHAR) && isAt(1, DOT_DOT)) {
            PsiBuilder.Marker range = mark();
            advance();
            advance();
            expect(CHAR, "Character expected");
            range.done(CHAR_RANGE_PAT);
            return range;
        }

        if (is(LITERALS)) {
            PsiBuilder.Marker literal = mark();
            advance();
            literal.done(LITERAL_PAT);
            return literal;
        }

        if (is(LPAREN) && isAt(1, RPAREN)) {
            PsiBuilder.Marker unit = mark();
            advance();
            advance();
            unit.done(UNIT_PAT);
            return unit;
        }

        if (isIdentOrOp() || is(ACCESS) && isIdentOrOpAt(1)) {
            PsiBuilder.Marker reference = mark();
            consumeAccessModifier();
            if (is(LPAREN_STAR_RPAREN)) {
                PsiBuilder.Marker name = mark();
                advance();
                name.done(EXPRESSION_REFERENCE_NAME);
            }
            else {
                parseExpressionReferenceName();
            }
            reference.done(local ? LOCAL_REFERENCE_PAT : TOP_REFERENCE_PAT);
            return reference;
        }

        if (is(STRUCT) && isAt(1, LPAREN)) {
            PsiBuilder.Marker tuple = mark();
            advance();
            advance();
            myParenDepth++;
            boolean annotationAllowed = myTypeAnnotationAllowed;
            myTypeAnnotationAllowed = true;
            PsiBuilder.Marker first = parseAndsPattern(local);
            while (first != null && consume(COMMA)) {
                if (parseAndsPattern(local) == null) {
                    error("Pattern expected");
                    break;
                }
            }
            if (first == null) {
                error("Pattern expected");
            }
            myTypeAnnotationAllowed = annotationAllowed;
            myParenDepth--;
            expect(RPAREN, "')' expected");
            tuple.done(TUPLE_PAT);
            return tuple;
        }

        if (is(LPAREN)) {
            PsiBuilder.Marker paren = mark();
            advance();
            if (parseParenPattern(local) == null) {
                error("Pattern expected");
            }
            expect(RPAREN, "')' expected");
            paren.done(PAREN_PAT);
            return paren;
        }

        if (is(LBRACK) || is(LBRACK_BAR)) {
            boolean array = is(LBRACK_BAR);
            FSharpLexFilterKind end = array ? BAR_RBRACK : RBRACK;
            PsiBuilder.Marker list = mark();
            advance();
            while (!myBuilder.eof() && !is(end)) {
                if (parseParenPattern(true) == null) {
                    break;
                }
                if (!consumeSeps()) {
                    break;
                }
            }
            expect(end, array ? "'|]' expected" : "']' expected");
            list.done(array ? ARRAY_PAT : LIST_PAT);
            return list;
        }

        if (is(LBRACE)) {
            PsiBuilder.Marker record = mark();
            advance();
            while (is(IDENT)) {
                parseFieldPattern(local);
                if (!consumeSeps()) {
                    break;
                }
            }
            expect(RBRACE, "'}' expected");
            record.done(RECORD_PAT);
            return record;
        }

        if (is(LQUOTE)) {
            PsiBuilder.Marker quote = mark();
            PsiBuilder.Marker expr = mark();
            parseQuotationExpression();
            expr.done(QUOTE_EXPR);
            quote.done(QUOTE_EXPR_PAT);
            return quote;
        }

        return null;
    }

    protected abstract void parseQuotationExpression();
}
