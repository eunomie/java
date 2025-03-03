package io.dagger.modules.java;

import io.dagger.client.Client;
import io.dagger.client.Container;
import io.dagger.client.Directory;
import io.dagger.module.AbstractModule;
import io.dagger.module.annotation.Function;
import io.dagger.module.annotation.Object;
import java.util.List;
import java.util.Optional;

@Object
public class Quarkus extends AbstractModule {
  private static final String graalvmImage = "ghcr.io/graalvm/jdk-community:23";
  private static final String graalvmDigest =
      "sha256:4ae63cc6caa75d0f5d144c20ed4a17760994fd12c8ceb8b7dc6e405f1593ac65";

  public Directory directory;

  public Quarkus() {}

  public Quarkus(Client dag, Directory directory) {
    super(dag);
    this.directory = directory;
  }

  @Function
  public Container JvmImage() {
    return Image(pkg(), "src/main/docker/Dockerfile.jvm");
  }

  @Function
  public Container NativeImage() {
    return Image(nativePkg(), "src/main/docker/Dockerfile.native");
  }

  @Function
  public Container NativeMicroImage() {
    return Image(nativePkg(), "src/main/docker/Dockerfile.native-micro");
  }

  @Function
  public Container Image(Directory directory, String dockerFilePath) {
    return directory.dockerBuild(
        new Directory.DockerBuildArguments().withDockerfile(dockerFilePath));
  }

  public Directory pkg() {
    Maven mvn = new Maven(this.dag, Optional.of(directory), Optional.empty(), Optional.of(false));
    return mvn.pkg().directory(".");
  }

  @Function
  public Directory nativePkg() {
    Maven mvn =
        new Maven(
            this.dag,
            Optional.of(directory),
            Optional.of("%s@%s".formatted(graalvmImage, graalvmDigest)),
            Optional.of(true));
    return mvn.withMvnExec(List.of("package", "-Dnative")).directory(".");
  }
}
