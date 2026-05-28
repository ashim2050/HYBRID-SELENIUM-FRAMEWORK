package com.framework.tests;

import com.framework.base.BaseTest;
import com.framework.listeners.TestListener;
import com.framework.pages.SearchPage;
import com.framework.utils.DataProviderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Search module tests.
 *
 * Test data is sourced from output/testdata/Search_testdata.json
 * (generated at runtime from testdata/Search_Module.xlsx).
 *
 * Controlled by ExecutionFlag in MasterConfig.xlsx.
 */
public class SearchTests extends BaseTest {

    private static final Logger logger = LogManager.getLogger(SearchTests.class);
    private static final String MODULE_NAME = "Search";

    // -------------------------------
    // DataProviders
    // -------------------------------
    @DataProvider(name = "keywordSearchData", parallel = false)
    public Object[][] keywordSearchData() {
        return DataProviderUtil.getTestData(MODULE_NAME, "testKeywordSearch");
    }

    // -------------------------------
    // Tests
    // -------------------------------

    /**
     * TC_SRC_001, TC_SRC_002 - Performs a Google search and verifies results page.
     */
    @Test(dataProvider = "keywordSearchData",
          description = "Verify search results contain the expected keyword")
    public void testKeywordSearch(Map<String, Object> testData) {
        String url = str(testData, "URL");
        String keyword = str(testData, "SearchKeyword");
        String expectedResult = str(testData, "ExpectedResult");

        TestListener.getTest().info("TC: " + str(testData, "TestCaseID") + " | Keyword: " + keyword);
        logger.info("testKeywordSearch 🔍 keyword: {}", keyword);

        SearchPage searchPage = new SearchPage(getDriver());
        searchPage.open(url)
                  .acceptCookiesIfPresent()
                  .search(keyword);

        String title = searchPage.getResultsPageTitle(expectedResult);
        Assert.assertTrue(title.contains(expectedResult),
                "Search results page title did not contain: " + expectedResult);

        TestListener.getTest().pass("testKeywordSearch passed | keyword: " + keyword);
        logger.info("testKeywordSearch PASSED ✅ keyword: {}", keyword);
    }

    // -------------------------------
    // Helpers
    // -------------------------------
    private static String str(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val != null ? String.valueOf(val).trim() : "";
    }
}
