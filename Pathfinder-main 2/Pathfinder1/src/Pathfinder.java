import java.io.File;
import java.util.*;
import processing.core.*;




//g key is display graph

//dfs look strange
//sometimes bounces between two of the same points
//is dfs supposed to just calculate the first point in the path and then go to that point and then calculate a new path and then go to the first point in that path
//or is it supposed to calculate 1 path and then follow it until can't follow it anymore or if reaches player position
//the problem with dfs is that it calculates the first point in a path then creates a new path and then finds the first point in that path
//what ends up happening is that it will go to the first point in one path and then go to it
//but then it finds a new path and goes to the point where it just came from
//and then it repeats

//dijkstra has problem that if the player point is visible but then moves away, it will try to move to the player position, but then run into a wall


//bfs also has similar problem
//bounces between two of the same points, but if the player moves, then it will choose one direction to go
//is this ok?

//need to debug

public class Pathfinder extends PApplet {
  
  // used for Serializable interface inherited via PApplet
  private static final long serialVersionUID = 1L;
  
  // ====================== //
  // ==== GENERAL CODE ==== //    (shared between modes)
  // ====================== //
  
  // WallSets are primary containers for the "map" that's edited
  //   and played on
  // wsCurr is always updated to refer to the current WallSet
  ArrayList<WallSet> wsList = new ArrayList<>();
  int wsIndex = 0;
  WallSet wsCurr;
  
  // Points primarily represent ends of walls but can also
  //   represent the mouse, moving objects in a game, etc
  Point mouse = new Point(0,0);
  
  // mode control variables
  Mode[] modes = new Mode[]{new BuildMode(), new PlayMode()};
  final int BUILD_MODE = 0;
  final int PLAY_MODE = 1;
  int modeIndex = BUILD_MODE;
  
  // modifier keys tracked here
  boolean ctrlHold, shiftHold;
  
  public void setup() {
    size(1000, 800);
    colorMode(HSB, 360, 100, 100, 100);
    
    loadWallSets();
    //WHERE TO CHOOSE WHAT WALL SET YOU ORIGINALLY LOAD INTO
    //______________________________________________________________________________________________
    wsCurr = wsList.get(2);
    //______________________________________________________________________________________________
  }
  
  // loads all valid wallsets in folder
  void loadWallSets() {
    File folder = new File("wallsets/");
    
    // check valid folder structure
    if (folder.exists() && folder.isDirectory()) {
      File[] files = folder.listFiles();
      if (files.length == 0) {
        System.out.println("No files in wallsets folder.");
      }
      
      System.out.println("Loading from wallsets folder:");
      for (File wsFile : folder.listFiles()) {
        if (!wsFile.isDirectory()) {
          WallSet ws = WallSet.fromFile(wsFile);
          if (ws != null) wsList.add(ws);
        }
      }
      if (wsList.isEmpty()) {
        System.out.println("No valid files in wallsets folder.");
      }
    }
    else {
      System.err.println("Couldn't find wallsets folder! Should be at " +
                         folder.getAbsolutePath());
    }
    
    if (wsList.isEmpty()) wsList.add(new WallSet());
  }
  
