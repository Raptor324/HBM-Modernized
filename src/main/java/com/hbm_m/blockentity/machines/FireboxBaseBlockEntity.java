package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.module.ModuleBurnTime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Порт {@code TileEntityFireboxBase} (1.7.10): твёрдотопливная печь — источник
 * тепла ({@link IHeatSource}) с двумя слотами топлива, трубой дыма и анимированной
 * дверцей (открывается, когда открыт GUI — {@code playersUsing}).
 *
 * <p>Потомки задают базовое тепловыделение ({@code getBaseHeat()}), множитель
 * длительности ({@code getTimeMult()}), теплоёмкость ({@code getMaxHeat()}) и
 * таблицу модификаторов топлива ({@code getModule()}). Порт {@code TileEntityHeaterFirebox}
 * — {@link MachineFireboxBlockEntity}, порт {@code TileEntityHeaterOven} —
 * {@link HeatingOvenBlockEntity}.
 */
public abstract class FireboxBaseBlockEntity extends BaseMachineBlockEntity implements IHeatSource, IFluidStandardSenderMK2 {

    public static final int FUEL_SLOTS = 2;

    /** Буфер дымовых баков — как в оригинале TileEntityMachinePolluting(2, 50). */
    public static final int SMOKE_BUFFER = 50;

    protected final FluidTank smoke = new FluidTank(ModFluids.SMOKE.getSource(), SMOKE_BUFFER);
    protected final FluidTank smokeLeaded = new FluidTank(ModFluids.SMOKE_LEADED.getSource(), SMOKE_BUFFER);
    protected final FluidTank smokePoison = new FluidTank(ModFluids.SMOKE_POISON.getSource(), SMOKE_BUFFER);

    public int maxBurnTime;
    public int burnTime;
    public int burnHeat;
    public boolean wasOn = false;

    private int playersUsing = 0;

    public int heatEnergy;

    // Клиентская анимация дверцы (0..135°), как в оригинале
    public float doorAngle = 0;
    public float prevDoorAngle = 0;

