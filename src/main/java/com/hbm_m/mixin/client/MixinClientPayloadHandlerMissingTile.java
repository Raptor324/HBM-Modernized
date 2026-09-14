package com.hbm_m.mixin.client;

//? if neoforge {
import com.hbm_m.inventory.menu.MenuBlockEntityMissingException;
import com.hbm_m.main.MainRegistry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A menu constructor that throws makes NeoForge disconnect the client
 * ("Failed to open a screen with advanced data"). With a replay mod (Flashback) the block entity
 * behind a menu can legitimately be absent, so that one failure is downgraded to a warning.
 * Any other exception still disconnects as before.
 */
@Mixin(targets = "net.neoforged.neoforge.network.handlers.ClientPayloadHandler")
public class MixinClientPayloadHandlerMissingTile {

    @Redirect(
            method = "handle(Lnet/neoforged/neoforge/network/payload/AdvancedOpenScreenPayload;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/handlers/ClientPayloadHandler;createMenuScreen(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/network/RegistryFriendlyByteBuf;)V"))
    private static void hbm_m$tolerateMissingTile(Component name, MenuType<?> menuType, int windowId, RegistryFriendlyByteBuf buf) {
        try {
            ClientPayloadHandlerInvoker.hbm_m$createMenuScreen(name, menuType, windowId, buf);
        } catch (MenuBlockEntityMissingException e) {
            MainRegistry.LOGGER.warn("Not opening menu {}: {}", menuType, e.getMessage());
            // The server already switched player.containerMenu to this window; tell it we closed,
            // otherwise inventory clicks are dropped until some other screen is closed.
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new net.minecraft.network.protocol.game.ServerboundContainerClosePacket(windowId));
            }
        }
    }
}
//?} else {
/*// NeoForge-only; the Mixin annotation processor rejects unknown string targets, so the stub
// points at a class that exists everywhere and contributes nothing.
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public class MixinClientPayloadHandlerMissingTile {
}
*///?}
