package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.MachineCrucibleMenu;
import com.hbm_m.inventory.recipes.CrucibleRecipes;
import com.hbm_m.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityCrucible}: Schmelztiegel, beheizt von einer {@link IHeatSource} darunter (Diffusion 0,25, max.
 * 100000 TU). Ab halber Hitze schmilzt er Stueck fuer Stueck (20000 TU je Stueck) aus den Plaetzen 1-9 in einen
 * Rezept- und einen Abfallstapel (je 16 Bloecke). Ein per GUI gewaehltes Legierungsrezept wird in seinem Takt
 * ausgefuehrt; der Rezeptstapel giesst nach vorne aus, der Abfall nach hinten. Gegenstaende ueber dem Tiegel werden
 * eingesammelt, Wesen darin verbrannt.
 */
public class MachineCrucibleBlockEntity extends BaseMachineBlockEntity implements ICrucibleAcceptor, com.hbm_m.api.tile.IControlReceiver {

    public int heat;
    public int progress;

    public String recipe = "null";

    public List<MaterialStack> recipeStack = new ArrayList<>();
    public List<MaterialStack> wasteStack = new ArrayList<>();

    public static int recipeZCapacity = MaterialShapes.BLOCK.q(16);
    public static int wasteZCapacity = MaterialShapes.BLOCK.q(16);
    public static int processTime = 20_000;
    public static double diffusion = 0.25D;
    public static int maxHeat = 100_000;

