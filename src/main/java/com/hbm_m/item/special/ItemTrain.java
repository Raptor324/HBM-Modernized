package com.hbm_m.item.special;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.rail.IRailNTM;
import com.hbm_m.block.rail.IRailNTM.MoveContext;
import com.hbm_m.block.rail.IRailNTM.RailCheckType;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.train.EntityRailCarBase;
import com.hbm_m.util.Vec3NT;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code ItemTrain}: setzt einen Waggon auf eine {@link IRailNTM}-Schiene passender Spurweite. Das Meta-Item des
 * Originals ({@code train}) ist hier je Typ ein eigener Gegenstand ({@code train_cargo_tram}, {@code train_cargo_tram_trailer}).
 */
public class ItemTrain extends Item {

    public final EnumTrainType train;

    public ItemTrain(Properties properties, EnumTrainType train) {
        super(properties.stacksTo(1));
        this.train = train;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(@NotNull ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
    *///?}
        EnumTrainType train = this.train;

        if (train.engine != null) list.add(Component.literal(ChatFormatting.GREEN + "Engine: " + ChatFormatting.RESET + train.engine));
        list.add(Component.literal(ChatFormatting.GREEN + "Gauge: " + ChatFormatting.RESET + train.gauge));
        if (train.maxSpeed != null) list.add(Component.literal(ChatFormatting.GREEN + "Max Speed: " + ChatFormatting.RESET + train.maxSpeed));
        if (train.acceleration != null) list.add(Component.literal(ChatFormatting.GREEN + "Acceleration: " + ChatFormatting.RESET + train.acceleration));
        if (train.brakeThreshold != null) list.add(Component.literal(ChatFormatting.GREEN + "Engine Brake Threshold: " + ChatFormatting.RESET + train.brakeThreshold));
        if (train.parkingBrake != null) list.add(Component.literal(ChatFormatting.GREEN + "Parking Brake: " + ChatFormatting.RESET + train.parkingBrake));
    }

    public enum EnumTrainType {

        //                                                  Engine          Gauge               Max Speed   Accel.      Eng. Brake  Parking Brake
        CARGO_TRAM(() -> ModEntities.TRAIN_CARGO_TRAM.get(),                "Electric", "Standard Gauge", "10m/s", "0.2m/s²", "<1m/s", "Yes"),
        CARGO_TRAM_TRAILER(() -> ModEntities.TRAIN_CARGO_TRAM_TRAILER.get(), null,      "Standard Gauge", "Yes",   null,      null,    "No");

        public final Supplier<? extends EntityType<? extends EntityRailCarBase>> train;
        public final String engine;
        public final String maxSpeed;
        public final String acceleration;
        public final String brakeThreshold;
        public final String parkingBrake;
        public final String gauge;

        EnumTrainType(Supplier<? extends EntityType<? extends EntityRailCarBase>> train, String engine, String gauge, String maxSpeed, String acceleration, String brakeThreshold, String parkingBrake) {
            this.train = train;
            this.engine = engine;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.brakeThreshold = brakeThreshold;
            this.parkingBrake = parkingBrake;
            this.gauge = gauge;
        }
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {

        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        Block b = world.getBlockState(pos).getBlock();
        Player entity = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();

        if (b instanceof IRailNTM rail) {

            EntityRailCarBase train = this.train.train.get().create(world);

            if (train != null && train.getGauge() == rail.getGauge(world, x, y, z)) {

                Vec3 hit = ctx.getClickLocation();
                train.setPos(hit.x, hit.y, hit.z);
                BlockPos anchor = train.getCurrentAnchorPos();
                train.setYRot(entity != null ? entity.getYRot() : 0F);
                Vec3NT corePos = train.getRelPosAlongRail(anchor, 0, new MoveContext(RailCheckType.CORE, 0));
                if (corePos != null) {
                    train.setPos(corePos.xCoord, corePos.yCoord, corePos.zCoord);
                    Vec3NT frontPos = train.getRelPosAlongRail(anchor, train.getLengthSpan(), new MoveContext(RailCheckType.FRONT, train.getCollisionSpan() - train.getLengthSpan()));
                    Vec3NT backPos = train.getRelPosAlongRail(anchor, -train.getLengthSpan(), new MoveContext(RailCheckType.BACK, train.getCollisionSpan() - train.getLengthSpan()));
                    if (frontPos != null && backPos != null) {
                        if (!world.isClientSide) {
                            train.setYRot(EntityRailCarBase.generateYaw(frontPos, backPos));
                            world.addFreshEntity(train);
                        }
                        stack.shrink(1);
                        return InteractionResult.sidedSuccess(world.isClientSide);
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }
}
