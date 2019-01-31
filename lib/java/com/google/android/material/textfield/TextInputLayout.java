/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.android.material.textfield;

import com.google.android.material.R;

import static com.google.android.material.internal.ThemeEnforcement.createThemedContext;
import static com.google.android.material.textfield.IndicatorViewController.COUNTER_INDEX;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.internal.CheckableImageButton;
import com.google.android.material.internal.CollapsingTextHelper;
import com.google.android.material.internal.DescendantOffsetUtils;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;
import com.google.android.material.resources.MaterialResources;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.customview.view.AbsSavedState;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.widget.TextViewCompat;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatDrawableManager;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.TintTypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Layout which wraps a {@link TextInputEditText}, {@link android.widget.EditText}, or descendant to
 * show a floating label when the hint is hidden while the user inputs text.
 *
 * <p>Also supports:
 *
 * <ul>
 *   <li>Showing an error via {@link #setErrorEnabled(boolean)} and {@link #setError(CharSequence)}
 *   <li>Showing helper text via {@link #setHelperTextEnabled(boolean)} and {@link
 *       #setHelperText(CharSequence)}
 *   <li>Showing a character counter via {@link #setCounterEnabled(boolean)} and {@link
 *       #setCounterMaxLength(int)}
 *   <li>Password visibility toggling via the {@link #setPasswordVisibilityToggleEnabled(boolean)}
 *       API and related attribute. If enabled, a button is displayed to toggle between the password
 *       being displayed as plain-text or disguised, when your EditText is set to display a
 *       password.
 *       <p><strong>Note:</strong> When using the password toggle functionality, the 'end' compound
 *       drawable of the EditText will be overridden while the toggle is enabled. To ensure that any
 *       existing drawables are restored correctly, you should set those compound drawables
 *       relatively (start/end), as opposed to absolutely (left/right).
 * </ul>
 *
 * <p>The {@link TextInputEditText} class is provided to be used as the input text child of this
 * layout. Using TextInputEditText instead of an EditText provides accessibility support for the
 * text field and allows TextInputLayout greater control over the visual aspects of the text field.
 * An example usage is as so:
 *
 * <pre>
 * &lt;com.google.android.material.textfield.TextInputLayout
 *         android:layout_width=&quot;match_parent&quot;
 *         android:layout_height=&quot;wrap_content&quot;
 *         android:hint=&quot;@string/form_username&quot;&gt;
 *
 *     &lt;com.google.android.material.textfield.TextInputEditText
 *             android:layout_width=&quot;match_parent&quot;
 *             android:layout_height=&quot;wrap_content&quot;/&gt;
 *
 * &lt;/com.google.android.material.textfield.TextInputLayout&gt;
 * </pre>
 *
 * The hint should be set on the TextInputLayout, rather than the EditText. If a hint is specified
 * on the child EditText in XML, the TextInputLayout might still work correctly; TextInputLayout
 * will use the EditText's hint as its floating label. However, future calls to modify the hint will
 * not update TextInputLayout's hint. To avoid unintended behavior, call {@link
 * TextInputLayout#setHint(CharSequence)} and {@link TextInputLayout#getHint()} on TextInputLayout,
 * instead of on EditText.
 *
 * <p>If the {@link EditText} child is not a {@link TextInputEditText}, make sure to set the {@link
 * EditText}'s {@code android:background} to {@code null} when using an outlined or filled text
 * field. This allows {@link TextInputLayout} to set the {@link EditText}'s background to an
 * outlined or filled box, respectively.
 *
 * <p><strong>Note:</strong> The actual view hierarchy present under TextInputLayout is
 * <strong>NOT</strong> guaranteed to match the view hierarchy as written in XML. As a result, calls
 * to getParent() on children of the TextInputLayout -- such as a TextInputEditText -- may not
 * return the TextInputLayout itself, but rather an intermediate View. If you need to access a View
 * directly, set an {@code android:id} and use {@link View#findViewById(int)}.
 */
public class TextInputLayout extends LinearLayout {

  private static final int DEF_STYLE_RES = R.style.Widget_Design_TextInputLayout;

  /** Duration for the label's scale up and down animations. */
  private static final int LABEL_SCALE_ANIMATION_DURATION = 167;

  private static final int INVALID_MAX_LENGTH = -1;

  private static final String LOG_TAG = "TextInputLayout";

  private final FrameLayout inputFrame;
  EditText editText;
  private CharSequence originalHint;

  private final IndicatorViewController indicatorViewController = new IndicatorViewController(this);

  boolean counterEnabled;
  private int counterMaxLength;
  private boolean counterOverflowed;
  private TextView counterView;
  private int counterOverflowTextAppearance;
  private int counterTextAppearance;

  @Nullable private ColorStateList counterTextColor;
  @Nullable private ColorStateList counterOverflowTextColor;

  private boolean hintEnabled;
  private CharSequence hint;

  /**
   * {@code true} when providing a hint on behalf of a child {@link EditText}. If the child is an
   * instance of {@link TextInputEditText}, this value defines the behavior of its {@link
   * TextInputEditText#getHint()} method.
   */
  private boolean isProvidingHint;

  private MaterialShapeDrawable boxBackground;
  private MaterialShapeDrawable boxUnderline;
  private final ShapeAppearanceModel shapeAppearanceModel;
  private final ShapeAppearanceModel cornerAdjustedShapeAppearanceModel;

  private final int boxLabelCutoutPaddingPx;
  @BoxBackgroundMode private int boxBackgroundMode;
  private final int boxCollapsedPaddingTopPx;
  private int boxStrokeWidthPx;
  private final int boxStrokeWidthDefaultPx;
  private final int boxStrokeWidthFocusedPx;
  @ColorInt private int boxStrokeColor;
  @ColorInt private int boxBackgroundColor;

  /**
   * Values for box background mode. There is either a filled background, an outline background, or
   * no background.
   */
  @IntDef({BOX_BACKGROUND_NONE, BOX_BACKGROUND_FILLED, BOX_BACKGROUND_OUTLINE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface BoxBackgroundMode {}

  public static final int BOX_BACKGROUND_NONE = 0;
  public static final int BOX_BACKGROUND_FILLED = 1;
  public static final int BOX_BACKGROUND_OUTLINE = 2;

  private final Rect tmpRect = new Rect();
  private final Rect tmpBoundsRect = new Rect();
  private final RectF tmpRectF = new RectF();
  private Typeface typeface;

  private boolean passwordToggleEnabled;
  private Drawable passwordToggleDrawable;
  private CharSequence passwordToggleContentDesc;
  private CheckableImageButton passwordToggleView;
  private boolean passwordToggledVisible;
  private Drawable passwordToggleDummyDrawable;
  private Drawable originalEditTextEndDrawable;

  private ColorStateList passwordToggleTintList;
  private boolean hasPasswordToggleTintList;
  private PorterDuff.Mode passwordToggleTintMode;
  private boolean hasPasswordToggleTintMode;

  private ColorStateList defaultHintTextColor;
  private ColorStateList focusedTextColor;

  @ColorInt private final int defaultStrokeColor;
  @ColorInt private final int hoveredStrokeColor;
  @ColorInt private int focusedStrokeColor;

  @ColorInt private int defaultFilledBackgroundColor;
  @ColorInt private final int disabledFilledBackgroundColor;
  @ColorInt private final int hoveredFilledBackgroundColor;

  @ColorInt private final int disabledColor;

  // Only used for testing
  private boolean hintExpanded;

  final CollapsingTextHelper collapsingTextHelper = new CollapsingTextHelper(this);

  private boolean hintAnimationEnabled;
  private ValueAnimator animator;

  private boolean inDrawableStateChanged;

  private boolean restoringSavedState;

  public TextInputLayout(Context context) {
    this(context, null);
  }

  public TextInputLayout(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, R.attr.textInputStyle);
  }

  public TextInputLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(createThemedContext(context, attrs, defStyleAttr, DEF_STYLE_RES), attrs, defStyleAttr);
    // Ensure we are using the correctly themed context rather than the context that was passed in.
    context = getContext();

    setOrientation(VERTICAL);
    setWillNotDraw(false);
    setAddStatesFromChildren(true);

    inputFrame = new FrameLayout(context);
    inputFrame.setAddStatesFromChildren(true);
    addView(inputFrame);

    collapsingTextHelper.setTextSizeInterpolator(AnimationUtils.LINEAR_INTERPOLATOR);
    collapsingTextHelper.setPositionInterpolator(AnimationUtils.LINEAR_INTERPOLATOR);
    collapsingTextHelper.setCollapsedTextGravity(Gravity.TOP | GravityCompat.START);

    final TintTypedArray a =
        ThemeEnforcement.obtainTintedStyledAttributes(
            context,
            attrs,
            R.styleable.TextInputLayout,
            defStyleAttr,
            DEF_STYLE_RES,
            R.styleable.TextInputLayout_counterTextAppearance,
            R.styleable.TextInputLayout_counterOverflowTextAppearance,
            R.styleable.TextInputLayout_errorTextAppearance,
            R.styleable.TextInputLayout_helperTextTextAppearance,
            R.styleable.TextInputLayout_hintTextAppearance);

    hintEnabled = a.getBoolean(R.styleable.TextInputLayout_hintEnabled, true);
    setHint(a.getText(R.styleable.TextInputLayout_android_hint));
    hintAnimationEnabled = a.getBoolean(R.styleable.TextInputLayout_hintAnimationEnabled, true);

    shapeAppearanceModel = new ShapeAppearanceModel(context, attrs, defStyleAttr, DEF_STYLE_RES);
    cornerAdjustedShapeAppearanceModel = new ShapeAppearanceModel(shapeAppearanceModel);

    boxLabelCutoutPaddingPx =
        context
            .getResources()
            .getDimensionPixelOffset(R.dimen.mtrl_textinput_box_label_cutout_padding);
    boxCollapsedPaddingTopPx =
        a.getDimensionPixelOffset(R.styleable.TextInputLayout_boxCollapsedPaddingTop, 0);

    boxStrokeWidthDefaultPx =
        context
            .getResources()
            .getDimensionPixelSize(R.dimen.mtrl_textinput_box_stroke_width_default);
    boxStrokeWidthFocusedPx =
        context
            .getResources()
            .getDimensionPixelSize(R.dimen.mtrl_textinput_box_stroke_width_focused);
    boxStrokeWidthPx = boxStrokeWidthDefaultPx;

    float boxCornerRadiusTopStart =
        a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusTopStart, -1f);
    float boxCornerRadiusTopEnd =
        a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusTopEnd, -1f);
    float boxCornerRadiusBottomEnd =
        a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusBottomEnd, -1f);
    float boxCornerRadiusBottomStart =
        a.getDimension(R.styleable.TextInputLayout_boxCornerRadiusBottomStart, -1f);
    if (boxCornerRadiusTopStart >= 0) {
      shapeAppearanceModel.getTopLeftCorner().setCornerSize(boxCornerRadiusTopStart);
    }
    if (boxCornerRadiusTopEnd >= 0) {
      shapeAppearanceModel.getTopRightCorner().setCornerSize(boxCornerRadiusTopEnd);
    }
    if (boxCornerRadiusBottomEnd >= 0) {
      shapeAppearanceModel.getBottomRightCorner().setCornerSize(boxCornerRadiusBottomEnd);
    }
    if (boxCornerRadiusBottomStart >= 0) {
      shapeAppearanceModel.getBottomLeftCorner().setCornerSize(boxCornerRadiusBottomStart);
    }
    adjustCornerSizeForStrokeWidth();

    ColorStateList filledBackgroundColorStateList =
        MaterialResources.getColorStateList(
            context, a, R.styleable.TextInputLayout_boxBackgroundColor);
    if (filledBackgroundColorStateList != null) {
      defaultFilledBackgroundColor = filledBackgroundColorStateList.getDefaultColor();
      boxBackgroundColor = defaultFilledBackgroundColor;
      if (filledBackgroundColorStateList.isStateful()) {
        disabledFilledBackgroundColor =
            filledBackgroundColorStateList.getColorForState(
                new int[] {-android.R.attr.state_enabled}, -1);
        hoveredFilledBackgroundColor =
            filledBackgroundColorStateList.getColorForState(
                new int[] {android.R.attr.state_hovered}, -1);
      } else {
        ColorStateList mtrlFilledBackgroundColorStateList =
            AppCompatResources.getColorStateList(context, R.color.mtrl_filled_background_color);
        disabledFilledBackgroundColor =
            mtrlFilledBackgroundColorStateList.getColorForState(
                new int[] {-android.R.attr.state_enabled}, -1);
        hoveredFilledBackgroundColor =
            mtrlFilledBackgroundColorStateList.getColorForState(
                new int[] {android.R.attr.state_hovered}, -1);
      }
    } else {
      boxBackgroundColor = Color.TRANSPARENT;
      defaultFilledBackgroundColor = Color.TRANSPARENT;
      disabledFilledBackgroundColor = Color.TRANSPARENT;
      hoveredFilledBackgroundColor = Color.TRANSPARENT;
    }

    if (a.hasValue(R.styleable.TextInputLayout_android_textColorHint)) {
      defaultHintTextColor =
          focusedTextColor = a.getColorStateList(R.styleable.TextInputLayout_android_textColorHint);
    }

    ColorStateList boxStrokeColorStateList =
        MaterialResources.getColorStateList(context, a, R.styleable.TextInputLayout_boxStrokeColor);
    if (boxStrokeColorStateList != null && boxStrokeColorStateList.isStateful()) {
      defaultStrokeColor = boxStrokeColorStateList.getDefaultColor();
      disabledColor =
          boxStrokeColorStateList.getColorForState(new int[] {-android.R.attr.state_enabled}, -1);
      hoveredStrokeColor =
          boxStrokeColorStateList.getColorForState(new int[] {android.R.attr.state_hovered}, -1);
      focusedStrokeColor =
          boxStrokeColorStateList.getColorForState(new int[] {android.R.attr.state_focused}, -1);
    } else {
      // If attribute boxStrokeColor is not a color state list but only a single value, its value
      // will be applied to the box's focus state.
      focusedStrokeColor =
          a.getColor(R.styleable.TextInputLayout_boxStrokeColor, Color.TRANSPARENT);
      defaultStrokeColor =
          ContextCompat.getColor(context, R.color.mtrl_textinput_default_box_stroke_color);
      disabledColor = ContextCompat.getColor(context, R.color.mtrl_textinput_disabled_color);
      hoveredStrokeColor =
          ContextCompat.getColor(context, R.color.mtrl_textinput_hovered_box_stroke_color);
    }

    final int hintAppearance = a.getResourceId(R.styleable.TextInputLayout_hintTextAppearance, -1);
    if (hintAppearance != -1) {
      setHintTextAppearance(a.getResourceId(R.styleable.TextInputLayout_hintTextAppearance, 0));
    }

    final int errorTextAppearance =
        a.getResourceId(R.styleable.TextInputLayout_errorTextAppearance, 0);
    final boolean errorEnabled = a.getBoolean(R.styleable.TextInputLayout_errorEnabled, false);

    final int helperTextTextAppearance =
        a.getResourceId(R.styleable.TextInputLayout_helperTextTextAppearance, 0);
    final boolean helperTextEnabled =
        a.getBoolean(R.styleable.TextInputLayout_helperTextEnabled, false);
    final CharSequence helperText = a.getText(R.styleable.TextInputLayout_helperText);

    final boolean counterEnabled = a.getBoolean(R.styleable.TextInputLayout_counterEnabled, false);
    setCounterMaxLength(a.getInt(R.styleable.TextInputLayout_counterMaxLength, INVALID_MAX_LENGTH));
    counterTextAppearance = a.getResourceId(R.styleable.TextInputLayout_counterTextAppearance, 0);
    counterOverflowTextAppearance =
        a.getResourceId(R.styleable.TextInputLayout_counterOverflowTextAppearance, 0);

    passwordToggleEnabled = a.getBoolean(R.styleable.TextInputLayout_passwordToggleEnabled, false);
    passwordToggleDrawable = a.getDrawable(R.styleable.TextInputLayout_passwordToggleDrawable);
    passwordToggleContentDesc =
        a.getText(R.styleable.TextInputLayout_passwordToggleContentDescription);
    if (a.hasValue(R.styleable.TextInputLayout_passwordToggleTint)) {
      hasPasswordToggleTintList = true;
      passwordToggleTintList =
          AppCompatResources.getColorStateList(
              context, a.getResourceId(R.styleable.TextInputLayout_passwordToggleTint, -1));
    }
    if (a.hasValue(R.styleable.TextInputLayout_passwordToggleTintMode)) {
      hasPasswordToggleTintMode = true;
      passwordToggleTintMode =
          ViewUtils.parseTintMode(
              a.getInt(R.styleable.TextInputLayout_passwordToggleTintMode, -1), null);
    }

    setHelperTextEnabled(helperTextEnabled);
    setHelperText(helperText);
    setHelperTextTextAppearance(helperTextTextAppearance);
    setErrorEnabled(errorEnabled);
    setErrorTextAppearance(errorTextAppearance);
    setCounterTextAppearance(counterTextAppearance);
    setCounterOverflowTextAppearance(counterOverflowTextAppearance);

    if (a.hasValue(R.styleable.TextInputLayout_errorTextColor)) {
      setErrorTextColor(a.getColorStateList(R.styleable.TextInputLayout_errorTextColor));
    }
    if (a.hasValue(R.styleable.TextInputLayout_helperTextTextColor)) {
      setHelperTextColor(a.getColorStateList(R.styleable.TextInputLayout_helperTextTextColor));
    }
    if (a.hasValue(R.styleable.TextInputLayout_hintTextColor)) {
      setHintTextColor(a.getColorStateList(R.styleable.TextInputLayout_hintTextColor));
    }
    if (a.hasValue(R.styleable.TextInputLayout_counterTextColor)) {
      setCounterTextColor(a.getColorStateList(R.styleable.TextInputLayout_counterTextColor));
    }
    if (a.hasValue(R.styleable.TextInputLayout_counterOverflowTextColor)) {
      setCounterOverflowTextColor(
          a.getColorStateList(R.styleable.TextInputLayout_counterOverflowTextColor));
    }
    setCounterEnabled(counterEnabled);

    setBoxBackgroundMode(
        a.getInt(R.styleable.TextInputLayout_boxBackgroundMode, BOX_BACKGROUND_NONE));
    a.recycle();

    applyPasswordToggleTint();

    // For accessibility, consider TextInputLayout itself to be a simple container for an EditText,
    // and do not expose it to accessibility services.
    ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  @Override
  public void addView(View child, int index, final ViewGroup.LayoutParams params) {
    if (child instanceof EditText) {
      // Make sure that the EditText is vertically at the bottom, so that it sits on the
      // EditText's underline
      FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(params);
      flp.gravity = Gravity.CENTER_VERTICAL | (flp.gravity & ~Gravity.VERTICAL_GRAVITY_MASK);
      inputFrame.addView(child, flp);

      // Now use the EditText's LayoutParams as our own and update them to make enough space
      // for the label
      inputFrame.setLayoutParams(params);
      updateInputLayoutMargins();

      setEditText((EditText) child);
    } else {
      // Carry on adding the View...
      super.addView(child, index, params);
    }
  }

  @NonNull
  private Drawable getBoxBackground() {
    if (boxBackgroundMode == BOX_BACKGROUND_FILLED || boxBackgroundMode == BOX_BACKGROUND_OUTLINE) {
      return boxBackground;
    }
    throw new IllegalStateException();
  }

  /**
   * Set the box background mode (filled, outline, or none).
   *
   * <p>May be one of {@link #BOX_BACKGROUND_NONE}, {@link #BOX_BACKGROUND_FILLED}, or {@link
   * #BOX_BACKGROUND_OUTLINE}.
   *
   * <p>Note: This method defines TextInputLayout's internal behavior (for example, it allows the
   * hint to be displayed inline with the stroke in a cutout), but doesn't set all attributes that
   * are set in the styles provided for the box background modes. To achieve the look of an outlined
   * or filled text field, supplement this method with other methods that modify the box, such as
   * {@link #setBoxStrokeColor(int)} and {@link #setBoxBackgroundColor(int)}.
   *
   * @param boxBackgroundMode box's background mode
   * @throws IllegalArgumentException if boxBackgroundMode is not a @BoxBackgroundMode constant
   */
  public void setBoxBackgroundMode(@BoxBackgroundMode int boxBackgroundMode) {
    if (boxBackgroundMode == this.boxBackgroundMode) {
      return;
    }
    this.boxBackgroundMode = boxBackgroundMode;
    if (editText != null) {
      onApplyBoxBackgroundMode();
    }
  }

  /**
   * Get the box background mode (filled, outline, or none).
   *
   * <p>May be one of {@link #BOX_BACKGROUND_NONE}, {@link #BOX_BACKGROUND_FILLED}, or {@link
   * #BOX_BACKGROUND_OUTLINE}.
   */
  @BoxBackgroundMode
  public int getBoxBackgroundMode() {
    return boxBackgroundMode;
  }

  private void onApplyBoxBackgroundMode() {
    assignBoxBackgroundByMode();
    setEditTextBoxBackground();
    updateTextInputBoxState();
    if (boxBackgroundMode != BOX_BACKGROUND_NONE) {
      updateInputLayoutMargins();
    }
  }

  private void assignBoxBackgroundByMode() {
    switch (boxBackgroundMode) {
      case BOX_BACKGROUND_FILLED:
        boxBackground = new MaterialShapeDrawable(shapeAppearanceModel);
        boxUnderline = new MaterialShapeDrawable();
        break;
      case BOX_BACKGROUND_OUTLINE:
        if (hintEnabled && !(boxBackground instanceof CutoutDrawable)) {
          boxBackground = new CutoutDrawable(shapeAppearanceModel);
        } else {
          boxBackground = new MaterialShapeDrawable(shapeAppearanceModel);
        }
        boxUnderline = null;
        break;
      case BOX_BACKGROUND_NONE:
        boxBackground = null;
        boxUnderline = null;
        break;
      default:
        throw new IllegalArgumentException(
            boxBackgroundMode + " is illegal; only @BoxBackgroundMode constants are supported.");
    }
  }

  private void setEditTextBoxBackground() {
    // Set the EditText background to boxBackground if we should use that as the box background.
    if (shouldUseEditTextBackgroundForBoxBackground()) {
      ViewCompat.setBackground(editText, boxBackground);
    }
  }

  private boolean shouldUseEditTextBackgroundForBoxBackground() {
    // When the text field's EditText's background is null, use the EditText's background for the
    // box background.
    return editText != null
        && boxBackground != null
        && editText.getBackground() == null
        && boxBackgroundMode != BOX_BACKGROUND_NONE;
  }

  /**
   * Set the outline box's stroke color.
   *
   * <p>Calling this method when not in outline box mode will do nothing.
   *
   * @param boxStrokeColor the color to use for the box's stroke
   * @see #getBoxStrokeColor()
   */
  public void setBoxStrokeColor(@ColorInt int boxStrokeColor) {
    if (focusedStrokeColor != boxStrokeColor) {
      focusedStrokeColor = boxStrokeColor;
      updateTextInputBoxState();
    }
  }

  /**
   * Returns the box's stroke color.
   *
   * @return the color used for the box's stroke
   * @see #setBoxStrokeColor(int)
   */
  public int getBoxStrokeColor() {
    return focusedStrokeColor;
  }

  /**
   * Set the resource used for the filled box's background color.
   *
   * @param boxBackgroundColorId the resource to use for the box's background color
   */
  public void setBoxBackgroundColorResource(@ColorRes int boxBackgroundColorId) {
    setBoxBackgroundColor(ContextCompat.getColor(getContext(), boxBackgroundColorId));
  }

  /**
   * Set the filled box's background color.
   *
   * @param boxBackgroundColor the color to use for the filled box's background
   * @see #getBoxBackgroundColor()
   */
  public void setBoxBackgroundColor(@ColorInt int boxBackgroundColor) {
    if (this.boxBackgroundColor != boxBackgroundColor) {
      this.boxBackgroundColor = boxBackgroundColor;
      defaultFilledBackgroundColor = boxBackgroundColor;
      applyBoxAttributes();
    }
  }

  /**
   * Returns the box's background color.
   *
   * @return the color used for the box's background
   * @see #setBoxBackgroundColor(int)
   */
  public int getBoxBackgroundColor() {
    return boxBackgroundColor;
  }

  /**
   * Set the resources used for the box's corner radii.
   *
   * @param boxCornerRadiusTopStartId the resource to use for the box's top start corner radius
   * @param boxCornerRadiusTopEndId the resource to use for the box's top end corner radius
   * @param boxCornerRadiusBottomEndId the resource to use for the box's bottom end corner radius
   * @param boxCornerRadiusBottomStartId the resource to use for the box's bottom start corner
   *     radius
   */
  public void setBoxCornerRadiiResources(
      @DimenRes int boxCornerRadiusTopStartId,
      @DimenRes int boxCornerRadiusTopEndId,
      @DimenRes int boxCornerRadiusBottomEndId,
      @DimenRes int boxCornerRadiusBottomStartId) {
    setBoxCornerRadii(
        getContext().getResources().getDimension(boxCornerRadiusTopStartId),
        getContext().getResources().getDimension(boxCornerRadiusTopEndId),
        getContext().getResources().getDimension(boxCornerRadiusBottomStartId),
        getContext().getResources().getDimension(boxCornerRadiusBottomEndId));
  }

  /**
   * Set the box's corner radii.
   *
   * @param boxCornerRadiusTopStart the value to use for the box's top start corner radius
   * @param boxCornerRadiusTopEnd the value to use for the box's top end corner radius
   * @param boxCornerRadiusBottomEnd the value to use for the box's bottom end corner radius
   * @param boxCornerRadiusBottomStart the value to use for the box's bottom start corner radius
   * @see #getBoxCornerRadiusTopStart()
   * @see #getBoxCornerRadiusTopEnd()
   * @see #getBoxCornerRadiusBottomEnd()
   * @see #getBoxCornerRadiusBottomStart()
   */
  public void setBoxCornerRadii(
      float boxCornerRadiusTopStart,
      float boxCornerRadiusTopEnd,
      float boxCornerRadiusBottomStart,
      float boxCornerRadiusBottomEnd) {
    if (shapeAppearanceModel.getTopLeftCorner().getCornerSize() != boxCornerRadiusTopStart
        || shapeAppearanceModel.getTopRightCorner().getCornerSize() != boxCornerRadiusTopEnd
        || shapeAppearanceModel.getBottomRightCorner().getCornerSize() != boxCornerRadiusBottomEnd
        || shapeAppearanceModel.getBottomLeftCorner().getCornerSize()
            != boxCornerRadiusBottomStart) {
      shapeAppearanceModel.getTopLeftCorner().setCornerSize(boxCornerRadiusTopStart);
      shapeAppearanceModel.getTopRightCorner().setCornerSize(boxCornerRadiusTopEnd);
      shapeAppearanceModel.getBottomRightCorner().setCornerSize(boxCornerRadiusBottomEnd);
      shapeAppearanceModel.getBottomLeftCorner().setCornerSize(boxCornerRadiusBottomStart);
      applyBoxAttributes();
    }
  }

  /**
   * Returns the box's top start corner radius.
   *
   * @return the value used for the box's top start corner radius
   * @see #setBoxCornerRadii(float, float, float, float)
   */
  public float getBoxCornerRadiusTopStart() {
    return shapeAppearanceModel.getTopLeftCorner().getCornerSize();
  }

  /**
   * Returns the box's top end corner radius.
   *
   * @return the value used for the box's top end corner radius
   * @see #setBoxCornerRadii(float, float, float, float)
   */
  public float getBoxCornerRadiusTopEnd() {
    return shapeAppearanceModel.getTopRightCorner().getCornerSize();
  }

  /**
   * Returns the box's bottom end corner radius.
   *
   * @return the value used for the box's bottom end corner radius
   * @see #setBoxCornerRadii(float, float, float, float)
   */
  public float getBoxCornerRadiusBottomEnd() {
    return shapeAppearanceModel.getBottomLeftCorner().getCornerSize();
  }

  /**
   * Returns the box's bottom start corner radius.
   *
   * @return the value used for the box's bottom start corner radius
   * @see #setBoxCornerRadii(float, float, float, float)
   */
  public float getBoxCornerRadiusBottomStart() {
    return shapeAppearanceModel.getBottomRightCorner().getCornerSize();
  }

  /**
   * Adjust the corner size based on the stroke width to maintain GradientDrawable's behavior.
   * MaterialShapeDrawable internally adjusts the corner size so that the corner size does not
   * depend on the stroke width. GradientDrawable does not account for stroke width, so this causes
   * a visual diff when migrating from GradientDrawable to MaterialShapeDrawable. This method
   * reverts the corner size adjustment in MaterialShapeDrawable to maintain the visual behavior
   * from GradientDrawable for now.
   */
  private void adjustCornerSizeForStrokeWidth() {
    float strokeInset = boxBackgroundMode == BOX_BACKGROUND_OUTLINE ? boxStrokeWidthPx / 2f : 0;
    if (strokeInset <= 0f) {
      return; // Only adjust the corner size if there's a stroke inset.
    }

    float cornerRadiusTopLeft = shapeAppearanceModel.getTopLeftCorner().getCornerSize();
    cornerAdjustedShapeAppearanceModel
        .getTopLeftCorner()
        .setCornerSize(cornerRadiusTopLeft + strokeInset);

    float cornerRadiusTopRight = shapeAppearanceModel.getTopRightCorner().getCornerSize();
    cornerAdjustedShapeAppearanceModel
        .getTopRightCorner()
        .setCornerSize(cornerRadiusTopRight + strokeInset);

    float cornerRadiusBottomRight = shapeAppearanceModel.getBottomRightCorner().getCornerSize();
    cornerAdjustedShapeAppearanceModel
        .getBottomRightCorner()
        .setCornerSize(cornerRadiusBottomRight + strokeInset);

    float cornerRadiusBottomLeft = shapeAppearanceModel.getBottomLeftCorner().getCornerSize();
    cornerAdjustedShapeAppearanceModel
        .getBottomLeftCorner()
        .setCornerSize(cornerRadiusBottomLeft + strokeInset);

    ensureCornerAdjustedShapeAppearanceModel();
  }

  private void ensureCornerAdjustedShapeAppearanceModel() {
    if (boxBackgroundMode != BOX_BACKGROUND_NONE
        && getBoxBackground() instanceof MaterialShapeDrawable) {
      ((MaterialShapeDrawable) getBoxBackground())
          .setShapeAppearanceModel(cornerAdjustedShapeAppearanceModel);
    }
  }

  /**
   * Set the typeface to use for the hint and any label views (such as counter and error views).
   *
   * @param typeface typeface to use, or {@code null} to use the default.
   */
  @SuppressWarnings("ReferenceEquality") // Matches the Typeface comparison in TextView
  public void setTypeface(@Nullable Typeface typeface) {
    if (typeface != this.typeface) {
      this.typeface = typeface;

      collapsingTextHelper.setTypefaces(typeface);
      indicatorViewController.setTypefaces(typeface);

      if (counterView != null) {
        counterView.setTypeface(typeface);
      }
    }
  }

  /**
   * Returns the typeface used for the hint and any label views (such as counter and error views).
   */
  @Nullable
  public Typeface getTypeface() {
    return typeface;
  }

  @Override
  public void dispatchProvideAutofillStructure(ViewStructure structure, int flags) {
    if (originalHint == null || editText == null) {
      super.dispatchProvideAutofillStructure(structure, flags);
      return;
    }

    // Temporarily sets child's hint to its original value so it is properly set in the
    // child's ViewStructure.
    boolean wasProvidingHint = isProvidingHint;
    // Ensures a child TextInputEditText does not retrieve its hint from this TextInputLayout.
    isProvidingHint = false;
    final CharSequence hint = editText.getHint();
    editText.setHint(originalHint);
    try {
      super.dispatchProvideAutofillStructure(structure, flags);
    } finally {
      editText.setHint(hint);
      isProvidingHint = wasProvidingHint;
    }
  }

  private void setEditText(EditText editText) {
    // If we already have an EditText, throw an exception
    if (this.editText != null) {
      throw new IllegalArgumentException("We already have an EditText, can only have one");
    }

    if (!(editText instanceof TextInputEditText)) {
      Log.i(
          LOG_TAG,
          "EditText added is not a TextInputEditText. Please switch to using that"
              + " class instead.");
    }

    this.editText = editText;
    onApplyBoxBackgroundMode();
    setTextInputAccessibilityDelegate(new AccessibilityDelegate(this));

    // Use the EditText's typeface, and its text size for our expanded text.
    collapsingTextHelper.setTypefaces(this.editText.getTypeface());
    collapsingTextHelper.setExpandedTextSize(this.editText.getTextSize());

    final int editTextGravity = this.editText.getGravity();
    collapsingTextHelper.setCollapsedTextGravity(
        Gravity.TOP | (editTextGravity & ~Gravity.VERTICAL_GRAVITY_MASK));
    collapsingTextHelper.setExpandedTextGravity(editTextGravity);

    // Add a TextWatcher so that we know when the text input has changed.
    this.editText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void afterTextChanged(Editable s) {
            updateLabelState(!restoringSavedState);
            if (counterEnabled) {
              updateCounter(s.length());
            }
          }

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

    // Use the EditText's hint colors if we don't have one set
    if (defaultHintTextColor == null) {
      defaultHintTextColor = this.editText.getHintTextColors();
    }

    // If we do not have a valid hint, try and retrieve it from the EditText, if enabled
    if (hintEnabled) {
      if (TextUtils.isEmpty(hint)) {
        // Save the hint so it can be restored on dispatchProvideAutofillStructure();
        originalHint = this.editText.getHint();
        setHint(originalHint);
        // Clear the EditText's hint as we will display it ourselves
        this.editText.setHint(null);
      }
      this.isProvidingHint = true;
    }

    if (counterView != null) {
      updateCounter(this.editText.getText().length());
    }
    updateEditTextBackground();

    indicatorViewController.adjustIndicatorPadding();

    updatePasswordToggleView();

    // Update the label visibility with no animation, but force a state change
    updateLabelState(false, true);
  }

  private void updateInputLayoutMargins() {
    // Create/update the LayoutParams so that we can add enough top margin
    // to the EditText to make room for the label.
    if (boxBackgroundMode != BOX_BACKGROUND_FILLED) {
      final LayoutParams lp = (LayoutParams) inputFrame.getLayoutParams();
      final int newTopMargin = calculateLabelMarginTop();

      if (newTopMargin != lp.topMargin) {
        lp.topMargin = newTopMargin;
        inputFrame.requestLayout();
      }
    }
  }

  @Override
  public int getBaseline() {
    if (editText != null) {
      return editText.getBaseline() + getPaddingTop() + calculateLabelMarginTop();
    } else {
      return super.getBaseline();
    }
  }

  void updateLabelState(boolean animate) {
    updateLabelState(animate, false);
  }

  private void updateLabelState(boolean animate, boolean force) {
    final boolean isEnabled = isEnabled();
    final boolean hasText = editText != null && !TextUtils.isEmpty(editText.getText());
    final boolean hasFocus = editText != null && editText.hasFocus();
    final boolean errorShouldBeShown = indicatorViewController.errorShouldBeShown();

    // Set the expanded and collapsed labels to the default text color.
    if (defaultHintTextColor != null) {
      collapsingTextHelper.setCollapsedTextColor(defaultHintTextColor);
      collapsingTextHelper.setExpandedTextColor(defaultHintTextColor);
    }

    // Set the collapsed and expanded label text colors based on the current state.
    if (!isEnabled) {
      collapsingTextHelper.setCollapsedTextColor(ColorStateList.valueOf(disabledColor));
      collapsingTextHelper.setExpandedTextColor(ColorStateList.valueOf(disabledColor));
    } else if (errorShouldBeShown) {
      collapsingTextHelper.setCollapsedTextColor(indicatorViewController.getErrorViewTextColors());
    } else if (counterOverflowed && counterView != null) {
      collapsingTextHelper.setCollapsedTextColor(counterView.getTextColors());
    } else if (hasFocus && focusedTextColor != null) {
      collapsingTextHelper.setCollapsedTextColor(focusedTextColor);
    } // If none of these states apply, leave the expanded and collapsed colors as they are.

    if (hasText || (isEnabled() && (hasFocus || errorShouldBeShown))) {
      // We should be showing the label so do so if it isn't already
      if (force || hintExpanded) {
        collapseHint(animate);
      }
    } else {
      // We should not be showing the label so hide it
      if (force || !hintExpanded) {
        expandHint(animate);
      }
    }
  }

  /** Returns the {@link android.widget.EditText} used for text input. */
  @Nullable
  public EditText getEditText() {
    return editText;
  }

  /**
   * Set the hint to be displayed in the floating label, if enabled.
   *
   * @see #setHintEnabled(boolean)
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_android_hint
   */
  public void setHint(@Nullable CharSequence hint) {
    if (hintEnabled) {
      setHintInternal(hint);
      sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }
  }

  private void setHintInternal(CharSequence hint) {
    if (!TextUtils.equals(hint, this.hint)) {
      this.hint = hint;
      collapsingTextHelper.setText(hint);
      // Reset the cutout to make room for a larger hint.
      if (!hintExpanded) {
        openCutout();
      }
    }
  }

  /**
   * Returns the hint which is displayed in the floating label, if enabled.
   *
   * @return the hint, or null if there isn't one set, or the hint is not enabled.
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_android_hint
   */
  @Nullable
  public CharSequence getHint() {
    return hintEnabled ? hint : null;
  }

  /**
   * Sets whether the floating label functionality is enabled or not in this layout.
   *
   * <p>If enabled, any non-empty hint in the child EditText will be moved into the floating hint,
   * and its existing hint will be cleared. If disabled, then any non-empty floating hint in this
   * layout will be moved into the EditText, and this layout's hint will be cleared.
   *
   * @see #setHint(CharSequence)
   * @see #isHintEnabled()
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_hintEnabled
   */
  public void setHintEnabled(boolean enabled) {
    if (enabled != hintEnabled) {
      hintEnabled = enabled;
      if (!hintEnabled) {
        // Ensures a child TextInputEditText provides its internal hint, not this TextInputLayout's.
        isProvidingHint = false;
        if (!TextUtils.isEmpty(hint) && TextUtils.isEmpty(editText.getHint())) {
          // If the child EditText has no hint, but this layout does, restore it on the child.
          editText.setHint(hint);
        }
        // Now clear out any set hint
        setHintInternal(null);
      } else {
        final CharSequence editTextHint = editText.getHint();
        if (!TextUtils.isEmpty(editTextHint)) {
          // If the hint is now enabled and the EditText has one set, we'll use it if
          // we don't already have one, and clear the EditText's
          if (TextUtils.isEmpty(hint)) {
            setHint(editTextHint);
          }
          editText.setHint(null);
        }
        isProvidingHint = true;
      }

      // Now update the EditText top margin
      if (editText != null) {
        updateInputLayoutMargins();
      }
    }
  }

  /**
   * Returns whether the floating label functionality is enabled or not in this layout.
   *
   * @see #setHintEnabled(boolean)
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_hintEnabled
   */
  public boolean isHintEnabled() {
    return hintEnabled;
  }

  /**
   * Returns whether or not this layout is actively managing a child {@link EditText}'s hint. If the
   * child is an instance of {@link TextInputEditText}, this value defines the behavior of {@link
   * TextInputEditText#getHint()}.
   */
  boolean isProvidingHint() {
    return isProvidingHint;
  }

  /**
   * Sets the collapsed hint text color, size, style from the specified TextAppearance resource.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_hintTextAppearance
   */
  public void setHintTextAppearance(@StyleRes int resId) {
    collapsingTextHelper.setCollapsedTextAppearance(resId);
    focusedTextColor = collapsingTextHelper.getCollapsedTextColor();

    if (editText != null) {
      updateLabelState(false);
      // Text size might have changed so update the top margin
      updateInputLayoutMargins();
    }
  }
  /**
   * Sets the collapsed hint text color from the specified ColorStateList resource.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_hintTextColor
   */
  public void setHintTextColor(@Nullable ColorStateList hintTextColor) {
    if (collapsingTextHelper.getCollapsedTextColor() != hintTextColor) {
      collapsingTextHelper.setCollapsedTextColor(hintTextColor);
      focusedTextColor = hintTextColor;

      if (editText != null) {
        updateLabelState(false);
      }
    }
  }

  /**
   * Gets the collapsed hint text color.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_hintTextColor
   */
  @Nullable
  public ColorStateList getHintTextColor() {
    return collapsingTextHelper.getCollapsedTextColor();
  }

  /** Sets the text color used by the hint in both the collapsed and expanded states. */
  public void setDefaultHintTextColor(@Nullable ColorStateList textColor) {
    defaultHintTextColor = textColor;
    focusedTextColor = textColor;

    if (editText != null) {
      updateLabelState(false);
    }
  }

  /**
   * Returns the text color used by the hint in both the collapsed and expanded states, or null if
   * no color has been set.
   */
  @Nullable
  public ColorStateList getDefaultHintTextColor() {
    return defaultHintTextColor;
  }

  /**
   * Whether the error functionality is enabled or not in this layout. Enabling this functionality
   * before setting an error message via {@link #setError(CharSequence)}, will mean that this layout
   * will not change size when an error is displayed.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_errorEnabled
   */
  public void setErrorEnabled(boolean enabled) {
    indicatorViewController.setErrorEnabled(enabled);
  }

  /**
   * Sets the text color and size for the error message from the specified TextAppearance resource.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_errorTextAppearance
   */
  public void setErrorTextAppearance(@StyleRes int errorTextAppearance) {
    indicatorViewController.setErrorTextAppearance(errorTextAppearance);
  }

  /** Sets the text color used by the error message in all states. */
  public void setErrorTextColor(@Nullable ColorStateList errorTextColor) {
    indicatorViewController.setErrorViewTextColor(errorTextColor);
  }

  /** Returns the text color used by the error message in current state. */
  @ColorInt
  public int getErrorCurrentTextColors() {
    return indicatorViewController.getErrorViewCurrentTextColor();
  }

  /**
   * Sets the text color and size for the helper text from the specified TextAppearance resource.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_helperTextTextAppearance
   */
  public void setHelperTextTextAppearance(@StyleRes int helperTextTextAppearance) {
    indicatorViewController.setHelperTextAppearance(helperTextTextAppearance);
  }

  /** Sets the text color used by the helper text in all states. */
  public void setHelperTextColor(@Nullable ColorStateList helperTextColor) {
    indicatorViewController.setHelperTextViewTextColor(helperTextColor);
  }

  /**
   * Returns whether the error functionality is enabled or not in this layout.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_errorEnabled
   * @see #setErrorEnabled(boolean)
   */
  public boolean isErrorEnabled() {
    return indicatorViewController.isErrorEnabled();
  }

  /**
   * Whether the helper text functionality is enabled or not in this layout. Enabling this
   * functionality before setting a helper message via {@link #setHelperText(CharSequence)} will
   * mean that this layout will not change size when a helper message is displayed.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_helperTextEnabled
   */
  public void setHelperTextEnabled(boolean enabled) {
    indicatorViewController.setHelperTextEnabled(enabled);
  }

  /**
   * Sets a helper message that will be displayed below the {@link EditText}. If the {@code helper}
   * is {@code null}, the helper text functionality will be disabled and the helper message will be
   * hidden.
   *
   * <p>If the helper text functionality has not been enabled via {@link
   * #setHelperTextEnabled(boolean)}, then it will be automatically enabled if {@code helper} is not
   * empty.
   *
   * @param helperText Helper text to display
   * @see #getHelperText()
   */
  public void setHelperText(@Nullable final CharSequence helperText) {
    // If helper text is null, disable helper if it's enabled.
    if (TextUtils.isEmpty(helperText)) {
      if (isHelperTextEnabled()) {
        setHelperTextEnabled(false);
      }
    } else {
      if (!isHelperTextEnabled()) {
        setHelperTextEnabled(true);
      }
      indicatorViewController.showHelper(helperText);
    }
  }

  /**
   * Returns whether the helper text functionality is enabled or not in this layout.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_helperTextEnabled
   * @see #setHelperTextEnabled(boolean)
   */
  public boolean isHelperTextEnabled() {
    return indicatorViewController.isHelperTextEnabled();
  }

  /** Returns the text color used by the helper text in the current states. */
  @ColorInt
  public int getHelperTextCurrentTextColor() {
    return indicatorViewController.getHelperTextViewCurrentTextColor();
  }

  /**
   * Sets an error message that will be displayed below our {@link EditText}. If the {@code error}
   * is {@code null}, the error message will be cleared.
   *
   * <p>If the error functionality has not been enabled via {@link #setErrorEnabled(boolean)}, then
   * it will be automatically enabled if {@code error} is not empty.
   *
   * @param errorText Error message to display, or null to clear
   * @see #getError()
   */
  public void setError(@Nullable final CharSequence errorText) {
    if (!indicatorViewController.isErrorEnabled()) {
      if (TextUtils.isEmpty(errorText)) {
        // If error isn't enabled, and the error is empty, just return
        return;
      }
      // Else, we'll assume that they want to enable the error functionality
      setErrorEnabled(true);
    }

    if (!TextUtils.isEmpty(errorText)) {
      indicatorViewController.showError(errorText);
    } else {
      indicatorViewController.hideError();
    }
  }

  /**
   * Whether the character counter functionality is enabled or not in this layout.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterEnabled
   */
  public void setCounterEnabled(boolean enabled) {
    if (counterEnabled != enabled) {
      if (enabled) {
        counterView = new AppCompatTextView(getContext());
        counterView.setId(R.id.textinput_counter);
        if (typeface != null) {
          counterView.setTypeface(typeface);
        }
        counterView.setMaxLines(1);
        indicatorViewController.addIndicator(counterView, COUNTER_INDEX);
        updateCounterTextAppearanceAndColor();
        updateCounter();
      } else {
        indicatorViewController.removeIndicator(counterView, COUNTER_INDEX);
        counterView = null;
      }
      counterEnabled = enabled;
    }
  }

  /**
   * Sets the text color and size for the character counter using the specified TextAppearance
   * resource.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterTextAppearance
   * @see #setCounterTextColor(ColorStateList)
   */
  public void setCounterTextAppearance(int counterTextAppearance) {
    if (this.counterTextAppearance != counterTextAppearance) {
      this.counterTextAppearance = counterTextAppearance;
      updateCounterTextAppearanceAndColor();
    }
  }

  /**
   * Sets the text color for the character counter using a ColorStateList.
   *
   * <p>This text color takes precedence over a text color set in counterTextAppearance.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterTextColor
   * @param counterTextColor text color used for the character counter
   */
  public void setCounterTextColor(@Nullable ColorStateList counterTextColor) {
    if (this.counterTextColor != counterTextColor) {
      this.counterTextColor = counterTextColor;
      updateCounterTextAppearanceAndColor();
    }
  }

  /**
   * Returns the text color used for the character counter, or null if one has not been set.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterOverflowTextColor
   * @see #setCounterTextAppearance(int)
   * @return the text color used for the character counter
   */
  @Nullable
  public ColorStateList getCounterTextColor() {
    return counterTextColor;
  }

  /**
   * Sets the text color and size for the overflowed character counter using the specified
   * TextAppearance resource.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterOverflowTextAppearance
   * @see #setCounterOverflowTextColor(ColorStateList)
   */
  public void setCounterOverflowTextAppearance(int counterOverflowTextAppearance) {
    if (this.counterOverflowTextAppearance != counterOverflowTextAppearance) {
      this.counterOverflowTextAppearance = counterOverflowTextAppearance;
      updateCounterTextAppearanceAndColor();
    }
  }

  /**
   * Sets the text color for the overflowed character counter using a ColorStateList.
   *
   * <p>This text color takes precedence over a text color set in counterOverflowTextAppearance.
   *
   * @see #setCounterOverflowTextAppearance(int)
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterOverflowTextColor
   * @param counterOverflowTextColor the text color used for the overflowed character counter
   */
  public void setCounterOverflowTextColor(@Nullable ColorStateList counterOverflowTextColor) {
    if (this.counterOverflowTextColor != counterOverflowTextColor) {
      this.counterOverflowTextColor = counterOverflowTextColor;
      updateCounterTextAppearanceAndColor();
    }
  }

  /**
   * Returns the text color used for the overflowed character counter, or null if one has not been
   * set.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterOverflowTextColor
   * @see #setCounterOverflowTextAppearance(int)
   * @return the text color used for the overflowed character counter
   */
  @Nullable
  public ColorStateList getCounterOverflowTextColor() {
    return counterTextColor;
  }

  /**
   * Returns whether the character counter functionality is enabled or not in this layout.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterEnabled
   * @see #setCounterEnabled(boolean)
   */
  public boolean isCounterEnabled() {
    return counterEnabled;
  }

  /**
   * Sets the max length to display at the character counter.
   *
   * @param maxLength maxLength to display. Any value less than or equal to 0 will not be shown.
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterMaxLength
   */
  public void setCounterMaxLength(int maxLength) {
    if (counterMaxLength != maxLength) {
      if (maxLength > 0) {
        counterMaxLength = maxLength;
      } else {
        counterMaxLength = INVALID_MAX_LENGTH;
      }
      if (counterEnabled) {
        updateCounter();
      }
    }
  }

  private void updateCounter() {
    if (counterView != null) {
      updateCounter(editText == null ? 0 : editText.getText().length());
    }
  }

  void updateCounter(int length) {
    boolean wasCounterOverflowed = counterOverflowed;
    if (counterMaxLength == INVALID_MAX_LENGTH) {
      counterView.setText(String.valueOf(length));
      counterView.setContentDescription(null);
      counterOverflowed = false;
    } else {
      // Make sure the counter view region is not live to prevent spamming the user with the counter
      // overflow message on every key press.
      if (ViewCompat.getAccessibilityLiveRegion(counterView)
          == ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE) {
        ViewCompat.setAccessibilityLiveRegion(
            counterView, ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE);
      }
      counterOverflowed = length > counterMaxLength;
      updateCounterContentDescription(
          getContext(), counterView, length, counterMaxLength, counterOverflowed);

      if (wasCounterOverflowed != counterOverflowed) {
        updateCounterTextAppearanceAndColor();

        // Announce when the character limit is exceeded.
        if (counterOverflowed) {
          ViewCompat.setAccessibilityLiveRegion(
              counterView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
      }
      counterView.setText(
          getContext().getString(R.string.character_counter_pattern, length, counterMaxLength));
    }
    if (editText != null && wasCounterOverflowed != counterOverflowed) {
      updateLabelState(false);
      updateTextInputBoxState();
      updateEditTextBackground();
    }
  }

  private static void updateCounterContentDescription(
      Context context,
      TextView counterView,
      int length,
      int counterMaxLength,
      boolean counterOverflowed) {
    counterView.setContentDescription(
        context.getString(
            counterOverflowed
                ? R.string.character_counter_overflowed_content_description
                : R.string.character_counter_content_description,
            length,
            counterMaxLength));
  }

  @Override
  public void setEnabled(boolean enabled) {
    // Since we're set to addStatesFromChildren, we need to make sure that we set all
    // children to enabled/disabled otherwise any enabled children will wipe out our disabled
    // drawable state
    recursiveSetEnabled(this, enabled);
    super.setEnabled(enabled);
  }

  private static void recursiveSetEnabled(final ViewGroup vg, final boolean enabled) {
    for (int i = 0, count = vg.getChildCount(); i < count; i++) {
      final View child = vg.getChildAt(i);
      child.setEnabled(enabled);
      if (child instanceof ViewGroup) {
        recursiveSetEnabled((ViewGroup) child, enabled);
      }
    }
  }

  /**
   * Returns the max length shown at the character counter.
   *
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_counterMaxLength
   */
  public int getCounterMaxLength() {
    return counterMaxLength;
  }

  /**
   * Returns the {@code contentDescription} for accessibility purposes of the counter view, or
   * {@code null} if the counter is not enabled, not overflowed, or has no description.
   */
  @Nullable
  CharSequence getCounterOverflowDescription() {
    if (counterEnabled && counterOverflowed && (counterView != null)) {
      return counterView.getContentDescription();
    }
    return null;
  }

  private void updateCounterTextAppearanceAndColor() {
    if (counterView != null) {
      setTextAppearanceCompatWithErrorFallback(
          counterView, counterOverflowed ? counterOverflowTextAppearance : counterTextAppearance);
      if (!counterOverflowed && counterTextColor != null) {
        counterView.setTextColor(counterTextColor);
      }
      if (counterOverflowed && counterOverflowTextColor != null) {
        counterView.setTextColor(counterOverflowTextColor);
      }
    }
  }

  void setTextAppearanceCompatWithErrorFallback(TextView textView, @StyleRes int textAppearance) {
    boolean useDefaultColor = false;
    try {
      TextViewCompat.setTextAppearance(textView, textAppearance);

      if (VERSION.SDK_INT >= VERSION_CODES.M
          && textView.getTextColors().getDefaultColor() == Color.MAGENTA) {
        // Caused by our theme not extending from Theme.Design*. On API 23 and
        // above, unresolved theme attrs result in MAGENTA rather than an exception.
        // Flag so that we use a decent default
        useDefaultColor = true;
      }
    } catch (Exception e) {
      // Caused by our theme not extending from Theme.Design*. Flag so that we use
      // a decent default
      useDefaultColor = true;
    }
    if (useDefaultColor) {
      // Probably caused by our theme not extending from Theme.Design*. Instead
      // we manually set something appropriate
      TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_AppCompat_Caption);
      textView.setTextColor(ContextCompat.getColor(getContext(), R.color.design_error));
    }
  }

  private int calculateLabelMarginTop() {
    if (!hintEnabled) {
      return 0;
    }

    switch (boxBackgroundMode) {
      case BOX_BACKGROUND_OUTLINE:
        return (int) (collapsingTextHelper.getCollapsedTextHeight() / 2);
      case BOX_BACKGROUND_FILLED:
      case BOX_BACKGROUND_NONE:
        return (int) collapsingTextHelper.getCollapsedTextHeight();
      default:
        return 0;
    }
  }

  private Rect calculateCollapsedTextBounds(Rect rect) {
    if (editText == null) {
      throw new IllegalStateException();
    }
    Rect bounds = tmpBoundsRect;

    bounds.bottom = rect.bottom;
    switch (boxBackgroundMode) {
      case BOX_BACKGROUND_OUTLINE:
        bounds.left = rect.left + editText.getPaddingLeft();
        bounds.top = rect.top - calculateLabelMarginTop();
        bounds.right = rect.right - editText.getPaddingRight();
        return bounds;
      case BOX_BACKGROUND_FILLED:
        bounds.left = rect.left + editText.getCompoundPaddingLeft();
        bounds.top = rect.top + boxCollapsedPaddingTopPx;
        bounds.right = rect.right - editText.getCompoundPaddingRight();
        return bounds;
      default:
        bounds.left = rect.left + editText.getCompoundPaddingLeft();
        bounds.top = getPaddingTop();
        bounds.right = rect.right - editText.getCompoundPaddingRight();
        return bounds;
    }
  }

  private Rect calculateExpandedTextBounds(Rect rect) {
    if (editText == null) {
      throw new IllegalStateException();
    }
    Rect bounds = tmpBoundsRect;

    bounds.left = rect.left + editText.getCompoundPaddingLeft();
    bounds.top = rect.top + editText.getCompoundPaddingTop();
    bounds.right = rect.right - editText.getCompoundPaddingRight();
    bounds.bottom = rect.bottom - editText.getCompoundPaddingBottom();

    return bounds;
  }

  /*
   * Calculates the box background color that should be set.
   *
   * The filled text field has a surface layer with value {@code ?attr/colorSurface} underneath its
   * background that is taken into account when calculating the background color.
   */
  private int calculateBoxBackgroundColor() {
    int backgroundColor = boxBackgroundColor;
    if (boxBackgroundMode == BOX_BACKGROUND_FILLED) {
      int surfaceLayerColor = MaterialColors.getColor(this, R.attr.colorSurface, Color.TRANSPARENT);
      backgroundColor = MaterialColors.layer(surfaceLayerColor, boxBackgroundColor);
    }
    return backgroundColor;
  }

  private void applyBoxAttributes() {
    if (boxBackground == null) {
      return;
    }

    if (canDrawOutlineStroke()) {
      boxBackground.setStroke(boxStrokeWidthPx, boxStrokeColor);
    }

    boxBackground.setFillColor(ColorStateList.valueOf(calculateBoxBackgroundColor()));
    applyBoxUnderlineAttributes();
    invalidate();
  }

  private void applyBoxUnderlineAttributes() {
    // Exit if the underline is not being drawn by TextInputLayout.
    if (boxUnderline == null) {
      return;
    }

    if (canDrawStroke()) {
      boxUnderline.setFillColor(ColorStateList.valueOf(boxStrokeColor));
    }
    invalidate();
  }

  private boolean canDrawOutlineStroke() {
    return boxBackgroundMode == BOX_BACKGROUND_OUTLINE && canDrawStroke();
  }

  private boolean canDrawStroke() {
    return boxStrokeWidthPx > -1 && boxStrokeColor != Color.TRANSPARENT;
  }

  void updateEditTextBackground() {
    // Only update the color filter for the legacy text field, since we can directly change the
    // Paint colors of the MaterialShapeDrawable box background without having to use color filters.
    if (editText == null || boxBackgroundMode != BOX_BACKGROUND_NONE) {
      return;
    }

    Drawable editTextBackground = editText.getBackground();
    if (editTextBackground == null) {
      return;
    }

    if (androidx.appcompat.widget.DrawableUtils.canSafelyMutateDrawable(editTextBackground)) {
      editTextBackground = editTextBackground.mutate();
    }

    if (indicatorViewController.errorShouldBeShown()) {
      // Set a color filter for the error color
      editTextBackground.setColorFilter(
          AppCompatDrawableManager.getPorterDuffColorFilter(
              indicatorViewController.getErrorViewCurrentTextColor(), PorterDuff.Mode.SRC_IN));
    } else if (counterOverflowed && counterView != null) {
      // Set a color filter of the counter color
      editTextBackground.setColorFilter(
          AppCompatDrawableManager.getPorterDuffColorFilter(
              counterView.getCurrentTextColor(), PorterDuff.Mode.SRC_IN));
    } else {
      // Else reset the color filter and refresh the drawable state so that the
      // normal tint is used
      DrawableCompat.clearColorFilter(editTextBackground);
      editText.refreshDrawableState();
    }
  }

  static class SavedState extends AbsSavedState {
    CharSequence error;
    boolean isPasswordToggledVisible;

    SavedState(Parcelable superState) {
      super(superState);
    }

    SavedState(Parcel source, ClassLoader loader) {
      super(source, loader);
      error = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
      isPasswordToggledVisible = (source.readInt() == 1);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      TextUtils.writeToParcel(error, dest, flags);
      dest.writeInt(isPasswordToggledVisible ? 1 : 0);
    }

    @Override
    public String toString() {
      return "TextInputLayout.SavedState{"
          + Integer.toHexString(System.identityHashCode(this))
          + " error="
          + error
          + "}";
    }

    public static final Creator<SavedState> CREATOR =
        new ClassLoaderCreator<SavedState>() {
          @Override
          public SavedState createFromParcel(Parcel in, ClassLoader loader) {
            return new SavedState(in, loader);
          }

          @Override
          public SavedState createFromParcel(Parcel in) {
            return new SavedState(in, null);
          }

          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }

  @Override
  public Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState ss = new SavedState(superState);
    if (indicatorViewController.errorShouldBeShown()) {
      ss.error = getError();
    }
    ss.isPasswordToggledVisible = passwordToggledVisible;
    return ss;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    if (!(state instanceof SavedState)) {
      super.onRestoreInstanceState(state);
      return;
    }
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    setError(ss.error);
    if (ss.isPasswordToggledVisible) {
      passwordVisibilityToggleRequested(true /* shouldSkipAnimations */);
    }
    requestLayout();
  }

  @Override
  protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
    restoringSavedState = true;
    super.dispatchRestoreInstanceState(container);
    restoringSavedState = false;
  }

  /**
   * Returns the error message that was set to be displayed with {@link #setError(CharSequence)}, or
   * <code>null</code> if no error was set or if error displaying is not enabled.
   *
   * @see #setError(CharSequence)
   */
  @Nullable
  public CharSequence getError() {
    return indicatorViewController.isErrorEnabled() ? indicatorViewController.getErrorText() : null;
  }

  /**
   * Returns the helper message that was set to be displayed with {@link
   * #setHelperText(CharSequence)}, or <code>null</code> if no helper text was set or if helper text
   * functionality is not enabled.
   *
   * @see #setHelperText(CharSequence)
   */
  @Nullable
  public CharSequence getHelperText() {
    return indicatorViewController.isHelperTextEnabled()
        ? indicatorViewController.getHelperText()
        : null;
  }

  /**
   * Returns whether any hint state changes, due to being focused or non-empty text, are animated.
   *
   * @see #setHintAnimationEnabled(boolean)
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_hintAnimationEnabled
   */
  public boolean isHintAnimationEnabled() {
    return hintAnimationEnabled;
  }

  /**
   * Set whether any hint state changes, due to being focused or non-empty text, are animated.
   *
   * @see #isHintAnimationEnabled()
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_hintAnimationEnabled
   */
  public void setHintAnimationEnabled(boolean enabled) {
    hintAnimationEnabled = enabled;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    updatePasswordToggleView();
  }

  private void updatePasswordToggleView() {
    if (editText == null) {
      // If there is no EditText, there is nothing to update
      return;
    }

    if (shouldShowPasswordIcon()) {
      if (passwordToggleView == null) {
        passwordToggleView =
            (CheckableImageButton)
                LayoutInflater.from(getContext())
                    .inflate(R.layout.design_text_input_password_icon, inputFrame, false);
        passwordToggleView.setImageDrawable(passwordToggleDrawable);
        passwordToggleView.setContentDescription(passwordToggleContentDesc);
        inputFrame.addView(passwordToggleView);

        passwordToggleView.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                passwordVisibilityToggleRequested(false /* shouldSkipAnimations */);
              }
            });
      }

      if (editText.getMeasuredHeight() < passwordToggleView.getMeasuredHeight()) {
        // We need to make sure that the EditText's height is at least the same as the password
        // toggle's height. This ensures focus works properly, and there is no visual jump if the
        // password toggle is enabled/disabled.
        editText.setMinimumHeight(passwordToggleView.getMeasuredHeight());
        editText.post(
            new Runnable() {
              @Override
              public void run() {
                editText.requestLayout();
              }
            });
      }

      passwordToggleView.setVisibility(VISIBLE);
      passwordToggleView.setChecked(passwordToggledVisible);

      // We need to add a dummy drawable as the end compound drawable so that the text is
      // indented and doesn't display below the toggle view
      if (passwordToggleDummyDrawable == null) {
        passwordToggleDummyDrawable = new ColorDrawable();
      }
      passwordToggleDummyDrawable.setBounds(0, 0, passwordToggleView.getMeasuredWidth(), 1);

      final Drawable[] compounds = TextViewCompat.getCompoundDrawablesRelative(editText);
      // Store the user defined end compound drawable so that we can restore it later
      if (compounds[2] != passwordToggleDummyDrawable) {
        originalEditTextEndDrawable = compounds[2];
      }
      TextViewCompat.setCompoundDrawablesRelative(
          editText, compounds[0], compounds[1], passwordToggleDummyDrawable, compounds[3]);

      // Copy over the EditText's padding so that we match
      passwordToggleView.setPadding(
          editText.getPaddingLeft(),
          editText.getPaddingTop(),
          editText.getPaddingRight(),
          editText.getPaddingBottom());
    } else {
      if (passwordToggleView != null && passwordToggleView.getVisibility() == VISIBLE) {
        passwordToggleView.setVisibility(View.GONE);
      }

      if (passwordToggleDummyDrawable != null) {
        // Make sure that we remove the dummy end compound drawable if it exists, and then
        // clear it
        final Drawable[] compounds = TextViewCompat.getCompoundDrawablesRelative(editText);
        if (compounds[2] == passwordToggleDummyDrawable) {
          TextViewCompat.setCompoundDrawablesRelative(
              editText, compounds[0], compounds[1], originalEditTextEndDrawable, compounds[3]);
          passwordToggleDummyDrawable = null;
        }
      }
    }
  }

  /**
   * Set the icon to use for the password visibility toggle button.
   *
   * <p>If you use an icon you should also set a description for its action using {@link
   * #setPasswordVisibilityToggleContentDescription(CharSequence)}. This is used for accessibility.
   *
   * @param resId resource id of the drawable to set, or 0 to clear the icon
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleDrawable
   */
  public void setPasswordVisibilityToggleDrawable(@DrawableRes int resId) {
    setPasswordVisibilityToggleDrawable(
        resId != 0 ? AppCompatResources.getDrawable(getContext(), resId) : null);
  }

  /**
   * Set the icon to use for the password visibility toggle button.
   *
   * <p>If you use an icon you should also set a description for its action using {@link
   * #setPasswordVisibilityToggleContentDescription(CharSequence)}. This is used for accessibility.
   *
   * @param icon Drawable to set, may be null to clear the icon
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleDrawable
   */
  public void setPasswordVisibilityToggleDrawable(@Nullable Drawable icon) {
    passwordToggleDrawable = icon;
    if (passwordToggleView != null) {
      passwordToggleView.setImageDrawable(icon);
    }
  }

  /**
   * Set a content description for the navigation button if one is present.
   *
   * <p>The content description will be read via screen readers or other accessibility systems to
   * explain the action of the password visibility toggle.
   *
   * @param resId Resource ID of a content description string to set, or 0 to clear the description
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleContentDescription
   */
  public void setPasswordVisibilityToggleContentDescription(@StringRes int resId) {
    setPasswordVisibilityToggleContentDescription(
        resId != 0 ? getResources().getText(resId) : null);
  }

  /**
   * Set a content description for the navigation button if one is present.
   *
   * <p>The content description will be read via screen readers or other accessibility systems to
   * explain the action of the password visibility toggle.
   *
   * @param description Content description to set, or null to clear the content description
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleContentDescription
   */
  public void setPasswordVisibilityToggleContentDescription(@Nullable CharSequence description) {
    passwordToggleContentDesc = description;
    if (passwordToggleView != null) {
      passwordToggleView.setContentDescription(description);
    }
  }

  /**
   * Returns the icon currently used for the password visibility toggle button.
   *
   * @see #setPasswordVisibilityToggleDrawable(Drawable)
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleDrawable
   */
  @Nullable
  public Drawable getPasswordVisibilityToggleDrawable() {
    return passwordToggleDrawable;
  }

  /**
   * Returns the currently configured content description for the password visibility toggle button.
   *
   * <p>This will be used to describe the navigation action to users through mechanisms such as
   * screen readers.
   */
  @Nullable
  public CharSequence getPasswordVisibilityToggleContentDescription() {
    return passwordToggleContentDesc;
  }

  /**
   * Returns whether the password visibility toggle functionality is currently enabled.
   *
   * @see #setPasswordVisibilityToggleEnabled(boolean)
   */
  public boolean isPasswordVisibilityToggleEnabled() {
    return passwordToggleEnabled;
  }

  /**
   * Returns whether the password visibility toggle functionality is enabled or not.
   *
   * <p>When enabled, a button is placed at the end of the EditText which enables the user to switch
   * between the field's input being visibly disguised or not.
   *
   * @param enabled true to enable the functionality
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleEnabled
   */
  public void setPasswordVisibilityToggleEnabled(final boolean enabled) {
    if (passwordToggleEnabled != enabled) {
      passwordToggleEnabled = enabled;

      if (!enabled && passwordToggledVisible && editText != null) {
        // If the toggle is no longer enabled, but we remove the PasswordTransformation
        // to make the password visible, add it back
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
      }

      // Reset the visibility tracking flag
      passwordToggledVisible = false;

      updatePasswordToggleView();
    }
  }

  /**
   * Applies a tint to the password visibility toggle drawable. Does not modify the current tint
   * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
   *
   * <p>Subsequent calls to {@link #setPasswordVisibilityToggleDrawable(Drawable)} will
   * automatically mutate the drawable and apply the specified tint and tint mode using {@link
   * DrawableCompat#setTintList(Drawable, ColorStateList)}.
   *
   * @param tintList the tint to apply, may be null to clear tint
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleTint
   */
  public void setPasswordVisibilityToggleTintList(@Nullable ColorStateList tintList) {
    passwordToggleTintList = tintList;
    hasPasswordToggleTintList = true;
    applyPasswordToggleTint();
  }

  /**
   * Specifies the blending mode used to apply the tint specified by {@link
   * #setPasswordVisibilityToggleTintList(ColorStateList)} to the password visibility toggle
   * drawable. The default mode is {@link PorterDuff.Mode#SRC_IN}.
   *
   * @param mode the blending mode used to apply the tint, may be null to clear tint
   * @attr ref com.google.android.material.R.styleable#TextInputLayout_passwordToggleTintMode
   */
  public void setPasswordVisibilityToggleTintMode(@Nullable PorterDuff.Mode mode) {
    passwordToggleTintMode = mode;
    hasPasswordToggleTintMode = true;
    applyPasswordToggleTint();
  }

  /**
   * Handles visibility for a password toggle icon when changing obfuscation in a password edit
   * text. Public so that clients can override this method for custom UI changes when toggling the
   * display of password text
   *
   * @param shouldSkipAnimations true if the password toggle indicator icon should not animate
   *     changes
   */
  public void passwordVisibilityToggleRequested(boolean shouldSkipAnimations) {
    if (passwordToggleEnabled) {
      // Store the current cursor position
      final int selection = editText.getSelectionEnd();

      if (hasPasswordTransformation()) {
        editText.setTransformationMethod(null);
        passwordToggledVisible = true;
      } else {
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordToggledVisible = false;
      }

      passwordToggleView.setChecked(passwordToggledVisible);
      if (shouldSkipAnimations) {
        passwordToggleView.jumpDrawablesToCurrentState();
      }

      // And restore the cursor position
      editText.setSelection(selection);
    }
  }

  /**
   * Sets an {@link TextInputLayout.AccessibilityDelegate} providing an accessibility implementation
   * for the {@link EditText} used by this layout.
   *
   * <p>Note: This method should be used in place of providing an {@link AccessibilityDelegate}
   * directly on the {@link EditText}.
   */
  public void setTextInputAccessibilityDelegate(TextInputLayout.AccessibilityDelegate delegate) {
    if (editText != null) {
      ViewCompat.setAccessibilityDelegate(editText, delegate);
    }
  }

  private boolean hasPasswordTransformation() {
    return editText != null
        && editText.getTransformationMethod() instanceof PasswordTransformationMethod;
  }

  private boolean shouldShowPasswordIcon() {
    return passwordToggleEnabled && (hasPasswordTransformation() || passwordToggledVisible);
  }

  private void applyPasswordToggleTint() {
    if (passwordToggleDrawable != null
        && (hasPasswordToggleTintList || hasPasswordToggleTintMode)) {
      passwordToggleDrawable = DrawableCompat.wrap(passwordToggleDrawable).mutate();

      if (hasPasswordToggleTintList) {
        DrawableCompat.setTintList(passwordToggleDrawable, passwordToggleTintList);
      }
      if (hasPasswordToggleTintMode) {
        DrawableCompat.setTintMode(passwordToggleDrawable, passwordToggleTintMode);
      }

      if (passwordToggleView != null
          && passwordToggleView.getDrawable() != passwordToggleDrawable) {
        passwordToggleView.setImageDrawable(passwordToggleDrawable);
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    if (editText != null) {
      Rect rect = tmpRect;
      DescendantOffsetUtils.getDescendantRect(this, editText, rect);
      updateBoxUnderlineBounds(rect);

      if (hintEnabled) {
        collapsingTextHelper.setCollapsedBounds(calculateCollapsedTextBounds(rect));
        collapsingTextHelper.setExpandedBounds(calculateExpandedTextBounds(rect));
        collapsingTextHelper.recalculate();

        // If the label should be collapsed, set the cutout bounds on the CutoutDrawable to make
        // sure it draws with a cutout in draw().
        if (cutoutEnabled() && !hintExpanded) {
          openCutout();
        }
      }
    }
  }

  private void updateBoxUnderlineBounds(Rect bounds) {
    if (boxUnderline != null) {
      int top = bounds.bottom - boxStrokeWidthFocusedPx;
      boxUnderline.setBounds(bounds.left, top, bounds.right, bounds.bottom);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    drawHint(canvas);
    drawBoxUnderline(canvas);
  }

  private void drawHint(Canvas canvas) {
    if (hintEnabled) {
      collapsingTextHelper.draw(canvas);
    }
  }

  private void drawBoxUnderline(Canvas canvas) {
    if (boxUnderline != null) {
      // Draw using the current boxStrokeWidth.
      Rect underlineBounds = boxUnderline.getBounds();
      underlineBounds.top = underlineBounds.bottom - boxStrokeWidthPx;
      boxUnderline.draw(canvas);
    }
  }

  private void collapseHint(boolean animate) {
    if (animator != null && animator.isRunning()) {
      animator.cancel();
    }
    if (animate && hintAnimationEnabled) {
      animateToExpansionFraction(1f);
    } else {
      collapsingTextHelper.setExpansionFraction(1f);
    }
    hintExpanded = false;
    if (cutoutEnabled()) {
      openCutout();
    }
  }

  private boolean cutoutEnabled() {
    return hintEnabled && !TextUtils.isEmpty(hint) && boxBackground instanceof CutoutDrawable;
  }

  private void openCutout() {
    if (!cutoutEnabled()) {
      return;
    }
    final RectF cutoutBounds = tmpRectF;
    collapsingTextHelper.getCollapsedTextActualBounds(cutoutBounds);
    applyCutoutPadding(cutoutBounds);
    // Offset the cutout bounds by the TextInputLayout's left padding to ensure that the cutout is
    // inset relative to the TextInputLayout's bounds.
    cutoutBounds.offset(-getPaddingLeft(), 0);
    ((CutoutDrawable) boxBackground).setCutout(cutoutBounds);
  }

  private void closeCutout() {
    if (cutoutEnabled()) {
      ((CutoutDrawable) boxBackground).removeCutout();
    }
  }

  private void applyCutoutPadding(RectF cutoutBounds) {
    cutoutBounds.left -= boxLabelCutoutPaddingPx;
    cutoutBounds.top -= boxLabelCutoutPaddingPx;
    cutoutBounds.right += boxLabelCutoutPaddingPx;
    cutoutBounds.bottom += boxLabelCutoutPaddingPx;
  }

  @VisibleForTesting
  boolean cutoutIsOpen() {
    return cutoutEnabled() && ((CutoutDrawable) boxBackground).hasCutout();
  }

  @Override
  protected void drawableStateChanged() {
    if (inDrawableStateChanged) {
      // Some of the calls below will update the drawable state of child views. Since we're
      // using addStatesFromChildren we can get into infinite recursion, hence we'll just
      // exit in this instance
      return;
    }

    inDrawableStateChanged = true;

    super.drawableStateChanged();

    final int[] state = getDrawableState();
    boolean changed = false;

    if (collapsingTextHelper != null) {
      changed |= collapsingTextHelper.setState(state);
    }

    // Drawable state has changed so see if we need to update the label
    updateLabelState(ViewCompat.isLaidOut(this) && isEnabled());
    updateEditTextBackground();
    updateTextInputBoxState();

    if (changed) {
      invalidate();
    }

    inDrawableStateChanged = false;
  }

  void updateTextInputBoxState() {
    if (boxBackground == null || boxBackgroundMode == BOX_BACKGROUND_NONE) {
      return;
    }

    final boolean hasFocus = isFocused() || (editText != null && editText.hasFocus());
    final boolean isHovered = isHovered() || (editText != null && editText.isHovered());

    // Update the text box's stroke color based on the current state.
    if (!isEnabled()) {
      boxStrokeColor = disabledColor;
    } else if (indicatorViewController.errorShouldBeShown()) {
      boxStrokeColor = indicatorViewController.getErrorViewCurrentTextColor();
    } else if (counterOverflowed && counterView != null) {
      boxStrokeColor = counterView.getCurrentTextColor();
    } else if (hasFocus) {
      boxStrokeColor = focusedStrokeColor;
    } else if (isHovered) {
      boxStrokeColor = hoveredStrokeColor;
    } else {
      boxStrokeColor = defaultStrokeColor;
    }

    // Update the text box's stroke width based on the current state.
    if ((isHovered || hasFocus) && isEnabled()) {
      boxStrokeWidthPx = boxStrokeWidthFocusedPx;
      adjustCornerSizeForStrokeWidth();
    } else {
      boxStrokeWidthPx = boxStrokeWidthDefaultPx;
      adjustCornerSizeForStrokeWidth();
    }

    // Update the text box's background color based on the current state.
    if (boxBackgroundMode == BOX_BACKGROUND_FILLED) {
      if (!isEnabled()) {
        boxBackgroundColor = disabledFilledBackgroundColor;
      } else if (isHovered) {
        boxBackgroundColor = hoveredFilledBackgroundColor;
      } else {
        boxBackgroundColor = defaultFilledBackgroundColor;
      }
    }

    applyBoxAttributes();
  }

  private void expandHint(boolean animate) {
    if (animator != null && animator.isRunning()) {
      animator.cancel();
    }
    if (animate && hintAnimationEnabled) {
      animateToExpansionFraction(0f);
    } else {
      collapsingTextHelper.setExpansionFraction(0f);
    }
    if (cutoutEnabled() && ((CutoutDrawable) boxBackground).hasCutout()) {
      closeCutout();
    }
    hintExpanded = true;
  }

  @VisibleForTesting
  void animateToExpansionFraction(final float target) {
    if (collapsingTextHelper.getExpansionFraction() == target) {
      return;
    }
    if (this.animator == null) {
      this.animator = new ValueAnimator();
      this.animator.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
      this.animator.setDuration(LABEL_SCALE_ANIMATION_DURATION);
      this.animator.addUpdateListener(
          new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
              collapsingTextHelper.setExpansionFraction((float) animator.getAnimatedValue());
            }
          });
    }
    this.animator.setFloatValues(collapsingTextHelper.getExpansionFraction(), target);
    this.animator.start();
  }

  @VisibleForTesting
  final boolean isHintExpanded() {
    return hintExpanded;
  }

  @VisibleForTesting
  final boolean isHelperTextDisplayed() {
    return indicatorViewController.helperTextIsDisplayed();
  }

  @VisibleForTesting
  final int getHintCurrentCollapsedTextColor() {
    return collapsingTextHelper.getCurrentCollapsedTextColor();
  }

  @VisibleForTesting
  final float getHintCollapsedTextHeight() {
    return collapsingTextHelper.getCollapsedTextHeight();
  }

  @VisibleForTesting
  final int getErrorTextCurrentColor() {
    return indicatorViewController.getErrorViewCurrentTextColor();
  }

  /**
   * An AccessibilityDelegate intended to be set on an {@link EditText} or {@link TextInputEditText}
   * with {@link
   * TextInputLayout#setTextInputAccessibilityDelegate(TextInputLayout.AccessibilityDelegate) to
   * provide attributes for accessibility that are managed by {@link TextInputLayout}.
   */
  public static class AccessibilityDelegate extends AccessibilityDelegateCompat {
    private final TextInputLayout layout;

    public AccessibilityDelegate(TextInputLayout layout) {
      this.layout = layout;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
      super.onInitializeAccessibilityNodeInfo(host, info);
      EditText editText = layout.getEditText();
      CharSequence text = (editText != null) ? editText.getText() : null;
      CharSequence hintText = layout.getHint();
      CharSequence errorText = layout.getError();
      CharSequence counterDesc = layout.getCounterOverflowDescription();
      boolean showingText = !TextUtils.isEmpty(text);
      boolean hasHint = !TextUtils.isEmpty(hintText);
      boolean showingError = !TextUtils.isEmpty(errorText);
      boolean contentInvalid = showingError || !TextUtils.isEmpty(counterDesc);

      if (showingText) {
        info.setText(text);
      } else if (hasHint) {
        info.setText(hintText);
      }

      if (hasHint) {
        info.setHintText(hintText);
        info.setShowingHintText(!showingText && hasHint);
      }

      if (contentInvalid) {
        info.setError(showingError ? errorText : counterDesc);
        info.setContentInvalid(true);
      }
    }

    @Override
    public void onPopulateAccessibilityEvent(View host, AccessibilityEvent event) {
      super.onPopulateAccessibilityEvent(host, event);
      EditText editText = layout.getEditText();
      CharSequence text = (editText != null) ? editText.getText() : null;
      CharSequence eventText = TextUtils.isEmpty(text) ? layout.getHint() : text;
      if (!TextUtils.isEmpty(eventText)) {
        event.getText().add(eventText);
      }
    }
  }
}
