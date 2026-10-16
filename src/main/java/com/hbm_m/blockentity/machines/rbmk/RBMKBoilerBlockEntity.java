package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.RBMKBoilerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 port of {@code TileEntityRBMKBoiler} - the steam channel.
 *
 * <p>The steam tank is a real typed tank whose fluid changes with the compression stage, exactly
 * as the original's {@code steam.setTankType(Fluids.HOTSTEAM)} does. It used to be a plain
 * untyped tank plus a loose {@code steamGrade} integer, which meant nothing downstream could tell
 * ordinary steam from ultra-hot steam.</p>
 */
public class RBMKBoilerBlockEntity extends RBMKColumnBlockEntity
        implements MenuProvider, com.hbm_m.api.fluids.IFluidStandardTransceiverMK2 {

    public final FluidTank waterTank;
    public final FluidTank steamTank;
    public int lastConsumption = 0;
    public int lastOutput      = 0;
    /** {@code ventDelay}: throttles the overpressure vent effect to one burst every 20-30 ticks. */
    private int ventDelay = 0;

    private static final double[] HEAT_THRESHOLD = { 100, 300, 450, 600 };
    private static final double[] STEAM_FACTOR   = { 1, 10, 100, 1000 };

    public RBMKBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_BOILER_BE.get(), pos, state);
        waterTank = new FluidTank(ModFluids.WATER.getSource(), 10_000);
        steamTank = new FluidTank(ModFluids.STEAM.getSource(), 1_000_000);
    }

    // ─── Steam grade ─────────────────────────────────────────────────────────

    private static Fluid steamFluidFor(int grade) {
        return switch (grade) {
            case 1  -> ModFluids.HOTSTEAM.getSource();
            case 2  -> ModFluids.SUPERHOTSTEAM.getSource();
            case 3  -> ModFluids.ULTRAHOTSTEAM.getSource();
            default -> ModFluids.STEAM.getSource();
        };
    }

    /**
     * The grade is derived from the tank's own type rather than tracked separately, so the two can
     * never drift apart - and the type already round-trips through NBT and the client sync.
     */
    public int getSteamGrade() {
        Fluid type = steamTank.getTankType();
        if (type == ModFluids.HOTSTEAM.getSource())      return 1;
        if (type == ModFluids.SUPERHOTSTEAM.getSource()) return 2;
        if (type == ModFluids.ULTRAHOTSTEAM.getSource()) return 3;
        return 0;
    }

    // ─── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKBoilerBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        be.lastConsumption = 0;
        be.lastOutput      = 0;
        if (be.ventDelay > 0) be.ventDelay--;

        int grade = be.getSteamGrade();
        double heatAvail = be.heat - HEAT_THRESHOLD[grade];

        if (heatAvail > 0) {
            double heatPerMb = RBMKDials.getBoilerHeatConsumption(level);
            double factor    = STEAM_FACTOR[grade];
            int waterUsed, steamProduced;

            if (grade == 3) {
                steamProduced = (int) Math.floor((heatAvail / heatPerMb) * 100 / factor);
                waterUsed     = (int) Math.floor(steamProduced / 100.0 * factor);
                if (be.waterTank.getFill() < waterUsed) {
                    steamProduced = (int) Math.floor(be.waterTank.getFill() * 100.0 / factor);
                    waterUsed     = (int) Math.floor(steamProduced / 100.0 * factor);
                }
            } else {
                waterUsed     = (int) Math.min(Math.floor(heatAvail / heatPerMb), be.waterTank.getFill());
                steamProduced = (int) Math.floor(waterUsed * 100.0 / factor);
            }

            if (waterUsed > 0) {
                be.lastConsumption = waterUsed;
                be.lastOutput      = steamProduced;

                be.waterTank.setFill(be.waterTank.getFill() - waterUsed);
                be.steamTank.setFill(be.steamTank.getFill() + steamProduced);

                if (be.steamTank.getFill() > be.steamTank.getMaxFill()) {
                    be.steamTank.setFill(be.steamTank.getMaxFill());
                    be.vent(level, pos);
                }

                be.heat -= waterUsed * heatPerMb;
                be.setChanged();
            }
        }

        be.exchangeFluids(level);
    }

    /**
     * A full steam tank blows off through the top of the column: one jet particle and the steam
     * engine report, on the original's 20-30 tick cooldown.
     */
    private void vent(Level level, BlockPos pos) {
        if (ventDelay > 0) return;

        double y = pos.getY() + RBMKDials.getColumnHeight(level);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(com.hbm_m.particle.ModParticleTypes.RBMK_STEAM.get(),
                    pos.getX() + 0.25 + level.random.nextInt(2) * 0.5,
                    y,
                    pos.getZ() + 0.25 + level.random.nextInt(2) * 0.5,
                    0, 0, 0, 0, 0);
        }
        level.playSound(null, pos.getX(), y, pos.getZ(),
                com.hbm_m.sound.ModSounds.STEAM_ENGINE_OPERATE.get(), SoundSource.BLOCKS,
                2F, 1F + level.random.nextFloat() * 0.25F);
        ventDelay = 20 + level.random.nextInt(10);
    }

    /**
     * {@code getOutputPos}: steam always leaves through the top of the column, and additionally
     * out of the sides and bottom of a loader sitting directly underneath - the loader is what
     * gives a boiler channel a reachable outlet at floor level. Each entry is the neighbouring
     * position paired with the direction pointing from this column towards it.
     */
    private java.util.List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> getOutputPos(Level level) {
        BlockPos pos = getBlockPos();
        int h = RBMKDials.getColumnHeight(level);
        java.util.List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> out = new java.util.ArrayList<>();
        out.add(com.mojang.datafixers.util.Pair.of(pos.above(h + 1), Direction.UP));

        BlockPos loader = null;
        if (level.getBlockState(pos.below()).is(com.hbm_m.block.ModBlocks.RBMK_LOADER.get())) {
            loader = pos.below();
        } else if (level.getBlockState(pos.below(2)).is(com.hbm_m.block.ModBlocks.RBMK_LOADER.get())) {
            loader = pos.below(2);
        }
        if (loader != null) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP) continue;
                out.add(com.mojang.datafixers.util.Pair.of(loader.relative(dir), dir));
            }
        }
        return out;
    }

    /**
     * The original's {@code trySubscribe(feed, ..., yCoord - 1, NEG_Y)} plus a {@code tryProvide}
     * for every output position. This replaces a hand-rolled "push into whatever fluid handler is
     * next door" pass: the port has the full MK2 fluid network, so the channel should join it the
     * same way every other machine does, otherwise it can only ever talk to a block it is
     * physically touching and never to a pipe run.
     */
    private void exchangeFluids(Level level) {
        BlockPos pos = getBlockPos();
        trySubscribe(waterTank.getTankType(), level, pos.below(), Direction.DOWN);

        if (steamTank.getFill() <= 0) return;
        for (com.mojang.datafixers.util.Pair<BlockPos, Direction> target : getOutputPos(level)) {
            tryProvide(steamTank, level, target.getFirst(), target.getSecond());
        }
    }

    // ─── IFluidUserMK2 / MK2 network ────────────────────────────────────────

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getAllTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { waterTank, steamTank };
    }

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getSendingTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { steamTank };
    }

    @Override
    public com.hbm_m.inventory.fluid.tank.FluidTank[] getReceivingTanks() {
        return new com.hbm_m.inventory.fluid.tank.FluidTank[] { waterTank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    //? if forge {
    /**
     * Without this the channel had two tanks that nothing could reach: no pipe, tank or bucket
     * could put water in and no machine could take steam out, so the column heated up, boiled
     * nothing, and sat there.
     *
     * <p>The original splits the two by direction - {@code getReceivingTanks} is the feed and
     * {@code getSendingTanks} is the steam, with water subscribed on NEG_Y - so the bottom face
     * is water-only and the sides are steam-only. A query with no side (buckets, most generic
     * inspections) gets a combined view that fills the feed and drains the steam, because a
     * side-less handler that exposed only one of the two would make the other unreachable by
     * hand.</p>
     */
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.DOWN) return waterTank.getForgeFluidCapability().cast();
            if (side != null)           return steamTank.getForgeFluidCapability().cast();
            return combinedHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler>
            combinedHandler = net.minecraftforge.common.util.LazyOptional.of(FeedAndSteamHandler::new);

    /** Feed in, steam out - the side-less view of {@link #waterTank} plus {@link #steamTank}. */
    private class FeedAndSteamHandler implements net.minecraftforge.fluids.capability.IFluidHandler {

        private net.minecraftforge.fluids.capability.IFluidHandler feed() {
            return waterTank.getForgeFluidCapability().orElseThrow(IllegalStateException::new);
        }

        private net.minecraftforge.fluids.capability.IFluidHandler steam() {
            return steamTank.getForgeFluidCapability().orElseThrow(IllegalStateException::new);
        }

        @Override public int getTanks() { return 2; }

        @Override
        public @org.jetbrains.annotations.NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return tank == 0 ? feed().getFluidInTank(0) : steam().getFluidInTank(0);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? waterTank.getMaxFill() : steamTank.getMaxFill();
        }

        @Override
        public boolean isFluidValid(int tank, @org.jetbrains.annotations.NotNull net.minecraftforge.fluids.FluidStack stack) {
            return tank == 0 && feed().isFluidValid(0, stack);
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return feed().fill(resource, action);
        }

        @Override
        public @org.jetbrains.annotations.NotNull net.minecraftforge.fluids.FluidStack drain(
                net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            return steam().drain(resource, action);
        }

        @Override
        public @org.jetbrains.annotations.NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            return steam().drain(maxDrain, action);
        }
    }
    //?}

    /**
     * {@code cyceCompressor}: each stage swaps the tank to the next steam type and divides the
     * contents by ten, and wrapping back round from ultra-hot multiplies by a thousand instead
     * (capped at the tank size) - the previous version divided on the wrap too, which quietly
     * destroyed the stored steam every fourth click.
     */
    public void cycleCompressor() {
        int grade = getSteamGrade();
        int fill  = steamTank.getFill();

        if (grade == 3) {
            steamTank.setTankType(steamFluidFor(0));
            steamTank.setFill(Math.min(fill * 1000, steamTank.getMaxFill()));
        } else {
            steamTank.setTankType(steamFluidFor(grade + 1));
            steamTank.setFill(fill / 10);
        }
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    /**
     * {@code TileEntityRBMKBoiler.onMelt}: a melting steam channel throws one or two pieces of
     * blank debris, and - when the overpressure dial is on - hands every steam network it was
     * feeding to the meltdown, which then tears those networks apart.
     */
    @Override
    public void onMelt(Level level, int reduce) {
        int count = 1 + level.random.nextInt(2);
        for (int i = 0; i < count; i++) {
            spawnDebris(level, "blank");
        }

        if (RBMKDials.getOverpressure(level) && level instanceof net.minecraft.server.level.ServerLevel server) {
            for (com.mojang.datafixers.util.Pair<BlockPos, Direction> target : getOutputPos(level)) {
                var node = com.hbm_m.api.network.UniNodespace.getNode(server, target.getFirst(),
                        com.hbm_m.api.fluids.FluidNetProvider.forFluid(steamTank.getTankType()));
                if (node != null && node.hasValidNet()) {
                    overpressureNets.add(node.net);
                }
            }
        }

        super.onMelt(level, reduce);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_boiler"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKBoilerMenu(id, inv, this); }
    @Override public RBMKType getRBMKType()      { return RBMKType.OTHER; }
    @Override public ColumnType getConsoleType() { return ColumnType.BOILER; }

    @Override
    public net.minecraft.nbt.CompoundTag getNBTForConsole() {
        net.minecraft.nbt.CompoundTag d = new net.minecraft.nbt.CompoundTag();
        d.putInt("water",    waterTank.getFill());
        d.putInt("maxWater", waterTank.getMaxFill());
        d.putInt("steam",    steamTank.getFill());
        d.putInt("maxSteam", steamTank.getMaxFill());
        d.putShort("steamGrade", (short) getSteamGrade());
        return d;
    }

    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        waterTank.writeToNBT(tag, "water"); // kept as "water" (not the original's "feed") so existing saves keep their contents
        steamTank.writeToNBT(tag, "steam");
        tag.putInt("ventDelay", ventDelay);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        waterTank.readFromNBT(tag, "water");
        steamTank.readFromNBT(tag, "steam");
        ventDelay = tag.getInt("ventDelay");
    }
}
