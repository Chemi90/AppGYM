package com.example.AppGYM.service;

import com.example.AppGYM.model.*;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final BodyStatsRepository statsRepo;
    private final DailyEntryRepository dailyRepo;

    /* ===== interfaz usada por ReportController ======================== */
    public byte[] buildFull(User u)                       { return build(u, null, null); }
    public byte[] buildPeriod(User u, LocalDate f, LocalDate t) { return build(u, f, t); }

    /* ================================================================= */
    private byte[] build(User u, LocalDate from, LocalDate to) {

        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf, page);

            final float margin = 50f, leading = 15f;
            float y = page.getMediaBox().getHeight() - margin;

            /* ---------- título ---------- */
            cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
            text(cs, margin, y,
                    "Informe de progreso – " + u.getFirstName() + " " + u.getLastName());
            y -= leading * 2;

            /* ---------- subtítulo período ---------- */
            if (from != null && to != null) {
                cs.setFont(PDType1Font.HELVETICA, 12);
                text(cs, margin, y,
                        "Período: " + from + " → " + to);
                y -= leading * 2;
            }

            /* ---------- MEDIDAS ---------- */
            y = drawMeasures(cs, u, y, margin, leading);

            /* ---------- HISTÓRICO ---------- */
            y = drawDaily(pdf, cs, u, from, to, y - leading, margin, leading);

            cs.close();

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                pdf.save(bos);
                return bos.toByteArray();
            }

        } catch (Exception ex) {
            throw new IllegalStateException("Error generando PDF", ex);
        }
    }

    /* ================== Medidas ================== */
    private float drawMeasures(PDPageContentStream cs, User u,
                               float y, float m, float lead) throws Exception {

        BodyStats s = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);

        Map<String, Double> rows = new LinkedHashMap<>();
        rows.put("Peso (kg)"     , s != null ? s.getWeightKg()   : u.getWeightKg());
        rows.put("Estatura (cm)" , u.getHeightCm());
        rows.put("Cuello"        , pick(s!=null?s.getNeckCm()       :u.getNeckCm()));
        rows.put("Pecho"         , pick(s!=null?s.getChestCm()      :u.getChestCm()));
        rows.put("Cintura"       , pick(s!=null?s.getWaistCm()      :u.getWaistCm()));
        rows.put("Abd. bajo"     , pick(s!=null?s.getLowerAbsCm()   :u.getLowerAbsCm()));
        rows.put("Cadera"        , pick(s!=null?s.getHipCm()        :u.getHipCm()));
        rows.put("Bíceps relax"  , pick(s!=null?s.getBicepsCm()     :u.getBicepsCm()));
        rows.put("Bíceps flex"   , pick(s!=null?s.getBicepsFlexCm() :u.getBicepsFlexCm()));
        rows.put("Antebrazo"     , pick(s!=null?s.getForearmCm()    :u.getForearmCm()));
        rows.put("Muslo"         , pick(s!=null?s.getThighCm()      :u.getThighCm()));
        rows.put("Pantorrilla"   , pick(s!=null?s.getCalfCm()       :u.getCalfCm()));

        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        text(cs, m, y, "Medidas actuales");
        y -= lead;

        cs.setFont(PDType1Font.HELVETICA, 11);
        for (var e : rows.entrySet()) {
            text(cs, m + 10, y,
                    String.format("%-17s %s", e.getKey() + ":", fmt(e.getValue())));
            y -= lead;
        }
        return y;
    }

    /* ================== Histórico diario ================== */
    private float drawDaily(PDDocument pdf,
                            PDPageContentStream cs,
                            User u, LocalDate from, LocalDate to,
                            float y, float m, float lead) throws Exception {

        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        text(cs, m, y, "Histórico de entrenos");
        y -= lead;

        /* cabecera tabla */
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        text(cs, m +   0, y, "Fecha");
        text(cs, m +  90, y, "Máquina");
        text(cs, m + 300, y, "Kg / Reps x Sets");
        y -= lead;

        cs.setFont(PDType1Font.HELVETICA, 10);

        List<DailyEntry> list = (from == null || to == null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to);

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (DailyEntry e : list) {
            for (DailyEntry.Exercise ex : e.getDetails().values()) {

                text(cs, m +  0, y, df.format(e.getDate()));
                text(cs, m + 90, y, ex.getName());
                text(cs, m +300, y,
                        ex.getWeightKg() + " kg / " + ex.getReps() + " x " + ex.getSets());

                y -= lead;

                /* salto de página simple */
                if (y < 70) {
                    cs.close();
                    PDPage newPg = new PDPage(PDRectangle.LETTER);
                    pdf.addPage(newPg);
                    cs = new PDPageContentStream(pdf, newPg);
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    y = newPg.getMediaBox().getHeight() - 50;
                }
            }
        }
        cs.close();          // cierra último stream
        return y;
    }

    /* ===== helpers ===================================================== */
    private static void text(PDPageContentStream cs, float x, float y, String t) throws Exception {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }
    private static Double pick(Double d) { return d; }              // null-safe
    private static String fmt(Double d) { return d == null ? "—" : "%.1f".formatted(d); }
}
