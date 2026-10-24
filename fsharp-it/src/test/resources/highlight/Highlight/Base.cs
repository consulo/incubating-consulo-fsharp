namespace Cs.Core
{
    public interface IShape
    {
        string Name { get; }
        double Area();
    }

    public abstract class ShapeBase : IShape
    {
        public const int Sides = 0;
        public static int Count;

        protected ShapeBase(string name)
        {
            Name = name;
        }

        public string Name { get; }
        public abstract double Area();
        public static string Describe(IShape shape) => shape.Name;
    }

    public enum Color
    {
        Red,
        Green
    }
}