    public MachineCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE_BE.get(), pos, state, 10, 0L, 0L, 0L);
    }

    /** Original {@code getInventoryStackLimit() = 1} - prevents clogging. */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCrucibleBlockEntity be) {
        if (level instanceof ServerLevel server) be.serverTick(server, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(ServerLevel world, BlockPos pos) {
        tryPullHeat();

        /* collect items */
        if (world.getGameTime() % 5 == 0) {
            List<ItemEntity> list = world.getEntitiesOfClass(ItemEntity.class, new AABB(pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 1, pos.getZ() + 1.5));

            for (ItemEntity item : list) {
                if (!item.isAlive()) continue;
                ItemStack stack = item.getItem();
                if (this.isItemSmeltable(stack)) {

                    for (int i = 1; i < 10; i++) {
                        if (inventory.getStackInSlot(i).isEmpty()) {

                            if (stack.getCount() == 1) {
                                inventory.setStackInSlot(i, stack.copy());
                                item.discard();
                                break;
                            } else {
                                inventory.setStackInSlot(i, stack.copyWithCount(1));
                                stack.shrink(1);
                                item.setItem(stack);
                            }

                            this.setChanged();
                        }
                    }
                }
            }
        }

        int totalCap = recipeZCapacity + wasteZCapacity;
        int totalMass = 0;

        for (MaterialStack stack : recipeStack) totalMass += stack.amount;
        for (MaterialStack stack : wasteStack) totalMass += stack.amount;

        double level = ((double) totalMass / (double) totalCap) * 0.875D;

        List<LivingEntity> living = world.getEntitiesOfClass(LivingEntity.class, new AABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5 + level, pos.getZ() + 0.5).inflate(1, 0, 1));
        for (LivingEntity entity : living) {
            entity.hurt(world.damageSources().lava(), 5F);
            PlatformHooks.setSecondsOnFire(entity, 5);
        }

        /* smelt items from buffer */
        if (!trySmelt()) {
            this.progress = 0;
        }

        tryRecipe();

        /* pour waste stack */
        if (!this.wasteStack.isEmpty()) {

            Direction dir = facing().getOpposite();
            double[] impact = new double[3];
            MaterialStack didPour = CrucibleUtil.pourFullStack(world, pos.getX() + 0.5D + dir.getStepX() * 1.875D, pos.getY() + 0.25D, pos.getZ() + 0.5D + dir.getStepZ() * 1.875D, 6, true, this.wasteStack, MaterialShapes.NUGGET.q(3), impact);

            if (didPour != null) {
                pourFx(world, pos, dir, didPour, impact);
            }

            PollutionHandler.incrementPollution(world, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 20F);
        }

        /* pour recipe stack */
        if (!this.recipeStack.isEmpty()) {

            Direction dir = facing();
            List<MaterialStack> toCast = new ArrayList<>();

            CrucibleRecipe recipe = this.getLoadedRecipe();
            //if no recipe is loaded, everything from the recipe stack will be drainable
            if (recipe == null) {
                toCast.addAll(this.recipeStack);
            } else {

                for (MaterialStack stack : this.recipeStack) {
                    for (MaterialStack output : recipe.output) {
                        if (stack.material == output.material) {
                            toCast.add(stack);
                            break;
                        }
                    }
                }
            }

            double[] impact = new double[3];
            MaterialStack didPour = CrucibleUtil.pourFullStack(world, pos.getX() + 0.5D + dir.getStepX() * 1.875D, pos.getY() + 0.25D, pos.getZ() + 0.5D + dir.getStepZ() * 1.875D, 6, true, toCast, MaterialShapes.NUGGET.q(3), impact);

            if (didPour != null) {
                pourFx(world, pos, dir, didPour, impact);
            }

            PollutionHandler.incrementPollution(world, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 20F);
        }

        /* clean up stacks */
        this.recipeStack.removeIf(o -> o.amount <= 0);
        this.wasteStack.removeIf(x -> x.amount <= 0);

        /* sync */
        setChanged();
        sendUpdateToClient();
    }

    private void pourFx(ServerLevel world, BlockPos pos, Direction dir, MaterialStack didPour, double[] impact) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "foundry");
        data.putInt("color", didPour.material.moltenColor);
        data.putByte("dir", (byte) dir.get3DDataValue());
        data.putFloat("off", 0.625F);
        data.putFloat("base", 0.625F);
        data.putFloat("len", Math.max(1F, pos.getY() - (float) (Math.ceil(impact[1]) - 0.875)));
        com.hbm_m.particle.helper.IParticleCreator.sendPacket(world, pos.getX() + 0.5D + dir.getStepX() * 1.875D, pos.getY(), pos.getZ() + 0.5D + dir.getStepZ() * 1.875D, 50, data);
    }

    private void clientTick(Level world, BlockPos pos) {
        if (!this.recipeStack.isEmpty() || !this.wasteStack.isEmpty()) {

            if (world.getGameTime() % 10 == 0) {
                CompoundTag fx = new CompoundTag();
                fx.putString("type", "tower");
                fx.putFloat("lift", 10F);
                fx.putFloat("base", 0.75F);
                fx.putFloat("max", 3.5F);
                fx.putInt("life", 100 + world.random.nextInt(20));
                fx.putInt("color", 0x202020);
                fx.putDouble("posX", pos.getX() + 0.5);
                fx.putDouble("posY", pos.getY() + 1);
                fx.putDouble("posZ", pos.getZ() + 0.5);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
            }
        }
    }

    protected void tryPullHeat() {

        if (this.heat >= maxHeat) return;

        BlockEntity con = level.getBlockEntity(worldPosition.below());

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;

            if (diff == 0) {
                return;
            }

            diff = Math.min(diff, maxHeat - this.heat);

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

    protected boolean trySmelt() {

        if (this.heat < maxHeat / 2) return false;

        int slot = this.getFirstSmeltableSlot();
        if (slot == -1) return false;

        int delta = this.heat - (maxHeat / 2);
        delta *= 0.05;

        this.progress += delta;
        this.heat -= delta;

        if (this.progress >= processTime) {
            this.progress = 0;

            List<MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(inventory.getStackInSlot(slot));
            CrucibleRecipe recipe = getLoadedRecipe();

            for (MaterialStack material : materials) {
                boolean recipeMaterial = recipe != null && (getQuantaFromType(recipe.input, material.material) > 0 || getQuantaFromType(recipe.output, material.material) > 0);

                if (recipeMaterial) {
                    this.addToStack(this.recipeStack, material);
                } else {
                    this.addToStack(this.wasteStack, material);
                }
            }

            inventory.extractItem(slot, 1, false);
        }

        return true;
    }

    protected void tryRecipe() {
        CrucibleRecipe recipe = this.getLoadedRecipe();

        if (recipe == null) return;
        if (level.getGameTime() % recipe.frequency > 0) return;

        for (MaterialStack stack : recipe.input) {
            if (getQuantaFromType(this.recipeStack, stack.material) < stack.amount) return;
        }

        for (MaterialStack stack : this.recipeStack) {
            stack.amount -= getQuantaFromType(recipe.input, stack.material);
        }

        outer:
        for (MaterialStack out : recipe.output) {

            for (MaterialStack stack : this.recipeStack) {
                if (stack.material == out.material) {
                    stack.amount += out.amount;
                    continue outer;
                }
            }

            this.recipeStack.add(out.copy());
        }
    }

    protected int getFirstSmeltableSlot() {

        for (int i = 1; i < 10; i++) {

            ItemStack stack = inventory.getStackInSlot(i);

            if (!stack.isEmpty() && isItemSmeltable(stack)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    protected boolean isItemValidForSlot(int i, ItemStack stack) {
        return isItemSmeltable(stack);
    }

    public boolean isItemSmeltable(ItemStack stack) {

        List<MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(stack);

        //if there's no materials in there at all, don't smelt
        if (materials.isEmpty()) return false;
        CrucibleRecipe recipe = getLoadedRecipe();

        //needs to be true, will always be true if there's no recipe loaded
        boolean matchesRecipe = recipe == null;

        //the amount of material in the entire recipe input
        int recipeContent = recipe != null ? recipe.getInputAmount() : 0;
        //the total amount of the current waste stack, used for simulation
        int recipeAmount = getQuantaFromType(this.recipeStack, null);
        int wasteAmount = getQuantaFromType(this.wasteStack, null);

        for (MaterialStack mat : materials) {
            //if no recipe is loaded, everything will land in the waste stack
            int recipeInputRequired = recipe != null ? getQuantaFromType(recipe.input, mat.material) : 0;

            //this allows pouring the output material back into the crucible
            if (recipe != null && getQuantaFromType(recipe.output, mat.material) > 0) {
                recipeAmount += mat.amount;
                matchesRecipe = true;
                continue;
            }

            if (recipeInputRequired == 0) {
                //if this type isn't required by the recipe, add it to the waste stack
                wasteAmount += mat.amount;
            } else {

                //the maximum is the recipe's ratio scaled up to the recipe stack's capacity
                int matMaximum = recipeInputRequired * recipeZCapacity / recipeContent;
                int amountStored = getQuantaFromType(recipeStack, mat.material);

                matchesRecipe = true;
                recipeAmount += mat.amount;

                //if the amount of that input would exceed the amount dictated by the recipe, return false
                if (recipe != null && amountStored + mat.amount > matMaximum)
                    return false;
            }
        }

        //if the amount doesn't exceed the capacity and the recipe matches (or isn't null), return true
        return recipeAmount <= recipeZCapacity && wasteAmount <= wasteZCapacity && matchesRecipe;
    }

    public void addToStack(List<MaterialStack> stack, MaterialStack matStack) {

        for (MaterialStack mat : stack) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }

        stack.add(matStack.copy());
    }

    @Nullable
    public CrucibleRecipe getLoadedRecipe() {
        return CrucibleRecipes.get(recipe);
    }

    public int getQuantaFromType(MaterialStack[] stacks, @Nullable NTMMaterial mat) {
        for (MaterialStack stack : stacks) {
            if (mat == null || stack.material == mat) {
                return stack.amount;
            }
        }
        return 0;
    }

    public int getQuantaFromType(List<MaterialStack> stacks, @Nullable NTMMaterial mat) {
        int sum = 0;
        for (MaterialStack stack : stacks) {
            if (stack.material == mat) {
                return stack.amount;
            }
            if (mat == null) {
                sum += stack.amount;
            }
        }
        return sum;
    }

    // ── ICrucibleAcceptor ────────────────────────────────────────────────────

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) {

        CrucibleRecipe recipe = getLoadedRecipe();

        if (recipe == null) {
            return getQuantaFromType(this.wasteStack, null) < wasteZCapacity;
        }

        int recipeContent = recipe.getInputAmount();
        int recipeInputRequired = getQuantaFromType(recipe.input, stack.material);
        int matMaximum = recipeInputRequired * recipeZCapacity / recipeContent;
        int amountStored = getQuantaFromType(recipeStack, stack.material);

        return amountStored < matMaximum && getQuantaFromType(this.recipeStack, null) < recipeZCapacity;
    }

    @Override
    @Nullable
    public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) {

        CrucibleRecipe recipe = getLoadedRecipe();

        if (recipe == null) {

            int amount = getQuantaFromType(this.wasteStack, null);

            if (amount + stack.amount <= wasteZCapacity) {
                this.addToStack(this.wasteStack, stack.copy());
                return null;
            } else {
                int toAdd = wasteZCapacity - amount;
                this.addToStack(this.wasteStack, new MaterialStack(stack.material, toAdd));
                return new MaterialStack(stack.material, stack.amount - toAdd);
            }
        }

        int recipeContent = recipe.getInputAmount();
        int recipeInputRequired = getQuantaFromType(recipe.input, stack.material);
        int matMaximum = recipeInputRequired * recipeZCapacity / recipeContent;

        if (recipeInputRequired + stack.amount <= matMaximum) {
            this.addToStack(this.recipeStack, stack.copy());
            return null;
        }

        int toAdd = matMaximum - stack.amount;
        toAdd = Math.min(toAdd, recipeZCapacity - getQuantaFromType(this.recipeStack, null));
        this.addToStack(this.recipeStack, new MaterialStack(stack.material, toAdd));
        return new MaterialStack(stack.material, stack.amount - toAdd);
    }

    @Override public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override @Nullable public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return null; }

    // ── Steuerung ────────────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            int index = data.getInt("index");
            String selection = data.getString("selection");
            if (index == 0) {
                this.recipe = selection;
                this.setChanged();
            }
        }
    }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);

        this.recipe = nbt.getString("recipe");

        recipeStack.clear();
        wasteStack.clear();

        int[] rec = nbt.getIntArray("rec");
        for (int i = 0; i < rec.length / 2; i++) {
            NTMMaterial mat = Mats.matById.get(rec[i * 2]);
            if (mat != null) recipeStack.add(new MaterialStack(mat, rec[i * 2 + 1]));
        }

        int[] was = nbt.getIntArray("was");
        for (int i = 0; i < was.length / 2; i++) {
            NTMMaterial mat = Mats.matById.get(was[i * 2]);
            if (mat != null) wasteStack.add(new MaterialStack(mat, was[i * 2 + 1]));
        }

        this.progress = nbt.getInt("progress");
        this.heat = nbt.getInt("heat");
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);

        nbt.putString("recipe", this.recipe);

        int[] rec = new int[recipeStack.size() * 2];
        int[] was = new int[wasteStack.size() * 2];
        for (int i = 0; i < recipeStack.size(); i++) { MaterialStack sta = recipeStack.get(i); rec[i * 2] = sta.material.id; rec[i * 2 + 1] = sta.amount; }
        for (int i = 0; i < wasteStack.size(); i++) { MaterialStack sta = wasteStack.get(i); was[i * 2] = sta.material.id; was[i * 2 + 1] = sta.amount; }
        nbt.putIntArray("rec", rec);
        nbt.putIntArray("was", was);
        nbt.putInt("progress", progress);
        nbt.putInt("heat", heat);
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /** Original {@code getAccessibleSlotsFromSide}: Plaetze 1-9, nur Schmelzbares hinein, nichts heraus. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return 9; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot + 1); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (!isItemSmeltable(stack)) return stack;
                    return inventory.insertItem(slot + 1, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
                @Override public int getSlotLimit(int slot) { return 1; }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isItemSmeltable(stack); }
            })).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sided.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        sided.clear();
    }
    //?} elif neoforge {
    /*private final Map<Direction, com.hbm_m.platform.LazyCap<net.neoforged.neoforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /^* Original {@code getAccessibleSlotsFromSide}: Plaetze 1-9, nur Schmelzbares hinein, nichts heraus. ^/
    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> com.hbm_m.platform.LazyCap.of(() -> new net.neoforged.neoforge.items.IItemHandler() {
                @Override public int getSlots() { return 9; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot + 1); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (!isItemSmeltable(stack)) return stack;
                    return inventory.insertItem(slot + 1, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
                @Override public int getSlotLimit(int slot) { return 1; }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isItemSmeltable(stack); }
            })).cast();
        }
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sided.values().forEach(com.hbm_m.platform.LazyCap::invalidate);
        sided.clear();
    }
    *///?}

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCrucible");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineCrucibleMenu(id, inventory, this);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
    }
}
