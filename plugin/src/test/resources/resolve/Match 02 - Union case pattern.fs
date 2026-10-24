type U =
    | <target>A of int
    | B

let f u =
    match u with
    | <caret>A x -> x
    | B -> 0