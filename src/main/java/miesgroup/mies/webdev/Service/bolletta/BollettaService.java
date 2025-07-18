package miesgroup.mies.webdev.Service.bolletta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Model.bolletta.dettaglioCosto;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Repository.bolletta.BollettaRepo;
import miesgroup.mies.webdev.Service.cliente.ClienteService;

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
    private final dettaglioCostoService costiService;


    public BollettaService(BollettaRepo bollettaRepo, ClienteService clienteService, CostoEnergiaService costoEnergiaService, CostoArticoloService costoArticoloService, dettaglioCostoService costiService) {
        this.bollettaRepo = bollettaRepo;
        this.clienteService = clienteService;
        this.costoEnergiaService = costoEnergiaService;
        this.costoArticoloService = costoArticoloService;
        this.costiService = costiService;
    }

    @Transactional
    public void A2AVerifica(BollettaPod b) {
        try {
            System.out.println("1.Recupero e controllo del consumo totale attivo");
            // ──────────────────────────────────────────────
            // 1. Recupero e controllo del consumo totale attivo
            // ──────────────────────────────────────────────
            double totAttiva = Optional.ofNullable(bollettaRepo.getConsumoA2A(b.getNomeBolletta(), b.getMese()))
                    .orElse(0.0);
            if (totAttiva <= 0) {
                System.err.println("Errore: TOT_Attiva non valido per " + b.getNomeBolletta());
                return;
            }
            System.out.println("2.Recupero e controllo della potenza impegnata");

            // ──────────────────────────────────────────────
            // 2. Recupero e controllo della potenza impegnata
            // ──────────────────────────────────────────────
            double potenzaImpegnata = Optional.ofNullable(bollettaRepo.getPotenzaImpegnata(b.getIdPod()))
                    .orElse(0.0);
            if (potenzaImpegnata <= 0) {
                System.err.println("Errore: Potenza Impegnata non valida per " + b.getNomeBolletta());
                return;
            }
            System.out.println("3. Calcolo del TOT Attiva Perdite");

            // ──────────────────────────────────────────────
            // 3. Calcolo del TOT Attiva Perdite
            // ──────────────────────────────────────────────
            Double totAttivaPerdite = (potenzaImpegnata < 100) ? totAttiva :
                    (potenzaImpegnata <= 500) ? totAttiva * 1.1 :
                            totAttiva * 1.038;
            bollettaRepo.updateTOTAttivaPerdite(totAttivaPerdite, b.getNomeBolletta(), b.getMese());

            System.out.println("4. Determinazione del trimestre in base al mese");


            // ──────────────────────────────────────────────
            // 4. Determinazione del trimestre in base al mese
            // ──────────────────────────────────────────────
            int trimestre = switch (b.getMese().toLowerCase()) {
                case "gennaio", "febbraio", "marzo" -> 1;
                case "aprile", "maggio", "giugno" -> 2;
                case "luglio", "agosto", "settembre" -> 3;
                default -> 4;
            };
            //NEL DB METTIAMO 0123 E QUI METTIAMO 1234, QUANTI CAMBI DOBBIAMO FARE A QUESTE VARIABILI 1000??

            System.out.println("5. Recupero dei corrispettivi di dispacciamento e calcolo del valore");

            // ──────────────────────────────────────────────
            // 5. Recupero dei corrispettivi di dispacciamento e calcolo del valore
            // ──────────────────────────────────────────────
            Double totCorrispettivi = Optional.ofNullable(bollettaRepo.getCorrispettiviDispacciamentoA2A(trimestre, b.getAnno()))
                    .orElse(0.0);
            String tipoTensione = bollettaRepo.getTipoTensione(b.getIdPod());
            Double verificaDispacciamento = totAttiva * totCorrispettivi;
            verificaDispacciamento *= switch (tipoTensione) {
                case "Bassa" -> 1.1;
                case "Media" -> 1.038;
                default -> 1.02;
            };
            bollettaRepo.updateVerificaDispacciamentoA2A(arrotonda(verificaDispacciamento), b.getNomeBolletta(), b.getMese());

            System.out.println(" 6. Calcolo della Generation");

            // ──────────────────────────────────────────────
            // 6. Calcolo della Generation
            // ──────────────────────────────────────────────
            Double generation = arrotonda(b.getSpeseEnergia() - verificaDispacciamento);
            bollettaRepo.updateGenerationA2A(generation, b.getNomeBolletta(), b.getMese());

            System.out.println("7. Calcolo dei costi per Trasporti");

            // ──────────────────────────────────────────────
            // 7. Calcolo dei costi per Trasporti
            // ──────────────────────────────────────────────
            Double maggiorePotenza = Optional.ofNullable(bollettaRepo.getMaggiorePotenza(b.getNomeBolletta()))
                    .orElse(0.0);
            String rangePotenza = (potenzaImpegnata <= 100) ? "<100KW" :
                    (potenzaImpegnata <= 500) ? "100-500KW" : ">500KW";
            Double quotaVariabileT = Optional.ofNullable(bollettaRepo.getCostiTrasporto(trimestre, rangePotenza, "€/KWh", b.getAnno()))
                    .orElse(0.0);
            Double quotaFissaT = Optional.ofNullable(bollettaRepo.getCostiTrasporto(trimestre, rangePotenza, "€/Month", b.getAnno()))
                    .orElse(0.0);
            Double quotaPotenzaT = Optional.ofNullable(bollettaRepo.getCostiTrasporto(trimestre, rangePotenza, "€/KW/Month", b.getAnno()))
                    .orElse(0.0) * maggiorePotenza;
            bollettaRepo.updateQuoteTrasporto(quotaVariabileT, quotaFissaT, quotaPotenzaT, b.getNomeBolletta(), b.getMese());

            double costiTrasporti = arrotonda((quotaVariabileT * totAttiva) + quotaFissaT + quotaPotenzaT);
            bollettaRepo.updateVerificaTrasportiA2A(costiTrasporti, b.getNomeBolletta(), b.getMese());

            System.out.println("8. Calcolo dei costi per le Imposte");

            // ──────────────────────────────────────────────
            // 8. Calcolo dei costi per le Imposte
            // ──────────────────────────────────────────────
            double costiImposte = (totAttiva <= 200000) ? totAttiva * 0.0125 :
                    (totAttiva <= 1200000) ? (200000 * 0.0125) + ((totAttiva - 200000) * 0.0075) :
                            (200000 * 0.0125) + (1000000 * 0.0075);
            bollettaRepo.updateVerificaImposte(arrotonda(costiImposte), b.getNomeBolletta(), b.getMese());

            System.out.println("9. Calcolo degli Oneri");

            // ──────────────────────────────────────────────
            // 9. Calcolo degli Oneri
            // ──────────────────────────────────────────────
            String classeAgevolazione = clienteService.getClasseAgevolazione(b.getIdPod());
            Double quotaEnergiaOneri = Optional.ofNullable(bollettaRepo.getCostiOneri(trimestre, rangePotenza, "€/KWh", classeAgevolazione, b.getAnno()))
                    .orElse(0.0);
            Double quotaFissaOneri = Optional.ofNullable(bollettaRepo.getCostiOneri(trimestre, rangePotenza, "€/Month", classeAgevolazione, b.getAnno()))
                    .orElse(0.0);
            Double quotaPotenzaOneri = Optional.ofNullable(bollettaRepo.getCostiOneri(trimestre, rangePotenza, "€/KW/Month", classeAgevolazione, b.getAnno()))
                    .orElse(0.0) * maggiorePotenza;
            bollettaRepo.updateQuoteOneri(quotaEnergiaOneri, quotaFissaOneri, quotaPotenzaOneri, b.getNomeBolletta(), b.getMese());

            Double costiOneri = arrotonda((quotaEnergiaOneri * totAttiva) + quotaFissaOneri + quotaPotenzaOneri);
            bollettaRepo.updateVerificaOneri(costiOneri, b.getNomeBolletta(), b.getMese());

            System.out.println("10. Calcolo delle Penali ARERA (PER FASCIA)");

            // ──────────────────────────────────────────────
            // 10. Calcolo delle Penali ARERA (PER FASCIA)
            // ──────────────────────────────────────────────

            // Recupero valori da DB
            Double f1Attiva = Optional.ofNullable(bollettaRepo.getF1A(b.getNomeBolletta(), b.getMese())).orElse(0.0);
            Double f2Attiva = Optional.ofNullable(bollettaRepo.getF2A(b.getNomeBolletta(), b.getMese())).orElse(0.0);
            Double f1Reattiva = Optional.ofNullable(bollettaRepo.getF1R(b.getNomeBolletta(), b.getMese())).orElse(0.0);
            Double f2Reattiva = Optional.ofNullable(bollettaRepo.getF2R(b.getNomeBolletta(), b.getMese())).orElse(0.0);

            // Tariffe €/kVArh
            Double costo33 = Optional.ofNullable(bollettaRepo.getPenaliSotto75(b.getAnno())).orElse(0.0);
            Double costo75 = Optional.ofNullable(bollettaRepo.getPenaliSopra75(b.getAnno())).orElse(0.0);

            // Inizializzo le penali
            double f1Penale33 = 0.0;
            double f1Penale75 = 0.0;
            double f2Penale33 = 0.0;
            double f2Penale75 = 0.0;

            // Calcolo penali per F1
            if (f1Attiva > 0) {
                double soglia33_F1 = 0.33 * f1Attiva;
                double soglia75_F1 = 0.75 * f1Attiva;

                if (f1Reattiva > soglia33_F1) {
                    double eccedenza33_F1 = f1Reattiva - soglia33_F1;
                    f1Penale33 = eccedenza33_F1 * costo33;
                }
                if (f1Reattiva > soglia75_F1) {
                    double eccedenza75_F1 = f1Reattiva - soglia75_F1;
                    f1Penale75 = eccedenza75_F1 * costo75;
                }
            }

            // Calcolo penali per F2
            if (f2Attiva > 0) {
                double soglia33_F2 = 0.33 * f2Attiva;
                double soglia75_F2 = 0.75 * f2Attiva;

                if (f2Reattiva > soglia33_F2) {
                    double eccedenza33_F2 = f2Reattiva - soglia33_F2;
                    f2Penale33 = eccedenza33_F2 * costo33;
                }
                if (f2Reattiva > soglia75_F2) {
                    double eccedenza75_F2 = f2Reattiva - soglia75_F2;
                    f2Penale75 = eccedenza75_F2 * costo75;
                }
            }

            // Salvataggio penali calcolate nel DB con arrotondamento
            bollettaRepo.updateF1Penale33(arrotonda(f1Penale33), b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateF1Penale75(arrotonda(f1Penale75), b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateF2Penale33(arrotonda(f2Penale33), b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateF2Penale75(arrotonda(f2Penale75), b.getNomeBolletta(), b.getMese());

            System.out.println("===== CONTROLLO PENALE ARERA =====");
            System.out.println("Bolletta: " + b.getNomeBolletta() + " | Mese: " + b.getMese());
            System.out.println("------ F1 ------");
            System.out.println("kWh F1 Attiva: " + f1Attiva);
            System.out.println("kVArh F1 Reattiva: " + f1Reattiva);
            System.out.println("Soglia 33% F1: " + (0.33 * f1Attiva));
            System.out.println("Soglia 75% F1: " + (0.75 * f1Attiva));
            System.out.println("Penale33 F1: " + f1Penale33);
            System.out.println("Penale75 F1: " + f1Penale75);
            System.out.println("------ F2 ------");
            System.out.println("kWh F2 Attiva: " + f2Attiva);
            System.out.println("kVArh F2 Reattiva: " + f2Reattiva);
            System.out.println("Soglia 33% F2: " + (0.33 * f2Attiva));
            System.out.println("Soglia 75% F2: " + (0.75 * f2Attiva));
            System.out.println("Penale33 F2: " + f2Penale33);
            System.out.println("Penale75 F2: " + f2Penale75);
            System.out.println("Tariffa penale 33%: " + costo33 + " €/kVArh");
            System.out.println("Tariffa penale 75%: " + costo75 + " €/kVArh");
            System.out.println("Totale penali calcolate: " + (f1Penale33 + f1Penale75 + f2Penale33 + f2Penale75) + " €");
            System.out.println("===================================");


            // ──────────────────────────────────────────────
            // 11. Calcolo Verifica Picco e Fuori Picco
            // ──────────────────────────────────────────────
            Double piccoKwh = b.getPiccoKwh();
            Double fuoriPiccoKwh = b.getFuoriPiccoKwh();
            Double costoPicco = Optional.ofNullable(bollettaRepo.getCostoPicco(trimestre, b.getAnno(), rangePotenza))
                    .orElse(0.0);
            Double costoFuoriPicco = Optional.ofNullable(bollettaRepo.getCostoFuoriPicco(trimestre, b.getAnno(), rangePotenza))
                    .orElse(0.0);
            bollettaRepo.updateVerificaPicco(arrotonda(piccoKwh * costoPicco), b.getNomeBolletta(), b.getMese());
            bollettaRepo.updateVerificaFuoriPicco(arrotonda(fuoriPiccoKwh * costoFuoriPicco), b.getNomeBolletta(), b.getMese());

            // ──────────────────────────────────────────────
            // 12. Recupero dei costi per Materia Energia per il cliente e calcolo dei costi in € in base ai kWh
            // ──────────────────────────────────────────────
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

            // Calcolo separato per ogni componente della spesa per la materia energia
            // Calcolo per le voci "F" standard
            Double f0Tot = f0Costo * f0kwh;  // Totale per F0
            Double f1Tot = f1Costo * f1kwh;  // Totale per F1
            Double f2Tot = f2Costo * f2kwh;  // Totale per F2
            Double f3Tot = f3Costo * f3kwh;  // Totale per F3

            // Calcolo per le voci delle perdite
            Double f1PerditeTot = f1PerditeCosto * f1PerditeKwh; // Totale per perdite F1
            Double f2PerditeTot = f2PerditeCosto * f2PerditeKwh; // Totale per perdite F2
            Double f3PerditeTot = f3PerditeCosto * f3PerditeKwh; // Totale per perdite F3

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

            // ──────────────────────────────────────────────
            // 14. Calcolo del costo per ogni singolo articolo
            // ──────────────────────────────────────────────

            // Calcolo del costo per ogni articolo dei trasporti
            List<dettaglioCosto> articoliTrasporti = costiService.getArticoli(b.getAnno(), b.getMese(), "trasporti", rangePotenza);
            costoArticoloService.calcolaCostiArticoli(articoliTrasporti, b, maggiorePotenza, "trasporti");

            // Calcolo del costo per ogni articolo del dispacciamento
            List<dettaglioCosto> articoliDispacciamento = costiService.getArticoliDispacciamento(b.getAnno(), b.getMese(), "dispacciamento");
            costoArticoloService.calcolaCostiArticoli(articoliDispacciamento, b, maggiorePotenza, "dispacciamento");

            // Calcolo del costo per ogni articolo degli oneri
            List<dettaglioCosto> articoliOneri = costiService.getArticoli(b.getAnno(), b.getMese(), "oneri", rangePotenza);
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
