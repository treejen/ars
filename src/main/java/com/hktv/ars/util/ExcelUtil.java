package com.hktv.ars.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class ExcelUtil {

    public static <T> List<T> readResource(String filePath, Map<String, String> excelHeaderToClassParamMap, Class<T> clazz, int sheetIndex) {
        List<T> dataList = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(filePath);
             OPCPackage pkg = OPCPackage.open(inputStream)) {
            XSSFReader reader = new XSSFReader(pkg);
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            StylesTable styles = reader.getStylesTable();

            Iterator<InputStream> sheets = reader.getSheetsData();
            int currentIndex = 0;
            while (sheets.hasNext()) {
                InputStream sheet = sheets.next();
                if (currentIndex++ != sheetIndex) {
                    sheet.close();
                    continue;
                }
                XMLReader parser = XMLReaderFactory.createXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(styles, sst,
                        new ReflectiveSheetHandler<>(clazz, excelHeaderToClassParamMap, dataList), false));
                parser.parse(new InputSource(sheet));
                log.info("[ExcelReadUtil] data size: {}", dataList.size());
                sheet.close();
            }
        } catch (Exception e) {
            log.error("[ExcelReadUtil] Error while reading Excel file", e);
        }

        return dataList;
    }

    private static class ReflectiveSheetHandler<T> implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final Class<T> clazz;
        private final Map<String, String> headerMap;
        private final List<T> dataList;
        private final Map<String, String> currentRow = new LinkedHashMap<>();
        private List<String> headers;
        private int rowNum = 0;

        public ReflectiveSheetHandler(Class<T> clazz, Map<String, String> headerMap, List<T> dataList) {
            this.clazz = clazz;
            this.headerMap = headerMap;
            this.dataList = dataList;
        }

        @Override
        public void startRow(int rowNum) {
            this.rowNum = rowNum;
            currentRow.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if (rowNum == 0) return; // skip header
            try {
                Constructor<T> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                T obj = constructor.newInstance();

                for (Map.Entry<String, String> entry : currentRow.entrySet()) {
                    String header = entry.getKey();
                    String fieldValue = entry.getValue();
                    String fieldName = headerMap.get(header);
                    if (fieldName == null) continue;

                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = convertValue(fieldValue, field.getType());
                    field.set(obj, value);
                }
                dataList.add(obj);
            } catch (Exception e) {
                log.info("[ExcelReadUtil] Error while creating object from row", e);
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (rowNum == 0) {
                if (headers == null) headers = new ArrayList<>();
                headers.add(formattedValue);
            } else {
                String colLetters = cellReference.replaceAll("\\d", "");
                int colIndex = colLetters.charAt(0) - 'A';
                if (headers != null && colIndex < headers.size()) {
                    currentRow.put(headers.get(colIndex), formattedValue);
                }
            }
        }

        private Object convertValue(String raw, Class<?> type) {
            if (type == String.class) return raw;
            if (type == Integer.class || type == int.class) return Integer.parseInt(raw);
            if (type == Double.class || type == double.class) return Double.parseDouble(raw);
            if (type == Boolean.class || type == boolean.class)
                return Boolean.parseBoolean(raw) || Integer.parseInt(raw) == 1;
            if (type == BigDecimal.class) return new BigDecimal(raw);
            return null;
        }
    }
}