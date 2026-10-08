package com.hbm_m.item.weapon.sedna.hud;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code IHUDComponent}. Das Zeichnen laeuft clientseitig im Hotbar-Overlay; {@code gui} ist ein
 * {@code net.minecraft.client.gui.GuiGraphics} (als Object, damit die Waffenkonfigurationen serverseitig keine
 * Client-Klassen laden).
 */
public interface IHUDComponent {

    int getComponentHeight(Player player, ItemStack stack);

    void renderHUDComponent(Object gui, Player player, ItemStack stack, int bottomOffset, int gunIndex);
}
