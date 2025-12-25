package net.conczin.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.CustomPageLifetime;
import com.hypixel.hytale.protocol.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.data.Melody;
import net.conczin.data.MidiParser;
import net.conczin.data.YmmersiveMelodiesRegistry;
import net.conczin.utils.RecordCodec;
import net.conczin.utils.Utils;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class MelodyUploadGui extends CodecDataInteractiveUIPage<MelodyUploadGui.Data> {
    public MelodyUploadGui(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, Data.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/YmmersiveMelodies/MelodyUpload.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Upload", new EventData().append("@Name", "#Name.Value").append("@Url", "#Url.Value"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Cancel", EventData.of("Action", "Cancel"));
    }

    public record Data(String name, String url, String action) {
        public static final Codec<Data> CODEC = RecordCodec.composite(
                "@Name", Codec.STRING, Data::name,
                "@Url", Codec.STRING, Data::url,
                "Action", Codec.STRING, Data::action,
                Data::new
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);

        // Upload melody
        if (data.name != null && data.url != null) {
            try {
                if (data.name.isBlank()) {
                    error("Name cannot be empty");
                    return;
                }

                // Get player UUID
                UUID uuid = Utils.getUUID(ref);

                // Check for duplicate names
                YmmersiveMelodiesRegistry resource = store.getResource(YmmersiveMelodiesRegistry.getResourceType());
                if (resource.get(uuid, data.name) != null) {
                    error("A melody with this title already exists");
                    return;
                }

                // Download and parse MIDI
                InputStream in = new URI(data.url).toURL().openStream();
                List<Melody.Track> tracks = MidiParser.parseMidi(in);
                resource.add(uuid, new Melody(data.name, tracks));
                returnToSelection(ref, store, data.name);
            } catch (Exception e) {
                error(e.getMessage());
                return;
            }
        }

        // Cancel
        if ("Cancel".equals(data.action)) {
            returnToSelection(ref, store, "");
        }
    }

    private static void returnToSelection(Ref<EntityStore> ref, Store<EntityStore> store, String initialMelody) {
        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
        assert playerRefComponent != null;
        MelodySelectionGui gui = new MelodySelectionGui(playerRefComponent, "", initialMelody);
        player.getPageManager().openCustomPage(ref, store, gui);
    }

    private void error(String error) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#Details.Text", Message.translation("customUI.melodyUpload.error").param("message", error == null ? "Unknown" : error));
        commandBuilder.set("#Details.Style.TextColor", "#ff0000");
        this.sendUpdate(commandBuilder, new UIEventBuilder(), false);
    }
}