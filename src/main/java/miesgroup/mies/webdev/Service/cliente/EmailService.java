package miesgroup.mies.webdev.Service.cliente;


import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.*;
import miesgroup.mies.webdev.Model.alert.GeneralAlert;
import miesgroup.mies.webdev.Model.alert.MonthlyAlert;
import miesgroup.mies.webdev.Model.alert.QuarterlyAlert;
import miesgroup.mies.webdev.Model.alert.YearlyAlert;
import miesgroup.mies.webdev.Model.cliente.Cliente;
import miesgroup.mies.webdev.Repository.*;
import miesgroup.mies.webdev.Repository.alert.GeneralAlertRepo;
import miesgroup.mies.webdev.Repository.alert.MonthlyAlertRepo;
import miesgroup.mies.webdev.Repository.alert.QuarterlyAlertRepo;
import miesgroup.mies.webdev.Repository.alert.YearlyAlertRepo;
import miesgroup.mies.webdev.Repository.bolletta.PodRepo;
import miesgroup.mies.webdev.Repository.cliente.ClienteRepo;

import java.util.*;

@ApplicationScoped
public class EmailService {

    @Inject
    ClienteRepo clienteRepo;
    @Inject
    PodRepo podRepo;
    @Inject
    GeneralAlertRepo generalAlertRepo;
    @Inject
    MonthlyAlertRepo monthlyAlertRepo;
    @Inject
    QuarterlyAlertRepo quarterlyAlertRepo;
    @Inject
    YearlyAlertRepo yearlyAlertRepo;
    @Inject
    Mailer mailer;

    @Inject EmailTemplateGenerator emailTempGen;
    @Inject SessionService sessionService;
    @Inject ClienteService clienteService;


    public boolean toggleEmailStatus(int idUtente, boolean frontendValue) {
        Cliente cliente = clienteRepo.findById(idUtente);
        if (cliente == null) return false;
        Boolean current = cliente.getCheckEmail();
        cliente.setCheckEmail(current == null ? frontendValue : !current);
        return true;
    }

    public Boolean getCheckEmailStatus(int idUtente) {
        Cliente cliente = clienteRepo.findById(idUtente);
        return cliente != null ? cliente.getCheckEmail() : null;
    }

    public Map<String, Boolean> checkUserAlert(int idUtente, String futuresType) {
        Map<String, Boolean> result = new HashMap<>();
        switch (futuresType) {
            case "General" -> result.put("GeneralAlert", generalAlertRepo.existsByUserId(idUtente));
            case "Monthly" -> result.put("MonthlyAlert", monthlyAlertRepo.existsByUserId(idUtente));
            case "Quarterly" -> result.put("QuarterlyAlert", quarterlyAlertRepo.existsByUserId(idUtente));
            case "Yearly" -> result.put("YearlyAlert", yearlyAlertRepo.existsByUserId(idUtente));
            case "All" -> {
                result.put("GeneralAlert", generalAlertRepo.existsByUserId(idUtente));
                result.put("MonthlyAlert", monthlyAlertRepo.existsByUserId(idUtente));
                result.put("QuarterlyAlert", quarterlyAlertRepo.existsByUserId(idUtente));
                result.put("YearlyAlert", yearlyAlertRepo.existsByUserId(idUtente));
            }
        }
        return result;
    }

    @Transactional
    public Map<String, Boolean> deleteUserAlert(int idUtente, String futuresType) {
        Map<String, Boolean> result = new HashMap<>();
        switch (futuresType) {
            case "General" -> result.put("GeneralAlert", generalAlertRepo.deleteByUserId(idUtente));
            case "Monthly" -> result.put("MonthlyAlert", monthlyAlertRepo.deleteByUserId(idUtente));
            case "Quarterly" -> result.put("QuarterlyAlert", quarterlyAlertRepo.deleteByUserId(idUtente));
            case "Yearly" -> result.put("YearlyAlert", yearlyAlertRepo.deleteByUserId(idUtente));
            case "All" -> {
                result.put("MonthlyAlert", monthlyAlertRepo.deleteByUserId(idUtente));
                result.put("QuarterlyAlert", quarterlyAlertRepo.deleteByUserId(idUtente));
                result.put("YearlyAlert", yearlyAlertRepo.deleteByUserId(idUtente));
            }
        }
        return result;
    }

