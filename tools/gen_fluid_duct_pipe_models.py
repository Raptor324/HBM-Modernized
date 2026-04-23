#!/usr/bin/env python3
"""Generate multipart pipe model JSONs (Forge OBJ + visibility) for all three duct styles."""
import json
import os

ROOT = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "hbm_m", "models", "block")
)
BLOCKSTATES = os.path.normpath(
    os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "hbm_m", "blockstates")
)

GROUPS = ["pZ", "pX", "nZ", "nX", "pY", "nY", "nnn", "nnp", "pnp", "pnn", "ppn", "npn", "npp", "ppp"]

# folder_name -> (base_texture without hbm_m:block/, overlay_texture)
STYLES = [
    ("pipe_neo", "pipe_neo", "pipe_neo_overlay"),
    ("pipe_colored", "pipe_colored", "pipe_colored_overlay"),
    ("pipe_silver", "pipe_silver", "pipe_silver_overlay"),
]


def vis(active: set[str]) -> dict:
    return {g: (g in active) for g in GROUPS}


def write_model(folder: str, fname: str, kind: str, render_type: str, active: set[str], tex_base: str, tex_over: str):
    os.makedirs(os.path.join(ROOT, folder), exist_ok=True)
    path = os.path.join(ROOT, folder, f"{fname}.json")
    mtl = f"hbm_m:models/block/{folder}_{kind}.mtl"
    textures = {
        "particle": f"hbm_m:block/{tex_base}",
        "base": f"hbm_m:block/{tex_base}",
    }
    if kind == "overlay":
        textures["overlay"] = f"hbm_m:block/{tex_over}"
    data = {
        "loader": "forge:obj",
        "model": "hbm_m:models/block/pipe_neo.obj",
        "flip_v": True,
        "mtl_override": mtl,
        "render_type": render_type,
        "textures": textures,
        "transform": {"translation": [0.5, 0.5, 0.5]},
        "visibility": vis(active),
    }
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def gen_models_for_style(folder: str, tex_base: str, tex_over: str):
    arms6 = {"pZ", "pX", "nZ", "nX", "pY", "nY"}
    write_model(folder, "iso_base", "base", "minecraft:solid", arms6, tex_base, tex_over)
    write_model(folder, "iso_overlay", "overlay", "minecraft:cutout", arms6, tex_base, tex_over)

    write_model(folder, "through_x_base", "base", "minecraft:solid", {"pX", "nX"}, tex_base, tex_over)
    write_model(folder, "through_x_overlay", "overlay", "minecraft:cutout", {"pX", "nX"}, tex_base, tex_over)
    write_model(folder, "through_y_base", "base", "minecraft:solid", {"pY", "nY"}, tex_base, tex_over)
    write_model(folder, "through_y_overlay", "overlay", "minecraft:cutout", {"pY", "nY"}, tex_base, tex_over)
    write_model(folder, "through_z_base", "base", "minecraft:solid", {"pZ", "nZ"}, tex_base, tex_over)
    write_model(folder, "through_z_overlay", "overlay", "minecraft:cutout", {"pZ", "nZ"}, tex_base, tex_over)

    arm_map = [
        ("arm_east", "pX"),
        ("arm_west", "nX"),
        ("arm_up", "pY"),
        ("arm_down", "nY"),
        ("arm_south", "pZ"),
        ("arm_north", "nZ"),
    ]
    for fname, part in arm_map:
        s = {part}
        write_model(folder, f"{fname}_base", "base", "minecraft:solid", s, tex_base, tex_over)
        write_model(folder, f"{fname}_overlay", "overlay", "minecraft:cutout", s, tex_base, tex_over)

    octants = ["ppn", "ppp", "npn", "npp", "pnn", "pnp", "nnn", "nnp"]
    for o in octants:
        s = {o}
        write_model(folder, f"oct_{o}_base", "base", "minecraft:solid", s, tex_base, tex_over)
        write_model(folder, f"oct_{o}_overlay", "overlay", "minecraft:cutout", s, tex_base, tex_over)


def multipart_for_blockstate(model_prefix: str) -> list:
    """model_prefix e.g. hbm_m:block/pipe_neo"""
    mp = []

    def pair(path_base: str, when: dict | None):
        entry = {
            "apply": [
                {"model": f"{model_prefix}/{path_base}_base"},
                {"model": f"{model_prefix}/{path_base}_overlay"},
            ]
        }
        if when is not None:
            entry["when"] = when
        mp.append(entry)

    pair("iso", {"shape": "isolated"})
    pair("through_x", {"shape": "through_x"})
    pair("through_y", {"shape": "through_y"})
    pair("through_z", {"shape": "through_z"})

    c = {"shape": "complex"}
    pair("arm_east", {**c, "east": "true"})
    pair("arm_west", {**c, "west": "true"})
    pair("arm_up", {**c, "up": "true"})
    pair("arm_down", {**c, "down": "true"})
    pair("arm_south", {**c, "south": "true"})
    pair("arm_north", {**c, "north": "true"})

    # NEO octant conditions (!east = !pX, !west = !nX, !up = !pY, !down = !nY, !south = !pZ, !north = !nZ)
    oct_when = [
        ("ppn", {"east": "false", "up": "false", "south": "false"}),
        ("ppp", {"east": "false", "up": "false", "north": "false"}),
        ("npn", {"west": "false", "up": "false", "south": "false"}),
        ("npp", {"west": "false", "up": "false", "north": "false"}),
        ("pnn", {"east": "false", "down": "false", "south": "false"}),
        ("pnp", {"east": "false", "down": "false", "north": "false"}),
        ("nnn", {"west": "false", "down": "false", "south": "false"}),
        ("nnp", {"west": "false", "down": "false", "north": "false"}),
    ]
    for name, w in oct_when:
        pair(f"oct_{name}", {**c, **w})

    return mp


def main():
    for folder, tb, to in STYLES:
        gen_models_for_style(folder, tb, to)

    blocks = [
        ("fluid_duct.json", "hbm_m:block/pipe_neo"),
        ("fluid_duct_colored.json", "hbm_m:block/pipe_colored"),
        ("fluid_duct_silver.json", "hbm_m:block/pipe_silver"),
    ]
    for fname, prefix in blocks:
        data = {"multipart": multipart_for_blockstate(prefix)}
        path = os.path.join(BLOCKSTATES, fname)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    print("Done.")


if __name__ == "__main__":
    main()
