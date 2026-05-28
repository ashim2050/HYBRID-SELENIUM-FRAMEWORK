Framework Overview — Hybrid Selenium + API Data-driven

Purpose
- Hybrid test automation framework using Excel-driven data, TestNG, RestAssured (API), Selenium WebDriver (UI), and ExtentReports.

Folder Structure (high-level)
- Input/
  - MasterConfig.xlsx (controls which modules run)
  - API.xlsx, Login.xlsx, Search.xlsx (module-specific test data)
- output/
  - testdata/ (generated JSON test data)
  - reports/ (Extent HTML reports)
  - diagrams/framework_diagram.mmd (Mermaid source)
  - testng-dynamic.xml (generated TestNG suite)
- src/
  - main/java/
    - com.framework.config -> `ConfigReader`
    - com.framework.drivers -> `DriverManager`
    - com.framework.listeners -> `DynamicSuiteListener`, `TestListener`
    - com.framework.utils -> `ExcelReader`, `JsonConverter`, `TestNGXmlGenerator`, `DataProviderUtil`
    - com.framework.base -> `BaseTest`
  - test/java/
    - com.framework.tests -> `ApiDataDrivenTests`, `LoginTests`, `SearchTests`
  - test/resources/
    - config.properties, testng.xml
- pom.xml

Execution Flow (short)
1. `DynamicSuiteListener` reads `Input/MasterConfig.xlsx` and identifies enabled modules.
2. For each module (e.g., API, Login, Search), the **ExcelReader** reads the corresponding Excel file from `Input/` (e.g., `Input/API.xlsx`).
3. **JsonConverter** transforms the enabled rows from module files into JSON test data and saves to `output/testdata/`.
4. **TestNGXmlGenerator** creates a dynamic suite XML (`output/testng-dynamic.xml`) pointing to the generated test data.
5. TestNG executes the suite; each test receives a map row via `DataProviderUtil`.
6. UI tests: `BaseTest` initialises thread-local WebDriver via `DriverManager`.
7. API tests: use RestAssured with `baseURI` from test-data or `api.base.url`.
8. `TestListener` creates `ExtentTest` nodes, logs results, captures screenshots for UI failures, and flushes HTML reports to `output/reports/`.

Interview Script (1–2 minute)
- Opening (15s): "This is a hybrid, data-driven automation framework designed to let non-developers control test execution through Excel. It supports both UI and API testing and produces rich HTML reports."
- Core idea (30s): "We read a master Excel file that lists modules to run. A `DynamicSuiteListener` converts Excel rows into JSON test data and a dynamic TestNG suite. Tests get a map of test data through data providers, keeping test code generic and data-driven."
- UI vs API (20s): "UI tests extend `BaseTest`, which initialises a thread-local WebDriver using `DriverManager` and WebDriverManager for driver binaries. API tests use RestAssured and don't require a WebDriver — this separation keeps runs fast and independent."
- Reporting & reliability (25s): "`TestListener` integrates ExtentReports, capturing logs and screenshots for UI failures. The framework handles browser version mismatches with WebDriverManager and allows headless runs via config."
- Closing / benefits (20s): "This design is maintainable — non-technical contributors can add test rows in Excel, CI can run the generated suite, and reports make triage easy."

## Excel Test Data Configuration

The framework uses a **master + module** Excel structure:

### Master Configuration (MasterConfig.xlsx)
- Controls **which modules are executed** and **in what order**
- Typically contains columns:
  - `Module` – Name of the test module (API, Login, Search, etc.)
  - `Enabled` – true/false flag to include or skip the module
  - `ExecutionMode` – Sequential, Parallel, or Custom
  - `Description` – Optional description of the module

### Module-Specific Sheets
Each module has its own Excel file in `Input/`:
- **API.xlsx** – API endpoint tests (GET, POST, PUT, DELETE, etc.)
- **Login.xlsx** – UI login test scenarios (valid/invalid credentials, edge cases)
- **Search.xlsx** – UI search functionality tests (keywords, filters, pagination, etc.)

#### Module Sheet Structure (Example: Login.xlsx)
| Test ID | Test Case | Username | Password | Expected Result | Enabled |
|---------|-----------|----------|----------|-----------------|---------|
| TC_LOGIN_001 | Valid login | validuser@domain.com | ValidPass123 | Dashboard displayed | Yes |
| TC_LOGIN_002 | Invalid password | validuser@domain.com | WrongPass | Error message shown | Yes |
| TC_LOGIN_003 | Empty username | (blank) | ValidPass123 | Error message shown | Yes |

