package io.stempedia.pictoblox.examples

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import io.stempedia.pictoblox.R


class ExampleDescriptionActivity : AppCompatActivity() {
    private val videoView by lazy {
        findViewById<View>(R.id.vvDescription) as VideoView
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example_description)
        //videoView.setVideoPath("android.resource://" + packageName + "/" + R.raw.sample_video)
        videoView.start()
        videoView.setOnCompletionListener {
            videoView.start()
        }

        val sampleBulletPoints = arrayListOf<String>("Easy","Animation","Bluetooth Required")
        val arrayAdapter = ArrayAdapter<String>(this,R.layout.list_view_examples_description,sampleBulletPoints)
        //lvBulletPoints.adapter = arrayAdapter

/*        btnEnter.setOnClickListener {

            val resultIntent = Intent()
            resultIntent.putExtra("shouldDownloadLatestSb3", intent.getBooleanExtra("shouldDownloadLatestSb3", false))
            resultIntent.putExtra("id", intent.getStringExtra("id"))
            resultIntent.putExtra("name", intent.getStringExtra("name"))
            resultIntent.putExtra("latestVersion", intent.getIntExtra("latestVersion", 0))
            setResult(RESULT_OK, resultIntent)
            videoView.stopPlayback()
            finish()
        }*/

    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(RESULT_CANCELED)
        videoView.stopPlayback()
        finish()
    }
}