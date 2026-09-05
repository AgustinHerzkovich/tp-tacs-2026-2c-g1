package com.solnotfound.storage;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import com.solnotfound.exception.ImageStorageException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "gcs")
public class GoogleCloudImageStorage implements ImageStorage {
  private final Storage storage;
  private final String bucket;

  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring injects the shared Google Cloud Storage client")
  public GoogleCloudImageStorage(Storage storage, @Value("${storage.bucket}") String bucket) {
    this.storage = storage;
    this.bucket = bucket;
  }

  @Override
  public void upload(String objectKey, ImageFile image) {
    BlobInfo blob =
        BlobInfo.newBuilder(BlobId.of(bucket, objectKey))
            .setContentType(image.contentType())
            .build();
    try (var stream = image.openStream()) {
      storage.createFrom(blob, stream);
    } catch (java.io.IOException | RuntimeException exception) {
      throw new ImageStorageException("Could not store activity image", exception);
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      storage.delete(BlobId.of(bucket, objectKey));
    } catch (RuntimeException exception) {
      throw new ImageStorageException("Could not delete activity image", exception);
    }
  }

  @Override
  public URI signedReadUrl(String objectKey, Duration validity) {
    try {
      return storage
          .signUrl(
              BlobInfo.newBuilder(BlobId.of(bucket, objectKey)).build(),
              validity.toSeconds(),
              TimeUnit.SECONDS,
              SignUrlOption.withV4Signature())
          .toURI();
    } catch (URISyntaxException | RuntimeException exception) {
      throw new ImageStorageException("Could not create activity image URL", exception);
    }
  }
}
