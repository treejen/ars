"""
地址处理工具模块 - 简化版本，提供基本的中文地址空格处理与简繁转换
"""
try:
    import opencc
    HAS_OPENCC = True
    # 創建簡體到繁體的轉換器
    converter = opencc.OpenCC('s2t')
except ImportError:
    HAS_OPENCC = False
    print("警告: opencc 未安裝, 簡繁轉換功能將不可用。請使用 pip install opencc-python-reimplemented 安裝。")

def has_chinese(text):
    """检查文本是否包含中文字符"""
    if not isinstance(text, str):
        return False
    # 检测中文字符 (CJK统一汉字范围)
    return any(ord(c) >= 0x4e00 and ord(c) <= 0x9fff for c in text)

def simplified_to_traditional(text):
    """將簡體中文轉換為繁體中文"""
    if not isinstance(text, str):
        return text
    
    # 檢查是否有 OpenCC 可用
    if not HAS_OPENCC:
        return text
    
    # 只有當文本包含中文時才進行轉換
    if has_chinese(text):
        return converter.convert(text)
    
    return text

def clean_chinese_spaces(result):
    """简化的中文空格清理函数，移除中文组件中的所有空格，并将简体转换为繁体"""
    if 'components' not in result:
        return result
    
    for key, value in result['components'].items():
        if isinstance(value, str) and has_chinese(value):
            # 先移除空格
            cleaned_value = value.replace(' ', '')
            
            # 再将简体转繁体
            cleaned_value = simplified_to_traditional(cleaned_value)
            
            # 更新值
            result['components'][key] = cleaned_value
    
    return result
