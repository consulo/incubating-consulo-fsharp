Expected `FSharpLexFilter` output for the `parsing` test sources, produced by the F# compiler itself
(`fsc --tokenize`, F# 10.0, .NET SDK 10.0.112):

```
dotnet <sdk>/FSharp/fsc.dll --nologo --tokenize "<name>.fs" > "<dumps>/<name>.fs.txt"
python3 normalize.py <dumps> .
```

`normalize.py` keeps `KIND (line,column)` of the token start, drops the parser recovery tokens (`*_COMING_SOON`)
and maps `*_IS_HERE` to the original token.

`lexfilter-passing.txt` lists the files that must match. Known differences:
`...` record spread (this compiler version has no `DOT_DOT_DOT` token) and `..^` (kept as one token).
