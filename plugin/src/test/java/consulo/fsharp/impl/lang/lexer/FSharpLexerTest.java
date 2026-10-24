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

import consulo.language.ast.IElementType;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.RestartableLexer;
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
 * Lexer tests on data taken from JetBrains resharper-fsharp:
 * <ul>
 * <li>{@code lexing}: ReSharper backend data, run through {@link FSharpPreprocessedLexer} without defined constants</li>
 * <li>{@code lexing-rider}: Rider frontend data, run through {@link FSharpLexer}</li>
 * </ul>
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public class FSharpLexerTest {
    private static final Path RESHARPER_DATA = Path.of("src/test/resources/lexing");
    private static final Path RIDER_DATA = Path.of("src/test/resources/lexing-rider");

    private record TokenInfo(IElementType type, int start, int end, int state) {
    }

    @TestFactory
    public Stream<DynamicTest> resharper() throws IOException {
        return sources(RESHARPER_DATA).map(source -> DynamicTest.dynamicTest(name(source), () -> {
            String expected = readText(source.resolveSibling(name(source) + ".gold")).strip();
            Assertions.assertEquals(expected, dumpTokens(new FSharpPreprocessedLexer(Set.of()), readText(source)));
        }));
    }

    @TestFactory
    public Stream<DynamicTest> rider() throws IOException {
        return sources(RIDER_DATA).map(source -> DynamicTest.dynamicTest(name(source), () -> {
            String testName = name(source).substring(0, name(source).length() - ".fs".length());
            String expected = readText(source.resolveSibling(testName + ".txt")).strip();
            Assertions.assertEquals(expected, dumpRiderTokens(new FSharpLexer(), readText(source)));
        }));
    }

    /**
     * The editor highlighter restarts lexing from tokens with a restartable state: the result must match a full pass.
     */
    @TestFactory
    public Stream<DynamicTest> restart() throws IOException {
        return Stream.concat(sources(RESHARPER_DATA), sources(RIDER_DATA)).flatMap(source -> Stream.of(
            DynamicTest.dynamicTest("FSharpLexer " + name(source), () -> checkRestart(new FSharpLexer(), readText(source))),
            DynamicTest.dynamicTest("FSharpPreprocessedLexer " + name(source),
                () -> checkRestart(new FSharpPreprocessedLexer(Set.of()), readText(source)))
        ));
    }

    private static void checkRestart(Lexer lexer, String text) {
        List<TokenInfo> tokens = lex(lexer, text, 0, lexer.getState());
        for (int i = 0; i < tokens.size(); i++) {
            TokenInfo token = tokens.get(i);
            if (!((RestartableLexer) lexer).isRestartableState(token.state())) {
                continue;
            }
            List<TokenInfo> restarted = lex(lexer, text, token.start(), token.state());
            Assertions.assertEquals(tokens.subList(i, tokens.size()), restarted, "restart at offset " + token.start());
        }
    }

    private static List<TokenInfo> lex(Lexer lexer, String text, int start, int state) {
        List<TokenInfo> tokens = new ArrayList<>();
        lexer.start(text, start, text.length(), state);
        IElementType tokenType;
        while ((tokenType = lexer.getTokenType()) != null) {
            tokens.add(new TokenInfo(tokenType, lexer.getTokenStart(), lexer.getTokenEnd(), lexer.getState()));
            lexer.advance();
        }
        return tokens;
    }

    private static Stream<Path> sources(Path dir) throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.list(dir)) {
            sources = files.filter(it -> name(it).endsWith(".fs")).sorted().toList();
        }
        return sources.stream();
    }

    private static String name(Path path) {
        return path.getFileName().toString();
    }

    private static String dumpRiderTokens(Lexer lexer, String text) {
        StringBuilder builder = new StringBuilder();
        lexer.start(text);
        IElementType tokenType;
        while ((tokenType = lexer.getTokenType()) != null) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            // Rider escapes only line breaks
            String tokenText = text.substring(lexer.getTokenStart(), lexer.getTokenEnd());
            builder.append(tokenType).append(" ('").append(tokenText.replace("\r", "\\r").replace("\n", "\\n")).append("')");
            lexer.advance();
        }
        return builder.toString();
    }

    public static String dumpTokens(Lexer lexer, String text) {
        StringBuilder builder = new StringBuilder();
        lexer.start(text);
        IElementType tokenType;
        while ((tokenType = lexer.getTokenType()) != null) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(String.format("%04d: %s '", lexer.getTokenStart(), tokenType));
            escape(builder, text.substring(lexer.getTokenStart(), lexer.getTokenEnd()));
            builder.append('\'');
            lexer.advance();
        }
        return builder.toString();
    }

    private static void escape(StringBuilder builder, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(c);
            }
        }
    }

    private static String readText(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return text.startsWith("﻿") ? text.substring(1) : text;
    }
}
