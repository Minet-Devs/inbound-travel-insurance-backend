package com.travel.insurance.touristattraction;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.touristattraction.dto.TouristAttractionRequest;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TouristAttractionServiceImplTest {

    @Mock
    private TouristAttractionRepository touristAttractionRepository;

    private final TouristAttractionMapper touristAttractionMapper = new TouristAttractionMapper();

    private TouristAttractionServiceImpl touristAttractionService;

    private TouristAttractionRequest request;

    @BeforeEach
    void setUp() {
        touristAttractionService = new TouristAttractionServiceImpl(touristAttractionRepository,
                touristAttractionMapper);
        request = new TouristAttractionRequest("Lake Nakuru National Park", "Nakuru");
    }

    private TouristAttraction attraction(String name, String county) {
        TouristAttraction attraction = new TouristAttraction();
        attraction.setId(UUID.randomUUID());
        attraction.setName(name);
        attraction.setCounty(county);
        return attraction;
    }

    private void givenAttractions(TouristAttraction... attractions) {
        when(touristAttractionRepository.findAll(any(Sort.class))).thenReturn(List.of(attractions));
    }

    @Test
    void createSavesAndReturnsAttraction() {
        when(touristAttractionRepository.existsByNameIgnoreCase("Lake Nakuru National Park")).thenReturn(false);
        when(touristAttractionRepository.save(any(TouristAttraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TouristAttractionResponse response = touristAttractionService.create(request);

        assertThat(response.name()).isEqualTo("Lake Nakuru National Park");
        assertThat(response.county()).isEqualTo("Nakuru");
        verify(touristAttractionRepository).save(any(TouristAttraction.class));
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(touristAttractionRepository.existsByNameIgnoreCase("Lake Nakuru National Park")).thenReturn(true);

        assertThatThrownBy(() -> touristAttractionService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lake Nakuru National Park");
        verify(touristAttractionRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(touristAttractionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> touristAttractionService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        UUID id = UUID.randomUUID();
        TouristAttraction existing = touristAttractionMapper.toEntity(request);
        when(touristAttractionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(touristAttractionRepository.existsByNameIgnoreCaseAndIdNot("Maasai Mara", id)).thenReturn(false);
        when(touristAttractionRepository.save(any(TouristAttraction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TouristAttractionResponse response =
                touristAttractionService.update(id, new TouristAttractionRequest("Maasai Mara", "Narok"));

        assertThat(response.name()).isEqualTo("Maasai Mara");
        assertThat(response.county()).isEqualTo("Narok");
    }

    @Test
    void updateRejectsNameUsedByAnotherAttraction() {
        UUID id = UUID.randomUUID();
        when(touristAttractionRepository.findById(id))
                .thenReturn(Optional.of(touristAttractionMapper.toEntity(request)));
        when(touristAttractionRepository.existsByNameIgnoreCaseAndIdNot("Maasai Mara", id)).thenReturn(true);

        assertThatThrownBy(() -> touristAttractionService.update(id,
                new TouristAttractionRequest("Maasai Mara", "Narok")))
                .isInstanceOf(IllegalStateException.class);
        verify(touristAttractionRepository, never()).save(any());
    }

    @Test
    void deleteRemovesEntity() {
        UUID id = UUID.randomUUID();
        TouristAttraction existing = touristAttractionMapper.toEntity(request);
        when(touristAttractionRepository.findById(id)).thenReturn(Optional.of(existing));

        touristAttractionService.delete(id);

        verify(touristAttractionRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(touristAttractionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> touristAttractionService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(touristAttractionRepository, never()).delete(any());
    }

    @Test
    void searchExactNameIgnoresCaseAndWinsOverPartialMatches() {
        givenAttractions(
                attraction("Nakuru", "Nakuru"),
                attraction("Nakuru Wildlife Conservancy", "Nakuru"),
                attraction("Lake Nakuru National Park", "Nakuru"));

        List<TouristAttractionResponse> results = touristAttractionService.searchByName("  NAKURU ", 3);

        assertThat(results).extracting(TouristAttractionResponse::name).containsExactly("Nakuru");
    }

    @Test
    void searchPartialNameReturnsContainsMatchesInNameOrder() {
        givenAttractions(
                attraction("Amboseli National Park", "Kajiado"),
                attraction("Lake Nakuru National Park", "Nakuru"),
                attraction("Tsavo National Park", "Taita Taveta"));

        List<TouristAttractionResponse> results = touristAttractionService.searchByName("national park", 5);

        assertThat(results).extracting(TouristAttractionResponse::name)
                .containsExactly("Amboseli National Park", "Lake Nakuru National Park", "Tsavo National Park");
    }

    @Test
    void searchToleratesTyposAndRanksClosestFirst() {
        givenAttractions(
                attraction("Amboseli National Park", "Kajiado"),
                attraction("Lake Nakuru National Park", "Nakuru"));

        List<TouristAttractionResponse> results = touristAttractionService.searchByName("nakuru natonal", 3);

        assertThat(results).extracting(TouristAttractionResponse::name)
                .containsExactly("Lake Nakuru National Park");
        assertThat(results.get(0).county()).isEqualTo("Nakuru");
    }

    @Test
    void searchMisspeltSingleWordStillResolves() {
        givenAttractions(
                attraction("Maasai Mara National Reserve", "Narok"),
                attraction("Amboseli National Park", "Kajiado"));

        List<TouristAttractionResponse> results = touristAttractionService.searchByName("masai mara", 3);

        assertThat(results).extracting(TouristAttractionResponse::name)
                .containsExactly("Maasai Mara National Reserve");
    }

    @Test
    void searchReturnsEmptyWhenNothingIsCloseEnough() {
        givenAttractions(attraction("Amboseli National Park", "Kajiado"));

        assertThat(touristAttractionService.searchByName("zzzzzzzz", 3)).isEmpty();
    }

    @Test
    void searchDoesNotGuessOnVeryShortQueries() {
        givenAttractions(attraction("Amboseli National Park", "Kajiado"));

        assertThat(touristAttractionService.searchByName("xyz", 3)).isEmpty();
    }

    @Test
    void searchRespectsLimit() {
        givenAttractions(
                attraction("A National Park", "X"),
                attraction("B National Park", "X"),
                attraction("C National Park", "X"),
                attraction("D National Park", "X"));

        assertThat(touristAttractionService.searchByName("national park", 3)).hasSize(3);
    }

    @Test
    void searchReturnsEmptyForBlankQuery() {
        assertThat(touristAttractionService.searchByName("   ", 3)).isEmpty();
        assertThat(touristAttractionService.searchByName(null, 3)).isEmpty();
        verify(touristAttractionRepository, never()).findAll(any(Sort.class));
    }
}
