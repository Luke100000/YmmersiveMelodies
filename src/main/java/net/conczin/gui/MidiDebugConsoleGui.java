package net.conczin.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.data.MidiInputService;
import net.conczin.data.MidiToQwertyState;
import net.conczin.utils.KeyboardInterceptor;
import net.conczin.utils.RecordCodec;
import net.conczin.utils.Utils;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MidiDebugConsoleGui extends CodecDataInteractiveUIPage<MidiDebugConsoleGui.Data> {
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    
    public MidiDebugConsoleGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/YmmersiveMelodies/MidiDebugConsole.ui");

        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Refresh", EventData.of("Action", "Refresh"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Close", EventData.of("Action", "Close"));

        this.updateStatus(ref, store, commandBuilder);
    }

    private void updateStatus(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder commandBuilder) {
        if (!ref.isValid()) return;

        // Get player info
        String playerId = Utils.getUUID(ref).toString();
        
        // Check if has instrument in hand
        com.hypixel.hytale.server.core.inventory.ItemStack itemInHand = Utils.getInventory(ref).getActiveHotbarItem();
        boolean hasInstrument = false;
        String itemId = "Nenhum";
        if (itemInHand != null) {
            itemId = itemInHand.getItemId() != null ? itemInHand.getItemId() : "Desconhecido";
            hasInstrument = itemId != null && itemId.startsWith("Ymmersive_Melodies_") &&
                          (itemId.contains("Piano") || itemId.contains("Flute") ||
                           itemId.contains("Lute") || itemId.contains("Trumpet") ||
                           itemId.contains("Bagpipe") || itemId.contains("Vielle") ||
                           itemId.contains("Handpan") || itemId.contains("Didgeridoo") ||
                           itemId.contains("Tiny_Drum"));
        }

        // Check if sitting on Tavern Chair
        boolean isSitting = Utils.hasMidiChair(ref);

        // Check if MidiToQwerty is active
        boolean midiToQwertyActive = false;
        if (itemInHand != null) {
            MidiToQwertyState state = itemInHand.getFromMetadataOrDefault("MidiToQwertyState", MidiToQwertyState.CODEC);
            midiToQwertyActive = state != null && state.active;
        }

        // Check if keyboard interception is active
        boolean keyboardInterceptionActive = KeyboardInterceptor.isIntercepting(Utils.getUUID(ref));

        // Check if MIDI device is connected
        boolean midiDeviceConnected = MidiInputService.getInstance().isInitialized();

        // Update UI
        commandBuilder.set("#HasInstrumentStatus.Text", 
            Message.translation("customUI.midiDebugConsole.hasInstrument")
                .param("status", hasInstrument ? "SIM" : "NÃO")
                .param("item", itemId));
        
        commandBuilder.set("#IsSittingStatus.Text", 
            Message.translation("customUI.midiDebugConsole.isSitting")
                .param("status", isSitting ? "SIM" : "NÃO"));
        
        commandBuilder.set("#MidiToQwertyActiveStatus.Text", 
            Message.translation("customUI.midiDebugConsole.midiToQwertyActive")
                .param("status", midiToQwertyActive ? "SIM" : "NÃO"));
        
        commandBuilder.set("#KeyboardInterceptionStatus.Text", 
            Message.translation("customUI.midiDebugConsole.keyboardInterception")
                .param("status", keyboardInterceptionActive ? "SIM" : "NÃO"));
        
        commandBuilder.set("#MidiDeviceStatus.Text", 
            Message.translation("customUI.midiDebugConsole.midiDevice")
                .param("status", midiDeviceConnected ? "CONECTADO" : "DESCONECTADO"));
        
        commandBuilder.set("#ItemInHandStatus.Text", 
            Message.translation("customUI.midiDebugConsole.itemInHand")
                .param("item", itemId));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);

        if ("Refresh".equals(data.action)) {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            this.updateStatus(ref, store, commandBuilder);
            this.sendUpdate(commandBuilder, null, false);
        }

        if ("Close".equals(data.action)) {
            // Close the page - send an empty update with clear flag
            this.sendUpdate(null, null, true);
        }
    }

    public record Data(String action) {
        public static final Codec<Data> CODEC = RecordCodec.composite(
                "Action", Codec.STRING, Data::action,
                Data::new
        );
    }
}

