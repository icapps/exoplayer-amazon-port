package com.google.android.exoplayer2.text.ttml;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Nicola Verbeeck
 * @version 1
 */
final class TtmlMetadata {

  private final Map<String, Bitmap> images;

  public TtmlMetadata() {
    images = new HashMap<>();
  }

  @Nullable
  public Bitmap getImage(final String id) {
    return images.get(id);
  }

  public void addImage(final String id, final Bitmap image) {
    images.put(id, image);
  }

}
