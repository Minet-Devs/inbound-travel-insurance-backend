package com.travel.insurance.document.dto;

/** Metadata about one uploaded PDF that was merged into a document; the original file is not kept. */
public record SourceFileInfo(String filename, long sizeBytes, int pageCount) {
}
