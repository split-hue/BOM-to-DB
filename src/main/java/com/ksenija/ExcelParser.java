package com.ksenija;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a CalcuQuote Excel BOM file into a list of {@link BomItem} objects.
 * <p>
 * The parser automatically locates the header row by scanning the first 20 rows
 * for a cell containing "mpn" or "description". It then builds a column index map
 * and delegates each data row to {@link BomItemMapper}.
 * <p>
 * Rows with a missing MPN are skipped.
 */
@Component                  //Spring bo naredil: ExcelParser parser = new ExcelParser();
public class ExcelParser {  // Spring naj ustvari objekt tega razreda in ga upravlja v containerju.
    /**
     * Delegate that converts a single Excel row into a {@link BomItem}.
     * Injected by Spring via constructor injection — not created manually.
     */

    private final BomItemMapper mapper;     // ExcelParser ima v sebi en objekt mapper, ki je tipa BomItemMapper
                                            //private final DataFormatter formatter = new DataFormatter();
                                            // konstruktor (se pokliče, ko narediš nov objekt.) Pokličeš: "ExcelParser parser = new ExcelParser(mapper);"
    /**
     * Constructor injection; Creates the parser with the given row mapper.
     * Spring provides the {@link BomItemMapper} instance automatically.
     *
     * @param mapper converts individual Excel rows into {@link BomItem} objects
     */
    public ExcelParser(BomItemMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Parses an Excel {@code .xlsx} file from the given input stream.
     * <p>
     * Process:
     * <ol>
     *   <li>Opens the first sheet of the workbook</li>
     *   <li>Scans the first 20 rows to find the header row (looks for "mpn" or "description")</li>
     *   <li>Builds a column index map from the header row</li>
     *   <li>Maps each subsequent data row to a {@link BomItem} via {@link BomItemMapper}</li>
     *   <li>Skips rows with a missing or empty MPN</li>
     * </ol>
     *
     * @param inputStream input stream of the uploaded {@code .xlsx} file
     * @return list of parsed {@link BomItem} objects, empty if no header row was found
     * @throws Exception if the file cannot be read or is not a valid {@code .xlsx}
     */
    public List<BomItem> parse(InputStream inputStream) throws Exception {  // sprejme Excel file -> vrne seznam z BomItem-i
        List<BomItem> items = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) { // ustvari Excel workbook iz file-a.
            Sheet sheet = workbook.getSheetAt(0);

            // najdi header vrsto: skenira prvih 20 vrstic
            int headerRowIndex = -1;        // še ni najden
            for (int r = 0; r < 20; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (Cell cell : row) {
                    try {
                        String val = cell.getStringCellValue().toLowerCase().trim();
                        if (val.equals("mpn") || val.equals("description")) {
                            headerRowIndex = r;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                if (headerRowIndex >= 0) break;
            }

            if (headerRowIndex < 0) {
                System.out.println("ERROR: Nisem našel Header vrstice!");
                return items;
            }
            //DEBUG
            System.out.println("===NAŠEL HEADER VRSTO NA ID: " + headerRowIndex + " ===");

            Row headerRow = sheet.getRow(headerRowIndex);
            Map<ExcelColumnDefinition, Integer> colMap = buildColumnMap(headerRow);     // <- ustvari colMap

            //DEBUG
            System.out.println("=== COLUMN MAP ===");
            colMap.forEach((k, v) -> System.out.println(k + " -> col " + v));
            System.out.println("=================");

            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);  // prebere vrstico
                if (row == null) continue;  // prazne preskoč
                                            // (id 5 vrste, slovar klj-vred headerja)
                BomItem item = mapper.mapRow(row, colMap);                      // <--- Kle kliče BomItemMapper
                if (item.getMpn() == null || item.getMpn().isEmpty()) continue; // če MPN ni, preskoč

                items.add(item);    //shran BOM item
            }
        }

        return items;               // vrn seznam BOM item
    }

    /**
     * Builds a map of {@link ExcelColumnDefinition} to column index from the header row.
     * <p>
     * For each header cell, checks all {@link ExcelColumnDefinition} values using
     * {@link ExcelColumnDefinition#matches(String)}. Uses {@link EnumMap} for
     * better performance and type safety compared to {@link java.util.HashMap}.
     *
     * @param headerRow     the Excel row containing column headers
     * @return              map of column definition -> column index (e.g. MPN -> 3, DESCRIPTION -> 5)
     */
    private Map<ExcelColumnDefinition, Integer> buildColumnMap(Row headerRow) {
        Map<ExcelColumnDefinition, Integer> colMap = new EnumMap<>(ExcelColumnDefinition.class);    //EnumMap is used instead of HashMap — it's faster and type-safe when keys are enums.

        if (headerRow == null) return colMap;

        headerRow.forEach(cell -> {
            String header = cell.getStringCellValue();
            for (ExcelColumnDefinition col : ExcelColumnDefinition.values()) {
                if (col.matches(header)) {
                    colMap.put(col, cell.getColumnIndex());
                    break;
                }
            }
        });
        return colMap;
    }
}