package com.hbm_m.armormod.item;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ISatChip;
import com.hbm_m.network.AuxParticlePacket;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;
import com.hbm_m.satellite.SatelliteScanner;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * 1:1 {@code com.hbm.items.armor.ItemModLens} ({@code neutrino_lens}, Sonderplatz aller Ruestungsteile): mit einem
 * Tiefenscan-Satelliten auf der eigenen Frequenz tastet sie Schicht fuer Schicht (Hoehe = Weltzeit modulo
 * max(min(y+10, 255), 64)) die 7x7 Chunks um den Spieler ab und markiert seltene Bloecke (Marker 15 s, 300 m).
 * Im Port gibt es Alexandrit- und Coltanerz doppelt; beide Varianten werden markiert.
 */
public class ItemModLens extends ItemArmorMod implements ISatChip {

    public ItemModLens(Properties properties) {
        super(properties.stacksTo(1), ArmorModificationHelper.extra, true, false, false, false);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Satellite Frequency: " + this.getFreq(stack)).withStyle(ChatFormatting.AQUA));
        list.add(Component.literal(""));
        super.appendHbmTooltip(stack, level, list, flag);
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(Component.literal("  " + stack.getHoverName().getString() + " (Freq: " + getFreq(stack) + ")").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity.level() instanceof ServerLevel world)) return;
        if (!(entity instanceof ServerPlayer player)) return;

        ItemStack lens = ArmorModificationHelper.pryMods(armor)[ArmorModificationHelper.extra];
        if (lens == null || lens.isEmpty()) return;

        int freq = this.getFreq(lens);
        Satellite sat = SatelliteManager.get(world).getSatFromFreq(freq);
        if (!(sat instanceof SatelliteScanner)) return;

        int x = (int) Math.floor(player.getX());
        int y = (int) Math.floor(player.getY());
        int z = (int) Math.floor(player.getZ());
        int range = 3;

        int cX = x >> 4;
        int cZ = z >> 4;

        int height = Math.max(Math.min(y + 10, 255), 64);
        int seg = (int) (world.getGameTime() % height);

        int hits = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Block bobblehead = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "bobblehead"));

        for (int chunkX = cX - range; chunkX <= cX + range; chunkX++) {
            for (int chunkZ = cZ - range; chunkZ <= cZ + range; chunkZ++) {

                LevelChunk c = world.getChunk(chunkX, chunkZ);

                for (int ix = 0; ix < 16; ix++) {
                    for (int iz = 0; iz < 16; iz++) {

                        int aX = (chunkX << 4) + ix;
                        int aZ = (chunkZ << 4) + iz;
                        Block b = c.getBlockState(pos.set(aX, seg, aZ)).getBlock();

                        if (addIf(ModBlocks.ALEXANDRITE_ORE.get(), b, 1, aX, seg, aZ, "Alexandrite", 0x00ffff, player)) hits++;
                        if (addIf(ModBlocks.ALEXANDRITE_ORE.get(), b, 1, aX, seg, aZ, "Alexandrite", 0x00ffff, player)) hits++;
                        if (addIf(ModBlocks.ORE_OIL.get(), b, 300, aX, seg, aZ, "Oil", 0xa0a0a0, player)) hits++;
                        if (addIf(ModBlocks.ORE_BEDROCK_OIL.get(), b, 300, aX, seg, aZ, "Bedrock Oil", 0xa0a0a0, player)) hits++;
                        if (addIf(ModBlocks.COLTAN_ORE.get(), b, 5, aX, seg, aZ, "Coltan", 0xa0a000, player)) hits++;
                        if (addIf(ModBlocks.COLTAN_ORE.get(), b, 5, aX, seg, aZ, "Coltan", 0xa0a000, player)) hits++;
                        if (addIf(ModBlocks.STONE_GNEISS.get(), b, 5000, aX, seg, aZ, "Schist", 0x8080ff, player)) hits++;
                        if (addIf(ModBlocks.ORE_AUSTRALIUM.get(), b, 1000, aX, seg, aZ, "Australium", 0xffff00, player)) hits++;
                        if (addIf(Blocks.END_PORTAL_FRAME, b, 1, aX, seg, aZ, "End Portal", 0x40b080, player)) hits++;
                        if (addIf(ModBlocks.VOLCANO_CORE.get(), b, 1, aX, seg, aZ, "Volcano Core", 0xff4000, player)) hits++;
                        if (addIf(ModBlocks.PINK_LOG.get(), b, 1, aX, seg, aZ, "Pink Log", 0xff00ff, player)) hits++;
                        if (bobblehead != Blocks.AIR && addIf(bobblehead, b, 1, aX, seg, aZ, "A Treasure!", 0xff0000, player)) hits++;
                        if (addIf(ModBlocks.DECO_LOOT.get(), b, 1, aX, seg, aZ, null, 0x800000, player)) hits++;
                        if (addIf(ModBlocks.CRATE_AMMO.get(), b, 1, aX, seg, aZ, null, 0x800000, player)) hits++;
                        if (addIf(ModBlocks.CRATE_CAN.get(), b, 1, aX, seg, aZ, null, 0x800000, player)) hits++;
                        if (addIf(ModBlocks.ORE_BEDROCK.get(), b, 1, aX, seg, aZ, "Bedrock Ore", 0xff0000, player)) hits++;

                        if (hits > 100) return;
                    }
                }
            }
        }
    }

    private boolean addIf(Block target, Block b, int chance, int x, int y, int z, @Nullable String label, int color, ServerPlayer player) {

        if (target == b && player.getRandom().nextInt(chance) == 0) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "marker");
            data.putInt("color", color);
            data.putInt("expires", 15_000);
            data.putDouble("dist", 300D);
            if (label != null) data.putString("label", label);
            AuxParticlePacket.sendTo(player, data, x, y, z);
            return true;
        }

        return false;
    }
}
