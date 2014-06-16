package jscover.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static jscover.maven.TestType.Custom;
import static jscover.maven.TestType.QUnit;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class JSCoverMojoTest {
    private JSCoverMojo mojo = new DummyMojo();

    protected File getFilePath(String pathname) {
        if (System.getProperty("user.dir").endsWith("JSCover-maven-plugin"))
            pathname = "plugin-parent/server/" + pathname;
        return new File(pathname).getAbsoluteFile();
    }

    @Test
    public void shouldFindTestFiles() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testDirectory", getFilePath("../data/src/test/javascript"));
        ReflectionUtils.setVariableValueInObject(mojo, "testIncludes", "jasmine-html-*pass.html");
        List<File> files = mojo.getTestFiles();
        for (File file : files) {
            System.out.println("file = " + file);
        }
        assertThat(files.size(), equalTo(2));
        assertThat(files, hasItem(getFilePath("../data/src/test/javascript/jasmine-html-reporter-code-pass.html")));
        assertThat(files, hasItem(getFilePath("../data/src/test/javascript/jasmine-html-reporter-util-pass.html")));
    }

    @Test
    public void shouldProcessBuiltInTestType() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testType", QUnit);
        assertThat((QUnitWebDriverRunner)mojo.getWebDriverRunner(), isA(QUnitWebDriverRunner.class));
    }

    @Test
    public void shouldProcessCustomTestType() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testType", Custom);
        ReflectionUtils.setVariableValueInObject(mojo, "testRunnerClassName", "jscover.maven.QUnitWebDriverRunner");
        assertThat((QUnitWebDriverRunner)mojo.getWebDriverRunner(), isA(QUnitWebDriverRunner.class));
    }

    @Test
    public void shouldFailCustomTestTypeIfNotSpecified() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testIncludes", "jasmine2-*pass.html");
        ReflectionUtils.setVariableValueInObject(mojo, "testType", Custom);
        try {
            mojo.execute();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), equalTo("Please provide a custom test type class that implements jscover.maven.WebDriverRunner"));
        }
    }

    @Test
    public void shouldFailCustomTestTypeIfWrongType() throws Exception {
        ReflectionUtils.setVariableValueInObject(mojo, "testIncludes", "jasmine2-*fail.html");
        ReflectionUtils.setVariableValueInObject(mojo, "testType", Custom);
        ReflectionUtils.setVariableValueInObject(mojo, "testRunnerClassName", "java.lang.String");
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            assertThat(e.getMessage(), equalTo("java.lang.String cannot be cast to jscover.maven.WebDriverRunner"));
        }
    }
}