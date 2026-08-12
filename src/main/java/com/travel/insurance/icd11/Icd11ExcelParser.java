package com.travel.insurance.icd11;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an ICD-11 {@code .xlsx} workbook into code/title rows. Reads the first
 * sheet, locates the {@code code} and {@code title} columns by header name
 * (case-insensitive), and streams the remaining rows. Blank rows are dropped.
 */
@Component
public class Icd11ExcelParser {

    private static final String CODE_HEADER = "code";
    private static final String TITLE_HEADER = "title";

    public record Icd11Row(String code, String title) {
    }

    public List<Icd11Row> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("The uploaded workbook has no sheets");
            }
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("The uploaded workbook has no header row");
            }
            int codeColumn = -1;
            int titleColumn = -1;
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).trim().toLowerCase();
                if (header.equals(CODE_HEADER)) {
                    codeColumn = cell.getColumnIndex();
                } else if (header.equals(TITLE_HEADER)) {
                    titleColumn = cell.getColumnIndex();
                }
            }
            if (codeColumn < 0 || titleColumn < 0) {
                throw new IllegalArgumentException(
                        "The uploaded workbook must have 'code' and 'title' header columns");
            }

            List<Icd11Row> rows = new ArrayList<>();
            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String code = formatter.formatCellValue(row.getCell(codeColumn)).trim();
                String title = formatter.formatCellValue(row.getCell(titleColumn)).trim();
                if (code.isEmpty() && title.isEmpty()) {
                    continue;
                }
                rows.add(new Icd11Row(code, title));
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded workbook", e);
        }
    }
}