    @Transactional
    public boolean updateDataFuturesAlert(int idUtente, String futuresType, double[] maxPriceValue, double[] minPriceValue, boolean[] checkModality, boolean checkEmail) {
        Cliente cliente = clienteRepo.findById(idUtente);
        if (cliente == null) return false;

        boolean updated = false;

        if ("All".equals(futuresType)) {
            updated |= monthlyAlertRepo.saveOrUpdate(cliente, maxPriceValue[2], minPriceValue[2], checkModality[2]);
            updated |= quarterlyAlertRepo.saveOrUpdate(cliente, maxPriceValue[1], minPriceValue[1], checkModality[1]);
            updated |= yearlyAlertRepo.saveOrUpdate(cliente, maxPriceValue[0], minPriceValue[0], checkModality[0]);
        } else {
            // Fixed this section - don't calculate index, just use 0 directly since arrays have length 1
            switch (futuresType) {
                case "General" -> updated = generalAlertRepo.saveOrUpdate(cliente, maxPriceValue[0], minPriceValue[0], checkModality[0]);
                case "Monthly" -> updated = monthlyAlertRepo.saveOrUpdate(cliente, maxPriceValue[0], minPriceValue[0], checkModality[0]);
                case "Quarterly" -> updated = quarterlyAlertRepo.saveOrUpdate(cliente, maxPriceValue[0], minPriceValue[0], checkModality[0]);
                case "Yearly" -> updated = yearlyAlertRepo.saveOrUpdate(cliente, maxPriceValue[0], minPriceValue[0], checkModality[0]);
                default -> throw new IllegalArgumentException("Tipo non valido");
            }
        }

        toggleEmailStatus(idUtente, checkEmail);
        return updated;
    }

    public Map<String, Boolean> checkAlertStates(int idUtente) {
        return checkUserAlert(idUtente, "All");
    }

    public List<Map<String, Object>> checkUserAlertFillField(int idUtente) {
        List<Map<String, Object>> alerts = new ArrayList<>();

        generalAlertRepo.findByUserId(idUtente).ifPresent(g -> {
            Map<String, Object> map = new HashMap<>();
            map.put("futuresType", "GeneralAlert");
            map.put("maxPriceValue", g.getMaxPriceValue());
            map.put("minPriceValue", g.getMinPriceValue());
            map.put("checkModality", g.getCheckModality());
            map.put("idUtente", idUtente);
            alerts.add(map);
        });

        monthlyAlertRepo.findByUserId(idUtente).ifPresent(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("futuresType", "MonthlyAlert");
            map.put("maxPriceValue", m.getMaxPriceValue());
            map.put("minPriceValue", m.getMinPriceValue());
            map.put("checkModality", m.getCheckModality());
            map.put("idUtente", idUtente);
            alerts.add(map);
        });

        quarterlyAlertRepo.findByUserId(idUtente).ifPresent(q -> {
            Map<String, Object> map = new HashMap<>();
            map.put("futuresType", "QuarterlyAlert");
            map.put("maxPriceValue", q.getMaxPriceValue());
            map.put("minPriceValue", q.getMinPriceValue());
            map.put("checkModality", q.getCheckModality());
            map.put("idUtente", idUtente);
            alerts.add(map);
        });

        yearlyAlertRepo.findByUserId(idUtente).ifPresent(y -> {
            Map<String, Object> map = new HashMap<>();
            map.put("futuresType", "YearlyAlert");
            map.put("maxPriceValue", y.getMaxPriceValue());
            map.put("minPriceValue", y.getMinPriceValue());
            map.put("checkModality", y.getCheckModality());
            map.put("idUtente", idUtente);
            alerts.add(map);
        });

