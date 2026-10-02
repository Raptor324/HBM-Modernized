package com.hbm_m.blockentity.machines;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.generic.NTMPlants;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.DeadBushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IPlantable;

/**
 * 1:1 {@code TileEntityMachineAutosaw}: ein drehender Saegearm. Im Suchmodus dreht er sich um 1 Grad pro Tick und
 * prueft im Ring 2-9 Bloecke um den Arm (5 Grad Kegel) nach Holz, Blaettern und Pflanzen; bei Fund faehrt der Arm aus,
 * das Saegeblatt erntet an der Spitze (zwei Armglieder zu je 4 plus Spitze 2) Pflanzen und faellt Baeume. Faellen:
 * alle Staemme im Arbeitsgebiet werden gesucht, ein 0-1-BFS (senkrecht 0, waagerecht 1, 18er-Nachbarschaft) verteilt
 * Holz und Laub auf den naechsten Stamm, nur der Baum des getroffenen Stamms wird gefaellt; im Arbeitsgebiet wird auf
 * tragfaehigem Boden ein passender Setzling gepflanzt. Wand im Weg: zurueckfahren und 5 Ticks nicht suchen.
 * <p>
 * Materialzuordnung (1.20 hat keine {@code Material}s mehr): Holz = Instrument Bass (so markiert Vanilla alle
 * frueheren Holz-Materialbloecke), Laub = Blaetter-Tag/{@link LeavesBlock}, Pflanzen = Busch-Bloecke ohne Gras/Farn und
 * toten Busch (im Original Material.vine), dazu Zuckerrohr, Kakao und die NTM-Hochpflanzen.
 */
public class MachineAutosawBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IFluidStandardReceiverMK2 {

    private static final int MIN_DIST = 2;
    private static final int MAX_DIST = 9;

    private static final int FELL_HORIZONTAL_RANGE = 10;
    private static final int FELL_BFS_RADIUS = MAX_DIST + FELL_HORIZONTAL_RANGE;
    private static final int FELL_VERTICAL_RANGE = 32;
    private static final int FELL_MAX_BASE_DEPTH = FELL_VERTICAL_RANGE / 2;

