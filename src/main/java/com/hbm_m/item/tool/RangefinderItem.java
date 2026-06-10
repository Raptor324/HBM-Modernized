package com.hbm_m.item.tool;

import com.hbm_m.client.overlay.OverlayInfoToast;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RangefinderItem extends Item {

    public static final int META_POLARIZED = 1;

    private static final int MAX_RANGE = 300;
    private static final int TOAST_TICKS = 100; // 5000 ms in 1.7.10 PlayerInformPacket

    public RangefinderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            Vec3 start = player.getEyePosition(1.0F);
            Vec3 end = start.add(player.getViewVector(1.0F).scale(MAX_RANGE));
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE, player));

            if (hit.getType() == HitResult.Type.BLOCK) {
                double dist = start.distanceTo(hit.getLocation());
                String msg = ((int) (dist * 10D)) / 10D + "m";
                boolean polarized = stack.getDamageValue() == META_POLARIZED;
                int rgb = polarized ? 0xFF55FF : 0xFFFFFF;
                OverlayInfoToast.show(Component.literal(msg), TOAST_TICKS, OverlayInfoToast.ID_DETONATOR, rgb);
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        if (stack.getDamageValue() == META_POLARIZED) {
            return name.copy().withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        return name;
    }
}
