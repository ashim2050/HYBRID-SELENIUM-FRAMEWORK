#!/bin/bash

################################################################################
# Jenkins Configuration Automation Script
# This script helps automate Jenkins setup for Hybrid Selenium Framework
# Usage: ./jenkins-setup.sh
################################################################################

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
JENKINS_URL="http://localhost:8080"
JENKINS_USERNAME="admin"
JENKINS_PASSWORD=""
JOB_NAME="Hybrid-Selenium-Framework"
GIT_REPO="https://github.com/your-org/hybrid-selenium-framework.git"
MAIL_TO="ashimnayak2050@gmail.com"
MAIL_CC="ashim.nayak2@gmail.com"
SMTP_SERVER="smtp.gmail.com"
SMTP_PORT="587"

################################################################################
# Functions
################################################################################

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}  $1"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# Check if Jenkins CLI is available
check_jenkins_cli() {
    print_info "Checking Jenkins CLI..."
    
    if ! command -v jenkins-cli &> /dev/null; then
        print_warning "Jenkins CLI not found. Installing..."
        curl -fsSL ${JENKINS_URL}/jnlpJars/jenkins-cli.jar -o jenkins-cli.jar
        print_success "Jenkins CLI installed"
    else
        print_success "Jenkins CLI found"
    fi
}

# Function to interact with Jenkins API
jenkins_curl() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$data" ]; then
        curl -s -X "$method" \
            -u "${JENKINS_USERNAME}:${JENKINS_PASSWORD}" \
            "${JENKINS_URL}${endpoint}"
    else
        curl -s -X "$method" \
            -u "${JENKINS_USERNAME}:${JENKINS_PASSWORD}" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "${JENKINS_URL}${endpoint}"
    fi
}

# Create Jenkins slave node
create_slave_node() {
    local node_name=$1
    local node_ip=$2
    local node_label=$3
    
    print_info "Creating slave node: $node_name"
    
    local node_config=$(cat <<EOF
<?xml version='1.1' encoding='UTF-8'?>
<org.jenkinsci.plugins.remotingworkspace.jar.ComputerImpl>
  <name>${node_name}</name>
  <description>Slave node for ${node_label} tests</description>
  <remoteFS>/var/jenkins_home/${node_name}</remoteFS>
  <numExecutors>4</numExecutors>
  <mode>NORMAL</mode>
  <retentionStrategy class="hudson.slaves.RetentionStrategy\$Always"/>
  <launcher class="hudson.plugins.sshslaves.SSHLauncher" plugin="ssh-slaves@1.32.0">
    <host>${node_ip}</host>
    <port>22</port>
    <credentialsId>jenkins-ssh-key</credentialsId>
    <maxNumRetries>3</maxNumRetries>
    <retryWaitTime>5</retryWaitTime>
  </launcher>
  <label>${node_label}</label>
  <nodeProperties/>
</org.jenkinsci.plugins.remotingworkspace.jar.ComputerImpl>
EOF
)
    
    jenkins_curl POST "/computer/${node_name}/config.xml" "$node_config"
    
    if [ $? -eq 0 ]; then
        print_success "Slave node $node_name created"
    else
        print_error "Failed to create slave node $node_name"
    fi
}

# Configure SMTP email
configure_email() {
    print_header "Configuring Email Settings"
    
    print_info "This requires manual configuration in Jenkins UI or using XML API"
    print_info "SMTP Server: ${SMTP_SERVER}"
    print_info "SMTP Port: ${SMTP_PORT}"
    print_info "From Address: jenkins@company.com"
}

