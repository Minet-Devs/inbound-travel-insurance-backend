package com.travel.insurance.notification;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Renders the personalized policy certificate: a Thymeleaf template to HTML,
 * then HTML to PDF via openhtmltopdf. Deliberately has no dependency on any
 * other feature's service — {@link VisitorActivatedNotificationListener}
 * gathers the data, this class only knows how to lay it out.
 */
@Component
public class PolicyDocumentRenderer {

    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final SpringTemplateEngine templateEngine;

    public PolicyDocumentRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    String renderHtml(PolicyDocumentData data) {
        Context context = new Context();
        context.setVariable("data", data);
        context.setVariable("underwriterName", String.join(", ", data.insurerNames()));
        context.setVariable("underwriterLogoUrl", data.underwriterLogoUrl());
        context.setVariable("esignatureUrl", data.esignatureUrl());
        context.setVariable("genderLabel",
                data.gender() != null ? displayName(data.gender().name()) : "");
        context.setVariable("issueDate", LocalDate.now().format(LONG_DATE));
        context.setVariable("dateOfBirthLabel",
                data.dateOfBirth() != null ? data.dateOfBirth().format(LONG_DATE) : "");
        context.setVariable("coverStart",
                data.dateIn() != null ? data.dateIn().format(SHORT_DATE) : "");
        context.setVariable("coverEnd",
                data.policyExpiryDate() != null ? data.policyExpiryDate().format(SHORT_DATE) : "");
        context.setVariable("coverDays",
                data.dateIn() != null && data.policyExpiryDate() != null
                        ? ChronoUnit.DAYS.between(data.dateIn(), data.policyExpiryDate()) : 0);
        context.setVariable("benefitLines", data.benefits().stream()
                .map(line -> new BenefitLineView(line.benefitName(), line.limitAmount()))
                .toList());
        return templateEngine.process("policy-certificate", context);
    }

    byte[] renderPdf(PolicyDocumentData data) {
        String html = renderHtml(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render policy document PDF", ex);
        }
        return out.toByteArray();
    }

    String renderPremiumReceiptHtml(PremiumReceiptData data) {
        Context context = new Context();
        context.setVariable("receipt", data);
        context.setVariable("generatedDate", LocalDate.now().format(SHORT_DATE));
        context.setVariable("pcfLevyAmount", levyAmount(data.totalPremium(), data.pcfLevy()));
        context.setVariable("insurancePremiumLevyAmount", levyAmount(data.totalPremium(), data.insurancePremiumLevy()));
        context.setVariable("trainingLevyAmount", levyAmount(data.totalPremium(), data.trainingLevy()));
        return templateEngine.process("premium-receipt", context);
    }

    private static BigDecimal levyAmount(BigDecimal totalPremium, BigDecimal levyRate) {
        return totalPremium.multiply(levyRate).setScale(2, RoundingMode.HALF_UP);
    }

    byte[] renderPremiumReceiptPdf(PremiumReceiptData data) {
        String html = renderPremiumReceiptHtml(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render premium receipt PDF", ex);
        }
        return out.toByteArray();
    }

    static String displayName(String enumName) {
        String[] words = enumName.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    public record BenefitLineView(String label, BigDecimal limitAmount) {
    }
}
