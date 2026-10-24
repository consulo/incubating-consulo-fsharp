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

import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.test.junit.impl.extension.ConsuloProjectLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parses the ReSharper F# parser test data ({@code src/test/resources/parsing}) and compares trees structurally:
 * composite nodes and significant tokens, ignoring whitespace, comments and {@code IChameleonExpression} wrappers.
 * <p>
 * Cases listed in {@code parsing-passing.txt} must pass, other cases are reported as skipped. The status of every
 * case is written to {@code target/parsing-report.txt}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtendWith(ConsuloProjectLoader.class)
public class FSharpParsingTest {
    private static final Path TEST_DATA = Path.of("src/test/resources/parsing");
    private static final Path PASSING = Path.of("src/test/resources/parsing-passing.txt");
    private static final Path REPORT = Path.of("target/parsing-report.txt");
    private static final Path DUMPS = Path.of("target/parsing-dumps");

    private static final Pattern LEAF = Pattern.compile("^\\S+\\(type:([A-Z_0-9]+), text:(.*)\\)( spaces:.*)?$");

    private static final Set<String> SKIPPED_TOKENS = Set.of(
        "WHITE_SPACE", "NEW_LINE", "LINE_COMMENT", "BLOCK_COMMENT", "DEAD_CODE", "SHEBANG", "BAD_TAB",
        "PP_IF_SECTION", "PP_ELIF_SECTION", "PP_ELSE_SECTION", "PP_ENDIF", "PP_CONDITIONAL_SYMBOL", "PP_AND", "PP_OR",
        "PP_NOT", "PP_LPAR", "PP_RPAR", "PP_BAD_CHARACTER"
    );

    private static final TokenSet SKIPPED = TokenSet.orSet(FSharpTokenType.WHITESPACES, FSharpTokenType.COMMENTS, TokenSet.create(
        FSharpTokenType.DEAD_CODE,
        FSharpTokenType.PP_IF_SECTION,
        FSharpTokenType.PP_ELIF_SECTION,
        FSharpTokenType.PP_ELSE_SECTION,
        FSharpTokenType.PP_ENDIF,
        FSharpTokenType.PP_CONDITIONAL_SYMBOL,
        FSharpTokenType.PP_AND,
        FSharpTokenType.PP_OR,
        FSharpTokenType.PP_NOT,
        FSharpTokenType.PP_LPAR,
        FSharpTokenType.PP_RPAR,
        FSharpTokenType.PP_BAD_CHARACTER
    ));

    private static final class Node {
        final String myText;
        final List<Node> myChildren = new ArrayList<>();

        Node(String text) {
            myText = text;
        }
    }

    @TestFactory
    public Stream<DynamicTest> resharper(PsiFileFactory factory) throws IOException {
        return goldTests(factory, TEST_DATA, ".fs", PASSING, REPORT);
    }

    /**
     * Signature files from {@code parsing-signatures}, also ReSharper data.
     */
    @TestFactory
    public Stream<DynamicTest> signatures(PsiFileFactory factory) throws IOException {
        return goldTests(factory, Path.of("src/test/resources/parsing-signatures"), ".fsi",
            Path.of("src/test/resources/parsing-signatures-passing.txt"), Path.of("target/parsing-signatures-report.txt"));
    }

