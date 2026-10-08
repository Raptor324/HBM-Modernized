package com.hbm_m.blockentity.generic;

import java.util.List;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.generic.BlockWandTandem;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.JigsawPool;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.NBTStructure.JigsawConnection;
import com.hbm_m.world.gen.nbt.SpawnCondition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockWandTandem.TileEntityWandTandem}: verzoegerter Jigsaw-Anschluss. Beim Strukturaufbau "scharf"
 * geschaltet ({@link #arm}), setzt er im naechsten Tick ein zufaelliges Teil aus dem Zielpool an seine Vorderseite
 * und ersetzt sich dann durch den Ersatzblock.
 */
public class WandTandemBlockEntity extends BaseHbmBlockEntity implements IControlReceiver {

    public static boolean copyMode = false;

    public String pool = "default";
    public String target = "default";
    public BlockState replaceBlock = Blocks.AIR.defaultBlockState();
    public boolean isRollable = true; // Gelenkart: rollbare Gelenke duerfen bei senkrechten Anschluessen beliebig gedreht werden

    private boolean isArmed = false;
    private SpawnCondition structure;

    public WandTandemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAND_TANDEM.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WandTandemBlockEntity be) {
        if (!level.isClientSide) {
            be.tryGenerate();
        }
    }

    /** Original {@code networkPackNT}. */
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void tryGenerate() {
        if (!this.isArmed || target == null || target.isEmpty() || pool == null || pool.isEmpty() || structure == null) return;

        JigsawPool pool = structure.getPool(this.pool);
        if (pool == null) return;

        JigsawPiece nextPiece = pool.get(new java.util.Random(level.random.nextLong()));
        if (nextPiece == null) return;

        ForgeDirection dir = ForgeDirection.getOrientation(getDirection());

        List<JigsawConnection> connectionPool = nextPiece.structure.getConnectionPool(dir, target);
        if (connectionPool == null) return;

        JigsawConnection toConnection = connectionPool.get(level.random.nextInt(connectionPool.size()));
        int nextCoordBase = directionOffsetToCoordBase(dir.getOpposite(), toConnection.dir);

        BlockPos pos = new BlockPos(worldPosition.getX() + dir.offsetX, worldPosition.getY() + dir.offsetY, worldPosition.getZ() + dir.offsetZ);

        // Startpunkt auf den Anschlusspunkt verschieben
        int ox = nextPiece.structure.rotateX(toConnection.pos[0], toConnection.pos[2], nextCoordBase);
        int oy = toConnection.pos[1];
        int oz = nextPiece.structure.rotateZ(toConnection.pos[0], toConnection.pos[2], nextCoordBase);

        nextPiece.structure.build(level, nextPiece, pos.getX() - ox, pos.getY() - oy, pos.getZ() - oz, nextCoordBase, structure.name);

        level.setBlock(worldPosition, replaceBlock, 2);
    }

    private int directionOffsetToCoordBase(ForgeDirection from, ForgeDirection to) {
        for (int i = 0; i < 4; i++) {
            if (from == to) return i % 4;
            from = from.getRotation(ForgeDirection.DOWN);
        }
        return 0;
    }

    public int getDirection() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockWandTandem.FACING)) return ForgeDirection.of(state.getValue(BlockWandTandem.FACING)).ordinal();
        return 2;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        if (!copyMode) {
            nbt.putInt("direction", getDirection());
            if (isArmed && structure != null) {
                nbt.putBoolean("isArmed", isArmed);
                nbt.putString("structure", structure.name);
            }
        }

        nbt.putString("pool", pool);
        nbt.putString("target", target);
        nbt.put("block", NbtUtils.writeBlockState(replaceBlock));
        nbt.putBoolean("roll", isRollable);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, HolderLookup.Provider registries) {
        if (!copyMode) {
            isArmed = nbt.getBoolean("isArmed");
            structure = nbt.contains("structure") ? NBTStructure.getStructure(nbt.getString("structure")) : null;
        }

        pool = nbt.getString("pool");
        target = nbt.getString("target");
        replaceBlock = nbt.contains("block") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), nbt.getCompound("block")) : Blocks.AIR.defaultBlockState();
        isRollable = nbt.getBoolean("roll");
    }

    /** Fuer die Papier-Kopie (Original copyMode). */
    public CompoundTag writeCopy() {
        copyMode = true;
        CompoundTag tag = new CompoundTag();
        writeNbtData(tag, null);
        copyMode = false;
        return tag;
    }

    public void readCopy(CompoundTag tag) {
        copyMode = true;
        readNbtData(tag, null);
        copyMode = false;
        sync();
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag nbt) {
        readNbtData(nbt, null);
        sync();
    }

    public void arm(SpawnCondition structure) {
        isArmed = true;
        this.structure = structure;
        setChanged();
    }
}
