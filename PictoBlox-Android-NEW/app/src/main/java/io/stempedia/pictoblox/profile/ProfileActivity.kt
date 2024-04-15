package io.stempedia.pictoblox.profile

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.GsonBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityProfileBinding
import io.stempedia.pictoblox.firebase.login.*
import io.stempedia.pictoblox.help.HelpActivity
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import io.stempedia.pictoblox.util.createNewAccountEntry
import io.stempedia.pictoblox.util.hasInternet
import java.text.SimpleDateFormat
import java.util.*


class ProfileActivity : AppCompatActivity(), LoginFragmentCallbacks {
    private val vm = ProfileActivityVM(this)
    private lateinit var mBinding: ActivityProfileBinding
    private lateinit var loginFragment: LoginWithEmailPasswordFragment
    private lateinit var spManager: SPManager
    private val TAG = "ProfileActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UPDATE LOCAL OF APP
        spManager = SPManager(this)
        fetchLocal()
        Log.d(TAG, "${resources.configuration.locale}")

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        mBinding.data = vm
        setSupportActionBar(mBinding.tbProfile)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        vm.onCreate()
        mBinding.tbProfile.setNavigationOnClickListener { finish() }

    }

    override fun onStart() {
        super.onStart()
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


    fun showSignUpInCompleteDialog(username: String) {
        val builder = SpannableStringBuilder()

        builder.append("Hi")

        val usernameSpan = SpannableString(username)


        usernameSpan.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            username.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        builder.append(" ")
        builder.append(usernameSpan)
        builder.append(", you have not yet verified your email address, complete email verification to claim free credits :)")


        AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("Claim your free Credits!")
            .setMessage(builder)
            .setNeutralButton("Verify Now") { d, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                d.dismiss()
            }
            .setPositiveButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
                finish()
            }
            .setNegativeButton(getString(R.string.sign_out)) { d, _ ->
                FirebaseAuth.getInstance().signOut()
                SPManager(this).firebaseUserDeviceId = ""
                d.dismiss()
                finish()
            }
            .create()
            .show()
    }

    fun hideSignInDialog() {
        supportFragmentManager
            .beginTransaction()
            .remove(loginFragment)
            .commitAllowingStateLoss()
    }

    fun showSignInDialog() {
        loginFragment = LoginWithEmailPasswordFragment()
        loginFragment.arguments = Bundle().apply {
            putString(FUNCTION, FUNCTION_EXTERNAL_VERIFICATION)
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fl_login_container, loginFragment)
            .commitAllowingStateLoss()

    }

    override fun onSignInComplete() {
        vm.onSignInVerified()
    }

    override fun switchToAgeQueryFragment() {}

    override fun switchToForgotPwdFragment() {}
    override fun switchToForgotUsernameFragment() {}
}

class ProfileActivityVM(val activity: ProfileActivity) {
    val points = ObservableInt(0)
    val totalScore = ObservableField("0")
    val isLoadingData = ObservableBoolean(false)
    val isErrorWhileLoadingData = ObservableBoolean(false)
    val error = ObservableField("")
    val profileIcon = ObservableField<Bitmap>()
    val username = ObservableField("")
    val email = ObservableField("")
    val dobOrAge = ObservableField("")
    val country = ObservableField("")

    val showLoginDialog = ObservableBoolean(false)
    val isDeletingInProcess = ObservableBoolean(false)
    private lateinit var spManager: SPManager

    fun onCreate() {
        spManager = SPManager(activity)
        if (FirebaseAuth.getInstance().currentUser != null) {
            fetchUserProfile(FirebaseAuth.getInstance().currentUser!!.uid)
            setUserProfileIcon(FirebaseAuth.getInstance().currentUser!!.uid)
            fetchUserCredits(FirebaseAuth.getInstance().currentUser!!.uid)

        } else {
            Toast.makeText(activity, "ERROR: no User signed in", Toast.LENGTH_LONG).show()
            activity.finish()
        }
    }

    fun onProfileEditClicked() {

    }

    fun onDismissLoginDialog() {
        activity.hideSignInDialog()
        showLoginDialog.set(false)
    }

    fun onComingSoonClicked() {
        Toast.makeText(activity,activity.getString(R.string.comming_soon) , Toast.LENGTH_LONG).show()
    }

    fun onCreditClicked() {
        activity.startActivity(Intent(activity, CreditDetailActivity::class.java))
    }

