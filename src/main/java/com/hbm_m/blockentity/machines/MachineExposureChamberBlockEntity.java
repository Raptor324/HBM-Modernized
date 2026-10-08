package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.menu.MachineExposureChamberMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.ExposureChamberRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineExposureChamber}. Slots: 0 Partikelkapsel, 1 geladene Partikel (intern), 2 leerer
 * Behaelter, 3 Zutat, 4 Ausgabe, 5 Batterie, 6-7 Upgrades. Eine Kapsel liefert acht Belichtungen; ihr Behaelter
 * wandert in Slot 2. Upgrades: Geschwindigkeit (-25 % Zeit, +50 % Verbrauch je zwei Stufen), Energiesparen,
 * Overdrive. Strom ueber die fuenf Anschluesse am Beschleunigerende.
 */
public class MachineExposureChamberBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.interfaces.IUpgradeInfoProvider {

    public static final int SLOT_PARTICLE = 0;
    public static final int SLOT_PARTICLE_INTERNAL = 1;
    public static final int SLOT_CONTAINER = 2;
    public static final int SLOT_INGREDIENT = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int SLOT_UPGRADE_1 = 6;
    public static final int SLOT_UPGRADE_2 = 7;

    public static final long maxPower = 1_000_000;
    public static final int processTimeBase = 200;
    public static final int consumptionBase = 10_000;
    public static final int maxParticles = 8;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public int progress;
    public int processTime = processTimeBase;
    public int consumption = consumptionBase;
    public int savedParticles;
    public boolean isOn = false;
    public float rotation;
    public float prevRotation;

    /** Original Slot 1 (dort mit Stapelgroesse 0 als reiner Merker; wird nicht fallen gelassen). */
    private ItemStack loadedParticle = ItemStack.EMPTY;

    public final UpgradeManager upgradeManager = new UpgradeManager();

