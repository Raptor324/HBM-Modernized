package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.NeutronNodeWorld;
import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKNeutronNode;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKNeutronStream;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.menu.RBMKRodMenu;
import com.hbm_m.item.rbmk.RBMKRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RBMKRodBlockEntity extends RBMKColumnBlockEntity
        implements IRBMKFluxReceiver, IRBMKLoadable, MenuProvider {

    public double fluxFastRatio    = 0;
    public double fluxQuantity     = 0;
    public double lastFluxQuantity = 0;
    public double lastFluxRatio    = 0;
    public boolean hasRod = false;
    public int rodColor   = 0x304825;
    public boolean moderated = false;
    public boolean explodeOnBroken = true;
    public ItemStack fuelSlot = ItemStack.EMPTY;

    public static final Direction[] FLUX_DIRS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    public RBMKRodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_ROD_BE.get(), pos, state);
    }

    // â"€â"€â"€ Tick â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKRodBlockEntity be) {
        baseTick(level, pos, state, be);
        if (level.isClientSide) return;

        if (!be.fuelSlot.isEmpty() && be.fuelSlot.getItem() instanceof RBMKRodItem rod) {
            be.rodColor = rod.colorTint;

            double fluxRatioOut, fluxIn;
            if (rod.specialFluxCurve) {
                fluxRatioOut = rod.fluxRatioOut(be.fluxFastRatio, RBMKRodItem.getEnrichment(be.fuelSlot));
                fluxIn = rod.fluxFromRatio(be.fluxQuantity, be.fluxFastRatio);
            } else {
                fluxRatioOut = (rod.rType == IRBMKFluxReceiver.NType.SLOW) ? 0 : 1;
                double fast = be.fluxQuantity * be.fluxFastRatio;
                double slow = be.fluxQuantity * (1 - be.fluxFastRatio);
                fluxIn = switch (rod.nType) {
                    case SLOW -> slow + fast * 0.5;
                    case FAST -> fast + slow * 0.3;
                    case ANY  -> be.fluxQuantity;
                };
            }

            double fluxOut = rod.burn(level, be.fuelSlot, fluxIn);
            rod.updateHeat(level, be.fuelSlot, 1.0);
            be.heat += rod.provideHeat(level, be.fuelSlot, be.heat, 1.0);

            if (be.heat > be.maxHeat()) {
                if (!RBMKDials.getMeltdownsDisabled(level)) be.meltdown(level);
                be.lastFluxQuantity = be.lastFluxRatio = 0;
                be.fluxQuantity = be.fluxFastRatio = 0;
                return;
            }
            be.heat = Math.min(be.heat, 10_000);

            if (!be.hasLid())
                ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(),
                    (float)(be.fluxQuantity * 0.05f));

            be.lastFluxQuantity = be.fluxQuantity;
            be.lastFluxRatio    = be.fluxFastRatio;
            be.fluxQuantity = be.fluxFastRatio = 0;
            be.spreadFlux(level, fluxOut, fluxRatioOut);
            be.hasRod = true;
        } else {
            be.lastFluxQuantity = be.lastFluxRatio = 0;
            be.fluxQuantity = be.fluxFastRatio = 0;
            be.hasRod = false;
        }
    }

    // â"€â"€â"€ Flux â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    @Override
    public void receiveFlux(RBMKNeutronStream stream) {
        double fastIn  = fluxQuantity * fluxFastRatio;
        double fastNew = stream.fluxQuantity * stream.fluxRatio;
        fluxQuantity  += stream.fluxQuantity;
        fluxFastRatio  = fluxQuantity > 0 ? (fastIn + fastNew) / fluxQuantity : 0;
    }

    /**
     * Normal channels emit along the four cardinal directions; ReaSim channels
     * ({@code TileEntityRBMKRodReaSim.spreadFlux}) instead fan out eight streams 45 degrees apart
     * at 75% flux each, with the whole fan jittered by a random multiple of 9 degrees so the
     * pattern is not axis-locked. That is the entire behavioural difference between the two rod
     * types, and without it a ReaSim channel is just an ordinary one.
     */
    private void spreadFlux(Level level, double flux, double ratio) {
        if (flux == 0) { NeutronNodeWorld.removeNode(level, getBlockPos()); return; }
        NeutronNodeWorld.StreamWorld sw = NeutronNodeWorld.getOrAddWorld(level);
        RBMKNeutronNode node = sw.getNode(getBlockPos());
        if (node == null) { node = RBMKNeutronHandler.makeNode(sw, this); sw.addNode(node); }

        if (isReaSim()) {
            double angle = level.random.nextInt(4) * 9.0D;
            for (int i = 0; i < 8; i++) {
                double rad = Math.toRadians(angle);
                sw.addStream(new RBMKNeutronStream(node,
                        new Vec3(Math.cos(rad), 0, Math.sin(rad)), flux * 0.75, ratio));
                angle += 45.0D;
            }
            return;
        }

        for (Direction dir : FLUX_DIRS)
            sw.addStream(new RBMKNeutronStream(node, new Vec3(dir.getStepX(), 0, dir.getStepZ()), flux, ratio));
    }

    /** Set from the block variant, see {@link com.hbm_m.block.machines.rbmk.RBMKRodBlock}. */
    public boolean reaSim = false;

    public boolean isReaSim() { return reaSim; }

    private void meltdown(Level level) { RBMKColumnBlockEntity.meltdownReactor(level, this); }

    @Override
    public void onMelt(Level level, int reduce) {
        boolean hadFuel = !fuelSlot.isEmpty();
        boolean isDigammaFuel = hadFuel && fuelSlot.getItem() == com.hbm_m.item.ModItems.RBMK_FUEL_DRX.get();
        if (isDigammaFuel) RBMKColumnBlockEntity.digamma = true;
        fuelSlot = ItemStack.EMPTY;

        if (hadFuel) {
            // A rod that was actively fueled melts down into corium top-to-bottom rather than
            // the ordinary rubble/air split used by passive columns - 1:1 with the original's
            // TileEntityRBMKRod.onMelt.
            int h = RBMKDials.getColumnHeight(level);
            BlockPos base = getBlockPos();
            for (int i = h; i >= 0; i--) {
                level.setBlock(base.above(i), com.hbm_m.block.ModBlocks.RBMK_CORIUM.get().defaultBlockState(), 3);
            }
            int count = 1 + level.random.nextInt(h);
            for (int i = 0; i < count; i++) spawnDebris(level, "fuel");
        } else {
            standardMelt(level, reduce);
        }

        if (moderated) {
            int count = 2 + level.random.nextInt(2);
            for (int i = 0; i < count; i++) spawnDebris(level, "graphite");
        }
        spawnDebris(level, "element");
        if (getLidState() == 1) spawnDebris(level, "lid");
    }

    // ─── Temperature gates ─────────────────────────────────────────────────────

    /** True when the rod is cool enough for the autoloader to handle. */
    public boolean coldEnoughForAutoloader() {
        if (!fuelSlot.isEmpty() && fuelSlot.getItem() instanceof RBMKRodItem)
            return RBMKRodItem.getHullHeat(fuelSlot) <= 1_000;
        return true;
    }

    /** True when the rod is cool enough for manual (player/crane) handling. */
    public boolean coldEnoughForManual() {
        if (!fuelSlot.isEmpty() && fuelSlot.getItem() instanceof RBMKRodItem)
            return RBMKRodItem.getHullHeat(fuelSlot) <= 200;
        return true;
    }

    // ─── Console data ──────────────────────────────────────────────────────────

    @Override
    public CompoundTag getNBTForConsole() {
        CompoundTag data = new CompoundTag();
        if (!fuelSlot.isEmpty() && fuelSlot.getItem() instanceof RBMKRodItem rod) {
            data.putDouble("enrichment", RBMKRodItem.getEnrichment(fuelSlot));
            data.putDouble("xenon",      RBMKRodItem.getPoison(fuelSlot));
            data.putDouble("c_heat",     RBMKRodItem.getHullHeat(fuelSlot));
            data.putDouble("c_coreHeat", RBMKRodItem.getCoreHeat(fuelSlot));
            data.putDouble("c_maxHeat",  rod.meltingPoint);
        }
        return data;
    }

    // â"€â"€â"€ MenuProvider â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_element"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKRodMenu(id, inv, this); }

    // â"€â"€â"€ IRBMKLoadable â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    @Override public boolean canLoad(ItemStack s)  { return !s.isEmpty() && s.getItem() instanceof RBMKRodItem && fuelSlot.isEmpty(); }
    @Override public void    load(ItemStack s)     { fuelSlot = s.copy(); setChanged(); }
    @Override public boolean canUnload()           { return !fuelSlot.isEmpty(); }
    @Override public ItemStack provideNext()       { return fuelSlot; }
    @Override public void    unload()              { fuelSlot = ItemStack.EMPTY; setChanged(); }

    @Override public RBMKType  getRBMKType()       { return RBMKType.ROD; }
    @Override public boolean   isModerated()       { return moderated; }
    @Override public double    maxHeat()           { return 1500; }
    @Override public ColumnType getConsoleType()   { return ColumnType.FUEL; }

    // â"€â"€â"€ NBT â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€â"€

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("fluxQuantity", lastFluxQuantity);
        tag.putDouble("fluxMod", lastFluxRatio);
        tag.putBoolean("hasRod", hasRod);
        tag.putBoolean("explodeOnBroken", explodeOnBroken);
        if (!fuelSlot.isEmpty()) tag.put("fuelSlot", safeItemSave(fuelSlot));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fluxQuantity    = tag.getDouble("fluxQuantity");
        fluxFastRatio   = tag.getDouble("fluxMod");
        hasRod          = tag.getBoolean("hasRod");
        explodeOnBroken = !tag.contains("explodeOnBroken") || tag.getBoolean("explodeOnBroken");
        if (tag.contains("fuelSlot"))
            fuelSlot = ItemStack.of(tag.getCompound("fuelSlot"));
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("fluxQuantity", lastFluxQuantity);
        tag.putDouble("fluxMod", lastFluxRatio);
        tag.putBoolean("hasRod", hasRod);
        tag.putBoolean("explodeOnBroken", explodeOnBroken);
        if (!fuelSlot.isEmpty()) tag.put("fuelSlot", safeItemSave(fuelSlot, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluxQuantity    = tag.getDouble("fluxQuantity");
        fluxFastRatio   = tag.getDouble("fluxMod");
        hasRod          = tag.getBoolean("hasRod");
        explodeOnBroken = !tag.contains("explodeOnBroken") || tag.getBoolean("explodeOnBroken");
        if (tag.contains("fuelSlot"))
            fuelSlot = ItemStack.parseOptional(registries, tag.getCompound("fuelSlot"));
    }
    *///?}
}

