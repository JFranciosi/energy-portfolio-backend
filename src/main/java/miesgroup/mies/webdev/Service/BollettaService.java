package miesgroup.mies.webdev.Service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.BollettaPod;
import miesgroup.mies.webdev.Model.Cliente;
import miesgroup.mies.webdev.Model.Costi;
import miesgroup.mies.webdev.Model.Pod;
import miesgroup.mies.webdev.Repository.BollettaRepo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class BollettaService {

    private final BollettaRepo bollettaRepo;
    private final ClienteService clienteService;
    private final CostoEnergiaService costoEnergiaService;
    private final CostoArticoloService costoArticoloService;
    private final CostiService costiService;

    public BollettaService(BollettaRepo bollettaRepo,
                           ClienteService clienteService,
                           CostoEnergiaService costoEnergiaService,
                           CostoArticoloService costoArticoloService,
                           CostiService costiService) {
        this.bollettaRepo = bollettaRepo;
        this.clienteService = clienteService;
        this.costoEnergiaService = costoEnergiaService;
        this.costoArticoloService = costoArticoloService;
        this.costiService = costiService;
    }

    @Transactional
    public void A2AVerifica(BollettaPod b) {
        try {
            // 1. Consumo totale attivo
            double totAttiva = Optional.ofNullable(bollettaRepo.getConsumoA2A(b.getNomeBolletta(), b.getMese()))
                    .orElse(0.0);
            if (totAttiva <= 0) {
                System.err.println("Errore: TOT_Attiva non valido per " + b.getNomeBolletta());
                return;
            }

            // 2. Potenza impegnata
            double potenzaImpegnata = Optional.ofNullable(bollettaRepo.getPotenzaImpegnata(b.getIdPod()))
                    .orElse(0.0);
            if (potenzaImpegnata <= 0) {
                System.err.println("Errore: Potenza Impegnata non valida per " + b.getNomeBolletta());
                return;
            }

            // 3. TOT Attiva Perdite
            Double totAttivaPerdite = (potenzaImpegnata < 100) ? totAttiva :
                    (potenzaImpegnata <= 500) ? totAttiva * 1.1 : totAttiva * 1.038;
            bollettaRepo.updateTOTAttivaPerdite(totAttivaPerdite, b.getNomeBolletta(), b.getMese());

            // 4. Trimestre
            int trimestre = switch (b.getMese().toLowerCase()) {
                case "gennaio", "febbraio", "marzo" -> 1;
                case "aprile", "maggio", "giugno" -> 2;
                case "luglio", "agosto", "settembre" -> 3;
                default -> 4;
            };

            // 5. Corrispettivi dispacciamento
            Double totCorrispettivi = Optional.ofNullable(
                    bollettaRepo.getCorrispettiviDispacciamentoA2A(trimestre, b.getAnno())
            ).orElse(0.0);
            String tipoTensione = bollettaRepo.getTipoTensione(b.getIdPod());
            Double verificaDispacciamento = totAttiva * totCorrispettivi;
            verificaDispacciamento *= switch (tipoTensione) {
                case "Bassa" -> 1.1;
                case "Media" -> 1.038;
                default -> 1.02;
            };
            bollettaRepo.updateVerificaDispacciamentoA2A(arrotonda(verificaDispacciamento), b.getNomeBolletta(), b.getMese());

            // 6. Generation
            Double generation = arrotonda(b.getSpeseEnergia() - verificaDispacciamento);
            bollettaRepo.updateGenerationA2A(generation, b.getNomeBolletta(), b.getMese());

            // 7. Trasporti
            Double maggiorePotenza = Optional.ofNullable(bollettaRepo.getMaggiorePotenza(b.getNomeBolletta())).orElse(0.0);
            String rangePotenza = (potenzaImpegnata <= 100) ? "<100KW" :
                    (potenzaImpegnata <= 500) ? "100-500KW" : ">500KW";
            Double quotaVariabileT = Optional.ofNullable(
                    bollettaRepo.getCostiTrasporto(trimestre, rangePotenza, "€/KWh", b.getAnno())
            ).orElse(0.0);
            Double quotaFissaT = Optional.ofNullable(
                    bollettaRepo.getCostiTrasporto(trimestre, rangePotenza, "€/Month", b.getAnno())
            ).orElse(0.0);
            Double quotaPotenzaT = Optional.ofNullable(
                    bollettaRepo.getCostiTrasporto(trimestre, rangePotenza, "€/KW/Month", b.getAnno())
            ).orElse(0.0) * maggiorePotenza;
            bollettaRepo.updateQuoteTrasporto(quotaVariabileT, quotaFissaT, quotaPotenzaT, b.getNomeBolletta(), b.getMese());

            double costiTrasporti = arrotonda((quotaVariabileT * totAttiva) + quotaFissaT + quotaPotenzaT);
            bollettaRepo.updateVerificaTrasportiA2A(costiTrasporti, b.getNomeBolletta(), b.getMese());

            // 8. Imposte
            double costiImposte = (totAttiva <= 200000) ? totAttiva * 0.0125 :
                    (totAttiva <= 1200000) ? (200000 * 0.0125) + ((totAttiva - 200000) * 0.0075) :
                            (200000 * 0.0125) + (1000000 * 0.0075);
            bollettaRepo.updateVerificaImposte(arrotonda(costiImposte), b.getNomeBolletta(), b.getMese());

            // 9. Oneri
            String classeAgevolazione = clienteService.getClasseAgevolazione(b.getIdPod());
            Double quotaEnergiaOneri = Optional.ofNullable(
                    bollettaRepo.getCostiOneri(trimestre, rangePotenza, "€/KWh", classeAgevolazione, b.getAnno())
            ).orElse(0.0);
            Double quotaFissaOneri = Optional.ofNullable(
                    bollettaRepo.getCostiOneri(trimestre, rangePotenza, "€/Month", classeAgevolazione, b.getAnno())
            ).orElse(0.0);
            Double quotaPotenzaOneri = Optional.ofNullable(
                    bollettaRepo.getCostiOneri(trimestre, rangePotenza, "€/KW/Month", classeAgevolazione, b.getAnno())
            ).orElse(0.0) * maggiorePotenza;
            bollettaRepo.updateQuoteOneri(quotaEnergiaOneri, quotaFissaOneri, quotaPotenzaOneri, b.getNomeBolletta(), b.getMese());

            Double costiOneri = arrotonda((quotaEnergiaOneri * totAttiva) + quotaFissaOneri + quotaPotenzaOneri);
            bollettaRepo.updateVerificaOneri(costiOneri, b.getNomeBolletta(), b.getMese());

            // 10. Penali
            Double f1Attiva = Optional.ofNullable(bollettaRepo.getF1(b.getNomeBolletta(), b.getMese())).orElse(0.0);
            Double f2Attiva = Optional.ofNullable(bollettaRepo.getF2(b.getNomeBolletta(), b.getMese())).orElse(0.0);
            Double f1Reattiva = Optional.ofNullable(bollettaRepo.getF1R(b.getNomeBolletta(), b.getMese())).orElse(0.0);
            Double f2Reattiva = Optional.ofNullable(bollettaRepo.getF2R(b.getNomeBolletta(), b.getMese())).orElse(0.0);

            Double sommaAttiva = f1Attiva + f2Attiva;
            Double sommaReattiva = f1Reattiva + f2Reattiva;
            Double percentualeDelleAR = (sommaAttiva > 0) ? (sommaReattiva / sommaAttiva) * 100 : 0.0;

            Double penali33 = 0.0;
            Double penali75 = 0.0;
            if (percentualeDelleAR >= 33) {
                Double costo33 = Optional.ofNullable(bollettaRepo.getPenaliSotto75(b.getAnno())).orElse(0.0);
                Double costo75 = Optional.ofNullable(bollettaRepo.getPenaliSopra75(b.getAnno())).orElse(0.0);
                if (percentualeDelleAR < 75) {
                    penali33 = (costo33 * sommaAttiva) * ((percentualeDelleAR - 33) / 100);
                } else {
                    penali33 = (costo33 * sommaAttiva) * ((percentualeDelleAR - 33) / 100);
                    penali75 = (costo75 * sommaAttiva) * ((percentualeDelleAR - 75) / 100);
                }
            }
            bollettaRepo.updatePenali33(arrotonda(penali33), b.getNomeBolletta(), b.getMese());
            bollettaRepo.updatePenali75(arrotonda(penali75), b.getNomeBolletta(), b.getMese());

            // 11. Verifica Picco e Fuori Picco
            Double piccoKwh = b.getPiccoKwh();
            Double fuoriPiccoKwh = b.getFuoriPiccoKwh();
            Double costoPicco = Optional.ofNullable(bollettaRepo.getCostoPicco(trimestre, b.getAnno(), rangePotenza)).orElse(0.0);
            Double costoFuoriPicco = Optional.ofNullable(bollettaRepo.getCostoFuoriPicco(trimestre, b.getAnno(), rangePotenza)).orElse(0.0);
            bollettaRepo.updateVerificaPicco(arrotonda(piccoKwh * costoPicco), b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateVerificaFuoriPicco(arrotonda(fuoriPiccoKwh * costoFuoriPicco), b.getNomeBolletta(), b.getMese());

            // 12. Materia Energia per il cliente (€)
            Cliente cliente = clienteService.getClienteByPod(b.getIdPod());
            Double f0Costo = costoEnergiaService.verificaMateriaEnergia(cliente, "f0");
            Double f1Costo = costoEnergiaService.verificaMateriaEnergia(cliente, "f1");
            Double f2Costo = costoEnergiaService.verificaMateriaEnergia(cliente, "f2");
            Double f3Costo = costoEnergiaService.verificaMateriaEnergia(cliente, "f3");
            Double f1PerditeCosto = costoEnergiaService.verificaMateriaEnergia(cliente, "f1_perdite");
            Double f2PerditeCosto = costoEnergiaService.verificaMateriaEnergia(cliente, "f2_perdite");
            Double f3PerditeCosto = costoEnergiaService.verificaMateriaEnergia(cliente, "f3_perdite");

            Double f0kwh = Optional.ofNullable(b.getF0Kwh()).orElse(0.0);
            Double f1kwh = Optional.ofNullable(b.getF1Kwh()).orElse(0.0);
            Double f2kwh = Optional.ofNullable(b.getF2Kwh()).orElse(0.0);
            Double f3kwh = Optional.ofNullable(b.getF3Kwh()).orElse(0.0);
            Double f1PerditeKwh = Optional.ofNullable(b.getF1PerditeKwh()).orElse(0.0);
            Double f2PerditeKwh = Optional.ofNullable(b.getF2PerditeKwh()).orElse(0.0);
            Double f3PerditeKwh = Optional.ofNullable(b.getF3PerditeKwh()).orElse(0.0);

            Double f0Tot = f0Costo * f0kwh;
            Double f1Tot = f1Costo * f1kwh;
            Double f2Tot = f2Costo * f2kwh;
            Double f3Tot = f3Costo * f3kwh;

            Double f1PerditeTot = f1PerditeCosto * f1PerditeKwh;
            Double f2PerditeTot = f2PerditeCosto * f2PerditeKwh;
            Double f3PerditeTot = f3PerditeCosto * f3PerditeKwh;

            // Calcolo della spesa totale per la materia energia
            Double spesaMateriaEnergia = arrotonda(f0Tot + f1Tot + f2Tot + f3Tot + f1PerditeTot + f2PerditeTot + f3PerditeTot);

            // ──────────────────────────────────────────────
            // 13. Aggiornamento dei costi nel database (campo per ogni voce)
            // ──────────────────────────────────────────────
            bollettaRepo.updateCostoF0(f0Tot, b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateCostoF1(f1Tot, b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateCostoF2(f2Tot, b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateCostoF3(f3Tot, b.getNomeBolletta(), b.getMese());

            bollettaRepo.updateCostoF1Perdite(f1PerditeTot, b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateCostoF2Perdite(f2PerditeTot, b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateCostoF3Perdite(f3PerditeTot, b.getNomeBolletta(), b.getMese());

            bollettaRepo.updateVerificaMateriaEnergia(spesaMateriaEnergia, b.getNomeBolletta(), b.getMese());

            // 14. Costi Articoli
            List<Costi> articoliTrasporti = costiService.getArticoli(b.getAnno(), b.getMese(), "trasporti", rangePotenza);
            costoArticoloService.calcolaCostiArticoli(articoliTrasporti, b, maggiorePotenza, "trasporti");

            // Calcolo del costo per ogni articolo del dispacciamento
            List<Costi> articoliDispacciamento = costiService.getArticoliDispacciamento(b.getAnno(), b.getMese(), "dispacciamento");
            costoArticoloService.calcolaCostiArticoli(articoliDispacciamento, b, maggiorePotenza, "dispacciamento");

            // Calcolo del costo per ogni articolo degli oneri
            List<Costi> articoliOneri = costiService.getArticoli(b.getAnno(), b.getMese(), "oneri", rangePotenza);
            costoArticoloService.calcolaCostiArticoli(articoliOneri, b, maggiorePotenza, "oneri");

        } catch (Exception e) {
            System.err.println("Errore: " + e.getMessage());
        }
    }


    /**
     * Arrotonda un valore a 8 decimali
     */
    public static double arrotonda(double valore) {
        return BigDecimal.valueOf(valore).setScale(8, RoundingMode.HALF_UP).doubleValue();
    }


    @Transactional
    public boolean A2AisPresent(String nomeBolletta, String idPod) {
        return bollettaRepo.A2AisPresent(nomeBolletta, idPod);
    }

    public List<BollettaPod> findBollettaPodByPods(List<Pod> pods) {
        return bollettaRepo.findBollettaPodByPods(pods);
    }
}
