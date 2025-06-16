// backend/src/main/java/com/example/AppGYM/service/PdfService.java
package com.example.AppGYM.service;

import com.example.AppGYM.model.User;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service @RequiredArgsConstructor
public class PdfService {

    public byte[] buildReport(User u, LocalDate from, LocalDate to) {
        try (PDDocument pdf = new PDDocument();
             PDPageContentStream cs = new PDPageContentStream(pdf,new PDPage())) {

            PDPage page = pdf.getPage(0);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD,18);
            cs.newLineAtOffset(50,750);
            cs.showText("Informe de progreso – "+u.getEmail());
            cs.endText();

            // …añade datos (medidas, histórico, fotos)…

            cs.close();
            return save(pdf);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] save(PDDocument doc) throws Exception {
        try (var bos = new java.io.ByteArrayOutputStream()) {
            doc.save(bos);
            return bos.toByteArray();
        }
    }
}
