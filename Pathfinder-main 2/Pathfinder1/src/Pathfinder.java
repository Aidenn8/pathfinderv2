import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import processing.core.PApplet;

public class Pathfinder extends PApplet {
  private static final long serialVersionUID = 1L;

  static final int APP_WIDTH = 1000;
  static final int APP_HEIGHT = 800;
  static final float REACHED_DISTANCE = 5.0f;

  enum PathStrategy {
    BFS,
    DFS,
    DIJKSTRA
  }

  ArrayList<WallSet> wsList = new ArrayList<>();
  int wsIndex = 0;
  WallSet wsCurr;

  Point mouse = new Point(0, 0);
  Point playerPos = new Point(0, 0);

  Mode[] modes;
  final int BUILD_MODE = 0;
  final int PLAY_MODE = 1;
  int modeIndex = BUILD_MODE;

  boolean ctrlHold;
  boolean shiftHold;

  Map<Mover, List<Point>> debugPaths = new HashMap<>();

  public static void main(String[] args) {
    PApplet.main(new String[] { "Pathfinder" });
  }

  public void setup() {
    size(APP_WIDTH, APP_HEIGHT);
    smooth();
    frameRate(60);
    colorMode(HSB, 360, 100, 100, 100);

    loadWallSets();
    wsIndex = defaultWallSetIndex();
    wsCurr = wsList.get(wsIndex);
    modes = new Mode[] { new BuildMode(), new PlayMode() };
  }

  void loadWallSets() {
    File folder = locateWallSetDirectory();
    WallSet.setDirectory(folder);

    if (folder.exists() && folder.isDirectory()) {
      File[] files = folder.listFiles();
      if (files == null || files.length == 0) {
        System.out.println("No files in wallsets folder.");
      }
      else {
        Arrays.sort(files, new Comparator<File>() {
          public int compare(File a, File b) {
            return a.getName().compareToIgnoreCase(b.getName());
          }
        });

        System.out.println("Loading from wallsets folder:");
        for (File wsFile : files) {
          if (!wsFile.isDirectory()) {
            WallSet ws = WallSet.fromFile(wsFile);
            if (ws != null) wsList.add(ws);
          }
        }
      }

      if (wsList.isEmpty()) System.out.println("No valid files in wallsets folder.");
    }
    else {
      System.err.println("Couldn't find wallsets folder! Should be at "
          + folder.getAbsolutePath());
    }

    if (wsList.isEmpty()) wsList.add(new WallSet());
  }

  File locateWallSetDirectory() {
    File localFolder = new File("wallsets/");
    if (localFolder.exists() && localFolder.isDirectory()) return localFolder;

    try {
      File codeSource = new File(Pathfinder.class.getProtectionDomain()
          .getCodeSource().getLocation().toURI());
      File appFolder = codeSource.isFile() ? codeSource.getParentFile() : codeSource;
      File bundledFolder = new File(appFolder, "wallsets");
      if (bundledFolder.exists() && bundledFolder.isDirectory()) return bundledFolder;
    }
    catch (Exception ex) {
      System.err.println("Could not inspect app folder for wallsets.");
      System.err.println("  " + ex.getMessage());
    }

    return localFolder;
  }

  int defaultWallSetIndex() {
    for (int i = 0; i < wsList.size(); i++) {
      if ("pathfinder.walls".equals(wsList.get(i).name)) return i;
    }
    return 0;
  }

