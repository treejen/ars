#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
地址分析命令行工具
使用方法: python analyze_address.py <地址>
"""

import sys
import json
from chinese_address_processor import parse_chinese_address, detect_language
from english_address_processor import parse_english_address
from modules.address_utils import clean_chinese_spaces

def parse_bilingual_address(address, force_language=None):
    """
    根據語言選擇適當的處理器解析地址
    
    參數:
        address (str): 要解析的地址
        force_language (str, optional): 強制使用特定語言模型 ('en' 或 'zh')
    
    返回:
        dict: 包含解析結果的字典
    """
    # 檢測或強制指定語言
    if force_language in ['en', 'zh']:
        language = force_language
    else:
        language = detect_language(address)
    
    # 根據語言選擇處理器
    if language == 'en':
        result = parse_english_address(address)
    else:
        result = parse_chinese_address(address)
    
    return result

def create_standard_json(result):
    """創建標準格式的JSON結果"""
    components = result.get('components', {})

    # 映射到標準字段
    standard_format = {
        "address": result.get('original', ''),
        "street": components.get('Street', ''),
        "dist": components.get('District', ''),
        "number": components.get('StreetNumber', ''),
        "estate": components.get('Estate', ''),
        "deliveryZoneCode": "",
        "latitude": 0.0,
        "longitude": 0.0
    }

    return standard_format

def main():
    # 檢查是否提供了地址
    if len(sys.argv) < 2:
        sys.exit(1)

    # 獲取地址參數 (合併所有參數以支持帶空格的地址)
    address = " ".join(sys.argv[1:])
    address = address.strip()

    try:
        # 解析地址
        result = parse_bilingual_address(address)

        # 清理中文空格並轉換簡繁體
        result = clean_chinese_spaces(result)

        # 創建標準格式的JSON
        standard_json = create_standard_json(result)

        # 只輸出JSON結果，不包含其他內容
        print(json.dumps(standard_json, ensure_ascii=False))

    except Exception as e:
        sys.exit(1)

if __name__ == "__main__":
    main()
