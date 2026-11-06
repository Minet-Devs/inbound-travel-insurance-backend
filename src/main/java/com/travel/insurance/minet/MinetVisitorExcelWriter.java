package com.travel.insurance.minet;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;

/** Writes visitors in the Minet member-upload layout; dates are {@code yyyy-MM-dd} text, as in the Minet payload. */
@Component
public class MinetVisitorExcelWriter {

    private static final String[] HEADERS = {"memberNumber", "firstName", "lastName", "otherNames", "gender",
            "dateOfBirth", "emailAddress", "primaryPhoneNumber", "residenceCountry", "dateJoined"};

    public byte[] write(List<MinetVisitorRequest> visitors) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Visitors");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIndex = 1;
            for (MinetVisitorRequest v : visitors) {
                Row row = sheet.createRow(rowIndex++);
                cell(row, 0, v.memberNumber());
                cell(row, 1, v.firstName());
                cell(row, 2, v.lastName());
                cell(row, 3, v.otherNames());
                cell(row, 4, v.gender());
                cell(row, 5, date(v.dateOfBirth()));
                cell(row, 6, v.emailAddress());
                cell(row, 7, v.primaryPhoneNumber());
                cell(row, 8, v.residenceCountry());
                cell(row, 9, date(v.dateJoined()));
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write Minet visitors Excel", e);
        }
    }

    private static String date(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private static void cell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }
}
