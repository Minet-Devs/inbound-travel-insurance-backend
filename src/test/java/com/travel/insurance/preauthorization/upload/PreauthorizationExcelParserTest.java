package com.travel.insurance.preauthorization.upload;

import com.travel.insurance.preauthorization.upload.PreauthorizationExcelParser.PreauthorizationExcelRow;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreauthorizationExcelParserTest {

    private static final String[] HEADERS = {"PASSPORT NO.", "NAME OF PROVIDER", "BENEFIT", "VISIT TYPE",
            "DATE OF TREATMENT", "DATE OF DISCHARGE", "AMOUNT"};

    private final PreauthorizationExcelParser parser = new PreauthorizationExcelParser();

    private InputStream workbook(String sheetName, String[] headers, boolean withData) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.createSheet("Summary").createRow(0).createCell(0).setCellValue("not the upload sheet");
            Sheet sheet = wb.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                header.createCell(c).setCellValue(headers[c]);
            }
            if (withData) {
                CellStyle dateStyle = wb.createCellStyle();
                dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

                Row first = sheet.createRow(1);
                first.createCell(0).setCellValue("X1234567");
                first.createCell(1).setCellValue("Nairobi Hospital");
                first.createCell(2).setCellValue("Medical Expenses");
                first.createCell(3).setCellValue("OUTPATIENT");
                first.createCell(4).setCellValue(java.sql.Date.valueOf(LocalDate.of(2026, 10, 10)));
                first.getCell(4).setCellStyle(dateStyle);
                first.createCell(5).setCellValue("19/10/2026");
                first.createCell(6).setCellValue(1234.5);

                Row numericPassport = sheet.createRow(2);
                numericPassport.createCell(0).setCellValue(987654321d);
                numericPassport.createCell(1).setCellValue("Nairobi Hospital");
                numericPassport.createCell(2).setCellValue("Medical Expenses");
                numericPassport.createCell(6).setCellValue("TBD");

                // totals row: amount only, no passport / provider / benefit
                sheet.createRow(3).createCell(6).setCellValue(1000000.00);
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Test
    void parsesPreauthUploadSheetPreservingRowNumbersAndNormalisingDates() throws IOException {
        List<PreauthorizationExcelRow> rows = parser.parse(workbook("preauth-upload", HEADERS, true));

        assertThat(rows).containsExactly(
                new PreauthorizationExcelRow(2, "X1234567", "Nairobi Hospital", "Medical Expenses",
                        "OUTPATIENT", "2026-10-10", "19/10/2026", "1234.5"),
                new PreauthorizationExcelRow(3, "987654321", "Nairobi Hospital", "Medical Expenses",
                        "", "", "", "TBD"));
    }

    @Test
    void fallsBackToFirstSheetWhenPreauthUploadSheetIsAbsent() throws IOException {
        // "Summary" is the first sheet and has no matching headers
        assertThatThrownBy(() -> parser.parse(workbook("other", HEADERS, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PASSPORT NO.");
    }

    @Test
    void rejectsWorkbookMissingRequiredHeader() {
        String[] headers = {"PASSPORT NO.", "NAME OF PROVIDER", "BENEFIT"};

        assertThatThrownBy(() -> parser.parse(workbook("preauth-upload", headers, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AMOUNT");
    }
}
