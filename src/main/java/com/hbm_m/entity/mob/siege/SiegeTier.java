package com.hbm_m.entity.mob.siege;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.entity.mob.siege.SiegeTier}: Stufen der Belagerungseinheiten (Panzerung DT/DR, Leben, Schaden,
 * Tempo, Feuerfestigkeit, Laser). Die Beute wird als Lieferant gehalten, weil die Stufen sonst vor den Gegenstaenden
 * angelegt wuerden.
 */
public class SiegeTier {

    public static SiegeTier[] tiers = new SiegeTier[100];
    private static int nextID = 0;

    public static int getLength() {
        return nextID;
    }

    public static SiegeTier DEFAULT_BUFF;
    public static SiegeTier CLAY;
    public static SiegeTier STONE;
    public static SiegeTier IRON;
    public static SiegeTier SILVER;
    public static SiegeTier GOLD;
    public static SiegeTier DESH;
    public static SiegeTier SCHRAB;
    public static SiegeTier DNT;

    static {
        registerTiers();
    }

    private static ItemStack steel() {
        return ModMaterialItems.stack(ModMaterials.STEEL, MaterialShape.INGOT, 1);
    }

    public static void registerTiers() {
        if (nextID > 0) return;
        DEFAULT_BUFF =	new SiegeTier(20, "buff")		.addDrop(SiegeTier::steel)	.setDR(0.2F)										.setDMG(2F)									.setLaser(0F, 0F, false);
        CLAY =			new SiegeTier(30, "clay")		.addDrop(SiegeTier::steel)	.setDR(0.2F)										.setDMG(3F)									.setLaser(0F, 0F, false);
        STONE =			new SiegeTier(40, "stone")		.addDrop(SiegeTier::steel)	.setDR(0.3F)	.setDT(1F)				.setFP()	.setDMG(5F)									.setLaser(0F, 0F, true);
        IRON =			new SiegeTier(50, "iron")		.addDrop(SiegeTier::steel)	.setDR(0.3F)	.setDT(2F)				.setFP()	.setDMG(7.5F)					.setFF()	.setLaser(0F, 1F, true);
        SILVER =		new SiegeTier(70, "silver")		.addDrop(SiegeTier::steel)	.setDR(0.5F)	.setDT(3F)	.setNF()	.setFP()	.setDMG(10F)	.setSP(0.5F)	.setFF()	.setLaser(0.01F, 1F, true);
        GOLD =			new SiegeTier(100, "gold")		.addDrop(SiegeTier::steel)	.setDR(0.5F)	.setDT(5F)	.setNF()	.setFP()	.setDMG(15F)	.setSP(0.5F)	.setFF()	.setLaser(0.02F, 1.5F, true);
        DESH =			new SiegeTier(150, "desh")		.addDrop(SiegeTier::steel)	.setDR(0.7F)	.setDT(7F)	.setNF()	.setFP()	.setDMG(25F)	.setSP(0.5F)	.setFF()	.setLaser(0.05F, 1.5F, true);
        SCHRAB =		new SiegeTier(250, "schrab")	.addDrop(SiegeTier::steel)	.setDR(0.7F)	.setDT(10F)	.setNF()	.setFP()	.setDMG(50F)	.setSP(1F)		.setFF()	.setLaser(0.1F, 2F, true);
        DNT =			new SiegeTier(500, "dnt")		.addDrop(SiegeTier::steel)	.setDR(0.9F)	.setDT(20F)	.setNF()	.setFP()	.setDMG(100F)	.setSP(1F)		.setFF()	.setLaser(0.2F, 2F, true);
    }

    public int id;
    public float dt = 0F;
    public float dr = 0F;
    public float health = 20F;
    public String name = "";
    public float damageMod = 1F;
    public float speedMod = 0F;
    public boolean fireProof = false;
    public boolean noFall = false;
    public boolean noFriendlyFire = false;
    public List<Supplier<ItemStack>> dropItem = new ArrayList<>();

    public float laserBreak = 0F;
    public float laserExplosive = 0F;
    public boolean laserIncendiary = false;

    public Consumer<LivingEntity> delegate;

    public SiegeTier(float baseHealth, String name) {
        this.id = nextID;
        SiegeTier.tiers[this.id] = this;
        nextID++;

        this.health = baseHealth;
        this.name = name;
    }

    private SiegeTier setDT(float dt) {
        this.dt = dt;
        return this;
    }

    private SiegeTier setDR(float dr) {
        this.dr = dr;
        return this;
    }

    private SiegeTier setDMG(float dmg) {
        this.damageMod = dmg;
        return this;
    }

    private SiegeTier setSP(float sp) {
        this.speedMod = sp;
        return this;
    }

    private SiegeTier setFP() {
        this.fireProof = true;
        return this;
    }

    private SiegeTier setNF() {
        this.noFall = true;
        return this;
    }

    private SiegeTier setFF() {
        this.noFriendlyFire = true;
        return this;
    }

    private SiegeTier setLaser(float breaking, float explosive, boolean incendiary) {
        this.laserBreak = breaking;
        this.laserExplosive = explosive;
        this.laserIncendiary = incendiary;
        return this;
    }

    private SiegeTier addDrop(Supplier<ItemStack> drop) {
        this.dropItem.add(drop);
        return this;
    }

    public void runDelegate(LivingEntity entity) {
        if (this.delegate != null) this.delegate.accept(entity);
    }
}
