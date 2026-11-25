# B1 GitHub Actions setup

## Pull request checks

`.github/workflows/pr-validation.yml` runs on pull requests and pushes to `master`. It compiles the **staging** Android build, runs its unit tests, checks Cloud Functions, runs Firestore/Storage rules tests and checkout integration against `demo-trennex` emulators, and validates the seeded catalog. It needs no Firebase credentials. The Android job generates an ignored, synthetic `app/src/staging/google-services.json` solely for compilation; it cannot authenticate to a live Firebase project. Device instrumentation is deliberately outside CI until the combined A+B regression has a safe emulator runner. Never use this generated config for a staging device test.

The staging APK produced by this job is not uploaded or released. The build does not run instrumentation tests on the owner's Motorola.

## Manual staging deployment gate

`.github/workflows/deploy-staging.yml` runs only by **Actions → Deploy Firebase staging → Run workflow**. Select `master` and type the exact staging project ID requested by the workflow. All PR checks run first. The deployment job can run only on `master`, targets `trennex-staging-ebbeb` explicitly, and deploys Cloud Functions, Firestore rules/indexes, and Storage rules. It does not publish Android, migrate catalog records, upload media, or touch production.

Before enabling it:

1. In GitHub repository **Settings → Environments**, create `staging`. Restrict deployment branches to `master`, configure required reviewers, and consider preventing self-review. Check your GitHub plan and repository visibility because environment protection availability varies.
2. In the **staging Google Cloud project only**, create a dedicated deploy service account with the permissions needed for the exact Firebase deploy targets. Do not reuse a production or broadly privileged service account. Grant it no IAM role on `trennex-ebbeb`.
3. Create a Workload Identity Federation provider for GitHub's OIDC issuer. Restrict its attribute condition to this repository's immutable repository ID where available, `refs/heads/master`, the `staging` environment, and the deploy workflow. Grant only that principal permission to impersonate the dedicated staging service account. Confirm the current GitHub OIDC subject format before configuring the condition; it can differ by repository creation date.
4. Add these **GitHub `staging` environment variables**, not credentials: `STAGING_WIF_PROVIDER` (full provider resource name using project number) and `STAGING_DEPLOY_SERVICE_ACCOUNT` (service account email). Do not add a service-account JSON, Firebase CI token, production project ID, or production secret.
5. Review the deploy diff and staging environment approval before the first manual run. A successful local test does not prove the GitHub OIDC and Firebase IAM configuration; verify the first run's logs and staging services after approval.

The workflow uses `google-github-actions/auth` for short-lived credentials, and generated credential files are ignored by Git. The GitHub environment reviewer setting is configured in GitHub, not by the YAML file. Until it exists, do not trigger deployment. If required reviewers are unavailable on the repository plan, leave this deployment workflow unused and use a separately reviewed staging release process.

## Cost and limits

PR jobs consume GitHub Actions minutes and download Android SDK, Gradle, Node, and Firebase emulator dependencies. The demo emulators make no live Firebase writes. A manual staging deployment can incur staging Cloud Build, artifact storage, Functions, Firestore, or Storage costs. No cost amount has been measured. The workflows do not include production deployment or production credentials.

## References

- [GitHub deployment environments](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments)
- [GitHub OIDC claims](https://docs.github.com/en/actions/reference/security/oidc)
- [Google Cloud Workload Identity Federation for deployment pipelines](https://docs.cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines)
- [Google GitHub Actions authentication](https://github.com/google-github-actions/auth)
- [Firebase CLI deployment scopes](https://firebase.google.com/docs/cli/)
