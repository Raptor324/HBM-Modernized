package com.hbm_m.blockentity.machines.fusion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fusion.IFusionPowerReceiver;
import com.hbm_m.api.fusion.PlasmaNetwork;
import com.hbm_m.api.fusion.PlasmaNetworkProvider;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.machines.fusion.FusionMultiblockBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFusionBreederMenu;
import com.hbm_m.handler.fusion.FluidBreederRecipes;
import com.hbm_m.handler.fusion.FluidBreederRecipes.FluidBreederRecipe;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes;
import com.hbm_m.handler.rbmk.RBMKOutgasserRecipes.OutgasserRecipe;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionBreeder} (1.7.10).
 *
 * <p>Der Brueter haengt als Empfaenger im Plasmanetz, nimmt aber <b>keine</b> Plasmaleistung ab
 * ({@code receivesFusionPower() == false}) - er lebt allein vom Neutronenfluss des Rezepts.
 * Damit bestrahlt er Gegenstaende (Rezepttabelle des Outgassers) oder Fluide
 * ({@link FluidBreederRecipes}).</p>
 */
public class FusionBreederBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, IFusionPowerReceiver {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int INVENTORY_SIZE = 3;

    /** Original: {@code TileEntityFusionBreeder.capacity} - auch die Bezugsgroesse der Rezept-Fluxwerte. */
    public static final double CAPACITY = 10_000D;

    private GenNode<PlasmaNetwork> plasmaNode;

    public final FluidTank[] tanks;

    public double neutronEnergy;
    public double neutronEnergySync;
    public double progress;

    public FusionBreederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_BREEDER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
        this.tanks = new FluidTank[] { new FluidTank(16_000), new FluidTank(16_000) };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionBreederBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {

        // Original: tanks[0].setType(0, slots) - der Fluid-Identifier im Slot legt den Eingangstyp fest.
        ItemStack id = getInventory().getStackInSlot(SLOT_FLUID_ID);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier identifier) {
            var fluid = identifier.getType(level, pos, id);
            if (fluid != null && fluid != tanks[0].getTankType()) tanks[0].setTankType(fluid);
        }

        if (!canProcessSolid() && !canProcessLiquid()) {
            this.progress = 0;
        }

        // Der zu synchronisierende Wert muss bis zum naechsten Tick ueberleben, weil die
        // Tile-Updates in beliebiger Reihenfolge laufen (Original-Kommentar).
        this.neutronEnergySync = this.neutronEnergy;

