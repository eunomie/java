package io.dagger.modules.java;

import static io.dagger.client.Dagger.dag;

import io.dagger.client.*;
import io.dagger.module.annotation.Function;
import io.dagger.module.annotation.Object;
import java.util.List;
import java.util.Optional;

@Object
public class Jre {
  private static final String jreImage = "eclipse-temurin:23-jre-alpine-3.21";
  private static final String jreDigest =
      "sha256:88593498863c64b43be16e8357a3c70ea475fc20a93bf1e07f4609213a357c87";

  /** Internal container maintained by the Jre module */
  public Container container;

  public Jre() {}

  public Jre(Optional<String> from) {
    this.container = dag().container().from(from.orElse("%s@%s".formatted(jreImage, jreDigest)));
  }

  /** Copy JAR file to the container */
  @Function
  public Jre withJar(File jar) {
    this.container = container.withFile("/opt/app.jar", jar);
    return this;
  }

  /** Copy application directory */
  @Function
  public Jre withAppDirectory(Directory directory) {
    this.container = container.withDirectory("/opt/app", directory);
    return this;
  }

  /** Mount application directory */
  @Function
  public Jre withMountedAppDirectory(Directory directory) {
    this.container = container.withMountedDirectory("/opt/app", directory);
    return this;
  }

  /** Set the working directory to the application one */
  @Function
  public Jre withAppWorkdir() {
    this.container = this.container.withWorkdir("/opt/app");
    return this;
  }

  /** Copy directory */
  @Function
  public Jre withDirectory(String path, Directory directory) {
    this.container = container.withDirectory(path, directory);
    return this;
  }

  /** Mount directory */
  @Function
  public Jre withMountedDirectory(String path, Directory directory) {
    this.container = container.withMountedDirectory(path, directory);
    return this;
  }

  /** Returns directory at a specified path */
  @Function
  public Directory directory(String path) {
    return this.container.directory(path);
  }

  /** Expose a network port. */
  @Function
  public Jre withExposedPort(int port) {
    this.container = this.container.withExposedPort(port);
    return this;
  }

  /** Run a jar file as a Dagger service */
  @Function
  public Service runAsService(Optional<String[]> args) {
    List<String> cmd = new java.util.ArrayList<>(List.of("java", "-jar"));
    args.ifPresent(strings -> cmd.addAll(List.of(strings)));
    cmd.add("/opt/app.jar");
    return this.container.asService(new Container.AsServiceArguments().withArgs(cmd));
  }

  /** Run any custom command as a Dagger service */
  @Function
  public Service customRunAsService(List<String> args) {
    return this.container.asService(new Container.AsServiceArguments().withArgs(args));
  }

  /** Establish a runtime dependency on a service. */
  @Function
  public Jre withServiceBinding(String serviceName, Service service) {
    this.container = this.container.withServiceBinding(serviceName, service);
    return this;
  }
}
