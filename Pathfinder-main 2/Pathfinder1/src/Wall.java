import processing.core.PApplet;

class Wall {
  static final float BUFFER = 0.1f;

  Point p1;
  Point p2;

  Wall(PApplet pa) {
    this(new Point(pa), new Point(pa));
  }

  Wall(Point a, Point b) {
    p1 = a;
    p2 = b;
    p1.wall = this;
    p2.wall = this;
  }

  Wall(float x1, float y1, float x2, float y2) {
    this(new Point(x1, y1), new Point(x2, y2));
  }

  void display(PApplet pa) {
    display(pa, false);
  }

  void display(PApplet pa, boolean showEndpoints) {
    pa.stroke(220, 20, 18, 28);
    pa.strokeWeight(9);
    pa.line(p1.x, p1.y, p2.x, p2.y);

    pa.stroke(210, 45, 24, 95);
    pa.strokeWeight(4);
    pa.line(p1.x, p1.y, p2.x, p2.y);

    if (showEndpoints) {
      p1.display(pa, 205, 70, 85);
      p2.display(pa, 205, 70, 85);
    }
  }

  Point intersection(Point a, Point b) {
    if (a.wall == this || b.wall == this) {
      if (a.wall == b.wall) return a;
      return null;
    }

    Point crash = Point.intersection(a, b, p1, p2);
    if (crash == null) return null;

    if (crash.distTo(p1) < BUFFER || crash.distTo(p2) < BUFFER) return null;
    return crash;
  }

  String arrayString() {
    return "{" + p1.x + "f," + p1.y + "f," + p2.x + "f," + p2.y + "f}";
  }

  public String toString() {
    return "{" + p1 + p2 + "}";
  }

  public String toJson() {
    return "[" + p1.x + ", " + p1.y + ", " + p2.x + ", " + p2.y + "]";
  }
}
