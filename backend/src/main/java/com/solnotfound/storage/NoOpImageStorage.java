package com.solnotfound.storage;

import com.solnotfound.exception.ImageStorageException;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "storage.provider", havingValue = "none", matchIfMissing = true)
public class NoOpImageStorage implements ImageStorage {
  @Override
  public void upload(String objectKey, ImageFile image) {
    throw new ImageStorageException("Image storage is not configured", null);
  }

  @Override
  public void delete(String objectKey) {}

  @Override
  public URI signedReadUrl(String objectKey, Duration validity) {
    throw new ImageStorageException("Image storage is not configured", null);
  }
}
