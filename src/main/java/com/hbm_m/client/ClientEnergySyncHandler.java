package com.hbm_m.client;

import com.hbm_m.api.energy.ILongEnergyMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientEnergySyncHandler {
    // Добавили аргумент long delta
    public static void handle(int containerId, long energy, long maxEnergy, long delta) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu != null) {
            if (player.containerMenu.containerId == containerId &&
                    player.containerMenu instanceof ILongEnergyMenu menu) {
                // Передаем дельту в меню
                menu.setEnergy(energy, maxEnergy, delta);
            }
        }
    }
}