namespace Fs.Geometry

open System

type Shape =
    | Circle of Radius: float
    | Rectangle of Width: float * Height: float
    | Empty

type Measurement = { Name: string; Area: float; Perimeter: float }

type Square(side: float) =
    member _.Side = side
    member _.Area() = side * side
    static member Unit = Square(1.0)

type IArea =
    abstract Area: unit -> float

module ShapeOps =
    let name shape =
        match shape with
        | Circle _ -> "Circle"
        | Rectangle _ -> "Rectangle"
        | Empty -> "Empty"

    let area (shape: Shape) : float =
        match shape with
        | Circle r -> Math.PI * r * r
        | Rectangle (w, h) -> w * h
        | Empty -> 0.0

    let scale (factor: float) (shape: Shape) = shape

    let defaultName = "shape"

    [<Literal>]
    let Sides = 4

module Shape =
    let isRound shape = match shape with Circle _ -> true | _ -> false
