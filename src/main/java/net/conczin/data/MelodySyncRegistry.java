package net.conczin.data;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MelodySyncRegistry {
    private static final long STALE_THRESHOLD_MS = 500L;
    private static final double SYNC_RANGE_SQ = 40.0 * 40.0;

    private static final ConcurrentHashMap<String, List<Anchor>> anchors = new ConcurrentHashMap<>();

    private MelodySyncRegistry() {
    }

    public static long getOrCreateAnchor(UUID playerId, String melodyId, Vector3d position, long currentWorldTime, long melodyDurationMs) {
        long[] result = new long[1];
        anchors.compute(melodyId, (key, list) -> {
            if (list == null) list = new ArrayList<>();

            // Single pass: prune stale entries and find closest active anchor
            Anchor closest = null;
            double closestDistSq = Double.MAX_VALUE;
            Iterator<Anchor> it = list.iterator();
            while (it.hasNext()) {
                Anchor a = it.next();
                if (currentWorldTime - a.lastActive > STALE_THRESHOLD_MS) {
                    it.remove();
                    continue;
                }
                if (currentWorldTime - a.startWorldTime > melodyDurationMs) continue;
                double distSq = a.distSq(position);
                if (distSq <= SYNC_RANGE_SQ && distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = a;
                }
            }

            // Sync to nearby player's anchor or start fresh
            long startTime = closest != null ? closest.startWorldTime : currentWorldTime;
            list.add(new Anchor(playerId, startTime, position.x, position.y, position.z, currentWorldTime));
            result[0] = startTime;
            return list;
        });
        return result[0];
    }

    public static void removePlayer(UUID playerId, String melodyId) {
        anchors.computeIfPresent(melodyId, (key, list) -> {
            list.removeIf(a -> a.playerId.equals(playerId));
            return list.isEmpty() ? null : list;
        });
    }

    public static void keepAlive(UUID playerId, String melodyId, long startWorldTime, Vector3d position, long currentWorldTime) {
        anchors.compute(melodyId, (key, list) -> {
            if (list == null) list = new ArrayList<>();
            // Single pass: remove this player's old entry and prune stale anchors
            list.removeIf(a -> a.playerId.equals(playerId) || currentWorldTime - a.lastActive > STALE_THRESHOLD_MS);
            list.add(new Anchor(playerId, startWorldTime, position.x, position.y, position.z, currentWorldTime));
            return list;
        });
    }

    private record Anchor(UUID playerId, long startWorldTime, double x, double y, double z, long lastActive) {
        double distSq(Vector3d pos) {
            double dx = pos.x - x, dy = pos.y - y, dz = pos.z - z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
