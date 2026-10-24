Parser test data copied from JetBrains [resharper-fsharp](https://github.com/JetBrains/resharper-fsharp)
(`ReSharper.FSharp/test/data/parsing`, commit f3d833155c8f), licensed under the Apache License 2.0.

Each `<name>.fs` has the expected ReSharper PSI tree in `<name>.fs.gold`. `FSharpParsingTest` compares trees
structurally: whitespace, comments and `IChameleonExpression` wrappers are ignored.
