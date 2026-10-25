package com.hbm_m.block.rail;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.util.ForgeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockDummyable} in dem Umfang, den die Schienen des Zugsystems brauchen: alle Zellen sind derselbe Block,
 * die Metadaten (hier {@link #META}) zeigen auf den Kern ({@code findCore}), Platzierung ueber Blickrichtung,
 * Waisen-Abbau ueber Nachbaraenderungen. Die Schienen arbeiten ueber {@code world.getBlock(..) instanceof IRailNTM}
 * auf jeder Zelle, deshalb nicht das Phantom-Block-System des Ports.
 *
 * <p>Metadaten: 0-5 Dummy-Richtung, 6-11 Extra, 12-15 Kernrichtung.</p>
 */
public abstract class RailDummyableBlock extends Block {

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    /** Metadaten-Versatz Dummy -> Kernrichtung */
    public static final int offset = 10;
    /** Metadaten-Versatz Dummy -> Extra */
    public static final int extra = 6;

    public static boolean safeRem = false;

    /** Original {@code bounding}: genaue Trefferboxen relativ zum Kern (Ausrichtung NORTH). */
    public List<AABB> bounding = new ArrayList<>();

    private static final VoxelShape RAIL_SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    protected RailDummyableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    // ---- Metadaten-Helfer (1.7.10 getBlockMetadata/setBlock) ----

    public static int getMeta(BlockGetter world, int x, int y, int z) {
        BlockState state = world.getBlockState(new BlockPos(x, y, z));
        return state.hasProperty(META) ? state.getValue(META) : 0;
    }

    public static Block getBlock(BlockGetter world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static boolean isReplaceable(BlockGetter world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).canBeReplaced();
    }

    public void setBlock(Level world, int x, int y, int z, int meta) {
        world.setBlock(new BlockPos(x, y, z), this.defaultBlockState().setValue(META, meta), 3);
    }

    public static void setBlockToAir(Level world, int x, int y, int z) {
        world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    // ---- BlockDummyable ----

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, world, pos, block, fromPos, moving);
        if (safeRem) return;
        destroyIfOrphan(world, pos.getX(), pos.getY(), pos.getZ());
    }

    private void destroyIfOrphan(Level world, int x, int y, int z) {
        if (world.isClientSide) return;

        int metadata = getMeta(world, x, y, z);

        // Extra-Markierung entfernen
        if (metadata >= extra) metadata -= extra;

        ForgeDirection dir = ForgeDirection.getOrientation(metadata).getOpposite();
        Block b = getBlock(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);

        // Schutz gegen Multibloecke an Chunkgrenzen, die beim Entladen faelschlich geloescht wuerden
        if (b != this && world.hasChunksAt(new BlockPos(x - 1, y - 1, z - 1), new BlockPos(x + 1, y + 1, z + 1))) {
            setBlockToAir(world, x, y, z);
        }
    }

    public int[] findCore(BlockGetter world, int x, int y, int z) {
        positions.clear();
        return findCoreRec(world, x, y, z);
    }

    private final List<BlockPos> positions = new ArrayList<>();

    public int[] findCoreRec(BlockGetter world, int x, int y, int z) {

        BlockPos pos = new BlockPos(x, y, z);

        int metadata = getMeta(world, x, y, z);

        // Extra-Markierung entfernen
        if (metadata >= extra) metadata -= extra;

        // gleicher Block mit Richtung UNKNOWN -> Kern
        if (getBlock(world, x, y, z) == this && ForgeDirection.getOrientation(metadata) == ForgeDirection.UNKNOWN)
            return new int[] { x, y, z };

        if (positions.contains(pos)) return null;

        ForgeDirection dir = ForgeDirection.getOrientation(metadata).getOpposite();

        Block b = getBlock(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);

        if (b != this) return null;

        positions.add(pos);

        return findCoreRec(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos placed, BlockState state, @Nullable LivingEntity player, ItemStack itemStack) {

        if (!(player instanceof Player pl)) return;

        int x = placed.getX();
        int y = placed.getY();
        int z = placed.getZ();

        safeRem = true;
        setBlockToAir(world, x, y, z);
        safeRem = false;

        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        int o = -getOffset();
        y += getHeightOffset();

        ForgeDirection dir = ForgeDirection.NORTH;

        if (i == 0) dir = ForgeDirection.getOrientation(2);
        if (i == 1) dir = ForgeDirection.getOrientation(5);
        if (i == 2) dir = ForgeDirection.getOrientation(3);
        if (i == 3) dir = ForgeDirection.getOrientation(4);

        dir = getDirModified(dir);

        if (!checkRequirement(world, x, y, z, dir, o)) {

            if (!pl.getAbilities().instabuild) {
                // Gegenstand zurueckgeben (er wird danach vom BlockItem verbraucht)
                Item item = this.asItem();
                if (itemStack.isEmpty() || itemStack.getItem() != item || itemStack.getCount() == itemStack.getMaxStackSize()) {
                    pl.getInventory().add(new ItemStack(this));
                } else {
                    itemStack.grow(1);
                }
            }

            return;
        }

        if (!world.isClientSide) {
            // getrennt, weil Multiblock-Drehung und Kern-Metadaten abweichen koennen
            int meta = getMetaForCore(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, pl, dir.ordinal() + offset);
            setBlock(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, meta);
            fillSpace(world, x, y, z, dir, o);
        }
    }

    protected int getMetaForCore(Level world, int x, int y, int z, Player player, int original) {
        return original;
    }

    protected ForgeDirection getDirModified(ForgeDirection dir) {
        return dir;
    }

    protected boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), x, y, z, dir);
    }

    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), this, dir);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        // Original breakBlock: nur bei echtem Blockwechsel
        if (!state.is(newState.getBlock())) {
            int i = state.getValue(META);
            if (i >= 12) {
            } else if (!safeRem) {

                if (i >= extra) i -= extra;

                ForgeDirection d = ForgeDirection.getOrientation(i);

                if (getBlock(world, pos.getX() - d.offsetX, pos.getY() - d.offsetY, pos.getZ() - d.offsetZ) == this)
                    setBlockToAir(world, pos.getX() - d.offsetX, pos.getY() - d.offsetY, pos.getZ() - d.offsetZ);
            }
        }

        super.onRemove(state, world, pos, newState, moving);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** Reihenfolge UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT */
    public abstract int[] getDimensions();

    public abstract int getOffset();

    public int getHeightOffset() {
        return 0;
    }

    public boolean useDetailedHitbox() {
        return !bounding.isEmpty();
    }

    /** Original {@code setBlockBounds(0, 0, 0, 1, 0.125, 1)} aller Schienen. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        if (useDetailedHitbox()) {
            VoxelShape detailed = detailedShape(world, pos);
            if (detailed != null) return detailed;
        }
        return RAIL_SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        if (useDetailedHitbox()) {
            VoxelShape detailed = detailedShape(world, pos);
            return detailed != null ? detailed : Shapes.empty();
        }
        return RAIL_SHAPE;
    }

    /** Original {@code addCollisionBoxesToList}/{@code collisionRayTrace}: Boxen am Kern, auf diese Zelle zugeschnitten. */
    private VoxelShape detailedShape(BlockGetter world, BlockPos pos) {
        int[] core = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());
        if (core == null) return null;

        ForgeDirection rot = ForgeDirection.getOrientation(getMeta(world, core[0], core[1], core[2]) - offset).getRotation(ForgeDirection.UP);
        AABB cell = new AABB(pos);
        VoxelShape shape = Shapes.empty();

        for (AABB aabb : this.bounding) {
            AABB boxlet = getAABBRotationOffset(aabb, core[0] + 0.5, core[1], core[2] + 0.5, rot);
            if (!boxlet.intersects(cell)) continue;
            AABB cut = boxlet.intersect(cell).move(-pos.getX(), -pos.getY(), -pos.getZ());
            shape = Shapes.or(shape, Shapes.create(cut));
        }

        return shape;
    }

    public static AABB getAABBRotationOffset(AABB aabb, double x, double y, double z, ForgeDirection dir) {

        AABB newBox = null;

        if (dir == ForgeDirection.NORTH) newBox = new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        if (dir == ForgeDirection.EAST) newBox = new AABB(-aabb.maxZ, aabb.minY, aabb.minX, -aabb.minZ, aabb.maxY, aabb.maxX);
        if (dir == ForgeDirection.SOUTH) newBox = new AABB(-aabb.maxX, aabb.minY, -aabb.maxZ, -aabb.minX, aabb.maxY, -aabb.minZ);
        if (dir == ForgeDirection.WEST) newBox = new AABB(aabb.minZ, aabb.minY, -aabb.maxX, aabb.maxZ, aabb.maxY, -aabb.minX);

        if (newBox != null) {
            return newBox.move(x, y, z);
        }

        return new AABB(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ).move(x + 0.5, y + 0.5, z + 0.5);
    }

    // ---- MultiblockHandlerXR (1:1) ----

    public static boolean checkSpace(Level world, int x, int y, int z, int[] dim, int ox, int oy, int oz, ForgeDirection dir) {

        if (dim == null || dim.length != 6) return false;

        int count = 0;

        int[] rot = rotate(dim, dir);

        for (int a = x - rot[4]; a <= x + rot[5]; a++) {
            for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                for (int c = z - rot[2]; c <= z + rot[3]; c++) {

                    // die gerade gesetzte Position zaehlt als frei
                    if (a == ox && b == oy && c == oz) continue;

                    if (!isReplaceable(world, a, b, c)) return false;

                    count++;

                    if (count > 2000) return false;
                }
            }
        }

        return true;
    }

    public static void fillSpace(Level world, int x, int y, int z, int[] dim, RailDummyableBlock block, ForgeDirection dir) {

        if (dim == null || dim.length != 6) return;

        int count = 0;

        int[] rot = rotate(dim, dir);

        safeRem = true;

        for (int a = x - rot[4]; a <= x + rot[5]; a++) {
            for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                for (int c = z - rot[2]; c <= z + rot[3]; c++) {

                    int meta;

                    if (b < y) {
                        meta = ForgeDirection.DOWN.ordinal();
                    } else if (b > y) {
                        meta = ForgeDirection.UP.ordinal();
                    } else if (a < x) {
                        meta = ForgeDirection.WEST.ordinal();
                    } else if (a > x) {
                        meta = ForgeDirection.EAST.ordinal();
                    } else if (c < z) {
                        meta = ForgeDirection.NORTH.ordinal();
                    } else if (c > z) {
                        meta = ForgeDirection.SOUTH.ordinal();
                    } else {
                        continue;
                    }

                    block.setBlock(world, a, b, c, meta);

                    count++;

                    if (count > 2000) {
                        safeRem = false;
                        return;
                    }
                }
            }
        }

        safeRem = false;
    }

    public static int[] rotate(int[] dim, ForgeDirection dir) {

        if (dim == null) return null;
        if (dir == ForgeDirection.SOUTH) return dim;

        if (dir == ForgeDirection.NORTH) {
            return new int[] { dim[0], dim[1], dim[3], dim[2], dim[5], dim[4] };
        }

        if (dir == ForgeDirection.EAST) {
            return new int[] { dim[0], dim[1], dim[5], dim[4], dim[2], dim[3] };
        }

        if (dir == ForgeDirection.WEST) {
            return new int[] { dim[0], dim[1], dim[4], dim[5], dim[3], dim[2] };
        }

        return dim;
    }
}
