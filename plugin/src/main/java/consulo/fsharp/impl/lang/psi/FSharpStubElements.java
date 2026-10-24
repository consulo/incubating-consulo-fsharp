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
package consulo.fsharp.impl.lang.psi;

import consulo.fsharp.impl.lang.psi.stub.elementTypes.FSharpFileStubElementType;
import consulo.fsharp.impl.lang.psi.stub.elementTypes.FSharpNamespaceStubElementType;
import consulo.fsharp.impl.lang.psi.stub.elementTypes.FSharpTypeStubElementType;

/**
 * Stubbed element types, registered by {@link FSharpStubElementsHolder}. The names are ReSharper node names.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public interface FSharpStubElements {
    FSharpFileStubElementType IMPL_FILE = new FSharpFileStubElementType("FSharpImplFile", "fsharp.file");
    FSharpFileStubElementType SIG_FILE = new FSharpFileStubElementType("FSharpSigFile", "fsharp.sig.file");

    FSharpNamespaceStubElementType NAMED_NAMESPACE_DECLARATION = new FSharpNamespaceStubElementType("NamedNamespaceDeclaration");
    FSharpNamespaceStubElementType GLOBAL_NAMESPACE_DECLARATION = new FSharpNamespaceStubElementType("GlobalNamespaceDeclaration");

    FSharpTypeStubElementType NAMED_MODULE_DECLARATION = new FSharpTypeStubElementType("NamedModuleDeclaration");
    FSharpTypeStubElementType ANON_MODULE_DECLARATION = new FSharpTypeStubElementType("AnonModuleDeclaration");
    FSharpTypeStubElementType NESTED_MODULE_DECLARATION = new FSharpTypeStubElementType("NestedModuleDeclaration");
    FSharpTypeStubElementType FSHARP_TYPE_DECLARATION = new FSharpTypeStubElementType("FSharpTypeDeclaration");
    FSharpTypeStubElementType EXCEPTION_DECLARATION = new FSharpTypeStubElementType("ExceptionDeclaration");
}
