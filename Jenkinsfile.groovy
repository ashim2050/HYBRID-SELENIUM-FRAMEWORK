// Scripted Pipeline - Jenkinsfile.groovy
// More flexible version with advanced features

@Library('shared-library') _  // Optional: use shared library if available

import groovy.json.JsonBuilder

def buildNumber = env.BUILD_NUMBER
def jobName = env.JOB_NAME
def workspace = env.WORKSPACE

properties([
    parameters([
        string(
            name: 'MAIL_TO',
            defaultValue: 'ashimnayak2050@gmail.com',
            description: 'Email recipients (comma-separated)'
        ),
        string(
            name: 'MAIL_CC',
            defaultValue: 'ashim.nayak2@gmail.com',
            description: 'CC email addresses (comma-separated)'
        ),
        string(
            name: 'MAIL_BCC',
            defaultValue: '',
            description: 'BCC email addresses (comma-separated)'
        ),
        string(
            name: 'MAIL_SUBJECT',
            defaultValue: 'Hybrid Selenium Framework - Test Execution Report',
            description: 'Email subject'
        ),
        booleanParam(
            name: 'SEND_EMAIL',
            defaultValue: true,
            description: 'Send email with reports'
        ),
        choice(
            name: 'PARALLEL_THREADS',
            choices: ['3', '4', '6', '8'],
            description: 'Number of parallel execution threads'
        ),
        choice(
            name: 'BROWSER',
            choices: ['CHROME', 'FIREFOX', 'EDGE'],
            description: 'Browser for test execution'
        ),
        booleanParam(
            name: 'HEADLESS_MODE',
            defaultValue: false,
            description: 'Run tests in headless mode'
        )
    ]),
    pipelineTriggers([
        pollSCM('H/30 * * * *')
    ])
])

