package com.travel.insurance.notification;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.travel.insurance.notification.PolicyDocumentData.BenefitLine;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
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
        context.setVariable("productLabel", displayName(data.policyType().name()));
        context.setVariable("underwriterName", String.join(", ", data.insurerNames()));
        context.setVariable("underwriterLogoUrl", data.underwriterLogoUrl());
        context.setVariable("genderLabel",
                data.gender() != null ? displayName(data.gender().name()) : "");
        context.setVariable("issueDate", LocalDate.now().format(LONG_DATE));
        context.setVariable("dateOfBirthLabel",
                data.dateOfBirth() != null ? data.dateOfBirth().format(LONG_DATE) : "");
        context.setVariable("coverStart",
                data.dateIn() != null ? data.dateIn().format(SHORT_DATE) : "");
        context.setVariable("coverEnd",
                data.dateOut() != null ? data.dateOut().format(SHORT_DATE) : "");
        context.setVariable("coverDays",
                data.dateIn() != null && data.dateOut() != null
                        ? ChronoUnit.DAYS.between(data.dateIn(), data.dateOut()) + 1 : 0);
        context.setVariable("cumulativeLimit", data.benefits().stream()
                .map(BenefitLine::limitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        context.setVariable("benefitLines", data.benefits().stream()
                .map(line -> new BenefitLineView(line.benefitName(), line.limitAmount()))
                .toList());
        return templateEngine.process("policy-document", context);
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
