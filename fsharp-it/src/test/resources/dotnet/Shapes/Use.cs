using Fs.Geometry;

public class Use
{
    public double M(Shape shape)
    {
        Shape circle = Shape.NewCircle(1.0);
        Shape.Circle c = (Shape.Circle) circle;
        double radius = c.Radius;
        int tag = Shape.Tags.Circle;
        bool round = ShapeModule.isRound(circle);
        Square square = new Square(2.0);
        double side = square.Side;
        Measurement m = new Measurement("a", 1.0, 2.0);
        string name = m.Name;
        string text = ShapeOps.defaultName;
        return ShapeOps.area(shape);
    }
}
