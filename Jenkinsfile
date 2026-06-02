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
                        mvn clean compile -DskipTests -X
                    '''
                        }
                    }
        }

        stage('Parallel Test Execution') {
            parallel {
                stage('API Tests') {
                    steps {
                        script {
                            echo "========== RUNNING API TESTS =========="
                            sh '''
                                cd ${WORKSPACE}
                                mvn test -Dtest=ApiDataDrivenTests -DsuiteXmlFile=src/test/resources/testng.xml
                            '''
                        }
                    }
                    post {
                        always {
                            // Archive test results
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'
                            
                            // Archive Extent reports
                            archiveArtifacts artifacts: 'output/reports/*.html', 
                                            allowEmptyArchive: true
                        }
                        failure {
                            echo "API Tests Failed"
                        }
                    }
                }

                stage('Login Tests') {
                    steps {
                        script {
                            echo "========== RUNNING LOGIN TESTS =========="
                            sh '''
                                cd ${WORKSPACE}
                                mvn test -Dtest=LoginTests -DsuiteXmlFile=src/test/resources/testng.xml
                            '''
                        }
                    }
                    post {
                        always {
                            // Archive test results
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'
                            
                            // Archive Extent reports
                            archiveArtifacts artifacts: 'output/reports/*.html', 
                                            allowEmptyArchive: true
                        }
                        failure {
                            echo "Login Tests Failed"
                        }
                    }
                }

                stage('Search Tests') {
                    steps {
                        script {
                            echo "========== RUNNING SEARCH TESTS =========="
                            sh '''
                                cd ${WORKSPACE}
                                mvn test -Dtest=SearchTests -DsuiteXmlFile=src/test/resources/testng.xml
                            '''
                        }
                    }
                    post {
                        always {
                            // Archive test results
                            junit allowEmptyResults: true, testResults: 'target/surefire-reports/TEST-*.xml'
                            
                            // Archive Extent reports
                            archiveArtifacts artifacts: 'output/reports/*.html', 
                                            allowEmptyArchive: true
                        }
                        failure {
                            echo "Search Tests Failed"
                        }
                    }
                }
            }
        }

        stage('Test Results Consolidation') {
            steps {
                script {
                    echo "========== CONSOLIDATING TEST RESULTS =========="
                    sh '''
                        # Copy all test results to a central location
                        mkdir -p ${WORKSPACE}/consolidated-reports
                        
                        # Copy Extent Reports
                        if [ -d "${REPORT_DIR}" ]; then
                            cp -r ${REPORT_DIR}/* ${WORKSPACE}/consolidated-reports/ 2>/dev/null || true
                        fi
                        
                        # Copy TestNG Reports
                        if [ -d "${TEST_REPORT_DIR}" ]; then
                            cp -r ${TEST_REPORT_DIR}/* ${WORKSPACE}/consolidated-reports/ 2>/dev/null || true
                        fi
                        
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
                    
                    def reportFiles = sh(
                        script: 'find ${WORKSPACE}/output/reports -name "ExtentReport*.html" -type f | head -1',
                        returnStdout: true
                    ).trim()
                    
                    def buildStatus = currentBuild.result ?: 'SUCCESS'
                    def buildNumber = env.BUILD_NUMBER
                    def jobName = env.JOB_NAME
                    def testReportUrl = "${env.BUILD_URL}artifact/consolidated-reports/"
                    
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
    </style>
</head>
<body>
    <div class="header">
        <h2>🧪 ${PROJECT_NAME} - Test Execution Report</h2>
        <p>From: ashim.nayak2@gmail.com | To: ashimnayak2050@gmail.com</p>
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
                <th>Execution Time</th>
                <td>${currentBuild.durationString}</td>
            </tr>
        </table>
        
        <p><strong>Test Summary:</strong></p>
        <ul>
            <li>✅ API Tests - Parallel Execution</li>
            <li>✅ Login Tests - Parallel Execution</li>
            <li>✅ Search Tests - Parallel Execution</li>
        </ul>
        
        <p><strong>Reports:</strong></p>
        <ul>
            <li><a href="${testReportUrl}">View Consolidated Reports</a></li>
            <li><a href="${env.BUILD_URL}testReport/">View Test Report</a></li>
        </ul>
        
        <div class="footer">
            <p>This is an automated email. Please do not reply to this email.</p>
            <p>Generated on ${new Date()}</p>
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
                        attachmentsPattern: 'output/reports/**/*.html, target/surefire-reports/**/*.html',
                        recipientProviders: [
                            developers(),
                            requestor()
                        ],
                        compressLog: true
                    )
                }
            }
        }
    }

    post {
        always {
            script {
                echo "========== PIPELINE CLEANUP =========="
                
                // Archive all reports
                archiveArtifacts artifacts: 'output/reports/**/*.html, target/surefire-reports/**/*.html',
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
