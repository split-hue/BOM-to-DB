package com.ksenija.model;

import com.ksenija.classification.ClassificationService;
import com.ksenija.service.BomService;
import jakarta.persistence.*;

/**
 * JPA entity representing a row in the {@code MaticniPodatki} (Master Data) table.
 * <p>
 * Each row is one item in the Largo item master — either a purchased component,
 * a sub-assembly, or a finished product. Both components and assemblies created
 * during BOM import are stored here.
 * <p>
 * Fields marked with {@code // * fill later} are intentionally left empty on import
 * and are expected to be completed manually in Largo afterward.
 * <p>
 * Fields written during import, see {@link BomService#importBom}:
 * {@code MpSifra}, {@code MpNaziv}, {@code MpDoNaziv}, {@code MpKupcOznaka},
 * {@code MpSifEnoteMere1}, {@code MpBarCode}, {@code MpOsnSklad}, {@code MpStatus},
 * {@code MpStaraSif}, {@code MpOznKlas}, {@code MpOzBlagSkup},
 * {@code MpCarinskaTar}, {@code MpDrzavaPorekla}, {@code MpRisbaFName},
 * {@code MpVodCas}, {@code Mp_Custom10}.
 */
@Entity
@Table(name = "MaticniPodatki")
public class MaticniPodatek {

    /** Primary key — unique item ID in Largo. Auto-incremented during import. */
    @Id
    @Column(name = "MpSifra")
    private Integer mpSifra;

    /** Short Slovenian name (max 35 chars). Shown in most Largo views. */
    @Column(name = "MpNaziv", length = 35)
    private String mpNaziv;

    /** Long name/English description (max 60 chars). */
    @Column(name = "MpDoNaziv", length = 60)
    private String mpDoNaziv;

    /** Manufacturer Part Number (MPN). Used as the key for DB lookup during import. */
    @Column(name = "MpKupcOznaka", length = 35)
    private String mpKupcOznaka;

    /**
     * Classification path string from {@code KlasOzn.KLoOpis}.
     * 250 characters total, each path segment padded to 25 characters.
     * Set by {@link ClassificationService}.
     */
    @Column(name = "MpOznKlas", length = 250)
    private String mpOznKlas;

    @Column(name = "MpStRisbe", length = 18)
    private String mpStRisbe;

    @Column(name = "MpStStandarda", length = 18)
    private String mpStStandarda;

    /** Primary unit of measure. Set to {@code "KOS"} (piece) on import. */
    @Column(name = "MpSifEnoteMere1", length = 3)
    private String mpSifEnoteMere1;

    @Column(name = "MpSifEnoteMere2", length = 3)
    private String mpSifEnoteMere2;

    @Column(name = "MpSifEnoteMere3", length = 3)
    private String mpSifEnoteMere3;

    @Column(name = "MpSifPlaKlj", length = 3)
    private String mpSifPlaKlj;

    /** NOT IN USE. Random 18-digit barcode generated during import. */
    @Column(name = "MpBarCode", length = 24)
    private String mpBarCode;

    /** Default warehouse ID. Set to {@code 1} on import. */
    @Column(name = "MpOsnSklad")
    private Integer mpOsnSklad;

    @Column(name = "MpOsnLokacija", length = 12)
    private String mpOsnLokacija;

    @Column(name = "MpABCkoda", length = 1)
    private String mpABCkoda;

    @Column(name = "MpOzPolitNar", length = 3)
    private String mpOzPolitNar;

    @Column(name = "MpKonto", length = 8)
    private String mpKonto;

    /** Item status. Set to {@code 0} (active) on import. */
    @Column(name = "MpStatus")
    private Integer mpStatus;

    @Column(name = "MpOpomba", length = 70)
    private String mpOpomba;

    /** Legacy item code, set to the same value as {@code MpSifra} on import. */
    @Column(name = "MpStaraSif", length = 24)
    private String mpStaraSif;

    @Column(name = "MpSifKarKlj", length = 1)
    private String mpSifKarKlj;

    @Column(name = "MpSifKarKljInt", length = 1)
    private String mpSifKarKljInt;

    /**
     * Commodity group ID from {@code BlagSkup.BsSifra}, stored as a string.
     * Set by {@link ClassificationService}.
     */
    @Column(name = "MpOzBlagSkup", length = 10)
    private String mpOzBlagSkup;

