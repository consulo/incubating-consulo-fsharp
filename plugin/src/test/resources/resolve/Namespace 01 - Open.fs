namespace Ns

module M =
    let <target>f x = x

namespace Other

open Ns.M

module N =
    let a = <caret>f 1