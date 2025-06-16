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
import java.util.Map;

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final BodyStatsRepository statsRepo;
    private final DailyEntryRepository dailyRepo;

    /* ---------------- API ---------------- */
    public byte[] buildFull(User u)                               { return build(u,null,null); }
    public byte[] buildPeriod(User u, LocalDate f, LocalDate t)   { return build(u,f,t); }

    /* -------------- Core --------------- */
    private byte[] build(User u, LocalDate from, LocalDate to) {
        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float y          = page.getMediaBox().getHeight() - 50;
                final float lead = 16;

                /* Título */
                text(cs,50,y, PDType1Font.HELVETICA_BOLD,20,
                        "Informe de progreso – "+u.getFirstName()+" "+u.getLastName());
                y -= lead*2;

                if (from!=null && to!=null){
                    text(cs,50,y, PDType1Font.HELVETICA,12,
                            "Período: "+from+" → "+to);
                    y -= lead;
                }

                /* Secciones */
                y = drawCurrentStats(cs,u,y-lead);
                y = drawStatsHistory(cs,u,from,to,y-lead);
                drawDaily(doc,cs,u,from,to,y-lead);          // usa doc para page-break
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                doc.save(bos);
                return bos.toByteArray();
            }

        }catch(Exception ex){
            throw new RuntimeException("PDF error",ex);
        }
    }

    /* ========== 1 · Medidas actuales ========== */
    private float drawCurrentStats(PDPageContentStream cs, User u, float y) throws Exception {

        BodyStats cur = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);

        Map<String,Double> vals = Map.ofEntries(
                entry("Peso (kg)"     , cur!=null?cur.getWeightKg():u.getWeightKg()),
                entry("Estatura (cm)" , u.getHeightCm()),
                entry("Cuello"        , val(cur,"getNeckCm")),
                entry("Pecho"         , val(cur,"getChestCm")),
                entry("Cintura"       , val(cur,"getWaistCm")),
                entry("Abd. bajo"     , val(cur,"getLowerAbsCm")),
                entry("Cadera"        , val(cur,"getHipCm")),
                entry("Bíceps relax"  , val(cur,"getBicepsCm")),
                entry("Bíceps flex"   , val(cur,"getBicepsFlexCm")),
                entry("Antebrazo"     , val(cur,"getForearmCm")),
                entry("Muslo"         , val(cur,"getThighCm")),
                entry("Pantorrilla"   , val(cur,"getCalfCm"))
        );

        text(cs,50,y, PDType1Font.HELVETICA_BOLD,14,"Medidas actuales");
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA,11);
        for (var e: vals.entrySet()){
            text(cs,60,y,PDType1Font.HELVETICA,11,
                    String.format("%-18s %s",e.getKey()+":",nf(e.getValue())));
            y -= 14;
        }
        return y;
    }

    /* ========== 2 · Histórico medidas ========== */
    private float drawStatsHistory(PDPageContentStream cs, User u,
                                   LocalDate f, LocalDate t,
                                   float y) throws Exception {

        List<BodyStats> list = (f==null||t==null)
                ? statsRepo.findByUserIdOrderByDateAsc(u.getId())
                : statsRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),f,t);

        if (list.isEmpty()) return y;

        text(cs,50,y, PDType1Font.HELVETICA_BOLD,14,"Histórico de medidas");
        y -= 18;

        String[] col = {"Fecha","Peso","Pecho","Cint.","Cadera","Bíceps","Muslo"};
        float[]  x   ={60,150,210,260,310,370,430};
        for(int i=0;i<col.length;i++)
            text(cs,x[i],y, PDType1Font.HELVETICA_BOLD,10,col[i]);
        y -= 14;

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        cs.setFont(PDType1Font.HELVETICA,9);
        for (BodyStats s: list){
            text(cs,60 ,y, PDType1Font.HELVETICA,9, df.format(s.getDate()));
            text(cs,150,y, PDType1Font.HELVETICA,9, nf(s.getWeightKg()));
            text(cs,210,y, PDType1Font.HELVETICA,9, nf(s.getChestCm()));
            text(cs,260,y, PDType1Font.HELVETICA,9, nf(s.getWaistCm()));
            text(cs,310,y, PDType1Font.HELVETICA,9, nf(s.getHipCm()));
            text(cs,370,y, PDType1Font.HELVETICA,9, nf(s.getBicepsCm()));
            text(cs,430,y, PDType1Font.HELVETICA,9, nf(s.getThighCm()));
            y -= 12;
        }
        return y;
    }

    /* ========== 3 · Histórico entrenos ========== */
    private void drawDaily(PDDocument doc, PDPageContentStream cs, User u,
                           LocalDate f, LocalDate t, float y) throws Exception {

        text(cs,50,y, PDType1Font.HELVETICA_BOLD,14,"Histórico de entrenos");
        y -= 18;

        /* cabecera */
        text(cs,60 ,y, PDType1Font.HELVETICA_BOLD,11,"Fecha");
        text(cs,150,y, PDType1Font.HELVETICA_BOLD,11,"Máquina");
        text(cs,350,y, PDType1Font.HELVETICA_BOLD,11,"Kg / Reps x Sets");
        y -= 14;

        List<DailyEntry> list = (f==null||t==null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),f,t);

        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;

        for (DailyEntry e: list){
            for (DailyEntry.Exercise ex : e.getDetails().values()){

                text(cs,60 ,y, PDType1Font.HELVETICA,10, df.format(e.getDate()));
                text(cs,150,y, PDType1Font.HELVETICA,10, ex.getName());
                text(cs,350,y, PDType1Font.HELVETICA,10,
                        nf(ex.getWeightKg())+" kg / "+ex.getReps()+"x"+ex.getSets());
                y -= 12;

                if (y < 70){
                    cs.close();
                    PDPage newP = new PDPage(PDRectangle.LETTER);
                    doc.addPage(newP);
                    cs = new PDPageContentStream(doc,newP);
                    y = newP.getMediaBox().getHeight() - 50;
                }
            }
        }
        cs.close();         // cerrar stream final
    }

    /* -------------- helpers -------------- */
    private static void text(PDPageContentStream cs,float x,float y,
                             PDType1Font f,int sz,String s)throws Exception{
        cs.setFont(f,sz);
        cs.beginText();
        cs.newLineAtOffset(x,y);
        cs.showText(s);
        cs.endText();
    }
    private static Double val(BodyStats s,String getter){
        try{return s==null?null:(Double)BodyStats.class.getMethod(getter).invoke(s);}catch(Exception e){return null;}
    }
    private static String nf(Double d){ return d==null? "—" : String.format("%.1f",d); }
}
