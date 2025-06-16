package com.example.AppGYM.service;

import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.DailyEntryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final BodyStatsRepository statsRepo;
    private final DailyEntryRepository dailyRepo;

    /* =================== API pública =================== */
    public byte[] buildFull(User u) { return build(u, null, null); }
    public byte[] buildPeriod(User u, LocalDate f, LocalDate t) { return build(u, f, t); }

    /* =================== Generador ===================== */
    private byte[] build(User user, LocalDate from, LocalDate to) {
        try (PDDocument pdf = new PDDocument()) {

            PDPage            page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);
            PDPageContentStream cs   = new PDPageContentStream(pdf, page);

            float margin  = 50, leading = 16;
            float y       = page.getMediaBox().getHeight() - margin;

            /* ---------- Título ---------- */
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
            cs.newLineAtOffset(margin, y);
            cs.showText("Informe de progreso – " + user.getFirstName() + " " + user.getLastName());
            cs.endText();   y -= leading * 2;

            if (from != null && to != null) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Período: " + from + " → " + to);
                cs.endText(); y -= leading;
            }

            /* ========== Secciones ========== */
            y = drawCurrentMeasures(cs, user, y - leading);
            y = drawMeasureHistory(pdf, cs, user, from, to, y - leading);
            y = drawDaily(pdf, cs, user, from, to, y - leading);

            cs.close();

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                pdf.save(bos);
                return bos.toByteArray();
            }

        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 1) Medidas actuales */
    private float drawCurrentMeasures(PDPageContentStream cs, User u, float y) throws Exception {

        statsRepo.findTopByUserIdOrderByDateDesc(u.getId())
                .ifPresent(bs -> {           /* sobreescribe con el último registro */
                    u.setWeightKg(bs.getWeightKg());
                    u.setNeckCm(bs.getNeckCm());
                    u.setChestCm(bs.getChestCm());
                    u.setWaistCm(bs.getWaistCm());
                    u.setLowerAbsCm(bs.getLowerAbsCm());
                    u.setHipCm(bs.getHipCm());
                    u.setBicepsCm(bs.getBicepsCm());
                    u.setBicepsFlexCm(bs.getBicepsFlexCm());
                    u.setForearmCm(bs.getForearmCm());
                    u.setThighCm(bs.getThighCm());
                    u.setCalfCm(bs.getCalfCm());
                });

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Medidas actuales");
        cs.endText(); y -= 18;

        cs.setFont(PDType1Font.HELVETICA, 11);
        String[][] rows = {
                {"Peso (kg)",      nf(u.getWeightKg())},
                {"Estatura (cm)",  nf(u.getHeightCm())},
                {"Cuello",         nf(u.getNeckCm())},
                {"Pecho",          nf(u.getChestCm())},
                {"Cintura",        nf(u.getWaistCm())},
                {"Abd. bajo",      nf(u.getLowerAbsCm())},
                {"Cadera",         nf(u.getHipCm())},
                {"Bíceps relax",   nf(u.getBicepsCm())},
                {"Bíceps flex",    nf(u.getBicepsFlexCm())},
                {"Antebrazo",      nf(u.getForearmCm())},
                {"Muslo",          nf(u.getThighCm())},
                {"Pantorrilla",    nf(u.getCalfCm())}
        };
        for (String[] r : rows) {
            cs.beginText();
            cs.newLineAtOffset(60, y);
            cs.showText(String.format("%-17s %s", r[0] + ":", r[1]));
            cs.endText();
            y -= 14;
        }
        return y;
    }

    /* ------------------------------------------------------------------ */
    /* 2) Histórico de medidas */
    private float drawMeasureHistory(PDDocument pdf, PDPageContentStream cs,
                                     User user, LocalDate from, LocalDate to,
                                     float y) throws Exception {

        List<BodyStats> list = (from == null || to == null)
                ? statsRepo.findByUserIdOrderByDateAsc(user.getId())
                : statsRepo.findByUserIdAndDateBetweenOrderByDateAsc(user.getId(), from, to);

        if (list.isEmpty()) return y;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Histórico de medidas");
        cs.endText(); y -= 18;

        /* cabecera */
        String[] head = {"Fecha", "Peso", "Pecho", "Cintura", "Cadera", "Bíceps", "Muslo"};
        float[] xCol  = {60,     120,    170,      230,        300,       360,      430};

        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        for (int i = 0; i < head.length; i++) {
            cs.beginText(); cs.newLineAtOffset(xCol[i], y); cs.showText(head[i]); cs.endText();
        }
        y -= 14; cs.setFont(PDType1Font.HELVETICA, 10);

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        for (BodyStats bs : list) {
            String[] row = {
                    df.format(bs.getDate()),
                    nf(bs.getWeightKg()),
                    nf(bs.getChestCm()),
                    nf(bs.getWaistCm()),
                    nf(bs.getHipCm()),
                    nf(bs.getBicepsCm()),
                    nf(bs.getThighCm())
            };
            for (int i = 0; i < row.length; i++) {
                cs.beginText(); cs.newLineAtOffset(xCol[i], y); cs.showText(row[i]); cs.endText();
            }
            y -= 12;
            if (y < 70) {                          /* salto de página */
                cs.close();
                PDPage newPage = new PDPage(PDRectangle.LETTER);
                pdf.addPage(newPage);
                cs = new PDPageContentStream(pdf, newPage);
                y = newPage.getMediaBox().getHeight() - 50;
            }
        }
        return y;
    }

    /* ------------------------------------------------------------------ */
    /* 3) Histórico de entrenos */
    private float drawDaily(PDDocument pdf, PDPageContentStream cs,
                            User user, LocalDate from, LocalDate to,
                            float y) throws Exception {

        List<DailyEntry> list = (from == null || to == null)
                ? dailyRepo.findByUserIdOrderByDateAsc(user.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(user.getId(), from, to);

        if (list.isEmpty()) return y;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Histórico de entrenos");
        cs.endText(); y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText(); cs.newLineAtOffset(60, y);  cs.showText("Fecha");                 cs.endText();
        cs.beginText(); cs.newLineAtOffset(140, y); cs.showText("Máquina");               cs.endText();
        cs.beginText(); cs.newLineAtOffset(340, y); cs.showText("Kg / Reps x Sets");      cs.endText();
        y -= 14; cs.setFont(PDType1Font.HELVETICA, 10);

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        for (DailyEntry de : list) {
            for (Map.Entry<Long, DailyEntry.Exercise> e : de.getDetails().entrySet()) {
                var ex = e.getValue();

                cs.beginText(); cs.newLineAtOffset(60, y);  cs.showText(df.format(de.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(140, y); cs.showText(ex.getName());            cs.endText();
                cs.beginText(); cs.newLineAtOffset(340, y);
                cs.showText(ex.getWeightKg() + " kg / " + ex.getReps() + "x" + ex.getSets());
                cs.endText();

                y -= 12;
                if (y < 70) {
                    cs.close();
                    PDPage newPage = new PDPage(PDRectangle.LETTER);
                    pdf.addPage(newPage);
                    cs = new PDPageContentStream(pdf, newPage);
                    y = newPage.getMediaBox().getHeight() - 50;
                }
            }
        }
        return y;
    }

    /* ------------------------------------------------------------------ */
    /* util */
    private String nf(Double d) { return d == null ? "—" : String.format("%.1f", d); }
}