    protected FireboxBaseBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                     BlockPos pos, BlockState state) {
        super(type, pos, state, FUEL_SLOTS, 0L, 0L, 0L);
    }

    public abstract ModuleBurnTime getModule();
    public abstract int getBaseHeat();
    public abstract double getTimeMult();
    public abstract int getMaxHeat();

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.firebox");
    }

    // ─────────────────────────── Тик сервера ───────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, FireboxBaseBlockEntity be) {
        // Дым: 3 позиции по каждой из 4 горизонтальных сторон, как в оригинале
        for (int i = 0; i < 4; i++) {
            Direction dir = Direction.from2DDataValue(i);
            Direction rot = dir.getCounterClockWise();
            for (int j = -1; j <= 1; j++) {
                be.sendSmokeAt(level, pos.offset(dir.getNormal().getX() * 2 + rot.getNormal().getX() * j, 0,
                        dir.getNormal().getZ() * 2 + rot.getNormal().getZ() * j), dir);
            }
        }

        be.wasOn = false;

        if (be.burnTime <= 0) {
            be.tryConsumeFuel(level, pos);
        } else {
            if (be.heatEnergy < be.getMaxHeat()) {
                be.burnTime--;
                // Оригинал: раз в секунду pollute(SOOT, SOOT_PER_SECOND * 3);
                // сетки загрязнения в порте нет — накопительный дым уходит в трубу.
                if (level.getGameTime() % 20 == 0) {
                    be.polluteSoot(3.0F / 25.0F);
                }
            }
            be.wasOn = true;

            // Треск огня (1/15 тиков), как в оригинале (VANILLA_FIRE)
            if (level.random.nextInt(15) == 0) {
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F,
                        0.5F + level.random.nextFloat() * 0.5F);
            }
        }

        if (be.wasOn) {
            be.heatEnergy = Math.min(be.heatEnergy + be.burnHeat, be.getMaxHeat());
        } else {
            be.heatEnergy = Math.max(be.heatEnergy - Math.max(be.heatEnergy / 1000, 1), 0);
            be.burnHeat = 0;
        }

        if (be.dataSyncDirty) {
            be.dataSyncDirty = false;
            be.sendUpdateToClient();
        }
    }

    /** Клиентский тик: анимация дверцы + частицы пламени (как в updateEntity remote-ветке). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, FireboxBaseBlockEntity be) {
        be.prevDoorAngle = be.doorAngle;
        float swingSpeed = (be.doorAngle / 10F) + 3;

        if (be.playersUsing > 0) {
            be.doorAngle += swingSpeed;
        } else {
            be.doorAngle -= swingSpeed;
        }
        be.doorAngle = net.minecraft.util.Mth.clamp(be.doorAngle, 0F, 135F);

        if (be.wasOn && level.getGameTime() % 5 == 0) {
            Direction dir = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    : Direction.NORTH;
            double x = pos.getX() + 0.5 + dir.getStepX();
            double y = pos.getY() + 0.25;
            double z = pos.getZ() + 0.5 + dir.getStepZ();
            level.addParticle(ParticleTypes.FLAME,
                    x + level.random.nextDouble() * 0.5 - 0.25,
                    y + level.random.nextDouble() * 0.25,
                    z + level.random.nextDouble() * 0.5 - 0.25, 0, 0, 0);
        }
    }

    /** Потребление топлива из слотов (оригинальная ветка burnTime <= 0). */
    private void tryConsumeFuel(Level level, BlockPos pos) {
        for (int i = 0; i < FUEL_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int baseTime = getModule().getBurnTime(stack);
            if (baseTime <= 0) continue;

            int fuel = (int) (baseTime * getTimeMult());

            // Зола в зольник под печью (как в оригинале — raw baseTime до множителей)
            BlockEntity below = level.getBlockEntity(pos.below());
            if (below instanceof MachineAshpitBlockEntity ashpit) {
                ashpit.addAsh(getAshFromFuel(stack), baseTime);
            }

            this.maxBurnTime = this.burnTime = fuel;
            this.burnHeat = getModule().getBurnHeat(getBaseHeat(), stack);

            if (stack.getCount() == 1) {
                net.minecraft.world.item.Item containerItem = stack.getItem().getCraftingRemainingItem();
                inventory.setStackInSlot(i, containerItem == null ? ItemStack.EMPTY : new ItemStack(containerItem));
            } else {
                stack.shrink(1);
            }

            this.wasOn = true;
            this.dataSyncDirty = true;
            break;
        }
    }

    // ─────────────────────── Дым (порт TileEntityMachinePolluting) ───────────────────────

    private void sendSmokeAt(Level level, BlockPos pipePos, Direction dir) {
        if (smoke.getFill() > 0) tryProvide(smoke, level, pipePos, dir);
        if (smokeLeaded.getFill() > 0) tryProvide(smokeLeaded, level, pipePos, dir);
        if (smokePoison.getFill() > 0) tryProvide(smokePoison, level, pipePos, dir);
    }

    /** SOOT → бак обычного дыма: ceil(amount * 100) mB, клампится при переполнении. */
    public void polluteSoot(float amount) {
        int mB = (int) Math.ceil(amount * 100);
        int fill = smoke.getFill();
        int newFill = Math.min(smoke.getMaxFill(), fill + mB);
        smoke.setFill(newFill);
    }

    // ─────────────────────────── Дверца / GUI ───────────────────────────

    /** Вызывается из конструктора меню (сервер) — как ContainerFirebox → openInventory. */
    public void playerOpened() {
        if (level != null && !level.isClientSide()) {
            playersUsing++;
            dataSyncDirty = true;
        }
    }

    /** Вызывается из {@code menu.removed} (сервер) — как closeInventory. */
    public void playerClosed() {
        if (level != null && !level.isClientSide()) {
            playersUsing = Math.max(0, playersUsing - 1);
            dataSyncDirty = true;
        }
    }

    public float getInterpolatedDoorAngle(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, prevDoorAngle, doorAngle);
    }

    public boolean isBurning() {
        return wasOn;
    }

    // ─────────────────────────── Тепло (IHeatSource) ───────────────────────────

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public int getMaxHeatStored() {
        return getMaxHeat();
    }

    @Override
    public void useUpHeat(int amount) {
        heatEnergy = Math.max(0, heatEnergy - amount);
        setChanged();
    }

    // ─────────────────────────── Инвентарь ───────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return ModuleBurnTime.getBaseBurnTime(stack) > 0;
    }

    /** Порт {@code TileEntityFireboxBase.getAshFromFuel} (ore-dict → категории ModuleBurnTime). */
    public static MachineAshpitBlockEntity.AshType getAshFromFuel(ItemStack stack) {
        return switch (ModuleBurnTime.getCategory(stack)) {
            case COKE, COAL, LIGNITE -> MachineAshpitBlockEntity.AshType.COAL;
            case LOG, WOOD -> MachineAshpitBlockEntity.AshType.WOOD;
            default -> MachineAshpitBlockEntity.AshType.MISC;
        };
    }

    // ─────────────────────────── Жидкости ───────────────────────────

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { smoke, smokeLeaded, smokePoison };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { smoke, smokeLeaded, smokePoison };
    }

    /** Оригинал canConnect: любая сторона кроме низа (и не UNKNOWN). */
    @Override
    public boolean canConnect(net.minecraft.world.level.material.Fluid fluid, Direction dir) {
        return dir != Direction.DOWN;
    }

    // ─────────────────────────── Синхронизация ───────────────────────────

    /** Поле-флаг вместо удержания старых значений: выставляется при изменениях, сбрасывается после отправки. */
    protected boolean dataSyncDirty = false;

    /** Данные для GUI (ContainerData меню): burnTime/maxBurnTime/burnHeat/heatEnergy/maxHeat. */
    public ContainerData getData() {
        return data;
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> maxBurnTime;
                case 2 -> burnHeat;
                case 3 -> heatEnergy;
                case 4 -> getMaxHeat();
                default -> 0;
            };
        }

        // Клиентская запись значений, пришедших синком меню (ванильный паттерн furnace).
        // Пустой set() ломал авто-обновление полосок GUI — значения отбрасывались.
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnTime = value;
                case 1 -> maxBurnTime = value;
                case 2 -> burnHeat = value;
                case 3 -> heatEnergy = value;
                // case 4 (maxHeat) — только чтение: вычисляется на сервере.
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("burnTime", burnTime);
        tag.putInt("burnHeat", burnHeat);
        tag.putInt("heatEnergy", heatEnergy);
        tag.putInt("playersUsing", playersUsing);
        tag.putBoolean("wasOn", wasOn);
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        maxBurnTime = tag.getInt("maxBurnTime");
        burnTime = tag.getInt("burnTime");
        burnHeat = tag.getInt("burnHeat");
        heatEnergy = tag.getInt("heatEnergy");
        playersUsing = tag.getInt("playersUsing");
        wasOn = tag.getBoolean("wasOn");
        if (tag.contains("smoke0")) smoke.readFromNBT(tag, "smoke0");
        if (tag.contains("smoke1")) smokeLeaded.readFromNBT(tag, "smoke1");
        if (tag.contains("smoke2")) smokePoison.readFromNBT(tag, "smoke2");
        prevDoorAngle = doorAngle;
    }

    /** Границы рендера 3×3 — модель мультиблока рисуется с контроллера. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
                worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }
}