    public MachineExposureChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPOSURE_CHAMBER_BE.get(), pos, state, 8, maxPower, maxPower, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineExposureChamberBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
        else be.clientTick();
    }

    private void serverTick(ServerLevel world) {

        this.isOn = false;
        chargeFromBatterySlot(SLOT_BATTERY);

        if (world.getGameTime() % 20 == 0) {
            for (DirPos con : getConPos()) this.trySubscribe(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
        }

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overdriveLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        this.consumption = consumptionBase;

        this.processTime = processTimeBase - processTimeBase / 4 * speedLevel;
        this.consumption *= (speedLevel / 2 + 1);
        this.processTime *= (powerLevel / 2 + 1);
        this.consumption /= (powerLevel + 1);
        this.processTime /= (overdriveLevel + 1);
        this.consumption *= (overdriveLevel * 2 + 1);

        ItemStack capsule = inventory.getStackInSlot(SLOT_PARTICLE);
        ItemStack ingredient = inventory.getStackInSlot(SLOT_INGREDIENT);

        if (loadedParticle.isEmpty() && !capsule.isEmpty() && !ingredient.isEmpty() && this.savedParticles <= 0) {
            ExposureChamberRecipe recipe = this.getRecipe(capsule, ingredient);

            if (recipe != null) {

                ItemStack container = capsule.getCraftingRemainingItem();

                boolean canStore = false;

                if (container.isEmpty()) {
                    canStore = true;
                } else {
                    ItemStack stored = inventory.getStackInSlot(SLOT_CONTAINER);
                    if (stored.isEmpty()) {
                        inventory.setStackInSlot(SLOT_CONTAINER, container.copy());
                        canStore = true;
                    } else if (stored.getItem() == container.getItem() && stored.getCount() < stored.getMaxStackSize()) {
                        stored.grow(1);
                        inventory.setStackInSlot(SLOT_CONTAINER, stored);
                        canStore = true;
                    }
                }

                if (canStore) {
                    loadedParticle = capsule.copyWithCount(1);
                    inventory.extractItem(SLOT_PARTICLE, 1, false);
                    this.savedParticles = maxParticles;
                }
            }
        }

        if (!loadedParticle.isEmpty() && this.savedParticles > 0 && this.energy >= this.consumption) {
            ExposureChamberRecipe recipe = this.getRecipe(loadedParticle, inventory.getStackInSlot(SLOT_INGREDIENT));
            ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);

            if (recipe != null && (out.isEmpty() || (out.getItem() == recipe.getOutput().getItem()
                    && out.getCount() + recipe.getOutput().getCount() <= out.getMaxStackSize()))) {
                this.progress++;
                this.energy -= this.consumption;
                this.isOn = true;

                if (this.progress >= this.processTime) {
                    this.progress = 0;
                    this.savedParticles--;
                    inventory.extractItem(SLOT_INGREDIENT, 1, false);

                    if (out.isEmpty()) {
                        inventory.setStackInSlot(SLOT_OUTPUT, recipe.getOutput().copy());
                    } else {
                        out.grow(recipe.getOutput().getCount());
                        inventory.setStackInSlot(SLOT_OUTPUT, out);
                    }
                }

            } else {
                this.progress = 0;
            }
        } else {
            this.progress = 0;
        }

        if (this.savedParticles <= 0) {
            loadedParticle = ItemStack.EMPTY;
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        this.prevRotation = this.rotation;

        if (this.isOn) {

            this.rotation += 10F;

            if (this.rotation >= 720F) {
                this.rotation -= 720F;
                this.prevRotation -= 720F;
            }
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    public DirPos[] getConPos() {
        BlockState state = getBlockState();
        Direction dir = state.hasProperty(com.hbm_m.block.machines.DummyableMachineBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING) : Direction.NORTH;
        Direction rot = dir.getCounterClockWise(); // Original: dir.getRotation(UP).getOpposite()
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.relative(rot, 7).relative(dir, 2), dir),
                new DirPos(p.relative(rot, 7).relative(dir, -2), dir.getOpposite()),
                new DirPos(p.relative(rot, 8).relative(dir, 2), dir),
                new DirPos(p.relative(rot, 8).relative(dir, -2), dir.getOpposite()),
                new DirPos(p.relative(rot, 9), rot)
        };
    }

    @Nullable
    public ExposureChamberRecipe getRecipe(ItemStack particle, ItemStack ingredient) {
        if (level == null) return null;
        for (ExposureChamberRecipe recipe : RecipeHooks.getAllRecipes(level, ExposureChamberRecipe.Type.INSTANCE)) {
            if (recipe.matches(particle, ingredient)) return recipe;
        }
        return null;
    }

    /** Original {@code isItemValidForSlot}: nimmt nur, was zu einem Rezept passt, und stopft nichts zu. */
    @Override
    protected boolean isItemValidForSlot(int i, ItemStack stack) {
        ItemStack capsule = inventory.getStackInSlot(SLOT_PARTICLE);
        ItemStack ingredient = inventory.getStackInSlot(SLOT_INGREDIENT);

        if (i == 0 && !capsule.isEmpty()) return true;
        if (i == 3 && !ingredient.isEmpty()) return true;

        ItemStack particle = !loadedParticle.isEmpty() ? loadedParticle : capsule;

        if (i == 0 && particle.isEmpty() && !ingredient.isEmpty()) {
            return getRecipe(stack, ingredient) != null;
        }

        if (i == 3 && !particle.isEmpty() && ingredient.isEmpty()) {
            return getRecipe(capsule, stack) != null;
        }

        if (particle.isEmpty() && ingredient.isEmpty() && level != null) {
            for (ExposureChamberRecipe recipe : RecipeHooks.getAllRecipes(level, ExposureChamberRecipe.Type.INSTANCE)) {
                if (i == 0 && stack.is(recipe.getParticle().getItem())) return true;
                if (i == 3 && recipe.getIngredient().test(stack)) return true;
            }
        }

        return false;
    }

    public int getProgressScaled(int scale) { return progress * scale / (processTime + 1); }
    public int getParticlesScaled(int scale) { return savedParticles * scale / maxParticles; }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle((System.currentTimeMillis() / 500) % 2 == 0 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("savedParticles", savedParticles);
        tag.putInt("processTime", processTime);
        tag.putInt("consumption", consumption);
        tag.putBoolean("isOn", isOn);
        if (!loadedParticle.isEmpty()) tag.put("loadedParticle", com.hbm_m.platform.PlatformHooks.saveItemStack(loadedParticle, new CompoundTag(), registries));
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
        savedParticles = tag.getInt("savedParticles");
        processTime = tag.contains("processTime") ? tag.getInt("processTime") : processTimeBase;
        consumption = tag.contains("consumption") ? tag.getInt("consumption") : consumptionBase;
        isOn = tag.getBoolean("isOn");
        loadedParticle = tag.contains("loadedParticle") ? com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("loadedParticle"), registries) : ItemStack.EMPTY;
    }

    // ==================== GUI ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.exposureChamber");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineExposureChamberMenu.create(id, inv, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 8, worldPosition.getY(), worldPosition.getZ() - 8,
                worldPosition.getX() + 9, worldPosition.getY() + 5, worldPosition.getZ() + 9);
        return bb;
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots {0, 2, 3, 4}; Teilchen und Zutat nach Rezept hinein, Behaelter und Ergebnis heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0, 2, 3, 4 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return (slot == 0 || slot == 3) && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot == 2 || slot == 4; }
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
    //?}
}
