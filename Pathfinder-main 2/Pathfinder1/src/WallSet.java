import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processing.core.PApplet;

public class WallSet {
  final static int STACK_LIMIT = 200;
  final static String PATH_PREFIX = "wallsets/";

  String name = null;
  int mods = 0;

  Set<Wall> walls = new LinkedHashSet<>();
  Set<Point> points = new LinkedHashSet<>();

  Deque<Event> undoStack = new ArrayDeque<>();
  Deque<Event> redoStack = new ArrayDeque<>();

  String getName() {
    if (name == null) return "(untitled wall set)";
    return name + (mods == 0 ? "" : " (modified)");
  }

  void add(Wall w) {
    WallEvent we = new WallEvent(w, true);
    we.doEvent();
    mods++;
    undoStack.push(we);
    redoStack.clear();
  }

  void rem(Wall w) {
    WallEvent we = new WallEvent(w, false);
    we.doEvent();
    mods++;
    undoStack.push(we);
    redoStack.clear();
  }

  void finishMove(Point p, Point old) {
    if (!points.contains(p)) return;

    MoveEvent me = new MoveEvent(p, old);
    mods++;
    undoStack.push(me);
    redoStack.clear();
  }

  void revert() {
    if (name == null) return;

    Set<Wall> backupWalls = walls;
    Set<Point> backupPoints = points;

    WallSet fromFile = fromFile(name);
    if (fromFile == null) return;

    walls = fromFile.walls;
    points = fromFile.points;

    RevertEvent re = new RevertEvent(backupWalls, backupPoints);
    mods = 0;
    undoStack.push(re);
    redoStack.clear();
  }

  String undoPeek() {
    if (undoStack.isEmpty()) return "nothing to undo";
    return "undo " + undoStack.peek();
  }

  String redoPeek() {
    if (redoStack.isEmpty()) return "nothing to redo";
    return "redo " + redoStack.peek();
  }

  boolean undo() {
    if (undoStack.isEmpty()) return false;

    Event ev = undoStack.pop();
    ev.undoEvent();
    redoStack.push(ev);
    if (redoStack.size() > STACK_LIMIT) redoStack.removeLast();

    mods--;
    return true;
  }

  boolean redo() {
    if (redoStack.isEmpty()) return false;

    Event ev = redoStack.pop();
    ev.doEvent();
    undoStack.push(ev);
    if (undoStack.size() > STACK_LIMIT) undoStack.removeLast();

    mods++;
    return true;
  }

  boolean save() {
    if (name == null) return false;

    String path = PATH_PREFIX + name;
    try (PrintWriter out = new PrintWriter(new File(path))) {
      out.println("[");
      int index = 0;
      for (Wall w : walls) {
        out.print("  " + w.toJson());
        if (++index < walls.size()) out.println(",");
        else out.println();
      }
      out.println("]");
    }
    catch (FileNotFoundException fnfe) {
      System.err.println("Failed to save " + path);
      System.err.println("  " + fnfe.getMessage());
      return false;
    }

    mods = 0;
    System.out.println("Saved " + path);
    return true;
  }

  void display(PApplet pa) {
    display(pa, false);
  }

  void display(PApplet pa, boolean showEndpoints) {
    for (Wall w : walls) w.display(pa, showEndpoints);
  }

  List<Point> intersections(Point a, Point b) {
    List<Point> results = new ArrayList<>();

    for (Wall w : walls) {
      Point crash = w.intersection(a, b);
      if (crash != null) results.add(crash);
    }

    return results;
  }

  boolean isClearPath(Point a, Point b) {
    for (Wall w : walls) {
      if (w.intersection(a, b) != null) return false;
    }
    return true;
  }

  static WallSet fromFile(String name) {
    return fromFile(new File(PATH_PREFIX + name));
  }

  static WallSet fromFile(File f) {
    StringBuilder jsonSB = new StringBuilder();
    try (Scanner in = new Scanner(f)) {
      while (in.hasNextLine()) jsonSB.append(in.nextLine().replaceAll("\\s+", ""));
    }
    catch (FileNotFoundException fnfe) {
      System.err.println("Could not read " + f.getName());
      System.err.println("  " + fnfe.getMessage());
      return null;
    }

    String json = jsonSB.toString();
    String number = "-?\\d+(?:\\.\\d+)?";
    String wallPatt = "\\[(" + number + "),(" + number + "),(" + number + "),(" + number + ")]";
    String filePatt = "\\[(" + wallPatt + ",)*(" + wallPatt + ")?,?]";

    if (!json.matches(filePatt)) {
      System.err.println("Invalid file pattern in " + f.getName());
      return null;
    }

    WallSet result = new WallSet();
    result.name = f.getName();

    Matcher m = Pattern.compile(wallPatt).matcher(json);
    while (m.find()) {
      float x1 = Float.parseFloat(m.group(1));
      float y1 = Float.parseFloat(m.group(2));
      float x2 = Float.parseFloat(m.group(3));
      float y2 = Float.parseFloat(m.group(4));

      Wall w = new Wall(x1, y1, x2, y2);
      result.walls.add(w);
      result.points.add(w.p1);
      result.points.add(w.p2);
    }

    System.out.println("Loaded walls from " + f.getName());
    return result;
  }

  interface Event {
    void doEvent();
    void undoEvent();
  }

  class WallEvent implements Event {
    Wall w;
    boolean adding;

    WallEvent(Wall ww, boolean aa) {
      w = ww;
      adding = aa;
    }

    public void doEvent() {
      handleEvent(false);
    }

    public void undoEvent() {
      handleEvent(true);
    }

    void handleEvent(boolean undo) {
      if (adding && !undo || !adding && undo) {
        walls.add(w);
        points.add(w.p1);
        points.add(w.p2);
      }
      else {
        walls.remove(w);
        points.remove(w.p1);
        points.remove(w.p2);
      }
    }

    public String toString() {
      return (adding ? "add " : "remove ") + w.p1 + "-" + w.p2;
    }
  }

  class MoveEvent implements Event {
    Point p;
    Point other;

    MoveEvent(Point p, Point other) {
      this.p = p;
      this.other = other;
    }

    public void doEvent() {
      float temp = p.x;
      p.x = other.x;
      other.x = temp;

      temp = p.y;
      p.y = other.y;
      other.y = temp;
    }

    public void undoEvent() {
      doEvent();
    }

    public String toString() {
      return "move " + other;
    }
  }

  class RevertEvent implements Event {
    Set<Wall> otherWalls;
    Set<Point> otherPoints;

    RevertEvent(Set<Wall> otherWalls, Set<Point> otherPoints) {
      this.otherWalls = otherWalls;
      this.otherPoints = otherPoints;
    }

    public void doEvent() {
      Set<Wall> tempWalls = otherWalls;
      otherWalls = walls;
      walls = tempWalls;

      Set<Point> tempPoints = otherPoints;
      otherPoints = points;
      points = tempPoints;
    }

    public void undoEvent() {
      doEvent();
    }

    public String toString() {
      return "revert";
    }
  }
}
