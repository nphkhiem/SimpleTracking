package com.khiemnph.simpletracking.utils.extension

import android.R
import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.*
import android.text.Annotation
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.*
import androidx.core.content.res.ResourcesCompat
import com.khiemnph.simpletracking.utils.AppUtil
import java.util.*

/**
 * Created by Khiem Nguyen on 12/28/2020.
 */

// View

val View.centerX: Int
    get() = (right + left) / 2

val View.centerY: Int
    get() = (bottom + top) / 2

val View.contentLeft: Int
    get() = left + paddingLeft

val View.contentTop: Int
    get() = top + paddingTop

val View.contentRight: Int
    get() = right - paddingRight

val View.contentBottom: Int
    get() = bottom - paddingBottom

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

fun View.isNotVisible(): Boolean {
    return visibility != View.VISIBLE
}

fun View.isInvisible(): Boolean {
    return visibility == View.INVISIBLE
}

fun View.isNotInvisible(): Boolean {
    return visibility != View.INVISIBLE
}

fun View.isGone(): Boolean {
    return visibility == View.GONE
}

fun View.isNotGone(): Boolean {
    return visibility != View.GONE
}

fun View.isCompletelyVisible(): Boolean {
    return visibility == View.VISIBLE && alpha == 1.0F
}

fun View.enable() {
    isEnabled = true
}

fun View.disable() {
    isEnabled = false
}

fun View.grayedOut() {
    alpha = 0.3F
}

fun View.brightenUp() {
    alpha = 1.0F
}

fun View.setPaddingLeft(value: Int) {
    setPadding(value, paddingTop, paddingRight, paddingBottom)
}

fun View.setPaddingTop(value: Int) {
    setPadding(paddingLeft, value, paddingRight, paddingBottom)
}

fun View.setPaddingRight(value: Int) {
    setPadding(paddingLeft, paddingTop, value, paddingBottom)
}

fun View.setPaddingBottom(value: Int) {
    setPadding(paddingLeft, paddingTop, paddingRight, value)
}

fun View.measureBy(width: Int, widthSpec: Int, height: Int, heightSpec: Int) {
    measure(
        View.MeasureSpec.makeMeasureSpec(width, widthSpec),
        View.MeasureSpec.makeMeasureSpec(height, heightSpec)
    )
}

fun View.layoutByTopLeft(t: Int, l: Int) {
    layout(l, t, l + measuredWidth, t + measuredHeight)
}

fun View.layoutByTopRight(t: Int, r: Int) {
    layout(r - measuredWidth, t, r, t + measuredHeight)
}

fun View.layoutByBottomLeft(b: Int, l: Int) {
    layout(l, b - measuredHeight, l + measuredWidth, b)
}

fun View.layoutByBottomRight(b: Int, r: Int) {
    layout(r - measuredWidth, b - measuredHeight, r, b)
}

fun View.getDrawableWithoutTheme(@DrawableRes id: Int): Drawable {
    return resources.getDrawable(id, null)
}

fun View.getColorWithoutTheme(@ColorRes id: Int): Int {
    return if (AppUtil.reachMarshmallow())
        resources.getColor(id, null)
    else
        resources.getColor(id)
}

fun View.getFloatValue(@DimenRes id: Int): Float {
    val typedValue = TypedValue()
    resources.getValue(id, typedValue, true)
    return typedValue.float
}

fun View.getFontAttribute(@AttrRes id: Int): Typeface? {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(id, typedValue, true)
    return ResourcesCompat.getFont(context, typedValue.resourceId)
}

fun View.getDimension(@DimenRes id: Int): Int {
    return resources.getDimensionPixelSize(id)
}

fun View.getDimensionInFloat(@DimenRes id: Int): Float {
    return resources.getDimension(id)
}

fun View.getString(@StringRes id: Int, vararg formatArgs: Any): String {
    return if (formatArgs.isEmpty()) resources.getString(id)
    else resources.getString(id, *formatArgs)
}

