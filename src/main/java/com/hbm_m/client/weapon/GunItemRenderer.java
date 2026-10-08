package com.hbm_m.client.weapon;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.client.weapon.ItemRenderWeaponBase.ItemRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Bruecke von 1.20-Itemdarstellung zu den 1.7.10-{@link ItemRenderWeaponBase}-Renderern (Original
 * {@code MinecraftForgeClient.registerItemRenderer} in {@code GunFactoryClient}). Die Item-Modelle sind
 * {@code builtin/entity} mit Einheits-Anzeigetransformationen; die Original-Transformationen (Hand, Inventar,
 * Boden) werden hier nachgestellt. Erstperson zeichnet {@link GunClientHooks} ueber das RenderHand-Ereignis.
 */
public class GunItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static final GunItemRenderer INSTANCE = new GunItemRenderer();

    /** Original {@code MinecraftForgeClient.registerItemRenderer(item, renderer)}. */
    public static final Map<Item, ItemRenderWeaponBase> RENDERERS = new HashMap<>();

    public static void register(Item item, ItemRenderWeaponBase renderer) {
        RENDERERS.put(item, renderer);
    }

    public static ItemRenderWeaponBase get(ItemStack stack) {
        return stack.isEmpty() ? null : RENDERERS.get(stack.getItem());
    }

    private GunItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        ItemRenderWeaponBase renderer = get(stack);
        if (renderer == null) return;
        boolean firstPerson = ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        if (firstPerson && renderer.customFirstPerson()) return;

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5); // ItemRenderer verschiebt vor dem BEWLR um -0.5

        GunGL.begin(ps, buffers, light, overlay);
        //? if < 1.21.1 {
        ItemRenderWeaponBase.interp = Minecraft.getInstance().getFrameTime();
        //?} else {
        /*ItemRenderWeaponBase.interp = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        *///?}

        switch (ctx) {
            case GUI -> {
                // 1.7.10-Inventar: Pixelraum 0..16 mit Y nach unten
                ps.translate(-0.5, 0.5, 0);
                ps.scale(1F / 16F, -1F / 16F, 1F / 16F);
                renderer.renderItem(ItemRenderType.INVENTORY, stack);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                // zurueck in den 1.7.10-Armraum: ItemInHandLayer dreht -90 X, 180 Y und verschiebt (1/16, 1/8, -10/16)
                boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                ps.translate((left ? 1 : -1) / 16F, -0.125F, 0.625F);
                ps.mulPose(Axis.YP.rotationDegrees(-180));
                ps.mulPose(Axis.XP.rotationDegrees(90));
                // RenderPlayer.renderEquippedItems (1.7.10) fuer nicht-3D-Gegenstaende mit eigenem Renderer
                ps.translate(-0.0625F, 0.4375F, 0.0625F);
                ps.translate(0.25F, 0.1875F, -0.1875F);
                ps.scale(0.375F, 0.375F, 0.375F);
                ps.mulPose(Axis.ZP.rotationDegrees(60));
                ps.mulPose(Axis.XP.rotationDegrees(-90));
                ps.mulPose(Axis.ZP.rotationDegrees(20));
                if (left) {
                    var entity = Minecraft.getInstance().player;
                    GunGL.pushMatrix();
                    renderer.setupThirdPersonAkimbo(stack);
                    renderer.renderEquippedAkimbo(stack, entity);
                    GunGL.popMatrix();
                } else {
                    renderer.renderItem(ItemRenderType.EQUIPPED, stack, null, Minecraft.getInstance().player);
                }
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // 1.7.10 ItemRenderer.renderItemInFirstPerson fuer IItemRenderer ohne Render-Helfer
                ps.mulPose(Axis.YP.rotationDegrees(-90));
                renderer.renderItem(ItemRenderType.EQUIPPED_FIRST_PERSON, stack, null, Minecraft.getInstance().player);
            }
            case GROUND, FIXED, HEAD, NONE -> {
                ps.scale(0.5F, 0.5F, 0.5F);
                renderer.renderItem(ItemRenderType.ENTITY, stack);
            }
            default -> { }
        }

        GunGL.end();
        ps.popPose();
    }
}
