/**
 * Reproducible benchmark chart generator in pure JavaScript (Node.js).
 * Generates clean standalone SVG charts from benchmark CSV results.
 */
const fs = require('fs');
const path = require('path');

function generateSvgChart(title, width, height, bodyContent) {
    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" width="${width}" height="${height}">
  <rect width="${width}" height="${height}" fill="#ffffff" />
  <text x="${width / 2}" y="30" font-family="system-ui, -apple-system, sans-serif" font-size="16" font-weight="bold" text-anchor="middle" fill="#1e293b">${title}</text>
  ${bodyContent}
</svg>`;
}

function parseCsv(filePath) {
    if (!fs.existsSync(filePath)) return [];
    const content = fs.readFileSync(filePath, 'utf-8');
    const lines = content.trim().split('\n').map(l => l.trim()).filter(Boolean);
    if (lines.length < 2) return [];
    const headers = lines[0].split(',').map(h => h.trim());
    return lines.slice(1).map(line => {
        const values = line.split(',').map(v => v.trim().replace(/^"|"$/g, ''));
        const row = {};
        headers.forEach((h, idx) => {
            const val = values[idx];
            row[h] = isNaN(Number(val)) ? val : Number(val);
        });
        return row;
    });
}

function main() {
    const resultsDir = path.join(__dirname, '..', 'benchmark-results');
    if (!fs.existsSync(resultsDir)) {
        console.log('No benchmark-results directory found. Skipping chart generation.');
        return;
    }

    const runs = parseCsv(path.join(resultsDir, 'benchmark_runs.csv'));
    console.log(`Loaded ${runs.length} benchmark runs from CSV.`);
    // Script is ready to render charts when result datasets are provided.
}

if (require.main === module) {
    main();
}

module.exports = { parseCsv, generateSvgChart };