    fun onUserDeleteClicked() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.delete_dialog_title))
            .setMessage(activity.getString(R.string.delete_dialog_body))
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.cancel)) { d, _ -> d.dismiss() }
            .setNegativeButton(activity.getString(R.string.continue_)) { d, _ ->
                activity.showSignInDialog()
                showLoginDialog.set(true)
                d.dismiss()
            }
            .create()
            .show()
    }

    fun onSignOutClicked() {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.delete_dialog_title))
            .setPositiveButton(activity.getString(R.string.yes)) { d, _ ->
                FirebaseAuth.getInstance().signOut()
                createAnonymousUser()
                d.dismiss()
            }
            .setNegativeButton(activity.getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            .create()
            .show()
    }

    fun onReloadClicked() {

    }

    fun onHelpClicked() {
        activity.startActivity(Intent(activity, HelpActivity::class.java))

    }

    private fun createAnonymousUser() {
        isLoadingData.set(true)
        isErrorWhileLoadingData.set(false)

        val disposable = hasInternet(activity)
            .andThen(accountCreationSingle())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableSingleObserver<String>() {
                override fun onSuccess(deviceId: String) {
                    isLoadingData.set(false)
                    SPManager(activity).firebaseUserDeviceId = deviceId
                    Toast.makeText(activity,activity.getString(R.string.sign_out_message), Toast.LENGTH_LONG).show()
                    activity.finish()
                    PictobloxLogger.getInstance().logd("Account creation success: device id :  $deviceId")
                }

                override fun onError(e: Throwable) {
                    isLoadingData.set(false)
                    PictobloxLogger.getInstance().logd("Account creation failed")
                    PictobloxLogger.getInstance().logException(e)
                    error.set(e.message)
                    isErrorWhileLoadingData.set(true)
                }

            })
    }

    private fun accountCreationSingle() = Single.create<String> { emitter ->

        createNewAccountEntry()
            .addOnSuccessListener {
                emitter.onSuccess(it)
            }
            .addOnFailureListener {
                emitter.onError(it)
            }

    }

    private fun fetchUserProfile(userId: String) {
        isLoadingData.set(true)
        isErrorWhileLoadingData.set(false)

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener {
                isLoadingData.set(false)
                try {
                    if (it.exists()) {

                       val accountType =  it.getString("account_type")
                        Log.e("TAG", "fetchUserProfile: $accountType", )
                        if (accountType != null) {
                            spManager.storeLastLoginAccountType(accountType)
                        }
                        isErrorWhileLoadingData.set(false)

                        val isStage2Completed = it.getBoolean("is_secondary_detail_filled") ?: false

                        if (isStage2Completed) {
                            setUserData(it)

                        } else {
                            val username = it.getString("username") ?: ""
                            activity.showSignUpInCompleteDialog(username)
                        }

                    } else {
                        error.set(activity.getString(R.string.error_fetching_message))
                        isErrorWhileLoadingData.set(true)
                    }
                } catch (e: Exception) {
                    error.set(activity.getString(R.string.something_wrong_message))
                    isErrorWhileLoadingData.set(true)
                }


            }
            .addOnFailureListener {

                error.set("Please check connectivity")
                isErrorWhileLoadingData.set(true)
                isLoadingData.set(false)

            }
    }

    private fun fetchUserCredits(userId: String) {

        FirebaseFirestore.getInstance().collection("user_credits").document(userId)
            .get()
            .addOnSuccessListener { snap ->
                try {
                    if (snap.exists()) {

                        val userCredit = snap.getLong("pictoblox_credits")

                        if (userCredit != null) {
                            totalScore.set(userCredit.toString())
                        }

                    }
                } catch (e: Exception) {
                    PictobloxLogger.getInstance().logException(e)
                }


            }
            .addOnFailureListener { e ->
                PictobloxLogger.getInstance().logException(e)
            }
    }

    private fun setUserData(snapshot: DocumentSnapshot) {

        username.set(snapshot.get("username") as String)
        email.set(snapshot.get("email") as String)
        country.set(snapshot.get("country") as String)

        val isMinor = if (snapshot.get("is_minor") != null) {
            snapshot.get("is_minor") as Boolean
        } else {
            snapshot.get("isMinor") as Boolean
        }

        if (isMinor) {
            dobOrAge.set("Age : ${snapshot.get("age").toString()}")

        } else {
            val birthdate = if (snapshot.get("birthdate") != null) {
                snapshot.get("birthdate") as Timestamp

            } else {
                snapshot.get("birthDate") as Timestamp
            }

            val date = birthdate.toDate()

            val mEEEddMMMyyyy = SimpleDateFormat("EEE dd-MMM-yyyy", Locale.US)

            val z = mEEEddMMMyyyy.format(date)

            dobOrAge.set("B'date : $z")

        }
    }

    private fun setUserProfileIcon(userId: String) {

        val thumbRef = FirebaseStorage.getInstance().getReference("user_assets").child(userId).child("profile_images").child("thumb.png")
        Glide.with(activity)
            .asBitmap()
            .apply(RequestOptions().circleCrop().error(R.drawable.ic_account3))
            .load(thumbRef)
            .into(object : CustomTarget<Bitmap>() {

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    setNoUserThumbIcon()

                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    profileIcon.set(resource)
                }
            })


    }

    private fun setNoUserThumbIcon() {
        Glide.with(activity)
            .asBitmap()
            .apply(RequestOptions().circleCrop())
            .load(R.drawable.ic_account3)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    profileIcon.set(resource)
                }
            })
    }

    fun onSignInVerified() {
        activity.hideSignInDialog()
        isDeletingInProcess.set(true)
        /*FirebaseAuth.getInstance().currentUser?.delete()
            ?.addOnFailureListener {
                isDeletingInProcess.set(false)
            }
            ?.addOnSuccessListener {
                isDeletingInProcess.set(false)

            }*/

        FirebaseFunctions.getInstance("asia-east2")
            .getHttpsCallable("deleteUser")
            .call()
            .addOnSuccessListener {
                FirebaseAuth.getInstance().signOut()
                isDeletingInProcess.set(false)
                activity.finish()
            }.addOnFailureListener {
                isDeletingInProcess.set(false)

            }

    }
}
