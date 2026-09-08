package com.solnotfound.storage;

import com.solnotfound.exception.ImageStorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "minio")
public class MinioImageStorage implements ImageStorage {
  private final MinioClient client;
  private final MinioClient publicClient;
  private final String bucket;

  public MinioImageStorage(
      @Value("${storage.minio.endpoint}") String endpoint,
      @Value("${storage.minio.public-endpoint:${storage.minio.endpoint}}") String publicEndpoint,
      @Value("${storage.minio.region:us-east-1}") String region,
      @Value("${storage.minio.access-key}") String accessKey,
      @Value("${storage.minio.secret-key}") String secretKey,
      @Value("${storage.bucket}") String bucket) {
    client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    publicClient =
        MinioClient.builder()
            .endpoint(publicEndpoint)
            .credentials(accessKey, secretKey)
            .region(region)
            .build();
    this.bucket = bucket;
  }

  /** Ensures that the configured private bucket exists before requests are accepted. */
  @PostConstruct
  public void initializeBucket() {
    try {
      if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      }
    } catch (io.minio.errors.MinioException
        | java.io.IOException
        | java.security.InvalidKeyException
        | java.security.NoSuchAlgorithmException
        | RuntimeException exception) {
      throw new ImageStorageException("Could not initialize image bucket", exception);
    }
  }

  @Override
  public void upload(String objectKey, ImageFile image) {
    try (var stream = image.openStream()) {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(stream, image.size(), -1)
              .contentType(image.contentType())
              .build());
    } catch (io.minio.errors.MinioException
        | java.io.IOException
        | java.security.InvalidKeyException
        | java.security.NoSuchAlgorithmException
        | RuntimeException exception) {
      throw new ImageStorageException("Could not store activity image", exception);
    }
  }

  @Override
  public void delete(String objectKey) {
    try {
      client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    } catch (io.minio.errors.MinioException
        | java.io.IOException
        | java.security.InvalidKeyException
        | java.security.NoSuchAlgorithmException
        | RuntimeException exception) {
      throw new ImageStorageException("Could not delete activity image", exception);
    }
  }

  @Override
  public URI signedReadUrl(String objectKey, Duration validity) {
    try {
      String url =
          publicClient.getPresignedObjectUrl(
              GetPresignedObjectUrlArgs.builder()
                  .method(Method.GET)
                  .bucket(bucket)
                  .object(objectKey)
                  .expiry(Math.toIntExact(validity.toSeconds()), TimeUnit.SECONDS)
                  .build());
      return URI.create(url);
    } catch (io.minio.errors.MinioException
        | java.io.IOException
        | java.security.InvalidKeyException
        | java.security.NoSuchAlgorithmException
        | RuntimeException exception) {
      throw new ImageStorageException("Could not create activity image URL", exception);
    }
  }
}
