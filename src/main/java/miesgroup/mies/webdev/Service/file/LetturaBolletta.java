package miesgroup.mies.webdev.Service.file;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.Periodo;
import miesgroup.mies.webdev.Model.bolletta.BollettaPod;
import miesgroup.mies.webdev.Model.bolletta.Pod;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Model.budget.BudgetAll;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.bolletta.BollettaPodRepo;
import miesgroup.mies.webdev.Repository.bolletta.PodRepo;
import miesgroup.mies.webdev.Repository.file.FileRepo;
import miesgroup.mies.webdev.Service.DateUtils;
import miesgroup.mies.webdev.Service.LogCustom;
import miesgroup.mies.webdev.Service.budget.BudgetAllService;
import miesgroup.mies.webdev.Service.budget.BudgetService;
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
    @Inject BudgetService budgetService;
    @Inject BudgetAllService budgetAllService;
    @Inject Lettura lettura;

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

                // Recupera il cliente dal POD
                Pod pod = podRepo.findById(bolletta.getIdPod());
                if (pod == null || pod.getUtente() == null) {
                    throw new IllegalStateException("Impossibile trovare il cliente associato al POD " + bolletta.getIdPod());
                }
                Cliente cliente = pod.getUtente();

                // --- Inserisci/aggiorna BUDGET
                Budget budget = new Budget();
                budget.setPodId(bolletta.getIdPod());
                budget.setAnno(Integer.parseInt(bolletta.getAnno()));
                budget.setMese(Integer.parseInt(DateUtils.getMonthNumber(mese))); // es. giugno -> 6
                budget.setPrezzoEnergiaBase(bolletta.getSpeseEne() != null ? bolletta.getSpeseEne() : 0.0);
                budget.setConsumiBase(bolletta.getTotAtt()      != null ? bolletta.getTotAtt()     : 0.0);
                budget.setOneriBase(bolletta.getOneri()            != null ? bolletta.getOneri()         : 0.0);
                budget.setPrezzoEnergiaPerc(0.0);
                budget.setConsumiPerc(0.0);
                budget.setOneriPerc(0.0);
                budget.setCliente(cliente);

                LogCustom.logTitle("BUDGET - Insert/Update");
                LogCustom.logKV("Cliente", cliente.getId());
                LogCustom.logKV("POD", budget.getPodId());
                LogCustom.logKV("Anno/Mese", budget.getAnno() + "/" + budget.getMese());
                LogCustom.logKV("PrezzoEnergiaBase", budget.getPrezzoEnergiaBase());
                LogCustom.logKV("ConsumiBase", budget.getConsumiBase());
                LogCustom.logKV("OneriBase", budget.getOneriBase());
                budgetService.creaBudget(budget);

                // --- Inserisci/aggiorna BUDGET_ALL
                BudgetAll budgetAll = new BudgetAll();
                budgetAll.setIdPod("ALL");
                budgetAll.setCliente(cliente);
                budgetAll.setAnno(Integer.parseInt(bolletta.getAnno()));
                budgetAll.setMese(Integer.parseInt(DateUtils.getMonthNumber(mese))); // es. 6 = giugno

                budgetAll.setPrezzoEnergiaBase(bolletta.getSpeseEne() != null ? bolletta.getSpeseEne() : 0.0);
                budgetAll.setConsumiBase(bolletta.getTotAtt() != null ? bolletta.getTotAtt() : 0.0);
                budgetAll.setOneriBase(bolletta.getOneri() != null ? bolletta.getOneri() : 0.0);
                budgetAll.setPrezzoEnergiaPerc(0.0);
                budgetAll.setConsumiPerc(0.0);
                budgetAll.setOneriPerc(0.0);

                LogCustom.logTitle("BUDGET_ALL - Upsert");
                LogCustom.logKV("Cliente", cliente.getId());
                LogCustom.logKV("Anno/Mese", budgetAll.getAnno() + "/" + budgetAll.getMese());
                LogCustom.logKV("PrezzoEnergiaBase", budgetAll.getPrezzoEnergiaBase());
                LogCustom.logKV("ConsumiBase", budgetAll.getConsumiBase());
                LogCustom.logKV("OneriBase", budgetAll.getOneriBase());
                budgetAllService.upsertAggregato(budgetAll);

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