  public void draw() {
    // keep mouse Point updated
    mouse.x = mouseX;
    mouse.y = mouseY;
 
    // draw based on mode
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].draw();
    }
    else {
      background(0); // black
      fill(0, 10, 100); // 10% red
      textAlign(CENTER, CENTER);
      textSize(48);
      text("Invalid mode!", width/2, height/2);
    }
  }
  
  public void keyPressed() {
    if (keyCode == CONTROL) ctrlHold = true;
    if (keyCode == SHIFT) shiftHold = true;
    
    // most things should only work on a valid mode
    if (0 <= modeIndex && modeIndex < modes.length) {
      // space will switch modes, so clean up the old one
      if (key == ' ') {
        modes[modeIndex].cleanup();
      }
      // send the key press to the mode
      else {
        modes[modeIndex].keyPressed();
      }
    }
    // space cycles to new mode and starts it up
    if (key == ' ') {
      modeIndex = (modeIndex + 1) % modes.length;
      modes[modeIndex].init();
    }
  }
  
  // most controls just get passed to the correct mode
  public void keyReleased() {
    if (keyCode == CONTROL) ctrlHold = false;
    if (keyCode == SHIFT) shiftHold = false;
    
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].keyReleased();
    }
  }
  
  public void mousePressed() {
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mousePressed();
    }
  }
  
  public void mouseDragged() {
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mouseDragged();
    }
  }
  
  public void mouseReleased() {
    if (0 <= modeIndex && modeIndex < modes.length) {
      modes[modeIndex].mouseReleased();
    }
  }
  //GRAPH VARIABLES
  //__________________________________________________________________________________________________________
  int indexOfPlayer;//store the index of the player point in the pointList
  ArrayList<Integer> indexOfMovers = new ArrayList<Integer>();//store the index of each mover in the pointList
 //set of all of the points that make up the walls (in other words "corners")
  List<Point> pointList;
  
  //make an adjacency matrix representing a graph of all of the points
  int[][] adjMatrix;
  Point playerPos;
  //______________________________________________________________________________________________________________________________________

 
  
  // RANDOM MOVEMENT GHOST BELOW
  //__________________________________________________________________________________________________________
  
  //generate a random point 
  Point randomPointMover;//point of random mover
  public Point randomPointGhost(){
  	//remember only random points that should be stored should be the ones that are accesisible through the current pos of the curr mover
  	//look at current position of ghost & check the other wall points that can be reached from the curr position and then choose a random point out of those
	ArrayList<Point> randomPoints = new ArrayList<>();
	if(wsCurr.isClearPath(randomPointMover, playerPos)) {
		return playerPos;
	}
	int randomPointIndex = indexOfMovers.get(0); //this will be the index of the random point mover in the adj matrix
	//println(randomPointIndex);
	for(int j = 0; j<adjMatrix[0].length; j++) {
		if((adjMatrix[randomPointIndex][j] == 1) || (adjMatrix[j][randomPointIndex] == 1)) {//if there is a connection
			//then add to list
			randomPoints.add(pointList.get(j));
		}
	}
	
	//now eliminate the point that are not part of the wall, as we want the mover to only move towards the wall points
	
	//make a copy of the randomPoint list so that i can iterate through the copy and actually edit the real list
	ArrayList<Point> randomPointsCopy = new ArrayList<>();
	for(Point p : randomPoints) {//make a copy of the random points list
		randomPointsCopy.add(p);
	}
	for(Point p : randomPointsCopy) {//iterate through the copy
		//if not part of the wall
		if(!wsCurr.points.contains(p)) {
			//then remove from the original
			randomPoints.remove(p);
		}
	}
	//now need to find a random point from the list of points
	
	//TODO: Check later
	int random = (int)(Math.random()*randomPoints.size()-1) + 0;//is this right? are the boundaries correct? 
	
	return randomPoints.get(random);
  	 
  }
  
  //BFS GHOST MOVEMENT BELOW
//______________________________________________________________________________________________________________________________________
  Point bfsGhostLocation; //location of the bfs ghost
 
 public Point getFirstPointFromPath() {
	 if(wsCurr.isClearPath(playerPos, bfsGhostLocation)) {
		 return playerPos;
	 }
	 path.clear();
	 buildPrev();
	 buildPath(bfsFinalPoint);
	 if(path.size() <=0) {
		 return bfsFinalPoint;
	 }
	 return path.get(path.size()-1);//get the last value of the path (the target point after the starting point)
 }
 
 ArrayList<Point> path = new ArrayList<>(); //path will be backwards (the first target point of the list will be the last element)
 public void buildPath(Point root) {//assumes that map has already been built (buildMap() has been called) and finalPoint is created
	 Point p = root;
	 while(p.prev!=null) {
		 path.add(p);
		 p = p.prev;
	 }
 }
 
 //builds prev of all points
 public void buildPrev() {
	 bfs();	
 }

public Point bfsFinalPoint;//final point before reaching the player point in bfs
public Point originalPoint; //first point (location of the ghost)

 public void bfs() {
	  Point start = bfsGhostLocation;
	  originalPoint = new Point(50,50);
	  originalPoint = start;
	  Deque<Point> dq = new ArrayDeque<>();
	  Set<Point> visited = new HashSet<>();
	  dq.add(start);
	  start.prev = null;//there is no previous to the original node location
	
	  while(dq.size() > 0) {//while the deque still has values
		  Point curr = new Point(50,50);
		  curr = dq.poll();//get the curr point
		  
		  //when to stop bfs-ing?
		  //stop when the player is visible from curr point

		  if(wsCurr.isClearPath(curr, playerPos)) {//TODO: Change into using the adjMatrix instead of using method isClearpath
			  bfsFinalPoint = curr; //store the point before reaching the player ("final point")
			  break; //end bfs
		  }

		  
		  if(visited.contains(curr)) {//if i have already been to this point
			  continue;//skip
		  }
		 
		  visited.add(curr);//mark as visited if not been here
		  
		  int currIndex = pointList.indexOf(curr); //find the index of the curr point
		  //queue up neighbors of current point
		  for(int i = 0; i<adjMatrix[0].length; i++) {//go through adjmatrix
			  if(visited.contains(pointList.get(i))) {
				  continue;
			  }
			  if(pointList.get(i).equals(curr)) {//if comparing itself
				  continue;//skip
			  }
			 
			  if(adjMatrix[currIndex][i] == 1) {//if there is a connection 
				  if(pointList.get(i).equals(curr)) {
					  //println("are equal");
					  continue;
				  }
				  
				  if(wsCurr.points.contains(pointList.get(i))) {//if the neighbor is a point on the wall
					  //map curr to prev	
					  Point newCurr = new Point(50,50);
					  newCurr = pointList.get(i);
					  newCurr.prev = curr;
					  
					  //queue up neighbor
					  dq.add(newCurr);
				  } 
			  }
		  }
	  }
  }
 
 //______________________________________________________________________________________________________________________________________

