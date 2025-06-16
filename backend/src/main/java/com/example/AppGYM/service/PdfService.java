// backend/src/main/java/com/example/AppGYM/service/PdfService.java
package com.example.AppGYM.service;

import com.example.AppGYM.model.*;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.DailyEntryRepository;
import com.example.AppGYM.repository.ProgressPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final BodyStatsRepository statsRepo;
    private final DailyEntryRepository dailyRepo;
    private final ProgressPhotoRepository photoRepo;

    /* ------------------------------------------------------------ */
    public byte[] buildFull(User u)                      { return build(u, null, null); }
    public byte[] buildPeriod(User u, LocalDate f, LocalDate t) { return build(u, f, t); }

    /* ------------------------------------------------------------ */
    private byte[] build(User u, LocalDate from, LocalDate to) {
        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {

                float y = page.getMediaBox().getHeight() - 50;
                final float leading = 15;

                /* ---------- título ---------- */
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                text(cs, 50, y, "Informe de progreso – " + u.getFirstName() + " " + u.getLastName());
                y -= leading * 2;

                if (from != null && to != null) {
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    text(cs, 50, y, "Período: " + from + " → " + to);
                    y -= leading * 1.5;
                }

                y = drawCurrentMeasures(cs, u, y - leading);
                y = drawStatsTable(cs, u, from, to, y - leading);
                drawDaily(cs, u, from, to, y - leading);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                pdf.save(bos);
                return bos.toByteArray();
            }
        } catch (Exception ex) {
            throw new RuntimeException("PDF build error", ex);
        }
    }

    /* ==================================================================== */
    /* =====================   BLOQUES DE DIBUJO   ======================== */
    /* ==================================================================== */

    /** 1 · medidas actuales */
    private float drawCurrentMeasures(PDPageContentStream cs, User u, float y) throws Exception {

        BodyStats last = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);

        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        text(cs, 50, y, "Medidas actuales");      y -= 18;
        cs.setFont(PDType1Font.HELVETICA, 11);

        /* LinkedHashMap mantiene el orden de inserción */
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Peso (kg)",      n(last != null ? last.getWeightKg()    : null));
        map.put("Estatura (cm)",  n(u.getHeightCm()));
        map.put("Cuello",         n(last != null ? last.getNeckCm()      : null));
        map.put("Pecho",          n(last != null ? last.getChestCm()     : null));
        map.put("Cintura",        n(last != null ? last.getWaistCm()     : null));
        map.put("Abd. bajo",      n(last != null ? last.getLowerAbsCm()  : null));
        map.put("Cadera",         n(last != null ? last.getHipCm()       : null));
        map.put("Bíceps relax",   n(last != null ? last.getBicepsCm()    : null));
        map.put("Bíceps flex",    n(last != null ? last.getBicepsFlexCm(): null));
        map.put("Antebrazo",      n(last != null ? last.getForearmCm()   : null));
        map.put("Muslo",          n(last != null ? last.getThighCm()     : null));
        map.put("Pantorrilla",    n(last != null ? last.getCalfCm()      : null));

        for (var e : map.entrySet()) {
            text(cs, 60, y, String.format("%-18s %s", e.getKey() + ":", e.getValue()));
            y -= 14;
        }
        return y;
    }

    /** 2 · tabla de medidas históricas */
    private float drawStatsTable(PDPageContentStream cs, User u,
                                 LocalDate from, LocalDate to, float y) throws Exception {

        List<BodyStats> stats = (from == null || to == null)
                ? statsRepo.findByUserIdOrderByDateAsc(u.getId())
                : statsRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to);

        if (stats.isEmpty()) return y;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        text(cs, 50, y, "Medidas históricas"); y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        text(cs, 60,  y, "Fecha");
        text(cs, 110, y, "Peso");
        text(cs, 150, y, "Cintura");
        text(cs, 210, y, "Cadera");
        text(cs, 270, y, "Muslo");
        text(cs, 320, y, "Bíceps");
        text(cs, 370, y, "IMG Front/Side/Back");
        y -= 12;

        cs.setFont(PDType1Font.HELVETICA, 9);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (BodyStats s : stats) {
            text(cs, 60,  y, df.format(s.getDate()));
            text(cs, 110, y, n(s.getWeightKg()));
            text(cs, 150, y, n(s.getWaistCm()));
            text(cs, 210, y, n(s.getHipCm()));
            text(cs, 270, y, n(s.getThighCm()));
            text(cs, 320, y, n(s.getBicepsCm()));
            String imgs = String.join(" / ",
                    nz(s.getFrontImgUrl()), nz(s.getSideImgUrl()), nz(s.getBackImgUrl()));
            text(cs, 370, y, imgs);
            y -= 12;
        }
        return y;
    }

    /** 3 · histórico de entrenos */
    private void drawDaily(PDPageContentStream cs, User u,
                           LocalDate from, LocalDate to, float y) throws Exception {

        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        text(cs, 50, y, "Histórico de entrenos");
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        text(cs, 60,  y, "Fecha");
        text(cs, 150, y, "Máquina");
        text(cs, 350, y, "Kg / Reps x Sets");
        y -= 12;

        cs.setFont(PDType1Font.HELVETICA, 10);

        List<DailyEntry> list = (from == null || to == null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to);

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        for (DailyEntry e : list) {
            for (DailyEntry.Exercise ex : e.getDetails().values()) {
                text(cs, 60,  y, df.format(e.getDate()));
                text(cs, 150, y, ex.getName());
                text(cs, 350, y, ex.getWeightKg() + " kg / " + ex.getReps() + " x " + ex.getSets());
                y -= 12;
            }
        }
    }

    /* ==================================================================== */
    /* =========================== Utilidades ============================= */
    /* ==================================================================== */
    private static void text(PDPageContentStream cs, float x, float y, String s) throws Exception {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(s == null ? "" : s);
        cs.endText();
    }
    private static String n(Double d){ return d == null ? "—" : String.format("%.1f", d); }
    private static String nz(String s){ return s == null ? "—" : "✔"; }
}
