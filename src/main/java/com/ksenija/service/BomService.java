package com.ksenija.service;

import com.ksenija.classification.ClassificationService;
import com.ksenija.model.BomItem;
import com.ksenija.model.Kosovnica;
import com.ksenija.model.MaticniPodatek;
import com.vaadin.pro.licensechecker.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service that orchestrates the two-step BOM import process.
 * <p>
 * Step 1: {@link #checkAgainstDatabase}: called right after Excel upload.
 * Checks deduplicates items by MPN, checks each against the DB,
 * auto-translates new items to Slovenian, and classifies them.
 * <p>
 * Step 2: {@link #importBom}: called when the user confirms the import.
 * Creates the parent assembly article, creates new component articles,
 * and writes all Kosovnica (BOM) rows to the DB.
 */
@Service                                    // tells Spring "manage this class". Spring creates one instance and shares it everywhere.
public class BomService {

    /**
     * JPA EntityManager used for all database operations.
     * Injected automatically by Spring via {@code @PersistenceContext}.
     */
    @PersistenceContext                     // tells Spring to inject it automatically. Everything we persist/flush/query goes through this.
    private EntityManager entityManager;    // JPA object that talks to the DB

                                            // Constructor injection - Spring sees the constructor, finds the matching beans, and injects them.

    private final TranslatorService translatorService;
    private final ClassificationService classificationService;

    /**
     * Constructor injection; Spring finds and provides all three dependencies automatically.
     *
     * @param translatorService      auto-translates English descriptions to Slovenian
     * @param classificationService  classifies components into commodity groups ???
     */
    public BomService(TranslatorService translatorService,
                      ClassificationService classificationService) {
        this.translatorService = translatorService;
        this.classificationService = classificationService;
    }


    // =========================================================
    // STEP 1 — Pre-import check
    // =========================================================
    /**
     * Prepares BOM items for user review before import, that they're going to be shown in the grid.
     * <p>
     * It performs the following in order:
     * <ol>
     *   <li>Finds deduplicates items with the same MPN and merges them into one (sums qty, merges designators, keeps max izmet)</li>
     *   <li>Batch-queries the database for all MPNs at once</li>
     *   <li>For each item: marks it as existing, flags multiple matches, or auto-translates if new</li>
     * </ol>
     *
     * @param items raw list of {@link BomItem}'s objects parsed from Excel
     * @return enriched list ready for display in the review grid
     */
    public List<BomItem> checkAgainstDatabase(List<BomItem> items) {
        items = deduplicatByMpn(items); // 1) preveri za duplikate MPNjev v Excelu

                                        // 1. nabere vse MPNje
        List<String> mpnList = items.stream()
                .map(BomItem::getMpn)
                .filter(mpn -> mpn != null && !mpn.isEmpty())
                .collect(Collectors.toList());

                                        // 2. prever DB k vrne ALL matches za en MPN
        Map<String, List<MaticniPodatek>> dbMap = findAllByMpnList(mpnList);

                                        // 3. update items
        for (BomItem item : items) {
            String mpn = item.getMpn() != null ? item.getMpn().trim() : "";
            List<MaticniPodatek> matches = dbMap.getOrDefault(mpn, List.of());

            if (matches.isEmpty()) {
                // not in DB-> prevedi in klasificiraj
                item.setExistsInDb(false);
                String translated = translatorService.translateShort(item.getDescription(), item.getTaxonomy(), item.getMpn());
                item.setNazivSlo(translated);

                ClassificationService.Classification cls = classificationService.classify(item.getDescription(), item.getMpn(), item.getTaxonomy());
                item.setMpOznKlas(cls.klasOpisPath);
                item.setMpOzBlagSkup(String.valueOf(cls.blagSkupSifra));

            } else if (matches.size() == 1) {
                // točn 1 match-> uporab to direkt
                MaticniPodatek art = matches.getFirst(); //get(0)
                item.setExistsInDb(true);
                item.setDbSifra(art.getMpSifra());
                item.setDbNaziv(art.getMpNaziv());
                item.setNazivSlo(art.getMpNaziv());

            } else {
                // multiple matches-> drop down v gridu
                item.setExistsInDb(true);
                item.setDbKandidati(matches);
                //item.setDbSifra(null);
                //item.setDbNaziv("Izberi iz spustnega seznama");
                //item.setNazivSlo("meu");
                item.setDbSifra(matches.getFirst().getMpSifra());        // pre-izbran
                item.setDbNaziv(matches.getFirst().getMpNaziv());        // pre-izbran
                item.setNazivSlo(matches.get(0).getMpNaziv());           // pre-izbran
            }
        }
        return items;
    }

    /**
     * Merges duplicate BOM rows that share the same MPN into a single item.
     * <p>
     * Merge rules:
     * <ul>
     *   <li>Quantity -> summed</li>
     *   <li>Izmet (scrap rate) -> highest value kept</li>
     *   <li>Designators -> concatenated with a comma</li>
     * </ul>
     * Uses {@link LinkedHashMap} to preserve the original Excel row order (ids consistent).
     *
     * @param items raw list that may contain duplicate MPNs
     * @return deduplicated list with merged MPNs (values)
     */
    private List<BomItem> deduplicatByMpn(List<BomItem> items){
        Map<String, BomItem> zdruzeni = new LinkedHashMap<>(); //(Linked) si zapomni vrstni red vnašanja

        for (BomItem item : items){                     // MPN -> mergean BomItem
            String mpn = item.getMpn();

            if (!zdruzeni.containsKey(mpn)){
                zdruzeni.put(mpn, item);                //če tega MPN-ja še nismo vidl ga dej v Map kukr je
            } else {
                BomItem obstaja = zdruzeni.get(mpn);    //ta MPN smo ŽE vidl! (get the one we already stored so we can merge into it)

                //SUM QTY
                double sumQty = (obstaja.getQty() != null ? obstaja.getQty() : 0)
                        + (item.getQty() != null ? item.getQty() : 0);
                obstaja.setQty(sumQty);

                //obdrži MAX(IZMET)
                if (item.getIzmet() != null && (obstaja.getIzmet() == null || item.getIzmet() > obstaja.getIzmet())){
                    obstaja.setIzmet(item.getIzmet());
                }

                //ZDRUŽI DESIGNATORJE
                String s1 = obstaja.getDesignator() != null ? obstaja.getDesignator() : "";
                String s2 = item.getDesignator() != null ? item.getDesignator() : "";
                if (!s1.isEmpty() && !s2.isEmpty())
                    obstaja.setDesignator(s1 + "," + s2);
                else
                    obstaja.setDesignator(s1.isEmpty() ? s2 : s1);
            }
        }
        return new ArrayList<>(zdruzeni.values());
    }




    // =========================================================
    // STEP 2 — Full import: create product, create new articles,
    //           write Kosovnica rows.                           // rollbackFor = Exception.class means: if ANYTHING goes wrong, roll back everything — no partial imports. This is critical for data integrity.
    // ========================================================= // REQUIRES_NEW means: always start a brand new transaction, even if one already exists.

    /**
     * Performs the full database import after the user confirms with button.
     * <p>
     * Runs in its own transaction ({@code REQUIRES_NEW}).
     * If anything fails, the entire import is rolled back (no partial data is written in DB).
     * <p>
     * Three phases:
     * <ol>
     *   <li>Create the parent assembly article in table MaticniPodatki</li>
     *   <li>Create new component articles for items not yet in the database</li>
     *   <li>Write Kosovnica rows linking the assembly to each component</li>
     * </ol>
     *
     * @param items         reviewed and validated list of BOM components
     * @param productName   name of the assembly (auto-prefixed with "sest. ")
     * @return              {@link ImportResult} with counts and a log of what was created
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ImportResult importBom(List<BomItem> items, String productName,Integer obstojecaSifra) {
        ImportResult result = new ImportResult();   // return
        System.out.println("=== IMPORT START: " + productName + " (" + items.size() + " items) ===");

        // ---1. nared parent product article (assembly)--- IZDELEK vvv
        Integer productSifra;
        if (obstojecaSifra != null) {
            productSifra = obstojecaSifra;
            result.setProductSifra(productSifra);
            result.setProductName(productName);
            result.addLog(":) Uporabljen obstoječ artikel: šifra " + productSifra + "(ne bomo ustvarjali novega)");
        } else {
            productSifra = getNextSifra();
            String fullProductName = productName.toLowerCase().startsWith("sest.") ?
                    productName : "sest. " + productName;
            fullProductName = truncate(fullProductName, 35);



            MaticniPodatek product = new MaticniPodatek();      // MaticniPodatek -> database object for a product

            product.setMpSifra(productSifra);
            product.setMpNaziv(fullProductName);
            product.setMpDoNaziv(fullProductName);
            product.setMpKupcOznaka("");
            product.setMpStRisbe("");
            product.setMpStStandarda("");
            product.setMpSifEnoteMere1("KOS");
            product.setMpSifEnoteMere2("");
            product.setMpSifEnoteMere3("");
            product.setMpSifPlaKlj("1");
            product.setMpBarCode("");
            product.setMpOsnSklad(11);
            product.setMpOsnLokacija("O");
            product.setMpABCkoda("");
            product.setMpOzPolitNar("");
            product.setMpKonto("");
            product.setMpStatus(0);
            product.setMpOpomba("");
            product.setMpStaraSif(String.valueOf(productSifra));
            product.setMpSifKarKlj("I");
            product.setMpSifKarKljInt("I");
            product.setMpOznKlas("");
            product.setMpOzBlagSkup(" ");
            product.setMpImaKosovn(1);// assembly has a BOM
            product.setMpCarinskaTar("");
            product.setMpDrzavaPorekla("");
            product.setMpVeliStev("");
            product.setMpVeliSistem("");
            product.setMpKontoProd("");
            product.setMpRisbaFName("");
            product.setMpOpis("");
            product.setMpCenik("N");
            product.setMpTipCene("DNC");
            product.setMpOpisSSlikcami("");
            product.setMpAktTeh("");       //"B"
            product.setMpVodCas(0.0);
            product.setMpCustom10("");
            product.setNameOper("dbo");
            product.setfNameOper("dbo");
            product.setMpSifPlanerja(81);
            product.setMpDavZapSt(1);
            product.setMpSifProdSkup(5);
            product.setMpIntrastat(0);
            product.setMpBrutoTeza(null);
            product.setMpKolNar(0);
            product.setMpZaokKolNar(0);
            entityManager.persist(product); // VNESE => DB (persist() stages it for insert — not written to DB yet, that happens at flush())

            result.setProductSifra(productSifra);
            result.setProductName(fullProductName);
            result.addLog(":)) Ustvarjen izdelek oz assembly: " + fullProductName + " (šifra " + productSifra + ")");
        }


//-----------------------------------------------------------------------------------------------------------------------
        // ---2. Create new artikel (items not yet in DB)--- COMPONENTA vv
        int nextSifra = productSifra;
        Map<String, Integer> newArticleMap = new HashMap<>();

        // Skips components already in DB. For new ones, increments nextSifra
        // — so articles get sequential IDs right after the assembly:
        // assembly=50590, components=50591, 50592, 50593... <- njihova šifra (comp. majo še nadŠifro 50590)
        for (BomItem item : items) {
            if (item.isExistsInDb()) continue; // če obstaja v DB, next

            nextSifra++;


            System.out.println("CLASSIFY: mpn=" + item.getMpn()
                    + " desc='" + item.getDescription() + "'"
                    + " taxonomy='" + item.getTaxonomy() + "'");

                // Classify component for MpOznKlas and MpOzBlagSkup
            ClassificationService.Classification cls =
                    classificationService.classify(item.getDescription(), item.getMpn(), item.getTaxonomy());

            MaticniPodatek art = new MaticniPodatek();

            art.setMpSifra(nextSifra);
            art.setMpNaziv(truncate(item.getNazivSlo(), 35));
            art.setMpDoNaziv(translatorService.translateLong(item.getDescription(),item.getTaxonomy(), item.getMpn()));
            art.setMpKupcOznaka(truncate(item.getMpn(), 35));
            art.setMpStRisbe("");
            art.setMpStStandarda("");
            art.setMpSifEnoteMere1("KOS");
            art.setMpSifEnoteMere2("");
            art.setMpSifEnoteMere3("");
            art.setMpSifPlaKlj("1");
            art.setMpBarCode("");//product.setMpBarCode(artikelService.generateBarcode());
            art.setMpOsnSklad(isPcb(item.getDescription()) ? 30 : 11); //če je PCB -> 30
            art.setMpOsnLokacija("O");
            art.setMpABCkoda("");
            art.setMpOzPolitNar("");
            art.setMpKonto("");
            art.setMpStatus(0);
            art.setMpOpomba("");
            art.setMpStaraSif(String.valueOf(nextSifra));
            art.setMpSifKarKlj("M");
            art.setMpSifKarKljInt("I");
            art.setMpOznKlas(cls.klasOpisPath);
            art.setMpOzBlagSkup(String.valueOf(cls.blagSkupSifra));
            art.setMpImaKosovn(0);                                  // 8538.90.8180 → 85389081
            art.setMpCarinskaTar(formatHtsCode(item.getHtsCode())); //art.setMpCarinskaTar(truncate(item.getHtsCode(), 16));
            art.setMpDrzavaPorekla(truncate(item.getCountryOrigin(), 3));
            art.setMpVeliStev("");
            art.setMpVeliSistem("");
            art.setMpKontoProd("");
            art.setMpRisbaFName(truncate(item.getDatasheetUrl(), 255));
            art.setMpOpis("");
            art.setMpCenik("N");
            art.setMpTipCene("DNC");
            art.setMpOpisSSlikcami("");
            art.setMpAktTeh("B");
            art.setMpVodCas(item.getLeadTime());
            art.setMpCustom10(truncate(item.getManufacturer(), 255));    // * from Excel
            art.setNameOper("dbo");
            art.setfNameOper("dbo");
            art.setMpSifPlanerja(81);
            art.setMpDavZapSt(1);
            art.setMpSifProdSkup(5);
            art.setMpIntrastat(0);
            art.setMpBrutoTeza(null);
            art.setMpKolNar(item.getLQty());
            art.setMpZaokKolNar(item.getLQty());        // mogoče rabmo kako drugače zaokrožiti?? :)
            entityManager.persist(art);                 // VNESE new comp. => DB

            newArticleMap.put(item.getMpn(), nextSifra);
            result.addLog(":D Nov artikel: " + item.getNazivSlo()
                    + " [" + cls.blagSkupSifra + "] (šifra " + nextSifra + ")");
        }
//--------for loop
        result.setCreatedCount(newArticleMap.size());
        result.addLog(":D :)) Ustvarjenih novih artiklov: " + newArticleMap.size());

            // Flush articles before Kosovnica inserts (FK dependency) !!! sends ALL staged persist() calls to the database as SQL INSERTs
        entityManager.flush();
        System.out.println("=== ARTICLES FLUSHED, starting Kosovnica inserts ===");

                                    // ---3. Write Kosovnica rows---vvv KOSOVNICA
        int koStZapisa     = getNextKoStZapisa();
        int seqNum         = 1;
        int kosovnicaCount = 0;

        for (BomItem item : items) {
            Integer componentSifra = item.isExistsInDb()
                    ? item.getDbSifra()
                    : newArticleMap.get(item.getMpn());

            if (componentSifra == null) {
                result.addLog("! Preskočeno (ni šifre): " + item.getMpn());
                continue;
            }

            double qty   = item.getQty()   != null ? item.getQty()   : 1.0;
            double izmet = item.getIzmet() != null ? item.getIzmet() : 0.0;
            String opomba2 = truncate(item.getDesignator(), 70);

            System.out.println("Kosovnica row " + koStZapisa + ": nad=" + productSifra
                    + " pod=" + componentSifra + " qty=" + qty
                    + " izmet=" + izmet + " designator=" + opomba2);

            Kosovnica kos = new Kosovnica();
            kos.setKoStZapisa(koStZapisa);
            kos.setKoNadSifMp(productSifra);
            kos.setKoPodSifMp(componentSifra);
            kos.setKoKolMateriala(qty);
            kos.setKoKolMaterialaEM2(0.0);
            kos.setKoInfZapSt(seqNum);
            kos.setKoSifStat("A ");
            kos.setKoOzAlter(" ");
            kos.setKoTipKos(0);
            kos.setKoVarianta("*");
            kos.setKoVariantaPod("*");
            kos.setKoModul("");
            kos.setKoOpomba("");
            kos.setKoOpomba2(opomba2);
            kos.setKoSeRezervira(1);
            kos.setKoSeNaroca(1);
            kos.setKoKolNadrMp(1.0);      //qty
            kos.setKoKolPodrMp(qty);
            kos.setKoKolPodrMpEM2(0.0);
            kos.setKoIzmet(izmet);
            kos.setKoCasDo(0.0);
            kos.setKoNacinZaokr(0);
            kos.setKoZaokrDec(0);
            kos.setKoDelPos(0);
            kos.setKoDostNaLok("");
            kos.setKoOdgorek(0.0);
            kos.setKoOdpadek(0.0);
            kos.setKoVarVOSled(0);
            kos.setKoVariable(0);
            kos.setKoMera1(0.0);
            kos.setKoMera2(0.0);
            kos.setKoMera3(0.0);
            kos.setKoPodNazMp("");
            kos.setKoLCena(0.0);
            kos.setKoVerzija(0);
            kos.setKoVerLUpd(0);
            kos.setKoTehPod("");
            kos.setKoPodSifMpZam(0);
            kos.setNameOper("dbo");
            kos.setDatum(java.time.LocalDateTime.now());
            kos.setKoDatVelOd(java.time.LocalDateTime.now());

            entityManager.persist(kos);

            koStZapisa++;
            seqNum++;
            kosovnicaCount++;
        }

        result.setKosovnicaCount(kosovnicaCount);
        result.addLog("✓ Vneseno v kosovnico: " + kosovnicaCount + " vrstic");

        return result;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Returns {@code true} if the description indicates a PCB (Printed Circuit Board).
     * This info used for determining {@code setMpOsnSklad(}; PCBs are stored in warehouse 30 instead of the default 11.
     *
     * @param description English component description
     * @return {@code true} if the component is a PCB
     */
    private boolean isPcb(String description) {
        if (description == null) return false;
        String d = description.toLowerCase().trim();
        return d.contains("pcb") || d.contains("printed circuit");
    }

    /**
     * OPOZORILO: TO NI PRAVI NAČIN PRETVORBE FORMATOV TERIFNIH ŠT.
     * -> UPORABA TE ŠT. NA LSTNO ODGOVORNOST
     * <p>
     * Strips non-digit characters from an HTS code and truncates to 8 digits.
     * E.g. {@code "8538.90.8180"} -> {@code "85389081"}
     *
     * @param hts raw HTS code string from Excel
     * @return cleaned 8-digit customs tariff code, or empty string if null/empty
     */
    private String formatHtsCode(String hts){
        if (hts == null || hts.isEmpty()) return "";
        String digits = hts.replaceAll("[^0-9]", "");
        return truncate(digits, 8);
    }

    /**
     * Returns the next available MpSifra (article ID) from MaticniPodatki.
     * Increments until it finds a value not already present in the DB,
     * to guard against gaps or race conditions.
     *
     * @return next safe, unused MpSifra
     */
    private Integer getNextSifra() {
        Object max = entityManager
                .createNativeQuery("SELECT ISNULL(MAX(MpSifra), 0) FROM MaticniPodatki")
                .getSingleResult();
        int candidate = ((Number) max).intValue() + 1;

        while (true) {
            Long count = (Long) entityManager
                    .createQuery("SELECT COUNT(m) FROM MaticniPodatek m WHERE m.mpSifra = :sifra")
                    .setParameter("sifra", candidate)
                    .getSingleResult();
            if (count == 0) return candidate;
            candidate++;
        }
    }

    /**
     * Returns the next available KoStZapisa (Kosovnica row ID).
     * Increments until it finds a value not already present in the DB.
     *
     * @return next safe, unused KoStZapisa
     */
    private int getNextKoStZapisa() {
        Object max = entityManager
                .createNativeQuery("SELECT ISNULL(MAX(KoStZapisa), 0) FROM Kosovnica")
                .getSingleResult();
        int candidate = ((Number) max).intValue() + 1;

        while (true) {
            Long count = (Long) entityManager
                    .createQuery("SELECT COUNT(k) FROM Kosovnica k WHERE k.koStZapisa = :id")
                    .setParameter("id", candidate)
                    .getSingleResult();
            if (count == 0) return candidate;
            candidate++;
        }
    }


    /**
     * Safely truncates a string to the given maximum length, that
     * prevents DB errors from Strings that are too long. Returns empty String instead of null.
     * Returns an empty string instead of {@code null} to prevent DB errors.
     *
     * @param text input string
     * @param max  maximum allowed length
     * @return truncated string, never {@code null}
     */
    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) : text;
    }
//==================================================================================
    /**
     * Batch-queries the database for all given MPNs in a single query,
     * and groups the results by MPN.
     *
     * @param mpnList list of MPN strings to look up
     * @return map of MPN → list of matching {@link MaticniPodatek} records
     */
    private Map<String, List<MaticniPodatek>> findAllByMpnList(List<String> mpnList) {
        List<MaticniPodatek> found = entityManager
                .createQuery(
                        "SELECT m FROM MaticniPodatek m WHERE TRIM(m.mpKupcOznaka) IN :mpnList",
                        MaticniPodatek.class)
                .setParameter("mpnList", mpnList)
                .getResultList();

        Map<String, List<MaticniPodatek>> result = new HashMap<>();
        for (MaticniPodatek art : found) {
            String mpn = art.getMpKupcOznaka().trim();
            result.computeIfAbsent(mpn, k -> new ArrayList<>()).add(art);
        }
        return result;
    }
//==================================================================================
    // =========================================================
    // IMPORT RESULT (inner class)
    // A simple data container returned to MainView after import. static means
    // =========================================================

    /**
     * Simple data container returned by {@link #importBom} to report, what was created.
     * <p>
     * {@code static} means it can be used without an instance of {@link BomService}.
     * The {@code log} list collects messages (for user to see) for each created article.
     */
    public static class ImportResult {
        private Integer productSifra;           //BomService service = new BomService(...); NENENEN TKO S EN DELA
        private String productName;             //BomService.ImportResult result = service.new ImportResult();
        private int createdCount;
        private int kosovnicaCount;             //BomService.ImportResult result = new BomService.ImportResult(); <<< TKOOO SE DELA
        private final List<String> log = new ArrayList<>();

        public void addLog(String message) { log.add(message); }

        public Integer getProductSifra()       { return productSifra; }
        public void setProductSifra(Integer v) { this.productSifra = v; }

        public String getProductName()         { return productName; }
        public void setProductName(String v)   { this.productName = v; }

        public int getCreatedCount()           { return createdCount; }
        public void setCreatedCount(int v)     { this.createdCount = v; }

        public int getKosovnicaCount()         { return kosovnicaCount; }
        public void setKosovnicaCount(int v)   { this.kosovnicaCount = v; }

        public List<String> getLog()           { return log; }
    }

    // =========================================================
    // SEARCH name of article/assembly
    //
    // =========================================================
    /**
     * Searches for assembly candidates by name.
     * Returns articles that have no Kosovnica rows as a parent (LEFT JOIN WHERE NULL).
     *
     * @param name  search string (partial match, case-insensitive)
     * @param limit max number of results
     * @return list of matching articles
     */
    public List<MaticniPodatek> searchIzdelek(String name, int limit) {
        return entityManager.createQuery(
                        "SELECT m FROM MaticniPodatek m " +
                                "LEFT JOIN Kosovnica k ON m.mpSifra = k.koNadSifMp " +
                                "WHERE k.koNadSifMp IS NULL " +
                                "AND LOWER(m.mpNaziv) LIKE LOWER(:q) " +
                                "ORDER BY m.mpNaziv",
                        MaticniPodatek.class)
                .setParameter("q", "%" + name + "%")
                .setMaxResults(limit)
                .getResultList();
    }
}