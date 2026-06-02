# Jenkins Job Configuration Reference

## Quick Start Configuration

### 1. Email Configuration (Jenkins UI)

Navigate to: **Manage Jenkins** → **Configure System** → **Extended E-mail Notification**

```
SMTP Server:                    smtp.gmail.com
SMTP Port:                      587
Authentication:                 ✓ (Enable)
Use SSL:                         ✓ (Enable)
Username:                       ashim.nayak2@gmail.com
Password:                       your-app-password

Default user E-mail suffix:     @gmail.com
From Address:                   ashim.nayak2@gmail.com
Reply-To Address:              ashimnayak2050@gmail.com
Charset:                        UTF-8
```

### 2. Gmail Configuration (OAuth2 Alternative)

If using Gmail with 2FA enabled:
1. Create App Password: https://myaccount.google.com/apppasswords
2. Use generated password in Jenkins SMTP settings
3. Do NOT use regular Gmail password

### 3. Job Parameter Setup

Go to **Hybrid-Selenium-Framework** job → **Configure** → **Pipeline**

```groovy
// Add parameters to your Jenkinsfile if not already present:

parameters {
    string(
        name: 'MAIL_TO',
        defaultValue: 'team@company.com',
        description: 'Recipients (comma-separated)'
    )
    string(
        name: 'MAIL_CC',
        defaultValue: 'manager@company.com',
        description: 'CC recipients (comma-separated)'
    )
    string(
        name: 'MAIL_SUBJECT',
        defaultValue: 'Hybrid Selenium Framework - Test Report',
        description: 'Email subject line'
    )
    choice(
        name: 'PARALLEL_NODES',
        choices: ['3', '4', '6'],
        description: 'Number of parallel nodes'
    )
    booleanParam(
        name: 'SEND_EMAIL',
        defaultValue: true,
        description: 'Send email after execution'
    )
}
```

---

## Node Configuration Examples

### Node: api-test-node

```
Name:                   api-test-node
Description:            API Automation Tests Execution
Remote root directory:  /var/jenkins_home/api-test-node
Number of executors:    4
Labels:                 test-node api-test parallel-node
Launch method:          Launch agents via SSH

SSH Configuration:
  Host:                 192.168.1.10
  Port:                 22
  Credentials:          jenkins-ssh-key
  Connection Timeout:   10 seconds
  Max Retries:          3
```

### Node: login-test-node

```
Name:                   login-test-node
Description:            Login/Auth Automation Tests
Remote root directory:  /var/jenkins_home/login-test-node
Number of executors:    4
Labels:                 test-node login-test parallel-node
Launch method:          Launch agents via SSH

SSH Configuration:
  Host:                 192.168.1.11
  Port:                 22
  Credentials:          jenkins-ssh-key
  Connection Timeout:   10 seconds
  Max Retries:          3
```

### Node: search-test-node

```
Name:                   search-test-node
Description:            Search Automation Tests
Remote root directory:  /var/jenkins_home/search-test-node
Number of executors:    4
Labels:                 test-node search-test parallel-node
Launch method:          Launch agents via SSH

SSH Configuration:
  Host:                 192.168.1.12
  Port:                 22
  Credentials:          jenkins-ssh-key
  Connection Timeout:   10 seconds
  Max Retries:          3
```

---

## Maven Build Configuration

### Build Commands Used

```bash
# Full build with tests
mvn clean install

# Build without tests
mvn clean compile -DskipTests

# Run specific test class
mvn test -Dtest=LoginTests

# Run with parallel threads
mvn test -Dthreads=4

# Run with specific XML suite
mvn test -DsuiteXmlFile=src/test/resources/testng.xml
```

### Environment Variables for Jenkins

Add to **Jenkins** → **Manage Jenkins** → **Configure System** → **Global properties**:

```
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
MAVEN_HOME=/usr/share/maven
MAVEN_OPTS=-Xms2g -Xmx4g
PROJECT_NAME=Hybrid-Selenium-Framework
REPORT_DIR=${WORKSPACE}/output/reports
BROWSER=CHROME
HEADLESS=true
```

---

## Email Template Variables

Available variables for email body customization:

```groovy
// Build Information
${BUILD_NUMBER}           // Build number
${BUILD_ID}               // Build ID
${BUILD_URL}              // Full build URL
${JOB_NAME}               // Job name
${JOB_URL}                // Job URL

// Test Results
${TEST_COUNTS}            // Test count summary
${FAILED_TESTS}           // Failed tests (if any)

// Environment
${WORKSPACE}              // Jenkins workspace path
${NODE_NAME}              // Current node name
${GIT_BRANCH}             // Current Git branch

// Status
${BUILD_STATUS}           // SUCCESS, FAILURE, etc.
${BUILD_RESULT}           // Result description
```

### Sample Email Variables Usage

```groovy
emailext(
    subject: "${params.MAIL_SUBJECT} - Build #${BUILD_NUMBER} - ${BUILD_STATUS}",
    body: """
    <h2>Build Summary</h2>
    <p>Job: ${JOB_NAME}</p>
    <p>Build: <a href="${BUILD_URL}">#${BUILD_NUMBER}</a></p>
    <p>Status: ${BUILD_RESULT}</p>
    <p>Workspace: ${WORKSPACE}</p>
    """,
    to: "${params.MAIL_TO}",
    cc: "${params.MAIL_CC}"
)
```

