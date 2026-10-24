namespace Fs.Geometry

open System
open Cs.Core

type Shape =
    | Circle of Radius: float
    | Rectangle of Width: float * Height: float

type Measurement = { Name: string; Area: float }

type Kind =
    | Small = 0
    | Big = 1

type IMeasured =
    abstract Measure: unit -> Measurement

type Square(side: float) =
    inherit ShapeBase("Square")
    member _.Side = side
    member val Label = "square" with get, set
    override _.Area() = side * side

module ShapeOps =
    let origin = 0.0

    let area shape =
        match shape with
        | Circle r -> Math.PI * r * r
        | Rectangle (w, h) -> w * h

    let measure (shape: IShape) : Measurement =
        let area = shape.Area()
        { Name = ShapeBase.Describe(shape); Area = area }

    let count () = ShapeBase.Count + ShapeBase.Sides

    let twice f x = f (f x)

    let doubled = twice (fun x -> x * 2.0) origin
