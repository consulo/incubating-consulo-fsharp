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

package consulo.fsharp.impl.lang.psi.stub;

import consulo.fsharp.impl.lang.psi.impl.FSharpNamespaceDeclarationImpl;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubBase;
import consulo.language.psi.stub.StubElement;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpNamespaceStub extends StubBase<FSharpNamespaceDeclarationImpl> {
    private final String myQName;

    public FSharpNamespaceStub(@Nullable StubElement parent, IStubElementType elementType, String qName) {
        super(parent, elementType);
        myQName = qName;
    }

    /**
     * Qualified name: {@code A.B} of {@code namespace A.B}, empty for {@code namespace global}.
     */
    public String getQName() {
        return myQName;
    }
}
