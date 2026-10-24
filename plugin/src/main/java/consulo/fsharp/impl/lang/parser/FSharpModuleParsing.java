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

import java.util.EnumSet;
import java.util.Set;

import static consulo.fsharp.impl.lang.parser.FSharpLexFilterKind.*;
import static consulo.fsharp.impl.lang.psi.FSharpElementTypes.*;

/**
 * Files, namespaces, modules, module members ({@code moduleDefn}), type declarations ({@code tyconDefn}) and type members
 * ({@code classDefnMember}).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpModuleParsing extends FSharpExpressionParsing {
    private static final Set<FSharpLexFilterKind> MEMBER_KEYWORDS = EnumSet.of(MEMBER, OVERRIDE, DEFAULT);

    private static final Set<FSharpLexFilterKind> TOP_SEPARATORS = EnumSet.of(OBLOCKSEP, SEMICOLON, SEMICOLON_SEMICOLON);

    private final boolean mySignature;

    public FSharpModuleParsing(FSharpBuilder builder, boolean signature) {
        super(builder);
        mySignature = signature;
    }

    // ------------------------------------------------------------------------
    // file

    public void parseFile() {
        PsiBuilder.Marker anon = myBuilder.markNoFlush();
        if (isTopLevelModuleOrNamespace()) {
            anon.drop();
            while (!myBuilder.eof()) {
                if (!parseTopLevelModuleOrNamespace()) {
                    PsiBuilder.Marker error = mark();
                    advance();
                    error.error("Namespace or module expected");
                }
            }
            return;
        }

        parseModuleDefns(true);
        myBuilder.flushDirectives();
        anon.done(ANON_MODULE_DECLARATION);
        while (!myBuilder.eof()) {
            if (is(NAMESPACE) && parseTopLevelModuleOrNamespace()) {
                continue;
            }
            PsiBuilder.Marker error = mark();
            advance();
            error.error("Unexpected token");
        }
    }

    private boolean isTopLevelModuleOrNamespace() {
        PsiBuilder.Marker probe = mark();
        try {
            parseAttributeLists();
            if (is(NAMESPACE)) {
                return true;
            }
            if (!consume(MODULE)) {
                return false;
            }
            consumeAccessModifier();
            consume(REC);
            if (!consume(IDENT)) {
                return false;
            }
            while (is(DOT) && isAt(1, IDENT)) {
                advance();
                advance();
            }
            return !is(EQUALS) && !is(COLON);
        }
        finally {
            probe.rollbackTo();
        }
    }

    private boolean parseTopLevelModuleOrNamespace() {
        PsiBuilder.Marker declaration = mark();
        parseAttributeLists();
        if (is(NAMESPACE)) {
            advance();
            consume(REC);
            boolean global = false;
            if (is(GLOBAL)) {
                advance();
                global = true;
            }
            else {
                parseDeclarationName();
            }
            consume(EQUALS);
            parseModuleDefnsBlock(false);
            declaration.done(global ? GLOBAL_NAMESPACE_DECLARATION : NAMED_NAMESPACE_DECLARATION);
            return true;
        }
        if (is(MODULE)) {
            advance();
            consumeAccessModifier();
            consume(REC);
            parseDeclarationName();
            parseModuleDefnsBlock(false);
            if (mySignature) {
                // unlike implementation files, trailing directives stay in the module
                myBuilder.flushDirectives();
            }
            declaration.done(NAMED_MODULE_DECLARATION);
            return true;
        }
        declaration.rollbackTo();
        return false;
    }

    /**
     * {@code A.B.C}: qualifier as type reference name, then the declared identifier.
     */
    private void parseDeclarationName() {
        if (!is(IDENT)) {
            error("Identifier expected");
            return;
        }
        if (!isAt(1, DOT)) {
            advance();
            return;
        }
        PsiBuilder.Marker qualifier = mark();
        advance();
        qualifier.done(TYPE_REFERENCE_NAME);
        while (is(DOT) && isAt(1, IDENT)) {
            if (!isAt(2, DOT)) {
                advance();
                advance();
                return;
            }
            qualifier = qualifier.precede();
            advance();
            advance();
            qualifier.done(TYPE_REFERENCE_NAME);
        }
    }

    /**
     * {@code moduleDefnsOrExprPossiblyEmptyOrBlock}
     */
    private void parseModuleDefnsBlock(boolean fileLevel) {
        if (is(OBLOCKBEGIN)) {
            int end = matchEnd();
            advance();
            parseModuleDefns(fileLevel);
            if (end >= 0) {
                skipTo(end);
            }
            else {
                skipToBlockEnd(OBLOCKBEGIN, OBLOCKEND);
            }
            consume(OBLOCKSEP);
            return;
        }
        parseModuleDefns(fileLevel);
    }

    /**
     * Module members up to the end of the current block.
     */
    private void parseModuleDefns(boolean fileLevel) {
        while (true) {
            while (is(TOP_SEPARATORS)) {
                advance();
            }
            if (myBuilder.eof() || is(OBLOCKEND) || is(END) || is(NAMESPACE)) {
                return;
            }
            int position = myBuilder.position();
            parseModuleMember();
            if (position == myBuilder.position()) {
                PsiBuilder.Marker error = mark();
                advance();
                error.error("Unexpected token");
            }
        }
    }

    private FSharpLexFilterKind memberKeywordAfterAttributes() {
        PsiBuilder.Marker probe = mark();
        parseAttributeLists();
        FSharpLexFilterKind kind = kind();
        probe.rollbackTo();
        return kind;
    }

    private void parseModuleMember() {
        FSharpLexFilterKind keyword = memberKeywordAfterAttributes();
        switch (keyword) {
            case LET, OLET -> {
                if (isLetInExpression()) {
                    parseExpressionStatement();
                }
                else {
                    PsiBuilder.Marker declaration = mark();
                    parseLetBindings(TOP_BINDING, false);
                    declaration.done(LET_BINDINGS_DECLARATION);
                }
            }
            case TYPE -> parseTypeDeclarationGroup();
            case MODULE -> parseNestedModule();
            case OPEN -> {
                PsiBuilder.Marker open = mark();
                advance();
                boolean type = consume(TYPE);
                PsiBuilder.Marker name = parseTypeReferenceName(true);
                if (name == null) {
                    error("Namespace or module name expected");
                }
                else if (type && is(IDENT)) {
                    parsePostfixTypeReferenceName(name);
                }
                open.done(OPEN_STATEMENT);
            }
            case EXCEPTION -> parseException();
            case DO, ODO -> parseDoStatement();
            case EXTERN -> parseExtern();
            case VAL -> parseBindingSignature();
            default -> {
                if (is(LBRACK_LESS) && !canStartExpressionAfterAttributes()) {
                    // unattached attributes
                    parseAttributeLists();
                    return;
                }
                parseExpressionStatement();
            }
        }
    }

    /**
     * {@code open type A.B C.D}: the type is a postfix application.
     */
    private void parsePostfixTypeReferenceName(PsiBuilder.Marker argumentName) {
        PsiBuilder.Marker named = argumentName.precede();
        named.done(NAMED_TYPE_USAGE);
        PsiBuilder.Marker arguments = named.precede();
        arguments.done(POSTFIX_APP_TYPE_ARGUMENT_LIST);
        PsiBuilder.Marker name = arguments.precede();
        if (isAt(1, DOT)) {
            PsiBuilder.Marker qualifier = mark();
            advance();
            qualifier.done(TYPE_REFERENCE_NAME);
            while (is(DOT) && isAt(1, IDENT) && isAt(2, DOT)) {
                qualifier = qualifier.precede();
                advance();
                advance();
                qualifier.done(TYPE_REFERENCE_NAME);
            }
            advance();
        }
        consume(IDENT);
        name.done(TYPE_REFERENCE_NAME);
    }

    private boolean canStartExpressionAfterAttributes() {
        PsiBuilder.Marker probe = mark();
        parseAttributeLists();
        boolean result = canStartExpression();
        probe.rollbackTo();
        return result;
    }

    /**
     * {@code let x = 1 in x + 1} at module level is an expression.
     */
    private boolean isLetInExpression() {
        PsiBuilder.Marker probe = mark();
        try {
            parseAttributeLists();
            if (is(OLET)) {
                int distance = myBuilder.matchDistance();
                if (distance < 0) {
                    return false;
                }
                FSharpLexFilterKind after = kind(distance + 1);
                return after != OBLOCKSEP && after != OBLOCKEND && after != EOF && !TOP_SEPARATORS.contains(after) && after != ODECLEND
                    && after != END && after != NAMESPACE && !isDeclarationStart(after);
            }
            parseBinding(TOP_BINDING, false);
            return is(IN);
        }
        finally {
            probe.rollbackTo();
        }
    }

    private static boolean isDeclarationStart(FSharpLexFilterKind kind) {
        return kind == OLET || kind == TYPE || kind == MODULE || kind == OPEN || kind == EXCEPTION || kind == LBRACK_LESS;
    }

    private void parseExpressionStatement() {
        PsiBuilder.Marker statement = mark();
        parseAttributeLists();
        consumeAccessModifier();
        if (parseDeclExpr() == null) {
            statement.drop();
            if (!myBuilder.eof() && !is(NAMESPACE) && !is(OBLOCKEND)) {
                PsiBuilder.Marker error = mark();
                advance();
                error.error("Expression expected");
            }
            return;
        }
        statement.done(EXPRESSION_STATEMENT);
    }

    private void parseDoStatement() {
        PsiBuilder.Marker doStatement = mark();
        parseAttributeLists();
        consume(STATIC);
        int end = matchEnd();
        advance();
        if (parseTypedSequentialExprBlock() == null) {
            error("Expression expected");
        }
        doStatement.done(DO_STATEMENT);
        if (end >= 0) {
            skipTo(end);
        }
    }

    private void parseBindingSignature() {
        PsiBuilder.Marker signature = mark();
        parseAttributeLists();
        advance();
        consume(INLINE);
        consume(MUTABLE);
        consumeAccessModifier();
        PsiBuilder.Marker head = mark();
        PsiBuilder.Marker name = mark();
        parseIdentOrOp(true);
        name.done(EXPRESSION_REFERENCE_NAME);
        head.done(TOP_REFERENCE_PAT);
        if (isPostfixTypeParameters()) {
            // like ReSharper: plain tokens
            consume(HIGH_PRECEDENCE_TYAPP);
            int depth = 0;
            while (!myBuilder.eof()) {
                if (is(LESS)) {
                    depth++;
                }
                else if (is(GREATER) && --depth == 0) {
                    advance();
                    break;
                }
                advance();
            }
        }
        if (is(COLON)) {
            parseReturnTypeInfo(true);
        }
        if (is(EQUALS)) {
            int end = matchEnd();
            advance();
            if (parseDeclExprBlock() == null) {
                error("Expression expected");
            }
            if (end >= 0) {
                skipTo(end);
            }
        }
        signature.done(BINDING_SIGNATURE);
    }

    private void parseExtern() {
        PsiBuilder.Marker extern = mark();
        parseAttributeLists();
        consume(STATIC);
        advance();
        int depth = 0;
        while (!myBuilder.eof()) {
            if (is(LBRACK_LESS) && depth == 0) {
                parseAttributeList();
                continue;
            }
            if (is(LPAREN)) {
                depth++;
            }
            else if (is(RPAREN)) {
                depth--;
                if (depth == 0) {
                    advance();
                    break;
                }
            }
            else if (depth == 0 && (is(OBLOCKSEP) || is(OBLOCKEND))) {
                break;
            }
            advance();
        }
        extern.done(EXTERN_DECLARATION);
    }

    // ------------------------------------------------------------------------
    // modules

    private void parseNestedModule() {
        PsiBuilder.Marker module = mark();
        parseAttributeLists();
        advance();
        parseAttributeLists();
        consumeAccessModifier();
        consume(REC);
        if (!consume(IDENT)) {
            error("Identifier expected");
        }
        if (!is(EQUALS) && !is(COLON)) {
            error("'=' expected");
            consume(OBLOCKSEP);
            module.done(NESTED_MODULE_DECLARATION);
            return;
        }
        advance();

        if (is(OBLOCKBEGIN) && isModuleAbbreviation()) {
            int end = matchEnd();
            advance();
            parseTypeReferenceName(false);
            skipTo(end);
            module.done(MODULE_ABBREVIATION_DECLARATION);
            return;
        }

        if (is(OBLOCKBEGIN)) {
            int end = matchEnd();
            advance();
            if (is(BEGIN)) {
                advance();
                parseModuleDefns(false);
                expect(END, "'end' expected");
            }
            else {
                parseModuleDefns(false);
            }
            if (end >= 0) {
                skipTo(end);
            }
        }
        else if (consume(BEGIN)) {
            parseModuleDefns(false);
            expect(END, "'end' expected");
        }
        module.done(NESTED_MODULE_DECLARATION);
    }

    /**
     * {@code module M = A.B}: the block is a single long identifier.
     */
    private boolean isModuleAbbreviation() {
        int distance = myBuilder.matchDistance();
        if (distance < 0 || kind(1) != IDENT) {
            return false;
        }
        int steps = 2;
        while (kind(steps) == DOT && kind(steps + 1) == IDENT) {
            steps += 2;
        }
        return steps == distance;
    }

    // ------------------------------------------------------------------------
    // exceptions

    private void parseException() {
        PsiBuilder.Marker exception = mark();
        parseAttributeLists();
        advance();
        parseAttributeLists();
        consumeAccessModifier();
        if (!consume(IDENT)) {
            error("Identifier expected");
        }
        if (consume(OF)) {
            parseCaseFields(EXCEPTION_FIELD_DECLARATION);
        }
        if (consume(EQUALS)) {
            parseTypeReferenceName(false);
        }
        if (is(WITH)) {
            parseAugmentation();
        }
        exception.done(EXCEPTION_DECLARATION);
    }

    // ------------------------------------------------------------------------
    // types

    private void parseTypeDeclarationGroup() {
        PsiBuilder.Marker group = mark();
        parseTypeDeclaration();
        while (is(AND) || is(LBRACK_LESS) && memberKeywordAfterAttributes() == AND) {
            parseTypeDeclaration();
        }
        group.done(TYPE_DECLARATION_GROUP);
    }

    private void parseTypeDeclaration() {
        if ((is(TYPE) || is(AND)) && isTypeExtension()) {
            // like ReSharper: the keyword of an extension belongs to the declaration group
            advance();
        }
        PsiBuilder.Marker declaration = mark();
        parseAttributeLists();
        if (is(TYPE) || is(AND)) {
            advance();
        }
        parseAttributeLists();
        consumeAccessModifier();

        boolean extension = false;
        if (isTypar() || is(LPAREN) && isTyparAt(1)) {
            parsePrefixTypeParameters();
        }

        if (is(IDENT) && isAt(1, DOT)) {
            PsiBuilder.Marker qualifier = mark();
            advance();
            qualifier.done(TYPE_REFERENCE_NAME);
            while (is(DOT) && isAt(1, IDENT) && isAt(2, DOT)) {
                qualifier = qualifier.precede();
                advance();
                advance();
                qualifier.done(TYPE_REFERENCE_NAME);
            }
            expect(DOT, "'.' expected");
            extension = true;
        }

        if (!consume(IDENT)) {
            error("Identifier expected");
        }

        if (isPostfixTypeParameters()) {
            parsePostfixTypeParameterDeclarationList(TYPE_PARAMETER_OF_TYPE_DECLARATION);
        }
        if (is(WHEN)) {
            parseTypeConstraintsClause();
        }

        if (is(WITH)) {
            parseAugmentation();
            declaration.done(TYPE_EXTENSION_DECLARATION);
            return;
        }
        if (extension) {
            error("'with' expected");
        }

        if (isPrimaryConstructorStart()) {
            parsePrimaryConstructor();
        }

        if (!consume(EQUALS)) {
            declaration.done(FSHARP_TYPE_DECLARATION);
            return;
        }

        if (is(OBLOCKBEGIN)) {
            int end = matchEnd();
            advance();
            parseTypeRepresentation();
            consumeSeps();
            parseTypeMembers();
            if (is(WITH)) {
                parseAugmentation();
            }
            if (end >= 0) {
                skipTo(end);
            }
            else {
                skipToBlockEnd(OBLOCKBEGIN, OBLOCKEND);
            }
        }
        else {
            parseTypeRepresentation();
        }
        if (is(WITH)) {
            parseAugmentation();
        }
        declaration.done(FSHARP_TYPE_DECLARATION);
    }

    private boolean isPrimaryConstructorStart() {
        int steps = 0;
        if (kind(steps) == LBRACK_LESS) {
            return true;
        }
        if (ACCESS.contains(kind(steps))) {
            steps++;
        }
        if (kind(steps) == HIGH_PRECEDENCE_PAREN_APP) {
            steps++;
        }
        return kind(steps) == LPAREN;
    }

    /**
     * {@code with members [end]}: type augmentation, exception members.
     */
    private void parseAugmentation() {
        int end = matchEnd();
        advance();
        parseTypeMembersOrBlock();
        consume(END);
        if (end >= 0) {
            skipTo(end);
        }
    }

    private boolean isTypeExtension() {
        PsiBuilder.Marker probe = mark();
        try {
            advance();
            parseAttributeLists();
            consumeAccessModifier();
            if (isTypar() || is(LPAREN) && isTyparAt(1)) {
                parsePrefixTypeParameters();
            }
            if (!consume(IDENT)) {
                return false;
            }
            while (is(DOT) && isAt(1, IDENT)) {
                advance();
                advance();
            }
            if (isPostfixTypeParameters()) {
                consume(HIGH_PRECEDENCE_TYAPP);
                int depth = 0;
                do {
                    if (is(LESS)) {
                        depth++;
                    }
                    else if (is(GREATER)) {
                        depth--;
                    }
                    advance();
                }
                while (!myBuilder.eof() && depth > 0);
            }
            return is(WITH);
        }
        finally {
            probe.rollbackTo();
        }
    }

    private void parsePrefixTypeParameters() {
        PsiBuilder.Marker list = mark();
        if (consume(LPAREN)) {
            do {
                PsiBuilder.Marker parameter = mark();
                parseTypeParameterId();
                parameter.done(TYPE_PARAMETER_OF_TYPE_DECLARATION);
            }
            while (consume(COMMA));
            expect(RPAREN, "')' expected");
        }
        else {
            PsiBuilder.Marker parameter = mark();
            parseTypeParameterId();
            parameter.done(TYPE_PARAMETER_OF_TYPE_DECLARATION);
        }
        list.done(PREFIX_TYPE_PARAMETER_DECLARATION_LIST);
    }

    private void parsePrimaryConstructor() {
        PsiBuilder.Marker constructor = mark();
        parseAttributeLists();
        consumeAccessModifier();
        consume(HIGH_PRECEDENCE_PAREN_APP);
        PsiBuilder.Marker parameters = mark();
        if (parseAtomicPattern(true) == null) {
            error("Parameters expected");
        }
        parameters.done(PARAMETERS_PATTERN_DECLARATION);
        if (is(AS)) {
            PsiBuilder.Marker self = mark();
            advance();
            if (!consume(IDENT)) {
                error("Identifier expected");
            }
            self.done(CTOR_SELF_ID);
        }
        constructor.done(PRIMARY_CONSTRUCTOR_DECLARATION);
    }

    private void parseTypeRepresentation() {
        FSharpLexFilterKind kind = kind();

        if (isTypeMemberStart()) {
            return;
        }

        if (kind == LBRACE || ACCESS.contains(kind) && isAt(1, LBRACE)) {
            parseRecordRepresentation();
            return;
        }

        if (kind == CLASS || kind == STRUCT && !isAt(1, LPAREN) || kind == INTERFACE) {
            PsiBuilder.Marker representation = mark();
            advance();
            parseTypeMembersOrBlock();
            expect(END, "'end' expected");
            representation.done(kind == CLASS ? CLASS_REPRESENTATION : kind == STRUCT ? STRUCT_REPRESENTATION : INTERFACE_REPRESENTATION);
            return;
        }

        if (kind == DELEGATE) {
            PsiBuilder.Marker representation = mark();
            advance();
            expect(OF, "'of' expected");
            if (parseSignatureTypeUsage() == null) {
                error("Type expected");
            }
            representation.done(DELEGATE_REPRESENTATION);
            return;
        }

        if (kind == LPAREN && isAt(1, HASH)) {
            PsiBuilder.Marker representation = mark();
            while (!myBuilder.eof() && !is(RPAREN)) {
                advance();
            }
            expect(RPAREN, "')' expected");
            representation.done(IL_ASSEMBLY_REPRESENTATION);
            return;
        }

        if (kind == BAR || ACCESS.contains(kind) && isAt(1, BAR) || isUnionCaseStart()) {
            parseUnionOrEnumRepresentation();
            return;
        }

        PsiBuilder.Marker abbreviation = mark();
        PsiBuilder.Marker typeUsage = mark();
        if (parseTypeUsage() == null) {
            typeUsage.drop();
            abbreviation.drop();
            error("Type representation expected");
            return;
        }
        typeUsage.done(TYPE_USAGE_OR_UNION_CASE_DECLARATION);
        abbreviation.done(TYPE_ABBREVIATION_REPRESENTATION);
    }

    /**
     * {@code A of int}, {@code A | B}, {@code [<Attr>] A}
     */
    private boolean isUnionCaseStart() {
        if (is(LBRACK_LESS)) {
            return true;
        }
        int steps = is(ACCESS) ? 1 : 0;
        if (kind(steps) != IDENT) {
            return false;
        }
        FSharpLexFilterKind next = kind(steps + 1);
        return next == OF || next == BAR || next == EQUALS && isEnumValueAt(steps + 2);
    }

    private boolean isEnumValueAt(int steps) {
        FSharpLexFilterKind kind = kind(steps);
        return LITERALS.contains(kind) || kind == MINUS || kind == ADJACENT_PREFIX_OP;
    }

    private void parseUnionOrEnumRepresentation() {
        PsiBuilder.Marker representation = mark();
        consumeAccessModifier();
        boolean isEnum = isEnumCase();
        while (true) {
            if (!is(BAR) && !is(IDENT) && !is(LBRACK_LESS) && !is(LPAREN)) {
                break;
            }
            if (isEnum) {
                parseEnumCase();
            }
            else {
                parseUnionCase();
            }
            if (!is(BAR)) {
                break;
            }
        }
        representation.done(isEnum ? ENUM_REPRESENTATION : UNION_REPRESENTATION);
    }

    private boolean isEnumCase() {
        PsiBuilder.Marker probe = mark();
        try {
            consume(BAR);
            parseAttributeLists();
            if (!consume(IDENT)) {
                return false;
            }
            return is(EQUALS);
        }
        finally {
            probe.rollbackTo();
        }
    }

    private void parseEnumCase() {
        PsiBuilder.Marker enumCase = mark();
        consume(BAR);
        parseAttributeLists();
        if (!consume(IDENT)) {
            error("Identifier expected");
        }
        if (expect(EQUALS, "'=' expected")) {
            if (parseDeclExpr() == null) {
                error("Expression expected");
            }
        }
        enumCase.done(ENUM_CASE_DECLARATION);
    }

    private void parseUnionCase() {
        PsiBuilder.Marker unionCase = mark();
        consume(BAR);
        parseAttributeLists();
        consumeAccessModifier();
        if (is(LPAREN) && (isAt(1, COLON_COLON) && isAt(2, RPAREN) || isAt(1, LBRACK) && isAt(2, RBRACK) && isAt(3, RPAREN))) {
            while (!is(RPAREN)) {
                advance();
            }
            advance();
        }
        else if (!consume(IDENT)) {
            error("Identifier expected");
        }
        if (consume(OF)) {
            parseCaseFields(UNION_CASE_FIELD_DECLARATION);
        }
        else if (is(COLON)) {
            advance();
            if (parseSignatureTypeUsage() == null) {
                error("Type expected");
            }
        }
        unionCase.done(UNION_CASE_DECLARATION);
    }

    private void parseCaseFields(IElementType fieldType) {
        PsiBuilder.Marker list = mark();
        do {
            PsiBuilder.Marker field = mark();
            parseAttributeLists();
            if (is(IDENT) && isAt(1, COLON)) {
                advance();
                advance();
            }
            // field types are separated by '*': a field type is not a tuple
            if (parsePostfixTypeUsage() == null) {
                field.drop();
                error("Type expected");
                break;
            }
            field.done(fieldType);
        }
        while (consume(STAR));
        list.done(UNION_CASE_FIELD_DECLARATION_LIST);
    }

    private void parseRecordRepresentation() {
        PsiBuilder.Marker representation = mark();
        consumeAccessModifier();
        advance();
        PsiBuilder.Marker list = mark();
        while (!myBuilder.eof() && !is(RBRACE)) {
            if (is(DOT_DOT_DOT)) {
                PsiBuilder.Marker spread = mark();
                advance();
                parseTypeReferenceName(true);
                consumeSeps();
                spread.done(DECLARATION_TYPE_SPREAD);
                continue;
            }
            PsiBuilder.Marker field = mark();
            parseAttributeLists();
            consume(MUTABLE);
            consumeAccessModifier();
            if (!consume(IDENT)) {
                field.drop();
                error("Field expected");
                break;
            }
            if (expect(COLON, "':' expected")) {
                if (parseTypeUsage() == null) {
                    error("Type expected");
                }
            }
            field.done(RECORD_FIELD_DECLARATION);
            if (isSepsBefore(RBRACE)) {
                break;
            }
            consumeSeps();
        }
        list.done(RECORD_MEMBER_DECLARATION_LIST);
        consumeSeps();
        expect(RBRACE, "'}' expected");
        representation.done(RECORD_REPRESENTATION);
    }

    /**
     * Separators right before {@code kind}.
     */
    private boolean isSepsBefore(FSharpLexFilterKind kind) {
        int steps = 0;
        while (kind(steps) == SEMICOLON || kind(steps) == OBLOCKSEP) {
            steps++;
        }
        return steps > 0 && kind(steps) == kind;
    }

    // ------------------------------------------------------------------------
    // type members

    private boolean isTypeMemberStart() {
        FSharpLexFilterKind kind = memberKeywordAfterAttributes();
        if (MEMBER_KEYWORDS.contains(kind) || kind == ABSTRACT || kind == NEW || kind == INHERIT || kind == VAL || kind == LET
            || kind == OLET || kind == DO || kind == ODO || kind == STATIC || kind == EXTERN || kind == OINTERFACE_MEMBER) {
            return true;
        }
        if (ACCESS.contains(kind)) {
            PsiBuilder.Marker probe = mark();
            parseAttributeLists();
            advance();
            boolean constructor = is(NEW);
            probe.rollbackTo();
            return constructor;
        }
        return false;
    }

    /**
     * {@code classDefnBlock}: members, possibly in a virtual block.
     */
    private void parseTypeMembersOrBlock() {
        if (is(OBLOCKBEGIN)) {
            int end = matchEnd();
            advance();
            parseTypeMembers();
            if (end >= 0) {
                skipTo(end);
            }
            else {
                skipToBlockEnd(OBLOCKBEGIN, OBLOCKEND);
            }
            return;
        }
        parseTypeMembers();
    }

    /**
     * {@code classDefnMembers}: members separated by optional separators.
     */
    protected void parseTypeMembers() {
        while (true) {
            consumeSeps();
            if (myBuilder.eof() || !isTypeMemberStart()) {
                return;
            }
            int position = myBuilder.position();
            parseTypeMember();
            if (position == myBuilder.position()) {
                return;
            }
        }
    }

    @Override
    protected void parseObjectExpressionMembers() {
        parseTypeMembersOrBlock();
    }

    @Override
    protected void parseInterfaceImplementation() {
        PsiBuilder.Marker implementation = mark();
        advance();
        parseTypeReferenceName(true);
        if (is(WITH)) {
            parseAugmentation();
        }
        implementation.done(INTERFACE_IMPLEMENTATION);
    }

    private void parseTypeMember() {
        PsiBuilder.Marker member = mark();
        parseAttributeLists();
        boolean isStatic = consume(STATIC);

        if (is(LET) || is(OLET) || is(DO) || is(ODO)) {
            member.rollbackTo();
            parseTypeLetOrDo();
            return;
        }

        if (is(EXTERN)) {
            member.rollbackTo();
            parseExtern();
            return;
        }

        if (is(MEMBER_KEYWORDS)) {
            advance();
            consumeAccessModifier();
            if (is(VAL)) {
                advance();
                consumeAccessModifier();
                if (!consume(IDENT)) {
                    error("Identifier expected");
                }
                if (is(COLON)) {
                    advance();
                    parseTypeUsage();
                }
                int end = -1;
                if (is(EQUALS)) {
                    end = parseMemberBodyWithoutEnd();
                }
                else {
                    error("'=' expected");
                }
                if (is(WITH) || is(OWITH)) {
                    parseAccessors(true);
                }
                if (end >= 0) {
                    skipTo(end);
                }
                member.done(AUTO_PROPERTY_DECLARATION);
                return;
            }

            consume(INLINE);
            consumeAccessModifier();
            if ((is(IDENT) || is(UNDERSCORE)) && isAt(1, DOT) && !isStatic) {
                PsiBuilder.Marker self = mark();
                boolean wild = is(UNDERSCORE);
                advance();
                self.done(wild ? WILD_SELF_ID : NAMED_SELF_ID);
                advance();
            }
            parseMemberName();
            if (isPostfixTypeParameters()) {
                parsePostfixTypeParameterDeclarationList(TYPE_PARAMETER_OF_METHOD_DECLARATION);
            }
            if (mySignature) {
                if (is(COLON)) {
                    parseReturnTypeInfo(true);
                }
                else {
                    error("':' expected");
                }
                if (is(WITH) || is(OWITH)) {
                    parseAccessors(true);
                }
                member.done(MEMBER_SIGNATURE);
                return;
            }
            if (is(WITH) || is(OWITH)) {
                parseAccessors(false);
                member.done(MEMBER_DECLARATION);
                return;
            }
            parseParameterPatterns();
            if (is(COLON)) {
                parseReturnTypeInfo();
            }
            if (is(WITH) || is(OWITH)) {
                parseAccessors(true);
                member.done(MEMBER_SIGNATURE);
                return;
            }
            if (is(EQUALS)) {
                parseMemberBody();
            }
            else {
                error("'=' expected");
            }
            member.done(MEMBER_DECLARATION);
            return;
        }

        if (is(ABSTRACT)) {
            advance();
            consume(MEMBER);
            consumeAccessModifier();
            consume(INLINE);
            parseMemberName();
            if (isPostfixTypeParameters()) {
                parsePostfixTypeParameterDeclarationList(TYPE_PARAMETER_OF_METHOD_DECLARATION);
            }
            if (is(COLON)) {
                parseReturnTypeInfo(true);
            }
            else {
                error("':' expected");
            }
            if (is(WITH) || is(OWITH)) {
                parseAccessors(true);
            }
            consume(ODECLEND);
            member.done(ABSTRACT_MEMBER_DECLARATION);
            return;
        }

        if (is(VAL)) {
            advance();
            consume(MUTABLE);
            consumeAccessModifier();
            if (!consume(IDENT)) {
                error("Identifier expected");
            }
            if (expect(COLON, "':' expected")) {
                parseTypeUsage();
            }
            member.done(VAL_FIELD_DECLARATION);
            return;
        }

        if (is(NEW) || is(ACCESS) && isAt(1, NEW)) {
            consumeAccessModifier();
            advance();
            if (is(COLON)) {
                parseReturnTypeInfo(mySignature);
                member.done(CONSTRUCTOR_SIGNATURE);
                return;
            }
            PsiBuilder.Marker parameters = mark();
            if (parseAtomicPattern(true) == null) {
                error("Parameters expected");
            }
            parameters.done(PARAMETERS_PATTERN_DECLARATION);
            if (is(AS)) {
                PsiBuilder.Marker self = mark();
                advance();
                consume(IDENT);
                self.done(CTOR_SELF_ID);
            }
            if (is(EQUALS)) {
                parseMemberBody();
            }
            else {
                error("'=' expected");
            }
            member.done(SECONDARY_CONSTRUCTOR_DECLARATION);
            return;
        }

        if (is(INHERIT)) {
            advance();
            parseTypeReferenceName(true);
            boolean arguments = consumeHighPrecedenceApp();
            if (arguments || canStartAtomicExprAfterType()) {
                parseAtomicExpression();
                if (is(AS)) {
                    advance();
                    consume(IDENT);
                }
                member.done(TYPE_INHERIT);
            }
            else {
                member.done(INTERFACE_INHERIT);
            }
            return;
        }

        if (is(OINTERFACE_MEMBER)) {
            member.rollbackTo();
            parseInterfaceImplementation();
            return;
        }

        member.rollbackTo();
    }

    /**
     * {@code = body} of a member: the member body context closes with {@code ODECLEND}.
     */
    private void parseMemberBody() {
        int end = parseMemberBodyWithoutEnd();
        if (end >= 0) {
            skipTo(end);
        }
    }

    /**
     * @return position of the member end
     */
    private int parseMemberBodyWithoutEnd() {
        int end = matchEnd();
        advance();
        if (parseTypedSequentialExprBlock() == null) {
            error("Expression expected");
        }
        return end;
    }

    private void parseTypeLetOrDo() {
        PsiBuilder.Marker probe = mark();
        parseAttributeLists();
        consume(STATIC);
        boolean isDo = is(DO) || is(ODO);
        probe.rollbackTo();

        if (isDo) {
            parseDoStatement();
            return;
        }

        PsiBuilder.Marker declaration = mark();
        parseLetBindings(TOP_BINDING, false);
        declaration.done(LET_BINDINGS_DECLARATION);
    }

    private void parseMemberName() {
        if (isIdentOrOp()) {
            parseIdentOrOp(false);
            return;
        }
        if (is(OPERATOR_NAMES)) {
            advance();
            return;
        }
        error("Member name expected");
    }

    /**
     * {@code with get() = ... and set v = ...} or {@code with get, set}
     */
    private void parseAccessors(boolean signature) {
        int end = is(OWITH) ? matchEnd() : -1;
        advance();
        while (true) {
            PsiBuilder.Marker accessor = mark();
            parseAttributeLists();
            consume(INLINE);
            consumeAccessModifier();
            if (!consume(IDENT)) {
                accessor.drop();
                error("'get' or 'set' expected");
                break;
            }
            if (!signature && !is(COMMA) && !is(AND)) {
                parseParameterPatterns();
                if (is(COLON)) {
                    parseReturnTypeInfo();
                }
                if (expect(EQUALS, "'=' expected")) {
                    if (parseTypedSequentialExprBlock() == null) {
                        error("Expression expected");
                    }
                }
            }
            accessor.done(ACCESSOR_DECLARATION);
            if (!consume(AND) && !consume(COMMA)) {
                break;
            }
        }
        if (end >= 0) {
            skipTo(end);
        }
        consume(ODECLEND);
    }
}
