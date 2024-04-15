package io.stempedia.pictoblox.home

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import io.stempedia.pictoblox.QR.CustomScanner
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.account.NEW_USER_TAG
import io.stempedia.pictoblox.account.SignUpDetailFragment
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.connectivity.PictoBloxWebLocale
import io.stempedia.pictoblox.databinding.ActivityHome2Binding
import io.stempedia.pictoblox.databinding.RowHomw2Binding
import io.stempedia.pictoblox.examples.ExamplesActivity
import io.stempedia.pictoblox.projectListing.ProjectListActivity
import io.stempedia.pictoblox.settings.SettingsActivity
import io.stempedia.pictoblox.uiUtils.AbsActivity
import io.stempedia.pictoblox.util.SPManager
import io.stempedia.pictoblox.web.PictoBloxWebActivity
import java.util.Locale


class Home2Activity : AbsActivity() {

    private lateinit var mBinding: ActivityHome2Binding
    private val vm = HomeActivityVM(this)
    private var popupWindow: PopupWindow? = null
    private lateinit var spManager: SPManager
    var lang = ""
    private val TAG = "Home2Activity"
    private var isFirstTime = true
    private lateinit var appUpdateManager : AppUpdateManager
    private val updateType  = AppUpdateType.FLEXIBLE
    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            result: ActivityResult ->
        if (result.resultCode == RESULT_OK){
            Toast.makeText(this,"Download started",Toast.LENGTH_SHORT).show()
        }
    }

    private val barcodeLauncher: ActivityResultLauncher<ScanOptions?>? = registerForActivityResult<ScanOptions, ScanIntentResult>(ScanContract()) { result: ScanIntentResult ->
        if (result.contents == null) {
            val originalIntent = result.originalIntent
            if (originalIntent == null) {
//                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                Log.d("MainActivity", "Cancelled scan due to missing camera permission")
                Toast.makeText(
                    this,
                    "Cancelled due to missing camera permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            checkQRUrl(result.contents.toUri().toString())
        }


    }

    val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADING) {
        }else if (state.installStatus() == InstallStatus.DOWNLOADED){
            popupSnackbarForCompleteUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_home2)
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        spManager = SPManager(this)
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        if (updateType == AppUpdateType.FLEXIBLE){
            appUpdateManager.registerListener(listener)
        }
        checkUpdate()
        checkLanguage()
        mBinding.data = vm
        mBinding.tbHome.setTitle("")

        val toggle = ActionBarDrawerToggle(this, mBinding.drawer, mBinding.tbHome, R.string.open, R.string.close)
        setSupportActionBar(mBinding.tbHome)

        mBinding.tbHome.setNavigationOnClickListener {
            mBinding.drawer.openDrawer(
                GravityCompat.START
            )
        }
        toggle.isDrawerIndicatorEnabled = false
        //toggle.setHomeAsUpIndicator(R.drawable.ic_dabble_icon);
        mBinding.drawer.addDrawerListener(toggle)

        mBinding.rvHome.layoutManager = GridLayoutManager(this, 3)
        mBinding.rvHome.setHasFixedSize(true)
        mBinding.rvHome.adapter = HomeAdapter()
        mBinding.navDrawerHomeActivity.menu.clear()
        mBinding.drawerAboutUs.setText(R.string.about_us)
        mBinding.drawerShare.setText(R.string.share_pictoblox)
        mBinding.drawerRateUs.setText(R.string.rate_us)
        var url: String? = intent?.getStringExtra("url")
        if (!url.isNullOrEmpty()) {
            Intent(this, ProjectListActivity::class.java).apply { data = url.toUri() }.also {
                startActivity(it)
            }
        }


//        findViewById<TextView>(R.id.drawerShare).setOnClickListener {
//            sharePictoBloxApp()
//        }
//        findViewById<TextView>(R.id.drawerRateUs).setOnClickListener {
//            ratePictoBloxApp()
//        }
//        findViewById<TextView>(R.id.drawerAboutUs).setOnClickListener {
//            Intent(this, AboutPictoBloxActivity::class.java).also {
//                startActivity(it)
//            }
//        }
        vm.onCreate()
    }

    fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(mBinding.drawer,
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") {
                appUpdateManager.completeUpdate()
//                finish()
            }
            setActionTextColor(resources.getColor(R.color.colorAccent))
            show()
        }
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

    private fun checkSystemLanguage() {
        val sysLang = Locale.getDefault().getLanguage()
        Log.e("lange", "1 ${sysLang}")
        var lang = PictoBloxWebLocale.values().find {
            it.code.equals(sysLang)
        }
        lang = if (lang != null) lang else PictoBloxWebLocale.ENGLISH
        spManager.pictobloxLocale = lang.code
        var local = Locale(lang.code)
        updateLocale(this, local)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState: ")
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.d(TAG, "onRestoreInstanceState: ")
        var local = Locale(savedInstanceState?.getString("lang") ?: "en")
        updateLocale(this, local)
        super.onRestoreInstanceState(savedInstanceState)
    }

    fun checkLanguage(){
        if (spManager.isFirstTimeInstall) {
            // first time
            Log.d("lange", "1")
            checkSystemLanguage()
            spManager.isFirstTimeInstall = false
//            showAlertDialogWhatsNew()
        } else {
            // Not first time
            Log.d("lange", "2")
            fetchLocal()
        }
    }

    fun checkUpdate(){
        Handler(Looper.getMainLooper()).postDelayed({
            appUpdateManager.appUpdateInfo.addOnSuccessListener {info ->
                val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val isUpdateAllowed = when(updateType){
                    AppUpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
                    AppUpdateType.IMMEDIATE ->info.isImmediateUpdateAllowed
                    else -> false
                }
                if (isUpdateAvailable && isUpdateAllowed){
                    appUpdateManager.startUpdateFlowForResult(info,activityResultLauncher,AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
                }
            }
        },1500)
    }

    fun checkQRUrl(url: String) {
        if (!url.contains("pictoblox.page.link")) {
            Toast.makeText(this, resources.getString(R.string.qr_error), Toast.LENGTH_LONG).show()
            Log.e("TAG", "1")
            return
        } else {
            val intent = Intent(this, ProjectListActivity::class.java)
            intent.data = url.toUri()
            startActivity(intent)
        }
    }

    fun onQRClicked(view: View) {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scanQR()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 103)
        }
    }

    fun scanQR() {
        val options =
            ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setOrientationLocked(false)
                .setCaptureActivity(
                    CustomScanner::class.java
                )
        barcodeLauncher!!.launch(options)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 103) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanQR()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    goToAppSetting()
                } else {
                    if (isFirstTime) {
                        isFirstTime = false
                        ActivityCompat.requestPermissions(this, arrayOf(permissions[0]),103)
                    } else {
                        goToAppSetting()
                    }
                }
            }
        }
    }
    fun goToAppSetting(){
        Toast.makeText(this,"Please provide camera permission",Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 103)
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        if (updateType== AppUpdateType.FLEXIBLE){
            appUpdateManager.unregisterListener(listener)
        }
    }

    private fun showAlertDialogWhatsNew() {
        val arrayListWhatsNew = arrayOf(
            Html.fromHtml("<b>New Language added:</b>"),
            "   - Italian",
        )
        AlertDialog.Builder(this).apply {
            setTitle("What's New")
            setItems(arrayListWhatsNew, null)
            setPositiveButton(getString(R.string.okay)) { _, _ -> }
            setCancelable(false)
            create()
            show()
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume: ")
        if (updateType == AppUpdateType.IMMEDIATE){
            appUpdateManager.appUpdateInfo.addOnSuccessListener {info->
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS){
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateType,
                        this,
                        123
                    )
                }
            }
        }
        vm.onResume()
        super.onResume()
    }

    fun startPictoBloxWebActivity() {
        val intent = Intent(this@Home2Activity, PictoBloxWebActivity::class.java)
        startActivity(intent)
    }

    fun startSettingsActivity() {
        startActivity(Intent(this@Home2Activity, SettingsActivity::class.java))
    }

    fun startExampleActivity() {
        startActivity(Intent(this@Home2Activity, ExamplesActivity::class.java))
    }

    fun showError(e: Throwable) {
        Toast.makeText(this@Home2Activity, "error: ${e.message}", Toast.LENGTH_LONG).show()
    }

    fun showComingSoon() {
        Toast.makeText(this@Home2Activity, getString(R.string.comming_soon), Toast.LENGTH_LONG).show()
    }

    fun goToExternalPage(url: String) {
        val stemLinkIntent = Intent().apply {
            action = Intent.ACTION_VIEW
            addCategory(Intent.CATEGORY_BROWSABLE)
            data =Uri.parse(url)
        }
               startActivity(Intent.createChooser(stemLinkIntent, ""))
    }

    fun startProjectListActivity() {
        startActivity(Intent(this@Home2Activity, ProjectListActivity::class.java))
    }

    override fun onPBServiceConnected(commManagerService: CommManagerServiceImpl) {
        vm.onServiceConnected(commManagerService)
    }

    override fun onBeforeServiceGetsDisconnected(commManagerService: CommManagerServiceImpl) {
        vm.onServiceDisconnected()
    }

    override fun onDeviceConnecting(name: String) {

    }

    override fun onDeviceConnected(name: String, address: String) {

    }

    override fun onDeviceDisconnected(name: String) {

    }

    override fun error(msg: String) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        vm.onActivityResult(requestCode, resultCode, data)
    }

    override fun getRootView(layoutInflater: LayoutInflater): View? {
        if (mBinding == null) {
            mBinding = ActivityHome2Binding.inflate(layoutInflater)
        }
        return mBinding!!.root
    }

    fun showUserLoginSuccess(name: String) {
        Toast.makeText(this, "hi, $name", Toast.LENGTH_LONG).show()
    }

    fun showSignUpFragment() {
        Toast.makeText(this, "Showing fragment for additional details", Toast.LENGTH_LONG).show()
    }

    fun showUserProfileFragment(isNewUser: Boolean) {
        val frag = SignUpDetailFragment()
        frag.arguments = Bundle().apply {
            putBoolean(NEW_USER_TAG, isNewUser)
        }
        frag.show(supportFragmentManager, "sdfsd")
    }

    fun popDialogWithContent(title: String, description: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(description)
            .setNeutralButton("OK") { d, p -> run { d.dismiss() } }
            .create()
            .show()
    }

    fun showLoginIncompleteDialog() {
        val accountIcon = mBinding.ivAccount

        popupWindow?.dismiss()

        val popupView =
            layoutInflater.inflate(
                R.layout.include_profile_incomplete_popup,
                null,
                false
            )

        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = false
        popupWindow = PopupWindow(popupView, width, height, focusable)

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val upIcon = popupView.findViewById<ImageView>(R.id.imageView34)

        val accountPos = IntArray(2)

        accountIcon.getLocationInWindow(accountPos)

        val x = (accountPos[0] + (upIcon.measuredWidth / 2) + (accountIcon.width / 2) - popupView.measuredWidth)
        val y = (accountPos[1] + accountIcon.measuredHeight)

        popupWindow?.animationStyle = R.style.style_incomplete_sign_up

        popupWindow?.showAtLocation(accountIcon, Gravity.NO_GRAVITY, x, y)

        Handler().postDelayed({
            popupWindow?.dismiss()
            popupWindow = null
        }, 3800)
    }

    fun hideLoginIncompleteDialog() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        vm.onWindowFocusChanged(hasFocus)
    }

    private inner class HomeAdapter : RecyclerView.Adapter<HomeViewHolder>() {

        private val list = listOf(
            HomeItemVM(
                1,
                vm,
                this@Home2Activity,
                R.drawable.home_myspace_content,
                R.drawable.home_myspace_bg,
                resources.getString(R.string.my_space),
                true
            ),
            HomeItemVM(
                2,
                vm,
                this@Home2Activity,
                R.drawable.home_examples_content,
                R.drawable.home_examples_bg,
                getString(R.string.examples),
                true
            ),
            HomeItemVM(
                3,
                vm,
                this@Home2Activity,
                R.drawable.home_project_content,
                R.drawable.home_project_bg,
                getString(R.string.projects),
                true
            ),
            HomeItemVM(
                4,
                vm,
                this@Home2Activity,
                R.drawable.codeavour_main,
                R.drawable.home_codeavour_bg,
                "Codeavour",
                true
            ),
            HomeItemVM(
                5,
                vm,
                this@Home2Activity,
                R.drawable.home_shop_content,
                R.drawable.home_shop_bg,
                getString(R.string.shop),
                true
            ),
            HomeItemVM(6,
                vm,
                this@Home2Activity,
                R.drawable.home_learn_content,
                R.drawable.home_learn_bg,
                getString(R.string.learn),
                false
            ),
            HomeItemVM(
                7,
                vm,
                this@Home2Activity,
                R.drawable.home_feed_content,
                R.drawable.home_feed_bg,
                getString(R.string.community),
                false
            )
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
            return HomeViewHolder(layoutInflater.inflate(R.layout.row_homw2, parent, false))
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
            holder.mBinding?.data = list[position]
        }
    }


    private inner class HomeViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var mBinding = DataBindingUtil.bind<RowHomw2Binding>(v)

    }

    override fun onRestart() {
        Log.d(TAG, "onRestart: ")
        if (spManager.isFirstTimeInstall) {
            // first time
            Log.d("lange","1")
            checkSystemLanguage()
            spManager.isFirstTimeInstall = false
//            showAlertDialogWhatsNew()
        } else {
            // Not first time
            Log.d("lange","2")
            fetchLocal()
        }
        super.onRestart()
    }

    override fun onPause() {
        Log.d(TAG, "onPause: ")
        super.onPause()
    }

    override fun onStart() {
        Log.d(TAG, "onStart: ")
        super.onStart()
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")
        super.onStop()
    }
}