---

## Trigger Configuration

### GitHub Webhook Setup

1. Go to GitHub Repository
2. **Settings** → **Webhooks** → **Add webhook**
3. Configure:
   ```
   Payload URL:     http://jenkins.company.com/github-webhook/
   Content type:    application/json
   Events:          Just the push event
   Active:          ✓ (Enable)
   ```

### GitLab Webhook Setup

1. Go to GitLab Project
2. **Settings** → **Integrations** → **Add Webhook**
3. Configure:
   ```
   URL:             http://jenkins.company.com/project/Hybrid-Selenium-Framework
   Trigger:         Push events
   SSL verification: ✓ (Enable)
   ```

### Poll SCM Setup

In Jenkins job configure → Build Triggers → Poll SCM:

```
// Every 30 minutes
H/30 * * * *

// Every hour at minute 0
0 * * * *

// Every 15 minutes
*/15 * * * *

// Every weekday at 8 AM
0 8 * * 1-5

// Multiple times per day
0,6,12,18 * * * *
```

---

## Archive & Report Configuration

### Artifact Archiving

```groovy
archiveArtifacts artifacts: '''
    output/reports/**/*.html,
    output/reports/**/*.xml,
    target/surefire-reports/**/*.xml,
    target/surefire-reports/**/*.html,
    consolidated-reports/**/*
''',
allowEmptyArchive: true,
fingerprint: true
```

### JUnit Report Publishing

```groovy
junit testResults: '''
    target/surefire-reports/**/*.xml,
    consolidated-reports/**/*.xml
''',
allowEmptyResults: true,
skipPublishingChecks: true
```

### Email Attachments

```groovy
emailext(
    // ... other parameters ...
    attachmentsPattern: '''
        output/reports/**/*.html,
        target/surefire-reports/**/*.html,
        consolidated-reports/**/*.xml
    ''',
    compressLog: true,
    mimeType: 'text/html'
)
```

---

## Performance Tuning

### For 3 Parallel Nodes

```
Total Test Threads:       6-12
Threads per Node:         2-4
Heap Memory per Node:     2GB min, 4GB max
Test Data Size:           < 100MB per node
```

### For 6 Parallel Nodes

```
Total Test Threads:       12-24
Threads per Node:         2-4
Heap Memory per Node:     4GB min, 8GB max
Test Data Size:           < 50MB per node
Database Connections:     2-4 per node
```

### JVM Settings (in environment)

```bash
# Set in Jenkins job environment
export MAVEN_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Alternative for high parallelism
export MAVEN_OPTS="-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=100"
```

---

## Troubleshooting Checklist

### Emails Not Sending

- [ ] SMTP server is reachable: `telnet smtp.gmail.com 587`
- [ ] Jenkins credentials are correct
- [ ] TLS/SSL is properly configured
- [ ] Recipient emails are valid
- [ ] No firewall blocking SMTP port
- [ ] Email Extension Plugin is installed
- [ ] Run test email from Jenkins configuration

### Tests Not Running

- [ ] All nodes are online and have correct labels
- [ ] `parallel` block is in Jenkinsfile
- [ ] Maven is installed on all nodes
- [ ] Java 8+ is installed on all nodes
- [ ] Jenkins user has SSH access to slave nodes
- [ ] Test classes are found by Maven

### Reports Not Generated

- [ ] Extent Reports dependency is in pom.xml
- [ ] Report listeners are configured
- [ ] Output directory has write permissions
- [ ] Test execution completed successfully
- [ ] Report paths match archiveArtifacts patterns

### Parallel Execution Slow

- [ ] Reduce test threads per node
- [ ] Increase heap memory
- [ ] Use parallel database connections
- [ ] Check network latency between nodes
- [ ] Monitor CPU/Memory on each node

---

## Sample Build Parameters (Copy-Paste)

### Standard QA Build
```
MAIL_TO: qa-team@company.com
MAIL_CC: qa-lead@company.com
PARALLEL_NODES: 3
SEND_EMAIL: Yes
```

### Nightly Full Build
```
MAIL_TO: qa-team@company.com, devops@company.com
MAIL_CC: engineering-manager@company.com
PARALLEL_NODES: 6
SEND_EMAIL: Yes
```

### Regression Testing
```
MAIL_TO: qa-team@company.com
MAIL_CC: product-owner@company.com
PARALLEL_NODES: 4
SEND_EMAIL: Yes
```

---

## Links & Resources

- Email Extension Plugin: https://plugins.jenkins.io/email-ext/
- Jenkins Pipeline: https://www.jenkins.io/doc/book/pipeline/
- Extent Reports: https://www.extentreports.com/
- Selenium Documentation: https://www.selenium.dev/documentation/
- Maven Documentation: https://maven.apache.org/guides/

---

## Support

For issues or questions:
1. Check Jenkins logs: `/var/log/jenkins/jenkins.log`
2. Review pipeline console output
3. Enable debug logging in Jenkinsfile
4. Contact DevOps/QA team

---

**Last Updated:** June 2026
**Version:** 1.0
