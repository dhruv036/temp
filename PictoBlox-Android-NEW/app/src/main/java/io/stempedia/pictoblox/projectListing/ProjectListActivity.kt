package io.stempedia.pictoblox.projectListing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.Lifecycle
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.databinding.ActivityHome2Binding
import io.stempedia.pictoblox.databinding.ActivityProjectListBinding
import io.stempedia.pictoblox.help.HelpActivity
import io.stempedia.pictoblox.settings.SettingsActivity
import io.stempedia.pictoblox.web.PictoBloxWebActivity
import io.stempedia.pictoblox.uiUtils.AbsActivity
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import java.lang.Exception
import java.util.Locale


class ProjectListActivity : AbsActivity(), ProjectListActivityVM.SharePopUpCallback {
    private val vm = ProjectListActivityVM(this)
    private lateinit var mBinding: ActivityProjectListBinding
    private lateinit var recentFrag: RecentProjectFragment
    private lateinit var localFrag: LocalProjectFragment
    private var popupWindow: PopupWindow? = null
    private lateinit var spManager: SPManager
    private val TAG = "ProjectListActivity"
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
        // UPDATE LOCAL OF APP
        spManager = SPManager(this)
        fetchLocal()
        Log.d(TAG, "${resources.configuration.locale}")
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_project_list)
        mBinding.data = vm
        recentFrag = supportFragmentManager.findFragmentById(R.id.frag_recent) as RecentProjectFragment
        localFrag = supportFragmentManager.findFragmentById(R.id.frag_local) as LocalProjectFragment

        setSupportActionBar(mBinding.tbProjectList)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        mBinding.tbProjectList.setNavigationOnClickListener { finish() }

        vm.onCreate(intent)
        vm.showPopUpcallback(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        vm.onNewIntent(intent)
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
        vm.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        vm.onWindowFocusChanged(hasFocus)
    }

    override fun onPBServiceConnected(commManagerService: CommManagerServiceImpl) {
        vm.onServiceConnected(commManagerService)
    }

    override fun onBeforeServiceGetsDisconnected(commManagerService: CommManagerServiceImpl) {
        vm.onBeforeServiceGetsDisconnected(commManagerService)
    }

    override fun onDeviceConnecting(name: String) {
        Toast.makeText(this@ProjectListActivity, getString(R.string.connecting_device, name), Toast.LENGTH_SHORT).show()
    }

    override fun onDeviceConnected(name: String, address: String) {
        Toast.makeText(
            this@ProjectListActivity,
            getString(R.string.connected_device, name),
            Toast.LENGTH_SHORT
        ).show()
        autoConnectDialog(name)
    }

    override fun onDeviceDisconnected(name: String) {
        Toast.makeText(
            this@ProjectListActivity,
            getString(R.string.disconnected_device, name),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun error(msg: String) {
        Toast.makeText(
            this@ProjectListActivity,
            getString(R.string.error_device, msg),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun populateRecentContent() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            supportFragmentManager.beginTransaction()
                .hide(localFrag)
                .show(recentFrag)
                .commit()
        }
    }

    fun populateLocalContent() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            supportFragmentManager.beginTransaction()
                .hide(recentFrag)
                .show(localFrag)
                .commit()
        }
    }

    fun populateCloudContent() {

    }

    override fun onBackPressed() {
        if (vm.shouldExitOnBackPress()) {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        vm.onActivityResultVM(requestCode, resultCode, data)
    }

    fun startPictobloxWeb() {
        val intent = Intent(this@ProjectListActivity, PictoBloxWebActivity::class.java)
        startActivity(intent)
    }

    fun errorInOpeningEmptyFile(e: Throwable) {
        Toast.makeText(this, "${getString(R.string.error)} : ${e.message}", Toast.LENGTH_LONG).show()
    }

    fun errorInOpeningCachedFile(e: Throwable) {
        Toast.makeText(this, "${getString(R.string.error)}  : ${e.message}", Toast.LENGTH_LONG).show()
    }

    fun errorInOpeningExternalFile(e: Throwable) {
        Toast.makeText(this, "${getString(R.string.error)}  : ${e.message}", Toast.LENGTH_LONG).show()
    }

    fun showNoFileSelectedMessage() {
        Toast.makeText(this, getString(R.string.no_file_selected), Toast.LENGTH_LONG).show()
    }

    fun showErrorInRetrievingFile() {
        Toast.makeText(this, "Failed to read file", Toast.LENGTH_LONG).show()
    }

    fun getRecentFragment(): AbsProjectListFragment {
        return recentFrag
    }

    fun getLocalFragment(): AbsProjectListFragment {
        return localFrag
    }

    fun showHelp() {
        startActivity(Intent(this, HelpActivity::class.java))

    }

    fun showSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun getVM(): ProjectListActivityVM {
        return vm
    }

    fun showNoFilesToShareMessage() {
        Toast.makeText(this, getString(R.string.no_file_selected), Toast.LENGTH_LONG).show()
    }


    fun hideLoginIncompleteDialog() {
        popupWindow?.dismiss()
        popupWindow = null
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

/*    fun restartOptionsMenu(){
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_project_list,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val default:Boolean = super.onOptionsItemSelected(item)
        return vm.onOptionItemSelected(item,default)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.export_local_projects)?.isVisible = !vm.isSelectionEnabled.get()
        menu?.findItem(R.id.export_selected_projects)?.isVisible = vm.isSelectionEnabled.get()
        return true
    }*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        vm.onRequestPermissionResultVM(requestCode,permissions,grantResults)
    }

    override fun showPopUp() {
//        Toast.makeText(this,"show popup",Toast.LENGTH_SHORT).show()
//
//        val accountIcon = mBinding.ivShare
//
//        popupWindow?.dismiss()
//
//        val popupView =
//            layoutInflater.inflate(
//                R.layout.share_type_dialog,
//                null,
//                false
//            )
//
//        val width = LinearLayout.LayoutParams.WRAP_CONTENT
//        val height = LinearLayout.LayoutParams.WRAP_CONTENT
//        val focusable = false
//        popupWindow = PopupWindow(popupView, width, height, focusable)
//
//        popupView.measure(
//            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
//            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        )
//
//        val typeFirst = popupView.findViewById<ImageView>(R.id.share_type_first)
//        val typeSecond = popupView.findViewById<ImageView>(R.id.share_type_second)
//        val typeThired = popupView.findViewById<ImageView>(R.id.share_type_thired)
//
//        val accountPos = IntArray(2)
//
//        accountIcon.getLocationInWindow(accountPos)
//
//        val x = (accountPos[0] + (upIcon.measuredWidth / 2) + (accountIcon.width / 2) - popupView.measuredWidth)
//        val y = (accountPos[1] + accountIcon.measuredHeight)
//
////        popupWindow?.animationStyle = R.style.style_incomplete_sign_up
//
//        popupWindow?.showAtLocation(accountIcon, Gravity.NO_GRAVITY, x, y)
//
//        Handler().postDelayed({
//            popupWindow?.dismiss()
//            popupWindow = null
//        }, 3800)

    }

}
