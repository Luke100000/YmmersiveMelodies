package net.conczin.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.conczin.utils.RecordCodec;

import javax.annotation.Nonnull;

public record MelodySelectionSupplier(String instrument) implements OpenCustomUIInteraction.CustomPageSupplier {
    public static final Codec<MelodySelectionSupplier> CODEC = RecordCodec.composite(
            "Instrument", Codec.STRING, MelodySelectionSupplier::instrument,
            MelodySelectionSupplier::new
    );

    @Nonnull
    @Override
    public CustomUIPage tryCreate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> componentAccessor, @Nonnull PlayerRef playerRef, InteractionContext context) {
        return new MelodySelectionGui(playerRef, instrument);
    }
}
