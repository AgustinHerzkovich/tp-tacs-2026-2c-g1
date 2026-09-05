package com.solnotfound.storage;

import com.solnotfound.exception.ImageStorageException;
import java.net.URI;
import java.time.Duration;

public interface ImageStorage {

  /**
   * Stores an image under the supplied object key.
   *
   * @param objectKey stable key assigned by the activity service
   * @param image validated image content
   * @throws ImageStorageException when the object cannot be persisted
   */
  void upload(String objectKey, ImageFile image);

  /**
   * Removes an image. Implementations should tolerate an already absent object.
   *
   * @param objectKey key of the object to remove
   * @throws ImageStorageException when the storage operation fails
   */
  void delete(String objectKey);

  /**
   * Creates a temporary URL that permits reading a private image.
   *
   * @param objectKey key of the image
   * @param validity requested URL validity
   * @return signed URL for direct browser access
   * @throws ImageStorageException when the URL cannot be generated
   */
  URI signedReadUrl(String objectKey, Duration validity);
}
