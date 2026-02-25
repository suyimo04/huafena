package com.pollen.management.service;

import com.pollen.management.entity.Application;
import com.pollen.management.entity.PublicLink;
import com.pollen.management.entity.QuestionnaireTemplate;
import com.pollen.management.entity.enums.ApplicationStatus;
import com.pollen.management.entity.enums.EntryType;
import com.pollen.management.repository.ApplicationRepository;
import com.pollen.management.repository.PublicLinkRepository;
import com.pollen.management.repository.QuestionnaireTemplateRepository;
import com.pollen.management.repository.QuestionnaireVersionRepository;
import com.pollen.management.repository.UserRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Feature: pollen-group-management, Property 13: 申请列表排序
 * Feature: pollen-group-management, Property 16: 公开链接令牌唯一性
 * **Validates: Requirements 4.3, 4.7**
 *
 * Property 13: For any set of application records, the list returned by listAll()
 * preserves the order from the repository (ordered by createdAt descending).
 *
 * Property 16: For any N calls to generate(), all produced link tokens are distinct.
 */
class ApplicationListAndPublicLinkProperties {

    // ========== Property 13: 申请列表排序 ==========

    /**
     * Feature: pollen-group-management, Property 13: 申请列表排序
     * **Validates: Requirements 4.3**
     *
     * The list returned by listAll() preserves the descending createdAt order
     * from the repository. We generate a random list of applications, sort them
     * by createdAt desc (simulating the repository query), and verify the service
     * returns them in the same order.
     */
    @Property(tries = 100)
    void listAllPreservesDescendingCreatedAtOrder(
            @ForAll("applicationLists") List<Application> unsortedApps) {

        // Sort by createdAt descending to simulate repository behavior
        List<Application> sortedApps = unsortedApps.stream()
                .sorted(Comparator.comparing(Application::getCreatedAt).reversed())
                .collect(Collectors.toList());

        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        QuestionnaireResponseService qrService = Mockito.mock(QuestionnaireResponseService.class);
        PublicLinkService publicLinkService = Mockito.mock(PublicLinkService.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ApplicationServiceImpl service = new ApplicationServiceImpl(
                appRepo, userRepo, qrService, publicLinkService, passwordEncoder,
                screeningService, objectMapper, Mockito.mock(ApplicationTimelineService.class));

        when(appRepo.findAllByOrderByCreatedAtDesc()).thenReturn(sortedApps);

        List<Application> result = service.listAll();

        assertThat(result).hasSize(sortedApps.size());

        // Verify the result is in the same order as the sorted list
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).getId())
                    .as("Application at index %d should match sorted order", i)
                    .isEqualTo(sortedApps.get(i).getId());
        }

        // Verify descending order: each createdAt >= next createdAt
        for (int i = 0; i + 1 < result.size(); i++) {
            assertThat(result.get(i).getCreatedAt())
                    .as("createdAt at index %d should be >= createdAt at index %d", i, i + 1)
                    .isAfterOrEqualTo(result.get(i + 1).getCreatedAt());
        }
    }

    /**
     * Feature: pollen-group-management, Property 13: 申请列表排序
     * **Validates: Requirements 4.3**
     *
     * An empty application list returns an empty result.
     */
    @Example
    void listAllReturnsEmptyForEmptyRepository() {
        ApplicationRepository appRepo = Mockito.mock(ApplicationRepository.class);
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        QuestionnaireResponseService qrService = Mockito.mock(QuestionnaireResponseService.class);
        PublicLinkService plService = Mockito.mock(PublicLinkService.class);
        PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
        ApplicationScreeningService screeningService = Mockito.mock(ApplicationScreeningService.class);
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        ApplicationServiceImpl service = new ApplicationServiceImpl(
                appRepo, userRepo, qrService, plService, passwordEncoder,
                screeningService, objectMapper, Mockito.mock(ApplicationTimelineService.class));

        when(appRepo.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

        List<Application> result = service.listAll();

        assertThat(result).isEmpty();
    }

    // ========== Property 16: 公开链接令牌唯一性 ==========

    /**
     * Feature: pollen-group-management, Property 16: 公开链接令牌唯一性
     * **Validates: Requirements 4.7**
     *
     * Calling generate() N times always produces N distinct link tokens.
     * Each token is a UUID generated by the service.
     */
    @Property(tries = 100)
    void generateAlwaysProducesUniqueTokens(
            @ForAll("generateCounts") int count) {

        PublicLinkRepository plRepo = Mockito.mock(PublicLinkRepository.class);
        QuestionnaireVersionRepository versionRepo = Mockito.mock(QuestionnaireVersionRepository.class);
        QuestionnaireTemplateRepository templateRepo = Mockito.mock(QuestionnaireTemplateRepository.class);
        QuestionnaireResponseService responseService = Mockito.mock(QuestionnaireResponseService.class);
        PublicLinkServiceImpl service = new PublicLinkServiceImpl(
                plRepo, versionRepo, templateRepo, responseService);

        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(1L).title("Test Template").activeVersionId(10L).createdBy(1L).build();
        when(templateRepo.findById(1L)).thenReturn(Optional.of(template));
        when(plRepo.save(any(PublicLink.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < count; i++) {
            PublicLink link = service.generate(1L, 1L);
            tokens.add(link.getLinkToken());
        }

        assertThat(tokens)
                .as("All %d generated tokens must be unique", count)
                .hasSize(count);
    }

    /**
     * Feature: pollen-group-management, Property 16: 公开链接令牌唯一性
     * **Validates: Requirements 4.7**
     *
     * Each generated token is a valid UUID format string.
     */
    @Property(tries = 100)
    void generatedTokenIsValidUuidFormat(
            @ForAll("positiveIds") Long templateId,
            @ForAll("positiveIds") Long createdBy) {

        PublicLinkRepository plRepo = Mockito.mock(PublicLinkRepository.class);
        QuestionnaireVersionRepository versionRepo = Mockito.mock(QuestionnaireVersionRepository.class);
        QuestionnaireTemplateRepository templateRepo = Mockito.mock(QuestionnaireTemplateRepository.class);
        QuestionnaireResponseService responseService = Mockito.mock(QuestionnaireResponseService.class);
        PublicLinkServiceImpl service = new PublicLinkServiceImpl(
                plRepo, versionRepo, templateRepo, responseService);

        QuestionnaireTemplate template = QuestionnaireTemplate.builder()
                .id(templateId).title("Template").activeVersionId(100L).createdBy(createdBy).build();
        when(templateRepo.findById(templateId)).thenReturn(Optional.of(template));
        when(plRepo.save(any(PublicLink.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicLink link = service.generate(templateId, createdBy);

        assertThat(link.getLinkToken())
                .as("Token must be a valid UUID format")
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<List<Application>> applicationLists() {
        return Arbitraries.integers().between(0, 20).flatMap(size -> {
            if (size == 0) {
                return Arbitraries.just(Collections.emptyList());
            }
            return Arbitraries.integers().between(1, 1000).list().ofSize(size).map(ids -> {
                List<Application> apps = new ArrayList<>();
                LocalDateTime base = LocalDateTime.of(2024, 1, 1, 0, 0);
                Random random = new Random();
                for (int i = 0; i < size; i++) {
                    Application app = Application.builder()
                            .id((long) ids.get(i))
                            .userId((long) (i + 1))
                            .status(ApplicationStatus.PENDING_INITIAL_REVIEW)
                            .entryType(EntryType.REGISTRATION)
                            .questionnaireResponseId((long) (i + 1))
                            .build();
                    // Manually set createdAt with varying offsets to create different timestamps
                    app.setCreatedAt(base.plusMinutes(random.nextInt(100000)));
                    apps.add(app);
                }
                return apps;
            });
        });
    }

    @Provide
    Arbitrary<Integer> generateCounts() {
        return Arbitraries.integers().between(2, 50);
    }

    @Provide
    Arbitrary<Long> positiveIds() {
        return Arbitraries.longs().between(1L, 100_000L);
    }
}
