# Dagger modules for Java

[Dagger](https://dagger.io) modules, written in Dang, that build, test, and
publish Java projects. The [`maven`](maven/) module compiles, tests, packages,
installs, and deploys Maven projects.

## Installation

```sh
dagger install github.com/dagger/java/maven
```

## Relationship to the Java SDK

[`dagger/java-sdk`](https://github.com/dagger/java-sdk) is the SDK for writing
Dagger modules and clients in Java. The modules in this repository instead
operate on Java projects, and do not require the Java SDK in your project or
workspace.
