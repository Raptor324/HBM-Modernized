package com.hbm_m.mixin.client;

//? if neoforge {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.neoforged.neoforge.network.handlers.ClientPayloadHandler")
public interface ClientPayloadHandlerInvoker {

    @Invoker("createMenuScreen")
    static void hbm_m$createMenuScreen(Component name, MenuType<?> menuType, int windowId, RegistryFriendlyByteBuf buf) {
        throw new AssertionError();
    }
}
//?} else {
/*// NeoForge-only; the Mixin annotation processor rejects unknown string targets, so the stub
// points at a class that exists everywhere and contributes nothing.
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public interface ClientPayloadHandlerInvoker {
}
*///?}
