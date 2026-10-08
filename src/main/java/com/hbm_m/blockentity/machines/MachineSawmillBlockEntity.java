package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.projectile.SawbladeEntity;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.SimpleCraftingContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntitySawmill}: Saegewerk ohne GUI, angetrieben von einer {@link IHeatSource} darunter (je Tick 10 %
 * der dort gespeicherten Hitze). Ab 100 TU/t laeuft das Blatt und saegt Stamm -> Bretter (Rezept x 6/4), Brett -> 6
 * Stoecke, Stock -> Saegemehl, Setzling -> Stock, mit Saegemehl-Nebenprodukt. Wesen am Blatt werden zersaegt. Ueber
 * 300 TU/t warnt es nach 3 s und wirft nach 15 s das Blatt als {@link SawbladeEntity} heraus.
 */
public class MachineSawmillBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BYPRODUCT = 2;
    private static final int SLOT_COUNT = 3;

    public int heat;
    public static final double diffusion = 0.1D;
    private int warnCooldown = 0;
    private int overspeed = 0;
    public boolean hasBlade = true;
    public int progress = 0;
    public static final int processingTime = 600;

    public float spin;
    public float lastSpin;

    /** Die Hitze wird am Tickende genullt; der Client bekommt den Wert dieses Ticks (Original: networkPackNT davor). */
    private int syncedHeat;

    public MachineSawmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAWMILL_BE.get(), pos, state, SLOT_COUNT, 0L, 0L, 0L);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSawmillBlockEntity be) {
        if (level instanceof ServerLevel server) be.serverTick(server, pos);
        else be.clientTick();
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        if (hasBlade) {
            tryPullHeat(world, pos);

            if (warnCooldown > 0)
                warnCooldown--;

            if (heat >= 100) {

                ItemStack result = this.getOutput(inventory.getStackInSlot(SLOT_INPUT));

                if (result != null) {
                    progress += heat / 10;

                    if (progress >= processingTime) {
                        progress = 0;
                        inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
                        inventory.setStackInSlot(SLOT_OUTPUT, result);

                        if (result.getItem() != ModItems.POWDER_SAWDUST.get()) {
                            float chance = result.getItem() == Items.STICK ? 0.1F : 0.5F;
                            if (world.random.nextFloat() < chance) {
                                inventory.setStackInSlot(SLOT_BYPRODUCT, new ItemStack(ModItems.POWDER_SAWDUST.get()));
                            }
                        }

                        this.setChanged();
                    }

                } else {
                    this.progress = 0;
                }

                AABB aabb = rotationOffset(new AABB(-1D, 0.375D, -1D, -0.875, 2.375D, 1D),
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing().getClockWise());
                for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, aabb)) {
                    if (e.isAlive() && e.hurt(ModDamageSources.blender(world), 100)) {
                        world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F, 0.95F + world.random.nextFloat() * 0.2F);
                        int count = Math.min((int) Math.ceil(e.getMaxHealth() / 4), 250);
                        CompoundTag data = new CompoundTag();
                        data.putString("type", "vanillaburst");
                        data.putInt("count", count * 4);
                        data.putDouble("motion", 0.1D);
                        data.putString("mode", "blockdust");
                        data.putInt("block", Block.getId(Blocks.REDSTONE_BLOCK.defaultBlockState()));
                        com.hbm_m.particle.helper.IParticleCreator.sendPacket(world, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 50, data);
                    }
                }

            } else {
                this.progress = 0;
            }

            if (heat > 300) {

                this.overspeed++;

                if (overspeed > 60 && warnCooldown == 0) {
                    warnCooldown = 100;
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                            com.hbm_m.sound.HbmSoundsNT.get("hbm:block.warnOverspeed"), SoundSource.BLOCKS, 2.0F, 1.0F);
                }

                if (overspeed > 300) {
                    this.hasBlade = false;
                    world.explode(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 5F, false, Level.ExplosionInteraction.NONE);

                    Direction dir = facing();
                    SawbladeEntity cog = SawbladeEntity.create(world, pos.getX() + 0.5 + dir.getStepX(), pos.getY() + 1, pos.getZ() + 0.5 + dir.getStepZ(), dir);
                    Direction rot = dir.getCounterClockWise();

                    cog.setDeltaMovement(rot.getStepX(), 1 + (heat - 100) * 0.0001D, rot.getStepZ());
                    world.addFreshEntity(cog);

                    this.setChanged();
                }

            } else {
                this.overspeed = 0;
            }
        } else {
            this.overspeed = 0;
            this.warnCooldown = 0;
        }

        this.syncedHeat = heat;
        sendUpdateToClient();

        this.heat = 0;
    }

    private void clientTick() {
        float momentum = heat * 25F / ((float) 300);

        this.lastSpin = this.spin;
        this.spin += momentum;

        if (this.spin >= 360F) {
            this.spin -= 360F;
            this.lastSpin -= 360F;
        }
    }

    /** Original {@code BlockDummyable.getAABBRotationOffset} (mit dem dort verschobenen Ursprung). */
    public static AABB rotationOffset(AABB aabb, double x, double y, double z, Direction dir) {
        AABB newBox = switch (dir) {
            case NORTH -> new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
            case EAST -> new AABB(-aabb.maxZ, aabb.minY, aabb.minX, -aabb.minZ, aabb.maxY, aabb.maxX);
            case SOUTH -> new AABB(-aabb.maxX, aabb.minY, -aabb.maxZ, -aabb.minX, aabb.maxY, -aabb.minZ);
            case WEST -> new AABB(aabb.minZ, aabb.minY, -aabb.maxX, aabb.maxZ, aabb.maxY, -aabb.minX);
            default -> null;
        };
        if (newBox != null) return newBox.move(x, y, z);
        return aabb.move(x + 0.5, y + 0.5, z + 0.5);
    }

    protected void tryPullHeat(Level world, BlockPos pos) {
        BlockEntity con = world.getBlockEntity(pos.below());

        if (con instanceof IHeatSource source) {
            int heatSrc = (int) (source.getHeatStored() * diffusion);

            if (heatSrc > 0) {
                source.useUpHeat(heatSrc);
                this.heat += heatSrc;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    /** Original {@code getOutput}: Ore-Dictionary-Namen als Item-Tags, Staemme ueber das 1x1-Werkbankrezept. */
    @Nullable
    public ItemStack getOutput(ItemStack input) {

        if (input == null || input.isEmpty())
            return null;

        if (input.is(Items.STICK) || input.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "rods/wooden")))) {
            return new ItemStack(ModItems.POWDER_SAWDUST.get());
        }

        if (input.is(ItemTags.LOGS) && level != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
            items.set(0, input.copyWithCount(1));
            SimpleCraftingContainer grid = new SimpleCraftingContainer(items, 1, 1);
            for (CraftingRecipe recipe : com.hbm_m.platform.recipe.RecipeHooks.getAllRecipes(level, RecipeType.CRAFTING)) {
                if (com.hbm_m.platform.recipe.RecipeHooks.craftingMatches(recipe, grid, level)) {
                    ItemStack out = com.hbm_m.platform.recipe.RecipeHooks.assembleCrafting(recipe, grid, level);
                    if (!out.isEmpty()) {
                        out = out.copy();
                        out.setCount(out.getCount() * 6 / 4); // 4 planks become 6
                        return out;
                    }
                }
            }
        }

        if (input.is(ItemTags.PLANKS)) {
            return new ItemStack(Items.STICK, 6);
        }

        if (input.is(ItemTags.SAPLINGS)) {
            return new ItemStack(Items.STICK, 1);
        }

        return null;
    }

    /** Original {@code isItemValidForSlot}: nur in ein leeres Saegewerk, nur einzeln, nur Saegbares. */
    @Override
    protected boolean isItemValidForSlot(int i, ItemStack stack) {
        return i == 0 && inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty()
                && inventory.getStackInSlot(2).isEmpty() && stack.getCount() == 1 && getOutput(stack) != null;
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /** Original {@code canExtractItem}: alles ausser dem Eingang. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return SLOT_COUNT; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (slot != 0 || stack.isEmpty()) return stack;
                    ItemStack one = stack.copyWithCount(1);
                    if (!isItemValidForSlot(0, one)) return stack;
                    if (!simulate) inventory.setStackInSlot(0, one);
                    return stack.copyWithCount(stack.getCount() - 1);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot == 0) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return slot == 0 ? 1 : inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot == 0 && isItemValidForSlot(0, stack.copyWithCount(1)); }
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
    //?}

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("hasBlade", hasBlade);
        tag.putInt("progress", progress);
        tag.putInt("heat", syncedHeat);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.hasBlade = !tag.contains("hasBlade") || tag.getBoolean("hasBlade");
        this.progress = tag.getInt("progress");
        this.heat = tag.getInt("heat");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.sawmill");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2, worldPosition.getZ() + 2);
        return bb;
    }
}
