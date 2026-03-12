package com.ksenija;

/**
 * Defines the expected column names in the CalcuQuote Excel BOM.
 * <p>
 * Each constant holds an array of possible header strings (case-insensitive match).
 * Multiple aliases handle variations between Excel exports.
 * <p>
 * Flow:
 *   ExcelColumnDefinition  -> defines what columns we expect
 *   ExcelParser            -> reads the file, finds columns
 *   BomItemMapper          -> converts each row to a BomItem
 *   BomItem                -> the plain data object
 */
public enum ExcelColumnDefinition {
    MPN(            new String[]{"mpn"}),
    DESCRIPTION(    new String[]{"description"}),
    WEB_DESCRIPTION(new String[]{"web description"}),
    QTY(            new String[]{"qty per", "qty"}),
    IZMET(          new String[]{"attr rate %", "attr rate", "scrap rate", "izmet"}),
    DESIGNATOR(     new String[]{"designator", "reference"}),
    MANUFACTURER(   new String[]{"mfgr", "manufacturer"}),
    TAXONOMY(       new String[]{"part class", "taxonomy", "category"}),
    HTS_CODE(       new String[]{"hts code", "hts"}),
    COUNTRY_ORIGIN( new String[]{"country of origin", "country origin", "coo"}),
    DATASHEET_URL(  new String[]{"datasheet", "datasheet url"}),
    LEAD_TIME(      new String[]{"lead time", "leadtime"}),
    SUPPLIER(       new String[]{"last known supplier", "supplier", "source"});


    private final String[] possibleHeaders;

    ExcelColumnDefinition(String[] possibleHeaders) {
        this.possibleHeaders = possibleHeaders;
    }

    /**
     * Returns {@code true} if the given Excel header cell matches any alias for this column.
     * Comparison is case-insensitive and trimmed.
     */
    public boolean matches(String header) {
        if (header == null) return false;
        String lower = header.toLowerCase().trim();
        for (String possible : possibleHeaders) {
            if (lower.equals(possible)) return true;
        }
        return false;
    }
}