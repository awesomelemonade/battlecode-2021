package latticebot.util;

import battlecode.common.MapLocation;

import java.util.function.Predicate;

public class MapLocationList {
    private MapLocationListNode head = null;
    private int size;
    public void add(MapLocation location) {
        head = new MapLocationListNode(location, head);
        size++;
    }
    public MapLocation getRandomLocation() {
        if (size == 0) {
            return null;
        }
        MapLocationListNode current = head;
        for (int i = (int) (size * Math.random()); --i >= 0;) {
            current = current.next;
        }
        return current.location;
    }
    public boolean contains(MapLocation location) {
        MapLocationListNode current = head;
        while (current != null) {
            if (current.location.equals(location)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }
    public void removeIf(Predicate<MapLocation> predicate) {
        MapLocationListNode prev = null;
        MapLocationListNode current = head;
        while (current != null) {
            if (predicate.test(current.location)) {
                if (prev == null) {
                    head = current.next;
                } else {
                    prev.next = current.next;
                }
            } else {
                prev = current;
            }
            current = current.next;
        }
    }
    static class MapLocationListNode {
        MapLocation location;
        MapLocationListNode next;
        public MapLocationListNode(MapLocation location, MapLocationListNode next) {
            this.location = location;
            this.next = next;
        }
    }
}
