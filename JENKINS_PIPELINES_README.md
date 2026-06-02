# Jenkins Pipeline Setup for Hybrid Selenium Framework

## 📋 Overview

This project includes **3 complete Jenkinsfiles** for running automated tests in parallel with email reporting capabilities:

1. **Jenkinsfile** - Declarative Pipeline (Recommended for beginners)
2. **Jenkinsfile.groovy** - Scripted Pipeline (Advanced features & flexibility)
3. **Jenkinsfile.docker** - Docker-based Pipeline (Containerized execution)

---

## 🚀 Quick Start

### Option 1: Declarative Pipeline (Recommended)

**Best for:** Standard parallel execution on 3-6 nodes

```bash
# 1. Push Jenkinsfile to your repository
git add Jenkinsfile
git commit -m "Add Jenkinsfile for Jenkins"
git push

# 2. Create new job in Jenkins
# - Name: Hybrid-Selenium-Framework
# - Type: Pipeline
# - Definition: Pipeline script from SCM
# - Git Repository URL: <your-repo>
# - Script Path: Jenkinsfile

# 3. Build with parameters
# - MAIL_TO: ashimnayak2050@gmail.com
# - MAIL_CC: ashim.nayak2@gmail.com
# - PARALLEL_NODES: 3
# - SEND_EMAIL: ✓
```

### Option 2: Scripted Pipeline (Advanced)

**Best for:** Complex workflows, custom logic, advanced features

```bash
# Use Jenkinsfile.groovy instead
# In Jenkins job configuration → Pipeline → Definition
# Select "Pipeline script from SCM"
# Script Path: Jenkinsfile.groovy
```

### Option 3: Docker Pipeline

**Best for:** Containerized execution, isolated environments

```bash
# Prerequisites:
# - Docker installed on Jenkins master/agents
# - Dockerfile in repository

# Build Docker image:
docker build -t hybrid-selenium:latest .

# Use Jenkinsfile.docker
# Script Path: Jenkinsfile.docker
```

---

## 📁 Files Included

### Pipeline Files

| File | Type | Use Case |
|------|------|----------|
| **Jenkinsfile** | Declarative | Standard parallel execution (3-6 nodes) |
| **Jenkinsfile.groovy** | Scripted | Advanced workflows, custom logic |
| **Jenkinsfile.docker** | Docker | Containerized execution |

### Configuration & Documentation

| File | Purpose |
|------|---------|
| **JENKINS_SETUP_GUIDE.md** | Complete step-by-step setup guide |
| **JENKINS_CONFIG_REFERENCE.md** | Quick configuration reference |
| **Dockerfile** | Docker image for containerized tests |
| **jenkins-setup.sh** | Automated Jenkins configuration script |

---

## 🔧 Setup Requirements

### Jenkins Server
- Jenkins 2.289+ (LTS recommended)
- Java 8+
- Maven 3.6+
- 4GB+ RAM

### Required Plugins
```
Email Extension
Pipeline
Blue Ocean
Timestamper
AnsiColor
JUnit
Git
SSH Slaves (for node execution)
```

### Test Nodes (For Parallel Execution)
- At least 3 nodes (api-test-node, login-test-node, search-test-node)
- Each with:
  - Java 8+
  - Maven 3.6+
  - Chrome/Firefox browser
  - WebDriver installed
  - 4GB+ RAM

### Email Configuration
- SMTP Server (Gmail, Office 365, or corporate SMTP)
- SMTP Port (usually 587 for TLS)
- Valid sender credentials

---

## ⚡ Quick Configuration Checklist

- [ ] **Step 1:** Install Jenkins plugins (Email Extension, Pipeline, etc.)
- [ ] **Step 2:** Configure SMTP in Jenkins Settings
- [ ] **Step 3:** Create SSH credentials for slave nodes
- [ ] **Step 4:** Create test nodes (api-test-node, login-test-node, search-test-node)
- [ ] **Step 5:** Create new Pipeline job pointing to Jenkinsfile
- [ ] **Step 6:** Add job parameters (MAIL_TO, MAIL_CC, PARALLEL_NODES)
- [ ] **Step 7:** Test build with parameters
- [ ] **Step 8:** Verify email received with reports