        return alerts;
    }

    public boolean hasGeneralAlert(int userId) {
        return GeneralAlert.find("idUtente", userId).firstResultOptional().isPresent();
    }

    public boolean hasMonthlyAlert(int userId) {
        return MonthlyAlert.find("idUtente", userId).firstResultOptional().isPresent();
    }

    public boolean hasQuarterlyAlert(int userId) {
        return QuarterlyAlert.find("idUtente", userId).firstResultOptional().isPresent();
    }

    public boolean hasYearlyAlert(int userId) {
        return YearlyAlert.find("idUtente", userId).firstResultOptional().isPresent();
    }


    public void sendAlertConfigurationEmail(Cliente cliente, JsonObject params){
        String futuresType = params.getString("futuresType");
        boolean checkEmail = params.getBoolean("activeAlert", false);

        if (futuresType.equals("All")) {
            double[] max = {
                    parseDoubleParam(params, "maximumLevelYearly"),
                    parseDoubleParam(params, "maximumLevelQuarterly"),
                    parseDoubleParam(params, "maximumLevelMonthly")
            };
            double[] min = {
                    parseDoubleParam(params, "minimumLevelYearly"),
                    parseDoubleParam(params, "minimumLevelQuarterly"),
                    parseDoubleParam(params, "minimumLevelMonthly")
            };
            boolean[] checks = {
                    params.getBoolean("checkModalityYearly", false),
                    params.getBoolean("checkModalityQuarterly", false),
                    params.getBoolean("checkModalityMonthly", false)
            };

            updateDataFuturesAlert(cliente.getId(), futuresType, max, min, checks, checkEmail);
        } else {
            double[] max = { parseDoubleParam(params, "maximumLevel") };
            double[] min = { parseDoubleParam(params, "minimumLevel") };
            boolean[] checks = { params.getBoolean("checkModality", false) };

            updateDataFuturesAlert(cliente.getId(), futuresType, max, min, checks, checkEmail);
        }

        Map<String, Object> configData = prepareAlertConfigData(params);

        String emailHtml = emailTempGen.generateAlertConfigEmail(
                cliente.getUsername(),
                cliente.getUsername(),
                futuresType,
                configData
        );

        Mail mail = Mail.withHtml(
                cliente.getEmail(),
                "Configurazione Alert Futures",
                emailHtml
        );

        mailer.send(mail);
        System.out.println("📧 Email di configurazione inviata a: " + cliente.getEmail());
    }

    private Map<String, Object> prepareAlertConfigData(JsonObject params) {
        Map<String, Object> configData = new HashMap<>();
        String futuresType = params.getString("futuresType");
        boolean checkEmail = params.getBoolean("activeAlert", false);

        if (futuresType.equals("All")) {
            double[] max = {
                    parseDoubleParam(params, "maximumLevelYearly"),
                    parseDoubleParam(params, "maximumLevelQuarterly"),
                    parseDoubleParam(params, "maximumLevelMonthly")
            };
            double[] min = {
                    parseDoubleParam(params, "minimumLevelYearly"),
                    parseDoubleParam(params, "minimumLevelQuarterly"),
                    parseDoubleParam(params, "minimumLevelMonthly")
            };
            String[] freq = {
                    params.getString("frequencyYearly", ""),
                    params.getString("frequencyQuarterly", ""),
                    params.getString("frequencyMonthly", "")
            };
            boolean[] checks = {
                    params.getBoolean("checkModalityYearly", false),
                    params.getBoolean("checkModalityQuarterly", false),
                    params.getBoolean("checkModalityMonthly", false)
            };

            configData.put("max", max);
            configData.put("min", min);
            configData.put("freq", freq);
            configData.put("checks", checks);
        } else {
            double[] max = { parseDoubleParam(params, "maximumLevel") };
            double[] min = { parseDoubleParam(params, "minimumLevel") };
            String[] freq = { params.getString("frequencyAlert", "") };
            boolean[] checks = { params.getBoolean("checkModality", false) };

            configData.put("max", max);
            configData.put("min", min);
            configData.put("freq", freq);
            configData.put("checks", checks);
        }

        configData.put("checkEmail", checkEmail);
        return configData;
    }

    private double parseDoubleParam(JsonObject obj, String key) {
        if (!obj.containsKey(key)) return 0.0;
        var val = obj.get(key);
        return val instanceof JsonNumber ? ((JsonNumber) val).doubleValue() : Double.parseDouble(obj.getString(key));
    }

}
