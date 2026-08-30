#!/usr/bin/env python3
"""Проверяет все item-ID из лут-таблиц против реестра мода + ванильных предметов."""
import json, os, glob, re

# Загружаем ключи мода
mod_keys = set(l.strip() for l in open('tools/_items_clean.txt', encoding='utf-8') if l.strip())

# Извлекаем все ID из en_us.json (и items, и blocks) -> набор 'name' (без префикса)
lang = json.load(open('src/generated/resources/assets/hbm_m/lang/en_us.json', encoding='utf-8'))
mod_ids = set()
for k in lang:
    if k.startswith('item.hbm_m.'):
        mod_ids.add(k.split('.', 2)[2])
    elif k.startswith('block.hbm_m.'):
        mod_ids.add(k.split('.', 2)[2])

# Ванильные предметы, которые могут встречаться
vanilla = {
    'minecraft:bread','minecraft:iron_ingot','minecraft:gold_ingot','minecraft:redstone',
    'minecraft:coal','minecraft:paper','minecraft:book','minecraft:clock',
    'minecraft:experience_bottle','minecraft:glass_bottle','minecraft:gunpowder',
    'minecraft:diamond','minecraft:emerald','minecraft:lapis_lazuli','minecraft:quartz',
    'minecraft:flint','minecraft:string','minecraft:leather','minecraft:feather',
    'minecraft:stick','minecraft:wheat','minecraft:apple','minecraft:bucket',
    'minecraft:water_bucket','minecraft:lava_bucket','minecraft:gold_nugget',
    'minecraft:iron_nugget','minecraft:clay_ball','minecraft:brick','minecraft:nether_brick',
    'minecraft:ender_pearl','minecraft:blaze_rod','minecraft:bone','minecraft:arrow',
}

# Проходим по всем лут-таблицам
problems = []
all_refs = {}
for lt in glob.glob('src/main/resources/data/hbm_m/loot_tables/**/*.json', recursive=True):
    with open(lt, encoding='utf-8') as f:
        text = f.read()
    # извлекаем все "name": "..." значения
    names = re.findall(r'"name"\s*:\s*"([^"]+)"', text)
    rel = os.path.relpath(lt, 'src/main/resources/data/hbm_m/loot_tables')
    for n in names:
        all_refs.setdefault(n, []).append(rel)
        if n.startswith('minecraft:'):
            if n not in vanilla:
                problems.append(f'{rel}: vanilla MISS {n}')
        elif n.startswith('hbm_m:'):
            idname = n.split(':', 1)[1]
            if idname not in mod_ids:
                problems.append(f'{rel}: MOD MISS {n}  (id="{idname}")')
        else:
            problems.append(f'{rel}: unknown namespace {n}')

print('=== UNIQUE ITEM REFS:', len(all_refs), '===')
if problems:
    print('!!! PROBLEMS (' + str(len(problems)) + '):')
    for p in problems:
        print('  ', p)
else:
    print('ALL GOOD - every referenced item exists.')
