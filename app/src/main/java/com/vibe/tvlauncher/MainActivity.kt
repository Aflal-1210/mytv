package com.vibe.tvlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.sin

/**
 * A marionette bar across the top, with every app hanging from it on a string.
 * Tiles sway gently; the focused tile gets "pulled" down and its string goes bright.
 */
class StringsLayout(ctx: Context) : FrameLayout(ctx) {
    private val d = resources.displayMetrics.density
    val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
    private val barY = 36 * d
    private var phase = 0f

    private val gold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD4AF37.toInt() }
    private val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * d; color = 0xAAD4AF37.toInt()
    }
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 3.5f * d; color = 0xFFFFE08A.toInt()
    }

    init {
        clipChildren = false
        clipToPadding = false
        setPadding((60 * d).toInt(), 0, (60 * d).toInt(), (50 * d).toInt())
        addView(row, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (130 * d).toInt()
        })
    }

    // Static gentle tilt, set once per layout. No endless animation, so the weak box isn't redrawing all the time.
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        for (i in 0 until row.childCount) {
            val v = row.getChildAt(i)
            v.pivotX = v.width / 2f
            v.pivotY = 0f
            if (!v.isFocused) v.rotation = baseTilt(i)
        }
    }

    fun baseTilt(i: Int): Float = ((i % 3) - 1) * 1.5f

    override fun dispatchDraw(c: Canvas) {
        // the control bar
        c.drawRoundRect(RectF(8 * d, barY - 5 * d, width - 8 * d, barY + 5 * d), 5 * d, 5 * d, gold)
        // strings
        for (i in 0 until row.childCount) {
            val v = row.getChildAt(i)
            val cx = row.left + v.left + v.width / 2f
            val endY = row.top + v.top + v.translationY
            val sway = sin(phase + i * 0.9f) * 6 * d
            val path = Path().apply {
                moveTo(cx, barY)
                quadTo(cx + sway, (barY + endY) / 2f, cx, endY)
            }
            c.drawPath(path, if (v.isFocused) focusPaint else stringPaint)
            c.drawCircle(cx, barY, 6 * d, gold)
            c.drawCircle(cx, endY, 5 * d, gold)
        }
        super.dispatchDraw(c)
    }
}

class MainActivity : Activity() {

    private data class Tile(
        val label: String,
        val emoji: String,
        val color: Int,
        val pkg: String? = null,   // native TV app package
        val url: String = "",      // web fallback
        val browser: Boolean = false,
        val tvUa: Boolean = false,
        val dummy: Boolean = false // just for show
    )

    private val tiles = listOf(
        // the real ones
        Tile("YouTube", "\uD83D\uDCFA", 0xFFB3121C.toInt(), "com.google.android.youtube.tv", "https://www.youtube.com/tv", tvUa = true),
        Tile("YouTube Music", "\uD83C\uDFB5", 0xFFB02A5B.toInt(), "com.google.android.youtube.tvmusic", "https://music.youtube.com"),
        Tile("Spotify", "\uD83C\uDFA7", 0xFF1A9E4A.toInt(), "com.spotify.tv.android", "https://open.spotify.com"),
        Tile("Web Browser", "\uD83C\uDF10", 0xFF2F6FDE.toInt(), null, "https://www.google.com", browser = true),
        // puppets, just for show
        Tile("Movies", "\uD83C\uDFAC", 0xFF16372A.toInt(), dummy = true),
        Tile("Games", "\uD83C\uDFAE", 0xFF16372A.toInt(), dummy = true),
        Tile("Radio", "\uD83D\uDCFB", 0xFF16372A.toInt(), dummy = true),
        Tile("Photos", "\uD83D\uDDBC", 0xFF16372A.toInt(), dummy = true),
        Tile("Weather", "\u26C5", 0xFF16372A.toInt(), dummy = true),
        Tile("Kids", "\uD83E\uDDF8", 0xFF16372A.toInt(), dummy = true)
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF030D08.toInt(), 0xFF0B2B1D.toInt(), 0xFF04120C.toInt())
            )
        }

        val title = TextView(this).apply {
            text = "PULLING THE STRINGS"
            setTextColor(0xFFD4AF37.toInt())
            textSize = 16f
            letterSpacing = 0.3f
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.TOP or Gravity.START; setMargins(dp(40), dp(24), 0, 0) })

        val strings = StringsLayout(this)
        val views = tiles.map { makeTile(it, strings) }
        views.forEach { strings.row.addView(it) }

        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            addView(strings)
        }
        root.addView(scroll, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
        ))

        setContentView(root)
        views.first().requestFocus()
    }

    private fun makeTile(t: Tile, strings: StringsLayout): LinearLayout {
        val bg = GradientDrawable().apply {
            cornerRadius = dp(20).toFloat()
            setColor(t.color)
            setStroke(dp(1), if (t.dummy) 0xFF3F6B55.toInt() else Color.TRANSPARENT)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(dp(150), dp(190)).apply {
                setMargins(dp(14), 0, dp(14), 0)
            }
            addView(TextView(context).apply {
                text = t.emoji; textSize = 44f; gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = t.label
                setTextColor(if (t.dummy) 0xFFB8D8C6.toInt() else Color.WHITE)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
            })
            setOnFocusChangeListener { v, focused ->
                bg.setStroke(if (focused) dp(5) else dp(1),
                    if (focused) 0xFFFFE08A.toInt() else if (t.dummy) 0xFF3F6B55.toInt() else Color.TRANSPARENT)
                val s = if (focused) 1.08f else 1f
                val idx = strings.row.indexOfChild(v)
                v.animate().translationY(if (focused) dp(30).toFloat() else 0f)
                    .rotation(if (focused) 0f else strings.baseTilt(idx))
                    .scaleX(s).scaleY(s).setDuration(150)
                    .setUpdateListener { strings.invalidate() }
                    .start()
            }
            setOnClickListener {
                if (t.dummy) {
                    Toast.makeText(this@MainActivity, "Just a puppet - for show only \uD83C\uDFAD", Toast.LENGTH_SHORT).show()
                } else launch(t)
            }
        }
    }

    private fun launch(t: Tile) {
        t.pkg?.let { p ->
            val i = packageManager.getLeanbackLaunchIntentForPackage(p)
                ?: packageManager.getLaunchIntentForPackage(p)
            if (i != null) {
                startActivity(i)
                return
            }
            Toast.makeText(this, "${t.label} app not installed - opening web version", Toast.LENGTH_LONG).show()
        }
        startActivity(
            Intent(this, WebActivity::class.java)
                .putExtra(WebActivity.EXTRA_URL, t.url)
                .putExtra(WebActivity.EXTRA_BROWSER, t.browser)
                .putExtra(WebActivity.EXTRA_TV_UA, t.tvUa)
        )
    }
}
