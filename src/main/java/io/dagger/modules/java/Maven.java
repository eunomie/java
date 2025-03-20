package io.dagger.modules.java;

import static io.dagger.client.Dagger.dag;

import io.dagger.client.*;
import io.dagger.module.annotation.Function;
import io.dagger.module.annotation.Object;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Object
public class Maven {
  private static final String mavenImage = "maven:3.9.9-eclipse-temurin-23-alpine";
  private static final String mavenDigest =
      "sha256:0e5e89100c3c1a0841ff67e0c1632b9b983e94ee5a9b1f758125d9e43c66856f";

  public Container container;
  public boolean useWrapper;

  public Maven() {}

  public Maven(Optional<Directory> sources, Optional<String> from, Optional<Boolean> useWrapper) {
    this.container =
        dag()
            .container()
            .from(from.orElse("%s@%s".formatted(mavenImage, mavenDigest)))
            .withMountedCache("/root/.m2", dag().cacheVolume("maven-m2"))
            .withWorkdir("/src");
    sources.ifPresent(this::withSources);
    useWrapper.ifPresent(b -> this.useWrapper = b);
  }

  /** Run maven commands */
  @Function
  public Maven withMvnExec(List<String> commands) {
    this.container = this.container.withExec(mvnCommand(commands));
    return this;
  }

  /** Retrieve the jar file */
  @Function
  public File jar() throws ExecutionException, DaggerQueryException, InterruptedException {
    pkg();
    return container.file(jarFileName());
  }

  public String jarFileName()
      throws ExecutionException, DaggerQueryException, InterruptedException {
    String artifactID =
        container
            .withExec(
                mvnCommand(
                    List.of(
                        "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate",
                        "-Dexpression=project.artifactId",
                        "-q",
                        "-DforceStdout")))
            .stdout();
    String version =
        container
            .withExec(
                mvnCommand(
                    List.of(
                        "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate",
                        "-Dexpression=project.version",
                        "-q",
                        "-DforceStdout")))
            .stdout();
    return String.format("target/%s-%s.jar", artifactID, version);
  }

  /**
   * Mount source directory
   *
   * @param source Source directory
   */
  @Function
  public Maven withSources(Directory source) {
    this.container = this.container.withMountedDirectory("/src", source);
    return this;
  }

  /** Returns directory at a specified path */
  @Function
  public Directory directory(String path) {
    return this.container.directory(path);
  }

  /** Run maven package */
  @Function
  public Maven pkg() {
    return this.withMvnExec(List.of("package"));
  }

  /** Run maven clean */
  @Function
  public Maven clean() {
    return this.withMvnExec(List.of("clean"));
  }

  /** Run maven install */
  @Function
  public Maven install() {
    return this.withMvnExec(List.of("install"));
  }

  /** Run maven test */
  @Function
  public Maven test() {
    return this.withMvnExec(List.of("test"));
  }

  /** Execute a command */
  @Function
  public Maven withExec(List<String> commands) {
    this.container = this.container.withExec(commands);
    return this;
  }

  /** Retrieves this maven module with a different working directory. */
  @Function
  public Maven withWorkdir(String path) {
    this.container = this.container.withWorkdir(path);
    return this;
  }

  /** Expose a network port. */
  @Function
  public Maven withExposedPort(int port) {
    this.container = this.container.withExposedPort(port);
    return this;
  }

  /** Establish a runtime dependency on a service. */
  @Function
  public Maven withServiceBinding(String serviceName, Service service) {
    this.container = this.container.withServiceBinding(serviceName, service);
    return this;
  }

  @Function
  public File file(String path) {
    return this.container.file(path);
  }

  @Function
  public Maven withNewFile(String path, String content) {
    this.container = this.container.withNewFile(path, content);
    return this;
  }

  private List<String> mvnCommand(List<String> commands) {
    List<String> result = new java.util.ArrayList<>();
    result.add(useWrapper ? "./mvnw" : "mvn");
    result.addAll(commands);
    return result;
  }

  private List<String> mvnCommand(String[] commands) {
    return mvnCommand(List.of(commands));
  }
}
