package com.han.takeit

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.han.takeit.databinding.DialogColorPickerBinding
import com.han.takeit.db.Tag

class ColorPickerDialog(
    context: Context,
    private val currentColor: String = "#6200EE",
    private val onColorSelected: (String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogColorPickerBinding
    private var selectedColor = currentColor
    private var currentHue = 240f
    private var currentSaturation = 1f
    private var currentBrightness = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogColorPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置对话框属性
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupViews()
    }

    private fun setupViews() {
        // 设置预设颜色网格
        val colorsAdapter = PresetColorsAdapter(Tag.PRESET_COLORS) { color ->
            selectColor(color)
        }
        binding.recyclerPresetColors.layoutManager = GridLayoutManager(context, 5)
        binding.recyclerPresetColors.adapter = colorsAdapter

        // 初始化当前颜色预览
        updateCurrentColorPreview()

        // 设置滑块监听器
        setupColorSliders()
        
        // 设置颜色轮监听器
        setupColorWheel()
        
        // 初始化滑块位置
        updateSlidersFromColor(selectedColor)
        
        // 设置按钮监听
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            onColorSelected(selectedColor)
            dismiss()
        }
    }
    
    private fun selectColor(color: String) {
        selectedColor = color
        updateSlidersFromColor(color)
        updateCurrentColorPreview()
    }
    
    private fun updateSlidersFromColor(colorString: String) {
        try {
            val color = Color.parseColor(colorString)
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            
            currentHue = hsv[0]
            currentSaturation = hsv[1]
            currentBrightness = hsv[2]
            
            // 更新滑块位置（不触发监听器）
            binding.seekbarHue.progress = currentHue.toInt()
            binding.seekbarSaturation.progress = (currentSaturation * 100).toInt()
            binding.seekbarBrightness.progress = (currentBrightness * 100).toInt()
            
            // 更新颜色轮显示
            binding.colorWheel.setColor(currentHue, currentSaturation, currentBrightness)
        } catch (e: Exception) {
            // 如果颜色解析失败，使用默认值
        }
    }

    private fun setupColorSliders() {
        // 色相滑块
        binding.seekbarHue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentHue = progress.toFloat()
                    updateColorFromHSV()
                    // 更新颜色轮显示
                    binding.colorWheel.setColor(currentHue, currentSaturation, currentBrightness)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 饱和度滑块
        binding.seekbarSaturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentSaturation = progress / 100f
                    updateColorFromHSV()
                    // 更新颜色轮显示
                    binding.colorWheel.setColor(currentHue, currentSaturation, currentBrightness)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 亮度滑块
        binding.seekbarBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentBrightness = progress / 100f
                    updateColorFromHSV()
                    // 更新颜色轮显示
                    binding.colorWheel.setColor(currentHue, currentSaturation, currentBrightness)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun setupColorWheel() {
        binding.colorWheel.setColor(currentHue, currentSaturation, currentBrightness)
        binding.colorWheel.setOnColorChangeListener { hue, saturation, brightness ->
            currentHue = hue
            currentSaturation = saturation
            currentBrightness = brightness
            
            // 更新滑块位置（不触发监听器）
            binding.seekbarHue.progress = currentHue.toInt()
            binding.seekbarSaturation.progress = (currentSaturation * 100).toInt()
            binding.seekbarBrightness.progress = (currentBrightness * 100).toInt()
            
            updateColorFromHSV()
        }
    }
    
    private fun updateColorFromHSV() {
        val hsv = floatArrayOf(currentHue, currentSaturation, currentBrightness)
        val color = Color.HSVToColor(hsv)
        selectedColor = String.format("#%06X", 0xFFFFFF and color)
        updateCurrentColorPreview()
    }

    private fun updateCurrentColorPreview() {
        val drawable = binding.viewCurrentColor.background as? GradientDrawable
        drawable?.setColor(Color.parseColor(selectedColor))
        binding.textColorCode.text = selectedColor
    }

    // 预设颜色适配器
    private class PresetColorsAdapter(
        private val colors: List<String>,
        private val onColorClick: (String) -> Unit
    ) : RecyclerView.Adapter<PresetColorsAdapter.ColorViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_preset_color, parent, false)
            return ColorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
            val color = colors[position]
            holder.bind(color, onColorClick)
        }

        override fun getItemCount() = colors.size

        class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorCircle: View = itemView.findViewById(R.id.color_circle)
            
            fun bind(color: String, onColorClick: (String) -> Unit) {
                try {
                    val colorInt = Color.parseColor(color)
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(colorInt)
                        setStroke(4, Color.WHITE)
                    }
                    colorCircle.background = drawable
                    itemView.setOnClickListener { onColorClick(color) }
                } catch (e: Exception) {
                    // 处理颜色解析错误
                }
            }
        }
    }
}