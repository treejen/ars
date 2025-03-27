#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Core utilities for Hong Kong address parsing.
This module contains shared functionality used by both Chinese and English address processors.
"""

import os
import sys
import argparse
import json
import re

def tokenize_address(address):
    """
    Tokenize an address string into a format suitable for NER tagging.
    This function handles Hong Kong specific patterns like floor indicators.
    """
    # Basic tokenization
    tokens = []
    current_token = ""
    
    for char in address:
        if char.isalnum() or char in "-'/":
            current_token += char
        else:
            if current_token:
                tokens.append(current_token)
                current_token = ""
            if char.strip():  # if not whitespace
                tokens.append(char)
    
    if current_token:
        tokens.append(current_token)
    
    # Group tokens for special address patterns
    grouped_tokens = []
    i = 0
    while i < len(tokens):
        # Handle floor indicators like "1/F", "G/F", etc.
        if i < len(tokens) - 2 and tokens[i+1] == "/" and tokens[i+2] == "F":
            grouped_tokens.append(tokens[i] + "/" + tokens[i+2])
            i += 3
        # Handle patterns like "B1/F"
        elif i < len(tokens) - 3 and tokens[i+1].isdigit() and tokens[i+2] == "/" and tokens[i+3] == "F":
            grouped_tokens.append(tokens[i] + tokens[i+1] + "/" + tokens[i+3])
            i += 4
        else:
            grouped_tokens.append(tokens[i])
            i += 1
    
    return grouped_tokens

def extract_tag_list():
    """
    Provide a list of common tags for Hong Kong addresses.
    """
    return [
        'O', 
        'B-Unit', 'I-Unit',
        'C-Floor', 
        'B-Estate', 'I-Estate',
        'C-StreetNumber',
        'B-Street', 'I-Street',
        'B-District', 'I-District',
        'B-SubDistrict', 'I-SubDistrict', 'C-SubDistrict',
        'B-Area', 'I-Area', 'C-Area',
        'B-POBox', 'I-POBox'
    ]

def format_address_components(tokens, tags):
    """
    Format the tagged address components into a structured format.
    """
    components = {}
    current_tag = None
    current_text = []
    
    for token, tag in zip(tokens, tags):
        # Skip 'O' tags and punctuation
        if tag == 'O' or token in [',', '.', ':', ';']:
            continue
        
        # Extract the tag type (removing B-, I-, E-, S-, C- prefixes)
        if tag.startswith('B-'):
            # If we were building a component, add it to the dictionary
            if current_tag and current_text:
                components[current_tag] = ' '.join(current_text)
                current_text = []
            
            current_tag = tag[2:]  # Remove B- prefix
            current_text = [token]
        elif tag.startswith('I-'):
            if current_tag == tag[2:]:  # Make sure it's the same entity type
                current_text.append(token)
        elif tag.startswith('C-'):  # Custom tag for standalone items
            tag_name = tag[2:]
            components[tag_name] = token
        
    # Add the last component if not added
    if current_tag and current_text:
        components[current_tag] = ' '.join(current_text)
    
    return components

def load_common_streets(file_path):
    """
    Load common street names from a file
    If file doesn't exist, return an empty list
    """
    try:
        if os.path.exists(file_path):
            with open(file_path, 'r', encoding='utf-8') as f:
                streets = [line.strip() for line in f if line.strip()]
            return streets
        else:
            return []
    except Exception as e:
        return []
