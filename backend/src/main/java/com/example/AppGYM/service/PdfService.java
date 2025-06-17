package com.example.AppGYM.service;

import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.ProgressPhoto;
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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /* ========= API público ========= */

    public byte[] buildFull(User u) {
        return build(u, null, null, false);
    }
    public byte[] buildPeriod(User u, LocalDate from, LocalDate to) {
        return build(u, from, to, false);
    }

    /* ========= Generador principal ========= */

    private byte[] build(User u,
                         LocalDate from,
                         LocalDate to,
                         boolean skipPhotos) {

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

            /* margenes & tipografía base */
            float x = 50, y = page.getMediaBox().getHeight() - 50;
            float leading = 15;

            /* ------ Título ------ */
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
            cs.newLineAtOffset(x, y);
            cs.showText("Informe de progreso – " + u.getFirstName() + " " + u.getLastName());
            cs.endText();
            y -= leading * 2;

            /* ===== MEDIDAS ACTUALES ===== */
            if (current != null) {
                y = drawCurrentMeasures(cs, current, u, x, y);
                y -= leading;
            }

            /* ===== MEDIDAS HISTÓRICAS ===== */
            if (!statsHist.isEmpty()) {
                y = drawHistoricalMeasures(cs, statsHist, x, y);
                y -= leading;
            }

            /* ===== FOTOS ===== */
            if (!skipPhotos)
                y = drawPhotos(pdf, cs, current, x, y);

            /* ===== HISTÓRICO ENTRENOS ===== */
            drawWorkouts(cs, workouts, x, y);

            cs.close();

            /* ---------- devolver bytes ---------- */
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

                pdf.save(bos);
                log.debug("[PDF] bytes totales = {}", bos.size());   // ➊ registro del peso final

                // ➌ Si supera los 5 MB volvemos a generarlo sin fotos
                if (!skipPhotos && bos.size() > 5_000_000) {
                    log.warn("[PDF] >5 MB, rehaciendo sin imágenes…");
                    return build(u, from, to, true);
                }

                return bos.toByteArray();
            }

        } catch (Exception ex) {
            log.error("PDF | ERROR {}", ex.getMessage(), ex);
            throw new RuntimeException("PDF error", ex);
        }
    }

    /* ---------------------------------------------------------- */
    /* ----------  BLOQUES DE DIBUJO  --------------------------- */

    /** Medidas actuales (perfil) */
    private float drawCurrentMeasures(PDPageContentStream cs,
                                      BodyStats s,
                                      User u,
                                      float x, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(x, y);
        cs.showText("Medidas actuales");
        cs.endText();
        y -= 18;

        Map<String,String> rows = Map.ofEntries(
                Map.entry("Peso (kg)",     nf(s == null ? null : s.getWeightKg())),
                Map.entry("Estatura (cm)", nf(u.getHeightCm())),
                Map.entry("Cuello",        nf(s == null ? null : s.getNeckCm())),
                Map.entry("Pecho",         nf(s == null ? null : s.getChestCm())),
                Map.entry("Cintura",       nf(s == null ? null : s.getWaistCm())),
                Map.entry("Abd. bajo",     nf(s == null ? null : s.getLowerAbsCm())),
                Map.entry("Cadera",        nf(s == null ? null : s.getHipCm())),
                Map.entry("Bíceps relax",  nf(s == null ? null : s.getBicepsCm())),
                Map.entry("Bíceps flex",   nf(s == null ? null : s.getBicepsFlexCm())),
                Map.entry("Antebrazo",     nf(s == null ? null : s.getForearmCm())),
                Map.entry("Muslo",         nf(s == null ? null : s.getThighCm())),
                Map.entry("Pantorrilla",   nf(s == null ? null : s.getCalfCm()))
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

    /** Tabla de medidas históricas */
    private float drawHistoricalMeasures(PDPageContentStream cs,
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
        for (String head : List.of("Fecha","Peso","Cintura","Cadera",
                "Muslo","Bíceps","IMG Front/Side/Back"))
        {
            cs.beginText();
            cs.newLineAtOffset(x + 10, yy);
            cs.showText(head);
            cs.endText();
            x += (head.equals("IMG Front/Side/Back") ? 180 : 60);
        }
        x -= 10 + (60*5) + 180;
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
                    nf(s.getBicepsCm()),
                    imgLabel(s)
            };
            float xx = x + 10;
            for (String c : cols) {
                cs.beginText();
                cs.newLineAtOffset(xx, y);
                cs.showText(c);
                cs.endText();
                xx += (c.equals(cols[6]) ? 180 : 60);
            }
            y -= 12;
        }
        return y;
    }

    /** Fotos escaladas dinámicamente */
    private float drawPhotos(PDDocument doc,
                             PDPageContentStream cs,
                             BodyStats current,
                             float x, float y) throws Exception {

        Map<ProgressPhoto.Type, String> map = Map.of(
                ProgressPhoto.Type.FRONT, current != null ? current.getFrontImgUrl() : null,
                ProgressPhoto.Type.SIDE , current != null ? current.getSideImgUrl()  : null,
                ProgressPhoto.Type.BACK , current != null ? current.getBackImgUrl()  : null
        );
        boolean any = map.values().stream().anyMatch(v -> v != null && !v.isBlank());
        if (!any) return y;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.newLineAtOffset(x, y);
        cs.showText("Fotos");
        cs.endText();
        y -= 18;

        float drawW = 170, gap = 15, xx = x, maxH = 0;

        for (var e : map.entrySet()) {
            if (e.getValue() == null) { xx += drawW + gap; continue; }

            /* ---------- ruta local relativa a /uploads ---------- */
            Path p = Path.of(e.getValue().startsWith("/uploads/")
                    ? e.getValue().substring(1)   // «uploads/…»
                    : e.getValue());
            if (!Files.exists(p)) { xx += drawW + gap; continue; }

            /* ---------- carga y re-escala si es necesario ---------- */
            PDImageXObject img = PDImageXObject.createFromFile(p.toString(), doc);

            /* ➋ Reducimos la imagen REAL a máx. 600 px de ancho */
            float realW = img.getWidth()  > 600 ? 600 : img.getWidth();
            float realH = img.getHeight() * realW / img.getWidth();

            /* dibujamos ocupando drawW ≈ 6 cm  */
            float drawH = drawW * realH / realW;
            maxH = Math.max(maxH, drawH);

            cs.drawImage(img, xx, y - drawH, drawW, drawH);

            xx += drawW + gap;
        }
        return y - maxH - 5;
    }

    /** Tabla de entrenamientos */
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
                cs.beginText(); cs.newLineAtOffset(x+70, y); cs.showText(ex.getName());         cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+270, y);
                cs.showText(ex.getWeightKg() + " kg / " + ex.getReps() + " x " + ex.getSets());
                cs.endText();
                y -= 12;
            }
        }
    }

    /* ========= util ========= */
    private static String nf(Double d){ return d == null ? "—" : String.format("%.1f", d); }
    private static String imgLabel(BodyStats s) {
        return (s.getFrontImgUrl()!=null? "✔":"—") + " / "
                + (s.getSideImgUrl() !=null? "✔":"—") + " / "
                + (s.getBackImgUrl() !=null? "✔":"—");
    }
}
