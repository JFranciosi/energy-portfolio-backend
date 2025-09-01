package miesgroup.mies.webdev.Rest.budget;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import miesgroup.mies.webdev.Model.budget.Budget;
import miesgroup.mies.webdev.Service.budget.BudgetService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Path("/budget")
public class BudgetExportResource {

    @Inject
    BudgetService budgetService;

    @GET
    @Path("/export")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response exportBudgetExcel(@QueryParam("pod") String pod, @QueryParam("anno") int anno) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Budget");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mese");
            header.createCell(1).setCellValue("Prezzo Energia (€)");
            header.createCell(2).setCellValue("Consumi (kWh)");
            header.createCell(3).setCellValue("Oneri (€)");
            header.createCell(4).setCellValue("Prezzo Energia %");
            header.createCell(5).setCellValue("Consumi %");
            header.createCell(6).setCellValue("Oneri %");
            header.createCell(7).setCellValue("Spesa Totale (€)");

            List<Budget> budgetList;

            if ("ALL".equalsIgnoreCase(pod)) {
                // Recupera lista POD diversi da ALL
                List<String> allPods = budgetService.getAllPodIds(); // IMPLEMENTA QUESTO METODO NEL SERVICE

                List<Budget> allBudgets = new java.util.ArrayList<>();

                for (String singlePod : allPods) {
                    List<Budget> budgetsPod = budgetService.getBudgetsPerPodEAnno(singlePod, anno);
                    allBudgets.addAll(budgetsPod);
                }

                java.util.Map<Integer, AggregatedBudget> aggMap = new java.util.HashMap<>();

                for (Budget b : allBudgets) {
                    int mese = b.getMese();
                    AggregatedBudget agg = aggMap.getOrDefault(mese, new AggregatedBudget(mese));
                    agg.add(b);
                    aggMap.put(mese, agg);
                }

                budgetList = new java.util.ArrayList<>();
                for (Integer mese : aggMap.keySet()) {
                    budgetList.add(aggMap.get(mese).compute());
                }
                budgetList.sort((a, b) -> Integer.compare(a.getMese(), b.getMese()));

            } else {
                // Singolo POD
                budgetList = budgetService.getBudgetsPerPodEAnno(pod, anno);
            }

            int rowNum = 1;
            for (Budget b : budgetList) {
                Row row = sheet.createRow(rowNum++);

                double prezzoBase = b.getPrezzoEnergiaBase() != null ? b.getPrezzoEnergiaBase() : 0;
                double consumiBase = b.getConsumiBase() != null ? b.getConsumiBase() : 0;
                double oneriBase = b.getOneriBase() != null ? b.getOneriBase() : 0;
                double percPrezzo = b.getPrezzoEnergiaPerc() != null ? b.getPrezzoEnergiaPerc() : 0;
                double percConsumi = b.getConsumiPerc() != null ? b.getConsumiPerc() : 0;
                double percOneri = b.getOneriPerc() != null ? b.getOneriPerc() : 0;

                // €/kWh applicando % prezzo
                double prezzoUnitario = consumiBase > 0
                        ? (prezzoBase / consumiBase) * (1 + percPrezzo / 100)
                        : 0;

                // Consumi attesi
                double consumiAttesi = consumiBase * (1 + percConsumi / 100);

                // Oneri attesi
                double oneriAttesi = oneriBase * (1 + percOneri / 100);

                // Spesa totale
                double spesaTotale = prezzoUnitario * consumiAttesi + oneriAttesi;

                // Scrivi colonne aggiornate
                row.createCell(0).setCellValue(b.getMese());
                row.createCell(1).setCellValue(prezzoUnitario * consumiAttesi); // Totale energia
                row.createCell(2).setCellValue(consumiAttesi);
                row.createCell(3).setCellValue(oneriAttesi);
                row.createCell(4).setCellValue(percPrezzo);
                row.createCell(5).setCellValue(percConsumi);
                row.createCell(6).setCellValue(percOneri);
                row.createCell(7).setCellValue(spesaTotale);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);

            return Response.ok(baos.toByteArray())
                    .header("Content-Disposition", "attachment; filename=budget_" + pod + "_" + anno + ".xlsx")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Errore durante generazione file Excel")
                    .build();
        }
    }

    private static class AggregatedBudget {
        int mese;
        double prezzoEnergiaBaseSum = 0;
        int prezzoEnergiaBaseCount = 0;

        double consumiBaseSum = 0;
        double oneriBaseSum = 0;

        double prezzoEnergiaPercSum = 0;
        int prezzoEnergiaPercCount = 0;

        double consumiPercSum = 0;
        int consumiPercCount = 0;

        double oneriPercSum = 0;
        int oneriPercCount = 0;

        public AggregatedBudget(int mese) {
            this.mese = mese;
        }

        public void add(Budget b) {
            if (b.getPrezzoEnergiaBase() != null) {
                prezzoEnergiaBaseSum += b.getPrezzoEnergiaBase();
                prezzoEnergiaBaseCount++;
            }
            if (b.getConsumiBase() != null) consumiBaseSum += b.getConsumiBase();
            if (b.getOneriBase() != null) oneriBaseSum += b.getOneriBase();

            if (b.getPrezzoEnergiaPerc() != null) {
                prezzoEnergiaPercSum += b.getPrezzoEnergiaPerc();
                prezzoEnergiaPercCount++;
            }
            if (b.getConsumiPerc() != null) {
                consumiPercSum += b.getConsumiPerc();
                consumiPercCount++;
            }
            if (b.getOneriPerc() != null) {
                oneriPercSum += b.getOneriPerc();
                oneriPercCount++;
            }
        }

        public Budget compute() {
            Budget b = new Budget();
            b.setMese(mese);
            b.setPrezzoEnergiaBase(prezzoEnergiaBaseCount > 0 ? prezzoEnergiaBaseSum / prezzoEnergiaBaseCount : 0);
            b.setConsumiBase(consumiBaseSum);
            b.setOneriBase(oneriBaseSum);
            b.setPrezzoEnergiaPerc(prezzoEnergiaPercCount > 0 ? prezzoEnergiaPercSum / prezzoEnergiaPercCount : 0);
            b.setConsumiPerc(consumiPercCount > 0 ? consumiPercSum / consumiPercCount : 0);
            b.setOneriPerc(oneriPercCount > 0 ? oneriPercSum / oneriPercCount : 0);
            return b;
        }
    }
}
