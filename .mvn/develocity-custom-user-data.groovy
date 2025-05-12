import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.stream.Collectors

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession
import org.apache.maven.project.MavenProject;

import static com.gradle.CiUtils.isGitHubActions
import static com.gradle.Utils.envVariable

// Add JVM Major version as tag
Runtime.Version version = Runtime.version()
buildScan.tag("Java ${version.feature()}")

// Add more details for CI builds
if (isGitHubActions()) {

  // Add Pull Request info to custom data
  envVariable("GITHUB_BASE_REF").ifPresent(value -> {
    buildScan.value("PR", "true")
    buildScan.value("PR Target Branch", value)
  })

  // 'Git branch' shows up as 'HEAD' sometimes on GH Actions
  envVariable("GITHUB_HEAD_REF").ifPresent(value ->
    buildScan.value("Git branch", value))

  // Add workflow name as tag
  envVariable("GITHUB_WORKFLOW").ifPresent(value ->
    buildScan.tag(value))

  // Add Build Scan info to step outputs
  envVariable("GITHUB_OUTPUT").ifPresent(value -> {
    buildScan.buildScanPublished({
      var id = it.buildScanId
      var uri = it.buildScanUri

      new File(value)
        << '\n'
        << "buildscan_id=${id}"
        << '\n'
        << "buildscan_uri=${uri}"
        << '\n'
    })
  })

  // Add Build Scan link to CI result
  envVariable("GITHUB_STEP_SUMMARY").ifPresent(value ->
    buildScan.buildScanPublished({
      new File(value)
        << '\n'
        << "View the [Build Scan](${it.buildScanUri}) to see more information about this build."
    })
  )
}

buildScan.buildFinished(result -> {
  if (result.getFailures().empty) {
    // get the output file
    File outputFile = checksumsFile(session);

    // for each artifact in the build calculate the checksum

    outputFile.parentFile.mkdirs()
    try (PrintStream out = new PrintStream(new FileOutputStream(outputFile))) {
      Map<String, String> checksums = checksumsProducedByBuild("SHA-256", session);
      checksums.forEach((name, checksum) -> {
        out.print(checksum);
        out.print("  ");
        out.println(name);
      });
    } catch (IOException e) {
      throw new RuntimeException("Failed to create checksum file: " + outputFile.getAbsolutePath(), e);
    }
    log.info("Checksums file created: {}", outputFile);

    envVariable("GITHUB_OUTPUT").ifPresent(value -> {
      new File(value)
          << '\n'
          << "artifact_sha256=${outputFile}"
          << '\n'
    })

  }
})

private Map<String, String> checksumsProducedByBuild(String algorithm, MavenSession session) {
  return session.getAllProjects().stream()
          .map(project -> checksumArtifacts(algorithm, project))
          .flatMap(map -> map.entrySet().stream())
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, TreeMap::new));
}

private Map<String, String> checksumArtifacts(String algorithm, MavenProject project) {

  Map<String, File> artifacts = new HashMap<>();

  Map<String, String> checksums = new HashMap<>();
  // POM
  artifacts.put(project.getArtifactId() + ".pom", project.getFile());

  // Main artifact
  Artifact artifact = project.getArtifact();
  if (artifact != null && artifact.getFile() != null) {
    artifacts.put(artifact.getFile().getName(), artifact.getFile());
  }

  // attached artifacts
  project.getAttachedArtifacts()
          .forEach(attachedArtifact ->
                  artifacts.put(attachedArtifact.getFile().getName(), attachedArtifact.getFile()));

  // TODO: there are probably a bunch of edge cases here too

  return artifacts.entrySet().stream()
    .filter { it.getValue().isFile() }
    .collect(Collectors.toMap(e -> e.getKey(), e -> checksum(algorithm, e.getValue())));
}


// TODO: for Maven 4 use the ChecksumAlgorithmService
String checksum(String algorithm, File file) {
  try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
    MessageDigest digest = MessageDigest.getInstance(algorithm);
    final byte[] buffer = new byte[1024 * 32];
    for (; ; ) {
      int read = stream.read(buffer);
      if (read < 0) {
        break;
      }
      digest.update(buffer, 0, read);
    }
    return HexFormat.of().formatHex(digest.digest());

  } catch (NoSuchAlgorithmException e) {
    throw new IllegalStateException("No such algorithm: " + algorithm, e);
  } catch (IOException e) {
    throw new IllegalStateException("Failed to read file: " + file.getAbsolutePath(), e);
  }
}

private File checksumsFile(MavenSession session) {
  return Optional.ofNullable(session.getTopLevelProject().getProperties().getProperty("checksum.output"))
          .map(File::new)
          .orElseGet(() -> new File(session.getTopLevelProject().getBuild().getDirectory(), "checksums-sha256.txt"));
}
