package com.ksenija.classification;

import org.springframework.stereotype.Service;

/**
 * Classifies BOM components into the Largo classification system.
 * <p>
 * Each component is classified into two fields written to {@code MaticniPodatki}:
 * <ul>
 *   <li>{@code MpOznKlas}; classification path string from {@code KlasOzn.KLoOpis}
 *       (250 chars total, each path segment padded to 25 chars)</li>
 *   <li>{@code MpOzBlagSkup}; commodity group ID from {@code BlagSkup.BsSifra}</li>
 * </ul>
 * <p>
 * Detection is delegated to {@link ComponentDetector}, which determines
 * the {@link ComponentType} and mounting type (SMD/THT) from the description and taxonomy.
 */
@Service
public class ClassificationService {

    private final ComponentDetector detector;

    /**
     * Constructor injection; Creates the service with the given component detector.
     * Spring injects this automatically via constructor injection.
     *
     * @param detector detects {@link ComponentType} and mounting type (SMD/THT)
     *                 from component description and taxonomy strings
     */
    public ClassificationService(ComponentDetector detector) {
        this.detector = detector;
    }

    // =========================================================
    // Classification rezultat holder
    // =========================================================

    /**
     * Holds the result of a component classification.
     * <p>
     * {@code static} so it can be used directly as {@code ClassificationService.Classification}
     * without needing an instance of {@link ClassificationService}.
     */
    public static class Classification {
        public final int blagSkupSifra;
        public final String klasOpisPath;

        public Classification(int blagSkupSifra, String klasOpisPath) {
            this.blagSkupSifra = blagSkupSifra;
            this.klasOpisPath = klasOpisPath;
        }
    }
    // =========================================================
    // Path builder helpers
    // =========================================================

    /**
     * Pads a path segment to exactly 25 chars.
     *
     * @param text segment text
     * @return left-aligned string padded to 25 chars
     */
    private static String seg(String text){ // Vsak segment nej bo 25 char dolg, skp 250
        return String.format("%-25s", text);
    }

    /**
     * Builds a full 250-char classification path from individual segments.
     * Each segment is padded to 25 chars; the result is space-padded to 250.
     *
     * @param segments path segments (e.g. "/MATERIAL", "/SMD", "/UPOR")
     * @return 250-character classification path string
     */
    private static String path(String... segments){
        StringBuilder sb = new StringBuilder();
        for (String s : segments) sb.append(seg(s));
        while (sb.length() < 250) sb.append(" ");
        return sb.toString();
    }

    // =========================================================
    // Pre-definirane classifications
    // =========================================================

    // SMD
    private static final Classification SMD_UPOR        = new Classification(11, path("/MATERIAL", "/SMD", "/UPOR"));
    private static final Classification SMD_KONDENZATOR = new Classification(12, path("/MATERIAL", "/SMD", "/KONDENZATOR"));
    private static final Classification SMD_IC          = new Classification(10, path("/MATERIAL", "/SMD", "/INTEGRIRANO VEZJE"));
    private static final Classification SMD_DIODA       = new Classification(14, path("/MATERIAL", "/SMD", "/DIODA"));
    private static final Classification SMD_LED         = new Classification(14, path("/MATERIAL", "/SMD", "/LED DIODA"));
    private static final Classification SMD_TRANZISTOR  = new Classification(10, path("/MATERIAL", "/SMD", "/TRANZISTOR"));
    private static final Classification SMD_KONEKTOR    = new Classification(7,  path("/MATERIAL", "/SMD", "/KONEKTOR"));
    private static final Classification SMD_VAROVALKA   = new Classification(30, path("/MATERIAL", "/SMD", "/VAROVALKA"));
    private static final Classification SMD_INDUKTOR    = new Classification(18, path("/MATERIAL", "/SMD"));
    private static final Classification SMD_QUARTZ      = new Classification(16, path("/MATERIAL", "/SMD"));
    private static final Classification SMD_GENERIC     = new Classification(30, path("/MATERIAL", "/SMD"));

