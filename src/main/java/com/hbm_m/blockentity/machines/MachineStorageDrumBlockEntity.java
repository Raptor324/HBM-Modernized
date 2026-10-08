package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.config.VersatileConfig;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityStorageDrum}: 24 Einzelplaetze fuer Atommuell. Langlebiger Muell zerfaellt im Schnitt nach
 * 3 h, kurzlebiger nach 15 min (kleine Brocken zehnmal so oft) und gibt dabei je nach Abfallklasse fluessigen und
 * gasfoermigen Abfall in zwei 16000-mB-Tanks ab, die ans Rohrnetz abgeben und bei Ueberlauf ins Freie entweichen.
 * Au-198, Pb-209 und Sr-90 zerfallen zu Quecksilber, Bismut und Zirkonium. Der Inhalt strahlt Lebewesen im Umkreis
 * von 32 Bloecken an, abgeschwaecht durch die Explosionsfestigkeit der Bloecke dazwischen.
 */
public class MachineStorageDrumBlockEntity extends BaseMachineBlockEntity implements IFluidStandardSenderMK2 {

    public static final int INVENTORY_SIZE = 24;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.WASTEFLUID.getSource(), 16000),
            new FluidTank(ModFluids.WASTEGAS.getSource(), 16000)
    };
    public int age = 0;

    /** Port: Activierungsabbau aus der 1.12-Linie fuer Stoffe ohne eigenen Zerfallspfad (zehn Sekunden Halbwertszeit). */
    private static final float DECAY_RATE = 0.9965402628F;

    public MachineStorageDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_STORAGE_DRUM_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    /** Original {@code getInventoryStackLimit() = 1}. */
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

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineStorageDrumBlockEntity be) {
        if (level instanceof ServerLevel server) be.serverTick(server, pos);
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        float rad = 0;

        int liquid = 0;
        int gas = 0;

        int longChance = VersatileConfig.getLongDecayChance();
        int shortChance = VersatileConfig.getShortDecayChance();

        for (int i = 0; i < 24; i++) {

            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            if (world.getGameTime() % 20 == 0) {
                rad += HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
            }

            boolean decayed = false;

            for (var path : com.hbm_m.item.special.WasteClasses.LONG_PATHS) {
                int index = path.indexOf(stack);
                if (index < 0) continue;
                if (world.random.nextInt(path.tiny() ? longChance / 10 : longChance) == 0) {
                    liquid += path.liquidAt(index);
                    gas += path.gasAt(index);
                    inventory.setStackInSlot(i, path.spentStack(index));
                }
                decayed = true;
            }

            for (var path : com.hbm_m.item.special.WasteClasses.SHORT_PATHS) {
                int index = path.indexOf(stack);
                if (index < 0) continue;
                if (world.random.nextInt(path.tiny() ? shortChance / 10 : shortChance) == 0) {
                    liquid += path.liquidAt(index);
                    gas += path.gasAt(index);
                    inventory.setStackInSlot(i, path.spentStack(index));
                }
                decayed = true;
            }

            Item item = stack.getItem();

            if (item == item("au198_ingot") && world.random.nextInt(shortChance / 20) == 0) {
                inventory.setStackInSlot(i, new ItemStack(item("nugget_mercury")));
            } else if (item == item("nugget_au198") && world.random.nextInt(shortChance / 100) == 0) {
                inventory.setStackInSlot(i, new ItemStack(item("nugget_mercury_tiny")));
            } else if (item == item("pb209_ingot") && world.random.nextInt(shortChance / 10) == 0) {
                inventory.setStackInSlot(i, new ItemStack(item("bismuth_ingot")));
            } else if (item == item("nugget_pb209") && world.random.nextInt(shortChance / 50) == 0) {
                inventory.setStackInSlot(i, new ItemStack(item("nugget_bismuth")));
            } else if (item == item("sr90_powder") && world.random.nextInt(shortChance / 10) == 0) {
                inventory.setStackInSlot(i, new ItemStack(item("zirconium_powder")));
            } else if (item == item("nugget_sr90") && world.random.nextInt(shortChance / 50) == 0) {
                inventory.setStackInSlot(i, new ItemStack(item("nugget_zirconium")));
            } else if (!decayed) {
                ContaminationUtil.neutronActivateItem(inventory.getStackInSlot(i), 0.0F, DECAY_RATE);
            }
        }

        this.tanks[0].setFill(this.tanks[0].getFill() + liquid);
        this.tanks[1].setFill(this.tanks[1].getFill() + gas);

        for (int i = 0; i < 2; i++) {

            int overflow = Math.max(this.tanks[i].getFill() - this.tanks[i].getMaxFill(), 0);

            if (overflow > 0) {
                this.tanks[i].setFill(this.tanks[i].getFill() - overflow);
                FluidType.forFluid(this.tanks[i].getTankType()).onFluidRelease(this, this.tanks[i], overflow);
            }
        }

        age++;

        if (age >= 20)
            age -= 20;

        for (Direction dir : Direction.values()) {
            if (tanks[0].getFill() > 0) this.tryProvide(tanks[0], world, pos.relative(dir), dir);
            if (tanks[1].getFill() > 0) this.tryProvide(tanks[1], world, pos.relative(dir), dir);
        }

        setChanged();
        sendUpdateToClient();

        if (rad > 0) {
            radiate(world, pos, rad);
        }
    }

    private void radiate(Level world, BlockPos pos, float rads) {

        double range = 32D;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                new AABB(x + 0.5, y + 0.5, z + 0.5, x + 0.5, y + 0.5, z + 0.5).inflate(range, range, range));

        for (LivingEntity e : entities) {

            Vec3 vec = new Vec3(e.getX() - (x + 0.5), (e.getY() + e.getEyeHeight()) - (y + 0.5), e.getZ() - (z + 0.5));
            double len = vec.length();
            vec = vec.normalize();

            float res = 0;

            for (int i = 1; i < len; i++) {

                int ix = (int) Math.floor(x + 0.5 + vec.x * i);
                int iy = (int) Math.floor(y + 0.5 + vec.y * i);
                int iz = (int) Math.floor(z + 0.5 + vec.z * i);

                res += world.getBlockState(new BlockPos(ix, iy, iz)).getBlock().getExplosionResistance();
            }

            if (res < 1)
                res = 1;

            float eRads = rads;
            eRads /= res;
            eRads /= (float) (len * len);

            ContaminationUtil.contaminate(e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);
        }
    }

    private static boolean isInsertable(ItemStack stack) {
        for (var path : com.hbm_m.item.special.WasteClasses.LONG_PATHS) {
            if (path.indexOf(stack) >= 0) return true;
        }
        for (var path : com.hbm_m.item.special.WasteClasses.SHORT_PATHS) {
            if (path.indexOf(stack) >= 0) return true;
        }
        return stack.getItem() == item("au198_ingot");
    }

    private static boolean isExtractable(ItemStack stack) {
        for (var path : com.hbm_m.item.special.WasteClasses.LONG_PATHS) {
            if (com.hbm_m.item.PartTabMetaItems.group(path.spent()).contains(stack.getItem())) return true;
        }
        for (var path : com.hbm_m.item.special.WasteClasses.SHORT_PATHS) {
            if (com.hbm_m.item.PartTabMetaItems.group(path.spent()).contains(stack.getItem())) return true;
        }
        return stack.getItem() == item("nugget_mercury");
    }

    /** In der GUI nimmt das Fass alles (die Container-Slots des Originals pruefen nichts); Automatik siehe unten. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /** Original {@code canExtractItem}: nur abgereicherter Muell und Quecksilber. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return INVENTORY_SIZE; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (!isInsertable(stack)) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (!isExtractable(inventory.getStackInSlot(slot))) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return 1; }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return isInsertable(stack); }
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

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[0], tanks[1] }; }
    @Override public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank getLiquidTank() { return tanks[0]; }
    public FluidTank getGasTank() { return tanks[1]; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        this.tanks[0].writeToNBT(tag, "liquid");
        this.tanks[1].writeToNBT(tag, "gas");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.tanks[0].readFromNBT(tag, "liquid");
        this.tanks[1].readFromNBT(tag, "gas");
        // Aeltere Port-Welten speicherten die Tanks unter eigenen Schluesseln.
        if (tag.contains("liquidTank")) tanks[0].readNBT(tag.getCompound("liquidTank"));
        if (tag.contains("gasTank")) tanks[1].readNBT(tag.getCompound("gasTank"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_storage_drum");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineStorageDrumMenu.create(id, inventory, this);
    }
}
