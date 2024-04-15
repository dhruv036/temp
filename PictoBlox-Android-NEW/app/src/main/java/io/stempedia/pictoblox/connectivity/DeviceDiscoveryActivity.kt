package io.stempedia.pictoblox.connectivity

import android.Manifest
import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.firebase.analytics.FirebaseAnalytics
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityDeviceDiscoveryBinding
import io.stempedia.pictoblox.databinding.RowDeviceListBinding
import io.stempedia.pictoblox.databinding.RowDeviceListTitleBinding
import io.stempedia.pictoblox.util.SPManager
import java.util.Locale


const val EXTRA_DEVICE_ADDRESS = "extraDeviceAddress"

class DeviceDiscoveryActivity : AppCompatActivity(), SearchResultCallback, View.OnClickListener {

    private lateinit var binding: ActivityDeviceDiscoveryBinding
    private lateinit var adapter: DeviceListAdapter
    private var searchService: SearchDeviceService? = null
    private val requestEnableGPS = 102
    private val vm = DeviceDiscoveryVM(
        this,
        isPermissionGranted = true,
        isGPSEnabled = false,
        errorMessage = ObservableInt(R.string.device_discovery_permission_info),
        locationRationalVisibility = ObservableBoolean()
    )
    private val myPermissionsRequestLocation = 213
    private var findingNameColor = 0
    private var nameAlreadyAvailableColor = 0
    private var nameChangedColor = 0
    private lateinit var llm: LinearLayoutManager
    private var sharedPreferencesManager: SPManager? = null
    private var isExitingWithoutClickingBluetoothDevice = true
    private var gpsResolvableApiException: ResolvableApiException? = null
    private lateinit var spManager: SPManager


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spManager = SPManager(this)
        var code = spManager.pictobloxLocale
        code = if (code.contains("tw") || code.contains("cn")) code.substring(3,5) else code
        updateLocale(this,Locale(code))
        binding = DataBindingUtil.setContentView(this, R.layout.activity_device_discovery)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ( ActivityCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN)== PackageManager.PERMISSION_GRANTED){

                startService(Intent(this@DeviceDiscoveryActivity, SearchDeviceServiceImpl::class.java))

            }else{
                this.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN),1004)
            }
        }
        else{
            startService(Intent(this@DeviceDiscoveryActivity, SearchDeviceServiceImpl::class.java))
        }
        binding.data = vm
        binding.tvDdTitle.text = getString(R.string.device_discovery_init_bt)

        sharedPreferencesManager = SPManager(this)
        findingNameColor = ContextCompat.getColor(this, R.color.device_search_no_name)
        nameAlreadyAvailableColor = ContextCompat.getColor(this, R.color.device_search_name)
        nameChangedColor = ContextCompat.getColor(this, R.color.device_search_name_changed)

        llm = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.deviceDiscoveryRecyclerView.layoutManager = llm
        binding.deviceDiscoveryRecyclerView.setHasFixedSize(true)
        adapter = DeviceListAdapter(this)
        binding.deviceDiscoveryRecyclerView.adapter = adapter

        //location permission message
        //binding.textView93?.setOnClickListener(this)
        binding.tvAllowLocationPerm.setOnClickListener(this)

        //We have to start this service as the activity can get destroyed and recreated several times base of orientation of device.
        //The service is pretty lightweight even if its running in background for a while it does not put much pressure on memory.

        setResult(Activity.RESULT_CANCELED)

        binding.deviceDiscoveryRecyclerView.addOnScrollListener(scrollListener)
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

    private val scrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    removeNameChangeIndication()
                }
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                }
                RecyclerView.SCROLL_STATE_SETTLING -> {
                }
            }

        }

        fun removeNameChangeIndication() {
            if (adapter.list.isNotEmpty()) {
                val start = llm.findFirstVisibleItemPosition()
                val end = llm.findLastVisibleItemPosition()

                if (start == -1 || end == -1) {
                    return
                }

                for (i in start..end) {
                    if (adapter.list[i].isNameChanged) {
                        adapter.list[i].isNameChanged = false
                        adapter.list[i].animateTextColor = true
                        adapter.notifyItemChanged(i)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, SearchDeviceServiceImpl::class.java).also { intent ->
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAnalytics.getInstance(this).setCurrentScreen(this, getString(R.string.analytics_connect_device_list), null)
    }

    override fun onStop() {
        super.onStop()
        searchService?.stopSearch()
        unbindService(mConnection)
    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            (service as SearchDeviceServiceImpl.LocalBinder).getService().apply {
                search(this@DeviceDiscoveryActivity)
                searchService = this
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {

        }
    }

    override fun error(msg: String) {
        Toast.makeText(this@DeviceDiscoveryActivity, msg, Toast.LENGTH_LONG).show()
    }


    /*override fun btNotEnabled() {
        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableIntent, requestEnableBt)
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            requestEnableGPS -> {
                if (resultCode == Activity.RESULT_OK) {
                    vm.isGPSEnabled = true
                    vm.locationRationalVisibility.set(false)
                    //hideRational()
                    searchService?.search(this@DeviceDiscoveryActivity)
                }
            }

        }

    }

    override fun gpsNotEnabledForV10(exception: ResolvableApiException) {
        this.gpsResolvableApiException = exception
        vm.isGPSEnabled = false
        vm.errorMessage.set(R.string.device_discovery_gps_info)
        vm.locationRationalVisibility.set(true)
        //showRational()
    }

    override fun locationPermissionNotGranted() {
        vm.isPermissionGranted = false
        vm.errorMessage.set(R.string.device_discovery_permission_info)

        Handler().postDelayed({ vm.locationRationalVisibility.set(true) }, 100)

        //showRational()

    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.tv_allow_location_perm -> {
                if (!vm.isPermissionGranted) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), myPermissionsRequestLocation)

                } else if (!vm.isGPSEnabled) {
                    gpsResolvableApiException?.also {
                        try {
                            it.startResolutionForResult(this, requestEnableGPS)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            showUnresolvableErrorMessage()
                        }
                    }
                }
            }
/*            R.id.textView93 -> {
                vm.locationRationalVisibility.set(false)
                //hideRational()
                searchService?.setSkipBLESearch(true)
                searchService?.search(this@DeviceDiscoveryActivity)
            }*/
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            myPermissionsRequestLocation -> {
                vm.isPermissionGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (vm.isPermissionGranted) {
                    vm.locationRationalVisibility.set(false)
                    //hideRational()
                    searchService?.search(this@DeviceDiscoveryActivity)
                }

            }
        }
    }

    override fun onBluetoothAdapterInitFailed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.device_discovery_bt_detect_error))
            .setCancelable(false)
            .setPositiveButton(R.string.general_okay) { _, _ -> finish() }
            .show()
    }


    override fun showUnresolvableErrorMessage() {
        if (!(this as Activity).isFinishing) {
            AlertDialog.Builder(this)
                .setTitle(R.string.gps_error_check_settings)
                .setCancelable(false)
                .setPositiveButton(R.string.general_okay) { _, _ -> finish() }
                .show()
        }

    }

    override fun onEnablingBluetooth() {
        binding.tvDdTitle.text = getString(R.string.device_discovery_enable_bt)
    }

    override fun askBluetoothStart() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, 102)
    }

    override fun onBluetoothEnabled() {
        binding.tvDdTitle.text = getString(R.string.device_discovery_select_bt)
    }

    override fun onDeviceFound(device: BluetoothDevice, type: DeviceType) {
        adapter.addDevice(device, type)
    }

    override fun onDeviceNameChanged(device: BluetoothDevice) {
        adapter.onNameChanged(device)
    }

    inner class DeviceListAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val list: MutableList<DeviceListModel> = mutableListOf()
        private val rowTypeItem = 0
        private val rowTypeHeader = 1
        private var pairedWithEviveItemPos = 0
        private var pairedOtherItemPos = 0


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                DeviceListTitleHolder(LayoutInflater.from(context).inflate(R.layout.row_device_list_title, parent, false))
            } else {
                DeviceListViewHolder(LayoutInflater.from(context).inflate(R.layout.row_device_list, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == 1) {
                (holder as DeviceListTitleHolder).setType(list[position].deviceType)
            } else {
                (holder as DeviceListViewHolder).setData(list[position])
            }
        }

        override fun getItemCount() = list.size

        internal fun onNameChanged(device: BluetoothDevice) {
            list.forEachIndexed { index, deviceListModel ->
                run {

                    if (deviceListModel.device.address == device.address) {
                        deviceListModel.device = device
                        deviceListModel.isNameChanged = true
                        notifyItemChanged(index)
                    }
                }
            }
        }

        internal fun addDevice(device: BluetoothDevice, type: DeviceType) {

            /*if (device.name != "FC:A8:9A:00:0F:03") {
                return
            }*/

            list.forEach {
                if (it.device.address == device.address) {
                    return
                }
            }

            if (type == DeviceType.RECENTLY_PAIRED_WITH_EVIVE) {
                if (pairedWithEviveItemPos == 0) {
                    val model = DeviceListModel(rowTypeHeader, type, device, isNameChanged = false, animateTextColor = false)
                    list.add(pairedWithEviveItemPos, model)
                    notifyItemInserted(pairedWithEviveItemPos)
                    pairedWithEviveItemPos++
                }

                list.add(pairedWithEviveItemPos, DeviceListModel(rowTypeItem, type, device, isNameChanged = false, animateTextColor = false))
                notifyItemInserted(pairedWithEviveItemPos)
                pairedWithEviveItemPos++

            } else {
                if (pairedOtherItemPos == 0) {
                    val model = DeviceListModel(rowTypeHeader, type, device, isNameChanged = false, animateTextColor = false)
                    list.add(pairedWithEviveItemPos + pairedOtherItemPos, model)
                    notifyItemInserted(pairedWithEviveItemPos + pairedOtherItemPos)
                    pairedOtherItemPos++
                }

                list.add(
                    pairedWithEviveItemPos + pairedOtherItemPos,
                    DeviceListModel(rowTypeItem, type, device, isNameChanged = false, animateTextColor = false)
                )
                notifyItemInserted(pairedWithEviveItemPos + pairedOtherItemPos)
                pairedOtherItemPos++

            }

        }

        override fun getItemViewType(position: Int) = list[position].rowType
    }


    inner class DeviceListViewHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener, Animator.AnimatorListener {

        private lateinit var bd: DeviceListModel
        private var binding: RowDeviceListBinding = DataBindingUtil.bind(v)!!

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            searchService?.stopSearch()
            searchService?.stopSelf()
            isExitingWithoutClickingBluetoothDevice = false
            val intent = Intent()
            intent.putExtra(EXTRA_DEVICE_ADDRESS, bd.device.address)

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent)

            finish()
        }

        fun setData(bd: DeviceListModel) {
            this.bd = bd
            binding.deviceRowAddress.text = bd.device.address

            binding.deviceRowTitle.text = if (!TextUtils.isEmpty(bd.device.name)) {
                if (bd.isNameChanged) {
                    binding.deviceRowTitle.setTextColor(nameChangedColor)

                } else {
                    if (bd.animateTextColor) {
                        val colorAnim = ObjectAnimator.ofInt(binding.deviceRowTitle, "textColor", nameChangedColor, nameAlreadyAvailableColor)
                        colorAnim.setEvaluator(ArgbEvaluator())
                        colorAnim.duration = 400
                        colorAnim.addListener(this)
                        colorAnim.start()

                    } else {
                        binding.deviceRowTitle.setTextColor(nameAlreadyAvailableColor)
                    }

                }

                bd.device.name

            } else {
                binding.deviceRowTitle.setTextColor(findingNameColor)
                getString(R.string.device_discovery_acquiring_name)
            }
        }

        override fun onAnimationRepeat(animation: Animator) {

        }

        override fun onAnimationEnd(animation: Animator) {
            bd.animateTextColor = false
        }

        override fun onAnimationCancel(animation: Animator) {

        }

        override fun onAnimationStart(animation: Animator) {

        }
    }

    inner class DeviceListTitleHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val binding = DataBindingUtil.bind<RowDeviceListTitleBinding>(v)

        fun setType(type: DeviceType) {
            when (type) {
                DeviceType.RECENTLY_PAIRED_WITH_EVIVE -> {
                    binding!!.textView2.text = getString(R.string.device_discovery_type_recently_connected)

                }
                else -> {
                    binding!!.textView2.text = getString(R.string.device_discovery_type_nearby_devices)

                }

            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //IF user is exiting this screen without clicking on bt device
        sharedPreferencesManager?.apply {

            if (isExitingWithoutClickingBluetoothDevice) {
                val stack = if (adapter.list.isEmpty()) ConnectStack.ICON_CLICK else ConnectStack.DEVICE_LISTING
                val bundle = Bundle()
                bundle.putString(getString(R.string.analytics_connect_stack_level), stack.value)

                /*FirebaseAnalytics
                    .getInstance(this@DeviceDiscoveryActivity)
                    .logEvent(getString(R.string.analytics_connect_stack), bundle)*/
            }
        }
    }

    private fun registerConnectionStackAnalytics(enabled: Boolean) {
        val bundle = Bundle()
        bundle.putString("from", "dialog")
        bundle.putBoolean("enabled", enabled)
        /*FirebaseAnalytics
            .getInstance(this)
            .logEvent("auto_connect", bundle)*/
    }

    data class DeviceListModel(
        val rowType: Int,
        val deviceType: DeviceType,
        var device: BluetoothDevice,
        var isNameChanged: Boolean,
        var animateTextColor: Boolean,
    )

}

class DeviceDiscoveryVM(
    val activity: AppCompatActivity,
    var isPermissionGranted: Boolean,
    var isGPSEnabled: Boolean,
    val errorMessage: ObservableInt,
    val locationRationalVisibility: ObservableBoolean,
) {
    fun onExternalPlanClick() {
        activity.finish()
    }

    fun onInternalPlanClick() {
        //IGNORE
    }

}
