package com.ksenija.ui;

import com.ksenija.model.BomItem;
import com.ksenija.model.MaticniPodatek;
import com.ksenija.parser.BomItemMapper;
import com.ksenija.parser.ExcelParser;
import com.ksenija.service.BomService;
import com.ksenija.service.TranslatorService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.shared.TooltipConfiguration;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.Lumo;


import java.util.Comparator;
import java.util.List;

/**
 * Main and only view of the application.
 * <p>
 * Handles the full user workflow:
 * upload an Excel file, preview parsed components in a grid,
 * fix any missing or mismatched designators, and confirm the import into the DB.
 */
@Route("") //root URL
public class MainView extends VerticalLayout {

    /** Parses the uploaded Excel file into a list of {@link BomItem} objects. */
    private final ExcelParser excelParser;              // Excel -> BomItem

    /** Checks items against the database and performs the final import. */
    private final BomService bomService;                // preverja BOM stvari & write v DB

    /** Validates and expands designator strings in real time (e.g. R1-R3 -> R1,R2,R3). */
    private final BomItemMapper bomItemMapper;          // za validacijo designatorjev (ERROR MISMATCH)

    /** Currently loaded and reviewed list of BOM components. {@code null} until a file is uploaded. */
    private List<BomItem> trenutenListKomponent;

    // -------------------------------------------------------------------------
    // UI components
    // -------------------------------------------------------------------------

    /** Input field for the product/assembly name (prepended with "sest. "). */
    private final TextField imeIzdelka = new TextField("Ime izdelka");

    /** Grid displaying all parsed BOM components for review before import. */
    private final Grid<BomItem> grid = new Grid<>(BomItem.class, false);

    /** Button that triggers the final DB import. Disabled until all validation passes. */
    private final Button importButton = new Button("Uvozi v bazo");

    /** Shows component counts: total, in DB, new, mismatches. */
    private final Span statsText = new Span();

    /** Warning message shown when any components are missing designators. */
    private final Span warningLabel = new Span();

    /** Warning message shown ONLY after the BOM import, not on every refreshGrid() */
    private boolean warningsShow = false;


    /**
     * Constructor injection; Creates the view and builds all UI sections.
     *
     * @param excelParser       parses uploaded Excel files
     * @param bomService        handles DB checks and import logic
     * @param bomItemMapper     validates and expands designator strings
     */
    public MainView(ExcelParser excelParser, BomService bomService, BomItemMapper bomItemMapper, TranslatorService translatorService) {
        this.excelParser = excelParser;
        this.bomService = bomService;
        this.bomItemMapper = bomItemMapper;

        translatorService.initBOM(translatorService);
        imeIzdelka.addValueChangeListener(e -> refreshGrid());

        setPadding(false);
        setSpacing(false);

        //UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);
        applyCookie();

        VerticalLayout ostalo = new VerticalLayout( //wrapper za ostalo razen bannerja
                buildHeader(),
                buildUploadSection(),
                buildGrid(),
                buildFooter()
        );
        ostalo.setPadding(true);
        ostalo.setSpacing(true);
        ostalo.setWidthFull();

        add(
                buildBanner(),
                ostalo
        );

    }






    // -------------------------------------------------------------------------
    // UI builders
    // -------------------------------------------------------------------------

    private HorizontalLayout buildBanner() {
        Image logo = new Image("slike/logo-transparent.png", "logo");
        logo.setHeight("60px");

        HorizontalLayout banner = new HorizontalLayout(logo);
        banner.setAlignItems(Alignment.CENTER);
        banner.setWidth("100%");
        banner.setPadding(true);
        banner.getStyle()
                .set("height", "64px");
        banner.setClassName("banner-barva");

        return banner;
    }

