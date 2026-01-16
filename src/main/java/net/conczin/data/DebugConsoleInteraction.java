package net.conczin.data;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.gui.MidiDebugConsoleGui;
import net.conczin.utils.Utils;

import javax.annotation.Nonnull;

/**
 * Interação para abrir o console de debug do MidiToQwerty.
 * Pode ser acionada por F5 ou por um comando/evento.
 */
public class DebugConsoleInteraction extends SimpleInteraction {
    public static final BuilderCodec<DebugConsoleInteraction> CODEC = BuilderCodec.builder(
                    DebugConsoleInteraction.class, DebugConsoleInteraction::new, SimpleInteraction.CODEC
            )
            .documentation("Opens the MidiToQwerty debug console.")
            .build();

    @Override
    protected void tick0(boolean firstRun, float time, InteractionType type, @Nonnull InteractionContext context, CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();

        // Abrir console de debug
        Utils.setPage(ref, store, (playerRef, lifetime) -> new MidiDebugConsoleGui(playerRef));
    }
}

