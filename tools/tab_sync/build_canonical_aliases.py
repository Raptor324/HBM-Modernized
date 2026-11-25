#!/usr/bin/env python3
"""
build_canonical_aliases.py

Builds a consolidated, definitive canonical_tab_aliases.json by fusing:
1. Ground truth 1.7.10 creative tabs (tab*.json) from Hbm-s-Nuclear-Tech-GIT
2. The 9 audited discrepancy registries in .agents/teamwork/tab_discrepancies/
3. Parsed statements from CreativeModeTabEventHandler.java
"""

import json
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
DEFAULT_ORIG_DIR = Path(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT\eclipse\creative_tabs")
DISCREPANCIES_DIR = REPO_ROOT / ".agents" / "teamwork" / "tab_discrepancies"
HANDLER_PATH = REPO_ROOT / "src" / "main" / "java" / "com" / "hbm_m" / "main" / "CreativeModeTabEventHandler.java"
OUTPUT_FILE = Path(__file__).resolve().parent / "canonical_tab_aliases.json"

TAB_CONFIG = {
    "tabParts": {
        "orig_file": "tabParts.json",
        "disc_file": "ntm_parts_tab_discrepancies.json",
        "method": "populatePartsTab",
        "hbm_m_tab": "hbm_m:parts_tab"
    },
    "tabControl": {
        "orig_file": "tabControl.json",
        "disc_file": "ntm_control_tab_discrepancies.json",
        "method": "populateControlTab",
        "hbm_m_tab": "hbm_m:control_tab"
    },
    "tabTemplate": {
        "orig_file": "tabTemplate.json",
        "disc_file": "ntm_template_tab_discrepancies.json",
        "method": "populateTemplatesTab",
        "hbm_m_tab": "hbm_m:templates_tab"
    },
    "tabBlocks": {
        "orig_file": "tabBlocks.json",
        "disc_file": "ntm_blocks_tab_discrepancies.json",
        "method": "populateBlocksTab",
        "hbm_m_tab": "hbm_m:blocks_tab"
    },
    "tabMachine": {
        "orig_file": "tabMachine.json",
        "disc_file": "ntm_machine_tab_discrepancies.json",
        "method": "populateMachinesTab",
        "hbm_m_tab": "hbm_m:machines_tab"
    },
    "tabNuke": {
        "orig_file": "tabNuke.json",
        "disc_file": "ntm_nuke_tab_discrepancies.json",
        "method": "populateNukeTab",
        "hbm_m_tab": "hbm_m:nuke_tab"
    },
    "tabMissile": {
        "orig_file": "tabMissile.json",
        "disc_file": "ntm_missile_tab_discrepancies.json",
        "method": "populateMissilesTab",
        "hbm_m_tab": "hbm_m:missiles_tab"
    },
    "tabWeapon": {
        "orig_file": "tabWeapon.json",
        "disc_file": "ntm_weapon_tab_discrepancies.json",
        "method": "populateWeaponsTab",
        "hbm_m_tab": "hbm_m:weapons_tab"
    },
    "tabConsumable": {
        "orig_file": "tabConsumable.json",
        "disc_file": "ntm_consumable_tab_discrepancies.json",
        "method": "populateConsumablesTab",
        "hbm_m_tab": "hbm_m:consumables_tab"
    }
}

def extract_handler_methods(handler_text):
    """Parses CreativeModeTabEventHandler.java to extract each populate method body."""
    method_pattern = re.compile(
        r'public\s+static\s+void\s+(populate\w+)\s*\([^)]*\)\s*\{',
        re.MULTILINE
    )
    matches = list(method_pattern.finditer(handler_text))
    methods = {}
    for i, m in enumerate(matches):
        m_name = m.group(1)
        start_idx = m.end()
        # Find end brace matching this method
        if i + 1 < len(matches):
            end_idx = matches[i + 1].start()
            # find last closing brace before next method
            body = handler_text[start_idx:end_idx]
            last_brace = body.rfind('}')
            if last_brace != -1:
                body = body[:last_brace]
        else:
            # last method
            body = handler_text[start_idx:]
            last_brace = body.rfind('}')
            if last_brace != -1:
                body = body[:last_brace]
        
        # Calculate line numbers
        start_line = handler_text[:start_idx].count('\n') + 1
        methods[m_name] = {
            "start_line": start_line,
            "body": body
        }
    return methods

def main():
    orig_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_ORIG_DIR
    if not orig_dir.exists():
        print(f"Error: Original tabs directory not found: {orig_dir}")
        sys.exit(1)

    print(f"Loading original tabs from: {orig_dir}")
    print(f"Loading discrepancies from: {DISCREPANCIES_DIR}")
    print(f"Loading handler from: {HANDLER_PATH}")

    with open(HANDLER_PATH, "r", encoding="utf-8") as f:
        handler_text = f.read()

    methods = extract_handler_methods(handler_text)

    master_database = {
        "version": "1.0.0",
        "description": "Canonical item mapping and alias registry between 1.7.10 creative tabs and HBM-Modernized",
        "tabs": {},
        "global_alias_lookup": {}, # "hbm:orig_id#meta" -> target
        "unported_items": []
    }

    total_items_processed = 0
    total_discrepancies_processed = 0

    for tab_key, cfg in TAB_CONFIG.items():
        orig_file = orig_dir / cfg["orig_file"]
        disc_file = DISCREPANCIES_DIR / cfg["disc_file"]

        if not orig_file.exists():
            print(f"Warning: {orig_file} does not exist!")
            continue

        with open(orig_file, "r", encoding="utf-8") as f:
            orig_data = json.load(f)

        disc_map = {}
        if disc_file.exists():
            with open(disc_file, "r", encoding="utf-8") as f:
                disc_data = json.load(f)
                for d in disc_data.get("discrepancies", []):
                    disc_map[d["expected_order"]] = d

        items_list = []
        tab_items = orig_data.get("items", [])
        total_items_processed += len(tab_items)
        total_discrepancies_processed += len(disc_map)

        for item in tab_items:
            order = item["order"]
            orig_id = item["id"]
            meta = item.get("meta", 0)
            name = item.get("name", "")
            key = f"{orig_id}#{meta}"

            item_entry = {
                "order": order,
                "orig_id": orig_id,
                "meta": meta,
                "numeric_id": item.get("numeric_id"),
                "name": name,
                "status": "DIRECT_MATCH",
                "discrepancy_type": None,
                "modernized_registry_id": None,
                "modernized_statement": None,
                "code_location": None
            }

            if order in disc_map:
                d = disc_map[order]
                dtype = d.get("discrepancy_type")
                cur = d.get("modernized_current", {})
                item_entry["status"] = dtype
                item_entry["discrepancy_type"] = dtype
                item_entry["modernized_registry_id"] = cur.get("registry_id")
                item_entry["modernized_statement"] = cur.get("code_statement")
                item_entry["code_location"] = cur.get("code_location")

                if dtype in ("UNPORTED", "MISSING_IN_PORT"):
                    master_database["unported_items"].append({
                        "tab": tab_key,
                        "order": order,
                        "orig_id": orig_id,
                        "meta": meta,
                        "name": name,
                        "type": dtype,
                        "comment": cur.get("code_statement")
                    })
            else:
                # Direct match: default mapping
                # e.g., hbm:tile.foo -> hbm_m:foo or hbm:item.bar -> hbm_m:bar
                # Let's derive clean registry_id
                clean_id = orig_id
                if clean_id.startswith("hbm:"):
                    clean_id = clean_id[4:]
                if clean_id.startswith("tile."):
                    clean_id = clean_id[5:]
                elif clean_id.startswith("item."):
                    clean_id = clean_id[5:]
                
                reg_id = f"hbm_m:{clean_id}"
                item_entry["modernized_registry_id"] = reg_id

            items_list.append(item_entry)

            # Store in global lookup
            if item_entry["modernized_registry_id"]:
                master_database["global_alias_lookup"][key] = {
                    "registry_id": item_entry["modernized_registry_id"],
                    "status": item_entry["status"],
                    "tab": tab_key
                }

        master_database["tabs"][tab_key] = {
            "tab_name": tab_key,
            "hbm_m_tab": cfg["hbm_m_tab"],
            "method": cfg["method"],
            "total_items": len(items_list),
            "discrepancies_count": len(disc_map),
            "items": items_list
        }

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(master_database, f, indent=2, ensure_ascii=False)

    print(f"\nSuccessfully compiled canonical aliases!")
    print(f"Total tabs: {len(master_database['tabs'])}")
    print(f"Total items accounted: {total_items_processed}")
    print(f"Total discrepancies mapped: {total_discrepancies_processed}")
    print(f"Total unported items: {len(master_database['unported_items'])}")
    print(f"Saved database to: {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
