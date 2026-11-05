package com.travel.insurance.minet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MinetVisitorExcelWriterTest {

    private final MinetVisitorExcelWriter writer = new MinetVisitorExcelWriter();

    @Test
    void writesMinetHeadersAndOneRowPerVisitor() throws Exception {
        byte[] bytes = writer.write(List.of(new MinetVisitorRequest("A1234567", "GARIMA", "THAPLIYAL", "KUMARI",
                "FEMALE", LocalDate.of(1994, 7, 1), LocalDate.of(2026, 11, 6), "g@example.com",
                "+254791531926", "IND")));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(0).cellIterator()).toIterable().extracting(c -> c.getStringCellValue())
                    .containsExactly("memberNumber", "firstName", "lastName", "otherNames", "gender",
                            "dateOfBirth", "emailAddress", "primaryPhoneNumber", "residenceCountry", "dateJoined");
            assertThat(sheet.getRow(1).cellIterator()).toIterable().extracting(c -> c.getStringCellValue())
                    .containsExactly("A1234567", "GARIMA", "THAPLIYAL", "KUMARI", "FEMALE", "1994-07-01",
                            "g@example.com", "+254791531926", "IND", "2026-11-06");
        }
    }

    @Test
    void leavesMissingValuesBlank() throws Exception {
        byte[] bytes = writer.write(List.of(new MinetVisitorRequest("A1", "ANNA", "", null, null, null, null,
                null, null, null)));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(3).getStringCellValue()).isEmpty();
            assertThat(row.getCell(5).getStringCellValue()).isEmpty();
            assertThat(row.getCell(9).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void writesOnlyHeadersWhenThereAreNoVisitors() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(writer.write(List.of())))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
        }
    }
}
