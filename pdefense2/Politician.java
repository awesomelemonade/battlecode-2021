package pdefense2;

import battlecode.common.*;
import pdefense2.util.Cache;
import pdefense2.util.Constants;
import pdefense2.util.LatticeUtil;
import pdefense2.util.MapInfo;
import pdefense2.util.Pathfinder;
import pdefense2.util.UnitCommunication;
import pdefense2.util.Util;

import java.util.Comparator;


public strictfp class Politician implements RunnableBot {
    private static RobotController rc;
    private static int power;
    private static final int[] DEFENSE_SQUARES_X = {2, 0, -2, 0, 2, 1, -1, -2, -2, -1, 1, 2, 2, -2, -2, 2, 3, 0, -3,
            0};
    private static final int[] DEFENSE_SQUARES_Y = {0, 2, 0, -2, 1, 2, 2, 1, -1, -2, -2, -1, 2, 2, -2, -2, 0, 3, 0,
            -3};
    private static boolean defender;
    private static MapLocation nearestAllyEC;
    private static MapLocation nearestEnemyEC;
    private static MapLocation nearestS;
    private static boolean selfempowerer;
    private static boolean attacking = false;

    private static int currentConviction;
    private static int currentConviction_10; // = currentConviction - 10
    private static double currentEmpowerFactor;

    public Politician(RobotController rc) {
        Politician.rc = rc;
    }

    @Override
    public void init() throws GameActionException {
        if (Math.random() < 0.4 || rc.getRoundNum() <= 100) {
            defender = true;
        } else {
            defender = false;
        }
        if (rc.getInfluence() % 10 == 6) {
            selfempowerer = true;
        } else {
            selfempowerer = false;
        }
    }

    private static void preTurn() throws GameActionException {
        currentConviction = rc.getConviction();
        currentConviction_10 = currentConviction - 10;
        currentEmpowerFactor = rc.getEmpowerFactor(Constants.ALLY_TEAM, 0);
        power = (int) (currentConviction_10 * currentEmpowerFactor);

        nearestAllyEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getClosestLocation().orElse(null);
        nearestEnemyEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).getClosestLocation().orElse(null);
        int minDist = Integer.MAX_VALUE;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (UnitCommunication.isPotentialSlanderer(robot)) {
                int dist = robot.location.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < minDist) {
                    minDist = dist;
                    nearestS = robot.location;
                }
            }
        }
        if (rc.getRoundNum() % 25 == 0) attacking = true;
    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        preTurn();
        if (currentConviction_10 <= 0) { // useless; best thing to do is try to block an enemy ec
            if (campEnemyEC()) {
                return;
            }
            Util.smartExplore();
            return;
        }
        if (currentConviction >= 50 && tryClaimEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 0, 255); // blue
            return;
        }
        if (currentConviction >= 50 && tryHealEC()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 102, 51, 0); // brown
            return;
        }
        if (tryEmpower()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 255); // cyan
            return;
        }
        if (chaseWorthwhileEnemy()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 0); // green
            return;
        }
        if (selfempowerer && currentConviction >= 50 && trySelfBuff()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 102, 102, 153); // bluish purple
            return;
        }
        if (currentConviction >= 50 && goToECs()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 255, 0); // yellow
            return;
        }
        if ((defender && currentConviction < 50) && tryDefend()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 255, 0, 255); // pink
            return;
        }
        if (Util.smartExplore()) {
            return;
        }
    }

    public static boolean shouldAttack(MapLocation loc) {
        if (rc.getRoundNum() <= 300) return true;
        if (attacking) return true;
        return loc.distanceSquaredTo(Cache.MY_LOCATION) >= 121;
    }

    public static boolean tryEmpowerAtEC(MapLocation loc, Team team) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            RobotInfo ec = rc.senseRobotAtLocation(loc);
            if (ec == null) {
                Util.println("WARNING: No EC detected at " + loc);
                return false;
            }
            int ecConviction = ec.getConviction();
            // Check if empowering will take the ec
            int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            if (team == Team.NEUTRAL) {
                // TODO: we should only claim if there aren't that many enemy politicians nearby
                if (distanceSquared <= 9) {
                    // empower range - see if we can take it ourselves
                    if (currentConviction_10 > ecConviction && rc.senseNearbyRobots(distanceSquared).length == 1) {
                        rc.empower(distanceSquared);
                        return true;
                    }
                }
                if (distanceSquared <= 16) {
                    int numNeighborsOpen = 0;
                    MapLocation closestCardinalAdjacentSquare = null;
                    int closestDistanceSquared = Integer.MAX_VALUE;
                    for (Direction direction : Constants.CARDINAL_DIRECTIONS) {
                        MapLocation neighbor = loc.add(direction);
                        if (rc.onTheMap(neighbor)) {
                            RobotInfo neighborRobot = rc.senseRobotAtLocation(neighbor);
                            if (neighborRobot == null) {
                                numNeighborsOpen++;
                                int dist = Cache.MY_LOCATION.distanceSquaredTo(neighbor);
                                if (dist < closestDistanceSquared) {
                                    closestCardinalAdjacentSquare = neighbor;
                                    closestDistanceSquared = dist;
                                }
                            } else {
                                if (neighborRobot.getType() != RobotType.ENLIGHTENMENT_CENTER) {
                                    numNeighborsOpen++;
                                }
                            }
                        }
                    }
                    if (distanceSquared <= 1) {
                        // Add up our influence & enemy influence
                        RobotInfo[] allNearbyRobots = rc.senseNearbyRobots(loc, 16, null);
                        int convictionBalance = Math.max(0, currentConviction_10); // current robot's conviction
                        boolean hasEnemy = false;
                        for (int i = allNearbyRobots.length; --i >= 0;) {
                            RobotInfo robot = allNearbyRobots[i];
                            if (robot.getType() == RobotType.POLITICIAN) {
                                if (robot.getTeam() == Constants.ALLY_TEAM) {
                                    convictionBalance += Math.max(0, robot.getConviction() - 10);
                                }
                                if (robot.getTeam() == Constants.ENEMY_TEAM) {
                                    hasEnemy = true;
                                    convictionBalance -= Math.max(0, robot.getConviction() - 10);
                                }
                            }
                        }
                        if (convictionBalance > 5) {
                            if ((!hasEnemy) && convictionBalance > ecConviction) {
                                rc.empower(1);
                                return true;
                            }
                            if (convictionBalance > ecConviction + 20) {
                                rc.empower(1);
                                return true;
                            }
                            RobotInfo[] allyRobots = rc.senseNearbyRobots(loc, 1, Constants.ALLY_TEAM);
                            if ((allyRobots.length + 1) == numNeighborsOpen) {
                                rc.empower(1);
                                return true;
                            }
                        }
                        // Stay
                        return true;
                    } else {
                        // Pathfind to nearest cardinal-adjacent square
                        if (closestCardinalAdjacentSquare == null) {
                            Pathfinder.execute(loc);
                        } else {
                            Pathfinder.execute(closestCardinalAdjacentSquare);
                        }
                        return true;
                    }
                }
            } else {
                if (distanceSquared <= 16) {
                    if (distanceSquared <= 1 || distanceSquared <= 9 && rc.senseNearbyRobots(distanceSquared).length == 1) {
                        rc.empower(distanceSquared);
                        return true;
                    }
                    MapLocation closestCardinalAdjacentSquare = null;
                    int closestDistanceSquared = Integer.MAX_VALUE;
                    for (Direction direction : Constants.CARDINAL_DIRECTIONS) {
                        MapLocation neighbor = loc.add(direction);
                        if (rc.onTheMap(neighbor)) {
                            RobotInfo neighborRobot = rc.senseRobotAtLocation(neighbor);
                            if (neighborRobot == null) {
                                int dist = Cache.MY_LOCATION.distanceSquaredTo(neighbor);
                                if (dist < closestDistanceSquared) {
                                    closestCardinalAdjacentSquare = neighbor;
                                    closestDistanceSquared = dist;
                                }
                            }
                        }
                    }
                    if (closestCardinalAdjacentSquare == null) {
                        Pathfinder.execute(loc);
                    } else {
                        Pathfinder.execute(closestCardinalAdjacentSquare);
                    }
                    return true;
                }
            }
        }
        Pathfinder.execute(loc);
        return true;
    }

    public static boolean shouldHeal(RobotInfo ec) throws GameActionException {
        if (Constants.SPAWN.isWithinDistanceSquared(ec.getLocation(), 16)) {
            return false;
        }
        // heal if ec is low and surrounded by enemy ps
        int enemyConviction = 0;
        RobotInfo[] enemies = rc.senseNearbyRobots(ec.location, 64, Constants.ENEMY_TEAM);
        for (RobotInfo robot : enemies) {
            if (robot.getType() == RobotType.POLITICIAN) {
                enemyConviction += robot.getConviction() - 10;
            }
        }
        if (2 * enemyConviction > ec.getConviction()) {
            return true;
        }
        return false;
    }

    public static boolean trySelfBuff() throws GameActionException {
        if (currentEmpowerFactor < 1.5) return false;
        int bestDist = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null) return false;
        return tryEmpowerAtEC(bestLoc, Constants.ALLY_TEAM);
    }

    public static boolean tryHealEC() throws GameActionException {
        int bestDist = Integer.MAX_VALUE;
        MapLocation bestLoc = null;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && shouldHeal(robot)) {
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null) return false;
        return tryEmpowerAtEC(bestLoc, Constants.ALLY_TEAM);
    }

    // if sees empty square next to ec, go to it
    public static boolean campEnemyEC() throws GameActionException {
        if (nearestEnemyEC == null) return false;
        if (!Cache.MY_LOCATION.isWithinDistanceSquared(nearestEnemyEC, 25)) {
            return Util.tryMove(nearestEnemyEC);
        }
        if (Cache.MY_LOCATION.isWithinDistanceSquared(nearestEnemyEC, 2)) {
            return true;
        }
        // TODO: Loop unrolling?
        for (Direction d : Constants.ORDINAL_DIRECTIONS) {
            MapLocation loc = nearestEnemyEC.add(d);
            if (rc.canSenseLocation(loc) && !rc.isLocationOccupied(loc)) {
                Util.tryMove(loc);
                return true;
            }
        }
        return false;
    }

    // Lower defense score better
    public static int getDefenseScore(MapLocation loc) {
        if (loc.x%3 != 0 || loc.y%3 != 0) {
            return Integer.MAX_VALUE;
        }
        if (nearestAllyEC == null) {
            return Integer.MAX_VALUE;
        }
        int dist = nearestAllyEC.distanceSquaredTo(loc);
        if (dist <= 16)
            return Integer.MAX_VALUE;
        // TODO: Unroll loop?
        for (Direction d : Constants.ORDINAL_DIRECTIONS) {
            MapLocation adj = loc.add(d);
            if (rc.canSenseLocation(adj)) {
                try {
                    RobotInfo robot = rc.senseRobotAtLocation(adj);
                    if (robot != null && UnitCommunication.isPotentialSlanderer(robot)) {
                        return Integer.MAX_VALUE;
                    }
                } catch (GameActionException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return nearestEnemyEC == null ? 5000 * dist : 5000 * dist + nearestEnemyEC.distanceSquaredTo(loc);
    }

    // Does not handle empowering or chasing after muckrakers/other enemies
    public static boolean tryDefend() throws GameActionException {
        double x = Cache.MY_LOCATION.x;
        double y = Cache.MY_LOCATION.y;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo ally = Cache.ALLY_ROBOTS[i];
            MapLocation allyLocation = ally.getLocation();
            // force vectors
            if (ally.getType() == RobotType.POLITICIAN && ally.getConviction() <= 20) {
                double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(allyLocation);
                double distance = Math.sqrt(distanceSquared);
                double distanceCubed = distance * distanceSquared;
                // repel force
                // (1 / d^2) * (vec / d) = (c * vec / d^3)
                x -= 2.25 * (allyLocation.x - Cache.MY_LOCATION.x) / distanceCubed;
                y -= 2.25 * (allyLocation.y - Cache.MY_LOCATION.y) / distanceCubed;
            }
            if (UnitCommunication.isPotentialSlanderer(ally)) {
                double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(allyLocation);
                double distance = Math.sqrt(distanceSquared);
                double distanceCubed = distance * distanceSquared;
                // repel force
                // (1 / d^2) * (vec / d) = (c * vec / d^3)
                x -= 2.25 * (allyLocation.x - Cache.MY_LOCATION.x) / distanceCubed;
                y -= 2.25 * (allyLocation.y - Cache.MY_LOCATION.y) / distanceCubed;
                /*double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(allyLocation);
                double distance = Math.sqrt(distanceSquared);
                // force based on distance
                double forceLocationX = allyLocation.x + (Cache.MY_LOCATION.x - allyLocation.x) / distance * 3.0;
                double forceLocationY = allyLocation.y + (Cache.MY_LOCATION.y - allyLocation.y) / distance * 3.0;
                Util.setIndicatorLine(allyLocation, new MapLocation((int) Math.round(forceLocationX), (int) Math.round(forceLocationY)), 255, 255, 255);
                double forceLocationDx = forceLocationX - Cache.MY_LOCATION.x;
                double forceLocationDy = forceLocationY - Cache.MY_LOCATION.y;
                double forceDistanceSquared = forceLocationDx * forceLocationDx + forceLocationDy * forceLocationDy;
                double forceDistanceCubed = forceDistanceSquared * Math.sqrt(forceDistanceSquared);
                x += 0.75 * forceLocationDx / forceDistanceCubed;
                y += 0.75 * forceLocationDy / forceDistanceCubed;*/
            }
        }
        if(MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(MapInfo.mapMinX, Cache.MY_LOCATION.y);
            double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            double distance = Math.sqrt(distanceSquared);
            double distanceCubed = distance * distanceSquared;
            // repel force
            // (1 / d^2) * (vec / d) = (c * vec / d^3)
            x -= 2.25 * (loc.x - Cache.MY_LOCATION.x) / distanceCubed;
            y -= 2.25 * (loc.y - Cache.MY_LOCATION.y) / distanceCubed;
        }
        if(MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(MapInfo.mapMaxX, Cache.MY_LOCATION.y);
            double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            double distance = Math.sqrt(distanceSquared);
            double distanceCubed = distance * distanceSquared;
            // repel force
            // (1 / d^2) * (vec / d) = (c * vec / d^3)
            x -= 2.25 * (loc.x - Cache.MY_LOCATION.x) / distanceCubed;
            y -= 2.25 * (loc.y - Cache.MY_LOCATION.y) / distanceCubed;
        }
        if(MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(Cache.MY_LOCATION.x, MapInfo.mapMinY);
            double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            double distance = Math.sqrt(distanceSquared);
            double distanceCubed = distance * distanceSquared;
            // repel force
            // (1 / d^2) * (vec / d) = (c * vec / d^3)
            x -= 2.25 * (loc.x - Cache.MY_LOCATION.x) / distanceCubed;
            y -= 2.25 * (loc.y - Cache.MY_LOCATION.y) / distanceCubed;
        }
        if(MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(Cache.MY_LOCATION.x, MapInfo.mapMaxY);
            double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            double distance = Math.sqrt(distanceSquared);
            double distanceCubed = distance * distanceSquared;
            // repel force
            // (1 / d^2) * (vec / d) = (c * vec / d^3)
            x -= 2.25 * (loc.x - Cache.MY_LOCATION.x) / distanceCubed;
            y -= 2.25 * (loc.y - Cache.MY_LOCATION.y) / distanceCubed;
        }
        Util.setIndicatorDot(Cache.MY_LOCATION, 102, 51, 0); // brown
        MapLocation targetLocation = new MapLocation((int) Math.round(x), (int) Math.round(y));
        if (!targetLocation.equals(Cache.MY_LOCATION)) {
            Pathfinder.execute(targetLocation);
        }
        return true;
    }

    private static Comparator<MapLocation> tiebreaker = Comparator.comparingInt((MapLocation loc) -> 100000 * loc.x + loc.y);
    private static Comparator<MapLocation> compareECs = Comparator.comparingInt((MapLocation loc) ->
            MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM)
                    .getClosestLocationDistance(loc, 1024) +
                    MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM)
                            .getClosestLocationDistance(loc, 1024) / 3)
            .thenComparing(tiebreaker);
    public static boolean goToECs() {
        MapLocation bestNeutralEC = MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).minLocation(compareECs).orElse(null);
        MapLocation bestEnemyEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).minLocation(compareECs).orElse(null);
        if (bestNeutralEC == null && bestEnemyEC == null) {
            return false;
        }
        if (bestEnemyEC == null || (bestNeutralEC != null && compareECs.compare(bestNeutralEC, bestEnemyEC) < 0)) {
            // go for neutral EC
            Pathfinder.execute(bestNeutralEC);
        } else {
            // go for enemy EC
            if (!shouldAttack(bestEnemyEC)) {
                return false;
            }
            Pathfinder.execute(bestEnemyEC);
        }
        return true;
    }

    private static int getScore(int radiusSquared) throws GameActionException {
        int numUnits = rc.senseNearbyRobots(radiusSquared).length;
        if (numUnits == 0) {
            return 0;
        }
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(radiusSquared, Constants.ENEMY_TEAM);
        if (enemyRobots.length + Cache.NEUTRAL_ROBOTS.length == 0) {
            return 0;
        }
        int damage = power / numUnits;
        int mKills = 0;
        int pKills = 0;
        int ecKills = 0;
        int mConviction = 0;
        int ecConviction = 0;
        int pConviction = 0;
        int distMtoS = 9999;
        int distPtoEC = 9999;
        for (int i = enemyRobots.length; --i >= 0; ) {
            RobotInfo enemy = enemyRobots[i];
            if (enemy.getType() == RobotType.POLITICIAN) {
                pConviction += Math.max(0, Math.min(damage, enemy.getConviction() - 10)) +
                        Math.max(0, Math.min(damage - enemy.getConviction(), enemy.getInfluence()) - 10);
                if (damage > enemy.getConviction() - 10) {
                    pKills++;
                }
                if (nearestAllyEC != null)
                    distPtoEC = Math.min(distPtoEC, enemy.location.distanceSquaredTo(nearestAllyEC));
            } else if (enemy.getType() == RobotType.MUCKRAKER) {
                mConviction += Math.min(damage, enemy.getConviction());
                if (damage > enemy.getConviction()) {
                    mKills++;
                }
                if (nearestS != null) distMtoS = Math.min(distMtoS, enemy.location.distanceSquaredTo(nearestS));
            } else if (enemy.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                ecConviction += Math.min(damage, enemy.getConviction());
                if (damage > enemy.getConviction()) {
                    ecKills++;
                }
            }
        }
        for (RobotInfo neutralEC : Cache.NEUTRAL_ROBOTS) {
            if (Cache.MY_LOCATION.isWithinDistanceSquared(neutralEC.getLocation(), radiusSquared)) {
                ecConviction += Math.min(damage, neutralEC.getConviction());
                if (damage > neutralEC.getConviction()) {
                    ecKills++;
                }
            }
        }
        int numKills = ecKills + mKills + pKills;
        int score = ecKills * 100000 * 100000 + numKills * 100000 + mConviction + ecConviction;
        if (shouldEmpower(ecKills, mKills, pKills, mConviction, ecConviction, pConviction, distPtoEC, distMtoS)) {
            return score;
        } else {
            return 0;
        }
    }

    public static boolean tryClaimEC() throws GameActionException {
        // if we see enemy/neutral ec, try to move closer to it
        // if can't move any closer, explode
        MapLocation bestLoc = null;
        int bestDist = Integer.MAX_VALUE;
        Team bestTeam = null;

        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.ENEMY_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation location = robot.getLocation();
                int distance = location.distanceSquaredTo(Cache.MY_LOCATION);
                if (distance < bestDist) {
                    bestDist = distance;
                    bestLoc = location;
                    bestTeam = Constants.ENEMY_TEAM;
                }
            }
        }
        for (int i = Cache.NEUTRAL_ROBOTS.length; --i >= 0; ) {
            RobotInfo robot = Cache.NEUTRAL_ROBOTS[i];
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                MapLocation location = robot.getLocation();
                int distance = location.distanceSquaredTo(Cache.MY_LOCATION);
                if (distance < bestDist) {
                    bestDist = distance;
                    bestLoc = location;
                    bestTeam = Team.NEUTRAL;
                }
            }
        }
        if (bestLoc == null)
            return false;
        return tryEmpowerAtEC(bestLoc, bestTeam);
    }

    public static boolean shouldEmpower(int ecKills, int mKills, int pKills, int mConviction, int ecConviction, int pConviction, int distPtoEC, int distMtoS) throws GameActionException {
        /*
        Util.println("\tecKills = " + ecKills);
        Util.println("\tmKills = " + mKills);
        Util.println("\tpKills = " + pKills);
        Util.println("\tmConviction = " + mConviction);
        Util.println("\tecConviction = " + ecConviction);
        Util.println("\tpConviction = " + pConviction);
        Util.println("\tdistPtoEC = " + distPtoEC);
        Util.println("\tdistMtoS = " + distMtoS);*/

        int mecConviction = mConviction + ecConviction;

        // Convert remaining units if we're losing on votes
        if (rc.getRoundNum() >= 1490 && mecConviction + pConviction > 0 &&
                rc.getRobotCount() >= 250 && rc.getTeamVotes() < 751) {
            //System.out.println("a");
            return true;
        }
        if (mecConviction + pConviction > currentConviction) return true;
        if (ecKills + mKills + pKills >= 8) return true;
        if (ecKills >= 1) {
            //System.out.println("b");
            return true;
        }
        int numKills = mKills + pKills;
        if (currentConviction < 50) {
            if (distMtoS <= 100 && mKills >= 1) {
                //System.out.println("c");
                return true;
            }
            if (distPtoEC <= 8 && pKills >= 1) {
                //System.out.println("d");
                return true;
            }
            if (mKills >= 2) {
                //System.out.println("e");
                return true;
            }
            if (nearestAllyEC != null &&
                    nearestAllyEC.isWithinDistanceSquared(Cache.MY_LOCATION, 100) && mKills >= 1) {
                //System.out.println("f");
                return true;
            }
        }
        if (currentConviction_10 >= 50 && numKills * 15 >= currentConviction_10) {
            //System.out.println("g");
            return true;
        }
        int distFromEC = nearestAllyEC == null ? 1024 : nearestAllyEC.distanceSquaredTo(Cache.MY_LOCATION);
        double euclidDist = Math.sqrt(distFromEC);
        double requiredRatio = 1.0 / (0.03 * (euclidDist + 5.0)) + 1;
        if (mConviction * requiredRatio >= currentConviction_10) {
            //System.out.println("h: " + requiredRatio + " - " + currentConviction_10);
            return true;
        }
        if (distPtoEC <= 2 && pConviction * 8 >= currentConviction_10) {
            //System.out.println("i");
            return true;
        }
        return false;
    }

    public static boolean tryEmpower() throws GameActionException {
        if (power <= 0) {
            return false;
        }
        if (Cache.ENEMY_ROBOTS.length + Cache.NEUTRAL_ROBOTS.length == 0) {
            return false;
        }
        // if can kill something, maximize the number
        int bestRadius = -1;
        int bestScore = 0;
        for (int r = 1; r <= Constants.POLITICIAN_ACTION_RADIUS_SQUARED; r++) {
            int score = getScore(r);
            if (score > bestScore) {
                bestScore = score;
                bestRadius = r;
            }
        }
        if (bestRadius == -1) {
            return false;
        }
        rc.empower(bestRadius);
        return true;
    }

    private static boolean shouldChase(RobotInfo robot) {
        if (robot.getType() == RobotType.POLITICIAN)
            return false;
        int robotConviction = robot.getConviction();
        if (currentConviction * 2 + 20 >= robotConviction
                && robotConviction * 2 + 20 >= currentConviction)
            return true;
        MapLocation robotLocation = robot.getLocation();
        if (robot.getType() == RobotType.MUCKRAKER
                && ((nearestS != null && robotLocation.isWithinDistanceSquared(nearestS, 81))
                || (nearestAllyEC != null && robotLocation.isWithinDistanceSquared(nearestAllyEC, 81)))
                && robotConviction * 10 + 30 >= currentConviction)
            return true;
        return false;
    }

    private static boolean chaseWorthwhileEnemy() throws GameActionException {
        int totx = 0;
        int toty = 0;
        int tot = 0;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (shouldChase(robot)) {
                int numNearbyPoliticians = 0;
                MapLocation loc = robot.location;
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, 5, Constants.ALLY_TEAM);
                for (RobotInfo robot2 : nearbyAllies) {
                    if (robot2.getType() == RobotType.POLITICIAN)
                        numNearbyPoliticians++;
                }
                if (numNearbyPoliticians >= 2)
                    continue;
                totx += loc.x;
                toty += loc.y;
                tot++;
            }
        }
        if (tot == 0) {
            return false;
        }
        MapLocation desired = new MapLocation(totx / tot, toty / tot);
        return Util.tryMove(desired);
    }
}
