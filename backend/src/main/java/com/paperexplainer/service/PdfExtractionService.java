package com.paperexplainer.service;

import com.paperexplainer.exception.InvalidPdfException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractionService {

    public String extractText(Path pdfPath) {

        requirePdfExtension(pdfPath);

        try (PDDocument document =
                     Loader.loadPDF(pdfPath.toFile())) {

            requireNotEncrypted(document);

            PDFTextStripper stripper =
                    new PDFTextStripper();

            String text =
                    stripper.getText(document);

            if (text == null || text.isBlank()) {
                throw new InvalidPdfException(
                        "No readable text was found in the PDF."
                );
            }

            return text.trim();

        } catch (InvalidPdfException exception) {
            throw exception;

        } catch (IOException exception) {
            throw new InvalidPdfException(
                    "Could not extract text from PDF.",
                    exception
            );
        }
    }

    /**
     * Extracts text one page at a time so callers can attribute
     * chunks to the page they actually came from. PDFTextStripper only
     * exposes page boundaries via setStartPage/setEndPage, so this is the
     * smallest change that makes page numbers real instead of guessed -
     * it does not attempt to map arbitrary character offsets within a
     * single flattened string back to page boundaries.
     */
    public List<String> extractTextByPage(Path pdfPath) {

        requirePdfExtension(pdfPath);

        try (PDDocument document =
                     Loader.loadPDF(pdfPath.toFile())) {

            requireNotEncrypted(document);

            PDFTextStripper stripper =
                    new PDFTextStripper();

            int pageCount = document.getNumberOfPages();

            List<String> pages = new ArrayList<>(pageCount);

            boolean hasContent = false;

            for (int page = 1; page <= pageCount; page++) {

                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String pageText = stripper.getText(document);
                pageText = pageText == null ? "" : pageText.trim();

                if (!pageText.isBlank()) {
                    hasContent = true;
                }

                pages.add(pageText);
            }

            if (!hasContent) {
                throw new InvalidPdfException(
                        "No readable text was found in the PDF."
                );
            }

            return pages;

        } catch (InvalidPdfException exception) {
            throw exception;

        } catch (IOException exception) {
            throw new InvalidPdfException(
                    "Could not extract text from PDF.",
                    exception
            );
        }
    }

    private void requirePdfExtension(Path pdfPath) {

        if (pdfPath == null) {
            throw new InvalidPdfException("PDF path is missing.");
        }

        if (pdfPath.getFileName() == null ||
                !pdfPath.getFileName()
                        .toString()
                        .toLowerCase()
                        .endsWith(".pdf")) {

            throw new InvalidPdfException(
                    "Only PDF files are supported."
            );
        }
    }

    private void requireNotEncrypted(PDDocument document) {

        if (document.isEncrypted()) {
            throw new InvalidPdfException(
                    "Encrypted PDF files are not supported."
            );
        }
    }
}