fun View.getText(@StringRes id: Int): CharSequence {
    return resources.getText(id)
}

fun View.getLowerCaseString(@StringRes id: Int): String {
    return getString(id).toLowerCase(Locale.getDefault())
}

fun View.getUpperCaseString(@StringRes id: Int): String {
    return getString(id).toUpperCase(Locale.getDefault())
}

fun View.getSpannableString(@StringRes id: Int): SpannableString {
    val spannedString = SpannedString(getText(id))
    val spannableString = SpannableString(spannedString)
    val annotations = spannedString.getSpans(0, spannedString.length, Annotation::class.java)
    for (annotation in annotations) {
        when (annotation.key) {
            "font" -> when (annotation.value) {
                "bold" -> {
                    spannableString.setSpan(
                        StyleSpan(Typeface.BOLD),
                        spannedString.getSpanStart(annotation),
                        spannedString.getSpanEnd(annotation),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else -> {
                    // for future
                }
            }
            else -> {
                // for future
            }
        }
    }
    return spannableString
}

fun View.getInputMethodManager(): InputMethodManager {
    return context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}

fun View.setSelectableItemBackground(visibility: Boolean, borderless: Boolean = true) {
    when {
        !visibility -> background = null
        !borderless -> with(TypedValue()) {
            context.theme.resolveAttribute(
                R.attr.selectableItemBackground, this, true
            )
            setBackgroundResource(resourceId)
        }
        else -> with(TypedValue()) {
            context.theme.resolveAttribute(
                R.attr.selectableItemBackgroundBorderless, this, true
            )
            setBackgroundResource(resourceId)
        }
    }
}

class SafeClickListener(private val onSafeClick: (View?) -> Unit) : View.OnClickListener {

    constructor(listener: View.OnClickListener) : this(listener::onClick)

    companion object {
        private const val MIN_INTERVAL = 1000L
    }

    private var lastClick = 0L

    override fun onClick(v: View?) {
        val currentMillis = System.currentTimeMillis()
        if (currentMillis - lastClick < MIN_INTERVAL) {
            return
        }
        lastClick = currentMillis
        onSafeClick(v)
    }
}

fun View.setSafeOnClickListener(onSafeClick: (View?) -> Unit) {
    setOnClickListener(SafeClickListener(onSafeClick))
}

fun View.setSafeOnClickListener(listener: View.OnClickListener) {
    setOnClickListener(SafeClickListener(listener))
}

fun View.doOnGlobalLayoutChange(action: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            action()
        }
    })
}

// TextView

fun TextView.setTextColorResource(@ColorRes id: Int) {
    setTextColor(getColorWithoutTheme(id))
}

fun TextView.setTypefaceAttribute(@AttrRes id: Int) {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(id, typedValue, true)
    typeface = ResourcesCompat.getFont(context, typedValue.resourceId)
}

fun TextView.setHtmlText(source: String) {
    text = if (AppUtil.reachNougat())
        Html.fromHtml(source, Html.FROM_HTML_MODE_COMPACT)
    else
        Html.fromHtml(source)
}

fun TextView.setDrawableStartWithIntrinsicBounds(@DrawableRes drawableId: Int) {
    setDrawableStartWithIntrinsicBounds(context.getDrawable(drawableId))
}

fun TextView.setDrawableStartWithIntrinsicBounds(drawable: Drawable?) {
    compoundDrawables.let {
        setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, it[1], it[2], it[3])
    }
}

fun TextView.setDrawableTopWithIntrinsicBounds(@DrawableRes drawableId: Int) {
    setDrawableTopWithIntrinsicBounds(context.getDrawable(drawableId))
}

fun TextView.setDrawableTopWithIntrinsicBounds(drawable: Drawable?) {
    compoundDrawables.let {
        setCompoundDrawablesRelativeWithIntrinsicBounds(it[0], drawable, it[2], it[3])
    }
}

