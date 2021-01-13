package mergedbot.util;

import battlecode.common.*;

public class Pathfinder {
    private static RobotController rc;
    private static boolean bugpathBlocked = false;
    private static MapLocation prevLoc;
    private static int bugpathDir;
    private static int bugpathTermCount = 0;
    public static void init(RobotController rc) {
        Pathfinder.rc = rc;
    }
    public static int moveDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }
    public static double getAngle(MapLocation a, MapLocation b) {
    	int dx1 = a.x - Cache.MY_LOCATION.x;
    	int dy1 = a.y - Cache.MY_LOCATION.y;
    	int dx2 = b.x - Cache.MY_LOCATION.x;
    	int dy2 = b.y - Cache.MY_LOCATION.y;
    	return (dx1 * dx2 + dy1 * dy2) / (double)(Math.sqrt(dx1 * dx1 + dy1 * dy1) * Math.sqrt(dx2 * dx2 + dy2 * dy2));
    }
    public static double getMoveHeuristic(MapLocation nextLoc, MapLocation target) throws GameActionException {
    	double angle = getAngle(nextLoc, target);
    	double passability = rc.sensePassability(nextLoc) * 2 - 1;
    	return angle + passability;
    }
    public static int getBestHeuristicDirection(MapLocation target) throws GameActionException {
    	int next = -1;
      double heuristicValue = 0.0;

      for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
      	MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
      	if (!rc.onTheMap(nextLoc)) {
      		continue;
      	}
      	int tempDist = nextLoc.distanceSquaredTo(target);
      	double tempHeuristic = getMoveHeuristic(nextLoc, target);
      	if (tempHeuristic > heuristicValue) {
      		next = i;
      		heuristicValue = tempHeuristic;
      	}
      }
      return next;
    }
    public static int getBestMovableHeuristicDirection(MapLocation target) throws GameActionException {
    	int next = -1;
      double heuristicValue = 0.0;

      for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
      	MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
      	if (!rc.onTheMap(nextLoc) || !rc.canMove(Constants.ORDINAL_DIRECTIONS[i])) {
      		continue;
      	}
      	int tempDist = nextLoc.distanceSquaredTo(target);
      	double tempHeuristic = getMoveHeuristic(nextLoc, target);
      	if (tempHeuristic > heuristicValue) {
      		next = i;
      		heuristicValue = tempHeuristic;
      	}
      }
      return next;
    }
    public static int getBestBugpathHeuristic(MapLocation target, MapLocation bugpathLoc) throws GameActionException {
    	// termination condition will be a heuristic of passability, angle to target, and angle to bugpath dir
    	int next = -1;
      double heuristicValue = 0.0;
      for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
      	MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
      	if (!rc.onTheMap(nextLoc) || !rc.canMove(Constants.ORDINAL_DIRECTIONS[i])) {
      		continue;
      	}
      	int tempDist = nextLoc.distanceSquaredTo(target);
      	double tempHeuristic = getMoveHeuristic(nextLoc, target);
	    	tempHeuristic += getAngle(nextLoc, bugpathLoc);
      	if (tempHeuristic > heuristicValue) {
      		next = i;
      		heuristicValue = tempHeuristic;
      	}
      }
      return next;
    }

    public static boolean execute(MapLocation target) {
        int before = Clock.getBytecodesLeft();
        boolean res = executeBugpath(target);
        return res;
    	// return executeOrig(target);
    }

    public static boolean executeOrig(MapLocation target) {
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

    public static boolean executeBugpath(MapLocation target) {
        Util.setIndicatorLine(Cache.MY_LOCATION, target, 0, 0, 255);
        if (Cache.MY_LOCATION.equals(target)) {
            // already there
            return true;
        }

        int greedy = -1;
        int greedyDist = 1048576;
        int next = -1;
        try {
	        next = getBestHeuristicDirection(target);
	      } catch (GameActionException ex) {
	      	throw new IllegalStateException(ex);
	      }

        for (int i = 0; i < Constants.ORDINAL_DIRECTIONS.length; i++) {
        	MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[i]);
        	int tempDist = nextLoc.distanceSquaredTo(target);
        	if (tempDist < greedyDist) {
        		greedyDist = tempDist;
        		greedy = i;
        	}
        }

        if (!bugpathBlocked && greedy == next) {
        	if (rc.canMove(Constants.ORDINAL_DIRECTIONS[next])) {
	        	Util.move(Constants.ORDINAL_DIRECTIONS[next]);
	        	return true;
	        } else {
	        	return executeOrig(target);
	        }
        } else {
        	if (!bugpathBlocked) {
	        	bugpathBlocked = true;
	        	bugpathDir = next;
	        	bugpathTermCount = 0;
        	}
        	MapLocation bugpathTarget = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[bugpathDir]);
	        try {
		        next = getBestBugpathHeuristic(target, bugpathTarget);
		      } catch (GameActionException ex) {
		      	throw new IllegalStateException(ex);
		      }
		      if (next != -1 && rc.canMove(Constants.ORDINAL_DIRECTIONS[next]))  {
		      	// terminating condition is if next square we move to is closer to target AND angle between direction we moved and target is greater than some threshold
		      	MapLocation nextLoc = Cache.MY_LOCATION.add(Constants.ORDINAL_DIRECTIONS[next]);
		      	if (nextLoc.distanceSquaredTo(target) < Cache.MY_LOCATION.distanceSquaredTo(target)) {
				    	if (getAngle(nextLoc, target) > 0) {
				    		bugpathTermCount++;
				    		if (bugpathTermCount > 1) {
				      		bugpathBlocked = false;
				    		}
				    	} else {
				    		bugpathTermCount = 0;
				    	}
		      	} else {
		      		bugpathTermCount = 0;
		      	}
		      	Util.move(Constants.ORDINAL_DIRECTIONS[next]);
		      	return true;
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
