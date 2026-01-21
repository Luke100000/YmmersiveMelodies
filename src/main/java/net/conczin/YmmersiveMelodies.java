package net.conczin;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.data.DebugConsoleInteraction;
import net.conczin.data.MelodyAsset;
import net.conczin.data.MelodyPlaybackInteraction;
import net.conczin.data.MidiChairInteraction;
import net.conczin.data.MidiInputService;
import net.conczin.data.MidiToQwertyState;
import net.conczin.data.YmmersiveMelodiesRegistry;
import net.conczin.gui.MelodySelectionSupplier;

import javax.annotation.Nonnull;


public class YmmersiveMelodies extends JavaPlugin {
    private static YmmersiveMelodies instance;

    private ResourceType<EntityStore, YmmersiveMelodiesRegistry> ymmersiveMelodiesRegistry;


    public YmmersiveMelodies(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        this.ymmersiveMelodiesRegistry = this.getEntityStoreRegistry().registerResource(
                YmmersiveMelodiesRegistry.class,
                "YmmersiveMelodiesRegistry",
                YmmersiveMelodiesRegistry.CODEC
        );

        this.getCodecRegistry(Interaction.CODEC).register(
                "Ymmersive_Melodies_Melody_Playback",
                MelodyPlaybackInteraction.class,
                MelodyPlaybackInteraction.CODEC
        );

        this.getCodecRegistry(Interaction.CODEC).register(
                "Ymmersive_Melodies_Midi_Chair_Sit",
                MidiChairInteraction.class,
                MidiChairInteraction.CODEC
        );

        this.getCodecRegistry(Interaction.CODEC).register(
                "Ymmersive_Melodies_Debug_Console",
                DebugConsoleInteraction.class,
                DebugConsoleInteraction.CODEC
        );

        AssetRegistry.register(HytaleAssetStore.builder(MelodyAsset.class, new DefaultAssetMap<>())
                .setPath("YmmersiveMelodies")
                .setCodec(MelodyAsset.CODEC)
                .setKeyFunction(MelodyAsset::getId)
                .build()
        );

        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register(
                "Ymmersive_Melodies_Selection",
                MelodySelectionSupplier.class,
                MelodySelectionSupplier.CODEC);

        // Inicializar serviço MIDI
        try {
            boolean initialized = MidiInputService.getInstance().initialize();
            if (!initialized) {
                System.out.println("[YmmersiveMelodies] Warning: No MIDI input devices found. MidiToQwerty mode will not be available.");
            } else {
                System.out.println("[YmmersiveMelodies] MIDI input service initialized successfully.");
            }
        } catch (Exception e) {
            System.err.println("[YmmersiveMelodies] Error initializing MIDI input service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static YmmersiveMelodies getInstance() {
        return instance;
    }

    public ResourceType<EntityStore, YmmersiveMelodiesRegistry> getYmmersiveMelodiesRegistry() {
        return ymmersiveMelodiesRegistry;
    }
}