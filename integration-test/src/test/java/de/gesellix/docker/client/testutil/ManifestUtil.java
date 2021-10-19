package de.gesellix.docker.client.testutil;

import com.squareup.moshi.Moshi;
import okio.Okio;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ManifestUtil {

  private final Moshi moshi;

  public ManifestUtil() {
    this.moshi = new Moshi.Builder().build();
  }

  public File getRootLayerLocation(File baseDir) throws IOException {
    File manifest = new File(baseDir, "manifest.json");
    Map manifestEntry = (Map) moshi.adapter(List.class).fromJson(Okio.buffer(Okio.source(manifest))).get(0);
    return new File(baseDir, ((String) ((List) manifestEntry.get("Layers")).get(0)));
//    return new File(destDir, ((String) ((List) manifestEntry.get("Layers")).get(0)).replaceAll("/", File.separator));
  }
}