    /**
     * Builds the page header with title and subtitle.
     *
     * @return header layout
     */
    private HorizontalLayout buildHeader() {

        H2 naslov = new H2("Uvoz kosovnic v Podatkovno bazo");
        naslov.getStyle().set("cursor", "default");
        H5 podNaslov = new H5("Uvoz Excel dat. BOM (Bill of Materials) iz programa CalcuQuote.");
        podNaslov.getStyle().set("cursor", "default");
        podNaslov.getStyle().set("font-weight", "normal");

        Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
        infoIcon.getStyle().set("cursor", "pointer");

        HorizontalLayout podNaslovInfo = new HorizontalLayout(podNaslov, infoIcon);
        podNaslovInfo.setSpacing(false);
        podNaslovInfo.getStyle().set("gap", "3px");

        Tooltip info = Tooltip.forComponent(infoIcon)
                .withText("CalcuQuote > (RFQ List) 'B' > Actions > Reports > Download")
                .withPosition(Tooltip.TooltipPosition.BOTTOM_END);
        TooltipConfiguration.setDefaultFocusDelay(0);
        TooltipConfiguration.setDefaultHoverDelay(0);
        TooltipConfiguration.setDefaultHideDelay(200);


        VerticalLayout naslovBlock = new VerticalLayout(naslov, podNaslovInfo);
        naslovBlock.setSpacing(false);
        naslovBlock.setPadding(false);
        naslovBlock.getStyle().set("gap", "5px");

        Button temaButton = new Button();
        temaButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        temaButton.getStyle().set("cursor", "pointer");

        updateThemeButton(temaButton);

        temaButton.addClickListener(click -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            if (themeList.contains(Lumo.DARK)) {
                themeList.remove(Lumo.DARK);
                ((Button) click.getSource()).setText("temno");
                saveCookie("light");
            } else {
                themeList.add(Lumo.DARK);
                ((Button) click.getSource()).setText("svetlo");
                saveCookie("dark");
            }
            updateThemeButton(temaButton);
        });

