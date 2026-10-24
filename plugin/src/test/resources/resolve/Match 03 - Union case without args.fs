type U =
    | A
    | <target>B

let f u =
    match u with
    | A -> 1
    | <caret>B -> 0