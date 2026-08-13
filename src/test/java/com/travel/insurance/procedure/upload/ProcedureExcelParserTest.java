package com.travel.insurance.procedure.upload;

import com.travel.insurance.procedure.upload.ProcedureExcelParser.ProcedureExcelRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcedureExcelParserTest {

    private final ProcedureExcelParser parser = new ProcedureExcelParser();

    private InputStream workbook(String[] headers, String[][] dataRows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Procedures");
            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                header.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < dataRows.length; r++) {
                if (dataRows[r] == null) {
                    continue;
                }
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < dataRows[r].length; c++) {
                    row.createCell(c).setCellValue(dataRows[r][c]);
                }
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Test
    void parsesRowsAndPreservesExcelRowNumbers() throws IOException {
        InputStream in = workbook(new String[]{"Procedure Name*", "Description"},
                new String[][]{{"Nebulization", "Aerosol therapy"}, {"Lumbar Puncture", ""}});

        List<ProcedureExcelRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(
                new ProcedureExcelRow(2, "Nebulization", "Aerosol therapy"),
                new ProcedureExcelRow(3, "Lumbar Puncture", ""));
    }

    @Test
    void matchesHeaderCaseInsensitivelyAndTreatsDescriptionAsOptional() throws IOException {
        InputStream in = workbook(new String[]{"procedure name"},
                new String[][]{{"Chest Tube Insertion"}});

        List<ProcedureExcelRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(new ProcedureExcelRow(2, "Chest Tube Insertion", ""));
    }

    @Test
    void skipsFullyBlankRowsButKeepsTheirNumbering() throws IOException {
        InputStream in = workbook(new String[]{"Procedure Name*", "Description"},
                new String[][]{{"Nebulization", ""}, {"", ""}, {"Lumbar Puncture", ""}});

        List<ProcedureExcelRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(
                new ProcedureExcelRow(2, "Nebulization", ""),
                new ProcedureExcelRow(4, "Lumbar Puncture", ""));
    }

    @Test
    void throwsWhenProcedureNameHeaderMissing() throws IOException {
        InputStream in = workbook(new String[]{"Name", "Description"}, new String[][]{{"x", "y"}});

        assertThatThrownBy(() -> parser.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Procedure Name");
    }

    @Test
    void throwsWhenProcedureNameHeaderDuplicated() throws IOException {
        InputStream in = workbook(new String[]{"Procedure Name*", "Procedure Name*"},
                new String[][]{{"a", "b"}});

        assertThatThrownBy(() -> parser.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly once");
    }
}
