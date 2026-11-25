import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

if (process.env.CI !== 'true') {
  throw new Error('This demo Firebase configuration may only be generated in CI.');
}

const destination = path.resolve('app/src/staging/google-services.json');
const config = {
  project_info: {
    project_number: '123456789012',
    project_id: 'demo-trennex',
    storage_bucket: 'demo-trennex.firebasestorage.app',
  },
  client: [{
    client_info: {
      mobilesdk_app_id: '1:123456789012:android:0000000000000000',
      android_client_info: { package_name: 'com.example.trennex' },
    },
    api_key: [{ current_key: 'ci-demo-key-no-live-firebase-access' }],
  }],
  configuration_version: '1',
};

await mkdir(path.dirname(destination), { recursive: true });
await writeFile(destination, `${JSON.stringify(config, null, 2)}\n`, { flag: 'wx' });
console.log('Created staging-build demo Firebase config for CI compilation.');
