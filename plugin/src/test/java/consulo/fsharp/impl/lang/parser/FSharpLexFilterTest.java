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

import consulo.fsharp.impl.lang.FSharpParserDefinition;
import consulo.fsharp.impl.lang.lexer.FSharpPreprocessedLexer;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.IElementType;
import consulo.language.lexer.Lexer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Compares {@link FSharpLexFilter} output with the F# compiler: gold files in {@code lexfilter} are produced by
 * {@code fsc --tokenize} for the {@code parsing} test sources (see README there).
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpLexFilterTest {
    private static final Path SOURCES = Path.of("src/test/resources/parsing");
    private static final Path GOLD = Path.of("src/test/resources/lexfilter");
    private static final Path PASSING = Path.of("src/test/resources/lexfilter-passing.txt");

    @TestFactory
    public Stream<DynamicTest> compiler() throws IOException {
        Set<String> passing = Set.copyOf(Files.readAllLines(PASSING, StandardCharsets.UTF_8));
        List<String> failed = new ArrayList<>();
        Path dumps = Path.of("target/lexfilter-dumps");
        Files.createDirectories(dumps);
        List<DynamicTest> tests = new ArrayList<>();
        try (Stream<Path> files = Files.list(GOLD)) {
            for (Path gold : files.filter(p -> p.toString().endsWith(".lexfilter")).sorted().toList()) {
                String fileName = gold.getFileName().toString();
                String name = fileName.substring(0, fileName.length() - ".lexfilter".length());
                String expected = Files.readString(gold, StandardCharsets.UTF_8).strip();
                String actual = dump(Files.readString(SOURCES.resolve(name), StandardCharsets.UTF_8));
                if (!expected.equals(actual)) {
                    failed.add(name);
                    Files.writeString(dumps.resolve(name + ".lexfilter"), actual + "\n", StandardCharsets.UTF_8);
                }
                if (passing.contains(name)) {
                    tests.add(DynamicTest.dynamicTest(name, () -> Assertions.assertEquals(expected, actual)));
                }
            }
        }
        Files.write(Path.of("target/lexfilter-failed.txt"), failed, StandardCharsets.UTF_8);
        return tests.stream();
    }

    /**
     * Optional check on a bigger corpus: {@code FSHARP_LEXFILTER_CORPUS} points to a directory with {@code src/<name>}
     * and {@code gold/<name>.lexfilter} made the same way as {@code lexfilter}.
     */
    @TestFactory
    public Stream<DynamicTest> corpus() throws IOException {
        String corpus = System.getenv("FSHARP_LEXFILTER_CORPUS");
        if (corpus == null) {
            return Stream.empty();
        }
        Path root = Path.of(corpus);
        Path dumps = Path.of("target/lexfilter-corpus-dumps");
        Files.createDirectories(dumps);
        List<DynamicTest> tests = new ArrayList<>();
        try (Stream<Path> files = Files.list(root.resolve("gold"))) {
            for (Path gold : files.sorted().toList()) {
                String fileName = gold.getFileName().toString();
                String name = fileName.substring(0, fileName.length() - ".lexfilter".length());
                String expected = Files.readString(gold, StandardCharsets.UTF_8).strip();
                String actual = dump(Files.readString(root.resolve("src").resolve(name), StandardCharsets.UTF_8));
                if (!expected.equals(actual)) {
                    Files.writeString(dumps.resolve(name + ".lexfilter"), actual + "\n", StandardCharsets.UTF_8);
                }
                tests.add(DynamicTest.dynamicTest(name, () -> Assertions.assertEquals(expected, actual)));
            }
        }
        return tests.stream();
    }

    static String dump(String source) {
        // the editor strips the byte order mark
        String text = source.startsWith("\uFEFF") ? source.substring(1) : source;
        FSharpLexFilterKinds.Collector collector = new FSharpLexFilterKinds.Collector(text);
        // fsc defines COMPILED
        Lexer lexer = new FSharpPreprocessedLexer(Set.of("COMPILED"));
        lexer.start(text);
        while (lexer.getTokenType() != null) {
            IElementType type = lexer.getTokenType();
            if (!FSharpTokenType.WHITESPACES.contains(type) && !FSharpParserDefinition.COMMENTS.contains(type)) {
                collector.add(type, lexer.getTokenStart(), lexer.getTokenEnd());
            }
            lexer.advance();
        }
        StringBuilder builder = new StringBuilder();
        for (FSharpLexFilter.Token token : FSharpLexFilter.filter(text, collector.getTokens())) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(token.kind()).append(" (").append(token.start().line()).append(',').append(Math.max(0, token.start().col())).append(')');
        }
        return builder.toString();
    }
}
