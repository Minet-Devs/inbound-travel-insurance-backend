package com.travel.insurance.preauthorization.upload;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an offline-approvals {@code .xlsx} into raw string rows, preserving the actual Excel
 * row number. Reads the {@code preauth-upload} sheet when present, otherwise the first sheet.
 * Columns are matched by header, case-insensitively: {@code PASSPORT NO.}, {@code NAME OF PROVIDER},
 * {@code BENEFIT}, {@code VISIT TYPE}, {@code DATE OF TREATMENT}, {@code DATE OF DISCHARGE} and
 * {@code AMOUNT}. Date cells are normalised to ISO ({@code yyyy-MM-dd}); text cells are passed
 * through untouched. Rows with no passport, provider or benefit (blank or total rows) are ignored.
 * File-level problems (missing header column) raise {@link IllegalArgumentException}.
 */
@Component
public class PreauthorizationExcelParser {

    static final String SHEET_NAME = "preauth-upload";

    private static final String PASSPORT = "passport no.";
    private static final String PROVIDER = "name of provider";
    private static final String BENEFIT = "benefit";
    private static final String VISIT_TYPE = "visit type";
    private static final String TREATMENT_DATE = "date of treatment";
    private static final String DISCHARGE_DATE = "date of discharge";
    private static final String AMOUNT = "amount";

    /** A single spreadsheet row. {@code excelRowNumber} is 1-based (as shown in Excel). */
    public record PreauthorizationExcelRow(int excelRowNumber, String passportNumber, String providerName,
                                           String benefit, String visitType, String treatmentDate,
                                           String dischargeDate, String amount) {
    }

    public List<PreauthorizationExcelRow> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("The uploaded workbook has no worksheet");
            }
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("The uploaded workbook has no header row");
            }
            int passportCol = -1;
            int providerCol = -1;
            int benefitCol = -1;
            int visitTypeCol = -1;
            int treatmentCol = -1;
            int dischargeCol = -1;
            int amountCol = -1;
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).replace("*", "").trim().toLowerCase();
                switch (header) {
                    case PASSPORT -> passportCol = cell.getColumnIndex();
                    case PROVIDER -> providerCol = cell.getColumnIndex();
                    case BENEFIT -> benefitCol = cell.getColumnIndex();
                    case VISIT_TYPE -> visitTypeCol = cell.getColumnIndex();
                    case TREATMENT_DATE -> treatmentCol = cell.getColumnIndex();
                    case DISCHARGE_DATE -> dischargeCol = cell.getColumnIndex();
                    case AMOUNT -> amountCol = cell.getColumnIndex();
                    default -> { }
                }
            }
            require(passportCol, "PASSPORT NO.");
            require(providerCol, "NAME OF PROVIDER");
            require(benefitCol, "BENEFIT");
            require(amountCol, "AMOUNT");

            List<PreauthorizationExcelRow> rows = new ArrayList<>();
            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String passport = text(row, passportCol, formatter);
                String provider = text(row, providerCol, formatter);
                String benefit = text(row, benefitCol, formatter);
                if (passport.isBlank() && provider.isBlank() && benefit.isBlank()) {
                    continue;
                }
                rows.add(new PreauthorizationExcelRow(i + 1, passport, provider, benefit,
                        text(row, visitTypeCol, formatter), text(row, treatmentCol, formatter),
                        text(row, dischargeCol, formatter), amount(row, amountCol, formatter)));
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded workbook", e);
        }
    }

    private void require(int column, String header) {
        if (column < 0) {
            throw new IllegalArgumentException("The uploaded workbook must have a '" + header + "' header column");
        }
    }

    private String text(Row row, int column, DataFormatter formatter) {
        if (column < 0) {
            return "";
        }
        Cell cell = row.getCell(column);
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private String amount(Row row, int column, DataFormatter formatter) {
        Cell cell = row.getCell(column);
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
        }
        return text(row, column, formatter);
    }
}
