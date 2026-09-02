package com.travel.insurance.notification;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.travel.insurance.common.util.AmountInWordsConverter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLConnection;
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

    private static final float PAGE_MARGIN = 36f;
    private static final float LOGO_MAX_WIDTH = 120f;
    private static final float LOGO_MAX_HEIGHT = 50f;
    private static final float SIGNATURE_MAX_WIDTH = 150f;
    private static final float SIGNATURE_MAX_HEIGHT = 60f;
    private static final int IMAGE_FETCH_TIMEOUT_MILLIS = 5000;

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
        context.setVariable("insurerLogoUrl", data.insurerLogoUrl());
        context.setVariable("totalPremiumInWords", AmountInWordsConverter.toWords(data.totalPremium()));
        return templateEngine.process("premium-receipt", context);
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

    /**
     * Concatenates already-rendered PDFs into a single multi-page document,
     * in the given order, via PDFBox's merger — used to send the policy
     * certificate and premium receipt as one continuous attachment rather
     * than two separate files.
     */
    byte[] mergePdfs(byte[]... pdfs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationStream(out);
        try {
            for (byte[] pdf : pdfs) {
                merger.addSource(new RandomAccessReadBuffer(pdf));
            }
            merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to merge PDF documents", ex);
        }
        return out.toByteArray();
    }

    /**
     * Overlays the insurer's logo onto the top-right of the first page and its
     * e-signature onto the bottom-right of the last page of the bundled policy
     * wording PDF. Either URL may be null, in which case that overlay is
     * skipped. Returns the document unmodified if both are null.
     */
    byte[] brandPolicyWording(byte[] policyWordingPdf, String logoUrl, String esignatureUrl) {
        if (logoUrl == null && esignatureUrl == null) {
            return policyWordingPdf;
        }
        try (PDDocument document = Loader.loadPDF(policyWordingPdf)) {
            if (logoUrl != null) {
                overlayImage(document, document.getPage(0), fetchImageBytes(logoUrl),
                        LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT, true);
            }
            if (esignatureUrl != null) {
                PDPage lastPage = document.getPage(document.getNumberOfPages() - 1);
                overlayImage(document, lastPage, fetchImageBytes(esignatureUrl),
                        SIGNATURE_MAX_WIDTH, SIGNATURE_MAX_HEIGHT, false);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to brand policy wording PDF", ex);
        }
    }

    /**
     * Draws an image into a page's top-right (logo) or bottom-right
     * (signature) corner, scaled down to fit within maxWidth/maxHeight while
     * preserving aspect ratio.
     */
    private void overlayImage(PDDocument document, PDPage page, byte[] imageBytes,
                               float maxWidth, float maxHeight, boolean topRight) throws IOException {
        PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "overlay");
        float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        PDRectangle mediaBox = page.getMediaBox();
        float x = mediaBox.getUpperRightX() - PAGE_MARGIN - width;
        float y = topRight
                ? mediaBox.getUpperRightY() - PAGE_MARGIN - height
                : mediaBox.getLowerLeftY() + PAGE_MARGIN;
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            contentStream.drawImage(image, x, y, width, height);
        }
    }

    private byte[] fetchImageBytes(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(IMAGE_FETCH_TIMEOUT_MILLIS);
        connection.setReadTimeout(IMAGE_FETCH_TIMEOUT_MILLIS);
        try (var in = connection.getInputStream()) {
            return in.readAllBytes();
        }
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
