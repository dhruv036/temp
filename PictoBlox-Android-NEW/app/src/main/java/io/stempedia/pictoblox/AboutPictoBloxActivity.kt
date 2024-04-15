package io.stempedia.pictoblox

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import io.stempedia.pictoblox.databinding.ActivityAboutPictoBloxBinding
import io.stempedia.pictoblox.util.SPManager
import java.util.Locale

class AboutPictoBloxActivity : AppCompatActivity() {
    lateinit var binding : ActivityAboutPictoBloxBinding
    var link: Uri? = null
    private lateinit var spManager: SPManager
    private  val TAG = "AboutPictoBloxActivity"

    val clickSpan = object : ClickableSpan(){
        override fun onClick(widget: View) {
            if (widget == binding.tv2)
            {
                link = Uri.parse("https://thestempedia.com")
            }else if (widget == binding.tv4){
                link = Uri.parse("https://thestempedia.com/product/pictoblox/release-notes/")
            }else if (widget == binding.tv13){
                link = Uri.parse("https://thestempedia.com/privacy-policy/")
            }else if (widget == binding.tv8){
                link =Uri.parse("https://github.com/LLK")
            }else if (widget == binding.tv15){
                link =Uri.parse("https://thestempedia.com/contact/")
            }
            Intent(Intent.ACTION_VIEW,link).also {
                startActivity(it)
            }
        }
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

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        var code = spManager.pictobloxLocale
        code = if (code.contains("tw") || code.contains("cn")) code.substring(3,5) else code
        updateLocale(this, Locale(code))
    }
    fun fetchLocal() {
        var code = spManager.pictobloxLocale
        var lang = code
        Log.d(TAG, "fetchLocal: $code")
        var local = Locale(lang)
        if (lang.contains("tw",true) ) {
            local  = Locale.TRADITIONAL_CHINESE
        }
        if (lang.contains("cn",true) ) {
            local  = Locale.SIMPLIFIED_CHINESE
        }
        Locale.setDefault(local)
        updateLocale(this, local)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: ")
        spManager = SPManager(this)
        fetchLocal()
        binding = ActivityAboutPictoBloxBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var sp1: SpannableString
        setSupportActionBar(binding.tbHome)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.tbHome.setNavigationOnClickListener {
            finish()
        }

        binding.tv2.apply {
            sp1 = SpannableString("STEMpedia")
            sp1.setSpan(clickSpan,0 ,sp1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(sp1)
            movementMethod = LinkMovementMethod.getInstance()
        }
        binding.tv4.apply {
            sp1 = SpannableString(getString(R.string.release_notes))
            append(BuildConfig.VERSION_NAME+" ")
            sp1.setSpan(clickSpan,0 ,sp1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(sp1)
            movementMethod = LinkMovementMethod.getInstance()
        }
        binding.tv13.apply {
            sp1 = SpannableString(getString(R.string.here))
            sp1.setSpan(clickSpan,0 ,sp1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(sp1)
            movementMethod = LinkMovementMethod.getInstance()
        }
        binding.tv15.apply {
            sp1 = SpannableString(getString(R.string.here))
            sp1.setSpan(clickSpan,0 ,sp1.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(sp1)
            append("\n ${getString(R.string.love_india)}")
            movementMethod = LinkMovementMethod.getInstance()
        }
        var btPoint1 = listOf<String>(getString(R.string.body_l1), getString(R.string.body_l2), getString(R.string.body_l3))

        binding.tv8.apply {
            append(convertToBulletList1(btPoint1,getString(R.string.following_repositories)))
            movementMethod = LinkMovementMethod.getInstance()
        }
        var btPoint2 = listOf<String>(getString(R.string.body_l4), getString(R.string.body_l5), getString(R.string.body_l6), getString(R.string.body_l7), getString(R.string.body_l8), getString(R.string.body_l9), getString(R.string.body_l10))
        binding.tv9.apply {

            append(convertToBulletList2(btPoint2))
        }
        binding.closeBt.setOnClickListener {
            finish()
        }
    }
    fun convertToBulletList1(stringList: List<String>,temp :String): CharSequence {
        val sp2 = SpannableStringBuilder("$temp ${getString(R.string.here)} :\n")
        sp2.setSpan(clickSpan , sp2.length-7, sp2.length-3, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        sp2.setSpan(StyleSpan(Typeface.BOLD), 0, sp2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        stringList.forEachIndexed { index, text ->
            val line: CharSequence = text + if (index < stringList.size - 1) "\n" else ""
            val spannable: Spannable = SpannableString(line)
            spannable.setSpan(
                BulletSpan(15, Color.DKGRAY),
                0,
                spannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            sp2.append(spannable)
        }
        return sp2
    }
    fun convertToBulletList2(stringList: List<String>): CharSequence {
        val sp2 = SpannableStringBuilder("${getString(R.string.extension_credits)} \n")
        sp2.setSpan(StyleSpan(Typeface.BOLD), 0, sp2.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        stringList.forEachIndexed { index, text ->
            val line: CharSequence = text + if (index < stringList.size - 1) "\n" else ""
            val spannable: Spannable = SpannableString(line)
            spannable.setSpan(
                BulletSpan(15, Color.DKGRAY),
                0,
                spannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            sp2.append(spannable)
        }
        return sp2
    }
}