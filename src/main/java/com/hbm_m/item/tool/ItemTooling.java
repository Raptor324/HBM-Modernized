package com.hbm_m.item.tool;

import com.hbm_m.platform.ItemHooks;

import com.hbm_m.api.block.IToolable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.items.tool.ItemTooling} ({@code hand_drill}, {@code hand_drill_desh}): Handwerkzeug, das im
 * Raster bleibt und sich bei jeder erfolgreichen Werkzeug-Aktion eines Blocks ({@code IToolable.onScrew} liefert
 * true) um 1 abnutzt, sofern es Haltbarkeit hat. Die Bloecke rufen dafuer {@link #onScrewed} auf.
 */
public class ItemTooling extends ItemCraftingDegradation {

    public final IToolable.ToolType type;

    public ItemTooling(IToolable.ToolType type, int durability, Properties properties) {
        super(durability, properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Block b = world.getBlockState(pos).getBlock();

        if (b instanceof IToolable toolable && ctx.getPlayer() != null) {
            Vec3 hit = ctx.getClickLocation();
            if (toolable.onScrew(world, ctx.getPlayer(), pos, ctx.getClickedFace(), (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()), (float) (hit.z - pos.getZ()), ctx.getHand(), this.type)) {
                if (ctx.getItemInHand().getMaxDamage() > 0)
                    ItemHooks.hurtAndBreak(ctx.getItemInHand(), 1, ctx.getPlayer(), ctx.getHand());
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    /** Original: {@code if(this.getMaxDamage() > 0) stack.damageItem(1, player)} nach erfolgreichem onScrew. */
    public static void onScrewed(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof ItemTooling && stack.getMaxDamage() > 0) {
            ItemHooks.hurtAndBreak(stack, 1, player, hand);
        }
    }
}
