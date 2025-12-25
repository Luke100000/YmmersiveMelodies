package net.conczin.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.CustomPage;
import com.hypixel.hytale.protocol.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public abstract class CodecDataInteractiveUIPage<T> extends CustomUIPage {
    protected final Codec<T> eventDataCodec;

    public CodecDataInteractiveUIPage(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime, @Nonnull Codec<T> eventDataCodec) {
        super(playerRef, lifetime);
        this.eventDataCodec = eventDataCodec;
    }

    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull T data) {
    }

    protected void sendUpdate(@Nullable UICommandBuilder commandBuilder, @Nullable UIEventBuilder eventBuilder, boolean clear) {
        Ref<EntityStore> ref = this.playerRef.getReference();
        if (ref != null) {
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(
                    () -> {
                        Player player = store.getComponent(ref, Player.getComponentType());

                        assert player != null;

                        player.getPageManager()
                                .updateCustomPage(
                                        new CustomPage(
                                                this.getClass().getName(),
                                                false,
                                                clear,
                                                this.lifetime,
                                                commandBuilder != null ? commandBuilder.getCommands() : UICommandBuilder.EMPTY_COMMAND_ARRAY,
                                                eventBuilder != null ? eventBuilder.getEvents() : UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY
                                        )
                                );
                    }
            );
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, String rawData) {
        ExtraInfo extrainfo = ExtraInfo.THREAD_LOCAL.get();

        T t;
        try {
            t = this.eventDataCodec.decodeJson(new RawJsonReader(rawData.toCharArray()), extrainfo);
        } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
        }

        assert t != null;
        this.handleDataEvent(ref, store, t);
    }

    @Override
    protected void sendUpdate(@Nullable UICommandBuilder commandBuilder, boolean clear) {
        this.sendUpdate(commandBuilder, null, clear);
    }
}

