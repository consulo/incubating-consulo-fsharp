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

package consulo.fsharp.impl.editor.folding;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.folding.FoldingBuilderEx;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Folds multi-line declarations after their first line, block comments and open statement groups.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class FSharpFoldingBuilder extends FoldingBuilderEx {
    private static final TokenSet FOLDED_DECLARATIONS = TokenSet.create(
        FSharpElementTypes.NESTED_MODULE_DECLARATION,
        FSharpElementTypes.FSHARP_TYPE_DECLARATION,
        FSharpElementTypes.TOP_BINDING,
        FSharpElementTypes.MEMBER_DECLARATION,
        FSharpElementTypes.MATCH_EXPR,
        FSharpElementTypes.COMPUTATION_EXPR,
        FSharpElementTypes.RECORD_EXPR,
        FSharpElementTypes.LIST_EXPR,
        FSharpElementTypes.ARRAY_EXPR
    );

    @RequiredReadAction
    @Override
    public FoldingDescriptor[] buildFoldRegions(PsiElement root, Document document, boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        collect(root.getNode(), document, descriptors);
        return descriptors.toArray(FoldingDescriptor.EMPTY);
    }

    private static void collect(ASTNode node, Document document, List<FoldingDescriptor> descriptors) {
        IElementType type = node.getElementType();
        TextRange range = node.getTextRange();
        if (FOLDED_DECLARATIONS.contains(type) && isMultiline(document, range)) {
            int lineEnd = document.getLineEndOffset(document.getLineNumber(range.getStartOffset()));
            int end = trimmedEnd(node);
            if (end > lineEnd) {
                descriptors.add(new FoldingDescriptor(node, new TextRange(lineEnd, end)));
            }
        }
        else if (type == FSharpTokenType.BLOCK_COMMENT && isMultiline(document, range)) {
            descriptors.add(new FoldingDescriptor(node, range));
        }
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            collect(child, document, descriptors);
        }
    }

    private static int trimmedEnd(ASTNode node) {
        ASTNode last = node.getLastChildNode();
        while (last != null && (FSharpTokenType.WHITESPACES.contains(last.getElementType()) || FSharpTokenType.COMMENTS.contains(last.getElementType()))) {
            last = last.getTreePrev();
        }
        return last == null ? node.getTextRange().getEndOffset() : last.getTextRange().getEndOffset();
    }

    private static boolean isMultiline(Document document, TextRange range) {
        return document.getLineNumber(range.getStartOffset()) != document.getLineNumber(range.getEndOffset());
    }

    @Override
    public @Nullable String getPlaceholderText(ASTNode node) {
        return node.getElementType() == FSharpTokenType.BLOCK_COMMENT ? "(*...*)" : " ...";
    }

    @Override
    public boolean isCollapsedByDefault(ASTNode node) {
        return false;
    }

    @Override
    public Language getLanguage() {
        return FSharpLanguage.INSTANCE;
    }
}
