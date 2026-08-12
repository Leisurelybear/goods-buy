#!/usr/bin/env python3
"""
Convert Excel to GoodsBuy backup JSON format.

Usage:
    python convert_excel_to_backup.py input.xlsx output.json

Excel structure expected:
    - Each sheet = one IP/Collection
    - Headers: 种类, 渠道, 系列, 制品, 角色, 收价(原价), 意愿, 出价, 失与得
    - Empty cells in columns 0-4 should be filled from above (same series)
"""

import sys
import json
from pathlib import Path
from datetime import datetime

try:
    from openpyxl import load_workbook
except ImportError:
    print("Error: openpyxl not installed. Run: pip install openpyxl")
    sys.exit(1)

# Status mapping from Excel "意愿" to GoodsBuy OrderStatus
STATUS_MAP = {
    '已出': 'SOLD',
    '犹豫出': 'HESITATING_SELL',
    '待出': 'LISTED',
    '持有': 'OWNED',
    '待补': 'PENDING_TAIL',
    '待邮': 'PENDING_SHIPPING_FEE',
    '待发货': 'PENDING_SEND',
    '运输中': 'IN_TRANSIT_ORDER',
    '赠品': 'GIFT',
    '遗失': 'LOST',
    '损坏': 'LOST',
}

def fill_forward(ws, max_col):
    """Fill empty cells with value from row above."""
    prev_row = [None] * max_col
    for row in ws.iter_rows(min_row=1, max_row=ws.max_row, max_col=max_col):
        curr = []
        for cell in row:
            if cell.value is not None and str(cell.value).strip():
                curr.append(str(cell.value).strip())
                prev_row[cell.coordinate[0]-1] = str(cell.value).strip()
            else:
                curr.append(prev_row[len(curr)])
        yield curr

def parse_status(val):
    """Parse status from Excel cell."""
    if val is None or str(val).strip() == '':
        return 'OWNED'  # Default
    s = str(val).strip()
    for key, status in STATUS_MAP.items():
        if key in s:
            return status
    return 'OWNED'

def parse_price(val):
    """Parse price value."""
    if val is None or str(val).strip() == '':
        return None
    try:
        return float(str(val).replace(',', '').replace('¥', '').replace('￥', ''))
    except:
        return None

def convert_sheet(ws, ip_name):
    """Convert one sheet to list of CollectibleRecord."""
    records = []
    headers = [cell.value for cell in ws[1]]
    max_cols = ws.max_column

    # Find column indices
    col_map = {}
    for i, h in enumerate(headers):
        if h:
            col_map[str(h).strip()] = i

    # Get data columns (skip headers, start from row 2)
    current_series = None
    current_type = None
    current_channel = None

    for row_idx in range(2, ws.max_row + 1):
        row = [ws.cell(row=row_idx, column=c+1).value for c in range(min(max_cols, len(headers)))]

        # Fill forward for type/channel/series
        type_val = row[col_map.get('种类', 0)] if col_map.get('种类', 0) < len(row) else None
        channel_val = row[col_map.get('渠道', 1)] if col_map.get('渠道', 1) < len(row) else None
        series_val = row[col_map.get('系列', 2)] if col_map.get('系列', 2) < len(row) else None
        product_val = row[col_map.get('制品', 3)] if col_map.get('制品', 3) < len(row) else None
        char_val = row[col_map.get('角色', 4)] if col_map.get('角色', 4) < len(row) else None
        price_val = row[col_map.get('收价（原价）', 5)] if col_map.get('收价（原价）', 5) < len(row) else None
        status_val = row[col_map.get('意愿', 6)] if col_map.get('意愿', 6) < len(row) else None
        sell_price_val = row[col_map.get('出价', 7)] if col_map.get('出价', 7) < len(row) else None
        pl_val = row[col_map.get('失与得', 8)] if col_map.get('失与得', 8) < len(row) else None

        # Fill forward
        if type_val:
            current_type = str(type_val).strip()
        if channel_val:
            current_channel = str(channel_val).strip()
        if series_val:
            current_series = str(series_val).strip()

        # Skip if no product name
        if not product_val:
            continue

        # Create record
        price = parse_price(price_val)
        sell_price = parse_price(sell_price_val)
        status = parse_status(status_val)

        records.append({
            'name': str(product_val).strip(),
            'category': current_type or '',
            'type': '官方',
            'ipName': ip_name,
            'seriesName': current_series or '',
            'characterTag': str(char_val).strip() if char_val else '',
            'remark': '',
            'purchaseChannel': current_channel or '',
            'purchaseShop': '',
            'purchaseDate': int(datetime.now().timestamp() * 1000),
            'purchasePrice': price if price else 0.0,
            'purchaseQuantity': 1,
            'purchaseShipping': 0.0,
            'expectedPrice': sell_price if sell_price else (price * 1.5 if price else 0.0),
            'sellPrice': sell_price,
            'sellQuantity': None,
            'sellShipping': None,
            'isFreeShipping': False,
            'sellDate': None,
            'buyerInfo': None,
            'sellRemark': None,
            'status': status,
            'storageStatus': 'IN_STOCK',
            'imageFilenames': [],
            'createdAt': int(datetime.now().timestamp() * 1000),
            'updatedAt': int(datetime.now().timestamp() * 1000),
        })

    return records

def main():
    if len(sys.argv) < 3:
        print("Usage: python convert_excel_to_backup.py input.xlsx output.json")
        sys.exit(1)

    input_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2])

    if not input_path.exists():
        print(f"Error: File not found: {input_path}")
        sys.exit(1)

    print(f"Reading: {input_path}")
    wb = load_workbook(input_path, data_only=True)

    all_records = []
    for sheet_name in wb.sheetnames:
        ws = wb[sheet_name]
        print(f"  Converting sheet: {sheet_name} ({ws.max_row} rows)")
        records = convert_sheet(ws, sheet_name)
        all_records.extend(records)
        print(f"    -> {len(records)} items")

    # Build manifest
    manifest = {
        'version': 1,
        'timestamp': int(datetime.now().timestamp() * 1000),
        'collectibles': all_records
    }

    # Write JSON
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    print(f"\nDone! {len(all_records)} total records written to: {output_path}")

if __name__ == '__main__':
    main()
