package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.api.block.IPileNeutronReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.pile.GraphitePileBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.GeneralConfig;
import com.hbm_m.handler.neutron.PileNeutronHandler;
import com.hbm_m.particle.helper.IParticleCreator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Blockentities der Chicago Pile MK1 ({@code com.hbm.tileentity.machine.pile}, LEGACY). */
public final class GraphitePileBlockEntities {

    private GraphitePileBlockEntities() {}

    /** Tick-Einstieg fuer den gemeinsamen Ticker von {@code DrilledTE}. */
    public interface Ticking {
        void serverTick();
    }

    /** 1:1 {@code TileEntityPileBase}: wirft Neutronenstrahlen in eine Zufallsrichtung. */
    public abstract static class Base extends BlockEntity implements Ticking {

        protected Base(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

        protected int meta() { return GraphitePileBlocks.meta(getBlockState()); }

        protected void castRay(int flux) {
            if (flux == 0) return;
            // Vec3(1, 0, 0).rotateAroundZ/Y/X (1.7.10-Drehformeln)
            double x = 1, y = 0, z = 0;
            float f = (float) (Math.PI * 2D * level.random.nextDouble());
            double c = Mth.cos(f), s = Mth.sin(f);
            double nx = x * c + y * s, ny = y * c - x * s;
            x = nx; y = ny;
            f = (float) (Math.PI * 2D * level.random.nextDouble());
            c = Mth.cos(f); s = Mth.sin(f);
            nx = x * c + z * s; double nz = z * c - x * s;
            x = nx; z = nz;
            f = (float) (Math.PI * 2D * level.random.nextDouble());
            c = Mth.cos(f); s = Mth.sin(f);
            ny = y * c + z * s; nz = z * c - y * s;
            y = ny; z = nz;
            PileNeutronHandler.addStream(level, this, new Vec3(x, y, z), flux);
        }
    }

    // ================================================================================================

    /** 1:1 {@code TileEntityPileFuel}: Uranstab - Hitze, Abbrand, Pu-239-Anreicherung, Ueberhitzung sprengt in dichtes Radon. */
    public static class Fuel extends Base implements IPileNeutronReceiver {

        public int heat;
        public static final int maxHeat = 1000;
        public int neutrons;
        public int lastNeutrons;
        public int progress;
        public static final int maxProgress = GeneralConfig.enable528 ? 75000 : 50000;

        public Fuel(BlockPos pos, BlockState state) { super(ModBlockEntities.GRAPHITE_PILE_FUEL.get(), pos, state); }

        @Override
        public void serverTick() {
            dissipateHeat();
            checkRedstone(react());
            transmute();
            if (isRemoved()) return;

            if (this.heat >= maxHeat) {
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 4, true, Level.ExplosionInteraction.BLOCK);
                level.setBlockAndUpdate(worldPosition, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState());
                return;
            }

            if (level.random.nextFloat() * 2F <= this.heat / (float) maxHeat && level instanceof ServerLevel sl) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "vanillaExt");
                data.putString("mode", "smoke");
                data.putDouble("mY", 0.05);
                IParticleCreator.sendPacket(sl, worldPosition.getX() + 0.25 + level.random.nextDouble() * 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.25 + level.random.nextDouble() * 0.5, 20, data);
            }