fun TextView.setDrawableEndWithIntrinsicBounds(@DrawableRes drawableId: Int) {
    setDrawableEndWithIntrinsicBounds(context.getDrawable(drawableId))
}

fun TextView.setDrawableEndWithIntrinsicBounds(drawable: Drawable?) {
    compoundDrawables.let {
        setCompoundDrawablesRelativeWithIntrinsicBounds(it[0], it[1], drawable, it[3])
    }
}

fun TextView.setDrawableBottomWithIntrinsicBounds(@DrawableRes drawableId: Int) {
    setDrawableBottomWithIntrinsicBounds(context.getDrawable(drawableId))
}

fun TextView.setDrawableBottomWithIntrinsicBounds(drawable: Drawable?) {
    compoundDrawables.let {
        setCompoundDrawablesRelativeWithIntrinsicBounds(it[0], it[1], it[2], drawable)
    }
}


// EditText

fun EditText.showSoftKeyboard(): Boolean {
    return getInputMethodManager().showSoftInput(this.apply { enable(); requestFocus() }, 0)
}

fun EditText.hideSoftKeyboard(): Boolean {
    return getInputMethodManager().hideSoftInputFromWindow(windowToken, 2)
}

fun EditText.setMaxLength(length: Int) {
    filters = arrayOf(InputFilter.LengthFilter(length))
}

fun EditText.clearTextGradient() {
    paint.shader = null
}

fun EditText.setTextGradient(colors: IntArray, positions: FloatArray) {
    paint.shader = LinearGradient(
        0f, 0f, width.toFloat(), 0f,
        colors, positions, Shader.TileMode.CLAMP
    )
}

fun EditText.doBeforeTextChanged(action: (CharSequence?, Int, Int, Int) -> Unit) {
    addTextWatcher(doBeforeChanged = action)
}

fun EditText.doOnTextChanged(action: (CharSequence?, Int, Int, Int) -> Unit) {
    addTextWatcher(doOnChanged = action)
}

fun EditText.doAfterTextChanged(action: (Editable?) -> Unit) {
    addTextWatcher(doAfterChanged = action)
}

private fun EditText.addTextWatcher(
    doBeforeChanged: (CharSequence?, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    doOnChanged: (CharSequence?, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    doAfterChanged: (Editable?) -> Unit = {}
) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?, start: Int, count: Int, after: Int
        ) = doBeforeChanged(s, start, count, after)

        override fun onTextChanged(
            s: CharSequence?, start: Int, before: Int, count: Int
        ) = doOnChanged(s, start, before, count)

        override fun afterTextChanged(s: Editable?) = doAfterChanged(s)
    })
}

fun EditText.doOnDoneAction(action: (TextView, KeyEvent?) -> Unit) {
    doOnActions { view, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_DONE) action(view, event)
    }
}

fun EditText.doOnSearchAction(action: (TextView, KeyEvent?) -> Unit) {
    doOnActions { view, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_SEARCH) action(view, event)
    }
}

fun EditText.doOnGoAction(action: (TextView, KeyEvent?) -> Unit) {
    doOnActions { view, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_GO) action(view, event)
    }
}

fun EditText.doOnNextAction(action: (TextView, KeyEvent?) -> Unit) {
    doOnActions { view, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_NEXT) action(view, event)
    }
}

fun EditText.doOnSendAction(action: (TextView, KeyEvent?) -> Unit) {
    doOnActions { view, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_SEND) action(view, event)
    }
}

fun EditText.doOnActions(action: (TextView, Int, KeyEvent?) -> Unit) {
    setOnEditorActionListener { view, actionId, event ->
        action(view, actionId, event); true
    }
}


// ViewGroup

val ScrollView.directChild: View?
    get() = getChildAt(0)

fun ViewGroup.inflate(@LayoutRes id: Int, attach: Boolean = false): View {
    return LayoutInflater.from(context).inflate(id, this, attach)
}