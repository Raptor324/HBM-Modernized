package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code FloodlightBeam.TileEntityFloodlightBeam}: Lichtpunkt eines Flutlichts; prueft alle 5 Ticks, ob die Quelle
 * noch existiert, an ist und diesen Punkt als Strahl {@link #index} fuehrt - sonst verschwindet er.
 */
public class FloodlightBeamBlockEntity extends BlockEntity {

    public FloodlightBlockEntity cache;
    public int sourceX;
    public int sourceY;
    public int sourceZ;
    public int index;

    public FloodlightBeamBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOODLIGHT_BEAM.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, FloodlightBeamBlockEntity be) {
        if (world.getGameTime() % 5 != 0) return;

        if (be.cache == null) {
            BlockPos src = new BlockPos(be.sourceX, be.sourceY, be.sourceZ);
            if (world.isLoaded(src)) {
                BlockEntity tile = world.getBlockEntity(src);
                if (tile instanceof FloodlightBlockEntity f) {
                    be.cache = f; // chunk is loaded, tile exists -> cache
                } else {
                    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2); // chunk is loaded, tile does not exist -> delete self
                    return;
                }
            }
        }

        if ((be.cache != null && (be.cache.isRemoved() || !be.cache.isOn || !pos.equals(be.cache.getLightPos(be.index)))) || be.sourceY == 0) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /** Original setSource(floodlight, x, y, z, i): im Original werden hier (faelschlich) die Strahl-Koordinaten gespeichert. */
    public void setSource(FloodlightBlockEntity floodlight, BlockPos source, int i) {
        cache = floodlight;
        sourceX = source.getX();
        sourceY = source.getY();
        sourceZ = source.getZ();
        index = i;
        setChanged();
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.sourceX = nbt.getInt("sourceX");
        this.sourceY = nbt.getInt("sourceY");
        this.sourceZ = nbt.getInt("sourceZ");
        this.index = nbt.getInt("index");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putInt("sourceX", sourceX);
        nbt.putInt("sourceY", sourceY);
        nbt.putInt("sourceZ", sourceZ);
        nbt.putInt("index", index);
    }
}
