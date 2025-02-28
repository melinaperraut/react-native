/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.react.views.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.UnderlineSpan;
import android.widget.TextView;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactTestHelper;
import com.facebook.react.modules.core.ChoreographerCompat;
import com.facebook.react.modules.core.ReactChoreographer;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.uimanager.ViewProps;
import com.facebook.react.views.view.ReactViewBackgroundDrawable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link UIManagerModule} specifically for React Text/RawText. */
@PrepareForTest({Arguments.class, ReactChoreographer.class})
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@Ignore // TODO T110934492
public class ReactTextTest {

  @Rule public PowerMockRule rule = new PowerMockRule();

  private ArrayList<ChoreographerCompat.FrameCallback> mPendingFrameCallbacks;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(Arguments.class, ReactChoreographer.class);

    ReactChoreographer uiDriverMock = mock(ReactChoreographer.class);
    PowerMockito.when(Arguments.createMap())
        .thenAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                return new JavaOnlyMap();
              }
            });
    PowerMockito.when(ReactChoreographer.getInstance()).thenReturn(uiDriverMock);

    mPendingFrameCallbacks = new ArrayList<>();
    doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                mPendingFrameCallbacks.add(
                    (ChoreographerCompat.FrameCallback) invocation.getArguments()[1]);
                return null;
              }
            })
        .when(uiDriverMock)
        .postFrameCallback(
            any(ReactChoreographer.CallbackType.class),
            any(ChoreographerCompat.FrameCallback.class));
  }

  @Test
  public void testFontSizeApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_SIZE, 21.0),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    AbsoluteSizeSpan sizeSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), AbsoluteSizeSpan.class);
    assertThat(sizeSpan.getSize()).isEqualTo(21);
  }

  @Test
  public void testBoldFontApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_WEIGHT, "bold"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isNotZero();
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isZero();
  }

  @Test
  public void testNumericBoldFontApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_WEIGHT, "500"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isNotZero();
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isZero();
  }

  @Test
  public void testItalicFontApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_STYLE, "italic"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isNotZero();
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isZero();
  }

  @Test
  public void testBoldItalicFontApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_WEIGHT, "bold", ViewProps.FONT_STYLE, "italic"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isNotZero();
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isNotZero();
  }

  @Test
  public void testNormalFontWeightApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_WEIGHT, "normal"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isZero();
  }

  @Test
  public void testNumericNormalFontWeightApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_WEIGHT, "200"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isZero();
  }

  @Test
  public void testNormalFontStyleApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_STYLE, "normal"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isZero();
  }

  @Test
  public void testFontFamilyStyleApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_FAMILY, "sans-serif"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getFontFamily()).isEqualTo("sans-serif");
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isZero();
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isZero();
  }

  @Test
  public void testFontFamilyBoldStyleApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_FAMILY, "sans-serif", ViewProps.FONT_WEIGHT, "bold"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getFontFamily()).isEqualTo("sans-serif");
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isZero();
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isNotZero();
  }

  @Test
  public void testFontFamilyItalicStyleApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_FAMILY, "sans-serif", ViewProps.FONT_STYLE, "italic"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getFontFamily()).isEqualTo("sans-serif");
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isNotZero();
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isZero();
  }

  @Test
  public void testFontFamilyBoldItalicStyleApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(
                ViewProps.FONT_FAMILY, "sans-serif",
                ViewProps.FONT_WEIGHT, "500",
                ViewProps.FONT_STYLE, "italic"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getFontFamily()).isEqualTo("sans-serif");
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isNotZero();
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isNotZero();
  }

  @Test
  public void testTextDecorationLineUnderlineApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.TEXT_DECORATION_LINE, "underline"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    TextView textView = (TextView) rootView.getChildAt(0);
    Spanned text = (Spanned) textView.getText();
    UnderlineSpan underlineSpan = getSingleSpan(textView, UnderlineSpan.class);
    StrikethroughSpan[] strikeThroughSpans =
        text.getSpans(0, text.length(), StrikethroughSpan.class);
    assertThat(underlineSpan instanceof UnderlineSpan).isTrue();
    assertThat(strikeThroughSpans).hasSize(0);
  }

  @Test
  public void testTextDecorationLineLineThroughApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.TEXT_DECORATION_LINE, "line-through"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    TextView textView = (TextView) rootView.getChildAt(0);
    Spanned text = (Spanned) textView.getText();
    UnderlineSpan[] underlineSpans = text.getSpans(0, text.length(), UnderlineSpan.class);
    StrikethroughSpan strikeThroughSpan = getSingleSpan(textView, StrikethroughSpan.class);
    assertThat(underlineSpans).hasSize(0);
    assertThat(strikeThroughSpan instanceof StrikethroughSpan).isTrue();
  }

  @Test
  public void testTextDecorationLineUnderlineLineThroughApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.TEXT_DECORATION_LINE, "underline line-through"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    UnderlineSpan underlineSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), UnderlineSpan.class);
    StrikethroughSpan strikeThroughSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), StrikethroughSpan.class);
    assertThat(underlineSpan instanceof UnderlineSpan).isTrue();
    assertThat(strikeThroughSpan instanceof StrikethroughSpan).isTrue();
  }

  @Test
  public void testBackgroundColorStyleApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.BACKGROUND_COLOR, Color.BLUE),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    Drawable backgroundDrawable = ((TextView) rootView.getChildAt(0)).getBackground();
    assertThat(((ReactViewBackgroundDrawable) backgroundDrawable).getColor()).isEqualTo(Color.BLUE);
  }

  @Test
  public void testTextTransformNoneApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    String testTextEntered =
        ".aa\tbb\t\tcc  dd EE \r\nZZ I like to eat apples. \n中文éé 我喜欢吃苹果。awdawd   ";
    String testTextTransformed = testTextEntered;

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of("textTransform", "none"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, testTextEntered));

    TextView textView = (TextView) rootView.getChildAt(0);
    assertThat(textView.getText().toString()).isEqualTo(testTextTransformed);
  }

  @Test
  public void testTextTransformUppercaseApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    String testTextEntered =
        ".aa\tbb\t\tcc  dd EE \r\nZZ I like to eat apples. \n中文éé 我喜欢吃苹果。awdawd   ";
    String testTextTransformed =
        ".AA\tBB\t\tCC  DD EE \r\nZZ I LIKE TO EAT APPLES. \n中文ÉÉ 我喜欢吃苹果。AWDAWD   ";

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of("textTransform", "uppercase"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, testTextEntered));

    TextView textView = (TextView) rootView.getChildAt(0);
    assertThat(textView.getText().toString()).isEqualTo(testTextTransformed);
  }

  @Test
  public void testTextTransformLowercaseApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    String testTextEntered =
        ".aa\tbb\t\tcc  dd EE \r\nZZ I like to eat apples. \n中文éé 我喜欢吃苹果。awdawd   ";
    String testTextTransformed =
        ".aa\tbb\t\tcc  dd ee \r\nzz i like to eat apples. \n中文éé 我喜欢吃苹果。awdawd   ";

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of("textTransform", "lowercase"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, testTextEntered));

    TextView textView = (TextView) rootView.getChildAt(0);
    assertThat(textView.getText().toString()).isEqualTo(testTextTransformed);
  }

  @Test
  public void testTextTransformCapitalizeApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    String testTextEntered =
        ".aa\tbb\t\tcc  dd EE \r\nZZ I like to eat apples. \n中文éé 我喜欢吃苹果。awdawd   ";
    String testTextTransformed =
        ".Aa\tBb\t\tCc  Dd Ee \r\nZz I Like To Eat Apples. \n中文Éé 我喜欢吃苹果。Awdawd   ";

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of("textTransform", "capitalize"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, testTextEntered));

    TextView textView = (TextView) rootView.getChildAt(0);
    assertThat(textView.getText().toString()).isEqualTo(testTextTransformed);
  }

  @Test
  public void testMaxLinesApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.NUMBER_OF_LINES, 2),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    TextView textView = (TextView) rootView.getChildAt(0);
    assertThat(textView.getText().toString()).isEqualTo("test text");
    assertThat(textView.getMaxLines()).isEqualTo(2);
    assertThat(textView.getEllipsize()).isEqualTo(TextUtils.TruncateAt.END);
  }

  @Test
  public void testTextAlignmentApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.TEXT_ALIGNMENT, "center"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    TextView textView = (TextView) rootView.getChildAt(0);
    assertThat(textView.getTextAlignment()).isEqualTo(View.TEXT_ALIGNMENT_CENTER);
  }

  @Test
  public void testCustomFontFamilyApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of(ViewProps.FONT_FAMILY, "CustomFont"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    CustomStyleSpan customStyleSpan =
        getSingleSpan((TextView) rootView.getChildAt(0), CustomStyleSpan.class);
    assertThat(customStyleSpan.getFontFamily()).isEqualTo("CustomFont");
    assertThat(customStyleSpan.getStyle() & Typeface.ITALIC).isZero();
    assertThat(customStyleSpan.getWeight() & Typeface.BOLD).isZero();
  }


  @TargetApi(Build.VERSION_CODES.O)
  @Test
  public void testTextAlignJustifyApplied() {
    UIManagerModule uiManager = getUIManagerModule();

    ReactRootView rootView =
        createText(
            uiManager,
            JavaOnlyMap.of("textAlign", "justify"),
            JavaOnlyMap.of(ReactRawTextShadowNode.PROP_TEXT, "test text"));

    TextView textView = (TextView) rootView.getChildAt(0);
    assertThat(textView.getText().toString()).isEqualTo("test text");
    assertThat(textView.getJustificationMode()).isEqualTo(Layout.JUSTIFICATION_MODE_INTER_WORD);
  }

  /** Make sure TextView has exactly one span and that span has given type. */
  private static <TSPAN> TSPAN getSingleSpan(TextView textView, Class<TSPAN> spanClass) {
    Spanned text = (Spanned) textView.getText();
    TSPAN[] spans = text.getSpans(0, text.length(), spanClass);
    assertThat(spans).hasSize(1);
    return spans[0];
  }

  private ReactRootView createText(
      UIManagerModule uiManager, JavaOnlyMap textProps, JavaOnlyMap rawTextProps) {
    ReactRootView rootView = new ReactRootView(RuntimeEnvironment.application);
    int rootTag = uiManager.addRootView(rootView);
    int textTag = rootTag + 1;
    int rawTextTag = textTag + 1;

    uiManager.createView(textTag, ReactTextViewManager.REACT_CLASS, rootTag, textProps);
    uiManager.createView(rawTextTag, ReactRawTextManager.REACT_CLASS, rootTag, rawTextProps);

    uiManager.manageChildren(
        textTag, null, null, JavaOnlyArray.of(rawTextTag), JavaOnlyArray.of(0), null);

    uiManager.manageChildren(
        rootTag, null, null, JavaOnlyArray.of(textTag), JavaOnlyArray.of(0), null);

    uiManager.onBatchComplete();
    executePendingFrameCallbacks();
    return rootView;
  }

  private void executePendingFrameCallbacks() {
    ArrayList<ChoreographerCompat.FrameCallback> callbacks =
        new ArrayList<>(mPendingFrameCallbacks);
    mPendingFrameCallbacks.clear();
    for (ChoreographerCompat.FrameCallback frameCallback : callbacks) {
      frameCallback.doFrame(0);
    }
  }

  public UIManagerModule getUIManagerModule() {
    ReactApplicationContext reactContext = ReactTestHelper.createCatalystContextForTest();
    List<ViewManager> viewManagers =
        Arrays.asList(
            new ViewManager[] {
              new ReactTextViewManager(), new ReactRawTextManager(),
            });
    UIManagerModule uiManagerModule = new UIManagerModule(reactContext, viewManagers, 0);
    uiManagerModule.onHostResume();
    return uiManagerModule;
  }
}
