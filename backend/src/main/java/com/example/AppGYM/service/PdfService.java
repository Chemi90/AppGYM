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

    /* ---------- API público ---------- */
    public byte[] buildFull(User u)                       { return build(u, null, null); }
    public byte[] buildPeriod(User u, LocalDate f, LocalDate t){ return build(u, f, t); }

    /* ---------- generador ---------- */
    private byte[] build(User u, LocalDate from, LocalDate to) {

        List<BodyStats> statsHist = statsRepo.findByUserIdOrderByDateAsc(u.getId());
        BodyStats       current   = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);

        List<DailyEntry> workouts  = (from==null||to==null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to);

        log.debug("PDF | usr={}, current={}, history={}, workouts={}",
                u.getEmail(), current!=null, statsHist.size(), workouts.size());

        try (PDDocument pdf = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf, page);

            float x = 50, y = page.getMediaBox().getHeight() - 50, leading = 15;

            /* título */
            cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,20);
            cs.newLineAtOffset(x,y); cs.showText("Informe de progreso – "
                    + u.getFirstName()+" "+u.getLastName());
            cs.endText(); y -= leading*2;

            if (current!=null){ y = drawCurrentMeasures(cs,current,x,y) - leading; }
            if (!statsHist.isEmpty()){ y = drawHistoricalMeasures(cs,statsHist,x,y) - leading; }

            y = drawPhotos(pdf,cs,current,x,y) - leading;
            drawWorkouts(cs,workouts,x,y);

            cs.close();
            try (ByteArrayOutputStream bos=new ByteArrayOutputStream()){
                pdf.save(bos); return bos.toByteArray();
            }

        } catch (Exception ex){
            throw new RuntimeException("PDF error",ex);
        }
    }

    /* ---------- bloques ---------- */

    private float drawCurrentMeasures(PDPageContentStream cs, BodyStats s,
                                      float x, float y) throws Exception {

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y); cs.showText("Medidas actuales");
        cs.endText(); y-=18;

        Map<String,String> rows = Map.ofEntries(
                Map.entry("Peso (kg)"      , nf(s.getWeightKg())),
                Map.entry("Estatura (cm)"  , nf(s.getUser().getHeightCm())),
                Map.entry("Cuello"         , nf(s.getNeckCm())),
                Map.entry("Pecho"          , nf(s.getChestCm())),
                Map.entry("Cintura"        , nf(s.getWaistCm())),
                Map.entry("Abd. bajo"      , nf(s.getLowerAbsCm())),
                Map.entry("Cadera"         , nf(s.getHipCm())),
                Map.entry("Bíceps relax"   , nf(s.getBicepsCm())),
                Map.entry("Bíceps flex"    , nf(s.getBicepsFlexCm())),
                Map.entry("Antebrazo"      , nf(s.getForearmCm())),
                Map.entry("Muslo"          , nf(s.getThighCm())),
                Map.entry("Pantorrilla"    , nf(s.getCalfCm()))
        );
        cs.setFont(PDType1Font.HELVETICA,11);
        for (var e: rows.entrySet()){
            cs.beginText();
            cs.newLineAtOffset(x+10,y);
            cs.showText(String.format("%-18s %s",e.getKey()+":",e.getValue()));
            cs.endText();
            y-=14;
        }
        return y;
    }

    private float drawHistoricalMeasures(PDPageContentStream cs,
                                         List<BodyStats> hist,
                                         float x,float y) throws Exception {

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y); cs.showText("Medidas históricas");
        cs.endText(); y-=18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        float headY=y;
        for (String h: List.of("Fecha","Peso","Cintura","Cadera","Muslo","Bíceps",
                "IMG Front/Side/Back")){
            cs.beginText(); cs.newLineAtOffset(x+10,headY); cs.showText(h); cs.endText();
            x += (h.equals("IMG Front/Side/Back")?180:60);
        }
        x -= 10 + 60*5 + 180; y-=14; cs.setFont(PDType1Font.HELVETICA,10);

        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;
        for (BodyStats s:hist){
            String[] col={
                    df.format(s.getDate()), nf(s.getWeightKg()), nf(s.getWaistCm()),
                    nf(s.getHipCm()), nf(s.getThighCm()), nf(s.getBicepsCm()), imgLabel(s)
            };
            float xx=x+10;
            for (String c:col){
                cs.beginText(); cs.newLineAtOffset(xx,y); cs.showText(c); cs.endText();
                xx += (c.equals(col[6])?180:60);
            }
            y-=12;
        }
        return y;
    }

    private float drawPhotos(PDDocument doc, PDPageContentStream cs,
                             BodyStats current, float x,float y) throws Exception {

        Map<ProgressPhoto.Type,String> map = Map.of(
                ProgressPhoto.Type.FRONT, current!=null?current.getFrontImgUrl():null,
                ProgressPhoto.Type.SIDE , current!=null?current.getSideImgUrl() :null,
                ProgressPhoto.Type.BACK , current!=null?current.getBackImgUrl() :null
        );
        if (map.values().stream().noneMatch(v->v!=null&& !v.isBlank())) return y;

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y); cs.showText("Fotos"); cs.endText(); y-=18;

        float imgW=160,imgH=0,gap=15,xx=x;
        for (var e: map.entrySet()){
            if (e.getValue()==null){ xx += imgW+gap; continue; }
            Path p = Path.of(e.getValue().startsWith("/uploads/")
                    ? e.getValue().substring(1) : e.getValue());
            if (!Files.exists(p)){ xx += imgW+gap; continue; }

            PDImageXObject img=PDImageXObject.createFromFile(p.toString(),doc);
            imgH = imgW*img.getHeight()/img.getWidth();
            cs.drawImage(img,xx,y-imgH,imgW,imgH);
            xx += imgW+gap;
        }
        return y-imgH-5;
    }

    private void drawWorkouts(PDPageContentStream cs,List<DailyEntry> wos,
                              float x,float y)throws Exception{

        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(x,y); cs.showText("Histórico de entrenos");
        cs.endText(); y-=18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        cs.beginText(); cs.newLineAtOffset(x+10,y); cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+70,y); cs.showText("Máquina"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x+270,y);cs.showText("Kg / Reps x Sets");cs.endText();
        y-=14; cs.setFont(PDType1Font.HELVETICA,10);

        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;
        for (DailyEntry e:wos){
            for (DailyEntry.Exercise ex:e.getDetails().values()){
                cs.beginText(); cs.newLineAtOffset(x+10,y); cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+70,y); cs.showText(ex.getName()); cs.endText();
                cs.beginText(); cs.newLineAtOffset(x+270,y);
                cs.showText(ex.getWeightKg()+" kg / "+ex.getReps()+" x "+ex.getSets());
                cs.endText(); y-=12;
            }
        }
    }

    /* ---------- util ---------- */
    private static String nf(Double d){ return d==null? "—" : String.format("%.1f",d); }
    private static String imgLabel(BodyStats s){
        return (s.getFrontImgUrl()!=null?"✔":"—")+" / "
                + (s.getSideImgUrl()!=null ?"✔":"—")+" / "
                + (s.getBackImgUrl()!=null ?"✔":"—");
    }
}
