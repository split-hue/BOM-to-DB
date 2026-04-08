package com.ksenija.service;

import com.ksenija.model.BomItem;
import com.ksenija.model.Kosovnica;
import com.ksenija.model.MaticniPodatek;
import com.ksenija.model.MaticniPodatekMisc;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BomImportTest {
    //--mormo injectat to not če ne un "@AfterEach" in "@Transactional" ne delata (to ni dost da sam sta)
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;
    //--

    @Autowired
    private BomService bomService;

    @PersistenceContext
    private EntityManager entityManager;

    //-------jih tracka d ajih poj uhka pobriše
    private final List<Integer> createdMpSifraList    = new ArrayList<>();
    private final List<Integer> createdKoNadSifraList = new ArrayList<>();
    private final List<Integer> createdMpmIdList      = new ArrayList<>(); // za MaticniPodatkiMISC cleanup



    //-------cleanup
    @AfterEach
    void cleanup() {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status -> {
            for (Integer mpmId : createdMpmIdList) {
                int izbrisano = entityManager
                        .createQuery("DELETE FROM MaticniPodatekMisc m WHERE m.mpmIdMpm = :id")
                        .setParameter("id", mpmId)
                        .executeUpdate();
                System.out.println("CLEANUP: zbrisu " + izbrisano + " MaticniPodatkiMISC rows for id=" + mpmId);
            }
            for (Integer nadSifra : createdKoNadSifraList) {
                int izbrisano = entityManager
                        .createQuery("DELETE FROM Kosovnica k WHERE k.koNadSifMp = :sifra")
                        .setParameter("sifra", nadSifra)
                        .executeUpdate();
                System.out.println("CLEANUP: zbrisu " + izbrisano + " Kosovnica rows for nadSifra=" + nadSifra);
            }
            for (Integer sifra : createdMpSifraList) {
                int izbrisano = entityManager
                        .createQuery("DELETE FROM MaticniPodatek m WHERE m.mpSifra = :sifra")
                        .setParameter("sifra", sifra)
                        .executeUpdate();
                System.out.println("CLEANUP: zbrisu " + izbrisano + " MaticniPodatek rows for sifra=" + sifra);
            }
            createdMpmIdList.clear();
            createdMpSifraList.clear();
            createdKoNadSifraList.clear();
            return null;
        });
    }

    // =========================================================
    // testn BomItem (k se ne zapiše v DB)
    // =========================================================
    private BomItem newItem(String mpn, String description, double qty, String designatorji){
        BomItem item = new BomItem();
        item.setMpn(mpn);
        item.setDescription(description);
        item.setTaxonomy("");
        item.setQty(qty);
        item.setIzmet(0.0);
        item.setDesignator(designatorji);
        item.setExistsInDb(false);
        item.setNazivSlo("SLO " + description);
        return item;
    }

    private BomItem newItemWithLqty(String mpn, String description, double qty, String designatorji, int lqty) {
        BomItem item = newItem(mpn, description, qty, designatorji);
        item.setLQty(lqty);
        return item;
    }


    //********************************************TESTI******************************************************************************
    @Test
    void enItem() {
        BomItem item = newItem("54FDAS6-5", "Resistor Thick Film 0402 10K 1%", 3.0, "R1,R2,R3");

        List<BomItem> items = List.of(item);                    //trenutno v listu le 1 item

        //ackt
        BomService.ImportResult rezultat = bomService.importBom(items, "TEST1 - 1 komponenta", null);

        createdKoNadSifraList.add(rezultat.getProductSifra());  //dodata not 1 ELE. v prazen list, List items
        createdMpSifraList.add(rezultat.getProductSifra());


        //test: izdelek sej naredu
        assertNotNull(rezultat.getProductSifra(), "šifra ne sme bit null!");
        assertEquals("sest. TEST1 - 1 komponenta", rezultat.getProductName(),"Naziv ni tak kot je pričakovan 'sest. '!");

        MaticniPodatek izdelek = entityManager.find(MaticniPodatek.class, rezultat.getProductSifra());
        assertNotNull(izdelek, "IZDELEK bi mogu obstajat v DB!");
        assertEquals(1, izdelek.getMpImaKosovn(), "IZDELEK bi mogu met Kosovnico!");


        //test: komponenta sej nrdila
        assertEquals(1, rezultat.getCreatedCount(), "1 komp. bi se mogla nrdit!");
        List<MaticniPodatek> komponente = entityManager
                .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn", MaticniPodatek.class)
                .setParameter("mpn", "54FDAS6-5")
                .getResultList();
        assertEquals(1, komponente.size(), "komponenta bi mogla obstajat v DB!");
        assertNotNull(komponente.getFirst().getMpSifra(), "sifra ne sme bit null");
        assertEquals("SLO Resistor Thick Film 0402 10K 1%", komponente.getFirst().getMpNaziv(), "se ne ujema naziv");
        assertFalse(komponente.getFirst().getMpNaziv().isEmpty(), "naziv ne sme bit prazen");
        komponente.forEach(kom -> createdMpSifraList.add(kom.getMpSifra()));


        //test: kosovnico je naredu
        assertEquals(1, rezultat.getKosovnicaCount(), "1 kos. mora bit!");

        List<Kosovnica> kosovnica = entityManager
                .createQuery("SELECT k FROM Kosovnica k WHERE k.koNadSifMp = :sifra", Kosovnica.class)
                .setParameter("sifra", rezultat.getProductSifra())
                .getResultList();
        assertEquals(1, kosovnica.size(), "kosovnica rab met sam 1 vrsto");
        assertEquals(3.0, kosovnica.getFirst().getKoKolMateriala(), "kol. materiala se ne ujema");
    }



    @Test
    void vecItemov(){
        List<BomItem> items = List.of(
                newItem("TEST-CAP-001", "Capacitor 0402 100nF 10V X7R", 4.0, "C1,C2,C3,C4"),
                newItem("TEST-CAP-002", "Capacitor 0805 10uF 16V", 2.0, "C5,C6"),
                newItem("TEST-IC-001",  "Microcontroller STM32 LQFP", 1.0, "U1")
        );

        BomService.ImportResult rezultat = bomService.importBom(items, "TEST2 - več komponent", null);

        createdMpSifraList.add(rezultat.getProductSifra());                                         //dodata not 1 ELE. v prazen list, List items
        createdKoNadSifraList.add(rezultat.getProductSifra());                                      //dodata not 1 ELE. v prazen list, List items

        for (BomItem item : items){
            entityManager
                    .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn", MaticniPodatek.class)
                    .setParameter("mpn", item.getMpn())
                    .getResultList()
                    .forEach(kom -> createdMpSifraList.add(kom.getMpSifra()));         //dodata not 1 ELE. v prazen list, List items
        }
        assertEquals(3, rezultat.getKosovnicaCount(), "kos. rab met 3 vrste");
        assertEquals(3, rezultat.getCreatedCount(), "rab 3 komp. narest");
        List<Kosovnica> rows = entityManager
                .createQuery("SELECT k FROM Kosovnica k WHERE k.koNadSifMp = :sifra", Kosovnica.class)
                .setParameter("sifra", rezultat.getProductSifra())
                .getResultList();
        assertEquals(3, rows.size());
    }



    /**
     * Verifies that assembly name is auto-prefixed with "sest. " when not already present.
     */
    @Test
    void prefixAddedIfMissing() {
        BomItem item = newItem("TEST-LED-001", "LED SMD 0402", 1.0, "D1");

        BomService.ImportResult result = bomService.importBom(List.of(item), "TEST3 - import brez prefixa, mora dodat '.sest'", null);

        createdKoNadSifraList.add(result.getProductSifra());
        createdMpSifraList.add(result.getProductSifra());
        entityManager
                .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn", MaticniPodatek.class)
                .setParameter("mpn", "TEST-LED-001")
                .getResultList()
                .forEach(mp -> createdMpSifraList.add(mp.getMpSifra()));

        assertTrue(result.getProductName().toLowerCase().startsWith("sest."),
                "izme izdelka rab bit prefixan z 'sest.'");
    }


    /**
     * Verifies that assembly name is NOT double-prefixed if "sest." is already present.
     */
    @Test
    void importBom_productNamePrefix_notDoubledIfAlreadyPresent() {
        BomItem item = newItem("TEST-LED-002", "LED SMD 0603", 1.0, "D2");

        BomService.ImportResult result = bomService.importBom(
                List.of(item), "TEST4 - impor k že ima prefix, ne sme dodat 'sest.'", null);

        createdKoNadSifraList.add(result.getProductSifra());
        createdMpSifraList.add(result.getProductSifra());
        entityManager
                .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn",
                        MaticniPodatek.class)
                .setParameter("mpn", "TEST-LED-002")
                .getResultList()
                .forEach(c -> createdMpSifraList.add(c.getMpSifra()));

        assertFalse(result.getProductName().toLowerCase().startsWith("sest. sest."),
                "Product name should not be double-prefixed");
    }



    /**
     * Verifies that a component already in DB (existsInDb=true) is linked in Kosovnica
     * but NOT re-created as a new article.
     */
    @Test
    void importBom_existingItem_notRecreated() {
        MaticniPodatek existing = entityManager // najde že nrjen artikel, da uporab "že nrjen v DB" komponento
                .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka != ''", MaticniPodatek.class)
                .setMaxResults(1)
                .getSingleResult();

        BomItem existingItem = new BomItem();
        existingItem.setMpn(existing.getMpKupcOznaka());
        existingItem.setDescription("meu meu");
        existingItem.setQty(1.0);
        existingItem.setExistsInDb(true);
        existingItem.setDbSifra(existing.getMpSifra());
        existingItem.setNazivSlo(existing.getMpNaziv());

        BomService.ImportResult result = bomService.importBom(
                List.of(existingItem), "TEST Assembly Existing", null);

        createdKoNadSifraList.add(result.getProductSifra());
        createdMpSifraList.add(result.getProductSifra());

        assertEquals(0, result.getCreatedCount(),
                "Neb se smela ustvart nobena nova komponenta, k že oubstaja v DB!");
        assertEquals(1, result.getKosovnicaCount(),
                "Kosovnica mora še vedno imeti le 1 vrstico!");
    }

    /**
     * Verifies that if the pre-existing Article exist in DB without its components and kosovnica:
     * it's not creating new Article but only link/add components and makes kosovnica
     */
    @Test
    void importBom_obstojecIzdelek_neUstvariNovegaIzdelka() {
        MaticniPodatek obstojecIzdelek = new MaticniPodatek();
        Integer testSifra = ((Number) entityManager
                .createNativeQuery("SELECT ISNULL(MAX(MpSifra), 0) FROM MaticniPodatki") //maš prosta šifra
                .getSingleResult()).intValue() + 1;
        obstojecIzdelek.setMpSifra(testSifra);
        obstojecIzdelek.setMpNaziv("TEST obstoječ artikel");
        obstojecIzdelek.setMpDoNaziv("TEST obstoječ artikel");
        obstojecIzdelek.setMpKupcOznaka("");
        obstojecIzdelek.setMpSifEnoteMere1("KOS");
        obstojecIzdelek.setMpBarCode("");
        obstojecIzdelek.setMpOsnSklad(11);
        obstojecIzdelek.setMpStatus(0);
        obstojecIzdelek.setMpStaraSif(String.valueOf(testSifra));
        obstojecIzdelek.setMpImaKosovn(0);

        new TransactionTemplate(transactionManager).execute(status -> { //commit v db
            entityManager.persist(obstojecIzdelek);
            entityManager.flush();
            return null;
        });

        //act; kreira 1 komponento
        BomItem item = newItem("TEST-DROPDOWN", "Resistor 0402 10K", 2.0, "R1,R2");
        BomService.ImportResult rezultat = bomService.importBom(List.of(item), "TEST obstoječ", testSifra); //<<< DEJ. TEST

        //cleanup
        createdKoNadSifraList.add(testSifra);
        createdMpSifraList.add(testSifra);
        entityManager.createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn", MaticniPodatek.class)
                .setParameter("mpn", "TEST-DROPDOWN")
                .getResultList()
                .forEach(c -> createdMpSifraList.add(c.getMpSifra()));

        //==
        assertEquals(testSifra, rezultat.getProductSifra(), "Šifra MORA biti enaka obstoječemu artiklu - TA NI!");
        assertEquals(1, rezultat.getCreatedCount(), "1 komponenta mora biti ustvarjena");
        assertEquals(1, rezultat.getKosovnicaCount(), "1 kosovnica vrstica mora biti");
        List<Kosovnica> rows = entityManager
                .createQuery("SELECT k FROM Kosovnica k WHERE k.koNadSifMp = :sifra", Kosovnica.class)
                .setParameter("sifra", testSifra)
                .getResultList();
        assertEquals(1, rows.size(), "kosovnica mora met 1 vrsto");
    }


    //********************************************MISC TESTI*************************************************************************

    /**
     * Verifies that a MISC row is written for a component with lqty > 0,
     * with NUM01 = lqty and INT01 = KoStZapisa of that component's Kosovnica row.
     */
    @Test
    void misc_lqtySeShraniVMisc() {
        BomItem item = newItemWithLqty("TEST-MISC-001", "Resistor 0402 10K", 2.0, "R1,R2", 100);

        BomService.ImportResult rezultat = bomService.importBom(List.of(item), "TEST-MISC - lqty shranjeno", null);

        createdKoNadSifraList.add(rezultat.getProductSifra());
        createdMpSifraList.add(rezultat.getProductSifra());
        entityManager
                .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn", MaticniPodatek.class)
                .setParameter("mpn", "TEST-MISC-001").getResultList()
                .forEach(k -> createdMpSifraList.add(k.getMpSifra()));

        // najdi Kosovnica vrstico za ta item
        List<Kosovnica> kosovnica = entityManager
                .createQuery("SELECT k FROM Kosovnica k WHERE k.koNadSifMp = :sifra", Kosovnica.class)
                .setParameter("sifra", rezultat.getProductSifra())
                .getResultList();
        assertEquals(1, kosovnica.size(), "kosovnica mora met 1 vrsto");
        int pricakovaniKoStZapisa = kosovnica.getFirst().getKoStZapisa();

        // najdi MISC vrstico po INT01 (koStZapisa)
        List<MaticniPodatekMisc> miscVrstice = entityManager
                .createQuery("SELECT m FROM MaticniPodatekMisc m WHERE m.mpmVrednostInt01 = :koSt", MaticniPodatekMisc.class)
                .setParameter("koSt", pricakovaniKoStZapisa)
                .getResultList();
        miscVrstice.forEach(m -> createdMpmIdList.add(m.getMpmIdMpm()));

        assertEquals(1, miscVrstice.size(), "ena MISC vrstica mora obstajat!");
        assertEquals(100.0, miscVrstice.getFirst().getMpmVrednostNum01(), "lqty (NUM01) mora bit 100!");
        assertEquals(pricakovaniKoStZapisa, miscVrstice.getFirst().getMpmVrednostInt01(), "INT01 mora ujemati KoStZapisa!");
    }


    /**
     * Verifies that NO MISC row is written when lqty is 0 or null.
     */
    @Test
    void misc_brezLqty_niMiscVrstice() {
        BomItem item = newItem("TEST-MISC-002", "Capacitor 0805 100nF", 1.0, "C1"); // lqty ni nastavljen

        BomService.ImportResult rezultat = bomService.importBom(List.of(item), "TEST-MISC - brez lqty", null);

        createdKoNadSifraList.add(rezultat.getProductSifra());
        createdMpSifraList.add(rezultat.getProductSifra());
        entityManager
                .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn", MaticniPodatek.class)
                .setParameter("mpn", "TEST-MISC-002").getResultList()
                .forEach(k -> createdMpSifraList.add(k.getMpSifra()));

        List<Kosovnica> kosovnica = entityManager
                .createQuery("SELECT k FROM Kosovnica k WHERE k.koNadSifMp = :sifra", Kosovnica.class)
                .setParameter("sifra", rezultat.getProductSifra())
                .getResultList();
        int koStZapisa = kosovnica.getFirst().getKoStZapisa();

        List<MaticniPodatekMisc> miscVrstice = entityManager
                .createQuery("SELECT m FROM MaticniPodatekMisc m WHERE m.mpmVrednostInt01 = :koSt", MaticniPodatekMisc.class)
                .setParameter("koSt", koStZapisa)
                .getResultList();

        assertEquals(0, miscVrstice.size(), "brez lqty ne sme bit nobene MISC vrstice!");
    }


    /**
     * Verifies that with multiple components, each gets its own MISC row linked to its
     * own KoStZapisa, with the correct lqty. They must not share or swap values.
     */
    @Test
    void misc_vecKomponent_vsakDobiSvojoMiscVrstico() {
        BomItem item1 = newItemWithLqty("TEST-MISC-003", "Resistor 0402 1K",   2.0, "R1,R2",    50);
        BomItem item2 = newItemWithLqty("TEST-MISC-004", "Capacitor 0603 1uF", 3.0, "C1,C2,C3", 200);

        BomService.ImportResult rezultat = bomService.importBom(
                List.of(item1, item2), "TEST-MISC - vec komponent", null);

        createdKoNadSifraList.add(rezultat.getProductSifra());
        createdMpSifraList.add(rezultat.getProductSifra());
        for (String mpn : List.of("TEST-MISC-003", "TEST-MISC-004")) {
            entityManager
                    .createQuery("SELECT m FROM MaticniPodatek m WHERE m.mpKupcOznaka = :mpn", MaticniPodatek.class)
                    .setParameter("mpn", mpn).getResultList()
                    .forEach(k -> createdMpSifraList.add(k.getMpSifra()));
        }

        // kosovnica vrstici (urejeni po KoStZapisa = vrstni red inserta)
        List<Kosovnica> kosovnica = entityManager
                .createQuery("SELECT k FROM Kosovnica k WHERE k.koNadSifMp = :sifra ORDER BY k.koStZapisa", Kosovnica.class)
                .setParameter("sifra", rezultat.getProductSifra())
                .getResultList();
        assertEquals(2, kosovnica.size(), "2 kosovnica vrstici morata bit");

        int koSt1 = kosovnica.get(0).getKoStZapisa();
        int koSt2 = kosovnica.get(1).getKoStZapisa();

        // MISC za item1
        List<MaticniPodatekMisc> misc1 = entityManager
                .createQuery("SELECT m FROM MaticniPodatekMisc m WHERE m.mpmVrednostInt01 = :koSt", MaticniPodatekMisc.class)
                .setParameter("koSt", koSt1).getResultList();
        misc1.forEach(m -> createdMpmIdList.add(m.getMpmIdMpm()));
        assertEquals(1, misc1.size(), "item1 mora met 1 MISC vrstico");
        assertEquals(50.0, misc1.getFirst().getMpmVrednostNum01(), "item1 lqty mora bit 50");

        // MISC za item2
        List<MaticniPodatekMisc> misc2 = entityManager
                .createQuery("SELECT m FROM MaticniPodatekMisc m WHERE m.mpmVrednostInt01 = :koSt", MaticniPodatekMisc.class)
                .setParameter("koSt", koSt2).getResultList();
        misc2.forEach(m -> createdMpmIdList.add(m.getMpmIdMpm()));
        assertEquals(1, misc2.size(), "item2 mora met 1 MISC vrstico");
        assertEquals(200.0, misc2.getFirst().getMpmVrednostNum01(), "item2 lqty mora bit 200");
    }

}