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

    /* ─────────── API ─────────── */

    public byte[] buildFull(User u){ return build(u,null,null); }

    public byte[] buildPeriod(User u, LocalDate from, LocalDate to){
        return build(u,from,to);
    }

    /* ───────── generador ───────── */

    /**
     * Si {@code from/to} se reciben <code>null</code>, se genera el PDF completo.
     * De lo contrario se filtran **tanto** las mediciones corporales como los
     * entrenos al intervalo indicado (fechas inclusivas).
     */
    private byte[] build(User u, LocalDate from, LocalDate to){

        /* ------------------------------------------------------------
           1) Datos filtrados (histórico + entrenos) según el rango
           ------------------------------------------------------------ */
        boolean filter = (from != null && to != null);

        List<BodyStats> hist = filter
                ? statsRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to)
                : statsRepo.findByUserIdOrderByDateAsc(u.getId());

        /* “Medidas actuales” = la última del intervalo (o global si no hay filtro) */
        BodyStats actual = hist.isEmpty() ? null : hist.get(hist.size() - 1);

        List<DailyEntry> wos = filter
                ? dailyRepo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to)
                : dailyRepo.findByUserIdOrderByDateAsc(u.getId());

        log.debug("PDF | usr={}, rango={}, hist.size={}, wos.size={}",
                u.getEmail(),
                filter ? from + " → " + to : "completo",
                hist.size(), wos.size());

        /* ------------------------------------------------------------
           2) Construcción del PDF
           ------------------------------------------------------------ */
        try (PDDocument pdf = new PDDocument()){

            PDPage page = new PDPage(PDRectangle.LETTER);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf,page);

            float x0 = 50, y  = page.getMediaBox().getHeight()-50;
            float leading = 15;

            /* título ---------------------------------------------------- */
            cs.beginText();
            cs.setFont(PDType1Font.TIMES_BOLD,20);
            cs.newLineAtOffset(x0,y);
            cs.showText("Informe de progreso – "+u.getFirstName()+" "+u.getLastName());
            cs.endText();
            y -= leading*2;

            /* bloque medidas actuales ----------------------------------- */
            if(actual!=null){
                y = drawCurrent(cs,actual,x0,y);
                y -= leading;
            }

            /* bloque históricas ----------------------------------------- */
            if(!hist.isEmpty()){
                y = drawHistoric(cs,hist,x0,y);
                y -= leading;
            }

            /* bloque entrenos ------------------------------------------- */
            drawWorkouts(cs,wos,x0,y);

            cs.close();

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                pdf.save(bos);
                log.debug("[PDF] Tamaño final = {} bytes",bos.size());
                return bos.toByteArray();
            }

        }catch(Exception ex){
            log.error("PDF | ERROR {}",ex.getMessage(),ex);
            throw new RuntimeException("PDF error",ex);
        }
    }

    /* ───────── medidas actuales ───────── */

    private float drawCurrent(PDPageContentStream cs, BodyStats s,
                              float x, float y) throws Exception{

        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.beginText(); cs.newLineAtOffset(x,y); cs.showText("Medidas actuales"); cs.endText();
        y -= 18;

        Map<String,String> rows = Map.ofEntries(
                e("Peso (kg)"        , nf(s.getWeightKg())),
                e("Estatura (cm)"    , nf(s.getUser().getHeightCm())),
                e("Cuello"           , nf(s.getNeckCm())),
                e("Pecho"            , nf(s.getChestCm())),
                e("Cintura"          , nf(s.getWaistCm())),
                e("Abd. bajo"        , nf(s.getLowerAbsCm())),
                e("Cadera"           , nf(s.getHipCm())),
                e("Muslo"            , nf(s.getThighCm())),
                e("Pantorrilla"      , nf(s.getCalfCm())),
                e("Bíceps relax"     , nf(s.getBicepsCm())),
                e("Bíceps flex"      , nf(s.getBicepsFlexCm())),
                e("Antebrazo"        , nf(s.getForearmCm()))
        );

        cs.setFont(PDType1Font.HELVETICA,11);
        for(var e:rows.entrySet()){
            cs.beginText();
            cs.newLineAtOffset(x+10,y);
            cs.showText(String.format("%-18s %s",e.getKey()+":",e.getValue()));
            cs.endText();
            y -= 14;
        }
        return y;
    }

    /* ───────── medidas históricas ───────── */

    private float drawHistoric(PDPageContentStream cs, List<BodyStats> list,
                               float x0, float y) throws Exception{

        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.beginText(); cs.newLineAtOffset(x0,y); cs.showText("Medidas históricas"); cs.endText();
        y -= 18;

        /* cabecera */
        cs.setFont(PDType1Font.HELVETICA_BOLD,10);
        float x = x0+4;
        String[] heads = {"Fecha","Peso","Cuello","Pecho","Cintura","Abd","Cadera",
                "Muslo","Pantor","Bícep R","Bícep F","Antebr","IMC"};
        int[]    wcols = {55,40,40,40,45,35,45,45,45,45,45,45,35};

        for(int i=0;i<heads.length;i++){
            cs.beginText(); cs.newLineAtOffset(x,y); cs.showText(heads[i]); cs.endText();
            x += wcols[i];
        }
        y -= 12;

        /* filas */
        cs.setFont(PDType1Font.HELVETICA,9);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        for(BodyStats s:list){
            x = x0+4;
            String[] vals = {
                    df.format(s.getDate()),
                    nf(s.getWeightKg()),
                    nf(s.getNeckCm()),
                    nf(s.getChestCm()),
                    nf(s.getWaistCm()),
                    nf(s.getLowerAbsCm()),
                    nf(s.getHipCm()),
                    nf(s.getThighCm()),
                    nf(s.getCalfCm()),
                    nf(s.getBicepsCm()),
                    nf(s.getBicepsFlexCm()),
                    nf(s.getForearmCm()),
                    bmi(s)
            };
            for(int i=0;i<vals.length;i++){
                cs.beginText(); cs.newLineAtOffset(x,y); cs.showText(vals[i]); cs.endText();
                x += wcols[i];
            }
            y -= 11;
        }
        return y;
    }

    /* ───────── entrenos ───────── */

    private void drawWorkouts(PDPageContentStream cs, List<DailyEntry> wos,
                              float x0, float y) throws Exception{

        cs.setFont(PDType1Font.HELVETICA_BOLD,14);
        cs.beginText(); cs.newLineAtOffset(x0,y); cs.showText("Histórico de entrenos"); cs.endText();
        y -= 18;

        cs.setFont(PDType1Font.HELVETICA_BOLD,11);
        cs.beginText(); cs.newLineAtOffset(x0+4,y);   cs.showText("Fecha"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x0+60,y);  cs.showText("Máquina"); cs.endText();
        cs.beginText(); cs.newLineAtOffset(x0+260,y); cs.showText("Kg / Reps x Sets"); cs.endText();
        y -= 14;

        cs.setFont(PDType1Font.HELVETICA,10);
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        LocalDate lastDate = null;

        for(DailyEntry e:wos){
            if(lastDate!=null && !lastDate.equals(e.getDate())){
                /* línea divisoria */
                cs.moveTo(x0+2,y+5);
                cs.lineTo(x0+500,y+5);
                cs.stroke();
                y -= 4;
            }
            lastDate = e.getDate();

            for(DailyEntry.Exercise ex:e.getDetails().values()){
                cs.beginText(); cs.newLineAtOffset(x0+4,y);  cs.showText(df.format(e.getDate())); cs.endText();
                cs.beginText(); cs.newLineAtOffset(x0+60,y); cs.showText(ex.getName());           cs.endText();
                String kgRepSet = ex.getWeightKg()+" kg / "+ex.getReps()+" x "+ex.getSets();
                cs.beginText(); cs.newLineAtOffset(x0+260,y);cs.showText(kgRepSet);               cs.endText();
                y -= 12;
            }
        }
    }

    /* ───────── util ───────── */

    private static Map.Entry<String,String> e(String k,String v){ return Map.entry(k,v); }

    private static String nf(Double d){ return d==null? "—" : String.format("%.1f",d); }

    private static String bmi(BodyStats s){
        Double w = s.getWeightKg(), h = s.getUser().getHeightCm();
        if(w==null||h==null) return "—";
        double bmi = w / Math.pow(h/100,2);
        return String.format("%.1f",bmi);
    }
}
