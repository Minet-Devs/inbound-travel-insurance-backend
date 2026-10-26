package com.travel.insurance.claim;

import com.travel.insurance.claim.dto.ClaimExportRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Component
public class ClaimExcelWriter {

    private static final String[] HEADERS =
            {"Visitor Name", "Passport Number", "Service Provider", "Benefit", "Insurer", "Amount", "Claim Date"};

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
                cell(row, 0, data.visitorName());
                cell(row, 1, data.passportNumber());
                cell(row, 2, data.serviceProviderName());
                cell(row, 3, data.benefitName());
                cell(row, 4, data.insurerName());
                if (data.amount() != null) {
                    row.createCell(5).setCellValue(data.amount().doubleValue());
                }
                cell(row, 6, data.claimDate() == null ? null : data.claimDate().toString());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to generate the claims workbook", e);
        }
    }

    private void cell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }
}
