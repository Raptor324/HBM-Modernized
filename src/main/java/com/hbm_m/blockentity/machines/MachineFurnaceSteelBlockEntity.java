package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.MachineFurnaceSteelMenu;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityFurnaceSteel}: Stahlofen ohne Brennstoff, beheizt von einer {@link IHeatSource} unter dem Kern
 * (Diffusion 0,05, max. 100000 TU). Ab einem Drittel der Hoechsthitze schmilzt er in drei Spuren je
 * {@code (heat - max/3) / 10} TU pro Tick bis 40000 TU. Bonusausbeute je geschmolzenem Stueck: Erze 25 %, Staemme
 * 50 %, Teer 50 %; volle 100 % ergeben ein weiteres Ergebnis. Wechselt der Gegenstand einer Spur, verfallen
 * Fortschritt und Bonus.
 */
public class MachineFurnaceSteelBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements MenuProvider {

    public static final int SLOT_INPUT_0 = 0;
    public static final int SLOT_OUTPUT_0 = 3;
    private static final int SLOT_COUNT = 6;

    public static final int processTime = 40_000;
    public static final int maxHeat = 100_000;
    public static final double diffusion = 0.05D;

    public int[] progress = new int[3];
    public int[] bonus = new int[3];
    public int heat;
    public boolean wasOn = false;

