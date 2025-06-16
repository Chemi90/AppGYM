package com.example.AppGYM.service;

import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.ProgressPhoto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.DailyEntryRepository;
import com.example.AppGYM.repository.ProgressPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor
public class PdfService {

    private final BodyStatsRepository      statsRepo;
    private final DailyEntryRepository     dailyRepo;
    private final ProgressPhotoRepository  photoRepo;

    /* =================== API =================== */
    public byte[] buildFull(User u)                 { return build(u,null,null); }
    public byte[] buildPeriod(User u, LocalDate f,
                              LocalDate t)          { return build(u,f,t); }

    /* ================ GENERADOR ================ */
    private byte[] build(User u, LocalDate from, LocalDate to) {

        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(pdf, page)) {

                final float margin = 50, leading = 16;
                float y = page.getMediaBox().getHeight() - margin;

                /* ---- título ---- */
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("Informe de progreso – "
                        + u.getFirstName() + " " + u.getLastName());
                cs.endText(); y -= leading * 2;

                /* ---- período ---- */
                if (from != null && to != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Período: " + from + " → " + to);
                    cs.endText(); y -= leading;
                }

                /* secciones */
                y = drawCurrentMeasures(cs, u, y - leading);
                y = drawStatsHistory(pdf, cs, u, from, to, y - 2*leading);
                y = drawDailyHistory(pdf, cs, u, from, to, y - 2*leading);
                drawPhotos(pdf, u, from, to);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                pdf.save(bos);
                return bos.toByteArray();
            }

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    /* ---------- Medidas actuales ---------- */
    private float drawCurrentMeasures(PDPageContentStream cs,
                                      User u, float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(50,y);
        cs.showText("Medidas actuales");
        cs.endText(); y -= 18;

        cs.setFont(PDType1Font.HELVETICA,11);
        String[][] rows = {
                {"Peso (kg)", nf(u.getWeightKg())},
                {"Estatura (cm)", nf(u.getHeightCm())},
                {"Cuello", nf(u.getNeckCm())},
                {"Pecho", nf(u.getChestCm())},
                {"Cintura", nf(u.getWaistCm())},
                {"Abd. bajo", nf(u.getLowerAbsCm())},
                {"Cadera", nf(u.getHipCm())},
                {"Bíceps relax", nf(u.getBicepsCm())},
                {"Bíceps flex", nf(u.getBicepsFlexCm())},
                {"Antebrazo", nf(u.getForearmCm())},
                {"Muslo", nf(u.getThighCm())},
                {"Pantorrilla", nf(u.getCalfCm())}
        };
        for (String[] r : rows) {
            cs.beginText();
            cs.newLineAtOffset(60,y);
            cs.showText(String.format("%-18s %s", r[0]+":", r[1]));
            cs.endText(); y -= 14;
        }
        return y;
    }

    /* ---------- Histórico de medidas ---------- */
    private float drawStatsHistory(PDDocument pdf,
                                   PDPageContentStream cs,
                                   User u,
                                   LocalDate from, LocalDate to,
                                   float y) throws Exception {

        List<BodyStats> list = (from==null||to==null)
                ? statsRepo.findByUserIdOrderByDateAsc(u.getId())
                : statsRepo.findByUserIdAndDateBetweenOrderByDateAsc(
                u.getId(),from,to);

        if (list.isEmpty()) return y;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(50,y);
        cs.showText("Histórico de medidas");
        cs.endText(); y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,10);
        float[] colX = {60,120,180,240,300,360,420};
        String[] head={"Fecha","Peso","Pecho","Cintura","Cadera","Bíceps","Muslo"};
        for(int i=0;i<head.length;i++){
            cs.beginText(); cs.newLineAtOffset(colX[i],y); cs.showText(head[i]); cs.endText();
        }
        y-=12; cs.setFont(PDType1Font.HELVETICA,9);
        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;

