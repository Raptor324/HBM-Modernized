package com.hbm_m.blockentity.bomb;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.VolcanoBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.projectile.EntityShrapnel;
import com.hbm_m.explosion.ExplosionNT;
import com.hbm_m.explosion.ExplosionNT.ExAttrib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1:1 {@code BlockVolcano.TileEntityVolcanoCore}: alle 10 Ticks Magmakanal (zwei Lavaexplosionen), Lava anheben,
 * Magmakammer/Oberflaeche schmelzen (Schildvulkan), Lavabrocken spucken, Rauch, Lavakugel 3x3x3. Nach der
 * Artrate waechst der Kern einen Block hoch oder erlischt zu Lava.
 */
public class VolcanoCoreBlockEntity extends BaseHbmBlockEntity {

    private static final List<ExAttrib> VOLCANO = Arrays.asList(ExAttrib.NODROP, ExAttrib.LAVA_V, ExAttrib.NOSOUND, ExAttrib.ALLMOD, ExAttrib.NOHURT);
    private static final List<ExAttrib> VOLCANO_RAD = Arrays.asList(ExAttrib.NODROP, ExAttrib.LAVA_R, ExAttrib.NOSOUND, ExAttrib.ALLMOD, ExAttrib.NOHURT);

    public int volcanoTimer;

    public VolcanoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOLCANO_CORE.get(), pos, state);
    }

    private int meta() { return getBlockState().getValue(VolcanoBlock.MODE); }
    private boolean isRadioactive() { return getBlockState().getBlock() instanceof VolcanoBlock v && v.radioactive; }
    private Block getLava() { return isRadioactive() ? ModBlocks.RAD_LAVA_BLOCK.get() : ModBlocks.VOLCANIC_LAVA_BLOCK.get(); }
    private List<ExAttrib> getExpAttrb() { return isRadioactive() ? VOLCANO_RAD : VOLCANO; }

    public static void serverTick(Level world, BlockPos pos, BlockState state, VolcanoCoreBlockEntity te) {
        te.volcanoTimer++;
        int meta = state.getValue(VolcanoBlock.MODE);

        if (te.volcanoTimer % 10 == 0) {
            if (meta != VolcanoBlock.META_SMOLDERING) {
                te.blastMagmaChannel();
                te.raiseMagma();
            }
            double chamber = meta == VolcanoBlock.META_SMOLDERING ? 15 : 0;
            if (chamber > 0) te.blastMagmaChamber(chamber);
            if (meta == VolcanoBlock.META_SMOLDERING) te.meltSurface(50, 50D, 10D);
            if (meta != VolcanoBlock.META_SMOLDERING) {
                te.spawnBlobs();
                te.spawnSmoke();
            }
            te.surroundLava();
        }

        if (te.volcanoTimer >= te.getUpdateRate()) {
            te.volcanoTimer = 0;
            if (VolcanoBlock.isGrowing(meta) && pos.getY() < 200) {
                world.setBlock(pos.above(), state, 3);
                world.setBlock(pos, te.getLava().defaultBlockState(), 3);
            } else if (VolcanoBlock.isExtinguishing(meta)) {
                world.setBlock(pos, te.getLava().defaultBlockState(), 3);
            }
        }
    }

    private int getUpdateRate() {
        return switch (meta()) {
            case VolcanoBlock.META_STATIC_EXTINGUISHING -> 60 * 60 * 20;
            case VolcanoBlock.META_GROWING_ACTIVE, VolcanoBlock.META_GROWING_EXTINGUISHING -> 60 * 60 * 20 / 250;
            default -> 10;
        };
    }

    private void blastMagmaChannel() {
        var r = level.random;
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        new ExplosionNT(level, null, x + 0.5, y + r.nextInt(15) + 1.5, z + 0.5, 7).addAllAttrib(getExpAttrb()).explode();
        new ExplosionNT(level, null, x + 0.5 + r.nextGaussian() * 3, r.nextInt(y + 1), z + 0.5 + r.nextGaussian() * 3, 10).addAllAttrib(getExpAttrb()).explode();
    }

    private void blastMagmaChamber(double size) {
        var r = level.random;
        for (int i = 0; i < 2; i++) {
            double dist = size / (double) (i + 1);
            new ExplosionNT(level, null, worldPosition.getX() + 0.5 + r.nextGaussian() * dist, worldPosition.getY() + 0.5 + r.nextGaussian() * dist,
                    worldPosition.getZ() + 0.5 + r.nextGaussian() * dist, 7).addAllAttrib(getExpAttrb()).explode();
        }
    }

    private void meltSurface(int count, double radius, double depth) {
        var r = level.random;
        float obsidian = Blocks.OBSIDIAN.getExplosionResistance();
        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(worldPosition.getX() + r.nextGaussian() * radius);
            int z = (int) Math.floor(worldPosition.getZ() + r.nextGaussian() * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1 - (int) Math.floor(Math.abs(r.nextGaussian() * depth));
            BlockPos p = new BlockPos(x, y, z);
            BlockState b = level.getBlockState(p);
            if (!b.isAir() && b.getBlock().getExplosionResistance() < obsidian) {
                level.setBlockAndUpdate(p, b.isRedstoneConductor(level, p) ? getLava().defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
    }

    private void raiseMagma() {
        var r = level.random;
        BlockPos p = new BlockPos(worldPosition.getX() - 10 + r.nextInt(21), worldPosition.getY() + r.nextInt(11), worldPosition.getZ() - 10 + r.nextInt(21));
        if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(getLava()))
            level.setBlockAndUpdate(p, getLava().defaultBlockState());
    }

    private void surroundLava() {
        for (int i = -1; i <= 1; i++)
            for (int j = -1; j <= 1; j++)
                for (int k = -1; k <= 1; k++)
                    if (i != 0 || j != 0 || k != 0) level.setBlockAndUpdate(worldPosition.offset(i, j, k), getLava().defaultBlockState());
    }

    private void spawnBlobs() {
        var r = level.random;
        for (int i = 0; i < 3; i++) {
            EntityShrapnel frag = new EntityShrapnel(level);
            frag.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 0.0F, 0.0F);
            frag.setDeltaMovement(r.nextGaussian() * 0.2D, 1D + r.nextDouble(), r.nextGaussian() * 0.2D);
            if (isRadioactive()) frag.setRadVolcano(true);
            else frag.setVolcano(true);
            level.addFreshEntity(frag);
        }
    }

    private void spawnSmoke() {
        CompoundTag d = new CompoundTag();
        d.putString("type", "vanillaExt");
        d.putString("mode", "volcano");
        com.hbm_m.particle.helper.IParticleCreator.sendPacket((ServerLevel) level, worldPosition.getX() + 0.5, worldPosition.getY() + 10, worldPosition.getZ() + 0.5, 250, d);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        volcanoTimer = nbt.getInt("timer");
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putInt("timer", volcanoTimer);
    }
}
