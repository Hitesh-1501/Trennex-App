// Trusted operator command. Never called by Android or a public Cloud Function.
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { parseRoleCommand, applyRoleChange } = require('./admin-roles');

async function main() {
  const command = parseRoleCommand(process.argv.slice(2));
  initializeApp({ projectId: command.project });
  await applyRoleChange({ auth: getAuth(), db: getFirestore(), command,
    serverTimestamp: () => FieldValue.serverTimestamp() });
  console.log(`Admin role ${command.action} completed for ${command.project}.`);
}

main().catch((error) => {
  console.error('Admin role change failed:', error.message);
  process.exitCode = 1;
});
