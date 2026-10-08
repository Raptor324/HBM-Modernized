package com.hbm_m.powerarmor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ShadyUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

//? if forge {
import java.util.function.Consumer;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;
//?}

/**
 * 1:1 {@code com.hbm.items.armor.ArmorFSB} - Ruestung mit Vollset-Bonus: Effekte, VATS/Thermal,
 * Geigerzaehler-Klang, harte Landung, Dashes, Schritthoehe, Schritt-/Sprung-/Fallgeraeusche,
 * Helm-Overlay, Gefahrenklassen und Strahlungsresistenz (HazmatRegistry), versteckte Koerperteile.
 * Die Haken {@link #handleTick}, {@link #handleJump}, {@link #handleFall}, {@link #handleAttack} und
 * {@link #handleHurt} ruft {@code HbmForgeEvents} fuer die Brustplatte auf wie der Original-ModEventHandler.
 */
public class ModArmorFSB extends ArmorItem implements com.hbm_m.item.ITooltipProvider {

    private String texture = "";
    private ResourceLocation overlay = null;
    public List<MobEffectInstance> effects = new ArrayList<>();
    public boolean noHelmet = false;
    public boolean vats = false;
    public boolean thermal = false;
    public boolean hasGeigerSound = false;
    public boolean customGeiger = false;
    public boolean hardLanding = false;
    public int dashCount = 0;
    public int stepSize = 0;
    public String stepSound;
    public String jumpSound;
    public String fallSound;
    public double radResist = 0;

    public ModArmorFSB(ModArmorMaterials material, Type type, Properties properties, String texture) {
        //? if forge {
        super(material, type, properties.stacksTo(1));
        //?} elif neoforge {
        /*super(com.hbm_m.item.tools_and_armor.ModArmorMaterialsAccess.holder(material), type, properties.stacksTo(1));
        *///?}
        this.texture = texture;
    }

    public ModArmorFSB addEffect(MobEffectInstance effect) { effects.add(effect); return this; }
    public ModArmorFSB setNoHelmet(boolean noHelmet) { this.noHelmet = noHelmet; return this; }
    public ModArmorFSB enableVATS(boolean vats) { this.vats = vats; return this; }
    public ModArmorFSB enableThermalSight(boolean thermal) { this.thermal = thermal; return this; }
    public ModArmorFSB setHasGeigerSound(boolean geiger) { this.hasGeigerSound = geiger; return this; }
    public ModArmorFSB setHasCustomGeiger(boolean geiger) { this.customGeiger = geiger; return this; }
    public ModArmorFSB setHasHardLanding(boolean hardLanding) { this.hardLanding = hardLanding; return this; }
    public ModArmorFSB setDashCount(int dashCount) { this.dashCount = dashCount; return this; }
    public ModArmorFSB setStepSize(int stepSize) { this.stepSize = stepSize; return this; }
    public ModArmorFSB setStep(String step) { this.stepSound = step; return this; }
    public ModArmorFSB setJump(String jump) { this.jumpSound = jump; return this; }
    public ModArmorFSB setFall(String fall) { this.fallSound = fall; return this; }
    /** Alte Port-Namen. */
    public ModArmorFSB setStepSound(String step) { return setStep(step); }
    public ModArmorFSB setJumpSound(String jump) { return setJump(jump); }
    public ModArmorFSB setFallSound(String fall) { return setFall(fall); }

    public ModArmorFSB setOverlay(String path) {
        this.overlay = ResourceLocation.tryParse(path);
        return this;
    }

    /** Original: {@code ArmorUtil.external.add(new Pair(this, classes))}. */
    public ModArmorFSB setHazardClass(HazardClass... classes) {
        com.hbm_m.util.ArmorUtil.registerExternalProtection(this, classes);
        return this;
    }

    /** Original: Vollset-Strahlungsresistenz, anteilig je Teil in die HazmatRegistry. */
    public ModArmorFSB setRadResist(double fullSet) {
        this.radResist = fullSet;
        if (fullSet > 0) {
            com.hbm_m.handler.HazmatRegistry.registerExternalFullSet(this, fullSet);
        }
        return this;
    }

