package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Model.bolletta.verBollettaPod;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.bolletta.PodRepo;
import miesgroup.mies.webdev.Repository.bolletta.dettaglioCostoRepo;
import miesgroup.mies.webdev.Repository.bolletta.verBollettaPodRepo;
import miesgroup.mies.webdev.Service.cliente.ClienteService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class verBollettaPodService {

    private final BollettaPodRepo bollettaPodRepo;
    private final dettaglioCostoRepo dettaglioCostoRepo;
    private final ClienteService clienteService;
    private final PodRepo podRepo;
    private final verBollettaPodRepo verBollettaPodRepo;


    public verBollettaPodService(BollettaPodRepo bollettaRepo, ClienteService clienteService, dettaglioCostoRepo dettaglioCostoRepo, PodRepo podRepo, verBollettaPodRepo verBollettaPodRepo) {
        this.bollettaPodRepo = bollettaRepo;
        this.clienteService = clienteService;
        this.dettaglioCostoRepo = dettaglioCostoRepo;
        this.podRepo = podRepo;
        this.verBollettaPodRepo = verBollettaPodRepo;
    }

    @Transactional
    public void A2AVerifica(BollettaPod b) {
        try {
            // ══════════════════════════════════════════════════════
            // 1. VARIABILI DA DB (tutte le query in un'unica volta)
            // ══════════════════════════════════════════════════════
            String idPod = b.getIdPod();
            String nomeBolletta = b.getNomeBolletta();
            String anno = b.getAnno();
            int mese = convertMeseInt(b.getMese());

            Double attivaTotale = Optional.ofNullable(bollettaPodRepo.getSommaAttiva(nomeBolletta)).orElse(0.0);
            Double potenzaImpegnata = Optional.ofNullable(podRepo.getPotenzaImpegnata(idPod)).orElse(0.0);
            String tipoTensione = bollettaPodRepo.getTipoTensione(idPod);
            Double maggiorePotenza = Optional.ofNullable(bollettaPodRepo.getMaggiorePotenza(nomeBolletta)).orElse(0.0);
            String classeAgevolazione = clienteService.getClasseAgevolazione(idPod);

            //DISPACCIAMENTO
            Double art25Bis = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 25 bis", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            Double art44 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 44.3", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            Double art44Bis = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 44 bis", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            Double art46 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 46", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            Double art48 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 48", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            Double art73 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 73", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            Double art45Ann = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 45 (Annuale)", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            Double art45Tri = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 45 (Trimestrale)", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);

            Double f1Attiva = Optional.ofNullable(bollettaPodRepo.getF1Att(nomeBolletta, b.getMese())).orElse(0.0);
            Double f2Attiva = Optional.ofNullable(bollettaPodRepo.getF2Att(nomeBolletta, b.getMese())).orElse(0.0);
            Double f1Reattiva = Optional.ofNullable(bollettaPodRepo.getF1R(nomeBolletta, b.getMese())).orElse(0.0);
            Double f2Reattiva = Optional.ofNullable(bollettaPodRepo.getF2R(nomeBolletta, b.getMese())).orElse(0.0);
            Double f3RCapI = Optional.ofNullable(bollettaPodRepo.getF3RCapI(nomeBolletta, b.getMese())).orElse(0.0);

            Double costo33 = Optional.ofNullable(
                    dettaglioCostoRepo.getPenaliSotto75(mese, potenzaImpegnata, tipoTensione, anno, classeAgevolazione)
            ).orElse(0.0);
            Double costo75 = Optional.ofNullable(
                    dettaglioCostoRepo.getPenaliSopra75(mese, potenzaImpegnata, tipoTensione, anno, classeAgevolazione)
            ).orElse(0.0);

            Double piccoKwh = Optional.ofNullable(b.getPiccoKwh()).orElse(0.0);
            Double fuoriPiccoKwh = Optional.ofNullable(b.getFuoriPiccoKwh()).orElse(0.0);
            Double costoPicco = Optional.ofNullable(
                    dettaglioCostoRepo.getCostoPicco(mese, potenzaImpegnata, tipoTensione, anno)
            ).orElse(0.0);
            Double costoFuoriPicco = Optional.ofNullable(
                    dettaglioCostoRepo.getCostoFuoriPicco(mese, potenzaImpegnata, tipoTensione, anno)
            ).orElse(0.0);

            //TRASPORTO
            Double UC3 = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiTrasporto("UC3/UC6", mese, potenzaImpegnata, tipoTensione, "€/KWh", anno)
            ).orElse(0.0);
            Double trasportoQuotaEne = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiTrasporto("Trasporto Quota Energia", mese, potenzaImpegnata, tipoTensione, "€/KWh", anno)
            ).orElse(0.0);
            Double distribuzioneQuotaEne = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiTrasporto("Distribuzione Quota Energia", mese, potenzaImpegnata, tipoTensione, "€/KWh", anno)
            ).orElse(0.0);

            Double distribuzioneQuotaFissa = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiTrasporto("Distribuzione Quota Fissa", mese, potenzaImpegnata, tipoTensione, "€/Mese", anno)
            ).orElse(0.0);

            Double misuraQuotaFissa = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiTrasporto("Misura Quota Fissa", mese, potenzaImpegnata, tipoTensione, "€/Mese", anno)
            ).orElse(0.0);

            Double distribuzioneQuotaPot = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiTrasporto("Distribuzione Quota Potenza", mese, potenzaImpegnata, tipoTensione, "€/(kWxMese)", anno)
            ).orElse(0.0);

            //ONERI DI SISTEMA
            Double quotaEnergiaOneriASOS = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiOneri("ASOS Energia", mese, potenzaImpegnata, tipoTensione, "€/KWh", classeAgevolazione, anno)
            ).orElse(0.0);
            Double quotaEnergiaOneriARIM = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiOneri("ARIM Energia", mese, potenzaImpegnata, tipoTensione, "€/KWh", classeAgevolazione, anno)
            ).orElse(0.0);
            Double quotaFissaOneriASOS = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiOneri("ASOS Fissa", mese, potenzaImpegnata, tipoTensione, "€/Mese", classeAgevolazione, anno)
            ).orElse(0.0);
            Double quotaFissaOneriARIM = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiOneri("ARIM Fissa", mese, potenzaImpegnata, tipoTensione, "€/Mese", classeAgevolazione, anno)
            ).orElse(0.0);
            Double quotaPotenzaOneriASOS = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiOneri("ASOS Potenza", mese, potenzaImpegnata, tipoTensione, "€/(kWxMese)", classeAgevolazione, anno)
            ).orElse(0.0);
            Double quotaPotenzaOneriARIM = Optional.ofNullable(
                    dettaglioCostoRepo.getCostiOneri("ARIM Potenza", mese, potenzaImpegnata, tipoTensione, "€/(kWxMese)", classeAgevolazione, anno)
            ).orElse(0.0);

            // ══════════════════════════════════════════════════════
            // 2. CALCOLI IN MEMORIA
            // ══════════════════════════════════════════════════════
            if (attivaTotale <= 0 || potenzaImpegnata <= 0) {
                System.err.println("⚠️ Attiva totale o potenza impegnata non validi per " + nomeBolletta);
                return;
            }

            Double totAttivaPerdite = attivaTotale *
                    ("Bassa".equals(tipoTensione) ? 1.1 : "Media".equals(tipoTensione) ? 1.038 : 1.02);

            //DISPACCIAMENTO
            Double art25BisD = totAttivaPerdite * art25Bis;
            Double art44D = totAttivaPerdite * art44;
            Double art44BisD = totAttivaPerdite * art44Bis;
            Double art46D = totAttivaPerdite * art46;
            Double art48D = totAttivaPerdite * art48;
            Double art73D = totAttivaPerdite * art73;
            Double art45AnnD = totAttivaPerdite * art45Ann;
            Double art45TriD = totAttivaPerdite * art45Tri;

            Double verificaDispacciamento = art25BisD + art44D + art44BisD + art46D + art48D + art73D + art45AnnD + art45TriD;

            //GENERATION
            Double generation = b.getSpeseEne() - verificaDispacciamento;


            //CALCOLO IMPOSTE
            double costiImposte = (attivaTotale <= 200000) ? attivaTotale * 0.0125 : (attivaTotale <= 1200000)
                    ? (200000 * 0.0125) + ((attivaTotale - 200000) * 0.0075) : (200000 * 0.0125) + 4820.00;


            //ONERI DI SISTEMA
            Double quotaPotenzaASOS = quotaPotenzaOneriASOS * maggiorePotenza;
            Double quotaPotenzaARIM = quotaPotenzaOneriARIM * maggiorePotenza;

            Double quotaEnergiaASOS = quotaEnergiaOneriASOS * attivaTotale;
            Double quotaEnergiaARIM = quotaEnergiaOneriARIM * attivaTotale;

            Double costiOneri = quotaPotenzaASOS + quotaPotenzaARIM + quotaEnergiaASOS + quotaEnergiaARIM + quotaFissaOneriASOS + quotaFissaOneriARIM;



            //TRASPORTO
            Double UC3T = UC3 * attivaTotale;
            Double trasportoQuotaEneT = trasportoQuotaEne * attivaTotale;
            Double distribuzioneQuotaEneT = distribuzioneQuotaEne * attivaTotale;
            Double quotaVariabileT = UC3T + trasportoQuotaEneT + distribuzioneQuotaEneT;

            Double distribuzioneQuotaPotT = distribuzioneQuotaPot * maggiorePotenza;

            Double penEneRCap = f3RCapI * costo33;

            Double speseTrasporto = distribuzioneQuotaPotT + quotaVariabileT + misuraQuotaFissa + distribuzioneQuotaFissa + penEneRCap;

            //PENALI
            Double penale33 = 0.33, penale75 = 0.75;

            double f1Penale33 = 0.0, f1Penale75 = 0.0, f2Penale33 = 0.0, f2Penale75 = 0.0;
            if (f1Attiva > 0) {
                if (f1Reattiva > penale33 * f1Attiva) f1Penale33 = (f1Reattiva - (penale33 * f1Attiva)) * costo33;
                if (f1Reattiva > penale75 * f1Attiva) f1Penale75 = (f1Reattiva - (penale75 * f1Attiva)) * costo75;
            }
            if (f2Attiva > 0) {
                if (f2Reattiva > penale33 * f2Attiva) f2Penale33 = (f2Reattiva - (penale33 * f2Attiva)) * costo33;
                if (f2Reattiva > penale75 * f2Attiva) f2Penale75 = (f2Reattiva - (penale75 * f2Attiva)) * costo75;
            }

            Double euroPicco = piccoKwh * costoPicco;
            Double euroFuoriPicco = fuoriPiccoKwh * costoFuoriPicco;

            // ══════════════════════════════════════════════════════
            // 3. SALVATAGGIO IN UNICA QUERY
            // ══════════════════════════════════════════════════════
            System.out.println("[A2AVerifica] start for bollettaId=" + b.getId()
                    + " nome=" + b.getNomeBolletta()
                    + " mese=" + b.getMese()
                    + " anno=" + b.getAnno());

            System.out.println("[A2AVerifica] totAtt=" + attivaTotale
                    + " potImp=" + potenzaImpegnata
                    + " totCorrispettivi=" );

            verBollettaPod dto = new verBollettaPod();
            dto.setBollettaId(b);
            dto.setNomeBolletta(nomeBolletta);
            dto.setTotAtt(arrotonda(attivaTotale));
            dto.setTotAttPerd(arrotonda(totAttivaPerdite));

            // DISPACCIAMENTO
            dto.setDispacciamento(arrotonda(verificaDispacciamento));
            dto.setArt25bis(arrotonda(art25BisD));
            dto.setArt44_3(arrotonda(art44D));
            dto.setArt44bis(arrotonda(art44BisD));
            dto.setArt46(arrotonda(art46D));
            dto.setArt48(arrotonda(art48D));
            dto.setArt73(arrotonda(art73D));
            dto.setArt45Ann(arrotonda(art45AnnD));
            dto.setArt45Tri(arrotonda(art45TriD));

            // GENERATION
            dto.setGeneration(arrotonda(generation));

            // TRASPORTO
            dto.setQVarTrasp(arrotonda(quotaVariabileT));
            dto.setQFixTrasp(arrotonda(misuraQuotaFissa + distribuzioneQuotaFissa));
            dto.setQPotTrasp(arrotonda(distribuzioneQuotaPotT));

            dto.setUc3Uc6(arrotonda(UC3T));
            dto.setTraspQEne(arrotonda(trasportoQuotaEne));
            dto.setDistrQEne(arrotonda(distribuzioneQuotaEne));

            dto.setDistrQPot(arrotonda(distribuzioneQuotaPotT));
            dto.setMisQFix(arrotonda(misuraQuotaFissa));
            dto.setDistrQFix(arrotonda(distribuzioneQuotaFissa));

            dto.setPenRCapI(arrotonda(penEneRCap));

            // TRASPORTO TOTALE
            dto.setSpeseTrasp(arrotonda(speseTrasporto));

            // IMPOSTE
            dto.setImposte(arrotonda(costiImposte));

            // ONERI DI SISTEMA
            dto.setQEnOnASOS(arrotonda(quotaEnergiaASOS));
            dto.setQEnOnARIM(arrotonda(quotaEnergiaARIM));
            dto.setQFixOnASOS(arrotonda(quotaFissaOneriASOS));
            dto.setQFixOnARIM(arrotonda(quotaFissaOneriARIM));
            dto.setQPotOnASOS(arrotonda(quotaPotenzaASOS));
            dto.setQPotOnARIM(arrotonda(quotaPotenzaARIM));

            // ONERI TOTALI
            dto.setOneri(arrotonda(costiOneri));

            // PENALI
            dto.setF1Pen33(arrotonda(f1Penale33));
            dto.setF1Pen75(arrotonda(f1Penale75));
            dto.setF2Pen33(arrotonda(f2Penale33));
            dto.setF2Pen75(arrotonda(f2Penale75));

            // PICCO / FUORI PICCO
            dto.setEuroPicco(arrotonda(euroPicco));
            dto.setEuroFuoriPicco(arrotonda(euroFuoriPicco));


            System.out.println("[A2AVerifica] DTO ver_bolletta_pod payload: "
                    + "bollettaId=" + dto.getBollettaId().getId()
                    + ", nomeBolletta=" + dto.getNomeBolletta()
                    + ", TOT_Att=" + dto.getTotAtt()
                    + ", TOT_Att_Perd=" + dto.getTotAttPerd()
                    + ", Dispacciamento=" + dto.getDispacciamento()
                    + ", Generation=" + dto.getGeneration()
                    + ", Spese_Trasp=" + dto.getSpeseTrasp()
                    + ", Oneri=" + dto.getOneri()
                    + ", Imposte=" + dto.getImposte()
                    + ", Euro_Picco=" + dto.getEuroPicco()
                    + ", Euro_FuoriPicco=" + dto.getEuroFuoriPicco());

            verBollettaPod saved = verBollettaPodRepo.upsertAllByBollettaId(dto);
            System.out.println("[A2AVerifica] upsert DONE. Saved id=" + saved.getId());


        } catch (Exception e) {
            System.err.println("❌ Errore in A2AVerifica: " + e.getMessage());
        }
    }


    public int convertMeseInt(String mese){
        int m = switch (mese.toLowerCase()) {
            case "gennaio" -> 1;
            case "febbraio" -> 2;
            case "marzo" -> 3;
            case "aprile" -> 4;
            case "maggio" -> 5;
            case "giugno" -> 6;
            case "luglio" -> 7;
            case "agosto" -> 8;
            case "settembre" -> 9;
            case "ottobre" -> 10;
            case "novembre" -> 11;
            case "dicembre" -> 12;
            default -> throw new IllegalArgumentException("Mese non valido: " + mese);
        };
        return m;
    }
    public static double arrotonda(double valore) {
        return BigDecimal.valueOf(valore).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Transactional
    public boolean A2AisPresent(String nomeBolletta, String idPod) {
        return bollettaPodRepo.A2AisPresent(nomeBolletta, idPod);
    }

    public List<BollettaPod> findBollettaPodByPods(List<Pod> pods) {
        return bollettaPodRepo.findBollettaPodByPods(pods);
    }
}