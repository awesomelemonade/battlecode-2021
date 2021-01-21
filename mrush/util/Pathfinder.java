package mrush.util;

import battlecode.common.*;

public class Pathfinder {
    private static RobotController rc;
    private static boolean bugpathBlocked = false;
    private static int bugpathDir;
    private static int bugpathTermCount = 0;
    private static int moveableTwo;
    private static double[] bugpathCache = new double[8];
    private static MapLocation prevLoc = Cache.MY_LOCATION;
    private static int bugpathTurnCount = 0;
    private static MapLocation prevTarget;
    public static void init(RobotController rc) {
        Pathfinder.rc = rc;
    }
    public static int moveDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }
    // public static int moveDistance(MapLocation a, MapLocation b) {
        // return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    // }
    public static double getAngle(MapLocation a, MapLocation b, MapLocation c) {
      // vector c->a and c->b
    	int dx1 = a.x - c.x;
    	int dy1 = a.y - c.y;
    	int dx2 = b.x - c.x;
    	int dy2 = b.y - c.y;
    	return (dx1 * dx2 + dy1 * dy2) / (double)(Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)));
    }
    public static int getTwoStepMoveHeuristic(MapLocation target) throws GameActionException {
      int next = -1;
      double heuristicValue = -1024;

      int nextMoveable = -1;
      double heuristicValueMoveable = -1024;
      moveableTwo = -1;
      double tempHeuristic = 0;

      for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
        MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
        if (nextLoc.equals(prevLoc)) {
          continue;
        }
        
        switch (Constants.ORDINAL_DIRECTIONS[i]) {
          case NORTH:
            tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc));
            break;
          case NORTHEAST:
            tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
            // tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc));
            break;
          case EAST:
            tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
            break;
          case SOUTHEAST:
            tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
            // tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
            break;
          case SOUTH:
            tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc));
            break;
          case SOUTHWEST:
            tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
            // tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc));
            break;
          case WEST:
            tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc));
            break;
          case NORTHWEST:
            tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc));
            // tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc));
            break;
        }
        bugpathCache[i] = getMoveHeuristic(nextLoc, target, Cache.MY_LOCATION);
        tempHeuristic = (tempHeuristic) + bugpathCache[i];
        if (tempHeuristic > heuristicValue) {
          next = i;
          heuristicValue = tempHeuristic;
        }
        if (rc.onTheMap(nextLoc) && rc.canMove(Constants.ORDINAL_DIRECTIONS[i]) && tempHeuristic > heuristicValueMoveable) {
          moveableTwo = i;
          heuristicValueMoveable = tempHeuristic;
        } else {
          bugpathCache[i] = -1024;
        }
      }
      return next;
    }
    public static double getMoveHeuristic(MapLocation nextLoc, MapLocation target, MapLocation curLoc) throws GameActionException {
      if (!rc.onTheMap(nextLoc)) {
        return -1024;
      }
    	// double angle = getAngle(nextLoc, target, curLoc);
    	double passability = 2 * Math.log(rc.sensePassability(nextLoc));
      double closer = Math.sqrt(curLoc.distanceSquaredTo(target)) - Math.sqrt(nextLoc.distanceSquaredTo(target));
    	return (passability + closer * 0.8);
    }
    public static int getBestHeuristicDirection(MapLocation target) throws GameActionException {
    	int next = -1;
      double heuristicValue = -1024;
      double moveValue = 0;
      moveableTwo = -1;

      for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
      	MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
      	if (!rc.onTheMap(nextLoc)) {
      		continue;
      	}
      	double tempHeuristic = getMoveHeuristic(nextLoc, target, Cache.MY_LOCATION);
        bugpathCache[i] = tempHeuristic;
      	if (tempHeuristic > heuristicValue) {
      		next = i;
      		heuristicValue = tempHeuristic;
      	}
        if (rc.canMove(Constants.ORDINAL_DIRECTIONS[i])) {
          if (heuristicValue > moveValue) {
            moveableTwo = i;
            moveValue = heuristicValue;
          }
        } else {
          bugpathCache[i] = -1;
        }
      }
      return next;
    }

    public static int getBestBugpathHeuristic(MapLocation target, MapLocation bugpathLoc) throws GameActionException {
      // termination condition will be a heuristic of passability, angle to target, and angle to bugpath dir
      int next = -1;
      double heuristicValue = -1024;
      for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
        MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
        double tempHeuristic = bugpathCache[i];
        if (tempHeuristic == -1024) {
          continue;
        }
        // tempHeuristic += getAngle(nextLoc, bugpathLoc, Cache.MY_LOCATION);
        tempHeuristic += Math.sqrt(Cache.MY_LOCATION.distanceSquaredTo(bugpathLoc)) - Math.sqrt(nextLoc.distanceSquaredTo(bugpathLoc)) * 0.8;
        // tempHeuristic += (moveDistance(Cache.MY_LOCATION, bugpathLoc) - moveDistance(nextLoc, bugpathLoc)) * 2 + 3;
        if (tempHeuristic > heuristicValue) {
          next = i;
          heuristicValue = tempHeuristic;
        }
      }
      return next;
    }

    public static boolean execute(MapLocation target) {
        Util.setIndicatorLine(Cache.MY_LOCATION, target, 0, 0, 255);
        boolean res;
        if (Cache.MY_LOCATION.distanceSquaredTo(target) <= 2) {
            if (rc.canMove(Cache.MY_LOCATION.directionTo(target))) {
                Util.move(Cache.MY_LOCATION.directionTo(target));
                res = true;
            } else {
                res = executeOrig(target);
            }
        } else {
            res = executeBugpath(target);
        }
        // boolean res = executeFullHeuristic(target);
        prevLoc = Cache.MY_LOCATION;
        prevTarget = target;
        return res;
    	// return executeOrig(target);
    }

    public static boolean executeFullHeuristic(MapLocation target) {
      int res = -1;
      try {
        res = getTwoStepMoveHeuristic(target);
      } catch (GameActionException ex) {
        throw new IllegalStateException(ex);
      }
      if (moveableTwo != -1) {
        Util.move(Constants.ORDINAL_DIRECTIONS[moveableTwo]);
        return true;
      }
      return false;
    }

    public static boolean executeOrig(MapLocation target) {
        if (Cache.MY_LOCATION.equals(target)) {
            // already there
            return true;
        }
        // Out of all possible moves that lead to a lower euclidean distance OR lower move distance,
        // find the direction that goes to the highest passability
        // euclidean distance defined by dx^2 + dy^2
        // move distance defined by max(dx, dy)
        // ties broken by "preferred direction" dictated by Constants.getAttemptOrder
        double highestPassability = 0;
        int targetDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(target) - 1; // subtract 1 to be strictly less
        int targetMoveDistance = moveDistance(Cache.MY_LOCATION, target);
        Direction bestDirection = null;
        for (Direction direction : Constants.getAttemptOrder(Cache.MY_LOCATION.directionTo(target))) {
            if (rc.canMove(direction)) {
                MapLocation location = Cache.MY_LOCATION.add(direction);
                if (location.isWithinDistanceSquared(target, targetDistanceSquared) || moveDistance(location, target) < targetMoveDistance) {
                    try {
                        double passability = rc.sensePassability(location);
                        if (passability > highestPassability) {
                            highestPassability = passability;
                            bestDirection = direction;
                        }
                    } catch (GameActionException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
        }
        if (bestDirection != null) {
            Util.move(bestDirection);
            return true;
        }
        return false;
    }

    public static int greedyMove(MapLocation target) {
        int leastDist = target.distanceSquaredTo(Cache.MY_LOCATION);
        int next = -1;
        for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
            MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
            int temp_dist = nextLoc.distanceSquaredTo(target);
            if (temp_dist < leastDist && rc.canMove(Constants.ORDINAL_DIRECTIONS[i])) {
                leastDist = temp_dist;
                next = i;
            }
        }
        return next;
    }

    public static boolean executeFallback(MapLocation target, int next) {
      int greedy = greedyMove(target);
      if (greedy == next) {
        bugpathBlocked = false;
      }
      if (greedy != -1) {
        Util.move(Constants.ORDINAL_DIRECTIONS[greedy]);
        return true;
      }
      return false;
    }

    public static boolean executeBugpath(MapLocation target) {
        if (Cache.MY_LOCATION.equals(target)) {
            // already there
            return true;
        }

        if (!target.equals(prevTarget)) {
          bugpathTurnCount = 0;
        }

        // int greedy = -1;
        // int greedyDist = 1048576;
        int next = -1;

        try {
	        // next = getBestHeuristicDirection(target);
          next = getTwoStepMoveHeuristic(target);
	      } catch (GameActionException ex) {
	      	throw new IllegalStateException(ex);
	      }

        /*for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
        	MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
        	int tempDist = nextLoc.distanceSquaredTo(target);
        	if (tempDist < greedyDist) {
        		greedyDist = tempDist;
        		greedy = i;
        	}
        }*/
        if (bugpathBlocked && bugpathTurnCount > 4) {
          // Util.println("BUGPATHFAIL: " + Cache.MY_LOCATION);
          return executeFallback(target, next);
        }
        if ((!bugpathBlocked && Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[next]).distanceSquaredTo(target) < Cache.MY_LOCATION.distanceSquaredTo(target))) {
        	if (rc.canMove(Constants.ORDINAL_DIRECTIONS[next])) {
	        	Util.move(Constants.ORDINAL_DIRECTIONS[next]);
	        	return true;
	        } else {
            // try going to moveable two step
            // maybe lower bound heuristic threshold here
            if (moveableTwo != -1 && rc.canMove(Constants.ORDINAL_DIRECTIONS[moveableTwo])) {
              Util.move(Constants.ORDINAL_DIRECTIONS[moveableTwo]);
              return true;
            }
	        }
        } else {
        	if (!bugpathBlocked) {
	        	bugpathBlocked = true;
	        	bugpathDir = next;
	        	bugpathTermCount = 0;
            bugpathTurnCount = 0;
        	}
        	MapLocation bugpathTarget = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[bugpathDir]).add(Constants.ORDINAL_DIRECTIONS[bugpathDir]);
	        try {
		        next = getBestBugpathHeuristic(target, bugpathTarget);
		      } catch (GameActionException ex) {
		      	throw new IllegalStateException(ex);
		      }

          // if next move moves backwards then we cancel bugpathing
          if (next != -1) {
            MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[next]);
            if (getAngle(nextLoc, target, Cache.MY_LOCATION) < -0.5) {
              bugpathTurnCount = 5;
              return executeFallback(target, next);
            }
  		      if (rc.canMove(Constants.ORDINAL_DIRECTIONS[next]))  {
              Util.move(Constants.ORDINAL_DIRECTIONS[next]);
              bugpathTurnCount++;
  		      	// terminating condition is if next square we move to is closer to target AND angle between direction we moved and target is greater than some threshold
  		      	if (nextLoc.distanceSquaredTo(target) < Cache.MY_LOCATION.distanceSquaredTo(target)) {
  				    	if (getAngle(nextLoc, target, Cache.MY_LOCATION) > 0) {
  				    		bugpathTermCount++;
  				    		if (bugpathTermCount > 1) {
  				      		bugpathBlocked = false;
                    bugpathTurnCount = 0;
  				    		}
  				    	} else {
  				    		bugpathTermCount = 0;
  				    	}
  		      	} else {
  		      		bugpathTermCount = 0;
  		      	}
  		      	return true;
  		      }
          }
        }
        return false;
    }
    public static boolean executeSpacedApart(MapLocation target) {
        Util.setIndicatorLine(Cache.MY_LOCATION, target, 0, 0, 255);
        if (Cache.MY_LOCATION.equals(target)) {
            // already there
            return true;
        }
        // Out of all possible moves that lead to a lower euclidean distance OR lower move distance,
        // find the direction that goes to the highest passability
        // euclidean distance defined by dx^2 + dy^2
        // move distance defined by max(dx, dy)
        // ties broken by "preferred direction" dictated by Constants.getAttemptOrder
        double highestPassability = 0;
        int targetDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(target) - 1; // subtract 1 to be strictly less
        int targetMoveDistance = moveDistance(Cache.MY_LOCATION, target);
        Direction bestDirection = null;
        for (Direction direction : Constants.getAttemptOrder(Cache.MY_LOCATION.directionTo(target))) {
            if (rc.canMove(direction) && !Util.hasAdjacentAllyRobot(Cache.MY_LOCATION.add(direction))) {
                MapLocation location = Cache.MY_LOCATION.add(direction);
                if (location.isWithinDistanceSquared(target, targetDistanceSquared) || moveDistance(location, target) < targetMoveDistance) {
                    try {
                        double passability = rc.sensePassability(location);
                        if (passability > highestPassability) {
                            highestPassability = passability;
                            bestDirection = direction;
                        }
                    } catch (GameActionException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
        }
        if (bestDirection != null) {
            Util.move(bestDirection);
            return true;
        }
        return false;
    }
}
