package io.stempedia.pictoblox.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityShareForMIUIBinding
import java.io.File

const val MIUI_SHARE_ARRAY = "miui_share"

class ShareForMIUIActivity : AppCompatActivity() {
    private lateinit var mBinding:ActivityShareForMIUIBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_share_for_m_i_u_i)
        setSupportActionBar(mBinding.tbShareMiui)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.tbShareMiui.setNavigationOnClickListener { finish() }

        mBinding.textView95.setOnClickListener { this.finish() }

        intent.getStringArrayListExtra(MIUI_SHARE_ARRAY)?.also { uris ->
            shareMultipleFiles(uris.map { File(it) })

        } ?: kotlin.run {
            finish()
        }
    }

    private fun shareMultipleFiles(list: List<File>) {

        val uris = ArrayList<Uri>()
        try {
            list.forEach {
                val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID , it)
                uris.add(uri)
            }
        }catch (e: Exception){
            Toast.makeText(this,getString(R.string.try_again),Toast.LENGTH_SHORT).show()
        }


        Intent().apply {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            action = Intent.ACTION_SEND_MULTIPLE
            putExtra(Intent.EXTRA_SUBJECT, "PictoBlox Project(s)")
            type = "application/octet-stream"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)

        }.also {
            it.resolveActivityInfo(this.packageManager, 0)?.apply {
                startActivity(Intent.createChooser(it, "Share ${list.size} PictoBlox project(s)"))
            }
        }

    }
}