        for(BodyStats s:list){
            String[] v={
                    df.format(s.getDate()),nf(s.getWeightKg()),nf(s.getChestCm()),
                    nf(s.getWaistCm()),nf(s.getHipCm()),
                    nf(s.getBicepsCm()),nf(s.getThighCm())};
            for(int i=0;i<v.length;i++){
                cs.beginText(); cs.newLineAtOffset(colX[i],y); cs.showText(v[i]); cs.endText();
            }
            y-=12;
            if(y<70){ newPage(pdf); cs.close(); cs=new PDPageContentStream(pdf,pdf.getPage(pdf.getNumberOfPages()-1)); y=pdf.getPage(0).getMediaBox().getHeight()-50; }
        }
        return y;
    }

    /* ---------- Histórico de entrenos ---------- */
    private float drawDailyHistory(PDDocument pdf,
                                   PDPageContentStream cs,
                                   User u, LocalDate from, LocalDate to,
                                   float y) throws Exception {

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.newLineAtOffset(50,y);
        cs.showText("Histórico de entrenos");
        cs.endText(); y-=18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,10);
        cs.beginText(); cs.newLineAtOffset(60,y);  cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(150,y); cs.showText("Máquina"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(350,y); cs.showText("Kg / Reps x Sets"); cs.endText();
        y-=12; cs.setFont(PDType1Font.HELVETICA,9);

        List<DailyEntry> list=(from==null||to==null)
                ? dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),LocalDate.MIN,LocalDate.MAX)
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to);

        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;
        for(DailyEntry e:list){
            for(Map.Entry<Long,DailyEntry.Exercise> it:e.getDetails().entrySet()){
                DailyEntry.Exercise ex=it.getValue();
                cs.beginText(); cs.newLineAtOffset(60,y);  cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(150,y); cs.showText(ex.getName());           cs.endText();
                cs.beginText(); cs.newLineAtOffset(350,y);
                cs.showText(ex.getWeightKg()+" kg / "+ex.getReps()+"x"+ex.getSets()); cs.endText();
                y-=12;
                if(y<70){ newPage(pdf); cs.close(); cs=new PDPageContentStream(pdf,pdf.getPage(pdf.getNumberOfPages()-1)); y=pdf.getPage(0).getMediaBox().getHeight()-50; }
            }
        }
        return y;
    }

    /* ---------- Fotos ---------- */
    private void drawPhotos(PDDocument pdf, User u,
                            LocalDate from, LocalDate to)throws Exception{

        List<ProgressPhoto> photos=(from==null||to==null)
                ? photoRepo.findByUserIdOrderByDateAsc(u.getId())
                : photoRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to);

        if(photos.isEmpty()) return;

        photos.sort(Comparator.comparing(ProgressPhoto::getDate));

        PDPage page=new PDPage(PDRectangle.LETTER); pdf.addPage(page);
        try(var cs=new PDPageContentStream(pdf,page)){

            cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,14);
            cs.newLineAtOffset(50,page.getMediaBox().getHeight()-50);
            cs.showText("Fotos de progreso"); cs.endText();

            float x=60,y=page.getMediaBox().getHeight()-90,maxH=0;
            DateTimeFormatter df=DateTimeFormatter.ISO_DATE;

            for(ProgressPhoto p:photos){
                try{
                    BufferedImage img=ImageIO.read(new URL(p.getUrl())); if(img==null) continue;
                    float scale=150f/img.getWidth();
                    PDImageXObject obj=LosslessFactory.createFromImage(pdf,img);
                    float h=img.getHeight()*scale;
                    cs.drawImage(obj,x,y-h,150,h);

                    cs.beginText(); cs.setFont(PDType1Font.HELVETICA,8);
                    cs.newLineAtOffset(x,y-h-10);
                    cs.showText(df.format(p.getDate())+" – "+p.getType()); cs.endText();

                    x+=170; maxH=Math.max(maxH,h+20);
                    if(x>400){ x=60; y-=maxH; maxH=0;
                        if(y<100){ cs.close(); newPage(pdf);
                            page=pdf.getPage(pdf.getNumberOfPages()-1);
                            x=60; y=page.getMediaBox().getHeight()-50;
                            cs.close(); }
                    }
                }catch(Exception ignored){}
            }
        }
    }

    /* ---------- helpers ---------- */
    private String nf(Double d){ return d==null?"—":String.format("%.1f",d); }
    private void newPage(PDDocument pdf){ pdf.addPage(new PDPage(PDRectangle.LETTER)); }
}
