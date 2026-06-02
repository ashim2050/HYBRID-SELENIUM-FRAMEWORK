# Hybrid Selenium Framework - Jenkins Pipeline Setup Guide

## Overview
This guide explains how to set up Jenkins for parallel test execution of the Hybrid Selenium Framework with Extent Report email notifications.

## Prerequisites

### Jenkins Requirements
- Jenkins 2.289+ (LTS recommended)
- Java 8 or higher
- Maven 3.6+
- Git plugin
- Email Extension Plugin
- Pipeline Plugin
- Timestamper Plugin
- AnsiColor Plugin

### Required Jenkins Plugins
```
1. Email Extension Plugin (v2.69+)
2. Pipeline Plugin (v2.6+)
3. Blue Ocean (optional but recommended)
4. Pipeline: Stage View
5. Git plugin (v4.4+)
6. Timestamper Plugin
7. AnsiColor Plugin
8. JUnit Plugin
```

### System Requirements
- At least 3 slave nodes for parallel execution
- Each node needs:
  - Java 8+
  - Maven 3.6+
  - Browser (Chrome/Firefox/Edge)
  - ChromeDriver/GeckoDriver installed
  - At least 4GB RAM

---

## Installation Steps

### Step 1: Install Required Jenkins Plugins

1. Navigate to **Jenkins Dashboard** → **Manage Jenkins** → **Manage Plugins**
2. Go to **Available** tab
3. Search and install:
   - Email Extension
   - Pipeline
   - Blue Ocean
   - Timestamper
   - AnsiColor
4. Restart Jenkins

### Step 2: Configure Email Notifications

#### Option A: SMTP Configuration (Built-in)
1. Go to **Manage Jenkins** → **Configure System**
2. Scroll to **E-mail Notification** section:
   ```
   SMTP Server: smtp.gmail.com
   SMTP Port: 587
   Username: your-email@gmail.com
   Password: your-app-password
   Use SSL: ✓
   ```

#### Option B: Using Extended Email Plugin
1. Go to **Manage Jenkins** → **Configure System**
2. Scroll to **Extended E-mail Notification**
3. Configure:
   ```
   SMTP Server: smtp.gmail.com
   SMTP Port: 587
   Default user E-mail suffix: @yourcompany.com
   From Address: jenkins@yourcompany.com
   Reply-To Address: qa-team@yourcompany.com
   Use SSL: ✓
   Use TLS: ✓
   ```

### Step 3: Create Slave Nodes for Parallel Execution

#### Node 1: API Test Node
1. **Dashboard** → **Manage Jenkins** → **Manage Nodes and Clouds**
2. Click **New Node**
   - Node name: `api-test-node`
   - Type: Permanent Agent
   - Configure:
     ```
     Description: For API automated tests
     Remote root directory: /var/jenkins_home/api-test-node
     Labels: test-node api-test
     Launch method: Launch agents via SSH
     SSH Server Details:
       - Host: <node-ip>
       - Credentials: ssh-credentials
       - Port: 22
       - Connection Timeout: 10
     ```

#### Node 2: Login Test Node
- Node name: `login-test-node`
- Labels: `test-node login-test`
- Remote directory: `/var/jenkins_home/login-test-node`

#### Node 3: Search Test Node
- Node name: `search-test-node`
- Labels: `test-node search-test`
- Remote directory: `/var/jenkins_home/search-test-node`

### Step 4: Create New Jenkins Job

1. Click **New Item**
2. Enter job name: `Hybrid-Selenium-Framework`
3. Select: **Pipeline**
4. Click **OK**

### Step 5: Configure Pipeline Job

#### General Settings
- Enable: **Do not allow concurrent builds**
- Build timeout: **2 hours**
- Discard old builds: Keep last 30 builds

#### Pipeline Configuration
```
Definition: Pipeline script from SCM
SCM: Git
Repository URL: <your-git-repo>
Credentials: <your-git-credentials>
Branch: */main (or your branch)
Script Path: Jenkinsfile
```

Or directly use **Pipeline script** tab:

**Option 1: Use Declarative Pipeline (Jenkinsfile)**
```groovy
// Copy content from Jenkinsfile (declarative approach)
```

**Option 2: Use Scripted Pipeline (Jenkinsfile.groovy)**
```groovy
// Copy content from Jenkinsfile.groovy (scripted approach)
```

#### Build Triggers
Configure when the job should run:

**Option 1: Poll SCM**
```
Schedule: H/30 * * * *  (Every 30 minutes)
```

**Option 2: GitHub Webhook**
- Go to GitHub repo → Settings → Webhooks
- Add: `http://jenkins-server:8080/github-webhook/`

**Option 3: GitLab Webhook**
- Go to GitLab repo → Settings → Integrations
- Add: `http://jenkins-server:8080/project/Hybrid-Selenium-Framework`

---

## Job Parameters Configuration

The pipeline supports the following configurable parameters:

### Email Parameters
| Parameter | Default Value | Description |
|-----------|---------------|-------------|
| MAIL_TO | ashimnayak2050@gmail.com | Email recipients (comma-separated) |
| MAIL_CC | ashim.nayak2@gmail.com | CC recipients |
| MAIL_BCC | (empty) | BCC recipients |
| MAIL_SUBJECT | Hybrid Selenium Framework - Test Execution Report | Email subject line |
| SEND_EMAIL | true | Enable/Disable email notifications |

### Execution Parameters
| Parameter | Choices | Description |
|-----------|---------|-------------|
| PARALLEL_NODES | 3, 4, 6 | Number of parallel nodes |
| PARALLEL_THREADS | 3, 4, 6, 8 | Number of parallel threads (scripted only) |
| BROWSER | CHROME, FIREFOX, EDGE | Browser selection (scripted only) |
| HEADLESS_MODE | true/false | Run in headless mode (scripted only) |

