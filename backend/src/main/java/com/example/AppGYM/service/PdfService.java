package com.example.AppGYM.service;

import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.ProgressPhoto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.DailyEntryRepository;
import com.example.AppGYM.repository.ProgressPhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PdfService {

    /* repos */
    private final BodyStatsRepository statsRepo;
    private final DailyEntryRepository dailyRepo;
    private final ProgressPhotoRepository photoRepo;

    /* ===================================================================================== */
    public byte[] buildFull(User u) { return build(u, null, null); }

    public byte[] buildPeriod(User u, LocalDate from, LocalDate to) { return build(u, from, to); }

    /* ===================================================================================== */
    private byte[] build(User u, LocalDate from, LocalDate to) {
        /* ─────────── DEBUG ─────────── */
        log.debug("PDF build  user={}  period={}→{}", u.getEmail(), from, to);

        BodyStats current = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);
        List<BodyStats> history = statsRepo.findByUserIdOrderByDateAsc(u.getId());
        List<DailyEntry> workouts =
                (from == null || to == null)
                        ? dailyRepo.findByUserId(u.getId())
                        : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to);

        log.debug("  currentStats = {}", current);
        log.debug("  history.size = {}", history.size());
        log.debug("  workouts.size= {}", workouts.size());

        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {

                final float margin = 50, leading = 16;
                float y = page.getMediaBox().getHeight() - margin;

                /* -------- Título -------- */
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("Informe de progreso – " + u.getFirstName() + " " + u.getLastName());
                cs.endText();
                y -= leading * 2;

                /* -------- Período -------- */
                if (from != null && to != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Período: " + from + " → " + to);
                    cs.endText();
                    y -= leading;
                }

                /* ================== MEDIDAS ACTUALES ================== */
                y = drawCurrent(cs, current, y - leading);

                /* ================== MEDIDAS HISTÓRICAS ================= */
                y = drawHistory(cs, history, y - leading);

                /* ================== ENTRENOS =========================== */
                drawWorkouts(cs, workouts, y - leading);
            }

            /* devolvemos bytes */
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                pdf.save(bos);
                return bos.toByteArray();
            }
        } catch (Exception ex) {
            log.error("Error generando PDF", ex);
            throw new RuntimeException("PDF error", ex);
        }
    }

    /* ------------------------------------------------------------------------------------- */
    private float drawCurrent(PDPageContentStream cs, BodyStats s, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Medidas actuales");
        cs.endText();
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA, 11);
        Map<String, String> rows = Map.of(
                "Peso (kg)",     nf(s == null ? null : s.getWeightKg()),
                "Estatura (cm)", nf(s == null ? null : s.getUser().getHeightCm()),
                "Cuello",        nf(s == null ? null : s.getNeckCm()),
                "Pecho",         nf(s == null ? null : s.getChestCm()),
                "Cintura",       nf(s == null ? null : s.getWaistCm()),
                "Abd. bajo",     nf(s == null ? null : s.getLowerAbsCm()),
                "Cadera",        nf(s == null ? null : s.getHipCm()),
                "Bíceps relax",  nf(s == null ? null : s.getBicepsCm()),
                "Bíceps flex",   nf(s == null ? null : s.getBicepsFlexCm()),
                "Antebrazo",     nf(s == null ? null : s.getForearmCm()),
                "Muslo",         nf(s == null ? null : s.getThighCm()),
                "Pantorrilla",   nf(s == null ? null : s.getCalfCm())
        );
        for (var entry : rows.entrySet()) {
            cs.beginText();
            cs.newLineAtOffset(60, y);
            cs.showText(String.format("%-18s %s", entry.getKey() + ":", entry.getValue()));
            cs.endText();
            y -= 14;
        }
        return y;
    }

    /* ------------------------------------------------------------------------------------- */
    private float drawHistory(PDPageContentStream cs, List<BodyStats> list, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Medidas históricas");
        cs.endText();
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        final String[] head = {"Fecha","Peso","Cintura","Cadera","Muslo","Bíceps","IMG Front/Side/Back"};
        float[] xPos = {60,120,180,240,300,360,420};
        for (int i=0;i<head.length;i++){
            cs.beginText(); cs.newLineAtOffset(xPos[i],y); cs.showText(head[i]); cs.endText();
        }
        y -= 14;
        cs.setFont(PDType1Font.HELVETICA, 10);

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        for (BodyStats s : list){
            String[] row = {
                    df.format(s.getDate()),
                    nf(s.getWeightKg()),
                    nf(s.getWaistCm()),
                    nf(s.getHipCm()),
                    nf(s.getThighCm()),
                    nf(s.getBicepsCm()),
                    (s.getFrontImgUrl()==null?"—": "✔") + " / " +
                            (s.getSideImgUrl() ==null?"—": "✔") + " / " +
                            (s.getBackImgUrl() ==null?"—": "✔")
            };
            for(int i=0;i<row.length;i++){
                cs.beginText(); cs.newLineAtOffset(xPos[i],y); cs.showText(row[i]); cs.endText();
            }
            y -= 12;
        }
        return y;
    }

    /* ------------------------------------------------------------------------------------- */
    private void drawWorkouts(PDPageContentStream cs, List<DailyEntry> list, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Histórico de entrenos");
        cs.endText();
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText(); cs.newLineAtOffset(60,y); cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(150,y);cs.showText("Máquina");cs.endText();
        cs.beginText(); cs.newLineAtOffset(350,y);cs.showText("Kg / Reps x Sets");cs.endText();
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA, 10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (DailyEntry e : list) {
            for (DailyEntry.Exercise ex : e.getDetails().values()) {
                cs.beginText(); cs.newLineAtOffset(60,y);  cs.showText(df.format(e.getDate()));             cs.endText();
                cs.beginText(); cs.newLineAtOffset(150,y); cs.showText(ex.getName());                        cs.endText();
                cs.beginText(); cs.newLineAtOffset(350,y); cs.showText(
                        nf(ex.getWeightKg())+" kg / "+ex.getReps()+"x"+ex.getSets());                       cs.endText();
                y -= 12;

                /* salto sencillo de página */
                if (y < 70) {
                    cs.close();                                   // terminar stream de página actual
                    PDPage newP = new PDPage(PDRectangle.LETTER);
                    cs.getDocument().addPage(newP);
                    cs = new PDPageContentStream(cs.getDocument(), newP);
                    y = newP.getMediaBox().getHeight() - 50;
                }
            }
        }
    }

    /* ------------------------------------------------------------------------------------- */
    private static String nf(Double d){ return d==null? "—" : String.format("%.1f", d); }
}
