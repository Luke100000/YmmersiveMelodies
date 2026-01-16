package net.conczin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.world.PlaySoundEvent3D;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.gui.MidiDebugConsoleGui;
import net.conczin.utils.KeyboardInterceptor;
import net.conczin.utils.Utils;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MelodyPlaybackInteraction extends SimpleInteraction {
    public static final BuilderCodec<MelodyPlaybackInteraction> CODEC = BuilderCodec.builder(
            MelodyPlaybackInteraction.class, MelodyPlaybackInteraction::new, SimpleInteraction.CODEC)
            .documentation("Plays back a melody.")
            .appendInherited(
                    new KeyedCodec<>("Instrument", Codec.STRING),
                    (o, v) -> o.instrument = v,
                    o -> o.instrument,
                    (o, p) -> o.instrument = p.instrument)
            .add()
            .build();

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final int[] LENGTHS = { 125, 250, 375, 500, 625, 750, 875, 1000, 1250, 1500, 1750, 2000, 2500, 3000,
            4000 };

    private String instrument;

    private int findClosestLength(int length) {
        int closest = LENGTHS[0];
        for (int l : LENGTHS) {
            if (Math.abs(length - l) < Math.abs(length - closest)) {
                closest = l;
            }
        }
        return closest;
    }

    // Checks if the player is sitting on a Tavern Chair by checking nearby entities
    private boolean isSittingOnChair(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        TransformComponent component = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (component == null)
            return false;
        Vector3d position = component.getPosition();

        SpatialResource<Ref<EntityStore>, EntityStore> spatialResource = store
                .getResource(EntityModule.get().getEntitySpatialResourceType());
        if (spatialResource == null)
            return false;

        List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
        spatialResource.getSpatialStructure().collect(position, 2.5, nearby); // Check within 2.5 units

        for (Ref<EntityStore> entityRef : nearby) {
            if (entityRef.equals(playerRef))
                continue;

            // Spatial check for nearby entities (assuming chairs)
            TransformComponent entityTransform = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (entityTransform != null) {
                double distSq = entityTransform.getPosition().distanceSquaredTo(position);
                if (distSq < 2.5) { // Relaxed threshold for sneaking/standing
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void tick0(boolean firstRun, float time, InteractionType type, @Nonnull InteractionContext context,
            CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid())
            return;
        Store<EntityStore> store = ref.getStore();

        // Get position
        TransformComponent component = store.getComponent(ref, TransformComponent.getComponentType());
        if (component == null)
            return;
        Vector3d position = component.getPosition();

        // Get style item
        ItemStack itemInHand = context.getHeldItem();
        if (itemInHand == null)
            return;

        // Check if item is Ymmersive Melody instrument
        String itemId = itemInHand.getItemId();
        boolean isInstrument = itemId != null && itemId.startsWith("Ymmersive_Melodies_") &&
                (itemId.contains("Piano") || itemId.contains("Flute") ||
                        itemId.contains("Lute") || itemId.contains("Trumpet") ||
                        itemId.contains("Bagpipe") || itemId.contains("Vielle") ||
                        itemId.contains("Handpan") || itemId.contains("Didgeridoo") ||
                        itemId.contains("Tiny_Drum"));

        UUID playerId = Utils.getUUID(ref);

        // --- CONSOLE OPEN HANDLER (Fix for F5) ---
        // If player is Sneaking and Interacting with instrument
        boolean isSneaking = false;
        MovementStatesComponent movementStatesComponent = store.getComponent(ref,
                MovementStatesComponent.getComponentType());
        if (movementStatesComponent != null) {
            isSneaking = movementStatesComponent.getMovementStates().crouching;
        }

        // Open Console logic: Holding Instrument + Sneaking + Right Click (First Run)
        if (isInstrument && isSneaking && firstRun) {
            // Check if sitting (or sneaking nearby chair)
            boolean nearbyChair = isSittingOnChair(ref, store);
            if (nearbyChair) {
                // If close to a chair and sneaking, we can force "sitting" state logic if
                // needed
            }

            // Open the new Performance GUI to capture input
            Utils.setPage(ref, store, (playerRef, lifetime) -> new net.conczin.gui.MidiPerformanceGui(playerRef));
        }

        // Sitting State Logic (Legacy + MovementStates + Spatial override if sneaking)
        boolean isSitting = Utils.hasMidiChair(ref);
        if (movementStatesComponent != null && movementStatesComponent.getMovementStates().sitting) {
            isSitting = true;
        }
        // Force sitting if sneaking and physically close to a chair
        if (!isSitting && isSneaking && isSittingOnChair(ref, store)) {
            isSitting = true;
        }

        Utils.setSittingOnTavernChair(ref, isSitting);

        // F5 Handler registration (kept for legacy/future support)
        if (isInstrument) {
            KeyboardInterceptor.registerF5Handler(playerId, ref2 -> {
                Utils.setPage(ref2, store, (playerRef, lifetime) -> new MidiDebugConsoleGui(playerRef));
                return true;
            });
        } else {
            KeyboardInterceptor.unregisterF5Handler(playerId);
            Utils.setSittingOnTavernChair(ref, false);
            return;
        }

        // Check for Tavern Chair (Stored state OR Spatial check)
        boolean storedState = Utils.hasMidiChair(ref);
        boolean spatialCheck = isSittingOnChair(ref, store);
        boolean hasMidiChair = storedState || spatialCheck;

        if (spatialCheck && !storedState) {
            Utils.setSittingOnTavernChair(ref, true);
        } else if (!spatialCheck && storedState) {
            // Maybe clear state if not near chair? Optional.
        }

        // MidiToQwerty Mode Logic
        if (hasMidiChair) {
            MidiToQwertyState state = itemInHand.getFromMetadataOrDefault("MidiToQwertyState", MidiToQwertyState.CODEC);
            StringWrapper instrumentWrapper = new StringWrapper();
            // Extract instrument name from ID (e.g. Ymmersive_Melodies_Piano -> Piano)
            if (itemId != null && itemId.startsWith("Ymmersive_Melodies_")) {
                instrumentWrapper.value = itemId.replace("Ymmersive_Melodies_", "");
            }

            if (!state.active) {
                state.active = true;
                state.active = true;

                MidiInputService.getInstance().initialize(); // Ensure service is running

                MidiInputService.getInstance().registerListener(playerId, midiNote -> {
                    // Direct MIDI to Sound (Bypassing QWERTY)
                    // We schedule this to run on the main thread via a queue or direct executor if
                    // context is safe.
                    // Since bufferNote buffers anyway, we can call it, but we need to watch out for
                    // Store access.
                    // For now, let's use the QWERTY mapping as a fallback if the user wants to
                    // Type,
                    // but for AUDIO, we want direct.

                    // Actually, let's just trigger the bufferNote directly.
                    // WARNING: This runs on MIDI Thread. Store access in bufferNote is UNSAFE.
                    // We will use a ConcurrentQueue to pass this to tick0.
                    pendingMidiNotes.computeIfAbsent(playerId, k -> new java.util.concurrent.ConcurrentLinkedQueue<>())
                            .add(midiNote);
                });

                KeyboardInterceptor.enableInterception(playerId, keyCode -> {
                    int midiNote = MidiToQwertyMapping.findMidiNoteForKey(keyCode);
                    if (midiNote != -1) {
                        playMidiNote(store, ref, instrumentWrapper.value, midiNote, 100);
                    }
                });

                KeyboardInterceptor.registerF5Handler(playerId, ref2 -> {
                    Utils.setPage(ref2, store, (playerRef, lifetime) -> new MidiDebugConsoleGui(playerRef));
                    return true;
                });

                ItemStack newItemInHand = itemInHand.withMetadata("MidiToQwertyState", MidiToQwertyState.CODEC, state);
                ItemContainer container = context.getHeldItemContainer();
                if (container != null) {
                    container.replaceItemStackInSlot(context.getHeldItemSlot(), itemInHand, newItemInHand);
                }
            }
            return;
        } else {
            MidiToQwertyState state = itemInHand.getFromMetadataOrDefault("MidiToQwertyState", MidiToQwertyState.CODEC);
            if (state.active) {
                state.active = false;
                MidiInputService.getInstance().unregisterListener(playerId);
                KeyboardInterceptor.disableInterception(playerId);
                KeyboardInterceptor.unregisterF5Handler(playerId);

                ItemStack newItemInHand = itemInHand.withMetadata("MidiToQwertyState", MidiToQwertyState.CODEC, state);
                ItemContainer container = context.getHeldItemContainer();
                if (container != null) {
                    container.replaceItemStackInSlot(context.getHeldItemSlot(), itemInHand, newItemInHand);
                }
            }
            Utils.setSittingOnTavernChair(ref, false);
        }

        // --- PROCESS PENDING MIDI NOTES (Main Thread Safe) ---
        java.util.concurrent.ConcurrentLinkedQueue<Integer> queue = pendingMidiNotes.get(playerId);
        if (queue != null && !queue.isEmpty()) {
            Integer note;
            while ((note = queue.poll()) != null) {
                // Determine Instrument (same logic as above or use held item)
                String instrumentName = "Piano";
                if (itemId != null && itemId.startsWith("Ymmersive_Melodies_")) {
                    instrumentName = itemId.replace("Ymmersive_Melodies_", "");
                }
                bufferNote(store, ref, instrumentName, note, 100);
            }
        }

        // Normal behavior: Play melody
        MelodyProgress progress = itemInHand.getFromMetadataOrDefault("MelodyProgress", MelodyProgress.CODEC);
        if (progress.melody.isEmpty())
            return;

        // Sync logic
        long buffer = 150L;
        Instant timeResource = store.getResource(TimeResource.getResourceType()).getNow();
        long timeMs = timeResource.getEpochSecond() * 1000L + timeResource.getNano() / 1_000_000L;
        long delta = Math.min(timeMs - progress.worldTime, buffer);
        if (delta <= 0)
            return;

        Melody melody;
        if (progress.melody.contains(":")) {
            YmmersiveMelodiesRegistry resource = store.getResource(YmmersiveMelodiesRegistry.getResourceType());
            String[] split = progress.melody.split(":", 2);
            melody = resource.get(UUID.fromString(split[0]), split[1]);
        } else {
            MelodyAsset asset = MelodyAsset.getAssetStore().getAssetMap().getAsset(progress.melody);
            if (asset == null)
                return;
            melody = asset.getMelody();
        }

        if (melody == null)
            return;

        for (Melody.Track track : melody.tracks()) {
            for (Melody.Note note : track.notes()) {
                if (note.time() >= progress.time && note.time() < progress.time + delta) {
                    long delay = note.time() - (progress.time + delta) + buffer;
                    if (delay <= 0)
                        continue;

                    float volume = note.velocity() / 64.0f;
                    float pitch = (float) Math.pow(2, (note.note() - 24) / 12.0);
                    int octave = 1;
                    while (octave < 8 && pitch > 4.0 / 3.0) {
                        pitch /= 2;
                        octave++;
                    }

                    float factor = 0.5f;
                    float adjustedVolume = (float) (volume / Math.sqrt(pitch * Math.pow(2, octave - 4)));
                    volume = volume * (1.0f - factor) + adjustedVolume * factor;

                    int length = findClosestLength(note.length());
                    int soundEventIndexNote = SoundEvent.getAssetMap()
                            .getIndex("SFX_Ymmersive_Melodies_%s_C%s_%sms".formatted(instrument, octave, length));

                    playSoundEvent3d(soundEventIndexNote, volume, pitch, SoundCategory.SFX, position, store, delay);
                }
            }
        }

        progress.worldTime = timeMs;
        progress.time += delta;
        ItemStack newItemInHand = itemInHand.withMetadata("MelodyProgress", MelodyProgress.CODEC, progress);
        ItemContainer container = context.getHeldItemContainer();
        if (container != null) {
            container.replaceItemStackInSlot(context.getHeldItemSlot(), itemInHand, newItemInHand);
        }
    }

    public static void playSoundEvent3d(int soundEventIndex, float volume, float pitch, SoundCategory soundCategory,
            Vector3d position, ComponentAccessor<EntityStore> componentAccessor, long delay) {
        SoundEvent soundevent = SoundEvent.getAssetMap().getAsset(soundEventIndex);
        if (soundevent == null)
            return;
        PlaySoundEvent3D soundEvent = new PlaySoundEvent3D(soundEventIndex, soundCategory,
                new Position(position.x, position.y, position.z), volume, pitch);
        SpatialResource<Ref<EntityStore>, EntityStore> spatialresource = componentAccessor.getResource(
                EntityModule.get().getPlayerSpatialResourceType());
        List<Ref<EntityStore>> list = SpatialResource.getThreadLocalReferenceList();
        spatialresource.getSpatialStructure().collect(position, soundevent.getMaxDistance(), list);
        for (Ref<EntityStore> ref : list) {
            PlayerRef playerref = componentAccessor.getComponent(ref, PlayerRef.getComponentType());
            assert playerref != null;
            executor.schedule(() -> playerref.getPacketHandler().write(soundEvent), delay, TimeUnit.MILLISECONDS);
        }
    }

    public static void playMidiNote(Store<EntityStore> store, Ref<EntityStore> playerRef, String instrument,
            int midiNote, int velocity) {
        TransformComponent component = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (component == null)
            return;
        Vector3d position = component.getPosition();

        float volume = velocity / 127.0f; // Standard MIDI velocity
        float pitch = (float) Math.pow(2, (midiNote - 24) / 12.0);
        int octave = 1;

        // Adjust octave and pitch to fit samples
        while (octave < 8 && pitch > 4.0 / 3.0) {
            pitch /= 2;
            octave++;
        }

        // Volume adjustment logic matching tick0
        float factor = 0.5f;
        float adjustedVolume = (float) (volume / Math.sqrt(pitch * Math.pow(2, octave - 4)));
        volume = volume * (1.0f - factor) + adjustedVolume * factor;

        // Use a default length for single notes (e.g., 500ms or 1000ms)
        // Since we don't have note-off events easily, we pick a sustain.
        int length = 1000;

        int soundEventIndexNote = SoundEvent.getAssetMap()
                .getIndex("SFX_Ymmersive_Melodies_%s_C%s_%sms".formatted(instrument, octave, length));

        playSoundEvent3d(soundEventIndexNote, volume, pitch, SoundCategory.SFX, position, store, 0);
    }

    // --- CHORD BUFFER SYSTEM ---
    // --- CHORD BUFFER SYSTEM ---
    private static final long BUFFER_WINDOW_MS = 80;
    private static final java.util.Map<UUID, List<BufferedNote>> noteBuffers = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<UUID, java.util.concurrent.ScheduledFuture<?>> bufferTimers = new java.util.concurrent.ConcurrentHashMap<>();
    // Queue for cross-thread MIDI events (MIDI Thread -> Main Thread)
    private static final java.util.Map<UUID, java.util.concurrent.ConcurrentLinkedQueue<Integer>> pendingMidiNotes = new java.util.concurrent.ConcurrentHashMap<>();

    public record BufferedNote(String instrument, int midiNote, int velocity) {
    }

    public static void enqueueMidiNote(UUID playerId, int midiNote) {
        pendingMidiNotes.computeIfAbsent(playerId, k -> new java.util.concurrent.ConcurrentLinkedQueue<>())
                .add(midiNote);
    }

    public static void bufferNote(Store<EntityStore> store, Ref<EntityStore> playerRef, String instrument, int midiNote,
            int velocity) {
        UUID playerId = Utils.getUUID(playerRef);

        // Add note to buffer
        noteBuffers.computeIfAbsent(playerId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(new BufferedNote(instrument, midiNote, velocity));

        // Schedule timer if not running
        bufferTimers.computeIfAbsent(playerId, k -> {
            // CAPTURE CONTEXT ON MAIN THREAD
            // 1. Position
            TransformComponent component = store.getComponent(playerRef, TransformComponent.getComponentType());
            final Vector3d position = (component != null) ? component.getPosition() : new Vector3d(0, 0, 0);

            // 2. Listeners (Spatial Query)
            // We use a safe estimate radius (e.g. 50 blocks) since we don't know exact note
            // sound yet
            List<PlayerRef> recipients = new java.util.ArrayList<>();
            SpatialResource<Ref<EntityStore>, EntityStore> spatialresource = store.getResource(
                    EntityModule.get().getPlayerSpatialResourceType());
            if (spatialresource != null) {
                List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
                spatialresource.getSpatialStructure().collect(position, 50.0, nearby);
                for (Ref<EntityStore> r : nearby) {
                    PlayerRef pr = store.getComponent(r, PlayerRef.getComponentType());
                    if (pr != null)
                        recipients.add(pr);
                }
            }

            return executor.schedule(() -> {
                try {
                    // Pass CAPTURED context
                    flushBuffer(playerId, position, recipients);
                } finally {
                    bufferTimers.remove(playerId);
                }
            }, BUFFER_WINDOW_MS, TimeUnit.MILLISECONDS);
        });
    }

    private static void flushBuffer(UUID playerId, Vector3d position, List<PlayerRef> recipients) {
        List<BufferedNote> notes = noteBuffers.remove(playerId);
        if (notes == null || notes.isEmpty())
            return;

        // Play all buffered notes using CAPTURED context
        for (BufferedNote note : notes) {
            playMidiNoteDirect(position, recipients, note.instrument(), note.midiNote(), note.velocity());
        }
    }

    // Direct version avoiding Store lookup
    private static void playMidiNoteDirect(Vector3d position, List<PlayerRef> recipients, String instrument,
            int midiNote, int velocity) {
        float volume = velocity / 127.0f; // Standard MIDI velocity
        float pitch = (float) Math.pow(2, (midiNote - 24) / 12.0);
        int octave = 1;
        // Adjust octave and pitch to fit samples
        while (octave < 8 && pitch > 4.0 / 3.0) {
            pitch /= 2;
            octave++;
        }

        // Volume adjustment logic matching tick0
        float factor = 0.5f;
        float adjustedVolume = (float) (volume / Math.sqrt(pitch * Math.pow(2, octave - 4)));
        volume = volume * (1.0f - factor) + adjustedVolume * factor;

        // Use a default length for single notes
        int length = 1000;
        int soundEventIndexNote = SoundEvent.getAssetMap()
                .getIndex("SFX_Ymmersive_Melodies_%s_C%s_%sms".formatted(instrument, octave, length));

        SoundEvent soundevent = SoundEvent.getAssetMap().getAsset(soundEventIndexNote);
        if (soundevent == null)
            return;

        // Create Packet
        PlaySoundEvent3D soundEventPacket = new PlaySoundEvent3D(soundEventIndexNote, SoundCategory.SFX,
                new Position(position.x, position.y, position.z), volume, pitch);

        // Broadcast to Pre-Calculated Recipients
        for (PlayerRef playerref : recipients) {
            // Safe to write packet from executor
            playerref.getPacketHandler().write(soundEventPacket);
        }
    }

    // Helper wrapper to pass string effectively in lambda
    private static class StringWrapper {
        String value;
    }
}
