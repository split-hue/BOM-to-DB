package com.ksenija.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class CalcuQuoteService {

    @Value("${calcuquote.username}")
    private String username;

    @Value("${calcuquote.password}")
    private String password;

    @Value("${calcuquote.url}")
    private String loginUrl;

    private static final By LOADING_OVERLAY = By.cssSelector("div.jquery-loading-modal__bg");

    public InputStream downloadRfqExcel(String quoteId) throws Exception {
        Path downloadDir = Files.createTempDirectory("calcuquote_download");

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.toAbsolutePath().toString());
        prefs.put("download.prompt_for_download", false);
        options.setExperimentalOption("prefs", prefs);

        ChromeDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            // 1. LOGIN
            driver.get(loginUrl);
            wait.until(ExpectedConditions.elementToBeClickable(By.id("username"))).sendKeys(username);
            driver.findElement(By.id("password")).sendKeys(password);
            driver.findElement(By.id("password")).sendKeys(Keys.RETURN);

            wait.until(ExpectedConditions.urlContains("panel.calcuquote.com"));
            waitForOverlay(wait);

            // 1.2 Klik na ikono aplikacije
            safeClick(driver, wait, By.cssSelector("i.application-icon"));
            waitForOverlay(wait);

            // 1.3 Klik na RFQ list
            safeClick(driver, wait, By.cssSelector("i.clsMenuColor.fa-list-alt"));
            waitForOverlay(wait);

            // 2. IŠČI QUOTE ID
            By searchFieldBy = By.cssSelector("input.ui-grid-filter-input");

            int attempts = 0;
            while (attempts < 3) {
                try {
                    WebElement searchField = wait.until(ExpectedConditions.elementToBeClickable(searchFieldBy));
                    searchField.clear();
                    searchField.sendKeys(quoteId);

                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", searchField);
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));", searchField);
                    searchField.sendKeys(Keys.ENTER);
                    break; // success
                } catch (StaleElementReferenceException e) {
                    attempts++;
                    if (attempts == 3) throw new RuntimeException("Iskalno polje ostaja stale po 3 poskusih.");
                    Thread.sleep(500);
                }
            }

            waitForOverlay(wait);

//            // SCREENSHOT 1
//            File scr1 = driver.getScreenshotAs(OutputType.FILE);
//            Files.copy(scr1.toPath(), Path.of("C:\\Users\\cam\\Documents\\003\\intellij_pesk\\logs\\selenium-login.png"), StandardCopyOption.REPLACE_EXISTING);

            // 3. POČAKAJ NA VRSTICO IN KLIKNI "B"
            String rowXpath = "//div[@role='row'][.//div[contains(normalize-space(.), '" + quoteId + "')]]";
            By rowBy = By.xpath(rowXpath);
            By btnBBy = By.xpath(rowXpath + "//span[contains(@class, 'steps') and contains(text(), 'B')]");

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(rowBy));
                waitForOverlay(wait);
                safeClick(driver, wait, btnBBy);
            } catch (TimeoutException e) {
                File errorScr = driver.getScreenshotAs(OutputType.FILE);
                Files.copy(errorScr.toPath(), Path.of("C:\\Users\\cam\\Documents\\003\\intellij_pesk\\logs\\error-not-found.png"), StandardCopyOption.REPLACE_EXISTING);
                throw new RuntimeException("ID " + quoteId + " se ni pojavil ali pa gumba 'B' ni bilo mogoče klikniti. Preveri error-not-found.png");
            }

            waitForOverlay(wait);

//            // SCREENSHOT 2
//            File scr2 = driver.getScreenshotAs(OutputType.FILE);
//            Files.copy(scr2.toPath(), Path.of("C:\\Users\\cam\\Documents\\003\\intellij_pesk\\logs\\selenium-login2.png"), StandardCopyOption.REPLACE_EXISTING);

            // 4. CLICK "Actions"
            safeClick(driver, wait, By.cssSelector("button.btn.btn-background.dropdown-toggle"));
            waitForOverlay(wait);

            // 5. CLICK "Reports"
            safeClick(driver, wait, By.cssSelector("a[ng-click='OpenBomExport()']"));
            waitForOverlay(wait);

            // 6. CLICK "Download"
            safeClick(driver, wait, By.cssSelector("button[name='brnSave']"));

            // 7. WAIT FOR FILE
            File downloadedFile = waitForDownload(downloadDir, 20);
            System.out.println("shranjeno na: " + downloadDir);

            //prebere mem predn .quit()
            byte[] fileBytes = Files.readAllBytes(downloadedFile.toPath());
            return new java.io.ByteArrayInputStream(fileBytes);

        } finally {
            driver.quit();
            try {
                Files.walk(downloadDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Waits for the loading overlay to disappear.
     */
    private void waitForOverlay(WebDriverWait wait) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING_OVERLAY));
    }

    /**
     * Clicks an element by locator, retrying up to 3 times on StaleElementReferenceException
     * or ElementClickInterceptedException. Re-finds the element fresh each attempt.
     */
    private void safeClick(WebDriver driver, WebDriverWait wait, By locator) {
        waitForOverlay(new WebDriverWait(driver, Duration.ofSeconds(10)));

        int attempts = 0;
        while (attempts < 3) {
            try {
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                element.click();
                return;
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                attempts++;
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Ne morem klikniti elementa: " + locator);
    }

    private File waitForDownload(Path downloadDir, int maxSeconds) throws Exception {
        for (int i = 0; i < maxSeconds; i++) {
            File[] files = downloadDir.toFile().listFiles(f ->
                    f.getName().endsWith(".xlsx") || f.getName().endsWith(".xls"));
            if (files != null && files.length > 0) {
                if (!files[0].getName().contains(".crdownload")) {
                    return files[0];
                }
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException("Excel datoteka se ni naložila pravočasno.");
    }
}
