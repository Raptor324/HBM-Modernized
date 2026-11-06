package com.hbm_m.block.gas;

import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.item.gasmask.IGasMask;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1-Port von {@code com.hbm.blocks.gas.BlockGasBase} (1.7.10): ein unsichtbarer, nicht
 * kollidierender Block, der sich ueber geplante Blockupdates durch die Luft bewegt.
 */
public abstract class BlockGasBase extends Block {

    private final float red;
    private final float green;
    private final float blue;

    /**
     * @param r,g,b Wolkenfarbe aus dem Original-Konstruktor fuer Partikelwolke mit Aschebrille.
     */
    protected BlockGasBase(float r, float g, float b) {
        super(gasProps());
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    protected BlockGasBase() {
        this(1.0F, 1.0F, 1.0F);
    }

    public float getRed()   { return red; }
    public float getGreen() { return green; }
    public float getBlue()  { return blue; }

    /** 1:1 {@code randomDisplayTick}: mit Aschebrille (Helmplatz) eine eingefaerbte "vanillaExt"-Wolke. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);

        Player p = com.hbm_m.client.ClientAccess.player();
        if (p != null && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(com.hbm_m.item.ModItems.ASHGLASSES.get())
                && (!com.hbm_m.api.space.SpaceApi.forkActive() || rand.nextInt(9) == 1)) {
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putString("type", "vanillaExt");
            data.putString("mode", "cloud");
            data.putDouble("posX", pos.getX() + 0.5);
            data.putDouble("posY", pos.getY() + 0.5);
            data.putDouble("posZ", pos.getZ() + 0.5);
            data.putFloat("r", red);
            data.putFloat("g", green);
            data.putFloat("b", blue);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }

        Player player = level.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 24.0D, EntitySelector.NO_SPECTATORS);
        if (player != null && isHoldingGas(player, this)) {
            level.addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state),
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    0.0D, 0.0D, 0.0D
            );
        }

        spawnAmbientParticles(state, level, pos, rand);
    }

    protected void spawnAmbientParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
    }

    protected static boolean isHoldingGas(Player player, Block gasBlock) {
        if (player.isHolding(gasBlock.asItem())) {
            return true;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return (main.getItem() instanceof BlockItem bi1 && bi1.getBlock() instanceof BlockGasBase)
                || (off.getItem() instanceof BlockItem bi2 && bi2.getBlock() instanceof BlockGasBase);
    }

    protected static Block.Properties gasProps() {
        return Block.Properties.of()
                .noCollission()
                .noOcclusion()
                .replaceable()
                .instabreak()
                .strength(-1.0F, 6000000.0F)
                .noLootTable()
                .mapColor(MapColor.NONE);
    }

    /* =================== RENDER & FORM =================== */

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    @Deprecated
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /* =================== AUSBREITUNG =================== */

    @Override
    @Deprecated
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 10);
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 10);
    }

    @Override
    @Deprecated
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int decay = getDecayChance();
        if (decay > 0 && random.nextInt(decay) == 0) {
            level.removeBlock(pos, false);
            return;
        }

        if (onGasTick(state, level, pos, random)) return;

        if (!tryMove(level, pos, state, getFirstDirection(level, pos, random))) {
            if (!tryMove(level, pos, state, getSecondDirection(level, pos, random))) {
                level.scheduleTick(pos, this, getDelay(level, random));
            }
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, getDelay(level, random));
        }
    }

    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return false;
    }

    protected boolean tryMove(ServerLevel level, BlockPos pos, BlockState state, Direction dir) {
        if (dir == null) return false;
        BlockPos target = pos.relative(dir);

        if (level.getBlockState(target).isAir()) {
            level.removeBlock(pos, false);
            level.setBlock(target, state, 3);
            level.scheduleTick(target, this, getDelay(level, level.random));
            return true;
        }
        return false;
    }

    public abstract Direction getFirstDirection(Level level, BlockPos pos, RandomSource random);

    public Direction getSecondDirection(Level level, BlockPos pos, RandomSource random) {
        return getFirstDirection(level, pos, random);
    }

    public int getDelay(Level level, RandomSource random) {
        return 2;
    }

    protected int getDecayChance() {
        return 0;
    }

    protected static Direction randomHorizontal(RandomSource random) {
        return Direction.from3DDataValue(random.nextInt(4) + 2);
    }

    protected static Direction randomAny(RandomSource random) {
        return Direction.from3DDataValue(random.nextInt(6));
    }

    protected static Direction randomVertical(RandomSource random) {
        return Direction.from3DDataValue(random.nextInt(2));
    }

    /* =================== WIRKUNG =================== */

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        affect(living);
    }

    protected abstract void affect(LivingEntity living);

    protected static void damageWornFilter(LivingEntity living) {
        GasMaskUtil.WornMask worn = GasMaskUtil.resolveWornMaskRef(living);
        if (worn.mask().getItem() instanceof IGasMask && IGasMask.damageFilter(worn.mask(), 1)) {
            worn.commit();
        }
    }
}
