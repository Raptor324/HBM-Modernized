package com.hbm_m.block.fluid;

import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.particle.helper.ParticleEffectClient;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 1:1-Port von {@code ToxicBlock} (Atommuell-Bruehe) und {@code SchrabidicBlock} (Schrabidiumsaeure):
 * beide bremsen wie Spinnennetz, bestrahlen mit 1 RAD pro Beruehrung (CREATIVE-Kontamination) und
 * erstarren zu {@code sellafield_slaked}, sobald eine fremde Fluessigkeit angrenzt. Die Saeure
 * steigt zusaetzlich als tuerkiser Nebel ({@code schrabfog}) auf.
 */
public class RadioactiveFluidBlock extends HbmFluidBlock {

    private final boolean schrabidic;

    public RadioactiveFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties, boolean schrabidic) {
        super(fluid, properties);
        this.schrabidic = schrabidic;
    }

    @Override
    public Boolean canDisplace(BlockGetter level, BlockPos pos, BlockState target) {
        if (!target.getFluidState().isEmpty()) return false;
        return null;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        setInWeb(entity, state);
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 1.0F);
        }
    }

    @Override
    protected void onNeighborChange(Level level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            BlockState other = level.getBlockState(pos.relative(d));
            if (other.getBlock() != this && !other.getFluidState().isEmpty()) {
                level.setBlockAndUpdate(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
                if (schrabidic) break;
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);
        if (!schrabidic) return;
        double ix = pos.getX() + 0.5F + rand.nextDouble() * 2 - 1D;
        double iy = pos.getY() + 0.5F + rand.nextDouble() * 2 - 1D;
        double iz = pos.getZ() + 0.5F + rand.nextDouble() * 2 - 1D;
        CompoundTag data = new CompoundTag();
        data.putString("type", "schrabfog");
        data.putDouble("posX", ix);
        data.putDouble("posY", iy);
        data.putDouble("posZ", iz);
        ParticleEffectClient.effectNTNow(data);
    }
}