        for (NodeDirPos con : getConPos(pos, state)) {
            if (tanks[0].getTankType() != Fluids.EMPTY) {
                trySubscribe(tanks[0].getTankType(), level, con.getPos(), con.getDir());
            }
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, con.getPos(), con.getDir());
        }

        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = state.getValue(FusionMultiblockBlock.FACING).getOpposite();
            BlockPos nodePos = pos.offset(dir.getStepX() * 2, 2, dir.getStepZ() * 2);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetworkProvider.THE_PROVIDER);

            if (plasmaNode == null) {
                plasmaNode = new GenNode<>(PlasmaNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(pos.offset(dir.getStepX() * 3, 2, dir.getStepZ() * 3), dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }

        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);

        setChanged();
        sendUpdateToClient();

        this.neutronEnergy = 0;
    }

    // ═════════════════════════════ Verarbeitung ═════════════════════════════

    public boolean canProcessSolid() {
        ItemStack input = getInventory().getStackInSlot(SLOT_INPUT);
        ItemStack output = getInventory().getStackInSlot(SLOT_OUTPUT);
        if (input.isEmpty()) return false;

        if (input.is(ModItems.METEORITE_SWORD_IRRADIATED.get()) && output.isEmpty()) return true;

        OutgasserRecipe recipe = RBMKOutgasserRecipes.getRecipe(input);
        if (recipe == null) return false;

        if (recipe.hasFluid()) {
            if (tanks[1].getTankType() != recipe.fluidType() && tanks[1].getFill() > 0) return false;
            tanks[1].setTankType(recipe.fluidType());
            if (tanks[1].getFill() + recipe.fluidAmount() > tanks[1].getMaxFill()) return false;
        }

        if (output.isEmpty() || !recipe.hasSolid()) return true;

        ItemStack out = recipe.solidOutput();
        return output.getItem() == out.getItem()
                && output.getCount() + out.getCount() <= output.getMaxStackSize();
    }

    public boolean canProcessLiquid() {
        FluidBreederRecipe recipe = FluidBreederRecipes.getOutput(tanks[0].getTankType());
        if (recipe == null) return false;
        if (tanks[0].getFill() < recipe.amountIn()) return false;

        if (tanks[1].getTankType() != recipe.output() && tanks[1].getFill() > 0) return false;
        tanks[1].setTankType(recipe.output());
        return tanks[1].getFill() + recipe.amountOut() <= tanks[1].getMaxFill();
    }

    private void processSolid() {
        ItemStack input = getInventory().getStackInSlot(SLOT_INPUT);

        if (input.is(ModItems.METEORITE_SWORD_IRRADIATED.get())) {
            input.shrink(1);
            getInventory().setStackInSlot(SLOT_INPUT, input);
            getInventory().setStackInSlot(SLOT_OUTPUT, new ItemStack(ModItems.METEORITE_SWORD_FUSED.get()));
            this.progress = 0;
            return;
        }

        OutgasserRecipe recipe = RBMKOutgasserRecipes.getRecipe(input);
        if (recipe == null) return;

        input.shrink(1);
        getInventory().setStackInSlot(SLOT_INPUT, input);
        this.progress = 0;

        if (recipe.hasFluid()) {
            tanks[1].setFill(tanks[1].getFill() + recipe.fluidAmount());
        }

        if (recipe.hasSolid()) {
            ItemStack out = recipe.solidOutput();
            ItemStack current = getInventory().getStackInSlot(SLOT_OUTPUT);
            if (current.isEmpty()) {
                getInventory().setStackInSlot(SLOT_OUTPUT, out.copy());
            } else {
                current.grow(out.getCount());
                getInventory().setStackInSlot(SLOT_OUTPUT, current);
            }
        }
    }

    private void processLiquid() {
        FluidBreederRecipe recipe = FluidBreederRecipes.getOutput(tanks[0].getTankType());
        if (recipe == null) return;
        tanks[0].setFill(tanks[0].getFill() - recipe.amountIn());
        tanks[1].setFill(tanks[1].getFill() + recipe.amountOut());
    }

    /** Original: {@code doProgress()} - wird direkt aus {@code receiveFusionPower} heraus getaktet. */
    public void doProgress() {
        if (canProcessSolid()) {
            this.progress += this.neutronEnergy;
            if (progress > CAPACITY) {
                processSolid();
                progress = 0;
                setChanged();
            }
        } else if (canProcessLiquid()) {
            this.progress += this.neutronEnergy;
            if (progress > CAPACITY) {
                processLiquid();
                progress = 0;
                setChanged();
            }
        } else {
            progress = 0;
        }
    }

    @Override
    public boolean receivesFusionPower() {
        return false;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.neutronEnergy = neutronPower;
        doProgress();
    }

    /** Original: {@code getConPos()} - Plasmaseite plus vier seitliche Rohranschluesse. */
    public NodeDirPos[] getConPos(BlockPos pos, BlockState state) {
        Direction dir = state.getValue(FusionMultiblockBlock.FACING);
        Direction rot = dir.getClockWise();

        return new NodeDirPos[] {
                new NodeDirPos(pos.offset(dir.getStepX() * 3, 2, dir.getStepZ() * 3), dir),
                new NodeDirPos(pos.offset(rot.getStepX() * 2, 0, rot.getStepZ() * 2), rot),
                new NodeDirPos(pos.offset(-rot.getStepX() * 2, 0, -rot.getStepZ() * 2), rot.getOpposite()),
                new NodeDirPos(pos.offset(dir.getStepX() + rot.getStepX() * 2, 0, dir.getStepZ() + rot.getStepZ() * 2), rot),
                new NodeDirPos(pos.offset(dir.getStepX() - rot.getStepX() * 2, 0, dir.getStepZ() - rot.getStepZ() * 2), rot.getOpposite())
        };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_FLUID_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == SLOT_INPUT) return RBMKOutgasserRecipes.getRecipe(stack) != null
                || stack.is(ModItems.METEORITE_SWORD_IRRADIATED.get());
        return false;
    }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel && plasmaNode != null) {
            UniNodespace.destroyNode(serverLevel, plasmaNode);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("t0", tanks[0].writeNBT(new CompoundTag()));
        tag.put("t1", tanks[1].writeNBT(new CompoundTag()));
        tag.putDouble("progress", progress);
        tag.putDouble("neutron", neutronEnergySync);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("t0")) tanks[0].readNBT(tag.getCompound("t0"));
        if (tag.contains("t1")) tanks[1].readNBT(tag.getCompound("t1"));
        this.progress = tag.getDouble("progress");
        this.neutronEnergy = tag.getDouble("neutron");
        this.neutronEnergySync = this.neutronEnergy;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.fusion_breeder");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFusionBreederMenu.create(id, inventory, this);
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                    worldPosition.getX() + 3, worldPosition.getY() + 4, worldPosition.getZ() + 3);
        }
        return renderBounds;
    }
}
