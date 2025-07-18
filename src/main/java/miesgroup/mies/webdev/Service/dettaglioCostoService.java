package miesgroup.mies.webdev.Service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import miesgroup.mies.webdev.Model.dettaglioCosto;
import miesgroup.mies.webdev.Repository.dettaglioCostoRepo;
import miesgroup.mies.webdev.Rest.Model.dettaglioCostoDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class dettaglioCostoService {
    private final dettaglioCostoRepo costiRepo;

    public dettaglioCostoService(dettaglioCostoRepo costiRepo) {
        this.costiRepo = costiRepo;
    }


    @Transactional
    public boolean createDettaglioCosto(String item, String unitaMisura, Integer modality, Integer checkModality, Double costo, String categoria, String intervalloPotenza, String classeAgevolazione, String annoRiferimento, String itemDescription) {
        dettaglioCosto dC = new dettaglioCosto();
        dC.setItem(item);
        dC.setCategoria(categoria);
        dC.setCosto(costo);
        dC.setUnitaMisura(unitaMisura);
        dC.setModality(modality);
        dC.setCheckModality(checkModality);
        dC.setIntervalloPotenza(intervalloPotenza);
        dC.setClasseAgevolazione(classeAgevolazione);
        dC.setAnnoRiferimento(annoRiferimento);
        dC.setItemDescription(itemDescription);
        return costiRepo.aggiungiCosto(dC);
    }

    @Transactional
    public dettaglioCosto getSum(String intervalloPotenza) {
        return costiRepo.getSum(intervalloPotenza);
    }

    @Transactional
    public void deleteCosto(int id) {
        costiRepo.deleteCosto(id);
    }

    @Transactional
    public boolean updateDettaglioCosto(String item, String unitaMisura, Integer modality, Integer checkModality, Double costo, String categoria, String intervalloPotenza, String classeAgevolazione, String annoRiferimento, String itemDescription) {
        dettaglioCosto dC = new dettaglioCosto();
        dC.setId(costiRepo.find("item = ?1", item).firstResult().getId());
        dC.setItem(item);
        dC.setCategoria(categoria);
        dC.setCosto(costo);
        dC.setUnitaMisura(unitaMisura);
        dC.setModality(modality);
        dC.setCheckModality(checkModality);
        dC.setIntervalloPotenza(intervalloPotenza);
        dC.setClasseAgevolazione(classeAgevolazione);
        dC.setAnnoRiferimento(annoRiferimento);
        dC.setItemDescription(itemDescription);
        return costiRepo.updateCosto(dC);
    }

    // Metodo che genera il file Excel
    @Transactional
    public ByteArrayOutputStream generateExcelFile() throws Exception {
        List<dettaglioCosto> costiList = costiRepo.getAllCosti();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("dettaglioCosto");

            // Aggiungi l'intestazione
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Item", "Unità di Misura", "Trimestre", "Anno",
                    "Costo", "Categoria", "Intervallo Potenza",
                    "Classe Agevolazione", "Anno di riferimento", "Descrizione Estesa"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderCellStyle(workbook));
            }

            // Aggiungi i dati
            int rowIndex = 1;
            for (dettaglioCosto costo : costiList) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(safeString(costo.getItem()));
                row.createCell(1).setCellValue(safeString(costo.getUnitaMisura()));
                row.createCell(2).setCellValue(safeInteger(costo.getModality()));
                row.createCell(3).setCellValue(safeInteger(costo.getCheckModality()));
                row.createCell(4).setCellValue(safeDouble(costo.getCosto()));
                row.createCell(5).setCellValue(safeString(costo.getCategoria()));
                row.createCell(6).setCellValue(safeString(costo.getIntervalloPotenza()));
                row.createCell(7).setCellValue(safeString(costo.getClasseAgevolazione()));
                row.createCell(8).setCellValue(safeString(costo.getAnnoRiferimento()));
                row.createCell(9).setCellValue(safeString(costo.getItemDescription()));
            }

            // Scrivi il workbook nel flusso di output
            workbook.write(out);

            return out;
        }
    }

    // Metodi di utilità per evitare NullPointerException
    private String safeString(String value) {
        return value != null ? value : "";
    }
    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }
    private int safeInteger(Integer value) {
        return value != null ? value : 0;
    }



    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    @Transactional
    public void processExcelFile(InputStream excelInputStream) {
        try (Workbook workbook = WorkbookFactory.create(excelInputStream)) {
            Sheet sheet = workbook.getSheet("dettaglioCosto");
            if (sheet == null) {
                throw new IllegalArgumentException("Il foglio 'dettaglioCosto' non esiste nel file Excel.");
            }

            List<dettaglioCosto> existingCosti = costiRepo.getAllCosti();
            int emptyRowCount = 0;

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue; // Salta l'intestazione
                }

                boolean isEmptyRow = true;
                for (int i = 0; i <= 9; i++) {
                    if (!getCellValue(row.getCell(i)).isEmpty()) {
                        isEmptyRow = false;
                        break;
                    }
                }

                if (isEmptyRow) {
                    emptyRowCount++;
                    if (emptyRowCount >= 3) {
                        System.out.println("Rilevate 3 righe vuote consecutive. Interruzione del parsing.");
                        break;
                    }
                    continue;
                } else {
                    emptyRowCount = 0;
                }

                String item = getCellValue(row.getCell(0));
                String unitaMisura = getCellValue(row.getCell(1));
                String modalityStr = getCellValue(row.getCell(2));
                String checkModalityStr = getCellValue(row.getCell(3));
                String costoStr = getCellValue(row.getCell(4));
                String categoria = getCellValue(row.getCell(5));
                String intervalloPotenza = getCellValue(row.getCell(6));
                String classeAgevolazione = getCellValue(row.getCell(7));
                String annoRiferimento = getCellValue(row.getCell(8));
                String itemDescription = getCellValue(row.getCell(9));

                int modality = modalityStr.isEmpty() ? 0 : (int) Double.parseDouble(modalityStr);
                int checkModality = checkModalityStr.isEmpty() ? 0 : (int) Double.parseDouble(checkModalityStr);
                Double costo = costoStr.isEmpty() ? 0.0 : Double.parseDouble(costoStr);

                if (!existsInList(existingCosti, item, unitaMisura, modality, checkModality, costo, categoria, intervalloPotenza, classeAgevolazione, annoRiferimento, itemDescription)) {
                    dettaglioCosto dettaglioCosto = new dettaglioCosto();
                    dettaglioCosto.setItem(item);
                    dettaglioCosto.setUnitaMisura(unitaMisura);
                    dettaglioCosto.setModality(modality);
                    dettaglioCosto.setCheckModality(checkModality);
                    dettaglioCosto.setCosto(costo);
                    dettaglioCosto.setCategoria(categoria);
                    dettaglioCosto.setIntervalloPotenza(intervalloPotenza);
                    dettaglioCosto.setClasseAgevolazione(classeAgevolazione);
                    dettaglioCosto.setAnnoRiferimento(annoRiferimento);
                    dettaglioCosto.setItemDescription(itemDescription);

                    costiRepo.persist(dettaglioCosto);
                    existingCosti.add(dettaglioCosto);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'elaborazione del file Excel", e);
        }
    }

    private boolean existsInList(List<dettaglioCosto> existingCosti, String item, String unitaMisura, int modality, int checkModality, Double costo, String categoria, String intervalloPotenza, String classeAgevolazione, String annoRiferimento, String itemDescription) {
        for (dettaglioCosto costoEsistente : existingCosti) {
            if (
                    areEqual(costoEsistente.getItem(), item) &&
                            areEqual(costoEsistente.getUnitaMisura(), unitaMisura) &&
                            costoEsistente.getModality() == modality &&
                            costoEsistente.getCheckModality() == checkModality &&
                            Double.compare(costoEsistente.getCosto(), costo) == 0 &&
                            areEqual(costoEsistente.getCategoria(), categoria) &&
                            areEqual(costoEsistente.getIntervalloPotenza(), intervalloPotenza) &&
                            areEqual(costoEsistente.getClasseAgevolazione(), classeAgevolazione) &&
                            areEqual(costoEsistente.getAnnoRiferimento(), annoRiferimento) &&
                            areEqual(costoEsistente.getItemDescription(), itemDescription)
            ) {
                return true;
            }
        }
        return false;
    }


    private boolean areEqual(String value1, String value2) {
        if (value1 == null && value2 == null) {
            return true; // Entrambi null
        }
        if (value1 == null || value2 == null) {
            return false; // Uno è null, l'altro no
        }
        return value1.equals(value2); // Confronto normale
    }


    private String getCellValue(Cell cell) {
        if (cell == null) {
            return ""; // Restituisce stringa vuota se la cella è null
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return ""; // Restituisce stringa vuota se la cella è vuota
            default:
                return "";
        }
    }


    public List<dettaglioCosto> getArticoli(String anno, String mese, String categoria, String rangePotenza) {
        return costiRepo.getArticoli(anno, mese, categoria, rangePotenza);
    }

    public List<dettaglioCosto> getArticoliDispacciamento(String anno, String mese, String dispacciamento) {
        return costiRepo.getArticoliDispacciamento(anno, mese, dispacciamento);
    }

    public PanacheQuery<dettaglioCosto> getQueryAllCosti(Integer idSessione) {
        if (idSessione == null) {
            throw new IllegalArgumentException("ID sessione mancante");
        }
        return costiRepo.getQueryAllCosti();
    }

    public long deleteIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Lista di ID mancante o vuota");
        }
        return costiRepo.deleteIds(ids);
    }

    public List<dettaglioCostoDTO> getCostiFiltrati(
            Optional<String> categoria,
            Optional<String> anno,
            Optional<String> annoRiferimento,
            Optional<String> intervalloPotenza,
            Optional<Integer> id,
            int page,
            int size
    ) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder query = new StringBuilder("1=1");

        id.ifPresent(i -> {
            query.append(" AND id = :id");
            params.put("id", i);
        });

        categoria.ifPresent(cat -> {
            query.append(" AND categoria = :categoria");
            params.put("categoria", cat);
        });

        anno.ifPresent(a -> {
            query.append(" AND anno = :anno");
            params.put("anno", a);
        });

        annoRiferimento.ifPresent(ar -> {
            query.append(" AND annoRiferimento = :annoRiferimento");
            params.put("annoRiferimento", ar);
        });

        intervalloPotenza.ifPresent(p -> {
            query.append(" AND intervalloPotenza = :intervalloPotenza");
            params.put("intervalloPotenza", p);
        });

        return costiRepo.find(query.toString(), params)
                .page(Page.of(page, size))
                .stream()
                .map(dettaglioCostoDTO::new)
                .toList();
    }

    public long countCostiFiltrati(Optional<String> categoria, Optional<String> anno, Optional<String> annoRif, Optional<String> intervalloPotenza) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder query = new StringBuilder("1=1");

        categoria.ifPresent(cat -> {
            query.append(" AND categoria = :categoria");
            params.put("categoria", cat);
        });

        anno.ifPresent(a -> {
            query.append(" AND anno = :anno");
            params.put("anno", a);
        });

        annoRif.ifPresent(ar -> {
            query.append(" AND annoRiferimento = :annoRiferimento");
            params.put("annoRiferimento", ar);
        });

        intervalloPotenza.ifPresent(p -> {
            query.append(" AND intervalloPotenza = :intervalloPotenza");
            params.put("intervalloPotenza", p);
        });

        return costiRepo.count(query.toString(), params);
    }

    public List<String> getAnniRiferimento() {
        return costiRepo.getAnniRiferimento();
    }

    public List<String> getIntervalliPotenza() {
        return costiRepo.getIntervalliPotenza();
    }

    public List<String> getCategorie() {
        return costiRepo.getCategorie();
    }

    public List<String> getClasseAgevolazione() {
        return costiRepo.getClassiAgevolazione();
    }

    public List<String> getUnitaMisure() {
        return costiRepo.getUnitaMisure();
    }

    public List<String> getItem() {
        return costiRepo.getItem();
    }
}