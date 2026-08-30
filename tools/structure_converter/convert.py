#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Конвертер структур 1.7.10 HBM (NTM) -> современный ванильный формат structure template.

Вход : C:\\Projects\\Hbm-s-Nuclear-Tech-GIT\\src\\main\\resources\\assets\\hbm\\structures
Выход: src/main/resources/data/hbm_m/structures/*.nbt            (DataVersion 3465 = 1.20.1,
        1.21.1 поднимает имена через DataFixerUpper автоматически)
        src/main/resources/data/hbm_m/worldgen/structure/*.json
        src/main/resources/data/hbm_m/worldgen/structure_set/*.json
        src/main/resources/data/hbm_m/worldgen/template_pool/*.json  (+ meteor/*)
        src/main/resources/data/hbm_m/tags/worldgen/biome/has_structure/*.json
        docs/structures_port_report.md

Запуск:  python tools/structure_converter/convert.py
"""
import gzip, json, os, re, struct, sys, zlib, collections, pathlib

SRC = r"C:\Projects\Hbm-s-Nuclear-Tech-GIT\src\main\resources\assets\hbm\structures"
DST = pathlib.Path(__file__).resolve().parents[2] / "src" / "main" / "resources" / "data" / "hbm_m"
DOCS = pathlib.Path(__file__).resolve().parents[2] / "docs"
DATA_VERSION = 3465  # 1.20.1
NS = "hbm_m"

report = collections.OrderedDict()
warnings = []

def warn(msg):
    if msg not in warnings:
        warnings.append(msg)

# ---------------------------------------------------------------------------
# Минимальный NBT writer/reader
# ---------------------------------------------------------------------------
class R:
    def __init__(self, d): self.d=d; self.i=0
    def u1(self): v=self.d[self.i]; self.i+=1; return v
    def i1(self): v=struct.unpack_from('>b',self.d,self.i)[0]; self.i+=1; return v
    def i2(self): v=struct.unpack_from('>h',self.d,self.i)[0]; self.i+=2; return v
    def i4(self): v=struct.unpack_from('>i',self.d,self.i)[0]; self.i+=4; return v
    def i8(self): v=struct.unpack_from('>q',self.d,self.i)[0]; self.i+=8; return v
    def f4(self): v=struct.unpack_from('>f',self.d,self.i)[0]; self.i+=4; return v
    def f8(self): v=struct.unpack_from('>d',self.d,self.i)[0]; self.i+=8; return v
    def u2(self): n=self.i2(); return n & 0xffff
    def s(self):
        n=self.u2(); v=self.d[self.i:self.i+n].decode('utf-8'); self.i+=n; return v

def read_payload(r,t):
    if t==1: return r.i1()
    if t==2: return r.i2()
    if t==3: return r.i4()
    if t==4: return r.i8()
    if t==5: return r.f4()
    if t==6: return r.f8()
    if t==7: n=r.i4(); v=bytearray(r.d[r.i:r.i+n]); r.i+=n; return v
    if t==8: return r.s()
    if t==9:
        et=r.u1(); n=r.i4(); return [read_payload(r,et) for _ in range(max(n,0))]
    if t==10:
        d={}
        while True:
            et=r.u1()
            if et==0: break
            key=r.s()
            d[key]=read_payload(r,et)
        return d
    if t==11: n=r.i4(); v=[r.i4() for _ in range(n)]; return v
    if t==12: n=r.i4(); v=[r.i8() for _ in range(n)]; return v
    raise ValueError("tag %d" % t)

def nbt_load(path):
    raw=open(path,'rb').read()
    if raw[:2]==b'\x1f\x8b': raw=gzip.decompress(raw)
    r=R(raw); t=r.u1(); name=r.s()
    return read_payload(r,t)

class W:
    def __init__(self): self.b=bytearray()
    def u1(self,v): self.b.append(v&0xff)
    def i2(self,v): self.b+=struct.pack('>h',v)
    def i4(self,v): self.b+=struct.pack('>i',v)
    def i8(self,v): self.b+=struct.pack('>q',v)
    def f4(self,v): self.b+=struct.pack('>f',v)
    def f8(self,v): self.b+=struct.pack('>d',v)
    def s(self,v):
        e=v.encode('utf-8'); self.i2(len(e)); self.b+=e

def tag_id(v):
    if isinstance(v,bool): return 1
    if isinstance(v,int):
        if -128<=v<=127: return 1
        if -32768<=v<=32767: return 2
        if -2**31<=v<2**31: return 3
        return 4
    if isinstance(v,float): return 6
    if isinstance(v,str): return 8
    if isinstance(v,list): return 9
    if isinstance(v,dict): return 10
    raise ValueError(type(v))

def wany(w,v):
    t=tag_id(v)
    if t==1: w.u1(int(v) & 0xff)   # НЕНУЛЕВЫЕ байты НЕ превращать в 1 (state-индексы!)
    elif t==2: w.i2(v)
    elif t==3: w.i4(v)
    elif t==4: w.i8(v)
    elif t==6: w.f8(v)
    elif t==8: w.s(v)
    elif t==9:
        # ВАЖНО: ванилла читает size/pos через getList(key, TAG_INT) с фильтром типа -
        # список должен быть ТОЧНО TAG_Int, иначе возвращается пустой список
        # и шаблон получается размером 0x0x0. Все int-списки пишем как TAG_Int.
        if v and all(isinstance(e,int) and not isinstance(e,bool) for e in v):
            w.u1(3); w.i4(len(v))
            for e in v: w.i4(int(e))
        else:
            et = tag_id(v[0]) if v else 8
            w.u1(et); w.i4(len(v))
            for e in v: wany(w,e)
    elif t==10:
        for k,x in v.items():
            w.u1(tag_id(x)); w.s(k); wany(w,x)
        w.u1(0)

def nbt_write(root: dict):
    w=W()
    w.u1(10); w.s("")
    for k,x in root.items():
        w.u1(tag_id(x)); w.s(k); wany(w,x)
    w.u1(0)
    return gzip.compress(bytes(w.b))

# ---------------------------------------------------------------------------
# Утилиты свойств
# ---------------------------------------------------------------------------
STAIR_FACING = {0:"east",1:"west",2:"south",3:"north"}
WALL_FACING  = {2:"north",3:"south",4:"west",5:"east"}          # печи/таблички/лестницы-консоли
TORCH_FACING = {1:"east",2:"west",3:"south",4:"north"}          # факелы/кнопки
DOOR_FACING  = {0:"east",1:"south",2:"west",3:"north"}
TRAPDOOR_FACING = {0:"east",1:"west",2:"south",3:"north"}
BED_FACING   = {0:"south",1:"west",2:"north",3:"east"}
REPEATER_FACING = {0:"north",1:"east",2:"south",3:"west"}
DYE_COLORS = ["white","orange","magenta","light_blue","yellow","lime","pink","gray",
              "light_gray","cyan","purple","blue","brown","green","red","black"]

def stairs(meta):
    return {"facing": STAIR_FACING.get(meta & 3, "east"),
            "half": "top" if meta & 4 else "bottom"}

def hbm_facing(meta):
    return {"facing": WALL_FACING.get(meta & 7, "north")}

# DecoBlock (steel_wall/scaffold): meta = сторона, где стоит панель
# (case 2 -> z+/south, 3 -> z-/north, 4 -> x+/east, 5 -> x-/west), а не направление "морды".
def deco_wall_facing(meta):
    return {"facing": {2:"south",3:"north",4:"east",5:"west"}.get(meta & 7, "north")}

def deco_rot(meta):  # BlockDecoModel: rot 0=south,1=west,2=north,3=east
    return {"facing": {0:"south",1:"west",2:"north",3:"east"}.get(meta & 3, "south")}

def axis_from_pillar(meta):
    return {"axis": {0:"y",4:"x",8:"z"}.get(meta & 12, "y")}

# Двери BlockModDoor (клон ванильного BlockDoor 1.7.10):
#   lower: биты 0-1 = ориентация, бит 2 (4) = открыта, бит 3 (8) = верхняя половина
#   upper: бит 0 (1) = петля справа, бит 1 (2) = запитана
# Без проперти двери спавнились двумя "half: lower" и осыпались при открытии.
def door_props(meta):
    upper = meta & 8
    return {"facing": DOOR_FACING.get(meta & 3, "east"),
            "half": "upper" if upper else "lower",
            "hinge": "right" if upper and meta & 1 else "left",
            "open": "true" if (not upper and meta & 4) else "false",
            "powered": "false"}

# Spotlight (cage lamp): meta = сторона_крепления<<1 | бит_битости,
# ForgeDirection.getOrientation(meta>>1): 0=down,1=up,2=north,3=south,4=west,5=east
SPOTLIGHT_FACING = {0:"down",1:"up",2:"north",3:"south",4:"west",5:"east"}
def spotlight_facing(meta):
    return {"facing": SPOTLIGHT_FACING.get((meta >> 1) & 7, "north")}

# Steel Grate: meta 0-7 = высота панели meta*2px, 8 = прижата к потолку, 9 = под блоком
def grate_pos(meta):
    return {"pos": str(min(meta, 9))}

# ---------------------------------------------------------------------------
# Маппинг HBM-блоков
#   значение: dict(name=, props=fn(meta), te=..., note=)
# ---------------------------------------------------------------------------
def M(name, props=None, note=None):
    return {"name": name, "props": props, "note": note}

HBM_MAP = {
    # --- простые алиасы (блок существует в modernized) ---
    "geiger":                 M("geiger_counter_block", hbm_facing),
    "door_metal":             M("metal_door", door_props),
    "door_bunker":            M("door_bunker", door_props),
    "door_office":            M("door_office", door_props),
    "red_barrel":             M("barrel_red"),
    "yellow_barrel":          M("barrel_yellow"),
    "pink_barrel":            M("barrel_pink"),
    "lox_barrel":             M("barrel_lox"),
    "vitrified_barrel":       M("barrel_vitrified"),
    "barrel_iron":            M("barrel_iron", hbm_facing),
    "barrel_plastic":         M("barrel_plastic"),
    "barrel_corroded":        M("barrel_corroded"),
    "deco_toaster":           M("toaster", deco_rot),
    "machine_diesel":         M("dieselgen", hbm_facing),
    "machine_electric_furnace_off": M("electric_furnace", hbm_facing),
    "machine_boiler_off":     M("machine_boiler", hbm_facing),
    "machine_battery":        M("machine_battery", hbm_facing),
    "machine_fluidtank":      M("machine_fluidtank", hbm_facing),
    "machine_microwave":      M("machine_microwave", hbm_facing),
    "machine_weapon_table":   M("armor_table", hbm_facing, "weapon_table отсутствует, ближайший - armor_table"),
    "machine_rotary_furnace": M("furnace_steel", hbm_facing, "rotary_furnace не портирован - статичная замена furnace_steel"),
    "machine_rtg_grey":       M("machine_battery", hbm_facing, "rtg отсутствует - замена machine_battery"),
    "machine_funnel":         M("machine_funnel"),
    "concrete_smooth":        M("concrete"),
    "lightstone":             None,  # вариантный, см. HBM_VARIANT
    "ntm_dirt":               M("dirt_dead", None, "ntm_dirt =~ dirt_dead"),
    "tnt_ntm":                M("minecraft:tnt", None, "tnt_ntm -> ванильный TNT"),
    "tile_lab":               M("concrete_tile", None, "tile_lab -> concrete_tile"),
    "tile_lab_cracked":       M("concrete_tile", None),
    "tile_lab_broken":        M("concrete_tile", None),
    "anvil_lead":             M("anvil_lead", hbm_facing),
    "mine_naval":             M("naval_mine"),
    "dungeon_chain":          M("minecraft:chain", lambda m: {"axis": {0:"y",2:"z",3:"z",4:"x",5:"x"}.get(m,"y")}, "hbm-цепь -> ванильная chain"),
    "turret_howard_damaged":  M("deco_steel", hbm_facing, "turret_howard_damaged -> deco_steel (турели требуют живого BE)"),
    "turret_sentry_damaged":  M("deco_steel", hbm_facing, "turret_sentry_damaged -> deco_steel"),
    "turret_sentry":          M("turret_sentry"),
    "turret_howard":          M("turret_howard"),
    "floodlight":             M("flood_lamp", hbm_facing, "floodlight -> flood_lamp"),
    "spotlight_incandescent": M("cage_lamp", spotlight_facing, "spotlight -> cage_lamp (ориентация = сторона крепления meta>>1)"),
    "spotlight_incandescent_off": M("cage_lamp", spotlight_facing),
    "spotlight_fluoro":       M("cage_lamp", spotlight_facing),
    "spotlight_halogen":      M("cage_lamp", spotlight_facing),
    "spotlight_beam":         M("cage_lamp", None),
    "filing_cabinet":         M("crate_metal", None, "filing_cabinet -> crate_metal"),
    "safe":                   M("crate_iron", None, "safe -> crate_iron"),
    "crate_supply":           M("crate_steel", None, "crate_supply -> crate_steel"),
    "meteor_battery":         M("machine_battery", hbm_facing, "meteor_battery -> machine_battery"),
    "charger":                M("machine_battery", hbm_facing, "charger -> machine_battery"),
    "gas_asbestos":           M("minecraft:air", None, "газовый блок асбеста -> воздух"),
    "ore_coal_oil":           M("ore_oil", None, "ore_coal_oil -> ore_oil"),
    "ore_oil_sand":           M("ore_oil", None, "ore_oil_sand -> ore_oil"),
    "fluid_duct_gauge":       M("fluid_duct"),
    "fluid_duct_neo":         M("fluid_duct"),
    "bobblehead":             M("minecraft:flower_pot", None, "bobblehead -> flower_pot (порт голов-фигурок отложен)"),
    "skeleton_holder":        M("minecraft:skeleton_skull", None, "skeleton_holder -> skeleton_skull"),
    "dungeon_spawner":        M("meteor_spawner", None, "dungeon_spawner -> meteor_spawner"),
    "wood_barrier":           M("wood_barrier", hbm_facing),
    "ladder_steel":           M("ladder_steel", hbm_facing),
    "ladder_tungsten":        M("ladder_steel", hbm_facing, "ladder_tungsten -> ladder_steel"),
    "trapdoor_steel":         M("trapdoor_steel"),
    "fence_metal":            M("fence_metal"),
    "fence_metal_post":       M("fence_metal_post"),
    "rail_narrow":            M("rail_narrow"),
    "red_cable":              M("red_cable"),
    "red_connector":          M("red_connector"),
    "red_wire_coated":        M("red_wire_coated"),
    "pole_top":               M("pole_top"),
    "pole_satellite_receiver":M("pole_satellite_receiver", hbm_facing),
    "radio_telex":            M("radio_telex", hbm_facing),
    "radiorec":               M("radiorec", hbm_facing),
    "tape_recorder":          M("tape_recorder", hbm_facing),
    "geiger_counter":         M("geiger_counter_block", hbm_facing),
    "hev_battery":            M("hev_battery", hbm_facing),
    "tesla":                  M("tesla"),
    "spikes":                 M("spikes"),
    "mine_ap":                M("mine_ap"),
    "mine_he":                M("mine_he"),
    "det_charge":             M("det_charge"),
    "mush":                   M("mush"),
    "meteor_spawner":         M("meteor_spawner"),
    "meteor_brick":           M("meteor_brick"),
    "meteor_brick_chiseled":  M("meteor_brick_chiseled"),
    "meteor_polished":        M("meteor_polished"),
    "block_slag":             M("block_slag"),
    "machine_controller":     M("deco_steel", None, "machine_controller (дисгайз спавнера) -> deco_steel"),
    "wand_jigsaw":            M("minecraft:air"),   # заменяется TE-логикой jigsaw
    "wand_logic":             M("minecraft:air"),   # заменяется TE-логикой спавнеров
    "wand_loot":              M("minecraft:air"),   # заменяется TE-логикой лута
    "wand_tandem":            M("minecraft:air"),
    "meteor_pillar":          M("meteor_pillar", axis_from_pillar),
    "concrete_pillar":        M("concrete_pillar", axis_from_pillar),
    "steel_poles":            M("steel_pole", hbm_facing),
    "gravel_obsidian":        M("gravel_obsidian"),
    "oil_spill":              M("oil_spill"),
    "sellafield_slaked":      M("sellafield_slaked"),
    "dirt_dead":              M("dirt_dead"),
    "waste_leaves":           M("waste_leaves"),
    "wood_structure":         M("wood_structure"),
    "deco_loot":              M("deco_loot"),
    "crate":                  M("crate"),
    "crate_iron":             M("crate_iron"),
    "crate_steel":            M("crate_steel"),
    "crate_metal":            M("crate_metal"),
    "crate_lead":             M("crate_lead"),
    "crate_red":              M("crate_red"),
    "crate_ammo":             M("crate_ammo"),
    "crate_can":              M("crate_can"),
    "crate_weapon":           M("crate_weapon"),
    "deco_steel":             M("deco_steel", hbm_facing),
    "deco_rusty_steel":       M("deco_rusty_steel", hbm_facing),
    "deco_aluminium":         M("deco_aluminium", hbm_facing),
    "deco_lead":              M("deco_lead", hbm_facing),
    "deco_tungsten":          M("deco_tungsten", hbm_facing),
    "deco_red_copper":        M("deco_red_copper", hbm_facing),
    "deco_beryllium":         M("deco_beryllium", hbm_facing),
    "deco_computer":          M("deco_computer", deco_rot),
    "deco_titanium":          M("deco_titanium", hbm_facing),
    "deco_crt":               None,  # вариантный
    "deco_pipe":              None,  # вариантные, см. HBM_PIPES
    "deco_pipe_rusted":       None,
    "deco_pipe_red":          None,
    "deco_pipe_marked":       None,
    "deco_pipe_quad":         None,
    "deco_pipe_quad_rusted":  None,
    "deco_pipe_quad_red":     None,
    "deco_pipe_quad_marked":  None,
    "deco_pipe_framed":       None,
    "deco_pipe_framed_rusted":None,
    "deco_pipe_framed_red":   None,
    "deco_pipe_framed_green_rusted": None,
    "deco_pipe_rim":          None,
    "deco_pipe_rim_rusted":   None,
    "deco_pipe_rim_marked":   None,
    "steel_corner":           M("steel_corner", hbm_facing),
    "steel_grate":            M("steel_grate", grate_pos),
    "steel_grate_wide":       M("steel_grate_wide", grate_pos),
    "steel_roof":             M("steel_roof", hbm_facing),
    "steel_wall":             M("steel_wall", deco_wall_facing),
    "steel_scaffold":         M("steel_scaffold", lambda m: {"axis": {0:"y",4:"z",8:"y",12:"x"}.get(m & 12,"y")}),
    "steel_beam":             M("steel_beam", lambda m: {"axis":"y"}, "в оригинале beam.obj всегда вертикален"),
    "reinforced_glass":       M("reinforced_glass"),
    "reinforced_glass_pane":  M("reinforced_glass_pane"),
    "reinforced_brick":       M("reinforced_brick"),
    "reinforced_stone":       M("reinforced_stone"),
    "reinforced_sand":        M("reinforced_sand"),
    "reinforced_lamp_off":    M("reinforced_lamp_off"),
    "reinforced_light":       M("reinforced_light"),
    "reinforced_glass_pane":  M("reinforced_glass_pane"),
    "balefire":               M("balefire"),
    "toxic_block":            M("toxic_block"),
    "plant_dead":             M("plant_dead"),
    "leaves_layer":           M("minecraft:air", None, "leaves_layer (листовой ковёр) -> воздух"),
    "brick_asbestos":         M("brick_asbestos"),
    "brick_compound":         M("brick_compound"),
    "brick_light":            M("brick_light"),
    "brick_concrete":         M("brick_concrete"),
    "brick_concrete_cracked": M("brick_concrete_cracked"),
    "brick_concrete_broken":  M("brick_concrete_broken"),
    "brick_concrete_mossy":   M("brick_concrete_mossy"),
    "concrete":               M("concrete"),
    "concrete_asbestos":      M("concrete_asbestos"),
    "concrete_super":         M("concrete_super"),
    "concrete_super_broken":  M("concrete_super_broken"),
    "concrete_rebar":         M("concrete_rebar"),
    "block_meteor":           M("block_meteor"),
    "block_meteor_cobble":    M("block_meteor_cobble"),
    "block_copper":           M("block_copper"),
    "block_red_copper":       M("block_red_copper"),
    "block_scrap":            M("block_scrap"),
    "block_electrical_scrap": M("block_electrical_scrap"),
    "block_starmetal":        M("block_starmetal"),
    "block_aluminium":        M("block_aluminium"),
    "block_copper":           M("block_copper"),
    "crt_blinking":           M("crt_clean"),
    "crt_broken":             M("crt_broken"),
    "mush":                   M("mush"),
    "dirt_dead":              M("dirt_dead"),
}

# вариантные слэбы/лестницы
HBM_SLABS = {  # (имя 1.7.10) -> [модерн-блок по meta&7]
    "brick_slab": ["reinforced_stone_slab", "reinforced_brick_slab", "brick_obsidian_slab",
                   "brick_light_slab", "brick_compound_slab"],
    "brick_double_slab": ["reinforced_stone", "reinforced_brick", "brick_obsidian",
                          "brick_light", "brick_compound"],
    "concrete_slab": ["concrete_slab", "concrete_slab", "concrete_asbestos_slab"],
    "concrete_double_slab": ["concrete", "concrete", "concrete_asbestos"],
    "concrete_brick_slab": ["brick_concrete_slab", "brick_concrete_mossy_slab",
                            "brick_concrete_cracked_slab", "brick_concrete_broken_slab"],
    "concrete_brick_double_slab": ["brick_concrete", "brick_concrete_mossy",
                                   "brick_concrete_cracked", "brick_concrete_broken"],
}
HBM_STAIRS = {
    "brick_concrete_stairs": "brick_concrete_stairs",
    "brick_concrete_cracked_stairs": "brick_concrete_cracked_stairs",
    "brick_concrete_broken_stairs": "brick_concrete_broken_stairs",
    "brick_concrete_mossy_stairs": "brick_concrete_mossy_stairs",
    "concrete_stairs": "concrete_stairs",
    "concrete_smooth_stairs": "concrete_stairs",
    "concrete_asbestos_stairs": "concrete_asbestos_stairs",
    "reinforced_stone_stairs": "reinforced_stone_stairs",
    "reinforced_brick_stairs": "reinforced_brick_stairs",
    "brick_compound_stairs": "brick_compound_stairs",
    "brick_light_stairs": "brick_light_stairs",
    "brick_obsidian_stairs": "brick_obsidian_stairs",
    "lightstone_bricks_stairs": "lightstone_bricks_stairs",
}
# бетон под покраску (meta = vanilla dye index)
HBM_VARIANT = {
    # NB: light_gray в modernized называется concrete_silver
    "concrete_colored": {i: M("concrete_" + ("silver" if c == "light_gray" else c)) for i, c in enumerate(DYE_COLORS)},
    "concrete_colored_ext": {0: M("concrete_colored_ext_machine"), 1: M("concrete_colored_ext_machine_stripe"),
                             2: M("concrete_colored_ext_indigo"), 3: M("concrete_colored_ext_purple"),
                             4: M("concrete_colored_ext_pink"), 5: M("concrete_colored_ext_hazard"),
                             6: M("concrete_colored_ext_sand"), 7: M("concrete_colored_ext_bronze")},
    "lightstone": {0: M("lightstone_unrefined"), 1: M("lightstone_tile"), 2: M("lightstone_bricks"),
                   3: M("lightstone_bricks_chiseled"), 4: M("lightstone_chiseled")},
    "deco_crt": {0: M("crt_clean", deco_rot), 1: M("crt_broken", deco_rot),
                 2: M("crt_clean", deco_rot, "crt_blinking -> crt_clean"), 3: M("crt_bsod", deco_rot)},
    "brick_double_slab": None, "brick_slab": None,  # через HBM_SLABS
}

# deco_pipe* -> axis-колонны (если портированы) или замена fluid_duct
PIPE_SUBSTITUTION = "fluid_duct"
PIPE_NAMES = {n for n in HBM_MAP if n.startswith("deco_pipe")}

# ---------------------------------------------------------------------------
# Ванильные блоки 1.7.10 -> 1.20.1
# ---------------------------------------------------------------------------
def v(name, props=None):
    return ("minecraft:" + name, props or {})

def v_planks(meta):
    return v(["oak_planks","spruce_planks","birch_planks","jungle_planks","acacia_planks","dark_oak_planks"][meta & 7])

def v_slab(meta, kinds, doubles):
    t = meta & 7
    top = meta & 8
    if t >= len(kinds): t = 0
    if top and t < len(doubles):
        return v(doubles[t])
    return v(kinds[t], {"type": "top" if top else "bottom", "waterlogged": "false"})

def convert_vanilla(name, meta):
    n = name.split(":",1)[1]
    if n == "air": return v("air")
    if n == "stone": return v("stone")
    if n == "grass": return v("grass_block", {"snowy":"false"})
    if n == "tallgrass": return v("grass", {"half":"lower" if meta==1 else "lower"})
    if n == "dirt": return v("dirt")
    if n == "gravel": return v("gravel")
    if n == "sand": return v("red_sand" if meta==1 else "sand")
    if n == "clay": return v("clay")
    if n == "sponge": return v("sponge")
    if n == "cobblestone": return v("cobblestone")
    if n == "cobblestone_wall": return v("mossy_cobblestone_wall" if meta==1 else "cobblestone_wall")
    if n == "brick_block": return v("bricks")
    if n == "bookshelf": return v("bookshelf")
    if n == "crafting_table": return v("crafting_table")
    if n == "coal_block": return v("coal_block")
    if n == "glowstone": return v("glowstone")
    if n == "web": return v("cobweb")
    if n == "water": return v("water", {"level": str(min(meta,7))})
    if n == "lava": return v("lava", {"level": str(min(meta&7,7))})
    if n == "bed":
        return v("bed", {"facing": BED_FACING.get(meta & 3,"south"),
                         "part": "head" if meta & 8 else "foot",
                         "occupied":"false"})
    if n == "bookshelf": return v("bookshelf")
    if n == "planks": return v_planks(meta)
    if n == "log":
        return v("oak_log", {"axis": {0:"y",4:"x",8:"z"}.get(meta & 12, "y")})
    if n == "leaves":
        return v("oak_leaves", {"distance":"7","persistent":"true","waterlogged":"false"})
    if n == "sandstone":
        return v("sandstone", {"type": {0:"default",1:"chiseled",2:"smooth"}.get(meta,"default")})
    if n == "stonebrick":
        return v(["stone_bricks","mossy_stone_bricks","cracked_stone_bricks","chiseled_stone_bricks"][meta & 3])
    if n == "wool":
        return v(DYE_COLORS[meta & 15] + "_wool")
    if n == "stained_glass":
        return v(DYE_COLORS[meta & 15] + "_stained_glass")
    if n == "stained_glass_pane":
        return v(DYE_COLORS[meta & 15] + "_stained_glass_pane", {"waterlogged":"false"})
    if n == "stained_hardened_clay":
        return v(DYE_COLORS[meta & 15] + "_terracotta")
    if n == "stone_slab":
        return v_slab(meta, ["smooth_stone_slab","sandstone_slab","petrified_oak_slab",
                             "cobblestone_slab","brick_slab","stone_brick_slab"], [])
    if n == "double_stone_slab":
        t = meta & 7
        return v({0:"smooth_stone",1:"sandstone",3:"cobblestone",4:"bricks",5:"stone_bricks"}.get(t,"smooth_stone"))
    if n == "wooden_slab":
        return v_slab(meta, ["oak_slab","spruce_slab","birch_slab","jungle_slab","acacia_slab","dark_oak_slab"], [])
    if n == "double_wooden_slab":
        return v_planks(meta & 7)
    if n == "glass_pane":
        return v("glass_pane", {"waterlogged":"false"})
    if n == "iron_bars":
        return v("iron_bars")
    if n == "fence":
        return v("oak_fence")
    if n == "torch":
        if meta == 5: return v("torch")
        return v("wall_torch", {"facing": TORCH_FACING.get(meta,"north")})
    if n == "redstone_torch":
        return v("redstone_wall_torch", {"facing": TORCH_FACING.get(meta,"north"), "lit":"true"})
    if n == "unlit_redstone_torch":
        return v("redstone_wall_torch", {"facing": TORCH_FACING.get(meta,"north"), "lit":"false"})
    if n == "redstone_lamp":
        return v("redstone_lamp", {"lit": "true" if meta else "false"})
    if n == "lever":
        p = {"powered": "true" if meta & 8 else "false"}
        o = meta & 7
        if o in (1,2,3,4):
            p.update({"face":"wall","facing":TORCH_FACING[o]})
        elif o == 5:
            p.update({"face":"floor","facing":"north"})
        else:
            p.update({"face":"ceiling","facing":"north"})
        return v("lever", p)
    if n == "stone_button":
        p = {"powered": "true" if meta & 8 else "false"}
        o = meta & 7
        if o in (1,2,3,4):
            p.update({"face":"wall","facing":TORCH_FACING[o]})
        elif o == 5:
            p.update({"face":"floor","facing":"north"})
        else:
            p.update({"face":"ceiling","facing":"north"})
        return v("stone_button", p)
    if n == "stone_stairs": return v("stone_stairs", stairs(meta))
    if n == "oak_stairs": return v("oak_stairs", stairs(meta))
    if n == "spruce_stairs": return v("spruce_stairs", stairs(meta))
    if n == "dark_oak_stairs": return v("dark_oak_stairs", stairs(meta))
    if n == "sandstone_stairs": return v("sandstone_stairs", stairs(meta))
    if n == "brick_stairs": return v("brick_stairs", stairs(meta))
    if n == "stone_brick_stairs": return v("stone_brick_stairs", stairs(meta))
    if n == "ladder":
        return v("ladder", {"facing": WALL_FACING.get(meta,"north"), "waterlogged":"false"})
    if n == "vine":
        p = {"north":"false","south":"false","east":"false","west":"false","up":"false"}
        if meta & 1: p["south"] = "true"
        if meta & 2: p["west"] = "true"
        if meta & 4: p["north"] = "true"
        if meta & 8: p["east"] = "true"
        return v("vine", p)
    if n == "trapdoor":
        return v("oak_trapdoor", {"facing": TRAPDOOR_FACING.get(meta & 3,"north"),
                                  "half": "top" if meta & 8 else "bottom",
                                  "open": "true" if meta & 4 else "false",
                                  "powered": "false", "waterlogged": "false"})
    if n == "wooden_door":
        upper = meta & 8
        return v("oak_door", {"facing": DOOR_FACING.get(meta & 3,"east"),
                              "half": "upper" if upper else "lower",
                              "hinge": "right" if upper and meta & 4 else "left",
                              "open": "true" if meta & 4 and not upper else "false",
                              "powered": "false"})
    if n == "wooden_pressure_plate":
        return v("oak_pressure_plate", {"powered":"false"})
    if n == "unpowered_repeater":
        return v("repeater", {"facing": REPEATER_FACING.get(meta & 3,"north"),
                              "delay": str((meta >> 2) + 1), "locked":"false", "powered":"false"})
    if n == "unpowered_comparator":
        return v("comparator", {"facing": REPEATER_FACING.get(meta & 3,"north"),
                                "mode": "subtract" if meta & 4 else "compare", "powered":"false"})
    if n == "wall_sign":
        return v("oak_wall_sign", {"facing": WALL_FACING.get(meta,"north"), "waterlogged":"false"})
    if n == "skull":
        return v("skeleton_skull", {"rotation": "0", "waterlogged": "false"})
    if n == "flower_pot":
        return v("flower_pot")
    if n == "double_plant":
        kinds = ["sunflower","lilac","tall_grass","large_fern","rose_bush","peony"]
        k = kinds[meta & 7] if (meta & 7) < 6 else "tall_grass"
        return v(k, {"half": "upper" if meta & 8 else "lower"})
    if n == "waterlily":
        return v("lily_pad")
    if n == "trapped_chest":
        return v("trapped_chest", {"facing": WALL_FACING.get(meta & 7,"north"), "type":"single", "waterlogged":"false"})
    if n == "redstone_torch" and meta == 0:
        return v("redstone_torch", {"lit":"true"})
    warn("vanilla block не обработан: %s (meta %s)" % (name, meta))
    return v("air")

# ---------------------------------------------------------------------------
# Ваниль:особые случаи
# ---------------------------------------------------------------------------
VANILLA_SPECIAL = {"grass","tallgrass","planks","log","leaves","sandstone","stonebrick","wool",
    "stained_glass","stained_glass_pane","stained_hardened_clay","stone_slab","double_stone_slab",
    "wooden_slab","double_wooden_slab","torch","redstone_torch","unlit_redstone_torch","redstone_lamp",
    "lever","stone_button","stone_stairs","oak_stairs","spruce_stairs","dark_oak_stairs","sandstone_stairs",
    "brick_stairs","stone_brick_stairs","ladder","vine","trapdoor","wooden_door","wooden_pressure_plate",
    "unpowered_repeater","unpowered_comparator","wall_sign","skull","flower_pot","double_plant",
    "waterlily","trapped_chest","bed","water","lava","sand","cobblestone_wall","glass_pane","iron_bars",
    "fence","web","brick_block","crafting_table","coal_block","glowstone","bookshelf","clay","gravel",
    "dirt","stone","air","sponge","chest","cobblestone","sandstone_stairs","coal_block"}

def convert_chest(state_name, meta, te, item_palette):
    facing = WALL_FACING.get(meta & 7, "north")
    props = {"facing": facing, "type": "single", "waterlogged": "false"}
    out = {"name": state_name if state_name.startswith("minecraft:") else "minecraft:chest", "Properties": props}
    nte = {"id": out["name"]}
    if te and ("Items" in te):
        items = []
        for it in te.get("Items", []):
            iid = it.get("id")
            name = None
            if isinstance(iid, str):
                name = iid
            elif isinstance(iid, int):
                name = item_palette.get(iid)
            if not name:
                warn("item id %r не найден в itemPalette - предмет пропущен" % (iid,))
                continue
            name = remap_item(name)
            if not name:
                continue
            items.append({"Slot": it.get("Slot", 0), "id": name, "Count": it.get("Count", 1)})
        if items:
            nte["Items"] = items
    return out, nte

ITEM_REMAP = {
    "minecraft:redstone_torch": "minecraft:redstone_torch",
    "hbm:item.fragment_lanthanium": "hbm_m:fragment_lanthanium",
    "hbm:item.fragment_boron": "hbm_m:fragment_boron",
    "hbm:item.nugget_mercury_tiny": "hbm_m:nugget_mercury_tiny",
    "hbm:item.pipe": None,  # нет прямого аналога
    "hbm:item.wiring_red_copper": "hbm_m:wiring_red_copper",
    "hbm:item.launch_code_piece": "hbm_m:launch_code_piece",
    "hbm:item.fluid_identifier": None,
}
def remap_item(name):
    if name in ITEM_REMAP:
        r = ITEM_REMAP[name]
        if r is None: warn("предмет %s не портирован - пропущен" % name)
        return r
    if name.startswith("minecraft:"): return name
    warn("предмет %s не найден в ITEM_REMAP - пропущен" % name)
    return None

# ---------------------------------------------------------------------------
# Лут-пулы wand_loot -> таблицы
# ---------------------------------------------------------------------------
POOL_TABLES = {
    "LOOT_SHIT": "hbm_m:loot_pile_common", "LOOT_BONES": "hbm_m:loot_pile_bones",
    "LOOT_SUPPLIES": "hbm_m:loot_pile_supplies", "LOOT_FLAREGUN": "hbm_m:loot_pile_weapons",
    "LOOT_MECHANICAL": "hbm_m:loot_pile_mechanical", "LOOT_CAPSTASH": "hbm_m:loot_pile_caps",
    "LOOT_CAPNUKE": "hbm_m:loot_pile_caps", "LOOT_MEDICINE": "hbm_m:loot_pile_medicine",
    "LOOT_MAKESHIFT_GUN": "hbm_m:loot_pile_weapons", "LOOT_GLYPHID_HIVE": "hbm_m:loot_pile_common",
    "LOOT_GEAR": "hbm_m:loot_pile_mechanical", "LOOT_METEOR": "hbm_m:loot_pile_common",
    "LOOT_BOOKLET": "hbm_m:loot_pile_common",
    "POOL_OFFICE_TRASH": "hbm_m:crates/steel_crate_office", "POOL_VERTIBIRD": "hbm_m:chests/military_cache",
    "POOL_METEORITE_TREASURE": "hbm_m:chests/military_cache", "POOL_MACHINE_PARTS": "hbm_m:crates/steel_crate",
    "POOL_BLACK_PART": "hbm_m:crates/steel_crate", "POOL_AMMO": "hbm_m:chests/military_cache",
    "POOL_VAULT_LAB": "hbm_m:crates/steel_crate_vault_lab", "POOL_ANTENNA": "hbm_m:chests/tech_cache",
    "POOL_SILO": "hbm_m:crates/steel_crate_silo", "POOL_WEAPONS": "hbm_m:chests/military_cache",
    "POOL_SUPPLIES": "hbm_m:chests/supply_drop", "POOL_FILING_CABINET": "hbm_m:chests/generic",
    "POOL_REPAIR_MATERIALS": "hbm_m:crates/steel_crate", "POOL_EXPENSIVE": "hbm_m:crates/iron_crate_expensive",
    "POOL_SOLID_FUEL": "hbm_m:chests/bunker_supplies", "POOL_NUKE_FUEL": "hbm_m:crates/iron_crate_nuke_fuel",
    "POOL_RTG": "hbm_m:crates/iron_crate", "POOL_OIL_RIG": "hbm_m:crates/steel_crate",
    "POOL_BLUEPRINTS": "hbm_m:chests/tech_cache", "POOL_METEOR_SAFE": "hbm_m:crates/iron_crate_expensive",
    "POOL_PILE_BONES": "hbm_m:loot_pile_bones",
}
# числовые id блоков в TE wand_loot (по контексту пулов)
BLOCK_ID_GUESS = {54: "minecraft:chest", 557: "hbm:tile.deco_loot", 683: "hbm:tile.crate_steel"}
CONTAINER_BE = {
    "minecraft:chest": "minecraft:chest",
    "minecraft:trapped_chest": "minecraft:trapped_chest",
    "hbm_m:crate_iron": "hbm_m:iron_crate_be",
    "hbm_m:crate_steel": "hbm_m:steel_crate_be",
}

FORGE_DIR_TO_VAN = {0: "down", 1: "up", 2: "north", 3: "south", 4: "west", 5: "east"}

def convert_state(name, meta):
    """Возвращает (имя модерн-блока, props dict)"""
    if name.startswith("minecraft:"):
        n = name.split(":",1)[1]
        if n in ("chest",):
            return ("minecraft:chest", {"facing": WALL_FACING.get(meta & 7,"north"), "type":"single", "waterlogged":"false"})
        if n in VANILLA_SPECIAL:
            return convert_vanilla(name, meta)
        warn("vanilla %s -> как есть" % name)
        return ("minecraft:" + n, {})
    base = name.split(":",1)[1]
    if base.startswith("tile."):
        base = base[5:]
    # слэбы
    if base in HBM_SLABS:
        kinds = HBM_SLABS[base]
        t = meta & 7
        if t >= len(kinds):
            warn("%s meta %d вне таблицы слэбов" % (base, meta))
            t = 0
        name2 = kinds[t]
        if "double" in base or meta & 8 and "double" in base:
            pass
        if "double" in base:
            return (NS + ":" + name2, {})
        return (NS + ":" + name2, {"type": "top" if meta & 8 else "bottom", "waterlogged": "false"})
    if base in HBM_STAIRS:
        return (NS + ":" + HBM_STAIRS[base], stairs(meta))
    if base in HBM_VARIANT and HBM_VARIANT[base]:
        vmap = HBM_VARIANT[base]
        m = meta
        if base == "deco_crt": m = (abs(meta) % 16) // 4
        ent = vmap.get(m, vmap.get(0))
        if ent is None:
            warn("%s meta %d не найден" % (base, meta))
            return ("minecraft:air", {})
        props = ent["props"](meta) if ent["props"] else {}
        if ent["note"]: warn("%s: %s" % (base, ent["note"]))
        nm = ent["name"]
        return (nm if ":" in nm else NS + ":" + nm, props)
    if base.startswith("deco_pipe"):
        # портированы как axis-колонны; meta/4: 0=y,1=x,2=z
        return (NS + ":" + base, {"axis": {0:"y",1:"x",2:"z"}.get(meta // 4, "y")})
    ent = HBM_MAP.get(base)
    if ent is None:
        warn("hbm-блок %s не найден в маппинге -> air" % base)
        return ("minecraft:air", {})
    props = ent["props"](meta) if ent["props"] else {}
    if ent["note"]:
        warn("%s: %s" % (base, ent["note"]))
    nm = ent["name"]
    return (nm if ":" in nm else NS + ":" + nm, props)

def props_to_tag(props):
    if not props: return None
    return {k: str(v) for k, v in props.items()}

# ---------------------------------------------------------------------------
# Таблица структур (из SpawnCondition в NTMWorldGenerator)
#   (json-имя, файл, тег биомов, высота, режим, вес, conform)
#   высота: int -> смещение от поверхности; (min,max) -> абсолютный uniform
#   режим: surface | oceanfloor | absolute
# ---------------------------------------------------------------------------
SINGLES = [
    ("spire",              "spire",              "flat",     -1, "surface",    2,  False),
    ("vertibird",          "vertibird",          "sandy",    -3, "surface",    6,  False),
    ("crashed_vertibird",  "crashed-vertibird",  "sandy",   -10, "surface",   10,  False),
    ("beached_patrol",     "beached_patrol",     "beach",  (58,67), "absolute",15, False),
    ("aircraft_carrier",   "aircraft_carrier",   "ocean",    -6, "oceanfloor", 3,  False),
    ("oil_rig",            "oil_rig",            "ocean",  (11,12), "absolute", 5,  False),
    ("lighthouse",         "lighthouse",         "coast",  (28,29), "absolute", 4,  False),
    ("dish",               "dish",               "plains", (53,65), "absolute",20,  False),
    ("water_pump",         "water_pump",         "waterside",-10, "surface",  15,  False),
    ("forest_chem",        "forest_chem",        "flat",     -9, "surface",   30,  False),
    ("laboratory",         "laboratory",         "flat",   (53,65), "absolute",20,  False),
    ("forest_post",        "forest_post",        "flat",    -10, "surface",   30,  False),
    ("radio_house",        "radio_house",        "flat",     -6, "surface",   30,  False),
    ("factory",            "factory",            "flat",    -10, "surface",   40,  False),
    ("crane",              "crane_mod",          "flat",    -13, "surface",   20,  False),
    ("broadcasting_tower", "broadcasting_tower", "flat",     -9, "surface",   25,  False),
    ("crashed_plane_1",    "crashed_plane_1",    "flat",     -5, "surface",   25,  False),
    ("crashed_plane_2",    "crashed_plane_2",    "flat",     -8, "surface",   25,  False),
    ("desert_shack_1",     "desert_shack_1",     "sandy",    -7, "surface",   18,  False),
    ("desert_shack_2",     "desert_shack_2",     "sandy",    -7, "surface",   20,  False),
    ("desert_shack_3",     "desert_shack_3",     "sandy",    -5, "surface",   22,  False),
    ("dead_dish_small",    "dead_dish_small",    "sandy",    -5, "surface",   15,  False),
    ("tower_base",         "tower_base",         "nonsandy_flat",-6,"surface",30, False),
]
RUINS_WEIGHTS = {"A":10,"B":12,"C":12,"D":12,"E":12,"F":12,"G":12,"H":12,"I":12,"J":12}
for letter, w in RUINS_WEIGHTS.items():
    SINGLES.append(("ntm_ruins_" + letter.lower(), "ntmruins" + letter, "rainy", -1, "surface", w, True))

BIOME_TAGS = {
    "land": ["badlands","bamboo_jungle","beach","birch_forest","cherry_grove","dark_forest","desert",
             "eroded_badlands","flower_forest","forest","frozen_peaks","grove","ice_spikes","jagged_peaks",
             "jungle","meadow","mushroom_fields","old_growth_birch_forest","old_growth_pine_taiga",
             "old_growth_spruce_taiga","plains","savanna","savanna_plateau","snowy_beach","snowy_plains",
             "snowy_slopes","snowy_taiga","sparse_jungle","stony_peaks","stony_shore","sunflower_plains",
             "taiga","windswept_forest","windswept_gravelly_hills","windswept_hills","windswept_savanna",
             "wooded_badlands"],
    "sandy": ["badlands","beach","desert","eroded_badlands","wooded_badlands"],
    "beach": ["beach","snowy_beach","stony_beach"],
    "coast": ["beach","snowy_beach","stony_beach","ocean","deep_ocean","warm_ocean","lukewarm_ocean",
              "deep_lukewarm_ocean","cold_ocean","deep_cold_ocean","frozen_ocean","deep_frozen_ocean"],
    "ocean": ["ocean","deep_ocean","warm_ocean","lukewarm_ocean","deep_lukewarm_ocean","cold_ocean",
              "deep_cold_ocean","frozen_ocean","deep_frozen_ocean"],
    "plains": ["plains","sunflower_plains"],
    "waterside": ["plains","sunflower_plains","swamp","mangrove_swamp"],
    "flat": ["plains","sunflower_plains","snowy_plains","ice_spikes","desert","savanna","savanna_plateau",
             "mushroom_fields","meadow"],
    "nonsandy_flat": ["plains","sunflower_plains","snowy_plains","ice_spikes","savanna","savanna_plateau",
                      "mushroom_fields","meadow"],
    "rainy": ["bamboo_jungle","birch_forest","cherry_grove","dark_forest","flower_forest","forest",
              "frozen_peaks","grove","ice_spikes","jagged_peaks","jungle","meadow","mangrove_swamp",
              "old_growth_birch_forest","old_growth_pine_taiga","old_growth_spruce_taiga","plains",
              "snowy_plains","snowy_slopes","snowy_taiga","sparse_jungle","stony_peaks","sunflower_plains",
              "swamp","taiga","windswept_forest","windswept_gravelly_hills","windswept_hills"],
}

# метеор-данж: кусок -> пул
METEOR_POOL = {
    "meteor-core": "start", "meteor-spike": "spike",
    "meteor-corner": "default", "meteor-t": "default", "meteor-stairs": "default",
    "meteor-fallback": "fallback",
    "room-base-end": "default", "room-base-thru": "default",
    "room-basic": "10room", "room-balcony": "10room", "room-dragon": "10room",
    "room-ladder": "10room", "room-ooze": "10room", "room-split": "10room",
    "room-stairs": "10room", "room-triple": "10room", "room-fallback": "roomback",
    "meteor-3-bale": "3x3loot", "meteor-3-blank": "3x3loot", "meteor-3-block": "3x3loot",
    "meteor-3-crab": "3x3loot", "meteor-3-crab-tesla": "3x3loot", "meteor-3-crate": "3x3loot",
    "meteor-3-dirt": "3x3loot", "meteor-3-lead": "3x3loot", "meteor-3-ooze": "3x3loot",
    "meteor-3-pillar": "3x3loot", "meteor-3-star": "3x3loot", "meteor-3-tesla": "3x3loot",
    "meteor-3-book": "3x3loot", "meteor-3-mku": "3x3loot", "meteor-3-statue": "3x3loot",
    "meteor-3-glow": "3x3loot",
    "loot-chest": "headloot", "loot-tesla": "headloot", "loot-trap": "headloot",
    "loot-crate-crab": "headloot", "loot-fallback": "headback",
}
METEOR_TEMPLATE_POOL = {  # имя пула -> [(кусок, вес)...], fallback
    "meteor_start":   ([("meteor/meteor-core", 1)], None),
    "meteor_spike":   ([("meteor/meteor-spike", 1)], None),
    "meteor_default": ([("meteor/meteor-corner",2),("meteor/meteor-t",3),("meteor/meteor-stairs",1),
                        ("meteor/room-base-thru",3),("meteor/room-base-end",4)], "hbm_m:meteor_fallback"),
    "meteor_fallback":([("meteor/meteor-fallback",1)], None),
    "meteor_10room":  ([("meteor/room-basic",1),("meteor/room-balcony",1),("meteor/room-dragon",1),
                        ("meteor/room-ladder",1),("meteor/room-ooze",1),("meteor/room-split",1),
                        ("meteor/room-stairs",1),("meteor/room-triple",1)], "hbm_m:meteor_roomback"),
    "meteor_roomback":([("meteor/room-fallback",1)], None),
    "meteor_3x3loot": ([("meteor/loot3x3/meteor-3-%s" % n, 1) for n in
                        ["bale","blank","block","crab","crab-tesla","crate","dirt","lead","ooze",
                         "pillar","star","tesla","book","mku","statue","glow"]], "hbm_m:meteor_3x3loot"),
    "meteor_headloot":([("meteor/room10/headloot/loot-chest",1),("meteor/room10/headloot/loot-tesla",1),
                        ("meteor/room10/headloot/loot-trap",1),("meteor/room10/headloot/loot-crate-crab",1)],
                       "hbm_m:meteor_headback"),
    "meteor_headback":([("meteor/room10/headloot/loot-fallback",1)], None),
}

# ---------------------------------------------------------------------------
# Конвертация одного файла
# ---------------------------------------------------------------------------
SKIP_FILES = {"crane.nbt", "test-rot.nbt", "test-jigsaw.nbt", "test-jigsaw-core.nbt",
              "test-jigsaw-hall.nbt", "test-tandem-core.nbt", "test-tandem.nbt"}

def blockstate_string(name, props):
    if not props:
        return name
    return name + "[" + ",".join("%s=%s" % kv for kv in sorted(props.items())) + "]"


# ---------------------------------------------------------------------------
# Таблица структур (из SpawnCondition в NTMWorldGenerator)
#   (json-имя, файл, тег биомов, высота, режим, вес, conform)
#   высота: int -> смещение от поверхности; (min,max) -> абсолютный uniform
#   режим: surface | oceanfloor | absolute
# ---------------------------------------------------------------------------
SINGLES = [
    ("spire",              "spire",              "flat",     -1, "surface",    2,  False),
    ("vertibird",          "vertibird",          "sandy",    -3, "surface",    6,  False),
    ("crashed_vertibird",  "crashed-vertibird",  "sandy",   -10, "surface",   10,  False),
    ("beached_patrol",     "beached_patrol",     "beach",  (58,67), "absolute",15, False),
    ("aircraft_carrier",   "aircraft_carrier",   "ocean",    -6, "oceanfloor", 3,  False),
    ("oil_rig",            "oil_rig",            "ocean",  (11,12), "absolute", 5,  False),
    ("lighthouse",         "lighthouse",         "coast",  (28,29), "absolute", 4,  False),
    ("dish",               "dish",               "plains", (53,65), "absolute",20,  False),
    ("water_pump",         "water_pump",         "waterside",-10, "surface",  15,  False),
    ("forest_chem",        "forest_chem",        "flat",     -9, "surface",   30,  False),
    ("laboratory",         "laboratory",         "flat",   (53,65), "absolute",20,  False),
    ("forest_post",        "forest_post",        "flat",    -10, "surface",   30,  False),
    ("radio_house",        "radio_house",        "flat",     -6, "surface",   30,  False),
    ("factory",            "factory",            "flat",    -10, "surface",   40,  False),
    ("crane",              "crane_mod",          "flat",    -13, "surface",   20,  False),
    ("broadcasting_tower", "broadcasting_tower", "flat",     -9, "surface",   25,  False),
    ("crashed_plane_1",    "crashed_plane_1",    "flat",     -5, "surface",   25,  False),
    ("crashed_plane_2",    "crashed_plane_2",    "flat",     -8, "surface",   25,  False),
    ("desert_shack_1",     "desert_shack_1",     "sandy",    -7, "surface",   18,  False),
    ("desert_shack_2",     "desert_shack_2",     "sandy",    -7, "surface",   20,  False),
    ("desert_shack_3",     "desert_shack_3",     "sandy",    -5, "surface",   22,  False),
    ("dead_dish_small",    "dead_dish_small",    "sandy",    -5, "surface",   15,  False),
    ("tower_base",         "tower_base",         "nonsandy_flat",-6,"surface",30, False),
]
RUINS_WEIGHTS = {"A":10,"B":12,"C":12,"D":12,"E":12,"F":12,"G":12,"H":12,"I":12,"J":12}
for letter, w in RUINS_WEIGHTS.items():
    SINGLES.append(("ntm_ruins_" + letter.lower(), "ntmruins" + letter, "rainy", -1, "surface", w, True))

BIOME_TAGS = {
    "land": ["badlands","bamboo_jungle","beach","birch_forest","cherry_grove","dark_forest","desert",
             "eroded_badlands","flower_forest","forest","frozen_peaks","grove","ice_spikes","jagged_peaks",
             "jungle","meadow","mushroom_fields","old_growth_birch_forest","old_growth_pine_taiga",
             "old_growth_spruce_taiga","plains","savanna","savanna_plateau","snowy_beach","snowy_plains",
             "snowy_slopes","snowy_taiga","sparse_jungle","stony_peaks","stony_shore","sunflower_plains",
             "taiga","windswept_forest","windswept_gravelly_hills","windswept_hills","windswept_savanna",
             "wooded_badlands"],
    "sandy": ["badlands","beach","desert","eroded_badlands","wooded_badlands"],
    "beach": ["beach","snowy_beach","stony_beach"],
    "coast": ["beach","snowy_beach","stony_beach","ocean","deep_ocean","warm_ocean","lukewarm_ocean",
              "deep_lukewarm_ocean","cold_ocean","deep_cold_ocean","frozen_ocean","deep_frozen_ocean"],
    "ocean": ["ocean","deep_ocean","warm_ocean","lukewarm_ocean","deep_lukewarm_ocean","cold_ocean",
              "deep_cold_ocean","frozen_ocean","deep_frozen_ocean"],
    "plains": ["plains","sunflower_plains"],
    "waterside": ["plains","sunflower_plains","swamp","mangrove_swamp"],
    "flat": ["plains","sunflower_plains","snowy_plains","ice_spikes","desert","savanna","savanna_plateau",
             "mushroom_fields","meadow"],
    "nonsandy_flat": ["plains","sunflower_plains","snowy_plains","ice_spikes","savanna","savanna_plateau",
                      "mushroom_fields","meadow"],
    "rainy": ["bamboo_jungle","birch_forest","cherry_grove","dark_forest","flower_forest","forest",
              "frozen_peaks","grove","ice_spikes","jagged_peaks","jungle","meadow","mangrove_swamp",
              "old_growth_birch_forest","old_growth_pine_taiga","old_growth_spruce_taiga","plains",
              "snowy_plains","snowy_slopes","snowy_taiga","sparse_jungle","stony_peaks","sunflower_plains",
              "swamp","taiga","windswept_forest","windswept_gravelly_hills","windswept_hills"],
}

# метеор-данж: кусок -> пул
METEOR_POOL = {
    "meteor-core": "start", "meteor-spike": "spike",
    "meteor-corner": "default", "meteor-t": "default", "meteor-stairs": "default",
    "meteor-fallback": "fallback",
    "room-base-end": "default", "room-base-thru": "default",
    "room-basic": "10room", "room-balcony": "10room", "room-dragon": "10room",
    "room-ladder": "10room", "room-ooze": "10room", "room-split": "10room",
    "room-stairs": "10room", "room-triple": "10room", "room-fallback": "roomback",
    "meteor-3-bale": "3x3loot", "meteor-3-blank": "3x3loot", "meteor-3-block": "3x3loot",
    "meteor-3-crab": "3x3loot", "meteor-3-crab-tesla": "3x3loot", "meteor-3-crate": "3x3loot",
    "meteor-3-dirt": "3x3loot", "meteor-3-lead": "3x3loot", "meteor-3-ooze": "3x3loot",
    "meteor-3-pillar": "3x3loot", "meteor-3-star": "3x3loot", "meteor-3-tesla": "3x3loot",
    "meteor-3-book": "3x3loot", "meteor-3-mku": "3x3loot", "meteor-3-statue": "3x3loot",
    "meteor-3-glow": "3x3loot",
    "loot-chest": "headloot", "loot-tesla": "headloot", "loot-trap": "headloot",
    "loot-crate-crab": "headloot", "loot-fallback": "headback",
}
METEOR_TEMPLATE_POOL = {  # имя -> [(кусок, вес)...], fallback
    "start":    ([("meteor/meteor-core", 1)], None),
    "spike":    ([("meteor/meteor-spike", 1)], None),
    "default":  ([("meteor/meteor-corner",2),("meteor/meteor-t",3),("meteor/meteor-stairs",1),
                  ("meteor/room-base-thru",3),("meteor/room-base-end",4)], "hbm_m:meteor_fallback"),
    "fallback": ([("meteor/meteor-fallback",1)], None),
    "10room":   ([("meteor/room-basic",1),("meteor/room-balcony",1),("meteor/room-dragon",1),
                  ("meteor/room-ladder",1),("meteor/room-ooze",1),("meteor/room-split",1),
                  ("meteor/room-stairs",1),("meteor/room-triple",1)], "hbm_m:meteor_roomback"),
    "roomback": ([("meteor/room-fallback",1)], None),
    "3x3loot":  ([("meteor/loot3x3/meteor-3-%s" % n, 1) for n in
                  ["bale","blank","block","crab","crab-tesla","crate","dirt","lead","ooze",
                   "pillar","star","tesla","book","mku","statue","glow"]], "hbm_m:meteor_3x3loot"),
    "headloot": ([("meteor/room10/headloot/loot-chest",1),("meteor/room10/headloot/loot-tesla",1),
                  ("meteor/room10/headloot/loot-trap",1),("meteor/room10/headloot/loot-crate-crab",1)],
                 "hbm_m:meteor_headback"),
    "headback": ([("meteor/room10/headloot/loot-fallback",1)], None),
}

# ---------------------------------------------------------------------------
# Конвертация одного файла
# ---------------------------------------------------------------------------
SKIP_FILES = {"crane.nbt", "test-rot.nbt", "test-jigsaw.nbt", "test-jigsaw-core.nbt",
              "test-jigsaw-hall.nbt", "test-tandem-core.nbt", "test-tandem.nbt"}

def blockstate_string(name, props):
    if not props:
        return name
    return name + "[" + ",".join("%s=%s" % kv for kv in sorted(props.items())) + "]"

def convert_structure(path, meteor_piece=None):
    root = nbt_load(path)
    size = root["size"]
    palette = root.get("palette", [])
    blocks = root.get("blocks", [])
    item_palette = {}
    for it in root.get("itemPalette", []) or []:
        item_palette[it.get("ID")] = it.get("Name")

    new_palette = []
    pal_index = {}
    def pal_id(name, props):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in pal_index:
            pal_index[key] = len(new_palette)
            e = {"Name": name}
            if props:
                e["Properties"] = {k: str(v) for k, v in props.items()}
            new_palette.append(e)
        return pal_index[key]

    new_blocks = []
    for b in blocks:
        pal = palette[b["state"]]
        try:
            meta = int(pal.get("Properties", {}).get("meta", "0"))
        except (TypeError, ValueError):
            meta = 0
        pos = b["pos"]
        nbt = b.get("nbt")
        te_id = nbt.get("id") if nbt else None
        name, props = convert_state(pal["Name"], meta)
        new_nbt = None

        if te_id == "tileentity_wand_loot":
            target = nbt.get("block")
            if isinstance(target, int):
                target = BLOCK_ID_GUESS.get(target)
                if target is None:
                    warn("wand_loot: числовой id блока %s неизвестен" % nbt.get("block"))
                    continue
            tname, tprops = convert_state(target, nbt.get("meta", 0))
            pool = POOL_TABLES.get(nbt.get("pool", ""))
            if pool is None:
                warn("лут-пул %r не найден" % nbt.get("pool"))
                pool = "hbm_m:loot_pile_common"
            be = CONTAINER_BE.get(tname)
            if be:
                new_nbt = {"id": be, "LootTable": pool}
            name, props = tname, tprops
        elif te_id == "tileentity_wand_spawner":
            action = nbt.get("actionID", "")
            disguise = nbt.get("disguise")
            if action.startswith("ZOMBIE"):
                entity = "minecraft:zombie"
            elif action.startswith("SKELETON") or action.startswith("DEAD_GUY"):
                entity = "minecraft:skeleton"
            else:
                entity = None
            if disguise:
                dname, dprops = convert_state(disguise, nbt.get("disguiseMeta", 0))
                name, props = dname, dprops
            elif entity:
                name = "minecraft:spawner"
                props = {}
                new_nbt = {"id": "minecraft:mob_spawner",
                           "SpawnData": {"entity": {"id": entity}},
                           "SpawnPotentials": [{"weight": 1, "data": {"entity": {"id": entity}}}],
                           "MinSpawnDelay": 200, "MaxSpawnDelay": 800,
                           "SpawnCount": 4, "MaxNearbyEntities": 6, "RequiredPlayerRange": 16,
                           "SpawnRange": 4}
            else:
                continue  # снос (COLLAPSE_*, POWER_LOCK, BOMB_CRANE)
        elif te_id == "tileentity_wand_tandem":
            continue
        elif te_id == "tileentity_wand_jigsaw" and meteor_piece:
            replace = nbt.get("block", "minecraft:air")
            rmeta = nbt.get("meta", 0)
            rname, rprops = convert_state(replace if isinstance(replace, str) else "minecraft:air", rmeta)
            own = "hbm_m:meteor_" + METEOR_POOL[meteor_piece]
            d = nbt.get("direction", 2)
            orientation = ("up_north" if d == 1 else
                           "down_south" if d == 0 else
                           FORGE_DIR_TO_VAN.get(d, "north") + "_up")
            new_nbt = {
                "id": "minecraft:jigsaw",
                "name": nbt.get("target", "default"),
                "target": "hbm_m:meteor_" + nbt.get("pool", "default"),
                "pool": own,
                "joint": "rollable" if nbt.get("roll") else "aligned",
                "final_state": blockstate_string(rname, rprops),
                "orientation": orientation,
            }
            name = "minecraft:jigsaw"
            props = {}
        elif te_id == "Chest":
            chest_state, chest_nbt = convert_chest("minecraft:chest", meta, nbt, item_palette)
            name = chest_state["name"]; props = chest_state["Properties"]
            new_nbt = chest_nbt
        elif te_id == "tileentity_crate_iron":
            new_nbt = {"id": "hbm_m:iron_crate_be"}
        elif te_id == "tileentity_supply_crate":
            new_nbt = {"id": "hbm_m:steel_crate_be", "LootTable": "hbm_m:chests/supply_drop"}
        elif te_id == "tileentity_landmine":
            new_nbt = {"id": "hbm_m:mine_block_entity"}
        elif te_id is not None:
            warn("TE %s отброшен (блок сохранён)" % te_id)

        new_blocks.append({
            "state": pal_id(name, props),
            "pos": pos,
            **({"nbt": new_nbt} if new_nbt else {})
        })

    return {
        "DataVersion": DATA_VERSION,
        "size": size,
        "entities": [],
        "palette": new_palette,
        "blocks": new_blocks,
    }

def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")

def main():
    files = []
    for dp, dn, fn in os.walk(SRC):
        for f in sorted(fn):
            if f.endswith(".nbt"):
                p = os.path.join(dp, f)
                rel = pathlib.Path(p).as_posix().split("/structures/")[-1]
                files.append((rel, p))
    files.sort()
    print("файлов: %d" % len(files))

    converted = {}
    for rel, p in files:
        fname = rel.split("/")[-1]
        if fname in SKIP_FILES:
            warn("файл пропущен (не используется/битый): " + rel)
            continue
        meteor_piece = None
        if rel.startswith("meteor/"):
            meteor_piece = fname[:-4]
        try:
            converted[rel] = convert_structure(p, meteor_piece)
        except Exception as e:
            warn("ОШИБКА конвертации %s: %r" % (rel, e))

    # --- NBT ---
    out_dir = DST / "structures"
    out_dir.mkdir(parents=True, exist_ok=True)
    for rel, obj in converted.items():
        tgt = out_dir / rel
        tgt.parent.mkdir(parents=True, exist_ok=True)
        tgt.write_bytes(nbt_write(obj))

    # --- одиночные структуры ---
    wg = DST / "worldgen"
    for name, file, tag, height, mode, weight, conform in SINGLES:
        struct_json = {
            "type": "minecraft:jigsaw",
            "biomes": "#hbm_m:has_structure/" + tag,
            "spawn_overrides": {},
            "step": "surface_structures",
            "terrain_adaptation": "beard_thin" if conform else "none",
            "start_pool": NS + ":" + name + "_pool",
            "size": 1,
            "use_expansion_hack": False,
            "max_distance_from_center": 116,
        }
        if isinstance(height, tuple):
            struct_json["start_height"] = {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": height[0]},
                "max_inclusive": {"absolute": height[1]}}
        else:
            struct_json["start_height"] = {"absolute": height}
            struct_json["project_start_to_heightmap"] = (
                "OCEAN_FLOOR_WG" if mode == "oceanfloor" else "WORLD_SURFACE_WG")
        write_json(wg / "structure" / (name + ".json"), struct_json)

        # Частота появления порезана в 5 раз: для random_spread плотность ~ 1/spacing^2,
        # поэтому после генерации все structure_set масштабируются на sqrt(5) ~ 2.236
        # (см. tools/structure_converter/scale_structure_sets.py)
        spacing = max(18, min(64, round(500.0 / weight)))
        if tag in ("ocean", "coast"):
            spacing = max(24, min(80, round(800.0 / weight)))
        sep = max(3, spacing // 4)
        set_json = {
            "structures": [{"structure": NS + ":" + name, "weight": 1}],
            "placement": {
                "type": "minecraft:random_spread",
                "salt": zlib.crc32(("hbm_struct_" + name).encode()) & 0x7FFFFFFF,
                "spacing": spacing,
                "separation": sep,
            },
        }
        write_json(wg / "structure_set" / (name + ".json"), set_json)

        pool_json = {
            "fallback": "minecraft:empty",
            "elements": [{
                "weight": 1,
                "element": {
                    "element_type": "minecraft:single_pool_element",
                    "projection": "rigid",
                    "location": NS + ":" + file,
                    "processors": "hbm_m:foundation_processor" if conform else "minecraft:empty",
                },
            }],
        }
        write_json(wg / "template_pool" / (name + "_pool.json"), pool_json)

    # --- метеор-данж ---
    if any(r.startswith("meteor/") for r in converted):
        struct_json = {
            "type": "minecraft:jigsaw",
            "biomes": "#hbm_m:has_structure/land",
            "spawn_overrides": {},
            "step": "underground_structures",
            "terrain_adaptation": "none",
            "start_pool": NS + ":meteor_start",
            "size": 7,   # максимум ванильного jigsaw: 0..7
            "start_height": {"absolute": 32},
            "use_expansion_hack": False,
            "max_distance_from_center": 128,
        }
        write_json(wg / "structure" / "meteor_dungeon.json", struct_json)
        set_json = {
            "structures": [{"structure": NS + ":meteor_dungeon", "weight": 1}],
            "placement": {"type": "minecraft:random_spread",
                          "salt": zlib.crc32(b"hbm_struct_meteor_dungeon") & 0x7FFFFFFF,
                          "spacing": 48, "separation": 12},
        }
        write_json(wg / "structure_set" / "meteor_dungeon.json", set_json)
        for pool, (elements, fallback) in METEOR_TEMPLATE_POOL.items():
            pj = {"fallback": fallback or "minecraft:empty",
                  "elements": [{"weight": w, "element": {
                      "element_type": "minecraft:single_pool_element",
                      "projection": "rigid",
                      "location": NS + ":" + loc,
                      "processors": "minecraft:empty"}} for loc, w in elements]}
            write_json(wg / "template_pool" / ("meteor_" + pool + ".json"), pj)

    # --- теги биомов ---
    for tag, biomes in BIOME_TAGS.items():
        write_json(DST / "tags" / "worldgen" / "biome" / "has_structure" / (tag + ".json"),
                   {"replace": False, "values": ["minecraft:" + b for b in biomes]})

    # --- отчёт ---
    DOCS.mkdir(exist_ok=True)
    with open(DOCS / "structures_port_report.md", "w", encoding="utf-8") as f:
        f.write("# Отчёт: портирование структур 1.7.10 -> 1.20.1/1.21.1\n\n")
        f.write("Конвертер: `tools/structure_converter/convert.py` (запуск из корня репозитория).\n\n")
        f.write("## Конвертированные файлы\n\n")
        for rel in sorted(converted):
            f.write("* `%s`\n" % rel)
        f.write("\n## Предупреждения/замены\n\n")
        for w in warnings:
            f.write("* %s\n" % w)
    print("предупреждений: %d" % len(warnings))
    for w in sorted(warnings)[:80]:
        print("  !", w)

if __name__ == "__main__":
    main()
