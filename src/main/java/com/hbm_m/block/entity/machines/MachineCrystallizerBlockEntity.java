package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.block.machines.MachineCrystallizerBlock;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCrystallizerMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.recipe.CrystallizerRecipe;
import com.hbm_m.recipe.CrystallizerRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
//? if forge {
/*import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
//?}

/**
 * Crystallizer BlockEntity — рудный окислитель, порт с 1.7.10.
 *
 * <p>Слоты:</p>
 * <ul>
 *   <li>0 — вход (руда / предмет)</li>
 *   <li>1 — батарея</li>
 *   <li>2 — выход (кристалл)</li>
 *   <li>3 — слот заливки жидкости (ведро/контейнер с кислотой)</li>
 *   <li>4 — слот выхода жидкости (опустевший контейнер)</li>
 *   <li>5, 6 — апгрейды (пока не реализовано)</li>
 *   <li>7 — слот идентификатора жидкости (пока не реализовано)</li>
 * </ul>
 *
 * <p>Логика обработки:</p>
 * <ol>
 *   <li>Зарядка от батареи в слоте 1.</li>
 *   <li>Перенос жидкости из контейнера в слоте 3 в внутренний бак.</li>
 *   <li>Поиск рецепта в {@link CrystallizerRecipes} по входу и текущей жидкости в баке.</li>
 *   <li>Если рецепт найден и есть энергия / кислота / место в выходе — крутим прогресс.</li>
 *   <li>По достижении {@code duration} — выдаём результат, тратим кислоту,
 *       с учётом productivity тратим (или не тратим) вход.</li>
 * </ol>
 */
