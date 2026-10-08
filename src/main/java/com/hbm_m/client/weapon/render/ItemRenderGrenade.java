package com.hbm_m.client.weapon.render;

import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.ItemRenderWeaponBase;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.render.item.ItemRenderGrenade}: Baukastengranate aus Grundtextur + eingefaerbten Schichten. */
public class ItemRenderGrenade extends ItemRenderWeaponBase {

    @Override public boolean customFirstPerson() { return false; }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {

        GunGL.pushMatrix();
        GunGL.enableCull();

        if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            renderFirstPerson(stack);
        }

        if (type == ItemRenderType.INVENTORY) {
            GunGL.enableLighting();
            GunGL.translate(8, 8, 0);
            GunGL.scale(-1, -1, -1);
            GunGL.rotate(45, 0, 0, 1);
            GunGL.rotate(150, 0, 1, 0);
            GunGL.rotate(15, 1, 0, 0);
            renderGrenade(stack, type);
        }

        if (type == ItemRenderType.EQUIPPED) {
            GunGL.scale(0.125, 0.125, 0.125);
            GunGL.translate(3, 1, -0.5);
            renderGrenade(stack, type);
        }

        if (type == ItemRenderType.ENTITY) {
            GunGL.scale(0.125, 0.125, 0.125);
            renderGrenade(stack, type);
        }

