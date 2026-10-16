import io, os, json, glob

PORT = r"C:\Users\Luke\Documents\VS Projekte HBM Modernized\HBM-Modernized"
ORIG_TEX = (r"C:\Users\Luke\Documents\VS Projekte HBM Modernized\Hbm-s-Nuclear-Tech-GIT-master"
            r"\Hbm-s-Nuclear-Tech-GIT-master\src\main\resources\assets\hbm\textures\items")

# every item the turrets accept, by registry name
AMMO = [
    "turret_ammo",
    "ammo_9mm_sp", "ammo_9mm_fmj", "ammo_9mm_jhp", "ammo_9mm_ap",
    "ammo_50_sp", "ammo_50_fmj", "ammo_50_jhp", "ammo_50_ap", "ammo_50_du",
    "ammo_556_sp", "ammo_556_fmj", "ammo_556_jhp", "ammo_556_ap",
    "ammo_shell", "ammo_shell_explosive", "ammo_shell_apfsds_t", "ammo_shell_apfsds_du", "ammo_shell_w9",
    "ammo_dgk", "ammo_tau_uranium", "ammo_flame_diesel",
    "ammo_arty", "ammo_arty_cargo", "ammo_arty_chlorine", "ammo_arty_classic", "ammo_arty_he",
    "ammo_arty_mini_nuke", "ammo_arty_mini_nuke_multi", "ammo_arty_mustard_gas", "ammo_arty_nuke",
    "ammo_arty_phosgene", "ammo_arty_phosphorus", "ammo_arty_phosphorus_multi",
    "rocket_turret_standard", "rocket_turret_heat", "rocket_turret_demo", "rocket_turret_inc",
    "rocket_turret_phosphorus",
    "rocket_himars_standard", "rocket_himars_he", "rocket_himars_lava", "rocket_himars_mini_nuke",
    "rocket_himars_wp", "rocket_himars_thermobaric", "rocket_himars_single", "rocket_himars_single_tb",
    "upgrade_speed_1", "upgrade_speed_2", "upgrade_speed_3",
    "upgrade_effect_1", "upgrade_effect_2", "upgrade_effect_3",
    "upgrade_power_1", "upgrade_power_2", "upgrade_power_3",
    "upgrade_afterburn_1", "upgrade_afterburn_2", "upgrade_afterburn_3",
    "upgrade_overdrive_1", "upgrade_overdrive_2", "upgrade_overdrive_3",
    "upgrade_5g", "upgrade_screm",
]

def model_texture(name):
    for root in ("src/generated/resources", "src/main/resources"):
        p = os.path.join(PORT, root, "assets", "hbm_m", "models", "item", name + ".json")
        if os.path.exists(p):
            try:
                d = json.load(io.open(p, encoding="utf-8-sig"))
            except Exception as e:
                return "?", "unparseable (%s)" % e
            tex = (d.get("textures") or {}).get("layer0")
            return tex, root
    return None, None

orig_names = {os.path.splitext(f)[0] for f in os.listdir(ORIG_TEX)}
port_tex_dir = os.path.join(PORT, "src", "main", "resources", "assets", "hbm_m", "textures", "item")
port_names = {os.path.splitext(f)[0] for f in os.listdir(port_tex_dir)}

print("%-32s %-34s %s" % ("item", "layer0 texture", "verdict"))
print("-" * 92)
problems = 0
for name in AMMO:
    tex, _ = model_texture(name)
    if tex is None:
        print("%-32s %-34s NO MODEL" % (name, "-")); problems += 1; continue
    short = tex.split("/")[-1]

    verdict = []
    if short not in port_names:
        verdict.append("TEXTURE FILE MISSING")
    # a texture whose name has nothing to do with the item is a leftover placeholder
    if short != name and short != name.replace("rocket_himars_", "rocket_himars_standard_") and not (name == "rocket_turret_standard" and short == "rocket_turret_he"):
        verdict.append("MISMATCHED (placeholder?)")
    if verdict:
        problems += 1
    print("%-32s %-34s %s" % (name, short, ", ".join(verdict) or "ok"))

print("\n%d of %d flagged" % (problems, len(AMMO)))
