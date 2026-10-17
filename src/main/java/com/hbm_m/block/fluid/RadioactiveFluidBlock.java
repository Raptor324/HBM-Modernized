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
        // E14 (Fork, nur mit Space-Addon): Space SchrabidicBlock - kein Spinnennetz, bremst nur den Fall, 10 Saeureschaden
        if (schrabidic && com.hbm_m.api.space.SpaceApi.forkActive()) {
            if (entity instanceof LivingEntity living) {
                net.minecraft.world.phys.Vec3 m = entity.getDeltaMovement();
                if (m.y < -0.2) entity.setDeltaMovement(m.x, m.y * 0.5, m.z);
                if (!level.isClientSide) entity.hurt(com.hbm_m.damagesource.ModDamageSources.acid(level), 10F);
                ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 1.0F);
            }
            return;
        }
        setInWeb(entity, state);
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 1.0F);
        }
    }

    @Override
    protected void onNeighborChange(Level level, BlockPos pos, BlockState state) {
        // E14: im Space-Fork ist die Nachbarreaktion auskommentiert, stattdessen getReaction im updateTick
        if (schrabidic && com.hbm_m.api.space.SpaceApi.forkActive()) return;
        for (Direction d : Direction.values()) {
            BlockState other = level.getBlockState(pos.relative(d));
            if (other.getBlock() != this && !other.getFluidState().isEmpty()) {
                level.setBlockAndUpdate(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
                if (schrabidic) break;
            }
        }
    }

    /** E14 (Fork, nur mit Space-Addon): Space {@code SchrabidicBlock.updateTick} - alle sechs Nachbarn zersetzen. */
    @Override
    public void onFluidTick(Level level, BlockPos pos, BlockState state) {
        super.onFluidTick(level, pos, state);
        if (!schrabidic || level.isClientSide || !com.hbm_m.api.space.SpaceApi.forkActive()) return;
        for (Direction d : Direction.values()) getReaction(level, pos.relative(d));
    }

    /**
     * 1:1 Space {@code SchrabidicBlock.getReaction}. Klaenge/Partikel des Originals entfallen: sie liefen dort im
     * serverseitigen {@code updateTick} ueber Client-Methoden und waren nie zu sehen. 1.7-Materialien: Eisen ~
     * Metallklang, Glas ~ Glasklang; {@code getExplosionResistance(null)} = 1.20-Explosionsfestigkeit.
     */
    private void getReaction(Level world, BlockPos p) {
        BlockState st = world.getBlockState(p);
        net.minecraft.world.level.block.Block b = st.getBlock();
        RandomSource rand = world.random;
        if (!st.getFluidState().isEmpty() && b != this) {
            world.setBlockAndUpdate(p, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState());
        } else if (st.is(net.minecraft.tags.BlockTags.LOGS) && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, ModBlocks.WASTE_LOG.get().defaultBlockState());
        } else if (st.is(net.minecraft.tags.BlockTags.PLANKS) && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, ModBlocks.WASTE_PLANKS.get().defaultBlockState());
        } else if ((st.is(net.minecraft.tags.BlockTags.LEAVES) || st.getSoundType() == net.minecraft.world.level.block.SoundType.METAL
                || (st.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS && b != ModBlocks.REINFORCED_GLASS.get())) && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        } else if ((b == net.minecraft.world.level.block.Blocks.STONE || b == net.minecraft.world.level.block.Blocks.STONE_BRICK_STAIRS
                || b == net.minecraft.world.level.block.Blocks.STONE_BRICKS || b == net.minecraft.world.level.block.Blocks.MOSSY_STONE_BRICKS
                || b == net.minecraft.world.level.block.Blocks.CRACKED_STONE_BRICKS || b == net.minecraft.world.level.block.Blocks.CHISELED_STONE_BRICKS
                || b == net.minecraft.world.level.block.Blocks.STONE_SLAB || b == net.minecraft.world.level.block.Blocks.SMOOTH_STONE_SLAB) && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState());
        } else if (b == net.minecraft.world.level.block.Blocks.COBBLESTONE && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.GRAVEL.defaultBlockState());
        } else if (b == net.minecraft.world.level.block.Blocks.GRAVEL && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.CLAY.defaultBlockState());
        } else if ((b == net.minecraft.world.level.block.Blocks.OBSIDIAN || b == ModBlocks.BRICK_OBSIDIAN.get() || b == ModBlocks.BRICK_OBSIDIAN_STAIRS.get()) && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, ModBlocks.GRAVEL_OBSIDIAN.get().defaultBlockState());
        } else if ((b == net.minecraft.world.level.block.Blocks.SANDSTONE || b == net.minecraft.world.level.block.Blocks.CHISELED_SANDSTONE
                || b == net.minecraft.world.level.block.Blocks.CUT_SANDSTONE || b == net.minecraft.world.level.block.Blocks.END_STONE) && rand.nextInt(4) == 0) {
            world.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.SAND.defaultBlockState());
        } else if (!st.isAir() && b.getExplosionResistance() < 1.2F && b != ModBlocks.BLOCK_POLYMER.get() && b != ModBlocks.GRAVEL_OBSIDIAN.get()
                && b != net.minecraft.world.level.block.Blocks.SAND && b != net.minecraft.world.level.block.Blocks.CLAY) {
            world.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
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
