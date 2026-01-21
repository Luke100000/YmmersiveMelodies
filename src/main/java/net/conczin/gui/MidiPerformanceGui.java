package net.conczin.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.utils.RecordCodec;
import net.conczin.data.MelodyPlaybackInteraction;

import javax.annotation.Nonnull;

/**
 * A dedicated GUI for capturing keyboard input to play instruments.
 * Uses Hytale's UI locking mechanism to prevent player movement and capture
 * keys.
 */
public class MidiPerformanceGui extends CodecDataInteractiveUIPage<MidiPerformanceGui.Data> {

    public MidiPerformanceGui(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }

    private String lastInput = "";

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        // Load the UI
        commandBuilder.append("Pages/YmmersiveMelodies/MidiDebugConsole.ui");

        // BINDING VALUE CHANGED
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InputCapture", new EventData()
                .append("Action", "InputChanged")
                .append("@Value", "#InputCapture.Value"));

        // Populate Device List
        // Populate Device List
        buildDeviceList(commandBuilder, eventBuilder);
    }

    private void buildDeviceList(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder) {
        commandBuilder.clear("#DeviceList");

        java.util.List<String> devices = net.conczin.data.MidiInputService.getInstance().getAvailableDevices();
        int rowIndex = 0;

        for (int i = 0; i < devices.size(); i++) {
            String name = devices.get(i);

            // Append the button template to the list
            commandBuilder.append("#DeviceList", "Pages/YmmersiveMelodies/MelodyButton.ui");

            // Set Text
            commandBuilder.set("#DeviceList[" + rowIndex + "] #Button.Text", name);

            // Bind Click
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#DeviceList[" + rowIndex + "] #Button",
                    new EventData()
                            .append("Action", "SelectDevice")
                            .append("Index", String.valueOf(i)));

            rowIndex++;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);

        if ("InputChanged".equals(data.action)) {
            String currentValue = data.value == null ? "" : data.value;

            // Robust Diff Logic
            // We compare 'lastInput' and 'currentValue' to find what was added.
            // This handles Appends ("A"->"AB") and Replacements ("A"->"B") gracefully.
            int commonPrefixLength = 0;
            int length = Math.min(lastInput.length(), currentValue.length());
            for (int i = 0; i < length; i++) {
                if (lastInput.charAt(i) == currentValue.charAt(i)) {
                    commonPrefixLength++;
                } else {
                    break;
                }
            }

            // The new characters are everything after the common prefix
            String newChars = currentValue.substring(commonPrefixLength);

            // Update state to current (for next delta)
            lastInput = currentValue;

            if (newChars.isEmpty())
                return;

            try {
                // Iterate through NEW characters only
                for (char c : newChars.toCharArray()) {
                    // Map character to KeyCode and Modifier
                    int keyCode = -1;
                    int modifier = 0; // 0 = None, 160 = Shift

                    // Logic to determine Shift based on Character
                    if (Character.isUpperCase(c)) {
                        modifier = 160; // Shift
                        keyCode = Character.toUpperCase(c);
                    } else if (Character.isLowerCase(c)) {
                        modifier = 0;
                        keyCode = Character.toUpperCase(c);
                    } else if (Character.isDigit(c)) {
                        // In standard piano layout, numbers are simply mapped keys (often black keys)
                        // We act as if they are base keys.
                        modifier = 0;
                        keyCode = (int) c;
                    } else {
                        // Symbols mapping (Standard US + ABNT2 Layout quirks)
                        // Maps punctuation back to KeyCode + Shift (160)
                        if (c == '!') {
                            keyCode = '1';
                            modifier = 160;
                        } else if (c == '@') {
                            keyCode = '2';
                            modifier = 160;
                        } else if (c == '#') {
                            keyCode = '3';
                            modifier = 160;
                        } else if (c == '$') {
                            keyCode = '4';
                            modifier = 160;
                        } else if (c == '%') {
                            keyCode = '5';
                            modifier = 160;
                        } else if (c == '^' || c == '¨') {
                            keyCode = '6';
                            modifier = 160;
                        } // Handle both US (^) and ABNT2 (¨)
                        else if (c == '&') {
                            keyCode = '7';
                            modifier = 160;
                        } else if (c == '*') {
                            keyCode = '8';
                            modifier = 160;
                        } else if (c == '(') {
                            keyCode = '9';
                            modifier = 160;
                        } else if (c == ')') {
                            keyCode = '0';
                            modifier = 160;
                        }

                        // Add commonly confused symbols or alternate layout mappings if needed

                        // If we get here, it might be a lower case letter that wasn't caught or some
                        // other symbol.
                        // Try strictly upper casing.
                        else {
                            keyCode = Character.toUpperCase(c);
                        }
                    }

                    // Find MIDI Note
                    int midiNote = -1;
                    if (keyCode != -1) {
                        if (modifier == 0) {
                            midiNote = net.conczin.data.MidiToQwertyMapping.findMidiNoteForKey(keyCode);
                        } else {
                            midiNote = net.conczin.data.MidiToQwertyMapping.findMidiNoteForKeyCombo(modifier, keyCode);
                        }
                    }

                    if (midiNote != -1) {
                        // Get Instrument
                        com.hypixel.hytale.server.core.inventory.ItemStack itemInHand = net.conczin.utils.Utils
                                .getInventory(ref).getActiveHotbarItem();
                        String instrument = "Piano";
                        if (itemInHand != null) {
                            String itemId = itemInHand.getItemId();
                            if (itemId != null && itemId.startsWith("Ymmersive_Melodies_")) {
                                instrument = itemId.replace("Ymmersive_Melodies_", "");
                            }
                        }

                        // Play Note (Buffered for Polyphony)
                        MelodyPlaybackInteraction.bufferNote(store, ref, instrument, midiNote, 100);
                    }
                }
            } catch (Exception e) {
                System.err.println("[MidiPerformanceGui] Error processing input: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // ALWAYS send an update to acknowledge the event and unblock the client UI
                // ("Loading..." state).
                // We do NOT clear the value unless necessary, to avoid race conditions.
                UICommandBuilder commandBuilder = new UICommandBuilder();

                // Only clear if buffer is getting invalidly large (safety net)
                if (currentValue.length() > 64) {
                    commandBuilder.set("#InputCapture.Value", "");
                    lastInput = "";
                }

                // Send the Ack
                this.sendUpdate(commandBuilder, null, false);
            }
        } else if ("SelectDevice".equals(data.action)) {
            // Handle Device Selection
            UICommandBuilder cb = new UICommandBuilder();
            if (data.index != null) {
                try {
                    int idx = Integer.parseInt(data.index);
                    java.util.List<String> devices = net.conczin.data.MidiInputService.getInstance()
                            .getAvailableDevices();

                    if (idx >= 0 && idx < devices.size()) {
                        boolean success = net.conczin.data.MidiInputService.getInstance().selectDevice(idx);

                        if (success) {
                            String deviceName = devices.get(idx);
                            System.out.println("Selected MIDI Device: " + deviceName);

                            // Register Listener for MIDI -> Game Linkage
                            java.util.UUID playerId = net.conczin.utils.Utils.getUUID(ref);
                            net.conczin.data.MidiInputService.getInstance().registerListener(playerId, note -> {
                                net.conczin.data.MelodyPlaybackInteraction.enqueueMidiNote(playerId, note);
                            });

                            // Update UI Feedback
                            cb.set("#Instructions.Text", "§aConnected to: " + deviceName);
                        } else {
                            cb.set("#Instructions.Text", "§cFailed to connect to device.");
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            // Send Ack to unblock UI + Visual Update
            this.sendUpdate(cb, null, false);
        }
    }

    // We override the raw handle to see what fields are coming in first!
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        System.out.println("[MidiPerformanceGui] RAW DATA: " + rawData);
        super.handleDataEvent(ref, store, rawData);
    }

    public record Data(String value, String action, String index) {
        public static final Codec<Data> CODEC = RecordCodec.composite(
                "@Value", Codec.STRING, Data::value,
                "Action", Codec.STRING, Data::action,
                "Index", Codec.STRING, Data::index,
                Data::new);
    }
}
