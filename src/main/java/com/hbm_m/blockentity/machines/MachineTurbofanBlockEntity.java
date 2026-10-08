package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.menu.MachineTurbofanMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityMachineTurbofan}: Strahltriebwerk fuer AERO-Treibstoff, 1 mB/t (+1 je Nachbrennerstufe, die
 * Flammenpony setzt 100), Leistung {@code burnValue * menge * (1 + min(ab/3, 4))}. Redstone an einem der vier
 * Anschluesse haelt es an. Hinten ein Abgasstrahl (19,5 Bloecke, mit Nachbrenner brennend + Feuerpartikel), vorne der
 * Ansog (8,5 Bloecke) und die Schaufelebene, die alles zerlegt und daraus Blut in einen eigenen Tank macht. Rotor und
 * Laufgeraeusch folgen einem Schwung von 0-100; der lokale Spieler wird clientseitig mitgezogen.
 */
public class MachineTurbofanBlockEntity extends com.hbm_m.blockentity.MachinePollutingBlockEntity
        implements IFluidStandardTransceiverMK2, com.hbm_m.interfaces.IUpgradeInfoProvider {

    /** Original: 0 Kanister rein, 1 Kanister raus, 2 Aufwertung, 3 Batterie, 4 Fluidkennung. */
    public static final int SLOT_FUEL_CONTAINER = 0;
    public static final int SLOT_EMPTY_CONTAINER = 1;
    public static final int SLOT_UPGRADE = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_IDENTIFIER = 4;

    public static final long maxPower = 1_000_000;

    public final FluidTank tank = new FluidTank(ModFluids.KEROSENE.getSource(), 24000);
    public final FluidTank blood = new FluidTank(ModFluids.BLOOD.getSource(), 24000);

    public int afterburner;
    public boolean wasOn;
    public boolean showBlood = false;
    protected int output;
    protected int consumption;

    public float spin;
    public float lastSpin;
    public int momentum = 0;

    public final UpgradeManager upgradeManager = new UpgradeManager();

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.AFTERBURN, 3);
    }

    public MachineTurbofanBlockEntity(BlockPos pos, BlockState state) {
        // Original: super(5, 150) - 150 mB Rauchpuffer je Sorte
        super(ModBlockEntities.TURBOFAN_BE.get(), pos, state, 5, maxPower, 0L, maxPower, 150);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    protected DirPos[] getConPos() {
        // Original: dir = orientation(meta - 10).getRotation(UP), rot = dir.getRotation(DOWN)
        Direction dir = facing().getClockWise();
        Direction rot = dir.getCounterClockWise();
        BlockPos p = worldPosition;

        return new DirPos[] {
                new DirPos(p.relative(rot, 2), rot),
                new DirPos(p.relative(rot, 2).relative(dir, -1), rot),
                new DirPos(p.relative(rot, -2), rot.getOpposite()),
                new DirPos(p.relative(rot, -2).relative(dir, -1), rot.getOpposite())
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbofanBlockEntity be) {
        if (level instanceof ServerLevel server) be.serverTick(server, pos);
        else be.clientTick(level, pos);
    }

    private ItemStack[] slotArray() {
        ItemStack[] arr = new ItemStack[5];
        for (int i = 0; i < 5; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotArray(ItemStack[] arr) {
        for (int i = 0; i < 5; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        this.output = 0;
        this.consumption = 0;

        ItemStack[] slots = slotArray();
        boolean changed = tank.setType(SLOT_FLUID_IDENTIFIER, slots);
        changed |= tank.loadTank(SLOT_FUEL_CONTAINER, SLOT_EMPTY_CONTAINER, slots);
        if (changed) applySlotArray(slots);
        blood.setTankType(ModFluids.BLOOD.getSource());

        this.wasOn = false;

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE, SLOT_UPGRADE, VALID_UPGRADES);
        this.afterburner = upgradeManager.getLevel(UpgradeType.AFTERBURN);

        if (inventory.getStackInSlot(SLOT_UPGRADE).is(ModItems.FLAME_PONY.get()))
            this.afterburner = 100;

        long burnValue = 0;
        int amount = 1 + this.afterburner;
        int amountToBurn = Math.min(amount, this.tank.getFill());

        boolean redstone = false;

        for (DirPos con : getConPos()) {
            if (world.hasNeighborSignal(con.pos)) {
                redstone = true;
                break;
            }
        }

        if (!redstone) {

            FT_Combustible combustible = FluidType.getTrait(tank.getTankType(), FT_Combustible.class);
            if (combustible != null && combustible.getGrade() == FuelGrade.AERO) {
                burnValue = combustible.getCombustionEnergy() / 1_000;
            }

            if (amountToBurn > 0) {
                this.wasOn = true;
                this.tank.setFill(this.tank.getFill() - amountToBurn);
                this.output = (int) (burnValue * amountToBurn * (1 + Math.min(this.afterburner / 3D, 4)));
                setEnergyStored(getEnergyStored() + this.output);
                this.consumption = amountToBurn;

                if (world.getGameTime() % 20 == 0) super.pollute(tank.getTankType(), FluidReleaseType.BURN, amountToBurn * 5);
            }
        }

        chargeItemInSlot(SLOT_BATTERY);

        for (DirPos con : getConPos()) {
            this.tryProvide(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            this.trySubscribe(tank.getTankType(), world, con.pos, con.dir);
            if (this.blood.getFill() > 0) this.tryProvide(blood, world, con.pos, con.dir);
            this.sendSmoke(world, con.pos, con.dir);
        }

        if (burnValue > 0 && amountToBurn > 0) {

            Direction dir = facing().getClockWise();
            Direction rot = dir.getClockWise();

            if (this.afterburner > 0) {

                for (int i = 0; i < 2; i++) {
                    double speed = 2 + world.random.nextDouble() * 3;
                    double deviation = world.random.nextGaussian() * 0.2;
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "gasfire");
                    data.putDouble("mX", -dir.getStepX() * speed + deviation);
                    data.putDouble("mZ", -dir.getStepZ() * speed + deviation);
                    data.putFloat("scale", 8F);
                    com.hbm_m.particle.helper.IParticleCreator.sendPacket(world, pos.getX() + 0.5F - dir.getStepX() * (3 - i), pos.getY() + 1.5F, pos.getZ() + 0.5F - dir.getStepZ() * (3 - i), 150, data);
                }

                if (this.afterburner > 90 && world.random.nextInt(30) == 0) {
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, com.hbm_m.sound.HbmSoundsNT.get("hbm:block.damage"), SoundSource.BLOCKS, 3.0F, 0.95F + world.random.nextFloat() * 0.2F);
                }

                if (this.afterburner > 90) {
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "gasfire");
                    data.putDouble("mY", 0.1 * world.random.nextDouble());
                    data.putFloat("scale", 4F);
                    com.hbm_m.particle.helper.IParticleCreator.sendPacket(world,
                            pos.getX() + 0.5F + dir.getStepX() * (world.random.nextDouble() * 4 - 2) + rot.getStepX() * (world.random.nextDouble() * 2 - 1),
                            pos.getY() + 1F + world.random.nextDouble() * 2,
                            pos.getZ() + 0.5F - dir.getStepZ() * (world.random.nextDouble() * 4 - 2) + rot.getStepZ() * (world.random.nextDouble() * 2 - 1),
                            150, data);
                }
            }

            for (Entity e : world.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, -3.5, -19.5))) {
                if (this.afterburner > 0) {
                    e.setSecondsOnFire(5);
                    e.hurt(world.damageSources().onFire(), 5F);
                }
                push(e, dir);
            }

            for (Entity e : world.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, 3.5, 8.5))) {
                push(e, dir);
            }

            for (Entity e : world.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, 3.5, 3.75))) {
                e.hurt(ModDamageSources.blender(world), 1000);
                e.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.25D, 0.05D, 0.25D));

                if (!e.isAlive() && e instanceof LivingEntity) {
                    CompoundTag vdat = new CompoundTag();
                    vdat.putString("type", "giblets");
                    vdat.putInt("ent", e.getId());
                    vdat.putInt("cDiv", 5);
                    com.hbm_m.particle.helper.IParticleCreator.sendPacket(world, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 150, vdat);

                    world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F, 0.95F + world.random.nextFloat() * 0.2F);

                    blood.setFill(blood.getFill() + 50);
                    if (blood.getFill() > blood.getMaxFill()) {
                        blood.setFill(blood.getMaxFill());
                    }
                    this.showBlood = true;
                }
            }
        }

        if (getEnergyStored() > maxPower) {
            setEnergyStored(maxPower);
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level world, BlockPos pos) {

        this.lastSpin = this.spin;

        if (wasOn) {
            if (this.momentum < 100F)
                this.momentum++;
        } else {
            if (this.momentum > 0)
                this.momentum--;
        }

        this.spin += momentum / 2;

        if (this.spin >= 360) {
            this.spin -= 360F;
            this.lastSpin -= 360F;
        }

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, momentum > 0, this::createAudioLoop);

        // Wie im Original nur fuer den eigenen Spieler: der Server schickt keine Bewegung mit.
        Player me = null;
        for (Player p : world.players()) if (p.isLocalPlayer()) me = p;

        if (wasOn && me != null && !me.isCreative()) {
            Direction dir = facing().getClockWise();
            Direction rot = dir.getClockWise();

            if (world.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, -3.5, -19.5)).contains(me)) push(me, dir);
            if (world.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, 3.5, 8.5)).contains(me)) push(me, dir);
            if (world.getEntitiesOfClass(Entity.class, zone(pos, dir, rot, 3.5, 3.75)).contains(me))
                me.makeStuckInBlock(Blocks.COBWEB.defaultBlockState(), new Vec3(0.25D, 0.05D, 0.25D));
        }
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.TurbofanLoopSoundFactory").getMethod("create", MachineTurbofanBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, false, null);
    }

    /** Original {@code e.motionX/Z -= dir.offset * 0.2}. */
    private static void push(Entity e, Direction dir) {
        e.setDeltaMovement(e.getDeltaMovement().subtract(dir.getStepX() * 0.2D, 0D, dir.getStepZ() * 0.2D));
        e.hurtMarked = true;
    }

    /** Wirkbereich von {@code from} bis {@code to} entlang {@code dir}, 1,5 zur Seite, 3 hoch. */
    private static AABB zone(BlockPos pos, Direction dir, Direction rot, double from, double to) {
        double minX = pos.getX() + 0.5 + dir.getStepX() * from - rot.getStepX() * 1.5;
        double maxX = pos.getX() + 0.5 + dir.getStepX() * to + rot.getStepX() * 1.5;
        double minZ = pos.getZ() + 0.5 + dir.getStepZ() * from - rot.getStepZ() * 1.5;
        double maxZ = pos.getZ() + 0.5 + dir.getStepZ() * to + rot.getStepZ() * 1.5;
        return new AABB(Math.min(minX, maxX), pos.getY(), Math.min(minZ, maxZ), Math.max(minX, maxX), pos.getY() + 3, Math.max(minZ, maxZ));
    }

    public long getPowerScaled(long i) {
        return (getEnergyStored() * i) / maxPower;
    }

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { blood }; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank, blood, smoke, smokeLeaded, smokePoison }; }

    public FluidTank getTank() { return tank; }
    public FluidTank getBloodTank() { return blood; }
    public boolean isShowingBlood() { return showBlood; }
    public int getAfterburner() { return afterburner; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "fuel");
        blood.writeToNBT(tag, "blood");
        tag.putBoolean("showBlood", showBlood);
        tag.putByte("afterburner", (byte) afterburner);
        tag.putBoolean("wasOn", wasOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "fuel");
        blood.readFromNBT(tag, "blood");
        showBlood = tag.getBoolean("showBlood");
        afterburner = tag.getByte("afterburner");
        wasOn = tag.getBoolean("wasOn");
    }

    // ── Upgrades ─────────────────────────────────────────────────────────────

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.AFTERBURN;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.AFTERBURN) {
            info.add(Component.translatable(KEY_EFFICIENCY, "+" + (int) (level * 100 * (1 + Math.min(level / 3D, 4D))) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineTurbofanMenu(id, inv, this);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return AABB.ofSize(worldPosition.getCenter(), 64, 64, 64);
    }
}
