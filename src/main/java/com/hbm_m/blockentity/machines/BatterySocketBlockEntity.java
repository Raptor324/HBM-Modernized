package com.hbm_m.blockentity.machines;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.multiblock.PartRole;
import com.hbm_m.block.machines.MachineBatterySocketBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.interfaces.IEnergyProvider;
import com.hbm_m.interfaces.IEnergyReceiver;
import com.hbm_m.inventory.menu.BatterySocketMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

//? if forge {
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import com.hbm_m.capability.ModCapabilities;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}

/**
 * Battery socket: one portable battery slot, modes like machine battery, energy from item capabilities.
 */
@SuppressWarnings("UnstableApiUsage")
public class BatterySocketBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder, com.hbm_m.api.energy.PowerBuffer,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider, com.hbm_m.api.redstoneoverradio.IRORInteractive {

    //? if forge {
    public static final ModelProperty<Boolean> HAS_INSERT = new ModelProperty<>();
    /** Original RenderBatterySocket: ein battery_pack zeigt je nach Art den Teil "Battery" oder "Capacitor" mit eigener Textur. */
    public static final ModelProperty<com.hbm_m.item.fekal_electric.ItemBatteryPack.EnumBatteryPack> PACK = new ModelProperty<>();
    /** 1:1 RenderBatterySocket: {@code battery_sc} zeigt das Batterieteil mit {@code battery_sc_tex}. */
    public static final ModelProperty<Boolean> SC = new ModelProperty<>();
    //?}

    private static final int SLOT_BATTERY = 0;

    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    private IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.NORMAL;