//build a path using dijkstra & find the shortest possible distance from ghost to player pos
//run dijkstras and once i have visited the playerpos then I have finished my algorithm
//return the point before the playerpos and retrace steps to get to the first target point
//use a priorityqueue to get the shortest distance point from start off the stack
//objetive of the priority queue is to to get the next shortest distance point from the start point

//public Point getFirstPointFromDijkstraPath() {
////	runDijkstra();//run until get the final point before playerpos
////	return buildDijkstraPath(dijkstraFinalPoint);//retrace steps to get to get out the next target point
////	
//	
//	if(wsCurr.isClearPath(playerPos, dijkstraGhostLocation)) {
//		 return playerPos;
//	 }
//	runDijkstra(dijkstraGhostLocation);//generate the final point
////	if(dijkstraFinalPoint == null) {
////		return start;
////	}
////	buildDijkstraPath(dijkstraFinalPoint);
////	if(dijkstraPath.size() <= 0) {
////		return dijkstraFinalPoint;
////	}
//	
////	buildDijkstraPath(dijkstraFinalPoint);
////	if(dijkstraPath.size() <= 0) {
////		return dijkstraFinalPoint;
////	}
////	return dijkstraPath.get(dijkstraPath.size()-1);
//	return buildDijkstraPath(dijkstraFinalPoint);
//
//}
//ArrayList<Point> dijkstraPath = new ArrayList<Point>();
////trace back steps using the prev object in each point to get to the next point after the start point
//public Point buildDijkstraPath(Point finalPoint){
//	
//	Point curr = finalPoint;
//	while(curr.prev != null) {
//		if(curr.prev.prev == null) {
//			return curr;
//		}
//		curr=curr.prev;
//		
//	}
//	return curr;
//	
//}
//
////run dijkstra algorithm and generate the final point (the point before the player) w/shortest path to get there
//public void runDijkstra(Point start) {
//	//check and figure out size of priority queue			//+1 because of original point (ghost point)
//	PriorityQueue<Point> pq = new PriorityQueue<>(wsCurr.points.size()+1, new Comparator<Point>() {//figure out how comparator works
//		@Override
//		public int compare(Point o1, Point o2) {
//			if(o1.distanceFromStart < o2.distanceFromStart) {//if bigger
//				return 1;//return positive
//			}
//			if(o1.distanceFromStart > o2.distanceFromStart) {//if smaller
//				return -1;//return negative
//			}
//			return 0;//if equal return 0
//		}
//	});
//	
//	Set<Point> visited = new HashSet<>(); //keep track of visited points
//	
//	pq.add(start);//queue up start point
//	start.prev = null;//mark prev as null
//	start.distanceFromStart = 0;//set the distance from the start from the start is 0
//	//int z = 0;
//	while(pq.size() > 0) {
//		Point curr = pq.poll();
//		
//		if(visited.contains(curr)) {
//			continue;
//		}
//		visited.add(curr);
//		
////		if(curr.equals(playerPos)) {
////			
////			dijkstraFinalPoint = curr;
////			break;
////		}
//		if(wsCurr.isClearPath(curr, playerPos)){		
//			
//			dijkstraFinalPoint = curr;//make the final point one before the player pos
//			
//			break;
//		}
////		
////		
//		
//		
//		
//		
//		int currIndex = pointList.indexOf(curr);
//		  for(int i = 0; i<adjMatrix[0].length; i++) {//go through adjmatrix
//			  
//			  if(pointList.get(i).equals(curr)) {//if comparing itself
//				  
//				  continue;//skip
//			  }
//			  if(visited.contains(pointList.get(i))) {
//				 
//				  continue;
//			  }
//			  
//			 
//			  if(adjMatrix[currIndex][i] == 1) {//if there is a connection 
//				  
////				  if(pointList.get(i).equals(playerPos)) {//if connection between current point and player
////					  println("hello");
////					  double newDist = curr.distanceFromStart + curr.distTo(playerPos);
////					  if(newDist < playerPos.distanceFromStart) {
////						  playerPos.distanceFromStart = newDist;
////						  playerPos.prev = curr;
////						  
////					  }
////					  pq.add(playerPos);
////					  continue;
////				  }
//				  //|| (pointList.get(i).equals(playerPos)
//				  if(wsCurr.points.contains(pointList.get(i))) {//if the neighbor is a point on the wall
//					 
//					  //map curr to prev	
//					  Point newCurr = pointList.get(i);
//					  double newDistanceFromStart = curr.distanceFromStart + curr.distTo(newCurr);
//					  if(newCurr.distanceFromStart > newDistanceFromStart) {
//						  println("hello");
//						  newCurr.distanceFromStart = newDistanceFromStart;
//						  
//						  newCurr.prev = curr;
//						  println(newCurr.prev);
//					  }
//					  
//					  
//					  //queue up neighbor
//					  pq.add(newCurr);
//				  } 
//			  }
//		  }
//	}
//}
Point dijkstraGhostLocation;//technically don't need can just pass in mover m location from the MoveRule class
Point dijkstraFinalPoint; //final point before player point