    public ModArmorFSB cloneStats(ModArmorFSB original) {
        //lists aren't being modified after instantiation, so there's no need to dereference
        this.effects = original.effects;
        this.noHelmet = original.noHelmet;
        this.vats = original.vats;
        this.thermal = original.thermal;
        this.hasGeigerSound = original.hasGeigerSound;
        this.customGeiger = original.customGeiger;
        this.hardLanding = original.hardLanding;
        this.dashCount = original.dashCount;
        this.stepSize = original.stepSize;
        this.stepSound = original.stepSound;
        this.jumpSound = original.jumpSound;
        this.fallSound = original.fallSound;
        this.setRadResist(original.radResist);
        //overlay doesn't need to be copied because it's helmet exclusive
        return this;
    }

    //? if forge {
    @Override
    //?}
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return texture;
    }

    //? if neoforge {
    /*/^* NeoForge: Textur ueber die String-Variante (1.20.1 Forge {@code getArmorTexture(.., String type)}). ^/
    @Override
    public net.minecraft.resources.ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
            net.minecraft.world.item.ArmorMaterial.Layer layer, boolean innerModel) {
        String tex = this.getArmorTexture(stack, entity, slot, (String) null);
        return tex == null ? null : net.minecraft.resources.ResourceLocation.parse(tex);
    }
    *///?}

    /** Original addInformation: "Full Set Bonus:" mit Effekten und Faehigkeiten. */
    @Override
    public void appendHbmTooltip(ItemStack stack, @org.jetbrains.annotations.Nullable Level level, List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
        addFSBInfo(list);
    }

    protected void addFSBInfo(List<net.minecraft.network.chat.Component> list) {
        List<net.minecraft.network.chat.Component> toAdd = new ArrayList<>();

        if (!effects.isEmpty()) {
            net.minecraft.network.chat.MutableComponent potionList = net.minecraft.network.chat.Component.empty();
            boolean first = true;
            for (MobEffectInstance effect : effects) {
                if (!first) potionList.append(", ");
                potionList.append(net.minecraft.network.chat.Component.translatable(effect.getDescriptionId()));
                first = false;
            }
            toAdd.add(potionList.withStyle(net.minecraft.ChatFormatting.AQUA));
        }

        if (hasGeigerSound) toAdd.add(line(net.minecraft.ChatFormatting.GOLD, "armor.geigerSound"));
        if (customGeiger) toAdd.add(line(net.minecraft.ChatFormatting.GOLD, "armor.geigerHUD"));
        if (vats) toAdd.add(line(net.minecraft.ChatFormatting.RED, "armor.vats"));
        if (thermal) toAdd.add(line(net.minecraft.ChatFormatting.RED, "armor.thermal"));
        if (hardLanding) toAdd.add(line(net.minecraft.ChatFormatting.RED, "armor.hardLanding"));
        if (stepSize != 0) toAdd.add(line(net.minecraft.ChatFormatting.BLUE, "armor.stepSize", stepSize));
        if (dashCount > 0) toAdd.add(line(net.minecraft.ChatFormatting.AQUA, "armor.dash", dashCount));

        if (!toAdd.isEmpty()) {
            list.add(net.minecraft.network.chat.Component.translatable("armor.fullSetBonus").withStyle(net.minecraft.ChatFormatting.GOLD));
            list.addAll(toAdd);
        }
    }

    protected static net.minecraft.network.chat.Component line(net.minecraft.ChatFormatting color, String key, Object... args) {
        return net.minecraft.network.chat.Component.literal("  ").append(net.minecraft.network.chat.Component.translatable(key, args)).withStyle(color);
    }

    public String getTexture() {
        return texture;
    }

    public ResourceLocation getOverlay() {
        return overlay;
    }

    private static final EquipmentSlot[] ORDER = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    public static boolean hasFSBArmor(Player player) {
        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);

        if (plate.getItem() instanceof ModArmorFSB chestplate) {
            boolean noHelmet = chestplate.noHelmet;

            for (int i = 0; i < (noHelmet ? 3 : 4); i++) {
                ItemStack armor = player.getItemBySlot(ORDER[i]);
                if (armor.isEmpty() || !(armor.getItem() instanceof ModArmorFSB fsb)) return false;
                if (fsb.getMaterial() != chestplate.getMaterial()) return false;
                if (!fsb.isArmorEnabled(armor)) return false;
            }
            return true;
        }
        return false;
    }

    public static boolean hasFSBArmorIgnoreCharge(Player player) {
        ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);

        if (plate.getItem() instanceof ModArmorFSB chestplate) {
            boolean noHelmet = chestplate.noHelmet;

            for (int i = 0; i < (noHelmet ? 3 : 4); i++) {
                ItemStack armor = player.getItemBySlot(ORDER[i]);
                if (armor.isEmpty() || !(armor.getItem() instanceof ModArmorFSB fsb)) return false;
                if (fsb.getMaterial() != chestplate.getMaterial()) return false;
            }
            return true;
        }
        return false;
    }

    /** Original {@code handleTick(PlayerTickEvent)}: beide Seiten, jede Phase. */
    public void handleTick(Player player) {
        boolean step = true;

        if (player.getUUID().toString().equals(ShadyUtil.the_NCR) || player.getUUID().toString().equals(ShadyUtil.Barnaby99_x)) {
            step = false;
            if (player.level().isClientSide && player.onGround()) {
                steppy(player, "hbm:step.powered");
            }
        }

        if (ModArmorFSB.hasFSBArmor(player)) {
            ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
            ModArmorFSB chestplate = (ModArmorFSB) plate.getItem();

            if (!chestplate.effects.isEmpty()) {
                for (MobEffectInstance i : chestplate.effects) {
                    player.addEffect(new MobEffectInstance(i.getEffect(), i.getDuration(), i.getAmplifier(), true, true));
                }
            }

            if (step && chestplate.stepSound != null && player.level().isClientSide && player.onGround()) {
                steppy(player, chestplate.stepSound);
            }
        }
    }

    /**
     * Original steppy: eigener Schritt-Klang im Takt der Vanilla-Schritte. Der naechste Schrittpunkt
     * liegt wie beim Vanilla-{@code nextStep} je eine Einheit {@code moveDist} weiter.
     */
    public static void steppy(Player player, String sound) {
        var data = player.getPersistentData();
        if (data.getFloat("hfr_nextStepDistance") == 0) {
            data.putFloat("hfr_nextStepDistance", player.moveDist + 1);
        }

        int px = Mth.floor(player.getX());
        int py = Mth.floor(player.getY() - 0.2D);
        int pz = Mth.floor(player.getZ());

        if (!player.level().getBlockState(new BlockPos(px, py, pz)).isAir() && data.getFloat("hfr_nextStepDistance") <= player.moveDist) {
            player.playSound(HbmSoundsNT.get(sound), 0.25F, 1.0F);
            data.putFloat("hfr_nextStepDistance", player.moveDist + 1);
        } else if (data.getFloat("hfr_nextStepDistance") > player.moveDist + 2) {
            data.putFloat("hfr_nextStepDistance", player.moveDist + 1);
        }
    }

    public void handleJump(Player player) {
        if (ModArmorFSB.hasFSBArmor(player)) {
            ModArmorFSB chestplate = (ModArmorFSB) player.getItemBySlot(EquipmentSlot.CHEST).getItem();

            if (chestplate.jumpSound != null)
                player.playSound(HbmSoundsNT.get(chestplate.jumpSound), 0.5F, 1.0F);
        }
    }

    public void handleFall(Player player) {
        if (ModArmorFSB.hasFSBArmor(player)) {
            ModArmorFSB chestplate = (ModArmorFSB) player.getItemBySlot(EquipmentSlot.CHEST).getItem();

            if (chestplate.hardLanding && player.fallDistance > 10) {

                List<Entity> entities = player.level().getEntities(player, player.getBoundingBox().inflate(3, 0, 3));

                for (Entity e : entities) {
                    if (e instanceof ItemEntity)
                        continue;

                    Vec3 vec = new Vec3(player.getX() - e.getX(), 0, player.getZ() - e.getZ());

                    if (vec.length() < 3) {
                        double intensity = 3 - vec.length();
                        e.setDeltaMovement(e.getDeltaMovement().add(vec.x * intensity * -2, 0.1D * intensity, vec.z * intensity * -2));
                        e.hurt(com.hbm_m.damagesource.ModDamageSources.hardlandingSmash(player), (float) (intensity * 10));
                    }
                }
            }

            if (chestplate.fallSound != null)
                player.playSound(HbmSoundsNT.get(chestplate.fallSound), 0.5F, 1.0F);
        }
    }

    //? if forge {
    @Override
    @SuppressWarnings("removal")
    //?}
    // NeoForge: Aufruf ueber ArmorTickNeoForge
    public void onArmorTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Player entity) {
        geigerTick(world, entity);
    }

    /** Original onArmorTick: Geigerzaehler-Klang der Brustplatte. */
    protected void geigerTick(Level world, Player entity) {
        if (this.getType() != Type.CHESTPLATE) return;
        if (!hasFSBArmor(entity) || !this.hasGeigerSound) return;
        if (entity.getInventory().hasAnyOf(Set.of(ModItems.GEIGER_COUNTER.get(), ModItems.DOSIMETER.get()))) return;

        if (world.getGameTime() % 5 == 0) {

            // Armor piece dosimeters indicate radiation dosage inside the armor, so reduce the counts by the effective protection
            float mod = ContaminationUtil.calculateRadiationMod(entity);
            float x = HbmLivingProps.getRadBuf(entity) * mod;

            if (x > 1E-5) {
                List<Integer> list = new ArrayList<>();

                if (x < 1) list.add(0);
                if (x < 5) list.add(0);
                if (x < 10) list.add(1);
                if (x > 5 && x < 15) list.add(2);
                if (x > 10 && x < 20) list.add(3);
                if (x > 15 && x < 25) list.add(4);
                if (x > 20 && x < 30) list.add(5);
                if (x > 25) list.add(6);

                int r = list.get(world.random.nextInt(list.size()));

                if (r > 0) world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HbmSoundsNT.get("hbm:item.geiger" + r), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public boolean isArmorEnabled(ItemStack stack) {
        return true;
    }

    private final Set<String> hidden = new HashSet<>();
    private boolean needsFullSet = false;

    /** Original hides(EnumPlayerPart...): Namen der Spielermodell-Teile ("hat", "left_leg", ...). */
    public ModArmorFSB hides(String... parts) {
        Collections.addAll(hidden, parts);
        return this;
    }

    public ModArmorFSB setFullSetForHide() {
        needsFullSet = true;
        return this;
    }

    public boolean disablesPart(Player player, ItemStack stack, String part) {
        return hidden.contains(part) && (!needsFullSet || hasFSBArmorIgnoreCharge(player));
    }

    /** Original handleAttack(LivingAttackEvent): {@code event.canceled} bricht den Angriff ab. */
    public void handleAttack(LivingEntity entity, com.hbm_m.armormod.item.ItemArmorMod.Hurt event) { }

    /** Original handleHurt(LivingHurtEvent): {@code event.amount} ist der Schaden. */
    public void handleHurt(LivingEntity entity, com.hbm_m.armormod.item.ItemArmorMod.Hurt event) { }

    /** true fuer Sets mit eigenem OBJ-Modell (Render-Layer); dann zeichnet die Vanilla-Ruestungsschicht nichts. */
    public boolean isObjArmor() {
        return false;
    }

    //? if forge {
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private com.hbm_m.powerarmor.layer.PowerArmorEmptyModel model;

            @Override
            public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTick) {
                ModArmorFSB.this.renderHelmetOverlay(stack, player, width, height, partialTick);
            }

            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack,
                    EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                if (!isObjArmor()) return IClientItemExtensions.super.getHumanoidArmorModel(living, stack, slot, original);
                if (this.model == null) {
                    this.model = new com.hbm_m.powerarmor.layer.PowerArmorEmptyModel(net.minecraft.client.Minecraft.getInstance()
                            .getEntityModels().bakeLayer(com.hbm_m.powerarmor.layer.ModModelLayers.POWER_ARMOR));
                }
                return com.hbm_m.powerarmor.layer.PowerArmorEmptyModel.prepare(this.model, slot, original);
            }
        });
    }
    //?} elif neoforge {
    /*@Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private com.hbm_m.powerarmor.layer.PowerArmorEmptyModel model;

            @Override
            public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTick) {
                ModArmorFSB.this.renderHelmetOverlay(stack, player, width, height, partialTick);
            }

            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity living, ItemStack stack,
                    EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                if (!isObjArmor()) return net.neoforged.neoforge.client.extensions.common.IClientItemExtensions.super.getHumanoidArmorModel(living, stack, slot, original);
                if (this.model == null) {
                    this.model = new com.hbm_m.powerarmor.layer.PowerArmorEmptyModel(net.minecraft.client.Minecraft.getInstance()
                            .getEntityModels().bakeLayer(com.hbm_m.powerarmor.layer.ModModelLayers.POWER_ARMOR));
                }
                return com.hbm_m.powerarmor.layer.PowerArmorEmptyModel.prepare(this.model, slot, original);
            }
        });
    }
    *///?}

    /** Original {@code renderHelmetOverlay}: Standard ist das Overlay aus {@link #setOverlay}. */
    public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTick) {
        if (overlay == null) return;
        com.hbm_m.powerarmor.overlay.FSBHelmetOverlay.render(overlay, width, height);
    }
}
