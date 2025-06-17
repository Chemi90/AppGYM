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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
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

    /* ------------------------------------------------------------------ */
    public byte[] buildFull(User u)                            { return build(u,null,null); }
    public byte[] buildPeriod(User u,LocalDate f,LocalDate t)  { return build(u,f,t); }

    /* ------------------------------------------------------------------ */
    private byte[] build(User u, LocalDate f, LocalDate t) {

        List<BodyStats> hist   = statsRepo.findByUserIdOrderByDateAsc(u.getId());
        BodyStats       latest = statsRepo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);
        List<DailyEntry> wos   = (f==null||t==null)
                ? dailyRepo.findByUserIdOrderByDateAsc(u.getId())
                : dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),f,t);

        log.debug("PDF | usr={}, current={}, history={}, workouts={}",
                u.getEmail(), latest!=null, hist.size(), wos.size());

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc,page)) {

                float x = 50, y = page.getMediaBox().getHeight()-50, lh = 15;

                /* título -------------------------------------------------- */
                head(cs,x,y,"Informe de progreso – "+
                        u.getFirstName()+" "+u.getLastName(),20); y-=lh*2;

                if(latest!=null)   y = blockCurrent(cs,latest,x,y) - lh;
                if(!hist.isEmpty())y = blockHistory(cs,hist,x,y)  - lh;
                y = blockPhotos(doc,cs,latest,x,y)- lh;
                blockWorkouts(cs,wos,x,y);
            }

            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                doc.save(bos);
                return bos.toByteArray();
            }

        } catch (Exception ex){
            log.error("PDF | ERROR {}",ex.getMessage(),ex);
            throw new RuntimeException("PDF error",ex);
        }
    }

    /* ========= bloques ================================================= */

    private float blockCurrent(PDPageContentStream cs,BodyStats s,float x,float y)throws Exception{
        head(cs,x,y,"Medidas actuales",14); y-=18;

        Map<String,String> rows = Map.ofEntries(
                Map.entry("Peso (kg)"     , nf(s.getWeightKg())),
                Map.entry("Estatura (cm)" , nf(s.getUser().getHeightCm())),
                Map.entry("Cuello"        , nf(s.getNeckCm())),
                Map.entry("Pecho"         , nf(s.getChestCm())),
                Map.entry("Cintura"       , nf(s.getWaistCm())),
                Map.entry("Abd. bajo"     , nf(s.getLowerAbsCm())),
                Map.entry("Cadera"        , nf(s.getHipCm())),
                Map.entry("Bíceps relax"  , nf(s.getBicepsCm())),
                Map.entry("Bíceps flex"   , nf(s.getBicepsFlexCm())),
                Map.entry("Antebrazo"     , nf(s.getForearmCm())),
                Map.entry("Muslo"         , nf(s.getThighCm())),
                Map.entry("Pantorrilla"   , nf(s.getCalfCm()))
        );
        cs.setFont(PDType1Font.HELVETICA,11);
        for(var e:rows.entrySet()){
            line(cs,x+10,y,String.format("%-18s %s",e.getKey()+":",e.getValue())); y-=14;
        }
        return y;
    }

    private float blockHistory(PDPageContentStream cs,List<BodyStats> hist,float x,float y)throws Exception{
        head(cs,x,y,"Medidas históricas",14); y-=18;

        String[] heads={"Fecha","Peso","Cintura","Cadera","Muslo","Bíceps","IMG F/S/B"};
        float[]  w    ={ 60   , 50  , 60      , 60     , 55   , 55     , 100       };

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        float xx=x+10;
        for(int i=0;i<heads.length;i++){ line(cs,xx,y,heads[i]); xx+=w[i]; }
        cs.setFont(PDType1Font.HELVETICA,10); y-=14;

        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;
        for(BodyStats s:hist){
            String[] row={
                    df.format(s.getDate()), nf(s.getWeightKg()), nf(s.getWaistCm()),
                    nf(s.getHipCm()), nf(s.getThighCm()), nf(s.getBicepsCm()), imgLabel(s)
            };
            xx=x+10;
            for(int i=0;i<row.length;i++){ line(cs,xx,y,row[i]); xx+=w[i]; }
            y-=12;
        }
        return y;
    }

    private float blockPhotos(PDDocument doc,PDPageContentStream cs,
                              BodyStats current,float x,float y)throws Exception{

        Map<ProgressPhoto.Type,String> map = Map.of(
                ProgressPhoto.Type.FRONT, current!=null?current.getFrontImgUrl():null,
                ProgressPhoto.Type.SIDE , current!=null?current.getSideImgUrl() :null,
                ProgressPhoto.Type.BACK , current!=null?current.getBackImgUrl() :null
        );
        if(map.values().stream().noneMatch(v->v!=null&&!v.isBlank())) return y;

        head(cs,x,y,"Fotos",14); y-=18;

        float imgW=160,imgH=0,gap=15,xx=x;
        for(var e:map.entrySet()){
            String link=e.getValue();
            if(link==null||link.isBlank()){ xx+=imgW+gap; continue; }

            try(InputStream in = new URL(abs(link)).openStream()){
                BufferedImage bim = ImageIO.read(in);             // redimensiona a 800 px máx.
                if(bim==null){ log.debug("PDF | no es imagen {}",link); xx+=imgW+gap; continue; }

                int w=bim.getWidth(),h=bim.getHeight();
                if(w>800){ h = (h*800)/w; w=800; }
                ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                ImageIO.write(bim,"jpeg",tmp);

                PDImageXObject img = PDImageXObject.createFromByteArray(doc,tmp.toByteArray(),null);
                imgH = imgW*img.getHeight()/img.getWidth();
                cs.drawImage(img,xx,y-imgH,imgW,imgH);

            }catch(Exception ex){
                log.debug("PDF | foto {} omitida ({})",link,ex.getMessage());
            }
            xx+=imgW+gap;
        }
        return y-imgH-5;
    }

    private void blockWorkouts(PDPageContentStream cs,List<DailyEntry> wos,float x,float y)throws Exception{
        head(cs,x,y,"Histórico de entrenos",14); y-=18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        line(cs,x+10,y,"Fecha"); line(cs,x+70,y,"Máquina"); line(cs,x+270,y,"Kg / Reps x Sets");
        cs.setFont(PDType1Font.HELVETICA,10); y-=14;

        DateTimeFormatter df=DateTimeFormatter.ISO_DATE;
        for(DailyEntry e:wos){
            for(DailyEntry.Exercise ex:e.getDetails().values()){
                line(cs,x+10 ,y,df.format(e.getDate()));
                line(cs,x+70 ,y,ex.getName());
                line(cs,x+270,y,ex.getWeightKg()+" kg / "+ex.getReps()+" x "+ex.getSets());
                y-=12;
            }
        }
    }

    /* ========= util ==================================================== */
    private static void head(PDPageContentStream cs,float x,float y,String txt,int sz)throws IOException{
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA_BOLD,sz);
        cs.newLineAtOffset(x,y); cs.showText(txt); cs.endText();
    }
    private static void line(PDPageContentStream cs,float x,float y,String txt)throws IOException{
        cs.beginText(); cs.newLineAtOffset(x,y); cs.showText(txt); cs.endText();
    }
    private static String nf(Double d){ return d==null? "—" : String.format("%.1f",d); }
    private static String imgLabel(BodyStats s){
        return (s.getFrontImgUrl()!=null?"✔":"—")+" / "+
                (s.getSideImgUrl() !=null?"✔":"—")+" / "+
                (s.getBackImgUrl() !=null?"✔":"—");
    }
    private static String abs(String url){         // convierte '/uploads/…' -> 'https://<host>/uploads/…'
        return url.startsWith("http")?url:
                System.getenv("PUBLIC_BASE_URL")+url;  // define PUBLIC_BASE_URL en Railway → https://appgym-production-64ac.up.railway.app
    }
}
