package com.travel.insurance.ussd.service;

import com.travel.insurance.ussd.domain.ProviderPanelEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProviderPanelService {

    private final ProviderPanelLoader loader;

    public ProviderPanelService(ProviderPanelLoader loader) {
        this.loader = loader;
    }

    public List<ProviderPanelEntry> searchByCounty(String query) {
        String q = normalise(query);
        return loader.getEntries().stream()
                .filter(e -> normalise(e.getCounty()).contains(q))
                .collect(Collectors.toList());
    }

    public List<ProviderPanelEntry> searchByTown(String query) {
        String q = normalise(query);
        return loader.getEntries().stream()
                .filter(e -> normalise(e.getTown()).contains(q))
                .collect(Collectors.toList());
    }

    public int totalEntries() {
        return loader.getEntries().size();
    }

    private String normalise(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
