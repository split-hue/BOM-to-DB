package com.ksenija.model;

import com.ksenija.service.BomService;
import jakarta.persistence.*;

/**
 * JPA entity representing a row in the {@code Kosovnica} (Bill of Materials) table.
 * <p>
 * Each row links a parent assembly ({@code KoNadSifMp}) to a child component ({@code KoPodSifMp})
 * with a required quantity and scrap factor. The full BOM for a product is the set of all
 * rows sharing the same {@code KoNadSifMp}.
 * <p>
 * Only a subset of fields is written during BOM import — see {@link BomService#importBom}.
 * The remaining fields are present to fully reflect the database table structure.
 */
@Entity
@Table(name = "Kosovnica")
public class Kosovnica {

    /** Primary key — unique record identifier. Auto-incremented during import. */
    @Id
    @Column(name = "KoStZapisa")
    private Integer koStZapisa;

    /** {@code MpSifra} of the parent assembly (the product this BOM belongs to). */
    @Column(name = "KoNadSifMp")
    private Integer koNadSifMp;

    /** {@code MpSifra} of the child component. */
    @Column(name = "KoPodSifMp")
    private Integer koPodSifMp;

    /** Required quantity of the child component per one assembly. */
    @Column(name = "KoKolMateriala")
    private Double koKolMateriala;

    /** Scrap factor multiplier (e.g. 1.05 = 5% scrap). Stored as a ratio, not a percentage. */
    @Column(name = "KoIzmet")
    private Double koIzmet;

    /** - */
    @Column(name = "KoKolMaterialaEM2")
    private Double koKolMaterialaEM2;

    /** Sequence number, determines display order in the BOM (increments by 10). */
    @Column(name = "KoInfZapSt")
    private Integer koInfZapSt;

    /** - */
    @Column(name = "KoCasDo")
    private Double koCasDo;

    /** - */
    @Column(name = "KoDatVelOd")
    private java.time.LocalDateTime koDatVelOd;

    /** Date from which this BOM row is valid. Set to {@code GETDATE()} on import. */
    @Column(name = "KoDatVelDo")
    private java.time.LocalDateTime koDatVelDo;

    @Column(name = "KoOpomba", length = 200)
    private String koOpomba;

    /** Secondary note, used to store the component designator(s) during import. */
    @Column(name = "KoOpomba2", length = 70)
    private String koOpomba2;

    @Column(name = "KoSeRezervira")
    private Integer koSeRezervira;

    @Column(name = "KoKolNadrMp")
    private Double koKolNadrMp;

    @Column(name = "KoKolPodrMp")
    private Double koKolPodrMp;

    @Column(name = "KoKolPodrMpEM2")
    private Double koKolPodrMpEM2;

    @Column(name = "KoNacinZaokr")
    private Integer koNacinZaokr;

    @Column(name = "KoZaokrDec")
    private Integer koZaokrDec;

    @Column(name = "KoDelPos")
    private Integer koDelPos;

    @Column(name = "KoDostNaLok", length = 12)
    private String koDostNaLok;

    @Column(name = "KoOdgorek")
    private Double koOdgorek;

    @Column(name = "KoOdpadek")
    private Double koOdpadek;

    @Column(name = "KoSeNaroca")
    private Integer koSeNaroca;

    @Column(name = "KoTipKos")
    private Integer koTipKos;

    /** BOM variant identifier. Set to {@code "*"} (all variants) on import. */
    @Column(name = "KoVarianta", length = 35)
    private String koVarianta;

    @Column(name = "KoModul", length = 35)
    private String koModul;

    @Column(name = "KoVarVOSled")
    private Integer koVarVOSled;

    @Column(name = "KoVariable")
    private Integer koVariable;

    @Column(name = "KoMera1")
    private Double koMera1;

    @Column(name = "KoMera2")
    private Double koMera2;

    @Column(name = "KoMera3")
    private Double koMera3;

    @Column(name = "KoPodNazMp", length = 255)
    private String koPodNazMp;

    @Column(name = "KoLCena")
    private Double koLCena;

    @Column(name = "KoVerzija")
    private Integer koVerzija;

    @Column(name = "KoVerLUpd")
    private Integer koVerLUpd;

    /** Sub-variant identifier. Set to {@code "*"} (all variants) on import. */
    @Column(name = "KoVariantaPod", length = 35)
    private String koVariantaPod;

    @Column(name = "KoTehPod", length = 35)
    private String koTehPod;

    /** Status code. Set to {@code "A "} (active) on import*/
    @Column(name = "KoSifStat", length = 2)
    private String koSifStat;

    @Column(name = "KoOzAlter", length = 35)
    private String koOzAlter;

    @Column(name = "KoPodSifMpZam")
    private Integer koPodSifMpZam;

    /** Name of the operator who created the record. Set to "{@code BOM_IMPORT}" on import. */
    @Column(name = "NameOper", length = 20)
    private String nameOper;

    /** Timestamp of record creation. Set to {@code GETDATE()} on import. */
    @Column(name = "Datum")
    private java.time.LocalDateTime datum;

    // =========================================================
    // Getters and Setters
    // =========================================================

    public Integer getKoStZapisa() { return koStZapisa; }
    public void setKoStZapisa(Integer v) { this.koStZapisa = v; }

    public Integer getKoNadSifMp() { return koNadSifMp; }
    public void setKoNadSifMp(Integer v) { this.koNadSifMp = v; }

    public Integer getKoPodSifMp() { return koPodSifMp; }
    public void setKoPodSifMp(Integer v) { this.koPodSifMp = v; }

