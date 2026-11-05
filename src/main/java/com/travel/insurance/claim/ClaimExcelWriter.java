package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimExportRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ClaimExcelWriter {

    /** dd-MM-yyyy; claim instants are rendered in UTC, consistent with the export's date filters. */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneOffset.UTC);

    private static final String[] HEADERS = {"Provider", "Provider ID", "Benefit", "Passport No", "ClaimDate",
            "Visit Type", "Diagnosis code", "Diagnosis description", "Visit Date", "AmountClaimed",
            "Invoice Number", "Invoice Date", "Invoice Amount", "Service", "department", "claim amount USD",
            "Exchange Rate"};

    public byte[] write(List<ClaimExportRow> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Claims");
            Row header = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                header.createCell(c).setCellValue(HEADERS[c]);
            }
            int rowIndex = 1;
            for (ClaimExportRow data : rows) {
                Row row = sheet.createRow(rowIndex++);
                cell(row, 0, data.serviceProviderName());
                cell(row, 1, data.serviceProviderId() == null ? null : data.serviceProviderId().toString());
                cell(row, 2, data.benefitName());
                cell(row, 3, data.passportNumber());
                String claimDate = data.claimDate() == null ? null : DATE_FORMAT.format(data.claimDate());
                cell(row, 4, claimDate);
                cell(row, 5, data.visitType() == null ? null : data.visitType().name());
                cell(row, 6, data.diagnosisCodes());
                cell(row, 7, data.diagnosisDescriptions());
                cell(row, 8, data.visitDate() == null ? null : DATE_FORMAT.format(data.visitDate()));
                number(row, 9, data.claimedAmount());
                cell(row, 10, data.invoiceNumber());
                cell(row, 11, claimDate);
                number(row, 12, data.invoiceAmount());
                cell(row, 13, data.serviceName());
                cell(row, 14, data.departmentName());
                number(row, 15, data.claimedAmountBase());
                number(row, 16, data.exchangeRate());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to generate the claims workbook", e);
        }
    }

    private void number(Row row, int column, BigDecimal value) {
        if (value != null) {
            row.createCell(column).setCellValue(value.doubleValue());
        }
    }

    private void cell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }
}
