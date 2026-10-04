package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFelMenu;
import com.hbm_m.item.machine.ItemFELCrystal;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityFEL}: Freie-Elektronen-Laser. Der Kristall im Platz 1 bestimmt die Wellenlaenge; je Tick kostet
 * der Strahl {@code 1250 * 3^Wellenlaenge} HE. Der Strahl laeuft bis 24 Bloecke weit in Blickrichtung (Hoehe +1),
 * versorgt eine passend ausgerichtete SILEX mit mindestens 5 Bloecken Abstand mit seiner Wellenlaenge (zu nah oder
 * schief gestellte werden abgebaut und fallen als Item heraus), schmilzt Fluessigkeiten weg, zuendet Bloecke mit
 * geringer Explosionsfestigkeit an (Digamma: Digamma-Feuer samt Asche) und trifft Lebewesen im Strahl (Blindheit,
 * Feuer, Strahlung, Digamma).
 */
public class MachineFelBlockEntity extends BaseMachineBlockEntity implements com.hbm_m.api.tile.IControlReceiver {

    public static final long maxPower = 20000000;
    public static final int powerReq = 1250;

    public EnumWavelengths mode = EnumWavelengths.NULL;
    public boolean isOn;
    public boolean missingValidSilex = true;
    public int distance;
    private int audioDuration = 0;

