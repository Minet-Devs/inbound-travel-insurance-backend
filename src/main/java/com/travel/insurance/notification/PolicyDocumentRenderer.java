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
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final DateTimeFormatter COMPACT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH);

    private static final float PAGE_MARGIN = 36f;
    private static final float LOGO_MAX_WIDTH = 120f;
    private static final float LOGO_MAX_HEIGHT = 50f;
    private static final float SIGNATURE_MAX_WIDTH = 150f;
    private static final float SIGNATURE_MAX_HEIGHT = 60f;
    private static final int IMAGE_FETCH_TIMEOUT_MILLIS = 5000;

    /** Zero-based index of the "POLICY AGREEMENT" page in the bundled policy wording PDF. */
    private static final int POLICY_AGREEMENT_PAGE_INDEX = 2;
    private static final float AGREEMENT_FONT_SIZE = 9.5f;
    private static final float AGREEMENT_TIGHT_FONT_SIZE = 7f;
    private static final PDFont AGREEMENT_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final String SIGNED_AT_LOCATION = "Nairobi";
    private static final Pattern PO_BOX_NUMBER = Pattern.compile("(?i)\\bbox\\s*([\\w-]+)");

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
     * Overlays the insurer's logo, horizontally centered near the top of the
     * first page, and its e-signature, horizontally centered near the bottom
     * of every page, onto the bundled policy wording PDF. Either URL may
     * be null, in which case that overlay is skipped. Returns the document
     * unmodified if both are null.
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
                byte[] esignatureBytes = fetchImageBytes(esignatureUrl);
                for (PDPage page : document.getPages()) {
                    overlayImage(document, page, esignatureBytes,
                            SIGNATURE_MAX_WIDTH, SIGNATURE_MAX_HEIGHT, false);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to brand policy wording PDF", ex);
        }
    }

    /**
     * Fills the blank underscores on the bundled policy wording's "POLICY
     * AGREEMENT" page (page 3) with the Company (insurer) and Insured
     * (visitor) details. Deliberately not folded into {@link #brandPolicyWording}
     * or its per-insurer cache: unlike the logo/e-signature branding, these
     * values are per-visitor PII and must be drawn fresh for every visitor,
     * never cached and reused across visitors of the same insurer. On any
     * failure, the input PDF is returned unfilled rather than dropping the
     * attachment. The recital's brief mentions of the Insured's name and PO
     * Box (a few points of blank space between pre-printed underscores) have
     * no usable room for a real name/address and are deliberately left
     * blank; the Insured's name is instead filled into the much wider
     * signature-block "Name:" line further down the same page.
     */
    byte[] fillPolicyAgreementDetails(byte[] brandedPolicyWordingPdf, String insurerName,
                                       String insurerAddress, String insuredName, LocalDate issueDate) {
        try (PDDocument document = Loader.loadPDF(brandedPolicyWordingPdf)) {
            if (document.getNumberOfPages() <= POLICY_AGREEMENT_PAGE_INDEX) {
                return brandedPolicyWordingPdf;
            }
            PDPage page = document.getPage(POLICY_AGREEMENT_PAGE_INDEX);
            String issueDateText = issueDate != null ? issueDate.format(SHORT_DATE) : "";
            String compactDateText = issueDate != null ? issueDate.format(COMPACT_DATE) : "";
            String poBoxNumber = extractPoBoxNumber(insurerAddress);
            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                // Recital: "THIS POLICY is made this ________ between ____________________, a limited..."
                drawText(contentStream, compactDateText, 188f, 742.8f, AGREEMENT_FONT_SIZE);
                drawText(contentStream, insurerName, 284f, 742.8f, AGREEMENT_FONT_SIZE);
                // Recital: "...of Post Office _____ Nairobi (hereinafter referred to as the Company)..."
                // only a short PO Box number fits the underscore run; the visitor's name mention
                // further down this recital has no usable room (a few points wide) and is
                // intentionally left blank rather than overlaid illegibly or overlapping text.
                drawText(contentStream, poBoxNumber, 276f, 726.9f, AGREEMENT_TIGHT_FONT_SIZE);
                // "In WITNESS WHEREOF this policy has been signed at_____."
                drawText(contentStream, SIGNED_AT_LOCATION, 297f, 481.7f, AGREEMENT_TIGHT_FONT_SIZE);
                // Signature block — Company
                drawText(contentStream, insurerName, 90f, 413.7f, AGREEMENT_FONT_SIZE);
                drawText(contentStream, issueDateText, 95f, 356.3f, AGREEMENT_FONT_SIZE);
                // Signature block — Insured
                drawText(contentStream, insuredName, 90f, 281.9f, AGREEMENT_FONT_SIZE);
                drawText(contentStream, issueDateText, 95f, 236.2f, AGREEMENT_FONT_SIZE);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to fill policy agreement details", ex);
        }
    }

    /**
     * Extracts the number/token following "Box" from an insurer address such
     * as {@code "PO Box 200, Nairobi"}, for the recital's narrow
     * "Post Office _____ Nairobi" blank, which has room for only a short
     * token, not the full address. Returns {@code null} (skip the overlay)
     * when the address doesn't contain a recognizable "Box <token>".
     */
    private static String extractPoBoxNumber(String insurerAddress) {
        if (insurerAddress == null) {
            return null;
        }
        Matcher matcher = PO_BOX_NUMBER.matcher(insurerAddress);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** Draws a single line of text at the given baseline; a blank/null value is skipped. */
    private void drawText(PDPageContentStream contentStream, String value, float x, float y, float fontSize)
            throws IOException {
        if (value == null || value.isBlank()) {
            return;
        }
        contentStream.beginText();
        contentStream.setFont(AGREEMENT_FONT, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(value);
        contentStream.endText();
    }

    /**
     * Draws an image horizontally centered near the top (logo) or bottom
     * (signature) of a page, scaled down to fit within maxWidth/maxHeight
     * while preserving aspect ratio.
     */
    private void overlayImage(PDDocument document, PDPage page, byte[] imageBytes,
                               float maxWidth, float maxHeight, boolean atTop) throws IOException {
        PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "overlay");
        float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        PDRectangle mediaBox = page.getMediaBox();
        float x = mediaBox.getLowerLeftX() + (mediaBox.getWidth() - width) / 2f;
        float y = atTop
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
