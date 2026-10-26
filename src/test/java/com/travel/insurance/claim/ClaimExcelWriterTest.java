package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimExportRow;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimExcelWriterTest {

    private final ClaimExcelWriter writer = new ClaimExcelWriter();

    @Test
    void writesHeadersAndOneRowPerClaim() throws Exception {
        Instant claimDate = Instant.parse("2026-10-05T08:30:00Z");
        byte[] bytes = writer.write(List.of(new ClaimExportRow("Jane Traveler", "P1234567",
                "Aga Khan Hospital", "Outpatient", "Jubilee Insurance", new BigDecimal("1000.50"), claimDate)));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(0).cellIterator()).toIterable().extracting(c -> c.getStringCellValue())
                    .containsExactly("Visitor Name", "Passport Number", "Service Provider", "Benefit",
                            "Insurer", "Amount", "Claim Date");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Jane Traveler");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("P1234567");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Aga Khan Hospital");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Outpatient");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("Jubilee Insurance");
            assertThat(sheet.getRow(1).getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(1000.50);
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo(claimDate.toString());
        }
    }

    @Test
    void writesOnlyHeadersWhenThereAreNoClaims() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(writer.write(List.of())))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
        }
    }
}
