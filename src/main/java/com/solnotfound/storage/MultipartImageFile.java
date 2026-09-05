package com.solnotfound.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public record MultipartImageFile(MultipartFile file) implements ImageFile {
  @Override
  public String contentType() {
    return file.getContentType();
  }

  @Override
  public long size() {
    return file.getSize();
  }

  @Override
  public InputStream openStream() throws IOException {
    return file.getInputStream();
  }
}
