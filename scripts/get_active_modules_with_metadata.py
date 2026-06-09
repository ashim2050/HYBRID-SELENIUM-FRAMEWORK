#!/usr/bin/env python3
"""
Parse MasterConfig.xlsx and return active modules with full metadata as JSON.
Usage: python3 get_active_modules_with_metadata.py Input/MasterConfig.xlsx
Output: JSON array of active modules with ModuleName, ExecutionFlag, TestClass, etc.
"""
import os
import sys
import json
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {
    'main': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main',
    'rel': 'http://schemas.openxmlformats.org/package/2006/relationships'
}


def col_index(cell_ref):
    letters = ''.join([c for c in cell_ref if c.isalpha()])
    index = 0
    for ch in letters:
        index = index * 26 + (ord(ch.upper()) - ord('A') + 1)
    return index - 1


def parse_shared_strings(zf):
    try:
        with zf.open('xl/sharedStrings.xml') as f:
            tree = ET.parse(f)
    except KeyError:
        return []
    root = tree.getroot()
    strings = []
    for si in root.findall('main:si', NS):
        text_parts = []
        t = si.find('main:t', NS)
        if t is not None and t.text is not None:
            text_parts.append(t.text)
        else:
            for tpart in si.findall('.//main:t', NS):
                if tpart.text:
                    text_parts.append(tpart.text)
        strings.append(''.join(text_parts))
    return strings


def get_sheet_path(zf, sheet_name=None):
    with zf.open('xl/workbook.xml') as f:
        tree = ET.parse(f)
    root = tree.getroot()
    sheets = root.find('main:sheets', NS)
    if sheets is None:
        raise RuntimeError('Workbook has no sheets')
    first_sheet = sheets.find('main:sheet', NS)
    if first_sheet is None:
        raise RuntimeError('Workbook has no sheet entries')
    rel_id = first_sheet.get('{http://schemas.openxmlformats.org/officeDocument/2006/relationships}id')
    with zf.open('xl/_rels/workbook.xml.rels') as f:
        tree = ET.parse(f)
    root = tree.getroot()
    for rel in root.findall('rel:Relationship', NS):
        if rel.get('Id') == rel_id:
            target = rel.get('Target')
            if target.startswith('..'):
                target = '/'.join(target.split('/')[2:])
            if target.startswith('/'):
                target = target[1:]
            if not target.startswith('xl/'):
                target = os.path.join('xl', target)
            return target
    raise RuntimeError('Could not resolve sheet path for id: ' + rel_id)


def get_cell_text(cell, shared_strings):
    if cell is None:
        return ''
    cell_type = cell.get('t')
    value = cell.find('main:v', NS)
    if value is None or value.text is None:
        inline = cell.find('main:is/main:t', NS)
        return inline.text.strip() if inline is not None and inline.text else ''
    text = value.text
    if cell_type == 's':
        idx = int(text)
        return shared_strings[idx] if idx < len(shared_strings) else ''
    return text


def parse_sheet(zf, sheet_path, shared_strings):
    with zf.open(sheet_path) as f:
        tree = ET.parse(f)
    root = tree.getroot()
    rows = []
    max_cols = 0
    for row in root.findall('.//main:row', NS):
        cells = row.findall('main:c', NS)
        row_values = {}
        for cell in cells:
            ref = cell.get('r')
            if not ref:
                continue
            idx = col_index(ref)
            row_values[idx] = get_cell_text(cell, shared_strings).strip()
            max_cols = max(max_cols, idx)
        if row_values:
            rows.append((row.get('r'), row_values))
    if not rows:
        return []
    headers = []
    first_row = rows[0][1]
    for col in range(max_cols + 1):
        headers.append(first_row.get(col, '').strip())
    data = []
    for _, row_values in rows[1:]:
        if all(not row_values.get(col, '').strip() for col in range(max_cols + 1)):
            continue
        row = {headers[col] if headers[col] else f'COL_{col}': row_values.get(col, '').strip()
               for col in range(max_cols + 1)}
        data.append(row)
    return data


def load_master(master_path):
    if not Path(master_path).exists():
        raise FileNotFoundError(f'Master config not found: {master_path}')
    with zipfile.ZipFile(master_path, 'r') as zf:
        shared_strings = parse_shared_strings(zf)
        sheet_path = get_sheet_path(zf)
        return parse_sheet(zf, sheet_path, shared_strings)


import re


def normalize_module_name(name):
    return re.sub(r'\s+', '', name.strip().lower())


def main():
    master_path = sys.argv[1] if len(sys.argv) > 1 else 'Input/MasterConfig.xlsx'
    rows = load_master(master_path)
    active_modules = []
    
    for row in rows:
        module_name = row.get('ModuleName', '').strip()
        flag = row.get('ExecutionFlag', '').strip().lower()
        test_class = row.get('TestClass', '').strip()
        
        if module_name and flag == 'yes' and test_class:
            normalized_name = normalize_module_name(module_name)
            active_modules.append({
                'moduleName': normalized_name,
                'displayName': module_name,
                'executionFlag': flag,
                'testClass': test_class,
                'reportFolder': normalized_name,
                'reportPrefix': f'ExtentReport_{normalized_name}.html',
                'stashName': f'{normalized_name}-results',
                'suiteDescription': row.get('SuiteDescription', '')
            })
    
    # Output as JSON
    print(json.dumps(active_modules, indent=2))


if __name__ == '__main__':
    main()
