package com.hbm_m.client;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

import com.hbm_m.blockentity.machines.PWRBlockEntity;
import com.hbm_m.inventory.gui.GUIScreenSlicePrinter;
import com.hbm_m.item.nuclear.PWRFuelPrinterItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client-Seite von {@code ItemPWRPrinter.deserialize}: schreibt die Bauteile in die Traeger der Client-Welt und oeffnet den
 * Schnittdrucker, wenn der Spieler den Drucker in der Hand haelt. Liegt ausserhalb des Pakets, damit der Server beim
 * Registrieren keine Client-Klassen laedt.
 */
//? if forge || neoforge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
public final class PwrPrinterClientHooks {

    private PwrPrinterClientHooks() {}

    public static void deserialize(int x1, int y1, int z1, int x2, int y2, int z2, int dirOrdinal, int[] blocks) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return;
        Direction dir = Direction.from3DDataValue(dirOrdinal);

        int i = 0;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    int id = i < blocks.length ? blocks[i] : 0;
                    i++;
                    Block block = BuiltInRegistries.BLOCK.byId(id);

                    if (!(world.getBlockEntity(new BlockPos(x, y, z)) instanceof PWRBlockEntity pwr)) continue;
                    pwr.block = block == Blocks.AIR ? null : block;
                }
            }
        }

        // Druckeransicht fuer den Spieler oeffnen, der den Drucker haelt
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof PWRFuelPrinterItem) {
            Minecraft.getInstance().setScreen(new GUIScreenSlicePrinter(x1, y1, z1, x2, y2, z2, dir, PWRFuelPrinterItem.whitelist()));
        }
    }
}
