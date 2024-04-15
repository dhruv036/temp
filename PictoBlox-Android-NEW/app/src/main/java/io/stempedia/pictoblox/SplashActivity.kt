package io.stempedia.pictoblox

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Intent
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.core.Completable
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.connectivity.SB3_FILES_DIR
import io.stempedia.pictoblox.databinding.ActivitySplash2Binding
import io.stempedia.pictoblox.firebase.login.SendEmailRes
import io.stempedia.pictoblox.home.Home2Activity
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import io.stempedia.pictoblox.util.createNewAccountEntry
import io.stempedia.pictoblox.util.firebaseCurrentUserDevice
import io.stempedia.pictoblox.util.firebaseUserDetail
import java.io.File
import java.io.FileOutputStream


class SplashActivity : AppCompatActivity(), Animator.AnimatorListener {

    private val handler = Handler()
    private lateinit var anim: ObjectAnimator
    private lateinit var spManager: SPManager
    private lateinit var mBinding: ActivitySplash2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.inflate(layoutInflater, R.layout.activity_splash2, null, false);
        super.setContentView(mBinding.root)
        spManager = SPManager(this)

        Log.e("error","1")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, CommManagerServiceImpl::class.java))
        } else {
            startService(Intent(this, CommManagerServiceImpl::class.java))
        }
        Log.e("error","2")

        anim = createAnimation()
        startService(Intent(this, CommManagerServiceImpl::class.java))

        registerUserForTopic(spManager)
        Log.e("error","3")

        //Bind this with loading time
        try {
            FirebaseApp.initializeApp(this)

            if (FirebaseAuth.getInstance().currentUser == null) {
                createNewAccountEntry()
                    .addOnSuccessListener {
                        SPManager(this).firebaseUserDeviceId = it
                        PictobloxLogger.getInstance().logd("Account creation success: device id :  $it")
                    }
                    .addOnFailureListener {
                        PictobloxLogger.getInstance().logd("Account creation failed")
                        PictobloxLogger.getInstance().logException(it)
                    }

            } else {
                updateExistingUserEntry()
            }
        }catch (_: Exception){
        }

        try {
            if (FirebaseAuth.getInstance().currentUser != null) {
                firebaseUserDetail(FirebaseAuth.getInstance().currentUser!!.uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener {}
            }
        }catch (ee : SQLiteDatabaseLockedException){

        }
        Log.e("error","4")

        handler.postDelayed(animRunnable, 500)

        /*        testUsernameEmail()
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {
                            PictobloxLogger.getInstance().logd("Success")
                        }

                        override fun onError(e: Throwable?) {
                            PictobloxLogger.getInstance().logException(e!!)
                        }

                    })*/
        /*
                val z = "[\n" +
                        "   {\n" +
                        "     \"username\": \"ankitTEST\$4545\",\n" +
                        "    \"password\": \"123456\",\n" +
                        "    \"is_minor\": false,\n" +
                        "    \"redeem_key\": \"123345\"\n" +
                        "  },\n" +
                        " {\n" +
                        "     \"username\": \"ankitTEST\$4546\",\n" +
                        "    \"password\": \"123456\",\n" +
                        "    \"is_minor\": false,\n" +
                        "    \"redeem_key\": \"000121VG\"\n" +
                        "  },\n" +
                        " {\n" +
                        "     \"username\": \"ankitTEST\$4547\",\n" +
                        "    \"password\": \"123456\",\n" +
                        "    \"is_minor\": false,\n" +
                        "    \"redeem_key\": \"123345\"\n" +
                        "  }\n" +
                        "]\n" +
                        "\n"

                FirebaseFunctions.getInstance("asia-east2")
                        .getHttpsCallable("createStudentAccounts")
                        .call(mapOf(
                                "student_entries" to z
                        )
                        )
                        .addOnSuccessListener { httpsCallableResult ->
                            PictobloxLogger.getInstance().logd(httpsCallableResult.data?.toString() ?: "NO data")
                        }.addOnFailureListener {
                            PictobloxLogger.getInstance().logException(it)
                        }*/
        /*
                findViewById<Button>(R.id.button8).setOnClickListener {
                    spManager = SPManager(this)
                    anim = createAnimation()
                    Toast.makeText(applicationContext, "step 1 completed", Toast.LENGTH_LONG).show()
                }
                findViewById<Button>(R.id.button15).setOnClickListener {
                    startService(Intent(this, CommManagerServiceImpl::class.java))
                    Toast.makeText(applicationContext, "step 2 completed", Toast.LENGTH_LONG).show()

                }
                findViewById<Button>(R.id.button16).setOnClickListener {
                    registerUserForTopic(spManager)
                    Toast.makeText(applicationContext, "step 3 completed", Toast.LENGTH_LONG).show()
                }
                findViewById<Button>(R.id.button17).setOnClickListener {
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        firebaseUserDetail(FirebaseAuth.getInstance().currentUser!!.uid)
                            .get(Source.SERVER)
                            .addOnSuccessListener {}
                    }
                    Toast.makeText(applicationContext, "step 4 completed", Toast.LENGTH_LONG).show()
                }
                findViewById<Button>(R.id.button18).setOnClickListener {
                    handler.postDelayed(animRunnable, 500)
                    Toast.makeText(applicationContext, "step 5 completed", Toast.LENGTH_LONG).show()
                }*/
    }

    private fun registerUserForTopic(spManager: SPManager) {
        Log.e("error","5")

        if (spManager.isSubscribedToDabbleNews) {
            FirebaseMessaging.getInstance().subscribeToTopic("Demo")
                .addOnCompleteListener { task ->
                    run {
                        PictobloxLogger.getInstance().logd("Subscribe -> ${task.isSuccessful}")

//                        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//                            if (!task.isSuccessful) {
//                                Log.w("token", "Fetching FCM registration token failed", task.exception)
//                                return@OnCompleteListener
//                            }
//
//                            // Get new FCM registration token
//                            val token = task.result
//
////                            // Log and toast
////                            val msg = getString(R.string.msg_token_fmt, token)
//                            Log.d("token", token)
////                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                        })
                    }
                }
        }
        Log.e("error","6")
    }
    private fun updateExistingUserEntry() {
        Log.e("error","12")
        try {
            if (!TextUtils.isEmpty(SPManager(this).firebaseUserDeviceId)) {
                firebaseCurrentUserDevice(SPManager(this).firebaseUserDeviceId)
                    .update(mapOf("latest_access" to Timestamp.now()))
                    .addOnCompleteListener {
                    }
            }
            Log.e("error","13")
        }catch (eeee : Exception){

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (anim.isStarted) {
            anim.cancel()
        }
        handler.removeCallbacks(animRunnable)
        handler.removeCallbacks(startActivityRunnable)
    }


    private fun copyFiles() {

        val file = File(filesDir, SB3_FILES_DIR)
        /*
                x("ASK_ANIMATIONS.sb3", file, "ASK ANIMATIONS.sb3")
                x(
                    "Gamepad_Module_Analog_value_Diplay_evive.sb3",
                    file,
                    "Gamepad Module Analog value Diplay evive.sb3"
                )
                x("LED_Brightness_Control_Arduino_Uno.sb3", file, "LED Brightness Control Arduino Uno.sb3")
                x("Motion_Activated_Camera_System.sb3", file, "Motion Activated Camera System.sb3")
                x(
                    "Publishing_Temperature_and_Humidity_Sensor_to_ThingSpeak.sb3",
                    file,
                    "Publishing Temperature and Humidity Sensor to ThingSpeak.sb3"
                )
                x("RGB_LED_Clock.sb3", file, "RGB LED Clock.sb3")
                x(
                    "Smartphone_Controlled_Robot_with_Dabble_for_Arduino_Uno.sb3",
                    file,
                    "Smartphone Controlled Robot with Dabble for Arduino Uno.sb3"
                )
                x("Tobi_Talking.sb3", file, "Tobi Talking.sb3")*/
        x("Tobi_Walking_new.sb3", file, "Tobi Walking.sb3")

    }

    private fun x(name: String, dir: File, nameToGive: String) {
        val inputStream = assets.open("sample_sb3/$name")

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, nameToGive)

        val baos = FileOutputStream(file)

        val array = ByteArray(512)

        var byteRead = inputStream.read(array)

        while (byteRead != -1) {
            baos.write(array, 0, byteRead)
            byteRead = inputStream.read(array)
        }

        baos.close()


    }

    private fun createAnimation(): ObjectAnimator {

        val stemLogoAnim = ObjectAnimator.ofFloat(mBinding.ivSplashStemLogo, "alpha", 0f, 1f)

        stemLogoAnim.duration = 1500

        stemLogoAnim.addListener(this)

        return stemLogoAnim
    }

    private val animRunnable = Runnable { anim.start() }

    private val startActivityRunnable = Runnable {
        if (spManager.isGettingStartedShown) {
            spManager.isGettingStartedShown = false
            var intent = Intent(this@SplashActivity, GettingStartedActivity::class.java)
            startActivity(intent)
        } else {
            startActivity(Intent(this@SplashActivity, Home2Activity::class.java))
        }
        finish()
    }

    override fun onAnimationRepeat(animation: Animator) {}

    override fun onAnimationEnd(animation: Animator) {
        handler.postDelayed(startActivityRunnable, 1000)
    }

    override fun onAnimationCancel(animation: Animator) {}

    override fun onAnimationStart(animation: Animator) {
        mBinding.ivSplashStemLogo.visibility = View.VISIBLE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun testUsernameEmail() = Completable.create { emitter ->

        FirebaseFunctions.getInstance("asia-east2")
            .getHttpsCallable("sendUsernameRecoverEmail")
            .call(mapOf("email" to "ankitgiri6@thestempedia.com"))
            .addOnSuccessListener { httpsCallableResult ->
                val res = GsonBuilder().create().fromJson<SendEmailRes>(
                    httpsCallableResult.data as String,
                    SendEmailRes::class.java
                )

                if (res.status == "success") {
                    emitter.onComplete()
                } else {
                    emitter.onError(Exception(res.error))
                }
            }.addOnFailureListener {
                PictobloxLogger.getInstance().logException(it)
                emitter.onError(Exception("Please check your Internet connection"))
            }
    }
}

