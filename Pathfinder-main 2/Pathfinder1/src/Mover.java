import java.util.List;

import processing.core.PApplet;

public class Mover extends Point {
  MoveRule rule;
  float speed = 2.0f;
  float hue;
  Pathfinder pf;

  Mover(Pathfinder pf) {
    this(pf, new StandStill());
  }

  Mover(Pathfinder pf, MoveRule rule) {
    super(pf);
    this.pf = pf;
    this.rule = rule;
    hue = pf.random(0, 360);
  }

  Point asPoint() {
    return new Point(x, y);
  }

  void move() {
    rule.move(this);
  }

  void moveTo(Point p) {
    if (p != null) moveTo(p.x, p.y);
  }

  void moveTo(double x2, double y2) {
    moveTo((float) x2, (float) y2);
  }

  void moveTo(float x2, float y2) {
    float dx = x2 - x;
    float dy = y2 - y;
    double d = Math.sqrt(dx * dx + dy * dy);

    if (d <= Point.EPSILON) return;

    if (d > speed) {
      dx *= speed / d;
      dy *= speed / d;
    }

    Point target = new Point(x + dx, y + dy);
    Point crashPoint = crashCheck(target);

    if (crashPoint == null) {
      x += dx;
      y += dy;
    }
    else if (distTo(crashPoint) >= 0.5) {
      x = (x + crashPoint.x) / 2;
      y = (y + crashPoint.y) / 2;
    }
  }

  Point crashCheck(Point target) {
    Point closest = null;
    double closestD = Double.MAX_VALUE;
    List<Point> crashes = pf.wsCurr.intersections(this, target);

    for (Point crash : crashes) {
      double crashD = distTo(crash);
      if (crashD < closestD) {
        closest = crash;
        closestD = crashD;
      }
    }

    return closest;
  }

  @Override
  void display(PApplet pa) {
    pa.stroke(hue, 52, 64, 96);
    pa.strokeWeight(2);
    pa.fill(hue, 18, 96, 96);
    pa.ellipse(x, y, 16, 16);
  }
}

class Player extends Mover {
  Player(Pathfinder pf) {
    super(pf, new MoveTo(pf.mouse));
    speed = 3.1f;
    hue = 198;
  }

  @Override
  void display(PApplet pa) {
    pa.stroke(hue, 48, 60, 96);
    pa.strokeWeight(2);
    pa.fill(hue, 16, 98, 98);
    pa.ellipse(x, y, 24, 24);
    pa.stroke(hue, 24, 72, 78);
    pa.noFill();
    pa.ellipse(x, y, 34, 34);
  }
}
