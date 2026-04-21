import java.util.Set;

import processing.core.PApplet;

// represents a wall in a 2d space
class Wall {
  // endpoints
  Point p1, p2;
  // how big is "buffer zone" around ends of wall where it will not
  //   actually block any movement/paths
  static final float BUFFER = 0.1f;
  
  // constructor: random wall within a PApplet
  Wall(PApplet pa) {
    p1 = new Point(pa);
    p2 = new Point(pa);
    p1.wall = this;
    p2.wall = this;
  }
  
  // constructor: wall from 2 Points
  Wall(Point a, Point b) {
    p1 = a;
    p2 = b;
    a.wall = this;
    b.wall = this;
  }
  
  // constructor: wall (with new Points) from 4 coordinates
  Wall(float x1, float y1, float x2, float y2) {
    this(new Point(x1, y1), new Point(x2, y2));
  }
  
  void display(PApplet pa) {
    pa.stroke(0); // black
    pa.strokeWeight(5);
    pa.line(p1.x, p1.y, p2.x, p2.y);
  }
  
  // returns intersection of path from a to b with this wall, or null
  //   if there is none, allowing for a small "free movement buffer"
  //   at ends of wall that will not count as intersecting the wall
  Point intersection(Point a, Point b) {
    // path actually begins or ends ON THIS WALL
    if (a.wall == this || b.wall == this) {
      // there can be no path EXACTLY along this wall; "intersect"
      //   at beginning of path
      //   (technically, there are infinitely many intersections)
      if (a.wall == b.wall) return a;
      
      // this wall will never block other movement to or away from
      //   its own ends
      // (technically, it should block movement to points collinear
      //   with the wall in the direction opposite this one, but
      //   let's not worry about that)
      return null;
    }
    
    Point crash = Point.intersection(a, b, p1, p2);
    
    if (crash == null) return null;
    
    // can't intersect with points within the buffer zone around the ends
    if (crash.distTo(p1) < BUFFER || crash.distTo(p2) < BUFFER) {
      return null;
    }
    
    return crash;
  }
 
  // generates array code for this wall (x1, y1, x2, y2)
  // NOTE: old version of this project had giant arrays of coordinates
  //   inside the code!
  String arrayString() {
    return "{" + p1.x + "f," + p1.y + "f," + p2.x + "f," + p2.y + "f}";
  }
  
  // human-readable String for this wall
  public String toString() {
    return "{" + p1 + p2 + "}";
  }
  
  // JSON array for 4 coordinates of this wall
  public String toJson() {
    return "[" + p1.x + ", " + p1.y + ", " + p2.x + ", " + p2.y + "]";
  }
}
