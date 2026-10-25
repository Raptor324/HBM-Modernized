package com.hbm_m.block.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * mine_ap mit der Tarnung aus {@code RenderLandmine}: ueberdacht (Hoehenkarte > y + 2) Stein, sonst
 * Schnee in Schneebiomen, Wueste bei Temperatur >= 1.5 ohne Niederschlag, ansonsten Gras. Das Original
 * waehlt die Textur jeden Frame im TESR; hier liegt sie im Blockzustand und wird bei Platzierung und
 * vom Mine-BlockEntity regelmaessig nachgefuehrt.
 */
public class LandmineAPBlock extends LandmineBlock {

    public enum Camo implements StringRepresentable {
        GRASS, DESERT, SNOW, STONE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Camo> CAMO = EnumProperty.create("camo", Camo.class);

    public LandmineAPBlock(Properties properties, double range, double height) {
        super(properties, range, height);
        this.registerDefaultState(this.defaultBlockState().setValue(CAMO, Camo.GRASS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CAMO);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(CAMO, camoAt(context.getLevel(), context.getClickedPos()));
    }

    /** Original-Bedingungen aus {@code RenderLandmine.renderTileEntityAt}. */
    public static Camo camoAt(LevelReader level, BlockPos pos) {
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) > pos.getY() + 2) return Camo.STONE;
        Holder<Biome> holder = level.getBiome(pos);
        Biome biome = holder.value();
        if (biome.coldEnoughToSnow(pos)) return Camo.SNOW;
        if (biome.getBaseTemperature() >= 1.5F && !biome.hasPrecipitation()) return Camo.DESERT;
        return Camo.GRASS;
    }

    /** Vom BlockEntity aufgerufen: Tarnung an die aktuelle Umgebung anpassen. */
    public static void updateCamo(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(CAMO)) return;
        Camo camo = camoAt(level, pos);
        if (state.getValue(CAMO) != camo) level.setBlock(pos, state.setValue(CAMO, camo), Block.UPDATE_CLIENTS);
    }
}
