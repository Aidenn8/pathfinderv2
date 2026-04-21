import processing.core.PApplet;

// simple class representing a point in 2d space, which
//   may or may not be part of a Wall
class Point {
  float x, y;
  Wall wall;
  Point prev; //if there is a problem w/bfs ghost and djikstra's could be from be using the same prev data object (not very likely though)
  
  double distanceFromStart;//distance of point from the starting point (used for djikstra's algorithm)
  
  
  
  // random point
  Point(PApplet pa) {
    x = pa.random(0, pa.width);
    y = pa.random(0, pa.height);
    prev=null;
    //distanceFromStart = Double.MAX_VALUE;//make the distance a very large number (max)
    //this is so that when comparing other distances to an "unitialized" distance from start, it will choose the initialized one over the untialized
  }
  
  // specific point
  Point(float x, float y) {
    this.x = x;
    this.y = y;
  }
  
  // copy constructor
  Point(Point other) {
    this.x = other.x;
    this.y = other.y;
  }
  
  // default display (bright red)
  void display(PApplet pa) {
    display(pa, 0, 100, 100);
  }
  
  // display with custom color
  void display(PApplet pa, int h, int s, int b) {
    pa.stroke(h,s,b);
    pa.strokeWeight(3);  // medium
    pa.noFill();
    pa.ellipse(x, y, 10, 10);
  }
  
  // distance to other point
  double distTo(Point other) {
    float dx = other.x - this.x;
    float dy = other.y - this.y;
    return Math.sqrt(dx*dx + dy*dy);
  }

  // finds intersection of line a1-a2 with b1-b2
  //   or null if there is none
  static Point intersection(Point a1, Point a2, Point b1, Point b2) {
    // if line b is vertical, swap it with line a, so "a is vertical"
    //   code will apply just as easily to the opposite case
    if (b1.x == b2.x) {
      Point c1 = a1;
      Point c2 = a2;
      a1 = b1;
      a2 = b2;
      b1 = c1;
      b2 = c2;
    }
    // line a is vertical
    if (a1.x == a2.x) {
      // both are vertical -> parallel -> no intersection
      if (b1.x == b2.x) return null;
      
      // line b is left or right of line a
      if (Math.min(b1.x, b2.x) < a1.x ||
          Math.max(b1.x, b2.x) > a1.x) return null;
      
      // calculate intercept
      float yIntercept = b1.y + (b1.y-b2.y)/(b1.x-b2.x)*(a1.x - b1.x);
      
      // line b is above or below line a
      if (yIntercept < Math.min(a1.y, a2.y) ||
          yIntercept > Math.max(a1.y, a2.y)) return null;
      
      // line b does intercept a at its x coordinate
      return new Point(a1.x, yIntercept);
    }
    
    // we now know neither line is vertical
    // we wish to find a point where ya = yb, xa = xb
    //   point-slope form -> slope-intercept
    //     y - y1 = m(x-x1)
    //     y = mx + (y1 - mx1)
    //   set y's equal to solve the system of equations
    //     ma*x + (y1a - ma*x1a) = mb*x + (y1b - mb*x1b)
    //     x*(ma - mb) = y1b - mb*x1b - y1a + ma*x1a
    //     x = (y1b - mb*x1b - y1a + ma*x1a) / (ma - mb)
    
    float aSlope = (a1.y - a2.y) / (a1.x - a2.x);
    float bSlope = (b1.y - b2.y) / (b1.x - b2.x);
    
    // parallel lines don't intersect
    if (aSlope == bSlope) return null;
    
    // use solution above to find where xs intersect
    float xIntercept = (b1.y - bSlope*b1.x - a1.y + aSlope*a1.x)
                     / (aSlope - bSlope);
    
    // check if x is outside of bounds of either line
    if (xIntercept < Math.min(a1.x, a2.x) ||
        xIntercept > Math.max(a1.x, a2.x) ||
        xIntercept < Math.min(b1.x, b2.x) ||
        xIntercept > Math.max(b1.x, b2.x)) return null;
    
    // lines do truly intersect; calculate y and return
    float yIntercept = a1.y + (xIntercept - a1.x) * aSlope;
    return new Point(xIntercept, yIntercept);
  }
  
  public String toString() {
    return "(" + x + "," + y + ")";
  }
  
  // equal if two Points have same coordinates
  public boolean equals(Object other) {
    if (!(other instanceof Point)) return false;
    
    Point pOther = (Point) other;
    return this.x == pOther.x && this.y == pOther.y;
  }
}