            if (this.progress >= maxProgress) {
                level.setBlock(worldPosition, ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState().setValue(GraphitePileBlocks.META, meta() & 7), 3);
            }
        }

        private void dissipateHeat() {
            this.heat = (int) (this.heat - ((meta() & 4) == 4 ? heat * 0.065 : heat * 0.05));
        }

        private int react() {
            int reaction = (int) (this.neutrons * (1D - ((double) this.heat / (double) maxHeat) * 0.5D));
            this.lastNeutrons = this.neutrons;
            this.neutrons = 0;
            int lastProgress = this.progress;
            this.progress += reaction;
            if (reaction <= 0) return lastProgress;
            this.heat += reaction;
            for (int i = 0; i < 12; i++) this.castRay((int) Math.max(reaction * 0.25, 1));
            return lastProgress;
        }

        private void checkRedstone(int lastProgress) {
            int lastLevel = Mth.clamp((lastProgress * 16) / maxProgress, 0, 15);
            int newLevel = Mth.clamp((progress * 16) / maxProgress, 0, 15);
            if (lastLevel != newLevel) level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }

        private void transmute() {
            int meta = meta();
            if ((meta & 8) == 8) {
                if (this.progress < maxProgress - 1000) this.progress = maxProgress - 1000;
            } else if (this.progress >= maxProgress - 1000) {
                level.setBlock(worldPosition, getBlockState().setValue(GraphitePileBlocks.META, meta | 8), 3);
            }
        }

        @Override public void receiveNeutrons(int n) { this.neutrons += n; }

        @Override
        //? if < 1.21.1 {
        public void load(CompoundTag nbt) {
        //?} else {
        /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.load(nbt);
            //?} else {
            /*super.loadAdditional(nbt, registries);
            *///?}
            this.heat = nbt.getInt("heat");
            this.progress = nbt.getInt("progress");
            this.neutrons = nbt.getInt("neutrons");
        }

        @Override
        //? if < 1.21.1 {
        protected void saveAdditional(CompoundTag nbt) {
        //?} else {
        /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.saveAdditional(nbt);
            //?} else {
            /*super.saveAdditional(nbt, registries);
            *///?}
            nbt.putInt("heat", this.heat);
            nbt.putInt("progress", this.progress);
            nbt.putInt("neutrons", this.neutrons);
        }
    }

    /** 1:1 {@code TileEntityPileBreedingFuel}: Lithium wird zu Tritium. */
    public static class BreedingFuel extends Base implements IPileNeutronReceiver {

        public int neutrons;
        public int lastNeutrons;
        public int progress;
        public static final int maxProgress = GeneralConfig.enable528 ? 50000 : 30000;

        public BreedingFuel(BlockPos pos, BlockState state) { super(ModBlockEntities.GRAPHITE_PILE_BREEDING.get(), pos, state); }

        @Override
        public void serverTick() {
            react();
            if (this.progress >= maxProgress) {
                level.setBlock(worldPosition, ModBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState().setValue(GraphitePileBlocks.META, meta()), 3);
            }
        }

        private void react() {
            this.lastNeutrons = this.neutrons;
            this.progress += this.neutrons;
            this.neutrons = 0;
            if (lastNeutrons <= 0) return;
            for (int i = 0; i < 2; i++) this.castRay(1);
        }

        @Override public void receiveNeutrons(int n) { this.neutrons += n; }

        @Override
        //? if < 1.21.1 {
        public void load(CompoundTag nbt) {
        //?} else {
        /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.load(nbt);
            //?} else {
            /*super.loadAdditional(nbt, registries);
            *///?}
            this.progress = nbt.getInt("progress");
            this.neutrons = nbt.getInt("neutrons");
        }

        @Override
        //? if < 1.21.1 {
        protected void saveAdditional(CompoundTag nbt) {
        //?} else {
        /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.saveAdditional(nbt);
            //?} else {
            /*super.saveAdditional(nbt, registries);
            *///?}
            nbt.putInt("progress", this.progress);
            nbt.putInt("neutrons", this.neutrons);
        }
    }

    /** 1:1 {@code TileEntityPileNeutronDetector} (kein PileBase - wirft selbst keine Strahlen). */
    public static class Detector extends BlockEntity implements IPileNeutronReceiver, Ticking {

        public int lastNeutrons;
        public int neutrons;
        public int maxNeutrons = 10;

        public Detector(BlockPos pos, BlockState state) { super(ModBlockEntities.GRAPHITE_PILE_DETECTOR.get(), pos, state); }

        @Override
        public void serverTick() {
            int meta = GraphitePileBlocks.meta(getBlockState());
            if (getBlockState().getBlock() instanceof GraphitePileBlocks.Detector d) {
                if (this.neutrons >= this.maxNeutrons && (meta & 8) > 0) d.triggerRods(level, worldPosition);
                else if (this.neutrons < this.maxNeutrons && this.lastNeutrons < this.maxNeutrons && (meta & 8) == 0) d.triggerRods(level, worldPosition);
            }
            this.lastNeutrons = this.neutrons;
            this.neutrons = 0;
        }

        @Override public void receiveNeutrons(int n) { this.neutrons += n; }

        @Override
        //? if < 1.21.1 {
        public void load(CompoundTag nbt) {
        //?} else {
        /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.load(nbt);
            //?} else {
            /*super.loadAdditional(nbt, registries);
            *///?}
            this.maxNeutrons = nbt.getInt("maxNeutrons");
        }

        @Override
        //? if < 1.21.1 {
        protected void saveAdditional(CompoundTag nbt) {
        //?} else {
        /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.saveAdditional(nbt);
            //?} else {
            /*super.saveAdditional(nbt, registries);
            *///?}
            nbt.putInt("maxNeutrons", this.maxNeutrons);
        }
    }

    /** 1:1 {@code TileEntityPileSource}: zwoelf Strahlen je Tick (Plutonium 2, Quelle 1 Neutron). */
    public static class Source extends Base {

        public Source(BlockPos pos, BlockState state) { super(ModBlockEntities.GRAPHITE_PILE_SOURCE.get(), pos, state); }

        @Override
        public void serverTick() {
            int n = getBlockState().is(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()) ? 1 : 2;
            for (int i = 0; i < 12; i++) this.castRay(n);
        }
    }
}
