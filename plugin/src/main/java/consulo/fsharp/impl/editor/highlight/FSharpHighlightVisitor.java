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

package consulo.fsharp.impl.editor.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.colorScheme.TextAttributesKey;
import consulo.dotnet.psi.DotNetConstructorDeclaration;
import consulo.dotnet.psi.DotNetLikeMethodDeclaration;
import consulo.dotnet.psi.DotNetLocalVariable;
import consulo.dotnet.psi.DotNetModifier;
import consulo.dotnet.psi.DotNetModifierListOwner;
import consulo.dotnet.psi.DotNetParameter;
import consulo.dotnet.psi.DotNetPropertyDeclaration;
import consulo.dotnet.psi.DotNetTypeDeclaration;
import consulo.dotnet.psi.DotNetVariable;
import consulo.fsharp.FSharpLanguage;
import consulo.fsharp.impl.lang.psi.impl.FSharpPsiUtil;
import consulo.fsharp.impl.lang.psi.impl.FSharpReferenceExpressionImpl;
import consulo.fsharp.impl.lang.psi.impl.FSharpReferenceNameImpl;
import consulo.fsharp.impl.lang.psi.impl.FSharpReferencePatImpl;
import consulo.fsharp.impl.lang.psi.impl.FSharpTypeDeclarationImpl;
import consulo.fsharp.impl.lang.psi.resolve.FSharpTypeRefs;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoHolder;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNameIdentifierOwner;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

import static consulo.fsharp.impl.lang.psi.FSharpElementTypes.*;

