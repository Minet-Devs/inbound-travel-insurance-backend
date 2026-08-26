package com.travel.insurance.ussd;

import com.travel.insurance.ussd.domain.ProviderPanelEntry;
import com.travel.insurance.ussd.service.ProviderPanelLoader;
import com.travel.insurance.ussd.service.ProviderPanelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderPanelServiceTest {

    private ProviderPanelService service;

    @BeforeEach
    void setUp() {
        ProviderPanelLoader loader = new ProviderPanelLoader();
        loader.load();
        service = new ProviderPanelService(loader);
    }

    @Test
    void loadsEntriesFromExcel() {
        assertThat(service.totalEntries()).isGreaterThan(0);
    }

    @Test
    void searchByCountyReturnsMatchingEntries() {
        List<ProviderPanelEntry> results = service.searchByCounty("Nairobi");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(e -> e.getCounty().toUpperCase().contains("NAIROBI"));
    }

    @Test
    void searchByCountyIsCaseInsensitive() {
        List<ProviderPanelEntry> upper = service.searchByCounty("NAIROBI");
        List<ProviderPanelEntry> lower = service.searchByCounty("nairobi");
        List<ProviderPanelEntry> mixed = service.searchByCounty("NaiRoBi");

        assertThat(upper).hasSameSizeAs(lower);
        assertThat(lower).hasSameSizeAs(mixed);
    }

    @Test
    void searchByCountyPartialMatch() {
        List<ProviderPanelEntry> results = service.searchByCounty("Momb");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(e -> e.getCounty().toLowerCase().contains("momb"));
    }

    @Test
    void searchByCountyNoMatch() {
        List<ProviderPanelEntry> results = service.searchByCounty("XYZNONEXISTENT");

        assertThat(results).isEmpty();
    }

    @Test
    void searchByTownReturnsMatchingEntries() {
        List<ProviderPanelEntry> results = service.searchByTown("Karen");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(e -> e.getTown().toLowerCase().contains("karen"));
    }

    @Test
    void searchByTownIsCaseInsensitive() {
        List<ProviderPanelEntry> upper = service.searchByTown("KAREN");
        List<ProviderPanelEntry> lower = service.searchByTown("karen");

        assertThat(upper).hasSameSizeAs(lower);
    }

    @Test
    void searchByTownPartialMatch() {
        List<ProviderPanelEntry> results = service.searchByTown("Bur");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(e -> e.getTown().toLowerCase().contains("bur"));
    }

    @Test
    void searchByTownNoMatch() {
        List<ProviderPanelEntry> results = service.searchByTown("XYZNONEXISTENT");

        assertThat(results).isEmpty();
    }

    @Test
    void searchByCountyReturnsNairobiAreaProviders() {
        List<ProviderPanelEntry> results = service.searchByCounty("NAIROBI");

        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(e -> e.getArea() != null && !e.getArea().isBlank());
    }

    @Test
    void searchByTownFindsUpcountryProviders() {
        List<ProviderPanelEntry> results = service.searchByTown("Bomet");

        assertThat(results).isNotEmpty();
    }
}
