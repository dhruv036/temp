package io.stempedia.pictoblox.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import io.stempedia.pictoblox.AboutPictoBloxActivity
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.databinding.DialogTutsBinding
import io.stempedia.pictoblox.firebase.login.LoginActivity
import io.stempedia.pictoblox.help.HelpActivity
import io.stempedia.pictoblox.learn.CourseListActivity
import io.stempedia.pictoblox.profile.ProfileActivity
import io.stempedia.pictoblox.util.AccountHandlerCallback
import io.stempedia.pictoblox.util.AccountIconHandler
import io.stempedia.pictoblox.util.OTGFirmwareUploadFragment
import io.stempedia.pictoblox.util.SPManager

class HomeActivityVM(val activity: Home2Activity) : AccountHandlerCallback {
    private var commManagerService: CommManagerServiceImpl? = null
    val fabIcon = ObservableInt(R.drawable.ic_code_new)
    val showProfileIncompleteError = ObservableBoolean(false)
    val profileIcon = ObservableField<Bitmap>()

    //private val RC_SIGN_IN = 190
    private lateinit var spManager: SPManager
    //private var isSignUpComplete = false

    private val accountIconHandler = AccountIconHandler(activity, this)

    fun onServiceConnected(commManagerService: CommManagerServiceImpl) {
        this.commManagerService = commManagerService

        if (commManagerService.communicationHandler.storageHandler.isCachedVersionExists()) {
            fabIcon.set(R.drawable.ic_code_resume)
        } else {
            fabIcon.set(R.drawable.ic_code_new)
        }
    }

    fun onCreate() {
        accountIconHandler.onCreate()
        //PictobloxLogger.getInstance().logd(FirebaseAuth.getInstance().uid?:"-------")
    }

    fun onResume() {
        spManager = SPManager(activity)
        commManagerService?.also { commManagerService ->
            if (commManagerService.communicationHandler.storageHandler.isCachedVersionExists()) {
                fabIcon.set(R.drawable.ic_code_resume)
            } else {
                fabIcon.set(R.drawable.ic_code_new)
            }
        }

        accountIconHandler.onResume()


    }

    fun onServiceDisconnected() {
        commManagerService = null
    }

    fun onFabClicked() {
        commManagerService?.also {

            if (!spManager.hasUserSeenTour) {
                spManager.hasUserSeenTour = true
                popTutDialog(it)
            } else {
                if (it.communicationHandler.storageHandler.isCachedVersionExists()) {
                    loadCached(it)
                } else {
                    loadNew(it)
                }
            }
        }
    }

    fun onSettingsClicked() {
        activity.startSettingsActivity()

        /*if (BuildConfig.DEBUG) {
            activity.startSettingsActivity()

        } else {
            activity.showComingSoon()
        }*/

       /* FirebaseFunctions.getInstance("asia-east2").getHttpsCallable("generateUploadToken")
            .call(mapOf("path" to "/path/to/resource"))
            .addOnSuccessListener {
                PictobloxLogger.getInstance().logd("Call success")
                val res = GsonBuilder().create().fromJson(it.data as String?, TestResponse::class.java)
                PictobloxLogger.getInstance().logd("${res.status} : ${res.token} : ${res.error}")

            }
            .addOnFailureListener {
                PictobloxLogger.getInstance().logException(it)
            }*/
    }

    fun aboutUsClicked(){
        Intent(activity, AboutPictoBloxActivity::class.java).also {
            activity.startActivity(it)
        }
    }
    fun ratePictoBloxApp() {
        val uri = Uri.parse("market://details?id=${activity.packageName}")
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            activity.startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, " unable to find market app", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePictoBloxApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.apply {
            this.setType("text/plain")
            this.putExtra(Intent.EXTRA_SUBJECT, "PictoBlox App")
            this.putExtra(
                Intent.EXTRA_TEXT,
                "https://play.google.com/store/apps/details?id=io.stempedia.pictoblox"
            )
            activity.startActivity(this)
        }
    }

