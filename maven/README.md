# Maven

A [Dagger](https://dagger.io) module written in Dang for compiling, testing,
packaging, and publishing Maven projects. Requires and is tested with Dagger
v1.0.0-beta.25.

## Install and verify

```sh
dagger install github.com/dagger/java/maven
dagger check --module maven
```

For local development, install this directory's path instead of the GitHub URL.

The default check runs `mvn verify` on every selected reactor. This includes unit
tests, packaging, and any integration tests and quality gates configured in the
POM. It runs the complete [Maven lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
through verification, including `post-integration-test` cleanup. The module
does not add test or lint plugins to your POM.

Use `dagger check` for CI verdicts. Calling a check through the API returns a
`Check`; inspect its `pass` field to run it and obtain the verdict.

## Projects and reactors

`projects` is a collection keyed by workspace-relative root directory, with `.`
representing the workspace root. Automatic discovery selects outermost
directories containing `pom.xml`, excluding `target`, `.m2`, `.git`,
`node_modules`, and gitignored files. Maven itself selects and orders reactor
members, including profile-dependent modules; the module does not attempt to
interpret POM XML.

From a reactor's subdirectory, discovery still selects its root. From elsewhere,
it selects reactors below the working directory. Members are not checked twice.
For a nested **independent** project, or aggregators referencing sibling projects,
set `roots` explicitly to the reactor roots you want built:

```toml
[modules.maven.settings]
roots = ["services/api", "tools/migration"]
```

`project(path)` also addresses any specific POM directory directly. All project
paths are relative to the workspace root, regardless of the current directory.
Paths must not contain `..`.

For a project at the workspace root:

```sh
dagger -c 'maven | project --path=. | compile | sync'
dagger -c 'maven | project --path=. | test | stdout'
dagger -c 'maven | project --path=. | build | export ./dist'
dagger -c 'maven | project --path=. | reports | export ./reports'
```

For a nested reactor:

```sh
dagger -c 'maven | projects | get services/api | build | export ./dist'
dagger -c 'maven | project --path=services/api | run --goals=dependency:tree | stdout'
```

## Selecting test classes

Each project exposes a `tests` collection of Java unit-test class candidates.
Keys combine the **reactor-relative module directory** and fully qualified
class name: `app/example.AppTest`, or `./example.RootTest` for a class in the
reactor root itself.

```sh
# List candidates without compiling or running Maven.
dagger -c 'maven | project --path=. | tests | keys'

# Run one class, including its parameterized and nested test cases.
dagger -c 'maven | project --path=. | tests | get app/example.AppTest | test | stdout'
```

From another Dang module, select several classes using `subset` and run them
together:

```dang
let classes = maven.project(".").tests(ws)
classes.subset(keys: [
  "app/example.AppTest",
  "library/example.LibraryTest",
]).test(ws).sync
```

A selection runs **one `mvn test` invocation for the reactor**, with a separate
Surefire filter for each module. Classes with identical names in different
modules remain independently selectable. Dependencies build in reactor order;
unselected tests still compile, but do not run. An empty subset does not invoke
Maven. Unknown classes fail, and a selected class producing no test cases in
Surefire's XML reports fails with a diagnostic rather than silently succeeding.
The returned container contains the selected classes' reports and build outputs.
Collection selections override `-Dtest` and `surefire.includesFile`, and force
test failures to propagate even if module arguments request ignoring them.

These collection operations are explicit calls, not additional `@check`
functions. `dagger check` still performs full `verify`, including tests that
class discovery does not list. Discovery filters never weaken that check.

Discovery defaults to `src/test/java` under each POM and Surefire's conventional
`Test*.java`, `*Test.java`, `*Tests.java`, and `*TestCase.java` names. Class names
come from source paths, so package declarations must match directory layout.
Configure custom source directories and filename patterns explicitly:

```toml
[modules.maven.settings]
testSourceDirectory = "src/spec/java"
testIncludes = ["**/*Spec.java"]
testExcludes = ["**/AbstractSpec.java"]
```

These settings only control discovery; configure the corresponding source
directory in your POM too. This first version does not interpret effective POMs,
active profiles, annotations, generated tests, or other JVM source languages.
A candidate can therefore be a helper or belong to an inactive module; its
explicit run fails if it produces no test cases. Individual methods and runtime
parameter values are not separate collection items.

Class selection requires modern Surefire (tested with 3.5.4), standard XML
reports under `target/surefire-reports`, and class-based test execution. JUnit 4
and JUnit 5 are covered by the integration suite. Custom TestNG suite files,
hard-coded plugin `<test>` selectors, and custom report layouts are outside
this initial support. Use `project.test(..., args: [...])` or `run` for those
workflows, method selectors, and Failsafe integration-test selection.

## Operations

| Function | Maven phase or goal | Result |
| --- | --- | --- |
| `compile` | `compile` | Container with compiled production code |
| `test` | `test` | Container with unit test results |
| `build` | `package` | All `target` trees, with module-relative paths |
| `verify` | `verify` | Dagger check; the only automatic check |
| `reports` | `verify`, ignoring test failures | Surefire and Failsafe report trees |
| `install` | `install` | Container with artifacts installed in its local repository |
| `deploy` | `deploy` | Output from publishing to the configured repository |
| `run` | Caller-supplied phases/goals | Container for custom workflows |
| `container` | None | Prepared container for customization or debugging |
| `tests` | None | Discover a selectable collection of Java unit-test classes |

`build` runs unit tests by default. For a reactor with `library` and `app`, its
output includes `library/target/…` and `app/target/…`. It preserves JARs, WARs,
EARs, attached artifacts, classes, and other target contents. Custom output
directories are available through `run(...).directory(...)`.

`reports` returns XML and text reports even when tests fail, using
`-Dmaven.test.failure.ignore=true`. Compilation and other build errors still
fail. Report export is separate from the CI verdict: always run `verify` too.
Custom report directories are available through `run`.

`run` supports `clean`, `dependency:tree`, `dependency:go-offline`, `site`,
plugin goals, and other Maven commands without adding a helper for each one.
Project-level execution methods other than `verify` accept additional `args`.
Configure module-level `args` to affect checks and class-collection runs too.

## Toolchain and settings

The default image is `maven:3.9.16-eclipse-temurin-21`. If the project root
contains `mvnw`, commands use `sh ./mvnw`, preserving the version and checksum
policy in `.mvn/wrapper/maven-wrapper.properties`. A wrapper failure is reported;
it does not silently fall back to another Maven version. Set `useWrapper=false`
to force the image's Maven.

Commit your wrapper and its distribution checksum for reproducible tool
selection. See the [Apache Maven Wrapper documentation](https://maven.apache.org/tools/wrapper/).
The wrapper selects Maven; `javaVersion` selects the JDK. Set the Java language
target in your POM, normally with `maven.compiler.release`.

```toml
[modules.maven.settings]
javaVersion = "21"
mavenVersion = "3.9.16"
useWrapper = true
args = ["-Pci", "-Dstyle.color=never"]
```

| Setting | Default | Purpose |
| --- | --- | --- |
| `javaVersion` | `"21"` | Temurin JDK tag suffix |
| `mavenVersion` | `"3.9.16"` | Image Maven version; wrapper takes precedence |
| `base` | None | Custom container with toolchain, environment, secrets, services, etc. |
| `useWrapper` | `true` | Prefer the project's wrapper |
| `args` | `[]` | Extra arguments for every Maven invocation |
| `roots` | `[]` | Explicit reactor roots; empty means automatic discovery |
| `settings` | None | Dagger `Secret` containing settings.xml |
| `toolchains` | None | Dagger `File` containing toolchains.xml |
| `cacheKey` | Workspace/project specific | Override the dependency-cache namespace |
| `testSourceDirectory` | `"src/test/java"` | Source directory per POM for class discovery |
| `testIncludes` | Surefire's conventional Java filenames | Discovery globs relative to the test source directory |
| `testExcludes` | `[]` | Exclusion globs for class discovery |

A custom `base` supplies its own JDK and Maven, so version settings are ignored.
It must provide `sh`, and either `mvn` or the prerequisites for your wrapper
(typically a JDK, curl/wget, and unzip). Multi-JDK builds can supply a custom
base and `toolchains`; paths in toolchains.xml refer to JDKs inside that image.

For reactor selection, pass Maven's own flags, such as `args = ["-pl", "app",
"-am"]`. The whole workspace stays available, preserving sibling modules,
relative parent POMs, resources, `.mvn/maven.config`, and `.mvn/jvm.config`.
Host `target` trees, local dependency caches, and files matched by `.gitignore`
(including nested `.gitignore` files) are excluded. Discovery applies the same
rules, so it never selects a POM that the build cannot see; an explicit root
whose `pom.xml` is ignored fails with `no pom.xml`. Builds requiring Git
metadata can supply it in a custom base.

## Caching and publishing

The local repository is `/maven/repository`; wrapper distributions also live
under `/maven`. A locked cache volume prevents concurrent Maven processes from
writing the same repository. By default each workspace and reactor has its own
cache; use `cacheKey` to choose a stable namespace for synthetic Directory
workspaces or intentionally share dependencies. A shared key serializes the
Maven processes using it and shares everything in the local repository,
including artifacts placed there by `install`, not only downloads. Build outputs
remain in the container snapshot, outside the dependency cache. Each operation
is its own Maven invocation, so `compile`, `test`, `build`, `verify`, and
`reports` may each execute separately.

Dependencies resolve during normal Maven execution. There is no mandatory
`dependency:go-offline` stage: reactor artifacts and plugins that need source
can make a POM-only prefetch incorrect. Maven commands use batch mode and suppress
transfer progress. Unchanged builds reuse Dagger's execution cache. To refresh
snapshots, use `run` with `fresh: true` and `args: ["-U"]`.

`install` and `deploy` request fresh execution. Deployment is explicit and never
runs as a check. Configure destinations in the POM's `distributionManagement`,
or pass `-DaltDeploymentRepository=id::url` with a compatible deploy plugin.
Credentials belong in `settings`, mounted as a secret outside the cache and
source tree. Server IDs must match the repository IDs in the POM. Signing and
staging follow your project's Maven plugins; supply additional secrets or tools
through `base` when needed.

From another Dang module with this module installed as a dependency:

```dang
publish(ws: Workspace!, settings: Secret!): String! {
  maven(settings: settings).project("services/api").deploy(ws)
}

check(ws: Workspace!): Void {
  let result = maven.projects(ws).verify(ws)
  if (result.pass == false) {
    raise result.error.message ?? "Maven verification failed"
  }
  null
}
```

Arguments are passed as an argument vector, without shell interpolation. Keep
credentials out of `args` and committed settings files.

## Development

From the repository root (the parent of this directory):

```sh
dang fmt -w maven/main.dang .dagger/modules/e2e/main.dang
```

Run the integration checks one at a time; running them all at once starts
many Maven builds concurrently:

```sh
for check in $(dagger check -l | awk '$1 == "maven-checks" { print $2 }'); do
  dagger check "maven-checks:$check" || exit 1
done
```

The suite covers discovery and cwd selection, source filtering, configuration,
wrapper selection and actual wrapper execution, single-module and reactor
builds, failed tests and report export, artifact installation, and deployment to
a disposable `file://` repository. It never publishes to a remote repository.
Class-collection checks cover discovery, custom source paths, selections across
modules, duplicate class names, empty subsets, failure propagation, and JUnit 5
parameterized and nested tests.
The shared workspace configuration, test module, and `dagger.lock` live at the
repository root. Commit that lockfile to retain the SDK and image resolutions
used by the suite.
