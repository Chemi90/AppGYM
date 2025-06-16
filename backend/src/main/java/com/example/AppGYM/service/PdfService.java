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

    /* ======== API público ======== */
    public byte[] buildFull(User u){ return build(u,null,null); }

    public byte[] buildPeriod(User u, LocalDate from, LocalDate to){
        return build(u,from,to);
    }

    /* ======== motor ======== */
    private byte[] build(User u, LocalDate from, LocalDate to){

        BodyStats current = statsRepo
                .findTopByUserIdOrderByDateDesc(u.getId())
                .orElse(null);

        List<BodyStats> history = statsRepo.findByUserIdOrderByDateAsc(u.getId());

        List<DailyEntry> workouts =
                (from==null||to==null)
                        ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                        : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to);

        log.debug("PDF | usr={}, current={}, history={}, workouts={}",
                u.getEmail(), current!=null, history.size(), workouts.size());

        try(PDDocument pdf = new PDDocument()){
            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);

            try(PDPageContentStream cs = new PDPageContentStream(pdf,page)){

                float margin = 50, leading = 16;
                float y = page.getMediaBox().getHeight()-margin;

                /* título */
                cs.setFont(PDType1Font.HELVETICA_BOLD,20);
                cs.beginText(); cs.newLineAtOffset(margin,y);
                cs.showText("Informe de progreso – "+u.getFirstName()+" "+u.getLastName());
                cs.endText(); y-=leading*2;

                if(from!=null && to!=null){
                    cs.setFont(PDType1Font.HELVETICA,12);
                    cs.beginText(); cs.newLineAtOffset(margin,y);
                    cs.showText("Período: "+from+" → "+to);
                    cs.endText(); y-=leading;
                }

                /* 1) bloque Medidas actuales */
                y = drawCurrent(pdf,cs,current,u,y-leading);

                /* 2) bloque Históricas */
                y = drawHistory(pdf,cs,history,y-leading);

                /* 3) bloque Entrenos */
                drawWorkouts(pdf,cs,workouts,y-leading);
            }

            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                pdf.save(bos);
                return bos.toByteArray();
            }

        }catch(Exception ex){
            log.error("PDF generation error",ex);
            throw new RuntimeException(ex);
        }
    }

    /* ------------------------------------------------------------------ */
    private float drawCurrent(PDDocument pdf, PDPageContentStream cs,
                              BodyStats s, User u, float y) throws Exception{

        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.beginText(); cs.newLineAtOffset(50,y);
        cs.showText("Medidas actuales"); cs.endText();
        y-=18;

        cs.setFont(PDType1Font.HELVETICA,11);

        Map<String,String> rows = Map.ofEntries(
                Map.entry("Peso (kg)",     nf(s==null?null:s.getWeightKg())),
                Map.entry("Estatura (cm)", nf(u.getHeightCm())),
                Map.entry("Cuello",        nf(s==null?null:s.getNeckCm())),
                Map.entry("Pecho",         nf(s==null?null:s.getChestCm())),
                Map.entry("Cintura",       nf(s==null?null:s.getWaistCm())),
                Map.entry("Abd. bajo",     nf(s==null?null:s.getLowerAbsCm())),
                Map.entry("Cadera",        nf(s==null?null:s.getHipCm())),
                Map.entry("Bíceps relax",  nf(s==null?null:s.getBicepsCm())),
                Map.entry("Bíceps flex",   nf(s==null?null:s.getBicepsFlexCm())),
                Map.entry("Antebrazo",     nf(s==null?null:s.getForearmCm())),
                Map.entry("Muslo",         nf(s==null?null:s.getThighCm())),
                Map.entry("Pantorrilla",   nf(s==null?null:s.getCalfCm()))
        );
        for(var entry:rows.entrySet()){
            cs.beginText(); cs.newLineAtOffset(60,y);
            cs.showText(String.format("%-18s %s",entry.getKey()+":",entry.getValue()));
            cs.endText();
            y-=14;
        }
        return y;
    }

    /* ------------------------------------------------------------------ */
    private float drawHistory(PDDocument pdf, PDPageContentStream cs,
                              List<BodyStats> list, float y) throws Exception{

        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.beginText(); cs.newLineAtOffset(50,y);
        cs.showText("Medidas históricas"); cs.endText();
        y-=18;

        /* cabeceras */
        String[] head = {"Fecha","Peso","Cintura","Cadera","Muslo","Bíceps","IMG Front/Side/Back"};
        float[]  colX = {60,110,160,210,260,310,370};

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        for(int i=0;i<head.length;i++){
            cs.beginText(); cs.newLineAtOffset(colX[i],y); cs.showText(head[i]); cs.endText();
        }
        y-=14;

        cs.setFont(PDType1Font.HELVETICA,10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for(BodyStats s : list){
            String[] row = {
                    df.format(s.getDate()),
                    nf(s.getWeightKg()),
                    nf(s.getWaistCm()),
                    nf(s.getHipCm()),
                    nf(s.getThighCm()),
                    nf(s.getBicepsCm()),
                    (s.getFrontImgUrl()==null?"—":"✔")+"/"+
                            (s.getSideImgUrl ()==null?"—":"✔")+"/"+
                            (s.getBackImgUrl ()==null?"—":"✔")
            };
            for(int i=0;i<row.length;i++){
                cs.beginText(); cs.newLineAtOffset(colX[i],y); cs.showText(row[i]); cs.endText();
            }
            y-=12;
        }
        return y;
    }

    /* ------------------------------------------------------------------ */
    private void drawWorkouts(PDDocument pdf, PDPageContentStream cs,
                              List<DailyEntry> list, float y) throws Exception{

        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.beginText(); cs.newLineAtOffset(50,y);
        cs.showText("Histórico de entrenos"); cs.endText();
        y-=18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        cs.beginText(); cs.newLineAtOffset(60,y);  cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(150,y); cs.showText("Máquina"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(350,y); cs.showText("Kg / Reps x Sets"); cs.endText();
        y-=14;

        cs.setFont(PDType1Font.HELVETICA,10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for(DailyEntry e : list){
            for(DailyEntry.Exercise ex : e.getDetails().values()){
                cs.beginText(); cs.newLineAtOffset(60,y);  cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(150,y); cs.showText(ex.getName());          cs.endText();
                cs.beginText(); cs.newLineAtOffset(350,y);
                cs.showText(nf(ex.getWeightKg())+" kg / "+ex.getReps()+"x"+ex.getSets());
                cs.endText();
                y-=12;

                /* salto de página */
                if(y<70){
                    cs.close();
                    PDPage newP = new PDPage(PDRectangle.LETTER);
                    pdf.addPage(newP);
                    cs = new PDPageContentStream(pdf,newP);
                    y = newP.getMediaBox().getHeight()-50;

                    /* re-dibujar cabeceras para la nueva página */
                    cs.setFont(PDType1Font.HELVETICA_BOLD,11);
                    cs.beginText(); cs.newLineAtOffset(60,y);  cs.showText("Fecha"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(150,y); cs.showText("Máquina"); cs.endText();
                    cs.beginText(); cs.newLineAtOffset(350,y); cs.showText("Kg / Reps x Sets"); cs.endText();
                    y-=14;
                    cs.setFont(PDType1Font.HELVETICA,10);
                }
            }
        }
        cs.close();  // última ContentStream
    }

    /* ------------------------------------------------------------------ */
    private static String nf(Double d){ return d==null?"—":String.format("%.1f",d); }
}
