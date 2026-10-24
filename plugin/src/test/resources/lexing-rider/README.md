Lexer test data converted from JetBrains [resharper-fsharp](https://github.com/JetBrains/resharper-fsharp)
(`rider-fsharp/parserTest/.../lexer/FSharpLexerTest.kt`, commit f3d833155c8f), licensed under the Apache License 2.0.

Each `<test>.fs` is the input of one Rider test, `<test>.txt` the expected `TOKEN ('text')` dump of the
lexer without preprocessing. Token names are mapped to the ReSharper backend names
(`IDENT` -> `IDENTIFIER`, `WHITESPACE` -> `WHITE_SPACE`).