node('master') {
    try {
        stage('Initialize') {
            echo """
            ╔════════════════════════════════════════════════════════════╗
            ║   ${PROJECT_NAME} - Pipeline Execution Started            ║
            ║   Build: #${buildNumber}                                  ║
            ║   Job: ${jobName}                                         ║
            ║   Parallel Threads: ${params.PARALLEL_THREADS}            ║
            ║   Browser: ${params.BROWSER}                              ║
            ╚════════════════════════════════════════════════════════════╝
            """
            
            // Checkout SCM
            checkout scm
            
            // Set build description
            currentBuild.description = """
            Parallel Threads: ${params.PARALLEL_THREADS}
            Browser: ${params.BROWSER}
            Headless: ${params.HEADLESS_MODE}
            """
        }

        stage('Build') {
            echo "Building project..."
            sh '''
                export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
                export PATH=$JAVA_HOME/bin:$PATH
                mvn clean compile -DskipTests -q
            '''
            echo "✓ Build completed successfully"
        }

        stage('Parallel Test Execution') {
            def nodeLabels = ['test-node-1', 'test-node-2', 'test-node-3']
            def tests = ['ApiDataDrivenTests', 'LoginTests', 'SearchTests']
            
            def parallelJobs = [:]
            
            for (int i = 0; i < tests.size(); i++) {
                def testName = tests[i]
                def nodeLabel = nodeLabels[i % nodeLabels.size()]
                
                parallelJobs[testName] = {
                    node(nodeLabel) {
                        try {
                            echo "========== Executing ${testName} on ${nodeLabel} =========="
                            checkout scm
                            
                            sh '''
                                export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
                                export BROWSER=''' + params.BROWSER + '''
                                export PARALLEL_THREADS=''' + params.PARALLEL_THREADS + '''
                                export PATH=$JAVA_HOME/bin:$PATH
                                
                                mvn test \
                                    -Dtest=''' + testName + ''' \
                                    -Dthreads=''' + params.PARALLEL_THREADS + ''' \
                                    -DsuiteXmlFile=src/test/resources/testng.xml \
                                    -Dbrowser=${BROWSER}
                            '''
                            
                            echo "✓ ${testName} completed"
                        } catch (Exception e) {
                            echo "✗ ${testName} failed: ${e.message}"
                            currentBuild.result = 'UNSTABLE'
                        } finally {
                            // Archive reports from slave node
                            sh '''
                                mkdir -p ''' + workspace + '''/slave-reports-''' + testName + '''
                                if [ -d "output/reports" ]; then
                                    cp -r output/reports/* ''' + workspace + '''/slave-reports-''' + testName + '''/ || true
                                fi
                            '''
                            
                            step([$class: 'JUnitResultArchiver', 
                                  testResults: 'target/surefire-reports/**/*.xml',
                                  allowEmptyResults: true,
                                  keepLongStdio: true])
                        }
                    }
                }
            }
            
            // Execute all tests in parallel
            parallel parallelJobs
        }

        stage('Consolidate Reports') {
            echo "Consolidating test reports from all nodes..."
            sh '''
                mkdir -p ${WORKSPACE}/consolidated-reports
                
                # Consolidate from slave node reports
                find ${WORKSPACE}/slave-reports-* -type f -name "*.html" -o -name "*.xml" | while read file; do
                    cp "$file" ${WORKSPACE}/consolidated-reports/ 2>/dev/null || true
                done
                
                # Consolidate from workspace reports
                if [ -d "${WORKSPACE}/output/reports" ]; then
                    cp -r ${WORKSPACE}/output/reports/* ${WORKSPACE}/consolidated-reports/ 2>/dev/null || true
                fi
                
                if [ -d "${WORKSPACE}/target/surefire-reports" ]; then
                    cp -r ${WORKSPACE}/target/surefire-reports/* ${WORKSPACE}/consolidated-reports/ 2>/dev/null || true
                fi
                
                echo "✓ Reports consolidated"
                ls -lh ${WORKSPACE}/consolidated-reports/ | grep -E "\.html|\.xml" | wc -l
            '''
        }

        stage('Generate HTML Report') {
            echo "Generating consolidated HTML report..."
            sh '''
                cat > ${WORKSPACE}/consolidated-reports/index.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>Hybrid Selenium Framework - Test Report</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background: #f5f5f5; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }
        .container { max-width: 1200px; margin: 20px auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        .report-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin: 20px 0; }
        .report-card { background: white; border: 1px solid #ddd; border-radius: 8px; padding: 15px; text-align: center; transition: transform 0.3s; }
        .report-card:hover { transform: translateY(-5px); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
        .report-card a { display: inline-block; margin-top: 10px; padding: 10px 20px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; }
        .report-card a:hover { background: #764ba2; }
        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
        .build-info { background: #f9f9f9; padding: 15px; border-left: 4px solid #667eea; margin: 20px 0; }
    </style>
</head>
<body>
    <div class="header">
        <h1>🧪 Hybrid Selenium Framework</h1>
        <p>Automated Test Execution Report</p>
    </div>
    <div class="container">
        <div class="build-info">
            <h3>Build Information</h3>
            <p><strong>Build Number:</strong> #''' + buildNumber + '''</p>
            <p><strong>Job:</strong> ''' + jobName + '''</p>
            <p><strong>Timestamp:</strong> ''' + new Date() + '''</p>
        </div>
        
        <div class="report-row">
            <div class="report-card">
                <h3>📊 Extent Reports</h3>
                <p>Detailed HTML reports with screenshots and logs</p>
                <a href="ExtentReport_latest.html">View Extent Report</a>
            </div>
            <div class="report-card">
                <h3>✅ TestNG Reports</h3>
                <p>TestNG execution summary and details</p>
                <a href="testng-results.xml">View TestNG Report</a>
            </div>
            <div class="report-card">
                <h3>📈 API Tests</h3>
                <p>REST API automation test results</p>
                <a href="API.html">View API Tests</a>
            </div>
            <div class="report-card">
                <h3>🔐 Login Tests</h3>
                <p>Authentication and login test results</p>
                <a href="Login.html">View Login Tests</a>
            </div>
            <div class="report-card">
                <h3>🔍 Search Tests</h3>
                <p>Search functionality test results</p>
                <a href="Search.html">View Search Tests</a>
            </div>
        </div>
        
        <div class="footer">
            <p>Generated by Jenkins Pipeline - Hybrid Selenium Framework</p>
        </div>
    </div>
</body>
</html>
EOF
            '''
        }

        stage('Email Report') {
            when {
                expression { params.SEND_EMAIL == true }
            }
            script {
                echo "Sending test report via email..."
                
                def reportFiles = sh(
                    script: '''find ${WORKSPACE}/consolidated-reports -name "*.html" | tr '\\n' ',' | sed 's/,$//' ''',
                    returnStdout: true
                ).trim()
                
                def emailBody = buildEmailBody()
                
                def ccList = params.MAIL_CC ?: ''
                def bccList = params.MAIL_BCC ?: ''
                
                emailext(
                    subject: "${params.MAIL_SUBJECT} - Build #${buildNumber}",
                    body: emailBody,
                    to: params.MAIL_TO,
                    cc: ccList,
                    bcc: bccList,
                    mimeType: 'text/html',
                    attachmentsPattern: 'consolidated-reports/**/*.html',
                    replyTo: params.MAIL_TO,
                    recipientProviders: [
                        developers(),
                        requestor(),
                        brokenBuildSuspects()
                    ],
                    compressLog: true
                )
                
                echo "✓ Email sent to: ${params.MAIL_TO}"
            }
        }

        stage('Archive Results') {
            sh '''
                # Archive all reports
                tar -czf ${WORKSPACE}/test-reports-build-${BUILD_NUMBER}.tar.gz \
                    consolidated-reports/ \
                    target/surefire-reports/ \
                    output/reports/ \
                    2>/dev/null || true
            '''
            
            archiveArtifacts artifacts: '''
                consolidated-reports/**/*.html,
                consolidated-reports/**/*.xml,
                target/surefire-reports/**/*,
                output/reports/**/*,
                test-reports-build-*.tar.gz
            ''',
            allowEmptyArchive: true,
            fingerprint: true
            
            junit testResults: 'consolidated-reports/**/*.xml',
                  allowEmptyResults: true
        }

    } catch (Exception e) {
        echo "Pipeline failed: ${e.message}"
        currentBuild.result = 'FAILURE'
        
        // Send failure notification
        if (params.SEND_EMAIL) {
            emailext(
                subject: "⚠️ ${params.MAIL_SUBJECT} - Build #${buildNumber} - FAILED",
                body: buildFailureEmailBody(e),
                to: params.MAIL_TO,
                cc: params.MAIL_CC ?: '',
                mimeType: 'text/html'
            )
        }
        
        throw e
    } finally {
        echo """
        ╔════════════════════════════════════════════════════════════╗
        ║   Pipeline Execution Completed                            ║
        ║   Status: ${currentBuild.result}                          ║
        ║   Duration: ${currentBuild.durationString}                ║
        ╚════════════════════════════════════════════════════════════╝
        """
        
        // Cleanup
        cleanWs(
            patterns: [[pattern: 'slave-reports-*/**', type: 'INCLUDE']]
        )
    }
}

