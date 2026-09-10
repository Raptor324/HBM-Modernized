package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.block.machines.MachineTurbofanBlock;
import com.hbm_m.damagesource.ModDamageSources;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.menu.MachineTurbofanMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Turbofan - 1:1-Port der Kernfunktion aus {@code TileEntityMachineTurbofan} (1.7.10-Original):
 * verbrennt AERO-Grade-Treibstoff (Kerosin) zu Energie. Original-Formel: pro Tick wird
 * {@code amountToBurn = min(1 + afterburner, tankFill)} mB Treibstoff verbrannt, Energie-Output
 * {@code burnValue * amountToBurn * (1 + min(afterburner/3, 4))} mit
 * {@code burnValue = combustionEnergy / 1000}.
 * <p>
 * Portiert sind inzwischen: die Nachbrenner-Aufwertung (Slot {@link #SLOT_UPGRADE}, Flammenpony
 * setzt Stufe 100), die Redstonesperre, die drei Wirkzonen (Ansaugung, Abgasstrahl, Schaufelebene)
 * samt Blutbehaelter. Nicht uebernommen bleiben Rauch-/Feuerpartikel, Sound und die
 * Rotoranimation. Die Verschmutzung ist portiert (BURN mit dem Fuenffachen der
 * verbrannten Menge, alle 20 Ticks, ueber die Rauchtanks der Basisklasse). Energieerzeugung aus Treibstoff
 * bleibt 1:1 erhalten.
 * <p>
 * GUI/Menu-Slots: wie im Original fuenf Stueck. Die Reihenfolge weicht ab - der Aufwertungsslot
 * haengt hinten an ({@link #SLOT_UPGRADE} = 4 statt 2), damit die zuvor vergebenen Indizes und
 * damit gespeicherte Inventare unveraendert bleiben. Nur der Batterie-Slot ist aktiv verdrahtet
 * (wird pro Tick ueber {@code chargeItemInSlot} geladen, analog zum Combustion Engine). Behaelter-
 * Befuellung/-Leerung (slot 0/1) und das erzwungene Setzen des Tank-Typs per Identifier-Item (slot 3)
 * sind NICHT implementiert - der Tank wird ausschliesslich ueber das Fluid-Netz (Capability) befuellt.
 */
public class MachineTurbofanBlockEntity extends com.hbm_m.blockentity.MachinePollutingBlockEntity {

    public static final int SLOT_FUEL_CONTAINER = 0;
    public static final int SLOT_EMPTY_CONTAINER = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FLUID_IDENTIFIER = 3;
    /**
     * Nachbrenner-Aufwertung. Im Original ist das Slot 2; hier haengt er hinten an, damit die
     * bereits vergebenen Indizes (und damit gespeicherte Inventare) unveraendert bleiben.
     */
    public static final int SLOT_UPGRADE = 4;

    private static final int TANK_CAPACITY_MB = 24_000;
    private static final int BASE_BURN_MB_PER_TICK = 1;
    /** Original: {@code blood = new FluidTank(Fluids.BLOOD, 24000)}. */
    private static final int BLOOD_CAPACITY_MB = 24_000;
    /** Original: {@code blood.setFill(blood.getFill() + 50)} je zerlegter Kreatur. */
    private static final int BLOOD_PER_KILL_MB = 50;

    private final FluidTank tank = new FluidTank(ModFluids.KEROSENE.getSource(), TANK_CAPACITY_MB);
    private final FluidTank blood = new FluidTank(ModFluids.BLOOD.getSource(), BLOOD_CAPACITY_MB);

    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();
    /** Original: {@code afterburner} - Stufe der Nachbrenner-Aufwertung, 100 mit der Flammenpony. */
    private int afterburner = 0;
    /** Original: {@code showBlood} - schaltet die Blutdarstellung im Modell frei. */
    private boolean showBlood = false;

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank, blood, smoke, smokeLeaded, smokePoison };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        // Original: der Turbofan schickt neben dem Rauch auch das Blut ins Rohrnetz.
        FluidTank[] smokeTanks = getSmokeTanks();
        return new FluidTank[] { blood, smokeTanks[0], smokeTanks[1], smokeTanks[2] };
    }

    public FluidTank getBloodTank() { return blood; }
    public boolean isShowingBlood() { return showBlood; }
    public int getAfterburner() { return afterburner; }

    public MachineTurbofanBlockEntity(BlockPos pos, BlockState state) {
        // Original: super(5, 150) - 150 mB Rauchpuffer je Sorte.
        super(ModBlockEntities.TURBOFAN_BE.get(), pos, state, 5, 2_000_000L, 0L, 80_000L, 150);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbofanBlockEntity be) {
        if (level.isClientSide()) return;

        be.chargeItemInSlot(SLOT_BATTERY);
        be.updateAfterburner();

        Fluid fuel = be.tank.getStoredFluid();
        FT_Combustible combustible = FluidType.getTrait(fuel, FT_Combustible.class);

        boolean dirty = false;
        boolean running = false;

        // Original: Redstonesperre ueber die vier Anschlusspunkte. Der Port prueft die Nachbarn des
        // Kernblocks - dieselbe Konvention wie Combustion Engine und Diesel-Generator.
        boolean redstone = level.hasNeighborSignal(pos);

        if (!redstone && combustible != null && combustible.getGrade() == FuelGrade.AERO) {
            // Original: amountToBurn = min(1 + afterburner, tankFill).
            int amount = BASE_BURN_MB_PER_TICK + be.afterburner;
            int amountToBurn = Math.min(amount, be.tank.getFluidAmountMb());

            if (amountToBurn > 0 && be.getEnergyStored() < be.getMaxEnergyStored()) {
                long burnValue = combustible.getCombustionEnergy() / 1_000L;
                // Original: output = burnValue * amountToBurn * (1 + min(afterburner / 3, 4)).
                long output = (long) (burnValue * amountToBurn * (1 + Math.min(be.afterburner / 3D, 4)));

                be.tank.drainMb(amountToBurn);

                // Original: alle 20 Ticks BURN mit dem Fuenffachen der verbrannten Menge.
                if (level.getGameTime() % 20 == 0) {
                    be.pollute(be.tank.getTankType(), FluidReleaseType.BURN, amountToBurn * 5F);
                }
                be.setEnergyStored(Math.min(be.getMaxEnergyStored(), be.getEnergyStored() + output));
                running = true;
                dirty = true;
            }
        }

        be.sendSmokeAllDirections();

        if (running) {
            be.affectEntities(level, pos, state);
        }

        if (dirty) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    /**
     * Original: {@code upgradeManager.getLevel(UpgradeType.AFTERBURN)}, und eine Flammenpony im
     * Aufwertungsslot setzt die Stufe hart auf 100.
     */
    private void updateAfterburner() {
        upgradeManager.checkSlots(getInventory(), SLOT_UPGRADE, SLOT_UPGRADE, null);
        this.afterburner = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.AFTERBURN);

        if (getInventory().getStackInSlot(SLOT_UPGRADE).is(com.hbm_m.item.ModItems.FLAME_PONY.get())) {
            this.afterburner = 100;
        }
    }

    // ═══════════════════════════ Ansaugung und Abgasstrahl ═══════════════════════════

    /**
     * 1:1-Port der drei Wirkzonen des Originals, gemessen entlang der Blickrichtung des Blocks:
     * <ul>
     *   <li><b>Abgasstrahl</b> (3,5 bis 19,5 Bloecke dahinter): blaest alles weg; mit Nachbrenner
     *       setzt er zusaetzlich in Brand und macht 5 Schaden.</li>
     *   <li><b>Ansaugung</b> (3,5 bis 8,5 Bloecke davor): zieht alles zur Maschine.</li>
     *   <li><b>Schaufelebene</b> (3,5 bis 3,75 davor): 1000 Schaden, Netz-Effekt, und Kreaturen
     *       hinterlassen 50 mB Blut im Tank.</li>
     * </ul>
     *
     * <p><b>Vorbehalt:</b> Das Original leitet seine Achse aus der Multiblock-Ausrichtung ab
     * ({@code orientation(meta - 10).getRotation(UP)}). Der Turbofan ist in diesem Port noch kein
     * Multiblock (Strukturstummel), darum dient hier {@code FACING} als Achse und der Kernblock als
     * Ursprung. Zeigt sich das im Spiel spiegelverkehrt, ist es ein Vorzeichenwechsel.</p>
     */
    private void affectEntities(Level level, BlockPos pos, BlockState state) {
        Direction dir = state.hasProperty(MachineTurbofanBlock.FACING)
                ? state.getValue(MachineTurbofanBlock.FACING)
                : Direction.NORTH;
        Direction rot = dir.getClockWise();

        // Abgasstrahl hinter der Maschine.
        for (Entity e : level.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, -3.5D, -19.5D))) {
            if (afterburner > 0) {
                e.setSecondsOnFire(5);
                e.hurt(level.damageSources().onFire(), 5F);
            }
            push(e, dir);
        }

        // Ansaugung vor der Maschine.
        for (Entity e : level.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, 3.5D, 8.5D))) {
            push(e, dir);
        }

        // Die Schaufelebene selbst.
        for (Entity e : level.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, 3.5D, 3.75D))) {
            boolean wasAlive = e.isAlive();
            e.hurt(ModDamageSources.blender(level), 1000F);
            e.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.25D, 0.05D, 0.25D));

            if (wasAlive && !e.isAlive() && e instanceof LivingEntity) {
                level.playSound(null, e.getX(), e.getY(), e.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.BLOCKS,
                        2.0F, 0.95F + level.random.nextFloat() * 0.2F);

                blood.setFill(Math.min(blood.getFill() + BLOOD_PER_KILL_MB, blood.getMaxFill()));
                showBlood = true;
                setChanged();
            }
        }
    }

    /** Original: {@code e.motionX/Z -= dir.offset * 0.2} - alles wird entgegen der Achse geschoben. */
    private static void push(Entity e, Direction dir) {
        e.setDeltaMovement(e.getDeltaMovement().subtract(dir.getStepX() * 0.2D, 0D, dir.getStepZ() * 0.2D));
        e.hurtMarked = true;
    }

    /** Baut den Wirkbereich zwischen zwei Abstaenden entlang {@code dir}, 1,5 Bloecke breit, 3 hoch. */
    private static AABB zone(BlockPos pos, Direction dir, Direction rot, double from, double to) {
        double cx = pos.getX() + 0.5D;
        double cz = pos.getZ() + 0.5D;

        double x1 = cx + dir.getStepX() * from - rot.getStepX() * 1.5D;
        double x2 = cx + dir.getStepX() * to + rot.getStepX() * 1.5D;
        double z1 = cz + dir.getStepZ() * from - rot.getStepZ() * 1.5D;
        double z2 = cz + dir.getStepZ() * to + rot.getStepZ() * 1.5D;

        return new AABB(Math.min(x1, x2), pos.getY(), Math.min(z1, z2),
                        Math.max(x1, x2), pos.getY() + 3D, Math.max(z1, z2));
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "fuel");
        blood.writeToNBT(tag, "blood");
        tag.putBoolean("showBlood", showBlood);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "fuel");
        blood.readFromNBT(tag, "blood");
        showBlood = tag.getBoolean("showBlood");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineTurbofanMenu(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack);
        if (slot == SLOT_FLUID_IDENTIFIER) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == SLOT_FUEL_CONTAINER) return true;
        if (slot == SLOT_UPGRADE) {
            return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade
                    || stack.is(com.hbm_m.item.ModItems.FLAME_PONY.get());
        }
        return false;
    }
}
