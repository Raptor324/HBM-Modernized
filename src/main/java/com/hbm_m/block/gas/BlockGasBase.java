package com.hbm_m.block.gas;

import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.item.gasmask.IGasMask;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1-Port von {@code com.hbm.blocks.gas.BlockGasBase} (1.7.10): ein unsichtbarer, nicht
 * kollidierender Block, der sich ueber geplante Blockupdates durch die Luft bewegt.
 *
 * <p>Der Bewegungsablauf ist der des Originals: pro Tick wird zuerst
 * {@link #getFirstDirection} versucht, danach {@link #getSecondDirection}; klappt keins von beiden,
 * plant sich der Block mit {@link #getDelay} neu ein. Der Zerfall ({@link #getDecayChance}) laeuft
 * im Original in den Unterklassen vor dem {@code super}-Aufruf - hier als Haken, damit die
 * Unterklassen nur noch ihre Wahrscheinlichkeit angeben muessen.</p>
 */
public abstract class BlockGasBase extends Block {

    private final float red;
    private final float green;
    private final float blue;

    /**
     * @param r,g,b Wolkenfarbe aus dem Original-Konstruktor. Sie faerbt dort die Partikelwolke,
     *              die nur mit aufgesetzter Aschebrille ({@code ashglasses}) sichtbar wird.
     *              Im Port ist {@code ASHGLASSES} noch ein reiner Gegenstand ohne Ruestungsslot,
     *              darum wird die Farbe bisher nur vorgehalten - siehe {@link #animateTick}.
     */
    protected BlockGasBase(float r, float g, float b) {
        super(gasProps());
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    public float getRed()   { return red; }
    public float getGreen() { return green; }
    public float getBlue()  { return blue; }

    protected static Block.Properties gasProps() {
        return Block.Properties.of()
                .noCollission()
                .noOcclusion()
                .replaceable()
                .instabreak()
                .strength(-1.0F, 6000000.0F)
                .noLootTable()
                .mapColor(net.minecraft.world.level.material.MapColor.NONE);
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    // ═════════════════════════════ Ausbreitung ═════════════════════════════

    /** Original: {@code onBlockAdded} plant den ersten Zug nach 10 Ticks ein. */
    @Override
    @Deprecated
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) level.scheduleTick(pos, this, 10);
    }

    /** Original: {@code onNeighborBlockChange} setzt den Block zurueck und plant neu ein. */
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

        // Zusatzverhalten der Unterklasse (Verbrennen, Bodenverseuchung, Folgegas ...).
        if (onGasTick(state, level, pos, random)) return;

        if (!tryMove(level, pos, state, getFirstDirection(level, pos, random))) {
            if (!tryMove(level, pos, state, getSecondDirection(level, pos, random))) {
                level.scheduleTick(pos, this, getDelay(level));
            }
        }
    }

    /**
     * Zusatzverhalten vor der Bewegung.
     *
     * @return true, wenn der Block dabei verschwunden ist - dann folgt keine Bewegung mehr.
     */
    protected boolean onGasTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return false;
    }

    /** Verschiebt das Gas, wenn das Ziel Luft ist. */
    protected boolean tryMove(ServerLevel level, BlockPos pos, BlockState state, Direction dir) {
        BlockPos target = pos.relative(dir);

        if (level.getBlockState(target).isAir()) {
            level.removeBlock(pos, false);
            level.setBlock(target, state, 3);
            level.scheduleTick(target, this, getDelay(level));
            return true;
        }
        return false;
    }

    public abstract Direction getFirstDirection(ServerLevel level, BlockPos pos, RandomSource random);

    public Direction getSecondDirection(ServerLevel level, BlockPos pos, RandomSource random) {
        return getFirstDirection(level, pos, random);
    }

    /** Original: {@code getDelay} - Standard sind zwei Ticks. */
    public int getDelay(Level level) {
        return 2;
    }

    /** 1 zu N Wahrscheinlichkeit, dass sich das Gas in diesem Tick aufloest; 0 = nie. */
    protected int getDecayChance() {
        return 0;
    }

    /** Original: {@code randomHorizontal}. */
    protected static Direction randomHorizontal(RandomSource random) {
        return Direction.from3DDataValue(random.nextInt(4) + 2);
    }

    /** Original: {@code ForgeDirection.getOrientation(rand.nextInt(6))} - alle sechs Richtungen. */
    protected static Direction randomAny(RandomSource random) {
        return Direction.from3DDataValue(random.nextInt(6));
    }

    /** Original: {@code ForgeDirection.getOrientation(rand.nextInt(2))} - nur unten oder oben. */
    protected static Direction randomVertical(RandomSource random) {
        return Direction.from3DDataValue(random.nextInt(2));
    }

    // ═════════════════════════════ Wirkung ═════════════════════════════

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) return;
        affect(living);
    }

    /** Wirkung auf eine Kreatur im Gas. */
    protected abstract void affect(LivingEntity living);

    /** Original: {@code ArmorUtil.damageGasMaskFilter(entity, 1)}. */
    protected static void damageWornFilter(LivingEntity living) {
        ItemStack mask = GasMaskUtil.resolveWornMask(living);
        if (!mask.isEmpty() && mask.getItem() instanceof IGasMask) {
            IGasMask.damageFilter(mask, 1);
        }
    }
}
