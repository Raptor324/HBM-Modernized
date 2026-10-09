package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.redstoneoverradio.IRORValueProvider;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.projectile.EntityShrapnel;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.menu.MachineWatzPowerplantMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.nuclear.WatzPelletItem;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * 1:1 {@code TileEntityWatz}. Mehrere Watz-Segmente (je 3 Bloecke hoch) lassen sich uebereinander stapeln: das oberste
 * Segment steuert alle darunter, die Tanks werden jeden Tick zu gemeinsamen Tanks zusammengelegt, die Reaktion laeuft
 * von oben nach unten, Pellets fallen in leere Plaetze des Segments darunter bzw. tauschen mit verbrauchten.
 * Eingeschaltet wird ueber eine Watz-Pumpe direkt auf dem obersten Segment, die von oben mit Redstone versorgt wird.
 * Laeuft der Schlammtank ueber, zerlegt sich der Reaktor.
 */
public class MachineWatzPowerplantBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IControlReceiver, IRORValueProvider {

    public static final int PELLET_SLOTS = 24;

    public FluidTank[] tanks;
    public FluidTank[] sharedTanks;
    public int heat;
    /** Fluss aus der passiven Emission (nur Anzeige). */
    public double fluxLastBase;
    /** Fluss der letzten Reaktion (fliesst in die naechste ein). */
    public double fluxLastReaction;
    public double fluxDisplay;
    public boolean isOn;

    /* Sperrtypen fuer Item-IO */
    public boolean isLocked = false;
    public ItemStack[] locks;

    //? if forge {
    private final LazyOptional<IItemHandler> automationHandler;
    //?} elif neoforge {
    /*private final com.hbm_m.platform.LazyCap<net.neoforged.neoforge.items.IItemHandler> automationHandler;
    *///?}

