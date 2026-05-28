package com.framework.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class SearchPage extends BasePage {

    @FindBy(id = "searchInput")
    private WebElement searchInput;

    public SearchPage(WebDriver driver) {
        super(driver);
        PageFactory.initElements(driver, this);
    }

    public SearchPage open(String url) {
        driver.get(url);
        wait.until(ExpectedConditions.visibilityOf(searchInput));
        return this;
    }

    public SearchPage acceptCookiesIfPresent() {
        try {
            List<WebElement> buttons = driver.findElements(By.xpath(
                    "//button[contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'accept') or contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'agree')]"));
            if (!buttons.isEmpty()) {
                WebElement btn = buttons.get(0);
                if (btn.isDisplayed()) {
                    btn.click();
                    wait.until(ExpectedConditions.stalenessOf(btn));
                }
            }
        } catch (Exception ignored) {
            // no cookie prompt present
        }
        return this;
    }

    public SearchPage search(String query) {
        wait.until(ExpectedConditions.visibilityOf(searchInput));
        searchInput.clear();
        searchInput.sendKeys(query);
        searchInput.sendKeys(Keys.ENTER);
        return this;
    }

    public String getResultsPageTitle(String expectedResult) {
        wait.until(ExpectedConditions.titleContains(expectedResult));
        return driver.getTitle();
    }
}