public class MachineCrystallizerBlockEntity extends BaseMachineBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    private static final String CRYSTALLIZER_SOUND_INSTANCE = "com.hbm_m.sound.CrystallizerSoundInstance";

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_BATTERY = 1;
    private static final int SLOT_OUTPUT = 2;
    private static final int SLOT_FLUID_INPUT = 3;
    private static final int SLOT_FLUID_OUTPUT = 4;
    private static final int SLOT_UPGRADE_1 = 5;
    private static final int SLOT_UPGRADE_2 = 6;
    private static final int SLOT_FLUID_ID = 7;

    private static final int SLOT_COUNT = 8;
    private static final long MAX_POWER = 1_000_000;
    private static final long MAX_RECEIVE = 1_000;
    private static final int TANK_CAPACITY = 8_000;
    private static final int DEFAULT_DURATION = 600;
    private static final int BASE_POWER_PER_TICK = 1_000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    //? if forge {
    /*// FluidTank itself is NOT an IFluidHandler; it exposes Forge handler via getCapability().
    private final LazyOptional<IFluidHandler> tankHandler = tank.getCapability();
    *///?}

    private int progress = 0;
    private int duration = DEFAULT_DURATION;
    private boolean isOn = false;

    // Client-side visual state for the rotating center part.
    public float angle = 0.0F;
    public float prevAngle = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> getDuration();
                case 2 -> (int) (getEnergyStored() & 0xFFFFFFFFL);          // energy low 32 bits
                case 3 -> (int) ((getEnergyStored() >>> 32) & 0xFFFFFFFFL); // energy high 32 bits
                case 4 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);          // maxEnergy low 32 bits
                case 5 -> (int) ((getMaxEnergyStored() >>> 32) & 0xFFFFFFFFL); // maxEnergy high 32 bits
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 6;
        }
    };

    public MachineCrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLIZER.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCrystallizerBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick(level, pos);
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();
        entity.applyFluidIdentifier();
        entity.transferFluidsFromItems();

        // Поиск рецепта по входу + текущей жидкости в баке.
        ItemStack inputStack = entity.inventory.getStackInSlot(SLOT_INPUT);
        FluidStack tankFluid = entity.getTankFluidStack();
        CrystallizerRecipe recipe = CrystallizerRecipes.findRecipe(inputStack, tankFluid);

        boolean wasOn = entity.isOn;
        entity.isOn = false;

        if (recipe != null) {
            // Длительность с учётом апгрейда скорости (пока заглушка — берём из рецепта).
            entity.duration = entity.calcDuration(recipe);

            if (entity.canProcess(recipe)) {
                int powerCost = entity.getPowerRequired();
                entity.setEnergyStored(entity.getEnergyStored() - powerCost);
                entity.progress++;
                entity.isOn = true;

                if (entity.progress >= entity.duration) {
                    entity.progress = 0;
                    entity.processItem(recipe);
                }
                entity.setChanged();
                entity.sendUpdateToClient();
            } else {
                if (entity.progress != 0) {
                    entity.progress = 0;
                    entity.setChanged();
                }
            }
        } else {
            if (entity.progress != 0) {
                entity.progress = 0;
                entity.setChanged();
            }
        }

        // Обновим клиента, если поменялся статус "вкл/выкл" (для рендера и индикаторов).
        if (wasOn != entity.isOn) {
            entity.sendUpdateToClient();
        }
    }


    /**
     * Client-side visuals: rotate the center part while the machine works and spawn
     * small old-style white steam particles from the roof.
     */
    private void clientTick(Level level, BlockPos pos) {
        ClientSoundBootstrap.updateSound(this, isOn, () -> newCrystallizerSoundInstance());

        prevAngle = angle;

        if (isOn) {
            angle += 5.0F;
            if (angle >= 360.0F) {
                angle -= 360.0F;
                prevAngle -= 360.0F;
            }

            // The original 1.7.10 machine used small white smoke/steam puffs, not the
            // large campfire smoke that exists in newer Minecraft versions. CLOUD is
            // visually closer: small white squares that drift upward and fade out.
            if (level.random.nextInt(4) == 0) {
                double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.85D;
                double y = pos.getY() + 6.5D;
                double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.85D;

                double vx = (level.random.nextDouble() - 0.5D) * 0.010D;
                double vy = 0.025D + level.random.nextDouble() * 0.015D;
                double vz = (level.random.nextDouble() - 0.5D) * 0.010D;

                level.addParticle(ParticleTypes.CLOUD, x, y, z, vx, vy, vz);
            }
        }
    }

    private Object newCrystallizerSoundInstance() {
        try {
            return Class.forName(CRYSTALLIZER_SOUND_INSTANCE)
                    .getConstructor(BlockPos.class)
                    .newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Проверяет, можно ли запустить или продолжить крафт.
     */
    private boolean canProcess(CrystallizerRecipe recipe) {
        ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);

        // Хватает ли количества входного предмета.
        if (inputStack.getCount() < recipe.getInputCount()) return false;

        // Хватает ли энергии на тик.
        if (getEnergyStored() < getPowerRequired()) return false;

        // Хватает ли кислоты в баке.
        if (recipe.getAcid() != null && tank.getFluidAmountMb() < recipe.getAcidAmount()) {
            return false;
        }

        // Поместится ли результат в выходной слот.
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        ItemStack out = recipe.getOutput();
        if (!outSlot.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(outSlot, out)) return false;
            if (outSlot.getCount() + out.getCount() > outSlot.getMaxStackSize()) return false;
        }

        return true;
    }

    /**
     * Завершение крафта: выдать выход, слить кислоту, потратить вход (с учётом productivity).
     */
    private void processItem(CrystallizerRecipe recipe) {
        ItemStack out = recipe.getOutput().copy();
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, out);
        } else {
            outSlot.grow(out.getCount());
        }

        // Слить кислоту, если рецепт её требует.
        if (recipe.getAcid() != null && recipe.getAcidAmount() > 0) {
            tank.drainMb(recipe.getAcidAmount());
        }

        // Productivity: шанс не тратить вход. С апгрейдом EFFECT шанс растёт
        // (пока без апгрейдов — берём базовое значение из рецепта).
        float freeChance = recipe.getProductivity();
        if (freeChance <= 0f || level.random.nextFloat() >= freeChance) {
            inventory.getStackInSlot(SLOT_INPUT).shrink(recipe.getInputCount());
        }

        setChanged();
    }

    /**
     * Применяет жидкостный идентификатор из слота 7 к баку.
     *
     * <p>Если в слоте лежит {@link IItemFluidIdentifier} (или {@link FluidIdentifierItem}),
     * берётся первичный тип жидкости и сравнивается с текущим типом бака. Если они различаются —
     * бак переключается на новый тип, имеющаяся жидкость сливается.</p>
     *
     * <p>Вызывается каждый тик, поэтому достаточно положить идентификатор в слот один раз
     * (даже на 1 тик): машина переключится мгновенно, после чего идентификатор можно забрать
     * без последствий — бак сохраняет свой тип.</p>
     */
    private void applyFluidIdentifier() {
        ItemStack idStack = inventory.getStackInSlot(SLOT_FLUID_ID);
        if (idStack.isEmpty()) return;

        Fluid resolved = resolveIdentifierFluid(idStack);
        if (resolved == null) return;

        Fluid currentType = tank.getTankType();

        // Если тип уже совпадает — ничего не делаем (не трогаем содержимое бака).
        if (VanillaFluidEquivalence.sameSubstance(resolved, currentType)) {
            return;
        }

        // Переключаем тип бака. Если в нём есть «старая» жидкость — она сливается.
        tank.assignTypeAndZeroFluid(resolved);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * Извлекает первичный тип жидкости из идентификатора. Возвращает {@code null}, если стэк
     * не идентификатор или его тип нельзя превратить в реальный {@link Fluid}.
     */
    @Nullable
    private Fluid resolveIdentifierFluid(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            // У FluidIdentifierItem уже есть удобный resolver: возвращает {@code ModFluids.NONE} вместо EMPTY.
            return FluidIdentifierItem.resolvePrimaryForTank(stack);
        }
        if (stack.getItem() instanceof IItemFluidIdentifier idItem) {
            Fluid f = idItem.getType(level, worldPosition, stack);
            if (f == null || f == Fluids.EMPTY) return null;
            return f;
        }
        return null;
    }

    /**
     * Постепенная перекачка жидкости из контейнера в слоте 3 в внутренний бак.
     *
     * <p>Логика: каждый тик пытаемся высосать максимум из контейнера в бак (но не более
     * того, что влезает). Если бак не принимает (тип жидкости не совпадает или нет места) —
     * ничего не делаем. Когда контейнер становится пуст — перемещаем его в слот 4 (выход).</p>
     *
     * <p>Это решает проблему с большими бочками (16 000 mB), которые не помещаются в бак
     * за один присест: бочка остаётся в верхнем слоте и продолжает доливать по мере того,
     * как машина расходует кислоту.</p>
     */
    private void transferFluidsFromItems() {
        ItemStack fillStack = inventory.getStackInSlot(SLOT_FLUID_INPUT);
        if (fillStack.isEmpty()) return;

        //? if forge {
        /*IFluidHandler tankH = tankHandler.orElse(null);
        if (tankH == null) return;

        // Без жидкостного идентификатора бак не принимает ничего — как в оригинале 1.7.10.
        // Если тип бака не задан (Fluids.EMPTY или ModFluids.NONE) — выходим, пусть игрок
        // сначала поставит идентификатор в слот 7.
        Fluid currentType = tank.getTankType();
        if (currentType == Fluids.EMPTY || currentType == ModFluids.NONE.getSource()) {
            return;
        }

        // Берём отдельный стак на 1 предмет — чтобы не модифицировать целый стак сразу.
        // (Хотя в слот мы и так пускаем максимум 1, на всякий случай страхуемся.)
        ItemStack singleItem = fillStack.copy();
        singleItem.setCount(1);

        var itemCapOpt = singleItem.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM);
        var itemHandler = itemCapOpt.orElse(null);
        if (itemHandler == null) return;

        // Сосём из контейнера столько, сколько влезает в бак.
        net.minecraftforge.fluids.FluidStack drained = itemHandler.drain(
                tankH.getTankCapacity(0), IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            // Контейнер уже пуст — пробуем переместить его в выходной слот (со стэкованием).
            tryMoveContainerToOutput(itemHandler.getContainer(), fillStack);
            return;
        }

        int filled = tankH.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return; // бак не принимает (другая жидкость / нет места).

        // Реально переливаем filled mB.
        net.minecraftforge.fluids.FluidStack actuallyDrained = itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        tankH.fill(actuallyDrained, IFluidHandler.FluidAction.EXECUTE);

        // Контейнер мог обновиться (новый NBT, например).
        ItemStack updatedContainer = itemHandler.getContainer();

        // Если после слива контейнер пуст — выгоняем его в выходной слот.
        var afterCapOpt = updatedContainer.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM);
        var afterHandler = afterCapOpt.orElse(null);
        boolean nowEmpty = afterHandler == null
                || afterHandler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty();

        if (nowEmpty) {
            tryMoveContainerToOutput(updatedContainer, fillStack);
        } else {
            // Контейнер ещё не пустой — оставляем во входном слоте.
            inventory.setStackInSlot(SLOT_FLUID_INPUT, updatedContainer);
        }
        setChanged();
        *///?}

        //? if fabric {
        ItemStack one = fillStack.copy();
        one.setCount(1);

        Storage<FluidVariant> itemStorage = FluidStorage.ITEM.find(one, null);
        if (itemStorage == null) return;

        try (Transaction tx = Transaction.openOuter()) {
            long moved = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(
                    itemStorage,
                    tank.getStorage(),
                    v -> true,
                    Long.MAX_VALUE,
                    tx
            );
            if (moved > 0) {
                tx.commit();
                inventory.setStackInSlot(SLOT_FLUID_INPUT, ItemStack.EMPTY);
                inventory.setStackInSlot(SLOT_FLUID_OUTPUT, one);
                setChanged();
            }
        }
        //?}
    }

    //? if forge {
    /*/^*
     * Пытается положить опустошённый контейнер в выходной слот.
     *
     * <p>Сценарии:</p>
     * <ul>
     *   <li>Контейнер исчез (например, ведро в Forge — drain уничтожает ведро) → просто
     *       убираем 1 предмет из входного слота, в выход ничего не кладём.</li>
     *   <li>Выходной слот пуст → кладём контейнер туда.</li>
     *   <li>В выходе уже лежит идентичный пустой контейнер с местом до maxStackSize →
     *       стэкуем (увеличиваем count).</li>
     *   <li>В выходе лежит другой предмет или стэк уже полон → не делаем ничего,
     *       контейнер остаётся во входном слоте до тех пор пока выход не освободится.</li>
     * </ul>
     ^/
    private void tryMoveContainerToOutput(ItemStack emptyContainer, ItemStack originalFillStack) {
        // Случай 0 — контейнер исчез.
        if (emptyContainer.isEmpty()) {
            ItemStack remaining = originalFillStack.copy();
            remaining.shrink(1);
            inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            return;
        }

        ItemStack outSlot = inventory.getStackInSlot(SLOT_FLUID_OUTPUT);

        // Случай 1 — выход пустой.
        if (outSlot.isEmpty()) {
            ItemStack remaining = originalFillStack.copy();
            remaining.shrink(1);
            inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            inventory.setStackInSlot(SLOT_FLUID_OUTPUT, emptyContainer);
            return;
        }

        // Случай 2 — выход содержит такой же предмет с местом → стэкуем.
        if (ItemStack.isSameItemSameTags(outSlot, emptyContainer)) {
            int max = outSlot.getMaxStackSize();
            int totalAfter = outSlot.getCount() + emptyContainer.getCount();
            if (totalAfter <= max) {
                ItemStack newOut = outSlot.copy();
                newOut.setCount(totalAfter);
                inventory.setStackInSlot(SLOT_FLUID_OUTPUT, newOut);

                ItemStack remaining = originalFillStack.copy();
                remaining.shrink(1);
                inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            }
            // Если totalAfter > max — выход полон, ждём, контейнер остаётся во входе.
            return;
        }

        // Случай 3 — выход занят другим предметом → ждём, контейнер остаётся во входе.
    }
    *///?}

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    private int calcDuration(CrystallizerRecipe recipe) {
        return recipe.getDuration();
    }

    //? if forge {
    /*private FluidStack getTankFluidStack() {
        var fluid = tank.getStoredFluid();
        int amount = tank.getFluidAmountMb();
        return fluid == null || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }
    *///?}

    private boolean canProcess() {
        if (inventory.getStackInSlot(SLOT_INPUT).isEmpty()) return false;
        if (getEnergyStored() < getPowerRequired()) return false;
        // Заглушка: CrystallizerRecipes.getOutput - всегда null
        return false;
    }

    private void processItem() {
        // Заглушка: логика крафтов
    }

    public int getPowerRequired() {
        return BASE_POWER_PER_TICK;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isOn() {
        return isOn;
    }

    public long getPowerScaled(int scale) {
        long max = getMaxEnergyStored();
        return max <= 0 ? 0 : (getEnergyStored() * scale) / max;
    }

    public int getProgressScaled(int scale) {
        int dur = getDuration();
        return dur <= 0 ? 0 : (progress * scale) / dur;
    }

    public FluidTank getTank() {
        return tank;
    }

    public ContainerData getContainerData() {
        return data;
    }

    // ───────────────────────────── IFluidStandardReceiverMK2 ─────────────────────────────
    // Регистрирует окислитель как приёмник в жидкостной сети MK2 (используется
    // UniversalMachinePartBlockEntity в углах нижнего слоя). Без этого интерфейса сеть не
    // знает что наш бак готов принять жидкость, пока в баке пусто — collectControllerFluidTypes
    // в part-BE возвращал бы пустое множество (он смотрит только на залитую жидкость через
    // getFluidInTank). С интерфейсом same путь идёт через mk2.getAllTanks() → tank.getTankType(),
    // которое корректно выдаёт настроенный тип даже при пустом баке.

    private final FluidTank[] receivingTanksArr = new FluidTank[] { tank };

    @Override
    public FluidTank[] getReceivingTanks() {
        return receivingTanksArr;
    }

    @Override
    public FluidTank[] getAllTanks() {
        return receivingTanksArr;
    }

    @Override
    public boolean isLoaded() {
        // Точная проверка — позиция действительно в загруженном чанке.
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
    // ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * The controller BlockEntity is only one block, but the animated spinner/fluid BER is
     * rendered across the whole 3x3x6 multiblock. Without the expanded render bounds,
     * Minecraft frustum-culls the BER when the controller block itself leaves the camera
     * frustum, which makes the spinner and fluid disappear at steep viewing angles.
     */
    //? if forge {
    /*@Override
    *///?}
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof MachineCrystallizerBlock block
                && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return block.getStructureHelper().getRenderBoundingBox(
                    worldPosition,
                    state.getValue(HorizontalDirectionalBlock.FACING),
                    1.25D
            );
        }
        return super.getRenderBoundingBox().inflate(3.0D, 6.0D, 3.0D);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crystallizer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            // Принимаем только то, что подходит хотя бы под один рецепт с текущей жидкостью.
            return CrystallizerRecipes.findRecipe(stack, getTankFluidStack()) != null;
        }
        if (slot == SLOT_BATTERY) {
            if (stack.getItem() instanceof ItemCreativeBattery) return true;
            return isEnergyProviderItem(stack);
        }
        if (slot == SLOT_OUTPUT || slot == SLOT_FLUID_OUTPUT) {
            return false;
        }
        if (slot == SLOT_FLUID_INPUT) {
            //? if forge {
            /*return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            *///?}
            //? if fabric {
            return FluidStorage.ITEM.find(stack, null) != null;
            //?}
        }
        if (slot == SLOT_UPGRADE_1 || slot == SLOT_UPGRADE_2) {
            // TODO: проверка ItemMachineUpgrade когда будет реализован
            return true;
        }
        if (slot == SLOT_FLUID_ID) {
            // Принимаем только мульти-жидкостный идентификатор.
            return stack.getItem() instanceof IItemFluidIdentifier;
        }
        return true;
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCrystallizerMenu(containerId, playerInventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("tank", tank.writeNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("tank")) {
            tank.readNBT(tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.updateSound(this, false, null);
        }
    }

    //? if forge {
    /*@Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return tankHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankHandler.invalidate();
    }
    *///?}
}
