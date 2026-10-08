package com.hbm_m.blockentity.machines;

import java.util.List;
import java.util.Set;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.generic.NTMPlants;
import com.hbm_m.block.machines.MachineThresherBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.common.IPlantable;
//?}

/**
 * 1:1 {@code TileEntityMachineThresher}: ein schwenkender Erntearm. Die Armspitze wird aus dem Winkel berechnet (zwei
 * Armglieder zu je 4 Bloecken plus Spitze), der Balken davor erntet sieben Felder quer: Sonnenblumen und hohes Gras mit
 * Zufallsdrops, NTM-Hochpflanzen, Zuckerrohr/Kakteen (oberste zwei Bloecke) und alles, was Knochenmehl annimmt, aber
 * nicht mehr waechst (erntet und pflanzt neu). Lebewesen im Balken nehmen 100 Schaden; getoetete Monster geben Salpeter.
 * Laeuft mit Holzoel (oder anderem erlaubten Brennstoff), pausiert per Schraubendreher.
 */
public class MachineThresherBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IFluidStandardReceiverMK2 {

    /** Original: {@code TileEntityMachineAutosaw.acceptedFuels}. */
    public static Set<Fluid> acceptedFuels() {
        return Set.of(ModFluids.WOODOIL.getSource(), ModFluids.ETHANOL.getSource(), ModFluids.FISHOIL.getSource(),
                ModFluids.HEAVYOIL.getSource(), ModFluids.COALCREOSOTE.getSource());
    }

    public final FluidTank tank = new FluidTank(ModFluids.WOODOIL.getSource(), 100);

    public boolean isOn;
    public boolean isSuspended;
    public int delay;
    private int turnProgress;
    public float syncAngle;
    public float angle;
    public float prevAngle;
    // 0: warten, 1: ausfahren, 2: einfahren
    private int state = 0;
    public float spin;
    public float lastSpin;

