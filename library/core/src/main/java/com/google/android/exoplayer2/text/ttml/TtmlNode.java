/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.text.ttml;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.util.Assertions;

import java.util.*;
import java.util.Map.Entry;

/**
 * A package internal representation of TTML node.
 */
/* package */ final class TtmlNode {

  public static final String TAG_TT = "tt";
  public static final String TAG_HEAD = "head";
  public static final String TAG_BODY = "body";
  public static final String TAG_DIV = "div";
  public static final String TAG_P = "p";
  public static final String TAG_SPAN = "span";
  public static final String TAG_BR = "br";
  public static final String TAG_STYLE = "style";
  public static final String TAG_STYLING = "styling";
  public static final String TAG_LAYOUT = "layout";
  public static final String TAG_REGION = "region";
  public static final String TAG_METADATA = "metadata";
  public static final String TAG_SMPTE_IMAGE = "smpte:image";
  public static final String TAG_SMPTE_DATA = "smpte:data";
  public static final String TAG_SMPTE_INFORMATION = "smpte:information";

  public static final String ANONYMOUS_REGION_ID = "";
  public static final String ATTR_ID = "id";
  public static final String ATTR_TTS_ORIGIN = "origin";
  public static final String ATTR_TTS_EXTENT = "extent";
  public static final String ATTR_TTS_DISPLAY_ALIGN = "displayAlign";
  public static final String ATTR_TTS_BACKGROUND_COLOR = "backgroundColor";
  public static final String ATTR_TTS_FONT_STYLE = "fontStyle";
  public static final String ATTR_TTS_FONT_SIZE = "fontSize";
  public static final String ATTR_TTS_FONT_FAMILY = "fontFamily";
  public static final String ATTR_TTS_FONT_WEIGHT = "fontWeight";
  public static final String ATTR_TTS_COLOR = "color";
  public static final String ATTR_TTS_TEXT_DECORATION = "textDecoration";
  public static final String ATTR_TTS_TEXT_ALIGN = "textAlign";

  public static final String LINETHROUGH = "linethrough";
  public static final String NO_LINETHROUGH = "nolinethrough";
  public static final String UNDERLINE = "underline";
  public static final String NO_UNDERLINE = "nounderline";
  public static final String ITALIC = "italic";
  public static final String BOLD = "bold";

  public static final String LEFT = "left";
  public static final String CENTER = "center";
  public static final String RIGHT = "right";
  public static final String START = "start";
  public static final String END = "end";

  public final String tag;
  public final String text;
  public final boolean isTextNode;
  public final long startTimeUs;
  public final long endTimeUs;
  public final TtmlStyle style;
  public final String regionId;
  public final String backgroundImageId;

  private final String[] styleIds;
  private final HashMap<String, Integer> nodeStartsByRegion;
  private final HashMap<String, Integer> nodeEndsByRegion;

  private List<TtmlNode> children;

  public static TtmlNode buildTextNode(String text) {
    return new TtmlNode(null, TtmlRenderUtil.applyTextElementSpacePolicy(text), C.TIME_UNSET,
            C.TIME_UNSET, null, null, ANONYMOUS_REGION_ID, null);
  }

  public static TtmlNode buildNode(String tag, long startTimeUs, long endTimeUs,
                                   TtmlStyle style, String[] styleIds, String regionId, String backgroundImageId) {
    return new TtmlNode(tag, null, startTimeUs, endTimeUs, style, styleIds, regionId, backgroundImageId);
  }

  private TtmlNode(String tag, String text, long startTimeUs, long endTimeUs,
                   TtmlStyle style, String[] styleIds, String regionId, String backgroundImageId) {
    this.tag = tag;
    this.text = text;
    this.style = style;
    this.styleIds = styleIds;
    this.isTextNode = text != null;
    this.startTimeUs = startTimeUs;
    this.endTimeUs = endTimeUs;
    this.regionId = Assertions.checkNotNull(regionId);
    this.backgroundImageId = backgroundImageId;
    nodeStartsByRegion = new HashMap<>();
    nodeEndsByRegion = new HashMap<>();
  }

  public boolean isActive(long timeUs) {
    return (startTimeUs == C.TIME_UNSET && endTimeUs == C.TIME_UNSET)
        || (startTimeUs <= timeUs && endTimeUs == C.TIME_UNSET)
        || (startTimeUs == C.TIME_UNSET && timeUs < endTimeUs)
        || (startTimeUs <= timeUs && timeUs < endTimeUs);
  }

  public void addChild(TtmlNode child) {
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(child);
  }

  public TtmlNode getChild(int index) {
    if (children == null) {
      throw new IndexOutOfBoundsException();
    }
    return children.get(index);
  }

  public int getChildCount() {
    return children == null ? 0 : children.size();
  }

  public long[] getEventTimesUs() {
    TreeSet<Long> eventTimeSet = new TreeSet<>();
    getEventTimes(eventTimeSet, false);
    long[] eventTimes = new long[eventTimeSet.size()];
    int i = 0;
    for (long eventTimeUs : eventTimeSet) {
      eventTimes[i++] = eventTimeUs;
    }
    return eventTimes;
  }

  private void getEventTimes(TreeSet<Long> out, boolean descendsPNode) {
    boolean isPNode = TAG_P.equals(tag);
    if (descendsPNode || isPNode) {
      if (startTimeUs != C.TIME_UNSET) {
        out.add(startTimeUs);
      }
      if (endTimeUs != C.TIME_UNSET) {
        out.add(endTimeUs);
      }
    }
    if (children == null) {
      return;
    }
    for (int i = 0; i < children.size(); i++) {
      children.get(i).getEventTimes(out, descendsPNode || isPNode);
    }
  }

  public String[] getStyleIds() {
    return styleIds;
  }

  public List<Cue> getCues(long timeUs, Map<String, TtmlStyle> globalStyles,
      Map<String, TtmlRegion> regionMap, TtmlMetadata metadata) {
    TreeMap<String, SubtitleAccumulator> regionOutputs = new TreeMap<>();
    traverseForText(timeUs, false, regionId, regionOutputs, metadata);
    traverseForStyle(timeUs, globalStyles, regionOutputs);
    List<Cue> cues = new ArrayList<>();
    for (Entry<String, SubtitleAccumulator> entry : regionOutputs.entrySet()) {
      TtmlRegion region = regionMap.get(entry.getKey());
      final Bitmap backgroundImage = entry.getValue().backgroundImage;
      cues.add(
          new Cue(
              cleanUpText(entry.getValue().builder),
              /* textAlignment= */ null,
              backgroundImage,
              getLineYValue(region),                  //line
              getLineType(region),                    //lineType
              region.lineAnchor,                      //lineAnchor
              getLineXValue(region.x),                //Position
              /* positionAnchor= */ Cue.TYPE_UNSET,   //positionAnchor
              region.textSizeType,                    //textSizeType
              region.textSize,                        //textSize
              getLineXValue(region.width),            //size
             1.0f,
              false,
              Color.BLACK));
    }
    return cues;
  }

  @SuppressLint("SwitchIntDef")
  private static float getLineYValue(final TtmlRegion region){
    final float value;
    if (region.y.type == TtmlLength.TYPE_PERCENTAGE)
      value = region.y.value / 100.0f;
    else
      value = region.y.value;
    switch (region.lineAnchor) {
      case Cue.ANCHOR_TYPE_MIDDLE:
        return value + region.height.value / 2.0f;
      case Cue.ANCHOR_TYPE_END:
        return value + region.height.value;
    }
    return value;
  }

  private static float getLineXValue(final TtmlLength length){
    if (length.type == TtmlLength.TYPE_PERCENTAGE)
      return length.value / 100.0f;
    return length.value;
  }


  private static int getLineType(final TtmlRegion region){
    return Cue.LINE_TYPE_FRACTION;
  }

  private void traverseForText(long timeUs,
                               boolean descendsPNode,
                               String inheritedRegion,
                               Map<String, SubtitleAccumulator> regionOutputs,
                               TtmlMetadata metadata) {
    nodeStartsByRegion.clear();
    nodeEndsByRegion.clear();

    if (TAG_METADATA.equals(tag)) {
      // Ignore metadata tag.
      return;
    }

    String resolvedRegionId = ANONYMOUS_REGION_ID.equals(regionId) ? inheritedRegion : regionId;

    if (isTextNode && descendsPNode) {
      getRegionOutput(resolvedRegionId, regionOutputs).append(text);
    } else if (TAG_BR.equals(tag) && descendsPNode) {
      getRegionOutput(resolvedRegionId, regionOutputs).append('\n');
    } else if (isActive(timeUs)) {
      if (backgroundImageId != null)
        getRegionOutput(resolvedRegionId, regionOutputs).backgroundImage = metadata.getImage(backgroundImageId);

      // This is a container node, which can contain zero or more children.
      for (Entry<String, SubtitleAccumulator> entry : regionOutputs.entrySet()) {
        nodeStartsByRegion.put(entry.getKey(), entry.getValue().length());
      }

      boolean isPNode = TAG_P.equals(tag);
      for (int i = 0; i < getChildCount(); i++) {
        getChild(i).traverseForText(timeUs, descendsPNode || isPNode, resolvedRegionId,
                regionOutputs, metadata);
      }
      if (isPNode) {
        TtmlRenderUtil.endParagraph(getRegionOutput(resolvedRegionId, regionOutputs).builder);
      }

      for (Entry<String, SubtitleAccumulator> entry : regionOutputs.entrySet()) {
        nodeEndsByRegion.put(entry.getKey(), entry.getValue().length());
      }
    }
  }

  private static SubtitleAccumulator getRegionOutput(String resolvedRegionId,
      Map<String, SubtitleAccumulator> regionOutputs) {
    if (!regionOutputs.containsKey(resolvedRegionId)) {
      regionOutputs.put(resolvedRegionId, new SubtitleAccumulator());
    }
    return regionOutputs.get(resolvedRegionId);
  }

  private void traverseForStyle(
      long timeUs,
      Map<String, TtmlStyle> globalStyles,
      Map<String, SubtitleAccumulator> regionOutputs) {
    if (!isActive(timeUs)) {
      return;
    }
    for (Entry<String, Integer> entry : nodeEndsByRegion.entrySet()) {
      String regionId = entry.getKey();
      int start = nodeStartsByRegion.containsKey(regionId) ? nodeStartsByRegion.get(regionId) : 0;
      int end = entry.getValue();
      if (start != end) {
        SubtitleAccumulator regionOutput = regionOutputs.get(regionId);
        applyStyleToOutput(globalStyles, regionOutput, start, end);
      }
    }
    for (int i = 0; i < getChildCount(); ++i) {
      getChild(i).traverseForStyle(timeUs, globalStyles, regionOutputs);
    }
  }

  private void applyStyleToOutput(
      Map<String, TtmlStyle> globalStyles,
      SubtitleAccumulator regionOutput,
      int start,
      int end) {
    TtmlStyle resolvedStyle = TtmlRenderUtil.resolveStyle(style, styleIds, globalStyles);
    if (resolvedStyle != null) {
      TtmlRenderUtil.applyStylesToSpan(regionOutput.builder, start, end, resolvedStyle);
    }
  }

  private SpannableStringBuilder cleanUpText(SpannableStringBuilder builder) {
    // Having joined the text elements, we need to do some final cleanup on the result.
    // 1. Collapse multiple consecutive spaces into a single space.
    int builderLength = builder.length();
    for (int i = 0; i < builderLength; i++) {
      if (builder.charAt(i) == ' ') {
        int j = i + 1;
        while (j < builder.length() && builder.charAt(j) == ' ') {
          j++;
        }
        int spacesToDelete = j - (i + 1);
        if (spacesToDelete > 0) {
          builder.delete(i, i + spacesToDelete);
          builderLength -= spacesToDelete;
        }
      }
    }
    // 2. Remove any spaces from the start of each line.
    if (builderLength > 0 && builder.charAt(0) == ' ') {
      builder.delete(0, 1);
      builderLength--;
    }
    for (int i = 0; i < builderLength - 1; i++) {
      if (builder.charAt(i) == '\n' && builder.charAt(i + 1) == ' ') {
        builder.delete(i + 1, i + 2);
        builderLength--;
      }
    }
    // 3. Remove any spaces from the end of each line.
    if (builderLength > 0 && builder.charAt(builderLength - 1) == ' ') {
      builder.delete(builderLength - 1, builderLength);
      builderLength--;
    }
    for (int i = 0; i < builderLength - 1; i++) {
      if (builder.charAt(i) == ' ' && builder.charAt(i + 1) == '\n') {
        builder.delete(i, i + 1);
        builderLength--;
      }
    }
    // 4. Trim a trailing newline, if there is one.
    if (builderLength > 0 && builder.charAt(builderLength - 1) == '\n') {
      builder.delete(builderLength - 1, builderLength);
      /*builderLength--;*/
    }
    return builder;
  }

  private static class SubtitleAccumulator {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    Bitmap backgroundImage = null;

    public void append(CharSequence text) {
      builder.append(text);
    }

    public void append(char c) {
      builder.append(c);
    }

    public int length() {
      return builder.length();
    }
  }

}
