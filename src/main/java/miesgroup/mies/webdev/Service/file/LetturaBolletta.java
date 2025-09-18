package miesgroup.mies.webdev.Service.file;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.bolletta.PodRepo;
import miesgroup.mies.webdev.Repository.file.FileRepo;
import miesgroup.mies.webdev.Service.DateUtils;
import miesgroup.mies.webdev.Service.LogCustom;
import miesgroup.mies.webdev.Service.bolletta.verBollettaPodService;
import miesgroup.mies.webdev.Service.budget.BudgetManagerService;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

@ApplicationScoped
public class LetturaBolletta {

    @Inject FileRepo fileRepo;
    @Inject BollettaPodRepo BollettaPodRepo;
    @Inject PodRepo podRepo;
    @Inject BudgetManagerService budgetManager;
    @Inject Lettura lettura;
    @Inject verBollettaPodService verBollettaPodService;

    @Transactional
    public String extractValuesFromXmlA2A(byte[] xmlData, String idPod) {
        LogCustom.logTitle("extractValuesFromXmlA2A");
        LogCustom.logKV("idPod", idPod);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xmlData));

            String nomeBolletta = lettura.extractBollettaNome(document);
            if (nomeBolletta == null) {
                LogCustom.logWarn("Nessun numero bolletta trovato.");
                return null;
            } else {
                LogCustom.logOk("Nome/Numero bolletta: " + nomeBolletta);
                BollettaPodRepo.A2AisPresent(nomeBolletta, idPod);
            }

            Map<String, Map<String, Map<String, Integer>>> lettureMese = lettura.extractLetture(document);
            Map<String, Map<String, Double>> spesePerMese = lettura.extractSpesePerMese(document, lettureMese);
            Map<String, Map<String, Double>> kWhPerMese = lettura.extractKwhPerMese(document);
            Periodo periodo = lettura.extractPeriodo(document);

            LogCustom.logKV("Periodo estratto", periodo.getInizio() + " -> " + periodo.getDataFine() + " (anno " + periodo.getAnno() + ")");
            LogCustom.logLetture("LETTURE - Mappa completa", lettureMese);
            LogCustom.logNestedDoubles("SPESE - Mappa aggregata per mese", spesePerMese);
            LogCustom.logNestedDoubles("KWH - Mappa aggregata per mese", kWhPerMese);

            if (lettureMese.isEmpty()) {
                LogCustom.logWarn("lettureMese vuoto. Interrompo.");
                return null;
            }

            LogCustom.logTitle("Persistenza dati su DB");
            LogCustom.logKV("idPod", idPod);
            LogCustom.logKV("nomeBolletta", nomeBolletta);
            LogCustom.logKV("Periodo (fine)", periodo.getDataFine());
            fileRepo.saveDataToDatabase(lettureMese, spesePerMese, idPod, nomeBolletta, periodo, kWhPerMese);

            // ——————— AGGIUNTA BUDGET E BUDGET_ALL ———————
            String mese = DateUtils.getMonthFromDateLocalized(periodo.getDataFine()); // es: "giugno"
            String anno = periodo.getAnno();
            BollettaPod bolletta = BollettaPodRepo.find(
                    "nomeBolletta = ?1 and idPod = ?2 and mese = ?3 and anno = ?4",
                    nomeBolletta, idPod, mese, anno).firstResult();

            if (bolletta != null) {
                LogCustom.logOk("Bolletta trovata in DB: id=" + bolletta.getId() + "  pod=" + bolletta.getIdPod()
                        + "  mese=" + bolletta.getMese() + "  anno=" + bolletta.getAnno());
                LogCustom.logKV("SpeseEnergia", bolletta.getSpeseEne());
                LogCustom.logKV("TotAttiva(kWh)", bolletta.getTotAtt());
                LogCustom.logKV("Oneri", bolletta.getOneri());

                // Chiamata al servizio per la verifica A2A
                verBollettaPodService.A2AVerifica(bolletta);

                // Recupera il cliente dal POD
                Pod pod = podRepo.findById(bolletta.getIdPod());
                if (pod == null || pod.getUtente() == null) {
                    throw new IllegalStateException("Impossibile trovare il cliente associato al POD " + bolletta.getIdPod());
                }
                Cliente cliente = pod.getUtente();

                // Chiamata ai due metodi estratti
                budgetManager.gestisciBudget(bolletta, cliente, mese);
                budgetManager.gestisciBudgetAll(bolletta, cliente, mese);

            } else {
                LogCustom.logWarn("Bolletta non trovata in DB dopo il salvataggio per mese/anno.");
            }
            // ——————— FINE AGGIUNTA ———————

            LogCustom.logOk("Completato extractValuesFromXmlA2A per bolletta " + nomeBolletta);
            return nomeBolletta;

        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
    }
}
