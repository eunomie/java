package io.dagger.modules.java;

import io.dagger.client.Directory;
import io.dagger.module.annotation.Function;
import io.dagger.module.annotation.Object;
import java.util.Optional;

/** Java Module */
@Object
public class Java {
  private static final String graalvmImage = "ghcr.io/graalvm/jdk-community:23";
  private static final String graalvmDigest =
      "sha256:4ae63cc6caa75d0f5d144c20ed4a17760994fd12c8ceb8b7dc6e405f1593ac65";

  @Function
  public Jre jre(Optional<String> from) {
    return new Jre(from);
  }

  @Function
  public Maven maven(
      Optional<Directory> sources, Optional<String> from, Optional<Boolean> useWrapper) {
    return new Maven(sources, from, useWrapper);
  }

  @Function
  public Maven graalvm(Optional<Directory> sources, Optional<String> from) {
    String graalVMImage = from.orElse("%s@%s".formatted(graalvmImage, graalvmDigest));
    return maven(sources, Optional.of(graalVMImage), Optional.of(true));
  }

  @Function
  public Quarkus quarkus(Directory sources) {
    return new Quarkus(sources);
  }
}
