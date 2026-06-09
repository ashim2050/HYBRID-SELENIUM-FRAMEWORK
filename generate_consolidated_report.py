#!/usr/bin/env python3
"""
Generate consolidated Extent report dashboard
"""
import os
import sys
import json
from datetime import datetime
from pathlib import Path

def generate_consolidated_report(workspace, build_num):
    """Generate the consolidated HTML report for only existing module reports"""

    reports_dir = os.path.join(workspace, 'output', 'reports')
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    gen_date = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    output_file = os.path.join(reports_dir, f'ExtentReport_Consolidated_{timestamp}.html')

    module_reports = []
    if os.path.isdir(reports_dir):
        for entry in sorted(os.listdir(reports_dir)):
            entry_path = os.path.join(reports_dir, entry)
            if os.path.isdir(entry_path):
                html_files = [f for f in sorted(os.listdir(entry_path)) if f.endswith('.html')]
                if html_files:
                    module_reports.append({
                        'module': entry,
                        'report': html_files[-1]
                    })

    if not module_reports:
        html_content = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset=\"UTF-8\"> 
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
    <title>Consolidated Extent Reports Dashboard</title>
    <style>
        body {{ font-family: Arial, sans-serif; background: #f4f6f8; color: #333; }}
        .container {{ max-width: 800px; margin: 100px auto; padding: 30px; background: white; border-radius: 8px; box-shadow: 0 12px 30px rgba(0,0,0,0.08); }}
        h1 {{ margin-bottom: 20px; }}
        p {{ font-size: 16px; line-height: 1.6; }}
    </style>
</head>
<body>
    <div class=\"container\">
        <h1>No Executed Module Reports Found</h1>
        <p>The pipeline did not generate any module reports under <strong>output/reports/</strong>.</p>
        <p>Confirm that <strong>Input/MasterConfig.xlsx</strong> has active modules with <strong>ExecutionFlag = Yes</strong>.</p>
        <p>Generated on <strong>{gen_date}</strong>.</p>
    </div>
</body>
</html>"""
        os.makedirs(os.path.dirname(output_file), exist_ok=True)
        with open(output_file, 'w') as f:
            f.write(html_content)
        print(f"Consolidated report generated successfully: {output_file}")
        return output_file

    cards_html = []
    chart_data = []
    for idx, item in enumerate(module_reports):
        module_label = item['module'].replace('-', ' ').replace('_', ' ').title()
        report_path = f"{item['module']}/{item['report']}"
        cards_html.append(f"""
            <div class=\"report-card\">
                <div class=\"report-header\"><div class=\"icon\">🧪</div><h2>{module_label}</h2></div>
                <div class=\"report-body\">
                    <div class=\"chart-container\"><canvas id=\"chart{idx}\"></canvas></div>
                    <div class=\"stats-row\" id=\"stats{idx}\"><div class=\"loading\">Loading...</div></div>
                    <div class=\"report-file\">📄 {item['report']}</div>
                    <a href=\"{report_path}\" target=\"_blank\" class=\"open-btn\">📊 View Full Report</a>
                </div>
            </div>
        """)
        chart_data.append({
            'id': f'chart{idx}',
            'statsId': f'stats{idx}',
            'file': report_path,
            'label': module_label
        })

    reports_html = '\n'.join(cards_html)
    data_json = json.dumps(chart_data)

    html_content = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset=\"UTF-8\"> 
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
    <title>Consolidated Extent Reports Dashboard</title>
    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>
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
    <div class=\"container\">
        <div class=\"header\">
            <h1>🧪 Test Execution Report Dashboard</h1>
            <p><strong>Build #{build_num}</strong></p>
            <p>Generated on <strong>{gen_date}</strong></p>
        </div>
        <div class=\"reports-grid\">{reports_html}</div>
        <div class=\"footer\">
            <p>Click \"View Full Report\" to see all test details</p>
            <div><span class=\"stat-item\">Build: {build_num}</span><span class=\"stat-item\">Date: {gen_date}</span></div>
        </div>
    </div>
    <script>
        const reports = {data_json};

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
                console.error('Error loading report:', reportInfo.file, e);
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
                '<div class=\"stat-box pass\"><div class=\"stat-label\">Pass</div><div class=\"stat-value\">' + stats.pass + '</div></div>' +
                '<div class=\"stat-box fail\"><div class=\"stat-label\">Fail</div><div class=\"stat-value\">' + stats.fail + '</div></div>' +
                '<div class=\"stat-box skip\"><div class=\"stat-label\">Skip</div><div class=\"stat-value\">' + stats.skip + '</div></div>';
        }}

        reports.forEach(async (r) => {{
            const s = await loadReportStats(r);
            createChart(r.id, s);
            updateStats(r.statsId, s);
        }});
    </script>
</body>
</html>"""

    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, 'w') as f:
        f.write(html_content)

    print(f"Consolidated report generated successfully: {output_file}")
    return output_file

if __name__ == '__main__':
    workspace = os.environ.get('WORKSPACE', '/Users/ashimnayak/.jenkins/workspace/Hybrid-Selenium-Parallel-Execution')
    build_num = os.environ.get('BUILD_NUMBER', '0')
    generate_consolidated_report(workspace, build_num)
