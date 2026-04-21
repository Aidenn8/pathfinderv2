# Pathfinder

A Processing/Java pathfinding playground with editable walls and four ghost movement strategies: random waypoint, BFS, Dijkstra, and DFS.

## Run

```bash
cd "Pathfinder-main 2/Pathfinder1"
mvn compile exec:java
```

## Controls

Build mode:

- `space`: switch to play mode
- left click: create or connect wall endpoints
- drag endpoint: reshape a wall
- right click endpoint: remove a wall
- `w`: add a random wall
- `n`: new wall set
- `s`: save
- `r`: revert
- up/down: switch wall sets
- `ctrl-z`: undo
- `ctrl-y` or `shift-ctrl-z`: redo

Play mode:

- mouse: move the player
- `g`: show or hide the visibility graph
- `p`: pause
- `r`: reset
- `space`: return to build mode
