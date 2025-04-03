# M5: Testing and Code Review

## 1. Change History

| **Change Date**  | **Modified Sections**  | **Rationale** |
| ---------------- | ---------------------- | ------------- |
| _Nothing to show_ |                        |               |

---

## 2. Back-end Test Specification: Questionnaires & StudentGroups Modules

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Test Files

| **Module**         | **Test File Location**                                                                                        | **Test Runner Command**             |
| ------------------ | ------------------------------------------------------------------------------------------------------------- | ----------------------------------- |
| **Questionnaires** | [`src/questionnaires/test/questionnaires.test.ts`](./src/questionnaires/test/questionnaires.test.ts)          | `npm run test` or `npm run coverage` |
| **StudentGroups**  | [`src/questionnaires/test/studentGroups.test.ts`](./src/questionnaires/test/studentGroups.test.ts)              | `npm run test` or `npm run coverage` |

#### 2.1.2. Commit Hash Where Tests Run

`66821fdfd79d79f2fc089f1a2b55aab40c46fa2b`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

   - Open your terminal and run:
     ```
     git clone https://github.com/illyaveksler/tasgf.git
     ```
   - Navigate to the backend directory:
     ```
     cd backend
     ```
   - Install dependencies:
     ```
     npm install
     ```

2. **Set Up the Database**:

   - Create an AWS account.
   - Create a credentials file in `~/.aws/credentials` with your AWS access key and secret access key:
    ```
   echo "[default]
   aws_access_key_id = <YOUR_ACCESS_KEY_ID>
   aws_secret_access_key = <YOUR_SECRET_ACCESS_KEY>" >> ~/.aws/credentials
    ```
   - Run the following commands to deploy the database:
   ```
   sudo npx sst tunnel install
   npx sst dev
   ```

Note: Since you won’t have my domain on AWS, you will need to also replace lines 24-30 in `sst.config.ts` with the following as well:
```
  loadBalancer: {
    rules: [
      { listen: "80/http" },
      { listen: "443/https", forward: "80/http" }
    ]
  }
```

3. **Run the Tests**:

   - To run all tests with coverage:
     ```
     npm run coverage
     ```
   - Alternatively, to run tests without coverage:
     ```
     npm test
     ```

4. **Review Test Logs**:

   - Test output includes detailed logs that indicate the pass/fail status of each test case.
   - Example log output for the questionnaires module might include:
     ```
     RUN  src/questionnaires/test/questionnaires.test.ts
     ✓ creates a questionnaire with associated questions (1363ms)
     ✓ throws an error if title is empty (339ms)
     ✓ throws an error if questions array is empty (326ms)
     ...
     ```

---

### 2.2. Test Coverage Report

After running the tests with coverage enabled (using `npm run coverage`), a report similar to the following is generated:

  ![Coverage Report](./images/screenshot1.png)

---

## 3. Automated Code Review Results

### 3.1. Commit Hash Where Automated Code Review Ran

`529c68280af42ccdc743433e75a246d88d65c302`

### 3.2. Unfixed Issues Summary

- **CRITICAL: Placing a void expression inside another expression is forbidden. Move it to its own statement instead.**
  - **Location:** `backend/src/auth.ts` at line 12  
  - **Code Reference:**  
    ```
    Promise.resolve(fn(req, res, next)).catch(next);
    ```
  - **Justification:**  
    This pattern helps simplify error handling in asynchronous middleware by catching and forwarding errors directly. Even though the tool flags it, our tests show it works reliably, and changing it now would require a lot of changes.

- **CRITICAL: Promise returned in function argument where a void return was expected.**
  - **Location:** `backend/src/auth.ts` at line 18  
  - **Code Reference:**  
    ```
    export const authenticateToken = asyncWrapper(async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    ```
  - **Justification:**  
    We use this approach to manage asynchronous token authentication smoothly. The async wrapper catches errors from the promise, and despite the tool’s warning, it’s proven to work in production. Refactoring it would mean major changes.

- **CRITICAL: Promise-returning function provided to return value where a void return was expected.**
  - **Location:** `backend/src/auth.ts` at line 11  
  - **Code Reference:**  
    ```
    return (req: Request, res: Response, next: NextFunction) =>
    ```
  - **Justification:**  
    This design choice lets us chain asynchronous middleware functions easily. Although the tool flags it, our extensive testing shows no issues. A full refactor would be too disruptive, so we’re keeping it as is.

---

### 3.3. Code Review Tool Dashboard