    /** Flag indicating whether this item has a BOM. Set to {@code 1} for assemblies. */
    @Column(name = "MpImaKosovn")
    private Integer mpImaKosovn;

    /** HTS / customs tariff code (max 16 chars). From CalcuQuote. */
    @Column(name = "MpCarinskaTar", length = 16)
    private String mpCarinskaTar;

    /** ISO 3166-1 alpha-2 country of origin code (max 3 chars). */
    @Column(name = "MpDrzavaPorekla", length = 3)
    private String mpDrzavaPorekla;

    @Column(name = "MpVeliStev", length = 12)
    private String mpVeliStev;

    @Column(name = "MpVeliSistem", length = 6)
    private String mpVeliSistem;

    @Column(name = "MpKontoProd", length = 8)
    private String mpKontoProd;

    /** NOT USED. Datasheet URL (max 255 chars). */
    @Column(name = "MpRisbaFName", length = 255)
    private String mpRisbaFName;

    @Column(name = "MpOpis")
    private String mpOpis;

    @Column(name = "MpCenik", length = 1)
    private String mpCenik;

    @Column(name = "MpTipCene", length = 4)
    private String mpTipCene;

    @Column(name = "MpOpisSSlikcami")
    private String mpOpisSSlikcami;

    /** Lead time in ??? days ???. */
    @Column(name = "MpVodCas")
    private Double mpVodCas;

    @Column(name = "MpAktTeh", length = 1)
    private String mpAktTeh;

    /** Manufacturer name (max 255 chars). Stored in custom field {@code Mp_Custom10}. */
    @Column(name = "Mp_Custom10", length = 255)
    private String mpCustom10;

    @Column(name = "NameOper", length = 20)
    private String nameOper;

    @Column(name = "fNameOper", length = 20)
    private String fNameOper;

    @Column(name = "MpSifPlanerja")
    private Integer mpSifPlanerja;

    @Column(name = "MpDavZapSt")
    private Integer mpDavZapSt;

    @Column(name = "MpSifProdSkup")
    private Integer mpSifProdSkup;

    @Column(name = "MpIntrastat")
    private Integer mpIntrastat;

    @Column(name = "MpBrutoTeza")
    private Integer mpBrutoTeza;

    // =========================================================
    // Getters and Setters
    // =========================================================
    public Integer getMpSifra() { return mpSifra; }
    public void setMpSifra(Integer v) { this.mpSifra = v; }

    public String getMpNaziv() { return mpNaziv; }
    public void setMpNaziv(String v) { this.mpNaziv = v; }

    public String getMpDoNaziv() { return mpDoNaziv; }
    public void setMpDoNaziv(String v) { this.mpDoNaziv = v; }

    public String getMpKupcOznaka() { return mpKupcOznaka; }
    public void setMpKupcOznaka(String v) { this.mpKupcOznaka = v; }

    public String getMpOznKlas() { return mpOznKlas; }
    public void setMpOznKlas(String v) { this.mpOznKlas = v; }

    public String getMpStRisbe() { return mpStRisbe; }
    public void setMpStRisbe(String v) { this.mpStRisbe = v; }

    public String getMpStStandarda() { return mpStStandarda; }
    public void setMpStStandarda(String v) { this.mpStStandarda = v; }

    public String getMpSifEnoteMere1() { return mpSifEnoteMere1; }
    public void setMpSifEnoteMere1(String v) { this.mpSifEnoteMere1 = v; }

    public String getMpSifEnoteMere2() { return mpSifEnoteMere2; }
    public void setMpSifEnoteMere2(String v) { this.mpSifEnoteMere2 = v; }

    public String getMpSifEnoteMere3() { return mpSifEnoteMere3; }
    public void setMpSifEnoteMere3(String v) { this.mpSifEnoteMere3 = v; }

    public String getMpSifPlaKlj() { return mpSifPlaKlj; }
    public void setMpSifPlaKlj(String v) { this.mpSifPlaKlj = v; }

    public String getMpBarCode() { return mpBarCode; }
    public void setMpBarCode(String v) { this.mpBarCode = v; }

    public Integer getMpOsnSklad() { return mpOsnSklad; }
    public void setMpOsnSklad(Integer v) { this.mpOsnSklad = v; }

    public String getMpOsnLokacija() { return mpOsnLokacija; }
    public void setMpOsnLokacija(String v) { this.mpOsnLokacija = v; }

