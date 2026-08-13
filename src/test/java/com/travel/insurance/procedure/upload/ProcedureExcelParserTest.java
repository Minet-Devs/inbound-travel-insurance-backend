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
        InputStream in = workbook(new String[]{"Procedure Name*", "Department*", "Description"},
                new String[][]{{"Nebulization", "Radiology", "Aerosol therapy"},
                        {"Lumbar Puncture", "Neurology", ""}});

        List<ProcedureExcelRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(
                new ProcedureExcelRow(2, "Nebulization", "Radiology", "Aerosol therapy"),
                new ProcedureExcelRow(3, "Lumbar Puncture", "Neurology", ""));
    }

    @Test
    void matchesHeadersCaseInsensitivelyAndTreatsDescriptionAsOptional() throws IOException {
        InputStream in = workbook(new String[]{"department", "procedure name"},
                new String[][]{{"Radiology", "Chest Tube Insertion"}});

        List<ProcedureExcelRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(new ProcedureExcelRow(2, "Chest Tube Insertion", "Radiology", ""));
    }

    @Test
    void skipsFullyBlankRowsButKeepsTheirNumbering() throws IOException {
        InputStream in = workbook(new String[]{"Procedure Name*", "Department*", "Description"},
                new String[][]{{"Nebulization", "Radiology", ""}, {"", "", ""}, {"Lumbar Puncture", "Neurology", ""}});

        List<ProcedureExcelRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(
                new ProcedureExcelRow(2, "Nebulization", "Radiology", ""),
                new ProcedureExcelRow(4, "Lumbar Puncture", "Neurology", ""));
    }

    @Test
    void throwsWhenProcedureNameHeaderMissing() throws IOException {
        InputStream in = workbook(new String[]{"Name", "Department*"}, new String[][]{{"x", "Radiology"}});

        assertThatThrownBy(() -> parser.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Procedure Name");
    }

    @Test
    void throwsWhenDepartmentHeaderMissing() throws IOException {
        InputStream in = workbook(new String[]{"Procedure Name*", "Description"}, new String[][]{{"Nebulization", "x"}});

        assertThatThrownBy(() -> parser.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Department");
    }

    @Test
    void throwsWhenProcedureNameHeaderDuplicated() throws IOException {
        InputStream in = workbook(new String[]{"Procedure Name*", "Procedure Name*", "Department*"},
                new String[][]{{"a", "b", "Radiology"}});

        assertThatThrownBy(() -> parser.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly once");
    }
}