---

## 📊 Pipeline Features

### All Pipelines Include:

✅ **Parallel Execution** - Run 3-6 tests simultaneously
✅ **Email Notifications** - Automatic report delivery
✅ **Extent Reports** - Beautiful HTML test reports
✅ **Configurable Parameters** - Email, threads, browser settings
✅ **Artifact Archiving** - All reports stored in Jenkins
✅ **Error Handling** - Graceful failure handling
✅ **Console Logging** - Detailed execution logs

### Declarative Pipeline (Jenkinsfile)

```groovy
✓ Clean, easy-to-read syntax
✓ Built-in validation
✓ Better for simple workflows
✓ Recommended for teams
✓ 3 parallel test stages
```

### Scripted Pipeline (Jenkinsfile.groovy)

```groovy
✓ Maximum flexibility
✓ Dynamic configuration
✓ Advanced features (loops, conditions)
✓ Custom DSL
✓ Multiple browser support
✓ Headless mode option
```

### Docker Pipeline (Jenkinsfile.docker)

```groovy
✓ Container-based execution
✓ Isolated test environments
✓ No node dependencies
✓ Scalable to any number
✓ Consistent execution
✓ Easy to replicate
```

---

## 🎯 Use Cases

### Scenario 1: Nightly Regression Tests
```bash
# Configuration
Jenkinsfile: Jenkinsfile (Declarative)
Schedule: 0 2 * * * (2 AM daily)
PARALLEL_NODES: 6
SEND_EMAIL: Yes (to qa-team@company.com)
```

### Scenario 2: CI/CD Integration
```bash
# Configuration
Jenkinsfile: Jenkinsfile.groovy (Scripted)
Trigger: GitHub webhook on push
PARALLEL_NODES: 3
SEND_EMAIL: Only on failure
```

### Scenario 3: Docker-based Testing
```bash
# Configuration
Jenkinsfile: Jenkinsfile.docker
Build Docker Image: Yes
PARALLEL_CONTAINERS: 4
No external nodes needed
```

---

## 📧 Email Configuration Examples

### Gmail (with 2FA)
```
SMTP Server: smtp.gmail.com
SMTP Port: 587
Username: your-email@gmail.com
Password: <app-password>  (not regular password)
Use TLS: Yes
```

### Office 365
```
SMTP Server: smtp.office365.com
SMTP Port: 587
Username: your-email@company.onmicrosoft.com
Password: <your-password>
Use TLS: Yes
```

### Corporate SMTP
```
SMTP Server: mail.company.com
SMTP Port: 25 or 587
Username: <your-username>
Password: <your-password>
Use TLS: Yes (if required)
```

---

## 🔍 Job Parameters Explained

| Parameter | Default | Description | Example |
|-----------|---------|-------------|---------|
| MAIL_TO | team@example.com | Email recipients | qa-team@company.com |
| MAIL_CC | manager@example.com | CC recipients | qa-lead@company.com |
| MAIL_BCC | (empty) | Hidden recipients | (optional) |
| MAIL_SUBJECT | Test Report | Email subject | Nightly Test Results |
| SEND_EMAIL | true | Enable/disable email | true/false |
| PARALLEL_NODES | 3 | Number of parallel nodes | 3, 4, or 6 |
| PARALLEL_THREADS | 4 | Threads per node | 3, 4, 6, or 8 |
| BROWSER | CHROME | Browser type | CHROME, FIREFOX, EDGE |
| HEADLESS_MODE | false | Headless execution | true/false |

---

## 📝 Sample Build Commands

### Run Immediate Build
```bash
# Via curl
curl -X POST \
  -F "MAIL_TO=qa@company.com" \
  -F "PARALLEL_NODES=3" \
  -F "SEND_EMAIL=true" \
  http://jenkins:8080/job/Hybrid-Selenium-Framework/buildWithParameters

# Via Jenkins CLI
java -jar jenkins-cli.jar -s http://jenkins:8080 \
  build "Hybrid-Selenium-Framework" \
  -p MAIL_TO=qa@company.com \
  -p PARALLEL_NODES=3
```

