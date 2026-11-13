package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimExportRow;
import com.travel.insurance.common.domain.VisitType;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimExcelWriterTest {

    private final ClaimExcelWriter writer = new ClaimExcelWriter();

    @Test
    void writesHeadersAndOneRowPerExportRow() throws Exception {
        Instant claimDate = Instant.parse("2026-10-05T08:30:00Z");
        UUID providerId = UUID.randomUUID();
        byte[] bytes = writer.write(List.of(new ClaimExportRow("Aga Khan Hospital", providerId, "Outpatient",
                "P1234567", claimDate, VisitType.OUTPATIENT, "1A00", "Cholera", LocalDate.of(2026, 10, 3),
                new BigDecimal("1000.50"), "INV-1", new BigDecimal("1000.50"),
                "Consultation", "General Medicine", new BigDecimal("7.70"), new BigDecimal("0.007700"), ClaimStatus.PARTIALLY_APPROVED)));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(0).cellIterator()).toIterable().extracting(c -> c.getStringCellValue())
                    .containsExactly("Provider", "Provider ID", "Benefit", "Passport No", "ClaimDate",
                            "Visit Type", "Diagnosis code", "Diagnosis description", "Visit Date",
                            "AmountClaimed", "Invoice Number", "Invoice Date", "Invoice Amount", "Service",
                            "department", "claim amount USD", "Exchange Rate", "Status");
            var row = sheet.getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEqualTo("Aga Khan Hospital");
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo(providerId.toString());
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("Outpatient");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("P1234567");
            assertThat(row.getCell(4).getStringCellValue()).isEqualTo("05-10-2026");
            assertThat(row.getCell(5).getStringCellValue()).isEqualTo("OUTPATIENT");
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("1A00");
            assertThat(row.getCell(7).getStringCellValue()).isEqualTo("Cholera");
            assertThat(row.getCell(8).getStringCellValue()).isEqualTo("03-10-2026");
            assertThat(row.getCell(9).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(9).getNumericCellValue()).isEqualTo(1000.50);
            assertThat(row.getCell(10).getStringCellValue()).isEqualTo("INV-1");
            assertThat(row.getCell(11).getStringCellValue()).isEqualTo("05-10-2026");
            assertThat(row.getCell(12).getNumericCellValue()).isEqualTo(1000.50);
            assertThat(row.getCell(13).getStringCellValue()).isEqualTo("Consultation");
            assertThat(row.getCell(14).getStringCellValue()).isEqualTo("General Medicine");
            assertThat(row.getCell(15).getNumericCellValue()).isEqualTo(7.70);
            assertThat(row.getCell(16).getNumericCellValue()).isEqualTo(0.0077);
            assertThat(row.getCell(17).getStringCellValue()).isEqualTo("PARTIALLY_APPROVED");
        }
    }

    @Test
    void leavesCellsBlankForMissingValues() throws Exception {
        byte[] bytes = writer.write(List.of(new ClaimExportRow(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null)));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(0).getStringCellValue()).isEmpty();
            assertThat(row.getCell(9)).isNull();
            assertThat(row.getCell(16)).isNull();
            assertThat(row.getCell(17).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void writesOnlyHeadersWhenThereAreNoClaims() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(writer.write(List.of())))) {
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
        }
    }
}
