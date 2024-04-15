package io.stempedia.pictoblox.settings

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.PictoBloxWebLocale
import io.stempedia.pictoblox.databinding.SettingsActivityBinding
import io.stempedia.pictoblox.util.PictoBloxAnalyticsEventLogger
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.RegisterService
import io.stempedia.pictoblox.util.SPManager
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream


class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferencesManager: SPManager
    private lateinit var mbinding: SettingsActivityBinding
    var dialogBuilder : AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferencesManager = SPManager(this)
        fetchLocal()
        mbinding = DataBindingUtil.setContentView(this, io.stempedia.pictoblox.R.layout.settings_activity)
        sharedPreferencesManager = SPManager(this)

        setSupportActionBar(mbinding.tbSettings)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        mbinding.tbSettings.setNavigationOnClickListener { finish() }


//        var code = sharedPreferencesManager.pictobloxLocale
//        Log.d("lange","4 $code")
//
//        if (code.length > 2 && code.contains("cn") || code.contains("tw")) {
//            code = code.substring(3,5)
//        }
//
//        var local = Locale(code)
//        Locale.setDefault(local)
//        updateLocale(this,local)

        mbinding.textView3.text = resources.getString(R.string.settings)
        mbinding.textView87.text = resources.getString(R.string.version)
        mbinding.textView88.text = resources.getString(R.string.date_release)
        mbinding.tvLocal.text = resources.getString(R.string.code_area_language)
        mbinding.switchEexternalEnabled.text = resources.getString(R.string.enable_external)
        mbinding.textView91.text = resources.getString(R.string.experimental_feature)
        mbinding.tvSource.text = resources.getString(R.string.internal)
        mbinding.btChoose.text = resources.getString(R.string.choose)


        mbinding.btChoose.setOnClickListener {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                //type = "*/*"
                type = "application/zip"
            }

            startActivityForResult(intent, 121)
        }

        setDetail()

        mbinding.switchEexternalEnabled.setOnClickListener {
            sharedPreferencesManager.isExternalPictoBloxEnabled = !sharedPreferencesManager.isExternalPictoBloxEnabled
            setDetail()
        }

        mbinding.textView89.text = BuildConfig.VERSION_NAME
        mbinding.textView90.text = BuildConfig.BUILD_DATE

        PictoBloxWebLocale.values().find { it.code == sharedPreferencesManager.pictobloxLocale }?.also {
            mbinding.tvLocaleValue.text = it.localisedName
        }

        mbinding.tvLocal.setOnClickListener { showLanguageOptions()}
        mbinding.tvLocaleValue.setOnClickListener { showLanguageOptions()}
    }
    fun fetchLocal() {
        var code = sharedPreferencesManager.pictobloxLocale
        var lang = code
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


    private fun setDetail() {
        if (sharedPreferencesManager.isExternalPictoBloxEnabled) {
            mbinding.switchEexternalEnabled.isChecked = true
            mbinding.btChoose.isEnabled = true

            mbinding.tvSource.text = if (sharedPreferencesManager.externalPictoBloxDetail.isEmpty()) {
                resources.getString(R.string.not_set)

            } else {
                sharedPreferencesManager.externalPictoBloxDetail
            }

        } else {
            mbinding.switchEexternalEnabled.isChecked = false
            mbinding.btChoose.isEnabled = false
            mbinding.tvSource.text = resources.getString(R.string.internal)
        }
    }

    private fun showLanguageOptions() {
        val optionArray = PictoBloxWebLocale.values().map { it.localisedName }.toTypedArray()
        val selectedLocalPos = PictoBloxWebLocale.values().indexOfFirst { it.code == sharedPreferencesManager.pictobloxLocale }
        dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(optionArray, selectedLocalPos) { dialog, which ->
                sharedPreferencesManager.pictobloxLocale = PictoBloxWebLocale.values()[which].code
                mbinding.tvLocaleValue.text = PictoBloxWebLocale.values()[which].localisedName
                 /** Service to RESTART APP
                     */
                val intent = Intent(this,RegisterService::class.java)
                startService(intent)
                finish()
                finishAffinity()
                PictoBloxAnalyticsEventLogger.getInstance().setLanguageSelected(PictoBloxWebLocale.values()[which].code)
            }.create()
        dialogBuilder?.show()
    }

    var progreDialog: ProgressDialog? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 121) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.also {
                    progreDialog = ProgressDialog(this@SettingsActivity)
                    progreDialog?.show()
                    loadExternalFile(it)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(object : DisposableSingleObserver<Pair<String, Long>>() {
                            override fun onSuccess(t: Pair<String, Long>) {

                                val sizeInMB = t.second / 1_000_000

                                sharedPreferencesManager.externalPictoBloxDetail = "${t.first} ($sizeInMB MB)"
                                setDetail()

                                progreDialog?.dismiss()
                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                progreDialog?.dismiss()
                            }


                        })
                }
            }
        }
    }

    override fun onDestroy() {
        if(dialogBuilder != null){
            dialogBuilder?.dismiss()
        }
        super.onDestroy()
    }

    private fun loadExternalFile(uri: Uri): Single<Pair<String, Long>> {

        return Single.create {
            val type = contentResolver.getType(uri)
            PictobloxLogger.getInstance().logd("Mime type : $type")

            try {
                val pair = getFileName(uri)
                val pictoDir = File(cacheDir, "pictoDir")
                if (!pictoDir.exists()) {
                    pictoDir.mkdirs()
                }
                val file = File(pictoDir, "pictoBlox.zip")
                copyToCachedFile(uri, file)
                unzipPictoBlox()
                it.onSuccess(Pair(pair.first!!, pair.second))

            } catch (e: Exception) {
                it.onError(e)
            }
        }
    }
    //cache/pictoDir
    private fun unzipPictoBlox() {
        val pictoDir = File(cacheDir, "pictoDir")
        val inputStream = ZipInputStream(File(pictoDir, "pictoBlox.zip").inputStream())
        var zipEntry = inputStream.nextEntry
        val buffer = ByteArray(1024)
        val pictoBloxUnzipDir = File(pictoDir, "pictoBloxUnzipped")
        if (!pictoBloxUnzipDir.exists()) {
            pictoBloxUnzipDir.mkdirs()
        }
        while (zipEntry != null) {
            PictobloxLogger.getInstance().logd("Unzip____ ${zipEntry.name}")
            val entry = File(pictoBloxUnzipDir, zipEntry.name)
//            ensureZipPathSafety(entry, zipEntry.name)
            if (zipEntry.isDirectory) {
                val dir = File(pictoBloxUnzipDir, zipEntry.name)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            } else {
                entry.parentFile?.also {
                    if (!it.exists())
                        it.mkdirs()
                }
                BufferedOutputStream(FileOutputStream(entry)).use {
                    var read = inputStream.read(buffer)
                    while (read != -1) {
                        it.write(buffer, 0, read)
                        read = inputStream.read(buffer)
                    }
                }
            }
            inputStream.closeEntry()
            zipEntry = inputStream.nextEntry
        }
        inputStream.close()
    }

    private fun getFileName(uri: Uri): Pair<String?, Long> {

        var displayName: String? = null
        var size = 0L

        val cursor = contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )

        cursor?.apply {

            if (moveToFirst()) {
                displayName = getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))

                PictobloxLogger.getInstance().logd("Display name : $displayName")

                val sizeIndex: Int = getColumnIndex(OpenableColumns.SIZE)

                size = if (!isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    getString(sizeIndex).toLong()
                } else {
                    -1L
                }
                PictobloxLogger.getInstance().logd("Size : $size")
            }
            close()
        }
        return Pair(displayName, size)

    }
    @Throws(java.lang.Exception::class)
    private fun ensureZipPathSafety(outputFile: File, destDirectory: String) {
        val destDirCanonicalPath = File(destDirectory).canonicalPath
        val outputFilecanonicalPath = outputFile.canonicalPath
        if (!outputFilecanonicalPath.startsWith(destDirCanonicalPath)) {
            throw java.lang.Exception(
                java.lang.String.format(
                    "Found Zip Path Traversal Vulnerability",
                )
            )
        }
    }

    private fun copyToCachedFile(uri: Uri, file: File) {

        contentResolver.openInputStream(uri)?.use {

            if (file.exists()) {
                file.delete()
            }

            FileOutputStream(file).use { out ->
                run {

                    val array = ByteArray(256)

                    var length = it.read(array)

                    while (length > 0) {
                        out.write(array, 0, length)
                        length = it.read(array)
                    }
                }
            }
        }
    }


}