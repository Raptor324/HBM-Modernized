package com.hbm_m.item.scanners;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.hbm_m.block.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OilDetectorItem extends Item implements ITooltipProvider {

    public OilDetectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }
    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, @Nullable List<Component> tooltip, TooltipFlag flag) {
        if (tooltip == null) return;
        // Original ItemOilDetector.addInformation: .desc1 / .desc2
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc2").withStyle(ChatFormatting.GRAY));
    }

    /** 1:1 Original onItemRightClick: Saeulen in 5 und 10 Bloecken Abstand plus die eigene Saeule abfragen. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        boolean oil = false;
        boolean direct = false;
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();

        // NTM Space: die Meldung haengt den Lagerstaettentyp an ("", "_gas", "_brine"); HBM-ore_oil = "".
        String directType = column(world, x, y, z, 5);
        if (directType != null) direct = true;

        String nearType = null;
        int[][] near = { {5, 0, 5}, {-5, 0, 5}, {0, 5, 5}, {0, -5, 5}, {10, 0, 10}, {-10, 0, 10}, {0, 10, 10}, {0, -10, 10},
                {5, 5, 5}, {-5, 5, 5}, {5, -5, 5}, {-5, -5, 5} };
        for (int[] n : near) {
            String t = column(world, x + n[0], y, z + n[1], n[2]);
            if (t != null) {
                oil = true;
                if (nearType == null) nearType = t;
            }
        }

        if (direct)
            oil = true;

        if (!world.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {

            if (direct) {
                inform(sp, this.getDescriptionId() + ".bullseye" + directType, ChatFormatting.DARK_GREEN, 0x00AA00);
            } else if (oil) {
                inform(sp, this.getDescriptionId() + ".detected" + nearType, ChatFormatting.GOLD, 0xFFAA00);
            } else {
                inform(sp, this.getDescriptionId() + ".noOil", ChatFormatting.RED, 0xFF5555);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BLEEP.get(), player.getSoundSource(), 1.0F, 1.0F);

        player.swing(hand);

        return InteractionResultHolder.pass(stack);
    }

    /** Original {@code PlayerInformPacket(..., ID_DETONATOR)}. */
    private static void inform(net.minecraft.server.level.ServerPlayer sp, String key, ChatFormatting color, int rgb) {
        com.hbm_m.network.InfoToastPacket.sendTo(sp, Component.translatable(key).withStyle(color), 60, com.hbm_m.client.overlay.OverlayInfoToast.ID_DETONATOR, rgb);
    }

    /** Saeule von y+15 bis ueber {@code bottom} abwaerts: erster Lagerstaettentyp-Suffix, sonst null. */
    @Nullable
    private static String column(Level world, int x, int y, int z, int bottom) {
        for (int i = y + 15; i > bottom; i--) {
            String t = reserveType(world, x, i, z);
            if (t != null) return t;
        }
        return null;
    }

    /** HBM {@code ore_oil} -> "" ; Addon-Lagerstaette (W-R8B-2, Original {@code instanceof BlockOreFluid}) -> Suffix. */
    @Nullable
    private static String reserveType(Level world, int x, int y, int z) {
        Block b = world.getBlockState(new BlockPos(x, y, z)).getBlock();
        if (b == ModBlocks.ORE_OIL.get()) return "";
        com.hbm_m.api.space.SpaceResources.FluidDepositBlock dep = com.hbm_m.api.space.SpaceResources.fluidDepositBlock(b);
        return dep != null ? dep.getUnlocalizedReserveType() : null;
    }
}
