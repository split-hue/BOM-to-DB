package com.ksenija.parser;

import com.ksenija.model.BomItem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps a single Excel row into a {@link BomItem} object.
 * <p>
 * Handles description fallback (DESCRIPTION -> WEB_DESCRIPTION -> TAXONOMY),
 * designator range expansion (e.g. R1-R3 -> R1,R2,R3),
 * and quantity vs. designator count mismatch detection.
 */
@Component
public class BomItemMapper {

    /**
     * Apache POI utility that reads any cell type as a formatted string.
     * More reliable than switching on cell type manually.
     */
    private final DataFormatter formatter = new DataFormatter();

    /**
     * Maps one Excel row to a {@link BomItem} using the provided column index map.
     * <p>
     * Description fallback order: DESCRIPTION → WEB_DESCRIPTION → TAXONOMY.
     * Designators are expanded from range notation before being stored.
     * A mismatch flag is set if the designator count does not match the quantity.
     *
     * @param row       the Excel row to read
     * @param colMap    map of {@link ExcelColumnDefinition} to column index, built from the header row
     * @return          populated {@link BomItem}
     */
    public BomItem mapRow(Row row, Map<ExcelColumnDefinition, Integer> colMap) {
        BomItem item = new BomItem();

        item.setMpn(getString(row, colMap, ExcelColumnDefinition.MPN));

        String desc = getString(row, colMap, ExcelColumnDefinition.DESCRIPTION);    //item.setDescription(getString(row, colMap, ExcelColumnDefinition.DESCRIPTION));
        if (desc.isEmpty())
            desc = getString(row, colMap, ExcelColumnDefinition.WEB_DESCRIPTION);
        if (desc.isEmpty())                                                         // ta if sam zato, da če je Desc. prazen,
            desc = getString(row, colMap, ExcelColumnDefinition.TAXONOMY);          // da uporabi taxonomy
        item.setDescription(desc);

        String designatorji = checkDesignatorji(getString(row, colMap, ExcelColumnDefinition.DESIGNATOR));
        item.setDesignator(designatorji);

        Double qty = getDouble(row, colMap, ExcelColumnDefinition.QTY, 1.0);
        item.setQty(qty);

        int desCount = countDesignatorji(designatorji);
        if (desCount > 0 && qty != null && desCount != qty.intValue()){
            item.setDesignatorMismatch(true);
            System.out.println("! QTY MISMATCH: mpn=" + item.getMpn() + " qty=" + qty.intValue() + " designators=" + desCount);
        }

        item.setManufacturer(getString(row, colMap, ExcelColumnDefinition.MANUFACTURER));
        item.setTaxonomy(getString(row, colMap, ExcelColumnDefinition.TAXONOMY));
        item.setHtsCode(getString(row, colMap, ExcelColumnDefinition.HTS_CODE));
        item.setCountryOrigin(getString(row, colMap, ExcelColumnDefinition.COUNTRY_ORIGIN));
        item.setDatasheetUrl(getString(row, colMap, ExcelColumnDefinition.DATASHEET_URL));
        item.setSupplier(getString(row, colMap, ExcelColumnDefinition.SUPPLIER));

        item.setIzmet(getDouble(row, colMap, ExcelColumnDefinition.IZMET, 0.0));
        item.setLeadTime(getDouble(row, colMap, ExcelColumnDefinition.LEAD_TIME, 0.0));
        item.setLQty(getDouble(row, colMap, ExcelColumnDefinition.LQTY, 0.0).intValue());

        return item;
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Reads a cell value as a String from the given column in a row.
     * Returns an empty String {@code ""} if the column is not in the map, or the cell is {@code null}.
     *
     * @param row       the Excel row
     * @param colMap    column index map (indexed names of columns)
     * @param col       the column to read
     * @return          trimmed cell value as String, or {@code ""} if missing
     */
    private String getString(Row row, Map<ExcelColumnDefinition, Integer> colMap, ExcelColumnDefinition col) {
        Integer idx = colMap.get(col);  // index stolpca
        if (idx == null) return "";
        Cell cell = row.getCell(idx);   // preberemo vsebino iz tega stolpca (idx), vrsta (row)
        return cell != null ? getCellAsString(cell) : "";
    }

    /**
     * Reads a cell value as a Double from the given column in a row.
     * Falls back to {@code defaultValue} if the cell is missing or not a valid number.
     * Handles European decimal notation (comma -> dot).
     *
     * @param row           the Excel row
     * @param colMap        column index map
     * @param col           the column to read
     * @param defaultValue  value to return if cell is empty or unparseable
     * @return              parsed Double, or {@code defaultValue}
     */
    private Double getDouble(Row row, Map<ExcelColumnDefinition, Integer> colMap,
                             ExcelColumnDefinition col, Double defaultValue) {
        String value = getString(row, colMap, col);
        if (value.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Reads any cell type as a plain String using Apache POI's {@link DataFormatter}.
     * Returns an empty String {@code ""} for null cells.
     *
     * @param cell the Excel cell to read
     * @return formatted cell value as trimmed String
     */
    private String getCellAsString(Cell cell) {
//        return switch (cell.getCellType()) {
//            case STRING  -> cell.getStringCellValue().trim();
//            case NUMERIC -> {
//                double val = cell.getNumericCellValue();
//                yield (val == Math.floor(val)) ? String.valueOf((int) val) : String.valueOf(val); //Math.floor(val) odstrani decimalke.
//            }
//            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
//            case FORMULA -> {   // npr. =A1+B1
//                try { yield String.valueOf((int) cell.getNumericCellValue()); }
//                catch (Exception e) { yield cell.getStringCellValue().trim(); }
//            }
//            default -> "";  //npr. BLANK, UNKNOWN, ERROR
//        };
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    /**
     * Expands designator range notation into a flat comma-separated list.
     * <p>
     * Supported range formats: {@code R1-R5}, {@code R1->R5}, {@code R1>R5}, {@code R1:R5}.
     * Prefix must match on both sides (e.g. {@code R3-C5} is left as-is).
     * Non-range entries are passed through unchanged.
     * <p>
     * E.g.: {@code "R1-R3,C5"} -> {@code "R1,R2,R3,C5"}
     *
     * @param designatorji raw designator string from Excel (comma-separated, may contain ranges)
     * @return expanded comma-separated designator string
     */
    public String checkDesignatorji(String designatorji) {
        //String[] deli = designatorji.split(",");
        List<String> rez = new ArrayList<>();

        for (String del : designatorji.split(",")) {
            del = del.strip();

            Pattern rangeVzorec = Pattern.compile("([A-Za-z]+\\d*(?:[_.\\d+]*)*)\\s*(?:-\\s*>|->|>|:|-)\\s*([A-Za-z]+\\d*(?:[_.\\d+]*)*)");

            Matcher m = rangeVzorec.matcher(del); //objekt (Matcher m), k preverja al niz ustreza temu vzorcu

            if (m.matches()) { //True
                String leva = m.group(1);   //npr. A1
                String desna = m.group(2);  //npr. A5

                Matcher m1 = Pattern.compile("^(.*?)(\\d+)$").matcher(leva);
                Matcher m2 = Pattern.compile("^(.*?)(\\d+)$").matcher(desna);

                if (!m1.matches() || !m2.matches()) {
                    rez.add(del);
                    continue;
                }

                String prefix1 = m1.group(1);
                String prefix2 = m2.group(1);
                int start = Integer.parseInt(m1.group(2));
                int end = Integer.parseInt(m2.group(2));

                if (!prefix1.equals(prefix2)) { //R3-C5 ni smiselno << to preverja
                    rez.add(del);
                    continue;
                }

                for (int i = start; i <= end; i++) { //npr. R3-R5 -> R3, R4, R5
                    rez.add(prefix1 + i);
                }

            } else { //False
                rez.add(del); //toj še vedno List
            }
        }
        return String.join(",", rez); //vred iz Lista združ v String z vejcam
    }

    /**
     * Counts the number of designators in a comma-separated designator string.
     * Used for determining {@link BomItem#setDesignatorMismatch(boolean)}.
     *
     * @param designatorji comma-separated designator string
     * @return number of designators, or {@code 0} if null or empty
     */
    private int countDesignatorji(String designatorji){
        if (designatorji == null || designatorji.isEmpty()) return 0;
        return designatorji.split(",").length;
    }
}