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

import consulo.fsharp.impl.lang.psi.impl.FSharpTypeDeclarationImpl;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubBase;
import consulo.language.psi.stub.StubElement;
import org.jspecify.annotations.Nullable;

/**
 * Module, type or exception.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public class FSharpTypeStub extends StubBase<FSharpTypeDeclarationImpl> {
    public static final int PRIVATE = 1;
    public static final int INTERNAL = 1 << 1;
    public static final int ABSTRACT = 1 << 2;
    public static final int SEALED = 1 << 3;
    public static final int NESTED = 1 << 4;
    /**
     * {@code [<Struct>]} record or union.
     */
    public static final int STRUCT = 1 << 5;

    private final @Nullable String myName;
    private final String myParentQName;
    private final @Nullable String myVmQName;
    private final FSharpTypeKind myKind;
    private final int myFlags;
    private final int myGenericParametersCount;

    public FSharpTypeStub(@Nullable StubElement parent,
                          IStubElementType elementType,
                          @Nullable String name,
                          String parentQName,
                          @Nullable String vmQName,
                          FSharpTypeKind kind,
                          int flags,
                          int genericParametersCount) {
        super(parent, elementType);
        myName = name;
        myParentQName = parentQName;
        myVmQName = vmQName;
        myKind = kind;
        myFlags = flags;
        myGenericParametersCount = genericParametersCount;
    }

    public @Nullable String getName() {
        return myName;
    }

    /**
     * F# qualified name of the enclosing namespace or module, may be empty.
     */
    public String getParentQName() {
        return myParentQName;
    }

    /**
     * Null when the declaration is no .NET type, see {@link FSharpTypeKind#isDotNetType()}.
     */
    public @Nullable String getVmQName() {
        return myVmQName;
    }

    public FSharpTypeKind getKind() {
        return myKind;
    }

    public int getFlags() {
        return myFlags;
    }

    public boolean is(int flag) {
        return (myFlags & flag) != 0;
    }

    public int getGenericParametersCount() {
        return myGenericParametersCount;
    }
}
