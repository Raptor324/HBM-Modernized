package com.hbm_m.block.machines;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RailGeneric} ({@code rail_wood}, {@code rail_narrow}, {@code rail_highspeed}) und {@code RailBooster}.
 * Im Original liefert {@code isFlexibleRail} {@code !isPowered()} - also sind alle Schienen kurvenfaehig;
 * {@code flexible} steuert nur die Kurventextur und den Tooltip.
 */
public class RailGenericBlock extends RailBlock {

    protected static final float baseSpeed = 0.4F;
    protected final float maxSpeed;
    protected final boolean flexible;
    protected final boolean booster;

    public RailGenericBlock(Properties p, float maxSpeed, boolean flexible, boolean booster) {
        super(p);
        this.maxSpeed = maxSpeed;
        this.flexible = flexible;
        this.booster = booster;
    }

    //? if forge {
    @Override
    public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return maxSpeed;
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        if (booster) {
            Vec3 m = cart.getDeltaMovement();
            cart.setDeltaMovement(m.x * 1.15F, m.y * 1.15F, m.z * 1.15F);
        }
    }
    //?}

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        float speed = this.maxSpeed / baseSpeed;
        if (speed != 1F) list.add(Component.literal("Speed: " + ((int) (speed * 100)) + "%").withStyle(speed > 1 ? ChatFormatting.BLUE : ChatFormatting.RED));
        if (!flexible) list.add(Component.literal("Cannot be used for turns!").withStyle(ChatFormatting.RED));
    }
}
