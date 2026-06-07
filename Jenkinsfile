pipeline {
    agent any
    tools {
        jdk 'jdk21'
        maven 'MAVEN'
    }

    // Define parameters that can be configured in Jenkins job
    parameters {
        string(
            name: 'MAIL_TO',
            defaultValue: 'ashimnayak2050@gmail.com',
            description: 'Email address to send test reports to'
        )
        string(
            name: 'MAIL_CC',
            defaultValue: 'ashim.nayak2@gmail.com',
            description: 'CC email addresses (comma-separated)'
        )
        string(
            name: 'MAIL_SUBJECT',
            defaultValue: 'Hybrid Selenium Framework - Test Execution Report',
            description: 'Email subject line'
        )
        choice(
            name: 'PARALLEL_NODES',
            choices: ['3', '4', '6'],
            description: 'Number of parallel nodes for test execution'
        )
        booleanParam(
            name: 'HEADLESS',
            defaultValue: true,
            description: 'Run browser in headless mode (unchecked = headed)'
        )
        booleanParam(
            name: 'SEND_EMAIL',
            defaultValue: true,
            description: 'Send email with test reports after execution'
        )
    }

    // Environment variables
    environment {
        PROJECT_NAME = 'Hybrid-Selenium-Framework'
        WORKSPACE_DIR = "${WORKSPACE}"
        REPORT_DIR = "${WORKSPACE}/output/reports"
        TEST_REPORT_DIR = "${WORKSPACE}/target/surefire-reports"
    }

    // Define build triggers
    triggers {
        // Trigger on SCM changes
        pollSCM('H/30 * * * *')
        
        // Uncomment for webhook triggers (GitHub, GitLab, Bitbucket)
        // githubPush()
    }

    // Build options
    options {
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        timeout(time: 2, unit: 'HOURS')
        timestamps()
        ansiColor('xterm')
    }

    stages {
        stage('Build') {
                    steps {
                        script {
                            echo "========== BUILDING PROJECT =========="
                            sh '''
                                rm -rf ${WORKSPACE}/output/reports ${WORKSPACE}/branch-output || true
                                mkdir -p ${WORKSPACE}/output/reports
                                mvn clean compile -DskipTests
                            '''
                        }
                    }
        }

        stage('Parallel Test Execution') {
            steps {
                script {
                    echo "========== RUNNING PARALLEL TESTS =========="

                    def headlessMode = params.HEADLESS ? 'true' : 'false'
                    def stageResults = [:]
                    def branches = [:]

                    branches['API Tests'] = {
                        try {
                            sh """
                                cd ${WORKSPACE}
                                mvn test -Dtest=ApiDataDrivenTests -DsuiteXmlFile=src/test/resources/testng.xml -Dheadless=${headlessMode} -Dreports.output.path=output/reports/api/ -Dreports.file.name=ExtentReport_api.html -Dsurefire.reportsDirectory=target/surefire-reports-api
                            """
                            stageResults['API Tests'] = 'SUCCESS'
                        } catch (err) {
                            stageResults['API Tests'] = 'FAILURE'
                            echo "API Tests Failed: ${err}"
                        } finally {
                                sh '''
                                rm -rf branch-output/output/reports/api || true
                                mkdir -p branch-output/output/reports/api
                                cp -r output/reports/api/* branch-output/output/reports/api/ 2>/dev/null || true
                            '''
                            stash includes: 'branch-output/output/reports/**', name: 'api-results', allowEmpty: true
                        }
                    }

                    branches['Login Tests'] = {
                        try {
                            sh """
                                cd ${WORKSPACE}
                                mvn test -Dtest=LoginTests -DsuiteXmlFile=src/test/resources/testng.xml -Dheadless=${headlessMode} -Dreports.output.path=output/reports/login/ -Dreports.file.name=ExtentReport_login.html -Dsurefire.reportsDirectory=target/surefire-reports-login
                            """
                            stageResults['Login Tests'] = 'SUCCESS'
                        } catch (err) {
                            stageResults['Login Tests'] = 'FAILURE'
                            echo "Login Tests Failed: ${err}"
                        } finally {
                                sh '''
                                rm -rf branch-output/output/reports/login || true
                                mkdir -p branch-output/output/reports/login
                                cp -r output/reports/login/* branch-output/output/reports/login/ 2>/dev/null || true
                            '''
                            stash includes: 'branch-output/output/reports/**', name: 'login-results', allowEmpty: true
                        }
                    }

                    branches['Search Tests'] = {
                        try {
                            sh """
                                cd ${WORKSPACE}
                                mvn test -Dtest=SearchTests -DsuiteXmlFile=src/test/resources/testng.xml -Dheadless=${headlessMode} -Dreports.output.path=output/reports/search/ -Dreports.file.name=ExtentReport_search.html -Dsurefire.reportsDirectory=target/surefire-reports-search
                            """
                            stageResults['Search Tests'] = 'SUCCESS'
                        } catch (err) {
                            stageResults['Search Tests'] = 'FAILURE'
                            echo "Search Tests Failed: ${err}"
                        } finally {
                                sh '''
                                rm -rf branch-output/output/reports/search || true
                                mkdir -p branch-output/output/reports/search
                                cp -r output/reports/search/* branch-output/output/reports/search/ 2>/dev/null || true
                            '''
                            stash includes: 'branch-output/output/reports/**', name: 'search-results', allowEmpty: true
                        }
                    }

                    parallel branches

                    writeFile file: 'parallel-results.txt', text: stageResults.collect { k, v -> "${k}: ${v}" }.join('\n')
                    if (stageResults.values().contains('FAILURE')) {
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        currentBuild.result = 'SUCCESS'
                    }
                }
            }
        }

        stage('Collect Test Results') {
            steps {
                script {
                    echo "========== COLLECTING TEST REPORTS =========="
                    unstash 'api-results'
                    unstash 'login-results'
                    unstash 'search-results'
                        sh '''
                        rm -rf ${WORKSPACE}/output/reports/api ${WORKSPACE}/output/reports/login ${WORKSPACE}/output/reports/search || true
                        mkdir -p ${WORKSPACE}/output/reports/api ${WORKSPACE}/output/reports/login ${WORKSPACE}/output/reports/search
                        cp -r branch-output/output/reports/api/* ${WORKSPACE}/output/reports/api/ 2>/dev/null || true
                        cp -r branch-output/output/reports/login/* ${WORKSPACE}/output/reports/login/ 2>/dev/null || true
                        cp -r branch-output/output/reports/search/* ${WORKSPACE}/output/reports/search/ 2>/dev/null || true
                        echo "Test reports collected successfully"
                        ls -la ${WORKSPACE}/output/reports/api ${WORKSPACE}/output/reports/login ${WORKSPACE}/output/reports/search || true
                    '''
                }
            }
        }

        stage('Generate Consolidated Report') {
            steps {
                script {
                    echo "========== GENERATING CONSOLIDATED EXTENT REPORT =========="
                    sh '''
                    BUILD_NUM=${BUILD_NUMBER:-0}
                    GEN_DATE=$(date +'%Y-%m-%d %H:%M:%S')
                    CONSOL_TIMESTAMP=$(date +'%Y%m%d_%H%M%S')
                    
                    # Find latest timestamped reports for each node
                    API_REPORT=$(find ${WORKSPACE}/output/reports/api -name "ExtentReport_api_*.html" -type f | sort | tail -1 | xargs basename 2>/dev/null || echo "ExtentReport_api.html")
                    LOGIN_REPORT=$(find ${WORKSPACE}/output/reports/login -name "ExtentReport_login_*.html" -type f | sort | tail -1 | xargs basename 2>/dev/null || echo "ExtentReport_login.html")
                    SEARCH_REPORT=$(find ${WORKSPACE}/output/reports/search -name "ExtentReport_search_*.html" -type f | sort | tail -1 | xargs basename 2>/dev/null || echo "ExtentReport_search.html")
                    
                    # Create consolidated report dashboard with links to open reports in new tabs
                    cat > ${WORKSPACE}/output/reports/ExtentReport_Consolidated_${CONSOL_TIMESTAMP}.html <<CONSOL
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Consolidated Extent Reports Dashboard</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 40px 20px;
        }
        .container { max-width: 1000px; margin: 0 auto; }
        .header { 
            background-color: rgba(255, 255, 255, 0.95);
            padding: 30px; 
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            text-align: center;
            margin-bottom: 40px;
        }
        .header h1 { 
            color: #333;
            margin-bottom: 10px;
            font-size: 32px;
        }
        .header p {
            color: #666;
            font-size: 14px;
            margin: 5px 0;
        }
        .reports-grid { 
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
            gap: 25px;
            margin-bottom: 40px;
        }
        .report-card {
            background-color: white;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            overflow: hidden;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        .report-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
        }
        .report-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            text-align: center;
        }
        .report-header h2 {
            font-size: 22px;
            margin-bottom: 5px;
        }
        .report-header .icon {
            font-size: 40px;
            margin-bottom: 10px;
        }
        .report-body {
            padding: 25px;
            text-align: center;
        }
        .report-file {
            color: #999;
            font-size: 12px;
            margin-bottom: 15px;
            word-break: break-all;
        }
        .open-btn {
            display: inline-block;
            background-color: #667eea;
            color: white;
            padding: 12px 30px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            font-weight: bold;
            text-decoration: none;
            transition: background-color 0.3s;
        }
        .open-btn:hover {
            background-color: #764ba2;
        }
        .footer { 
            background-color: rgba(255, 255, 255, 0.95);
            padding: 20px; 
            border-radius: 8px;
            text-align: center;
            color: #666;
            font-size: 12px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
        .stats {
            display: flex;
            gap: 20px;
            justify-content: center;
            flex-wrap: wrap;
            margin-top: 15px;
        }
        .stat-item {
            background-color: #f0f0f0;
            padding: 10px 15px;
            border-radius: 5px;
            font-size: 13px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🧪 Test Execution Report Dashboard</h1>
            <p><strong>Build #${BUILD_NUM}</strong></p>
            <p>Generated on <strong>${GEN_DATE}</strong></p>
        </div>

        <div class="reports-grid">
            <div class="report-card">
                <div class="report-header">
                    <div class="icon">🔌</div>
                    <h2>API Tests</h2>
                </div>
                <div class="report-body">
                    <div class="report-file">📄 ${API_REPORT}</div>
                    <a href="api/${API_REPORT}" target="_blank" class="open-btn">View Report</a>
                </div>
            </div>

            <div class="report-card">
                <div class="report-header">
                    <div class="icon">🔐</div>
                    <h2>Login Tests</h2>
                </div>
                <div class="report-body">
                    <div class="report-file">📄 ${LOGIN_REPORT}</div>
                    <a href="login/${LOGIN_REPORT}" target="_blank" class="open-btn">View Report</a>
                </div>
            </div>

            <div class="report-card">
                <div class="report-header">
                    <div class="icon">🔍</div>
                    <h2>Search Tests</h2>
                </div>
                <div class="report-body">
                    <div class="report-file">📄 ${SEARCH_REPORT}</div>
                    <a href="search/${SEARCH_REPORT}" target="_blank" class="open-btn">View Report</a>
                </div>
            </div>
        </div>

        <div class="footer">
            <p>✅ Click "View Report" on any card above to open the detailed test report in a new tab</p>
            <div class="stats">
                <div class="stat-item">🐳 Build: #${BUILD_NUM}</div>
                <div class="stat-item">📅 Date: ${GEN_DATE}</div>
                <div class="stat-item">📦 Modules: 3</div>
            </div>
        </div>
    </div>
</body>
</html>
CONSOL

                    echo "Consolidated report generated successfully"
                    ls -lh ${WORKSPACE}/output/reports/ExtentReport_Consolidated.html
                    '''
                }
            }
        }

        stage('Email Report') {
            when {
                expression { params.SEND_EMAIL == true }
            }
            steps {
                script {
                    echo "========== SENDING EMAIL REPORT =========="
                    
                    // Verify attachments exist
                    sh '''
                    echo "Verifying attachments exist:"
                    ls -lh ${WORKSPACE}/output/reports/ExtentReport_Consolidated.html 2>/dev/null || echo "Consolidated report not found"
                    ls -lh ${WORKSPACE}/output/reports/api/*.html 2>/dev/null || echo "API reports not found"
                    ls -lh ${WORKSPACE}/output/reports/login/*.html 2>/dev/null || echo "Login reports not found"
                    ls -lh ${WORKSPACE}/output/reports/search/*.html 2>/dev/null || echo "Search reports not found"
                    '''

                    def parallelSummary = fileExists('parallel-results.txt') ? readFile('parallel-results.txt').trim() : 'Parallel stage summary not available.'
                    def tests = 0
                    def failures = 0
                    def skipped = 0
                    if (fileExists('target/surefire-reports')) {
                        def summary = sh(script: """
                            set +e
                            find target/surefire-reports -name 'TEST-*.xml' -print0 2>/dev/null |
                              xargs -0 grep -Eo 'tests="[0-9]+"|failures="[0-9]+"|skipped="[0-9]+"' |
                              sed -E 's/.*="([0-9]+)"/\\1/' | paste - - - |
                              awk '{t += \$1; f += \$2; s += \$3} END {print t","f","s}'
                        """, returnStdout: true).trim()
                        if (summary) {
                            def parts = summary.split(',')
                            if (parts.size() == 3) {
                                tests = parts[0].toInteger()
                                failures = parts[1].toInteger()
                                skipped = parts[2].toInteger()
                            }
                        }
                    }
                    def buildStatus = currentBuild.result ?: 'SUCCESS'
                    def buildNumber = env.BUILD_NUMBER
                    def jobName = env.JOB_NAME
                    def reportGeneratedAt = sh(script: "date +'%Y-%m-%d %H:%M:%S'", returnStdout: true).trim()

                    def mailBody = """
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; }
        .header { background-color: #333; color: white; padding: 10px; }
        .content { padding: 15px; }
        .status-success { color: green; font-weight: bold; }
        .status-failure { color: red; font-weight: bold; }
        table { border-collapse: collapse; width: 100%; margin-top: 10px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        .footer { color: #666; font-size: 12px; margin-top: 15px; }
        .summary-box { background: #f9f9f9; border: 1px solid #ddd; padding: 12px; margin-top: 12px; }
    </style>
</head>
<body>
    <div class="header">
        <h2>🧪 ${PROJECT_NAME} - Test Execution Report</h2>
        <p>From: ${params.MAIL_CC ?: params.MAIL_TO} | To: ${params.MAIL_TO}</p>
    </div>
    <div class="content">
        <p><strong>Build Information:</strong></p>
        <table>
            <tr>
                <th>Job Name</th>
                <td>${jobName}</td>
            </tr>
            <tr>
                <th>Build Number</th>
                <td><a href="${env.BUILD_URL}">#${buildNumber}</a></td>
            </tr>
            <tr>
                <th>Build Status</th>
                <td class="status-${buildStatus.toLowerCase()}">${buildStatus}</td>
            </tr>
            <tr>
                <th>Parallel Nodes</th>
                <td>${params.PARALLEL_NODES}</td>
            </tr>
            <tr>
                <th>Browser Mode</th>
                <td>${params.HEADLESS ? 'headless' : 'headed'}</td>
            </tr>
            <tr>
                <th>Execution Time</th>
                <td>${currentBuild.durationString}</td>
            </tr>
        </table>

        <div class="summary-box">
            <p><strong>Aggregated Test Counts:</strong></p>
            <table>
                <tr><th>Total Tests</th><td>${tests}</td></tr>
                <tr><th>Failures</th><td>${failures}</td></tr>
                <tr><th>Skipped</th><td>${skipped}</td></tr>
            </table>
        </div>

        <div class="summary-box">
            <p><strong>Parallel Node Results:</strong></p>
            <pre>${parallelSummary}</pre>
        </div>

        <div class="footer">
            <p>This is an automated email. Please do not reply to this email.</p>
            <p>Generated on ${reportGeneratedAt}</p>
        </div>
    </div>
</body>
</html>
                    """

                    // Verify report files exist before emailing
                    sh '''
                    echo "========== VERIFYING REPORT FILES =========="
                    find ${WORKSPACE}/output/reports -name "*.html" -type f
                    '''

                    emailext(
                        subject: "${params.MAIL_SUBJECT} - Build #${buildNumber} - ${buildStatus}",
                        body: mailBody,
                        to: "${params.MAIL_TO}${params.MAIL_CC ? ',' + params.MAIL_CC : ''}",
                        mimeType: 'text/html',
                        attachmentsPattern: 'output/reports/**/*.html'
                    )
                }
            }
        }
    }

    post {
        always {
            script {
                echo "========== PIPELINE CLEANUP =========="
                
                // Archive only Extent HTML reports from this run
                archiveArtifacts artifacts: 'output/reports/**/*.html',
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // Clean workspace - but keep output/reports for access
                cleanWs(
                    deleteDirs: true,
                    patterns: [
                        [pattern: 'target/surefire-reports/**', type: 'INCLUDE'],
                        [pattern: 'branch-output/**', type: 'INCLUDE']
                    ]
                )
            }
        }
        
        success {
            script {
                echo "========== BUILD SUCCESSFUL =========="
                currentBuild.result = 'SUCCESS'
            }
        }
        
        failure {
            script {
                echo "========== BUILD FAILED =========="
                currentBuild.result = 'FAILURE'
                
                // Send failure notification email if configured
                if (params.SEND_EMAIL) {
                    emailext(
                        subject: "⚠️ ${params.MAIL_SUBJECT} - Build #${env.BUILD_NUMBER} - FAILED",
                        body: """
                        <h2>Build Failed!</h2>
                        <p>Job: ${env.JOB_NAME}</p>
                        <p>Build: <a href="${env.BUILD_URL}">#${env.BUILD_NUMBER}</a></p>
                        <p>Check the logs: <a href="${env.BUILD_URL}console">Console Output</a></p>
                        """,
                        to: "${params.MAIL_TO}${params.MAIL_CC ? ',' + params.MAIL_CC : ''}",
                        mimeType: 'text/html'
                    )
                }
            }
        }
        
        unstable {
            echo "========== BUILD UNSTABLE =========="
        }
    }
}
