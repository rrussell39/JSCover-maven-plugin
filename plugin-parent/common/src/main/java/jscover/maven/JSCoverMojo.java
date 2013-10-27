package jscover.maven;

import jscover.ConfigurationCommon;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class JSCoverMojo extends AbstractMojo {
    private ConfigurationCommon defaults = new ConfigurationCommon();

    //JSCover Common Parameters
    @Parameter
    protected boolean includeBranch = defaults.isIncludeBranch();
    @Parameter
    protected boolean includeFunction = defaults.isIncludeFunction();
    @Parameter
    protected boolean localStorage = defaults.isLocalStorage();
    @Parameter
    protected final List<String> instrumentPathArgs = new ArrayList<String>();
    @Parameter
    protected File reportDir = new File("target/reports/jscover-maven");
    @Parameter
    protected int JSVersion = defaults.getJSVersion();

    //Test Parameters
    @Parameter(required = true)
    protected File testDirectory = new File("src/test/javascript/spec");
    @Parameter(required = true)
    protected String testIncludes = "*.html";
    @Parameter
    protected String testExcludes;
    @Parameter
    protected String testType = "Jasmine";
    @Parameter(required = true)
    protected int lineCoverageMinimum;
    @Parameter
    protected int branchCoverageMinimum;
    @Parameter
    protected int functionCoverageMinimum;
    @Parameter
    protected String webDriverClassName = PhantomJSDriver.class.getName();
    @Parameter
    protected Properties systemProperties = new Properties();
    @Parameter
    protected boolean reportLCOV;
    @Parameter
    protected boolean reportCoberturaXML;

    protected WebDriver getWebClient() {
        Class<WebDriver> webDriverClass = getWebDriverClass();
        try {
            try {
                return webDriverClass.getConstructor(Capabilities.class).newInstance(getDesiredCapabilities());
            } catch (final NoSuchMethodException e) {
                return webDriverClass.newInstance();
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Capabilities getDesiredCapabilities() {
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setJavascriptEnabled(true);
        return desiredCapabilities;
    }

    protected Class<WebDriver> getWebDriverClass() {
        try {
            return (Class<WebDriver>) Class.forName(webDriverClassName);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}