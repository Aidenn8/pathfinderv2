# Pathfinder

This project uses Processing with Java to visualize pathfinding around editable line-segment walls.

The runnable Maven project is in `Pathfinder1`.

```bash
cd Pathfinder1
mvn compile exec:java
```

To build a runnable jar:

```bash
mvn package
java -jar target/Pathfinder.jar
```

To build a Mac app:

```bash
./package-mac-app.sh
open target/dist/Pathfinder.app
```

See the repository root README for controls.
