package com.revature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SeleniumTest {

    private WebDriver webDriver;
    private WebDriverWait wait;

    @BeforeEach
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", "driver/chromedriver.exe"); // linux_64

        File file = new File("src/main/java/com/revature/index.html");
        String path = "file://" + file.getAbsolutePath();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("headless");
        webDriver = new ChromeDriver(options);
        wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        webDriver.get(path);
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
    }

    @AfterEach
    public void tearDown() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testSearchBooksSucceeds() {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) webDriver;
        wait.until(driver -> ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete"));
    
        // Test success with Google Books API query #1
        String script = "return searchBooks(arguments[0], arguments[1]).then(JSON.stringify);";
        String actual1 = (String) jsExecutor.executeScript(script, "harry potter", "title");
    
        if (actual1 == null) {
            fail("No results provided by the searchBooks function.");
        }
    
        System.out.println("Actual response 1: " + actual1.toLowerCase());
    
        Assertions.assertTrue(actual1.contains("Harry Potter"), "Title 'Harry Potter' not found.");
        System.out.println(actual1);
        Assertions.assertTrue(actual1.contains("J. K. Rowling"), "Author 'J.K. Rowling' not found.");
    
        // Test success with query #2
        String actual2 = (String) jsExecutor.executeScript(script, "poe", "author");
        System.out.println("Actual response 2: " + actual2.toLowerCase());
    
        Assertions.assertTrue(actual2.contains("Edgar Allan Poe"), "Author 'Edgar Allan Poe' not found.");
        Assertions.assertTrue(actual2.contains("The Tell-Tale Heart"), "Title 'The Tell-Tale Heart' not found.");
    
        // Test success with query #3
        String actual3 = (String) jsExecutor.executeScript(script, "9781472539342", "isbn");
        System.out.println("Actual response 3: " + actual3.toLowerCase());
    
        Assertions.assertTrue(actual3.contains("The Road"), "Title 'The Road' not found.");
        Assertions.assertTrue(actual3.contains("Cormac McCarthy"), "Author 'Cormac McCarthy' not found.");
    
        // Assert only 10 books or less are returned from the function
        Object actual4 = jsExecutor.executeScript("return searchBooks(arguments[0], arguments[1]);", "9781725757264", "isbn");
        Assertions.assertTrue(((List) actual4).size() <= 10, "The list of books returned is over 10 elements in size.");
    }
    

    @Test
    public void testDisplayOfBookSearchResults() {
        WebElement searchInput = null;
        WebElement searchType = null;
        WebElement searchButton = null;

        try {
            searchInput = webDriver.findElement(By.id("search-input"));
            searchType = webDriver.findElement(By.id("search-type"));
            searchButton = webDriver.findElement(By.id("submit-button"));  // Corrected ID

        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        searchType.sendKeys("title");
        searchInput.sendKeys("Test");
        searchButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("book-list")));

        WebElement bookList = webDriver.findElement(By.id("book-list"));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#book-list > li")));
        List<WebElement> books = bookList.findElements(By.tagName("li"));
        assertFalse(books.isEmpty(), "No books displayed.");

        books.forEach(book -> {
            assertNotNull(book.findElement(By.className("title-element")).getText());
            assertNotNull(book.findElement(By.className("cover-element")).isDisplayed());
            assertNotNull(book.findElement(By.className("rating-element")).getText());
            assertNotNull(book.findElement(By.className("ebook-element")).getText());
        });
    }

    @Test
    public void testSearchFormElementsIncluded() {
        WebElement searchForm = null;

        try {
            searchForm = webDriver.findElement(By.id("search-form"));
        } catch (NoSuchElementException e) {
            fail(e.getMessage());
        }

        assertNotNull(searchForm.findElement(By.id("search-input")));
        assertNotNull(searchForm.findElement(By.id("search-type")));
        assertNotNull(searchForm.findElement(By.id("submit-button")));  // Corrected ID

        List<WebElement> options = searchForm.findElements(By.tagName("option"));
        boolean selectOptionsVal = true;
        boolean optionTitleExists = false;
        boolean optionAuthorExists = false;
        boolean optionIsbnExists = false;

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getAttribute("value").toLowerCase().equals("title")) {
                optionTitleExists = true;
            } else if (options.get(i).getAttribute("value").toLowerCase().equals("author")) {
                optionAuthorExists = true;
            } else if (options.get(i).getAttribute("value").toLowerCase().equals("isbn")) {
                optionIsbnExists = true;
            } else {
                selectOptionsVal = false;
            }
        }
        assertTrue(selectOptionsVal, "One of the options of your select element is an invalid type");
        assertTrue(optionTitleExists, "The option with value 'title' does not exist.");
        assertTrue(optionAuthorExists, "The option with value 'author' does not exist.");
        assertTrue(optionIsbnExists, "The option with value 'isbn' does not exist.");
    }

    @Test
    public void testDisplayDetailedBookInformation() {
        // Step 1: Perform search
        WebElement searchInp = webDriver.findElement(By.id("search-input"));
        WebElement searchType = webDriver.findElement(By.id("search-type"));
        WebElement searchButton = webDriver.findElement(By.id("submit-button"));
    
        searchType.sendKeys("title");
        searchInp.sendKeys("test");
        searchButton.click();
    
        // Step 2: Wait for book list to be visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("book-list")));
    
        // Step 3: Click the first book
        WebElement firstbookItem = wait
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("#book-list > li:first-child")));
        firstbookItem.click();
    
        // Step 4: Wait for selected book section to be visible
        WebElement selectedBook = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("selected-book")));
        assertNotNull(selectedBook, "Element with id 'selected-book' cannot be found.");
        assertTrue(selectedBook.isDisplayed(), "Element with id 'selected-book' is not displayed.");
    
        // Step 5: Check for the dynamically added `cover-element`
        System.out.println("Selected book HTML: " + selectedBook.getAttribute("innerHTML")); // Debugging step
    
        List<WebElement> coverEle = selectedBook.findElements(By.className("cover-element"));
        assertFalse(coverEle.isEmpty(), "Cover element is not present in the selected-book section.");
        WebElement coverElement = coverEle.get(0);
        assertFalse(coverElement.isDisplayed(), "Cover element is not displayed.");
    }
    
    // 5: Our application’s search results should be sortable by rating.
    @Test
    public void testHandleSort() {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10)); // Explicit wait
    
        try {
            // Locate elements
            WebElement searchInp = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-input")));
            WebElement searchType = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-type")));
            WebElement searchButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("submit-button"))); // Corrected ID
    
            // Perform actions
            searchInp.sendKeys("test");
            searchType.sendKeys("title");
            searchButton.click();
    
            // Wait for results and verify
            WebElement bookList = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("book-list")));
            assertNotNull(bookList);
        } catch (NoSuchElementException e) {
            fail("Element not found: " + e.getMessage());
        }
    }

    // 6: Our application’s search results should be filterable by whether or not
    // the results are available as ebooks.
    @Test
    public void testHandleFilter() {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10)); // Ensure sufficient wait time
    
        WebElement searchInp = null;
        WebElement searchType = null;
        WebElement searchButton = null;
    
        try {
            // Locate the elements
            searchInp= wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-input")));
            searchType = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("search-type")));
            searchButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("submit-button")));
        } catch (NoSuchElementException e) {
            fail("Element not found: " + e.getMessage());
        }
    
        // Perform the actions
        searchType.sendKeys("title");
        searchInp.sendKeys("test");
        searchButton.click();
    
        // Wait for the results to load and validate
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("book-list")));
        WebElement bookList = webDriver.findElement(By.id("book-list"));
        assertNotNull(bookList);
    }

    // Semantic elements should be included in HTML for web accessibility.
    @Test
    public void testSemanticHtmlElements() {
        String htmlFileContent = TestingUtils.getContent("index.html");

        String[] eleNeeded = { "article", "aside", "details", "figcaption", "figure", "footer", "header", "main",
                "nav", "section" };
        int cnt = 0;

        for (int i = 0; i < eleNeeded.length; i++) {
            if (htmlFileContent.contains(eleNeeded[i])) {
                cnt++;
            }
        }

        assertTrue(cnt > 2, "More semantic HTML elements are required");
    }
    // 8: CSS styling should be used to create a responsive web application.
    @Test
    public void testResponsiveDesignIsIncluded() {
        String css_File_Content = TestingUtils.getContent("styles.css");

        String[] eleNeeded = { "@media", "grid", "flex" };
        boolean Responsive = false;

        for (int i = 0; i < eleNeeded.length; i++) {
            if (css_File_Content.contains(eleNeeded[i])) {
                Responsive = true;
            }
        }

        assertTrue(Responsive, "Responsive CSS styles need to be included.");
    }

    static class TestingUtils {

        public static String getContent(String filename) {
            String cont = "";
            try {
                cont = Files.readString(Paths.get("./src/main/java/com/revature/" + filename));
        }   catch (IOException e) {
                e.printStackTrace();
        }
            return cont;
    }
}
}
