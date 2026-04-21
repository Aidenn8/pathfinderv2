# Pathfinder

A Processing/Java pathfinding playground with editable walls and four ghost movement strategies: random waypoint, BFS, Dijkstra, and DFS.

## Run

```bash
cd "Pathfinder-main 2/Pathfinder1"
mvn compile exec:java
```

## Build A Jar

```bash
cd "Pathfinder-main 2/Pathfinder1"
mvn package
java -jar target/Pathfinder.jar
```

Run the jar from the `Pathfinder1` folder so it can find the `wallsets/` maps.

## Build A Mac App

```bash
cd "Pathfinder-main 2/Pathfinder1"
./package-mac-app.sh
open target/dist/Pathfinder.app
```

The `.app` includes a Java runtime, the Pathfinder jar, Processing, and the bundled wall maps.

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
