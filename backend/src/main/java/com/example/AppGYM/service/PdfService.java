// backend/src/main/java/com/example/AppGYM/service/PdfService.java
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

    /* ---------- Config imagen ---------- */
    private static final long   MAX_BYTES_ORIGINAL = 1_000_000;            // 1 MB
    private static final float  TARGET_IMG_W       = 160f;                 // ancho lógico
    private static final float  GAP_W              = 15f;                  // separación entre fotos

    /* ===================================================================== */
    /*                                API                                   */
    /* ===================================================================== */

    public byte[] buildFull(User u)                    { return build(u,null,null); }
    public byte[] buildPeriod(User u,LocalDate f,LocalDate t){ return build(u,f,t); }

    /* ===================================================================== */
    /*                        GENERADOR PRINCIPAL                            */
    /* ===================================================================== */

    private byte[] build(User u, LocalDate from, LocalDate to) {

        BodyStats current = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);
        List<BodyStats> hist = statsRepo.findByUserIdOrderByDateAsc(u.getId());

        List<DailyEntry> workouts = (from==null||to==null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to);

        log.debug("PDF | usr={}, current={}, history={}, workouts={}",
                u.getEmail(), current!=null, hist.size(), workouts.size());

        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(pdf,page)) {

                float x = 50, y = page.getMediaBox().getHeight() - 50;
                float leading = 15;

                /* ---- título ---- */
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD,20);
                cs.newLineAtOffset(x,y);
                cs.showText("Informe de progreso – "+u.getFirstName()+" "+u.getLastName());
                cs.endText();
                y -= leading*2;

                /* ---- medidas actuales ---- */
                if (current!=null){
                    y = drawCurrentMeasures(cs,current,u,x,y) - leading;
                }

                /* ---- histórico medidas ---- */
                if (!hist.isEmpty()){
                    y = drawHistoricalMeasures(cs,hist,x,y) - leading;
                }

                /* ---- fotos ---- */
                y = drawPhotos(pdf,cs,current,x,y) - leading;

                /* ---- entrenos ---- */
                drawWorkouts(cs,workouts,x,y);
            }

            /* devolver bytes */
            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                pdf.save(bos);
                log.debug("[PDF] Tamaño final = {} bytes", bos.size());
                return bos.toByteArray();
            }

        } catch (Exception ex){
            log.error("PDF | ERROR {}", ex.getMessage(), ex);
            throw new RuntimeException("PDF error", ex);
        }
    }

    /* ===================================================================== */
    /*                           BLOQUES DE DIBUJO                           */
    /* ===================================================================== */

    /* --- cuadro con las medidas “actuales” (último registro + perfil) ---- */
    private float drawCurrentMeasures(PDPageContentStream cs, BodyStats s,
                                      User u, float x, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y);
        cs.showText("Medidas actuales");
        cs.endText();
        y -= 18;

        Map<String,String> rows = Map.ofEntries(
                Map.entry("Peso (kg)",      nf(s.getWeightKg())),
                Map.entry("Estatura (cm)",  nf(u.getHeightCm())),
                Map.entry("Cuello",         nf(s.getNeckCm())),
                Map.entry("Pecho",          nf(s.getChestCm())),
                Map.entry("Cintura",        nf(s.getWaistCm())),
                Map.entry("Abd. bajo",      nf(s.getLowerAbsCm())),
                Map.entry("Cadera",         nf(s.getHipCm())),
                Map.entry("Bíceps relax",   nf(s.getBicepsCm())),
                Map.entry("Bíceps flex",    nf(s.getBicepsFlexCm())),
                Map.entry("Antebrazo",      nf(s.getForearmCm())),
                Map.entry("Muslo",          nf(s.getThighCm())),
                Map.entry("Pantorrilla",    nf(s.getCalfCm()))
        );

        cs.setFont(PDType1Font.HELVETICA,11);
        for (var e : rows.entrySet()){
            cs.beginText();
            cs.newLineAtOffset(x+10,y);
            cs.showText(String.format("%-18s %s", e.getKey()+":", e.getValue()));
            cs.endText();
            y -= 14;
        }
        return y;
    }

    /* --- tabla de medidas históricas ------------------------------------ */
    private float drawHistoricalMeasures(PDPageContentStream cs,
                                         List<BodyStats> hist,
                                         float x,float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y);
        cs.showText("Medidas históricas");
        cs.endText();
        y -= 18;

        /* cabecera */
        String[] heads = {"Fecha","Peso","Cintura","Cadera","Muslo","Bíceps","IMG Front/Side/Back"};
        float[]  colW  = {60,45,50,50,45,50,180};
        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        float xx = x+10;
        for (int i=0;i<heads.length;i++){
            cs.beginText(); cs.newLineAtOffset(xx,y); cs.showText(heads[i]); cs.endText();
            xx += colW[i];
        }
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA,10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (BodyStats s: hist){
            String[] data = {
                    df.format(s.getDate()),
                    nf(s.getWeightKg()),
                    nf(s.getWaistCm()),
                    nf(s.getHipCm()),
                    nf(s.getThighCm()),
                    nf(s.getBicepsCm()),
                    imgLabel(s)
            };
            xx = x+10;
            for (int i=0;i<data.length;i++){
                cs.beginText(); cs.newLineAtOffset(xx,y); cs.showText(data[i]); cs.endText();
                xx += colW[i];
            }
            y -= 12;
        }
        return y;
    }

    /* --- inserta hasta 3 fotos (front/side/back) ------------------------- */
    private float drawPhotos(PDDocument doc,
                             PDPageContentStream cs,
                             BodyStats cur,
                             float x,float y) {

        try{
            if (cur==null) return y;

            Map<ProgressPhoto.Type,String> map = Map.of(
                    ProgressPhoto.Type.FRONT, cur.getFrontImgUrl(),
                    ProgressPhoto.Type.SIDE , cur.getSideImgUrl(),
                    ProgressPhoto.Type.BACK , cur.getBackImgUrl()
            );

            boolean any = map.values().stream().anyMatch(p -> p!=null && !p.isBlank());
            if (!any) return y;

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD,14);
            cs.newLineAtOffset(x,y);
            cs.showText("Fotos");
            cs.endText();
            y -= 18;

            float yy = y;         // la línea base de imágenes
            float xx = x;

            for (var entry: map.entrySet()){
                String pathStr = entry.getValue();
                if (pathStr==null || pathStr.isBlank()){ xx += TARGET_IMG_W+GAP_W; continue; }

                Path p = Path.of(pathStr.startsWith("/uploads/") ? pathStr.substring(1) : pathStr);
                if (!Files.exists(p)){ xx += TARGET_IMG_W+GAP_W; continue; }

                /* --- escalado inteligente -------------------------------- */
                long bytes = Files.size(p);
                float factor = 1f;
                if (bytes > MAX_BYTES_ORIGINAL){
                    factor = (float)Math.sqrt((double)MAX_BYTES_ORIGINAL / bytes);
                }

                PDImageXObject img = PDImageXObject.createFromFile(p.toString(), doc);

                float w = TARGET_IMG_W * factor;
                float h = w * img.getHeight() / img.getWidth();

                cs.drawImage(img, xx, yy-h, w, h);
                xx += w + GAP_W;
            }
            /* altura real ocupada = la mayor h (≈ TARGET_IMG_W) */
            return yy - TARGET_IMG_W - 5;

        }catch(Exception ex){
            log.warn("PDF | drawPhotos error: {}", ex.getMessage());
            return y;   // continúa sin fotos
        }
    }

    /* --- histórico de entrenamientos ------------------------------------ */
    private void drawWorkouts(PDPageContentStream cs,
                              List<DailyEntry> list,
                              float x,float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y);
        cs.showText("Histórico de entrenos");
        cs.endText();
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        cs.beginText(); cs.newLineAtOffset(x+10,y); cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+70,y); cs.showText("Máquina"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+270,y);cs.showText("Kg / Reps x Sets"); cs.endText();
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA,10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (DailyEntry e: list){
            for (DailyEntry.Exercise ex: e.getDetails().values()){
                cs.beginText(); cs.newLineAtOffset(x+10,y);  cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+70,y);  cs.showText(ex.getName());           cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+270,y); cs.showText(
                        ex.getWeightKg()+" kg / "+ex.getReps()+" x "+ex.getSets());               cs.endText();
                y -= 12;
            }
        }
    }

    /* ===================================================================== */
    /*                                 util                                  */
    /* ===================================================================== */

    private static String nf(Double d){ return d==null ? "—" : String.format("%.1f",d); }

    private static String imgLabel(BodyStats s){
        return (s.getFrontImgUrl()!=null? "✔":"—")+" / "
                + (s.getSideImgUrl() !=null? "✔":"—")+" / "
                + (s.getBackImgUrl() !=null? "✔":"—");
    }
}
