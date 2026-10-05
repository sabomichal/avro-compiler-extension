# Deployment Instructions

Artifacts are published to Maven Central through the [Central Portal](https://central.sonatype.org/publish/publish-portal-maven/)
using `central-publishing-maven-plugin`. The `central` server credentials (Portal user token) and `gpg.passphrase`
must be present in `~/.m2/settings.xml`.

The version in `pom.xml` is `0-SNAPSHOT` between releases, so the release version has to be bumped by hand.
The extension version follows the Avro version it targets, check the latest
[release](https://github.com/sabomichal/avro-compiler-extension/releases) to pick the next one:

* new Avro version (`1.12.2-2` → `1.12.3`): the version matches the Avro version
* fix release for the same Avro version (`1.12.2` → `1.12.2-1` → `1.12.2-2`): bump the `-N` suffix

## Release steps

1. Point the README at the new version and commit:
   ```shell
   git commit -am "docs: point README at X.Y.Z"
   ```
2. Set `<version>` in `pom.xml` to the release version, commit and create an annotated tag
   (a lightweight tag is not pushed by `--follow-tags`):
   ```shell
   git commit -am "release version X.Y.Z"
   git tag -a vX.Y.Z -m "vX.Y.Z"
   ```
3. Deploy. The `sign` profile signs the artifacts, and the deployment is published automatically after validation.
   The profile has to be enabled explicitly, `gpg.passphrase` from `settings.xml` does not activate it.
   On Windows point the plugin to the GnuPG installation holding the key, the `gpg` bundled with Git Bash has an empty keyring:
   ```shell
   mvn -B clean deploy -Psign -Dgpg.executable=C:/bin/GnuPG/bin/gpg.exe
   ```
4. Set `<version>` back to `0-SNAPSHOT`, commit and push the commits together with the tag:
   ```shell
   git commit -am "next development version"
   git push --follow-tags origin master
   ```
5. Create a [GitHub release](https://github.com/sabomichal/avro-compiler-extension/releases/new) for the tag `vX.Y.Z` with release notes.
