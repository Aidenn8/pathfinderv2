// interface allowing specification of movement rules
interface MoveRule {
  void move(Mover m);
}

// rule where no movement is ever done
class StandStill implements MoveRule {
  public void move(Mover m) {}


  
}

// rule to move in random direction at all times
class RandomMovement implements MoveRule {
  public void move(Mover m) {
    double angle = Math.random() * Math.PI * 2;
    
    m.moveTo(m.x + Math.cos(angle) * m.speed,
             m.y + Math.sin(angle) * m.speed);
  }

public void move(Mover m, Point p) {
	// TODO Auto-generated method stub
	
}
}

// rule to move towards a Point (which could be moving itself)
class MoveTo implements MoveRule {
  Point target;
  
  MoveTo(Point target) {
    this.target = target;
  }
  
  public void move(Mover m) {
    m.moveTo(target);
  }

public void move(Mover m, Point p) {
	// TODO Auto-generated method stub
	
}
}

//random movement
class RandomPoint implements MoveRule {
	Point target;
	Point curr;
	RandomPoint(){
	
	}
	//get a random point
	public void move(Mover m) {
		curr = new Point(m.x, m.y);//store curr mover pos as a point
		if(target == null) {
			target = m.pf.randomPointGhost();
		}
		if(curr.equals(target)) {//if I have reached my target point
			target = m.pf.randomPointGhost();//generate new random point and make it the target
		}
		m.moveTo(target);
		
	}
}
class bfs implements MoveRule{
	Point target;//point I want to go to
	Point curr;//curent mover position
	public void move(Mover m) {
		curr = new Point(m.x,m.y); //store curr mover pos
		if(target == null) {//if I don't have a target
			target = m.pf.getFirstPointFromPath();
		}
		if(curr.equals(target)) {//if I have reached the first point
			target = m.pf.getFirstPointFromPath();
		}
		m.moveTo(target);
	}
}

class dijkstra implements MoveRule{
	Point target;
	Point curr;
	public void move(Mover m) {
		curr = new Point(m.x,m.y); //store curr mover pos
		if(target == null) {//if I don't have a target
			target = m.pf.firstPointFromDijkstraPath();
		}
		//if(m.pf.wsCurr.isClearPath(curr, m.pf.playerPos) == false){
			if(curr.equals(target)){
			 //if I have reached the first point
				target = m.pf.firstPointFromDijkstraPath();
				
			}
			if(target.equals(m.pf.playerPos)) {
				if(m.pf.wsCurr.isClearPath(curr, m.pf.playerPos)) {
					target = m.pf.playerPos;
				}
			}
			m.moveTo(target);
		//}
//		}else if(m.pf.wsCurr.isClearPath(curr, m.pf.playerPos)){
//			m.moveTo(m.pf.playerPos);
//		}
		
	}
}

class dfs implements MoveRule{
	Point target;//point I want to go to
	Point curr;//curent mover position
	public void move(Mover m) {
		curr = new Point(m.x,m.y); //store curr mover pos
		if(target == null) {//if I don't have a target
			target = m.pf.getFirstPointFromDfs();
		}
		
		if(curr.equals(target)) {//if I have reached the first point
			target = m.pf.getFirstPointFromDfs();
		}
		m.moveTo(target);
		
		
		
	}
}











