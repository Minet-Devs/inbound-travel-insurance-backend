package com.travel.insurance.memberstatement;

import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementTransaction;
import com.travel.insurance.visitor.VisitorStatus;
import com.travel.insurance.visitorbenefit.dto.VisitorBenefitResponse;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemberStatementPdfRendererTest {

    private MemberStatementPdfRenderer newRenderer() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return new MemberStatementPdfRenderer(templateEngine);
    }

    private MemberStatementResponse statementWith(
            List<VisitorBenefitResponse> benefits, List<MemberStatementTransaction> transactions) {
        return new MemberStatementResponse(
                UUID.randomUUID(), "Jane Traveler", "P1234567",
                UUID.randomUUID(), benefits, transactions);
    }

    @Test
    void rendersHtmlWithMemberAndTransactionDetails() {
        MemberStatementPdfRenderer renderer = newRenderer();
        VisitorBenefitResponse benefit = new VisitorBenefitResponse(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Medical Expenses", new BigDecimal("20000.00"), new BigDecimal("500.00"),
                new BigDecimal("19500.00"), VisitorStatus.ACTIVE, Instant.now(), Instant.now());
        MemberStatementTransaction transaction = new MemberStatementTransaction(
                UUID.randomUUID(), LocalDate.of(2026, 6, 1), UUID.randomUUID(), "Medical Expenses",
                new BigDecimal("500.00"), UUID.randomUUID(), "Nairobi Hospital");
        MemberStatementResponse statement = statementWith(List.of(benefit), List.of(transaction));

        String html = renderer.renderHtml(statement);

        assertThat(html).contains("Jane Traveler");
        assertThat(html).contains("P1234567");
        assertThat(html).contains("MINET KENYA INSURANCE BROKERS");
        assertThat(html).contains("Nairobi Hospital");
        assertThat(html).contains("20,000.00");
        assertThat(html).contains("19,500.00");
        assertThat(html).doesNotContain("No member statement transaction data found");
    }

    @Test
    void rendersEmptyStateWhenNoTransactions() {
        MemberStatementPdfRenderer renderer = newRenderer();
        MemberStatementResponse statement = statementWith(List.of(), List.of());

        String html = renderer.renderHtml(statement);

        assertThat(html).contains("No member statement transaction data found");
    }

    @Test
    void rendersPdfStartingWithPdfMagicHeader() {
        MemberStatementPdfRenderer renderer = newRenderer();
        MemberStatementResponse statement = statementWith(List.of(), List.of());

        byte[] pdf = renderer.render(statement);

        assertThat(pdf.length).isGreaterThan(4);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}