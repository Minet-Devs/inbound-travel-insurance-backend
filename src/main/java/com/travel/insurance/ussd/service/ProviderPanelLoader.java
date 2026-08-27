package com.travel.insurance.ussd.service;

import com.travel.insurance.ussd.domain.ProviderPanelEntry;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ProviderPanelLoader {

    private static final String EXCEL_FILE = "provider-panel.xlsx";

    private List<ProviderPanelEntry> entries = Collections.emptyList();

    @PostConstruct
    public void load() {
        List<ProviderPanelEntry> loaded = new ArrayList<>();

        try (InputStream is = new ClassPathResource(EXCEL_FILE).getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            loadNairobiSheet(workbook, loaded);
            loadUpcountrySheet(workbook, loaded);

        } catch (Exception e) {
            log.warn("Provider panel file not found at {} — search will return empty results", EXCEL_FILE);
        }

        entries = List.copyOf(loaded);
        log.info("Loaded {} provider panel entries", entries.size());
    }

    private void loadNairobiSheet(Workbook workbook, List<ProviderPanelEntry> loaded) {
        Sheet sheet = workbook.getSheet("NAIROBI COUNTY");
        if (sheet == null) {
            log.warn("Sheet 'NAIROBI COUNTY' not found");
            return;
        }

        String currentArea = "";
        for (int i = 23; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            String colA = getCellString(row, 0);
            String colB = getCellString(row, 1);
            String colC = getCellString(row, 2);
            String colD = getCellString(row, 3);

            if (colB == null || colB.isBlank()) {
                continue;
            }

            if (colA != null && !colA.isBlank()) {
                currentArea = colA.trim();
            }

            loaded.add(new ProviderPanelEntry(
                    currentArea,
                    currentArea,
                    "NAIROBI",
                    colB.trim(),
                    colC != null ? colC.trim() : "",
                    colD != null ? colD.trim() : ""
            ));
        }
    }

    private void loadUpcountrySheet(Workbook workbook, List<ProviderPanelEntry> loaded) {
        Sheet sheet = workbook.getSheet("upcountry");
        if (sheet == null) {
            log.warn("Sheet 'upcountry' not found");
            return;
        }

        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            String town = getCellString(row, 0);
            String county = getCellString(row, 1);
            String providerName = getCellString(row, 2);
            String address = getCellString(row, 3);
            String services = getCellString(row, 4);

            if (providerName == null || providerName.isBlank()) {
                continue;
            }

            loaded.add(new ProviderPanelEntry(
                    town != null ? town.trim() : "",
                    town != null ? town.trim() : "",
                    county != null ? county.trim() : "",
                    providerName.trim(),
                    address != null ? address.trim() : "",
                    services != null ? services.trim() : ""
            ));
        }
    }

    private String getCellString(Row row, int colIndex) {
        var cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    public List<ProviderPanelEntry> getEntries() {
        return entries;
    }
}
