# Android signing without secrets in Git

## Rule

Do not commit keystores, passwords, signing property files, generated APKs, or generated AABs.

The repo supports release signing through either:

1. ignored local file: `apps/android/signing.properties`, or
2. local environment variables on the build machine.

This does not add any new Learn runtime environment variables. It only controls local Android release signing.

## Local signing file

Create this file locally only:

```properties
FLOENTLY_RELEASE_STORE_FILE=/absolute/path/to/floently-release.jks
FLOENTLY_RELEASE_STORE_PASSWORD=replace-locally
FLOENTLY_RELEASE_KEY_ALIAS=replace-locally
FLOENTLY_RELEASE_KEY_PASSWORD=replace-locally
```

`apps/android/signing.properties` is ignored by Git.

## Environment variable alternative

```bash
export FLOENTLY_RELEASE_STORE_FILE=/absolute/path/to/floently-release.jks
export FLOENTLY_RELEASE_STORE_PASSWORD=replace-locally
export FLOENTLY_RELEASE_KEY_ALIAS=replace-locally
export FLOENTLY_RELEASE_KEY_PASSWORD=replace-locally
```

## Check status

```bash
./scripts/nativectl signing-status
```

The command prints only present/missing, not secret values.

## Build commands

```bash
./scripts/nativectl build android-learn
./scripts/nativectl build android-learn-release
./scripts/nativectl build android-learn-bundle
```

If signing values are present, the release build uses them. If they are missing, the app can still build unsigned release artifacts for local verification.