- **Unfixed Issues per Category:**  
  [Best Practice](https://app.codacy.com/gh/illyaveksler/tasgf/issues/current?categories=BestPractice)
  [Error Prone](https://app.codacy.com/gh/illyaveksler/tasgf/issues/current?categories=ErrorProne)
  [Security](https://app.codacy.com/gh/illyaveksler/tasgf/issues/current?categories=Security)

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/app/src/androidTest/java/com/example/groupformer`

### 4.2. Tests

- **Use Case: Form Groups**

  - **Expected Behaviors:**
    | **Scenario Steps**                   | **Test Case Steps**                              |
    |-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
    | 1. A professor user navigates to the form groups screen. | Form groups button is checked to exist on the Professor screen and clicked|
    | 2. Wait for surveys to be fetched | Wait 5s for SurveyItems to appear |
    | 3. Choose the first survey to appear to form groups in  | Click on first SurveyItem |
    | 4. User fills out 3 for group size | Find the TextField and enter 3  |
    | 5. System forms groups and displays groups of max 3 with Student Ids   |  Check that the text with "Groups: " shows up and each group has max 3 students  |
    | **Failure 2a:** No surveys to form groups | Check "No surveys Available" text shows up |
    | **Failure 2b:** Network Error in fetching surveys | Check for an "Error fetching surveys" text |
    | **Failure 3a:** Network Error in fetching survey details | Check for an "Error fetching survey" text |
    | **Failure 4a:** Forming group with empty input | Check for an error message saying "Please enter a valid group size" |
    | **Failure 5a:** Network Error in forming groups | Check for a network error message "Error forming groups" text |                       

  - **Test Logs:**
      ```
      > Task :app:connectedDebugAndroidTest
      Starting 1 tests on Pixel_9_API_31(AVD) - 12

      Finished 1 tests on Pixel_9_API_31(AVD) - 12

      BUILD SUCCESSFUL in 16s
      69 actionable tasks: 1 executed, 68 up-to-date

      Build Analyzer results available
      ```

- **Use Case: Submitting Surveys**

  - **Expected Behaviors:**

    | **Scenario Steps**                      | **Test Case Steps**                              |
    |-------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
    | 1. Student User wait for surveys to be fetched | Wait 5s for SurveyItems to appear |
    | 2. Choose the first survey to fill | Click on first SurveyItem  |
    | 3. User fills in StudentID | Find TextField and Enter "123"  |
    | 4. User fills in rest of Survey  | Find Next button and click it, automatically filling in 1 as the answer in the Survey  |
    | 5. User submits Survey  | Find Submit Button and click it  |
    | 6. User gets to Success screen | Check for success text |
    | **Failure 1a:** No surveys to fill | Check "No surveys available" text shows up |
    | **Failure 1b:** Network Error in fetching sruveys | Check for an "Error fetching surveys" text |
    | **Failure 2a:** Network Error in fetching survey details | Check for an "Error fetching survey" text |
    | **Failure 5a:** Network Error in submitting Survey | Check "Error Submitting Survey" text (To check this failure, I created an extra test that does steps 1-5 but fails on step 5) |


  - **Test Logs:**
      ```
      > Task :app:connectedDebugAndroidTest
      Starting 2 tests on Pixel_9_API_31(AVD) - 12

      Pixel_9_API_31(AVD) - 12 Tests 1/2 completed. (0 skipped) (0 failed)
      Finished 2 tests on Pixel_9_API_31(AVD) - 12

      BUILD SUCCESSFUL in 27s
      69 actionable tasks: 1 executed, 68 up-to-date

      Build Analyzer results available
      ```

- **Use Case: Create Survey**

  - **Expected Behaviors:**

    | **Scenario Steps**         | **Test Case Steps**       |
    |------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
    | 1. Professor user navigates  to create survey page | Click Create Survey Button |
    | 2. User fills in Title, Description | Find InputTextFields and Enter corresponding Title and Description  |
    | 3. User add Question to survey | Find QuestionInput Field and fill in text in the format of "On a scale of 1-5...?", then click add question |
    | 4. User add another Question to survey | Find QuestionInput Field and fill in text in the format of "On a scale of 1-5... ?", then click add question |
    | 5. User submits survey for Creation  | Find Create Survey button and click it  |
    | 6. User gets to Success screen | Check for "Survey Created Successfully!" text |
    | **Failure 2a:** Title and Description are empty when trying to submit survey | Check for "Title/Description must not be empty" text |
    | **Failure 3a:** Question is empty or not in the right format when trying to add question | Check for "Question must not be empty" or "Question must start with: On a scale of 1-5" text |
    | **Failure 5a:** No questions were added before submitting survey | Check for "You must add at least one question before creating" text |
    | **Failure 5b:** Network Error when submitting answers | Check for a "Error creating survey" message text |

- **Test Logs:**
    ```
    > Task :app:connectedDebugAndroidTest
    Starting 1 tests on Pixel_9_API_31(AVD) - 12

    Connected to process 20106 on device 'emulator-5554'.
    Finished 1 tests on Pixel_9_API_31(AVD) - 12

    BUILD SUCCESSFUL in 17s
    69 actionable tasks: 10 executed, 59 up-to-date

    Build Analyzer results available
    ```

## 5. Summary of Testing

- **Questionnaires Module:**
  - Validates questionnaire creation, retrieval, and error handling.
  - Tests include both positive flows (valid data) and negative flows (missing title or empty questions array).

- **StudentGroups Module:**
  - Verifies group generation logic based on student submissions.
  - Ensures proper error handling for cases such as invalid group size or missing submissions.
  - Checks that generated groups contain the correct student IDs without duplication.

- **Test Coverage:**
  - The project’s functional tests are executed using Vitest.
  - Coverage reports indicate robust testing for critical backend modules.

- **Automated Code Review:**
  - The automated review identified critical error-prone issues in the authentication module.
  - The critical issues in `backend/src/auth.ts` have been carefully justified based on architectural choices and stability in production. Although refactoring these patterns is not currently feasible without significant changes, thorough testing has ensured that they do not compromise system integrity.