        HorizontalLayout header = new HorizontalLayout(naslovBlock, temaButton);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        return header;
    }

    private void updateThemeButton(Button button) {
        ThemeList themeList = UI.getCurrent().getElement().getThemeList();

        if (themeList.contains(Lumo.DARK)) {
            button.setText("svetlo");
            button.setIcon(VaadinIcon.MOON.create());
        } else {
            button.setText("temno");
            button.setIcon(VaadinIcon.MOON.create());
        }
    }

    /**
     * Builds the upload section with the product name field and Excel file drop/select zone.
     * When a file is uploaded, it is parsed and checked against the database immediately.
     *
     * @return upload section layout
     */
    private HorizontalLayout buildUploadSection() {
        imeIzdelka.setPlaceholder("sest.");
        imeIzdelka.setWidth("300px"); // adjust as needed

        UI ui = UI.getCurrent();

        UploadHandler uploadHandler = click -> {
            try {
                List<BomItem> parsed = excelParser.parse(click.getInputStream());
                List<BomItem> checked = bomService.checkAgainstDatabase(parsed);

                ui.access(() -> {
                    trenutenListKomponent = checked;
                    warningsShow = false;
                    refreshGrid();
                    showNotification("Datoteka naložena: " + trenutenListKomponent.size() + " komponent", false);
                });

            } catch (Exception e) {
                ui.access(() ->
                        showNotification("! Napaka: " + e.getMessage(), true)
                );
            }
        };

        Upload upload = new Upload(uploadHandler);
        upload.setAcceptedFileTypes(".xlsx");
        upload.setMaxFiles(1);
        upload.setDropLabel(new Span("Sem povleci Excel BOM dat. ali klikni za izbiro"));
        upload.setWidth("400px");
        upload.getUploadButton().getStyle().set("cursor", "pointer"); // je notranji gumb, ne nš

        // ime izdelka in upload v -> horizontal layout
        HorizontalLayout horizontalnoZgorej = new HorizontalLayout(imeIzdelka, upload);
        horizontalnoZgorej.setSpacing(true);
        horizontalnoZgorej.getStyle().set("gap", "40px");
        horizontalnoZgorej.setAlignItems(FlexComponent.Alignment.START);
        horizontalnoZgorej.setPadding(true);
        horizontalnoZgorej.getStyle().set("padding-left", "20px");

        return horizontalnoZgorej;
    }

    /**
     * Builds the component review grid with all columns.
     * <p>
     * Columns: Status badge, MPN (with warning icon), English description,
     * Slovenian name (editable or dropdown for multiple DB matches),
     * quantity, scrap rate, and designators (with live validation).
     *
     * @return grid section layout
     */
    private Component buildGrid() {
        H4 gridNaslov = new H4("Pregled komponent");


        grid.getStyle().set("border", "none");
        grid.getStyle().set("--lumo-base-color", "transparent");

        grid.addComponentColumn(item -> {
            VerticalLayout cell = new VerticalLayout();
            cell.setSpacing(false);
            cell.setPadding(false);

// BADGES :)
            Span stanje = new Span(item.isExistsInDb() ? "V bazi" : "Novo");
            stanje.getElement().getThemeList().add(item.isExistsInDb() ?
                    "badge success pill" :
                    "badge warning pill"
            );
            cell.add(stanje);

            return cell;
        }).setHeader("Status").setWidth("110px").setFlexGrow(0);
        
        
        grid.addComponentColumn(item -> {
            HorizontalLayout cell = new HorizontalLayout();
            cell.setAlignItems(Alignment.CENTER);
            cell.setSpacing(true);
            cell.setPadding(false);


            boolean emptyDesignator = item.getDesignator() == null || item.getDesignator().isEmpty();
            boolean mismatch = item.isDesignatorMismatch();

            if (emptyDesignator) {
                Icon warn = VaadinIcon.WARNING.create();
                warn.setColor("var(--lumo-error-color)");
                warn.setSize("16px");
                cell.add(warn);

                Tooltip info = Tooltip.forComponent(warn)
                        .withText("Št. designatorjev (Designatorji) je prazno")
                        .withPosition(Tooltip.TooltipPosition.BOTTOM_END);
                TooltipConfiguration.setDefaultFocusDelay(0);
                TooltipConfiguration.setDefaultHoverDelay(0);
                TooltipConfiguration.setDefaultHideDelay(200);
            } else if (mismatch) {
                Icon warn = VaadinIcon.WARNING.create();
                warn.setColor("var(--lumo-warning-color)");
                warn.setSize("16px");
                cell.add(warn);

                Tooltip info = Tooltip.forComponent(warn)
                        .withText("Št. designatorjev (Designatorji) se ne ujema s količino (Kol.)")
                        .withPosition(Tooltip.TooltipPosition.BOTTOM_END);
                TooltipConfiguration.setDefaultFocusDelay(0);
                TooltipConfiguration.setDefaultHoverDelay(0);
                TooltipConfiguration.setDefaultHideDelay(200);
            }


            cell.add(new Span(item.getMpn()));
            return cell;
        }).setHeader("MPN").setWidth("200px").setFlexGrow(0).setSortable(true)
                .setComparator(Comparator.comparing(BomItem::getMpn, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));




        grid.addColumn(BomItem::getDescription)
                .setHeader("Opis (ENG)")
                .setWidth("360px")
                .setFlexGrow(2);


        grid.addComponentColumn(item -> {
            if (item.getDbKandidati() != null && !item.getDbKandidati().isEmpty()) {
                ComboBox<MaticniPodatek> combo = new ComboBox<>();
                combo.setItems(item.getDbKandidati());
                combo.setItemLabelGenerator(m -> m.getMpSifra() + " - " + m.getMpNaziv());
                combo.setWidth("100%");
                combo.setPlaceholder("Izberi komponento...");

                //restore kar smo prej izbral (refreshGrid() kleče grid.setItems() (zgradi ostalo row od scratch) << če ne se posodobi ko pišeš drugam)
                if (item.getDbSifra() != null) {
                    item.getDbKandidati().stream()
                            .filter(m -> m.getMpSifra().equals(item.getDbSifra()))
                            .findFirst()
                            .ifPresent(combo::setValue);
                }

                combo.addValueChangeListener(e -> {
                    if (e.getValue() != null) {
                        item.setDbSifra(e.getValue().getMpSifra());
                        item.setNazivSlo(e.getValue().getMpNaziv());
                    }
                });
                return combo;
            }

            // normalno (če ni podvojenih)
            TextField nameField = new TextField();
            nameField.setValue(item.getNazivSlo() != null ? item.getNazivSlo() : "");
            nameField.setWidth("100%");
            nameField.setMaxLength(35);
            nameField.setReadOnly(item.isExistsInDb()); //al lah pišemo al ne
            nameField.addValueChangeListener(e -> item.setNazivSlo(e.getValue()));
            return nameField;
        }).setHeader("Naziv SLO").setFlexGrow(2);


        grid.addColumn(BomItem::getQty)
                .setHeader("Kol.")
                .setWidth("80px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        grid.addColumn(BomItem::getIzmet)
                .setHeader("Izmet %")
                .setWidth("90px")
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);


        grid.addComponentColumn(item -> {
            TextField desField = new TextField();
            desField.setValue(item.getDesignator() != null ? item.getDesignator() : "");                // rdeč textFiel ob vsakmu refreshGrid()
            boolean emptyDesignator = item.getDesignator() == null || item.getDesignator().isEmpty();   // -||-
            desField.setInvalid(emptyDesignator || item.isDesignatorMismatch());
            desField.setWidth("100%");
            desField.setMinLength(2);
            desField.setMaxLength(70);


            desField.addValueChangeListener(e -> {
                String razsirjeni = bomItemMapper.checkDesignatorji(e.getValue());
                item.setDesignator(razsirjeni);

                int stDesignatorjev = razsirjeni.isEmpty() ? 0 : razsirjeni.split(",").length;
                item.setDesignatorMismatch(item.getQty() != null && stDesignatorjev > 0 && stDesignatorjev != item.getQty().intValue());

                desField.setInvalid(razsirjeni.isEmpty() || item.isDesignatorMismatch());   //2.
                refreshGrid();
            });
            return desField;
        }).setHeader("Designatorji").setFlexGrow(1);



        grid.setHeight("470px");

        VerticalLayout section = new VerticalLayout(gridNaslov, statsText, grid);
        section.setPadding(true);
        section.setSpacing(false);

        Div card = new Div(section);
        card.setWidthFull();
        card.getStyle()
                .set("border-radius", "12px")
                .set("border", "1.5px solid var(--lumo-contrast-10pct)")
                .set("background", "var(--lumo-contrast-5pct)") //var(--lumo-base-color)
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.15)")
                .set("overflow", "hidden");

        return card;
    }

    /**
     * Builds the footer with the import button.
     *
     * @return footer layout
     */
    private HorizontalLayout buildFooter() {
        importButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        importButton.setEnabled(false);
        importButton.addClickListener(e -> handleImport());

        HorizontalLayout footer = new HorizontalLayout(importButton);
        footer.setPadding(false);
        return footer;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Refreshes the grid with current items and updates the stats label and import button state.
     * Called after every change to {@code trenutenListKomponent} or any field in the grid.
     */
    private void refreshGrid() {
        if (trenutenListKomponent == null) return;

        grid.setItems(trenutenListKomponent);

        long inDb       = trenutenListKomponent.stream().filter(BomItem::isExistsInDb).count();
        long newItems   = trenutenListKomponent.size() - inDb;
        long missingDesignators = trenutenListKomponent.stream()
                .filter(i -> i.getDesignator() == null || i.getDesignator().isEmpty())
                .count();

        long mismatches = trenutenListKomponent.stream().filter(BomItem::isDesignatorMismatch).count();
        statsText.setText("Skupaj: " + trenutenListKomponent.size()
                + "   V bazi: " + inDb
                + "   Novih: " + newItems
                + (mismatches > 0 ? "   Vrstic za popraviti (ne ujemanje Kol. z Designatorji): " + mismatches : ""));


        importButton.setEnabled(
                !trenutenListKomponent.isEmpty()
                        && !imeIzdelka.isEmpty()
                        && missingDesignators == 0
                        && mismatches == 0
                        //&& neizbrani == 0             //to če češ da na začetku ni izbrana nubena (sam dej poj tm prefixe zameni na null pa to)
        );

        if (!warningsShow && trenutenListKomponent != null) {
            trenutenListKomponent.stream()
                    .filter(i -> i.getDesignator() == null || i.getDesignator().isEmpty())
                    .forEach(i -> showTextError(i.getMpn(), "je prazno.", true));
            trenutenListKomponent.stream()                      // .filter obdrži le tiste k ustrezajo dol. pogoju
                    .filter(BomItem::isDesignatorMismatch)      //i -> i.isDesignatorMismatch() //za vsak BomItem i, kliče metodo isDes...
                    .forEach(i -> showTextError(i.getMpn(), "se ne ujema s količino.", false));
            warningsShow = true;
        }
    }

    /**
     * Handles the import button click.
     * Validates that there are components, all designators are present and the right number of them, then calls {@link BomService#importBom}.
     * Shows a success or error notification when done.
     */
    private void handleImport() {
        if (trenutenListKomponent == null || trenutenListKomponent.isEmpty()) {
            showNotification("Ni komponent za uvoz", true);
            return;
        }

        String productName = imeIzdelka.getValue().trim();
        if (productName.isEmpty()) {
            showNotification("Vnesi ime izdelka", true);
            return;
        }

        //nkol ne gre čez to k je gimb disabled, za usak slučaj
        long missingDesignators = trenutenListKomponent.stream()
                .filter(i -> i.getDesignator() == null || i.getDesignator().isEmpty())
                .count();
        if (missingDesignators > 0) {
            showNotification("Najprej vnesi designatorje za ostalo komponente!", true);
            return;
        }

        try {
            importButton.setEnabled(false);
            importButton.getElement().getStyle().set("cursor", "not-allowed");
            importButton.setText("Uvažam...");

            BomService.ImportResult rez = bomService.importBom(trenutenListKomponent, productName); //<<<<<<<UVOZ V DB<<<<<<<

            String text = "Uvoz uspešen! Izdelek šifra: " + rez.getProductSifra()
                    + "\nNovih artiklov: " + rez.getCreatedCount()
                    + "\nKosovnica: " + rez.getKosovnicaCount() + " vrstic";

            showNotification(text, false);
            importButton.setEnabled(true);
            importButton.getElement().getStyle().set("cursor", "pointer");
            importButton.setText("Uvozi v bazo");

        } catch (Exception e) {
            showNotification("Napaka pri uvozu: " + e.getMessage(), true);
            importButton.setEnabled(true);
            importButton.getElement().getStyle().set("cursor", "pointer");
            importButton.setText("Uvozi v bazo");
        }
    }

    // -------------------------------------------------------------------------
    // Notification helpers
    // -------------------------------------------------------------------------

    /**
     * Shows a simple success or error notification at the top of the screen.
     *
     * @param text      message to display
     * @param isError   {@code true} for red error style, {@code false} for green success style
     */
    private void showNotification(String text, boolean isError) {
        Notification notification = new Notification(text, 5000);
        notification.addThemeVariants(isError
                ? NotificationVariant.LUMO_ERROR
                : NotificationVariant.LUMO_SUCCESS);
        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.open();
    }

    /**
     * Shows a detailed warning notification for designator field issues.
     * Used on grid load to alert the user about empty or mismatched designators.
     *
     * @param mpn  the MPN of the affected component, that has an error
     * @param text message to display (e.g. "je prazno." or "se ne ujema s količino.")
     * @param red  {@code true} for red error icon, {@code false} for yellow warning icon
     */
    private void showTextError(String mpn, String text, boolean red) {    //ERROR za prazne designatorje
        Notification notification = new Notification();

        Icon icon = VaadinIcon.WARNING.create();
        if (red)
            icon.setColor("var(--lumo-error-color)");
         else
            icon.setColor("var(--lumo-warning-color)");


        Span bes1 = new Span(new Text("Polje designatorjev"));
        bes1.getStyle().set("font-size", "0.875rem")
                .setColor("var(--lumo-secondary-text-color)");

        Span kje = new Span(mpn);
        kje.getStyle().set("font-size", "0.875rem").set("font-weight",
                "500");

        Span bes2 = new Span(new Text(text));
        bes2.getStyle().set("font-size", "0.875rem")
                .setColor("var(--lumo-secondary-text-color)");

        var vrsta = new HorizontalLayout(icon, bes1, kje, bes2);
        vrsta.setAlignItems(FlexComponent.Alignment.CENTER);

        notification.add(vrsta);

        notification.setPosition(Notification.Position.TOP_CENTER);
        notification.setDuration(4000);
        notification.open();
    }

    //=============================================
    // cookie za shranjevanje zadnje izbrane teme
    //=============================================

    private void saveCookie(String theme) {
        UI.getCurrent().getPage().executeJs(
                "document.cookie = 'tema=' + $0 + '; max-age=' + (7*24*60*60) + '; path=/'",
                theme
        );
    }

    private void applyCookie(){
        UI.getCurrent().getPage().executeJs(
                "return document.cookie.split(';').map(c => c.trim()).find(c => c.startsWith('tema='))?.split('=')[1] || 'dark';"
        ).then(String.class, theme -> {
            ThemeList themeList = UI.getCurrent().getElement().getThemeList();
            if ("dark".equals(theme)) {
                themeList.add(Lumo.DARK);
            } else {
                themeList.remove(Lumo.DARK);
            }
        });
    }
}