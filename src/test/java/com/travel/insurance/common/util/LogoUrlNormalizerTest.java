package com.travel.insurance.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogoUrlNormalizerTest {

    @Test
    void rewritesDropboxShareLinkToDirectDownload() {
        String input = "https://www.dropbox.com/scl/fi/abc/ga-logo.png?rlkey=key&st=abc&dl=0";

        String result = LogoUrlNormalizer.normalize(input);

        assertThat(result)
                .isEqualTo("https://dl.dropboxusercontent.com/scl/fi/abc/ga-logo.png?rlkey=key&st=abc&dl=1");
    }

    @Test
    void addsDownloadParamWhenDropboxLinkHasNone() {
        String input = "https://www.dropbox.com/scl/fi/abc/ga-logo.png?rlkey=key";

        String result = LogoUrlNormalizer.normalize(input);

        assertThat(result)
                .isEqualTo("https://dl.dropboxusercontent.com/scl/fi/abc/ga-logo.png?rlkey=key&dl=1");
    }

    @Test
    void leavesAlreadyDirectDropboxLinkOnHostRewriteOnly() {
        String input = "https://dl.dropboxusercontent.com/scl/fi/abc/ga-logo.png?rlkey=key&dl=1";

        String result = LogoUrlNormalizer.normalize(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void leavesNonDropboxUrlUnchanged() {
        String input = "https://cdn.example.com/logos/ga-logo.png";

        assertThat(LogoUrlNormalizer.normalize(input)).isEqualTo(input);
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(LogoUrlNormalizer.normalize("  https://cdn.example.com/a.png  "))
                .isEqualTo("https://cdn.example.com/a.png");
    }

    @Test
    void returnsNullAndBlankUnchanged() {
        assertThat(LogoUrlNormalizer.normalize(null)).isNull();
        assertThat(LogoUrlNormalizer.normalize("   ")).isEmpty();
    }
}
