package io.stempedia.pictoblox.QR

import android.R.attr.height
import android.R.attr.width
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.ViewfinderView
import com.journeyapps.barcodescanner.camera.CameraSettings
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.home.Home2Activity
import kotlinx.coroutines.delay


class CustomScanner: AppCompatActivity(){
    lateinit var capture: CaptureManager
    lateinit var barcodeScannerView: DecoratedBarcodeView
    lateinit var switchFlashlightButton: Button
    lateinit var viewfinderView: ViewfinderView
    lateinit var errorText : TextView

    val getImage = registerForActivityResult(ActivityResultContracts.GetContent()){ uri->
        uri?.let {
            if(Uri.parse(uri?.path) != null){
                try {
                    val bitmap  = MediaStore.Images.Media.getBitmap(contentResolver,uri)
                    scanImage(bitmap)
                }catch (e: Exception){
                    e.localizedMessage?.let {
                        Toast.makeText(this,it,Toast.LENGTH_SHORT).show()
                    }
                    Log.e("Bitmap","can't generate exception")
                }
            }
        }
    }

    fun scanImage(bMap : Bitmap){
        var contents: String? = null

        val intArray = IntArray(bMap.getWidth() * bMap.getHeight())
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight())

        val source: LuminanceSource =
            RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        val reader: Reader = MultiFormatReader()
        try {
            val result = reader.decode(bitmap)
            contents = result.getText()
            if (!contents.contains("pictoblox.page.link")){
                Toast.makeText(this,"Invalid QR code. Please use PictoBlox QR.",Toast.LENGTH_SHORT).show()
//                errorText.setText("Invalid QR code. Please use PictoBlox QR.")
                return
            }
            val intent  = Intent(this@CustomScanner,Home2Activity::class.java)
            intent.putExtra("url",contents.toString())
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.localizedMessage?.let {
//                Toast.makeText(this,it,Toast.LENGTH_SHORT).show()
                Log.e("QrTest", "$it", e)
            }
            Toast.makeText(this,"Error decoding barcode",Toast.LENGTH_SHORT).show()

            Log.e("QrTest", "Error decoding barcode", e)
        }
//        Toast.makeText(this,contents ?: "Nothing", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_scanner)

        val toolbar = findViewById<Toolbar>(R.id.toolb)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        supportActionBar?.title = null
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner)
        try {

            viewfinderView = findViewById(R.id.zxing_viewfinder_view)
            errorText  = findViewById(R.id.zxing_msg)
//        barcodeScannerView.setStatusText("Invalid QR Code. Please use PictoBlox QR.")
//        barcodeScannerView?.setTorchListener(this)
            switchFlashlightButton = findViewById(R.id.switch_flashlight)

        }catch (e: Exception){

        }

        viewfinderView.setLaserVisibility(false)
        viewfinderView.animation = null
//        val corners = floatArrayOf(
//            80f, 80f,   // Top left radius in px
//            80f, 80f,   // Top right radius in px
//            0f, 0f,     // Bottom right radius in px
//            0f, 0f      // Bottom left radius in px
//        )

//        val path = Path()
//        val mPaint = Paint()
//        mPaint.color = Color.YELLOW
//        var canvas : Canvas? = null
//        path.addRoundRect(RectF(0f, 0f, 240f,240f), 20f, 20f, Path.Direction.CW)
//        canvas?.clipPath(path, Region.Op.DIFFERENCE)
//
//        viewfinderView.draw(canvas)

        capture = CaptureManager(this, barcodeScannerView)
        startDecode(savedInstanceState)
    }
    fun startDecode(savedInstanceState: Bundle?) {
        capture.apply {

            this.initializeFromIntent(intent,savedInstanceState)
            this.setShowMissingCameraPermissionDialog(false)
            this.decode()
        }
    }
    override fun onResume() {
        super.onResume()
        capture!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture!!.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture!!.onDestroy()
    }

    fun scanLocalImage(view : View){
        getImage.launch("image/*")
    }
    fun changeCamera(view : View){
//        Toast.makeText(this,"Clicked",Toast.LENGTH_SHORT).show()
        val settings: CameraSettings = barcodeScannerView.getBarcodeView().getCameraSettings()

        if(barcodeScannerView.barcodeView.isPreviewActive){
            barcodeScannerView.pause()
        }

        if (settings.requestedCameraId == 1){
            settings.requestedCameraId = 0
        }else{
            settings.requestedCameraId = 1
        }
        barcodeScannerView.barcodeView.cameraSettings =settings

        barcodeScannerView.resume()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        capture.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }
    fun switchFlashlight(view: View?) {
        if ("Turn on".equals(switchFlashlightButton!!.text)) {
            barcodeScannerView!!.setTorchOn()
        } else {
            barcodeScannerView!!.setTorchOff()
        }
    }


//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String?>,
//        grantResults: IntArray,
//    ) {
////        if (re)
//        capture!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

}