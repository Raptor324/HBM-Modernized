package com.hbm_m.block;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Порт {@code BlockRedBrick} (1.7.10). В оригинале metadata задавала номер
 * грани, которая рисуется красной текстурой ({@code brick_red}), остальные
 * грани — серые ({@code brick_base}); meta 6 делала все грани серыми.
 * Здесь то же самое выражено свойством {@link #RED_FACE}.
 */
public class RedBrickBlock extends Block {

    public enum RedFace implements StringRepresentable {
        NONE("none"),
        DOWN("down"),
        UP("up"),
        NORTH("north"),
        SOUTH("south"),
        WEST("west"),
        EAST("east");

        public static final Codec<RedFace> CODEC = StringRepresentable.fromEnum(RedFace::values);
        private final String name;

        RedFace(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<RedFace> RED_FACE = EnumProperty.create("red_face", RedFace.class);

    public RedBrickBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RED_FACE, RedFace.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RED_FACE);
    }

    /**
     * audit10: Original {@code onBlockPlacedBy} -> {@code BlockPistonBase.determineOrientation}: steht der Spieler
     * nah (unter 2 Bloecke waagrecht), zeigt die rote Seite nach oben/unten, sonst zum Spieler hin.
     */
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        net.minecraft.world.entity.player.Player player = ctx.getPlayer();
        if (player == null) return defaultBlockState();
        net.minecraft.core.BlockPos pos = ctx.getClickedPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (Math.abs((float) player.getX() - x) < 2.0F && Math.abs((float) player.getZ() - z) < 2.0F) {
            double d0 = player.getY() + 1.82D; // Server: posY = Fuesse, yOffset 0
            if (d0 - y > 2.0D) return defaultBlockState().setValue(RED_FACE, RedFace.UP);
            if (y - d0 > 0.0D) return defaultBlockState().setValue(RED_FACE, RedFace.DOWN);
        }

        int l = net.minecraft.util.Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        RedFace face = l == 0 ? RedFace.NORTH : (l == 1 ? RedFace.EAST : (l == 2 ? RedFace.SOUTH : RedFace.WEST));
        return defaultBlockState().setValue(RED_FACE, face);
    }
}
