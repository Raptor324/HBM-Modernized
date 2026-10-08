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

        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x, i, z))
                direct = true;
        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x + 5, i, z))
                oil = true;
        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x - 5, i, z))
                oil = true;
        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x, i, z + 5))
                oil = true;
        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x, i, z - 5))
                oil = true;

        for (int i = y + 15; i > 10; i--)
            if (isOil(world, x + 10, i, z))
                oil = true;
        for (int i = y + 15; i > 10; i--)
            if (isOil(world, x - 10, i, z))
                oil = true;
        for (int i = y + 15; i > 10; i--)
            if (isOil(world, x, i, z + 10))
                oil = true;
        for (int i = y + 15; i > 10; i--)
            if (isOil(world, x, i, z - 10))
                oil = true;

        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x + 5, i, z + 5))
                oil = true;
        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x - 5, i, z + 5))
                oil = true;
        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x + 5, i, z - 5))
                oil = true;
        for (int i = y + 15; i > 5; i--)
            if (isOil(world, x - 5, i, z - 5))
                oil = true;

        if (direct)
            oil = true;

        if (!world.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {

            if (direct) {
                inform(sp, this.getDescriptionId() + ".bullseye", ChatFormatting.DARK_GREEN, 0x00AA00);
            } else if (oil) {
                inform(sp, this.getDescriptionId() + ".detected", ChatFormatting.GOLD, 0xFFAA00);
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

    private static boolean isOil(Level world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.ORE_OIL.get());
    }
}
