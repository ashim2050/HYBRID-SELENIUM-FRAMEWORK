#!/usr/bin/env python3
"""
Generate consolidated Extent report dashboard
"""
import os
import sys
import json
from datetime import datetime
from pathlib import Path

def generate_consolidated_report(workspace, build_num, api_report, login_report, search_report):
    """Generate the consolidated HTML report"""
    
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    gen_date = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    output_file = os.path.join(workspace, 'output', 'reports', f'ExtentReport_Consolidated_{timestamp}.html')
    
    html_content = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Consolidated Extent Reports Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: 'Segoe UI', sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 40px 20px; }}
        .container {{ max-width: 1200px; margin: 0 auto; }}
        .header {{ background-color: rgba(255,255,255,0.95); padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center; margin-bottom: 40px; }}
        .header h1 {{ color: #333; margin-bottom: 10px; font-size: 32px; }}
        .header p {{ color: #666; font-size: 14px; margin: 5px 0; }}
        .reports-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 25px; margin-bottom: 40px; }}
        .report-card {{ background: white; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); overflow: hidden; transition: all 0.3s; }}
        .report-card:hover {{ transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.2); }}
        .report-header {{ background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; text-align: center; }}
        .report-header h2 {{ font-size: 22px; margin-bottom: 5px; }}
        .icon {{ font-size: 40px; margin-bottom: 10px; }}
        .report-body {{ padding: 25px; }}
        .chart-container {{ position: relative; height: 200px; margin-bottom: 15px; }}
        .stats-row {{ display: flex; gap: 8px; margin-bottom: 15px; justify-content: center; }}
        .stat-box {{ padding: 10px 12px; background: #f8f9fa; border-radius: 5px; flex: 1; text-align: center; border-left: 3px solid #667eea; }}
        .stat-label {{ font-size: 10px; color: #999; text-transform: uppercase; }}
        .stat-value {{ font-size: 18px; font-weight: bold; color: #333; }}
        .stat-box.pass {{ border-left-color: #28a745; }}
        .stat-box.pass .stat-value {{ color: #28a745; }}
        .stat-box.fail {{ border-left-color: #dc3545; }}
        .stat-box.fail .stat-value {{ color: #dc3545; }}
        .stat-box.skip {{ border-left-color: #ffc107; }}
        .stat-box.skip .stat-value {{ color: #ffc107; }}
        .report-file {{ color: #999; font-size: 11px; margin-bottom: 12px; word-break: break-all; }}
        .open-btn {{ display: block; background: #667eea; color: white; padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; font-weight: bold; text-decoration: none; width: 100%; transition: all 0.3s; }}
        .open-btn:hover {{ background: #764ba2; }}
        .footer {{ background: rgba(255,255,255,0.95); padding: 20px; border-radius: 8px; text-align: center; color: #666; font-size: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }}
        .stat-item {{ display: inline-block; background: #f0f0f0; padding: 8px 12px; border-radius: 5px; margin: 0 5px; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🧪 Test Execution Report Dashboard</h1>
            <p><strong>Build #{build_num}</strong></p>
            <p>Generated on <strong>{gen_date}</strong></p>
        </div>
        <div class="reports-grid">
            <div class="report-card">
                <div class="report-header"><div class="icon">🔌</div><h2>API Tests</h2></div>
                <div class="report-body">
                    <div class="chart-container"><canvas id="apiChart"></canvas></div>
                    <div class="stats-row" id="apiStats"><div class="loading">Loading...</div></div>
                    <div class="report-file">📄 {api_report}</div>
                    <a href="api/{api_report}" target="_blank" class="open-btn">📊 View Full Report</a>
                </div>
            </div>
            <div class="report-card">
                <div class="report-header"><div class="icon">🔐</div><h2>Login Tests</h2></div>
                <div class="report-body">
                    <div class="chart-container"><canvas id="loginChart"></canvas></div>
                    <div class="stats-row" id="loginStats"><div class="loading">Loading...</div></div>
                    <div class="report-file">📄 {login_report}</div>
                    <a href="login/{login_report}" target="_blank" class="open-btn">📊 View Full Report</a>
                </div>
            </div>
            <div class="report-card">
                <div class="report-header"><div class="icon">🔍</div><h2>Search Tests</h2></div>
                <div class="report-body">
                    <div class="chart-container"><canvas id="searchChart"></canvas></div>
                    <div class="stats-row" id="searchStats"><div class="loading">Loading...</div></div>
                    <div class="report-file">📄 {search_report}</div>
                    <a href="search/{search_report}" target="_blank" class="open-btn">📊 View Full Report</a>
                </div>
            </div>
        </div>
        <div class="footer">
            <p>Click "View Full Report" to see all test details</p>
            <div><span class="stat-item">Build: {build_num}</span><span class="stat-item">Date: {gen_date}</span></div>
        </div>
    </div>
    <script>
        const reports = [
            {{ id: 'api', name: 'API Tests', file: 'api/{api_report}' }},
            {{ id: 'login', name: 'Login Tests', file: 'login/{login_report}' }},
            {{ id: 'search', name: 'Search Tests', file: 'search/{search_report}' }}
        ];
        
        async function loadReportStats(reportInfo) {{
            try {{
                const response = await fetch(reportInfo.file);
                const html = await response.text();
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                const passCount = parseInt(doc.querySelector('[data-testcount-pass]')?.getAttribute('data-testcount-pass') || 0) || 0;
                const failCount = parseInt(doc.querySelector('[data-testcount-fail]')?.getAttribute('data-testcount-fail') || 0) || 0;
                const skipCount = parseInt(doc.querySelector('[data-testcount-skip]')?.getAttribute('data-testcount-skip') || 0) || 0;
                return {{ pass: passCount, fail: failCount, skip: skipCount }};
            }} catch (e) {{
                console.error('Error loading report:', reportInfo.id, e);
                return {{ pass: 0, fail: 0, skip: 0 }};
            }}
        }}
        
        function createChart(canvasId, stats) {{
            const ctx = document.getElementById(canvasId).getContext('2d');
            new Chart(ctx, {{
                type: 'doughnut',
                data: {{
                    labels: ['Passed', 'Failed', 'Skipped'],
                    datasets: [{{
                        data: [stats.pass, stats.fail, stats.skip],
                        backgroundColor: ['#28a745', '#dc3545', '#ffc107'],
                        borderColor: ['#20c997', '#c82333', '#e0a800'],
                        borderWidth: 2
                    }}]
                }},
                options: {{ responsive: true, maintainAspectRatio: false, plugins: {{ legend: {{ position: 'bottom', labels: {{ font: {{ size: 11 }} }} }} }} }}
            }});
        }}
        
        function updateStats(elementId, stats) {{
            document.getElementById(elementId).innerHTML = 
                '<div class="stat-box pass"><div class="stat-label">Pass</div><div class="stat-value">' + stats.pass + '</div></div>' +
                '<div class="stat-box fail"><div class="stat-label">Fail</div><div class="stat-value">' + stats.fail + '</div></div>' +
                '<div class="stat-box skip"><div class="stat-label">Skip</div><div class="stat-value">' + stats.skip + '</div></div>';
        }}
        
        reports.forEach(async (r) => {{
            const s = await loadReportStats(r);
            createChart(r.id + 'Chart', s);
            updateStats(r.id + 'Stats', s);
        }});
    </script>
</body>
</html>"""

    # Create output directory if needed
    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    
    # Write the HTML file
    with open(output_file, 'w') as f:
        f.write(html_content)
    
    print(f"Consolidated report generated successfully: {output_file}")
    return output_file

if __name__ == '__main__':
    workspace = os.environ.get('WORKSPACE', '/Users/ashimnayak/.jenkins/workspace/Hybrid-Selenium-Parallel-Execution')
    build_num = os.environ.get('BUILD_NUMBER', '0')
    
    # Find latest timestamped reports
    reports_dir = os.path.join(workspace, 'output', 'reports')
    
    def find_latest_report(module_dir):
        """Find latest timestamped report in a directory"""
        path = Path(os.path.join(reports_dir, module_dir))
        if not path.exists():
            return f"ExtentReport_{module_dir}.html"
        files = sorted(path.glob(f"ExtentReport_{module_dir}_*.html"))
        return files[-1].name if files else f"ExtentReport_{module_dir}.html"
    
    api_report = find_latest_report('api')
    login_report = find_latest_report('login')
    search_report = find_latest_report('search')
    
    print(f"API Report: {api_report}")
    print(f"Login Report: {login_report}")
    print(f"Search Report: {search_report}")
    
    generate_consolidated_report(workspace, build_num, api_report, login_report, search_report)
