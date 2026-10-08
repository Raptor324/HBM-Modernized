package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.MachinePollutingBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IConditionalInvAccess;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.menu.MachineRotaryFurnaceMenu;
import com.hbm_m.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm_m.inventory.recipes.RotaryFurnaceRecipes.RecipeInput;
import com.hbm_m.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe;
import com.hbm_m.module.ModuleBurnTime;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineRotaryFurnace}: 5x5-Drehrohrofen ohne Strom. Bis zu drei Eingaben plus optionales Fluid
 * (Typ ueber den Identifikator in Slot 3) werden mit Festbrennstoff (Slot 4, Brenndauer halbiert, Waermebonus des
 * {@link #burnModule} beschleunigt den Vorgang) und Dampf zu einer Schmelze; der Ofen giesst sie 2,875 Bloecke seitlich
 * in eine Giessanlage. Verbrauchter Dampf kommt als Abdampf (1 je 100) zurueck, Russ geht ueber den Schornstein.
 */
public class MachineRotaryFurnaceBlockEntity extends MachinePollutingBlockEntity implements IFluidStandardTransceiverMK2, IConditionalInvAccess {

    public static final int SLOT_IN1 = 0, SLOT_IN2 = 1, SLOT_IN3 = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_FUEL = 4;
    public static final int INVENTORY_SIZE = 5;

    public final FluidTank[] tanks;
    public boolean isProgressing;
    public float progress;
    public int burnTime;
    public double burnHeat = 1D;
    public int maxBurnTime;
    public int steamUsed = 0;
    public boolean isVenting;
    @Nullable public MaterialStack output;
    public static final int maxOutput = MaterialShapes.BLOCK.q(16);

    public int anim;
    public int lastAnim;

    /** Given this has no heat, the heat mod instead affects the progress per fuel **/
    public static ModuleBurnTime burnModule = new ModuleBurnTime()
            .setCokeTimeMod(1.25)
            .setRocketTimeMod(1.5)
            .setSolidTimeMod(1.5)
            .setBalefireTimeMod(1.5)

            .setSolidHeatMod(1.5)
            .setRocketHeatMod(3)
            .setBalefireHeatMod(10);

    public MachineRotaryFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROTARY_FURNACE_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L, 50);
        tanks = new FluidTank[3];
        tanks[0] = new FluidTank(ModFluids.NONE.getSource(), 16_000);
        tanks[1] = new FluidTank(ModFluids.STEAM.getSource(), 12_000);
        tanks[2] = new FluidTank(ModFluids.SPENTSTEAM.getSource(), 120);
        // Original pollute(): Ueberlauf eines Rauchtanks laesst den Ofen abblasen
        this.smokeTanks.onOverflow(() -> this.isVenting = true);
    }

    private Direction getDir() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRotaryFurnaceBlockEntity be) {
        if (!level.isClientSide()) be.serverTick((ServerLevel) level, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        Direction dir = getDir();
        Direction rot = dir.getCounterClockWise();

        ItemStack[] slots = slotArray();
        if (tanks[0].setType(SLOT_FLUID_ID, slots)) applySlotArray(slots);

        for (BlockPos p : getSteamPos()) {
            Direction d = dir.getOpposite();
            this.trySubscribe(tanks[1].getTankType(), level, p, d);
            if (tanks[2].getFill() > 0) this.tryProvide(tanks[2], level, p, d);
        }
        if (tanks[0].getTankType() != ModFluids.NONE.getSource()) for (BlockPos p : getFluidPos()) {
            this.trySubscribe(tanks[0].getTankType(), level, p, rot);
        }

        if (smoke.getFill() > 0) this.tryProvide(smoke, level, new BlockPos(pos.getX() + rot.getStepX(), pos.getY() + 5, pos.getZ() + rot.getStepZ()), Direction.UP);

        if (this.output != null) {

            int prev = this.output.amount;
            double[] impact = new double[3];
            MaterialStack leftover = CrucibleUtil.pourSingleStack(level, pos.getX() + 0.5D + rot.getStepX() * 2.875D, pos.getY() + 1.25D, pos.getZ() + 0.5D + rot.getStepZ() * 2.875D, 6, true, this.output, MaterialShapes.INGOT.q(1), impact);
            this.output = leftover;

            if (prev != this.output.amount) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "foundry");
                data.putInt("color", leftover.material.moltenColor);
                data.putByte("dir", (byte) rot.get3DDataValue());
                data.putFloat("off", 0.625F);
                data.putFloat("base", 0.625F);
                data.putFloat("len", Math.max(1F, pos.getY() + 1 - (float) (Math.ceil(impact[1]) - 1.125)));
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(level, pos.getX() + 0.5D + rot.getStepX() * 2.875D, pos.getY() + 0.75, pos.getZ() + 0.5D + rot.getStepZ() * 2.875D, 50, data);
            }

            if (output.amount <= 0) this.output = null;
        }

        RotaryFurnaceRecipe recipe = RotaryFurnaceRecipes.getRecipe(inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2));
        this.isProgressing = false;

        if (recipe != null) {

            ItemStack fuel = inventory.getStackInSlot(SLOT_FUEL);
            if (this.burnTime <= 0 && !fuel.isEmpty() && AbstractFurnaceBlockEntity.isFuel(fuel)) {
                this.burnHeat = burnModule.getMod(fuel, burnModule.getModHeat());
                this.maxBurnTime = this.burnTime = burnModule.getBurnTime(fuel) / 2;
                inventory.extractItem(SLOT_FUEL, 1, false);
                this.setChanged();
            }

            float processSpeed = Math.max((float) burnHeat, 1);
            float steamUseMult = (float) (10 * Math.log10(processSpeed) + 1);

            if (this.canProcess(recipe, steamUseMult)) {
                this.progress += processSpeed / recipe.duration;

                tanks[1].setFill((int) (tanks[1].getFill() - recipe.steam * steamUseMult));
                steamUsed += recipe.steam * steamUseMult;
                this.isProgressing = true;

                if (this.progress >= 1F) {
                    this.progress -= 1F;
                    this.consumeItems(recipe);

                    if (this.output == null) {
                        this.output = recipe.output.copy();
                    } else {
                        this.output.amount += recipe.output.amount;
                    }
                    this.setChanged();
                }

                if (this.burnTime > 0) {
                    this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 10F);
                    this.burnTime--;
                }

            } else {
                this.progress = 0;
            }

            if (this.steamUsed >= 100) {
                int steamReturn = this.steamUsed / 100;
                int canReturn = tanks[2].getMaxFill() - tanks[2].getFill();
                int doesReturn = Math.min(steamReturn, canReturn);
                this.steamUsed -= doesReturn * 100;
                tanks[2].setFill(tanks[2].getFill() + doesReturn);
            }

        } else {
            this.progress = 0;
        }

        // Original: networkPackNT nach dem Ruecksetzen von isVenting - hier wird vorher gesendet, damit der
        // Client das Abblasen ueberhaupt sieht (die Pollute-Ueberlaeufe passieren innerhalb dieses Ticks)
        setChanged();
        sendUpdateToClient();
        this.isVenting = false;
    }

    private void clientTick(Level level, BlockPos pos) {
        Direction dir = getDir();
        Direction rot = dir.getCounterClockWise();

        Player me = null;
        for (Player p : level.players()) if (p.isLocalPlayer()) { me = p; break; }

        if (this.burnTime > 0 && me != null && me.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 25 * 25) {
            RandomSource rand = level.random;
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5 + dir.getStepX() * 0.5 + rot.getStepX() + rand.nextGaussian() * 0.25, pos.getY() + 0.375,
                    pos.getZ() + 0.5 + dir.getStepZ() * 0.5 + rot.getStepZ() + rand.nextGaussian() * 0.25, 0, 0, 0);
        }

        if (isVenting && level.getGameTime() % 2 == 0) {

            CompoundTag fx = new CompoundTag();
            fx.putString("type", "tower");
            fx.putFloat("lift", 10F);
            fx.putFloat("base", 0.25F);
            fx.putFloat("max", 2.5F);
            fx.putInt("life", 100 + level.random.nextInt(20));
            fx.putInt("color", 0x202020);
            fx.putDouble("posX", pos.getX() + 0.5 + rot.getStepX());
            fx.putDouble("posY", pos.getY() + 5);
            fx.putDouble("posZ", pos.getZ() + 0.5 + rot.getStepZ());
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
        }
        this.lastAnim = this.anim;
        if (this.isProgressing) {
            this.anim += (int) Math.max(burnModule.getMod(inventory.getStackInSlot(SLOT_FUEL), burnModule.getModHeat()), 1);
        }
    }

    /** Original {@code getSteamPos()}: hinter der Rueckwand, Richtung {@code dir.getOpposite()}. */
    public BlockPos[] getSteamPos() {
        Direction dir = getDir();
        Direction rot = dir.getCounterClockWise();
        BlockPos p = worldPosition;
        return new BlockPos[] {
                p.relative(dir, -2).relative(rot, -2),
                p.relative(dir, -2).relative(rot, -1)
        };
    }

    /** Original {@code getFluidPos()}: Seitenanschluesse vorn und hinten, Richtung {@code rot}. */
    public BlockPos[] getFluidPos() {
        Direction dir = getDir();
        Direction rot = dir.getCounterClockWise();
        BlockPos p = worldPosition;
        return new BlockPos[] {
                p.relative(dir, 1).relative(rot, 3),
                p.relative(dir, -1).relative(rot, 3)
        };
    }

    public boolean canProcess(RotaryFurnaceRecipe recipe, float steamUseMult) {

        if (this.burnTime <= 0) return false;

        if (recipe.fluid != null) {
            if (this.tanks[0].getTankType() != recipe.fluid.type()) return false;
            if (this.tanks[0].getFill() < recipe.fluid.fill()) return false;
        }

        if (tanks[1].getFill() < recipe.steam * steamUseMult) return false;
        if (tanks[2].getMaxFill() - tanks[2].getFill() < recipe.steam * steamUseMult / 100) return false;
        if (this.steamUsed > 100) return false;

        if (this.output != null) {
            if (this.output.material != recipe.output.material) return false;
            if (this.output.amount + recipe.output.amount > maxOutput) return false;
        }

        return true;
    }

    public void consumeItems(RotaryFurnaceRecipe recipe) {

        for (RecipeInput aStack : recipe.ingredients) {

            for (int i = 0; i < 3; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (aStack.matchesRecipe(stack) && stack.getCount() >= aStack.stacksize()) {
                    inventory.extractItem(i, aStack.stacksize(), false);
                    break;
                }
            }
        }

        if (recipe.fluid != null) {
            this.tanks[0].setFill(tanks[0].getFill() - recipe.fluid.fill());
        }
    }

    // ── Inventar ─────────────────────────────────────────────────────────────

    private ItemStack[] slotArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
        setChanged();
    }

    /** Original {@code isItemValidForSlot}: Eingaben und Brennstoff; Slot 3 nimmt in der GUI den Identifikator. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_FLUID_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        return slot < 3 || slot == SLOT_FUEL;
    }

    //? if forge {
    /**
     * Original {@code getAccessibleSlotsFromSide(x, y, z, side)}: die drei Rueckwandzellen (rot, gelb, gruen) fuehren
     * je in einen Eingabeslot, die vordere Brennstoffklappe in Slot 4. Entnehmen ist nirgends moeglich.
     */
    @Override
    public net.minecraftforge.items.IItemHandler getConditionalItemHandler(BlockPos part, @Nullable Direction side) {
        if (side == null) return null;
        Direction dir = getDir();
        Direction rot = dir.getClockWise();
        BlockPos core = worldPosition;

        int slot = -1;
        if (side == dir.getOpposite() && part.equals(core.relative(dir, -1).relative(rot, -2))) slot = 0;
        else if (side == dir.getOpposite() && part.equals(core.relative(dir, -1).relative(rot, -1))) slot = 1;
        else if (side == dir.getOpposite() && part.equals(core.relative(dir, -1))) slot = 2;
        else if (side == dir && part.equals(core.relative(dir, 1).relative(rot, -1))) slot = 4;

        if (slot < 0) return null;
        final int target = slot;
        return new net.minecraftforge.items.IItemHandler() {
            @Override public int getSlots() { return 1; }
            @Override public @NotNull ItemStack getStackInSlot(int s) { return inventory.getStackInSlot(target); }
            @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack stack, boolean simulate) {
                if (!isItemValidForSlot(target, stack)) return stack;
                return inventory.insertItem(target, stack, simulate);
            }
            @Override public @NotNull ItemStack extractItem(int s, int amount, boolean simulate) { return ItemStack.EMPTY; }
            @Override public int getSlotLimit(int s) { return inventory.getSlotLimit(target); }
            @Override public boolean isItemValid(int s, @NotNull ItemStack stack) { return isItemValidForSlot(target, stack); }
        };
    }

    /** Original {@code getAccessibleSlotsFromSide(side)}: der Kern selbst bietet keinen Zugriff. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return net.minecraftforge.common.util.LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }
    //?} elif neoforge {
    /*/^*
     * Original {@code getAccessibleSlotsFromSide(x, y, z, side)}: die drei Rueckwandzellen (rot, gelb, gruen) fuehren
     * je in einen Eingabeslot, die vordere Brennstoffklappe in Slot 4. Entnehmen ist nirgends moeglich.
     ^/
    @Override
    public net.neoforged.neoforge.items.IItemHandler getConditionalItemHandler(BlockPos part, @Nullable Direction side) {
        if (side == null) return null;
        Direction dir = getDir();
        Direction rot = dir.getClockWise();
        BlockPos core = worldPosition;

        int slot = -1;
        if (side == dir.getOpposite() && part.equals(core.relative(dir, -1).relative(rot, -2))) slot = 0;
        else if (side == dir.getOpposite() && part.equals(core.relative(dir, -1).relative(rot, -1))) slot = 1;
        else if (side == dir.getOpposite() && part.equals(core.relative(dir, -1))) slot = 2;
        else if (side == dir && part.equals(core.relative(dir, 1).relative(rot, -1))) slot = 4;

        if (slot < 0) return null;
        final int target = slot;
        return new net.neoforged.neoforge.items.IItemHandler() {
            @Override public int getSlots() { return 1; }
            @Override public @NotNull ItemStack getStackInSlot(int s) { return inventory.getStackInSlot(target); }
            @Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack stack, boolean simulate) {
                if (!isItemValidForSlot(target, stack)) return stack;
                return inventory.insertItem(target, stack, simulate);
            }
            @Override public @NotNull ItemStack extractItem(int s, int amount, boolean simulate) { return ItemStack.EMPTY; }
            @Override public int getSlotLimit(int s) { return inventory.getSlotLimit(target); }
            @Override public boolean isItemValid(int s, @NotNull ItemStack stack) { return isItemValidForSlot(target, stack); }
        };
    }

    /^* Original {@code getAccessibleSlotsFromSide(side)}: der Kern selbst bietet keinen Zugriff. ^/
    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            return com.hbm_m.platform.LazyCap.empty();
        }
        return super.getHbmCapability(cap, side);
    }
    *///?}

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tanks[0], tanks[1], tanks[2], smoke }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[2], smoke }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0], tanks[1] }; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        this.tanks[0].writeToNBT(nbt, "t0");
        this.tanks[1].writeToNBT(nbt, "t1");
        this.tanks[2].writeToNBT(nbt, "t2");
        nbt.putFloat("prog", progress);
        nbt.putInt("burn", burnTime);
        nbt.putDouble("heat", burnHeat);
        nbt.putInt("maxBurn", maxBurnTime);
        nbt.putInt("steamUsed", steamUsed);
        nbt.putBoolean("isVenting", isVenting);
        nbt.putBoolean("isProgressing", isProgressing);
        if (this.output != null) {
            nbt.putInt("outType", this.output.material.id);
            nbt.putInt("outAmount", this.output.amount);
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.tanks[0].readFromNBT(nbt, "t0");
        this.tanks[1].readFromNBT(nbt, "t1");
        this.tanks[2].readFromNBT(nbt, "t2");
        this.progress = nbt.getFloat("prog");
        this.burnTime = nbt.getInt("burn");
        this.burnHeat = nbt.getDouble("heat");
        this.maxBurnTime = nbt.getInt("maxBurn");
        this.steamUsed = nbt.getInt("steamUsed");
        this.isVenting = nbt.getBoolean("isVenting");
        this.isProgressing = nbt.getBoolean("isProgressing");
        this.output = null;
        if (nbt.contains("outType")) {
            NTMMaterial mat = Mats.matById.get(nbt.getInt("outType"));
            if (mat != null) this.output = new MaterialStack(mat, nbt.getInt("outAmount"));
        }
    }

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRotaryFurnace");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineRotaryFurnaceMenu(id, inventory, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 5, worldPosition.getZ() + 3);
        return bb;
    }
}
