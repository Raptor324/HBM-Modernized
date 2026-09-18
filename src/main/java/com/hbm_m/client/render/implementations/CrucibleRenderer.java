package com.hbm_m.client.render.implementations;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.client.model.ConfiguredMultipartBakedModel;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;

import com.hbm_m.platform.RenderHooks;

/**
 * Тигель на фабрике {@link MachineRenderers} — порт {@code RenderCrucible} (1.7.10):
 * корпус (Main) запечён в chunk-mesh; поверхность расплава — динамическая часть
 * из OBJ-части "Lava", поднятой на уровень заполнения
 * ({@code 0.5 + fill * 0.875} в оригинале) и перетекстурированной лавой
 * ({@code hbm_m:block/machine/lava} — копия {@code lava.png} оригинала). VBO кешируется по квантованному уровню.
 */
public final class CrucibleRenderer {

    private static final RandomSource RANDOM = RandomSource.create(42);
    private static final ResourceLocation LAVA_SPRITE =
            ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "block/machine/lava");

    private CrucibleRenderer() {}

    public static void register() {
        MachineRenderers.machine("crucible", com.hbm_m.blockentity.ModBlockEntities.CRUCIBLE_BE.get(),
                MachineCrucibleBlockEntity.class)
            .part("Main") // empty_world_quads: корпус рисует BER
            .lightOverride("Melt", be -> be.getFillLevel() > 0 ? net.minecraft.client.renderer.LightTexture.pack(15, 15) : -1)
            .dynamicPart("Melt", CrucibleRenderer::meltQuads,
                    be -> String.valueOf((int) (be.getFillLevel() * 64)))
            .register();
    }

    /** Квады поверхности расплава: Lava-часть, поднята на уровень, с лавовым спрайтом. */
    private static List<BakedQuad> meltQuads(MachineCrucibleBlockEntity be) {
        if (be.getFillLevel() <= 0f) return List.of();
        BakedModel raw = Minecraft.getInstance().getBlockRenderer().getBlockModel(be.getBlockState());
        if (!(raw instanceof ConfiguredMultipartBakedModel model)) return List.of();

        BakedModel lavaPart = model.getPart("Lava");
        if (lavaPart == null) return List.of();

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            quads.addAll(RenderHooks.getModelQuads(lavaPart, null, dir, RANDOM, null));
        }
        quads.addAll(RenderHooks.getModelQuads(lavaPart, null, null, RANDOM, null));
        if (quads.isEmpty()) return List.of();

        TextureAtlasSprite lava = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LAVA_SPRITE);

        List<BakedQuad> result = new ArrayList<>(quads.size());
        float lift = be.getFillLevel() * 0.875F;
        for (BakedQuad quad : quads) {
            BakedQuad retex = retextureAndFixUV(quad, lava);
            result.addAll(com.hbm_m.client.model.ModelHelper.translateQuads(List.of(retex), 0f, lift, 0f));
        }
        return result;
    }

    /** Перенос UV квада со старого спрайта на лавовый (формат BLOCK: 8 int на вершину). */
    private static BakedQuad retextureAndFixUV(BakedQuad original, TextureAtlasSprite newSprite) {
        var oldSprite = original.getSprite();
        if (oldSprite == null) return original;

        float oldUDiff = oldSprite.getU1() - oldSprite.getU0();
        float oldVDiff = oldSprite.getV1() - oldSprite.getV0();
        float newUDiff = newSprite.getU1() - newSprite.getU0();
        float newVDiff = newSprite.getV1() - newSprite.getV0();
        if (oldUDiff == 0 || oldVDiff == 0 || newUDiff == 0 || newVDiff == 0) return original;

        int[] oldData = original.getVertices();
        int[] newData = new int[oldData.length];
        System.arraycopy(oldData, 0, newData, 0, oldData.length);

        int vertexSize = oldData.length / 4;
        for (int i = 0; i < 4; i++) {
            int offset = i * vertexSize;
            float oldU = Float.intBitsToFloat(oldData[offset + 4]);
            float oldV = Float.intBitsToFloat(oldData[offset + 5]);

            float normU = (oldU - oldSprite.getU0()) / oldUDiff;
            float normV = (oldV - oldSprite.getV0()) / oldVDiff;

            newData[offset + 4] = Float.floatToRawIntBits(newSprite.getU0() + normU * newUDiff);
            newData[offset + 5] = Float.floatToRawIntBits(newSprite.getV0() + normV * newVDiff);
        }

        return new BakedQuad(newData, original.getTintIndex(), original.getDirection(), newSprite, false);
    }
}
