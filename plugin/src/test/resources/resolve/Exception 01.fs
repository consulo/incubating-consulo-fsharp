exception <target>MyError of string

let f () = raise (<caret>MyError "x")