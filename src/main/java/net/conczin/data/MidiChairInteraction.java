package net.conczin.data;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.utils.Utils;

import javax.annotation.Nonnull;

/**
 * Interação para detectar quando o jogador senta na cadeira Tavern.
 * Quando o jogador senta na cadeira Tavern (Furniture_Tavern_Chair), marca o estado.
 * Este pode ser usado como interação customizada ou ser chamado quando detectamos que o jogador está sentado.
 */
public class MidiChairInteraction extends SimpleInteraction {
    public static final BuilderCodec<MidiChairInteraction> CODEC = BuilderCodec.builder(
                    MidiChairInteraction.class, MidiChairInteraction::new, SimpleInteraction.CODEC
            )
            .documentation("Detects when player sits on Tavern Chair for MidiToQwerty mode.")
            .build();

    @Override
    protected void tick0(boolean firstRun, float time, InteractionType type, @Nonnull InteractionContext context, CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) return;
        
        // Quando o jogador usa esta interação (pode ser chamada quando senta na cadeira),
        // marcar que está sentado na cadeira Tavern
        Utils.setSittingOnTavernChair(ref, true);
    }
    
    /**
     * Método auxiliar para marcar jogador como sentado.
     * Pode ser chamado de outras partes do código quando detectamos que está sentado.
     */
    public static void markPlayerSitting(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) return;
        Utils.setSittingOnTavernChair(ref, true);
    }
    
    /**
     * Método auxiliar para marcar jogador como levantado.
     */
    public static void markPlayerStanding(Ref<EntityStore> ref) {
        if (ref == null || !ref.isValid()) return;
        Utils.setSittingOnTavernChair(ref, false);
    }
}

