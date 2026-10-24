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

import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderAdapter;
import consulo.language.parser.WhitespacesAndCommentsBinder;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link PsiBuilder} over the token stream of {@link FSharpLexFilter}: the parser sees compiler token kinds
 * ({@link #kind()}), including the virtual offside tokens. Virtual tokens have no text and produce no PSI leaves.
 * <p>
 * Line directives ({@code #nowarn}, {@code #r}, ...) are not seen by the compiler parser and not by our parser: like
 * ReSharper, a directive node is inserted where it is, right before the next marker or real token after it.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpBuilder extends PsiBuilderAdapter {
    private record Entry(FSharpLexFilterKind kind, String text, boolean flag, int realCount) {
    }

    private record Directive(int start, int count, String text) {
    }

    /**
     * @param anchor index of the entry the directive precedes
     */
    private record PendingDirective(int anchor, int count, String text) {
    }

    private final Entry[] myEntries;
    private final int[] myMatches;
    private final PendingDirective[] myDirectives;
    private int myIndex;
    private int myNextDirective;

    public FSharpBuilder(PsiBuilder delegate) {
        super(delegate);
        CharSequence text = delegate.getOriginalText();

        FSharpLexFilterKinds.Collector collector = new FSharpLexFilterKinds.Collector(text);
        List<Directive> directives = new ArrayList<>();
        PsiBuilder.Marker probe = delegate.mark();
        int directiveStart = -1;
        int directiveLineEnd = -1;
        int directiveCount = 0;
        while (!delegate.eof()) {
            IElementType type = delegate.getTokenType();
            int start = delegate.getCurrentOffset();
            String tokenText = delegate.getTokenText();
            int end = start + (tokenText == null ? 0 : tokenText.length());
            if (directiveStart >= 0 && start < directiveLineEnd) {
                directiveCount++;
            }
            else {
                if (directiveStart >= 0) {
                    directives.add(new Directive(directiveStart, directiveCount, text.subSequence(directiveStart, directiveLineEnd).toString()));
                    directiveStart = -1;
                }
                if (type != null && FSharpLexFilterKinds.LINE_DIRECTIVES.contains(type)) {
                    directiveStart = start;
                    directiveLineEnd = lineEnd(text, start);
                    directiveCount = 1;
                }
                else if (type != null) {
                    collector.add(type, start, end);
                }
            }
            delegate.advanceLexer();
        }
        if (directiveStart >= 0) {
            directives.add(new Directive(directiveStart, directiveCount, text.subSequence(directiveStart, directiveLineEnd).toString()));
        }
        probe.rollbackTo();

        List<FSharpLexFilter.RealToken> real = collector.getTokens();
        List<FSharpLexFilter.Token> filtered = FSharpLexFilter.filter(text, real);
        List<Entry> entries = new ArrayList<>(filtered.size() + directives.size());
        int[] filteredMatches = FSharpLexFilter.matches(filtered);
        int[] entryIndices = new int[filtered.size()];
        List<PendingDirective> pending = new ArrayList<>(directives.size());
        int nextDirective = 0;
        for (int i = 0; i < filtered.size(); i++) {
            FSharpLexFilter.Token token = filtered.get(i);
            if (!token.isVirtual() || token.kind() == FSharpLexFilterKind.EOF) {
                int start = token.kind() == FSharpLexFilterKind.EOF ? text.length() : real.get(token.realIndex()).start();
                while (nextDirective < directives.size() && directives.get(nextDirective).start() < start) {
                    Directive directive = directives.get(nextDirective++);
                    pending.add(new PendingDirective(entries.size(), directive.count(), directive.text()));
                }
            }
            entryIndices[i] = entries.size();
            if (token.kind() == FSharpLexFilterKind.ELIF && token.realCount() == 2) {
                // 'else if': the inner 'if' is a separate expression in PSI
                entries.add(new Entry(FSharpLexFilterKind.ELSE, real.get(token.realIndex()).text(), false, 1));
                entries.add(new Entry(FSharpLexFilterKind.IF, real.get(token.realIndex() + 1).text(), false, 1));
                continue;
            }
            String tokenText;
            if (token.isVirtual()) {
                tokenText = "";
            }
            else if (token.realCount() > 1) {
                // merged tokens like '-1'
                StringBuilder builder = new StringBuilder();
                for (int r = 0; r < token.realCount(); r++) {
                    builder.append(real.get(token.realIndex() + r).text());
                }
                tokenText = builder.toString();
            }
            else {
                tokenText = token.text();
            }
            entries.add(new Entry(token.kind(), tokenText, token.flag(), token.realCount()));
        }
        myEntries = entries.toArray(new Entry[0]);
        myDirectives = pending.toArray(new PendingDirective[0]);
        myMatches = new int[myEntries.length];
        java.util.Arrays.fill(myMatches, -1);
        for (int i = 0; i < filtered.size(); i++) {
            if (filteredMatches[i] >= 0) {
                myMatches[entryIndices[i]] = entryIndices[filteredMatches[i]];
            }
        }
    }

    private static int lineEnd(CharSequence text, int offset) {
        int end = offset;
        while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
            end++;
        }
        return end;
    }

    private Entry entry(int index) {
        return index < myEntries.length ? myEntries[index] : myEntries[myEntries.length - 1];
    }

    public FSharpLexFilterKind kind() {
        return entry(myIndex).kind();
    }

    public FSharpLexFilterKind kind(int lookAhead) {
        return entry(myIndex + lookAhead).kind();
    }

    /**
     * Text of the current token: the real text, empty for virtual tokens.
     */
    public String text() {
        return entry(myIndex).text();
    }

    public String text(int lookAhead) {
        return entry(myIndex + lookAhead).text();
    }

    /**
     * {@code LESS}/{@code GREATER}: type application brackets; {@code LQUOTE}/{@code RQUOTE}: untyped quotation.
     */
    public boolean flag() {
        return entry(myIndex).flag();
    }

    /**
     * @return look ahead distance to the token matching the current one (end of a virtual block for its opener), or -1
     */
    public int matchDistance() {
        int match = myIndex < myMatches.length ? myMatches[myIndex] : -1;
        return match > myIndex ? match - myIndex : -1;
    }

    public int position() {
        return myIndex;
    }

    public boolean isVirtual() {
        return entry(myIndex).realCount() == 0;
    }

    public boolean isVirtual(int lookAhead) {
        return entry(myIndex + lookAhead).realCount() == 0;
    }

    @Override
    public boolean eof() {
        return kind() == FSharpLexFilterKind.EOF;
    }

    @Override
    public void advanceLexer() {
        if (myIndex >= myEntries.length - 1) {
            // EOF
            return;
        }
        Entry entry = myEntries[myIndex];
        if (entry.realCount() > 0) {
            flushDirectives();
        }
        myIndex++;
        for (int i = 0; i < entry.realCount(); i++) {
            myDelegate.advanceLexer();
        }
    }

    /**
     * Moves the underlying builder to its end: remaining real tokens become an error.
     */
    public void finish() {
        flushDirectives();
        if (myDelegate.eof()) {
            return;
        }
        PsiBuilder.Marker error = myDelegate.mark();
        while (!myDelegate.eof()) {
            myDelegate.advanceLexer();
        }
        error.error(LocalizeValue.localizeTODO("Unexpected tokens"));
    }

    @Override
    public String getTokenText() {
        return text();
    }

    @Override
    public PsiBuilder.Marker mark() {
        flushDirectives();
        return markNoFlush();
    }

    /**
     * Marker before directives at the current position.
     */
    public PsiBuilder.Marker markNoFlush() {
        return new FSharpMarker(myDelegate.mark(), myIndex, myNextDirective);
    }

    /**
     * Inserts directives placed before the current token.
     */
    public void flushDirectives() {
        while (myNextDirective < myDirectives.length && myDirectives[myNextDirective].anchor() <= myIndex) {
            PendingDirective directive = myDirectives[myNextDirective++];
            PsiBuilder.Marker marker = myDelegate.mark();
            for (int i = 0; i < directive.count(); i++) {
                myDelegate.advanceLexer();
            }
            marker.done(directiveType(directive.text()));
        }
    }

    private static IElementType directiveType(String text) {
        String name = text.split("\\s", 2)[0];
        return switch (name) {
            case "#nowarn", "#warnon" -> FSharpElementTypes.WARNING_DIRECTIVE;
            case "#load" -> FSharpElementTypes.LOAD_DIRECTIVE;
            case "#r", "#reference" -> FSharpElementTypes.REFERENCE_DIRECTIVE;
            case "#I", "#Include" -> FSharpElementTypes.I_DIRECTIVE;
            case "#line" -> FSharpElementTypes.LINE_DIRECTIVE;
            default -> name.length() > 1 && Character.isDigit(name.charAt(1)) || name.equals("#")
                ? FSharpElementTypes.LINE_DIRECTIVE
                : FSharpElementTypes.OTHER_DIRECTIVE;
        };
    }

    public boolean is(FSharpLexFilterKind kind) {
        return kind() == kind;
    }

    public boolean consume(FSharpLexFilterKind kind) {
        if (kind() == kind) {
            advanceLexer();
            return true;
        }
        return false;
    }

    public boolean expect(FSharpLexFilterKind kind, String message) {
        if (kind() == kind) {
            advanceLexer();
            return true;
        }
        error(message);
        return false;
    }

    private final class FSharpMarker implements PsiBuilder.Marker {
        private final PsiBuilder.Marker myMarker;
        private final int myMarkerIndex;
        private final int myMarkerDirective;

        private FSharpMarker(PsiBuilder.Marker marker, int index, int directive) {
            myMarker = marker;
            myMarkerIndex = index;
            myMarkerDirective = directive;
        }

        private static PsiBuilder.Marker unwrap(PsiBuilder.Marker marker) {
            return marker instanceof FSharpMarker fsharpMarker ? fsharpMarker.myMarker : marker;
        }

        @Override
        public PsiBuilder.Marker precede() {
            return new FSharpMarker(myMarker.precede(), myMarkerIndex, myMarkerDirective);
        }

        @Override
        public void drop() {
            myMarker.drop();
        }

        @Override
        public void rollbackTo() {
            myMarker.rollbackTo();
            myIndex = myMarkerIndex;
            myNextDirective = myMarkerDirective;
        }

        @Override
        public void done(IElementType type) {
            myMarker.done(type);
        }

        @Override
        public void collapse(IElementType type) {
            myMarker.collapse(type);
        }

        @Override
        public void doneBefore(IElementType type, PsiBuilder.Marker before) {
            myMarker.doneBefore(type, unwrap(before));
        }

        @Override
        public void doneBefore(IElementType type, PsiBuilder.Marker before, LocalizeValue errorMessage) {
            myMarker.doneBefore(type, unwrap(before), errorMessage);
        }

        @Override
        public void error(LocalizeValue message) {
            myMarker.error(message);
        }

        @Override
        public void errorBefore(LocalizeValue message, PsiBuilder.Marker before) {
            myMarker.errorBefore(message, unwrap(before));
        }

        @Override
        public void setCustomEdgeTokenBinders(@Nullable WhitespacesAndCommentsBinder left, @Nullable WhitespacesAndCommentsBinder right) {
            myMarker.setCustomEdgeTokenBinders(left, right);
        }
    }
}
