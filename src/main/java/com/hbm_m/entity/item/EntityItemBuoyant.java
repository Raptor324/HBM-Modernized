package com.hbm_m.entity.item;

import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

/** 1:1 {@code com.hbm.entity.item.EntityItemBuoyant}: Gegenstand, der in stehendem Wasser nach oben treibt. */
public class EntityItemBuoyant extends ItemEntity {

    public EntityItemBuoyant(EntityType<? extends EntityItemBuoyant> type, Level world) {
        super(type, world);
    }

    public EntityItemBuoyant(Level world, double x, double y, double z, ItemStack stack) {
        this(ModEntities.ITEM_BUOYANT.get(), world);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.setYRot(this.random.nextFloat() * 360.0F);
        this.setDeltaMovement(this.random.nextDouble() * 0.2D - 0.1D, 0.2D, this.random.nextDouble() * 0.2D - 0.1D);
    }

    @Override
    public void tick() {
        BlockPos pos = new BlockPos(Mth.floor(getX()), Mth.floor(getY() - 0.0625), Mth.floor(getZ()));
        FluidState fluid = this.level().getFluidState(pos);
        // Material.water && meta < 8: Wasser, das nicht faellt
        if (fluid.is(FluidTags.WATER) && !fluid.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.045D, 0));
        }
        super.tick();
    }
}