# Create job from template
create_job() {
    print_header "Creating Jenkins Job"
    
    # Read Jenkinsfile content
    if [ ! -f "Jenkinsfile" ]; then
        print_error "Jenkinsfile not found in current directory"
        return 1
    fi
    
    local job_config=$(cat <<EOF
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@2.41">
  <description>Hybrid Selenium Framework - Automated Test Execution with Parallel Nodes</description>
  <displayName>${JOB_NAME}</displayName>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.87">
    <scm class="hudson.plugins.git.GitSCM" plugin="git@4.11.5">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>${GIT_REPO}</url>
          <credentialsId>github-credentials</credentialsId>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>*/main</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
    </scm>
    <scriptPath>Jenkinsfile</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers>
    <com.cloudbees.jenkins.plugins.kubernetes.trigger.PushTrigger plugin="kubernetes@1.33.7">
      <secret/>
    </com.cloudbees.jenkins.plugins.kubernetes.trigger.PushTrigger>
    <hudson.triggers.TimerTrigger>
      <spec>H/30 * * * *</spec>
    </hudson.triggers.TimerTrigger>
  </triggers>
  <properties>
    <hudson.model.ParametersDefinitionProperty>
      <parameterDefinitions>
        <hudson.model.StringParameterDefinition>
          <name>MAIL_TO</name>
          <description>Email recipients</description>
          <defaultValue>${MAIL_TO}</defaultValue>
          <trim>false</trim>
        </hudson.model.StringParameterDefinition>
        <hudson.model.StringParameterDefinition>
          <name>MAIL_CC</name>
          <description>CC recipients</description>
          <defaultValue>${MAIL_CC}</defaultValue>
          <trim>false</trim>
        </hudson.model.StringParameterDefinition>
        <hudson.model.StringParameterDefinition>
          <name>PARALLEL_NODES</name>
          <description>Number of parallel nodes</description>
          <defaultValue>3</defaultValue>
          <trim>false</trim>
        </hudson.model.StringParameterDefinition>
        <hudson.model.BooleanParameterDefinition>
          <name>SEND_EMAIL</name>
          <description>Send email with reports</description>
          <defaultValue>true</defaultValue>
        </hudson.model.BooleanParameterDefinition>
      </parameterDefinitions>
    </hudson.model.ParametersDefinitionProperty>
  </properties>
</flow-definition>
EOF
)
    
    print_info "Creating job: ${JOB_NAME}"
    jenkins_curl POST "/createItem?name=${JOB_NAME}" "$job_config"
    
    if [ $? -eq 0 ]; then
        print_success "Job ${JOB_NAME} created successfully"
    else
        print_error "Failed to create job ${JOB_NAME}"
        return 1
    fi
}

# Test Jenkins connectivity
test_connectivity() {
    print_info "Testing Jenkins connectivity..."
    
    local response=$(jenkins_curl GET "/api/json" "")
    
    if echo "$response" | grep -q "jobs"; then
        print_success "Connected to Jenkins successfully"
        return 0
    else
        print_error "Failed to connect to Jenkins"
        print_error "Response: $response"
        return 1
    fi
}

# Install required plugins
install_plugins() {
    print_header "Installing Required Plugins"
    
    local plugins=(
        "email-ext"
        "workflow-aggregator"
        "blue-ocean"
        "timestamper"
        "ansicolor"
        "junit"
        "git"
        "ssh-slaves"
    )
    
    for plugin in "${plugins[@]}"; do
        print_info "Installing plugin: $plugin"
        jenkins_curl POST "/pluginManager/installPlugins" \
            "{\"plugins\": [{\"name\": \"$plugin\"}]}"
        
        if [ $? -eq 0 ]; then
            print_success "Plugin $plugin installed"
        else
            print_warning "Plugin $plugin installation may require manual intervention"
        fi
    done
}

# Configure system
configure_system() {
    print_header "Configuring Jenkins System Settings"
    
    print_info "This requires manual configuration in Jenkins UI:"
    print_info "1. Go to Manage Jenkins → Configure System"
    print_info "2. Configure E-mail Notification:"
    print_info "   - SMTP Server: ${SMTP_SERVER}"
    print_info "   - SMTP Port: ${SMTP_PORT}"
    print_info "   - Use SSL: Yes"
    print_info "3. Configure Extended E-mail Notification"
    print_info "4. Save and Apply"
}

# Display configuration summary
show_summary() {
    print_header "Configuration Summary"
    
    echo -e "Jenkins URL:        ${BLUE}${JENKINS_URL}${NC}"
    echo -e "Job Name:           ${BLUE}${JOB_NAME}${NC}"
    echo -e "Git Repository:     ${BLUE}${GIT_REPO}${NC}"
    echo -e "Email To:           ${BLUE}${MAIL_TO}${NC}"
    echo -e "Email CC:           ${BLUE}${MAIL_CC}${NC}"
    echo -e "SMTP Server:        ${BLUE}${SMTP_SERVER}:${SMTP_PORT}${NC}"
}

