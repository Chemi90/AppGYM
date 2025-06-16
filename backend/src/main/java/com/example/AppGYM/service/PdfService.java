// backend/src/main/java/com/example/AppGYM/service/PdfService.java
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

@Service @RequiredArgsConstructor
public class PdfService {

    private final BodyStatsRepository statsRepo;
    private final DailyEntryRepository dailyRepo;

    /* ======  PDF completo  ====== */
    public byte[] buildFull(User u) {
        return build(u, null, null);
    }

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
                y = drawDaily(cs, u, from, to, y - leading);

                /* (podrías seguir añadiendo fotos, gráficas, etc.) */
            }

            /* ---- devolver bytes ---- */
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
        cs.endText();
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA, 11);
        final String[][] rows = {
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
            cs.showText(String.format("%-18s %s", r[0] + ":", r[1]));
            cs.endText();
            y -= 14;
        }
        return y;
    }

    /* ----------------------------------------------------------- */
    private float drawDaily(PDPageContentStream cs, User u,
                            LocalDate from, LocalDate to,
                            float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(50, y);
        cs.showText("Histórico de entrenos");
        cs.endText();
        y -= 18;

        /* tabla cabecera */
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText(); cs.newLineAtOffset(60,y); cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(150,y);cs.showText("Máquina");cs.endText();
        cs.beginText(); cs.newLineAtOffset(350,y);cs.showText("Kg/Reps/Series");cs.endText();
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA, 10);

        List<DailyEntry> list = (from==null||to==null)
                ? dailyRepo.findByUserId(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to)
                ;

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (DailyEntry e : list) {
            for (var ex : e.getDetails().values()) {
                cs.beginText(); cs.newLineAtOffset(60,y); cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(150,y);cs.showText(ex.getMachine().getName()); cs.endText();
                cs.beginText(); cs.newLineAtOffset(350,y);
                cs.showText(ex.getWeightKg()+" kg / "+ex.getReps()+"x"+ex.getSets());
                cs.endText();
                y -= 12;
                if(y < 70){          // salto de página sencillo
                    cs.close();
                    PDPage newP = new PDPage(PDRectangle.LETTER);
                    cs.getPdDocument().addPage(newP);
                    cs = new PDPageContentStream(cs.getPdDocument(), newP);
                    y = newP.getMediaBox().getHeight() - 50;
                }
            }
        }
        return y;
    }

    /* util */
    private String nf(Double d){ return d==null? "—" : String.format("%.1f",d); }
}