    public MachineWatzPowerplantBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ_POWERPLANT_BE.get(), pos, state, PELLET_SLOTS, 0L, 0L);
        this.locks = emptyLocks();
        this.tanks = new FluidTank[3];
        this.tanks[0] = new FluidTank(ModFluids.COOLANT.getSource(), 64_000);
        this.tanks[1] = new FluidTank(ModFluids.COOLANT_HOT.getSource(), 64_000);
        this.tanks[2] = new FluidTank(ModFluids.WATZ.getSource(), 64_000);
        resetSharedTanks();
        //? if forge {
        this.automationHandler = LazyOptional.of(() -> new AutomationHandler(inventory));
        //?} elif neoforge {
        /*this.automationHandler = com.hbm_m.platform.LazyCap.of(() -> new AutomationHandler(inventory));
        *///?}
    }

    private static ItemStack[] emptyLocks() {
        ItemStack[] l = new ItemStack[PELLET_SLOTS];
        java.util.Arrays.fill(l, ItemStack.EMPTY);
        return l;
    }

    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isItemValidForSlot(slot, stack); }

            /** Original: {@code getInventoryStackLimit() = 1}. */
            @Override
            public int getSlotLimit(int slot) { return 1; }
        };
    }

    protected void resetSharedTanks() {
        this.sharedTanks = new FluidTank[3];
        this.sharedTanks[0] = new FluidTank(ModFluids.COOLANT.getSource(), 64_000);
        this.sharedTanks[1] = new FluidTank(ModFluids.COOLANT_HOT.getSource(), 64_000);
        this.sharedTanks[2] = new FluidTank(ModFluids.WATZ.getSource(), 64_000);
        this.sharedTanks[0].setFill(tanks[0].getFill());
        this.sharedTanks[1].setFill(tanks[1].getFill());
        this.sharedTanks[2].setFill(tanks[2].getFill());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineWatzPowerplantBlockEntity be) {
        if (level.isClientSide) return;
        be.updateEntity(level, pos);
    }

    private void updateEntity(Level level, BlockPos pos) {

        resetSharedTanks();

        if (updateLock()) return;

        boolean turnedOn = level.getBlockState(pos.above(3)).is(ModBlocks.WATZ_PUMP.get())
                && level.getSignal(pos.above(5), Direction.DOWN) > 0;
        List<MachineWatzPowerplantBlockEntity> segments = new ArrayList<>();
        segments.add(this);
        this.subscribeToTop();

        /* alle Segmente sammeln */
        for (int y = pos.getY() - 3; y >= level.getMinBuildHeight(); y -= 3) {
            BlockEntity tile = level.getBlockEntity(new BlockPos(pos.getX(), y, pos.getZ()));
            if (tile instanceof MachineWatzPowerplantBlockEntity watz) {
                segments.add(watz);
            } else {
                break;
            }
        }

        /* gemeinsame Tanks aufsetzen */
        FluidTank[] shared = new FluidTank[3];
        for (int i = 0; i < 3; i++) shared[i] = new FluidTank(tanks[i].getTankType(), 0);

        for (MachineWatzPowerplantBlockEntity segment : segments) {
            segment.setupCoolant();
            for (int i = 0; i < 3; i++) {
                shared[i].changeTankSize(shared[i].getMaxFill() + segment.tanks[i].getMaxFill());
                shared[i].setFill(shared[i].getFill() + segment.tanks[i].getFill());
            }
        }

        // Kuehlmittel, unten nach oben
        for (int i = segments.size() - 1; i >= 0; i--) {
            segments.get(i).updateCoolant(shared);
        }

        /* Reaktion, oben nach unten */
        this.updateReaction(null, shared, turnedOn);
        for (int i = 1; i < segments.size(); i++) {
            segments.get(i).updateReaction(segments.get(i - 1), shared, turnedOn);
        }

        /* Sync (Reihenfolge egal) */
        for (MachineWatzPowerplantBlockEntity segment : segments) {
            segment.sharedTanks[0] = shared[0];
            segment.sharedTanks[1] = shared[1];
            segment.sharedTanks[2] = shared[2];
            segment.isOn = turnedOn;
            segment.setChanged();
            segment.sendUpdateToClient();
            segment.heat *= 0.99; // 1% Abkuehlung pro Tick
        }

        /* Fluessigkeit aus den gemeinsamen Tanks zurueckverteilen, unten nach oben */
        for (int i = segments.size() - 1; i >= 0; i--) {
            MachineWatzPowerplantBlockEntity segment = segments.get(i);
            for (int j = 0; j < 3; j++) {
                int min = Math.min(segment.tanks[j].getMaxFill(), shared[j].getFill());
                shared[j].setFill(shared[j].getFill() - min);
                segment.tanks[j].setFill(min);
            }
        }

        segments.get(segments.size() - 1).sendOutBottom();

        /* Explosion bei Schlammueberlauf */
        if (shared[2].getFill() > 0) {
            for (int x = -3; x <= 3; x++) {
                for (int y = 3; y < 6; y++) {
                    for (int z = -3; z <= 3; z++) {
                        level.setBlock(pos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            this.disassemble(level, pos);

            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY() + 1, pos.getZ(), 1_000F);

            level.playSound(null, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                    com.hbm_m.sound.ModSounds.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 50.0F, 1.0F);
            if (level instanceof ServerLevel server) {
                // Anzahl 0: der Client nimmt xd als Skalierung (Original: "rbmkmush", scale 5)
                server.sendParticles(com.hbm_m.particle.ModParticleTypes.RBMK_MUSH.get(),
                        pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0, 5, 0, 0, 1);
            }
        }
    }

    /** Plausibilitaetspruefung, greift normalerweise nur bei verkorkstem NBT. */
    public void setupCoolant() {
        tanks[0].setTankType(ModFluids.COOLANT.getSource());
        FT_Heatable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait != null && trait.getFirstStep() != null) tanks[1].setTankType(trait.getFirstStep().typeProduced);
    }

    public void updateCoolant(FluidTank[] tanks) {

        double coolingFactor = 0.2D; // 20% pro Tick
        double heatToUse = this.heat * coolingFactor;

        FT_Heatable trait = FluidType.getTrait(tanks[0].getTankType(), FT_Heatable.class);
        if (trait == null) return;
        HeatingStep step = trait.getFirstStep();
        if (step == null) return;

        int heatCycles = (int) (heatToUse / step.heatReq);
        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;

        int cycles = Math.min(heatCycles, Math.min(hotCycles, coolCycles));
        this.heat -= cycles * step.heatReq;
        tanks[0].setFill(tanks[0].getFill() - cycles * step.amountReq);
        tanks[1].setFill(tanks[1].getFill() + cycles * step.amountProduced);
    }

    /** Erzwingt die strikte Aktualisierung von oben nach unten. */
    public void updateReaction(@Nullable MachineWatzPowerplantBlockEntity above, FluidTank[] tanks, boolean turnedOn) {

        if (turnedOn) {
            List<ItemStack> pellets = new ArrayList<>();

            for (int i = 0; i < PELLET_SLOTS; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (WatzPelletItem.isFresh(stack)) pellets.add(stack);
            }

            double baseFlux = 0D;

            /* Grundfluss */
            for (ItemStack stack : pellets) {
                baseFlux += WatzPelletItem.typeOf(stack).passive;
            }

            double inputFlux = baseFlux + fluxLastReaction;
            double addedFlux = 0D;
            double addedHeat = 0D;

            for (ItemStack stack : pellets) {
                WatzPelletType type = WatzPelletItem.typeOf(stack);

                if (type.burnFunc != null) {
                    double div = type.heatDiv != null ? type.heatDiv.effonix(heat) : 1D;
                    double burn = type.burnFunc.effonix(inputFlux) / div;
                    WatzPelletItem.setYield(stack, WatzPelletItem.getYield(stack) - burn);
                    addedFlux += burn;
                    addedHeat += type.heatEmission * burn;
                    tanks[2].setFill(tanks[2].getFill() + (int) Math.round(type.mudContent * burn));
                }
            }

            for (ItemStack stack : pellets) {
                WatzPelletType type = WatzPelletItem.typeOf(stack);

                if (type.absorbFunc != null) {
                    double absorb = type.absorbFunc.effonix(baseFlux + fluxLastReaction);
                    addedHeat += absorb;
                    WatzPelletItem.setYield(stack, WatzPelletItem.getYield(stack) - absorb);
                    tanks[2].setFill(tanks[2].getFill() + (int) Math.round(type.mudContent * absorb));
                }
            }

            this.heat += addedHeat;
            this.fluxLastBase = baseFlux;
            this.fluxLastReaction = addedFlux;

        } else {
            this.fluxLastBase = 0;
            this.fluxLastReaction = 0;
        }

        for (int i = 0; i < PELLET_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);

            /* verbrauchen */
            if (WatzPelletItem.isFresh(stack) && WatzPelletItem.getEnrichment(stack) <= 0) {
                inventory.setStackInSlot(i, new ItemStack(ModItems.WATZ_PELLET_DEPLETED.get(WatzPelletItem.typeOf(stack)).get()));
            }
        }

        if (above != null) {
            for (int i = 0; i < PELLET_SLOTS; i++) {
                ItemStack stackBottom = inventory.getStackInSlot(i);
                ItemStack stackTop = above.inventory.getStackInSlot(i);

                /* Items fallen nach unten, wenn der untere Platz leer ist */
                if (stackBottom.isEmpty() && !stackTop.isEmpty()) {
                    inventory.setStackInSlot(i, stackTop.copy());
                    above.inventory.setStackInSlot(i, ItemStack.EMPTY);
                }

                /* Items tauschen, wenn der obere Platz verbraucht ist (Original: vorige Stack-Referenzen) */
                if (WatzPelletItem.isFresh(stackBottom) && WatzPelletItem.isDepleted(stackTop)) {
                    ItemStack buf = stackTop.copy();
                    above.inventory.setStackInSlot(i, stackBottom.copy());
                    inventory.setStackInSlot(i, buf);
                }
            }
        }
    }

    /** Verhindert eigene Updates, solange ein anderes Segment darueber sitzt. */
    public boolean updateLock() {
        return level != null && level.getBlockEntity(worldPosition.above(3)) instanceof MachineWatzPowerplantBlockEntity;
    }

    protected void subscribeToTop() {
        BlockPos p = worldPosition;
        trySubscribe(tanks[0].getTankType(), level, p.offset(0, 3, 0), Direction.UP);
        trySubscribe(tanks[0].getTankType(), level, p.offset(2, 3, 0), Direction.UP);
        trySubscribe(tanks[0].getTankType(), level, p.offset(-2, 3, 0), Direction.UP);
        trySubscribe(tanks[0].getTankType(), level, p.offset(0, 3, 2), Direction.UP);
        trySubscribe(tanks[0].getTankType(), level, p.offset(0, 3, -2), Direction.UP);
    }

    protected void sendOutBottom() {
        for (BlockPos pos : getSendingPos()) {
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, pos, Direction.DOWN);
            if (tanks[2].getFill() > 0) tryProvide(tanks[2], level, pos, Direction.DOWN);
        }
    }

    protected BlockPos[] getSendingPos() {
        BlockPos p = worldPosition;
        return new BlockPos[] {
                p.offset(0, -1, 0),
                p.offset(2, -1, 0),
                p.offset(-2, -1, 0),
                p.offset(0, -1, 2),
                p.offset(0, -1, -2)
        };
    }

    // ── Zerlegung ───────────────────────────────────────────────────────────

    private void disassemble(Level world, BlockPos pos) {

        int count = 20;
        RandomSource rand = world.random;
        for (int i = 0; i < count * 5; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(world);
            shrapnel.setPos(pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);
            double my = ((rand.nextFloat() * 0.5) + 0.5) * (1 + (count / (15 + rand.nextInt(21)))) + (rand.nextFloat() / 50 * count);
            double mx = rand.nextGaussian() * 1 * (1 + (count / 100));
            double mz = rand.nextGaussian() * 1 * (1 + (count / 100));
            shrapnel.setDeltaMovement(mx, my, mz);
            shrapnel.setWatz(true);
            world.addFreshEntity(shrapnel);
        }

        Block mud = ModBlocks.MUD_BLOCK.get();
        world.setBlock(pos, mud.defaultBlockState(), 3);
        world.setBlock(pos.above(1), mud.defaultBlockState(), 3);
        world.setBlock(pos.above(2), mud.defaultBlockState(), 3);

        Block element = ModBlocks.WATZ_ELEMENT.get();
        Block cooler = ModBlocks.WATZ_COOLER.get();
        Block end = ModBlocks.WATZ_END_BOLTED.get();

        setBrokenColumn(world, pos, 0, element, 1, 0);
        setBrokenColumn(world, pos, 0, element, 2, 0);
        setBrokenColumn(world, pos, 0, element, 0, 1);
        setBrokenColumn(world, pos, 0, element, 0, 2);
        setBrokenColumn(world, pos, 0, element, -1, 0);
        setBrokenColumn(world, pos, 0, element, -2, 0);
        setBrokenColumn(world, pos, 0, element, 0, -1);
        setBrokenColumn(world, pos, 0, element, 0, -2);
        setBrokenColumn(world, pos, 0, element, 1, 1);
        setBrokenColumn(world, pos, 0, element, 1, -1);
        setBrokenColumn(world, pos, 0, element, -1, 1);
        setBrokenColumn(world, pos, 0, element, -1, -1);
        setBrokenColumn(world, pos, 0, cooler, 2, 1);
        setBrokenColumn(world, pos, 0, cooler, 2, -1);
        setBrokenColumn(world, pos, 0, cooler, 1, 2);
        setBrokenColumn(world, pos, 0, cooler, -1, 2);
        setBrokenColumn(world, pos, 0, cooler, -2, 1);
        setBrokenColumn(world, pos, 0, cooler, -2, -1);
        setBrokenColumn(world, pos, 0, cooler, 1, -2);
        setBrokenColumn(world, pos, 0, cooler, -1, -2);

        for (int j = -1; j < 2; j++) {
            setBrokenColumn(world, pos, 1, end, 3, j);
            setBrokenColumn(world, pos, 1, end, j, 3);
            setBrokenColumn(world, pos, 1, end, -3, j);
            setBrokenColumn(world, pos, 1, end, j, -3);
        }
        setBrokenColumn(world, pos, 1, end, 2, 2);
        setBrokenColumn(world, pos, 1, end, 2, -2);
        setBrokenColumn(world, pos, 1, end, -2, 2);
        setBrokenColumn(world, pos, 1, end, -2, -2);

        com.hbm_m.advancement.ModAdvancements.grantNearby(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 50D,
                com.hbm_m.advancement.ModAdvancements.WATZ_BOOM);
    }

    private void setBrokenColumn(Level world, BlockPos pos, int minHeight, Block b, int x, int z) {

        int height = minHeight + world.random.nextInt(3 - minHeight);

        for (int i = 0; i < 3; i++) {
            if (i <= height) {
                world.setBlock(pos.offset(x, i, z), b.defaultBlockState(), 3);
            } else {
                world.setBlock(pos.offset(x, i, z), ModBlocks.MUD_BLOCK.get().defaultBlockState(), 3);
            }
        }
    }

    // ── NBT / Sync ──────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);

        ListTag list = new ListTag();
        for (int i = 0; i < locks.length; i++) {
            if (!locks[i].isEmpty()) {
                CompoundTag nbt1 = new CompoundTag();
                nbt1.putByte("slot", (byte) i);
                com.hbm_m.platform.StackNbt.save(locks[i], nbt1);
                list.add(nbt1);
            }
        }
        nbt.put("locks", list);

        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
        nbt.putInt("heat", this.heat);
        nbt.putDouble("lastFluxB", fluxLastBase);
        nbt.putDouble("lastFluxR", fluxLastReaction);
        nbt.putBoolean("isLocked", isLocked);

        // Original: serialize() schickt die gemeinsamen Tanks und die Flussanzeige an den Client
        nbt.putBoolean("isOn", isOn);
        nbt.putDouble("fluxDisplay", this.fluxLastReaction + this.fluxLastBase);
        for (int i = 0; i < sharedTanks.length; i++) {
            sharedTanks[i].writeToNBT(nbt, "s" + i);
            nbt.putInt("sc" + i, sharedTanks[i].getMaxFill());
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);

        this.locks = emptyLocks();
        ListTag list = nbt.getList("locks", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt1 = list.getCompound(i);
            byte b0 = nbt1.getByte("slot");
            if (b0 >= 0 && b0 < locks.length) locks[b0] = StackNbt.parse(nbt1);
        }

        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
        this.heat = nbt.getInt("heat");
        this.fluxLastBase = nbt.getDouble("lastFluxB");
        this.fluxLastReaction = nbt.getDouble("lastFluxR");
        this.isLocked = nbt.getBoolean("isLocked");
    }

    /** Original: {@code deserialize} liest die gemeinsamen Tanks in die eigenen Tanks. */
    @Override
    protected void applyClientUpdate(@NotNull CompoundTag nbt) {
        readNbtData(nbt, null);
        this.isOn = nbt.getBoolean("isOn");
        this.fluxDisplay = nbt.getDouble("fluxDisplay");
        for (int i = 0; i < tanks.length; i++) {
            if (nbt.contains("sc" + i)) tanks[i].changeTankSize(nbt.getInt("sc" + i));
            tanks[i].readFromNBT(nbt, "s" + i);
        }
    }

    // ── Steuerung ───────────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("lock")) {
            if (this.isLocked) {
                this.locks = emptyLocks();
            } else {
                for (int i = 0; i < PELLET_SLOTS; i++) this.locks[i] = inventory.getStackInSlot(i).copy();
            }
            this.isLocked = !this.isLocked;
            setChanged();
            sendUpdateToClient();
        }
    }

    @Override
    protected boolean isItemValidForSlot(int i, ItemStack stack) {
        if (!WatzPelletItem.isFresh(stack)) return false;
        if (!this.isLocked) return true;
        return this.locks != null && !this.locks[i].isEmpty() && this.locks[i].getItem() == stack.getItem();
    }

    /** Original: {@code canExtractItem} - frische Pellets bleiben fuer Automatisierung im Reaktor. */
    public boolean canExtractItem(ItemStack stack) {
        return !WatzPelletItem.isFresh(stack);
    }

    // ── Fluessigkeiten ──────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1], tanks[2] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── ROR ─────────────────────────────────────────────────────────────────

    public static final String[] ROR = new String[] { // nicht mit RUR verwechseln
            PREFIX_VALUE + "heat",
            PREFIX_VALUE + "flux",
            PREFIX_VALUE + "mud",
            PREFIX_VALUE + "coolant_hot",
            PREFIX_VALUE + "coolant_cold",
    };

    @Override
    public String[] getFunctionInfo() { return ROR; }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + this.heat;
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) (this.fluxLastBase + this.fluxLastReaction);
        if ((PREFIX_VALUE + "mud").equals(name)) return "" + this.tanks[2].getFill();
        if ((PREFIX_VALUE + "coolant_hot").equals(name)) return "" + this.tanks[1].getFill();
        if ((PREFIX_VALUE + "coolant_cold").equals(name)) return "" + this.tanks[0].getFill();
        return null;
    }

    // ── Sonstiges ───────────────────────────────────────────────────────────

    @Override protected Component getDefaultName() { return Component.translatable("container.watzPowerplant"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineWatzPowerplantMenu.create(id, inv, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) {
            bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                    worldPosition.getX() + 4, worldPosition.getY() + 3, worldPosition.getZ() + 4);
        }
        return bb;
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return automationHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public @Nullable Object getItemHandler(@Nullable Direction side) {
        return automationHandler.orElse(null);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automationHandler.invalidate();
    }

    /** Automatisierungszugriff mit den Regeln von {@code isItemValidForSlot} / {@code canExtractItem}. */
    private class AutomationHandler implements IItemHandler {
        private final ModItemStackHandler inv;
        AutomationHandler(ModItemStackHandler inv) { this.inv = inv; }
        @Override public int getSlots() { return inv.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inv.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return inv.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExtractItem(inv.getStackInSlot(slot))) return ItemStack.EMPTY;
            return inv.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return inv.isItemValid(slot, stack); }
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER) return automationHandler.cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public @Nullable Object getItemHandler(@Nullable Direction side) {
        return automationHandler.orElse(null);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        automationHandler.invalidate();
    }

    /^* Automatisierungszugriff mit den Regeln von {@code isItemValidForSlot} / {@code canExtractItem}. ^/
    private class AutomationHandler implements net.neoforged.neoforge.items.IItemHandler {
        private final ModItemStackHandler inv;
        AutomationHandler(ModItemStackHandler inv) { this.inv = inv; }
        @Override public int getSlots() { return inv.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inv.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return inv.insertItem(slot, stack, simulate); }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExtractItem(inv.getStackInSlot(slot))) return ItemStack.EMPTY;
            return inv.extractItem(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return inv.isItemValid(slot, stack); }
    }
    *///?}
}