    private ItemStack[] lastItems = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isItemValidForSlot(slot, stack);
        }
    };

    public static final int DATA_COUNT = 8;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 3) return progress[index];
            if (index < 6) return bonus[index - 3];
            if (index == 6) return heat;
            if (index == 7) return wasOn ? 1 : 0;
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index < 3) progress[index] = value;
            else if (index < 6) bonus[index - 3] = value;
            else if (index == 6) heat = value;
            else if (index == 7) wasOn = value != 0;
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MachineFurnaceSteelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_STEEL_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() {
        return inventory;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFurnaceSteelBlockEntity be) {
        if (!level.isClientSide()) be.serverTick(level, pos);
        else be.clientTick(level, pos, state);
    }

    private void serverTick(Level world, BlockPos pos) {
        tryPullHeat();

        this.wasOn = false;

        int burn = (heat - maxHeat / 3) / 10;

        for (int i = 0; i < 3; i++) {

            ItemStack slot = inventory.getStackInSlot(i);
            if (slot.isEmpty() || lastItems[i].isEmpty() || !ItemStack.isSameItem(slot, lastItems[i])) {
                progress[i] = 0;
                bonus[i] = 0;
            }

            if (canSmelt(i)) {
                progress[i] += burn;
                this.heat -= burn;
                this.wasOn = true;
                if (world.getGameTime() % 20 == 0) PollutionHandler.incrementPollution(world, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 2);
            }

            lastItems[i] = slot.copy();

            if (progress[i] >= processTime) {
                ItemStack result = getRecipe(world, slot).map(r -> r.getResultItem(world.registryAccess())).orElse(ItemStack.EMPTY);

                ItemStack out = inventory.getStackInSlot(i + 3);
                if (out.isEmpty()) {
                    out = result.copy();
                } else {
                    out.grow(result.getCount());
                }

                this.addBonus(slot, i);

                while (bonus[i] >= 100) {
                    out.setCount(Math.min(out.getMaxStackSize(), out.getCount() + result.getCount()));
                    bonus[i] -= 100;
                }
                inventory.setStackInSlot(i + 3, out);

                inventory.extractItem(i, 1, false);

                progress[i] = 0;
            }
        }

        setChanged();
        world.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
    }

    private void clientTick(Level world, BlockPos pos, BlockState state) {

        if (this.wasOn) {
            Direction dir = state.hasProperty(com.hbm_m.block.machines.DummyableMachineBlock.FACING)
                    ? state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING) : Direction.NORTH;
            Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)

            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 - dir.getStepX() * 1.125 - rot.getStepX() * 0.75, pos.getY() + 2.625,
                    pos.getZ() + 0.5 - dir.getStepZ() * 1.125 - rot.getStepZ() * 0.75, 0.0, 0.05, 0.0);

            if (world.random.nextInt(20) == 0)
                world.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + dir.getStepX() * 0.75, pos.getY() + 2, pos.getZ() + 0.5 + dir.getStepZ() * 0.75, 0.0, 0.05, 0.0);

            if (world.random.nextInt(15) == 0)
                world.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5 + dir.getStepX() * 1.5 + rot.getStepX() * (world.random.nextDouble() - 0.5), pos.getY() + 0.75,
                        pos.getZ() + 0.5 + dir.getStepZ() * 1.5 + rot.getStepZ() * (world.random.nextDouble() - 0.5), dir.getStepX() * 0.5D, 0.05, dir.getStepZ() * 0.5D);
        }
    }

    /** Original {@code addBonus}: Oredict "ore*" 25 %, "log*" 50 %, "anyTar" 50 % - im Port ueber Tags bzw. Teer-Items. */
    protected void addBonus(ItemStack stack, int index) {

        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        var tags = stack.getTags().map(t -> t.location().getPath()).toList();

        for (String name : tags) {
            if (name.startsWith("ores") || name.endsWith("_ores")) { this.bonus[index] += 25; return; }
            if (name.equals("logs") || name.startsWith("logs")) { this.bonus[index] += 50; return; }
        }
        if (id.startsWith("oil_tar")) { this.bonus[index] += 50; }
    }

    protected void tryPullHeat() {

        if (this.heat >= maxHeat) return;

        BlockEntity con = level.getBlockEntity(worldPosition.below());

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;

            if (diff == 0) {
                return;
            }

            if (diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > maxHeat)
                    this.heat = maxHeat;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    public boolean canSmelt(int index) {

        if (this.heat < maxHeat / 3) return false;
        ItemStack slot = inventory.getStackInSlot(index);
        if (slot.isEmpty() || level == null) return false;

        var recipe = getRecipe(level, slot);
        if (recipe.isEmpty()) return false;
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;

        ItemStack out = inventory.getStackInSlot(index + 3);
        if (out.isEmpty()) return true;

        if (!StackNbt.sameItemSameTags(result, out)) return false;
        if (result.getCount() + out.getCount() > out.getMaxStackSize()) return false;

        return true;
    }

    private java.util.Optional<SmeltingRecipe> getRecipe(Level level, ItemStack input) {
        return com.hbm_m.platform.recipe.RecipeHooks.getRecipeFor(level, RecipeType.SMELTING, input);
    }

    /** Original: nur Schmelzbares in die Eingaenge. */
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        if (i < 3) return level != null && getRecipe(level, itemStack).isPresent();
        return false;
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(level, worldPosition, container);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        tag.putIntArray("progress", progress);
        tag.putIntArray("bonus", bonus);
        tag.putInt("heat", heat);
        tag.putBoolean("wasOn", wasOn);

        ListTag list = new ListTag();
        for (int i = 0; i < lastItems.length; i++) {
            if (!lastItems[i].isEmpty()) {
                CompoundTag nbt1 = com.hbm_m.platform.PlatformHooks.saveItemStack(lastItems[i], new CompoundTag(), registries);
                nbt1.putByte("lastItem", (byte) i);
                list.add(nbt1);
            }
        }
        tag.put("lastItems", list);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        int[] p = tag.getIntArray("progress");
        int[] b = tag.getIntArray("bonus");
        if (p.length == 3) progress = p;
        if (b.length == 3) bonus = b;
        heat = tag.getInt("heat");
        wasOn = tag.getBoolean("wasOn");

        lastItems = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
        ListTag list = tag.getList("lastItems", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt1 = list.getCompound(i);
            byte b0 = nbt1.getByte("lastItem");
            if (b0 >= 0 && b0 < lastItems.length) {
                lastItems[b0] = com.hbm_m.platform.PlatformHooks.itemStackOf(nbt1, registries);
            }
        }
    }

    // ==================== GUI ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.furnaceSteel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineFurnaceSteelMenu(id, inv, this, data);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 3, worldPosition.getZ() + 2);
        return bb;
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0-5}; Schmelzgut in 0-2, Ergebnisse 3-5 heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1, 2, 3, 4, 5 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 3 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot > 2; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: Slots {0-5}; Schmelzgut in 0-2, Ergebnisse 3-5 heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 1, 2, 3, 4, 5 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 3 && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot > 2; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
