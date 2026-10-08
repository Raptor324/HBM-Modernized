package com.hbm_m.client.render.implementations;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.StructLauncherCoreBlock;
import com.hbm_m.blockentity.machines.CompactLauncherBlockEntity;
import com.hbm_m.blockentity.machines.LaunchTableBlockEntity;
import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.util.MissilePronter;
import com.hbm_m.item.missile.ItemCustomMissilePart.PartSize;
import com.hbm_m.item.missile.MissileStruct;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * 1:1 {@code RenderCompactLauncher}, {@code RenderLaunchTable} und {@code RenderMultiblock} (Bauplanvorschau der
 * beiden Strukturkerne).
 */
public final class CustomLauncherRenderers {

    private CustomLauncherRenderers() { }

    private static final Map<String, SimpleObjModel> MODELS = new HashMap<>();

    private static void draw(String model, String texture, PoseStack ps, MultiBufferSource buffers, int light) {
        SimpleObjModel m = MODELS.computeIfAbsent(model, p -> new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/launch_table/" + p + ".obj")));
        m.renderAll(ps, buffers.getBuffer(RenderType.entityCutout(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/" + texture + ".png"))), light);
    }

    private static MissileStruct orEmpty(MissileStruct s) {
        return s != null ? s : new MissileStruct();
    }

    /** {@code RenderCompactLauncher}: Gestell, Rakete auf 1.0625. */
    public static class Compact implements com.hbm_m.client.render.HbmBerBounds<CompactLauncherBlockEntity> {

        public Compact(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(CompactLauncherBlockEntity te, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            draw("compact_launcher", "compact_launcher", ps, buffers, light);

            ps.translate(0, 1.0625, 0);
            MissilePronter.prontMissile(orEmpty(te.getLoad()), ps, buffers, light);
            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(CompactLauncherBlockEntity te) { return true; }
        @Override public int getViewDistance() { return 256; }
    }

    /** {@code RenderLaunchTable}: Tisch, Rampe nach Groesse, Geruestturm bis zur Rakete, die Rakete auf 2.0625. */
    public static class Table implements com.hbm_m.client.render.HbmBerBounds<LaunchTableBlockEntity> {

        public Table(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(LaunchTableBlockEntity launcher, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);

            switch (launcher.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
                case NORTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
                default -> { }
            }

            draw("launch_table_base", "missile_parts/launch_table", ps, buffers, light);

            if (launcher.padSize == PartSize.SIZE_10 || launcher.padSize == PartSize.SIZE_15) {
                draw("launch_table_small_pad", "missile_parts/launch_table_small_pad", ps, buffers, light);
            }
            if (launcher.padSize == PartSize.SIZE_20) {
                draw("launch_table_large_pad", "missile_parts/launch_table_large_pad", ps, buffers, light);
            }

            MissileStruct load = orEmpty(launcher.getLoad());

            ps.pushPose();

            if (load.fuselage != null) launcher.height = (int) MissilePronter.guiHeight(load);

            int height = (int) (launcher.height * 0.75);
            String base = "missile_parts/launch_table_large_scaffold_base";
            String connector = "missile_parts/launch_table_large_scaffold_connector";
            String baseM = "launch_table_large_scaffold_base";
            String connectorM = "launch_table_large_scaffold_connector";
            String emptyM = "launch_table_large_scaffold_empty";

            if (launcher.padSize == PartSize.SIZE_10) {
                base = "missile_parts/launch_table_small_scaffold_base";
                connector = "missile_parts/launch_table_small_scaffold_connector";
                baseM = "launch_table_small_scaffold_base";
                connectorM = "launch_table_small_scaffold_connector";
                emptyM = "launch_table_small_scaffold_empty";
                ps.translate(0, 0, -1);
            }
            ps.translate(0, 1, 3.5);
            for (int i = 0; i < launcher.height + 1; i++) {
                if (i < height) {
                    draw(baseM, base, ps, buffers, light);
                } else if (i > height) {
                    draw(emptyM, base, ps, buffers, light);
                } else {
                    if (load.fuselage != null && load.fuselage.top == launcher.padSize) {
                        draw(connectorM, connector, ps, buffers, light);
                    } else {
                        draw(baseM, base, ps, buffers, light);
                    }
                }
                ps.translate(0, 1, 0);
            }
            ps.popPose();

            ps.translate(0, 2.0625, 0);

            if (load.fuselage != null && load.fuselage.top == launcher.padSize) {
                MissilePronter.prontMissile(load, ps, buffers, light);
            }

            ps.popPose();
        }

        @Override public boolean shouldRenderOffScreen(LaunchTableBlockEntity te) { return true; }
        @Override public int getViewDistance() { return 256; }
    }

    /**
     * {@code RenderMultiblock}: Bauplan als durchscheinende Miniwuerfel - der Ring aus Startrampenbloecken, beim
     * grossen Kern dazu die Geruestsaeule, die jede Sekunde reihum auf eine andere Seite springt.
     */
    public static class Struct implements com.hbm_m.client.render.HbmBerBounds<StructLauncherCoreBlock.StructBE> {

        public Struct(BlockEntityRendererProvider.Context ctx) { }

        @Override
        public void render(StructLauncherCoreBlock.StructBE be, float partialTick, PoseStack ps, MultiBufferSource buffer, int light, int overlay) {
            Matrix4f m = ps.last().pose();
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS));
            TextureAtlasSprite launcher = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/struct_launcher");

            int r = be.isLarge() ? 4 : 1;
            for (int i = -r; i <= r; i++)
                for (int j = -r; j <= r; j++)
                    if (i != 0 || j != 0) StructSoyuzCoreRenderer.cube(vc, m, launcher, i, 0, j, light, overlay);

            if (be.isLarge()) {
                TextureAtlasSprite scaffold = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/struct_scaffold");
                int[][] sides = { { 3, 0 }, { 0, 3 }, { -3, 0 }, { 0, -3 } };
                int[] side = sides[(int) (System.currentTimeMillis() % 4000 / 1000)];
                for (int k = 1; k < 12; k++) StructSoyuzCoreRenderer.cube(vc, m, scaffold, side[0], k, side[1], light, overlay);
            }
        }

        @Override public int getViewDistance() { return 256; }
        @Override public boolean shouldRenderOffScreen(StructLauncherCoreBlock.StructBE be) { return true; }
    }
}