# Main menu
main_menu() {
    while true; do
        echo ""
        print_header "Jenkins Setup for Hybrid Selenium Framework"
        echo ""
        echo "1. Test Jenkins Connectivity"
        echo "2. Install Required Plugins"
        echo "3. Create Slave Nodes"
        echo "4. Create Job from Jenkinsfile"
        echo "5. Configure Email Settings"
        echo "6. Show Configuration Summary"
        echo "7. Run Full Setup"
        echo "8. Exit"
        echo ""
        read -p "Select option (1-8): " choice
        
        case $choice in
            1)
                test_connectivity
                ;;
            2)
                install_plugins
                ;;
            3)
                create_slave_nodes
                ;;
            4)
                create_job
                ;;
            5)
                configure_email
                ;;
            6)
                show_summary
                ;;
            7)
                run_full_setup
                ;;
            8)
                print_info "Exiting setup script"
                exit 0
                ;;
            *)
                print_error "Invalid option"
                ;;
        esac
    done
}

# Create all slave nodes
create_slave_nodes() {
    print_header "Creating Slave Nodes"
    
    read -p "Enter API test node IP (default: 192.168.1.10): " api_ip
    api_ip=${api_ip:-192.168.1.10}
    
    read -p "Enter Login test node IP (default: 192.168.1.11): " login_ip
    login_ip=${login_ip:-192.168.1.11}
    
    read -p "Enter Search test node IP (default: 192.168.1.12): " search_ip
    search_ip=${search_ip:-192.168.1.12}
    
    create_slave_node "api-test-node" "$api_ip" "api-test"
    create_slave_node "login-test-node" "$login_ip" "login-test"
    create_slave_node "search-test-node" "$search_ip" "search-test"
}

# Run full setup
run_full_setup() {
    print_header "Running Full Setup"
    
    read -p "Enter Jenkins URL (default: http://localhost:8080): " url
    JENKINS_URL=${url:-http://localhost:8080}
    
    read -p "Enter Jenkins Username (default: admin): " username
    JENKINS_USERNAME=${username:-admin}
    
    read -sp "Enter Jenkins Password: " password
    JENKINS_PASSWORD="$password"
    echo ""
    
    read -p "Enter Git Repository URL: " repo
    GIT_REPO="$repo"
    
    read -p "Enter email recipient (default: ashimnayak2050@gmail.com): " email
    MAIL_TO=${email:-ashimnayak2050@gmail.com}
    
    test_connectivity && \
    install_plugins && \
    create_slave_nodes && \
    create_job && \
    configure_email && \
    show_summary
    
    print_success "Setup completed!"
}

# Interactive setup
interactive_setup() {
    print_header "Interactive Jenkins Setup"
    
    read -p "Enter Jenkins URL [http://localhost:8080]: " jenkins_url
    JENKINS_URL="${jenkins_url:-http://localhost:8080}"
    
    read -p "Enter Jenkins Username [admin]: " jenkins_user
    JENKINS_USERNAME="${jenkins_user:-admin}"
    
    read -sp "Enter Jenkins Password: " jenkins_pass
    JENKINS_PASSWORD="$jenkins_pass"
    echo ""
    
    read -p "Enter Git Repository URL: " git_repo
    GIT_REPO="$git_repo"
    
    read -p "Enter MAIL_TO email [team@example.com]: " mail_to
    MAIL_TO="${mail_to:-team@example.com}"
    
    read -p "Enter MAIL_CC email [manager@example.com]: " mail_cc
    MAIL_CC="${mail_cc:-manager@example.com}"
    
    print_info "Configuration saved. Starting setup..."
    sleep 2
    
    main_menu
}

################################################################################
# Script Entry Point
################################################################################

if [ $# -eq 0 ]; then
    interactive_setup
else
    case "$1" in
        --help|-h)
            echo "Usage: $0 [option]"
            echo ""
            echo "Options:"
            echo "  --test-connection    Test Jenkins connectivity"
            echo "  --install-plugins    Install required plugins"
            echo "  --create-job         Create Jenkins job"
            echo "  --setup-nodes        Create slave nodes"
            echo "  --full-setup         Run complete setup"
            echo "  --help              Show this help"
            echo ""
            ;;
        --test-connection)
            test_connectivity
            ;;
        --install-plugins)
            install_plugins
            ;;
        --create-job)
            create_job
            ;;
        --setup-nodes)
            create_slave_nodes
            ;;
        --full-setup)
            interactive_setup
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            ;;
    esac
fi
