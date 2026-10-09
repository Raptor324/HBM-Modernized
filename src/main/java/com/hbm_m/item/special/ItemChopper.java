package com.hbm_m.item.special;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.EntityUFO;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code ItemChopper}: Spawn-Gegenstaende fuer Kampfhubschrauber ({@code chopper}), Balls-O-Tron Prime
 * ({@code spawn_worm}), Marsianer-Schiff ({@code spawn_ufo}, 35 Bloecke hoeher, Scan-Abklingzeit 100) und
 * Ente ({@code spawn_duck}). Wie ein Spawn-Ei auf Bloecke oder in Fluessigkeiten.
 */
public class ItemChopper extends Item implements ITooltipProvider {

    public ItemChopper(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockPos pos = ctx.getClickedPos();
            Direction side = ctx.getClickedFace();
            var state = world.getBlockState(pos);

            BlockPos p = pos.relative(side);
            double offset = 0.0D;

            // Original: renderType 11 (Zaun) auf der Oberseite
            if (side == Direction.UP && state.getBlock() instanceof net.minecraft.world.level.block.FenceBlock)
                offset = 0.5D;

            ItemStack stack = ctx.getItemInHand();
            Entity entity = spawnCreature(world, p.getX() + 0.5D, p.getY() + offset, p.getZ() + 0.5D);

            if (entity != null) {
                if (entity instanceof Mob mob && StackNbt.hasCustomName(stack)) {
                    mob.setCustomName(stack.getHoverName());
                }

                if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide) {
            return InteractionResultHolder.pass(stack);

        } else {
            BlockHitResult hit = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();

                if (!world.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
                    return InteractionResultHolder.pass(stack);
                }

                if (world.getBlockState(pos).getBlock() instanceof LiquidBlock) {
                    Entity entity = spawnCreature(world, pos.getX(), pos.getY(), pos.getZ());

                    if (entity != null) {
                        if (entity instanceof Mob mob && StackNbt.hasCustomName(stack)) {
                            mob.setCustomName(stack.getHoverName());
                        }

                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                    }
                }
            }

            return InteractionResultHolder.pass(stack);
        }
    }

    @Nullable
    public Entity spawnCreature(Level world, double x, double y, double z) {
        Mob entity = null;

        if (this == ModItems.CHOPPER.get())
            entity = ModEntities.HUNTER_CHOPPER.get().create(world);

        if (this == ModItems.SPAWN_WORM.get())
            entity = ModEntities.BOT_PRIME_HEAD.get().create(world);

        if (this == ModItems.SPAWN_UFO.get()) {
            EntityUFO ufo = ModEntities.UFO.get().create(world);
            if (ufo != null) ufo.scanCooldown = 100;
            entity = ufo;
            y += 35;
        }

        if (this == ModItems.SPAWN_DUCK.get())
            entity = ModEntities.DUCK.get().create(world);

        if (entity != null) {
            entity.moveTo(x, y, z, Mth.wrapDegrees(world.random.nextFloat() * 360.0F), 0.0F);
            entity.yHeadRot = entity.getYRot();
            entity.yBodyRot = entity.getYRot();
            if (world instanceof ServerLevel sl)
                //? if < 1.21.1 {
                entity.finalizeSpawn(sl, sl.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWN_EGG, null, null);
                //?} else {
                /*entity.finalizeSpawn(sl, sl.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWN_EGG, null);
                *///?}
            world.addFreshEntity(entity);
        }

        return entity;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        if (this == ModItems.SPAWN_WORM.get()) {
            list.add(Component.literal("Without a player in survival mode"));
            list.add(Component.literal("to target, he struggles around a lot."));
            list.add(Component.literal(""));
            list.add(Component.literal("He's doing his best so please show him"));
            list.add(Component.literal("some consideration."));
        }
    }
}
