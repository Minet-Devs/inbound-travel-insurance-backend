package com.travel.insurance.notification;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Renders the personalized policy certificate: a Thymeleaf template to HTML,
 * then HTML to PDF via openhtmltopdf. Deliberately has no dependency on any
 * other feature's service — {@link VisitorActivatedNotificationListener}
 * gathers the data, this class only knows how to lay it out.
 */
@Component
public class PolicyDocumentRenderer {

    private final SpringTemplateEngine templateEngine;

    public PolicyDocumentRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    String renderHtml(PolicyDocumentData data) {
        Context context = new Context();
        context.setVariable("data", data);
        context.setVariable("policyTypeLabel", displayName(data.policyType().name()));
        context.setVariable("insurerNamesJoined", String.join(", ", data.insurerNames()));
        context.setVariable("benefitLines", data.benefits().stream()
                .map(line -> new BenefitLineView(displayName(line.benefitType()), line.limitAmount()))
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
