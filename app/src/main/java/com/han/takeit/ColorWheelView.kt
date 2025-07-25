package com.han.takeit

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var innerRadius = 0f
    
    private var currentHue = 240f
    private var currentSaturation = 1f
    private var currentBrightness = 0.5f
    
    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val saturationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var onColorChangeListener: ((Float, Float, Float) -> Unit)? = null
    
    init {
        setupPaints()
    }
    
    private fun setupPaints() {
        selectorPaint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        
        centerPaint.apply {
            style = Paint.Style.FILL
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f - 20f
        innerRadius = radius * 0.3f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制色相环
        drawHueWheel(canvas)
        
        // 绘制饱和度/亮度区域
        drawSaturationBrightnessArea(canvas)
        
        // 绘制选择器
        drawSelector(canvas)
    }
    
    private fun drawHueWheel(canvas: Canvas) {
        val hueColors = IntArray(360)
        for (i in 0 until 360) {
            hueColors[i] = Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f))
        }
        
        val shader = SweepGradient(centerX, centerY, hueColors, null)
        huePaint.shader = shader
        
        canvas.drawCircle(centerX, centerY, radius, huePaint)
        
        // 绘制内圆遮罩
        centerPaint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, innerRadius, centerPaint)
    }
    
    private fun drawSaturationBrightnessArea(canvas: Canvas) {
        // 绘制饱和度渐变背景
        val saturationColors = IntArray(100)
        for (i in 0 until 100) {
            val sat = i / 100f
            saturationColors[i] = Color.HSVToColor(floatArrayOf(currentHue, sat, 1f))
        }
        
        // 创建径向渐变
        val saturationShader = RadialGradient(
            centerX, centerY, innerRadius - 4f,
            Color.WHITE, Color.HSVToColor(floatArrayOf(currentHue, 1f, 1f)),
            Shader.TileMode.CLAMP
        )
        saturationPaint.shader = saturationShader
        canvas.drawCircle(centerX, centerY, innerRadius - 4f, saturationPaint)
        
        // 根据亮度添加遮罩
        val brightnessAlpha = (255 * (1f - currentBrightness)).toInt()
        centerPaint.color = Color.argb(brightnessAlpha, 0, 0, 0)
        centerPaint.shader = null
        canvas.drawCircle(centerX, centerY, innerRadius - 4f, centerPaint)
    }
    
    private fun drawSelector(canvas: Canvas) {
        // 在色相环上绘制色相选择器
        val hueAngle = Math.toRadians(currentHue.toDouble())
        val hueX = centerX + (radius - 10f) * cos(hueAngle).toFloat()
        val hueY = centerY + (radius - 10f) * sin(hueAngle).toFloat()
        
        canvas.drawCircle(hueX, hueY, 8f, selectorPaint)
        
        // 在中心区域绘制饱和度/亮度选择器
        val satRadius = currentSaturation * (innerRadius - 15f)
        val brightAngle = currentBrightness * 2 * PI
        val satX = centerX + satRadius * cos(brightAngle).toFloat()
        val satY = centerY + satRadius * sin(brightAngle).toFloat()
        
        selectorPaint.color = Color.BLACK
        canvas.drawCircle(satX, satY, 6f, selectorPaint)
        selectorPaint.color = Color.WHITE
        canvas.drawCircle(satX, satY, 4f, selectorPaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                val dx = x - centerX
                val dy = y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                
                if (distance > innerRadius && distance <= radius) {
                    // 在色相环区域
                    var angle = atan2(dy, dx) * 180 / PI
                    if (angle < 0) angle += 360
                    currentHue = angle.toFloat()
                    
                    onColorChangeListener?.invoke(currentHue, currentSaturation, currentBrightness)
                    invalidate()
                    return true
                } else if (distance <= innerRadius) {
                    // 在饱和度/亮度区域
                    val saturation = min(1f, distance / (innerRadius - 15f))
                    var brightness = atan2(dy, dx) * 180 / PI
                    if (brightness < 0) brightness += 360
                    brightness = brightness / 360
                    
                    currentSaturation = saturation
                    currentBrightness = brightness.toFloat()
                    
                    onColorChangeListener?.invoke(currentHue, currentSaturation, currentBrightness)
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
    
    fun setColor(hue: Float, saturation: Float, brightness: Float) {
        currentHue = hue
        currentSaturation = saturation
        currentBrightness = brightness
        invalidate()
    }
    
    fun setOnColorChangeListener(listener: (Float, Float, Float) -> Unit) {
        onColorChangeListener = listener
    }
}