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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Constants;
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
                    MelodyPlaybackInteraction.class, MelodyPlaybackInteraction::new, SimpleInteraction.CODEC
            )
            .documentation("Plays back a melody.")
            .appendInherited(
                    new KeyedCodec<>("Instrument", Codec.STRING),
                    (o, v) -> o.instrument = v,
                    o -> o.instrument,
                    (o, p) -> o.instrument = p.instrument)
            .add()
            .build();

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final int[] LENGTHS = {125, 250, 375, 500, 625, 750, 875, 1000, 1250, 1500, 1750, 2000, 2500, 3000, 4000};

    public static volatile boolean multiplayerMode = !Constants.SINGLEPLAYER;

    public static void setMultiplayerMode(boolean multiplayer) {
        multiplayerMode = multiplayer;
    }

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

    @Override
    protected void tick0(boolean firstRun, float time, InteractionType type, @Nonnull InteractionContext context, CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();

        // Get position
        TransformComponent component = store.getComponent(ref, TransformComponent.getComponentType());
        if (component == null) return;
        Vector3d position = component.getPosition();

        // Get style item
        ItemStack itemInHand = context.getHeldItem();
        if (itemInHand == null) return;
        MelodyProgress progress = itemInHand.getFromMetadataOrDefault("MelodyProgress", MelodyProgress.CODEC);
        if (progress.melody.isEmpty()) return;

        // This should be the tick rate plus max jitter margin
        long buffer = 150L;

        // Get time
        Instant timeResource = store.getResource(TimeResource.getResourceType()).getNow();
        long timeMs = timeResource.getEpochSecond() * 1000L + timeResource.getNano() / 1_000_000L;

        // Get melody
        Melody melody;
        if (progress.melody.contains(":")) {
            YmmersiveMelodiesRegistry resource = store.getResource(YmmersiveMelodiesRegistry.getResourceType());
            String[] split = progress.melody.split(":", 2);
            melody = resource.get(UUID.fromString(split[0]), split[1]);
        } else {
            MelodyAsset asset = MelodyAsset.getAssetStore().getAssetMap().getAsset(progress.melody);
            if (asset == null) return;
            melody = asset.getMelody();
        }

        if (melody == null) return;

        UUID uuid = Utils.getUUID(ref);

        int duration = melody.duration();

        // In singleplayer, cancel on item change (pause on scroll-away)
        // In multiplayer, keep ticking in background for other players
        this.cancelOnItemChange = !multiplayerMode;

        long prevPlaybackTime;
        long delta;

        if (multiplayerMode) {
            // Sync: initialize shared time anchor on first tick
            if (progress.startWorldTime == 0) {
                progress.startWorldTime = MelodySyncRegistry.getOrCreateAnchor(uuid, progress.melody, position, timeMs, duration);
                progress.worldTime = timeMs;
                progress.time = 0;
                saveProgress(context, itemInHand, progress);
                return;
            }

            // Derive playback position from shared anchor
            long playbackTime = timeMs - progress.startWorldTime;
            prevPlaybackTime = progress.worldTime - progress.startWorldTime;
            delta = Math.min(playbackTime - prevPlaybackTime, buffer);
            if (delta <= 0) return;

            MelodySyncRegistry.keepAlive(uuid, progress.melody, progress.startWorldTime, position, timeMs);

            progress.worldTime = timeMs;
            progress.time = playbackTime;
        } else {
            // Singleplayer: warmup on first tick
            if (progress.worldTime == 0) {
                progress.worldTime = timeMs;
                saveProgress(context, itemInHand, progress);
                return;
            }

            prevPlaybackTime = progress.time;
            delta = Math.min(timeMs - progress.worldTime, buffer);
            if (delta <= 0) return;

            progress.worldTime = timeMs;
            progress.time += delta;
        }

        // Play notes
        for (Melody.Track track : melody.tracks()) {
            // TODO: Track filter
            for (Melody.Note note : track.notes()) {
                if (note.time() >= prevPlaybackTime && note.time() < prevPlaybackTime + delta) {
                    long delay = note.time() - (prevPlaybackTime + delta) + buffer;
                    if (delay <= 0) continue;

                    float volume = note.velocity() / 64.0f;
                    float pitch = (float) Math.pow(2, (note.note() - 24) / 12.0);
                    int octave = 1;
                    while (octave < 8 && pitch > 4.0 / 3.0) {
                        pitch /= 2;
                        octave++;
                    }

                    // Adjust volume based on perceived loudness
                    float factor = 0.5f;
                    float adjustedVolume = (float) (volume / Math.sqrt(pitch * Math.pow(2, octave - 4)));
                    volume = volume * (1.0f - factor) + adjustedVolume * factor;

                    int length = findClosestLength(note.length());
                    int soundEventIndexNote = SoundEvent.getAssetMap().getIndex("SFX_Ymmersive_Melodies_%s_C%s_%sms".formatted(instrument, octave, length));

                    playSoundEvent3d(soundEventIndexNote, volume, pitch, SoundCategory.SFX, position, store, delay);
                }
            }
        }

        // Auto-stop: song finished, clear melody
        if (progress.time > duration) {
            if (multiplayerMode) {
                MelodySyncRegistry.removePlayer(uuid, progress.melody);
            }
            progress.melody = "";
            progress.time = 0;
            progress.startWorldTime = 0;
            progress.worldTime = 0;
        }

        saveProgress(context, itemInHand, progress);
    }

    private static void saveProgress(InteractionContext context, ItemStack itemInHand, MelodyProgress progress) {
        ItemStack newItemInHand = itemInHand.withMetadata("MelodyProgress", MelodyProgress.CODEC, progress);
        ItemContainer container = context.getHeldItemContainer();
        if (container != null) {
            container.replaceItemStackInSlot(context.getHeldItemSlot(), itemInHand, newItemInHand);
        }
    }

    // That's just the inbuilt playsound but with delay
    public static void playSoundEvent3d(int soundEventIndex, float volume, float pitch, SoundCategory soundCategory, Vector3d position, ComponentAccessor<EntityStore> componentAccessor, long delay) {
        SoundEvent soundevent = SoundEvent.getAssetMap().getAsset(soundEventIndex);
        if (soundevent == null) return;
        PlaySoundEvent3D soundEvent = new PlaySoundEvent3D(soundEventIndex, soundCategory, new Position(position.x, position.y, position.z), volume, pitch);
        SpatialResource<Ref<EntityStore>, EntityStore> spatialresource = componentAccessor.getResource(
                EntityModule.get().getPlayerSpatialResourceType()
        );
        List<Ref<EntityStore>> list = SpatialResource.getThreadLocalReferenceList();
        spatialresource.getSpatialStructure().collect(position, soundevent.getMaxDistance(), list);
        for (Ref<EntityStore> ref : list) {
            PlayerRef playerref = componentAccessor.getComponent(ref, PlayerRef.getComponentType());
            assert playerref != null;
            executor.schedule(() -> playerref.getPacketHandler().write(soundEvent), delay, TimeUnit.MILLISECONDS);
        }
    }
}
