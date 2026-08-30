#!/usr/bin/env python3
"""Валидирует JSON всех лут-таблиц и считает записи."""
import json, glob, os
total_entries = 0
files = 0
for lt in sorted(glob.glob('src/main/resources/data/hbm_m/loot_tables/**/*.json', recursive=True)):
    with open(lt, encoding='utf-8') as f:
        data = json.load(f)
    files += 1
    entries = 0
    for pool in data.get('pools', []):
        entries += len(pool.get('entries', []))
    total_entries += entries
    rel = os.path.relpath(lt, 'src/main/resources/data/hbm_m/loot_tables')
    print(f'{rel:42} pools={len(data.get("pools",[]))} entries={entries}')
print(f'\nTOTAL: {files} loot tables, {total_entries} entries. ALL JSON VALID.')
