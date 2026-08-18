package com.travel.insurance.memberstatement;

import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementTransaction;
import com.travel.insurance.visitor.VisitorStatus;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemberStatementExcelWriterTest {

    private final MemberStatementExcelWriter writer = new MemberStatementExcelWriter();

    private MemberStatementResponse statementWith(
            List<VisitorBenefitResponse> benefits, List<MemberStatementTransaction> transactions) {
        return new MemberStatementResponse(
                UUID.randomUUID(), "Jane Traveler", "P1234567",
                UUID.randomUUID(), "POL-0001", benefits, transactions);
    }

    @Test
    void writesTransactionRowAndSummaryRow() throws IOException {
        VisitorBenefitResponse benefit = new VisitorBenefitResponse(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Medical Expenses", new BigDecimal("20000.00"), new BigDecimal("500.00"),
                new BigDecimal("19500.00"), VisitorStatus.ACTIVE, Instant.now(), Instant.now());
        MemberStatementTransaction transaction = new MemberStatementTransaction(
                UUID.randomUUID(), LocalDate.of(2026, 6, 1), UUID.randomUUID(), "Medical Expenses",
                new BigDecimal("500.00"), UUID.randomUUID(), "Nairobi Hospital");
        MemberStatementResponse statement = statementWith(List.of(benefit), List.of(transaction));

        byte[] bytes = writer.write(statement);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(cellText(sheet, 0, 0)).isEqualTo("MINET KENYA INSURANCE BROKERS");
            assertThat(cellText(sheet, 1, 1)).isEqualTo("Jane Traveler");
            assertThat(cellText(sheet, 2, 1)).isEqualTo("P1234567");

            Row headerRow = findRow(sheet, "MEMBERSHIP NUMBER");
            assertThat(headerRow).isNotNull();
            Row dataRow = sheet.getRow(headerRow.getRowNum() + 1);
            assertThat(cellText(dataRow, 0)).isEqualTo("P1234567");
            assertThat(cellText(dataRow, 5)).isEqualTo("Nairobi Hospital");
            assertThat(dataRow.getCell(4).getNumericCellValue()).isEqualTo(500.00);

            Row summaryHeaderRow = findRow(sheet, "BENEFIT");
            assertThat(summaryHeaderRow).isNotNull();
            Row summaryDataRow = sheet.getRow(summaryHeaderRow.getRowNum() + 1);
            assertThat(cellText(summaryDataRow, 0)).isEqualTo("Medical Expenses");
            assertThat(summaryDataRow.getCell(1).getNumericCellValue()).isEqualTo(20000.00);
            assertThat(summaryDataRow.getCell(2).getNumericCellValue()).isEqualTo(500.00);
            assertThat(summaryDataRow.getCell(3).getNumericCellValue()).isEqualTo(19500.00);
        }
    }

    @Test
    void writesEmptyStateRowWhenNoTransactions() throws IOException {
        MemberStatementResponse statement = statementWith(List.of(), List.of());

        byte[] bytes = writer.write(statement);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row emptyRow = findRow(sheet, "No member statement transaction data found");
            assertThat(emptyRow).isNotNull();
        }
    }

    private Row findRow(Sheet sheet, String firstCellContains) {
        for (Row row : sheet) {
            String text = cellText(row, 0);
            if (text != null && text.contains(firstCellContains)) {
                return row;
            }
        }
        return null;
    }

    private String cellText(Sheet sheet, int rowIndex, int columnIndex) {
        return cellText(sheet.getRow(rowIndex), columnIndex);
    }

    private String cellText(Row row, int columnIndex) {
        if (row == null || row.getCell(columnIndex) == null) {
            return null;
        }
        return row.getCell(columnIndex).getStringCellValue();
    }
}