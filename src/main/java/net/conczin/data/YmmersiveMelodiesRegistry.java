package net.conczin.data;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.YmmersiveMelodies;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YmmersiveMelodiesRegistry implements Resource<EntityStore> {
    public static final BuilderCodec<YmmersiveMelodiesRegistry> CODEC = BuilderCodec.builder(
                    YmmersiveMelodiesRegistry.class, YmmersiveMelodiesRegistry::new
            )
            .append(
                    new KeyedCodec<>(
                            "Melodies", // TODO Unmodifiable map codec by default!
                            new MapCodec<>(
                                    new MapCodec<>(Melody.CODEC, HashMap::new),
                                    HashMap::new
                            ),
                            true
                    ),
                    (o, map) -> {
                        if (map != null) { // TODO KeyMapCodec
                            map.forEach((k, v) ->
                                    o.melodies.put(UUID.fromString(k), new HashMap<>(v))
                            );
                        }
                    },
                    o -> {
                        Map<String, Map<String, Melody>> out = new HashMap<>();
                        o.melodies.forEach((uuid, inner) ->
                                out.put(uuid.toString(), inner)
                        );
                        return out;
                    }
            )
            .add()
            .build();


    private Map<UUID, Map<String, Melody>> melodies = new HashMap<>();

    public static ResourceType<EntityStore, YmmersiveMelodiesRegistry> getResourceType() {
        return YmmersiveMelodies.getInstance().getYmmersiveMelodiesRegistry();
    }

    public YmmersiveMelodiesRegistry() {
    }

    public YmmersiveMelodiesRegistry(@Nonnull YmmersiveMelodiesRegistry other) {
        this.melodies = other.melodies;
    }

    public void add(UUID uuid, Melody melody) {
        melodies.computeIfAbsent(uuid, _ -> new HashMap<>()).put(melody.name(), melody);
    }

    public void delete(UUID uuid, String name) {
        melodies.get(uuid).remove(name);
    }

    public Melody get(UUID uuid, String name) {
        return get(uuid).get(name);
    }

    public Map<String, Melody> get(UUID uuid) {
        return melodies.getOrDefault(uuid, new HashMap<>());
    }

    public Map<UUID, Map<String, Melody>> getMelodies() {
        return melodies;
    }

    @Nonnull
    @Override
    public Resource<EntityStore> clone() {
        return new YmmersiveMelodiesRegistry(this);
    }
}
