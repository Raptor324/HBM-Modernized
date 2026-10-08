package com.hbm_m.client.render.implementations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.hbm_m.blockentity.decorations.DecoLootBlockEntity;
import com.hbm_m.client.render.HbmBerBounds;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.WeaponItems;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.powerarmor.ArmorNCRPA;
import com.hbm_m.powerarmor.ArmorTrenchmaster;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
/**
 * 1:1 {@code RenderLoot}: jeder Gegenstand der Lootkiste an seiner gespeicherten Stelle (ab Blockecke). Sonderfaelle
 * wie im Original: Mini-Nuke-Munition (NUKE_STANDARD..NUKE_HIVE) als Mini-Nuke-Modell, die Maresleg als Waffenmodell,
 * Trenchmaster-/NCR-Powerruestung als Ruestungsmodell auf unsichtbarer Figur; alles andere flach liegend
 * ({@code renderItemIn2D}). GL-Zustaende: Mischung -> translucent, Lichtkarte 240 -> volle Helligkeit.
 */
public class DecoLootRenderer implements HbmBerBounds<DecoLootBlockEntity> {

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path); }

    public static final ResourceLocation MINI_NUKE_TEX = rl("textures/models/projectiles/mini_nuke.png");

    public static final com.hbm_m.client.render.SimpleObjModel ARMOR_TRENCHMASTER = new com.hbm_m.client.render.SimpleObjModel(rl("models/armor/trenchmaster.obj"));
    public static final ResourceLocation TRENCHMASTER_HELMET = rl("textures/armor/trenchmaster_helmet.png");
    public static final ResourceLocation TRENCHMASTER_CHEST = rl("textures/armor/trenchmaster_chest.png");
    public static final ResourceLocation TRENCHMASTER_ARM = rl("textures/armor/trenchmaster_arm.png");
    public static final ResourceLocation TRENCHMASTER_LEG = rl("textures/armor/trenchmaster_leg.png");

    public static final ResourceLocation NCRPA_HELMET = rl("textures/armor/ncrpa_helmet.png");
    public static final ResourceLocation NCRPA_CHEST = rl("textures/armor/ncrpa_chest.png");
    public static final ResourceLocation NCRPA_ARM = rl("textures/armor/ncrpa_arm.png");
    public static final ResourceLocation NCRPA_LEG = rl("textures/armor/ncrpa_leg.png");

    public DecoLootRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(DecoLootBlockEntity loot, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        for (DecoLootBlockEntity.LootEntry entry : loot.getItems()) {
            ItemStack stack = entry.stack();
            if (stack.isEmpty()) continue;

            pose.pushPose();
            pose.translate(entry.dx(), entry.dy(), entry.dz());

            if (isMiniNuke(stack)) {
                renderNuke(pose, buffers, light);
            } else if (stack.getItem() == WeaponItems.gun("gun_maresleg")) {
                renderShotgun(pose, buffers, light);
            } else if (stack.getItem() instanceof ArmorTrenchmaster) {
                renderTrenchmaster(stack, pose, buffers, light);
            } else if (stack.getItem() instanceof ArmorNCRPA) {
                renderNCR(stack, pose, buffers, light);
            } else {
                renderStandardItem(stack, pose, buffers, light);
            }

            pose.popPose();
        }
    }

    /** Original: ammo_standard mit Meta zwischen NUKE_STANDARD und NUKE_HIVE (je Meta eine eigene ID im Port). */
    private static boolean isMiniNuke(ItemStack stack) {
        for (int i = EnumAmmo.NUKE_STANDARD.ordinal(); i <= EnumAmmo.NUKE_HIVE.ordinal(); i++) {
            if (stack.getItem() == WeaponItems.ammo(EnumAmmo.values()[i])) return true;
        }
        return false;
    }

    private static void armorBase(PoseStack pose) {
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(0.0625F, 0.0625F, 0.0625F);
        pose.mulPose(Axis.XP.rotationDegrees(180));
    }

    private static void renderTrenchmaster(ItemStack stack, PoseStack pose, MultiBufferSource buf, int light) {
        pose.pushPose();
        armorBase(pose);
        Item item = stack.getItem();
        if (item == ModItems.TRENCHMASTER_HELMET.get()) {
            ARMOR_TRENCHMASTER.renderPart("Helmet", pose, buf.getBuffer(RenderType.entityTranslucent(TRENCHMASTER_HELMET)), light);
            ARMOR_TRENCHMASTER.renderPart("Light", pose, buf.getBuffer(RenderType.entityCutout(TRENCHMASTER_HELMET)), TrinketRenderer.FULLBRIGHT);
        }
        if (item == ModItems.TRENCHMASTER_PLATE.get()) {
            ARMOR_TRENCHMASTER.renderPart("Chest", pose, buf.getBuffer(RenderType.entityCutout(TRENCHMASTER_CHEST)), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-3));
            var vc = buf.getBuffer(RenderType.entityCutout(TRENCHMASTER_ARM));
            ARMOR_TRENCHMASTER.renderPart("LeftArm", pose, vc, light);
            ARMOR_TRENCHMASTER.renderPart("RightArm", pose, vc, light);
            pose.popPose();
        }
        if (item == ModItems.TRENCHMASTER_LEGS.get()) {
            var vc = buf.getBuffer(RenderType.entityCutout(TRENCHMASTER_LEG));
            ARMOR_TRENCHMASTER.renderPart("LeftLeg", pose, vc, light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            ARMOR_TRENCHMASTER.renderPart("RightLeg", pose, vc, light);
            pose.popPose();
        }
        if (item == ModItems.TRENCHMASTER_BOOTS.get()) {
            var vc = buf.getBuffer(RenderType.entityCutout(TRENCHMASTER_LEG));
            ARMOR_TRENCHMASTER.renderPart("LeftBoot", pose, vc, light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            ARMOR_TRENCHMASTER.renderPart("RightBoot", pose, vc, light);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void renderNCR(ItemStack stack, PoseStack pose, MultiBufferSource buf, int light) {
        pose.pushPose();
        armorBase(pose);
        Item item = stack.getItem();
        if (item == ModItems.NCRPA_HELMET.get()) {
            WeaponResources.armor_ncr.renderPart("Helmet", pose, buf.getBuffer(RenderType.entityTranslucent(NCRPA_HELMET)), light);
            WeaponResources.armor_ncr.renderPart("Eyes", pose, buf.getBuffer(RenderType.entityCutout(NCRPA_HELMET)), TrinketRenderer.FULLBRIGHT);
        }
        if (item == ModItems.NCRPA_PLATE.get()) {
            WeaponResources.armor_ncr.renderPart("Chest", pose, buf.getBuffer(RenderType.entityCutout(NCRPA_CHEST)), light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-3));
            var vc = buf.getBuffer(RenderType.entityCutout(NCRPA_ARM));
            WeaponResources.armor_ncr.renderPart("LeftArm", pose, vc, light);
            WeaponResources.armor_ncr.renderPart("RightArm", pose, vc, light);
            pose.popPose();
        }
        if (item == ModItems.NCRPA_LEGS.get()) {
            var vc = buf.getBuffer(RenderType.entityCutout(NCRPA_LEG));
            WeaponResources.armor_ncr.renderPart("LeftLeg", pose, vc, light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            WeaponResources.armor_ncr.renderPart("RightLeg", pose, vc, light);
            pose.popPose();
        }
        if (item == ModItems.NCRPA_BOOTS.get()) {
            var vc = buf.getBuffer(RenderType.entityCutout(NCRPA_LEG));
            WeaponResources.armor_ncr.renderPart("LeftBoot", pose, vc, light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-0.1F));
            WeaponResources.armor_ncr.renderPart("RightBoot", pose, vc, light);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void renderNuke(PoseStack pose, MultiBufferSource buf, int light) {
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(1, 0.5, 1);
        WeaponResources.projectiles.renderPart("MiniNuke", pose, buf.getBuffer(RenderType.entityCutout(MINI_NUKE_TEX)), light);
    }

    private static void renderShotgun(PoseStack pose, MultiBufferSource buf, int light) {
        pose.scale(0.125F, 0.125F, 0.125F);
        pose.translate(3, 0, 0);
        pose.mulPose(Axis.YP.rotationDegrees(25));
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.mulPose(Axis.YP.rotationDegrees(90));
        WeaponResources.maresleg.renderAll(pose, buf.getBuffer(RenderType.entityCutout(WeaponResources.maresleg_tex)), light);
    }

    private static void renderStandardItem(ItemStack stack, PoseStack pose, MultiBufferSource buf, int light) {
        pose.translate(0.25, 0, 0.25);
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        // renderItemIn2D mit allen Renderdurchgaengen und Faerbung: flaches Itemmodell (0,0)-(1,1), 1/16 dick
        TrinketRenderer.renderItem2D(pose, buf, stack, light);
    }
}
