## Publishing

Packages are automatically published from the `main` branch to the GitHub Package Registry.
Manually cut releases will be published to both GitHub Package Registry and Maven Central.

## Release Workflow

There are multiple GitHub Action Workflows for the different steps in the package's lifecycle:

- CI: Builds and checks incoming changes on a pull request
  - triggered on every push to a non-default branch
- CD: Publishes the Gradle artifacts to GitHub Package Registry
  - triggered only on pushes to the default branch
- Release: Publishes Gradle artifacts to Sonatype and releases them to Maven Central
  - triggered on a published GitHub release using the underlying tag as artifact version, e.g. via `git tag -m "$MESSAGE" v$(date +"%Y-%m-%dT%H-%M-%S")`