    // THT
    private static final Classification THT_UPOR        = new Classification(11, path("/MATERIAL", "/THT", "/UPOR"));
    private static final Classification THT_KONDENZATOR = new Classification(12, path("/MATERIAL", "/THT", "/KONDENZATOR"));
    private static final Classification THT_IC          = new Classification(10, path("/MATERIAL", "/THT", "/INTERGRIRANO VEZJE"));
    private static final Classification THT_DIODA       = new Classification(14, path("/MATERIAL", "/THT", "/DIODA"));
    private static final Classification THT_LED         = new Classification(14, path("/MATERIAL", "/THT", "/LED DIODA"));
    private static final Classification THT_KONEKTOR    = new Classification(7,  path("/MATERIAL", "/THT", "/KONEKTOR"));
    private static final Classification THT_TULJAVA     = new Classification(18, path("/MATERIAL", "/THT", "/TULJAVA"));
    private static final Classification THT_QUARTZ      = new Classification(16, path("/MATERIAL", "/THT", "/QUARTZ"));
    private static final Classification THT_STIKALO     = new Classification(30, path("/MATERIAL", "/THT", "/STIKALO"));
    private static final Classification THT_LETVICA     = new Classification(7,  path("/MATERIAL", "/THT", "/LETVICA"));
    private static final Classification THT_GENERIC     = new Classification(30, path("/MATERIAL", "/THT"));

    // Posebne
    private static final Classification TIV             = new Classification(8,  path("/TIV"));
    private static final Classification NIC             = new Classification(8,  path(""));



    // =========================================================
    // Main classification metoda: detect type, return classification
    // =========================================================
    /**
     * Classifies a component based on its description, MPN, and taxonomy.
     * <p>
     * Detection is delegated to {@link ComponentDetector}.
     * Special case: if a LED or Diode description contains "driver", it is classified as an IC instead.
     *
     * @param description English component description
     * @param mpn         Manufacturer Part Number
     * @param taxonomy    component category string
     * @return {@link Classification} with commodity group ID and classification path
     */
    public Classification classify(String description, String mpn, String taxonomy) {
        ComponentType type = detector.detect(description, taxonomy);
        boolean isTht = detector.isTht(description, taxonomy);
        System.out.println("CLASSIFY: type=" + type + " isTht=" + isTht
                + " desc='" + description + "' tax='" + taxonomy + "'");

        // LED in Diode: if description contains "driver" -> IC instead
        if (type == ComponentType.LED){
            String d = (description != null) ? description.toLowerCase() : "";
            if (d.contains("driver")) return isTht ? THT_IC : SMD_IC;
        }
        if (type == ComponentType.DIODE){
            String d = (description != null) ? description.toLowerCase() : "";
            if (d.contains("driver")) return isTht ? THT_IC : SMD_IC;
        }
        return switch (type) { // npr. 'ComponentType.CONNECTOR'
            case RESISTOR               -> isTht ? THT_UPOR        : SMD_UPOR;
            case CAPACITOR              -> isTht ? THT_KONDENZATOR : SMD_KONDENZATOR;
            case LED                    -> isTht ? THT_LED         : SMD_LED;
            case DIODE                  -> isTht ? THT_DIODA       : SMD_DIODA;
            case MOSFET                 -> isTht ? THT_IC          : SMD_TRANZISTOR;
            case IC                     -> isTht ? THT_IC          : SMD_IC;
            case CONNECTOR              -> isTht ? THT_KONEKTOR    : SMD_KONEKTOR;
            case HEADER                 ->         THT_LETVICA;
            case INDUCTOR               -> isTht ? THT_TULJAVA     : SMD_INDUKTOR;
            case CRYSTAL, OSCILLATOR    -> isTht ? THT_QUARTZ      : SMD_QUARTZ;
            case FUSE                   ->         SMD_VAROVALKA;
            case SWITCH                 -> isTht ? THT_STIKALO     : SMD_GENERIC;
            case PCB                    ->         TIV;
            case UNKNOWN,VTICNICA,EMBALAZA,VIJAK,MECHANICAL,LEPILO  ->  isTht ? THT_GENERIC     : SMD_GENERIC;
        };
    }
}