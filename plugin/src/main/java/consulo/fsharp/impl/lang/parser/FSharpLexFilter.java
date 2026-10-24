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

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static consulo.fsharp.impl.lang.parser.FSharpLexFilterKind.*;

/**
 * Port of the F# compiler {@code LexFilter.fs} (offside rule): consumes the real tokens of a file and produces the token
 * stream the compiler grammar is written against - real tokens (sometimes retyped, like {@code LET} to {@code OLET})
 * interleaved with virtual ones ({@code OBLOCKBEGIN}, {@code OBLOCKSEP}, {@code ODECLEND}, ...).
 * <p>
 * Differences from the compiler: no diagnostics, no {@code *_COMING_SOON} recovery tokens, token splitting is mostly
 * done by our lexer already (type application closing {@code >>}, {@code 1..2}, {@code @>.}).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public final class FSharpLexFilter {
    public record Pos(int line, int col) implements Comparable<Pos> {
        public Pos shift(int columns) {
            return new Pos(line, col + columns);
        }

        @Override
        public int compareTo(Pos o) {
            return line != o.line ? Integer.compare(line, o.line) : Integer.compare(col, o.col);
        }

        @Override
        public String toString() {
            return "(" + line + "," + col + ")";
        }
    }

    public static final class Token {
        private final FSharpLexFilterKind myKind;
        private final String myText;
        private final Pos myStart;
        private final Pos myEnd;
        private final Pos myLastTokenPos;
        private final int myRealIndex;
        private final int myRealCount;
        private final boolean myFlag;
        private final @Nullable Token myWrapped;
        /**
         * Virtual block opener and its end token, see {@link #matches}.
         */
        private @Nullable Token myMatch;

        private Token(FSharpLexFilterKind kind,
                      String text,
                      Pos start,
                      Pos end,
                      Pos lastTokenPos,
                      int realIndex,
                      int realCount,
                      boolean flag,
                      @Nullable Token wrapped) {
            myKind = kind;
            myText = text;
            myStart = start;
            myEnd = end;
            myLastTokenPos = lastTokenPos;
            myRealIndex = realIndex;
            myRealCount = realCount;
            myFlag = flag;
            myWrapped = wrapped;
        }

        public FSharpLexFilterKind kind() {
            return myKind;
        }

        /**
         * Operator text for operator kinds.
         */
        public String text() {
            return myText;
        }

        public Pos start() {
            return myStart;
        }

        public Pos end() {
            return myEnd;
        }

        /**
         * @return index of the first real token this token covers, or -1 for a virtual token
         */
        public int realIndex() {
            return myRealIndex;
        }

        /**
         * @return number of real tokens covered: 0 for virtual, 2 for merged tokens like {@code else if} or {@code -1}
         */
        public int realCount() {
            return myRealCount;
        }

        public boolean isVirtual() {
            return myRealCount == 0;
        }

        /**
         * {@code LESS}/{@code GREATER}: type application; {@code LQUOTE}/{@code RQUOTE}: untyped quotation.
         */
        public boolean flag() {
            return myFlag;
        }

        private Token retype(FSharpLexFilterKind kind) {
            return new Token(kind, myText, myStart, myEnd, myLastTokenPos, myRealIndex, myRealCount, myFlag, null);
        }

        private Token retype(FSharpLexFilterKind kind, boolean flag) {
            return new Token(kind, myText, myStart, myEnd, myLastTokenPos, myRealIndex, myRealCount, flag, null);
        }

        private Token virtualCopy(FSharpLexFilterKind kind) {
            return new Token(kind, "", myStart, myEnd, myLastTokenPos, -1, 0, false, null);
        }

        private Token dummy() {
            return new Token(ODUMMY, "", myStart, myEnd, myLastTokenPos, -1, 0, false, this);
        }

        @Override
        public String toString() {
            return myKind + " " + myStart + "--" + myEnd;
        }
    }

    /**
     * Real token: kind (see {@link FSharpLexFilterKinds}) and offsets.
     */
    public record RealToken(FSharpLexFilterKind kind, String text, boolean flag, int start, int end) {
    }

    private enum AddBlockEnd {
        ADD,
        NO,
        ONE_SIDED
    }

    private enum C {
        LET_DECL,
        IF,
        TRY,
        FUN,
        FUNCTION,
        WITH_AS_LET,
        WITH_AS_AUGMENT,
        MATCH,
        FOR,
        WHILE,
        WHEN,
        VANILLA,
        THEN,
        ELSE,
        DO,
        INTERFACE_HEAD,
        TYPE_DEFNS,
        NAMESPACE_HEAD,
        MODULE_HEAD,
        MEMBER_HEAD,
        MEMBER_BODY,
        MODULE_BODY,
        NAMESPACE_BODY,
        EXCEPTION,
        PAREN,
        SEQ_BLOCK,
        MATCH_CLAUSES
    }

    private static final class Ctx {
        private final C myKind;
        private final Pos myStart;
        /**
         * LetDecl: blockLet, Vanilla: isLongIdentEquals, ModuleBody: wholeFile, MatchClauses: leadingBar, ModuleHead: isNested,
         * SeqBlock: first in block.
         */
        private final boolean myFlag;
        /**
         * Paren: the opening token, NamespaceHead/ModuleHead: previous token.
         */
        private final @Nullable Token myToken;
        private final boolean myLexingModuleAttributes;
        private final AddBlockEnd myAddBlockEnd;
        private final @Nullable Pos myEqualsEndPos;
        /**
         * Token that opened the context for the parser ({@code OBLOCKBEGIN}, {@code OLET}, {@code OFUN}, ...).
         */
        private @Nullable Token myOpen;

        private Ctx(C kind, Pos start, boolean flag, @Nullable Token token, boolean lexingModuleAttributes, AddBlockEnd addBlockEnd,
                    @Nullable Pos equalsEndPos) {
            myKind = kind;
            myStart = start;
            myFlag = flag;
            myToken = token;
            myLexingModuleAttributes = lexingModuleAttributes;
            myAddBlockEnd = addBlockEnd;
            myEqualsEndPos = equalsEndPos;
        }

        private static Ctx of(C kind, Pos start) {
            return new Ctx(kind, start, false, null, false, AddBlockEnd.NO, null);
        }

        private static Ctx of(C kind, Pos start, boolean flag) {
            return new Ctx(kind, start, flag, null, false, AddBlockEnd.NO, null);
        }

        private static Ctx paren(Token token, Pos start) {
            return new Ctx(C.PAREN, start, false, token, false, AddBlockEnd.NO, null);
        }

        private static Ctx seqBlock(boolean first, Pos start, AddBlockEnd addBlockEnd) {
            return new Ctx(C.SEQ_BLOCK, start, first, null, false, addBlockEnd, null);
        }

        private static Ctx head(C kind, Pos start, Token prev, boolean lexingModuleAttributes, boolean nested) {
            return new Ctx(kind, start, nested, prev, lexingModuleAttributes, AddBlockEnd.NO, null);
        }

        private static Ctx typeDefns(Pos start, @Nullable Pos equalsEndPos) {
            return new Ctx(C.TYPE_DEFNS, start, false, null, false, AddBlockEnd.NO, equalsEndPos);
        }

        private int col() {
            return myStart.col();
        }

        private Ctx seqBlockWithFirst(boolean first) {
            Ctx ctx = seqBlock(first, myStart, myAddBlockEnd);
            ctx.myOpen = myOpen;
            return ctx;
        }

        private Token token() {
            Token token = myToken;
            if (token == null) {
                throw new IllegalStateException(myKind.name());
            }
            return token;
        }

        @Override
        public String toString() {
            return myKind + myStart.toString();
        }
    }

    private record Limit(Pos pos, int col) {
    }

    private final RealToken[] myReal;
    private final int[] myLineStarts;
    private final int myTextLength;

    private int myNextReal;
    private Pos myLastRealEnd = new Pos(1, 0);

    private final Deque<Token> myDelayed = new ArrayDeque<>();
    private int myNoProcessingCount;
    private final List<Ctx> myStack = new ArrayList<>();
    private boolean myPrevWasAtomicEnd;
    private int mySteps;
    private final int myMaxSteps;

    private FSharpLexFilter(CharSequence text, List<RealToken> real) {
        myReal = real.toArray(new RealToken[0]);
        myTextLength = text.length();
        int[] starts = new int[16];
        int count = 0;
        starts[count++] = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' && (i + 1 >= text.length() || text.charAt(i + 1) != '\n')) {
                if (count == starts.length) {
                    starts = Arrays.copyOf(starts, count * 2);
                }
                starts[count++] = i + 1;
            }
        }
        myLineStarts = Arrays.copyOf(starts, count);
        myMaxSteps = 64 * (myReal.length + 16);
    }

    public static List<Token> filter(CharSequence text, List<RealToken> real) {
        return new FSharpLexFilter(text, real).run();
    }

    private List<Token> run() {
        List<Token> result = new ArrayList<>();
        peekInitial();
        try {
            while (true) {
                Token token = hwTokenFetch(true);
                if (token.kind() != ODUMMY) {
                    result.add(token);
                }
                if (token.kind() == EOF) {
                    break;
                }
            }
        }
        catch (StepLimitException e) {
            // should not happen - pass the rest of real tokens as is
            int next = 0;
            for (Token token : result) {
                if (!token.isVirtual()) {
                    next = Math.max(next, token.realIndex() + token.realCount());
                }
            }
            for (int i = next; i < myReal.length; i++) {
                result.add(realToken(i));
            }
            result.add(eofToken());
        }
        return result;
    }

    private static final class StepLimitException extends RuntimeException {
        private StepLimitException() {
            super(null, null, false, false);
        }
    }

    // ------------------------------------------------------------------------
    // positions, raw stream

    private Pos pos(int offset) {
        int index = Arrays.binarySearch(myLineStarts, offset);
        int line = index >= 0 ? index : -index - 2;
        return new Pos(line + 1, offset - myLineStarts[line]);
    }

    private Token realToken(int index) {
        RealToken real = myReal[index];
        Pos last = index == 0 ? new Pos(1, 0) : pos(myReal[index - 1].end());
        return new Token(real.kind(), real.text(), pos(real.start()), pos(real.end()), last, index, 1, real.flag(), null);
    }

    private Token eofToken() {
        Pos end = pos(myTextLength);
        Pos last = myReal.length == 0 ? new Pos(1, 0) : pos(myReal[myReal.length - 1].end());
        return new Token(EOF, "", end, end, last, -1, 0, false, null);
    }

    private static Pos startPos(Token token) {
        // EOF token is processed as if on column -1, this forces the closure of all contexts
        return token.kind() == EOF ? new Pos(token.start().line(), -1) : token.start();
    }

    private Token popNextTokenTup() {
        if (!myDelayed.isEmpty()) {
            return myDelayed.pop();
        }
        if (myNextReal < myReal.length) {
            Token token = realToken(myNextReal++);
            myLastRealEnd = token.end();
            return token;
        }
        return eofToken();
    }

    private void delayToken(Token token) {
        myDelayed.push(token);
    }

    private void delayTokenNoProcessing(Token token) {
        delayToken(token);
        myNoProcessingCount++;
    }

    private Token peekNextTokenTup() {
        Token token = popNextTokenTup();
        delayToken(token);
        return token;
    }

    private FSharpLexFilterKind peekNextToken() {
        return peekNextTokenTup().kind();
    }

    private void peekInitial() {
        Token initial = popNextTokenTup();
        delayToken(initial);
        myStack.add(Ctx.seqBlock(true, startPos(initial), AddBlockEnd.NO));
    }

    // ------------------------------------------------------------------------
    // token classes

    private static boolean isInfix(Token token) {
        return switch (token.kind()) {
            case COMMA, BAR_BAR, AMP_AMP, AMP, OR, INFIX_BAR_OP, INFIX_AMP_OP, INFIX_COMPARE_OP, DOLLAR, INFIX_AT_HAT_OP, PLUS_MINUS_OP,
                 COLON_COLON, COLON_GREATER, COLON_QMARK_GREATER, COLON_EQUALS, MINUS, STAR, INFIX_STAR_DIV_MOD_OP, INFIX_STAR_STAR_OP,
                 QMARK_QMARK -> true;
            default -> false;
        };
    }

    private static int infixTokenLength(Token token) {
        return switch (token.kind()) {
            case COMMA, AMP, OR, DOLLAR, MINUS, STAR, BAR, LESS, GREATER, EQUALS -> 1;
            case QMARK_QMARK, COLON_GREATER, COLON_COLON, COLON_EQUALS, BAR_BAR, AMP_AMP -> 2;
            case INFIX_BAR_OP, INFIX_AMP_OP, INFIX_COMPARE_OP, INFIX_AT_HAT_OP, PLUS_MINUS_OP, INFIX_STAR_DIV_MOD_OP, INFIX_STAR_STAR_OP ->
                token.text().length();
            case COLON_QMARK_GREATER -> 3;
            default -> 1;
        };
    }

    private static boolean isLExprParen(Token token) {
        return switch (token.kind()) {
            case BEGIN, LPAREN, LBRACE, LBRACE_BAR, LBRACK, LBRACK_BAR, LQUOTE -> true;
            case LESS -> token.flag();
            default -> false;
        };
    }

    private static boolean isRExprParen(Token token) {
        return switch (token.kind()) {
            case END, RPAREN, RBRACE, BAR_RBRACE, RBRACK, BAR_RBRACK, RQUOTE -> true;
            case GREATER -> token.flag();
            default -> false;
        };
    }

    private static Token unwrap(Token token) {
        Token result = token;
        while (result.kind() == ODUMMY && result.myWrapped != null) {
            result = result.myWrapped;
        }
        return result;
    }

    private static boolean isInsertedEnd(FSharpLexFilterKind kind) {
        return kind == ORIGHT_BLOCK_END || kind == OBLOCKEND || kind == ODECLEND;
    }

    private static boolean isIfBlockContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == THEN || kind == ELSE || kind == ELIF || kind == END || kind == RPAREN || isInsertedEnd(kind);
    }

    private static boolean isMatchBlockContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == WITH || isInsertedEnd(kind);
    }

    private static boolean isTryBlockContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == FINALLY || kind == WITH || isInsertedEnd(kind);
    }

    private static boolean isThenBlockContinuator(Token token) {
        return isInsertedEnd(unwrap(token).kind());
    }

    private static boolean isDoneContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == DONE || isInsertedEnd(kind);
    }

    private static boolean isInterfaceContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == END || isInsertedEnd(kind);
    }

    private static boolean isNamespaceContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind != EOF && kind != NAMESPACE;
    }

    private static boolean isTypeContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == RBRACE || kind == WITH || kind == BAR || kind == AND || kind == END || isInsertedEnd(kind);
    }

    private static boolean isLetContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == AND || isInsertedEnd(kind);
    }

    private static boolean isTypeSeqBlockElementContinuator(Token token) {
        FSharpLexFilterKind kind = unwrap(token).kind();
        return kind == BAR || kind == BAR_RBRACE || kind == OBLOCKBEGIN || isInsertedEnd(kind);
    }

    private static boolean isSeqBlockElementContinuator(Token token) {
        Token t = unwrap(token);
        if (isInfix(t)) {
            return true;
        }
        return switch (t.kind()) {
            case END, AND, WITH, THEN, RPAREN, RBRACE, BAR_RBRACE, RBRACK, BAR_RBRACK, RQUOTE -> true;
            case GREATER -> t.flag();
            default -> isInsertedEnd(t.kind());
        };
    }

    private static boolean isWithAugmentBlockContinuator(Token token) {
        return unwrap(token).kind() == END;
    }

    private static boolean isAtomicExprEndToken(FSharpLexFilterKind kind) {
        return switch (kind) {
            case IDENT, INT8, INT16, INT32, INT64, NATIVEINT, UINT8, UINT16, UINT32, UINT64, UNATIVEINT, DECIMAL, BIGNUM, STRING,
                 BYTEARRAY, CHAR, IEEE32, IEEE64, RPAREN, RBRACK, RBRACE, BAR_RBRACE, BAR_RBRACK, END, NULL, FALSE, TRUE, UNDERSCORE -> true;
            default -> false;
        };
    }

    private static boolean isNumericForMerge(FSharpLexFilterKind kind) {
        return switch (kind) {
            case INT8, INT16, INT32, INT64, NATIVEINT, IEEE32, IEEE64, DECIMAL, BIGNUM -> true;
            default -> false;
        };
    }

    private static boolean parenTokensBalance(Token t1, Token t2) {
        FSharpLexFilterKind k1 = t1.kind();
        FSharpLexFilterKind k2 = t2.kind();
        return switch (k1) {
            case LPAREN -> k2 == RPAREN;
            case LBRACE -> k2 == RBRACE;
            case LBRACE_BAR -> k2 == BAR_RBRACE;
            case LBRACK -> k2 == RBRACK;
            case INTERFACE, CLASS, SIG, STRUCT, BEGIN -> k2 == END;
            case INTERP_STRING_BEGIN_PART, INTERP_STRING_PART -> k2 == INTERP_STRING_END || k2 == INTERP_STRING_PART;
            case LBRACK_BAR -> k2 == BAR_RBRACK;
            case LESS -> t1.flag() && k2 == GREATER && t2.flag();
            case LQUOTE -> k2 == RQUOTE && t1.flag() == t2.flag();
            default -> false;
        };
    }

    private static boolean is(@Nullable Ctx ctx, C kind) {
        return ctx != null && ctx.myKind == kind;
    }

    private static boolean isParen(@Nullable Ctx ctx, FSharpLexFilterKind... kinds) {
        if (ctx == null || ctx.myKind != C.PAREN) {
            return false;
        }
        FSharpLexFilterKind kind = ctx.token().kind();
        for (FSharpLexFilterKind k : kinds) {
            if (k == kind) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOneOf(FSharpLexFilterKind kind, FSharpLexFilterKind... kinds) {
        for (FSharpLexFilterKind k : kinds) {
            if (k == kind) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // context stack

    private @Nullable Ctx ctx(int depth) {
        int index = myStack.size() - 1 - depth;
        return index >= 0 ? myStack.get(index) : null;
    }

    private Ctx head() {
        return myStack.get(myStack.size() - 1);
    }

    private static Limit limit(Ctx ctx, int delta) {
        return new Limit(ctx.myStart, ctx.col() + delta);
    }

    private Limit undentationLimit(Ctx newCtx, boolean initialStrict, int initialDepth) {
        boolean strict = initialStrict;
        int d = initialDepth;
        while (true) {
            Ctx c0 = ctx(d);
            if (c0 == null) {
                return new Limit(newCtx.myStart, -1);
            }
            Ctx c1 = ctx(d + 1);
            Ctx c2 = ctx(d + 2);
            Ctx c3 = ctx(d + 3);

            // ignore Vanilla because a SeqBlock is always coming
            if (c0.myKind == C.VANILLA) {
                d++;
                continue;
            }
            boolean newFirstSeqBlock = newCtx.myKind == C.SEQ_BLOCK && newCtx.myFlag;
            if (newFirstSeqBlock && c0.myKind == C.DO && is(c1, C.SEQ_BLOCK) && (is(c2, C.TYPE_DEFNS) || is(c2, C.MODULE_BODY))) {
                return limit(c0, 1);
            }
            if (newFirstSeqBlock && c0.myKind == C.WITH_AS_AUGMENT && c1 != null && c1.myKind == C.TYPE_DEFNS) {
                return limit(c1, 1);
            }
            if (!strict && (c0.myKind == C.SEQ_BLOCK || c0.myKind == C.PAREN)) {
                d++;
                continue;
            }
            // 'begin match' / '(match' limited by minimum of two
            if (c0.myKind == C.MATCH && is(c1, C.SEQ_BLOCK) && isParen(c2, BEGIN, LPAREN) && c2 != null) {
                return c0.col() <= c2.col() ? limit(c0, 0) : limit(c2, 0);
            }
            if (c0.myKind == C.MATCH_CLAUSES && is(c1, C.MATCH) && is(c2, C.SEQ_BLOCK) && isParen(c3, BEGIN, LPAREN) && c3 != null) {
                return c0.col() <= c3.col() ? limit(c0, 0) : limit(c3, 0);
            }
            // 'let ... = function' limited by 'let', precisely
            if (newCtx.myKind == C.MATCH_CLAUSES && c0.myKind == C.FUNCTION && is(c1, C.SEQ_BLOCK) && c2 != null && c2.myKind == C.LET_DECL) {
                return limit(c2, 0);
            }
            if (newCtx.myKind == C.MATCH_CLAUSES && c0.myKind == C.FUNCTION) {
                strict = false;
                d++;
                continue;
            }
            // 'try ... with' limited by 'try', 'match ... with' limited by 'match'
            if (c0.myKind == C.MATCH_CLAUSES && c1 != null && (c1.myKind == C.TRY || c1.myKind == C.MATCH)) {
                return limit(c1, 0);
            }
            // 'fun ->' places no limit until we hit a CtxtLetDecl etc...
            if (c0.myKind == C.FUN) {
                strict = false;
                d++;
                continue;
            }
            if (c0.myKind == C.PAREN && isLExprParen(c0.token())) {
                strict = false;
                d++;
                continue;
            }
            if (c0.myKind == C.SEQ_BLOCK && c1 != null && c1.myKind == C.PAREN && isLExprParen(c1.token())) {
                strict = false;
                d += 2;
                continue;
            }
            // MAJOR PERMITTED UNDENTATION: if x then y else <newline> let x = ...
            if (newCtx.myKind == C.SEQ_BLOCK && c0.myKind == C.ELSE && c1 != null && c1.myKind == C.IF) {
                return limit(c1, 0);
            }
            if (newCtx.myKind == C.WITH_AS_AUGMENT
                && (c0.myKind == C.INTERFACE_HEAD || c0.myKind == C.MEMBER_HEAD || c0.myKind == C.EXCEPTION || c0.myKind == C.TYPE_DEFNS)) {
                return limit(c0, 0);
            }
            if (c0.myKind == C.WITH_AS_AUGMENT || c0.myKind == C.THEN || c0.myKind == C.ELSE || c0.myKind == C.DO) {
                strict = false;
                d++;
                continue;
            }
            if (c0.myKind == C.FUNCTION) {
                strict = false;
                d++;
                continue;
            }
            // 'module ... : sig' / 'module ... : struct' limited by 'module'
            if (isParen(c0, SIG, STRUCT) && is(c1, C.SEQ_BLOCK) && c2 != null && c2.myKind == C.MODULE_BODY && !c2.myFlag) {
                return limit(c2, 1);
            }
            if (isParen(c0, LBRACK_LESS) && is(c1, C.SEQ_BLOCK) && is(c2, C.ELSE) && c3 != null && c3.myKind == C.IF) {
                return limit(c3, 1);
            }
            // 'type C = class ... ' limited by 'type'
            if (isParen(c0, CLASS, STRUCT, INTERFACE) && is(c1, C.SEQ_BLOCK) && c2 != null && c2.myKind == C.TYPE_DEFNS) {
                return limit(c2, 1);
            }
            // 'static member P with get() = ' limited by 'static', likewise others
            if (c0.myKind == C.WITH_AS_LET && c1 != null && c1.myKind == C.MEMBER_HEAD) {
                return limit(c1, 1);
            }
            if ((newCtx.myKind == C.IF || newCtx.myKind == C.ELSE || newCtx.myKind == C.THEN) && c0.myKind == C.IF) {
                return limit(c0, 0);
            }
            if (newCtx.myKind == C.DO && (c0.myKind == C.FOR || c0.myKind == C.WHILE)) {
                return limit(c0, 0);
            }
            switch (c0.myKind) {
                case INTERFACE_HEAD, NAMESPACE_HEAD, MODULE_HEAD, EXCEPTION, IF, WITH_AS_LET, LET_DECL, MEMBER_HEAD, MEMBER_BODY -> {
                    return limit(c0, 1);
                }
                case MODULE_BODY -> {
                    return limit(c0, c0.myFlag ? 0 : 1);
                }
                default -> {
                    return limit(c0, 0);
                }
            }
        }
    }

    private boolean tryPushCtxt(boolean strict, boolean ignoreIndent, Ctx newCtx) {
        boolean isCorrectIndent;
        if (ignoreIndent) {
            isCorrectIndent = true;
        }
        else if (newCtx.myKind == C.VANILLA || isParen(newCtx, INTERP_STRING_BEGIN_PART, INTERP_STRING_PART)) {
            isCorrectIndent = true;
        }
        else {
            Limit limit = undentationLimit(newCtx, true, 0);
            isCorrectIndent = newCtx.col() >= limit.col();
        }
        if (strict && !isCorrectIndent) {
            return false;
        }
        myStack.add(newCtx);
        return true;
    }

    private void pushCtxt(Ctx newCtx) {
        tryPushCtxt(false, false, newCtx);
    }

    private void popCtxt() {
        if (myStack.isEmpty()) {
            return;
        }
        Ctx head = myStack.remove(myStack.size() - 1);
        // for CtxtMatchClauses, also pop the CtxtMatch, if present
        if (head.myKind == C.MATCH_CLAUSES && !myStack.isEmpty() && head().myKind == C.MATCH) {
            popCtxt();
        }
    }

    private void replaceCtxt(Ctx ctx) {
        popCtxt();
        pushCtxt(ctx);
    }

    private void replaceCtxtIgnoreIndent(Ctx ctx) {
        popCtxt();
        tryPushCtxt(false, true, ctx);
    }

    private void pushCtxtSeqBlock(Token fallbackToken, AddBlockEnd addBlockEnd) {
        pushCtxtSeqBlockAt(true, true, fallbackToken, peekNextTokenTup(), addBlockEnd);
    }

    private void tryPushCtxtSeqBlock(Token fallbackToken, AddBlockEnd addBlockEnd) {
        pushCtxtSeqBlockAt(true, false, fallbackToken, peekNextTokenTup(), addBlockEnd);
    }

    private void pushCtxtSeqBlockAt(boolean strict, boolean useFallback, Token fallbackToken, Token token, AddBlockEnd addBlockEnd) {
        boolean pushed = tryPushCtxt(strict, false, Ctx.seqBlock(true, startPos(token), addBlockEnd));
        if (!pushed && useFallback) {
            // the upcoming token isn't sufficiently indented to start the new context
            pushCtxt(Ctx.seqBlock(false, startPos(fallbackToken), addBlockEnd));
        }
        if ((pushed || useFallback) && addBlockEnd == AddBlockEnd.ADD) {
            Token begin = (pushed ? token : fallbackToken).virtualCopy(OBLOCKBEGIN);
            head().myOpen = begin;
            delayToken(begin);
        }
    }

    // ------------------------------------------------------------------------
    // adjacency

    private static boolean isAdjacent(Token left, Token right) {
        return left.end().equals(startPos(right));
    }

    private boolean nextTokenIsAdjacentLBrack(Token token) {
        Token lookahead = peekNextTokenTup();
        return lookahead.kind() == LBRACK && isAdjacent(token, lookahead);
    }

    private boolean nextTokenIsAdjacentLParen(Token token) {
        Token lookahead = peekNextTokenTup();
        return lookahead.kind() == LPAREN && isAdjacent(token, lookahead);
    }

    private boolean nextTokenIsAdjacent(Token token) {
        return isAdjacent(token, peekNextTokenTup());
    }

    private static boolean isTypeAppOpen(Token token) {
        return token.kind() == LESS
            || token.kind() == INFIX_COMPARE_OP && ("</".equals(token.text()) || "<^".equals(token.text()))
            || token.kind() == LQUOTE && !token.flag();
    }

    /**
     * @return count of leading '>' and whether there is an operator after them, or null if the text does not start with '>'
     */
    private static int @Nullable [] typarsCloseOp(String text) {
        if (!text.startsWith(">")) {
            return null;
        }
        int angles = 0;
        while (angles < text.length() && text.charAt(angles) == '>') {
            angles++;
        }
        if (angles == text.length()) {
            return new int[]{angles, 0};
        }
        String after = text.substring(angles);
        boolean afterOp = switch (after) {
            case ".", "]", "-", "..", "?", "??", ":=", "::", "*", "&", "->", "<-", "=", "<", "$", "%", "%%" -> true;
            default -> {
                char c = after.charAt(0);
                yield after.startsWith("!=") || c == '=' || c == '<' || c == '>' || c == '$' || c == '&' || c == '|' || c == '!'
                    || c == '?' || c == '~' || c == '@' || c == '^' || c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
            }
        };
        return new int[]{angles, afterOp ? 1 : 0};
    }

    private boolean isTypeAppInnerToken(Token token) {
        return switch (token.kind()) {
            case DEFAULT, COLON, COLON_GREATER, STRUCT, NULL, DELEGATE, AND, WHEN, AMP, BAR_JUST_BEFORE_NULL, BAR, DOT_DOT, NEW, LBRACE_BAR,
                 SEMICOLON, BAR_RBRACE, MINUS, GLOBAL, CONST, KEYWORD_STRING, INT8, INT16, INT32, INT64, NATIVEINT, UINT8, UINT16, UINT32,
                 UINT64, UNATIVEINT, DECIMAL, BIGNUM, STRING, BYTEARRAY, CHAR, TRUE, FALSE, IEEE32, IEEE64, DOT, UNDERSCORE, EQUALS, IDENT,
                 COMMA, RARROW, HASH, STAR, QUOTE -> true;
            case INFIX_AT_HAT_OP -> "^".equals(token.text()) || "^-".equals(token.text());
            case INFIX_STAR_DIV_MOD_OP -> "/".equals(token.text());
            default -> false;
        };
    }

    private boolean peekAdjacentTypars(boolean indentation, Token tokenTup) {
        Token lookahead = peekNextTokenTup();
        if (!isTypeAppOpen(lookahead) || !isAdjacent(tokenTup, lookahead)) {
            return false;
        }
        Pos tokenEndPos = tokenTup.end();
        // head of the stack is the last element
        List<Token> stackTokens = new ArrayList<>();
        List<Boolean> stackSmash = new ArrayList<>();
        int nParen = 0;
        boolean res;
        while (true) {
            Token next = popNextTokenTup();
            stackTokens.add(next);
            stackSmash.add(Boolean.TRUE);
            FSharpLexFilterKind kind = next.kind();
            int[] closeOp = kind == INFIX_COMPARE_OP ? typarsCloseOp(next.text()) : null;
            if (kind == EOF || kind == SEMICOLON_SEMICOLON) {
                res = false;
                break;
            }
            if (indentation && startPos(next).compareTo(tokenEndPos) < 0) {
                res = false;
                break;
            }
            if (kind == RPAREN || kind == RBRACK) {
                nParen--;
                if (nParen > 0) {
                    continue;
                }
                res = false;
                break;
            }
            if (kind == GREATER || kind == GREATER_RBRACK || kind == GREATER_BAR_RBRACK) {
                nParen--;
                boolean hasAfterOp = kind != GREATER;
                if (nParen > 0) {
                    stackSmash.set(stackSmash.size() - 1, !hasAfterOp);
                    continue;
                }
                if (!hasAfterOp && nextTokenIsAdjacentLParen(next)) {
                    stackTokens.add(peekNextTokenTup().virtualCopy(HIGH_PRECEDENCE_PAREN_APP));
                    stackSmash.add(Boolean.FALSE);
                }
                res = true;
                break;
            }
            if (closeOp != null) {
                nParen -= closeOp[0];
                boolean afterOp = closeOp[1] != 0;
                if (nParen > 0) {
                    stackSmash.set(stackSmash.size() - 1, !afterOp);
                    continue;
                }
                if (!afterOp && nextTokenIsAdjacentLParen(next)) {
                    stackTokens.add(peekNextTokenTup().virtualCopy(HIGH_PRECEDENCE_PAREN_APP));
                    stackSmash.add(Boolean.FALSE);
                }
                res = true;
                break;
            }
            if (kind == LPAREN || kind == LESS || kind == LBRACK || kind == LBRACK_LESS || isTypeAppOpen(next)) {
                nParen++;
                continue;
            }
            if (isTypeAppInnerToken(next)) {
                continue;
            }
            if (nParen > 1) {
                continue;
            }
            res = false;
            break;
        }
        // put the tokens back on and smash them up if needed
        for (int i = stackTokens.size() - 1; i >= 0; i--) {
            Token token = stackTokens.get(i);
            if (stackSmash.get(i) && token.kind() == GREATER) {
                delayToken(token.retype(GREATER, res));
            }
            else {
                delayToken(token);
            }
        }
        return res;
    }

    // ------------------------------------------------------------------------
    // the filter

    private Token returnToken(Token token, FSharpLexFilterKind kind) {
        myPrevWasAtomicEnd = isAtomicExprEndToken(kind);
        return token.kind() == kind ? token : token.retype(kind);
    }

    private Token returnVirtual(FSharpLexFilterKind kind, Pos start, Pos end, Pos last) {
        myPrevWasAtomicEnd = isAtomicExprEndToken(kind);
        return new Token(kind, "", start, end, last, -1, 0, false, null);
    }

    private boolean detectJoinInCtxt(int depth) {
        if (!is(ctx(depth), C.VANILLA)) {
            return false;
        }
        int d = depth + 1;
        while (true) {
            Ctx c = ctx(d);
            if (c == null) {
                return false;
            }
            if (isParen(c, LBRACE)) {
                return true;
            }
            if (c.myKind == C.SEQ_BLOCK || c.myKind == C.DO || c.myKind == C.FOR) {
                d++;
                continue;
            }
            return false;
        }
    }

    private boolean tokenBalancesHeadContext(Token token, int depth) {
        Ctx c0 = ctx(depth);
        if (c0 == null) {
            return false;
        }
        Ctx c1 = ctx(depth + 1);
        FSharpLexFilterKind kind = token.kind();
        switch (kind) {
            case END -> {
                if (c0.myKind == C.WITH_AS_AUGMENT) {
                    return true;
                }
            }
            case ELSE, ELIF -> {
                if (c0.myKind == C.IF) {
                    return true;
                }
            }
            case DONE -> {
                if (c0.myKind == C.DO) {
                    return true;
                }
            }
            case WITH -> {
                switch (c0.myKind) {
                    case MATCH, EXCEPTION, MEMBER_HEAD, INTERFACE_HEAD, TRY, TYPE_DEFNS, MEMBER_BODY -> {
                        return true;
                    }
                    default -> {
                        if (c0.myKind == C.SEQ_BLOCK && isParen(c1, LBRACE, LBRACE_BAR)) {
                            return true;
                        }
                    }
                }
            }
            case FINALLY -> {
                if (c0.myKind == C.TRY) {
                    return true;
                }
            }
            case IN -> {
                if (c0.myKind == C.FOR || c0.myKind == C.LET_DECL) {
                    return true;
                }
                if (detectJoinInCtxt(depth)) {
                    return true;
                }
            }
            case SEMICOLON_SEMICOLON -> {
                if (c0.myKind == C.SEQ_BLOCK && c1 != null && (c1.myKind == C.NAMESPACE_BODY || c1.myKind == C.MODULE_BODY && c1.myFlag)) {
                    return true;
                }
            }
            default -> {
            }
        }
        if (c0.myKind == C.PAREN) {
            return parenTokensBalance(c0.token(), token);
        }
        return false;
    }

    private boolean tokenForcesHeadContextClosure(Token token) {
        if (myStack.isEmpty()) {
            return false;
        }
        FSharpLexFilterKind kind = token.kind();
        if (kind == EOF) {
            return true;
        }
        if (kind == SEMICOLON_SEMICOLON) {
            return !tokenBalancesHeadContext(token, 0);
        }
        if (isRExprParen(token) || isOneOf(kind, ELSE, ELIF, DONE, IN, WITH, FINALLY, INTERP_STRING_PART, INTERP_STRING_END)) {
            if (tokenBalancesHeadContext(token, 0)) {
                return false;
            }
            for (int d = 1; d < myStack.size(); d++) {
                if (tokenBalancesHeadContext(token, d)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static @Nullable FSharpLexFilterKind endTokenForACtxt(Ctx ctx) {
        return switch (ctx.myKind) {
            case FUN, MATCH_CLAUSES, WITH_AS_LET -> OEND;
            case WITH_AS_AUGMENT, DO -> ODECLEND;
            case LET_DECL -> ctx.myFlag ? ODECLEND : null;
            case SEQ_BLOCK -> switch (ctx.myAddBlockEnd) {
                case ADD -> OBLOCKEND;
                case ONE_SIDED -> ORIGHT_BLOCK_END;
                case NO -> null;
            };
            case MODULE_HEAD -> ctx.myFlag ? OBLOCKSEP : null;
            default -> null;
        };
    }

    private boolean nextOuterMostInterestingContextIsNamespaceOrModule(int depth) {
        Ctx c1 = ctx(depth + 1);
        if (c1 == null) {
            // at the top of the stack there is an implicit module
            return true;
        }
        if (c1.myKind == C.NAMESPACE_BODY || c1.myKind == C.MODULE_BODY) {
            return true;
        }
        if (isParen(c1, BEGIN, STRUCT) && is(ctx(depth + 2), C.SEQ_BLOCK)) {
            return nextOuterMostInterestingContextIsNamespaceOrModule(depth + 2);
        }
        return false;
    }

    /**
     * TYPE and MODULE close currently open expression contexts.
     */
    private void insertComingSoonTokens(Token tokenTup) {
        List<FSharpLexFilterKind> effects = new ArrayList<>();
        List<Ctx> effectContexts = new ArrayList<>();
        while (!myStack.isEmpty() && !nextOuterMostInterestingContextIsNamespaceOrModule(0)) {
            Ctx head = head();
            boolean interesting = head.myKind == C.PAREN && isLExprParen(head.token()) || head.myKind == C.SEQ_BLOCK || head.myKind == C.VANILLA;
            if (!interesting) {
                break;
            }
            popCtxt();
            if (head.myKind == C.SEQ_BLOCK) {
                if (head.myAddBlockEnd == AddBlockEnd.ADD) {
                    effects.add(OBLOCKEND);
                    effectContexts.add(head);
                }
                else if (head.myAddBlockEnd == AddBlockEnd.ONE_SIDED) {
                    effects.add(ORIGHT_BLOCK_END);
                    effectContexts.add(head);
                }
            }
        }
        delayTokenNoProcessing(tokenTup);
        for (int i = 0; i < effects.size(); i++) {
            delayTokenNoProcessing(link(effectContexts.get(i), tokenTup.virtualCopy(effects.get(i))));
        }
    }

    private boolean thereIsACtxtMemberBodyOnTheStackAndWeShouldPopStackForUpcomingMember() {
        boolean hasMemberBody = false;
        int lparens = 0;
        for (Ctx ctx : myStack) {
            if (ctx.myKind == C.MEMBER_BODY) {
                hasMemberBody = true;
            }
            if (isParen(ctx, LBRACE)) {
                // an object expression can have 'member' tokens in it
                return false;
            }
            if (isParen(ctx, LPAREN)) {
                lparens++;
            }
        }
        // static member constraints always are embedded in at least two LPARENS
        return hasMemberBody && lparens < 2;
    }

    private boolean isLongIdentEquals(Token token) {
        if (token.kind() != GLOBAL && token.kind() != IDENT) {
            return false;
        }
        return isLongIdentEqualsLoop();
    }

    private boolean isLongIdentEqualsLoop() {
        Token token = popNextTokenTup();
        boolean res;
        switch (token.kind()) {
            case DOT -> {
                Token next = popNextTokenTup();
                res = next.kind() == IDENT && isLongIdentEqualsLoop();
                delayToken(next);
            }
            case EQUALS -> res = true;
            default -> res = false;
        }
        delayToken(token);
        return res;
    }

    private void insertHighPrecedenceApp(Token tokenTup) {
        Token next = peekNextTokenTup();
        FSharpLexFilterKind hpa = nextTokenIsAdjacentLParen(tokenTup) ? HIGH_PRECEDENCE_PAREN_APP : HIGH_PRECEDENCE_BRACK_APP;
        delayToken(next.virtualCopy(hpa));
        delayToken(tokenTup);
    }

    private boolean rulesForBothSoftWhiteAndHardWhite(Token tokenTup) {
        FSharpLexFilterKind kind = tokenTup.kind();
        if (kind == IDENT && (nextTokenIsAdjacentLBrack(tokenTup) || nextTokenIsAdjacentLParen(tokenTup))) {
            insertHighPrecedenceApp(tokenTup);
            return true;
        }
        if (isOneOf(kind, DELEGATE, IDENT, IEEE64, IEEE32, DECIMAL, INT8, INT16, INT32, INT64, NATIVEINT, UINT8, UINT16, UINT32, UINT64,
            UNATIVEINT, BIGNUM) && peekAdjacentTypars(false, tokenTup)) {
            Token less = popNextTokenTup();
            delayToken(less.kind() == LESS ? less.retype(LESS, true) : less);
            delayToken(less.virtualCopy(HIGH_PRECEDENCE_TYAPP));
            delayToken(tokenTup);
            return true;
        }
        boolean prefixCandidate = switch (kind) {
            case MINUS, AMP, AMP_AMP -> true;
            case PLUS_MINUS_OP -> "+".equals(tokenTup.text()) || "+.".equals(tokenTup.text()) || "-.".equals(tokenTup.text());
            case PERCENT_OP -> "%".equals(tokenTup.text()) || "%%".equals(tokenTup.text());
            default -> false;
        };
        if (prefixCandidate && nextTokenIsAdjacent(tokenTup) && !(myPrevWasAtomicEnd && tokenTup.myLastTokenPos.equals(startPos(tokenTup)))) {
            boolean plusOrMinus = kind == MINUS || kind == PLUS_MINUS_OP && "+".equals(tokenTup.text());
            Token next = popNextTokenTup();
            if (plusOrMinus && isNumericForMerge(next.kind()) && !next.isVirtual() && !tokenTup.isVirtual()) {
                delayToken(new Token(next.kind(), next.text(), tokenTup.start(), next.end(), tokenTup.myLastTokenPos, tokenTup.realIndex(),
                    tokenTup.realCount() + next.realCount(), false, null));
            }
            else {
                delayToken(next);
                delayToken(tokenTup.retype(ADJACENT_PREFIX_OP));
            }
            return true;
        }
        return false;
    }

    private Token hwTokenFetch(boolean useBlockRule) {
        if (++mySteps > myMaxSteps) {
            throw new StepLimitException();
        }
        Token tokenTup = popNextTokenTup();
        if (rulesForBothSoftWhiteAndHardWhite(tokenTup)) {
            return hwTokenFetch(useBlockRule);
        }

        Pos tokenStartPos = startPos(tokenTup);
        FSharpLexFilterKind token = tokenTup.kind();
        int tokenStartCol = tokenStartPos.col();
        boolean isSemiSemi = token == SEMICOLON_SEMICOLON;
        // ODUMMY is a context closer token, after its context is closed
        boolean relaxWhitespace2OffsideRule = token == ODUMMY && tokenTup.myWrapped != null && isRExprParen(tokenTup.myWrapped);

        // inserted faux tokens need no other processing
        if (myNoProcessingCount > 0) {
            myNoProcessingCount--;
            return returnToken(tokenTup, token);
        }

        if (tokenForcesHeadContextClosure(tokenTup)) {
            Ctx ctx = head();
            popCtxt();
            FSharpLexFilterKind end = endTokenForACtxt(ctx);
            if (end != null) {
                return insertToken(tokenTup, end, ctx);
            }
            return reprocess(tokenTup, useBlockRule);
        }

        Ctx c0 = ctx(0);

        // reset on ';;' rule. A ';;' terminates ALL entries
        if (token == SEMICOLON_SEMICOLON && c0 == null) {
            delayToken(tokenTup.virtualCopy(ORESET));
            return returnToken(tokenTup, token);
        }
        if (token == ORESET && c0 == null) {
            peekInitial();
            return hwTokenFetch(true);
        }

        if (token == IN && detectJoinInCtxt(0)) {
            return returnToken(tokenTup, JOIN_IN);
        }

        // balancing rule: 'in' balances with a 'let'
        if (token == IN && c0 != null && c0.myKind == C.LET_DECL) {
            popCtxt();
            delayToken(tokenTup.dummy());
            return c0.myFlag ? link(c0, returnToken(tokenTup, ODECLEND)) : returnToken(tokenTup, token);
        }

        // balancing rule: 'done' balances with a 'do'
        if (token == DONE && c0 != null && c0.myKind == C.DO) {
            popCtxt();
            delayToken(link(c0, tokenTup.retype(ODECLEND)));
            return hwTokenFetch(useBlockRule);
        }

        // balancing rule: ')' or '}' balances with a '(' or '{', even if not offside
        if ((isRExprParen(tokenTup) || token == INTERP_STRING_END || token == INTERP_STRING_PART)
            && c0 != null && c0.myKind == C.PAREN && parenTokensBalance(c0.token(), tokenTup)) {
            popCtxt();
            if (token == INTERP_STRING_PART) {
                pushCtxt(Ctx.paren(tokenTup, tokenTup.end()));
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.NO);
            }
            else if (token != INTERP_STRING_END) {
                delayToken(tokenTup.dummy());
            }
            return returnToken(tokenTup, token);
        }

        // balancing rule: 'end' can balance with a 'with' but only when not offside
        if (token == END && c0 != null && c0.myKind == C.WITH_AS_AUGMENT && !(tokenStartCol + 1 <= c0.col())) {
            popCtxt();
            delayToken(tokenTup.dummy());
            return link(c0, returnToken(tokenTup, OEND));
        }

        // transition rule: CtxtNamespaceHead ~~~> CtxtSeqBlock, applied when a token other than a long identifier is seen
        if (c0 != null && c0.myKind == C.NAMESPACE_HEAD) {
            Pos namespaceTokenPos = c0.myStart;
            FSharpLexFilterKind prevToken = c0.token().kind();
            boolean afterColumn = namespaceTokenPos.col() < tokenStartPos.col();
            if (isOneOf(prevToken, NAMESPACE, DOT, REC, GLOBAL) && isOneOf(token, REC, IDENT, GLOBAL) && afterColumn
                || prevToken == IDENT && token == DOT && afterColumn) {
                replaceCtxt(Ctx.head(C.NAMESPACE_HEAD, namespaceTokenPos, tokenTup, false, false));
                return returnToken(tokenTup, token);
            }
            popCtxt();
            // don't push a new context if next token is EOF, since that raises an offside warning
            if (token == EOF) {
                return returnToken(tokenTup, token);
            }
            delayToken(tokenTup);
            pushCtxt(Ctx.of(C.NAMESPACE_BODY, namespaceTokenPos));
            pushCtxtSeqBlockAt(false, false, tokenTup, tokenTup, AddBlockEnd.ADD);
            return hwTokenFetch(false);
        }

        // transition rule: CtxtModuleHead ~~~> push CtxtModuleBody; push CtxtSeqBlock, applied when a ':' or '=' token is seen
        if (c0 != null && c0.myKind == C.MODULE_HEAD) {
            Pos moduleTokenPos = c0.myStart;
            Token prevTokenTup = c0.token();
            FSharpLexFilterKind prevToken = prevTokenTup.kind();
            boolean lexingModuleAttributes = c0.myLexingModuleAttributes;
            boolean isNested = c0.myFlag;
            boolean afterColumn = moduleTokenPos.col() < tokenStartPos.col();
            if (token == GREATER_RBRACK && lexingModuleAttributes && afterColumn) {
                replaceCtxt(Ctx.head(C.MODULE_HEAD, moduleTokenPos, prevTokenTup, false, isNested));
                return returnToken(tokenTup, token);
            }
            if (lexingModuleAttributes && afterColumn) {
                return returnToken(tokenTup, token);
            }
            if (prevToken == MODULE && isOneOf(token, PUBLIC, PRIVATE, INTERNAL) && afterColumn) {
                return returnToken(tokenTup, token);
            }
            if ((prevToken == MODULE && token == GLOBAL
                || isOneOf(prevToken, MODULE, REC, DOT) && isOneOf(token, REC, IDENT)
                || prevToken == IDENT && token == DOT) && afterColumn) {
                replaceCtxt(Ctx.head(C.MODULE_HEAD, moduleTokenPos, tokenTup, false, isNested));
                return returnToken(tokenTup, token);
            }
            if (prevToken == MODULE && token == LBRACK_LESS && afterColumn) {
                replaceCtxt(Ctx.head(C.MODULE_HEAD, moduleTokenPos, prevTokenTup, true, isNested));
                return returnToken(tokenTup, token);
            }
            if (token == EQUALS || token == COLON) {
                popCtxt();
                pushCtxt(Ctx.of(C.MODULE_BODY, moduleTokenPos, false));
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                return returnToken(tokenTup, token);
            }
            if (myStack.size() == 2 && is(ctx(1), C.SEQ_BLOCK)) {
                popCtxt();
                if (token == EOF) {
                    return returnToken(tokenTup, token);
                }
                // this is a module declaration spanning the whole file
                delayToken(tokenTup);
                pushCtxt(Ctx.of(C.MODULE_BODY, moduleTokenPos, true));
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                return hwTokenFetch(false);
            }
            // adding a new nested module, EQUALS hasn't been typed yet
            popCtxt();
            return insertTokenFromPrevPosToCurrentPos(tokenTup, OBLOCKSEP);
        }

        // offside rule for SeqBlock
        if (c0 != null && c0.myKind == C.SEQ_BLOCK) {
            Ctx c1 = ctx(1);
            Pos offsidePos = c0.myStart;
            boolean offside;
            if (isSemiSemi && !(is(c1, C.NAMESPACE_BODY) || c1 != null && c1.myKind == C.MODULE_BODY && c1.myFlag)) {
                offside = true;
            }
            else {
                int grace;
                if (token == BAR && is(c1, C.TYPE_DEFNS)) {
                    // type x =
                    //     A of string
                    //   | B of int
                    grace = 2;
                }
                else if (c1 != null && c1.myKind == C.TYPE_DEFNS && offsidePos.col() == c1.col() && !isTypeSeqBlockElementContinuator(tokenTup)) {
                    grace = -1;
                }
                else if (c1 != null && c1.myKind == C.NAMESPACE_BODY && offsidePos.col() == c1.col() && token == NAMESPACE) {
                    grace = -1;
                }
                else {
                    grace = isInfix(tokenTup) ? infixTokenLength(tokenTup) + 1 : 0;
                }
                offside = tokenStartCol + grace < offsidePos.col();
            }
            if (offside) {
                popCtxt();
                return switch (c0.myAddBlockEnd) {
                    case ADD -> insertToken(tokenTup, OBLOCKEND, c0);
                    case ONE_SIDED -> insertToken(tokenTup, ORIGHT_BLOCK_END, c0);
                    case NO -> reprocess(tokenTup, useBlockRule);
                };
            }
        }

        if (c0 != null && c0.myKind == C.VANILLA && (isSemiSemi || tokenStartCol <= c0.col())) {
            popCtxt();
            return reprocess(tokenTup, useBlockRule);
        }

        if (c0 != null && c0.myKind == C.SEQ_BLOCK) {
            Pos offsidePos = c0.myStart;
            AddBlockEnd addBlockEnd = c0.myAddBlockEnd;
            // attribute-end tokens mean CtxtSeqBlock rule is NOT applied to the next token
            if (!c0.myFlag && token == GREATER_RBRACK) {
                replaceCtxt(c0.seqBlockWithFirst(true));
                return reprocessWithoutBlockRule(tokenTup);
            }
            // avoiding inserting OBLOCKSEP on first item in block
            if (c0.myFlag && useBlockRule) {
                replaceCtxt(c0.seqBlockWithFirst(false));
                return reprocessWithoutBlockRule(tokenTup);
            }
            // inserting OBLOCKSEP on subsequent items in a block when they are precisely aligned
            if (!c0.myFlag && useBlockRule) {
                Ctx c1 = ctx(1);
                boolean continuator;
                if (is(c1, C.NAMESPACE_BODY)) {
                    continuator = token == NAMESPACE;
                }
                else if (is(c1, C.TYPE_DEFNS)) {
                    continuator = isTypeSeqBlockElementContinuator(tokenTup);
                }
                else {
                    continuator = isSeqBlockElementContinuator(tokenTup);
                }
                if (!continuator && tokenStartCol == offsidePos.col() && tokenStartPos.line() != offsidePos.line()) {
                    replaceCtxt(c0.seqBlockWithFirst(true));
                    return insertTokenFromPrevPosToCurrentPos(tokenTup, OBLOCKSEP);
                }
            }
        }

        if (c0 != null) {
            int col = tokenStartCol;
            int offsideCol = c0.col();
            switch (c0.myKind) {
                case LET_DECL -> {
                    if (c0.myFlag && (isSemiSemi || (relaxWhitespace2OffsideRule || isLetContinuator(tokenTup) ? col + 1 : col) <= offsideCol)) {
                        popCtxt();
                        return insertToken(tokenTup, ODECLEND, c0);
                    }
                }
                case DO -> {
                    if (isSemiSemi || (isDoneContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return insertToken(tokenTup, ODECLEND, c0);
                    }
                }
                case INTERFACE_HEAD -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isInterfaceContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case TYPE_DEFNS -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isTypeContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case MODULE_BODY -> {
                    if (isSemiSemi && !c0.myFlag || (relaxWhitespace2OffsideRule ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case NAMESPACE_BODY -> {
                    if ((relaxWhitespace2OffsideRule || isNamespaceContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case EXCEPTION, MEMBER_HEAD -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case MEMBER_BODY -> {
                    if (isSemiSemi || col <= offsideCol) {
                        popCtxt();
                        return insertToken(tokenTup, ODECLEND, c0);
                    }
                }
                case IF -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isIfBlockContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case WITH_AS_LET -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isLetContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return insertToken(tokenTup, OEND, c0);
                    }
                }
                case WITH_AS_AUGMENT -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isWithAugmentBlockContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return insertToken(tokenTup, ODECLEND, c0);
                    }
                }
                case MATCH -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isMatchBlockContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case FOR, WHILE -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isDoneContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case WHEN, ELSE -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case FUN -> {
                    if (isSemiSemi || col <= offsideCol) {
                        popCtxt();
                        return insertToken(tokenTup, OEND, c0);
                    }
                }
                case FUNCTION -> {
                    if (isSemiSemi || col <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case TRY -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isTryBlockContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case THEN -> {
                    if (isSemiSemi || (relaxWhitespace2OffsideRule || isThenBlockContinuator(tokenTup) ? col + 1 : col) <= offsideCol) {
                        popCtxt();
                        return reprocess(tokenTup, useBlockRule);
                    }
                }
                case MATCH_CLAUSES -> {
                    boolean leadingBar = c0.myFlag;
                    boolean offside;
                    if (isSemiSemi) {
                        offside = true;
                    }
                    else if (token == BAR) {
                        offside = col + (leadingBar ? 0 : 2) < offsideCol;
                    }
                    else {
                        offside = col + (leadingBar ? -1 : 1) < offsideCol;
                    }
                    if (offside) {
                        popCtxt();
                        return insertToken(tokenTup, OEND, c0);
                    }
                }
                default -> {
                }
            }
        }

        return pushRules(tokenTup, tokenStartPos, useBlockRule);
    }

    private Token pushRules(Token tokenTup, Pos tokenStartPos, boolean useBlockRule) {
        FSharpLexFilterKind token = tokenTup.kind();
        Ctx c0 = ctx(0);
        Ctx c1 = ctx(1);
        Ctx c2 = ctx(2);

        if (c0 != null) {
            switch (token) {
                case NAMESPACE -> {
                    pushCtxt(Ctx.head(C.NAMESPACE_HEAD, tokenStartPos, tokenTup, false, false));
                    return returnToken(tokenTup, token);
                }
                case MODULE -> {
                    insertComingSoonTokens(tokenTup);
                    boolean isNested = !(myStack.size() == 1 && head().myKind == C.SEQ_BLOCK);
                    pushCtxt(Ctx.head(C.MODULE_HEAD, tokenStartPos, tokenTup, false, isNested));
                    return hwTokenFetch(useBlockRule);
                }
                case EXCEPTION -> {
                    pushCtxt(Ctx.of(C.EXCEPTION, tokenStartPos));
                    return returnToken(tokenTup, token);
                }
                case LET -> {
                    if (c0.myKind == C.MEMBER_HEAD) {
                        // 'static let'
                        Pos startPos = c0.myStart;
                        popCtxt();
                        Ctx let = Ctx.of(C.LET_DECL, startPos, true);
                        pushCtxt(let);
                        return open(let, returnToken(tokenTup, OLET));
                    }
                    boolean blockLet = c0.myKind == C.SEQ_BLOCK || c0.myKind == C.MATCH_CLAUSES;
                    Ctx let = Ctx.of(C.LET_DECL, tokenStartPos, blockLet);
                    pushCtxt(let);
                    return open(let, returnToken(tokenTup, blockLet ? OLET : token));
                }
                case BINDER, AND_BANG -> {
                    boolean blockLet = c0.myKind == C.SEQ_BLOCK;
                    Ctx let = Ctx.of(C.LET_DECL, tokenStartPos, blockLet);
                    pushCtxt(let);
                    return open(let, returnToken(tokenTup, blockLet ? (token == BINDER ? OBINDER : OAND_BANG) : token));
                }
                default -> {
                }
            }
        }

        boolean memberStart = isOneOf(token, VAL, STATIC, ABSTRACT, MEMBER, OVERRIDE, DEFAULT);
        if (memberStart && thereIsACtxtMemberBodyOnTheStackAndWeShouldPopStackForUpcomingMember()) {
            delayTokenNoProcessing(tokenTup);
            while (head().myKind != C.MEMBER_BODY) {
                Ctx popped = head();
                FSharpLexFilterKind end = endTokenForACtxt(popped);
                popCtxt();
                if (end != null) {
                    delayTokenNoProcessing(link(popped, tokenTup.virtualCopy(end)));
                }
            }
            popCtxt();
            return hwTokenFetch(useBlockRule);
        }

        if (memberStart && c0 != null && c0.myKind != C.MEMBER_HEAD) {
            pushCtxt(Ctx.of(C.MEMBER_HEAD, tokenStartPos));
            return returnToken(tokenTup, token);
        }

        if (isOneOf(token, PUBLIC, PRIVATE, INTERNAL) && c0 != null && peekNextToken() == NEW) {
            pushCtxt(Ctx.of(C.MEMBER_HEAD, tokenStartPos));
            return returnToken(tokenTup, token);
        }

        if (token == NEW && c0 != null && peekNextToken() == LPAREN && c0.myKind != C.MEMBER_HEAD) {
            pushCtxt(Ctx.of(C.MEMBER_HEAD, tokenStartPos));
            return returnToken(tokenTup, token);
        }

        if (token == EQUALS && is(c0, C.LET_DECL)) {
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            return returnToken(tokenTup, token);
        }

        if (token == EQUALS && c0 != null && c0.myKind == C.TYPE_DEFNS) {
            replaceCtxtIgnoreIndent(Ctx.typeDefns(c0.myStart, tokenTup.end()));
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            return returnToken(tokenTup, token);
        }

        if (token == LAZY || token == ASSERT) {
            if (isControlFlowOrNotSameLine(tokenTup, tokenStartPos)) {
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                return returnToken(tokenTup, token == LAZY ? OLAZY : OASSERT);
            }
            return returnToken(tokenTup, token);
        }

        if (token == EQUALS && (is(c0, C.WITH_AS_LET)
            || c0 != null && c0.myKind == C.VANILLA && c0.myFlag && is(c1, C.SEQ_BLOCK) && (is(c2, C.WITH_AS_LET) || isParen(c2, LBRACE, LBRACE_BAR)))) {
            if (isControlFlowOrNotSameLine(tokenTup, tokenStartPos)) {
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            }
            else {
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.NO);
            }
            return returnToken(tokenTup, token);
        }

        if (token == EQUALS && c0 != null && c0.myKind == C.MEMBER_HEAD) {
            Ctx body = Ctx.of(C.MEMBER_BODY, c0.myStart);
            replaceCtxt(body);
            Token equals = open(body, returnToken(tokenTup, token));
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            return equals;
        }

        // '(' tokens are balanced with ')' tokens and also introduce a CtxtSeqBlock
        if (isLExprParen(tokenTup) || token == SIG || token == INTERP_STRING_BEGIN_PART) {
            Pos pos = token == INTERP_STRING_BEGIN_PART ? tokenTup.end() : tokenStartPos;
            pushCtxt(Ctx.paren(tokenTup, pos));
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.NO);
            return returnToken(tokenTup, token);
        }

        // type ... = struct ... end
        if (token == STRUCT && is(c0, C.SEQ_BLOCK) && (is(c1, C.MODULE_BODY) || is(c1, C.TYPE_DEFNS))) {
            pushCtxt(Ctx.paren(tokenTup, tokenStartPos));
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.NO);
            return returnToken(tokenTup, token);
        }

        if (token == RARROW && c0 != null) {
            boolean push = switch (c0.myKind) {
                case WHILE, FOR, WHEN, MATCH_CLAUSES, FUN -> true;
                case SEQ_BLOCK -> isParen(c1, LBRACK, LBRACE, LBRACE_BAR, LBRACK_BAR)
                    || c1 != null && isOneOfCtx(c1.myKind, C.DO, C.WHILE, C.FOR, C.WHEN, C.MATCH_CLAUSES, C.TRY, C.THEN, C.ELSE);
                default -> false;
            };
            if (push) {
                Token arrow = returnToken(tokenTup, token);
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ONE_SIDED);
                head().myOpen = arrow;
                return arrow;
            }
        }

        if (token == LARROW && isControlFlowOrNotSameLine(tokenTup, tokenStartPos)) {
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            return returnToken(tokenTup, token);
        }

        if (token == DO || token == DO_BANG) {
            Ctx doCtx = Ctx.of(C.DO, tokenStartPos);
            pushCtxt(doCtx);
            Token result = open(doCtx, returnToken(tokenTup, token == DO ? ODO : ODO_BANG));
            tryPushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            return result;
        }

        // the r.h.s. of an infix token begins a new block
        if (isInfix(tokenTup) && !isSameLine(tokenTup, tokenStartPos) && !is(c0, C.MATCH_CLAUSES)) {
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.NO);
            return returnToken(tokenTup, token);
        }

        if (token == WITH && (is(c0, C.TRY) || is(c0, C.MATCH))) {
            Token lookahead = peekNextTokenTup();
            boolean leadingBar = lookahead.kind() == BAR;
            Ctx clauses = Ctx.of(C.MATCH_CLAUSES, startPos(lookahead), leadingBar);
            Token with = returnToken(tokenTup, OWITH);
            if (tryPushCtxt(true, false, clauses)) {
                open(clauses, with);
            }
            return with;
        }

        if (token == FINALLY && is(c0, C.TRY)) {
            pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            return returnToken(tokenTup, token);
        }

        if (token == WITH && c0 != null
            && (isOneOfCtx(c0.myKind, C.EXCEPTION, C.TYPE_DEFNS, C.MEMBER_HEAD, C.INTERFACE_HEAD, C.MEMBER_BODY)
            || c0.myKind == C.SEQ_BLOCK && isParen(c1, LBRACE, LBRACE_BAR))) {
            Ctx limCtxt = c0;
            Token lookahead = peekNextTokenTup();
            Pos lookaheadStartPos = startPos(lookahead);
            switch (lookahead.kind()) {
                case RBRACE, IDENT, DOT_DOT_DOT, PUBLIC, PRIVATE, INTERNAL, INLINE -> {
                    Pos offsidePos = lookaheadStartPos.col() > tokenTup.end().col() ? tokenStartPos : limCtxt.myStart;
                    Ctx withAsLet = Ctx.of(C.WITH_AS_LET, offsidePos);
                    pushCtxt(withAsLet);
                    Token with = open(withAsLet, returnToken(tokenTup, OWITH));
                    Token next = popNextTokenTup();
                    boolean followed = isLongIdentEquals(next) || next.kind() == DOT_DOT_DOT;
                    delayToken(next);
                    if (followed) {
                        pushCtxtSeqBlock(tokenTup, AddBlockEnd.NO);
                    }
                    return with;
                }
                default -> {
                    if (lookahead.kind() == LBRACK_LESS && lookaheadStartPos.line() == tokenTup.start().line()) {
                        Ctx withAsLet = Ctx.of(C.WITH_AS_LET, tokenStartPos);
                        pushCtxt(withAsLet);
                        return open(withAsLet, returnToken(tokenTup, OWITH));
                    }
                    if (lookaheadStartPos.col() <= limCtxt.col() && limCtxt.myKind == C.INTERFACE_HEAD) {
                        return returnToken(tokenTup, token);
                    }
                    Ctx augment = Ctx.of(C.WITH_AS_AUGMENT, limCtxt.myStart);
                    pushCtxt(augment);
                    Token with = open(augment, returnToken(tokenTup, token));
                    pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                    return with;
                }
            }
        }

        if (token == WITH) {
            Ctx augment = Ctx.of(C.WITH_AS_AUGMENT, tokenStartPos);
            pushCtxt(augment);
            Token with = open(augment, returnToken(tokenTup, token));
            tryPushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
            return with;
        }

        switch (token) {
            case FUNCTION -> {
                Token lookahead = peekNextTokenTup();
                boolean leadingBar = lookahead.kind() == BAR;
                pushCtxt(Ctx.of(C.FUNCTION, tokenStartPos));
                Ctx clauses = Ctx.of(C.MATCH_CLAUSES, startPos(lookahead), leadingBar);
                pushCtxt(clauses);
                return open(clauses, returnToken(tokenTup, OFUNCTION));
            }
            case THEN -> {
                pushCtxt(Ctx.of(C.THEN, tokenStartPos));
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                return returnToken(tokenTup, OTHEN);
            }
            case ELSE -> {
                Token lookahead = peekNextTokenTup();
                if (lookahead.kind() == IF && isSameLine(tokenTup, tokenStartPos)) {
                    // convert ELSE IF to ELIF since it then opens the block at the right point
                    popNextTokenTup();
                    pushCtxt(Ctx.of(C.IF, tokenStartPos));
                    myPrevWasAtomicEnd = false;
                    return new Token(ELIF, "", tokenStartPos, lookahead.end(), tokenTup.myLastTokenPos, tokenTup.realIndex(),
                        tokenTup.realCount() + lookahead.realCount(), false, null);
                }
                pushCtxt(Ctx.of(C.ELSE, tokenStartPos));
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                return returnToken(tokenTup, OELSE);
            }
            case ELIF, IF -> {
                pushCtxt(Ctx.of(C.IF, tokenStartPos));
                return returnToken(tokenTup, token);
            }
            case MATCH, MATCH_BANG -> {
                pushCtxt(Ctx.of(C.MATCH, tokenStartPos));
                return returnToken(tokenTup, token);
            }
            case FOR -> {
                pushCtxt(Ctx.of(C.FOR, tokenStartPos));
                return returnToken(tokenTup, token);
            }
            case WHILE, WHILE_BANG -> {
                pushCtxt(Ctx.of(C.WHILE, tokenStartPos));
                return returnToken(tokenTup, token);
            }
            case WHEN -> {
                if (is(c0, C.SEQ_BLOCK)) {
                    pushCtxt(Ctx.of(C.WHEN, tokenStartPos));
                    return returnToken(tokenTup, token);
                }
            }
            case FUN -> {
                Ctx fun = Ctx.of(C.FUN, tokenStartPos);
                pushCtxt(fun);
                return open(fun, returnToken(tokenTup, OFUN));
            }
            case INTERFACE -> {
                if (isInterfaceRepresentation(tokenTup, c0, c1)) {
                    pushCtxt(Ctx.paren(tokenTup, tokenStartPos));
                    pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                    return returnToken(tokenTup, token);
                }
                pushCtxt(Ctx.of(C.INTERFACE_HEAD, tokenStartPos));
                return returnToken(tokenTup, OINTERFACE_MEMBER);
            }
            case CLASS -> {
                pushCtxt(Ctx.paren(tokenTup, tokenStartPos));
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ADD);
                return returnToken(tokenTup, token);
            }
            case TYPE -> {
                insertComingSoonTokens(tokenTup);
                pushCtxt(Ctx.typeDefns(tokenStartPos, null));
                return hwTokenFetch(useBlockRule);
            }
            case TRY -> {
                pushCtxt(Ctx.of(C.TRY, tokenStartPos));
                Token tryToken = returnToken(tokenTup, token);
                pushCtxtSeqBlock(tokenTup, AddBlockEnd.ONE_SIDED);
                head().myOpen = tryToken;
                return tryToken;
            }
            case OBLOCKBEGIN, OPEN -> {
                return returnToken(tokenTup, token);
            }
            case ODUMMY -> {
                return hwTokenFetch(useBlockRule);
            }
            case BAR -> {
                if (peekNextToken() == NULL) {
                    return returnToken(tokenTup, BAR_JUST_BEFORE_NULL);
                }
            }
            default -> {
            }
        }

        // ordinary tokens start a vanilla block
        if (is(c0, C.SEQ_BLOCK)) {
            pushCtxt(Ctx.of(C.VANILLA, tokenStartPos, isLongIdentEquals(tokenTup)));
            return returnToken(tokenTup, token);
        }
        return returnToken(tokenTup, token);
    }

    private boolean isInterfaceRepresentation(Token tokenTup, @Nullable Ctx c0, @Nullable Ctx c1) {
        // type I = interface .... end
        if (!is(c0, C.SEQ_BLOCK) || c1 == null || c1.myKind != C.TYPE_DEFNS || c1.myEqualsEndPos == null) {
            return false;
        }
        Pos typePos = c1.myStart;
        Pos equalsEndPos = c1.myEqualsEndPos;
        if (!tokenTup.myLastTokenPos.equals(equalsEndPos)) {
            return false;
        }
        boolean allowDeindent = tokenTup.end().line() == equalsEndPos.line();
        Token lookahead = peekNextTokenTup();
        Pos lookaheadStartPos = startPos(lookahead);
        return switch (lookahead.kind()) {
            case END -> lookaheadStartPos.col() >= typePos.col();
            case DEFAULT, OVERRIDE, INTERFACE, NEW, TYPE, STATIC, MEMBER, ABSTRACT, INHERIT, LBRACK_LESS -> {
                Pos limitPos = allowDeindent ? typePos : tokenTup.start();
                yield lookaheadStartPos.col() >= limitPos.col() + 1;
            }
            default -> false;
        };
    }

    private static boolean isOneOfCtx(C kind, C... kinds) {
        for (C k : kinds) {
            if (k == kind) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameLine(Token tokenTup, Pos tokenStartPos) {
        return tokenTup.kind() != EOF && startPos(peekNextTokenTup()).line() == tokenStartPos.line();
    }

    private boolean isControlFlowOrNotSameLine(Token tokenTup, Pos tokenStartPos) {
        if (tokenTup.kind() == EOF) {
            return false;
        }
        return !isSameLine(tokenTup, tokenStartPos)
            || isOneOf(peekNextToken(), TRY, MATCH, MATCH_BANG, IF, LET, FOR, WHILE, WHILE_BANG);
    }

    private Token reprocess(Token tokenTup, boolean useBlockRule) {
        delayToken(tokenTup);
        return hwTokenFetch(useBlockRule);
    }

    private Token reprocessWithoutBlockRule(Token tokenTup) {
        delayToken(tokenTup);
        return hwTokenFetch(false);
    }

    private Token insertTokenFromPrevPosToCurrentPos(Token tokenTup, FSharpLexFilterKind kind) {
        delayToken(tokenTup);
        // span of inserted token lasts from the col + 1 of the prev token to the beginning of current token
        return returnVirtual(kind, tokenTup.myLastTokenPos.shift(1), tokenTup.start(), tokenTup.myLastTokenPos);
    }

    private Token insertToken(Token tokenTup, FSharpLexFilterKind kind, Ctx closed) {
        delayToken(tokenTup);
        return link(closed, returnVirtual(kind, startPos(tokenTup), tokenTup.end(), tokenTup.myLastTokenPos));
    }

    private static Token open(Ctx ctx, Token token) {
        ctx.myOpen = token;
        return token;
    }

    private static Token link(Ctx closed, Token end) {
        Token open = closed.myOpen;
        if (open != null) {
            open.myMatch = end;
            end.myMatch = open;
        }
        return end;
    }

    /**
     * @return for each token the index of its matching opener/end token ({@code OBLOCKBEGIN}/{@code OBLOCKEND},
     * {@code OLET}/{@code ODECLEND}, {@code OFUN}/{@code OEND}, ...), or -1
     */
    public static int[] matches(List<Token> tokens) {
        java.util.Map<Token, Integer> indices = new java.util.IdentityHashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            indices.put(tokens.get(i), i);
        }
        int[] result = new int[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            Token match = tokens.get(i).myMatch;
            Integer index = match == null ? null : indices.get(match);
            result[i] = index == null ? -1 : index;
        }
        return result;
    }
}
