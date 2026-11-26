#!/usr/bin/env node
// Single entry point for all automated tests (docs/testing-strategy.md, "Quality gate").
//
//   npm test                    -> core (JUnit + API integration on PostgreSQL/Testcontainers), frontend (Vitest), ai-service (pytest)
//   npm test -- core frontend   -> only the listed suites
//
// Requirements: JDK 21+, Node 20+, Python 3.12+, and a running Docker engine for the core integration tests.
import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const isWindows = process.platform === 'win32';

function javaEnv() {
  const env = { ...process.env };
  const javaHome = env.JAVA_HOME;
  if (javaHome && !existsSync(join(javaHome, 'bin', isWindows ? 'java.exe' : 'java'))) {
    console.warn(`! JAVA_HOME (${javaHome}) has no bin/java - ignoring it and using "java" from PATH.`);
    delete env.JAVA_HOME;
  }
  return env;
}

function pythonCommand() {
  const venvPython = isWindows
    ? join(root, 'ai-service', '.venv', 'Scripts', 'python.exe')
    : join(root, 'ai-service', '.venv', 'bin', 'python');
  if (existsSync(venvPython)) {
    return `"${venvPython}"`;
  }
  return isWindows ? 'python' : 'python3';
}

const suites = {
  core: {
    title: 'core      (Spring Boot: unit + MockMvc + API integration on PostgreSQL 16)',
    cwd: join(root, 'core'),
    // Explicit path: cmd.exe may be configured not to search the current directory (NoDefaultCurrentDirectoryInExePath).
    command: isWindows ? `"${join(root, 'core', 'mvnw.cmd')}" -B test` : './mvnw -B test',
    env: javaEnv(),
  },
  frontend: {
    title: 'frontend  (React: Vitest + Testing Library)',
    cwd: join(root, 'frontend'),
    command: 'npm test',
    precheck: () => existsSync(join(root, 'frontend', 'node_modules')) || 'run "npm ci" in frontend/ first',
  },
  ai: {
    title: 'ai-service (FastAPI: pytest, mock AI provider)',
    cwd: join(root, 'ai-service'),
    command: `${pythonCommand()} -m pytest`,
  },
};

const requested = process.argv.slice(2);
const selected = requested.length > 0 ? requested : Object.keys(suites);
const unknown = selected.filter((name) => !suites[name]);
if (unknown.length > 0) {
  console.error(`Unknown suite(s): ${unknown.join(', ')}. Available: ${Object.keys(suites).join(', ')}`);
  process.exit(2);
}

const results = [];
for (const name of selected) {
  const suite = suites[name];
  console.log(`\n=== ${suite.title}\n    $ ${suite.command}\n`);
  const precheck = suite.precheck ? suite.precheck() : true;
  if (precheck !== true) {
    console.error(`!!! ${name}: ${precheck}`);
    results.push({ name, ok: false, seconds: 0 });
    continue;
  }
  const started = Date.now();
  const run = spawnSync(suite.command, {
    cwd: suite.cwd,
    env: suite.env ?? process.env,
    stdio: 'inherit',
    shell: true,
  });
  results.push({ name, ok: run.status === 0, seconds: Math.round((Date.now() - started) / 1000) });
}

console.log('\n=== Test summary');
for (const { name, ok, seconds } of results) {
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name.padEnd(9)} ${seconds}s`);
}
const failed = results.some((result) => !result.ok);
console.log(failed ? '\nSome suites FAILED.' : '\nAll suites passed.');
process.exit(failed ? 1 : 0);