---

## Running the Pipeline

### Method 1: Manual Trigger
1. Go to **Hybrid-Selenium-Framework** job
2. Click **Build with Parameters**
3. Fill in parameters:
   ```
   MAIL_TO: your-email@company.com
   MAIL_CC: team-lead@company.com
   PARALLEL_NODES: 3
   SEND_EMAIL: ✓
   ```
4. Click **Build**

### Method 2: Scheduled Trigger
Job runs automatically based on configured schedule

### Method 3: SCM Webhook
Job triggers on Git push/merge

---

## Email Report Details

### Email Content Includes:
1. **Build Information**
   - Build number with link to Jenkins
   - Job name
   - Build status (SUCCESS/FAILED)
   - Duration
   - Parallel nodes used
   - Browser type

2. **Test Summary**
   - API Tests results
   - Login Tests results
   - Search Tests results
   - Overall status

3. **Reports & Links**
   - Extent Report HTML attachments
   - TestNG Report links
   - Direct links to individual test reports
   - Console output link

4. **Attachments**
   - All Extent Report HTML files
   - TestNG XML reports
   - Consolidated test reports

### Sample Email Subject:
```
Hybrid Selenium Framework - Test Execution Report - Build #45 - SUCCESS
```

---

## Troubleshooting

### Issue 1: Email not sending

**Solution:**
1. Check **Manage Jenkins** → **Configure System** → **E-mail Notification**
2. Click **Test Configuration** with your email
3. Check mail logs: `/var/log/jenkins/jenkins.log`
4. Verify SMTP server settings and credentials
5. For Gmail, use App Passwords (not regular password)

### Issue 2: Slave nodes offline

**Solution:**
1. Check node agent connectivity:
   ```
   Dashboard → Manage Jenkins → Manage Nodes → [Node Name]
   ```
2. Check logs for errors
3. Verify SSH credentials and network connectivity
4. Restart node agent:
   ```
   systemctl restart jenkins-slave-api-test-node
   ```

### Issue 3: Tests not running in parallel

**Solution:**
1. Verify all 3 nodes are online and have correct labels
2. Check Jenkinsfile has `parallel` block
3. Monitor Blue Ocean view to see which node each test runs on
4. Check agent availability: **Manage Jenkins** → **Manage Nodes**

### Issue 4: Reports not attached to email

**Solution:**
1. Verify reports exist: `output/reports/` directory
2. Check `archiveArtifacts` pattern in pipeline
3. Verify Email Extension Plugin version is 2.69+
4. Check file permissions on Jenkins workspace

---

## Advanced Configurations

### Custom Extent Report Template
Modify report template in `src/main/java/com/framework/listeners/ExtentReportListener.java`

### Slack Notifications (Optional)
Add to Jenkinsfile:
```groovy
stage('Slack Notification') {
    steps {
        slackSend(
            channel: '#qa-automation',
            color: currentBuild.result == 'SUCCESS' ? 'good' : 'danger',
            message: "Test Report: ${env.BUILD_URL}"
        )
    }
}
```

### Database Logging
Store test results in database:
```groovy
stage('Log Results') {
    steps {
        sh '''
            java -cp . com.framework.utils.DatabaseLogger \
                --build=${BUILD_NUMBER} \
                --status=${BUILD_STATUS} \
                --report=${REPORT_DIR}
        '''
    }
}
```

---

## Performance Optimization

### For 6 Parallel Nodes:
```groovy
- API Tests: Thread Pool 2
- Login Tests: Thread Pool 2  
- Search Tests: Thread Pool 2
```

### Memory Configuration:
Each node should have:
- Minimum Heap: 2GB (`-Xms2g`)
- Maximum Heap: 4GB (`-Xmx4g`)

### Configure in Jenkins:
**Manage Jenkins** → **Configure System** → **Global properties**
```
MAVEN_OPTS=-Xms2g -Xmx4g
```

---

## Security Considerations

1. **Credentials Management**
   - Use Jenkins Credentials Store for sensitive data
   - Never hardcode passwords in Jenkinsfile

2. **Access Control**
   - Restrict job access to QA team
   - Use Jenkins Role-based Access Control (RBAC)

3. **Email Security**
   - Use TLS/SSL for SMTP
   - Use service account for Jenkins emails
   - Encrypt sensitive email data

4. **Node Security**
   - Use SSH key-based authentication for agents
   - Restrict node access
   - Keep Jenkins and plugins updated

---

## Maintenance

### Regular Tasks
- **Weekly**: Check pipeline logs for errors
- **Monthly**: Update Jenkins plugins
- **Quarterly**: Review and archive old reports
- **Quarterly**: Update test data and configurations

### Backup Strategy
```bash
# Backup Jenkins configuration
tar -czf jenkins-backup-$(date +%Y%m%d).tar.gz \
  /var/lib/jenkins/jobs/Hybrid-Selenium-Framework

# Backup reports
tar -czf test-reports-$(date +%Y%m%d).tar.gz \
  /var/lib/jenkins/jobs/Hybrid-Selenium-Framework/builds/*/archive
```

---

## References

- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Email Extension Plugin](https://plugins.jenkins.io/email-ext/)
- [Extent Reports](https://www.extentreports.com/)
- [TestNG Documentation](https://testng.org/doc/)
- [Maven Documentation](https://maven.apache.org/guides/)

---

## Support & Questions

For issues or questions regarding this setup:
1. Check Jenkins logs: `/var/log/jenkins/jenkins.log`
2. Review pipeline console output
3. Consult the troubleshooting section above
4. Contact DevOps/QA Lead

---

**Last Updated:** June 2026
**Version:** 1.0
