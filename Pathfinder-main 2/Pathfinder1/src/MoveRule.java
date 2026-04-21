import java.util.Arrays;

interface MoveRule {
  void move(Mover m);
}

class StandStill implements MoveRule {
  public void move(Mover m) {
  }
}

class RandomMovement implements MoveRule {
  public void move(Mover m) {
    double angle = Math.random() * Math.PI * 2;
    m.moveTo(m.x + Math.cos(angle) * m.speed, m.y + Math.sin(angle) * m.speed);
  }
}

class MoveTo implements MoveRule {
  Point target;

  MoveTo(Point target) {
    this.target = target;
  }

  public void move(Mover m) {
    m.moveTo(target);
  }
}

class RandomPoint implements MoveRule {
  Point target;

  public void move(Mover m) {
    Point current = m.asPoint();
    if (target == null || current.isNear(target, m.speed + 1.0f)
        || !m.pf.wsCurr.isClearPath(current, target)) {
      target = m.pf.randomWaypoint(current, m);
    }
    else {
      m.pf.rememberPath(m, Arrays.asList(current, target));
    }

    m.moveTo(target);
  }
}

abstract class PathFollower implements MoveRule {
  Pathfinder.PathStrategy strategy;

  PathFollower(Pathfinder.PathStrategy strategy) {
    this.strategy = strategy;
  }

  public void move(Mover m) {
    m.moveTo(m.pf.nextWaypoint(m, strategy));
  }
}

class BfsPath extends PathFollower {
  BfsPath() {
    super(Pathfinder.PathStrategy.BFS);
  }
}

class DijkstraPath extends PathFollower {
  DijkstraPath() {
    super(Pathfinder.PathStrategy.DIJKSTRA);
  }
}

class DfsPath extends PathFollower {
  DfsPath() {
    super(Pathfinder.PathStrategy.DFS);
  }
}
