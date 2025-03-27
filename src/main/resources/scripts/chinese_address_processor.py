#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Chinese address processing module for Hong Kong addresses.
This module handles the processing of Chinese addresses with specific rules for Hong Kong.
"""

import os
import sys
import re
from collections import defaultdict

# Import core utilities
from address_core_utils import format_address_components, load_common_streets

# Path to common streets data file
COMMON_STREETS_ZH_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'data', 'common_hk_streets_zh.txt')

def is_chinese_char(c):
    """Check if a character is Chinese"""
    return '\u4e00' <= c <= '\u9fff'

def tokenize_chinese_address(address):
    """
    Tokenize a Chinese address using character-based approach
    """
    tokens = []
    current_token = ""
    
    for char in address:
        if is_chinese_char(char):
            # For Chinese characters, treat each as a separate token
            if current_token:
                tokens.append(current_token)
                current_token = ""
            tokens.append(char)
        elif char.isalnum() or char in "-'/":
            # For Latin characters, numbers, and some punctuation, build tokens
            current_token += char
        else:
            if current_token:
                tokens.append(current_token)
                current_token = ""
            if char.strip():  # if not whitespace
                tokens.append(char)
    
    if current_token:
        tokens.append(current_token)
    
    return tokens

def predict_tags_with_zh_rules(tokens):
    """
    Use rule-based approach to predict tags for Chinese addresses
    """
    tags = ['O'] * len(tokens)
    components_found = defaultdict(bool)
    
    # Define Chinese patterns
    district_keywords = ['區', '区', '圍', '灣']
    subdistrict_keywords = ['灣仔', '中環', '銅鑼灣', '尖沙咀', '旺角', '觀塘', '荃灣', '太古', '北角', '上環', '西環', '鴨脷洲', '黃竹坑', '大圍', '將軍澳']
    street_keywords = ['街', '道', '路', '里', '巷', '橋', '段', '徑', '門', '圍']
    street_suffix_keywords = ['西', '東', '南', '北', '中']
    estate_keywords = ['大廈', '中心', '廣場', '商場', '大樓', '商業中心', '樓', '廣場', '花園', '廣場', '邨', '閣', '灣', '城']
    area_keywords = ['香港', '九龍', '新界', '離島']
    unit_keywords = ['室', '單位', '舖', '號舖']
    
    # Load common street names from external file (Chinese version)
    common_hk_streets = load_common_streets(COMMON_STREETS_ZH_FILE)
    
    # Process tokens
    i = 0
    while i < len(tokens):
        token = tokens[i]
        
        # Check for common multi-character Hong Kong street names
        found_common_street = False
        if common_hk_streets:  # Only check if we have loaded street names
            for street in common_hk_streets:
                street_chars = list(street)
                if i + len(street_chars) <= len(tokens):
                    # Check if the next n tokens match the street name
                    match = True
                    for j, char in enumerate(street_chars):
                        if tokens[i+j] != char:
                            match = False
                            break
                    
                    if match:
                        # Tag the street name
                        tags[i] = 'B-Street'
                        for j in range(1, len(street_chars)):
                            tags[i+j] = 'I-Street'
                        
                        components_found['Street'] = True
                        i += len(street_chars)
                        found_common_street = True
                        break
        
        if found_common_street:
            continue
            
        # Check for areas
        if token in area_keywords and not components_found['Area']:
            tags[i] = 'C-AREA'
            components_found['Area'] = True
            i += 1
            continue
        
        # Check for districts
        if any(keyword in token for keyword in district_keywords) and not components_found['District']:
            # For multi-character districts
            district_start = i
            
            # Look back for the full district name
            for j in range(1, 3):
                if i >= j and tags[i-j] == 'O':
                    district_start = i-j
                else:
                    break
            
            # Tag district
            if district_start < i:
                tags[district_start] = 'B-District'
                for j in range(district_start+1, i+1):
                    tags[j] = 'I-District'
            else:
                tags[i] = 'B-District'
                
            components_found['District'] = True
            i += 1
            continue
        
        # Check for subdistricts
        if any(token == keyword for keyword in subdistrict_keywords) and not components_found['SubDistrict']:
            tags[i] = 'B-SubDistrict'
            components_found['SubDistrict'] = True
            i += 1
            continue
            
        # Check for 2-character sub-districts
        if i < len(tokens) - 1 and not components_found['SubDistrict']:
            potential_subdistrict = token + tokens[i+1]
            if potential_subdistrict in subdistrict_keywords:
                tags[i] = 'B-SubDistrict'
                tags[i+1] = 'I-SubDistrict'
                components_found['SubDistrict'] = True
                i += 2
                continue
        
        # Check for streets
        if any(keyword in token for keyword in street_keywords) and not components_found['Street']:
            start_idx = i
            
            # Look backward for street name parts
            for j in range(1, 4):
                if i >= j and tags[i-j] == 'O':
                    start_idx = i-j
                else:
                    break
            
            # Tag street
            if start_idx < i:
                tags[start_idx] = 'B-Street'
                for j in range(start_idx+1, i+1):
                    tags[j] = 'I-Street'
            else:
                tags[i] = 'B-Street'

            # Check for directional suffix
            if i < len(tokens) - 1 and tokens[i+1] in street_suffix_keywords and tags[i+1] == 'O':
                tags[i+1] = 'I-Street'
                i += 2
            else:
                i += 1
                
            components_found['Street'] = True
            continue
        
        # Check for street numbers
        if (token == '號' or token == '蒙') and i > 0 and not components_found['StreetNumber']:
            street_num_start = i - 1
            
            # Look back for digits and hyphens
            while street_num_start > 0:
                prev_token = tokens[street_num_start - 1]
                if prev_token.isdigit() or prev_token == '-' or prev_token == '–':
                    street_num_start -= 1
                else:
                    break
                    
            # Tag digits and hyphens
            if tokens[street_num_start].isdigit():
                for j in range(street_num_start, i):
                    if tokens[j].isdigit() or tokens[j] == '-' or tokens[j] == '–':
                        tags[j] = 'C-StreetNumber'
                        
                components_found['StreetNumber'] = True
            i += 1
            continue
        
        # Check for estates/buildings
        if any(keyword in token for keyword in estate_keywords) and not components_found['Estate']:
            estate_start = i
            
            # Look back for estate name parts
            for j in range(1, 5):
                if i >= j and tags[i-j] == 'O':
                    estate_start = i-j
                else:
                    break
            
            # Tag estate
            if estate_start < i:
                tags[estate_start] = 'B-Estate'
                for j in range(estate_start+1, i+1):
                    tags[j] = 'I-Estate'
            else:
                tags[i] = 'B-Estate'
                
            components_found['Estate'] = True
            i += 1
            continue
        
        # Check for units
        if any(keyword in token for keyword in unit_keywords) and not components_found['Unit']:
            unit_start = i
            
            # Look back for unit identifiers
            for j in range(1, 5):
                if i >= j and (tokens[i-j].isdigit() or tokens[i-j].isalpha()) and tags[i-j] == 'O':
                    unit_start = i-j
                else:
                    break
            
            # Tag unit
            if unit_start < i:
                if tokens[unit_start].isdigit() or tokens[unit_start].isalpha():
                    tags[unit_start] = 'B-Unit'
                    for j in range(unit_start+1, i+1):
                        tags[j] = 'I-Unit'
                else:
                    tags[i] = 'B-Unit'
            else:
                tags[i] = 'B-Unit'
                
            components_found['Unit'] = True
            i += 1
            continue
        
        # Floor detection
        if (token == '樓' or token == '層') and not components_found['Floor']:
            if i > 0 and (tokens[i-1].isdigit() or tokens[i-1] == '地'):
                if tokens[i-1] == '地':
                    tags[i-1] = 'C-Floor'  # Ground floor
                else:
                    # Check for multi-digit floor numbers
                    digits_start = i - 1
                    while digits_start > 0 and tokens[digits_start-1].isdigit():
                        digits_start -= 1
                    
                    # Tag floor number
                    for j in range(digits_start, i):
                        tags[j] = 'C-Floor'
                        
                components_found['Floor'] = True
            i += 1
            continue
        
        # Move to next token if no rules matched
        i += 1
    
    return tags

def detect_language(text):
    """
    Detect the primary language of the text (Chinese or English)
    Returns primary language code ('zh' or 'en')
    """
    if not text:
        return 'en'
    
    text = text.strip()
    total_chars = len(text)
    chinese_chars = sum(1 for c in text if is_chinese_char(c))
    
    # Calculate ratio of Chinese characters
    zh_ratio = chinese_chars / total_chars if total_chars > 0 else 0
    
    # If more than 30% is Chinese, treat as Chinese
    if zh_ratio > 0.3:
        return 'zh'
    else:
        return 'en'

def parse_chinese_address(address):
    """
    Parse a Chinese address and return structured components
    """
    # Tokenize the address
    tokens = tokenize_chinese_address(address)
    
    # Predict tags
    tags = predict_tags_with_zh_rules(tokens)
    
    # Format the components
    components = format_address_components(tokens, tags)
    
    return {
        'original': address,
        'language': 'zh',
        'tokens': tokens,
        'tags': tags,
        'components': components
    }
