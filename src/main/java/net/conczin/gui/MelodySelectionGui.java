package net.conczin.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.data.Melody;
import net.conczin.data.MelodyAsset;
import net.conczin.data.MelodyPlaybackInteraction;
import net.conczin.data.MelodyProgress;
import net.conczin.data.MelodySyncRegistry;
import net.conczin.data.YmmersiveMelodiesRegistry;
import net.conczin.utils.RecordCodec;
import net.conczin.utils.Utils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MelodySelectionGui extends CodecDataInteractiveUIPage<MelodySelectionGui.Data> {
    private static final Value<String> BUTTON_LABEL_STYLE = Value.ref("Pages/YmmersiveMelodies/MelodyButton.ui", "LabelStyle");
    private static final Value<String> BUTTON_LABEL_STYLE_SELECTED = Value.ref("Pages/YmmersiveMelodies/MelodyButton.ui", "SelectedLabelStyle");

    private final String instrument;

    private String selectedMelody;
    private String searchQuery = "";

    public MelodySelectionGui(@Nonnull PlayerRef playerRef, String instrument) {
        this(playerRef, instrument, playerRef.getReference() == null ? "" : getMelody(playerRef.getReference()));
    }

    public MelodySelectionGui(@Nonnull PlayerRef playerRef, String instrument, String initialMelody) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);

        this.instrument = instrument;
        this.selectedMelody = initialMelody;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/YmmersiveMelodies/MelodySelection.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"), false);

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Stop", EventData.of("Action", "Stop"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Delete", EventData.of("Action", "Delete"));
        // eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Tracks", EventData.of("Action", "Tracks"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Upload", EventData.of("Action", "Upload"));

        this.buildList(ref, commandBuilder, eventBuilder);
    }

    private void buildList(Ref<EntityStore> ref, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.clear("#MelodyList");

        YmmersiveMelodiesRegistry resource = ref.getStore().getResource(YmmersiveMelodiesRegistry.getResourceType());
        UUID uuid = Utils.getUUID(ref);

        Map<String, Melody> ownMelodies = resource.get(uuid);
        List<Melody> melodies = filterAndSort(ownMelodies);

        int rowIndex = 0;

        // Own melodies section
        if (!melodies.isEmpty()) {
            addSeparator(commandBuilder, rowIndex, "server.customUI.melodySelection.separator.own");
            rowIndex++;
        }
        for (Melody melody : melodies) {
            addMelody(commandBuilder, eventBuilder, rowIndex, uuid + ":" + melody.name(), melody.name());
            rowIndex++;
        }

        // Public melodies section
        if (!ownMelodies.isEmpty()) {
            addSeparator(commandBuilder, rowIndex, "server.customUI.melodySelection.separator.server");
            rowIndex++;
        }
        for (MelodyAsset value : MelodyAsset.getAssetStore().getAssetMap().getAssetMap().values()) {
            String name = value.getMelody().name();
            if (this.searchQuery.isEmpty() || value.getId().toLowerCase().contains(this.searchQuery) || name.toLowerCase().contains(this.searchQuery)) {
                addMelody(commandBuilder, eventBuilder, rowIndex, value.getId(), name);
                rowIndex++;
            }
        }

        // Button states
        commandBuilder.set("#Stop.Disabled", selectedMelody.isEmpty());
        commandBuilder.set("#Delete.Disabled", selectedMelody.isEmpty() || !selectedMelody.startsWith(uuid + ":"));
        // commandBuilder.set("#Tracks.Disabled", selectedMelody.isEmpty());
    }

    private static void addSeparator(UICommandBuilder commandBuilder, int rowIndex, String message) {
        commandBuilder.append("#MelodyList", "Pages/YmmersiveMelodies/MelodySeperator.ui");
        commandBuilder.set("#MelodyList[" + rowIndex + "] #Label.Text", Message.translation(message));
    }

    private List<Melody> filterAndSort(Map<String, Melody> melodies) {
        return melodies.entrySet().stream()
                .filter(e -> this.searchQuery.isEmpty() || e.getValue().name().toLowerCase().contains(this.searchQuery) || e.getKey().toLowerCase().contains(this.searchQuery))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    private void addMelody(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, int rowIndex, String identifier, String name) {
        commandBuilder.append("#MelodyList", "Pages/YmmersiveMelodies/MelodyButton.ui");
        commandBuilder.set("#MelodyList[" + rowIndex + "] #Button.Text", Message.translation(name));
        commandBuilder.set("#MelodyList[" + rowIndex + "] #Button.Style", identifier.equals(selectedMelody) ? BUTTON_LABEL_STYLE_SELECTED : BUTTON_LABEL_STYLE);
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#MelodyList[" + rowIndex + "] #Button", EventData.of("SelectedMelody", identifier));
    }

    private void rebuildList(Ref<EntityStore> ref) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        this.buildList(ref, commandBuilder, eventBuilder);
        this.sendUpdate(commandBuilder, eventBuilder, false);
    }

    public record Data(String searchQuery, String selectedMelody, String action) {
        public static final Codec<Data> CODEC = RecordCodec.composite(
                "@SearchQuery", Codec.STRING, Data::searchQuery,
                "SelectedMelody", Codec.STRING, Data::selectedMelody,
                "Action", Codec.STRING, Data::action,
                Data::new
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);

        // Search
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
            rebuildList(ref);
        }

        // Select melody
        if (data.selectedMelody != null) {
            setMelody(ref, data.selectedMelody);
            rebuildList(ref);
        }

        // Stop music
        if ("Stop".equals(data.action)) {
            setMelody(ref, "");
            rebuildList(ref);
        }

        // Delete music
        if ("Delete".equals(data.action)) {
            YmmersiveMelodiesRegistry resource = store.getResource(YmmersiveMelodiesRegistry.getResourceType());
            String melodyName = selectedMelody.contains(":") ? selectedMelody.split(":", 2)[1] : selectedMelody;
            resource.delete(Utils.getUUID(ref), melodyName);
            setMelody(ref, "");
            rebuildList(ref);
        }

        // Switch to tracks selection
        if ("Tracks".equals(data.action)) {
            Utils.setPage(ref, store, MelodyUploadGui::new);
        }

        // Switch to upload screen
        if ("Upload".equals(data.action)) {
            Utils.setPage(ref, store, MelodyUploadGui::new);
        }
    }

    private void setMelody(Ref<EntityStore> ref, String selectedMelody) {
        MelodyProgress progress = Utils.getData(ref, "MelodyProgress", MelodyProgress.CODEC);
        if (!progress.melody.isEmpty()) {
            MelodySyncRegistry.removePlayer(Utils.getUUID(ref), progress.melody);
        }
        progress.melody = selectedMelody;
        progress.time = 0;
        progress.startWorldTime = 0;
        progress.worldTime = 0;
        Utils.setData(ref, "MelodyProgress", MelodyProgress.CODEC, progress);
        this.selectedMelody = selectedMelody;
    }

    private static String getMelody(Ref<EntityStore> ref) {
        return Utils.getData(ref, "MelodyProgress", MelodyProgress.CODEC).melody;
    }
}