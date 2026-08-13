package com.travel.insurance.medicalservice;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a service/department master-list {@code .xlsx} workbook into rows.
 * Reads the first sheet, locates the {@code service} and {@code department}
 * columns by header name (case-insensitive), and streams the remaining rows.
 * Blank rows are dropped.
 */
@Component
public class MedicalServiceExcelParser {

    private static final String SERVICE_HEADER = "service";
    private static final String DEPARTMENT_HEADER = "department";

    public record MedicalServiceRow(String service, String department) {
    }

    public List<MedicalServiceRow> parse(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("The uploaded workbook has no sheets");
            }
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("The uploaded workbook has no header row");
            }
            int serviceColumn = -1;
            int departmentColumn = -1;
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).trim().toLowerCase();
                if (header.equals(SERVICE_HEADER)) {
                    serviceColumn = cell.getColumnIndex();
                } else if (header.equals(DEPARTMENT_HEADER)) {
                    departmentColumn = cell.getColumnIndex();
                }
            }
            if (serviceColumn < 0 || departmentColumn < 0) {
                throw new IllegalArgumentException(
                        "The uploaded workbook must have 'service' and 'department' header columns");
            }

            List<MedicalServiceRow> rows = new ArrayList<>();
            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String service = formatter.formatCellValue(row.getCell(serviceColumn)).trim();
                String department = formatter.formatCellValue(row.getCell(departmentColumn)).trim();
                if (service.isEmpty() && department.isEmpty()) {
                    continue;
                }
                rows.add(new MedicalServiceRow(service, department));
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded workbook", e);
        }
    }
}