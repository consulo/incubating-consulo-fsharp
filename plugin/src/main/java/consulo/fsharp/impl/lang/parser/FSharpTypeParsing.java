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

import java.util.EnumSet;
import java.util.Set;

import static consulo.fsharp.impl.lang.parser.FSharpLexFilterKind.*;
import static consulo.fsharp.impl.lang.psi.FSharpElementTypes.*;

/**
 * Types ({@code typ} and friends of {@code pars.fsy}), type parameters, constraints and attributes.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public abstract class FSharpTypeParsing {
    protected static final Set<FSharpLexFilterKind> ACCESS = EnumSet.of(PUBLIC, PRIVATE, INTERNAL);

    /**
     * {@code rawConstant} and booleans.
     */
    protected static final Set<FSharpLexFilterKind> LITERALS = EnumSet.of(
        TRUE, FALSE, INT8, UINT8, INT16, UINT16, INT32, UINT32, INT64, UINT64, NATIVEINT, UNATIVEINT, IEEE32, IEEE64, CHAR, DECIMAL,
        BIGNUM, STRING, KEYWORD_STRING, BYTEARRAY
    );

    /**
     * Tokens of operator names: {@code (+)}, {@code ( * )}.
     */
    protected static final Set<FSharpLexFilterKind> OPERATOR_NAMES = EnumSet.of(
        PREFIX_OP, INFIX_STAR_STAR_OP, INFIX_COMPARE_OP, INFIX_AT_HAT_OP, INFIX_BAR_OP, INFIX_AMP_OP, PLUS_MINUS_OP,
        INFIX_STAR_DIV_MOD_OP, DOLLAR, ADJACENT_PREFIX_OP, MINUS, STAR, EQUALS, LESS, GREATER, QMARK, AMP_AMP, BAR_BAR, COLON_EQUALS,
        FUNKY_OPERATOR_NAME, PERCENT_OP, DOT_DOT, AMP, COLON_COLON, QMARK_QMARK, COLON_GREATER, COLON_QMARK_GREATER, COLON_QMARK,
        DOT_DOT_HAT, OR
    );

    protected final FSharpBuilder myBuilder;

    protected FSharpTypeParsing(FSharpBuilder builder) {
        myBuilder = builder;
    }

    // ------------------------------------------------------------------------
    // helpers

    protected boolean is(FSharpLexFilterKind kind) {
        return myBuilder.kind() == kind;
    }

    protected boolean is(Set<FSharpLexFilterKind> kinds) {
        return kinds.contains(myBuilder.kind());
    }

    protected boolean isAt(int lookAhead, FSharpLexFilterKind kind) {
        return myBuilder.kind(lookAhead) == kind;
    }

    protected FSharpLexFilterKind kind() {
        return myBuilder.kind();
    }

    protected FSharpLexFilterKind kind(int lookAhead) {
        return myBuilder.kind(lookAhead);
    }

    protected boolean consume(FSharpLexFilterKind kind) {
        return myBuilder.consume(kind);
    }

    protected boolean expect(FSharpLexFilterKind kind, String message) {
        return myBuilder.expect(kind, message);
    }

    protected void advance() {
        myBuilder.advanceLexer();
    }

    protected PsiBuilder.Marker mark() {
        return myBuilder.mark();
    }

    protected void error(String message) {
        myBuilder.error(message);
    }

    protected String tokenText() {
        return myBuilder.text();
    }

    protected boolean isOp(FSharpLexFilterKind kind, String text) {
        return is(kind) && text.equals(myBuilder.text());
    }

    /**
     * {@code ^} of statically resolved type parameters.
     */
    protected boolean isHat() {
        return isOp(INFIX_AT_HAT_OP, "^");
    }

    protected boolean consumeAccessModifier() {
        if (is(ACCESS)) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * {@code seps}: {@code ;}, a virtual block separator or both.
     */
    protected boolean consumeSeps() {
        if (consume(SEMICOLON)) {
            consume(OBLOCKSEP);
            return true;
        }
        if (consume(OBLOCKSEP)) {
            consume(SEMICOLON);
            return true;
        }
        return false;
    }

    /**
     * Error recovery: skips tokens up to the virtual end of the current block, keeping nested blocks balanced.
     *
     * @return whether the block end was found (and consumed)
     */
    protected boolean skipToBlockEnd(FSharpLexFilterKind begin, FSharpLexFilterKind end) {
        if (is(end)) {
            advance();
            return true;
        }
        PsiBuilder.Marker error = mark();
        int depth = 0;
        boolean any = false;
        while (!myBuilder.eof()) {
            if (is(begin)) {
                depth++;
            }
            else if (is(end)) {
                if (depth == 0) {
                    break;
                }
                depth--;
            }
            advance();
            any = true;
        }
        if (any) {
            error.error("Unexpected tokens");
        }
        else {
            error.drop();
        }
        return consume(end);
    }

    // ------------------------------------------------------------------------
    // attributes

    protected boolean parseAttributeLists() {
        boolean any = false;
        while (is(LBRACK_LESS)) {
            parseAttributeList();
            any = true;
        }
        return any;
    }

    protected void parseAttributeList() {
        PsiBuilder.Marker list = mark();
        advance();
        while (!myBuilder.eof() && !is(GREATER_RBRACK)) {
            if (!parseAttribute()) {
                error("Attribute expected");
                break;
            }
            if (!consumeSeps()) {
                break;
            }
        }
        expect(GREATER_RBRACK, "'>]' expected");
        list.done(ATTRIBUTE_LIST);
        consume(OBLOCKSEP);
    }

    private boolean parseAttribute() {
        boolean target = isAt(1, COLON) && (is(IDENT) || is(MODULE) || is(TYPE) || is(YIELD));
        if (!target && !is(IDENT) && !is(GLOBAL)) {
            return false;
        }
        PsiBuilder.Marker attribute = mark();
        if (target) {
            PsiBuilder.Marker targetMarker = mark();
            advance();
            targetMarker.done(ATTRIBUTE_TARGET);
            advance();
        }
        boolean block = consume(OBLOCKBEGIN);
        parseTypeReferenceName(false);
        if (block) {
            skipToBlockEnd(OBLOCKBEGIN, OBLOCKEND);
        }
        consumeHighPrecedenceApp();
        if (canStartAtomicExprAfterType()) {
            parseAttributeArgument();
        }
        attribute.done(ATTRIBUTE);
        return true;
    }

    protected boolean consumeHighPrecedenceApp() {
        return consume(HIGH_PRECEDENCE_PAREN_APP) || consume(HIGH_PRECEDENCE_BRACK_APP);
    }

    protected abstract boolean canStartAtomicExprAfterType();

    protected abstract void parseAttributeArgument();

    // ------------------------------------------------------------------------
    // names

    /**
     * {@code A.B.C<T>} as nested {@link consulo.fsharp.impl.lang.psi.FSharpElementTypes#TYPE_REFERENCE_NAME}
     */
    protected PsiBuilder.@Nullable Marker parseTypeReferenceName(boolean allowTypeArguments) {
        if (!is(IDENT) && !is(GLOBAL)) {
            return null;
        }
        PsiBuilder.Marker name = mark();
        advance();
        if (allowTypeArguments) {
            parseTypeArgumentsIfAny();
        }
        name.done(TYPE_REFERENCE_NAME);
        while (is(DOT) && isAt(1, IDENT)) {
            name = name.precede();
            advance();
            advance();
            if (allowTypeArguments) {
                parseTypeArgumentsIfAny();
            }
            name.done(TYPE_REFERENCE_NAME);
        }
        return name;
    }

    /**
     * {@code A.B.C} as nested {@link consulo.fsharp.impl.lang.psi.FSharpElementTypes#EXPRESSION_REFERENCE_NAME}
     */
    protected PsiBuilder.@Nullable Marker parseExpressionReferenceName() {
        if (!isIdentOrOp()) {
            return null;
        }
        PsiBuilder.Marker name = mark();
        parseIdentOrOp(false);
        name.done(EXPRESSION_REFERENCE_NAME);
        while (is(DOT) && isIdentOrOpAt(1)) {
            name = name.precede();
            advance();
            parseIdentOrOp(false);
            name.done(EXPRESSION_REFERENCE_NAME);
        }
        return name;
    }

    /**
     * {@code identOrOp}: identifier or {@code opName}.
     */
    protected boolean isIdentOrOp() {
        return isIdentOrOpAt(0);
    }

    protected boolean isIdentOrOpAt(int lookAhead) {
        FSharpLexFilterKind kind = kind(lookAhead);
        if (kind == IDENT || kind == LPAREN_STAR_RPAREN) {
            return true;
        }
        return kind == LPAREN && isOpNameAt(lookAhead);
    }

    /**
     * {@code (+)}, {@code (|A|B|)}, {@code ( * )}, {@code (..)}, {@code (<@ @>)}
     */
    protected boolean isOpNameAt(int lookAhead) {
        FSharpLexFilterKind next = kind(lookAhead + 1);
        if (next == BAR) {
            return kind(lookAhead + 2) == IDENT;
        }
        if (next == DOT_DOT && kind(lookAhead + 2) == DOT_DOT) {
            return kind(lookAhead + 3) == RPAREN;
        }
        if (next == LQUOTE && kind(lookAhead + 2) == RQUOTE) {
            return kind(lookAhead + 3) == RPAREN;
        }
        return OPERATOR_NAMES.contains(next) && kind(lookAhead + 2) == RPAREN;
    }

    protected void parseIdentOrOp(boolean declaration) {
        if (is(LPAREN) && isAt(1, BAR)) {
            parseActivePatternId(declaration);
            return;
        }
        if (is(LPAREN)) {
            advance();
            while (!myBuilder.eof() && !is(RPAREN)) {
                advance();
            }
            expect(RPAREN, "')' expected");
            return;
        }
        advance();
    }

    /**
     * {@code (|A|B|_|)}
     */
    protected void parseActivePatternId(boolean declaration) {
        PsiBuilder.Marker id = mark();
        advance();
        advance();
        while (!myBuilder.eof() && !is(RPAREN)) {
            if (is(UNDERSCORE)) {
                PsiBuilder.Marker wild = mark();
                advance();
                wild.done(WILD_ACTIVE_PATTERN_WILD_CASE);
            }
            else if (is(IDENT)) {
                PsiBuilder.Marker caseMark = mark();
                advance();
                caseMark.done(declaration ? TOP_ACTIVE_PATTERN_CASE_DECLARATION : ACTIVE_PATTERN_NAMED_CASE_REFERENCE_NAME);
            }
            else {
                break;
            }
            if (!consume(BAR)) {
                break;
            }
        }
        expect(RPAREN, "')' expected");
        id.done(ACTIVE_PATTERN_ID);
    }

    // ------------------------------------------------------------------------
    // type arguments and parameters

    /**
     * {@code typeArgsNoHpaDeprecated} after a type name.
     */
    protected void parseTypeArgumentsIfAny() {
        if (is(HIGH_PRECEDENCE_TYAPP) && isAt(1, LESS) || is(LESS)) {
            parseTypeArgumentList();
        }
    }

    /**
     * {@code typeArgsActual}, optionally after {@code HIGH_PRECEDENCE_TYAPP}.
     */
    protected void parseTypeArgumentList() {
        consume(HIGH_PRECEDENCE_TYAPP);
        PsiBuilder.Marker list = mark();
        advance();
        if (!is(GREATER)) {
            parseTypeArgument();
            while (consume(COMMA)) {
                parseTypeArgument();
            }
        }
        expect(GREATER, "'>' expected");
        list.done(PREFIX_APP_TYPE_ARGUMENT_LIST);
    }

    private void parseTypeArgument() {
        PsiBuilder.Marker type = parseTypeUsage();
        if (type != null && is(EQUALS)) {
            PsiBuilder.Marker named = type.precede();
            advance();
            if (parseTypeUsage() == null) {
                error("Type expected");
            }
            named.done(NAMED_STATIC_CONSTANT_TYPE_USAGE);
        }
    }

    /**
     * {@code postfixTyparDecls}: {@code <'T, 'U when 'T : struct>}
     */
    protected boolean isPostfixTypeParameters() {
        return is(HIGH_PRECEDENCE_TYAPP) && isAt(1, LESS) || is(LESS);
    }

    protected void parsePostfixTypeParameterDeclarationList(consulo.language.ast.IElementType parameterType) {
        consume(HIGH_PRECEDENCE_TYAPP);
        PsiBuilder.Marker list = mark();
        advance();
        while (!myBuilder.eof() && !is(GREATER) && !is(WHEN)) {
            if (is(DOT_DOT)) {
                advance();
                break;
            }
            PsiBuilder.Marker parameter = mark();
            parseAttributeLists();
            if (!parseTypeParameterId()) {
                parameter.drop();
                error("Type parameter expected");
                break;
            }
            parameter.done(parameterType);
            if (!consume(COMMA)) {
                break;
            }
        }
        if (is(WHEN)) {
            parseTypeConstraintsClause();
        }
        expect(GREATER, "'>' expected");
        list.done(POSTFIX_TYPE_PARAMETER_DECLARATION_LIST);
    }

    protected boolean isTypar() {
        return isTyparAt(0);
    }

    protected boolean isTyparAt(int lookAhead) {
        return (kind(lookAhead) == QUOTE || kind(lookAhead) == INFIX_AT_HAT_OP && "^".equals(myBuilder.text(lookAhead)))
            && kind(lookAhead + 1) == IDENT;
    }

    /**
     * {@code typar}: {@code 'T} or {@code ^T}; an identifier is accepted for recovery.
     */
    protected boolean parseTypeParameterId() {
        if (is(QUOTE) || isHat()) {
            PsiBuilder.Marker id = mark();
            advance();
            if (!consume(IDENT)) {
                error("Identifier expected");
            }
            id.done(TYPE_PARAMETER_ID);
            return true;
        }
        if (is(IDENT)) {
            advance();
            return true;
        }
        return false;
    }

    protected void parseTypeConstraintsClause() {
        PsiBuilder.Marker clause = mark();
        advance();
        parseTypeConstraint();
        while (consume(AND)) {
            parseTypeConstraint();
        }
        clause.done(TYPE_CONSTRAINTS_CLAUSE);
    }

    private void parseTypeConstraint() {
        PsiBuilder.Marker constraint = mark();
        if (consume(DEFAULT)) {
            parseConstraintTypeName();
            expect(COLON, "':' expected");
            parseTypeUsage();
            constraint.done(DEFAULTS_TO_CONSTRAINT);
            return;
        }

        if (is(LPAREN)) {
            advance();
            parseConstraintTypeName();
            while (consume(OR)) {
                parseConstraintTypeName();
            }
            expect(RPAREN, "')' expected");
            expect(COLON, "':' expected");
            parseMemberConstraintSignature();
            constraint.done(MEMBER_CONSTRAINT);
            return;
        }

        if (!isTypar()) {
            // self constraint: I<'T>
            parseTypeReferenceName(true);
            constraint.done(SELF_CONSTRAINT);
            return;
        }
        parseConstraintTypeName();
        if (consume(COLON_GREATER)) {
            if (parseTypeUsage() == null) {
                error("Type expected");
            }
            constraint.done(SUBTYPE_CONSTRAINT);
            return;
        }
        if (!consume(COLON)) {
            constraint.done(SELF_CONSTRAINT);
            return;
        }

        if (consume(STRUCT)) {
            constraint.done(VALUE_TYPE_CONSTRAINT);
        }
        else if (consume(NULL)) {
            constraint.done(NULL_CONSTRAINT);
        }
        else if (is(IDENT) && "not".equals(tokenText())) {
            advance();
            if (consume(STRUCT)) {
                constraint.done(REFERENCE_TYPE_CONSTRAINT);
            }
            else {
                expect(NULL, "'null' expected");
                constraint.done(NOT_NULL_CONSTRAINT);
            }
        }
        else if (is(LPAREN)) {
            parseMemberConstraintSignature();
            constraint.done(MEMBER_CONSTRAINT);
        }
        else if (is(IDENT)) {
            String text = tokenText();
            advance();
            switch (text) {
                case "comparison" -> constraint.done(COMPARABLE_CONSTRAINT);
                case "equality" -> constraint.done(EQUATABLE_CONSTRAINT);
                case "unmanaged" -> constraint.done(UNMANAGED_TYPE_CONSTRAINT);
                case "enum" -> {
                    parseTypeArgumentsIfAny();
                    constraint.done(ENUM_CONSTRAINT);
                }
                case "delegate" -> {
                    parseTypeArgumentsIfAny();
                    constraint.done(DELEGATE_CONSTRAINT);
                }
                default -> constraint.done(REFERENCE_TYPE_CONSTRAINT);
            }
        }
        else if (is(DELEGATE)) {
            advance();
            parseTypeArgumentsIfAny();
            constraint.done(DELEGATE_CONSTRAINT);
        }
        else {
            parseTypeUsage();
            constraint.done(SUBTYPE_CONSTRAINT);
        }
    }

    private void parseConstraintTypeName() {
        PsiBuilder.Marker name = mark();
        if (!parseTypeParameterId()) {
            error("Type parameter expected");
        }
        name.done(TYPE_REFERENCE_NAME);
    }

    /**
     * {@code (member P : int)}, {@code (static member (+) : ^T * ^T -> ^T)}, {@code (new : unit -> 'T)}
     */
    private void parseMemberConstraintSignature() {
        if (!expect(LPAREN, "'(' expected")) {
            return;
        }
        PsiBuilder.Marker signature = mark();
        if (consume(NEW)) {
            parseSignatureReturnType();
            signature.done(CONSTRUCTOR_SIGNATURE);
        }
        else {
            consume(STATIC);
            if (!consume(MEMBER) && !consume(ABSTRACT)) {
                error("'member' expected");
            }
            consume(MEMBER);
            if (isIdentOrOp()) {
                parseIdentOrOp(false);
            }
            else {
                error("Member name expected");
            }
            parseSignatureReturnType();
            signature.done(MEMBER_SIGNATURE);
        }
        expect(RPAREN, "')' expected");
    }

    private void parseSignatureReturnType() {
        PsiBuilder.Marker info = mark();
        if (!expect(COLON, "':' expected")) {
            info.drop();
            return;
        }
        if (parseSignatureTypeUsage() == null) {
            error("Type expected");
        }
        info.done(RETURN_TYPE_INFO);
    }

    // ------------------------------------------------------------------------
    // types

    protected boolean canStartTypeUsage() {
        return is(IDENT) || is(GLOBAL) || isTypar() || is(UNDERSCORE) || is(LPAREN) || is(LBRACE_BAR) || is(HASH) || is(STRUCT)
            || is(LBRACK_LESS) || is(CONST) || is(NULL) || is(QMARK) || is(LITERALS);
    }

    /**
     * {@code typeWithTypeConstraints}: functions, tuples, applications.
     */
    protected PsiBuilder.@Nullable Marker parseTypeUsage() {
        PsiBuilder.Marker type = parseFunctionTypeUsage();
        if (type != null && is(WHEN)) {
            PsiBuilder.Marker constrained = type.precede();
            parseTypeConstraintsClause();
            constrained.done(CONSTRAINED_TYPE_USAGE);
            return constrained;
        }
        return type;
    }

    private PsiBuilder.@Nullable Marker parseFunctionTypeUsage() {
        PsiBuilder.Marker type = parseTupleTypeUsage();
        if (type == null) {
            return null;
        }
        if (is(RARROW)) {
            PsiBuilder.Marker function = type.precede();
            advance();
            if (parseFunctionTypeUsage() == null) {
                error("Type expected");
            }
            function.done(FUNCTION_TYPE_USAGE);
            return function;
        }
        return type;
    }

    /**
     * Type of an abstract member, delegate or signature ({@code topType}): every component is a
     * {@code PARAMETER_SIGNATURE_TYPE_USAGE}, optionally named ({@code x: int -> int}).
     */
    protected PsiBuilder.@Nullable Marker parseSignatureTypeUsage() {
        PsiBuilder.Marker type = parseSignatureFunctionTypeUsage();
        if (type != null && is(WHEN)) {
            PsiBuilder.Marker constrained = type.precede();
            parseTypeConstraintsClause();
            constrained.done(CONSTRAINED_TYPE_USAGE);
            return constrained;
        }
        return type;
    }

    private PsiBuilder.@Nullable Marker parseSignatureFunctionTypeUsage() {
        PsiBuilder.Marker type = parseSignatureTupleTypeUsage();
        if (type == null) {
            return null;
        }
        if (is(RARROW)) {
            PsiBuilder.Marker function = type.precede();
            advance();
            if (parseSignatureFunctionTypeUsage() == null) {
                error("Type expected");
            }
            function.done(FUNCTION_TYPE_USAGE);
            return function;
        }
        return type;
    }

    private PsiBuilder.@Nullable Marker parseSignatureTupleTypeUsage() {
        PsiBuilder.Marker type = parseSignatureParameter();
        if (type == null || !is(STAR)) {
            return type;
        }
        PsiBuilder.Marker tuple = type.precede();
        while (consume(STAR)) {
            if (parseSignatureParameter() == null) {
                error("Type expected");
                break;
            }
        }
        tuple.done(TUPLE_TYPE_USAGE);
        return tuple;
    }

    private PsiBuilder.@Nullable Marker parseSignatureParameter() {
        PsiBuilder.Marker parameter = mark();
        parseAttributeLists();
        consume(QMARK);
        if (is(IDENT) && isAt(1, COLON)) {
            advance();
            advance();
        }
        if (parsePostfixTypeUsage() == null) {
            parameter.rollbackTo();
            return null;
        }
        parameter.done(PARAMETER_SIGNATURE_TYPE_USAGE);
        return parameter;
    }

    protected PsiBuilder.@Nullable Marker parseTupleTypeUsage() {
        PsiBuilder.Marker type = parseIntersectionTypeUsage();
        if (type == null) {
            return null;
        }
        if (!is(STAR) && !isTupleSlash()) {
            return type;
        }
        PsiBuilder.Marker tuple = type.precede();
        while (is(STAR) || isTupleSlash()) {
            advance();
            if (parseIntersectionTypeUsage() == null) {
                error("Type expected");
                break;
            }
        }
        tuple.done(TUPLE_TYPE_USAGE);
        return tuple;
    }

    private PsiBuilder.@Nullable Marker parseIntersectionTypeUsage() {
        PsiBuilder.Marker type = parsePostfixTypeUsage();
        if (type == null || !is(AMP)) {
            return type;
        }
        PsiBuilder.Marker intersection = type.precede();
        while (consume(AMP)) {
            if (parsePostfixTypeUsage() == null) {
                error("Type expected");
                break;
            }
        }
        intersection.done(INTERSECTION_TYPE_USAGE);
        return intersection;
    }

    private boolean isTupleSlash() {
        return isOp(INFIX_STAR_DIV_MOD_OP, "/");
    }

    /**
     * {@code appTypeCanBeNullable}: atomic type with postfix applications ({@code int list}), array suffixes and {@code | null}.
     */
    protected PsiBuilder.@Nullable Marker parsePostfixTypeUsage() {
        PsiBuilder.Marker type = parseAtomicTypeUsage();
        if (type == null) {
            return null;
        }
        while (true) {
            int arrayStart = is(HIGH_PRECEDENCE_BRACK_APP) ? 1 : 0;
            if (kind(arrayStart) == LBRACK && (kind(arrayStart + 1) == RBRACK || kind(arrayStart + 1) == COMMA)) {
                PsiBuilder.Marker array = type.precede();
                consume(HIGH_PRECEDENCE_BRACK_APP);
                advance();
                while (consume(COMMA)) {
                    // multidimensional array
                }
                expect(RBRACK, "']' expected");
                array.done(ARRAY_TYPE_USAGE);
                type = array;
            }
            else if (isTypar()) {
                PsiBuilder.Marker argument = type.precede();
                argument.done(POSTFIX_APP_TYPE_ARGUMENT_LIST);
                PsiBuilder.Marker name = argument.precede();
                parseTypeParameterId();
                name.done(TYPE_REFERENCE_NAME);
                PsiBuilder.Marker named = name.precede();
                named.done(NAMED_TYPE_USAGE);
                type = named;
            }
            else if (is(IDENT) && !isAt(1, COLON)) {
                // postfix application: int list
                PsiBuilder.Marker argument = type.precede();
                argument.done(POSTFIX_APP_TYPE_ARGUMENT_LIST);
                PsiBuilder.Marker name = argument.precede();
                advance();
                parseTypeArgumentsIfAny();
                name.done(TYPE_REFERENCE_NAME);
                while (is(DOT) && isAt(1, IDENT)) {
                    name = name.precede();
                    advance();
                    advance();
                    parseTypeArgumentsIfAny();
                    name.done(TYPE_REFERENCE_NAME);
                }
                PsiBuilder.Marker named = name.precede();
                named.done(NAMED_TYPE_USAGE);
                type = named;
            }
            else if (is(BAR_JUST_BEFORE_NULL) && isAt(1, NULL)) {
                PsiBuilder.Marker withNull = type.precede();
                advance();
                advance();
                withNull.done(WITH_NULL_TYPE_USAGE);
                type = withNull;
            }
            else {
                break;
            }
        }
        return type;
    }

    private PsiBuilder.@Nullable Marker parseAtomicTypeUsage() {
        if (is(LBRACK_LESS) || is(QMARK) && isAt(1, IDENT) || is(IDENT) && isAt(1, COLON)) {
            PsiBuilder.Marker parameter = mark();
            parseAttributeLists();
            consume(QMARK);
            if (is(IDENT) && isAt(1, COLON)) {
                advance();
                advance();
            }
            if (parsePostfixTypeUsage() == null) {
                error("Type expected");
            }
            parameter.done(PARAMETER_SIGNATURE_TYPE_USAGE);
            return parameter;
        }

        if (is(IDENT) || is(GLOBAL)) {
            PsiBuilder.Marker named = mark();
            parseTypeReferenceName(true);
            named.done(NAMED_TYPE_USAGE);
            return named;
        }

        if (is(STRUCT) && isAt(1, LPAREN)) {
            PsiBuilder.Marker tuple = mark();
            advance();
            advance();
            if (parseIntersectionTypeUsage() == null) {
                error("Type expected");
            }
            while (consume(STAR)) {
                if (parseIntersectionTypeUsage() == null) {
                    error("Type expected");
                    break;
                }
            }
            expect(RPAREN, "')' expected");
            tuple.done(TUPLE_TYPE_USAGE);
            return tuple;
        }

        if (is(LPAREN) && isMlStyleTypeArguments()) {
            PsiBuilder.Marker named = mark();
            PsiBuilder.Marker name = mark();
            advance();
            PsiBuilder.Marker arguments = mark();
            parseTypeUsage();
            while (consume(COMMA)) {
                parseTypeUsage();
            }
            arguments.done(POSTFIX_APP_TYPE_ARGUMENT_LIST);
            expect(RPAREN, "')' expected");
            if (!consume(IDENT)) {
                error("Type name expected");
            }
            name.done(TYPE_REFERENCE_NAME);
            named.done(NAMED_TYPE_USAGE);
            return named;
        }

        if (isTypar()) {
            PsiBuilder.Marker named = mark();
            PsiBuilder.Marker name = mark();
            parseTypeParameterId();
            name.done(TYPE_REFERENCE_NAME);
            named.done(NAMED_TYPE_USAGE);
            return named;
        }

        if (is(UNDERSCORE)) {
            PsiBuilder.Marker anon = mark();
            advance();
            anon.done(ANON_TYPE_USAGE);
            return anon;
        }

        if (is(HASH)) {
            PsiBuilder.Marker hash = mark();
            advance();
            if (parseAtomicTypeUsage() == null) {
                error("Type expected");
            }
            hash.done(HASH_TYPE_USAGE);
            return hash;
        }

        if (is(LPAREN)) {
            PsiBuilder.Marker paren = mark();
            advance();
            if (parseTypeUsage() == null) {
                error("Type expected");
            }
            expect(RPAREN, "')' expected");
            paren.done(PAREN_TYPE_USAGE);
            return paren;
        }

        if (is(LBRACE_BAR) || is(STRUCT) && isAt(1, LBRACE_BAR)) {
            PsiBuilder.Marker record = mark();
            consume(STRUCT);
            advance();
            while (is(IDENT) || is(DOT_DOT_DOT)) {
                if (is(DOT_DOT_DOT)) {
                    PsiBuilder.Marker spread = mark();
                    advance();
                    parseTypeReferenceName(true);
                    spread.done(DECLARATION_TYPE_SPREAD);
                    consumeSeps();
                    continue;
                }
                PsiBuilder.Marker field = mark();
                parseExpressionReferenceName();
                expect(COLON, "':' expected");
                if (parseTypeUsage() == null) {
                    error("Type expected");
                }
                field.done(ANON_RECORD_FIELD);
                consumeSeps();
            }
            expect(BAR_RBRACE, "'|}' expected");
            record.done(ANON_RECORD_TYPE_USAGE);
            return record;
        }

        if (is(CONST) || is(LITERALS) || is(NULL)) {
            PsiBuilder.Marker constant = mark();
            consume(CONST);
            parseStaticConstantExpression();
            constant.done(EXPR_STATIC_CONSTANT_TYPE_USAGE);
            return constant;
        }

        return null;
    }

    /**
     * {@code (int, string) list}
     */
    private boolean isMlStyleTypeArguments() {
        PsiBuilder.Marker probe = mark();
        try {
            advance();
            PsiBuilder.Marker type = parseTypeUsage();
            return type != null && is(COMMA);
        }
        finally {
            probe.rollbackTo();
        }
    }

    protected abstract void parseStaticConstantExpression();
}