    // 18er-Nachbarschaft: 6 Flaechen- plus 12 Kantennachbarn (genau eine Koordinate 0)
    private static final int[][] EIGHTEEN_DIRS = {
        {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
        {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
        {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
        {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1}
    };

    public static Set<Fluid> acceptedFuels() {
        return Set.of(ModFluids.WOODOIL.getSource(), ModFluids.ETHANOL.getSource(), ModFluids.FISHOIL.getSource(),
                ModFluids.HEAVYOIL.getSource(), ModFluids.COALCREOSOTE.getSource());
    }

    public final FluidTank tank = new FluidTank(ModFluids.WOODOIL.getSource(), 100);

    public boolean isOn;
    public boolean isSuspended;
    private int forceSkip;
    public float syncYaw;
    public float rotationYaw;
    public float prevRotationYaw;
    public float syncPitch;
    public float rotationPitch;
    public float prevRotationPitch;

    // 0: suchen, 1: ausfahren, 2: einfahren
    private int state = 0;

    private int turnProgress;

    public float spin;
    public float lastSpin;

    public MachineAutosawBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOSAW_BE.get(), pos, state);
    }

    public FluidTank getTank() { return tank; }

    public static void tick(Level level, BlockPos pos, BlockState blockState, MachineAutosawBlockEntity be) {
        if (!level.isClientSide) be.serverTick((ServerLevel) level, pos, blockState);
        else be.clientTick(level, pos);
    }

    private void serverTick(ServerLevel world, BlockPos pos, BlockState blockState) {

        if (!isSuspended && world.getGameTime() % 20 == 0) {
            if (tank.getFill() > 0) {
                tank.setFill(tank.getFill() - 1);
                this.isOn = true;
            } else {
                this.isOn = false;
            }

            for (Direction dir : Direction.values()) {
                if (dir != Direction.UP) trySubscribe(tank.getTankType(), world, pos.relative(dir), dir);
            }
        }

        if (isOn && !isSuspended) {
            Vec3 pivot = new Vec3(pos.getX() + 0.5, pos.getY() + 1.75, pos.getZ() + 0.5);
            Vec3 upperArm = new Vec3(0, 0, -4).xRot((float) Math.toRadians(80 - rotationPitch)).yRot(-(float) Math.toRadians(rotationYaw));
            Vec3 lowerArm = new Vec3(0, 0, -4).xRot((float) -Math.toRadians(80 - rotationPitch)).yRot(-(float) Math.toRadians(rotationYaw));
            Vec3 armTip = new Vec3(0, 0, -2).yRot(-(float) Math.toRadians(rotationYaw));

            double cX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
            double cY = pivot.y;
            double cZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;

            List<LivingEntity> affected = world.getEntitiesOfClass(LivingEntity.class, new AABB(cX - 1, cY - 0.25, cZ - 1, cX + 1, cY + 0.25, cZ + 1));

            for (LivingEntity e : affected) {
                if (e.isAlive() && e.hurt(com.hbm_m.damagesource.ModDamageSources.blender(world), 100)) {
                    world.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F, 0.95F + world.random.nextFloat() * 0.2F);
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

            if (state == 0) {

                this.rotationYaw += 1;

                if (this.rotationYaw >= 360) {
                    this.rotationYaw -= 360;
                }

                if (forceSkip > 0) {
                    forceSkip--;
                } else {
                    final double CUT_ANGLE = Math.toRadians(5);
                    double rotationYawRads = Math.toRadians((rotationYaw + 270) % 360);

                    outer:
                    for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
                        for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                            int sqrDst = dx * dx + dz * dz;

                            if (sqrDst <= MIN_DIST * MIN_DIST || sqrDst > MAX_DIST * MAX_DIST)
                                continue;

                            double angle = Math.atan2(dz, dx);
                            double relAngle = Math.abs(angle - rotationYawRads);
                            relAngle = Math.abs((relAngle + Math.PI) % (2 * Math.PI) - Math.PI);

                            if (relAngle > CUT_ANGLE) continue;

                            BlockPos p = pos.offset(dx, 1, dz);
                            BlockState b = world.getBlockState(p);
                            if (!(isWood(b) || isLeaves(b) || isPlant(b)))
                                continue;

                            if (shouldIgnore(world, p, b)) continue;

                            state = 1;
                            break outer;
                        }
                    }
                }
            }

            int hitY = Mth.floor(cY);
            int hitX0 = Mth.floor(cX - 0.5);
            int hitZ0 = Mth.floor(cZ - 0.5);
            int hitX1 = Mth.floor(cX + 0.5);
            int hitZ1 = Mth.floor(cZ + 0.5);

            this.tryInteract(world, hitX0, hitY, hitZ0);
            this.tryInteract(world, hitX1, hitY, hitZ0);
            this.tryInteract(world, hitX0, hitY, hitZ1);
            this.tryInteract(world, hitX1, hitY, hitZ1);

            if (state == 1) {
                this.rotationPitch += 2;

                if (this.rotationPitch > 80) {
                    this.rotationPitch = 80;
                    state = 2;
                }
            }

            if (state == 2) {
                this.rotationPitch -= 2;

                if (this.rotationPitch <= 0) {
                    this.rotationPitch = 0;
                    state = 0;
                }
            }
        }

        setChanged();
        world.sendBlockUpdated(pos, blockState, blockState, 3);
    }

