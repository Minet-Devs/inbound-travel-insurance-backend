package com.travel.insurance.procedure.upload;

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
 * Parses a procedure upload {@code .xlsx} into rows, preserving the actual Excel
 * row number. The template has a single required {@code Procedure Name} column and
 * an optional {@code Description} column; the department is supplied outside the
 * file. Fully blank rows are ignored. Formulas are never evaluated — cached cell
 * values are read via {@link DataFormatter}. File-level problems (missing sheet,
 * missing/duplicated header) raise {@link IllegalArgumentException}.
 */
@Component
public class ProcedureExcelParser {

    private static final String NAME_HEADER = "procedure name";
    private static final String DESCRIPTION_HEADER = "description";

    /** A single spreadsheet row. {@code excelRowNumber} is 1-based (as shown in Excel). */
    public record ProcedureExcelRow(int excelRowNumber, String name, String description) {
    }

    public List<ProcedureExcelRow> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("The uploaded workbook has no worksheet");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("The uploaded workbook has no header row");
            }

            int nameColumn = -1;
            int descriptionColumn = -1;
            int nameHeaderCount = 0;
            for (Cell cell : headerRow) {
                String header = normalizeHeader(formatter.formatCellValue(cell));
                if (header.equals(NAME_HEADER)) {
                    nameHeaderCount++;
                    nameColumn = cell.getColumnIndex();
                } else if (header.equals(DESCRIPTION_HEADER)) {
                    descriptionColumn = cell.getColumnIndex();
                }
            }
            if (nameHeaderCount == 0) {
                throw new IllegalArgumentException(
                        "The uploaded workbook must have a 'Procedure Name' header column");
            }
            if (nameHeaderCount > 1) {
                throw new IllegalArgumentException(
                        "The 'Procedure Name' header column must appear exactly once");
            }

            List<ProcedureExcelRow> rows = new ArrayList<>();
            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = formatter.formatCellValue(row.getCell(nameColumn));
                String description = descriptionColumn < 0
                        ? "" : formatter.formatCellValue(row.getCell(descriptionColumn));
                if (name.isBlank() && description.isBlank()) {
                    continue;
                }
                rows.add(new ProcedureExcelRow(i + 1, name, description));
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded workbook", e);
        }
    }

    private String normalizeHeader(String header) {
        // Drop the required-field asterisk (e.g. "Procedure Name*") before matching.
        return header == null ? "" : header.replace("*", "").trim().toLowerCase();
    }
}
