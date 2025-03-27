#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
English address processing module for Hong Kong addresses.
This module handles the processing of English addresses with specific rules for Hong Kong.
"""

import os
import sys
import re
from collections import defaultdict

# Import core utilities
from address_core_utils import tokenize_address, format_address_components

# Path to common streets data file
COMMON_STREETS_EN_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'data', 'common_hk_streets_en.txt')

def predict_tags_with_rules(tokens):
    """
    Use rule-based approach to predict tags based on patterns typically 
    found in Hong Kong English addresses.
    """
    tags = ['O'] * len(tokens)
    components_found = {
        'Unit': False,
        'Floor': False,
        'Estate': False,
        'StreetNumber': False,
        'Street': False,
        'SubDistrict': False,
        'District': False,
        'Area': False,
        'POBox': False
    }
    
    # Hong Kong area patterns
    areas = {
        'hong kong': 'Area',
        'kowloon': 'Area',
        'new territories': 'Area',
        'outlying islands': 'Area'
    }
    
    # District patterns
    districts = {
        'central': 'District',
        'wan chai': 'District',
        'eastern': 'District',
        'southern': 'District',
        'yau tsim mong': 'District',
        'sham shui po': 'District',
        'kowloon city': 'District',
        'wong tai sin': 'District',
        'kwun tong': 'District',
        'kwai tsing': 'District',
        'tsuen wan': 'District',
        'tuen mun': 'District',
        'yuen long': 'District',
        'north': 'District',
        'tai po': 'District',
        'sha tin': 'District',
        'sai kung': 'District',
        'islands': 'District'
    }
    
    # SubDistrict patterns
    subdistricts = {
        'admiralty': 'SubDistrict',
        'causeway bay': 'SubDistrict',
        'central': 'SubDistrict',
        'kennedy town': 'SubDistrict',
        'mid-levels': 'SubDistrict',
        'the peak': 'SubDistrict',
        'repulse bay': 'SubDistrict',
        'stanley': 'SubDistrict',
        'tin hau': 'SubDistrict',
        'to kwa wan': 'SubDistrict',
        'tsim sha tsui': 'SubDistrict',
        'yau ma tei': 'SubDistrict',
        'mong kok': 'SubDistrict',
        'sham shui po': 'SubDistrict',
        'diamond hill': 'SubDistrict',
        'wong tai sin': 'SubDistrict',
        'kwun tong': 'SubDistrict',
        'tsuen wan east': 'SubDistrict',
        'tsuen wan west': 'SubDistrict',
        'tuen mun': 'SubDistrict',
        'tai wo': 'SubDistrict',
        'ting kau': 'SubDistrict',
        'tiu keng leng': 'SubDistrict',
        'tseung kwan o': 'SubDistrict',
        'tin shui wai': 'SubDistrict',
        'tsz wan shan': 'SubDistrict'
    }
    
    # Load common street names from external file
    try:
        from address_core_utils import load_common_streets
        common_streets = load_common_streets(COMMON_STREETS_EN_FILE)
    except ImportError:
        print("Could not import load_common_streets function")
        common_streets = []
    
    # Process tokens
    i = 0
    while i < len(tokens):
        token = tokens[i]
        
        # Skip commas and other punctuation
        if token in [',', '.', ':', ';']:
            i += 1
            continue
        
        # Check for common streets from the loaded file
        if common_streets:
            street_match = False
            # Try to match common streets from our file
            for street_name in common_streets:
                street_name_lower = street_name.lower()
                street_parts = street_name_lower.split()
                
                if i + len(street_parts) <= len(tokens):
                    potential_match = True
                    for j, part in enumerate(street_parts):
                        if i+j >= len(tokens) or tokens[i+j].lower() != part:
                            potential_match = False
                            break
                    
                    if potential_match:
                        # Tag as Street
                        tags[i] = 'B-Street'
                        for j in range(1, len(street_parts)):
                            tags[i+j] = 'I-Street'
                        components_found['Street'] = True
                        i += len(street_parts)
                        street_match = True
                        break
            
            if street_match:
                continue
        
        # Floor pattern (e.g., G/F, 1/F, B/F)
        if re.match(r'[BG0-9]+/F', token):
            tags[i] = 'C-Floor'
            components_found['Floor'] = True
            i += 1
            continue
        
        # Shop/Flat/Room patterns
        if i < len(tokens) - 1 and token.lower() in ['shop', 'flat', 'room', 'unit', 'block'] and tokens[i+1] not in [',', '.']:
            tags[i] = 'B-Unit'
            tags[i+1] = 'I-Unit'
            components_found['Unit'] = True
            i += 2
            continue
        
        # Street number - can appear at beginning or before street name
        if (token.isdigit() or re.match(r'\d+-\d+', token)) and not components_found['StreetNumber']:
            # Check if this looks like a street number, not a unit number
            # Unit numbers are usually preceded by Unit, Room, Flat, etc. or followed by /F
            is_unit_number = False
            
            # Check if previous token suggests this is a unit number
            if i > 0 and tokens[i-1].lower() in ['unit', 'room', 'flat', 'shop']:
                is_unit_number = True
            
            # Check if next token suggests this is a floor indicator
            if i < len(tokens) - 1 and re.match(r'/F', tokens[i+1]):
                is_unit_number = True
                
            if not is_unit_number:
                tags[i] = 'C-StreetNumber'
                components_found['StreetNumber'] = True
                i += 1
                continue
            
        # Check for multi-token areas
        area_match = False
        for area_name, area_type in areas.items():
            area_parts = area_name.split()
            if i + len(area_parts) <= len(tokens):
                match = True
                for j, part in enumerate(area_parts):
                    if tokens[i+j].lower() != part:
                        match = False
                        break
                
                if match:
                    tags[i] = 'B-' + area_type
                    for j in range(1, len(area_parts)):
                        tags[i+j] = 'I-' + area_type
                    components_found[area_type] = True
                    i += len(area_parts)
                    area_match = True
                    break
        
        if area_match:
            continue
            
        # Check for multi-token districts
        district_match = False
        for district_name, district_type in districts.items():
            district_parts = district_name.split()
            if i + len(district_parts) <= len(tokens):
                match = True
                for j, part in enumerate(district_parts):
                    if tokens[i+j].lower() != part:
                        match = False
                        break
                
                if match:
                    tags[i] = 'B-' + district_type
                    for j in range(1, len(district_parts)):
                        tags[i+j] = 'I-' + district_type
                    components_found[district_type] = True
                    i += len(district_parts)
                    district_match = True
                    break
        
        if district_match:
            continue
            
        # Check for multi-token subdistricts
        subdistrict_match = False
        for subdistrict_name, subdistrict_type in subdistricts.items():
            subdistrict_parts = subdistrict_name.split()
            if i + len(subdistrict_parts) <= len(tokens):
                match = True
                for j, part in enumerate(subdistrict_parts):
                    if tokens[i+j].lower() != part:
                        match = False
                        break
                
                if match:
                    tags[i] = 'B-' + subdistrict_type
                    for j in range(1, len(subdistrict_parts)):
                        tags[i+j] = 'I-' + subdistrict_type
                    components_found[subdistrict_type] = True
                    i += len(subdistrict_parts)
                    subdistrict_match = True
                    break
        
        if subdistrict_match:
            continue
            
        # Estate/Building/Centre/Plaza
        estate_keywords = ['building', 'centre', 'center', 'plaza', 'mall', 'house', 'tower', 'mansion', 'court']
        if (token.lower() in estate_keywords or 
            (i > 0 and tokens[i-1].lower() in estate_keywords) or
            token in ['Times', 'Square', 'IFC', 'K11']):
            
            # Check if we need to start a new entity or continue one
            if i > 0 and (tags[i-1].startswith('B-Estate') or tags[i-1].startswith('I-Estate')):
                tags[i] = 'I-Estate'
            else:
                tags[i] = 'B-Estate'
            components_found['Estate'] = True
            i += 1
            continue
        
        # Street pattern - Handle common Hong Kong streets
        street_keywords = ['road', 'street', 'avenue', 'lane', 'path', 'rd', 'st', 'ave', 'square', 'place', 'way']
        
        # Multi-word street names in Hong Kong
        two_word_streets = [
            'fa yuen', 'des voeux', 'des voeux road', 'des voeux road west', 'des voeux road central',
            'connaught road', 'connaught road central', 'connaught road west',
            'queens road', 'queens road central', 'queens road east', 'queens road west',
            'hennessy road', 'johnston road', 'queens way', 'wing lok',
            'tai po', 'tung choi', 'tung chung', 'nathan road',
            'argyle street', 'canton road', 'chatham road', 'jordan road',
            'lai chi', 'lung cheung', 'mody road', 'tat chee', 'wai yip', 'lockhart road',
            'castle peak road', 'sha tin wai road', 'prince edward road', 'prince edward road west',
            'cheung sha wan road', 'kwun tong road', 'hung hom road'
        ]
        
        # Check for multi-word street names (up to 4 words)
        max_lookahead = min(4, len(tokens) - i)
        for words_to_check in range(max_lookahead, 1, -1):
            if i + words_to_check <= len(tokens):
                # Build potential street name of multiple words
                potential_street = ' '.join(token.lower() for token in tokens[i:i+words_to_check])
                
                # Check for street name matches
                matched = False
                for street in two_word_streets:
                    if street == potential_street or street.startswith(potential_street) or potential_street.startswith(street):
                        # Mark beginning token
                        tags[i] = 'B-Street'
                        # Mark rest as inside tokens
                        for j in range(1, words_to_check):
                            tags[i+j] = 'I-Street'
                        
                        # Check if the next token after the street name is a street keyword
                        if i+words_to_check < len(tokens) and tokens[i+words_to_check].lower() in street_keywords:
                            tags[i+words_to_check] = 'I-Street'
                            i += words_to_check + 1
                        else:
                            i += words_to_check
                        
                        components_found['Street'] = True
                        matched = True
                        break
                
                if matched:
                    break
            
        # Continue the outer loop if we matched a multi-word street
        if 'matched' in locals() and matched:
            continue
        
        # Check for standard street pattern
        if token.lower() in street_keywords or (i < len(tokens) - 1 and tokens[i+1].lower() in street_keywords):
            if i > 0 and (tags[i-1].startswith('B-Street') or tags[i-1].startswith('I-Street')):
                tags[i] = 'I-Street'
            else:
                tags[i] = 'B-Street'
            components_found['Street'] = True
            i += 1
            continue

        # Now check for any multi-word street pattern
        if i < len(tokens) - 2:  # Need at least 3 tokens: [Word] [Word] [St/Rd/...]
            # Check if the pattern is "[Word] [Word] St/Rd/..."
            if tokens[i+2].lower() in street_keywords:
                # If StreetNumber was just found before this potential street name, more likely to be a street
                is_likely_street = False
                
                # Check if a street number was just processed
                if i > 0 and tags[i-1] == 'C-StreetNumber':
                    is_likely_street = True
                
                # Check if this doesn't look like a district or area
                if not any(d.lower() in tokens[i].lower() + ' ' + tokens[i+1].lower() for d in districts.keys()) and \
                   not any(a.lower() in tokens[i].lower() + ' ' + tokens[i+1].lower() for a in areas.keys()):
                    is_likely_street = True
                
                if is_likely_street:
                    # Assume this is the start of a multi-word street name
                    tags[i] = 'B-Street'
                    tags[i+1] = 'I-Street'
                    tags[i+2] = 'I-Street'
                    components_found['Street'] = True
                    i += 3
                    continue
                    
        # Move to next token if none of the rules matched
        i += 1
    
    # Post-processing to ensure tag consistency
    for i in range(len(tags)):
        # If tagged as I- without preceding B-, convert to B-
        if tags[i].startswith('I-') and (i == 0 or not tags[i-1].startswith('B-') and not tags[i-1].startswith('I-')):
            tag_type = tags[i][2:]  # Extract type without 'I-'
            tags[i] = 'B-' + tag_type
    
    return tags

def parse_english_address(address):
    """
    Parse an English address and return structured components
    """
    # Tokenize the address
    tokens = tokenize_address(address)
    
    # Predict tags
    tags = predict_tags_with_rules(tokens)
    
    # Format the components
    components = format_address_components(tokens, tags)
    
    return {
        'original': address,
        'language': 'en',
        'tokens': tokens,
        'tags': tags,
        'components': components
    }
