package experiment2.util;

import battlecode.common.MapLocation;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EnlightenmentCenterList {
    public EnlightenmentCenterListNode head = null;
    private EnlightenmentCenterListNode curptr = null;
    private int size;
    public void addOrUpdate(MapLocation location, int conviction) {
        EnlightenmentCenterListNode current = head;
        while (current != null) {
            if (current.location.equals(location)) {
                current.lastKnownConviction = conviction;
                return;
            }
            current = current.next;
        }
        // doesn't contain
        head = new EnlightenmentCenterListNode(location, conviction, head);
        size++;
    }
    public EnlightenmentCenterListNode getNext() {
        if (size == 0) {
            return null;
        }
        if(curptr == null) {
            curptr = head;
            return curptr;
        }
        curptr = curptr.next;
        if(curptr == null) curptr = head;
        return curptr;
    }
    public Optional<MapLocation> getNextLocation() {
        if (size == 0) {
            return Optional.empty();
        }
        if(curptr == null) {
            curptr = head;
            return Optional.of(curptr.location);
        }
        curptr = curptr.next;
        if(curptr == null) curptr = head;
        return Optional.of(curptr.location);
    }
    public Optional<MapLocation> getRandomLocation() {
        if (size == 0) {
            return Optional.empty();
        }
        EnlightenmentCenterListNode current = head;
        int r = (int) (size * Math.random());
        for (int i = r; --i >= 0;) {
            current = current.next;
        }
        return Optional.of(current.location);
    }
    public boolean contains(MapLocation location) {
        EnlightenmentCenterListNode current = head;
        while (current != null) {
            if (current.location.equals(location)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }
    public void removeIf(Predicate<MapLocation> predicate) {
        EnlightenmentCenterListNode prev = null;
        EnlightenmentCenterListNode current = head;
        while (current != null) {
            if (predicate.test(current.location)) {
                if(current == curptr) {
                    curptr = current.next;
                }
                if (prev == null) {
                    head = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
            } else {
                prev = current;
            }
            current = current.next;
        }
    }
    public void forEach(Consumer<MapLocation> consumer) {
        EnlightenmentCenterListNode current = head;
        while (current != null) {
            consumer.accept(current.location);
            current = current.next;
        }
    }
    public int getClosestLocationDistance(MapLocation location, int defaultDistance) {
        return getClosestLocation(location).map(x -> x.distanceSquaredTo(location)).orElse(defaultDistance);
    }
    public int getClosestLocationDistance(int defaultDistance) {
        return getClosestLocation().map(x -> x.distanceSquaredTo(Cache.MY_LOCATION)).orElse(defaultDistance);
    }
    public Optional<MapLocation> getClosestLocation() {
        return getClosestLocation(Cache.MY_LOCATION);
    }
    public Optional<MapLocation> getClosestLocation(MapLocation location) {
        return min(Comparator.comparingInt(x -> x.location.distanceSquaredTo(location))).map(x -> x.location);
    }
    public Optional<EnlightenmentCenterListNode> min(Comparator<EnlightenmentCenterListNode> comparator) {
        if (size == 0) {
            return Optional.empty();
        }
        EnlightenmentCenterListNode min = head;
        EnlightenmentCenterListNode current = head.next;
        while (current != null) {
            if (comparator.compare(current, min) < 0) {
                min = current;
            }
            current = current.next;
        }
        return Optional.of(min);
    }
    public Optional<MapLocation> minLocation(Comparator<MapLocation> comparator) {
        if (size == 0) {
            return Optional.empty();
        }
        MapLocation minLocation = head.location;
        EnlightenmentCenterListNode current = head.next;
        while (current != null) {
            MapLocation location = current.location;
            if (comparator.compare(location, minLocation) < 0) {
                minLocation = location;
            }
            current = current.next;
        }
        return Optional.of(minLocation);
    }
    public boolean isEmpty() {
        return size == 0;
    }
    public static class EnlightenmentCenterListNode {
        public MapLocation location;
        public int lastKnownConviction;
        public EnlightenmentCenterListNode next;
        public EnlightenmentCenterListNode(MapLocation location, int lastKnownConviction, EnlightenmentCenterListNode next) {
            this.location = location;
            this.lastKnownConviction = lastKnownConviction;
            this.next = next;
        }
    }
}