    fun onHelpClicked() {
        //commManagerService?.communicationHandler?.soundHandler?.playKey(1)
        //activity.showComingSoon()
        /*commManagerService?.also {
            loadTour(it)
        }*/

        activity.startActivity(Intent(activity, HelpActivity::class.java))
    }

    fun onShowFirmwareUpload() {
        activity
            .supportFragmentManager
            .beginTransaction()
            //.setReorderingAllowed()
            .addToBackStack(null)
            .replace(R.id.fl_dummy_dfu, OTGFirmwareUploadFragment())
            .commit()
    }

    fun onAccountClicked() {
        accountIconHandler.onAccountClicked()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
/*        when (requestCode) {
            RC_SIGN_IN -> {
                IdpResponse.fromResultIntent(data)?.also {

                    if (it.isNewUser) {
                        activity.showUserProfileFragment(true)

                    } else {
                        activity.showUserLoginSuccess(accountHelper.getUserName())
                    }
                }
            }
        }*/
    }

    private fun loadCached(commManagerService: CommManagerServiceImpl) {
        activity.add(
            commManagerService.communicationHandler.loadCachedProject()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {

                    override fun onComplete() {
                        activity.startPictoBloxWebActivity()
                    }

                    override fun onError(e: Throwable) {
                        activity.showError(e)
                    }

                })
        )
    }

    private fun loadNew(commManagerService: CommManagerServiceImpl) {
        activity.add(
            commManagerService.communicationHandler.loadEmptyProject()
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        activity.startPictoBloxWebActivity()
                    }

                    override fun onError(e: Throwable) {
                        activity.showError(e)
                    }

                })
        )
    }

    fun onLearnClicked() {
        activity.startActivity(Intent(activity, CourseListActivity::class.java))
        /*if (accountHelper.isLoggedIn()) {
            activity.startActivity(Intent(activity, CourseListActivity::class.java))

        } else {

            onAccountClicked()
        }*/
    }


    private fun onStartTutorials(commManagerService: CommManagerServiceImpl) {
        activity.add(
            commManagerService.communicationHandler.loadTour().subscribeWith(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    activity.startPictoBloxWebActivity()
                }

                override fun onError(e: Throwable) {
                    //TODO
                }
            })

        )
    }

    private fun onSkipTutorials(commManagerService: CommManagerServiceImpl) {
        loadNew(commManagerService)
    }


    private fun popTutDialog(commManagerService: CommManagerServiceImpl) {

        val mBinding = DataBindingUtil.inflate<DialogTutsBinding>(activity.layoutInflater, R.layout.dialog_tuts, null, false)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(activity)
            .setView(mBinding.root)
            .create()

        mBinding.tvTutContinue.setOnClickListener {
            dialog.dismiss()
            onStartTutorials(commManagerService)

        }
        mBinding.tvTutSkip.setOnClickListener {
            dialog.dismiss()
            onSkipTutorials(commManagerService)
        }


        /*val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window!!.attributes)
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.show()
        dialog.window!!.attributes = lp*/
        dialog.show()
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        accountIconHandler.onWindowFocusChanged(hasFocus)
    }

    override fun showSignUpIncompleteAnimation() {
        showProfileIncompleteError.set(true)
        activity.showLoginIncompleteDialog()
    }

    override fun dismissSignUpIncompleteAnimationIfRequired() {
        activity.hideLoginIncompleteDialog()
        showProfileIncompleteError.set(false)
    }

    override fun redirectToSignInProcess() {
        activity.startActivity(Intent(activity, LoginActivity::class.java))
    }

    override fun redirectToProfile() {
        activity.startActivity(Intent(activity, ProfileActivity::class.java))
    }

    override fun setAccountImage(bitmap: Bitmap) {
        profileIcon.set(bitmap)
    }

}

class TestResponse(val status: String?, val token: String?, val error: String?)