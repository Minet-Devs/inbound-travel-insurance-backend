package com.travel.insurance.medicalservice;

import com.travel.insurance.medicalservice.MedicalServiceExcelParser.MedicalServiceRow;
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

class MedicalServiceExcelParserTest {

    private final MedicalServiceExcelParser parser = new MedicalServiceExcelParser();

    private InputStream workbook(String[] headers, String[][] dataRows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("services");
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
    void parsesServiceAndDepartmentRows() throws IOException {
        InputStream in = workbook(new String[]{"service", "department"},
                new String[][]{{"PHARMACY GENERAL", "PHARMACY"}, {"LABORATORY GENERAL", "LABORATORY"}});

        List<MedicalServiceRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(
                new MedicalServiceRow("PHARMACY GENERAL", "PHARMACY"),
                new MedicalServiceRow("LABORATORY GENERAL", "LABORATORY"));
    }

    @Test
    void matchesHeadersCaseInsensitivelyAndAnyColumnOrder() throws IOException {
        InputStream in = workbook(new String[]{"DEPARTMENT", "SERVICE"},
                new String[][]{{"LABORATORY", "ADENO VIRUS TEST"}});

        List<MedicalServiceRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(new MedicalServiceRow("ADENO VIRUS TEST", "LABORATORY"));
    }

    @Test
    void skipsFullyBlankRows() throws IOException {
        InputStream in = workbook(new String[]{"service", "department"},
                new String[][]{{"PHARMACY GENERAL", "PHARMACY"}, {"", ""}});

        List<MedicalServiceRow> rows = parser.parse(in);

        assertThat(rows).containsExactly(new MedicalServiceRow("PHARMACY GENERAL", "PHARMACY"));
    }

    @Test
    void throwsWhenRequiredHeadersMissing() throws IOException {
        InputStream in = workbook(new String[]{"Name", "Category"},
                new String[][]{{"x", "y"}});

        assertThatThrownBy(() -> parser.parse(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("service");
    }
}