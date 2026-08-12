package com.travel.insurance.common.util;

/**
 * Rewrites logo image URLs into a form that returns raw image bytes rather than
 * an HTML preview page. Dropbox share links (`www.dropbox.com/...?dl=0`) serve
 * an HTML preview, which the PDF renderer cannot read ("Unrecognized Image
 * format"); the direct-download host serves the file itself. Any URL that is
 * not a recognized share link is returned unchanged.
 */
public final class LogoUrlNormalizer {

    private LogoUrlNormalizer() {
    }

    public static String normalize(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return normalizeDropbox(trimmed);
    }

    private static String normalizeDropbox(String url) {
        if (!url.contains("dropbox.com")) {
            return url;
        }
        // Serve the file directly instead of the HTML preview page. Rewriting the
        // host to the content domain avoids relying on the renderer following the
        // 302 that dl=1 would otherwise produce.
        String rewritten = url
                .replace("://www.dropbox.com", "://dl.dropboxusercontent.com")
                .replace("://dropbox.com", "://dl.dropboxusercontent.com");
        return forceDownloadParam(rewritten);
    }

    private static String forceDownloadParam(String url) {
        if (url.contains("dl=0")) {
            return url.replace("dl=0", "dl=1");
        }
        if (url.contains("dl=1")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "dl=1";
    }
}
