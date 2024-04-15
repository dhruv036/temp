package io.stempedia.pictoblox

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import io.stempedia.pictoblox.connectivity.PictoBloxWebLocale
import io.stempedia.pictoblox.databinding.ActivityGettingStartedBinding
import io.stempedia.pictoblox.home.Home2Activity
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.util.Locale


class GettingStartedActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityGettingStartedBinding
    private val adapter = GettingStartedAdapter()
    private lateinit var data: Array<GettingStartedRowVM>
    private lateinit var spManager: SPManager
    fun fetchLocal(){
        var code = spManager.pictobloxLocale
        var lang = code
        if (lang.contains("cn") || lang.contains("tw")) {
            lang = lang.substring(3,5)
        }
        var local = Locale(lang)
        Locale.setDefault(local)
        updateLocale(this,local)
    }
    private fun checkSystemLanguage(){
        val sysLang = Locale.getDefault().getLanguage()
        Log.e("lange","1 ${sysLang}")
        var lang = PictoBloxWebLocale.values().find {
            it.code.equals(sysLang)
        }
        lang = if (lang != null) lang else PictoBloxWebLocale.ENGLISH
        spManager.pictobloxLocale = lang.code
        var local = Locale(lang.code)
        updateLocale(this,local)
    }
    fun updateLocale(c: Context, localeToSwitchTo: Locale) {
        var context = c
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(localeToSwitchTo)
        } else {
            configuration.locale = localeToSwitchTo
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createConfigurationContext(configuration)
        }
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spManager = SPManager(this)

        if (spManager.isFirstTimeInstall) {
            // first time
            Log.d("lange","1")
            checkSystemLanguage()
        } else {
            // Not first time
            Log.d("lange","2")
            fetchLocal()
        }
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_getting_started)

        data = arrayOf(
            GettingStartedRowVM(ContextCompat.getColor(this, R.color.getting_started_slice_1_bg), R.drawable.getting_started_slice_1),
            GettingStartedRowVM(ContextCompat.getColor(this, R.color.getting_started_slice_2_bg), R.drawable.getting_started_slice_2),
            GettingStartedRowVM(ContextCompat.getColor(this, R.color.getting_started_slice_3_bg), R.drawable.getting_started_slice_3),
            GettingStartedRowVM(ContextCompat.getColor(this, R.color.getting_started_slice_4_bg), R.drawable.getting_started_slice_4),
            GettingStartedRowVM(ContextCompat.getColor(this, R.color.getting_started_slice_5_bg), R.drawable.getting_started_slice_5),
            GettingStartedRowVM(ContextCompat.getColor(this, R.color.getting_started_slice_6_bg), R.drawable.getting_started_slice_6),
            GettingStartedRowVM(ContextCompat.getColor(this, R.color.getting_started_slice_7_bg), R.drawable.getting_started_slice_7)
        )

        mBinding.vp2GettingStarted.registerOnPageChangeCallback(vpListener)
        mBinding.vp2GettingStarted.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        mBinding.vp2GettingStarted.adapter = adapter

        mBinding.vp2GettingStartedIndicator
            .setSliderColor(
                ContextCompat.getColor(this, R.color.getting_started_indicator_normal),
                ContextCompat.getColor(this, R.color.getting_started_indicator_selected)
            )
            .setSliderWidth(resources.getDimension(R.dimen.dp_8))
            //.setSliderHeight(resources.getDimension(R.dimen.dp_5))
            .setSlideMode(IndicatorSlideMode.WORM)
            .setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setupWithViewPager(mBinding.vp2GettingStarted)

        mBinding.textView63.setOnClickListener {
            startActivity(Intent(this@GettingStartedActivity, Home2Activity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.vp2GettingStarted.unregisterOnPageChangeCallback(vpListener)
    }

    private val vpListener = object : ViewPager2.OnPageChangeCallback() {
        private val argbEvaluator = ArgbEvaluator()

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            if (position < adapter.itemCount - 1) {
                mBinding.root.setBackgroundColor(argbEvaluator.evaluate(positionOffset, data[position].color, data[position + 1].color) as Int)
            } else {
                mBinding.root.setBackgroundColor(data.last().color)
            }
        }

        override fun onPageSelected(position: Int) {

            if (position == data.size - 1) {

                val cx = (mBinding.textView63.width / 2)
                val cy = (mBinding.textView63.height / 2)

                val animator = ViewAnimationUtils.createCircularReveal(mBinding.textView63, cx, cy, 0f, mBinding.textView63.width.toFloat())
                animator.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationStart(animation: Animator) {
                        mBinding.textView63.visibility = View.VISIBLE
                    }
                })

                animator.startDelay = 300
                animator.start()

            } else if (mBinding.textView63.visibility == View.VISIBLE) {

                val cx = (mBinding.textView63.width / 2)
                val cy = (mBinding.textView63.height / 2)

                val animator = ViewAnimationUtils.createCircularReveal(mBinding.textView63, cx, cy, mBinding.textView63.width.toFloat(), 0f)
                animator.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        mBinding.textView63.visibility = View.INVISIBLE
                    }

                })

                animator.start()
            }
        }
    }

    inner class GettingStartedAdapter : RecyclerView.Adapter<GettingStartedVH>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GettingStartedVH {
            val view = layoutInflater.inflate(R.layout.row_getting_started, parent, false)
            return GettingStartedVH((view))
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: GettingStartedVH, position: Int) {
            holder.setImage(data[position].imageResourceId)
        }
    }

    inner class GettingStartedVH(v: View) : RecyclerView.ViewHolder(v) {
        private val imageView = v.findViewById<ImageView>(R.id.imageView38)

        fun setImage(resource: Int) {
            imageView.setImageResource(resource)
        }
    }


    class GettingStartedRowVM(val color: Int, val imageResourceId: Int)
}

