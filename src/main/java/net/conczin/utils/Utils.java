package net.conczin.utils;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.function.BiFunction;

public class Utils {
    public static UUID getUUID(Ref<EntityStore> ref) {
        UUIDComponent uuidComponent = ref.getStore().getComponent(ref, UUIDComponent.getComponentType());
        assert uuidComponent != null;
        return uuidComponent.getUuid();
    }

    public static <T> void setData(Ref<EntityStore> ref, String field, BuilderCodec<T> codec, T data) {
        Inventory inventory = getInventory(ref);
        ItemStack itemInHand = inventory.getActiveHotbarItem();
        if (itemInHand != null) {
            ItemStack newItemInHand = itemInHand.withMetadata(field, codec, data);
            inventory.getHotbar().replaceItemStackInSlot(inventory.getActiveHotbarSlot(), itemInHand, newItemInHand);
        }
    }

    public static <T> T getData(Ref<EntityStore> ref, String field, BuilderCodec<T> codec) {
        Inventory inventory = getInventory(ref);
        ItemStack itemInHand = inventory.getActiveHotbarItem();
        if (itemInHand != null) {
            return itemInHand.getFromMetadataOrDefault(field, codec);
        }
        return codec.getDefaultValue();
    }

    public static Inventory getInventory(Ref<EntityStore> ref) {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        assert player != null;
        return player.getInventory();
    }

    public static void setPage(Ref<EntityStore> ref, Store<EntityStore> store, BiFunction<PlayerRef, CustomPageLifetime, ? extends CustomUIPage> pageConstructor) {
        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
        assert playerRefComponent != null;
        player.getPageManager().openCustomPage(ref, store, pageConstructor.apply(playerRefComponent, CustomPageLifetime.CanDismiss));
    }
}