    private void clientTick(Level world, BlockPos pos) {

        this.lastSpin = this.spin;

        if (isOn && !isSuspended) {
            this.spin += 15F;

            Vec3 vec = new Vec3(0.625, 0, 1.625).yRot(-(float) Math.toRadians(rotationYaw));

            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + vec.x, pos.getY() + 2.0625, pos.getZ() + 0.5 + vec.z, 0, 0, 0);
        }

        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, isOn && !isSuspended, this::createAudioLoop);

        if (this.spin >= 360F) {
            this.spin -= 360F;
            this.lastSpin -= 360F;
        }

        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;

        if (this.turnProgress > 0) {
            double d0 = Mth.wrapDegrees(this.syncYaw - (double) this.rotationYaw);
            double d1 = Mth.wrapDegrees(this.syncPitch - (double) this.rotationPitch);
            this.rotationYaw = (float) ((double) this.rotationYaw + d0 / (double) this.turnProgress);
            this.rotationPitch = (float) ((double) this.rotationPitch + d1 / (double) this.turnProgress);
            --this.turnProgress;
        } else {
            this.rotationYaw = this.syncYaw;
            this.rotationPitch = this.syncPitch;
        }
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.AutosawLoopSoundFactory").getMethod("create", MachineAutosawBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Original: {@code Material.wood}. */
    public static boolean isWood(BlockState b) {
        return b.instrument() == NoteBlockInstrument.BASS;
    }

    /** Original: {@code Material.leaves}. */
    public static boolean isLeaves(BlockState b) {
        return b.is(BlockTags.LEAVES) || b.getBlock() instanceof LeavesBlock;
    }

    /** Original: {@code Material.plants} (Gras, Farn und toter Busch waren {@code Material.vine}). */
    public static boolean isPlant(BlockState b) {
        Block block = b.getBlock();
        if (block instanceof TallGrassBlock || block instanceof DeadBushBlock) return false;
        return block instanceof BushBlock || block instanceof SugarCaneBlock || block instanceof CocoaBlock || block instanceof NTMPlants.TallPlant;
    }

    /** Was weder Detektor noch Saegeblatt anruehren sollen, etwa unreife Weiden (obere Haelfte CD2/CD3). */
    public static boolean shouldIgnore(Level world, BlockPos pos, BlockState b) {
        if (b.getBlock() instanceof NTMPlants.TallPlant tall) {
            return b.getValue(NTMPlants.TallPlant.HALF) == DoubleBlockHalf.UPPER
                    && (tall.type == NTMPlants.TallType.CD2 || tall.type == NTMPlants.TallType.CD3);
        }
        return false;
    }

    protected void tryInteract(ServerLevel world, int x, int y, int z) {

        BlockPos p = new BlockPos(x, y, z);
        BlockState b = world.getBlockState(p);

        if (!shouldIgnore(world, p, b)) {
            if (isLeaves(b) || isPlant(b)) {
                cutCrop(world, p);
            } else if (isWood(b)) {
                fellTree(world, x, y, z);
                if (state == 1) {
                    state = 2;
                }
            }
        }

        // Zurueckfahren, wenn eine Wand im Weg ist
        if (state == 1 && world.getBlockState(p).isRedstoneConductor(world, p)) {
            state = 2;
            forceSkip = 5;
        }
    }

    protected void cutCrop(ServerLevel world, BlockPos p) {

        BlockState b = world.getBlockState(p);

        world.levelEvent(2001, p, Block.getId(b));

        for (ItemStack drop : Block.getDrops(b, world, p, world.getBlockEntity(p))) {

            float delta = 0.7F;
            double dx = (double) (world.random.nextFloat() * delta) + (double) (1.0F - delta) * 0.5D;
            double dy = (double) (world.random.nextFloat() * delta) + (double) (1.0F - delta) * 0.5D;
            double dz = (double) (world.random.nextFloat() * delta) + (double) (1.0F - delta) * 0.5D;

            ItemEntity entityItem = new ItemEntity(world, p.getX() + dx, p.getY() + dy, p.getZ() + dz, drop);
            entityItem.setPickUpDelay(10);
            world.addFreshEntity(entityItem);
        }

        world.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
    }

    protected void fellTree(ServerLevel world, int hitX, int hitY, int hitZ) {

        int sawY = hitY;
        int xCoord = worldPosition.getX();
        int zCoord = worldPosition.getZ();
        BlockPos hitCol = new BlockPos(hitX, -1, hitZ);

        // Schritt A: Arbeitsgebiet nach Staemmen absuchen (Spalte -> Stammfuss)
        HashMap<BlockPos, BlockPos> trunks = new HashMap<>();

        for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
            for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                if (dx * dx + dz * dz > MAX_DIST * MAX_DIST) {
                    continue;
                }

                int colX = xCoord + dx;
                int colZ = zCoord + dz;

                if (!isWood(world.getBlockState(new BlockPos(colX, sawY, colZ)))) {
                    continue;
                }

                int baseY = sawY;
                while (sawY - baseY < FELL_MAX_BASE_DEPTH && isWood(world.getBlockState(new BlockPos(colX, baseY - 1, colZ)))) {
                    baseY--;
                }

                if (!canSupportSapling(world, colX, baseY - 1, colZ)) {
                    continue;
                }

                trunks.put(new BlockPos(colX, -1, colZ), new BlockPos(colX, baseY, colZ));
            }
        }

        // Der getroffene Stamm ist immer dabei
        if (!trunks.containsKey(hitCol)) {
            int baseY = hitY;
            while (sawY - baseY < FELL_MAX_BASE_DEPTH && isWood(world.getBlockState(new BlockPos(hitX, baseY - 1, hitZ)))) {
                baseY--;
            }
            trunks.put(hitCol, new BlockPos(hitX, baseY, hitZ));
        }

        // Schritt B: 0-1-BFS von allen Staemmen; senkrechte Nachbarn Abstand 0, waagerechte 1.
        // blockOwner: Blockposition -> Spalte des besitzenden Stamms
        HashMap<BlockPos, BlockPos> blockOwner = new HashMap<>();
        ArrayDeque<BlockPos[]> deque = new ArrayDeque<>();
        int hitColCount = 1;

        // Original: 0 bis 255
        int minY = Math.max(world.getMinBuildHeight(), sawY - FELL_MAX_BASE_DEPTH);
        int maxY = Math.min(world.getMaxBuildHeight() - 1, sawY + FELL_VERTICAL_RANGE);

        for (Map.Entry<BlockPos, BlockPos> trunk : trunks.entrySet()) {
            deque.addFirst(new BlockPos[] { trunk.getValue(), trunk.getKey() });
        }

        while (!deque.isEmpty()) {
            BlockPos[] pair = deque.pollFirst();
            BlockPos current = pair[0];
            BlockPos currentCol = pair[1];

            if (blockOwner.containsKey(current)) {
                if (currentCol.equals(hitCol)) {
                    hitColCount--;
                    if (hitColCount == 0) {
                        break;
                    }
                }
                continue;
            }
            blockOwner.put(current, currentCol);

            for (int[] dir : EIGHTEEN_DIRS) {
                int neighborX = current.getX() + dir[0];
                int neighborY = current.getY() + dir[1];
                int neighborZ = current.getZ() + dir[2];

                // Grenzen: waagerecht Radius FELL_BFS_RADIUS, senkrecht minY bis maxY
                int neighborDx = neighborX - xCoord;
                int neighborDz = neighborZ - zCoord;
                if (neighborDx * neighborDx + neighborDz * neighborDz > FELL_BFS_RADIUS * FELL_BFS_RADIUS) {
                    continue;
                }
                if (neighborY < minY || neighborY > maxY) {
                    continue;
                }

                BlockPos neighborPos = new BlockPos(neighborX, neighborY, neighborZ);
                if (blockOwner.containsKey(neighborPos)) {
                    continue;
                }

                BlockState b = world.getBlockState(neighborPos);
                if (!isWood(b) && !isLeaves(b)) {
                    continue;
                }

                boolean hasHorizontal = dir[0] != 0 || dir[2] != 0;
                BlockPos[] entry = new BlockPos[] { neighborPos, currentCol };
                if (!hasHorizontal) {
                    deque.addFirst(entry);
                } else {
                    deque.addLast(entry);
                }
                if (currentCol.equals(hitCol)) {
                    hitColCount++;
                }
            }

            if (currentCol.equals(hitCol)) {
                hitColCount--;
                if (hitColCount == 0) {
                    break; // alle Bloecke des getroffenen Baums verarbeitet
                }
            }
        }

        // Schritt C: dem getroffenen Stamm zugeordnete Bloecke faellen
        for (Map.Entry<BlockPos, BlockPos> entry : blockOwner.entrySet()) {
            if (!entry.getValue().equals(hitCol)) {
                continue;
            }

            BlockPos p = entry.getKey();
            BlockState b = world.getBlockState(p);

            // im Arbeitsgebiet Setzling nachpflanzen
            if (isWood(b) && isWithinWorkingArea(p.getX(), p.getZ()) && canSupportSapling(world, p.getX(), p.getY() - 1, p.getZ())) {
                BlockState sapling = saplingFor(b);
                world.destroyBlock(p, true);
                world.setBlock(p, sapling, 3);
            } else {
                world.destroyBlock(p, true);
            }
        }
    }

    /**
     * Original: {@code Blocks.log} Meta &amp; 3 bzw. {@code Blocks.log2} (Meta &amp; 3) + 4 als Setzlingsmeta, alles andere
     * Eiche. Im Port ueber die Holzart der Vanilla-Staemme und -Holzbloecke (auch entrindet).
     */
    private static BlockState saplingFor(BlockState log) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        if ("minecraft".equals(id.getNamespace())) {
            String path = id.getPath();
            if (path.startsWith("stripped_")) path = path.substring(9);
            if (path.endsWith("_log") || path.endsWith("_wood")) {
                String wood = path.substring(0, path.lastIndexOf('_'));
                switch (wood) {
                    case "spruce": return Blocks.SPRUCE_SAPLING.defaultBlockState();
                    case "birch": return Blocks.BIRCH_SAPLING.defaultBlockState();
                    case "jungle": return Blocks.JUNGLE_SAPLING.defaultBlockState();
                    case "acacia": return Blocks.ACACIA_SAPLING.defaultBlockState();
                    case "dark_oak": return Blocks.DARK_OAK_SAPLING.defaultBlockState();
                    default: break;
                }
            }
        }
        return Blocks.OAK_SAPLING.defaultBlockState();
    }

    private boolean isWithinWorkingArea(int x, int z) {
        int dx = x - worldPosition.getX();
        int dz = z - worldPosition.getZ();
        int distSq = dx * dx + dz * dz;
        return distSq > MIN_DIST * MIN_DIST && distSq <= MAX_DIST * MAX_DIST;
    }

    private static boolean canSupportSapling(Level world, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return world.getBlockState(p).canSustainPlant(world, p, Direction.UP, (IPlantable) Blocks.OAK_SAPLING);
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
        nbt.putInt("skip", this.forceSkip);
        nbt.putFloat("yaw", this.rotationYaw);
        nbt.putFloat("pitch", this.rotationPitch);
        nbt.putInt("state", this.state);
        tank.writeToNBT(nbt, "t");
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        this.isOn = nbt.getBoolean("isOn");
        this.isSuspended = nbt.getBoolean("isSuspended");
        this.forceSkip = nbt.getInt("skip");
        this.rotationYaw = nbt.getFloat("yaw");
        this.rotationPitch = nbt.getFloat("pitch");
        this.state = nbt.getInt("state");
        tank.readFromNBT(nbt, "t");
    }

    /** Original {@code deserialize}: Drehung dreistufig nachziehen. */
    @Override
    protected void applyClientUpdate(CompoundTag nbt) {
        this.isOn = nbt.getBoolean("isOn");
        this.isSuspended = nbt.getBoolean("isSuspended");
        this.syncYaw = nbt.getFloat("yaw");
        this.syncPitch = nbt.getFloat("pitch");
        this.turnProgress = 3;
        tank.readFromNBT(nbt, "t");
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 12, worldPosition.getY(), worldPosition.getZ() - 12,
                worldPosition.getX() + 13, worldPosition.getY() + 10, worldPosition.getZ() + 13);
        return bb;
    }
}
