package com.ksenija.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single row from an Excel BOM (Bill of Materials) file.
 * <p>
 * Each {@code BomItem} corresponds to one component in the bill of materials.
 * After the Excel file is uploaded, each item is checked against the database,
 * auto-translated to Slovenian, and classified before the user confirms the import.
 */

public class BomItem {

    /** Manufacturer Part Number - unique identifier (id) assigned by the it's component manufacturer (e.g. RC0603FR-071KL). */
    private String mpn;

    /** English component description from internal program CalcuQuote (e.g. 1uF 50V 0805 C0805). */
    private String description;

    /** Quantity of this component needed per assembled product (e.g. 8.0). */
    private Double qty;

    /** Scrap in percent - how many components are expected to be wasted during assembly (e.g. 2.0). */
    private Double izmet;

    /** PCB reference designators where this component is placed (e.g. R1, C2, U5). */
    private String designator;

    /** Name of the component manufacturer. */
    private String manufacturer;

    /** Component category (e.g. Resistors - SMD). */
    private String taxonomy;

    /** Harmonized Tariff Schedule code used for customs declarations. */
    private String htsCode;

    /** 2-3 chars of Country of origin (e.g. "CN", "DE"). */
    private String countryOrigin;

    /** URL to the component datasheet. */
    private String datasheetUrl;

    /** Lead time in weeks. */
    private Double leadTime;

    /** Name of the component supplier. */
    private String supplier;


    // -------------------------------------------------------------------------
    // Status after DB check
    // -------------------------------------------------------------------------
    /** {@code true} if this MPN was found in the database (MaticniPodatki table). */
    private boolean existsInDb;

    /** Database article ID (MpSifra) if found. {@code null} if not found or not yet selected. */
    private Integer dbSifra;

    /** Database article name (MpNaziv) if found. */
    private String dbNaziv;


    // -------------------------------------------------------------------------
    // Translated Slovenian names (editable by user before import)
    // -------------------------------------------------------------------------
    /** Short Slovenian name (35 char) -> saved to MpNaziv. */
    private String nazivSlo;

    /** Long Slovenian name (60 char) -> saved to MpDoNaziv. */
    private String doNazivSlo;


    // -------------------------------------------------------------------------
    // Classification
    // -------------------------------------------------------------------------
    /** Classification path for the article (e.g. Electronic material, Resistor). */
    private String mpOznKlas;

    /** Commodity group code as a String (e.g. 9). */
    private String mpOzBlagSkup;


    // -------------------------------------------------------------------------
    // UI validation state
    // -------------------------------------------------------------------------
    /**
     * {@code true} if the number of designators does not match the quantity.
     * Example: qty=3 but designators are only "R1,R2" -> mismatch.
     */
    private boolean designatorMismatch;

    /**
     * All database matches for one (.this) MPN when more than one is found in DB.
     * One is uato-selected, but the user can manually select different one from a dropdown menu in the grid.
     */
    private List<MaticniPodatek> dbKandidati = new ArrayList<>();







    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getMpn() { return mpn; }
    public void setMpn(String mpn) { this.mpn = mpn; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    public Double getIzmet() { return izmet; }
    public void setIzmet(Double izmet) { this.izmet = izmet; }

    public String getDesignator() { return designator; }
    public void setDesignator(String designator) { this.designator = designator; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getTaxonomy() { return taxonomy; }
    public void setTaxonomy(String taxonomy) { this.taxonomy = taxonomy; }

    public String getHtsCode() { return htsCode; }
    public void setHtsCode(String htsCode) { this.htsCode = htsCode; }

    public String getCountryOrigin() { return countryOrigin; }
    public void setCountryOrigin(String countryOrigin) { this.countryOrigin = countryOrigin; }

    public String getDatasheetUrl() { return datasheetUrl; }
    public void setDatasheetUrl(String datasheetUrl) { this.datasheetUrl = datasheetUrl; }

    public Double getLeadTime() { return leadTime; }
    public void setLeadTime(Double leadTime) { this.leadTime = leadTime; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public boolean isExistsInDb() { return existsInDb; }
    public void setExistsInDb(boolean existsInDb) { this.existsInDb = existsInDb; }

    public Integer getDbSifra() { return dbSifra; }
    public void setDbSifra(Integer dbSifra) { this.dbSifra = dbSifra; }

    public String getDbNaziv() { return dbNaziv; }
    public void setDbNaziv(String dbNaziv) { this.dbNaziv = dbNaziv; }

    public String getNazivSlo() { return nazivSlo; }
    public void setNazivSlo(String nazivSlo) { this.nazivSlo = nazivSlo; }

    public String getDoNazivSlo() { return doNazivSlo; }
    public void setDoNazivSlo(String doNazivSlo) { this.doNazivSlo = doNazivSlo; }

    public String getMpOznKlas() { return mpOznKlas; }
    public void setMpOznKlas(String mpOznKlas) { this.mpOznKlas = mpOznKlas; }

    public String getMpOzBlagSkup() { return mpOzBlagSkup; }
    public void setMpOzBlagSkup(String mpOzBlagSkup) { this.mpOzBlagSkup = mpOzBlagSkup; }

//--------pomožne za shranjevat stanja
    public boolean isDesignatorMismatch() {return designatorMismatch;}
    public void setDesignatorMismatch(boolean designatorMismatch) {this.designatorMismatch = designatorMismatch;}


    public List<MaticniPodatek> getDbKandidati() {return dbKandidati;}
    public void setDbKandidati(List<MaticniPodatek> dbKandidati) {this.dbKandidati = dbKandidati;}

}