    public Double getKoKolMateriala() { return koKolMateriala; }
    public void setKoKolMateriala(Double v) { this.koKolMateriala = v; }

    public Double getKoIzmet() { return koIzmet; }
    public void setKoIzmet(Double v) { this.koIzmet = v; }

    public Double getKoKolMaterialaEM2() { return koKolMaterialaEM2; }
    public void setKoKolMaterialaEM2(Double v) { this.koKolMaterialaEM2 = v; }

    public Integer getKoInfZapSt() { return koInfZapSt; }
    public void setKoInfZapSt(Integer v) { this.koInfZapSt = v; }

    public Double getKoCasDo() { return koCasDo; }
    public void setKoCasDo(Double v) { this.koCasDo = v; }

    public java.time.LocalDateTime getKoDatVelOd() { return koDatVelOd; }
    public void setKoDatVelOd(java.time.LocalDateTime v) { this.koDatVelOd = v; }

    public java.time.LocalDateTime getKoDatVelDo() { return koDatVelDo; }
    public void setKoDatVelDo(java.time.LocalDateTime v) { this.koDatVelDo = v; }

    public String getKoOpomba() { return koOpomba; }
    public void setKoOpomba(String v) { this.koOpomba = v; }

    public String getKoOpomba2() { return koOpomba2; }
    public void setKoOpomba2(String v) { this.koOpomba2 = v; }

    public Integer getKoSeRezervira() { return koSeRezervira; }
    public void setKoSeRezervira(Integer v) { this.koSeRezervira = v; }

    public Double getKoKolNadrMp() { return koKolNadrMp; }
    public void setKoKolNadrMp(Double v) { this.koKolNadrMp = v; }

    public Double getKoKolPodrMp() { return koKolPodrMp; }
    public void setKoKolPodrMp(Double v) { this.koKolPodrMp = v; }

    public Double getKoKolPodrMpEM2() { return koKolPodrMpEM2; }
    public void setKoKolPodrMpEM2(Double v) { this.koKolPodrMpEM2 = v; }

    public Integer getKoNacinZaokr() { return koNacinZaokr; }
    public void setKoNacinZaokr(Integer v) { this.koNacinZaokr = v; }

    public Integer getKoZaokrDec() { return koZaokrDec; }
    public void setKoZaokrDec(Integer v) { this.koZaokrDec = v; }

    public Integer getKoDelPos() { return koDelPos; }
    public void setKoDelPos(Integer v) { this.koDelPos = v; }

    public String getKoDostNaLok() { return koDostNaLok; }
    public void setKoDostNaLok(String v) { this.koDostNaLok = v; }

    public Double getKoOdgorek() { return koOdgorek; }
    public void setKoOdgorek(Double v) { this.koOdgorek = v; }

    public Double getKoOdpadek() { return koOdpadek; }
    public void setKoOdpadek(Double v) { this.koOdpadek = v; }

    public Integer getKoSeNaroca() { return koSeNaroca; }
    public void setKoSeNaroca(Integer v) { this.koSeNaroca = v; }

    public Integer getKoTipKos() { return koTipKos; }
    public void setKoTipKos(Integer v) { this.koTipKos = v; }

    public String getKoVarianta() { return koVarianta; }
    public void setKoVarianta(String v) { this.koVarianta = v; }

    public String getKoModul() { return koModul; }
    public void setKoModul(String v) { this.koModul = v; }

    public Integer getKoVarVOSled() { return koVarVOSled; }
    public void setKoVarVOSled(Integer v) { this.koVarVOSled = v; }

    public Integer getKoVariable() { return koVariable; }
    public void setKoVariable(Integer v) { this.koVariable = v; }

    public Double getKoMera1() { return koMera1; }
    public void setKoMera1(Double v) { this.koMera1 = v; }

    public Double getKoMera2() { return koMera2; }
    public void setKoMera2(Double v) { this.koMera2 = v; }

    public Double getKoMera3() { return koMera3; }
    public void setKoMera3(Double v) { this.koMera3 = v; }

    public String getKoPodNazMp() { return koPodNazMp; }
    public void setKoPodNazMp(String v) { this.koPodNazMp = v; }

    public Double getKoLCena() { return koLCena; }
    public void setKoLCena(Double v) { this.koLCena = v; }

    public Integer getKoVerzija() { return koVerzija; }
    public void setKoVerzija(Integer v) { this.koVerzija = v; }

    public Integer getKoVerLUpd() { return koVerLUpd; }
    public void setKoVerLUpd(Integer v) { this.koVerLUpd = v; }

    public String getKoVariantaPod() { return koVariantaPod; }
    public void setKoVariantaPod(String v) { this.koVariantaPod = v; }

    public String getKoTehPod() { return koTehPod; }
    public void setKoTehPod(String v) { this.koTehPod = v; }

    public String getKoSifStat() { return koSifStat; }
    public void setKoSifStat(String v) { this.koSifStat = v; }

    public String getKoOzAlter() { return koOzAlter; }
    public void setKoOzAlter(String v) { this.koOzAlter = v; }

    public Integer getKoPodSifMpZam() { return koPodSifMpZam; }
    public void setKoPodSifMpZam(Integer v) { this.koPodSifMpZam = v; }

    public String getNameOper() { return nameOper; }
    public void setNameOper(String v) { this.nameOper = v; }

    public java.time.LocalDateTime getDatum() { return datum; }
    public void setDatum(java.time.LocalDateTime v) { this.datum = v; }
}