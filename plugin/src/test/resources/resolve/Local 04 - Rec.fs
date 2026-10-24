let f () =
    let rec <target>g n = if n = 0 then 0 else <caret>g (n - 1)
    g 3