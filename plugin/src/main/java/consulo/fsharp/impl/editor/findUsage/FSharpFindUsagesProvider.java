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

package consulo.fsharp.impl.editor.findUsage;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.lexer.FSharpLexer;
import consulo.fsharp.impl.lang.psi.FSharpElementTypes;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.fsharp.impl.lang.psi.impl.FSharpNamedElementImpl;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.cacheBuilder.DefaultWordsScanner;
import consulo.language.cacheBuilder.WordsScanner;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl
public class FSharpFindUsagesProvider implements FindUsagesProvider {
    @Override
    public WordsScanner getWordsScanner() {
        return new DefaultWordsScanner(new FSharpLexer(), TokenSet.create(FSharpTokenType.IDENTIFIER), FSharpTokenType.COMMENTS,
            FSharpTokenType.STRINGS);
    }

    @Override
    public boolean canFindUsagesFor(PsiElement element) {
        return element instanceof FSharpNamedElementImpl || element instanceof consulo.fsharp.impl.lang.psi.impl.FSharpStubElementImpl<?>;
    }

    @Override
    public String getType(PsiElement element) {
        IElementType type = element.getNode().getElementType();
        if (type == FSharpElementTypes.FSHARP_TYPE_DECLARATION) {
            return "type";
        }
        if (type == FSharpElementTypes.NESTED_MODULE_DECLARATION || type == FSharpElementTypes.NAMED_MODULE_DECLARATION) {
            return "module";
        }
        if (type == FSharpElementTypes.NAMED_NAMESPACE_DECLARATION) {
            return "namespace";
        }
        if (type == FSharpElementTypes.UNION_CASE_DECLARATION) {
            return "union case";
        }
        if (type == FSharpElementTypes.RECORD_FIELD_DECLARATION) {
            return "field";
        }
        if (type == FSharpElementTypes.EXCEPTION_DECLARATION) {
            return "exception";
        }
        if (type == FSharpElementTypes.MEMBER_DECLARATION || type == FSharpElementTypes.ABSTRACT_MEMBER_DECLARATION
            || type == FSharpElementTypes.AUTO_PROPERTY_DECLARATION) {
            return "member";
        }
        if (type == FSharpElementTypes.TOP_REFERENCE_PAT) {
            return "value";
        }
        return "local";
    }

    @RequiredReadAction
    @Override
    public String getDescriptiveName(PsiElement element) {
        String name = element instanceof PsiNamedElement named ? named.getName() : null;
        return name == null ? element.getText() : name;
    }

    @RequiredReadAction
    @Override
    public String getNodeText(PsiElement element, boolean useFullName) {
        return getDescriptiveName(element);
    }

    @Override
    public Language getLanguage() {
        return FSharpLanguage.INSTANCE;
    }
}
