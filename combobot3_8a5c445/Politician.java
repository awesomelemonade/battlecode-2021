package combobot3_8a5c445;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import combobot3_8a5c445.util.EnlightenmentCenterList.EnlightenmentCenterListNode;
import combobot3_8a5c445.util.Cache;
import combobot3_8a5c445.util.Constants;
import combobot3_8a5c445.util.LambdaUtil;
import combobot3_8a5c445.util.MapInfo;
import combobot3_8a5c445.util.Pathfinder;
import combobot3_8a5c445.util.UnitCommunication;
import combobot3_8a5c445.util.Util;

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
    private static MapLocation allyECCentroid;
    private static boolean attacking = false;

    private static int currentConviction;
    private static int currentConviction_10; // = currentConviction - 10
    private static double currentEmpowerFactor;

    private static MapLocation targetLoc;
    private static Team targetTeam;

    private static final int[] EMPOWER_RADII = {1,2,4,5,8,9};

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
        if (rc.getRoundNum() % 25 == 0 || currentEmpowerFactor >= 1.1) attacking = true;
        allyECCentroid = MapInfo.getKnownEnlightenmentCenterList(Constants.ALLY_TEAM).getCentroid();
        computeTarget();
    }

    @Override
    public void turn() throws GameActionException {
        if (!rc.isReady()) {
            return;
        }
        preTurn();
        if (currentConviction < 50 && kiteBigPNearNeutral()) {
            return;
        }
        if (currentConviction_10 <= 0) { // useless; best thing to do is chase enemy big ps to absorb damage
            if (chaseBigPs()) {
                return;
            }
            Util.smartExplore();
            return;
        }
        if (tryEmpower()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 255); // cyan
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
        if (chaseWorthwhileEnemy()) {
            Util.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 0); // green
            return;
        }
        if (defendCommunicatedEnemy()) {
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

    public static boolean defendCommunicatedEnemy() {
        MapLocation enemy = UnitCommunication.closestCommunicatedEnemyToKite;
        if (enemy != null) {
            // go towards muckraker
            Pathfinder.execute(enemy);
            return true;
        }
        return false;
    }

    public static boolean kiteBigPNearNeutral() {
        // If we're near a neutral enlightenment center, kite from ally big p
        if (MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL)
                .getClosestLocationDistance(Cache.MY_LOCATION, Integer.MAX_VALUE) <= 9) {
            // kite from closest big p
            return LambdaUtil.arraysStreamMin(Cache.ALLY_ROBOTS,
                    r -> r.getType() == RobotType.POLITICIAN && r.getConviction() >= 50,
                    Comparator.comparingInt(
                            r -> r.getLocation().distanceSquaredTo(Cache.MY_LOCATION))).map(r -> {
                Util.tryKiteFrom(r.getLocation());
                return true;
            }).orElse(false);
        }
        return false;
    }

    public static boolean shouldAttackTargetEC() {
        return attacking || targetTeam == Team.NEUTRAL || rc.getRoundNum() <= 300 ||
                !targetLoc.isWithinDistanceSquared(Cache.MY_LOCATION, 121);
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
                    if (distanceSquared <= 1 || distanceSquared <= 9 && rc.senseNearbyRobots(distanceSquared).length == 1) {
                        // Add up our influence & enemy influence
                        RobotInfo[] allNearbyRobots = rc.senseNearbyRobots(loc, 16, null);
                        int convictionBalance = power; // current robot's conviction
                        boolean hasEnemy = false;
                        for (int i = allNearbyRobots.length; --i >= 0; ) {
                            RobotInfo robot = allNearbyRobots[i];
                            if (robot.getType() == RobotType.POLITICIAN) {
                                if (robot.getTeam() == Constants.ALLY_TEAM) {
                                    convictionBalance += Math.max(0, robot.getConviction() - 10) * currentEmpowerFactor;
                                }
                                if (robot.getTeam() == Constants.ENEMY_TEAM) {
                                    hasEnemy = true;
                                    convictionBalance -= Math.max(0, robot.getConviction() - 10) * rc.getEmpowerFactor(Constants.ENEMY_TEAM, 0);
                                }
                            }
                        }
                        if ((!hasEnemy) && convictionBalance > ecConviction) {
                            rc.empower(distanceSquared);
                            return true;
                        }
                        if (convictionBalance > ecConviction + 20) {
                            rc.empower(distanceSquared);
                            return true;
                        }
                        if (distanceSquared <= 1 && convictionBalance > 0) {
                            RobotInfo[] allyRobots = rc.senseNearbyRobots(loc, 1, Constants.ALLY_TEAM);
                            if ((allyRobots.length + 1) == numNeighborsOpen) {
                                rc.empower(distanceSquared);
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
                    if ((team == Constants.ENEMY_TEAM && distanceSquared <= 1) ||
                            distanceSquared <= 9 && rc.senseNearbyRobots(distanceSquared).length == 1) {
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
                        if (distanceSquared <= 2 && team == Constants.ENEMY_TEAM) {
                            rc.empower(distanceSquared);
                        } else {
                            Pathfinder.execute(loc);
                        }
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

    // Does not handle empowering or chasing after muckrakers/other enemies
    public static boolean tryDefend() throws GameActionException {
        double x = Cache.MY_LOCATION.x;
        double y = Cache.MY_LOCATION.y;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0; ) {
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
            }
            if (ally.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(allyLocation);
                double distance = Math.sqrt(distanceSquared);
                double distanceCubed = distance * distanceSquared;
                // repel force
                // (1 / d^2) * (vec / d) = (c * vec / d^3)
                x -= 2.25 * (allyLocation.x - Cache.MY_LOCATION.x) / distanceCubed;
                y -= 2.25 * (allyLocation.y - Cache.MY_LOCATION.y) / distanceCubed;
            }
        }
        if (MapInfo.mapMinX != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(MapInfo.mapMinX-1, Cache.MY_LOCATION.y);
            double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            double distance = Math.sqrt(distanceSquared);
            double distanceCubed = distance * distanceSquared;
            // repel force
            // (1 / d^2) * (vec / d) = (c * vec / d^3)
            x -= 2.25 * (loc.x - Cache.MY_LOCATION.x) / distanceCubed;
            y -= 2.25 * (loc.y - Cache.MY_LOCATION.y) / distanceCubed;
        }
        if (MapInfo.mapMaxX != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(MapInfo.mapMaxX+1, Cache.MY_LOCATION.y);
            double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            double distance = Math.sqrt(distanceSquared);
            double distanceCubed = distance * distanceSquared;
            // repel force
            // (1 / d^2) * (vec / d) = (c * vec / d^3)
            x -= 2.25 * (loc.x - Cache.MY_LOCATION.x) / distanceCubed;
            y -= 2.25 * (loc.y - Cache.MY_LOCATION.y) / distanceCubed;
        }
        if (MapInfo.mapMinY != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(Cache.MY_LOCATION.x, MapInfo.mapMinY-1);
            double distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(loc);
            double distance = Math.sqrt(distanceSquared);
            double distanceCubed = distance * distanceSquared;
            // repel force
            // (1 / d^2) * (vec / d) = (c * vec / d^3)
            x -= 2.25 * (loc.x - Cache.MY_LOCATION.x) / distanceCubed;
            y -= 2.25 * (loc.y - Cache.MY_LOCATION.y) / distanceCubed;
        }
        if (MapInfo.mapMaxY != MapInfo.MAP_UNKNOWN_EDGE) {
            MapLocation loc = new MapLocation(Cache.MY_LOCATION.x, MapInfo.mapMaxY+1);
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


    // 1. closest neutral we can claim solo
    // 2. closest enemy ec, if we are near one (distSquared <= 400)
    // 3. closest ec to ally ec centroid
    // smaller value is better
    private static Comparator<EnlightenmentCenterListNode> compareECs =
            Comparator.<EnlightenmentCenterListNode>comparingInt(ec -> {
                if (ec.team != Team.NEUTRAL) {
                    return Integer.MAX_VALUE;
                }
                // Closest neutral if we can claim solo
                int conviction = ec.lastKnownConviction;
                if (conviction == -1) {
                    conviction = 500;
                }
                return currentConviction_10 > conviction ? Cache.MY_LOCATION.distanceSquaredTo(ec.location) : Integer.MAX_VALUE;
            }).thenComparingInt(ec -> {
                if (ec.team != Constants.ENEMY_TEAM) {
                    return Integer.MAX_VALUE;
                }
                int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(ec.location);
                return distanceSquared <= 400 ? distanceSquared : Integer.MAX_VALUE;
            }).thenComparingInt(ec -> {
                return allyECCentroid.distanceSquaredTo(ec.location);
            }).thenComparingInt(ec -> {
                MapLocation location = ec.location;
                return location.x * 100000 + location.y;
            });
    public static void computeTarget() {
        EnlightenmentCenterListNode bestNeutralEC = MapInfo.getKnownEnlightenmentCenterList(Team.NEUTRAL).min(compareECs).orElse(null);
        EnlightenmentCenterListNode bestEnemyEC = MapInfo.getKnownEnlightenmentCenterList(Constants.ENEMY_TEAM).min(compareECs).orElse(null);
        EnlightenmentCenterListNode bestEC = bestNeutralEC;
        if (bestEnemyEC != null && (bestEC == null || compareECs.compare(bestEnemyEC, bestEC) < 0)) {
            bestEC = bestEnemyEC;
        }
        if (bestEC == null) {
            targetLoc = null;
            targetTeam = null;
        } else {
            targetLoc = bestEC.location;
            targetTeam = bestEC.team;
        }
    }

    public static boolean goToECs() {
        if (targetLoc == null) return false;
        if (!shouldAttackTargetEC()) return false;
        return Util.tryMove(targetLoc);
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
        if (targetLoc == null)
            return false;
        if (!shouldAttackTargetEC()) {
            return false;
        }
        return tryEmpowerAtEC(targetLoc, targetTeam);
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
                rc.getRobotCount() >= 10*(1500-rc.getRoundNum()) && rc.getTeamVotes() < 751) {
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
        if(numKills == 0 && mecConviction + pConviction < Math.min(3, currentConviction_10)) return false;
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
        for (int r : EMPOWER_RADII) {
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

    private static boolean chaseBigPs() throws GameActionException {
        MapLocation bestDest = null;
        int bestConvictionAbsorbed = 0;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if(robot.getType() == RobotType.POLITICIAN && robot.getConviction() > 10) {
                int numNearbyPoliticians = 0;
                MapLocation loc = robot.location;
                RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, 5, Constants.ALLY_TEAM);
                for (RobotInfo robot2 : nearbyAllies) {
                    if (robot2.getType() == RobotType.POLITICIAN && robot2.getConviction() <= 10)
                        numNearbyPoliticians++;
                }
                int convictionAbsorbed = (robot.getConviction() - 10) / (numNearbyPoliticians + 2);
                if(convictionAbsorbed > bestConvictionAbsorbed) {
                    bestConvictionAbsorbed = convictionAbsorbed;
                    bestDest = loc;
                }
            }
        }
        if(bestDest == null) return false;
        if(bestDest.distanceSquaredTo(Cache.MY_LOCATION) <= 1) return true;
        return Util.tryMove(bestDest);
    }
}
