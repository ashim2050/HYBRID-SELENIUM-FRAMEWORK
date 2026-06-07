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
                                rm -rf ${WORKSPACE}/output/reports/* ${WORKSPACE}/consolidated-reports/* || true
                                mkdir -p ${WORKSPACE}/output/reports
                                mkdir -p ${WORKSPACE}/consolidated-reports
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
                                mvn test -Dtest=ApiDataDrivenTests -DsuiteXmlFile=src/test/resources/testng.xml -Dheadless=${headlessMode} -Dreports.output.path=output/reports/api/ -Dsurefire.reportsDirectory=target/surefire-reports-api
                            """
                            stageResults['API Tests'] = 'SUCCESS'
                        } catch (err) {
                            stageResults['API Tests'] = 'FAILURE'
                            echo "API Tests Failed: ${err}"
                        } finally {
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports-api/TEST-*.xml'
                            archiveArtifacts artifacts: 'output/reports/api/*.html', allowEmptyArchive: true
                            sh '''
                                mkdir -p branch-output/output/reports/api
                                cp -r output/reports/api branch-output/output/reports/api 2>/dev/null || true
                            '''
                            stash includes: 'branch-output/output/reports/**', name: 'api-results', allowEmpty: true
                        }
                    }

                    branches['Login Tests'] = {
                        try {
                            sh """
                                cd ${WORKSPACE}
                                mvn test -Dtest=LoginTests -DsuiteXmlFile=src/test/resources/testng.xml -Dheadless=${headlessMode} -Dreports.output.path=output/reports/login/ -Dsurefire.reportsDirectory=target/surefire-reports-login
                            """
                            stageResults['Login Tests'] = 'SUCCESS'
                        } catch (err) {
                            stageResults['Login Tests'] = 'FAILURE'
                            echo "Login Tests Failed: ${err}"
                        } finally {
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports-login/TEST-*.xml'
                            archiveArtifacts artifacts: 'output/reports/login/*.html', allowEmptyArchive: true
                            sh '''
                                mkdir -p branch-output/output/reports/login
                                cp -r output/reports/login branch-output/output/reports/login 2>/dev/null || true
                            '''
                            stash includes: 'branch-output/output/reports/**', name: 'login-results', allowEmpty: true
                        }
                    }

                    branches['Search Tests'] = {
                        try {
                            sh """
                                cd ${WORKSPACE}
                                mvn test -Dtest=SearchTests -DsuiteXmlFile=src/test/resources/testng.xml -Dheadless=${headlessMode} -Dreports.output.path=output/reports/search/ -Dsurefire.reportsDirectory=target/surefire-reports-search
                            """
                            stageResults['Search Tests'] = 'SUCCESS'
                        } catch (err) {
                            stageResults['Search Tests'] = 'FAILURE'
                            echo "Search Tests Failed: ${err}"
                        } finally {
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports-search/TEST-*.xml'
                            archiveArtifacts artifacts: 'output/reports/search/*.html', allowEmptyArchive: true
                            sh '''
                                mkdir -p branch-output/output/reports/search
                                cp -r output/reports/search branch-output/output/reports/search 2>/dev/null || true
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

        stage('Test Results Consolidation') {
            steps {
                script {
                    echo "========== CONSOLIDATING TEST RESULTS =========="
                    unstash 'api-results'
                    unstash 'login-results'
                    unstash 'search-results'
                        sh '''
                        mkdir -p ${WORKSPACE}/consolidated-reports
                        cp -r branch-output/output/reports/* ${WORKSPACE}/consolidated-reports/ 2>/dev/null || true
                        # remove any XML files (TestNG/JUnit) from consolidated reports to keep only extent HTML
                        find ${WORKSPACE}/consolidated-reports -name '*.xml' -delete || true
                        cat > ${WORKSPACE}/consolidated-reports/index.html <<'INDEX'
<html>
<head><title>Consolidated Extent Reports</title></head>
<body>
<h1>Consolidated Extent Reports</h1>
<ul>
INDEX
                        find ${WORKSPACE}/consolidated-reports -name '*.html' ! -name 'index.html' | sort | while IFS= read -r f; do
                          rel=${f#${WORKSPACE}/}
                          echo "<li><a href=\"${rel}\">${rel}</a></li>" >> ${WORKSPACE}/consolidated-reports/index.html
                        done
                        cat >> ${WORKSPACE}/consolidated-reports/index.html <<'INDEX'
</ul>
</body>
</html>
INDEX
                        echo "Test results consolidated successfully"
                        ls -la ${WORKSPACE}/consolidated-reports/
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
                    def latestReport = sh(script: "find ${WORKSPACE}/consolidated-reports -name '*.html' ! -name 'index.html' | sort | tail -1 || true", returnStdout: true).trim()
                    def reportLink = latestReport ? "${env.BUILD_URL}artifact/${latestReport.replace("${WORKSPACE}/", '')}" : ''
                    def latestReportHtml = reportLink ? "<li><a href='${reportLink}'>View Latest HTML Report</a></li>" : ''

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

        <p><strong>Reports:</strong></p>
        <ul>
            <li><a href="${env.BUILD_URL}artifact/consolidated-reports/index.html">View Consolidated Report Index</a></li>
            <li><a href="${env.BUILD_URL}testReport/">View Test Report</a></li>
            ${latestReportHtml}
        </ul>

        <div class="footer">
            <p>This is an automated email. Please do not reply to this email.</p>
            <p>Generated on ${reportGeneratedAt}</p>
        </div>
    </div>
</body>
</html>
                    """

                    emailext(
                        subject: "${params.MAIL_SUBJECT} - Build #${buildNumber} - ${buildStatus}",
                        body: mailBody,
                        to: "${params.MAIL_TO}${params.MAIL_CC ? ',' + params.MAIL_CC : ''}",
                        mimeType: 'text/html',
                        attachmentsPattern: 'consolidated-reports/**/*.html,output/reports/**/*.html'
                    )
                }
            }
        }
    }

    post {
        always {
            script {
                echo "========== PIPELINE CLEANUP =========="
                
                // Archive only extent HTML reports
                archiveArtifacts artifacts: 'consolidated-reports/**/*.html,output/reports/**/*.html',
                                allowEmptyArchive: true,
                                fingerprint: true
                
                // Publish test results
                junit testResults: 'target/surefire-reports/**/*.xml',
                      allowEmptyResults: true,
                      skipPublishingChecks: true
                
                // Clean workspace
                cleanWs(
                    deleteDirs: true,
                    patterns: [
                        [pattern: 'consolidated-reports', type: 'INCLUDE'],
                        [pattern: 'target/surefire-reports/**', type: 'INCLUDE']
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
