package com.hbm_m.item.tool;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.TurretBaseBlockEntity;
import com.hbm_m.blockentity.machines.TurretStats;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.multiblock.MultiblockInteractionHelper;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code ItemDesignatorArtyRange}: Rechtsklick auf ein Artilleriegeschuetz (Arty/HIMARS) verknuepft es,
 * Rechtsklick in die Luft reiht den anvisierten Block (bis 500 m) in dessen Zielwarteschlange ein.
 */
public class ItemDesignatorArtyRange extends Item implements ITooltipProvider {

    public ItemDesignatorArtyRange(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (!PlatformHooks.hasItemTag(stack)) {
            list.add(Component.literal("No turret linked!").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("Linked to " + PlatformHooks.getInt(stack, "x") + ", " + PlatformHooks.getInt(stack, "y") + ", " + PlatformHooks.getInt(stack, "z")).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** {@code te instanceof TileEntityTurretBaseArtillery}: Arty und HIMARS. */
    @Nullable
    private static TurretBaseBlockEntity arty(BlockEntity te) {
        if (te instanceof TurretBaseBlockEntity turret && (turret.getStats() == TurretStats.ARTY || turret.getStats() == TurretStats.HIMARS)) return turret;
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        // BlockDummyable.findCore
        BlockPos core = MultiblockInteractionHelper.resolveControllerPos(world, ctx.getClickedPos());
        TurretBaseBlockEntity te = arty(world.getBlockEntity(core));

        if (te != null) {

            if (world.isClientSide)
                return InteractionResult.SUCCESS;

            ItemStack stack = ctx.getItemInHand();
            PlatformHooks.putInt(stack, "x", core.getX());
            PlatformHooks.putInt(stack, "y", core.getY());
            PlatformHooks.putInt(stack, "z", core.getZ());
            Player player = ctx.getPlayer();
            if (player != null) world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.techBleep"), SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!PlatformHooks.hasItemTag(stack))
            return InteractionResultHolder.pass(stack);

        // Library.rayTrace(player, 500, 1)
        HitResult hit = player.pick(500, 1F, false);
        BlockPos pos = BlockPos.containing(hit.getLocation());
        if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) pos = bhr.getBlockPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (!world.isClientSide) {
            TurretBaseBlockEntity arty = arty(world.getBlockEntity(new BlockPos(PlatformHooks.getInt(stack, "x"), PlatformHooks.getInt(stack, "y"), PlatformHooks.getInt(stack, "z"))));

            if (arty != null) {
                arty.enqueueTarget(x + 0.5, y + 0.5, z + 0.5);
                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.techBoop"), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }

        return InteractionResultHolder.pass(stack);
    }
}