    public String getMpABCkoda() { return mpABCkoda; }
    public void setMpABCkoda(String v) { this.mpABCkoda = v; }

    public String getMpOzPolitNar() { return mpOzPolitNar; }
    public void setMpOzPolitNar(String v) { this.mpOzPolitNar = v; }

    public String getMpKonto() { return mpKonto; }
    public void setMpKonto(String v) { this.mpKonto = v; }

    public Integer getMpStatus() { return mpStatus; }
    public void setMpStatus(Integer v) { this.mpStatus = v; }

    public String getMpOpomba() { return mpOpomba; }
    public void setMpOpomba(String v) { this.mpOpomba = v; }

    public String getMpStaraSif() { return mpStaraSif; }
    public void setMpStaraSif(String v) { this.mpStaraSif = v; }

    public String getMpSifKarKlj() { return mpSifKarKlj; }
    public void setMpSifKarKlj(String v) { this.mpSifKarKlj = v; }

    public String getMpSifKarKljInt() { return mpSifKarKljInt; }
    public void setMpSifKarKljInt(String v) { this.mpSifKarKljInt = v; }

    public String getMpOzBlagSkup() { return mpOzBlagSkup; }
    public void setMpOzBlagSkup(String v) { this.mpOzBlagSkup = v; }

    public Integer getMpImaKosovn() { return mpImaKosovn; }
    public void setMpImaKosovn(Integer v) { this.mpImaKosovn = v; }

    public String getMpCarinskaTar() { return mpCarinskaTar; }
    public void setMpCarinskaTar(String v) { this.mpCarinskaTar = v; }

    public String getMpDrzavaPorekla() { return mpDrzavaPorekla; }
    public void setMpDrzavaPorekla(String v) { this.mpDrzavaPorekla = v; }

    public String getMpVeliStev() { return mpVeliStev; }
    public void setMpVeliStev(String v) { this.mpVeliStev = v; }

    public String getMpVeliSistem() { return mpVeliSistem; }
    public void setMpVeliSistem(String v) { this.mpVeliSistem = v; }

    public String getMpKontoProd() { return mpKontoProd; }
    public void setMpKontoProd(String v) { this.mpKontoProd = v; }

    public String getMpRisbaFName() { return mpRisbaFName; }
    public void setMpRisbaFName(String v) { this.mpRisbaFName = v; }

    public String getMpOpis() { return mpOpis; }
    public void setMpOpis(String v) { this.mpOpis = v; }

    public String getMpCenik() { return mpCenik; }
    public void setMpCenik(String v) { this.mpCenik = v; }

    public String getMpTipCene() { return mpTipCene; }
    public void setMpTipCene(String v) { this.mpTipCene = v; }

    public String getMpOpisSSlikcami() { return mpOpisSSlikcami; }
    public void setMpOpisSSlikcami(String v) { this.mpOpisSSlikcami = v; }

    public Double getMpVodCas() { return mpVodCas; }
    public void setMpVodCas(Double v) { this.mpVodCas = v; }

    public String getMpAktTeh() { return mpAktTeh; }
    public void setMpAktTeh(String v) { this.mpAktTeh = v; }

    public String getMpCustom10() { return mpCustom10; }
    public void setMpCustom10(String v) { this.mpCustom10 = v; }

    public String getNameOper() { return nameOper; }
    public void setNameOper(String v) { this.nameOper = v; }

    public String getfNameOper() { return fNameOper; }
    public void setfNameOper(String v) { this.fNameOper = v; }

    public Integer getMpSifPlanerja() { return mpSifPlanerja; }
    public void setMpSifPlanerja(Integer v) { this.mpSifPlanerja = v; }

    public Integer getMpDavZapSt() { return mpDavZapSt; }
    public void setMpDavZapSt(Integer v) { this.mpDavZapSt = v; }

    public Integer getMpSifProdSkup() { return mpSifProdSkup; }
    public void setMpSifProdSkup(Integer v) { this.mpSifProdSkup = v; }

    public Integer getMpIntrastat() { return mpIntrastat; }
    public void setMpIntrastat(Integer v) { this.mpIntrastat = v; }

    public Integer getMpBrutoTeza() { return mpBrutoTeza; } //v DB je tip 'numeric'
    public void setMpBrutoTeza(Integer v) { this.mpBrutoTeza = v; }

}