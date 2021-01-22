package experiment2.util;

import battlecode.common.MapLocation;

public class ECCaptureTracker {
    private ECCaptureTrackerNode head = null;
    public void addOrUpdate(MapLocation location, int round) {
        ECCaptureTrackerNode current = head;
        while (current != null) {
            if (current.location.equals(location)) {
                current.roundSent = round;
                return;
            }
            current = current.next;
        }
        // doesn't contain
        head = new ECCaptureTrackerNode(location, round, head);
    }
    public int getRoundSent(MapLocation location) {
        ECCaptureTrackerNode current = head;
        while (current != null) {
            if (current.location.equals(location)) {
                return current.roundSent;
            }
            current = current.next;
        }
        return -1;
    }
    public static class ECCaptureTrackerNode {
        public MapLocation location;
        public int roundSent;
        private ECCaptureTrackerNode next;
        public ECCaptureTrackerNode(MapLocation location, int roundSent, ECCaptureTrackerNode next) {
            this.location = location;
            this.roundSent = roundSent;
            this.next = next;
        }
    }
}