### Schedule Automatic Execution
```bash
# Poll SCM every 30 minutes
H/30 * * * *

# Daily at 2 AM
0 2 * * *

# Every weekday at 9 AM
0 9 * * 1-5

# Multiple times daily
0,6,12,18 * * * *
```

---

## 🐛 Troubleshooting

### Problem: Emails not sending
**Solution:**
1. Check SMTP settings: **Manage Jenkins** → **Configure System**
2. Test email: Click "Test Configuration"
3. Check firewall: Ensure port 587 is open
4. Review logs: `/var/log/jenkins/jenkins.log`

### Problem: Tests not running in parallel
**Solution:**
1. Verify all nodes are online: **Manage Jenkins** → **Manage Nodes**
2. Check node labels match stage agent definitions
3. Monitor Blue Ocean view to see execution flow
4. Increase executors on each node if needed

### Problem: Reports not attached to email
**Solution:**
1. Verify `archiveArtifacts` pattern matches your reports
2. Check file permissions on Jenkins workspace
3. Ensure Email Extension Plugin version is 2.69+
4. Check Jenkins logs for attachment errors

### Problem: Docker containers failing
**Solution:**
1. Build image: `docker build -t hybrid-selenium:latest .`
2. Test image: `docker run hybrid-selenium:latest mvn -version`
3. Check volume mounts: Verify workspace path
4. Review Docker logs: `docker logs <container-id>`

---

## 📚 Documentation Files

1. **JENKINS_SETUP_GUIDE.md** - Complete setup walkthrough
2. **JENKINS_CONFIG_REFERENCE.md** - Configuration examples
3. **README.md** (this file) - Overview and quick start

---

## 🔐 Security Best Practices

1. **Credentials Management**
   - Use Jenkins Credentials Store for all secrets
   - Never commit passwords to Git
   - Use service accounts for Jenkins

2. **Email Security**
   - Use TLS/SSL for SMTP connections
   - Use app-specific passwords for Gmail
   - Restrict email recipient access

3. **Node Access**
   - Use SSH key-based authentication
   - Restrict SSH access to Jenkins IPs
   - Keep Jenkins and plugins updated

---

## 🚀 Performance Optimization

### For 3 Parallel Nodes
```
Total Threads: 6-8
RAM per node: 2GB minimum
Database connections: 2-3
```

### For 6 Parallel Nodes
```
Total Threads: 12-16
RAM per node: 4GB minimum
Database connections: 4-6
```

### JVM Configuration
```bash
export MAVEN_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"
```

---

## 📞 Support & Resources

- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Email Extension Plugin Docs](https://plugins.jenkins.io/email-ext/)
- [Extent Reports Guide](https://www.extentreports.com/docs/v5/)
- [TestNG Documentation](https://testng.org/doc/)
- [Selenium Documentation](https://www.selenium.dev/documentation/)

---

## 📋 Checklist for First-Time Setup

- [ ] Jenkins installed and running
- [ ] All required plugins installed
- [ ] SMTP configured and tested
- [ ] SSH credentials created for nodes
- [ ] Test nodes created and online
- [ ] Jenkinsfile committed to Git repository
- [ ] Pipeline job created in Jenkins
- [ ] Job parameters configured
- [ ] Test build executed successfully
- [ ] Email received with reports attached

---

## 🎉 Next Steps

1. **Choose your pipeline type** (Declarative, Scripted, or Docker)
2. **Follow JENKINS_SETUP_GUIDE.md** for step-by-step instructions
3. **Configure email in Jenkins**
4. **Create test nodes**
5. **Run your first build!**

---

## 📄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | June 2026 | Initial release with 3 Jenkinsfiles |

---

**Created:** June 2026  
**Last Updated:** June 2026  
**Maintained by:** QA Engineering Team
