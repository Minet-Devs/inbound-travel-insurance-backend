package com.travel.insurance.icd11;

import com.travel.insurance.icd11.Icd11ExcelParser.Icd11Row;
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

class Icd11ExcelParserTest {

    private final Icd11ExcelParser parser = new Icd11ExcelParser();

    private InputStream workbook(String[] headers, String[][] dataRows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("codes");
            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                header.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < dataRows.length; r++) {
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
    void parsesCodeAndTitleRows() throws IOException {
        InputStream in = workbook(new String[]{"Code", "Title"},
                new String[][]{{"BA00", "Hypertensive heart disease"}, {"1A00", "Cholera"}});

        List<Icd11Row> rows = parser.parse(in);

        assertThat(rows).containsExactly(
                new Icd11Row("BA00", "Hypertensive heart disease"),
                new Icd11Row("1A00", "Cholera"));
    }

    @Test
    void matchesHeadersCaseInsensitivelyAndAnyColumnOrder() throws IOException {
        InputStream in = workbook(new String[]{"TITLE", "CODE"},
                new String[][]{{"Cholera", "1A00"}});

        List<Icd11Row> rows = parser.parse(in);

        assertThat(rows).containsExactly(new Icd11Row("1A00", "Cholera"));
    }

    @Test
    void skipsFullyBlankRows() throws IOException {
        InputStream in = workbook(new String[]{"Code", "Title"},
                new String[][]{{"BA00", "Hypertensive heart disease"}, {"", ""}});

        List<Icd11Row> rows = parser.parse(in);

        assertThat(rows).containsExactly(new Icd11Row("BA00", "Hypertensive heart disease"));
    }

    @Test
    void throwsWhenRequiredHeadersMissing() throws IOException {
        InputStream in = workbook(new String[]{"Foundation URI", "Description"},
                new String[][]{{"x", "y"}});

        assertThatThrownBy(() -> parser.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }
}
