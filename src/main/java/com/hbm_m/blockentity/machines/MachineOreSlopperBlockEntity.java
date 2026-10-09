package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineOreSlopperMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.worldgen.BedrockOreDensity;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineOreSlopper}: schlaemmt Grundgestein-Erz ({@code bedrock_ore_base}) mit Wasser zu den sechs
 * sortierten Basiserzen; das Wasser wird 1:1 zu Schlamm (SLOP). 200 HE/t, ein Vorgang dauert {@code 600 - speed*150}
 * Ticks; Effizienz-Upgrades steigern die Ausbeute um 10 % je Stufe. Wesen im Schredderbereich ueber dem Becken werden
 * zerhackt. Animation: Schaufel senkt/hebt sich, Schlitten faehrt zum Schredder und zurueck, Messer und Luefter drehen.
 */
public class MachineOreSlopperBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2,
        com.hbm_m.interfaces.IUpgradeInfoProvider {

    /** Original: 0 Batterie, 1 Fluid-ID, 2 Eingang, 3-8 Ausgaenge, 9-10 Upgrades. */
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_INPUT = 2;
    public static final int OUTPUT_START = 3;
    public static final int OUTPUT_END = 8;
    public static final int SLOT_UPGRADE_1 = 9;
    public static final int SLOT_UPGRADE_2 = 10;
    public static final int INVENTORY_SIZE = 11;

    public static final long maxPower = 100_000;

    public static final int waterUsedBase = 1_000;
    public int waterUsed = waterUsedBase;
    public static final long consumptionBase = 200;
    public long consumption = consumptionBase;

    public float progress;
    public boolean processing;

    public enum SlopperAnimation { LOWERING, LIFTING, MOVE_SHREDDER, DUMPING, MOVE_BUCKET }

    public SlopperAnimation animation = SlopperAnimation.LOWERING;
    public float slider;
    public float prevSlider;
    public float bucket;
    public float prevBucket;
    public float blades;
    public float prevBlades;
    public float fan;
    public float prevFan;
    public int delay;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.WATER.getSource(), 16_000),
            new FluidTank(ModFluids.SLOP.getSource(), 16_000)
    };
    public double[] ores = new double[BedrockOreDensity.Type.values().length];

    public final UpgradeManager upgradeManager = new UpgradeManager();

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.EFFECT, 3);
    }

    public MachineOreSlopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_SLOPPER_BE.get(), pos, state, INVENTORY_SIZE, maxPower, maxPower, 0L);
    }

    /** Original {@code ItemBedrockOreNew.make(BedrockOreGrade.BASE, type)}. */
    private static RegistrySupplier<Item> outputItemSupplier(BedrockOreDensity.Type type) {
        return switch (type) {
            case LIGHT -> ModItems.BEDROCK_ORE_BASE_LIGHT;
            case HEAVY -> ModItems.BEDROCK_ORE_BASE_HEAVY;
            case RARE -> ModItems.BEDROCK_ORE_BASE_RARE;
            case ACTINIDE -> ModItems.BEDROCK_ORE_BASE_ACTINIDE;
            case NONMETAL -> ModItems.BEDROCK_ORE_BASE_NONMETAL;
            case CRYSTAL -> ModItems.BEDROCK_ORE_BASE_CRYSTAL;
        };
    }

    /** Original {@code ItemBedrockOreBase.getOreAmount}. */
    private static double getOreAmount(ItemStack stack, BedrockOreDensity.Type type) {
        CompoundTag nbt = PlatformHooks.getItemTag(stack);
        if (nbt == null) return 0;
        return nbt.getDouble(type.name().toLowerCase(Locale.ROOT));
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineOreSlopperBlockEntity be) {
        if (level instanceof ServerLevel server) be.serverTick(server, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(ServerLevel level, BlockPos pos) {

        chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack[] slots = slotArray();
        if (tanks[0].setType(SLOT_FLUID_ID, slots)) applySlotArray(slots);
        Fluid conversion = getFluidOutput(tanks[0].getTankType());
        if (conversion != null) tanks[1].setTankType(conversion);

        for (DirPos con : getConPos()) {
            this.trySubscribe(level, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            this.trySubscribe(tanks[0].getTankType(), level, con.pos, con.dir);
            if (tanks[1].getFill() > 0) this.tryProvide(tanks[1], level, con.pos, con.dir);
        }

        this.processing = false;

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int efficiency = upgradeManager.getLevel(UpgradeType.EFFECT);

        this.consumption = consumptionBase + (consumptionBase * speed) / 2 + (consumptionBase * efficiency);

        if (canSlop()) {
            setEnergyStored(getEnergyStored() - this.consumption);
            this.progress += 1F / (600 - speed * 150);
            this.processing = true;
            boolean markDirty = false;

            while (progress >= 1F && canSlop()) {
                progress -= 1F;

                ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
                for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
                    ores[type.ordinal()] += (getOreAmount(input, type) * (1D + efficiency * 0.1));
                }

                ItemStack in = input.copy();
                in.shrink(1);
                inventory.setStackInSlot(SLOT_INPUT, in.isEmpty() ? ItemStack.EMPTY : in);
                this.tanks[0].setFill(this.tanks[0].getFill() - waterUsed);
                this.tanks[1].setFill(this.tanks[1].getFill() + waterUsed);
                markDirty = true;
            }

            if (markDirty) this.setChanged();

            Direction dir = facing();
            List<Entity> entities = level.getEntitiesOfClass(Entity.class,
                    new AABB(pos.getX() - 0.5, pos.getY() + 1, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 3, pos.getZ() + 1.5)
                            .move(dir.getStepX(), 0, dir.getStepZ()));

            for (Entity e : entities) {
                e.hurt(ModDamageSources.blender(level), 1000F);

                if (!e.isAlive() && e instanceof LivingEntity) {
                    CompoundTag vdat = new CompoundTag();
                    vdat.putString("type", "giblets");
                    vdat.putInt("ent", e.getId());
                    vdat.putInt("cDiv", 5);
                    com.hbm_m.particle.helper.IParticleCreator.sendPacket(level, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 150, vdat);

                    level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F, 0.95F + level.random.nextFloat() * 0.2F);
                }
            }

        } else {
            this.progress = 0;
        }

        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            Item output = outputItemSupplier(type).get();
            outer:
            while (ores[type.ordinal()] >= 1) {
                for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
                    ItemStack s = inventory.getStackInSlot(i);
                    if (!s.isEmpty() && s.getItem() == output && s.getCount() < s.getMaxStackSize()) {
                        ItemStack grown = s.copy();
                        grown.grow(1);
                        inventory.setStackInSlot(i, grown);
                        ores[type.ordinal()] -= 1F;
                        continue outer;
                    }
                }
                for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
                    if (inventory.getStackInSlot(i).isEmpty()) {
                        inventory.setStackInSlot(i, new ItemStack(output));
                        ores[type.ordinal()] -= 1F;
                        continue outer;
                    }
                }
                break outer;
            }
        }

        sendUpdateToClient();
    }

    private void clientTick(Level level, BlockPos pos) {

        this.prevSlider = this.slider;
        this.prevBucket = this.bucket;
        this.prevBlades = this.blades;
        this.prevFan = this.fan;

        if (this.processing) {

            this.blades += 15F;
            this.fan += 35F;

            if (blades >= 360) {
                blades -= 360;
                prevBlades -= 360;
            }

            if (fan >= 360) {
                fan -= 360;
                prevFan -= 360;
            }

            Player me = clientPlayer(level);
            if (animation == SlopperAnimation.DUMPING && me != null
                    && me.distanceToSqr(pos.getX() + 0.5, pos.getY() + 4, pos.getZ() + 0.5) <= 50 * 50) {
                Direction dir = facing();
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                        pos.getX() + 0.5 + dir.getStepX() + level.random.nextGaussian() * 0.25, pos.getY() + 4.25,
                        pos.getZ() + 0.5 + dir.getStepZ() + level.random.nextGaussian() * 0.25, 0, -0.2D, 0);
            }

            if (delay > 0) {
                delay--;
                return;
            }

            switch (animation) {
                case LOWERING -> {
                    this.bucket += 1F / 40F;
                    if (bucket >= 1F) {
                        bucket = 1F;
                        animation = SlopperAnimation.LIFTING;
                        delay = 20;
                    }
                }
                case LIFTING -> {
                    this.bucket -= 1F / 40F;
                    if (bucket <= 0) {
                        bucket = 0F;
                        animation = SlopperAnimation.MOVE_SHREDDER;
                        delay = 10;
                    }
                }
                case MOVE_SHREDDER -> {
                    this.slider += 1 / 50F;
                    if (slider >= 1F) {
                        slider = 1F;
                        animation = SlopperAnimation.DUMPING;
                        delay = 60;
                    }
                }
                case DUMPING -> animation = SlopperAnimation.MOVE_BUCKET;
                case MOVE_BUCKET -> {
                    this.slider -= 1 / 50F;
                    if (slider <= 0F) {
                        animation = SlopperAnimation.LOWERING;
                        delay = 10;
                    }
                }
            }
        }
    }

    /** Original {@code MainRegistry.proxy.me()}: der lokale Spieler dieser Client-Welt. */
    @Nullable
    private static Player clientPlayer(Level level) {
        for (Player p : level.players()) if (p.isLocalPlayer()) return p;
        return null;
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        Direction dir = facing();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;

        return new DirPos[] {
                new DirPos(p.relative(dir, 4), dir),
                new DirPos(p.relative(dir, -4), dir.getOpposite()),
                new DirPos(p.relative(rot, 2), rot),
                new DirPos(p.relative(rot, -2), rot.getOpposite()),
                new DirPos(p.relative(dir, 2).relative(rot, 2), rot),
                new DirPos(p.relative(dir, 2).relative(rot, -2), rot.getOpposite()),
                new DirPos(p.relative(dir, -2).relative(rot, 2), rot),
                new DirPos(p.relative(dir, -2).relative(rot, -2), rot.getOpposite())
        };
    }

    public boolean canSlop() {
        if (getFluidOutput(tanks[0].getTankType()) == null) return false;
        if (tanks[0].getFill() < waterUsed) return false;
        if (tanks[1].getFill() + waterUsed > tanks[1].getMaxFill()) return false;
        if (getEnergyStored() < consumption) return false;

        ItemStack in = inventory.getStackInSlot(SLOT_INPUT);
        return !in.isEmpty() && in.getItem() == ModItems.BEDROCK_ORE_BASE.get();
    }

    @Nullable
    public Fluid getFluidOutput(Fluid input) {
        if (input == ModFluids.WATER.getSource()) return ModFluids.SLOP.getSource();
        return null;
    }

    // ── Inventar-Hilfen ──────────────────────────────────────────────────────

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

    /** Original {@code isItemValidForSlot}: nur Slot 2 (Grundgestein-Erz) automatisiert; GUI-Plaetze zusaetzlich offen. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) return stack.getItem() == ModItems.BEDROCK_ORE_BASE.get();
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == SLOT_FLUID_ID) return true;
        if (slot == SLOT_UPGRADE_1 || slot == SLOT_UPGRADE_2) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /** Original {@code slot_access {2..8}}: Slot 2 einfuegbar, 3-8 entnehmbar. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return 7; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot + SLOT_INPUT); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (slot != 0 || stack.getItem() != ModItems.BEDROCK_ORE_BASE.get()) return stack;
                    return inventory.insertItem(SLOT_INPUT, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot == 0) return ItemStack.EMPTY;
                    return inventory.extractItem(slot + SLOT_INPUT, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot + SLOT_INPUT); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot == 0 && stack.getItem() == ModItems.BEDROCK_ORE_BASE.get(); }
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

    /^* Original {@code slot_access {2..8}}: Slot 2 einfuegbar, 3-8 entnehmbar. ^/
    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> com.hbm_m.platform.LazyCap.of(() -> new net.neoforged.neoforge.items.IItemHandler() {
                @Override public int getSlots() { return 7; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot + SLOT_INPUT); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (slot != 0 || stack.getItem() != ModItems.BEDROCK_ORE_BASE.get()) return stack;
                    return inventory.insertItem(SLOT_INPUT, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot == 0) return ItemStack.EMPTY;
                    return inventory.extractItem(slot + SLOT_INPUT, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot + SLOT_INPUT); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot == 0 && stack.getItem() == ModItems.BEDROCK_ORE_BASE.get(); }
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

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getWaterTank() { return tanks[0]; }
    public FluidTank getSlopTank() { return tanks[1]; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putFloat("progress", progress);
        tag.putLong("consumption", consumption);
        tag.putBoolean("processing", processing);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "slop");
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            tag.putDouble("ores_" + type.name().toLowerCase(Locale.ROOT), ores[type.ordinal()]);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getFloat("progress");
        if (tag.contains("consumption")) consumption = tag.getLong("consumption");
        processing = tag.getBoolean("processing");
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "slop");
        for (BedrockOreDensity.Type type : BedrockOreDensity.Type.values()) {
            ores[type.ordinal()] = tag.getDouble("ores_" + type.name().toLowerCase(Locale.ROOT));
        }
    }

    // ── Upgrades ─────────────────────────────────────────────────────────────

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.EFFECT;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_EFFICIENCY, "+" + (level * 10) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.ore_slopper");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineOreSlopperMenu(containerId, playerInventory, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 7, worldPosition.getZ() + 4);
        return bb;
    }
}
