package jscover.maven;

import jscover.report.ConfigurationForReport;
import jscover.report.Main;
import jscover.server.ConfigurationForServer;
import jscover.util.IoUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.util.List;

import static java.lang.String.format;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElement;

public class TestRunner {

    protected WebDriver webClient;
    private ConfigurationForServer config;
    private WebDriverRunner webDriverRunner;
    private int lineCoverageMinimum;
    private int branchCoverageMinimum;
    private int functionCoverageMinimum;
    private final boolean reportLCOV;
    private final boolean reportCoberturaXML;
    private IoUtils ioUtils;

    public TestRunner(WebDriver webClient, WebDriverRunner webDriverRunner, ConfigurationForServer config, int lineCoverageMinimum, int branchCoverageMinimum, int functionCoverageMinimum, boolean reportLCOV, boolean reportCoberturaXML) {
        this.webClient = webClient;
        this.webDriverRunner = webDriverRunner;
        this.config = config;
        this.lineCoverageMinimum = lineCoverageMinimum;
        this.branchCoverageMinimum = branchCoverageMinimum;
        this.functionCoverageMinimum = functionCoverageMinimum;
        this.reportLCOV = reportLCOV;
        this.reportCoberturaXML = reportCoberturaXML;
    }

    public void runTests(List<File> testPages) throws MojoFailureException, MojoExecutionException {
        File jsonFile = new File(config.getReportDir() + "/jscoverage.json");
        if (jsonFile.exists())
            jsonFile.delete();
        try {
            webClient.get(String.format("http://localhost:%d/jscoverage.html", config.getPort()));
            for (File testPage : testPages) {
                ioUtils = IoUtils.getInstance();
                runTest(ioUtils.getRelativePath(testPage, config.getDocumentRoot()));
            }
            saveCoverageData();
            verifyTotal();
            generateOtherReportFormats();
        } finally {
            stopWebClient();
        }
    }

    private void saveCoverageData() {
        new WebDriverWait(webClient, 1).until(ExpectedConditions.elementToBeClickable(By.id("storeTab")));
        webClient.findElement(By.id("storeTab")).click();

        new WebDriverWait(webClient, 1).until(ExpectedConditions.elementToBeClickable(By.id("storeButton")));
        webClient.findElement(By.id("storeButton")).click();
        new WebDriverWait(webClient, 2).until(ExpectedConditions.textToBePresentInElement(By.id("storeDiv"), "Coverage data stored at"));

        webClient.get(format("http://localhost:%d/%s/jscoverage.html", config.getPort(), ioUtils.getRelativePath(config.getReportDir(), config.getDocumentRoot())));
    }

    private void generateOtherReportFormats() throws MojoExecutionException {
        try {
            if (reportLCOV || reportCoberturaXML) {
                ConfigurationForReport configurationForReport = new ConfigurationForReport();
                Main main = new Main();
                main.initialize();
                configurationForReport.setProperties(Main.properties);
                configurationForReport.setJsonDirectory(config.getReportDir());
                configurationForReport.setSourceDirectory(new File(config.getReportDir(), jscover.Main.reportSrcSubDir));
                main.setConfig(configurationForReport);
                if (reportLCOV) {
                    main.generateLCovDataFile();
                }
                if (reportCoberturaXML) {
                    main.saveCoberturaXml();
                }
            }
        } catch (Throwable t) {
            throw new MojoExecutionException("Error generating other report formats", t);
        }
    }

    public void runTest(String testPage) throws MojoFailureException, MojoExecutionException {
        webClient.findElement(By.id("location")).clear();
        webClient.findElement(By.id("location")).sendKeys(String.format("http://localhost:%d/%s", config.getPort(), testPage));
        webClient.findElement(By.id("openInFrameButton")).click();

        String handle = webClient.getWindowHandle();
        new WebDriverWait(webClient, 1).until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("browserIframe"));
        webDriverRunner.waitForTestsToComplete(webClient);
        webDriverRunner.verifyTestsPassed(webClient);

        webClient.switchTo().window(handle);
    }

    protected void verifyTotal() throws MojoFailureException {
        webClient.findElement(By.id("summaryTab")).click();
        new WebDriverWait(webClient, 1).until(textToBePresentInElement(By.id("summaryTotal"), "%"));

        verifyField("Line", "summaryTotal", lineCoverageMinimum);
        verifyField("Branch", "branchSummaryTotal", branchCoverageMinimum);
        verifyField("Function", "functionSummaryTotal", functionCoverageMinimum);
    }

    private void verifyField(String coverageName, String fieldName, int percentageMin) throws MojoFailureException {
        int percentage = extractInt(webClient.findElement(By.id(fieldName)).getText());
        if (percentage < percentageMin) {
            throw new MojoFailureException(format("%s coverage %d less than %d", coverageName, percentage, percentageMin));
        }
    }

    private int extractInt(String percentage) {
        return Integer.parseInt(percentage.replaceAll("%", ""));
    }

    public void stopWebClient() {
        try {
            webClient.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            webClient.quit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}