    public MachineThresherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THRESHER_BE.get(), pos, state);
    }

    public FluidTank getTank() { return tank; }

    /** Original: {@code ForgeDirection.getOrientation(meta).getOpposite()} - im Port die FACING-Richtung. */
    private Direction dir() {
        BlockState s = getBlockState();
        return s.hasProperty(MachineThresherBlock.FACING) ? s.getValue(MachineThresherBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState blockState, MachineThresherBlockEntity be) {
        if (!level.isClientSide) be.serverTick((ServerLevel) level, pos, blockState);
        else be.clientTick(level, pos);
    }

    private void serverTick(ServerLevel world, BlockPos pos, BlockState blockState) {
        Direction dir = dir();
        Direction rot = dir.getClockWise(); // Original: dir.getRotation(UP)

        if (!isSuspended && world.getGameTime() % 20 == 0) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                this.isOn = true;
            } else {
                this.isOn = false;
            }
            trySubscribe(tank.getTankType(), world, pos.relative(rot), rot);
            trySubscribe(tank.getTankType(), world, pos.relative(rot.getOpposite()), rot.getOpposite());
            trySubscribe(tank.getTankType(), world, pos.below(), Direction.DOWN);
        }

        if (isOn && !isSuspended) {
            if (this.state == 0) {
                this.delay--;
                if (delay <= 0) this.state = 1;
            }

            if (this.state == 1) {
                this.angle += 82.5F / 60F;
                if (this.angle >= 82.5F) {
                    this.angle = 82.5F;
                    this.state = 2;
                }
            } else if (this.state == 2) {
                this.angle -= 82.5F / 60F;
                if (this.angle <= 0F) {
                    this.angle = 0F;
                    this.state = 0;
                    this.delay = 200 + world.random.nextInt(100);
                }
            }

            if (this.angle != 0) {
                // Armspitze: Drehpunkt - dir, zwei Glieder zu 4 (Horizontalanteil 4 cos), dazu 2 fuer die Spitze
                double c = Math.cos(Math.toRadians(82.5 - angle));
                double endX = pos.getX() + 0.5 - dir.getStepX() - dir.getStepX() * 8 * c - dir.getStepX() * 2;
                double endZ = pos.getZ() + 0.5 - dir.getStepZ() - dir.getStepZ() * 8 * c - dir.getStepZ() * 2;

                for (int i = -3; i <= 3; i++) {
                    int hitX = Mth.floor(endX + rot.getStepX() * i);
                    int hitZ = Mth.floor(endZ + rot.getStepZ() * i);
                    BlockPos hit = new BlockPos(hitX, pos.getY(), hitZ);
                    BlockState bs = world.getBlockState(hit);
                    Block b = bs.getBlock();

                    if (bs.isRedstoneConductor(world, hit)) {
                        this.state = 2;
                        break;
                    }

                    if (b == Blocks.SUNFLOWER || b == Blocks.TALL_GRASS) {
                        boolean lower = bs.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
                        // Sonnenblume
                        if (b == Blocks.SUNFLOWER && lower && world.random.nextInt(250) == 0) {
                            world.levelEvent(2001, hit, Block.getId(bs));
                            dropItem(new ItemStack(Blocks.SUNFLOWER));
                        }
                        // hohes Gras
                        if (b == Blocks.TALL_GRASS && lower && world.random.nextInt(100) == 0) {
                            world.levelEvent(2001, hit, Block.getId(bs));
                            dropItem(new ItemStack(Items.WHEAT_SEEDS));
                        }
                        continue;
                    }

                    // NTM-Hochpflanzen wie Hanf
                    if (b instanceof NTMPlants.TallPlant tall) {
                        cutTallPlant(world, tall, hit, bs);
                        continue;
                    }

                    if (b instanceof SugarCaneBlock || b instanceof CactusBlock) {
                        cutCane(world, b, hit);
                        continue;
                    }

                    // BonemealableBlock deckt auch alles ab, was Knochenmehl annimmt - daher Feldfruechte zuletzt
                    if (b instanceof BonemealableBlock && !shouldIgnore(world, hit, bs)) cutCrop(world, bs, hit);
                }

                AABB box = new AABB(endX, pos.getY() + 0.5, endZ, endX, pos.getY() + 0.5, endZ)
                        .inflate(Math.abs(dir.getStepX() * 0.5) + Math.abs(rot.getStepX() * 4.5), 0.5, Math.abs(dir.getStepZ() * 0.5) + Math.abs(rot.getStepZ() * 4.5));
                List<LivingEntity> affected = world.getEntitiesOfClass(LivingEntity.class, box);

                for (LivingEntity e : affected) {
                    if (e.isAlive() && e.hurt(com.hbm_m.damagesource.ModDamageSources.blender(world), 100)) {
                        if (e instanceof Enemy && !e.isAlive()) this.dropItem(new ItemStack(ModItems.NITRA_SMALL.get()));
                        world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.95F + world.random.nextFloat() * 0.2F);
                        int count = Math.min((int) Math.ceil(e.getMaxHealth() / 4), 250);
                        CompoundTag data = new CompoundTag();
                        data.putString("type", "vanillaburst");
                        data.putInt("count", count * 4);
                        data.putDouble("motion", 0.1D);
                        data.putString("mode", "blockdust");
                        data.putInt("block", Block.getId(Blocks.REDSTONE_BLOCK.defaultBlockState()));
                        for (ServerPlayer p : world.players()) {
                            if (p.distanceToSqr(e.getX(), e.getY(), e.getZ()) < 50 * 50)
                                com.hbm_m.network.AuxParticlePacket.sendTo(p, data, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
                        }
                    }
                }
            }
        }

        setChanged();
        world.sendBlockUpdated(pos, blockState, blockState, 3);
    }

    private void clientTick(Level world, BlockPos pos) {
        this.lastSpin = this.spin;

        if (isOn && !isSuspended) {
            if (this.angle > 0) this.spin += 15F;
            Direction dir = dir();
            Direction rot = dir.getClockWise();
            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + dir.getStepX() * 0.8125 + rot.getStepX() * 0.375, pos.getY() + 1.5625,
                    pos.getZ() + 0.5 + dir.getStepZ() * 0.8125 + rot.getStepZ() * 0.375, 0, 0, 0);
        }

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, isOn && !isSuspended, this::createAudioLoop);

        if (this.spin >= 360F) {
            this.spin -= 360F;
            this.lastSpin -= 360F;
        }

        this.prevAngle = this.angle;

        if (this.turnProgress > 0) {
            double d0 = Mth.wrapDegrees(this.syncAngle - (double) this.angle);
            this.angle = (float) ((double) this.angle + d0 / (double) this.turnProgress);
            --this.turnProgress;
        } else {
            this.angle = this.syncAngle;
        }
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.ThresherLoopSoundFactory").getMethod("create", MachineThresherBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Original: {@code IGrowable.func_149851_a} - was noch waechst, wird nicht geerntet. */
    public static boolean shouldIgnore(Level world, BlockPos pos, BlockState state) {
        //? if < 1.21.1 {
        if (state.getBlock() instanceof BonemealableBlock g) return g.isValidBonemealTarget(world, pos, state, world.isClientSide);
        //?} else {
        /*if (state.getBlock() instanceof BonemealableBlock g) return g.isValidBonemealTarget(world, pos, state);
        *///?}
        return false;
    }

    protected void cutTallPlant(ServerLevel world, NTMPlants.TallPlant b, BlockPos pos, BlockState bs) {
        // trifft den unteren Block: eins nach oben
        if (bs.getValue(NTMPlants.TallPlant.HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.above();
            bs = world.getBlockState(pos);
            if (!bs.is(b)) return;
        }
        // unreife Weiden ignorieren
        if (b.type == NTMPlants.TallType.CD2 || b.type == NTMPlants.TallType.CD3) return;

        world.levelEvent(2001, pos, Block.getId(bs));
        for (ItemStack drop : Block.getDrops(bs, world, pos, null)) dropItem(drop);
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    /** Entfernt die oberen zwei Bloecke dreihoher Pflanzen wie Kakteen und Zuckerrohr. */
    protected void cutCane(ServerLevel world, Block target, BlockPos pos) {
        // wer die Maschine einen Block zu hoch setzt, bekommt einen Ausgleich
        int offset = world.getBlockState(pos.below()).is(target) ? -1 : 0;

        // von oben nach unten
        for (int i = 2 + offset; i > offset; i--) {
            BlockPos p = pos.above(i);
            BlockState bs = world.getBlockState(p);
            world.levelEvent(2001, p, Block.getId(bs));
            for (ItemStack drop : Block.getDrops(bs, world, p, null)) dropItem(drop);
            world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Erntet Feldfruechte wie Weizen und pflanzt neu. */
    protected void cutCrop(ServerLevel world, BlockState bs, BlockPos pos) {
        BlockState soil = world.getBlockState(pos.below());
        world.levelEvent(2001, pos, Block.getId(bs));
        BlockState replacement = Blocks.AIR.defaultBlockState();

        List<ItemStack> drops = Block.getDrops(bs, world, pos, null);
        boolean replanted = false;

        for (ItemStack drop : drops) {
            //? if forge {
            if (!replanted && drop.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof IPlantable seed) {
                if (soil.canSustainPlant(world, pos.below(), Direction.UP, seed)) {
                    replacement = seed.getPlant(world, pos);
                    replanted = true;
                    drop.shrink(1);
                }
            }
            //?} else {
            /*// NeoForge hat kein IPlantable mehr: Forge-1.20.1-IPlantables waren BushBlock, Kaktus und Zuckerrohr;
            // getPlant = vorhandener Zustand, falls derselbe Block, sonst Grundzustand.
            if (!replanted && drop.getItem() instanceof net.minecraft.world.item.BlockItem bi
                    && (bi.getBlock() instanceof net.minecraft.world.level.block.BushBlock || bi.getBlock() instanceof net.minecraft.world.level.block.CactusBlock
                        || bi.getBlock() instanceof net.minecraft.world.level.block.SugarCaneBlock)) {
                Block seed = bi.getBlock();
                BlockState cur = world.getBlockState(pos);
                BlockState plant = cur.getBlock() == seed ? cur : seed.defaultBlockState();
                net.neoforged.neoforge.common.util.TriState t = soil.canSustainPlant(world, pos.below(), Direction.UP, plant);
                if (t.isDefault() ? plant.canSurvive(world, pos) : t.isTrue()) {
                    replacement = plant;
                    replanted = true;
                    drop.shrink(1);
                }
            }
            *///?}
            if (!drop.isEmpty()) dropItem(drop);
        }

        // voll ausgewachsener Weizen gab frueher manchmal keine Samen
        if (bs.is(Blocks.WHEAT) && !replanted) {
            replacement = Blocks.WHEAT.defaultBlockState();
        }

        world.setBlock(pos, replacement, 3);
    }

    protected void dropItem(ItemStack drop) {
        Direction meta = dir().getOpposite();
        double spawnX = worldPosition.getX() + 0.5 - meta.getStepX() * 0.75;
        double spawnZ = worldPosition.getZ() + 0.5 - meta.getStepZ() * 0.75;
        ItemEntity entityItem = new ItemEntity(level, spawnX, worldPosition.getY(), spawnZ, drop);
        entityItem.setPickUpDelay(10);
        entityItem.setDeltaMovement(meta.getStepX() * -0.2 + 0.2, entityItem.getDeltaMovement().y, meta.getStepZ() * -0.2);
        level.addFreshEntity(entityItem);
    }

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        nbt.putBoolean("isOn", this.isOn);
        nbt.putBoolean("isSuspended", this.isSuspended);
        nbt.putFloat("angle", this.angle);
        nbt.putInt("state", this.state);
        tank.writeToNBT(nbt, "t");
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        this.isOn = nbt.getBoolean("isOn");
        this.isSuspended = nbt.getBoolean("isSuspended");
        this.angle = nbt.getFloat("angle");
        this.state = nbt.getInt("state");
        tank.readFromNBT(nbt, "t");
    }

    /** Original {@code deserialize}: Winkel dreistufig nachziehen. */
    @Override
    protected void applyClientUpdate(CompoundTag nbt) {
        this.isOn = nbt.getBoolean("isOn");
        this.isSuspended = nbt.getBoolean("isSuspended");
        this.syncAngle = nbt.getFloat("angle");
        this.turnProgress = 3;
        tank.readFromNBT(nbt, "t");
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 10, worldPosition.getY(), worldPosition.getZ() - 10,
                worldPosition.getX() + 11, worldPosition.getY() + 7, worldPosition.getZ() + 11);
        return bb;
    }
}
