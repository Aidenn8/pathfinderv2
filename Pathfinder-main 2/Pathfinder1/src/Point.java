import processing.core.PApplet;

class Point {
  static final float EPSILON = 0.0001f;

  float x;
  float y;
  Wall wall;

  Point(PApplet pa) {
    x = pa.random(0, pa.width);
    y = pa.random(0, pa.height);
  }

  Point(float x, float y) {
    this.x = x;
    this.y = y;
  }

  Point(Point other) {
    this.x = other.x;
    this.y = other.y;
  }

  void display(PApplet pa) {
    display(pa, 0, 80, 95);
  }

  void display(PApplet pa, int h, int s, int b) {
    pa.stroke(h, s, b, 90);
    pa.strokeWeight(2);
    pa.fill(h, 25, 100, 75);
    pa.ellipse(x, y, 10, 10);
  }

  double distTo(Point other) {
    float dx = other.x - x;
    float dy = other.y - y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  boolean sameLocation(Point other) {
    return other != null && Math.abs(x - other.x) <= EPSILON
        && Math.abs(y - other.y) <= EPSILON;
  }

  boolean isNear(Point other, float distance) {
    return other != null && distTo(other) <= distance;
  }

  static Point intersection(Point a1, Point a2, Point b1, Point b2) {
    float rx = a2.x - a1.x;
    float ry = a2.y - a1.y;
    float sx = b2.x - b1.x;
    float sy = b2.y - b1.y;
    float denominator = cross(rx, ry, sx, sy);
    float qpx = b1.x - a1.x;
    float qpy = b1.y - a1.y;

    if (Math.abs(denominator) <= EPSILON) {
      if (Math.abs(cross(qpx, qpy, rx, ry)) > EPSILON) return null;

      float rLengthSquared = rx * rx + ry * ry;
      if (rLengthSquared <= EPSILON) return a1.sameLocation(b1) ? new Point(a1) : null;

      float t0 = dot(qpx, qpy, rx, ry) / rLengthSquared;
      float t1 = t0 + dot(sx, sy, rx, ry) / rLengthSquared;
      float overlapStart = Math.max(0, Math.min(t0, t1));
      float overlapEnd = Math.min(1, Math.max(t0, t1));

      if (overlapStart > overlapEnd + EPSILON) return null;
      return new Point(a1.x + overlapStart * rx, a1.y + overlapStart * ry);
    }

    float t = cross(qpx, qpy, sx, sy) / denominator;
    float u = cross(qpx, qpy, rx, ry) / denominator;

    if (t < -EPSILON || t > 1 + EPSILON || u < -EPSILON || u > 1 + EPSILON) {
      return null;
    }

    return new Point(a1.x + t * rx, a1.y + t * ry);
  }

  static float cross(float ax, float ay, float bx, float by) {
    return ax * by - ay * bx;
  }

  static float dot(float ax, float ay, float bx, float by) {
    return ax * bx + ay * by;
  }

  public String toString() {
    return "(" + x + "," + y + ")";
  }
}
