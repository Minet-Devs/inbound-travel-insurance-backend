package com.travel.insurance.notification;

import com.travel.insurance.notification.PolicyDocumentData.BenefitLine;
import com.travel.insurance.policy.PolicyType;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyDocumentRendererTest {

    private PolicyDocumentRenderer newRenderer() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return new PolicyDocumentRenderer(templateEngine);
    }

    private PolicyDocumentData sampleData(List<BenefitLine> benefits) {
        return new PolicyDocumentData(
                "Jane Traveler",
                "P1234567",
                LocalDate.of(1990, 5, 12),
                "Germany",
                "12 Example Street, Berlin",
                "jane.traveler@example.com",
                "+254700000000",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 11, 1),
                "Tourism",
                "POL-0001",
                PolicyType.IPMI_61_DAYS_TO_12_MONTHS,
                List.of("Acme Insurance"),
                benefits,
                "+254 700 000000",
                "assistance@example.com");
    }

    @Test
    void rendersHtmlWithKeyFields() {
        PolicyDocumentRenderer renderer = newRenderer();
        PolicyDocumentData data = sampleData(List.of(
                new BenefitLine("EMERGENCY_MEDICAL_EXPENSES", new BigDecimal("20000.00"))));

        String html = renderer.renderHtml(data);

        assertThat(html).contains("Jane Traveler");
        assertThat(html).contains("P1234567");
        assertThat(html).contains("POL-0001");
        assertThat(html).contains("Ipmi 61 Days To 12 Months");
        assertThat(html).contains("Emergency Medical Expenses");
        assertThat(html).contains("20000.00");
        assertThat(html).contains("Acme Insurance");
        assertThat(html).doesNotContain("underlyingConditions");
    }

    @Test
    void rendersPlaceholderWhenNoBenefitsAssigned() {
        PolicyDocumentRenderer renderer = newRenderer();
        PolicyDocumentData data = sampleData(List.of());

        String html = renderer.renderHtml(data);

        assertThat(html).contains("No benefits assigned yet.");
    }

    @Test
    void rendersPdfStartingWithPdfMagicHeader() {
        PolicyDocumentRenderer renderer = newRenderer();
        PolicyDocumentData data = sampleData(List.of(
                new BenefitLine("PRESCRIPTION_MEDICINES", new BigDecimal("300.00"))));

        byte[] pdf = renderer.renderPdf(data);

        assertThat(pdf.length).isGreaterThan(4);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