    private static Stream<DynamicTest> goldTests(PsiFileFactory factory, Path data, String extension, Path passingFile, Path reportFile)
        throws IOException {
        Set<String> passing = new HashSet<>();
        if (Files.exists(passingFile)) {
            for (String line : Files.readAllLines(passingFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    passing.add(line.strip());
                }
            }
        }

        List<Path> sources;
        try (Stream<Path> files = Files.list(data)) {
            sources = files.filter(it -> it.getFileName().toString().endsWith(extension)).sorted().toList();
        }

        Map<String, Boolean> report = new TreeMap<>();
        List<DynamicTest> tests = new ArrayList<>();
        for (Path source : sources) {
            String name = source.getFileName().toString();
            tests.add(DynamicTest.dynamicTest(name, () -> {
                String expected = normalizeGold(readText(source.resolveSibling(name + ".gold")));
                PsiFile file = factory.createFileFromText(name, FSharpLanguage.INSTANCE, readText(source));
                String actual = dump(file.getNode());
                boolean ok = expected.equals(actual);
                report.put(name, ok);
                if (!ok) {
                    Files.createDirectories(DUMPS);
                    Files.writeString(DUMPS.resolve(name + ".expected"), expected, StandardCharsets.UTF_8);
                    Files.writeString(DUMPS.resolve(name + ".actual"), actual, StandardCharsets.UTF_8);
                }
                writeReport(report, reportFile);
                if (passing.contains(name)) {
                    Assertions.assertEquals(expected, actual);
                }
                else {
                    Assumptions.assumeTrue(ok, "not supported yet");
                }
            }));
        }
        return tests.stream();
    }

    /**
     * Incomplete code: every prefix of the test files must parse, keeping all of the text in the tree.
     */
    @TestFactory
    public Stream<DynamicTest> incomplete(PsiFileFactory factory) throws IOException {
        List<Path> sources;
        try (Stream<Path> files = Files.list(TEST_DATA)) {
            sources = files.filter(it -> it.getFileName().toString().endsWith(".fs")).sorted().toList();
        }
        return sources.stream().map(source -> DynamicTest.dynamicTest(source.getFileName().toString(), () -> {
            String text = readText(source);
            for (int length = 0; length <= text.length(); length += 3) {
                String prefix = text.substring(0, length);
                PsiFile file = factory.createFileFromText("a.fs", FSharpLanguage.INSTANCE, prefix);
                Assertions.assertEquals(prefix, file.getNode().getText(), "prefix of length " + length);
            }
        }));
    }

    /**
     * Optional check on real code: {@code FSHARP_LEXFILTER_CORPUS} points to a directory with sources in {@code src}
     * (see {@code FSharpLexFilterTest}). Every file must keep its text; syntax errors are counted in
     * {@code target/parsing-corpus.txt}.
     */
    @TestFactory
    public Stream<DynamicTest> corpus(PsiFileFactory factory) throws IOException {
        String corpus = System.getenv("FSHARP_LEXFILTER_CORPUS");
        if (corpus == null) {
            return Stream.empty();
        }
        List<Path> sources;
        try (Stream<Path> files = Files.list(Path.of(corpus).resolve("src"))) {
            sources = files.sorted().toList();
        }
        Map<String, String> report = new TreeMap<>();
        return sources.stream().map(source -> DynamicTest.dynamicTest(source.getFileName().toString(), () -> {
            String text = readText(source);
            if (text.startsWith("\uFEFF")) {
                text = text.substring(1);
            }
            PsiFile file = factory.createFileFromText(source.getFileName().toString(), FSharpLanguage.INSTANCE, text);
            Assertions.assertEquals(text, file.getNode().getText());
            List<String> errors = new ArrayList<>();
            collectErrors(file, text, errors);
            report.put(source.getFileName().toString(), errors.size() + (errors.isEmpty() ? "" : " " + String.join("; ", errors.subList(0, Math.min(3, errors.size())))));
            StringBuilder builder = new StringBuilder();
            long clean = report.values().stream().filter(it -> it.equals("0")).count();
            builder.append("without errors ").append(clean).append(" of ").append(report.size()).append('\n');
            report.forEach((name, value) -> builder.append(name).append(": ").append(value).append('\n'));
            Files.writeString(Path.of("target/parsing-corpus.txt"), builder.toString(), StandardCharsets.UTF_8);
        }));
    }

