package combobot2.util;

import battlecode.common.MapLocation;
import battlecode.common.Team;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EnlightenmentCenterList {
    private EnlightenmentCenterListNode head = null;
    private int size;
    private Team team;
    private EnlightenmentCenterListNode currentPointer = null;
    public EnlightenmentCenterList(Team team) {
        this.team = team;
    }
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
        head = new EnlightenmentCenterListNode(location, conviction, team, head);
        size++;
    }
    public Optional<EnlightenmentCenterListNode> getNext() {
        if (size == 0) {
            return Optional.empty();
        }
        if (currentPointer == null) {
            currentPointer = head;
            return Optional.of(currentPointer);
        } else {
            currentPointer = currentPointer.next;
            if (currentPointer == null) {
                currentPointer = head;
            }
            return Optional.of(currentPointer);
        }
    }
    public Optional<EnlightenmentCenterListNode> getRandom() {
        if (size == 0) {
            return Optional.empty();
        }
        EnlightenmentCenterListNode current = head;
        for (int i = (int) (size * Math.random()); --i >= 0;) {
            current = current.next;
        }
        return Optional.of(current);
    }
    public Optional<MapLocation> getRandomLocation() {
        if (size == 0) {
            return Optional.empty();
        }
        EnlightenmentCenterListNode current = head;
        for (int i = (int) (size * Math.random()); --i >= 0;) {
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
    public void forEach(Consumer<EnlightenmentCenterListNode> consumer) {
        EnlightenmentCenterListNode current = head;
        while (current != null) {
            consumer.accept(current);
            current = current.next;
        }
    }
    public void forEachLocation(Consumer<MapLocation> consumer) {
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
    public Optional<EnlightenmentCenterListNode> getClosest() {
        return min(Comparator.comparingInt(ec -> ec.location.distanceSquaredTo(Cache.MY_LOCATION)));
    }
    public MapLocation getCentroid() {
        int totx = 0;
        int toty = 0;
        int tot = 0;
        EnlightenmentCenterList.EnlightenmentCenterListNode current = head;
        while (current != null) {
            totx += current.location.x;
            toty += current.location.y;
            tot++;
            current = current.next;
        }
        if(tot == 0) return Cache.MY_LOCATION;
        return new MapLocation(totx/tot, toty/tot);
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
        public Team team;
        private EnlightenmentCenterListNode next;
        public EnlightenmentCenterListNode(MapLocation location, int lastKnownConviction, Team team, EnlightenmentCenterListNode next) {
            this.location = location;
            this.lastKnownConviction = lastKnownConviction;
            this.team = team;
            this.next = next;
        }
    }
}
