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

/**
 * What an F# type-like declaration compiles to.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public enum FSharpTypeKind {
    /**
     * Module: a static class.
     */
    MODULE,
    CLASS,
    INTERFACE,
    STRUCT,
    RECORD,
    UNION,
    ENUM,
    DELEGATE,
    EXCEPTION,
    /**
     * {@code type X = int}: no .NET type.
     */
    ABBREVIATION,
    /**
     * {@code [<Measure>] type kg}: no .NET type.
     */
    MEASURE;

    public boolean isDotNetType() {
        return this != ABBREVIATION && this != MEASURE;
    }
}
