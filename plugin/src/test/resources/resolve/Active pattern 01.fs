let (|<target>Even|Odd|) n = if n % 2 = 0 then Even else Odd

let f n =
    match n with
    | <caret>Even -> 1
    | Odd -> 0