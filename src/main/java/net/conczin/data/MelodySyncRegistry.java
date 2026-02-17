package net.conczin.data;

import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MelodySyncRegistry {
    private static final long STALE_THRESHOLD_MS = 500L;
    private static final double SYNC_RANGE = 40.0;
    private static final double SYNC_RANGE_SQ = SYNC_RANGE * SYNC_RANGE;

    private static final ConcurrentHashMap<String, List<SyncSession>> sessions = new ConcurrentHashMap<>();

    private MelodySyncRegistry() {
    }

    public static long getOrCreateAnchor(UUID playerId, String melodyId, Vector3d position, long currentWorldTime, long melodyDurationMs) {
        SyncSession[] matched = new SyncSession[1];
        sessions.compute(melodyId, (key, existing) -> {
            if (existing == null) {
                existing = new ArrayList<>();
            }

            // Prune stale positions from all sessions; remove empty sessions
            existing.removeIf(session -> {
                session.pruneStalePositions(currentWorldTime, STALE_THRESHOLD_MS);
                return session.playerPositions.isEmpty();
            });

            // Find session with closest active player within range
            SyncSession closestSession = null;
            double closestDistSq = Double.MAX_VALUE;
            for (SyncSession session : existing) {
                // Skip expired sessions so late joiners get a fresh anchor
                if (currentWorldTime - session.startWorldTime > melodyDurationMs) continue;

                double distSq = session.closestPlayerDistSq(position);
                if (distSq <= SYNC_RANGE_SQ && distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closestSession = session;
                }
            }
            if (closestSession != null) {
                closestSession.playerPositions.add(new ActivePosition(playerId, position, currentWorldTime));
                matched[0] = closestSession;
                return existing;
            }

            // No nearby session found — create a new one
            SyncSession newSession = new SyncSession(playerId, currentWorldTime, position, currentWorldTime);
            existing.add(newSession);
            matched[0] = newSession;
            return existing;
        });
        return matched[0].startWorldTime;
    }

    public static void removePlayer(UUID playerId, String melodyId) {
        sessions.compute(melodyId, (key, existing) -> {
            if (existing == null) return null;
            for (SyncSession session : existing) {
                session.playerPositions.removeIf(p -> p.playerId().equals(playerId));
            }
            existing.removeIf(session -> session.playerPositions.isEmpty());
            return existing.isEmpty() ? null : existing;
        });
    }

    public static void keepAlive(UUID playerId, String melodyId, long startWorldTime, Vector3d position, long currentWorldTime) {
        sessions.compute(melodyId, (key, existing) -> {
            if (existing == null) {
                existing = new ArrayList<>();
            }

            // Find session by matching anchor
            SyncSession target = null;
            for (SyncSession session : existing) {
                if (session.startWorldTime == startWorldTime) {
                    target = session;
                    break;
                }
            }

            if (target != null) {
                boolean found = false;
                for (int i = 0; i < target.playerPositions.size(); i++) {
                    if (target.playerPositions.get(i).playerId().equals(playerId)) {
                        target.playerPositions.set(i, new ActivePosition(playerId, position, currentWorldTime));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    target.playerPositions.add(new ActivePosition(playerId, position, currentWorldTime));
                }
            } else {
                // Session was pruned — re-create so this player is discoverable for sync
                existing.add(new SyncSession(playerId, startWorldTime, position, currentWorldTime));
            }

            // Prune stale positions; remove empty sessions
            existing.removeIf(session -> {
                session.pruneStalePositions(currentWorldTime, STALE_THRESHOLD_MS);
                return session.playerPositions.isEmpty();
            });

            return existing.isEmpty() ? null : existing;
        });
    }

    private record ActivePosition(UUID playerId, double x, double y, double z, long lastActiveTime) {
        ActivePosition(UUID playerId, Vector3d position, long time) {
            this(playerId, position.x, position.y, position.z, time);
        }
    }

    private static final class SyncSession {
        final long startWorldTime;
        final List<ActivePosition> playerPositions = new ArrayList<>();

        SyncSession(UUID playerId, long startWorldTime, Vector3d position, long currentTime) {
            this.startWorldTime = startWorldTime;
            this.playerPositions.add(new ActivePosition(playerId, position, currentTime));
        }

        void pruneStalePositions(long currentTime, long thresholdMs) {
            playerPositions.removeIf(p -> currentTime - p.lastActiveTime() > thresholdMs);
        }

        double closestPlayerDistSq(Vector3d position) {
            double closestDistSq = Double.MAX_VALUE;
            for (ActivePosition p : playerPositions) {
                double dx = position.x - p.x();
                double dy = position.y - p.y();
                double dz = position.z - p.z();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                }
            }
            return closestDistSq;
        }
    }
}
