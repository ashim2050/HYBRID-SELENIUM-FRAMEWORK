package com.framework.utils;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Generates a dynamic TestNG XML suite file based on the active modules
 * (ExecutionFlag = "Yes") read from MasterConfig.xlsx.
 *
 * Expected module-map keys (from MasterConfig.xlsx columns):
 * <pre>
 * ModuleName       - logical name used as the <test name>
 * TestClass        - fully-qualified Java class (e.g. com.framework.tests.LoginTests)
 * SuiteDescription - optional free-text description
 * </pre>
 *
 * Parallel/thread settings are taken from config.properties.
 */
public class TestNGXmlGenerator {

    private static final Logger logger = LogManager.getLogger(TestNGXmlGenerator.class);

    private static final String LISTENER_CLASS = "com.framework.listeners.TestListener";

    private TestNGXmlGenerator() { }

    // -------------------------------
    // Public API
    // -------------------------------

    /**
     * Builds and writes a testng-dynamic.xml to {@code outputPath}.
     *
     * @param activeModules Modules whose ExecutionFlag == "Yes"
     * @param outputPath Destination file path (e.g. "output/testng-dynamic.xml")
     */
    public static void generate(List<Map<String, String>> activeModules, String outputPath) {
        if (activeModules == null || activeModules.isEmpty()) {
            logger.warn("No active modules found. TestNG XML will not be generated.");
            return;
        }

        try {
            Document doc = buildDocument(activeModules);
            writeFile(doc, outputPath);
            logger.info("Dynamic TestNG XML written to: {}", outputPath);
        } catch (Exception e) {
            logger.error("Failed to generate TestNG XML", e);
            throw new RuntimeException("TestNG XML generation failed", e);
        }
    }

    // -------------------------------
    // Build DOM
    // -------------------------------

    private static Document buildDocument(List<Map<String, String>> activeModules) throws Exception {
        // Secure DOM factory - we are building, not parsing external content
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        // <suite>
        Element suite = doc.createElement("suite");
        suite.setAttribute("name", "HybridFramework_DynamicSuite");
        suite.setAttribute("verbose", "1");
        suite.setAttribute("parallel", ConfigReader.getParallelExecution());
        suite.setAttribute("thread-count", String.valueOf(ConfigReader.getThreadCount()));
        doc.appendChild(suite);

        // <listeners>
        Element listeners = doc.createElement("listeners");
        Element listener = doc.createElement("listener");
        listener.setAttribute("class-name", LISTENER_CLASS);
        listeners.appendChild(listener);
        suite.appendChild(listeners);

        // One <test> block per active module
        for (Map<String, String> module : activeModules) {
            String moduleName = module.getOrDefault("ModuleName", "UnknownModule").trim();
            String testClass = module.getOrDefault("TestClass", "").trim();

            if (testClass.isEmpty()) {
                logger.warn("Module '{}' has no TestClass defined - skipped in XML.", moduleName);
                continue;
            }

            Element test = doc.createElement("test");
            test.setAttribute("name", moduleName + " Tests");
            test.setAttribute("preserve-order", "true");

            Element classes = doc.createElement("classes");
            Element clazz = doc.createElement("class");
            clazz.setAttribute("name", testClass);
            classes.appendChild(clazz);
            test.appendChild(classes);

            suite.appendChild(test);
            logger.debug("Added to XML - module: {}, class: {}", moduleName, testClass);
        }

        return doc;
    }

    // -------------------------------
    // Write file
    // -------------------------------

    private static void writeFile(Document doc, String outputPath) throws Exception {
        File outputFile = new File(outputPath);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.warn("Could not create output directory for {}", outputPath);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                "https://testng.org/testng-1.0.dtd");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));
    }
}
