import re
import json
import os
from collections import defaultdict

LOG_FILE = r'C:\Projects\HBM-Modernized\compile.log'
MD_OUTPUT = r'C:\Projects\HBM-Modernized\scripts\report\errors_report.md'
JSON_OUTPUT = r'C:\Projects\HBM-Modernized\scripts\report\errors_report.json'
CLASSES_JSON_OUTPUT = r'C:\Projects\HBM-Modernized\scripts\report\top_classes_errors.json'

def categorize_error(message, details):
    msg = message.lower()
    det = details.lower()
    
    # 1. Отсутствие классов, методов, пакетов
    if "cannot find symbol" in msg:
        if "method" in det: return "Missing Symbol (Method)"
        if "class" in det: return "Missing Symbol (Class)"
        if "variable" in det: return "Missing Symbol (Variable)"
        return "Missing Symbol (General)"
    if "package" in msg and "does not exist" in msg:
        if "minecraftforge" in msg: return "Removed/Renamed Package (Forge -> NeoForge)"
        return "Missing Package"
        
    # 2. Сигнатуры и переопределения
    if "method does not override or implement" in msg: return "Broken @Override (Signature Changed/Removed)"
    if "is not abstract and does not override abstract method" in msg: return "Missing Interface/Abstract Method Implementation"
    if "cannot override" in msg: return "Override Error (Return Type / Access Modifier)"
    
    # 3. Несовпадение типов
    if "incompatible types" in msg:
        if "holder" in msg: return "Incompatible Types (Direct Object vs Holder<T>)"
        if "compoundtag cannot be converted to provider" in msg: return "NBT Serialization (Missing HolderLookup.Provider)"
        if "mapcodec" in msg: return "Codec/MapCodec Issues"
        return "Incompatible Types (General)"
        
    # 4. Проблемы с вызовом методов и конструкторов
    if "no suitable method found" in msg:
        if "putbulkdata" in msg: return "Rendering: putBulkData Signature Changed"
        if "getcapability" in msg: return "Capabilities System Rewrite (NeoForge)"
        return "Method Overload Resolution Failed"
    if "cannot be applied to given types" in msg:
        if "saveadditional" in msg or "load" in msg: return "BlockEntity save/load signature (Needs Provider)"
        return "Method/Constructor Parameters Changed"
        
    # 5. Остальное
    if "has private access" in msg: return "Access Modifier Issue (Private/Protected)"
    if "warning: [removal]" in msg: return "Deprecated API Usage"
    if "reference to" in msg and "is ambiguous" in msg: return "Ambiguous Method Reference"
    
    return "Other / Uncategorized"

def parse_log(file_path):
    pattern = re.compile(r"^(?P<file>.*\.java):(?P<line>\d+): (?P<severity>error|warning): (?P<message>.*)$")
    
    errors = []
    current_error = None
    
    with open(file_path, 'r', encoding='utf-8', errors='replace') as f:
        for line in f:
            line = line.rstrip()
            
            # Игнорируем итоговый блок Gradle, где он дублирует все ошибки заново
            if "FAILURE: Build failed" in line or "* What went wrong:" in line:
                break
                
            match = pattern.match(line)
            
            if match:
                if current_error:
                    errors.append(current_error)
                
                filepath = match.group('file')
                if "src\\main\\java\\" in filepath:
                    filepath = filepath.split("src\\main\\java\\")[-1]
                
                current_error = {
                    "file": filepath,
                    "line": match.group('line'),
                    "severity": match.group('severity'),
                    "message": match.group('message'),
                    "details": "",
                    "category": ""
                }
            elif current_error and line.strip() and not line.startswith("> Task"):
                current_error["details"] += line + "\n"
                
        if current_error:
            errors.append(current_error)
            
    for err in errors:
        err["category"] = categorize_error(err["message"], err["details"])
        
    return errors

def aggregate_errors(errors):
    catalog = defaultdict(lambda: defaultdict(list))
    for err in errors:
        loc = f"{err['file']}:{err['line']}"
        catalog[err['category']][err['message']].append(loc)
    return catalog

def aggregate_by_class(errors):
    """Группирует ошибки по классам/файлам и возвращает отсортированный список по убыванию количества ошибок."""
    class_map = defaultdict(lambda: {
        "total_errors": 0,
        "categories": defaultdict(int),
        "errors": []
    })
    
    for err in errors:
        filepath = err["file"]
        class_map[filepath]["total_errors"] += 1
        class_map[filepath]["categories"][err["category"]] += 1
        class_map[filepath]["errors"].append({
            "line": err["line"],
            "severity": err["severity"],
            "category": err["category"],
            "message": err["message"]
        })
        
    result = []
    for filepath, data in class_map.items():
        result.append({
            "file": filepath,
            "total_errors": data["total_errors"],
            "categories": dict(data["categories"]),
            "errors": data["errors"]
        })
        
    # Сортировка классов от большего числа ошибок к меньшему
    result.sort(key=lambda x: x["total_errors"], reverse=True)
    return result

def write_reports(catalog, top_classes):
    # Автоматически создаем папку для отчетов, если её еще нет
    os.makedirs(os.path.dirname(JSON_OUTPUT), exist_ok=True)
    os.makedirs(os.path.dirname(MD_OUTPUT), exist_ok=True)
    os.makedirs(os.path.dirname(CLASSES_JSON_OUTPUT), exist_ok=True)

    # 1. Экспорт стандартного каталога в JSON
    with open(JSON_OUTPUT, 'w', encoding='utf-8') as f:
        json.dump(catalog, f, indent=4, ensure_ascii=False)

    # 2. Экспорт отсортированных классов в JSON
    with open(CLASSES_JSON_OUTPUT, 'w', encoding='utf-8') as f:
        json.dump(top_classes, f, indent=4, ensure_ascii=False)
        
    # 3. Экспорт в Markdown
    with open(MD_OUTPUT, 'w', encoding='utf-8') as f:
        f.write("# Отчет о компиляции мода\n\n")
        
        sorted_categories = sorted(catalog.keys(), key=lambda k: sum(len(v) for v in catalog[k].values()), reverse=True)
        
        for cat in sorted_categories:
            total_in_cat = sum(len(locs) for locs in catalog[cat].values())
            f.write(f"## {cat} (Total: {total_in_cat})\n")
            
            for msg, locs in catalog[cat].items():
                f.write(f"**Error:** `{msg}`\n")
                f.write(f"- Affected files ({len(locs)} occurrences):\n")
                for loc in list(set(locs))[:5]: 
                    f.write(f"  - `{loc}`\n")
                if len(set(locs)) > 5:
                    f.write(f"  - *...and {len(set(locs)) - 5} more*\n")
                f.write("\n")
            f.write("---\n\n")

if __name__ == "__main__":
    print("Парсинг лога...")
    errors = parse_log(LOG_FILE)
    print(f"Найдено уникальных ошибок/предупреждений: {len(errors)}")
    
    catalog = aggregate_errors(errors)
    top_classes = aggregate_by_class(errors)
    
    write_reports(catalog, top_classes)
    print(f"Отчеты успешно сохранены в:\n - {MD_OUTPUT}\n - {JSON_OUTPUT}\n - {CLASSES_JSON_OUTPUT}")