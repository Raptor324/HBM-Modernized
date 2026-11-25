"""
Bake autogen material textures (dense wire, cast plate, welded plate)
using the exact 1.7.10 RGBMutatorInterpolatedComponentRemap algorithm
and colors from Mats.java.
"""

import os
import re
from PIL import Image

def comp_r(col): return (col >> 16) & 0xff
def comp_g(col): return (col >> 8) & 0xff
def comp_b(col): return col & 0xff

def get_pos(lower, upper, val):
    return (float(val) - float(lower)) / (float(upper) - float(lower))

def get_comp(lower, upper, interp):
    return float(lower) + interp * (float(upper) - float(lower))

def shift(lighter, darker, bound_lighter, bound_darker, component):
    scaled = get_pos(bound_lighter, bound_darker, component)
    return get_comp(lighter, darker, scaled)

def shift_color(bound_lighter, bound_darker, lighter, darker, r, g, b, a):
    if a == 0:
        return (0, 0, 0, 0)
    nr = int(shift(comp_r(lighter), comp_r(darker), comp_r(bound_lighter), comp_r(bound_darker), r)) & 0xff
    ng = int(shift(comp_g(lighter), comp_g(darker), comp_g(bound_lighter), comp_g(bound_darker), g)) & 0xff
    nb = int(shift(comp_b(lighter), comp_b(darker), comp_b(bound_lighter), comp_b(bound_darker), b)) & 0xff
    return (nr, ng, nb, a)

def mutate_image(base_img, light, dark):
    w, h = base_img.size
    out = Image.new('RGBA', (w, h))
    for x in range(w):
        for y in range(h):
            r, g, b, a = base_img.getpixel((x, y))
            out.putpixel((x, y), shift_color(0xFFFFFF, 0x505050, light, dark, r, g, b, a))
    return out

def main():
    orig_mats = r"C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\java\com\hbm\inventory\material\Mats.java"
    with open(orig_mats, "r", encoding="utf-8", errors="ignore") as f:
        text = f.read()

    mats_colors = {}
    for m in re.finditer(r'(?:public\s+static\s+final\s+NTMMaterial|//\s*public\s+static\s+final\s+NTMMaterial)\s+(\w+)\s*=\s*make(?:Smeltable|Unsmeltable)\s*\([^,]+,\s*([^,]+),\s*(0x[0-9a-fA-F]+),\s*(0x[0-9a-fA-F]+)', text):
        var_name = m.group(1)
        light = int(m.group(3), 16)
        dark = int(m.group(4), 16)
        mats_colors[var_name] = (light, dark)

    # 1. Wire Dense
    base_wd = Image.open(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\resources\assets\hbm\textures\items\wire_dense.png").convert("RGBA")
    wd_dir = r"src\main\resources\assets\hbm_m\textures\item\wire_dense"
    wire_dense_map = {
        'advanced_alloy': 'MAT_ALLOY',
        'aluminium': 'MAT_ALUMINIUM',
        'bscco': 'MAT_BSCCO',
        'combine_steel': 'MAT_CMB',
        'copper': 'MAT_COPPER',
        'dnt': 'MAT_DNT',
        'gold': 'MAT_GOLD',
        'iron': 'MAT_IRON',
        'lead': 'MAT_LEAD',
        'magnetized_tungsten': 'MAT_MAGTUNG',
        'neodymium': 'MAT_NEODYMIUM',
        'niobium': 'MAT_NIOBIUM',
        'red_copper': 'MAT_MINGRADE',
        'saturnite': 'MAT_SATURN',
        'schrabidate': 'MAT_SCHRABIDATE',
        'schrabidium': 'MAT_SCHRABIDIUM',
        'star_metal': 'MAT_STAR',
        'steel': 'MAT_STEEL',
        'titanium': 'MAT_TITANIUM',
        'tungsten': 'MAT_TUNGSTEN',
    }

    print("Generating Wire Dense textures...")
    for name, var in wire_dense_map.items():
        light, dark = mats_colors[var]
        mutated = mutate_image(base_wd, light, dark)
        out_path = os.path.join(wd_dir, f"wire_dense_{name}.png")
        mutated.save(out_path, format="PNG")
        print(f"  Saved {out_path} (light=0x{light:06X}, dark=0x{dark:06X})")

    # 2. Plate Cast
    base_pc = Image.open(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\resources\assets\hbm\textures\items\plate_cast.png").convert("RGBA")
    pc_dir = r"src\main\resources\assets\hbm_m\textures\block"
    plate_cast_map = {
        'abronze': 'MAT_ABRONZE',
        'alloy': 'MAT_ALLOY',
        'aluminium': 'MAT_ALUMINIUM',
        'bbronze': 'MAT_BBRONZE',
        'cdalloy': 'MAT_CDALLOY',
        'cmb': 'MAT_CMB',
        'copper': 'MAT_COPPER',
        'desh': 'MAT_DESH',
        'dura_steel': 'MAT_DURA',
        'ferrouranium': 'MAT_FERRO',
        'gold': 'MAT_GOLD',
        'iron': 'MAT_IRON',
        'lead': 'MAT_LEAD',
        'osmiridium': 'MAT_OSMIRIDIUM',
        'saturnite': 'MAT_SATURN',
        'schrabidate': 'MAT_SCHRABIDATE',
        'schrabidium': 'MAT_SCHRABIDIUM',
        'star_metal': 'MAT_STAR',
        'steel': 'MAT_STEEL',
        'tcalloy': 'MAT_TCALLOY',
        'titanium': 'MAT_TITANIUM',
        'tungsten': 'MAT_TUNGSTEN',
        'weaponsteel': 'MAT_WEAPONSTEEL',
        'zirconium': 'MAT_ZIRCONIUM',
    }

    print("\nGenerating Plate Cast textures...")
    for name, var in plate_cast_map.items():
        light, dark = mats_colors[var]
        mutated = mutate_image(base_pc, light, dark)
        out_path = os.path.join(pc_dir, f"plate_cast_{name}.png")
        mutated.save(out_path, format="PNG")
        print(f"  Saved {out_path} (light=0x{light:06X}, dark=0x{dark:06X})")

    # 3. Plate Welded
    base_pw = Image.open(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\resources\assets\hbm\textures\items\plate_welded.png").convert("RGBA")
    pw_dir = r"src\main\resources\assets\hbm_m\textures\block"
    plate_welded_map = {
        'aluminium': 'MAT_ALUMINIUM',
        'cdalloy': 'MAT_CDALLOY',
        'cmb': 'MAT_CMB',
        'copper': 'MAT_COPPER',
        'iron': 'MAT_IRON',
        'osmiridium': 'MAT_OSMIRIDIUM',
        'steel': 'MAT_STEEL',
        'tcalloy': 'MAT_TCALLOY',
        'titanium': 'MAT_TITANIUM',
        'tungsten': 'MAT_TUNGSTEN',
        'zirconium': 'MAT_ZIRCONIUM',
    }

    print("\nGenerating Plate Welded textures...")
    for name, var in plate_welded_map.items():
        light, dark = mats_colors[var]
        mutated = mutate_image(base_pw, light, dark)
        out_path = os.path.join(pw_dir, f"plate_welded_{name}.png")
        mutated.save(out_path, format="PNG")
        print(f"  Saved {out_path} (light=0x{light:06X}, dark=0x{dark:06X})")

    print("\nAll textures successfully baked!")

if __name__ == "__main__":
    main()
