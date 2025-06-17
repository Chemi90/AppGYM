package com.example.AppGYM.service;

import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.DailyEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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

    private final BodyStatsRepository  statsRepo;
    private final DailyEntryRepository dailyRepo;

    /* ========== API ========== */
    public byte[] buildFull(User u)                          { return build(u, null, null); }
    public byte[] buildPeriod(User u, LocalDate f, LocalDate t) { return build(u, f, t); }

    /* ========== core ========== */
    private byte[] build(User u, LocalDate from, LocalDate to) {

        List<BodyStats> statsHist = statsRepo.findByUserIdOrderByDateAsc(u.getId());
        BodyStats       current   = statsRepo
                .findTopByUserIdOrderByDateDesc(u.getId())
                .orElse(null);

        List<DailyEntry> workouts = (from == null || to == null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to);

        log.debug("PDF | usr={}, current={}, history={}, workouts={}",
                u.getEmail(), current != null, statsHist.size(), workouts.size());

        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf, page);

            float x = 50, y = page.getMediaBox().getHeight() - 50;
            float leading = 15;

            /* ---------- título ---------- */
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
            cs.newLineAtOffset(x, y);
            cs.showText("Informe de progreso – " + u.getFirstName() + " " + u.getLastName());
            cs.endText();
            y -= leading * 2;

            /* ---------- medidas actuales ---------- */
            if (current != null) {
                y = drawCurrent(cs, current, u.getHeightCm(), x, y);
                y -= leading;
            }

            /* ---------- medidas históricas ---------- */
            if (!statsHist.isEmpty()) {
                y = drawHistorical(cs, statsHist, x, y);
                y -= leading;
            }

            /* ---------- histórico entrenos ---------- */
            drawWorkouts(cs, workouts, x, y);

            cs.close();

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                pdf.save(bos);
                log.debug("[PDF] Tamaño final = {} bytes", bos.size());
                return bos.toByteArray();
            }

        } catch (Exception ex) {
            log.error("PDF | ERROR {}", ex.getMessage(), ex);
            throw new RuntimeException("PDF error", ex);
        }
    }

    /* =========================================================
       BLOQUES DE DIBUJO
       ========================================================= */
    private float drawCurrent(PDPageContentStream cs, BodyStats s,
                              Double heightCm, float x, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(x, y);
        cs.showText("Medidas actuales");
        cs.endText();
        y -= 18;

        Map<String,String> rows = Map.ofEntries(
                Map.entry("Peso (kg)",     nf(s.getWeightKg())),
                Map.entry("Estatura (cm)", nf(heightCm)),
                Map.entry("Cuello",        nf(s.getNeckCm())),
                Map.entry("Pecho",         nf(s.getChestCm())),
                Map.entry("Cintura",       nf(s.getWaistCm())),
                Map.entry("Abd. bajo",     nf(s.getLowerAbsCm())),
                Map.entry("Cadera",        nf(s.getHipCm())),
                Map.entry("Bíceps relax",  nf(s.getBicepsCm())),
                Map.entry("Bíceps flex",   nf(s.getBicepsFlexCm())),
                Map.entry("Antebrazo",     nf(s.getForearmCm())),
                Map.entry("Muslo",         nf(s.getThighCm())),
                Map.entry("Pantorrilla",   nf(s.getCalfCm()))
        );
        cs.setFont(PDType1Font.HELVETICA, 11);
        for (var e : rows.entrySet()) {
            cs.beginText();
            cs.newLineAtOffset(x + 10, y);
            cs.showText(String.format("%-18s %s", e.getKey() + ":", e.getValue()));
            cs.endText();
            y -= 14;
        }
        return y;
    }

    private float drawHistorical(PDPageContentStream cs,
                                 List<BodyStats> hist,
                                 float x, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(x, y);
        cs.showText("Medidas históricas");
        cs.endText();
        y -= 18;

        /* cabecera */
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        float yy = y;
        for (String head : List.of("Fecha","Peso","Cintura","Cadera","Muslo","Bíceps"))
        {
            cs.beginText();
            cs.newLineAtOffset(x + 10, yy);
            cs.showText(head);
            cs.endText();
            x += 60;
        }
        x -= 10 + 60*5;
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA, 10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        for (BodyStats s : hist) {
            String[] cols = {
                    df.format(s.getDate()),
                    nf(s.getWeightKg()),
                    nf(s.getWaistCm()),
                    nf(s.getHipCm()),
                    nf(s.getThighCm()),
                    nf(s.getBicepsCm())
            };
            float xx = x + 10;
            for (String c : cols) {
                cs.beginText();
                cs.newLineAtOffset(xx, y);
                cs.showText(c);
                cs.endText();
                xx += 60;
            }
            y -= 12;
        }
        return y;
    }

    private void drawWorkouts(PDPageContentStream cs,
                              List<DailyEntry> wos,
                              float x, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(x, y);
        cs.showText("Histórico de entrenos");
        cs.endText();
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText(); cs.newLineAtOffset(x+10, y); cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+70, y); cs.showText("Máquina"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+270, y);cs.showText("Kg / Reps x Sets"); cs.endText();
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA, 10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (DailyEntry e : wos) {
            for (DailyEntry.Exercise ex : e.getDetails().values()) {
                cs.beginText(); cs.newLineAtOffset(x+10, y); cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+70, y); cs.showText(ex.getName());          cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+270, y);
                cs.showText(ex.getWeightKg() + " kg / " + ex.getReps() + " x " + ex.getSets());
                cs.endText();
                y -= 12;
            }
        }
    }

    /* ========================================================= */
    private static String nf(Double d){ return d == null ? "—" : String.format("%.1f", d); }
}
