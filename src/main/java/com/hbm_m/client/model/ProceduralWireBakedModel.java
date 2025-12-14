package com.hbm_m.client.model;

// Модель процедурного провода, которая генерирует геометрию на основе состояния блока.
// Поддерживает соединения в 6 направлениях и корректно ориентирует текстуры на всех гранях.
// Логика текстурирования унифицирована для центрального куба и "рукавов" провода.
import com.hbm_m.api.energy.WireBlock;
import com.hbm_m.client.model.ModelHelper.UVBox;
import com.hbm_m.client.model.ModelHelper.UVSpec;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public class ProceduralWireBakedModel extends AbstractProceduralBakedModel {
    
    private static final UVSpec BODY_SPEC = new UVSpec(new UVBox(2, 0, 7, 5));
    private static final UVSpec BODY_ROTATED_SPEC = new UVSpec(new UVBox(2, 0, 7, 5), true);
    private static final UVSpec END_CONTACT_SPEC = new UVSpec(new UVBox(7, 13, 10, 16));
    private static final UVSpec CORNER_CONTACT_SPEC = new UVSpec(new UVBox(4, 11, 9, 16));

    public ProceduralWireBakedModel(TextureAtlasSprite sprite) {
        super(sprite);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, 
                                              @NotNull RandomSource rand, @NotNull ModelData data, 
                                              @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();
        float min = 5.5f, max = 10.5f;

        if (state == null) {
            ModelHelper.createCuboid(quads, new Vector3f(min, min, min), new Vector3f(max, max, max), sprite, Map.of(
                Direction.UP, CORNER_CONTACT_SPEC, Direction.DOWN, CORNER_CONTACT_SPEC,
                Direction.NORTH, BODY_SPEC, Direction.SOUTH, BODY_SPEC,
                Direction.WEST, BODY_SPEC, Direction.EAST, BODY_SPEC
            ), Collections.emptySet());
            return quads;
        }

        // 1. Анализ соединений
        Set<Direction> connections = new HashSet<>();
        for (Direction dir : Direction.values()) {
            if (state.getValue(WireBlock.getProperty(dir))) {
                connections.add(dir);
            }
        }

        boolean isStraight = connections.size() == 2 && connections.stream().anyMatch(d -> connections.contains(d.getOpposite()));
        boolean showCornerContacts = connections.size() <= 1 || !isStraight;

        // Определяем главное направление провода, если он прямой
        Direction wireDirection = null;
        if (isStraight) {
            // Берем любое из соединений, чтобы узнать ось провода
            wireDirection = connections.iterator().next();
        }

        // 2. Рендер центрального куба
        Map<Direction, UVSpec> coreUvMap = new HashMap<>();
        for (Direction faceDir : Direction.values()) {
            // Рендерим грань, только если к ней нет подключения
            if (!connections.contains(faceDir)) {
                if (showCornerContacts) {
                    coreUvMap.put(faceDir, CORNER_CONTACT_SPEC);
                } else {
                    coreUvMap.put(faceDir, getBodyTextureForArmFace(wireDirection, faceDir));
                }
            }
        }

        ModelHelper.createCuboid(quads, new Vector3f(min, min, min), new Vector3f(max, max, max), sprite, coreUvMap, Collections.emptySet());


        // 3. Рендер рукавов
        for (Direction armDir : connections) {
            Map<Direction, UVSpec> armUvMap = new HashMap<>();
            armUvMap.put(armDir, END_CONTACT_SPEC);

            for (Direction faceDir : Direction.values()) {
                if (faceDir.getAxis() != armDir.getAxis()) {
                    armUvMap.put(faceDir, getBodyTextureForArmFace(armDir, faceDir));
                }
            }

            Vector3f from = new Vector3f(min, min, min);
            Vector3f to = new Vector3f(max, max, max);
            boolean isNegative = armDir.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
            from.setComponent(armDir.getAxis().ordinal(), isNegative ? 0 : max);
            to.setComponent(armDir.getAxis().ordinal(), isNegative ? min : 16);
            
            ModelHelper.createCuboid(quads, from, to, sprite, armUvMap, Set.of(armDir.getOpposite()));
        }
        return quads;
    }

    private UVSpec getBodyTextureForArmFace(Direction armDir, Direction faceDir) {
        Direction.Axis armAxis = armDir.getAxis();
        Direction.Axis faceAxis = faceDir.getAxis();

        if (armAxis == Direction.Axis.Y) {
            return BODY_SPEC;
        }

        if (faceAxis == Direction.Axis.Y) {
            return armAxis == Direction.Axis.X ? BODY_ROTATED_SPEC : BODY_SPEC;
        } else {
            return BODY_ROTATED_SPEC;
        }
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, 
                                              @NotNull RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }
}