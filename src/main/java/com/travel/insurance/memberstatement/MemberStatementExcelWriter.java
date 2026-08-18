package com.travel.insurance.memberstatement;

import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementTransaction;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;


@Component
public class MemberStatementExcelWriter {

    private static final String[] TRANSACTION_HEADERS =
            {"MEMBERSHIP NUMBER", "MEMBER NAME", "TRANSACTION DATE", "BENEFIT", "AMOUNT", "SERVICE PROVIDER"};
    private static final String[] SUMMARY_HEADERS = {"BENEFIT", "ALLOCATION", "EXPENDITURE", "BALANCE"};

    public byte[] write(MemberStatementResponse statement) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Member Statement");
            int rowIndex = 0;
            rowIndex = writeHeaderBlock(sheet, rowIndex, statement);
            rowIndex = writeTransactions(sheet, rowIndex, statement);
            writeSummary(sheet, rowIndex, statement);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to generate the member statement workbook", e);
        }
    }

    private int writeHeaderBlock(Sheet sheet, int rowIndex, MemberStatementResponse statement) {
        cell(sheet.createRow(rowIndex++), 0, "MINET KENYA INSURANCE BROKERS");
        Row nameRow = sheet.createRow(rowIndex++);
        cell(nameRow, 0, "MEMBER NAME :");
        cell(nameRow, 1, statement.memberName());
        Row numberRow = sheet.createRow(rowIndex++);
        cell(numberRow, 0, "MEMBER NUMBER :");
        cell(numberRow, 1, statement.passportNumber());
        rowIndex++;
        return rowIndex;
    }

    private int writeTransactions(Sheet sheet, int rowIndex, MemberStatementResponse statement) {
        cell(sheet.createRow(rowIndex++), 0, "MEMBER STATEMENT");
        writeRow(sheet.createRow(rowIndex++), TRANSACTION_HEADERS);
        List<MemberStatementTransaction> transactions = statement.transactions();
        if (transactions.isEmpty()) {
            cell(sheet.createRow(rowIndex++), 0, "No member statement transaction data found");
        } else {
            for (MemberStatementTransaction transaction : transactions) {
                Row row = sheet.createRow(rowIndex++);
                cell(row, 0, statement.passportNumber());
                cell(row, 1, statement.memberName());
                cell(row, 2, nullSafe(transaction.transactionDate()));
                cell(row, 3, nullSafe(transaction.benefitName()));
                row.createCell(4).setCellValue(transaction.amount().doubleValue());
                cell(row, 5, nullSafe(transaction.serviceProviderName()));
            }
        }
        rowIndex++;
        return rowIndex;
    }

    private void writeSummary(Sheet sheet, int rowIndex, MemberStatementResponse statement) {
        cell(sheet.createRow(rowIndex++), 0, statement.memberName() + " SUMMARY UTILIZATION");
        writeRow(sheet.createRow(rowIndex++), SUMMARY_HEADERS);
        for (VisitorBenefitResponse benefit : statement.benefits()) {
            Row row = sheet.createRow(rowIndex++);
            cell(row, 0, benefit.benefitName());
            row.createCell(1).setCellValue(benefit.limitAmount().doubleValue());
            row.createCell(2).setCellValue(benefit.utilizedAmount().doubleValue());
            row.createCell(3).setCellValue(benefit.balance().doubleValue());
        }
    }

    private void writeRow(Row row, String[] values) {
        for (int c = 0; c < values.length; c++) {
            row.createCell(c).setCellValue(values[c]);
        }
    }

    private void cell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}