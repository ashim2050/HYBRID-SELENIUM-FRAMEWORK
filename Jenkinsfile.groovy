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
                emailext(
                    subject: "${params.MAIL_SUBJECT} - Build #${buildNumber}",
                    body: emailBody,
                    to: params.MAIL_TO,
                    cc: ccList,
                    bcc: bccList,
                    mimeType: 'text/html',
                    attachmentsPattern: 'output/reports/**/*.html',
                    replyTo: params.MAIL_TO,
                    recipientProviders: [
                        developers(),
                        requestor(),
                        brokenBuildSuspects()
                    ],
                    compressLog: true
                )
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
            
            // Fetch active modules with full metadata as JSON
            def metadataJson = sh(
                script: "python3 ${WORKSPACE}/scripts/get_active_modules_with_metadata.py ${WORKSPACE}/Input/MasterConfig.xlsx",
                returnStdout: true
            ).trim()
            
            def modules = readJSON text: metadataJson
            echo "Active modules from MasterConfig.xlsx: ${modules.collect { it.displayName }.join(', ')}"
            
            def parallelJobs = [:]
            
            modules.eachWithIndex { module, index ->
                def moduleCfg = module
                def nodeLabel = nodeLabels[index % nodeLabels.size()]
                
                parallelJobs[moduleCfg.displayName] = {
                    node(nodeLabel) {
                        try {
                            echo "========== Executing ${moduleCfg.displayName} (${moduleCfg.testClass}) on ${nodeLabel} =========="
                            checkout scm
                            
                            sh '''
                                export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
                                export BROWSER=''' + params.BROWSER + '''
                                export PARALLEL_THREADS=''' + params.PARALLEL_THREADS + '''
                                export PATH=$JAVA_HOME/bin:$PATH
                                
                                mvn test \
                                    -Dtest=''' + moduleCfg.testClass + ''' \
                                    -Dthreads=''' + params.PARALLEL_THREADS + ''' \
                                    -DsuiteXmlFile=src/test/resources/testng.xml \
                                    -Dreports.output.path=output/reports/''' + moduleCfg.reportFolder + '''/ \
                                    -Dreports.file.name=''' + moduleCfg.reportPrefix + ''' \
                                    -Dbrowser=${BROWSER}
                            '''
                            
                            echo "✓ ${moduleCfg.displayName} completed"
                        } catch (Exception e) {
                            echo "✗ ${moduleCfg.displayName} failed: ${e.message}"
                            currentBuild.result = 'UNSTABLE'
                        } finally {
                            // Archive reports from slave node
                            sh '''
                                mkdir -p ''' + workspace + '''/slave-reports-''' + moduleCfg.moduleName + '''
                                if [ -d "output/reports/''' + moduleCfg.reportFolder + '''" ]; then
                                    cp -r output/reports/''' + moduleCfg.reportFolder + '''/* ''' + workspace + '''/slave-reports-''' + moduleCfg.moduleName + '''/ || true
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
            
            if (parallelJobs.isEmpty()) {
                echo 'No active modules configured in MasterConfig.xlsx. Skipping test execution.'
                return
            }
            
            // Execute all active modules in parallel
            parallel parallelJobs
        }

        stage('Preserve Module Reports') {
            echo "Preserving module-specific report folders under output/reports"
        }
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
                    script: '''find ${WORKSPACE}/output/reports -mindepth 2 -maxdepth 2 -name "*.html" | tr '\n' ',' | sed 's/,$//' ''',
                    returnStdout: true
                ).trim()
                
                // Fetch active modules for email body
                def metadataJson = sh(
                    script: "python3 ${WORKSPACE}/scripts/get_active_modules_with_metadata.py ${WORKSPACE}/Input/MasterConfig.xlsx",
                    returnStdout: true
                ).trim()
                def modules = readJSON text: metadataJson
                
                def emailBody = buildEmailBody(modules)
                
                def ccList = params.MAIL_CC ?: ''
                def bccList = params.MAIL_BCC ?: ''
                
                emailext(
                    subject: "${params.MAIL_SUBJECT} - Build #${buildNumber}",
                    body: emailBody,
                    to: params.MAIL_TO,
                    cc: ccList,
                    bcc: bccList,
                    mimeType: 'text/html',
                    attachmentsPattern: 'output/reports/**/*.html',
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
                        output/reports/ \
                    target/surefire-reports/ \
                    2>/dev/null || true
            '''
            
            archiveArtifacts artifacts: '''
                    output/reports/**/*.html,
                    output/reports/**/*.xml,
                target/surefire-reports/**/*,
                output/reports/**/*,
                test-reports-build-*.tar.gz
            ''',
            allowEmptyArchive: true,
            fingerprint: true
            
                junit testResults: 'output/reports/**/*.xml',
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

def buildEmailBody(modules) {
    def testList = modules.collect { m -> "<li>✅ ${m.displayName}</li>" }.join('\n')
    
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
                <tr>
                    <td>Active Modules</td>
                    <td>${modules.collect { it.displayName }.join(', ')}</td>
                </tr>
            </table>
            
            <h3>Test Summary (${modules.size()} Module${modules.size() > 1 ? 's' : ''})</h3>
            <ul>
                ${testList}
            </ul>
            
            <p>
                <a href="${env.BUILD_URL}artifact/output/reports/" class="button">View Module Reports</a>
            </p>
            
            <div class="footer">
                <p>This is an automated email generated by Jenkins. Please do not reply.</p>
                <p>Timestamp: ${new Date()}</p>
                <p>Pipeline: Dynamic Module Execution (from MasterConfig.xlsx)</p>
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
