package com.hktv.ars.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class ExcelUtil {

    public static <T> List<T> readResource(String filePath, Map<String, String> excelHeaderToClassParamMap, Class<T> clazz, int sheetIndex) {
        List<T> dataList = new ArrayList<>();
        try {
            // 建立檔案輸入流
            FileInputStream file = new FileInputStream(filePath);
            Workbook workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(sheetIndex);

            // Read headers from the first row
            Row headerRow = sheet.getRow(0);
            Map<Integer, String> columnIndexToParamMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String headerCellValue = cell.getStringCellValue().trim();
                    if (excelHeaderToClassParamMap.containsKey(headerCellValue)) {
                        columnIndexToParamMap.put(i, excelHeaderToClassParamMap.get(headerCellValue));
                    }
                }
            }
            if (columnIndexToParamMap.isEmpty()) {
                return dataList;
            }

            // Read data rows
            List<Row> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    boolean hasData = false;
                    for (Cell cell : row) {
                        if (cell.getCellType() != CellType.BLANK) {
                            hasData = true;
                            break;
                        }
                    }
                    if (hasData) {
                        rows.add(row);
                    }
                }
            }
            dataList.addAll(createObjectFromRow(rows, columnIndexToParamMap, clazz));
            log.info("[ExcelUtil] row size : {}", dataList.size());

        } catch (Exception e) {
            log.error("[ExcelUtil] read excel fail : {}", e.getLocalizedMessage());
            return dataList;
        }

        return dataList;
    }

    public static <T> List<T> parseExcelFile(BufferedReader reader, List<String> expectedHeaders, Function<String[], T> recordMapper) {
        List<String> lines = reader.lines().toList();

        if (lines.isEmpty()) {
            return null;
        }

        String headerLine = lines.getFirst();
        String[] headers = headerLine.split("\t");

        if (!validateHeaders(headers, expectedHeaders)) {
            return null;
        }

        List<T> records = new ArrayList<>();
        // Iterate through lines, skipping the header line
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (StringUtils.isNotBlank(line)) {
                String[] columns = line.split("\t");
                T recordData = recordMapper.apply(columns);
                records.add(recordData);
            }
        }

        return records;
    }

    private static <T> List<T> createObjectFromRow(List<Row> rows, Map<Integer, String> columnIndexToParamMap, Class<T> clazz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        List<T> objects = new ArrayList<>();
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        for (Row row : rows) {
            T object = constructor.newInstance();

            for (Map.Entry<Integer, String> entry : columnIndexToParamMap.entrySet()) {
                Integer columnIndex = entry.getKey();
                String fieldName = entry.getValue();
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    // 處理普通字段
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    setField(field, object, cell);
                }
            }

            objects.add(object);
        }
        return objects;
    }

    private static void setField(Field field, Object object, Cell cell) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        Object value = getCellValue(cell, fieldType);
        if (value != null) {
            field.set(object, value);
        }
    }

    private static Object getCellValue(Cell cell, Class<?> fieldType) {
        CellType cellType = cell.getCellType();
        switch (cellType) {
            case NUMERIC:
                return getNumericCellValue(cell, fieldType);
            case STRING:
                return getStringCellValue(cell, fieldType);
            case BOOLEAN:
                return getBooleanCellValue(cell, fieldType);
            case FORMULA:
                return getFormulaCellValue(cell, fieldType);
            default:
                return null;
        }
    }

    private static Object getNumericCellValue(Cell cell, Class<?> fieldType) {
        double numericValue = cell.getNumericCellValue();
        String result = String.valueOf(numericValue);
        if (fieldType == String.class) {
            boolean hasExponential = result.contains("E");
            if (hasExponential) {
                BigDecimal number = BigDecimal.valueOf(cell.getNumericCellValue());
                return number.toPlainString();
            }

            Pattern p = Pattern.compile(".0$");
            Matcher m = p.matcher(result);

            if (m.find()) {
                result = result.replace(".0", "");
            }
            return result;
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return (int) numericValue;
        } else if (fieldType == double.class || fieldType == Double.class) {
            return numericValue;
        } else if (fieldType == BigDecimal.class) {
            return BigDecimal.valueOf(numericValue).setScale(2, RoundingMode.HALF_UP);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return numericValue != 0;
        } else if (fieldType == LocalDateTime.class) {
            return DateUtil.getLocalDateTime(numericValue);
        } else if (fieldType == LocalDate.class) {
            return DateUtil.getLocalDateTime(numericValue).toLocalDate();
        }
        return null;
    }

    private static Object getStringCellValue(Cell cell, Class<?> fieldType) {
        String stringValue = cell.getStringCellValue();
        if (fieldType == String.class) {
            return stringValue;
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(stringValue);
        } else if (fieldType == double.class || fieldType == Double.class) {
            return Double.parseDouble(stringValue);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return stringValue.equals("1") || stringValue.equals("true");
        }
        return null;
    }


    private static Object getBooleanCellValue(Cell cell, Class<?> fieldType) {
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            return cell.getBooleanCellValue();
        }
        return null;
    }

    private static Object getFormulaCellValue(Cell cell, Class<?> fieldType) {
        switch (cell.getCachedFormulaResultType()) {
            case NUMERIC:
                return getNumericCellValue(cell, fieldType);
            case STRING:
                return getStringCellValue(cell, fieldType);
            case BOOLEAN:
                return getBooleanCellValue(cell, fieldType);
            default:
                return null;
        }
    }

    private static boolean validateHeaders(String[] headers, List<String> expectedHeaders) {
        Set<String> headerSet = Arrays.stream(headers)
                .map(String::trim)
                .map(header -> header.replace("\uFEFF", ""))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        for (String expectedHeader : expectedHeaders) {
            if (!headerSet.contains(expectedHeader.toLowerCase())) {
                log.info("Missing header: {}", expectedHeader);
                return false;
            }
        }
        return true;
    }

    public static CellStyle getHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    public static CellStyle getHeaderStyleWithColor(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setRightBorderColor(IndexedColors.WHITE.getIndex());
        return headerStyle;
    }

    public static CellStyle getCenterStyle(Workbook workbook) {
        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return centerStyle;
    }

}
