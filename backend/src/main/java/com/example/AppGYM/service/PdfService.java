// backend/src/main/java/com/example/AppGYM/service/PdfService.java
package com.example.AppGYM.service;

import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.DailyEntryRepository;
import com.example.AppGYM.repository.MachineRepository;
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

@Service @RequiredArgsConstructor
public class PdfService {

    private final BodyStatsRepository  statsRepo;
    private final DailyEntryRepository dailyRepo;
    private final MachineRepository    machines;

    /* ======  PDF completo  ====== */
    public byte[] buildFull(User u) { return build(u, null, null); }

    /* ======  PDF por período  ====== */
    public byte[] buildPeriod(User u, LocalDate from, LocalDate to) {
        return build(u, from, to);
    }

    /* ======  Generador común  ====== */
    private byte[] build(User u, LocalDate from, LocalDate to) {
        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {

                /* ----  Estilos base ---- */
                final float margin = 50, leading = 16;
                float y = page.getMediaBox().getHeight() - margin;

                /* ----  Título ---- */
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("Informe de progreso – " + u.getFirstName() + " " + u.getLastName());
                cs.endText(); y -= leading * 2;

                /* ----  Sub-título período ---- */
                if (from != null && to != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Período: " + from + " → " + to);
                    cs.endText(); y -= leading;
                }

                /* =========  MEDIDAS  ========= */
                y = drawMeasures(cs, u, y - leading);

                /* =========  HISTÓRICO DIARIO  ========= */
                y = drawDaily(cs, pdf, u, from, to, y - leading);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                pdf.save(bos);
                return bos.toByteArray();
            }

        } catch (Exception ex) {
            throw new RuntimeException("PDF error", ex);
        }
    }

    /* ----------------------------------------------------------- */
    private float drawMeasures(PDPageContentStream cs, User u, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Medidas actuales");
        cs.endText(); y -= 18;

        cs.setFont(PDType1Font.HELVETICA, 11);
        String[][] rows = {
                {"Peso (kg)",       nf(u.getWeightKg())},
                {"Estatura (cm)",   nf(u.getHeightCm())},
                {"Cuello",          nf(u.getNeckCm())},
                {"Pecho",           nf(u.getChestCm())},
                {"Cintura",         nf(u.getWaistCm())},
                {"Abd. bajo",       nf(u.getLowerAbsCm())},
                {"Cadera",          nf(u.getHipCm())},
                {"Bíceps relax",    nf(u.getBicepsCm())},
                {"Bíceps flex",     nf(u.getBicepsFlexCm())},
                {"Antebrazo",       nf(u.getForearmCm())},
                {"Muslo",           nf(u.getThighCm())},
                {"Pantorrilla",     nf(u.getCalfCm())}
        };
        for (String[] r : rows) {
            cs.beginText();
            cs.newLineAtOffset(60, y);
            cs.showText(String.format("%-18s %s", r[0] + ":", r[1]));
            cs.endText();
            y -= 14;
        }
        return y;
    }

    /* ----------------------------------------------------------- */
    private float drawDaily(PDPageContentStream cs, PDDocument pdf, User u,
                            LocalDate from, LocalDate to, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Histórico de entrenos");
        cs.endText(); y -= 18;

        /* cabecera tabla */
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText(); cs.newLineAtOffset(60,y);  cs.showText("Fecha");           cs.endText();
        cs.beginText(); cs.newLineAtOffset(150,y); cs.showText("Máquina");         cs.endText();
        cs.beginText(); cs.newLineAtOffset(350,y); cs.showText("Kg / Reps x Sets");cs.endText();
        y -= 14; cs.setFont(PDType1Font.HELVETICA, 10);

        List<DailyEntry> list = (from==null||to==null)
                ? dailyRepo.findByUserId(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to);

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (DailyEntry e : list) {
            for (Map.Entry<Long, DailyEntry.Exercise> ent : e.getDetails().entrySet()) {

                String machineName = machines.findById(ent.getKey())
                        .map(m -> m.getName())
                        .orElse("ID " + ent.getKey());
                var ex = ent.getValue();

                cs.beginText(); cs.newLineAtOffset(60, y);  cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(150, y); cs.showText(machineName);            cs.endText();
                cs.beginText(); cs.newLineAtOffset(350, y);
                cs.showText(ex.getWeightKg() + " kg / " + ex.getReps() + " x " + ex.getSets());
                cs.endText();

                y -= 12;
                if (y < 70) {                     /* salto de página */
                    cs.close();
                    PDPage newP = new PDPage(PDRectangle.LETTER);
                    pdf.addPage(newP);
                    cs = new PDPageContentStream(pdf, newP);
                    y  = newP.getMediaBox().getHeight() - 50;
                }
            }
        }
        cs.close();                 /* cierra el último content-stream */
        return y;
    }

    /* util */
    private String nf(Double d){ return d==null ? "—" : String.format("%.1f", d); }
}