public Point firstPointFromDijkstraPath() {
	//TODO: PROBLEM: DIJKSTRA GHOST SEEING PLAYER AND TRYING TO MOVE TOWARDS IT BUT PLAYER MOVES SO GHOST RUNS INTO WALL
	//TODO: FIX LATER ADJ MATRIX NOT WORKING
	
	if(wsCurr.isClearPath(dijkstraGhostLocation, playerPos)) {
		return playerPos;
		//return dijkstraGhostLocation;
	}
//	println(dijkstraGhostLocation);
//	println(playerPos);
	runDijkstra(dijkstraGhostLocation);//generates dijkstra final point
	//println(dijkstraFinalPoint);
	
	ArrayList<Point> dijkstraPath = new ArrayList<>();
	dijkstraPath = generateDijkstraPath(dijkstraFinalPoint);
	
	Point target = new Point(50,50);
	target = dijkstraPath.get(dijkstraPath.size()-1);
	return target;
	
}

public ArrayList<Point> generateDijkstraPath(Point root) {
	ArrayList<Point> dijkstraPath = new ArrayList<>();
	while(root.prev != null) {
		dijkstraPath.add(root);
		root = root.prev;
	}
	
	return dijkstraPath;
	
}

//queue up neighbors 
//if the new dist is smaller than the old dist then replace the dist and replace the previous node
public void runDijkstra(Point start) {
	PriorityQueue<Point> pq = new PriorityQueue<>(wsCurr.points.size()+1, new Comparator<Point>() {//figure out how comparator works
		@Override
		public int compare(Point o1, Point o2) {
			if(o1.distanceFromStart < o2.distanceFromStart) {//if bigger
				return -1;//return positive
			}
			if(o1.distanceFromStart > o2.distanceFromStart) {//if smaller
				return 1;//return negative
			}
			return 0;//if equal return 0
		}
	});
	
	Set<Point> visited = new HashSet<>(); //keep track of visited points
	
	pq.add(start);
	start.prev = null;
	start.distanceFromStart = 0; //set the start distance from start as 0 away from itself
	
	while(pq.size()>0) {
		Point curr = pq.poll();
		if(curr.equals(playerPos)) {
			//println("ive been her");
			dijkstraFinalPoint = new Point(50,50);
			dijkstraFinalPoint = curr;
			break;
		}
		if(visited.contains(curr)) {
			continue;
		}
		visited.add(curr);
		
		
		//int currIndex = pointList.indexOf(curr);
		for(int i = 0; i < adjMatrix[0].length; i++) {
			Point newCurr = pointList.get(i);
			
			if(visited.contains(pointList.get(i))) {
				continue;
			}
			if(newCurr.equals(curr)) {
				continue;
			}
			
			if(wsCurr.isClearPath(newCurr, curr)) {
				
			//TODO: PROBLEM AREA THE ADJ MATRIX ISN'T WORKING FIX LATER 
				//MARKING THAT THERE IS A CONNECTION BETWEEN PLAYER POINT AND GHOST EVEN THOUGH NOT
//			if(adjMatrix[currIndex][i] == 1) {
				if(newCurr.distanceFromStart == 0) {
					newCurr.distanceFromStart = 1000000;
					
				}//if not initialized then set as big number
				
				if(wsCurr.points.contains(newCurr) || newCurr.equals(playerPos)) {
					
					double newDist = curr.distanceFromStart + curr.distTo(newCurr);
					
					//println("im here 1");
					if(newDist < newCurr.distanceFromStart) {
						//println("im here 2");
						newCurr.distanceFromStart = newDist;
						newCurr.prev = curr;
					}
					pq.add(newCurr);
					
				}
			//}
			}
		}
	}
	
	
}

//DFS
//____________________________________________________________________________________________________________________________________
Point dfsGhostLocation;
public Point getFirstPointFromDfs() {
	//TODO: adj Matrix not working properly fix later
	if(wsCurr.isClearPath(dfsGhostLocation, playerPos)) {
		return playerPos;
	}
	visited.clear();
	runDFS(dfsGhostLocation);

	ArrayList<Point> dfsPath = new ArrayList<>();
	dfsPath = generateDFSPath(finalDFSPoint);
//	if(dfsPath.size() <= 0) {
//		return finalDFSPoint;
//	}
	Point target = new Point(50,50);
	
	target= dfsPath.get(dfsPath.size()-1);
	
	//println("pont: "  + target);
	return target;
	
}