#### Module Sheet Structure (Example: API.xlsx)
| Test ID | Method | Endpoint | Request Body | Response Status | Response Field | Field Value | Enabled |
|---------|--------|----------|--------------|-----------------|-----------------|-------------|---------|
| TC_API_001 | GET | /posts | N/A | 200 | id | non-null | Yes |
| TC_API_002 | POST | /posts | {"title":"Test"} | 201 | id | non-null | Yes |
| TC_API_003 | DELETE | /posts/1 | N/A | 200 | – | – | Yes |

### Data Flow
1. `DynamicSuiteListener` reads **MasterConfig.xlsx** → identifies enabled modules
2. For each enabled module, **reads the corresponding Excel file** from `Input/` folder
   - **ExcelReader** utility parses the module sheet
   - Reads all rows where `Enabled = Yes`
   - Converts each row into a map (column name → value)
3. **JsonConverter** transforms the maps into JSON objects and writes them to `output/testdata/`
   - `output/testdata/API_testdata.json`
   - `output/testdata/Login_testdata.json`
   - `output/testdata/Search_testdata.json`
4. **TestNGXmlGenerator** creates a dynamic TestNG suite XML (`output/testng-dynamic.xml`)
   - Defines test methods for each enabled module
   - Points to the generated JSON files
5. TestNG executes the suite using **DataProviderUtil**
   - Each test method receives a map (one row from the JSON)
   - Tests run with the data and produce reports via ExtentReports

### How Module Files Are Read (Technical Details)

**ExcelReader** (in `src/main/java/com/framework/utils/`)
- Reads module Excel files using Apache POI
- Extracts **header row** (column names) from the first row
- Iterates through data rows and creates a `Map<String, String>` for each row
- Filters rows where the `Enabled` column = "Yes"
- Returns a `List<Map<String, String>>` for the module

**Process flow:**
```
Input/API.xlsx 
  ↓ [ExcelReader.readSheet("API")]
Map<String, String> {
  "TestID": "TC_API_001",
  "Method": "GET",
  "Endpoint": "/posts",
  "ResponseStatus": "200",
  "ResponseField": "id",
  "FieldValue": "non-null",
  "Enabled": "Yes"
}
  ↓ [JsonConverter.toJson()]
output/testdata/API_testdata.json
```

**Module Reading Configuration:**
- Framework looks for files in `Input/` with naming convention: `{ModuleName}.xlsx`
- Module names come from `MasterConfig.xlsx` `Module` column
- If a module Excel file is missing, the listener logs a warning and skips it
- Only rows with `Enabled = Yes` are converted to JSON and executed

### Adding a New Test Case
1. Open the corresponding module Excel file (e.g., `Input/Login.xlsx`)
2. Add a new row with test data
3. Set `Enabled = Yes`
4. Save the file
5. Run `mvn test` — the listener will auto-pick up the new row

### Editing Data Without Code Changes
✅ **Non-developers can:**
- Modify test data directly in Excel
- Enable/disable tests by toggling the `Enabled` column
- Add new test cases without touching code
- Update endpoints, URLs, or expected values

✅ **Developers** only need to add new columns if new test properties are required.

How to export the block diagram to PNG (local)
1. Install mermaid CLI (requires Node.js/npm):

```bash
npm install -g @mermaid-js/mermaid-cli
```

2. Generate PNG from the saved source:

```bash
mmdc -i output/diagrams/framework_diagram.mmd -o output/reports/framework_diagram.png
```

3. Open `output/reports/framework_diagram.png`.

Quick commands
- Run all tests (dynamic suite will be generated at runtime):

```bash
mvn test
```

- Run only API tests:

```bash
mvn -Dtest=ApiDataDrivenTests test
```

Files to review
- `src/main/java/com/framework/config/ConfigReader.java`
- `src/main/java/com/framework/drivers/DriverManager.java`
- `src/main/java/com/framework/listeners/DynamicSuiteListener.java`
- `src/main/java/com/framework/listeners/TestListener.java`
- `src/test/java/com/framework/tests/ApiDataDrivenTests.java`

Contact
- If you want, I can also generate the PNG here and add it to `output/reports/` (requires mermaid rendering support on this host).