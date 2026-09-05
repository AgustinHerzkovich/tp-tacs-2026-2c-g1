package com.solnotfound.storage;

import java.io.IOException;
import java.io.InputStream;

public interface ImageFile {
  String contentType();

  long size();

  InputStream openStream() throws IOException;
}
