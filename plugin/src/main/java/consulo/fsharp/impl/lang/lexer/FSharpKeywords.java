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

import consulo.fsharp.impl.lang.psi.FSharpKeywordTokenNodeType;
import consulo.fsharp.impl.lang.psi.FSharpTokenType;
import consulo.language.ast.IElementType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps identifier text to keyword token types.
 * <p>
 * Ported from JetBrains resharper-fsharp (Apache 2.0).
 *
 * @author VISTALL
 * @since 2026-10-23
 */
public final class FSharpKeywords {
    private static final Map<String, IElementType> KEYWORDS = new HashMap<>();

    static {
        for (IElementType type : FSharpTokenType.KEYWORDS.getTypes()) {
            if (type instanceof FSharpKeywordTokenNodeType keyword) {
                KEYWORDS.put(keyword.getRepresentation(), keyword);
            }
        }
    }

    private FSharpKeywords() {
    }

    public static @Nullable IElementType findKeyword(CharSequence buffer, int start, int end) {
        return KEYWORDS.get(buffer.subSequence(start, end).toString());
    }
}
