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
}
