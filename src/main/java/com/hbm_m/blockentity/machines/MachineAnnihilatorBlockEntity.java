package com.hbm_m.blockentity.machines;

import java.math.BigInteger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.annihilator.AnnihilatorPoolManager;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineAnnihilatorMenu;
import com.hbm_m.radiation.ChunkRadiationAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;

/**
 * Annihilator - Port von {@code TileEntityMachineAnnihilator} (1.7.10 Original). Kein Antimaterie-
 * Reaktor trotz des Namens: eine Item-/Fluid-"Muellvernichtung", die jede zerstoerte Menge in
 * einem persistenten, welt-gespeicherten, per Namen waehlbaren "Pool" ({@link AnnihilatorPoolManager})
 * aufsummiert (BigInteger, da die Summen ueber long hinauswachsen koennen). Der "Monitor"-Slot
 * zeigt per Tooltip den aktuellen Zaehlerstand fuer das dort abgelegte Item/Fluid an.
 * <p>
 * SCOPE-Entscheidung (Meilenstein-Auszahlung): Das Original zahlt bei Erreichen konfigurierter
 * BigInteger-Schwellwerte pro Pool spezifische Blueprint-Items aus ({@code ItemBlueprints.make(...)},
 * ~12 einzigartige Items), gated hinter {@code GeneralConfig.enable528} (in 1.7.10 standardmaessig
 * AUS). Diese individuellen Blueprint-Items wurden in diesem Port nie angelegt (nur ein generisches
 * {@code ModItems.BLUEPRINTS}-Item existiert) - das Meilenstein-Auszahlungssystem (Slots 2-7, Payout-
 * Request/-Claim) wird daher NICHT uebernommen, da die Zielinhalte fehlen und das Feature selbst im
 * Original experimentell/deaktiviert war. Die eigentliche Kernmechanik (Zaehlung, Pools, Monitor,
 * Strahlung) ist vollstaendig 1:1 uebernommen.
 * <p>
 * Ebenfalls nicht uebernommen: aktive Pollution-Effekte beim Vernichten von Fluiden ({@code
 * FT_Polluting.pollute(...)} im Original) - {@link com.hbm_m.inventory.fluid.trait.FT_Polluting}
 * ist in diesem Port rein deklarative Tooltip-Metadata ohne Wirkmethode (siehe gleiche Entscheidung
 * bei {@link MachineFlareStackBlockEntity}).
 */
public class MachineAnnihilatorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_MONITOR = 1;
    private static final int SLOT_COUNT = 2;

    private static final int TANK_CAPACITY_MB = 64_000;
    private static final String DEFAULT_POOL = "Recycling";

    private final FluidTank tank = new FluidTank(TANK_CAPACITY_MB) {
        @Override
        public void onContentsChanged() {
            setChanged();
            sendUpdateToClient();
        }
    };

    private String poolName = DEFAULT_POOL;
    private String monitorDisplay = "";

    public MachineAnnihilatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANNIHILATOR_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    // ==================== TICK ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineAnnihilatorBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        be.destroyInput(serverLevel);
        be.destroyFluid(serverLevel);
        be.updateMonitor(serverLevel);

        be.setChanged();
        be.sendUpdateToClient();
    }

    private void destroyInput(ServerLevel level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return;

        Item item = input.getItem();
        int count = input.getCount();

        applyRadiation(level, input, count);

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        AnnihilatorPoolManager.get(level).add(poolName, "item:" + id, count);

        inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
    }

    private void destroyFluid(ServerLevel level) {
        int fill = tank.getFill();
        if (fill <= 0) return;

        Fluid fluid = tank.getTankType();
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        AnnihilatorPoolManager.get(level).add(poolName, "fluid:" + id, fill);

        tank.drainMb(fill);
    }

    /** Port of {@code TileEntityMachineAnnihilator.onDestroy}'s radiation hazard check. */
    private void applyRadiation(ServerLevel level, ItemStack stack, int count) {
        float hazard = HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
        if (hazard <= 0f) return;

        float amount = Math.min(1000f, hazard * count);
        LevelChunk chunk = level.getChunkAt(worldPosition);
        ChunkRadiationAccess.get(chunk).ifPresent(rad ->
                rad.setAmbientRadiation(rad.getAmbientRadiation() + amount));
    }

    private void updateMonitor(ServerLevel level) {
        ItemStack monitorStack = inventory.getStackInSlot(SLOT_MONITOR);
        if (monitorStack.isEmpty()) {
            monitorDisplay = "";
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(monitorStack.getItem());
        BigInteger count = AnnihilatorPoolManager.get(level).get(poolName, "item:" + id);
        monitorDisplay = count.toString();
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putString("pool_name", poolName);
        tag.putString("monitor_display", monitorDisplay);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        poolName = tag.contains("pool_name") ? tag.getString("pool_name") : DEFAULT_POOL;
        monitorDisplay = tag.getString("monitor_display");
        tank.readFromNBT(tag, "tank");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.annihilator");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Beide Slots sind reine Ablage-Slots (Input wird sofort vernichtet, Monitor ist nur
        // eine Anzeige-Referenz) - Automation legt hier ab, die Maschine nimmt selbst nichts weg.
        return slot == SLOT_INPUT || slot == SLOT_MONITOR;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineAnnihilatorMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = (poolName == null || poolName.isBlank()) ? DEFAULT_POOL : poolName.trim();
        setChanged();
        sendUpdateToClient();
    }

    /** Kumulierter Zaehlerstand fuer das aktuell im Monitor-Slot liegende Item, als String (BigInteger). */
    public String getMonitorDisplay() {
        return monitorDisplay;
    }
}