        GunGL.popMatrix();
    }

    @Override
    public void renderFirstPerson(ItemStack stack) {
        EnumGrenadeShell shell = ItemGrenadeUniversal.getShell(stack);

        GunGL.scale(0.125, 0.125, 0.125);
        GunGL.translate(3, 1, -3);
        GunGL.rotate(180, 0, -1, 0);

        double[] bodyMove = HbmAnimations.getRelevantTransformation("BODYMOVE");
        double[] bodyTurn = HbmAnimations.getRelevantTransformation("BODYTURN");
        double[] ringMove = HbmAnimations.getRelevantTransformation("RINGMOVE");
        double[] ringTurn = HbmAnimations.getRelevantTransformation("RINGTURN");
        double[] renderRing = HbmAnimations.getRelevantTransformation("RENDERRING");
        GunGL.translate(bodyMove[0], bodyMove[1], bodyMove[2]);

        if (shell == EnumGrenadeShell.FRAG) {
            GunGL.rotate(bodyTurn[2], 1, 0, 0);
            renderFragBody(stack);
            GunGL.bindTexture(WeaponResources.grenade_frag_tex);
            GunGL.renderPart(WeaponResources.grenades, "FragSpoon");
            if (renderRing[0] != 0) {
                GunGL.translate(ringMove[0], ringMove[1], ringMove[2]);
                GunGL.rotate(ringTurn[2], 1, 0, 0);
                GunGL.renderPart(WeaponResources.grenades, "FragRing");
            }
        }

        if (shell == EnumGrenadeShell.STICK) {
            GunGL.rotate(bodyTurn[2], 0, 0, 1);
            renderStickBody(stack);
            if (renderRing[0] != 0) {
                GunGL.translate(ringMove[0], ringMove[1], ringMove[2]);
                GunGL.rotate(ringTurn[1], 0, 1, 0);
                EnumGrenadeFilling filling = ItemGrenadeUniversal.getFilling(stack);
                GunGL.enableBlend();
                GunGL.blendFunc(770, 771, 1, 0);
                bind(WeaponResources.grenade_stick_tex);
                GunGL.renderPart(WeaponResources.grenades, "StickCap");
                setColor(filling.bodyColor);
                bind(WeaponResources.grenade_stick_body_tex);
                GunGL.renderPart(WeaponResources.grenades, "StickCap");
                GunGL.color(1F, 1F, 1F);
                GunGL.disableBlend();
            }
        }

        if (shell == EnumGrenadeShell.TECH) {
            GunGL.rotate(bodyTurn[2], 1, 0, 0);
            renderTechBody(stack);
            if (renderRing[0] != 0) {
                GunGL.translate(ringMove[0], ringMove[1], ringMove[2]);
                GunGL.rotate(ringTurn[2], 1, 0, 0);
                GunGL.bindTexture(WeaponResources.grenade_tech_tex);
                GunGL.renderPart(WeaponResources.grenades, "TechRing");
            }
        }

        if (shell == EnumGrenadeShell.NUKE) {
            GunGL.rotate(bodyTurn[2], 0, 0, 1);
            renderNukeBody(stack);
            GunGL.bindTexture(WeaponResources.grenade_nuka_tex);
            GunGL.renderPart(WeaponResources.grenades, "NukaSpoon");
            if (renderRing[0] != 0) {
                GunGL.translate(ringMove[0], ringMove[1], ringMove[2]);
                GunGL.translate(-1, 5, 0);
                GunGL.rotate(ringTurn[2], 0, 0, -1);
                GunGL.translate(1, -5, 0);
                GunGL.renderPart(WeaponResources.grenades, "NukaRing");
            }
        }
    }

    /** {@code renderType == null}: Darstellung als geworfenes Entity. */
    public static void renderGrenade(ItemStack stack, ItemRenderType renderType) {
        EnumGrenadeShell shell = ItemGrenadeUniversal.getShell(stack);

        if (shell == EnumGrenadeShell.FRAG) {
            if (renderType == ItemRenderType.INVENTORY) {
                GunGL.scale(3, 3, 3);
                GunGL.translate(0, -2, 0);
            }
            if (renderType == null) {
                GunGL.translate(0, -2, 0);
            }
            renderFragBody(stack);
            if (renderType != null) {
                GunGL.bindTexture(WeaponResources.grenade_frag_tex);
                GunGL.renderPart(WeaponResources.grenades, "FragSpoon");
                GunGL.renderPart(WeaponResources.grenades, "FragRing");
            }
        }

        if (shell == EnumGrenadeShell.STICK) {
            if (renderType == ItemRenderType.INVENTORY) {
                GunGL.scale(2, 2, 2);
                GunGL.translate(0, -4.5, 0);
            }
            if (renderType == ItemRenderType.EQUIPPED) {
                GunGL.translate(0, -2, 0);
            }
            if (renderType == null) {
                GunGL.translate(0, -2, 0);
            }
            renderStickBody(stack);
            if (renderType != null) {
                EnumGrenadeFilling filling = ItemGrenadeUniversal.getFilling(stack);
                setColor(filling.bodyColor);
                bind(WeaponResources.grenade_stick_body_tex);
                GunGL.renderPart(WeaponResources.grenades, "StickCap");
                GunGL.color(1F, 1F, 1F);
            }
        }

        if (shell == EnumGrenadeShell.TECH) {
            if (renderType == ItemRenderType.INVENTORY) {
                GunGL.scale(3.5, 3.5, 3.5);
                GunGL.translate(0, -1.75, 0);
            }
            if (renderType == ItemRenderType.EQUIPPED) {
                GunGL.scale(1.5, 1.5, 1.5);
                GunGL.translate(0.5, -1, 0.5);
            }
            if (renderType == null) {
                GunGL.scale(1.5, 1.5, 1.5);
                GunGL.translate(0, -1, 0);
            }
            renderTechBody(stack);
            if (renderType != null) {
                GunGL.bindTexture(WeaponResources.grenade_tech_tex);
                GunGL.renderPart(WeaponResources.grenades, "TechRing");
            }
        }

        if (shell == EnumGrenadeShell.NUKE) {
            if (renderType == ItemRenderType.INVENTORY) {
                GunGL.scale(2.5, 2.5, 2.5);
                GunGL.translate(0, -2.75, 0);
            }
            if (renderType == ItemRenderType.EQUIPPED) {
                GunGL.scale(1.5, 1.5, 1.5);
                GunGL.translate(0.5, -3, 0.5);
            }
            if (renderType == null) {
                GunGL.scale(1.5, 1.5, 1.5);
                GunGL.translate(0, -3, 0);
            }
            renderNukeBody(stack);
            if (renderType != null) {
                GunGL.bindTexture(WeaponResources.grenade_nuka_tex);
                GunGL.renderPart(WeaponResources.grenades, "NukaSpoon");
                GunGL.renderPart(WeaponResources.grenades, "NukaRing");
            }
        }
    }

    public static void renderFragBody(ItemStack stack) {
        renderBodyStandard(stack, "Frag", WeaponResources.grenade_frag_tex, WeaponResources.grenade_frag_body_tex, WeaponResources.grenade_frag_label_tex, WeaponResources.grenade_frag_fuze_tex);
    }

    public static void renderStickBody(ItemStack stack) {
        renderBodyStandard(stack, "Stick", WeaponResources.grenade_stick_tex, WeaponResources.grenade_stick_body_tex, WeaponResources.grenade_stick_label_tex, WeaponResources.grenade_stick_fuze_tex);
    }

    public static void renderTechBody(ItemStack stack) {

        EnumGrenadeFilling filling = ItemGrenadeUniversal.getFilling(stack);
        EnumGrenadeFuze fuze = ItemGrenadeUniversal.getFuze(stack);

        renderWithTexture(WeaponResources.grenade_tech_tex, "Tech");

        GunGL.enableBlend();
        GunGL.blendFunc(770, 771, 1, 0);

        setColor(filling.bodyColor);
        renderWithTexture(WeaponResources.grenade_tech_body_tex, "Tech");
        setColor(fuze.bandColor);
        renderWithTexture(WeaponResources.grenade_tech_fuze_tex, "Tech");
        GunGL.fullbright(true);
        setColor(filling.labelColor);
        renderWithTexture(WeaponResources.grenade_tech_lights_tex, "Tech");
        GunGL.fullbright(false);

        GunGL.color(1F, 1F, 1F);
        GunGL.disableBlend();
    }

    public static void renderNukeBody(ItemStack stack) {
        renderBodyStandard(stack, "Nuka", WeaponResources.grenade_nuka_tex, WeaponResources.grenade_nuka_body_tex, WeaponResources.grenade_nuka_label_tex, WeaponResources.grenade_nuka_fuze_tex);
    }

    public static void renderBodyStandard(ItemStack stack, String part, ResourceLocation baseTex, ResourceLocation bodyTex, ResourceLocation labelTex, ResourceLocation fuzeTex) {

        EnumGrenadeFilling filling = ItemGrenadeUniversal.getFilling(stack);
        EnumGrenadeFuze fuze = ItemGrenadeUniversal.getFuze(stack);

        renderWithTexture(baseTex, part);

        GunGL.enableBlend();
        GunGL.blendFunc(770, 771, 1, 0);

        setColor(filling.bodyColor);
        renderWithTexture(bodyTex, part);
        setColor(filling.labelColor);
        renderWithTexture(labelTex, part);
        setColor(fuze.bandColor);
        renderWithTexture(fuzeTex, part);

        GunGL.color(1F, 1F, 1F);
        GunGL.disableBlend();
    }

    /** Original {@code GL11.glColor3f(ColorUtil.fr(hex), ColorUtil.fg(hex), ColorUtil.fb(hex))}. */
    public static void setColor(int hex) {
        GunGL.color(((hex >> 16) & 255) / 255F, ((hex >> 8) & 255) / 255F, (hex & 255) / 255F);
    }

    public static void bind(ResourceLocation res) {
        GunGL.bindTexture(res);
    }

    public static void renderWithTexture(ResourceLocation res, String part) {
        bind(res);
        GunGL.renderPart(WeaponResources.grenades, part);
    }
}