    public MachineFelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEL_BE.get(), pos, state, 2, maxPower, maxPower, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFelBlockEntity be) {
        if (level instanceof ServerLevel world) be.serverTick(world, pos);
        else be.clientTick();
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        Direction dir = getBlockState().getValue(DummyableMachineBlock.FACING);
        BlockPos sub = pos.relative(dir, -5).above();
        this.trySubscribe(world, sub.getX(), sub.getY(), sub.getZ(), dir.getOpposite());
        chargeFromBatterySlot(0);

        ItemStack crystal = inventory.getStackInSlot(1);
        if (this.isOn && !crystal.isEmpty()) {
            if (crystal.getItem() instanceof ItemFELCrystal c) {
                this.mode = c.wavelength;
            } else {
                this.mode = EnumWavelengths.NULL;
            }
        } else {
            this.mode = EnumWavelengths.NULL;
        }

        int range = 24;
        boolean silexSpacing = false;

        int req = (int) (powerReq * ((mode.ordinal() == 0) ? 0 : Math.pow(3, mode.ordinal())));

        if (this.isOn && this.mode != EnumWavelengths.NULL && energy < req) {
            this.energy = 0;
        }

        if (this.isOn && energy >= req && this.mode != EnumWavelengths.NULL) {

            int distance = this.distance - 1;
            double blx = Math.min(pos.getX(), pos.getX() + dir.getStepX() * distance) + 0.2;
            double bux = Math.max(pos.getX(), pos.getX() + dir.getStepX() * distance) + 0.8;
            double bly = Math.min(pos.getY(), 1 + pos.getY() + dir.getStepY() * distance) + 0.2;
            double buy = Math.max(pos.getY(), 1 + pos.getY() + dir.getStepY() * distance) + 0.8;
            double blz = Math.min(pos.getZ(), pos.getZ() + dir.getStepZ() * distance) + 0.2;
            double buz = Math.max(pos.getZ(), pos.getZ() + dir.getStepZ() * distance) + 0.8;

            List<LivingEntity> list = world.getEntitiesOfClass(LivingEntity.class, new AABB(blx, bly, blz, bux, buy, buz));

            for (LivingEntity entity : list) {
                switch (this.mode) {
                    case VISIBLE -> {
                        entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60 * 60 * 65536, 0));
                        com.hbm_m.platform.PlatformHooks.setSecondsOnFire(entity, 10);
                    }
                    case IR, UV -> com.hbm_m.platform.PlatformHooks.setSecondsOnFire(entity, 10);
                    case GAMMA -> ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 25);
                    case DRX -> ContaminationUtil.applyDigammaData(entity, 0.1F);
                    default -> { }
                }
            }

            energy -= req;
            for (int i = 3; i < range; i++) {

                BlockPos p = new BlockPos(pos.getX() + dir.getStepX() * i, pos.getY() + 1, pos.getZ() + dir.getStepZ() * i);
                BlockState bs = world.getBlockState(p);
                Block b = bs.getBlock();

                MachineSilexBlockEntity silexAtBeam = silexAt(world, p);

                // Original: nicht-opake Bloecke (auch Fluessigkeiten) laesst der Strahl durch
                if (silexAtBeam == null && !bs.canOcclude() && b != Blocks.TNT) {
                    this.distance = range;
                    silexSpacing = false;
                    continue;
                }

                if (silexAtBeam != null) {

                    // Original: die SILEX muss genau einen Block hinter dem getroffenen Teil auf Laserhoehe sitzen
                    BlockPos corePos = new BlockPos(p.getX() + dir.getStepX(), pos.getY(), p.getZ() + dir.getStepZ());
                    if (world.getBlockEntity(corePos) instanceof MachineSilexBlockEntity silex) {
                        Direction silexDir = silex.getBlockState().getValue(DummyableMachineBlock.FACING);
                        if ((silexDir == dir || silexDir == dir.getOpposite()) && i >= 5 && !silexSpacing) {
                            if (silex.mode != this.mode) {
                                silex.mode = this.mode;
                                this.missingValidSilex = false;
                                silexSpacing = true;
                                continue;
                            }
                        } else {
                            // Original: falsch gestellte SILEX wird abgebaut und faellt heraus
                            world.destroyBlock(silex.getBlockPos(), false);
                            world.addFreshEntity(new ItemEntity(world, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                                    new ItemStack(ModBlocks.SILEX.get())));
                        }
                    }

                } else {

                    this.distance = i;

                    if (bs.liquid()) {
                        world.playSound(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                        world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                        break;
                    }

                    float hardness = b.getExplosionResistance();
                    if (hardness < 75 && world.random.nextInt(5) == 0) {
                        world.playSound(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                        BlockState fire = (this.mode != EnumWavelengths.DRX) ? Blocks.FIRE.defaultBlockState()
                                : (com.hbm_m.main.Polaroid.id() == 11) ? ModBlocks.DIGAMMA_MATTER.get().defaultBlockState()
                                : ModBlocks.FIRE_DIGAMMA.get().defaultBlockState();
                        world.setBlockAndUpdate(p, fire);
                        if (this.mode == EnumWavelengths.DRX)
                            world.setBlockAndUpdate(p.below(), ModBlocks.ASH_DIGAMMA.get().defaultBlockState());
                    }
                    break;
                }
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    /** SILEX an dieser Stelle (Kernblock oder eine ihrer Strukturzellen), sonst null. */
    private static MachineSilexBlockEntity silexAt(Level world, BlockPos p) {
        var be = world.getBlockEntity(p);
        if (be instanceof MachineSilexBlockEntity silex) return silex;
        if (be instanceof com.hbm_m.interfaces.IMultiblockPart part && part.getControllerPos() != null
                && world.getBlockEntity(part.getControllerPos()) instanceof MachineSilexBlockEntity silex) return silex;
        return null;
    }

    private void clientTick() {
        if (energy > powerReq * Math.pow(2, mode.ordinal()) && isOn && mode != EnumWavelengths.NULL && distance - 3 > 0) {
            audioDuration += 2;
        } else {
            audioDuration -= 3;
        }

        audioDuration = Mth.clamp(audioDuration, 0, 60);

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, audioDuration > 10, this::createAudioLoop);
    }

    public int getAudioDuration() { return audioDuration; }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.FelLoopSoundFactory").getMethod("create", MachineFelBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, false, null);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) toggle();
    }

    /** Original {@code handleButtonPacket(value, 2)}: Ein/Aus. */
    public void toggle() {
        this.isOn = !this.isOn;
        setChanged();
    }

    public long getPowerScaled(long i) {
        return (energy * i) / maxPower;
    }

    /** Fuer GUI und Renderer: laeuft der Strahl gerade? */
    public boolean isBeaming() {
        return energy > powerReq * Math.pow(2, mode.ordinal()) && isOn && mode != EnumWavelengths.NULL;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putLong("power", energy);
        tag.putString("mode", mode.toString());
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("valid", missingValidSilex);
        tag.putInt("distance", distance);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        energy = tag.getLong("power");
        try { mode = EnumWavelengths.valueOf(tag.getString("mode")); } catch (IllegalArgumentException e) { mode = EnumWavelengths.NULL; }
        isOn = tag.getBoolean("isOn");
        missingValidSilex = !tag.contains("valid") || tag.getBoolean("valid");
        distance = tag.getInt("distance");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.fel");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return isEnergyProviderItem(stack);
        return slot == 1;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFelMenu.create(id, inventory, this);
    }

    /** Original: {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
