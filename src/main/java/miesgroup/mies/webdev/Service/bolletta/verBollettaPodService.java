package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.bolletta.*;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.bolletta.*;
import miesgroup.mies.webdev.Service.cliente.ClienteService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class verBollettaPodService {

    private final BollettaPodRepo bollettaPodRepo;
    private final dettaglioCostoRepo dettaglioCostoRepo;
    private final ClienteService clienteService;
    private final PodRepo podRepo;
    private final verBollettaPodRepo verBollettaPodRepo;
    // Aggiungi queste injection nella classe verBollettaPodService

    @Inject
    CostiEnergiaService costiEnergiaService;

    @Inject
    CostiPeriodiService costiPeriodiService;

    @Inject
    CostoEnergiaRepo costoEnergiaRepo;

    @Inject
    CostiPeriodiRepo costiPeriodiRepo;



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
            System.out.println("[A2AVerifica] START verifica per BollettaPod id=" + b.getId());

            // ══════════════════════════════════════════════════════
            // 1. VARIABILI DA DB (tutte le query in un'unica volta)
            // ══════════════════════════════════════════════════════
            String idPod = b.getIdPod();
            String nomeBolletta = b.getNomeBolletta();
            String anno = b.getAnno();
            int mese = convertMeseInt(b.getMese());

            System.out.println("idPod: " + idPod);
            System.out.println("nomeBolletta: " + nomeBolletta);
            System.out.println("anno: " + anno);
            System.out.println("mese (int): " + mese);

            Double attivaTotale = Optional.ofNullable(bollettaPodRepo.getSommaAttiva(nomeBolletta)).orElse(0.0);
            System.out.println("attivaTotale: " + attivaTotale);

            Double potenzaImpegnata = Optional.ofNullable(podRepo.getPotenzaImpegnata(idPod)).orElse(0.0);
            System.out.println("potenzaImpegnata: " + potenzaImpegnata);

            String tipoTensione = bollettaPodRepo.getTipoTensione(idPod);
            System.out.println("tipoTensione: " + tipoTensione);

            Double maggiorePotenza = Optional.ofNullable(bollettaPodRepo.getMaggiorePotenza(nomeBolletta)).orElse(0.0);
            System.out.println("maggiorePotenza: " + maggiorePotenza);

            String classeAgevolazione = clienteService.getClasseAgevolazione(idPod);
            System.out.println("classeAgevolazione: " + classeAgevolazione);

            // DISPACCIAMENTO
            Double art25Bis = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 25 bis", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art25Bis: " + art25Bis);

            Double art44 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 44.3", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art44: " + art44);

            Double art44Bis = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 44 bis", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art44Bis: " + art44Bis);

            Double art46 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 46", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art46: " + art46);

            Double art48 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 48", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art48: " + art48);

            Double art73 = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 73", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art73: " + art73);

            Double art45Ann = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 45 (Annuale)", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art45Ann: " + art45Ann);

            Double art45Tri = Optional.ofNullable(
                    dettaglioCostoRepo.getCorrispettiviDispacciamentoA2A("Art. 45 (Trimestrale)", mese, anno, potenzaImpegnata, tipoTensione)
            ).orElse(0.0);
            System.out.println("art45Tri: " + art45Tri);

            Double f1Attiva = Optional.ofNullable(bollettaPodRepo.getF1Att(nomeBolletta, b.getMese())).orElse(0.0);
            Double f2Attiva = Optional.ofNullable(bollettaPodRepo.getF2Att(nomeBolletta, b.getMese())).orElse(0.0);
            Double f1Reattiva = Optional.ofNullable(bollettaPodRepo.getF1R(nomeBolletta, b.getMese())).orElse(0.0);
            Double f2Reattiva = Optional.ofNullable(bollettaPodRepo.getF2R(nomeBolletta, b.getMese())).orElse(0.0);
            Double f3RCapI = Optional.ofNullable(bollettaPodRepo.getF3RCapI(nomeBolletta, b.getMese())).orElse(0.0);

            System.out.println("f1Attiva: " + f1Attiva);
            System.out.println("f2Attiva: " + f2Attiva);
            System.out.println("f1Reattiva: " + f1Reattiva);
            System.out.println("f2Reattiva: " + f2Reattiva);
            System.out.println("f3RCapI: " + f3RCapI);

            Double costo33 = Optional.ofNullable(
                    dettaglioCostoRepo.getPenaliSotto75(mese, potenzaImpegnata, tipoTensione, anno, classeAgevolazione)
            ).orElse(0.0);
            Double costo75 = Optional.ofNullable(
                    dettaglioCostoRepo.getPenaliSopra75(mese, potenzaImpegnata, tipoTensione, anno, classeAgevolazione)
            ).orElse(0.0);
            System.out.println("costo33: " + costo33);
            System.out.println("costo75: " + costo75);

            Double piccoKwh = Optional.ofNullable(b.getPiccoKwh()).orElse(0.0);
            Double fuoriPiccoKwh = Optional.ofNullable(b.getFuoriPiccoKwh()).orElse(0.0);
            System.out.println("piccoKwh: " + piccoKwh);
            System.out.println("fuoriPiccoKwh: " + fuoriPiccoKwh);

            Double costoPicco = Optional.ofNullable(
                    dettaglioCostoRepo.getCostoPicco(mese, potenzaImpegnata, tipoTensione, anno)
            ).orElse(0.0);
            Double costoFuoriPicco = Optional.ofNullable(
                    dettaglioCostoRepo.getCostoFuoriPicco(mese, potenzaImpegnata, tipoTensione, anno)
            ).orElse(0.0);
            System.out.println("costoPicco: " + costoPicco);
            System.out.println("costoFuoriPicco: " + costoFuoriPicco);

            // TRASPORTO
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

            System.out.println("UC3: " + UC3);
            System.out.println("trasportoQuotaEne: " + trasportoQuotaEne * attivaTotale);
            System.out.println("distribuzioneQuotaEne: " + distribuzioneQuotaEne * attivaTotale);
            System.out.println("distribuzioneQuotaFissa: " + distribuzioneQuotaFissa);
            System.out.println("misuraQuotaFissa: " + misuraQuotaFissa);
            System.out.println("distribuzioneQuotaPot: " + distribuzioneQuotaPot);

            // ONERI DI SISTEMA
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

            System.out.println("quotaEnergiaOneriASOS: " + quotaEnergiaOneriASOS);
            System.out.println("quotaEnergiaOneriARIM: " + quotaEnergiaOneriARIM);
            System.out.println("quotaFissaOneriASOS: " + quotaFissaOneriASOS);
            System.out.println("quotaFissaOneriARIM: " + quotaFissaOneriARIM);
            System.out.println("quotaPotenzaOneriASOS: " + quotaPotenzaOneriASOS);
            System.out.println("quotaPotenzaOneriARIM: " + quotaPotenzaOneriARIM);

            // ══════════════════════════════════════════════════════
            // 2. CALCOLI IN MEMORIA
            // ══════════════════════════════════════════════════════

            if (attivaTotale <= 0 || potenzaImpegnata <= 0) {
                System.err.println("⚠️ Attiva totale o potenza impegnata non validi per " + nomeBolletta);
                return;
            }

            Double totAttivaPerdite = attivaTotale *
                    ("Bassa".equals(tipoTensione) ? 1.1 : "Media".equals(tipoTensione) ? 1.038 : 1.02);
            System.out.println("totAttivaPerdite: " + totAttivaPerdite);

            Double art25BisD = totAttivaPerdite * art25Bis;
            Double art44D = totAttivaPerdite * art44;
            Double art44BisD = totAttivaPerdite * art44Bis;
            Double art46D = totAttivaPerdite * art46;
            Double art48D = totAttivaPerdite * art48;
            Double art73D = totAttivaPerdite * art73;
            Double art45AnnD = totAttivaPerdite * art45Ann;
            Double art45TriD = totAttivaPerdite * art45Tri;

            System.out.println("art25BisD: " + art25BisD);
            System.out.println("art44D: " + art44D);
            System.out.println("art44BisD: " + art44BisD);
            System.out.println("art46D: " + art46D);
            System.out.println("art48D: " + art48D);
            System.out.println("art73D: " + art73D);
            System.out.println("art45AnnD: " + art45AnnD);
            System.out.println("art45TriD: " + art45TriD);

            Double verificaDispacciamento = art25BisD + art44D + art44BisD + art46D + art48D + art73D + art45AnnD + art45TriD;
            System.out.println("verificaDispacciamento: " + verificaDispacciamento);

            Double generation = b.getSpeseEne() - verificaDispacciamento;
            System.out.println("generation: " + generation);

            double costiImposte = (attivaTotale <= 200000) ? attivaTotale * 0.0125 :
                    (attivaTotale <= 1200000) ? (200000 * 0.0125) + ((attivaTotale - 200000) * 0.0075) :
                            (200000 * 0.0125) + 4820.00;
            System.out.println("costiImposte: " + costiImposte);

            Double quotaPotenzaASOS = quotaPotenzaOneriASOS * maggiorePotenza;
            Double quotaPotenzaARIM = quotaPotenzaOneriARIM * maggiorePotenza;
            Double quotaEnergiaASOS = quotaEnergiaOneriASOS * attivaTotale;
            Double quotaEnergiaARIM = quotaEnergiaOneriARIM * attivaTotale;

            Double costiOneri = quotaPotenzaASOS + quotaPotenzaARIM + quotaEnergiaASOS + quotaEnergiaARIM + quotaFissaOneriASOS + quotaFissaOneriARIM;
            System.out.println("costiOneri: " + costiOneri);

            Double UC3T = UC3 * attivaTotale;
            Double trasportoQuotaEneT = trasportoQuotaEne * attivaTotale;
            Double distribuzioneQuotaEneT = distribuzioneQuotaEne * attivaTotale;
            Double quotaVariabileT = UC3T + trasportoQuotaEneT + distribuzioneQuotaEneT;
            Double distribuzioneQuotaPotT = distribuzioneQuotaPot * maggiorePotenza;
            Double penEneRCap = f3RCapI * costo33;
            System.out.println("f3RCapI " + penEneRCap);

            Double speseTrasporto = distribuzioneQuotaPotT + quotaVariabileT + misuraQuotaFissa + distribuzioneQuotaFissa + penEneRCap;
            System.out.println("speseTrasporto: " + speseTrasporto);

            Double penale33 = 0.33, penale75 = 0.75;
            double f1Penale33 = 0.0, f1Penale75 = 0.0, f2Penale33 = 0.0, f2Penale75 = 0.0;

            if (f1Attiva > 0) {
                if (f1Reattiva > penale33 * f1Attiva) {
                    f1Penale33 = (f1Reattiva - (penale33 * f1Attiva)) * costo33;
                }
                if (f1Reattiva > penale75 * f1Attiva) {
                    f1Penale75 = (f1Reattiva - (penale75 * f1Attiva)) * costo75;
                }
            }
            System.out.println("f1Penale33: " + f1Penale33);
            System.out.println("f1Penale75: " + f1Penale75);

            if (f2Attiva > 0) {
                if (f2Reattiva > penale33 * f2Attiva) {
                    f2Penale33 = (f2Reattiva - (penale33 * f2Attiva)) * costo33;
                }
                if (f2Reattiva > penale75 * f2Attiva) {
                    f2Penale75 = (f2Reattiva - (penale75 * f2Attiva)) * costo75;
                }
            }
            System.out.println("f2Penale33: " + f2Penale33);
            System.out.println("f2Penale75: " + f2Penale75);

            Double euroPicco = piccoKwh * costoPicco;
            Double euroFuoriPicco = fuoriPiccoKwh * costoFuoriPicco;
            System.out.println("eufroPicco: " + euroPicco);
            System.out.println("euroFuoriPicco: " + euroFuoriPicco);

            // ══════════════════════════════════════════════════════
            // 3. SALVATAGGIO IN UNICA QUERY
            // ══════════════════════════════════════════════════════
            System.out.println("[A2AVerifica] start for bollettaId=" + b.getId()
                    + " nome=" + b.getNomeBolletta()
                    + " mese=" + b.getMese()
                    + " anno=" + b.getAnno());
            System.out.println("[A2AVerifica] totAtt=" + attivaTotale
                    + " potImp=" + potenzaImpegnata
                    + " totCorrispettivi=");

            verBollettaPod dto = new verBollettaPod();
            dto.setBollettaId(b);
            dto.setNomeBolletta(nomeBolletta);
            dto.setIdPod(idPod);
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
            dto.setTraspQEne(arrotonda(trasportoQuotaEneT));
            dto.setDistrQEne(arrotonda(distribuzioneQuotaEneT));
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
                    + " bollettaId=" + dto.getBollettaId().getId()
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

            // Calcola i costi dell'energia per questa bolletta
            calcolaCostiEnergia(b, idPod);

        } catch (Exception e) {
            System.err.println("❌ Errore in A2AVerifica: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void verificaBolletteDaPeriodo(String idPod, Date periodoInizio, Date periodoFine) {
        try {
            System.out.println("[verificaBolletteDaPeriodo] START verifica bollette per POD: " + idPod);
            System.out.println("[verificaBolletteDaPeriodo] Periodo: " + periodoInizio + " - " + periodoFine);

            Calendar calInizio = Calendar.getInstance();
            calInizio.setTime(periodoInizio);

            Calendar calFine = Calendar.getInstance();
            calFine.setTime(periodoFine);

            int verificateSuccesso = 0;
            int verificateFallite = 0;
            int nonTrovate = 0;

            Calendar corrente = (Calendar) calInizio.clone();

            while (corrente.compareTo(calFine) <= 0) {
                // Converti il numero del mese (0-11) nel nome italiano
                int numeroMese = corrente.get(Calendar.MONTH) + 1; // Calendar.MONTH è 0-based
                String meseItaliano = convertMeseNumeroToIta(numeroMese);
                String anno = String.valueOf(corrente.get(Calendar.YEAR));

                System.out.println("[verificaBolletteDaPeriodo] Elaboro mese: " + meseItaliano + " " + anno);

                try {
                    // Cerca bollette usando il nome del mese in italiano
                    List<BollettaPod> bolletteMese = bollettaPodRepo.findByIdPodAndMeseAndAnno(idPod, meseItaliano, anno);

                    if (bolletteMese.isEmpty()) {
                        nonTrovate++;
                        System.out.println("[verificaBolletteDaPeriodo] Nessuna bolletta trovata per POD: "
                                + idPod + " mese: " + meseItaliano + " anno: " + anno);
                    } else {
                        System.out.println("[verificaBolletteDaPeriodo] Trovate " + bolletteMese.size()
                                + " bollette per " + meseItaliano + " " + anno);

                        for (BollettaPod bolletta : bolletteMese) {
                            try {
                                System.out.println("[verificaBolletteDaPeriodo] Verifico bolletta: "
                                        + bolletta.getNomeBolletta() + " - ID: " + bolletta.getId());

                                A2AVerifica(bolletta);

                                verificateSuccesso++;
                                System.out.println("[verificaBolletteDaPeriodo] Bolletta "
                                        + bolletta.getNomeBolletta() + " verificata con successo");

                            } catch (Exception e) {
                                verificateFallite++;
                                System.err.println("[verificaBolletteDaPeriodo] Errore verifica bolletta ID: "
                                        + bolletta.getId() + " - " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[verificaBolletteDaPeriodo] Errore ricerca bollette per "
                            + meseItaliano + " " + anno + ": " + e.getMessage());
                    e.printStackTrace();
                }

                // Passa al mese successivo
                corrente.add(Calendar.MONTH, 1);
            }

            System.out.println("[verificaBolletteDaPeriodo] RIEPILOGO FINALE:");
            System.out.println("[verificaBolletteDaPeriodo] - Verificate con successo: " + verificateSuccesso);
            System.out.println("[verificaBolletteDaPeriodo] - Verifiche fallite: " + verificateFallite);
            System.out.println("[verificaBolletteDaPeriodo] - Mesi senza bollette: " + nonTrovate);
            System.out.println("[verificaBolletteDaPeriodo] END verifica bollette per POD: " + idPod);

        } catch (Exception e) {
            System.err.println("[verificaBolletteDaPeriodo] Errore generale: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String convertMeseNumeroToIta(int numeroMese) {
        switch (numeroMese) {
            case 1: return "gennaio";
            case 2: return "febbraio";
            case 3: return "marzo";
            case 4: return "aprile";
            case 5: return "maggio";
            case 6: return "giugno";
            case 7: return "luglio";
            case 8: return "agosto";
            case 9: return "settembre";
            case 10: return "ottobre";
            case 11: return "novembre";
            case 12: return "dicembre";
            default:
                throw new IllegalArgumentException("Numero mese non valido: " + numeroMese);
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

    // Aggiungi questo metodo nel verBollettaPodService.java

    /**
     * Calcola i costi dell'energia per una bolletta e salva i risultati in ver_bolletta_pod
     */
    @Transactional
    public void calcolaCostiEnergia(BollettaPod bollettaPod, String idPod) {
        try {
            // 1. Recupera il cliente dal POD
            Cliente cliente = clienteService.getClienteByPod(idPod);
            if (cliente == null) {
                System.err.println("Cliente non trovato per POD: " + idPod);
                return;
            }

            Integer clientId = cliente.getId();
            Integer annoInt = Integer.parseInt(bollettaPod.getAnno());
            Integer meseInt = convertMeseInt(bollettaPod.getMese());

            // 2. Recupera i dati kWh DIRETTAMENTE dall'oggetto BollettaPod (NON dal DB!)
            Double f0Kwh = Optional.ofNullable(bollettaPod.getF0Kwh()).orElse(0.0);
            Double f1Kwh = Optional.ofNullable(bollettaPod.getF1Kwh()).orElse(0.0);
            Double f2Kwh = Optional.ofNullable(bollettaPod.getF2Kwh()).orElse(0.0);
            Double f3Kwh = Optional.ofNullable(bollettaPod.getF3Kwh()).orElse(0.0);
            Double f1PerditeKwh = Optional.ofNullable(bollettaPod.getF1PerdKwh()).orElse(0.0);
            Double f2PerditeKwh = Optional.ofNullable(bollettaPod.getF2PerdKwh()).orElse(0.0);
            Double f3PerditeKwh = Optional.ofNullable(bollettaPod.getF3PerdKwh()).orElse(0.0);

            System.out.println("✅ Dati kWh recuperati direttamente dall'oggetto BollettaPod:");
            System.out.println("f0Kwh: " + f0Kwh);
            System.out.println("f1Kwh: " + f1Kwh + ", f1PerditeKwh: " + f1PerditeKwh);
            System.out.println("f2Kwh: " + f2Kwh + ", f2PerditeKwh: " + f2PerditeKwh);
            System.out.println("f3Kwh: " + f3Kwh + ", f3PerditeKwh: " + f3PerditeKwh);

            // 3. Continua con il resto della logica...
            Optional<CostiEnergia> costiEnergiaOpt = costoEnergiaRepo.findByClientIdAndYear(clientId, annoInt);

            if (!costiEnergiaOpt.isPresent()) {
                System.err.println("Costi energia non trovati per cliente: " + clientId + ", anno: " + annoInt);
                return;
            }

            CostiEnergia costiEnergia = costiEnergiaOpt.get();
            String tipoPrezzo = costiEnergia.getTipoPrezzo();
            String tipoTariffa = costiEnergia.getTipoTariffa();

            // 4. Calcola i costi in base al tipo di contratto
            CostoCalcolatoResult risultato = calcolaCostoByTipo(
                    tipoPrezzo, tipoTariffa, costiEnergia, meseInt, annoInt,
                    f0Kwh, f1Kwh, f2Kwh, f3Kwh, f1PerditeKwh, f2PerditeKwh, f3PerditeKwh
            );

            double totaleCostiEnergia =
                    (risultato.f0Euro != null ? risultato.f0Euro : 0.0) +
                            (risultato.f1Euro != null ? risultato.f1Euro : 0.0) +
                            (risultato.f2Euro != null ? risultato.f2Euro : 0.0) +
                            (risultato.f3Euro != null ? risultato.f3Euro : 0.0);

            System.out.println("[DEBUG] Totale costi energia calcolato: " + totaleCostiEnergia);

            // Imposto il campo speseEne di bollettaPod con il totale calcolato
            //bollettaPod.setSpeseEne(totaleCostiEnergia);

            // 5. Salva i risultati in ver_bolletta_pod
            salvaRisultatiCalcolo(bollettaPod.getNomeBolletta(), String.valueOf(meseInt),
                    bollettaPod.getAnno(), risultato,
                    f0Kwh, f1Kwh, f2Kwh, f3Kwh, f1PerditeKwh, f2PerditeKwh, f3PerditeKwh);

            // Resto del debug già presente
            System.out.println("=== DEBUG CALCOLO COSTI ===");
            System.out.println("clientId: " + clientId + ", anno: " + annoInt + ", mese: " + meseInt);
            System.out.println("f0Kwh: " + f0Kwh);
            System.out.println("f1Kwh: " + f1Kwh + ", f1PerditeKwh: " + f1PerditeKwh);
            System.out.println("f2Kwh: " + f2Kwh + ", f2PerditeKwh: " + f2PerditeKwh);
            System.out.println("f3Kwh: " + f3Kwh + ", f3PerditeKwh: " + f3PerditeKwh);
            System.out.println("CostiEnergia: " + costiEnergia);
            System.out.println("tipoPrezzo: " + tipoPrezzo + ", tipoTariffa: " + tipoTariffa);
            System.out.println("===============================");
            System.out.println("=== DEBUG COSTI ENERGIA DETTAGLIATO ===");
            System.out.println("CostiEnergia found: " + (costiEnergia != null));
            if (costiEnergia != null) {
                System.out.println("TipoPrezzo: " + costiEnergia.getTipoPrezzo());
                System.out.println("TipoTariffa: " + costiEnergia.getTipoTariffa());
                System.out.println("CostF1: " + costiEnergia.getCostF1());
                System.out.println("CostF2: " + costiEnergia.getCostF2());
                System.out.println("CostF3: " + costiEnergia.getCostF3());
                System.out.println("PercentageVariable: " + costiEnergia.getPercentageVariable());
            }
        } catch (Exception e) {
            System.err.println("Errore nel calcolo costi energia: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calcola i costi in base al tipo di contratto
     */
    private CostoCalcolatoResult calcolaCostoByTipo(String tipoPrezzo, String tipoTariffa,
                                                    CostiEnergia costiEnergia, Integer mese, Integer anno,
                                                    Double f0Kwh, Double f1Kwh, Double f2Kwh, Double f3Kwh,
                                                    Double f1PerditeKwh, Double f2PerditeKwh, Double f3PerditeKwh) {

        CostoCalcolatoResult result = new CostoCalcolatoResult();

        switch (tipoPrezzo.toLowerCase()) {
            case "fisso":
                result = calcolaCostoFisso(tipoTariffa, costiEnergia,
                        f0Kwh, f1Kwh, f2Kwh, f3Kwh, f1PerditeKwh, f2PerditeKwh, f3PerditeKwh);
                break;

            case "indicizzato":
                result = calcolaCostoIndicizzato(tipoTariffa, costiEnergia, mese, anno,
                        f0Kwh, f1Kwh, f2Kwh, f3Kwh, f1PerditeKwh, f2PerditeKwh, f3PerditeKwh);
                break;

            case "misto":
                result = calcolaCostoMisto(tipoTariffa, costiEnergia, mese, anno,
                        f0Kwh, f1Kwh, f2Kwh, f3Kwh, f1PerditeKwh, f2PerditeKwh, f3PerditeKwh);
                break;

            case "dinamico":
                result = calcolaCostoDinamico(costiEnergia.getId(), mese,
                        f0Kwh, f1Kwh, f2Kwh, f3Kwh, f1PerditeKwh, f2PerditeKwh, f3PerditeKwh);
                break;

            default:
                System.err.println("Tipo prezzo non riconosciuto: " + tipoPrezzo);
                break;
        }

        return result;
    }

    /**
     * Calcola costo per contratto FISSO
     */
    private CostoCalcolatoResult calcolaCostoFisso(String tipoTariffa, CostiEnergia costiEnergia,
                                                   Double f0Kwh, Double f1Kwh, Double f2Kwh, Double f3Kwh,
                                                   Double f1PerditeKwh, Double f2PerditeKwh, Double f3PerditeKwh) {

        CostoCalcolatoResult result = new CostoCalcolatoResult();

        switch (tipoTariffa.toLowerCase()) {
            case "monoraria":
                // Solo F0
                BigDecimal costF0 = costiEnergia.getCostF0() != null ? costiEnergia.getCostF0() : BigDecimal.ZERO;
                result.f0Euro = costF0.multiply(BigDecimal.valueOf(f0Kwh != null ? f0Kwh : 0.0)).doubleValue();
                break;

            case "bioraria":
                // F1 e F2 (F3 = F2)
                BigDecimal costF1 = costiEnergia.getCostF1() != null ? costiEnergia.getCostF1() : BigDecimal.ZERO;
                BigDecimal costF2 = costiEnergia.getCostF2() != null ? costiEnergia.getCostF2() : BigDecimal.ZERO;

                result.f1Euro = costF1.multiply(BigDecimal.valueOf(f1Kwh != null ? f1Kwh : 0.0)).doubleValue();
                result.f1PerdEuro = costF1.multiply(BigDecimal.valueOf(f1PerditeKwh != null ? f1PerditeKwh : 0.0)).doubleValue();

                result.f2Euro = costF2.multiply(BigDecimal.valueOf(f2Kwh != null ? f2Kwh : 0.0)).doubleValue();
                result.f2PerdEuro = costF2.multiply(BigDecimal.valueOf(f2PerditeKwh != null ? f2PerditeKwh : 0.0)).doubleValue();

                // F3 = F2 per bioraria
                result.f3Euro = costF2.multiply(BigDecimal.valueOf(f3Kwh != null ? f3Kwh : 0.0)).doubleValue();
                result.f3PerdEuro = costF2.multiply(BigDecimal.valueOf(f3PerditeKwh != null ? f3PerditeKwh : 0.0)).doubleValue();
                break;

            case "trioraria":
                // F1, F2, F3
                BigDecimal costF1Tri = costiEnergia.getCostF1() != null ? costiEnergia.getCostF1() : BigDecimal.ZERO;
                BigDecimal costF2Tri = costiEnergia.getCostF2() != null ? costiEnergia.getCostF2() : BigDecimal.ZERO;
                BigDecimal costF3Tri = costiEnergia.getCostF3() != null ? costiEnergia.getCostF3() : BigDecimal.ZERO;

                result.f1Euro = costF1Tri.multiply(BigDecimal.valueOf(f1Kwh != null ? f1Kwh : 0.0)).doubleValue();
                result.f1PerdEuro = costF1Tri.multiply(BigDecimal.valueOf(f1PerditeKwh != null ? f1PerditeKwh : 0.0)).doubleValue();

                result.f2Euro = costF2Tri.multiply(BigDecimal.valueOf(f2Kwh != null ? f2Kwh : 0.0)).doubleValue();
                result.f2PerdEuro = costF2Tri.multiply(BigDecimal.valueOf(f2PerditeKwh != null ? f2PerditeKwh : 0.0)).doubleValue();

                result.f3Euro = costF3Tri.multiply(BigDecimal.valueOf(f3Kwh != null ? f3Kwh : 0.0)).doubleValue();
                result.f3PerdEuro = costF3Tri.multiply(BigDecimal.valueOf(f3PerditeKwh != null ? f3PerditeKwh : 0.0)).doubleValue();
                break;
        }

        return result;
    }

    /**
     * Calcola costo per contratto INDICIZZATO (GME + spread)
     */
    private CostoCalcolatoResult calcolaCostoIndicizzato(String tipoTariffa, CostiEnergia costiEnergia, Integer mese, Integer anno,
                                                         Double f0Kwh, Double f1Kwh, Double f2Kwh, Double f3Kwh,
                                                         Double f1PerditeKwh, Double f2PerditeKwh, Double f3PerditeKwh) {

        CostoCalcolatoResult result = new CostoCalcolatoResult();

        // TODO: Implementare recupero GME quando disponibile
        Double gmeValue = getGMEValue(mese, anno); // Metodo da implementare in futuro

        BigDecimal spreadF1 = costiEnergia.getSpreadF1() != null ? costiEnergia.getSpreadF1() : BigDecimal.ZERO;
        BigDecimal spreadF2 = costiEnergia.getSpreadF2() != null ? costiEnergia.getSpreadF2() : BigDecimal.ZERO;
        BigDecimal spreadF3 = costiEnergia.getSpreadF3() != null ? costiEnergia.getSpreadF3() : BigDecimal.ZERO;

        BigDecimal gme = BigDecimal.valueOf(gmeValue);

        // Calcola: (GME + spread) * kWh
        BigDecimal prezzoF1 = gme.add(spreadF1);
        BigDecimal prezzoF2 = gme.add(spreadF2);
        BigDecimal prezzoF3 = gme.add(spreadF3);

        result.f1Euro = prezzoF1.multiply(BigDecimal.valueOf(f1Kwh != null ? f1Kwh : 0.0)).doubleValue();
        result.f1PerdEuro = prezzoF1.multiply(BigDecimal.valueOf(f1PerditeKwh != null ? f1PerditeKwh : 0.0)).doubleValue();

        result.f2Euro = prezzoF2.multiply(BigDecimal.valueOf(f2Kwh != null ? f2Kwh : 0.0)).doubleValue();
        result.f2PerdEuro = prezzoF2.multiply(BigDecimal.valueOf(f2PerditeKwh != null ? f2PerditeKwh : 0.0)).doubleValue();

        result.f3Euro = prezzoF3.multiply(BigDecimal.valueOf(f3Kwh != null ? f3Kwh : 0.0)).doubleValue();
        result.f3PerdEuro = prezzoF3.multiply(BigDecimal.valueOf(f3PerditeKwh != null ? f3PerditeKwh : 0.0)).doubleValue();

        return result;
    }

    /**
     * Calcola costo per contratto MISTO (parte fissa + parte variabile a GME)
     */
    private CostoCalcolatoResult calcolaCostoMisto(String tipoTariffa, CostiEnergia costiEnergia, Integer mese, Integer anno,
                                                   Double f0Kwh, Double f1Kwh, Double f2Kwh, Double f3Kwh,
                                                   Double f1PerditeKwh, Double f2PerditeKwh, Double f3PerditeKwh) {

        CostoCalcolatoResult result = new CostoCalcolatoResult();

        //Double percentageVariable = costiEnergia.getPercentageVariable() != null ?costiEnergia.getPercentageVariable().doubleValue() : 0.0;
        //Double percentageFixed = 100.0 - percentageVariable;

        //BigDecimal percVar = BigDecimal.valueOf(percentageVariable / 100.0);
        //BigDecimal percFixed = BigDecimal.valueOf(percentageFixed / 100.0);

        Double gmeValue = getGMEValue(mese, anno);
        BigDecimal gme = BigDecimal.valueOf(gmeValue);
        System.out.println("GME: " + gme);

        BigDecimal costF1 = costiEnergia.getCostF1() != null ? costiEnergia.getCostF1() : BigDecimal.ZERO;
        BigDecimal costF2 = costiEnergia.getCostF2() != null ? costiEnergia.getCostF2() : BigDecimal.ZERO;
        BigDecimal costF3 = costiEnergia.getCostF3() != null ? costiEnergia.getCostF3() : BigDecimal.ZERO;

        // Calcola: (costo_fisso * perc_fissa + GME * perc_variabile) * kWh

        result.f0Euro = gme.multiply(BigDecimal.valueOf(f0Kwh != null ? f0Kwh : 0.0)).doubleValue();
        System.out.println("f0Euro: " + result.f0Euro);

        result.f1Euro = costF1.multiply(BigDecimal.valueOf(f1Kwh != null ? f1Kwh : 0.0)).doubleValue();
        result.f1PerdEuro = costF1.multiply(BigDecimal.valueOf(f1PerditeKwh != null ? f1PerditeKwh : 0.0)).doubleValue();

        result.f2Euro = costF2.multiply(BigDecimal.valueOf(f2Kwh != null ? f2Kwh : 0.0)).doubleValue();
        result.f2PerdEuro = costF2.multiply(BigDecimal.valueOf(f2PerditeKwh != null ? f2PerditeKwh : 0.0)).doubleValue();

        result.f3Euro = costF3.multiply(BigDecimal.valueOf(f3Kwh != null ? f3Kwh : 0.0)).doubleValue();
        result.f3PerdEuro = costF3.multiply(BigDecimal.valueOf(f3PerditeKwh != null ? f3PerditeKwh : 0.0)).doubleValue();

        return result;
    }

    /**
     * Calcola costo per contratto DINAMICO (da costi_periodi)
     */
    private CostoCalcolatoResult calcolaCostoDinamico(Integer energyCostId, Integer mese,
                                                      Double f0Kwh, Double f1Kwh, Double f2Kwh, Double f3Kwh,
                                                      Double f1PerditeKwh, Double f2PerditeKwh, Double f3PerditeKwh) {

        CostoCalcolatoResult result = new CostoCalcolatoResult();

        // Trova il periodo attivo per il mese corrente
        Optional<CostiPeriodi> periodoOpt = costiPeriodiRepo.findActiveByEnergyIdAndMonth(energyCostId, mese);
        if (!periodoOpt.isPresent()) {
            System.err.println("Periodo dinamico non trovato per energyCostId: " + energyCostId + ", mese: " + mese);
            return result;
        }

        CostiPeriodi periodo = periodoOpt.get();

        BigDecimal costF1 = periodo.getCostF1();
        BigDecimal costF2 = periodo.getCostF2();
        BigDecimal costF3 = periodo.getCostF3();

        result.f1Euro = costF1.multiply(BigDecimal.valueOf(f1Kwh != null ? f1Kwh : 0.0)).doubleValue();
        result.f1PerdEuro = costF1.multiply(BigDecimal.valueOf(f1PerditeKwh != null ? f1PerditeKwh : 0.0)).doubleValue();

        result.f2Euro = costF2.multiply(BigDecimal.valueOf(f2Kwh != null ? f2Kwh : 0.0)).doubleValue();
        result.f2PerdEuro = costF2.multiply(BigDecimal.valueOf(f2PerditeKwh != null ? f2PerditeKwh : 0.0)).doubleValue();

        result.f3Euro = costF3.multiply(BigDecimal.valueOf(f3Kwh != null ? f3Kwh : 0.0)).doubleValue();
        result.f3PerdEuro = costF3.multiply(BigDecimal.valueOf(f3PerditeKwh != null ? f3PerditeKwh : 0.0)).doubleValue();

        return result;
    }

    /**
     * Placeholder per GME - da implementare in futuro
     */
    private Double getGMEValue(int month, int year) {
        String key = year + "-" + month;

        switch (key) {
            // 2023 Data
            case "2023-1": return 0.174;
            case "2023-2": return 0.161;
            case "2023-3": return 0.136;
            case "2023-4": return 0.135;
            case "2023-5": return 0.106;
            case "2023-6": return 0.106;
            case "2023-7": return 0.112;
            case "2023-8": return 0.112;
            case "2023-9": return 0.116;
            case "2023-10": return 0.134;
            case "2023-11": return 0.122;
            case "2023-12": return 0.116;

            // 2024 Data
            case "2024-1": return 0.1091779800;
            case "2024-2": return 0.0972745100;
            case "2024-3": return 0.1006013200;
            case "2024-4": return 0.0933715100 ;
            case "2024-5": return 0.1069370800;
            case "2024-6": return 0.1164437100;
            case "2024-7": return 0.1216231300;
            case "2024-8": return 0.1384999400;
            case "2024-9": return 0.1277687100;
            case "2024-10": return 0.1198823100;
            case "2024-11": return 0.1338299500;
            case "2024-12": return 0.1572625200;

            // 2025 Data
            case "2025-1": return 0.143;
            case "2025-2": return 0.150;
            case "2025-3": return 0.121;
            case "2025-4": return 0.100;
            case "2025-5": return 0.094;
            case "2025-6": return 0.112;
            case "2025-7": return 0.113;
            case "2025-8": return 0.109;

            // Default fallback per mesi/anni non disponibili
            default: return 0.08; // Valore di default
        }
    }


    /**
     * Salva i risultati del calcolo in ver_bolletta_pod
     */
    @Transactional
    public void salvaRisultatiCalcolo(String nomeBolletta, String mese, String anno,
                                       CostoCalcolatoResult risultato,
                                       Double f0Kwh, Double f1Kwh, Double f2Kwh, Double f3Kwh,
                                       Double f1PerditeKwh, Double f2PerditeKwh, Double f3PerditeKwh) {

        // Trova o crea il record in ver_bolletta_pod
        Optional<verBollettaPod> existingOpt = verBollettaPodRepo.findByNomeBolletta(nomeBolletta);

        verBollettaPod record;
        if (existingOpt.isPresent()) {
            record = existingOpt.get();
        } else {
            // Crea nuovo record se non esiste
            record = new verBollettaPod();
            record.setNomeBolletta(nomeBolletta);

            // Trova il bolletta_id dalla tabella bolletta_pod
            Optional<BollettaPod> bollettaOpt = bollettaPodRepo.findOne(nomeBolletta, mese, anno);
            if (bollettaOpt.isPresent()) {
                record.setBollettaId(bollettaOpt.get());
            }
        }

        // Imposta i valori calcolati
        record.setF0Euro(risultato.f0Euro);
        record.setF0Kwh(f0Kwh);
        record.setF1Euro(risultato.f1Euro);
        record.setF1Kwh(f1Kwh);
        record.setF1PerdEuro(risultato.f1PerdEuro);
        record.setF1PerdKwh(f1PerditeKwh);
        record.setF2Euro(risultato.f2Euro);
        record.setF2Kwh(f2Kwh);
        record.setF2PerdEuro(risultato.f2PerdEuro);
        record.setF2PerdKwh(f2PerditeKwh);
        record.setF3Euro(risultato.f3Euro);
        record.setF3Kwh(f3Kwh);
        record.setF3PerdEuro(risultato.f3PerdEuro);
        record.setF3PerdKwh(f3PerditeKwh);

        // Salva il record
        verBollettaPodRepo.persist(record);

        System.out.println("Costi energia salvati per bolletta: " + nomeBolletta);
    }

    /**
     * Classe di supporto per contenere i risultati del calcolo
     */
    private static class CostoCalcolatoResult {
        public Double f0Euro = 0.0;
        public Double f1Euro = 0.0;
        public Double f1PerdEuro = 0.0;
        public Double f2Euro = 0.0;
        public Double f2PerdEuro = 0.0;
        public Double f3Euro = 0.0;
        public Double f3PerdEuro = 0.0;
    }

}