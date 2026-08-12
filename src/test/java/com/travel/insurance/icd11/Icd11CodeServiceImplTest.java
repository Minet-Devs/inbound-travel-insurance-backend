package com.travel.insurance.icd11;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.icd11.Icd11ExcelParser.Icd11Row;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.icd11.dto.Icd11ImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Icd11CodeServiceImplTest {

    @Mock
    private Icd11CodeRepository icd11CodeRepository;

    @Mock
    private Icd11ExcelParser icd11ExcelParser;

    private final Icd11CodeMapper icd11CodeMapper = new Icd11CodeMapper();

    private Icd11CodeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new Icd11CodeServiceImpl(icd11CodeRepository, icd11CodeMapper, icd11ExcelParser);
    }

    private MultipartFile anyXlsx() {
        return new MockMultipartFile("file", "codes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
    }

    @Test
    void importInsertsNewCodesAndUpdatesExisting() {
        when(icd11ExcelParser.parse(any())).thenReturn(List.of(
                new Icd11Row("BA00", "Hypertensive heart disease"),
                new Icd11Row("1A00", "Cholera")));
        when(icd11CodeRepository.findByCode("BA00")).thenReturn(Optional.empty());
        Icd11Code existing = new Icd11Code();
        existing.setCode("1A00");
        existing.setTitle("Old title");
        when(icd11CodeRepository.findByCode("1A00")).thenReturn(Optional.of(existing));
        when(icd11CodeRepository.save(any(Icd11Code.class))).thenAnswer(i -> i.getArgument(0));

        Icd11ImportResult result = service.importFromExcel(anyXlsx());

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(existing.getTitle()).isEqualTo("Cholera");
    }

    @Test
    void importSkipsRowsWithBlankCodeOrTitle() {
        when(icd11ExcelParser.parse(any())).thenReturn(List.of(
                new Icd11Row("BA00", "Hypertensive heart disease"),
                new Icd11Row("", "orphan title"),
                new Icd11Row("1A00", "")));
        when(icd11CodeRepository.findByCode("BA00")).thenReturn(Optional.empty());
        when(icd11CodeRepository.save(any(Icd11Code.class))).thenAnswer(i -> i.getArgument(0));

        Icd11ImportResult result = service.importFromExcel(anyXlsx());

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);
        verify(icd11CodeRepository).save(any(Icd11Code.class));
    }

    @Test
    void importRejectsEmptyFile() {
        MultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> service.importFromExcel(empty))
                .isInstanceOf(IllegalArgumentException.class);
        verify(icd11ExcelParser, never()).parse(any());
    }

    @Test
    void searchWithBlankQueryListsAll() {
        Icd11Code code = new Icd11Code();
        code.setCode("BA00");
        code.setTitle("Hypertensive heart disease");
        Page<Icd11Code> page = new PageImpl<>(List.of(code));
        when(icd11CodeRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<Icd11CodeResponse> result = service.search("  ", Pageable.unpaged());

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("BA00");
        verify(icd11CodeRepository).findAll(any(Pageable.class));
    }

    @Test
    void searchWithQueryDelegatesToRepository() {
        Page<Icd11Code> page = new PageImpl<>(List.of());
        when(icd11CodeRepository.findByCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
                "chol", "chol", Pageable.unpaged())).thenReturn(page);

        service.search("chol", Pageable.unpaged());

        verify(icd11CodeRepository)
                .findByCodeContainingIgnoreCaseOrTitleContainingIgnoreCase("chol", "chol", Pageable.unpaged());
    }

    @Test
    void searchByTitleDelegatesToRepository() {
        Icd11Code code = new Icd11Code();
        code.setCode("1A07");
        code.setTitle("Salmonella infection");
        Page<Icd11Code> page = new PageImpl<>(List.of(code));
        when(icd11CodeRepository.findByTitleContainingIgnoreCase("Salmonella", Pageable.unpaged()))
                .thenReturn(page);

        Page<Icd11CodeResponse> result = service.searchByTitle("Salmonella", Pageable.unpaged());

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Salmonella infection");
        verify(icd11CodeRepository).findByTitleContainingIgnoreCase("Salmonella", Pageable.unpaged());
    }

    @Test
    void searchByTitleRejectsBlankTitle() {
        assertThatThrownBy(() -> service.searchByTitle("  ", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class);
        verify(icd11CodeRepository, never()).findByTitleContainingIgnoreCase(any(), any());
    }

    @Test
    void getByCodeThrowsWhenMissing() {
        when(icd11CodeRepository.findByCode("ZZZZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByCode("ZZZZ"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