public ArrayList<Point> generateDFSPath(Point root) {
	ArrayList<Point> dfsPath = new ArrayList<Point>();
	
	while(root.prev != null) {
		
		dfsPath.add(root);
		root = root.prev;
	}
	
	return dfsPath;
	
}

Set<Point> visited = new HashSet<>();
Point finalDFSPoint;

public void runDFS(Point curr) {
	
	//println(visited);
	if(curr.equals(playerPos)) {
		visited.add(curr);
		//println("found!");
		finalDFSPoint = new Point(50,50);
		finalDFSPoint = curr;
		return;
	}
	//println("currrent:"+ curr);
	if(visited.contains(curr)) {//if i have already been here
		return;//end path search
	}
	visited.add(curr);//mark as visited
	
	int currIndex = pointList.indexOf(curr);
	  for(int i = 0; i<adjMatrix[0].length; i++) {//go through adjmatrix
		  
		  if(pointList.get(i).equals(curr)) {//if comparing itself
			  continue;//skip
		  }
		  if(visited.contains(pointList.get(i))) {
			  continue;
		  }
		  if(wsCurr.isClearPath(pointList.get(i), curr)) {
			  
		  
		  //if(adjMatrix[currIndex][i] == 1) {//if there is a connection 
			  if(wsCurr.points.contains(pointList.get(i)) || pointList.get(i).equals(playerPos)) {//if the neighbor is a point on the wall
				  Point newCurr = pointList.get(i);
				  newCurr.prev = curr;
				  runDFS(newCurr);
			  } 
		  //}
		  }  
	  }
	  
}
//if the player moves but the dfs ghost already has seen the player and tries to move 

 
  
  // interface for different modes with different behaviors
  interface Mode {
    void draw();
    // default: allows interfaces to provide simple implementations,
    //   allowing subclasses to treat those methods as optional
    default void init() {}
    default void cleanup() {}
    
    default void keyPressed() {}
    default void keyReleased() {}
    
    default void mousePressed() {}
    default void mouseDragged() {}
    default void mouseReleased() {}
  }
  

  // ==================== //
  // ==== BUILD MODE ==== //
  // ==================== //
  
  // this mode allows display, editing, loading, saving, etc,
  //   of WallSets
  class BuildMode implements Mode {
    Point lastPoint = null;       // last point created/dragged
    Point lastPointOrigin = null; // point where dragging began
    
    String typed = null;     // used for typed input
    boolean saving = false;  // sub-mode for typing save filename
    
    
    
    public void draw() {
      background(120, 50, 100);  // green
      
      // NOTE: this class is BuildMode, so "this" would refer to the
      //     instance of BuildMode
      //   "Pathfinder.this" is a "qualified this" allowing access to the
      //     instance of the Pathfinder / PApplet object representing
      //     representing the program
      //   (as an inner class, members of BuildMode are part of a
      //     BuildMode instance AND part of a Pathfinder instance)
      wsCurr.display(Pathfinder.this);
      
      // draw last point and line connecting it to mouse
      if (lastPoint != null) {
        drawCrossingLine(lastPoint, mouse);
        lastPoint.display(Pathfinder.this);
      }
      
      showInstructions();
      
      if (saving) showSavePrompt();
    }
    
    void showSavePrompt() {
      fill(100, 0, 0);
      textAlign(CENTER, BOTTOM);
      textSize(24);
      float tSize = textAscent() + textDescent();
      
      text("Enter file name:", width/2, height - tSize - 5);
      text(typed, width/2, height - 5);
    }
    
    void showInstructions() {
      fill(0, 40);  // black, 40% opacity
      textAlign(LEFT, BOTTOM);
      textSize(12);
      float tSize = textAscent() + textDescent();
      float y = 0;
      
      String label = "BUILD MODE - #" + wsIndex + " - " + wsCurr.getName();
      text(label,                                      5, y += tSize);
      text("space: switch mode (works in both modes)", 5, y += tSize);
      
      y += tSize;
      int maxWall = Math.min(9, wsList.size()-1);
      text("any #: load wallset (0-" + maxWall + ")", 5, y += tSize);
      
      // unicode 2191 & 2193: up & down arrows
      text("\u2191\u2193: browse wall sets",    5, y += tSize);
      text("n: new wall set",                   5, y+= tSize);
      text("s: save current walls",             5, y += tSize);
      text("r: revert to saved walls",          5, y += tSize);
      
      y += tSize;
      text("click: create endpoint",            5, y += tSize);
      text("right-click endpoint: remove wall", 5, y += tSize);
      text("click and drag: move endpoint",     5, y += tSize);
      text("w: Add random wall.",               5, y += tSize);
      
      y += tSize;
      text("ctrl-z: " + wsCurr.undoPeek(), 5, y += tSize);
      text("shift-ctrl-z: " + wsCurr.redoPeek(), 5, y += tSize);
    }
    
    // draws line from a->b, with intersections with walls
    void drawCrossingLine(Point a, Point b) {
      List<Point> intersects = wsCurr.intersections(a, b);
   
      strokeWeight(3);
      if (intersects.isEmpty()) {
        stroke(120, 100, 50); // green - no crossings
        line(a.x, a.y, b.x, b.y);
      }
      else {
        stroke(0, 100, 100); // red - crosses other lines
        line(a.x, a.y, b.x, b.y);
        
        // display each intersection point
        for (Point intersect : intersects) {
          intersect.display(Pathfinder.this);
        }
      }
    }
    
    public void keyPressed() {
      // typing a file name as part of save operation
      if (saving) {
        // typable ASCII values get typed
        if (key >= 32 && key < 127) {
          typed += key;
        }
        // backspace backspaces
        if (key == BACKSPACE && typed.length() > 0) {
          typed = typed.substring(0, typed.length()-1);
        }
        // enter completes save operation
        if (key == ENTER) {
          finishSave();
        }
      }
      // normal controls when not in mid-save
      else {
        if (key == 'w') wsCurr.add(new Wall(Pathfinder.this));
        if (key == 'r') wsCurr.revert();
        if ('0' <= key && key <= '9') loadWalls(key - '0');
        if (key == 's') {
          // begin typing name to save to, if none exists
          if (wsCurr.name == null) {
            saving = true;
            typed = "";
          }
          // or just save
          else {
            wsCurr.save();
          }
        }
        // ctrl-Z with or without shift for undo/redo
        // checks keyCode not key because weird stuff happens when
        //   holding control
        if ((keyCode == 'z' || keyCode == 'Z') && ctrlHold) {
          if (shiftHold) wsCurr.redo();
          else           wsCurr.undo();
        }
        // secret support for ctrl-Y
        if (keyCode == 'y' && ctrlHold) wsCurr.redo();
        if (keyCode == UP) {
          loadWalls( (wsIndex + 1) % wsList.size() );
        }
        if (keyCode == DOWN) {
          loadWalls( (wsIndex + wsList.size() - 1) % wsList.size());
        }
        if (key == 'n') {
          lastPoint = null;  // deselect point
          
          wsCurr = new WallSet(); // brand new WallSet at end of list
          wsIndex = wsList.size();
          wsList.add(wsCurr);
        }
      }
    }
    
    // attempts to name and save file based on typed name
    void finishSave() {
      // entering nothing cancels save
      if (typed.isEmpty()) return;
      
      // add file extension if none was provided
      if (!typed.contains(".")) typed += ".walls";
      
      // lock in name
      wsCurr.name = typed;
      
      // try to save, but don't keep name if it fails
      //   (it's probably an invalid name if that happens)
      if (!wsCurr.save()) wsCurr.name = null;
      
      // reset typing vars and exit saving operation
      typed = null;
      saving = false;
    }
    
    // load a set of walls by number
    void loadWalls(int index) {
      if (index >= wsList.size()) {
        System.err.println("Only " + wsList.size() + " wall sets.");
      }
      else {
        lastPoint = null;  // deselect points when loading
        
        wsIndex = index;
        wsCurr = wsList.get(index);
      }
    }
    
    public void cleanup() {
      lastPoint = null;  // deselect
    }
    
    public void mousePressed() {
      Point clicked = findPoint(mouseX, mouseY);
      if (mouseButton == LEFT) {
        // create new Point if one was not already there
        if (clicked == null) {
          clicked = new Point(mouseX, mouseY);
          
          // create new wall if there was another point before
          if (lastPoint != null) {
            wsCurr.add(new Wall(clicked, lastPoint));
          }
          // ... or just start forming a wall at that point
          else {
            lastPoint = clicked;
          }
        }
        // start moving point if there was already a point there
        else {
          lastPointOrigin = new Point(clicked);
        }
        
        // whether point is new or pre-existing, select it for
        //   potential dragging/moving
        lastPoint = clicked;
      }
      else if (mouseButton == RIGHT) {
        // remove Point or Wall
        if (clicked != null) {
          if (clicked.wall == null) {
            if (clicked == lastPoint) lastPoint = null;
          }
          else {
            wsCurr.rem(clicked.wall);
            // reset lastPoint if part of this wall
            if (lastPoint != null && lastPoint.wall == clicked.wall) {
              lastPoint = null;
            }
          }
        }
        // clicked on nothing; deselect lastPoint
        else if (lastPoint != null) lastPoint = null;
      }
    }
    
    public void mouseDragged() {
      // drag point if one is selected
      if (lastPoint != null) {
        lastPoint.x = mouseX;
        lastPoint.y = mouseY;
      }
    }
    
    public void mouseReleased() {
      // "let go" of any point that's already part of a wall, after
      //   dragging or placing it
      if (lastPoint != null && lastPoint.wall != null) {
        // also lock in movement if it was moved
        if (lastPointOrigin != null && !lastPoint.equals(lastPointOrigin)) {
          wsCurr.finishMove(lastPoint, lastPointOrigin);
          lastPointOrigin = null;
        }
        lastPoint = null;
      }
    }
    
    // finds Point with 5 px of a given point
    Point findPoint(float x, float y) {
      if (lastPoint != null &&
          dist(lastPoint.x, lastPoint.y, x, y) <= 5) {
        return lastPoint;
      }
      
      for (Point p : wsCurr.points) {
        if (dist(p.x, p.y, x, y) <= 5) return p;
      }
      
      return null;
    }
  }
  
  // ===================
  // ==== PLAY MODE ====
  // ===================
  class PlayMode implements Mode {
    boolean playPaused = false;
    
    Player player;
    Mover[] ghosts;
    
    int start;
    
    // TODO: graph settings?
    
    //create a boolean that tells if we can toggle to show graph
    boolean showGraph = false;
    
    
    
    // controls display and movement of game
    public void draw() {
    	
      // TODO: update movement graph based on Mover positions
      
      
    	//after init is called which calls createGraph, I now have an adj matrix stored with all points
    	//go through this adj matrix and if they have a connection, then draw a line between points
    	//TODO: ADD MOVER POSITIONS
    	//println("this is done second");
    	Point playerr = new Point(player.x, player.y);
    	playerPos = playerr;
    	
        pointList.set(indexOfPlayer, playerr);//update the player point
    	for(Point p : pointList) {//go through all of my wall points
    		//if there is a clear path between a wall point and the player
    		if(wsCurr.isClearPath(p, playerr)) {
    			//println("hi");
    			//mark the connection
    			adjMatrix[pointList.indexOf(p)][indexOfPlayer] = 1;
    		}else {
    			//otherwise mark that there is no connection
    			adjMatrix[pointList.indexOf(p)][indexOfPlayer] = 1;
    		}
    	}
    	//println("player movement updated!");
    	for(int i = 0; i < ghosts.length; i++) {//go through my list of ghosts
    		Mover m = ghosts[i];//store current ghost that we are on
    		
    		Point moverr = new Point(m.x,m.y);//make the curr mover into a point
    		if(i == 0) {
    			randomPointMover = moverr;
    		}
    		pointList.set(indexOfMovers.get(i), moverr); //update pos or in other words replace the previous ghost at this point with my new updated mover position
    		for(Point p : pointList) {//go through the list of all points
    	  		if(wsCurr.isClearPath(moverr, p)) {//if there is a path from my curr point to another point
    	  			adjMatrix[indexOfMovers.get(i)][pointList.indexOf(p)] = 1; //mark it as a connection
    	  		}else {
    	  			adjMatrix[indexOfMovers.get(i)][pointList.indexOf(p)] = 0; //otherwise mark it as no connection
    	  		}
//    	  		if(wsCurr.isClearPath(p, moverr)) {//if there is a path from another point to curr point
//    	  			adjMatrix[pointList.indexOf(p)][indexOfMovers.get(i)] = 1; //mark it as a connection
//    	  		}else {
//    	  			adjMatrix[pointList.indexOf(p)][indexOfMovers.get(i)] = 0; //otherwise mark it as no connection
//    	  		}
    	  	}
    	}
    	//println("ghost movers updated!");
      	//print out my adjacency matrix
//      	for(int i = 0; i<adjMatrix.length; i++) {
//    		  for(int j = 0; j<adjMatrix[0].length; j++) {
//    			  print(adjMatrix[i][j]);
//    		  }
//    		  println();
//    	  }
      	
      background(0, 0, 100);  // white
      
      if (!playPaused) player.move();
      
      // half the time display player first; half the time last
      if (frameCount % 2 != 0) player.display(Pathfinder.this);
   
      // handle all ghosts
      for (Mover m : ghosts) {
        if (!playPaused) 
        	
        	m.move();
        	m.display(Pathfinder.this);
        
        // TODO: display movement paths?
      }
      
      bfsGhostLocation = new Point(ghosts[1].x, ghosts[1].y);//update where the bfs ghost is
      dijkstraGhostLocation = new Point(ghosts[2].x, ghosts[2].y);
      dfsGhostLocation = new Point(ghosts[3].x, ghosts[3].y);
      
      // half the time display player last; half the time first
      if (frameCount % 2 == 0) player.display(Pathfinder.this);
      
      wsCurr.display(Pathfinder.this);
      
      int elapsed = millis() - start;
      if (elapsed < 10000) {
        // in 1st 10 seconds of play mode, display instructions
        //   fading from 40 to 0 opacity in seconds 5-10,
        //   also keeping it no more than 40 in seconds 0-5
        fill(0, min(40, map(elapsed, 5000, 10000, 40, 0)));
        showInstructions();
      }
      
  	
  
      // TODO: display graph (perhaps based on settings)
      if(showGraph) {
    	  displayGraph();
      }
      
      
    }
    public void displayGraph() {
    	
    	for(int i = 0; i<adjMatrix.length; i++) {
      	  for(int j = 0; j < adjMatrix[0].length; j++) {
      		  //if there is a connection
      		  if(adjMatrix[i][j] == 1) {
      			  //println(pointList.get(i), pointList.get(j));
      			  //draw a line between the two points
      			  stroke(100,191,255); 
      			  strokeWeight(2);
      			  line(pointList.get(i).x, pointList.get(i).y, pointList.get(j).x, pointList.get(j).y);
      		  }
      	  }
        }
        //println("graph displayed!");
    }
    
    void showInstructions() {
      textAlign(LEFT, BOTTOM);
      textSize(12);
      float tSize = textAscent() + textDescent();
      float y = 0;
      
      text("PLAY MODE",          5, y += tSize);
      text("space: switch mode", 5, y += tSize);
      text("p: pause/unpause",   5, y += tSize);
      text("r: reset game",      5, y += tSize);
      text("g: show graph",      5, y += tSize);
    }
    
    public void keyPressed() {
      if (key == 'p') playPaused = !playPaused;
      if (key == 'r') resetPlayers();
      if (key == 'g') {
    	  if(showGraph == true) {
    		  showGraph = false;
    	  }else {
    		  showGraph = true;
    	  }
      }
      
      // TODO: control graph settings?
      
    }
   
    
    // entering and exiting play mode
    public void init() {
      createGraph();
      // TODO: create graph among Points
     
//      println("graph created!!!!!!!");
//  	  for(int i = 0; i<adjMatrix.length; i++) {
//  		  for(int j = 0; j<adjMatrix[0].length; j++) {
//  			  print(adjMatrix[i][j]);
//  		  }
//  		  println();
//  	  }
  	  //now adjMatrix contains all of the connections between points
  	
    }
    //THE GRAPH CREATED CONTAINS ALL LINKS FROM POINTS,
    //BUT FOR THE MOVER POINTS WHILE THEY ARE LINKED TO OTHER POINTS,
    //OTHER POINTS AREN'T LINKED TO THEM
    //THIS SHOULDN'T REALLY EFFECT BECAUSE KEEP IN MIND IF ONE X CAN GO TO Y,
    //THEN Y CAN GO TO X
    //JUST WATCH OUT FOR THIS (COULD BE A POSSIBLE PROBLEM IN THE FUTURE)
    public void createGraph(){
    	//println("this is done first");
    	resetPlayers();
    	//TODO: draw lines maybe to the corners of lines?
    	//(drawing graph)
    	pointList = new ArrayList<Point>();
    	//create adj matrix that can also contain all of my mover objects
    	adjMatrix = new int[wsCurr.points.size()+ghosts.length+1][wsCurr.points.size()+ghosts.length+1];
    	

    	//fill pointList with all of the points
        for(Point p : wsCurr.points) {
      	  pointList.add(p);
        }
        Point playerr = new Point(player.x, player.y);
        pointList.add(playerr);
        indexOfPlayer = (pointList.indexOf(playerr));
        for(Mover m : ghosts) {
        	Point moverr = new Point(m.x, m.y);
        	pointList.add(moverr);
        	indexOfMovers.add(pointList.indexOf(moverr));
        }
        //println(pointList);
        
        //iterate through pointList but don't edit the mover characters in the adjMatrix
    	int moversLength = 1+ghosts.length;
        for(int i = 0; i < pointList.size()-moversLength; i++) {
      	  for(int j = 0; j < pointList.size()-moversLength; j++) {
      		  //if my point has a connection to any of the other points
      		  if(wsCurr.isClearPath(pointList.get(i), pointList.get(j))) {
      			  adjMatrix[i][j] = 1;//mark the connection
      		  }else{    	//possible error is that I am marking a connection to the point itself making the next closest connection be itself
      			  adjMatrix[i][j] = 0;//mark as no connection
      		  }
      		
      	  }
        }
        
    }
    
    //FINISH 1 MOVE RULE THIS WEEK
    
    
    
    // reset player/enemy positions
    void resetPlayers() {
    	
      player = new Player(Pathfinder.this);
      ghosts = new Mover[]{
        new Mover(Pathfinder.this, new RandomPoint()),//one will randomly select an edge and move to that edge (maybe something like FIFO, queue up places to move)
        new Mover(Pathfinder.this, new bfs()),//bfs - finds bfs path to the player and head to first point in the path
        new Mover(Pathfinder.this, new dijkstra()),//dijkstra's - prioritize distance and calc shortest distance (can do this by calculating distance between points)
        new Mover(Pathfinder.this, new dfs())//dfs - find any path and follow it to the end (until it can't follow it anymore)
        												//progress one point at the time
        //if there is a path going from point directly to the player, it should go directly to the player
        //if no walls between point and player go straight to player
      };
      bfsGhostLocation = new Point(ghosts[1].x, ghosts[1].y);
      dijkstraGhostLocation = new Point(ghosts[2].x, ghosts[2].y);
      dfsGhostLocation = new Point(ghosts[3].x, ghosts[3].y);
      start = millis();
      
    }
  }
}
 