    private static void collectErrors(consulo.language.psi.PsiElement element, String text, List<String> errors) {
        if (element instanceof PsiErrorElement error) {
            int offset = error.getTextRange().getStartOffset();
            int line = 1;
            for (int i = 0; i < offset; i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                }
            }
            errors.add("line " + line + ": " + error.getErrorDescription());
        }
        for (consulo.language.psi.PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            collectErrors(child, text, errors);
        }
    }

    private static void writeReport(Map<String, Boolean> report, Path reportFile) throws IOException {
        StringBuilder builder = new StringBuilder();
        long passed = report.values().stream().filter(it -> it).count();
        builder.append("passed ").append(passed).append(" of ").append(report.size()).append('\n');
        report.forEach((name, ok) -> builder.append(ok ? "+ " : "- ").append(name).append('\n'));
        Files.createDirectories(reportFile.getParent());
        Files.writeString(reportFile, builder.toString(), StandardCharsets.UTF_8);
    }

    // ReSharper gold

    static String normalizeGold(String gold) {
        Node root = new Node("ROOT");
        List<Node> stack = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        stack.add(root);
        depths.add(-1);
        for (String line : gold.split("\n")) {
            if (line.isBlank() || line.startsWith("Language:")) {
                continue;
            }
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            int depth = indent / 2;
            String content = line.substring(indent).stripTrailing();

            String text;
            if (content.equals("XmlDocBlock")) {
                // doc comments are plain comments in our tree
                text = null;
            }
            else if (content.matches("I[A-Za-z0-9]+")) {
                text = content;
            }
            else {
                Matcher matcher = LEAF.matcher(content);
                if (!matcher.matches()) {
                    text = "?" + content;
                }
                else if (SKIPPED_TOKENS.contains(matcher.group(1))) {
                    text = null;
                }
                else {
                    text = matcher.group(1) + " " + matcher.group(2);
                }
            }

            while (depths.get(depths.size() - 1) >= depth) {
                stack.remove(stack.size() - 1);
                depths.remove(depths.size() - 1);
            }
            Node node = new Node(text == null ? "" : text);
            if (text != null) {
                stack.get(stack.size() - 1).myChildren.add(node);
            }
            stack.add(node);
            depths.add(depth);
        }
        StringBuilder builder = new StringBuilder();
        for (Node child : root.myChildren) {
            print(builder, child, 0);
        }
        return builder.toString().strip();
    }

    private static void print(StringBuilder builder, Node node, int depth) {
        if (node.myText.equals("IChameleonExpression")) {
            for (Node child : node.myChildren) {
                print(builder, child, depth);
            }
            return;
        }
        builder.append("  ".repeat(depth)).append(node.myText).append('\n');
        for (Node child : node.myChildren) {
            print(builder, child, depth + 1);
        }
    }

    // Our tree

    static String dump(ASTNode file) {
        StringBuilder builder = new StringBuilder();
        dump(builder, file, 0);
        return builder.toString().strip();
    }

    private static void dump(StringBuilder builder, ASTNode node, int depth) {
        IElementType type = node.getElementType();
        if (node.getPsi() instanceof PsiErrorElement error) {
            builder.append("  ".repeat(depth)).append("ERROR ").append(error.getErrorDescription()).append('\n');
            for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                dump(builder, child, depth + 1);
            }
            return;
        }
        if (node.getFirstChildNode() == null && !(node.getTreeParent() == null)) {
            if (node.getTextLength() == 0 && !(node.getElementType() instanceof consulo.fsharp.impl.lang.psi.FSharpCompositeElementType)) {
                return;
            }
            if (SKIPPED.contains(type)) {
                return;
            }
            if (!(type instanceof consulo.fsharp.impl.lang.psi.FSharpCompositeElementType)) {
                builder.append("  ".repeat(depth)).append(type).append(' ').append(escape(node.getText())).append('\n');
                return;
            }
        }
        builder.append("  ".repeat(depth)).append('I').append(type).append('\n');
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            dump(builder, child, depth + 1);
        }
    }

    private static String escape(String text) {
        return text.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }

    private static String readText(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }
}
