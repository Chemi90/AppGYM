// backend/src/main/java/com/example/AppGYM/service/PdfService.java
package com.example.AppGYM.service;

import com.example.AppGYM.model.*;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.DailyEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.*;
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

    /* ======================  API  ====================== */

    public byte[] buildFull(User u)                       { return build(u, null, null); }
    public byte[] buildPeriod(User u, LocalDate f, LocalDate t){ return build(u, f, t); }

    /* ================  GENERADOR PRINCIPAL  ================ */

    private byte[] build(User u, LocalDate from, LocalDate to) {

        List<BodyStats> statsHist = statsRepo.findByUserIdOrderByDateAsc(u.getId());
        BodyStats       current   = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);

        List<DailyEntry> workouts = (from==null||to==null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to);

        log.debug("PDF | usr={}, current={}, history={}, workouts={}",
                u.getEmail(), current!=null, statsHist.size(), workouts.size());

        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf, page);

            try {                           /* ==== cuerpo principal ==== */
                float x = 50, y = page.getMediaBox().getHeight() - 50, leading = 15;

                /*  TÍTULO  */
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.newLineAtOffset(x, y);
                cs.showText("Informe de progreso – "+u.getFirstName()+" "+u.getLastName());
                cs.endText();
                y -= leading*2;

                if (current     !=null) y = drawCurrent(cs,current,u,x,y)-leading;
                if (!statsHist.isEmpty()) y = drawHistory(cs,statsHist,x,y)-leading;
                y = drawPhotos(pdf,cs,current,x,y)-leading;
                drawWorkouts(cs,workouts,x,y);

            } finally { cs.close(); }       /* ciérralo aunque falle algo */

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                pdf.save(bos);
                return bos.toByteArray();
            }

        } catch (Exception ex){
            log.error("PDF | ERROR al generar informe: {}", ex.getMessage(), ex);
            throw new RuntimeException("No se pudo generar el PDF",ex);
        }
    }

    /* ===================  BLOQUES =================== */

    private float drawCurrent(PDPageContentStream cs, BodyStats s, User u,
                              float x,float y) throws Exception{

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y);
        cs.showText("Medidas actuales");
        cs.endText();
        y-=18;

        Map<String,String> rows = Map.ofEntries(
                Map.entry("Peso (kg)",      nf(s.getWeightKg())),
                Map.entry("Estatura (cm)",  nf(u.getHeightCm())),
                Map.entry("Cintura",        nf(s.getWaistCm())),
                Map.entry("Cadera",         nf(s.getHipCm())),
                Map.entry("Muslo",          nf(s.getThighCm())),
                Map.entry("Bíceps",         nf(s.getBicepsCm()))
        );
        cs.setFont(PDType1Font.HELVETICA,11);
        for(var e:rows.entrySet()){
            cs.beginText();
            cs.newLineAtOffset(x+10,y);
            cs.showText(String.format("%-15s %s",e.getKey()+":",e.getValue()));
            cs.endText();
            y-=14;
        }
        return y;
    }

    private float drawHistory(PDPageContentStream cs,List<BodyStats> h,
                              float x,float y)throws Exception{

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y);
        cs.showText("Medidas históricas");
        cs.endText();
        y-=18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        String[] heads={"Fecha","Peso","Cintura","Cadera","Muslo","Bíceps"};
        float[]  offs ={0,70,130,190,250,310};

        for(int i=0;i<heads.length;i++){
            cs.beginText(); cs.newLineAtOffset(x+offs[i],y); cs.showText(heads[i]); cs.endText();
        }
        y-=14; cs.setFont(PDType1Font.HELVETICA,10);
        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;

        for(BodyStats s:h){
            String[] cols={
                    df.format(s.getDate()),
                    nf(s.getWeightKg()),
                    nf(s.getWaistCm()),
                    nf(s.getHipCm()),
                    nf(s.getThighCm()),
                    nf(s.getBicepsCm())
            };
            for(int i=0;i<cols.length;i++){
                cs.beginText(); cs.newLineAtOffset(x+offs[i],y); cs.showText(cols[i]); cs.endText();
            }
            y-=12;
        }
        return y;
    }

    /* ----------------  Fotos ---------------- */
    private float drawPhotos(PDDocument doc, PDPageContentStream cs,
                             BodyStats cur, float x,float y) throws Exception{

        if(cur==null) return y;

        Map<ProgressPhoto.Type,String> map = Map.of(
                ProgressPhoto.Type.FRONT, cur.getFrontImgUrl(),
                ProgressPhoto.Type.SIDE , cur.getSideImgUrl(),
                ProgressPhoto.Type.BACK , cur.getBackImgUrl()
        );
        if(map.values().stream().allMatch(v->v==null||v.isBlank())) return y;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y);
        cs.showText("Fotos");
        cs.endText();
        y-=18;

        float imgW=150, imgH=0, gap=10, xx=x;
        for(var e:map.entrySet()){
            String url=e.getValue();
            if(url==null||url.isBlank()){ xx+=imgW+gap; continue; }

            /* ---- búsqueda del archivo real ---- */
            Path p = url.startsWith("/") ? Path.of(url) : Path.of("uploads").resolve(url);
            log.debug("[PDF] buscando foto {} -> {}", e.getKey(), p);

            if(!Files.exists(p)){
                log.warn("[PDF] NO existe {}", p);
                xx+=imgW+gap; continue;
            }
            try{
                PDImageXObject img = PDImageXObject.createFromFile(p.toString(), doc);
                imgH = imgW * img.getHeight() / img.getWidth();
                cs.drawImage(img, xx, y-imgH, imgW, imgH);
                log.debug("[PDF] insertada {} ({}x{})", p, img.getWidth(), img.getHeight());
            }catch(Exception ex){
                log.error("[PDF] error con {}: {}", p, ex.getMessage());
            }
            xx+=imgW+gap;
        }
        return y - imgH - 5;
    }

    /* ----------------  Workouts ---------------- */
    private void drawWorkouts(PDPageContentStream cs,List<DailyEntry> w,float x,float y)throws Exception{

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y); cs.showText("Histórico de entrenos"); cs.endText();
        y-=18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        cs.beginText(); cs.newLineAtOffset(x,     y); cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+70,  y); cs.showText("Máquina"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+270, y); cs.showText("Kg / Reps x Sets"); cs.endText();
        y-=14;

        cs.setFont(PDType1Font.HELVETICA,10);
        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;
        for(DailyEntry e:w){
            for(DailyEntry.Exercise ex:e.getDetails().values()){
                cs.beginText(); cs.newLineAtOffset(x,     y); cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+70,  y); cs.showText(ex.getName());            cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+270, y);
                cs.showText(ex.getWeightKg()+" kg / "+ex.getReps()+" x "+ex.getSets()); cs.endText();
                y-=12;
            }
        }
    }

    /* ---------------- util ---------------- */
    private static String nf(Double d){ return d==null?"—":String.format("%.1f",d); }
}