  public void draw() {
    mouse.x = mouseX;
    mouse.y = mouseY;

    if (modes != null && 0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].draw();
    }
  }

  public void keyPressed() {
    if (keyCode == CONTROL) ctrlHold = true;
    if (keyCode == SHIFT) shiftHold = true;
    if (modes == null || modeIndex < 0 || modeIndex >= modes.length) return;

    if (key == ' ') {
      modes[modeIndex].cleanup();
      modeIndex = (modeIndex + 1) % modes.length;
      modes[modeIndex].init();
      return;
    }

    modes[modeIndex].keyPressed();
  }

  public void keyReleased() {
    if (keyCode == CONTROL) ctrlHold = false;
    if (keyCode == SHIFT) shiftHold = false;

    if (modes != null && 0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].keyReleased();
    }
  }

  public void mousePressed() {
    if (modes != null && 0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mousePressed();
    }
  }

  public void mouseDragged() {
    if (modes != null && 0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mouseDragged();
    }
  }

  public void mouseReleased() {
    if (modes != null && 0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mouseReleased();
    }
  }

  Point nextWaypoint(Mover mover, PathStrategy strategy) {
    Point start = mover.asPoint();
    Point goal = new Point(playerPos);

    if (wsCurr.isClearPath(start, goal)) {
      rememberPath(mover, Arrays.asList(start, goal));
      return goal;
    }

    List<Point> path = findPath(start, goal, strategy);
    rememberPath(mover, path);
    if (path.size() >= 2) return path.get(1);
    return start;
  }

  Point randomWaypoint(Point start, Mover mover) {
    Point goal = new Point(playerPos);
    if (wsCurr.isClearPath(start, goal)) {
      rememberPath(mover, Arrays.asList(start, goal));
      return goal;
    }

    List<Point> candidates = sortedWallPoints();
    for (int i = candidates.size() - 1; i >= 0; i--) {
      if (!wsCurr.isClearPath(start, candidates.get(i))) candidates.remove(i);
    }

    if (candidates.isEmpty()) {
      rememberPath(mover, Collections.singletonList(start));
      return start;
    }

    Point target = candidates.get((int) random(candidates.size()));
    rememberPath(mover, Arrays.asList(start, target));
    return target;
  }

  void rememberPath(Mover mover, List<Point> path) {
    debugPaths.put(mover, new ArrayList<Point>(path));
  }

  List<Point> findPath(Point start, Point goal, PathStrategy strategy) {
    if (strategy == PathStrategy.DIJKSTRA) return findDijkstraPath(start, goal);
    return findUnweightedPath(start, goal, strategy == PathStrategy.DFS);
  }

  List<Point> findUnweightedPath(Point start, Point goal, boolean depthFirst) {
    List<Point> nodes = graphNodes(start, goal);
    Map<Point, Point> previous = new HashMap<>();
    Set<Point> visited = new HashSet<>();
    Deque<Point> frontier = new ArrayDeque<>();

    frontier.add(start);
    visited.add(start);

    while (!frontier.isEmpty()) {
      Point current = depthFirst ? frontier.removeLast() : frontier.removeFirst();
      if (current == goal) return reconstructPath(start, goal, previous);

      List<Point> neighbors = visibleNeighbors(current, nodes, goal);
      if (depthFirst) Collections.reverse(neighbors);

      for (Point next : neighbors) {
        if (visited.contains(next)) continue;
        visited.add(next);
        previous.put(next, current);
        frontier.add(next);
      }
    }

    return Collections.singletonList(start);
  }

  List<Point> findDijkstraPath(Point start, Point goal) {
    List<Point> nodes = graphNodes(start, goal);
    Map<Point, Point> previous = new HashMap<>();
    Map<Point, Double> distances = new HashMap<>();
    PriorityQueue<PathNode> frontier = new PriorityQueue<>();

    for (Point node : nodes) distances.put(node, Double.MAX_VALUE);
    distances.put(start, 0.0);
    frontier.add(new PathNode(start, 0.0));

    while (!frontier.isEmpty()) {
      PathNode currentNode = frontier.poll();
      Point current = currentNode.point;
      if (currentNode.distance > distances.get(current) + Point.EPSILON) continue;
      if (current == goal) return reconstructPath(start, goal, previous);

      for (Point next : visibleNeighbors(current, nodes, goal)) {
        double nextDistance = currentNode.distance + current.distTo(next);
        if (nextDistance >= distances.get(next)) continue;

        distances.put(next, nextDistance);
        previous.put(next, current);
        frontier.add(new PathNode(next, nextDistance));
      }
    }

    return Collections.singletonList(start);
  }

  List<Point> reconstructPath(Point start, Point goal, Map<Point, Point> previous) {
    LinkedList<Point> path = new LinkedList<>();
    Point current = goal;
    path.addFirst(goal);

    while (current != start) {
      current = previous.get(current);
      if (current == null) return Collections.singletonList(start);
      path.addFirst(current);
    }

    return path;
  }

  List<Point> graphNodes(Point start, Point goal) {
    List<Point> nodes = sortedWallPoints();
    nodes.add(start);
    nodes.add(goal);
    return nodes;
  }

  List<Point> sortedWallPoints() {
    List<Point> nodes = new ArrayList<Point>(wsCurr.points);
    Collections.sort(nodes, new Comparator<Point>() {
      public int compare(Point a, Point b) {
        int byX = Float.compare(a.x, b.x);
        if (byX != 0) return byX;
        return Float.compare(a.y, b.y);
      }
    });
    return nodes;
  }

  List<Point> visibleNeighbors(final Point source, List<Point> nodes, final Point goal) {
    List<Point> neighbors = new ArrayList<>();

    for (Point candidate : nodes) {
      if (candidate == source || source.isNear(candidate, Point.EPSILON)) continue;
      if (wsCurr.isClearPath(source, candidate)) neighbors.add(candidate);
    }

    Collections.sort(neighbors, new Comparator<Point>() {
      public int compare(Point a, Point b) {
        int byGoal = Double.compare(a.distTo(goal), b.distTo(goal));
        if (byGoal != 0) return byGoal;
        int byX = Float.compare(a.x, b.x);
        if (byX != 0) return byX;
        return Float.compare(a.y, b.y);
      }
    });

    return neighbors;
  }

  void drawGrid(int hue, int sat, int bri, int alpha) {
    stroke(hue, sat, bri, alpha);
    strokeWeight(1);
    for (int x = 0; x <= width; x += 40) line(x, 0, x, height);
    for (int y = 0; y <= height; y += 40) line(0, y, width, y);
  }

  void drawHud(String title, String subtitle, String[] lines) {
    int panelWidth = 360;
    int panelHeight = 58 + lines.length * 17;

    noStroke();
    fill(215, 22, 17, 82);
    rect(16, 16, panelWidth, panelHeight, 8);

    fill(0, 0, 100, 95);
    textAlign(LEFT, TOP);
    textSize(19);
    text(title, 30, 27);

    fill(190, 20, 92, 78);
    textSize(12);
    text(subtitle, 30, 52);

    fill(0, 0, 100, 82);
    for (int i = 0; i < lines.length; i++) {
      text(lines[i], 30, 78 + i * 17);
    }
  }

  void drawAgentLabel(Mover mover, String label) {
    textAlign(CENTER, CENTER);
    textSize(10);
    fill(0, 0, 100, 72);
    text(label, mover.x, mover.y - 22);
  }

  void drawPath(List<Point> path, float hue) {
    if (path == null || path.size() < 2) return;

    noFill();
    stroke(hue, 34, 86, 45);
    strokeWeight(2);
    for (int i = 0; i < path.size() - 1; i++) {
      Point a = path.get(i);
      Point b = path.get(i + 1);
      line(a.x, a.y, b.x, b.y);
    }
  }

  void drawVisibilityGraph(List<Point> extraPoints) {
    List<Point> nodes = sortedWallPoints();
    nodes.addAll(extraPoints);

    stroke(188, 55, 92, 14);
    strokeWeight(1);
    for (int i = 0; i < nodes.size(); i++) {
      for (int j = i + 1; j < nodes.size(); j++) {
        Point a = nodes.get(i);
        Point b = nodes.get(j);
        if (wsCurr.isClearPath(a, b)) line(a.x, a.y, b.x, b.y);
      }
    }
  }

  class PathNode implements Comparable<PathNode> {
    Point point;
    double distance;

    PathNode(Point point, double distance) {
      this.point = point;
      this.distance = distance;
    }

    public int compareTo(PathNode other) {
      return Double.compare(distance, other.distance);
    }
  }

  interface Mode {
    void draw();
    default void init() {}
    default void cleanup() {}
    default void keyPressed() {}
    default void keyReleased() {}
    default void mousePressed() {}
    default void mouseDragged() {}
    default void mouseReleased() {}
  }

  class BuildMode implements Mode {
    Point lastPoint = null;
    Point lastPointOrigin = null;

    String typed = null;
    boolean saving = false;

    public void draw() {
      background(48, 12, 96);
      drawGrid(48, 18, 55, 16);

      wsCurr.display(Pathfinder.this, true);

      if (lastPoint != null) {
        drawCrossingLine(lastPoint, mouse);
        lastPoint.display(Pathfinder.this, 168, 64, 82);
      }

      showInstructions();
      if (saving) showSavePrompt();
    }

    void showInstructions() {
      String subtitle = "#" + wsIndex + "  " + wsCurr.getName();
      String[] controls = new String[] {
          "space: switch to play mode",
          "click: create/connect endpoint",
          "drag endpoint: reshape a wall",
          "right click endpoint: remove wall",
          "w: add random wall    n: new map",
          "s: save    r: revert    up/down: maps",
          "ctrl-z: " + wsCurr.undoPeek(),
          "ctrl-y or shift-ctrl-z: " + wsCurr.redoPeek()
      };
      drawHud("Build Mode", subtitle, controls);
    }

    void showSavePrompt() {
      int panelWidth = 430;
      int panelHeight = 74;
      int x = width / 2 - panelWidth / 2;
      int y = height - panelHeight - 24;

      noStroke();
      fill(215, 22, 17, 88);
      rect(x, y, panelWidth, panelHeight, 8);

      fill(0, 0, 100, 92);
      textAlign(CENTER, TOP);
      textSize(15);
      text("Save wall set as", width / 2, y + 13);

      fill(190, 25, 95, 90);
      textSize(22);
      text(typed, width / 2, y + 39);
    }

    void drawCrossingLine(Point a, Point b) {
      List<Point> intersects = wsCurr.intersections(a, b);

      strokeWeight(3);
      if (intersects.isEmpty()) {
        stroke(152, 70, 72, 86);
      }
      else {
        stroke(2, 78, 94, 90);
      }
      line(a.x, a.y, b.x, b.y);

      for (Point intersect : intersects) {
        intersect.display(Pathfinder.this, 2, 78, 94);
      }
    }

    public void keyPressed() {
      if (saving) {
        if (key == ESC) {
          key = 0;
          saving = false;
          typed = null;
          return;
        }
        if (key >= 32 && key < 127) typed += key;
        if (key == BACKSPACE && typed.length() > 0) {
          typed = typed.substring(0, typed.length() - 1);
        }
        if (key == ENTER || key == RETURN) finishSave();
        return;
      }

      char lowerKey = Character.toLowerCase(key);
      if (lowerKey == 'w') wsCurr.add(new Wall(Pathfinder.this));
      if (lowerKey == 'r') wsCurr.revert();
      if ('0' <= key && key <= '9') loadWalls(key - '0');
      if (lowerKey == 's') beginOrFinishSave();
      if ((lowerKey == 'z') && ctrlHold) {
        if (shiftHold) wsCurr.redo();
        else wsCurr.undo();
      }
      if (lowerKey == 'y' && ctrlHold) wsCurr.redo();
      if (keyCode == UP) loadWalls((wsIndex + 1) % wsList.size());
      if (keyCode == DOWN) loadWalls((wsIndex + wsList.size() - 1) % wsList.size());
      if (lowerKey == 'n') newWallSet();
    }

    void beginOrFinishSave() {
      if (wsCurr.name == null) {
        saving = true;
        typed = "";
      }
      else {
        wsCurr.save();
      }
    }

    void finishSave() {
      if (typed == null || typed.trim().isEmpty()) {
        saving = false;
        typed = null;
        return;
      }

      if (!typed.contains(".")) typed += ".walls";
      wsCurr.name = typed;
      if (!wsCurr.save()) wsCurr.name = null;

      typed = null;
      saving = false;
    }

    void loadWalls(int index) {
      if (index >= wsList.size()) {
        System.err.println("Only " + wsList.size() + " wall sets.");
        return;
      }

      lastPoint = null;
      wsIndex = index;
      wsCurr = wsList.get(index);
    }

    void newWallSet() {
      lastPoint = null;
      wsCurr = new WallSet();
      wsIndex = wsList.size();
      wsList.add(wsCurr);
    }

    public void cleanup() {
      lastPoint = null;
      saving = false;
      typed = null;
    }

    public void mousePressed() {
      Point clicked = findPoint(mouseX, mouseY);

      if (mouseButton == LEFT) {
        if (clicked == null) {
          clicked = new Point(mouseX, mouseY);
          if (lastPoint != null) wsCurr.add(new Wall(clicked, lastPoint));
          else lastPoint = clicked;
        }
        else {
          lastPointOrigin = new Point(clicked);
        }

        lastPoint = clicked;
      }
      else if (mouseButton == RIGHT) {
        if (clicked != null) {
          if (clicked.wall == null) {
            if (clicked == lastPoint) lastPoint = null;
          }
          else {
            wsCurr.rem(clicked.wall);
            if (lastPoint != null && lastPoint.wall == clicked.wall) lastPoint = null;
          }
        }
        else if (lastPoint != null) {
          lastPoint = null;
        }
      }
    }

    public void mouseDragged() {
      if (lastPoint != null) {
        lastPoint.x = mouseX;
        lastPoint.y = mouseY;
      }
    }

    public void mouseReleased() {
      if (lastPoint != null && lastPoint.wall != null) {
        if (lastPointOrigin != null && !lastPoint.sameLocation(lastPointOrigin)) {
          wsCurr.finishMove(lastPoint, lastPointOrigin);
        }
        lastPointOrigin = null;
        lastPoint = null;
      }
    }

    Point findPoint(float x, float y) {
      if (lastPoint != null && dist(lastPoint.x, lastPoint.y, x, y) <= 7) {
        return lastPoint;
      }

      for (Point p : wsCurr.points) {
        if (dist(p.x, p.y, x, y) <= 7) return p;
      }

      return null;
    }
  }

  class PlayMode implements Mode {
    boolean playPaused = false;
    boolean showGraph = false;

    Player player;
    Mover[] ghosts;
    String[] ghostLabels = new String[] { "RAND", "BFS", "DIJK", "DFS" };
    int start;

    public void draw() {
      if (player == null || ghosts == null) resetPlayers();

      debugPaths.clear();
      if (!playPaused) player.move();
      playerPos = player.asPoint();

      if (!playPaused) {
        for (Mover ghost : ghosts) ghost.move();
      }

      background(212, 24, 14);
      drawGrid(205, 28, 70, 9);

      List<Point> dynamicPoints = new ArrayList<>();
      dynamicPoints.add(player.asPoint());
      for (Mover ghost : ghosts) dynamicPoints.add(ghost.asPoint());

      if (showGraph) drawVisibilityGraph(dynamicPoints);
      for (Mover ghost : ghosts) drawPath(debugPaths.get(ghost), ghost.hue);

      wsCurr.display(Pathfinder.this, false);

      player.display(Pathfinder.this);
      drawAgentLabel(player, "YOU");

      for (int i = 0; i < ghosts.length; i++) {
        ghosts[i].display(Pathfinder.this);
        drawAgentLabel(ghosts[i], ghostLabels[i]);
      }

      showInstructions();
    }

    void showInstructions() {
      String subtitle = playPaused ? "Paused" : "Mouse controlled chase";
      String graphStatus = showGraph ? "on" : "off";
      String[] controls = new String[] {
          "space: return to build mode",
          "mouse: move player",
          "p: pause    r: reset",
          "g: visibility graph " + graphStatus,
          "colored lines: current ghost routes"
      };
      drawHud("Play Mode", subtitle, controls);
    }

    public void keyPressed() {
      char lowerKey = Character.toLowerCase(key);
      if (lowerKey == 'p') playPaused = !playPaused;
      if (lowerKey == 'r') resetPlayers();
      if (lowerKey == 'g') showGraph = !showGraph;
    }

    public void init() {
      resetPlayers();
    }

    void resetPlayers() {
      player = new Player(Pathfinder.this);
      playerPos = player.asPoint();

      ghosts = new Mover[] {
          new Mover(Pathfinder.this, new RandomPoint()),
          new Mover(Pathfinder.this, new BfsPath()),
          new Mover(Pathfinder.this, new DijkstraPath()),
          new Mover(Pathfinder.this, new DfsPath())
      };

      float[] hues = new float[] { 24, 145, 260, 345 };
      for (int i = 0; i < ghosts.length; i++) {
        ghosts[i].hue = hues[i];
        ghosts[i].speed = 1.75f;
      }

      start = millis();
    }
  }
}