    public long energyDelta = 0;
    private long lastEnergySample = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> modeOnNoSignal;
                case 1 -> modeOnSignal;
                case 2 -> priority.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> modeOnNoSignal = value;
                case 1 -> modeOnSignal = value;
                case 2 -> priority = IEnergyReceiver.Priority.values()[Math.max(0, Math.min(value, IEnergyReceiver.Priority.values().length - 1))];
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public BatterySocketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_SOCKET_BE.get(), pos, state, 1, 0L, 0L, 0L);
    }

    public static boolean isAllowedPortableEnergyStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemCreativeBattery) return true;
        return isEnergyProviderItem(stack) || isEnergyReceiverItem(stack);
    }

    @Override
    protected Component getDefaultName() { return Component.translatable("container.hbm_m.battery_socket"); }

    // Энергопорты мультиблока: сама батарея (ядро, подписывается базовым классом)
    // плюс все ENERGY_CONNECTOR-фантомы структуры.
    @Override
    protected net.minecraft.core.BlockPos[] getExtraEnergyPorts() {
        if (level == null || level.isClientSide) return new BlockPos[0];
        if (!(getBlockState().getBlock() instanceof MachineBatterySocketBlock controller)) {
            return new BlockPos[0];
        }
        Direction facing = getBlockState().getValue(MachineBatterySocketBlock.FACING);
        var helper = controller.getStructureHelper();
        java.util.List<BlockPos> ports = new java.util.ArrayList<>();
        for (BlockPos local : helper.getStructureMap().keySet()) {
            if (helper.resolvePartRole(local, controller) == PartRole.ENERGY_CONNECTOR) {
                ports.add(helper.getRotatedPos(worldPosition, local, facing));
            }
        }
        return ports.toArray(new BlockPos[0]);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatterySocketBlockEntity be) {
        if (level.isClientSide) return;

        be.ensureNetworkInitialized();

        // 1:1 TileEntityBatterySocket: eingelegte Radionuklidbatterie schwankt in der Leistung und entlaedt sich alle 1-3 Minuten
        if (be.hasSCLoaded()) {
            if (be.damageTarget == 0) be.pickNewSCTarget();
            be.damageTimer++;
            if (be.damageTimer >= be.damageTarget) be.discharge();
            be.fluctuate();
        }

        long gameTime = level.getGameTime();
        if (gameTime % 10 == 0) {
            long cur = be.getEnergyStoredFromStack();
            be.energyDelta = (cur - be.lastEnergySample) / 10;
            be.lastEnergySample = cur;
        }
    }

    public int damageTimer;
    public int damageTarget;
    public double scPowerMult = 1D;

    protected boolean hasSCLoaded() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        return stack.getItem() instanceof com.hbm_m.item.fekal_electric.ItemBatterySC sc
                && sc.type != com.hbm_m.item.fekal_electric.ItemBatterySC.EnumBatterySC.EMPTY;
    }

    protected void pickNewSCTarget() {
        this.damageTimer = 0;
        this.damageTarget = 1200 + level.random.nextInt(2400); // 1-3 minutes;
        this.setChanged();
    }

    protected void fluctuate() {
        double steppy = 1D / 100D;
        this.scPowerMult += (steppy * (level.random.nextDouble() * 2 - 1));
        this.scPowerMult = net.minecraft.util.Mth.clamp(scPowerMult, 0.1D, 1D);
    }

    /** 1:1 {@code discharge}: SEDNA-Strahl (EntityBulletBeamBase), Renderer RENDER_LIGHTNING_SUB in GunFactoryClient. */
    public static com.hbm_m.item.weapon.sedna.BulletConfig discharge;
    public static java.util.function.BiConsumer<com.hbm_m.entity.projectile.EntityBulletBeamBase, com.hbm_m.util.MovingObjectPosition> BEAM_DISCHARGE_HIT = (beam, mop) -> {

        if (mop.typeOfHit == com.hbm_m.util.MovingObjectPosition.MovingObjectType.BLOCK) {
            beam.level().destroyBlock(mop.getBlockPos(), false); // func_147480_a(x, y, z, false)
            explodeDischarge(beam.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
        }

        if (mop.typeOfHit == com.hbm_m.util.MovingObjectPosition.MovingObjectType.ENTITY) {
            explodeDischarge(beam.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
        }
    };

    private static boolean configsDone = false;

    /** Original: static-Block; im Port aus GunFactory.init, damit die Config-ID auf beiden Seiten gleich ist. */
    public static void initConfigs() {
        if (configsDone) return;
        configsDone = true;
        discharge = new com.hbm_m.item.weapon.sedna.BulletConfig().setupDamageClass(com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass.ELECTRIC).setBeam().setSpread(0.0F).setLife(3).setThresholdNegation(20F).setArmorPiercing(0.5F).setRenderRotations(false).setDoesPenetrate(true)
                .setOnBeamImpact(BEAM_DISCHARGE_HIT);
    }

    /** 1:1 {@code discharge()}: auf jedes Lebewesen im Umkreis von 15 Bloecken geht ein Strahl {@link #discharge} mit 50 Schaden. */
    protected void discharge() {
        pickNewSCTarget();

        Direction dir = getBlockState().getValue(MachineBatterySocketBlock.FACING);
        Direction rot = dir.getClockWise();

        double x = worldPosition.getX() + 0.5 - dir.getStepX() * 0.5 + rot.getStepX() * 0.5;
        double y = worldPosition.getY() + 1;
        double z = worldPosition.getZ() + 0.5 - dir.getStepZ() * 0.5 + rot.getStepX() * 0.5;

        double range = 15;
        java.util.List<net.minecraft.world.entity.LivingEntity> potentialTargets = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new AABB(x, y, z, x, y, z).inflate(range, range, range));
        java.util.Collections.shuffle(potentialTargets);

        for (net.minecraft.world.entity.LivingEntity target : potentialTargets) {

            com.hbm_m.util.Vec3NT initialDelta = new com.hbm_m.util.Vec3NT(target.getX() - x, target.getY() + target.getBbHeight() / 2 - y, target.getZ() - z);
            if (initialDelta.lengthVector() > range) continue;
            com.hbm_m.entity.projectile.EntityBulletBeamBase sub = new com.hbm_m.entity.projectile.EntityBulletBeamBase(level, discharge, 50F);
            initialDelta.normalizeSelf();
            double dominantAxis = com.hbm_m.util.BobMathUtil.max(Math.abs(initialDelta.xCoord), Math.abs(initialDelta.yCoord), Math.abs(initialDelta.zCoord));
            initialDelta.multiply(1.125D / dominantAxis); // move 1.125 blocks outwards
            sub.setPos(worldPosition.getX() + initialDelta.xCoord, worldPosition.getY() + initialDelta.yCoord, worldPosition.getZ() + initialDelta.zCoord);
            com.hbm_m.util.Vec3NT actualDelta = new com.hbm_m.util.Vec3NT(target.getX() - sub.getX(), target.getY() + target.getBbHeight() / 2 - sub.getY(), target.getZ() - sub.getZ());

            sub.setRotationsFromVector(actualDelta);
            sub.performHitscanExternal(actualDelta.lengthVector());
            level.addFreshEntity(sub);
        }

        explodeDischarge(level, x + level.random.nextGaussian() * 0.5, y + level.random.nextGaussian() * 0.5, z + level.random.nextGaussian() * 0.5);
    }

    public static void explodeDischarge(Level world, double x, double y, double z) {
        com.hbm_m.explosion.vanillant.ExplosionVNT vnt = new com.hbm_m.explosion.vanillant.ExplosionVNT(world, x, y, z, 5F);
        vnt.setEntityProcessor(new com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth(1, 20).setDamageClass(com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass.ELECTRIC));
        vnt.setPlayerProcessor(new com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard());
        vnt.setSFX(new com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard());
        vnt.explode();
        world.playSound(null, x, y, z, com.hbm_m.sound.HbmSoundsNT.get("hbm:entity.ufoBlast"), net.minecraft.sounds.SoundSource.BLOCKS, 5.0F, 0.9F + world.random.nextFloat() * 0.2F);
    }

    /** Backwards-compatible accessor for renderers/menus. */
    public com.hbm_m.platform.ModItemStackHandler getItemHandler() {
        return this.inventory;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && isAllowedPortableEnergyStack(stack);
    }

    @Override
    protected boolean isCriticalSlot(int slot) {
        return slot == SLOT_BATTERY;
    }

    //? if forge {
    @Override
    public ModelData getModelData() {
        ItemStack inserted = inventory.getStackInSlot(0);
        return ModelData.builder()
                .with(HAS_INSERT, !inserted.isEmpty())
                .with(PACK, inserted.getItem() instanceof com.hbm_m.item.fekal_electric.ItemBatteryPack bp ? bp.pack : null)
                .with(SC, inserted.getItem() instanceof com.hbm_m.item.fekal_electric.ItemBatterySC)
                .build();
    }
    //?}

    //? if fabric {
    /*@Override
    public @Nullable Object getRenderAttachmentData() {
        return !inventory.getStackInSlot(0).isEmpty();
    }
    *///?}

    public long getEnergyDelta() {
        return energyDelta;
    }

    @Override
    public int getCurrentMode() {
        if (level == null) return modeOnNoSignal;
        return level.hasNeighborSignal(worldPosition) ? modeOnSignal : modeOnNoSignal;
    }

    private int getMode() {
        return getCurrentMode();
    }

    private Optional<IEnergyProvider> stackProvider() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return Optional.empty();
        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).resolve();
        //?}
        //? if fabric {
        /*return Optional.empty();
        *///?}
        //? if neoforge {
        /*// NeoForge: HBM item-capability через ItemEnergyAccess (использует ModCapabilities.HBM_ITEM_ENERGY_PROVIDER).
        return com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack);
        *///?}
    }

    private Optional<IEnergyReceiver> stackReceiver() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return Optional.empty();
        //? if forge {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).resolve();
        //?}
        //? if fabric {
        /*return Optional.empty();
        *///?}
        //? if neoforge {
        /*// NeoForge: HBM item-capability через ItemEnergyAccess (использует ModCapabilities.HBM_ITEM_ENERGY_RECEIVER).
        return com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(stack);
        *///?}
    }

    private long getEnergyStoredFromStack() {
        long result = 0L;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty()) {
            var es = EnergyStorage.ITEM.find(stack, null);
            if (es != null) result = es.getAmount();
        }
        *///?} else {
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) result = r.get().getEnergyStored();
        else result = stackProvider().map(IEnergyProvider::getEnergyStored).orElse(0L);
        //?}
        if (level != null && hasSCLoaded()) result *= this.scPowerMult;
        return result;
    }

    private long getMaxEnergyStoredFromStack() {
        long result = 1L;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty()) {
            var es = EnergyStorage.ITEM.find(stack, null);
            if (es != null) result = Math.max(1L, es.getCapacity());
        }
        *///?} else {
        Optional<IEnergyReceiver> r = stackReceiver();
        if (r.isPresent()) result = Math.max(1L, r.get().getMaxEnergyStored());
        else result = stackProvider().map(p -> Math.max(1L, p.getMaxEnergyStored())).orElse(1L);
        //?}
        return result;
    }

    @Override
    public long getEnergyStored() {
        return getEnergyStoredFromStack();
    }
    @Override
    public long getMaxEnergyStored() {
        return getMaxEnergyStoredFromStack();
    }

    @Override
    public void setEnergyStored(long energy) {
        Optional<IEnergyReceiver> rec = stackReceiver();
        if (rec.isPresent()) {
            rec.get().setEnergyStored(energy);
        } else {
            stackProvider().ifPresent(p -> p.setEnergyStored(energy));
        }
    }

    @Override
    public long getReceiveSpeed() {
        long speed = 0L;
        //? if fabric {
        /*speed = getMaxEnergyStoredFromStack(); // ограничим реально капом предмета
        *///?} else {
        speed = stackReceiver().map(IEnergyReceiver::getReceiveSpeed).orElse(0L);
        //?}
        return speed;
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return priority;
    }

    @Override
    public long receiveEnergy(long maxReceive, boolean simulate) {
        if (!canReceive()) return 0;
        long accepted = 0L;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        if (es != null && es.supportsInsertion()) {
            if (simulate) {
                try (Transaction tx = Transaction.openOuter()) {
                    accepted = es.insert(maxReceive, tx);
                }
            } else {
                try (Transaction tx = Transaction.openOuter()) {
                    accepted = es.insert(maxReceive, tx);
                    if (accepted > 0) tx.commit();
                }
            }
        }
        *///?} else {
        accepted = stackReceiver().map(r -> r.receiveEnergy(maxReceive, simulate)).orElse(0L);
        //?}
        return accepted;
    }

    @Override
    public boolean canReceive() {
        int mode = getMode();
        if (!(mode == 0 || mode == 1)) return false;
        boolean result = false;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        result = es != null && es.supportsInsertion();
        *///?} else {
        result = stackReceiver().map(IEnergyReceiver::canReceive).orElse(false);
        //?}
        return result;
    }

    @Override
    public long getProvideSpeed() {
        long speed = 0L;
        //? if fabric {
        /*speed = getEnergyStoredFromStack();
        *///?} else {
        speed = stackProvider().map(IEnergyProvider::getProvideSpeed).orElse(0L);
        //?}
        return speed;
    }

    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        long extracted = 0L;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        if (es != null && es.supportsExtraction()) {
            if (simulate) {
                try (Transaction tx = Transaction.openOuter()) {
                    extracted = es.extract(maxExtract, tx);
                }
            } else {
                try (Transaction tx = Transaction.openOuter()) {
                    extracted = es.extract(maxExtract, tx);
                    if (extracted > 0) tx.commit();
                }
            }
        }
        *///?} else {
        extracted = stackProvider().map(p -> p.extractEnergy(maxExtract, simulate)).orElse(0L);
        //?}
        return extracted;
    }

    @Override
    public boolean canExtract() {
        int mode = getMode();
        if (!(mode == 0 || mode == 2)) return false;
        boolean result = false;
        //? if fabric {
        /*ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        var es = EnergyStorage.ITEM.find(stack, null);
        result = es != null && es.supportsExtraction();
        *///?} else {
        result = stackProvider().map(IEnergyProvider::canExtract).orElse(false);
        //?}
        return result;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case 0 -> this.data.set(0, (this.modeOnNoSignal + 1) % 4);
            case 1 -> this.data.set(1, (this.modeOnSignal + 1) % 4);
            case 2 -> {
                // Паритет с оригиналом: батареи ограничены приоритетами LOW..HIGH (без LOWEST/HIGHEST)
                IEnergyReceiver.Priority[] priorities = IEnergyReceiver.Priority.values();
                int current = Math.max(1, Math.min(this.priority.ordinal(), priorities.length - 2));
                int next = (current >= priorities.length - 2) ? 1 : current + 1;
                this.data.set(2, next);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public int getComparatorOutput() {
        long max = getMaxEnergyStoredFromStack();
        if (max <= 0) return 0;
        double frac = (double) getEnergyStoredFromStack() / (double) max * 15.0;
        return Math.min(15, Math.max(0, (int) Math.round(frac)));
    }

    
    // (кастомный лёгкий getUpdateTag ниже сохранён как есть)
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priorityV2", priority.ordinal());
        tag.putLong("energyDelta", energyDelta);
        tag.putLong("lastEnergySample", lastEnergySample);
        tag.putInt("damageTimer", damageTimer);
        tag.putInt("damageTarget", damageTarget);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        modeOnNoSignal = tag.getInt("modeOnNoSignal");
        damageTimer = tag.getInt("damageTimer");
        damageTarget = tag.getInt("damageTarget");
        modeOnSignal = tag.getInt("modeOnSignal");
        // priorityV2 — ординал нового 5-уровневого enum'а; старый "priority" (3 уровня) сдвигаем на +1
        if (tag.contains("priorityV2")) {
            int p = tag.getInt("priorityV2");
            IEnergyReceiver.Priority[] vals = IEnergyReceiver.Priority.values();
            priority = vals[Math.max(0, Math.min(p, vals.length - 1))];
        } else if (tag.contains("priority")) {
            int p = tag.getInt("priority") + 1;
            IEnergyReceiver.Priority[] vals = IEnergyReceiver.Priority.values();
            priority = vals[Math.max(0, Math.min(p, vals.length - 1))];
        }
        energyDelta = tag.getLong("energyDelta");
        lastEnergySample = tag.getLong("lastEnergySample");
        //? if fabric {
        /*if (level != null && level.isClientSide) {
            com.hbm_m.client.render.DoorChunkInvalidationHelper.scheduleChunkInvalidation(worldPosition);
        }
        *///?}
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(
            worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
            worldPosition.getX() + 3, worldPosition.getY() + 3, worldPosition.getZ() + 3
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new BatterySocketMenu(containerId, inv, this, data);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slot {0}; Akkus hinein, nur volle heraus (Original vergleicht den Slot mit mode_input = 0). */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.isFullBattery(stack); }
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

    // ── Redstone-over-Radio (1:1 TileEntityBatterySocket) ──

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "fill",
                PREFIX_VALUE + "maxfill",
                PREFIX_VALUE + "fillpercent",
                PREFIX_VALUE + "delta",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name))        return "" + this.getEnergyStored();
        if ((PREFIX_VALUE + "maxfill").equals(name))     return "" + this.getMaxEnergyStored();
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + this.getEnergyStored() * 100 / (Math.max(this.getMaxEnergyStored(), 1));
        if ((PREFIX_VALUE + "delta").equals(name))       return "" + energyDelta;
        return null;
    }

    /**
     * Original-Zaehlung der Modi (0 = Eingang, 1 = Puffer, 2 = Ausgang, 3 = aus) - der Port zaehlt
     * 0 = Puffer, 1 = Eingang; die Funkbefehle bleiben bei der Original-Zaehlung.
     */
    private static int swapModeROR(int mode) {
        return mode == 0 ? 1 : mode == 1 ? 0 : mode;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // redLow = Modus ohne Redstone, redHigh = Modus mit Redstone
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int mode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != swapModeROR(this.modeOnNoSignal)) {
                this.modeOnNoSignal = swapModeROR(mode);
                this.markChangedROR();
                return null;
            } else if (params.length > 1) {
                int altmode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[1], 0, 3);
                this.modeOnNoSignal = swapModeROR(altmode);
                this.markChangedROR();
                return null;
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setredmode").equals(name) && params.length > 0) {
            int mode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != swapModeROR(this.modeOnSignal)) {
                this.modeOnSignal = swapModeROR(mode);
                this.markChangedROR();
                return null;
            } else if (params.length > 1) {
                int altmode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[1], 0, 3);
                this.modeOnSignal = swapModeROR(altmode);
                this.markChangedROR();
                return null;
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            // Original: parseInt(0..2) + 1 -> LOW, NORMAL, HIGH
            int priority = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 2) + 1;
            this.priority = IEnergyReceiver.Priority.values()[priority];
            this.markChangedROR();
            return null;
        }
        return null;
    }

    /** Original {@code markChanged()}. */
    private void markChangedROR() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
}
