package com.ksenija.model;

import jakarta.persistence.*;

/**
 * JPA entity representing a row in the {@code MaticniPodatkiMISC} table.
 * <p>
 * Used to store extra numeric data per Kosovnica row that does not fit
 * in the main {@link MaticniPodatek} or {@link Kosovnica} tables.
 * <p>
 * Fields written during BOM import:
 * <ul>
 *   <li>{@code MPM_SifMp}         - {@code MpSifra} of the component article</li>
 *   <li>{@code MPM_VrednostNUM01} - lead quantity (odgorki / Lead Qty from Excel)</li>
 *   <li>{@code MPM_VrednostINT01} - {@code KoStZapisa} of the matching Kosovnica row</li>
 * </ul>
 */
@Entity
@Table(name = "MaticniPodatkiMISC")
public class MaticniPodatekMisc {

    /** Primary key */
    @Id
    @Column(name = "MPM_ID_MPM")
    private Integer mpmIdMpm;

    /** {@code MpSifra} of the component article this MISC row belongs to. */
    @Column(name = "MPM_MpSifra")
    private Integer mpmMpSifra;

    /**
     * Lead quantity (odgorki): read from the {@code Lead Qty} column in the Excel BOM.
     * Stored as a numeric value.
     */
    @Column(name = "MPM_VrednostNUM01")
    private Double mpmVrednostNum01;

    /**
     * {@code KoStZapisa} of the Kosovnica row this component belongs to.
     * Links the MISC record back to its BOM entry.
     */
    @Column(name = "MPM_VrednostINT01")
    private Integer mpmVrednostInt01;

    /**
     * Have to store this as default 'FIXIZM'
     */
    @Column(name = "MPM_Namen")
    private String mpmNamen;

    // =========================================================
    // Getters and Setters
    // =========================================================

    public Integer getMpmIdMpm() { return mpmIdMpm; }
    public void setMpmIdMpm(Integer mpmIdMpm) { this.mpmIdMpm = mpmIdMpm; }

    public Integer getMpmMpSifra() { return mpmMpSifra; }
    public void setMpmMpSifra(Integer mpmMpSifra) { this.mpmMpSifra = mpmMpSifra; }

    public Double getMpmVrednostNum01() { return mpmVrednostNum01; }
    public void setMpmVrednostNum01(Double v) { this.mpmVrednostNum01 = v; }

    public Integer getMpmVrednostInt01() { return mpmVrednostInt01; }
    public void setMpmVrednostInt01(Integer v) { this.mpmVrednostInt01 = v; }

    public String getMpmNamen() { return mpmNamen;}
    public void setMpmNamen(String mpmNamen) { this.mpmNamen = mpmNamen;}
}