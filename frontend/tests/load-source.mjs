import { readFile } from 'node:fs/promises';
import ts from 'typescript';
const modules = new Map();

// Compile local TS modules with the repository's own TypeScript dependency.
// External packages keep their real Node implementations; tests can replace network adapters.
export async function sourceModuleUrl(relativePath) {
  const file = new URL(`../src/${relativePath}`, import.meta.url);
  if (modules.has(file.href)) return modules.get(file.href);
  const source = (await readFile(file, 'utf8')).replaceAll('import.meta.env.VITE_API_BASE_URL', 'undefined');
  let code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } }).outputText;
  for (const match of [...code.matchAll(/\bfrom\s+["']([^"']+)["']/g)]) {
    const specifier = match[1];
    const target = specifier.startsWith('@/') ? await sourceModuleUrl(`${specifier.slice(2)}.ts`) : import.meta.resolve(specifier);
    code = code.replace(match[0], `from ${JSON.stringify(target)}`);
  }
  const url = `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`;
  modules.set(file.href, url);
  return url;
}
export const loadSource = async path => import(await sourceModuleUrl(path));
