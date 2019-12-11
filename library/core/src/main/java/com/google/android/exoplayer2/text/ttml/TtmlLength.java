package com.google.android.exoplayer2.text.ttml;

import android.util.Log;

import androidx.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nicola Verbeeck
 * @version 1
 */
class TtmlLength {

  private static final String TAG = "TtmlDecoder";
  private static final Pattern LENGTH_FORMAT = Pattern.compile("^(-?\\d+\\.?\\d*?)([^\\.\\s]+)$");

  static final int TYPE_PERCENTAGE = 0;
  static final int TYPE_PIXEL = 1;
  static final int TYPE_EM = 2;
  static final int TYPE_CELL = 3;
  static final int TYPE_AUTO = 4;

  final float value;
  final int type;

  TtmlLength(final float value, final int type) {
    this.value = value;
    this.type = type;
  }

  @Nullable
  static TtmlLength parse(final String param) {
    if (param.equalsIgnoreCase("auto"))
      return new TtmlLength(-1.0f, TYPE_AUTO);

    final Matcher matcher = LENGTH_FORMAT.matcher(param);
    if (!matcher.matches())
      return null;

    final Float value;
    try {
      value = Float.parseFloat(matcher.group(1));
    } catch (final NumberFormatException e) {
      Log.w(TAG, "Invalid length: " + param);
      return null;
    }

    final String type = matcher.group(2).toLowerCase();
    switch (type) {
      case "px":
        return new TtmlLength(value, TYPE_PIXEL);
      case "%":
        return new TtmlLength(value, TYPE_PERCENTAGE);
      case "em":
        return new TtmlLength(value, TYPE_EM);
      case "c":
        return new TtmlLength(value, TYPE_CELL);
      default:
        Log.w(TAG, "Unknown length type: " + param);
        return null;
    }
  }

}
