import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(scriptDir, '..');
const outputPath = resolve(projectRoot, 'public', 'app-config.js');

const apiBaseUrl = process.env.TAARA_API_BASE_URL || 'http://localhost:8080/api';
const supabaseUrl = process.env.TAARA_SUPABASE_URL || '';
const supabaseAnonKey = process.env.TAARA_SUPABASE_ANON_KEY || '';

const content = `window.__TAARA_CONFIG__ = {
  apiBaseUrl: ${JSON.stringify(apiBaseUrl)},
  supabaseUrl: ${JSON.stringify(supabaseUrl)},
  supabaseAnonKey: ${JSON.stringify(supabaseAnonKey)}
};
`;

await mkdir(dirname(outputPath), { recursive: true });
await writeFile(outputPath, content, 'utf8');

console.log(`Generated ${outputPath}`);
