package com.travel.insurance.memberstatement;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Renders the member statement: a Thymeleaf template to HTML, then HTML to PDF
 * via openhtmltopdf — same pipeline as {@code PolicyDocumentRenderer}.
 */
@Component
public class MemberStatementPdfRenderer {

    private final SpringTemplateEngine templateEngine;

    public MemberStatementPdfRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    String renderHtml(MemberStatementResponse statement) {
        Context context = new Context();
        context.setVariable("statement", statement);
        return templateEngine.process("member-statement", context);
    }

    byte[] render(MemberStatementResponse statement) {
        String html = renderHtml(statement);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render member statement PDF", ex);
        }
        return out.toByteArray();
    }
}