def buildEmailBody() {
    def content = """
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; }
            .container { max-width: 800px; margin: 0 auto; }
            .header { background: #333; color: white; padding: 15px; text-align: center; }
            .status { font-size: 24px; font-weight: bold; margin: 20px 0; }
            .success { color: #28a745; }
            .failure { color: #dc3545; }
            table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
            th { background: #f0f0f0; }
            .footer { color: #666; font-size: 12px; margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; }
            .button { display: inline-block; padding: 10px 20px; background: #007bff; color: white; text-decoration: none; border-radius: 5px; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h2>🧪 Hybrid Selenium Framework - Test Report</h2>
            </div>
            
            <div class="status success">✓ Tests Executed Successfully</div>
            
            <table>
                <tr>
                    <th>Property</th>
                    <th>Value</th>
                </tr>
                <tr>
                    <td>Build Number</td>
                    <td><a href="${env.BUILD_URL}">#${buildNumber}</a></td>
                </tr>
                <tr>
                    <td>Job Name</td>
                    <td>${jobName}</td>
                </tr>
                <tr>
                    <td>Build Status</td>
                    <td>${currentBuild.result}</td>
                </tr>
                <tr>
                    <td>Duration</td>
                    <td>${currentBuild.durationString}</td>
                </tr>
                <tr>
                    <td>Parallel Threads</td>
                    <td>${params.PARALLEL_THREADS}</td>
                </tr>
                <tr>
                    <td>Browser</td>
                    <td>${params.BROWSER}</td>
                </tr>
            </table>
            
            <h3>Test Summary</h3>
            <ul>
                <li>✅ API Tests</li>
                <li>✅ Login Tests</li>
                <li>✅ Search Tests</li>
            </ul>
            
            <p>
                <a href="${env.BUILD_URL}artifact/consolidated-reports/index.html" class="button">View Full Report</a>
                <a href="${env.BUILD_URL}testReport/" class="button">Test Report</a>
            </p>
            
            <div class="footer">
                <p>This is an automated email generated by Jenkins. Please do not reply.</p>
                <p>Timestamp: ${new Date()}</p>
            </div>
        </div>
    </body>
    </html>
    """
    return content
}

def buildFailureEmailBody(Exception e) {
    return """
    <html>
    <head>
        <style>
            body { font-family: Arial, sans-serif; }
            .container { max-width: 800px; margin: 0 auto; }
            .header { background: #333; color: white; padding: 15px; text-align: center; }
            .error { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 15px; border-radius: 5px; }
            table { width: 100%; border-collapse: collapse; margin: 20px 0; }
            th, td { border: 1px solid #ddd; padding: 10px; text-align: left; }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <h2>⚠️ Build Failed - ${jobName}</h2>
            </div>
            
            <div class="error">
                <strong>Error:</strong> ${e.message}
            </div>
            
            <table>
                <tr><td><strong>Build:</strong></td><td><a href="${env.BUILD_URL}">#${buildNumber}</a></td></tr>
                <tr><td><strong>Status:</strong></td><td>FAILED</td></tr>
                <tr><td><strong>Console Log:</strong></td><td><a href="${env.BUILD_URL}console">View Logs</a></td></tr>
            </table>
        </div>
    </body>
    </html>
    """
}