/**
 * Colors declarations and resolved references by what they are: types, modules, union cases, functions, values,
 * members, parameters and locals. References to C# and VB elements are colored by their .NET kind.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class FSharpHighlightVisitor implements HighlightVisitor {
    private static final TokenSet PATTERNS = TokenSet.create(
        LOCAL_REFERENCE_PAT, TOP_REFERENCE_PAT, PARAMETERS_OWNER_PAT, AS_PAT, TYPED_PAT, ATTRIB_PAT, OR_PAT, ANDS_PAT,
        ARRAY_PAT, LIST_PAT, TUPLE_PAT, PAREN_PAT, RECORD_PAT, NAMED_UNION_CASE_FIELDS_PAT, FIELD_PAT,
        OPTIONAL_VAL_PAT, IS_INST_PAT, LIST_CONS_PAT, WILD_PAT, LITERAL_PAT, UNIT_PAT, NULL_PAT
    );

    private @Nullable HighlightInfoHolder myHolder;

    @Override
    @RequiredReadAction
    public void visit(PsiElement element) {
        HighlightInfoHolder holder = myHolder;
        if (holder == null) {
            return;
        }
        highlight(element, (identifier, key) ->
            holder.add(HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(identifier).textAttributes(key).create()));
    }

    /**
     * Name ranges of {@code element} with their colors.
     */
    @RequiredReadAction
    public static void highlight(PsiElement element, BiConsumer<PsiElement, TextAttributesKey> consumer) {
        PsiElement identifier = null;
        TextAttributesKey key = null;
        if (element instanceof FSharpReferencePatImpl pat) {
            identifier = pat.getNameIdentifier();
            if (identifier != null) {
                key = pat.isConstructorReference() ? FSharpHighlightKey.UNION_CASE : keyOfPattern(pat);
            }
        }
        else if (element instanceof FSharpReferenceExpressionImpl reference) {
            identifier = reference.getIdentifier();
            if (identifier != null) {
                key = keyOf(reference.resolve());
            }
        }
        else if (element instanceof FSharpReferenceNameImpl reference) {
            // the name of a pattern declaration is colored by the pattern
            if (reference.getParent() instanceof FSharpReferencePatImpl pat && pat.getNameIdentifier() != null) {
                return;
            }
            identifier = reference.getIdentifier();
            if (identifier != null) {
                PsiElement target = reference.resolve();
                key = isPredefinedType(reference) && !(target instanceof FSharpTypeDeclarationImpl) ? FSharpHighlightKey.PREDEFINED_TYPE : keyOf(target);
            }
        }
        else if (element instanceof PsiNameIdentifierOwner owner && element.getNode() != null) {
            identifier = owner.getNameIdentifier();
            if (identifier != null) {
                key = keyOf(element);
            }
        }
        if (identifier != null && key != null) {
            consumer.accept(identifier, key);
        }
    }

    /**
     * Unqualified {@code int}, {@code string}... in a type; a type of the same name declared in F# source wins
     */
    @RequiredReadAction
    private static boolean isPredefinedType(FSharpReferenceNameImpl reference) {
        if (reference.getNode().getElementType() != TYPE_REFERENCE_NAME || reference.getQualifier() != null) {
            return false;
        }
        String name = reference.getReferenceName();
        return name != null && FSharpTypeRefs.isPredefinedTypeName(name);
    }

    @RequiredReadAction
    public static @Nullable TextAttributesKey keyOf(@Nullable PsiElement element) {
        if (element == null) {
            return null;
        }
        if (element instanceof FSharpTypeDeclarationImpl type) {
            return switch (type.getKind()) {
                case MODULE -> FSharpHighlightKey.MODULE_NAME;
                case INTERFACE -> FSharpHighlightKey.INTERFACE_NAME;
                case STRUCT -> FSharpHighlightKey.STRUCT_NAME;
                case ENUM -> FSharpHighlightKey.ENUM_NAME;
                case RECORD -> FSharpHighlightKey.RECORD_NAME;
                case UNION -> FSharpHighlightKey.UNION_NAME;
                default -> FSharpHighlightKey.CLASS_NAME;
            };
        }
        if (element instanceof FSharpReferencePatImpl pat) {
            return keyOfPattern(pat);
        }
        ASTNode node = element.getNode();
        if (node != null && element.getLanguage() == FSharpLanguage.INSTANCE) {
            IElementType type = node.getElementType();
            if (type == UNION_CASE_DECLARATION || type == ENUM_CASE_DECLARATION) {
                return FSharpHighlightKey.UNION_CASE;
            }
            if (type == TOP_ACTIVE_PATTERN_CASE_DECLARATION || type == LOCAL_ACTIVE_PATTERN_CASE_DECLARATION) {
                return FSharpHighlightKey.UNION_CASE;
            }
            if (type == RECORD_FIELD_DECLARATION || type == VAL_FIELD_DECLARATION) {
                return FSharpHighlightKey.FIELD;
            }
            if (type == AUTO_PROPERTY_DECLARATION) {
                return FSharpHighlightKey.PROPERTY;
            }
            if (type == MEMBER_DECLARATION || type == MEMBER_SIGNATURE || type == ABSTRACT_MEMBER_DECLARATION) {
                return FSharpHighlightKey.METHOD;
            }
            if (type == MODULE_ABBREVIATION_DECLARATION) {
                return FSharpHighlightKey.MODULE_NAME;
            }
            if (type == CTOR_SELF_ID || type == NAMED_SELF_ID) {
                return FSharpHighlightKey.PARAMETER;
            }
            if (type == LOCAL_DECLARATION) {
                return FSharpHighlightKey.LOCAL_VARIABLE;
            }
            return null;
        }
        return keyOfDotNet(element);
    }

    /**
     * C#, VB and library elements, also the compiled view of F# declarations.
     */
    @RequiredReadAction
    private static @Nullable TextAttributesKey keyOfDotNet(PsiElement element) {
        if (element instanceof DotNetTypeDeclaration type) {
            if (type.isInterface()) {
                return FSharpHighlightKey.INTERFACE_NAME;
            }
            if (type.isEnum()) {
                return FSharpHighlightKey.ENUM_NAME;
            }
            if (type.isStruct()) {
                return FSharpHighlightKey.STRUCT_NAME;
            }
            return FSharpHighlightKey.CLASS_NAME;
        }
        if (element instanceof DotNetConstructorDeclaration) {
            return FSharpHighlightKey.METHOD;
        }
        if (element instanceof DotNetLikeMethodDeclaration method) {
            return isStatic(method) ? FSharpHighlightKey.STATIC_METHOD : FSharpHighlightKey.METHOD;
        }
        if (element instanceof DotNetParameter) {
            return FSharpHighlightKey.PARAMETER;
        }
        if (element instanceof DotNetLocalVariable) {
            return FSharpHighlightKey.LOCAL_VARIABLE;
        }
        if (element instanceof DotNetVariable variable) {
            if (variable.isConstant()) {
                return FSharpHighlightKey.CONSTANT;
            }
            if (isStatic(variable)) {
                return FSharpHighlightKey.STATIC_FIELD;
            }
            return element instanceof DotNetPropertyDeclaration ? FSharpHighlightKey.PROPERTY : FSharpHighlightKey.FIELD;
        }
        return null;
    }

    @RequiredReadAction
    private static boolean isStatic(PsiElement element) {
        return element instanceof DotNetModifierListOwner owner && owner.hasModifier(DotNetModifier.STATIC);
    }

    /**
     * {@code let f x = ..}: {@code f} is a function, {@code x} a parameter; {@code let v = ..} is a value,
     * names in match clauses and destructuring are locals.
     */
    @RequiredReadAction
    private static TextAttributesKey keyOfPattern(FSharpReferencePatImpl pat) {
        PsiElement child = pat;
        PsiElement parent = pat.getParent();
        while (parent != null && PATTERNS.contains(parent.getNode().getElementType())) {
            child = parent;
            parent = parent.getParent();
        }
        if (parent == null) {
            return FSharpHighlightKey.LOCAL_VARIABLE;
        }
        IElementType parentType = parent.getNode().getElementType();
        if (parentType == PARAMETERS_PATTERN_DECLARATION || parentType == LAMBDA_PARAMETERS_LIST) {
            return FSharpHighlightKey.PARAMETER;
        }
        if (parentType == TOP_BINDING || parentType == LOCAL_BINDING) {
            boolean head = child == pat;
            if (head && FSharpPsiUtil.findChild(parent, PARAMETERS_PATTERN_DECLARATION) != null) {
                return FSharpHighlightKey.FUNCTION;
            }
            return parentType == TOP_BINDING ? FSharpHighlightKey.VALUE : FSharpHighlightKey.LOCAL_VARIABLE;
        }
        return FSharpHighlightKey.LOCAL_VARIABLE;
    }

    @Override
    public boolean analyze(PsiFile file, boolean updateWholeFile, HighlightInfoHolder holder, Runnable action) {
        myHolder = holder;
        try {
            action.run();
        }
        finally {
            myHolder = null;
        }
        return true;
    }
}
