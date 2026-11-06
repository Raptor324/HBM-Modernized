package com.hbm_m.client.render.implementations;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.block.machines.MachineMassStorageBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineAshpitBlockEntity;
import com.hbm_m.blockentity.machines.MachineMassStorageBlockEntity;
import com.hbm_m.blockentity.machines.MachineRockMillBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Modern Nucleus machine renderers for miscellaneous metallurgical & processing installations:
 * - Ashpit: Combustion ash collection pit with sliding maintenance hatch door
 * - Rock Mill: Industrial ore pulverizing rock mill with rotating grinding wheel
 * - Mass Storage: High-density warehouse storage unit with LED item display and fill level gauge
 */
public final class MachineMetallurgyMiscRenderer {

    public static void register() {
        registerAshpit();
        registerRockMill();
        registerMassStorage();
    }

    private static void registerAshpit() {
        MachineRenderers.machine("ashpit", ModBlockEntities.ASHPIT_BE.get(),
                MachineAshpitBlockEntity.class)
            .part("Main")
            .translatingPart("Door", new Vector3f(0f, 0f, 1f), (be, pt) -> {
                float door = Mth.lerp(pt, be.prevDoorAngle, be.doorAngle);
                return (float) (door * 0.75 / 135.0);
            })
            .conditionalPart("InnerBurning", be -> be.isFull)
            .conditionalPart("Inner", be -> !be.isFull)
            .blockTransform(MachineMetallurgyMiscRenderer::applyAshpitTransform)
            .itemParts("Main", "Door")
            .register();
    }

    private static void registerRockMill() {
        MachineRenderers.machine("rockmill", ModBlockEntities.ROCKMILL_BE.get(),
                MachineRockMillBlockEntity.class)
            .part("Base")
            .conditionalPart("Frame", be -> be.frame)
            .rotateY("Wheel", 0f, 0f, (be, pt) -> -(Mth.lerp(pt, be.prevRotation, be.rotation)))
            .blockTransform(MachineMetallurgyMiscRenderer::applyRockMillTransform)
            .itemParts("Base", "Wheel")
            .register();
    }

    private static void registerMassStorage() {
        MachineRenderers.machine("mass_storage", ModBlockEntities.MACHINE_MASS_STORAGE_BE.get(),
                MachineMassStorageBlockEntity.class)
            .part("Body")
            .blockTransform((be, animator) -> animator.translate(0.5, 0.0, 0.5))
            .hook(new MachineRenderHook<MachineMassStorageBlockEntity>() {
                @Override
                public void render(MachineMassStorageBlockEntity storage, float partialTick, PoseStack ps,
                                   MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
                    renderMassStorageFront(storage, ps, buf);
                }

                @Override
                public boolean mayDrawContent(MachineMassStorageBlockEntity storage) {
                    return !storage.getInventory().getStackInSlot(MachineMassStorageBlockEntity.SLOT_FILTER).isEmpty();
                }
            })
            .register();
    }

    private static void applyAshpitTransform(MachineAshpitBlockEntity oven, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        switch (ObjBerHelper.facing(oven)) {
            case EAST -> animator.rotate(90, 0, 1, 0);
            case NORTH -> animator.rotate(180, 0, 1, 0);
            case WEST -> animator.rotate(270, 0, 1, 0);
            default -> {}
        }
        animator.rotate(-90, 0, 1, 0);
    }

    private static void applyRockMillTransform(MachineRockMillBlockEntity mill, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
        animator.rotate(90, 0, 1, 0);
        if (mill.getBlockState().hasProperty(DummyableMachineBlock.FACING)) {
            switch (mill.getBlockState().getValue(DummyableMachineBlock.FACING)) {
                case WEST -> animator.rotate(90, 0, 1, 0);
                case SOUTH -> animator.rotate(180, 0, 1, 0);
                case EAST -> animator.rotate(270, 0, 1, 0);
                default -> {}
            }
        }
    }

    private static void renderMassStorageFront(MachineMassStorageBlockEntity storage, PoseStack ps, MultiBufferSource buf) {
        ItemStack type = storage.getInventory().getStackInSlot(MachineMassStorageBlockEntity.SLOT_FILTER);
        if (type.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Direction facing = storage.getBlockState().hasProperty(MachineMassStorageBlock.FACING)
                ? storage.getBlockState().getValue(MachineMassStorageBlock.FACING) : Direction.NORTH;
        int dir = switch (facing) {
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
        int full = LightTexture.FULL_BRIGHT;

        ps.pushPose();
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
        switch (dir) {
            case 1 -> ps.mulPose(Axis.YP.rotationDegrees(180.0F));
            case 2 -> ps.mulPose(Axis.YP.rotationDegrees(-90.0F));
            case 3 -> ps.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {}
        }
        ps.translate(-0.5F, -0.5F, -0.5F);

        ps.translate(0.0, 0.0, -0.005F);
        ps.scale(1.0F / 16.0F, 1.0F / 16.0F, -0.0001F);

        // Filter item icon
        ps.pushPose();
        ps.translate(4.0F, 2.5F, 0.0F);
        ps.scale(0.5F, 0.5F, 1.0F);
        ps.translate(8.0F, 8.0F, 0.0F);
        ps.scale(16.0F, -16.0F, 16.0F);
        ItemStack one = type.copy();
        one.setCount(1);
        mc.getItemRenderer().renderStatic(one, ItemDisplayContext.GUI, full, OverlayTexture.NO_OVERLAY, ps, buf, storage.getLevel(), 0);
        ps.popPose();

        // Stockpile count string
        boolean unicode = mc.options.forceUnicodeFont().get();
        String text = getTextForCount(storage.getStockpile(), unicode);
        Font font = mc.font;
        int textX = 32 - font.width(text) / 2;
        int textY = 44;

        ps.pushPose();
        ps.scale(0.25F, 0.25F, 0.25F);
        font.drawInBatch(text, textX + 1, textY + 1, 0xFF003F00, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, full);
        ps.translate(0.0, 0.0, 1.0);
        font.drawInBatch(text, textX, textY, 0xFF00FF00, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, full);
        ps.popPose();

        // LED stockpile gauge bar
        double fraction = storage.getCapacity() > 0 ? (double) storage.getStockpile() / (double) storage.getCapacity() : 0.0;
        float r = (float) (1.0 - fraction), g = (float) fraction;
        float bMinX = 2.0F, bMaxX = (float) (2.0 + fraction * 12.0), bMinY = 13.5F, bMaxY = 14.0F;
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(RenderType.debugQuads());
        int ir = (int) (r * 255.0F), ig = (int) (g * 255.0F);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMinX, bMaxY, 0.0F, ir, ig, 0, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMaxX, bMaxY, 0.0F, ir, ig, 0, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMaxX, bMinY, 0.0F, ir, ig, 0, 255);
        com.hbm_m.platform.RenderHooks.vertexColor(vc, m, bMinX, bMinY, 0.0F, ir, ig, 0, 255);

        ps.popPose();
    }

    private static String getTextForCount(long count, boolean unicode) {
        if (count < 1_000L) return String.valueOf(count);
        if (count < 1_000_000L) return (count / 1_000L) + "k";
        if (count < 1_000_000_000L) return (count / 1_000_000L) + "M";
        return (count / 1_000_000_000L) + "G";
    }

    private MachineMetallurgyMiscRenderer() {}
}
