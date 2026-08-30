import re
import json
import os
import shutil
from collections import defaultdict

# Базовые пути
PROJECT_ROOT = r'C:\Projects\HBM-Modernized'
LOG_FILE = os.path.join(PROJECT_ROOT, 'compile.log')
REPORT_DIR = os.path.join(PROJECT_ROOT, 'scripts', 'report')

# Пути к отчетам и копиям
MD_OUTPUT = os.path.join(REPORT_DIR, 'errors_report.md')
JSON_OUTPUT = os.path.join(REPORT_DIR, 'errors_report.json')
CLASSES_JSON_OUTPUT = os.path.join(REPORT_DIR, 'top_classes_errors.json')
FILES_OUTPUT_DIR = os.path.join(REPORT_DIR, 'files')

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

def get_clean_relative_path(raw_path, project_root):
    """
    Извлекает относительный путь Java-пакета (например, com/hbm/items/ItemBomb.java)
    из любого абсолютного или смешанного пути.
    """
    norm = os.path.normpath(raw_path)
    
    # Поиск маркеров каталогов исходного кода
    markers = [
        os.path.normpath("src/main/java"),
        os.path.normpath("src/test/java"),
        os.path.normpath("src/api/java")
    ]
    
    for marker in markers:
        if marker in norm:
            parts = norm.split(marker)
            return parts[-1].lstrip("\\/")
            
    # Если маркера нет, пробуем сделать относительным к PROJECT_ROOT
    try:
        rel = os.path.relpath(norm, project_root)
        if not rel.startswith("..") and not os.path.splitdrive(rel)[0]:
            return rel
    except ValueError:
        pass
        
    # В крайнем случае возвращаем только имя файла
    return os.path.basename(norm)

def parse_log(file_path):
    pattern = re.compile(r"^(?P<file>.*\.java):(?P<line>\d+): (?P<severity>error|warning): (?P<message>.*)$")
    
    errors = []
    current_error = None
    
    if not os.path.exists(file_path):
        print(f"Ошибка: Лог-файл '{file_path}' не найден!")
        return []

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
                
                raw_filepath = match.group('file').strip()
                rel_filepath = get_clean_relative_path(raw_filepath, PROJECT_ROOT)
                
                current_error = {
                    "file": rel_filepath,         # Относительный путь (com/hbm/...)
                    "full_path": raw_filepath,     # Исходный путь из лога
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
        
    result.sort(key=lambda x: x["total_errors"], reverse=True)
    return result

def copy_error_files(errors, target_dir):
    """
    Полностью очищает целевую папку и копирует все проблемные исходные файлы,
    сохраняя структуру пакетов.
    """
    # 1. Если папка уже существует — удаляем её полностью со всем содержимым
    if os.path.exists(target_dir):
        # Снимаем защиту от записи с файлов перед удалением (актуально для Windows)
        for root, dirs, files in os.walk(target_dir):
            for file in files:
                try:
                    os.chmod(os.path.join(root, file), 0o777)
                except OSError:
                    pass
        try:
            shutil.rmtree(target_dir)
        except Exception as e:
            print(f" [!] Предупреждение при удалении старой папки: {e}")

    # 2. Создаем чистую целевую папку
    os.makedirs(target_dir, exist_ok=True)
    
    # Собираем уникальные файлы {full_path: rel_path}
    unique_files = {}
    for err in errors:
        unique_files[err["full_path"]] = err["file"]
        
    copied = 0
    missing = 0
    failed = 0
    
    for full_path, rel_path in unique_files.items():
        # Определяем абсолютный путь к исходнику
        if os.path.isabs(full_path):
            src_path = os.path.normpath(full_path)
        else:
            src_path = os.path.normpath(os.path.join(PROJECT_ROOT, full_path))
            
        # Проверяем, существует ли файл, если нет — пробуем найти в src/main/java
        if not os.path.exists(src_path):
            alt_src = os.path.normpath(os.path.join(PROJECT_ROOT, "src", "main", "java", rel_path))
            if os.path.exists(alt_src):
                src_path = alt_src
            else:
                print(f" [!] Исходный файл не найден: {src_path}")
                missing += 1
                continue
                
        # Формируем целевой путь внутри target_dir
        dest_path = os.path.normpath(os.path.join(target_dir, rel_path))
        
        # Защита от перезаписи самого себя
        if os.path.abspath(src_path) == os.path.abspath(dest_path):
            print(f" [!] Пропуск: исходный путь совпадает с целевым ({src_path})")
            continue
            
        try:
            os.makedirs(os.path.dirname(dest_path), exist_ok=True)
            shutil.copy2(src_path, dest_path)
            copied += 1
        except Exception:
            # Резервный вариант через бинарное чтение/запись при блокировках Windows API
            try:
                with open(src_path, 'rb') as f_src, open(dest_path, 'wb') as f_dst:
                    shutil.copyfileobj(f_src, f_dst)
                copied += 1
            except Exception as e_inner:
                print(f" [!] Не удалось скопировать {src_path}: {e_inner}")
                failed += 1
            
    return copied, missing, failed

def write_reports(catalog, top_classes):
    os.makedirs(REPORT_DIR, exist_ok=True)

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
    
    if not errors:
        print("Ошибок не найдено или файл пуст.")
        exit(0)
        
    print(f"Найдено уникальных записей об ошибках/предупреждениях: {len(errors)}")
    
    catalog = aggregate_errors(errors)
    top_classes = aggregate_by_class(errors)
    
    print("Генерация отчетов...")
    write_reports(catalog, top_classes)
    
    print("Очистка и копирование файлов с ошибками...")
    copied, missing, failed = copy_error_files(errors, FILES_OUTPUT_DIR)
    
    print("\n--- Результаты работы ---")
    print(f"Отчеты успешно сохранены в:\n - {MD_OUTPUT}\n - {JSON_OUTPUT}\n - {CLASSES_JSON_OUTPUT}")
    print(f"Файлы с ошибками скопированы в:\n - {FILES_OUTPUT_DIR}")
    print(f"Скопировано уникальных Java-файлов: {copied}" + 
          (f" (не найдено: {missing})" if missing > 0 else "") +
          (f" (ошибок доступа: {failed})" if failed > 0 else ""))