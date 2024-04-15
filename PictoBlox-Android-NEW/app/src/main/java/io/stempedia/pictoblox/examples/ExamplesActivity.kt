package io.stempedia.pictoblox.examples

import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableObserver
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.databinding.ActivityExamplesBinding
import io.stempedia.pictoblox.databinding.RowExampleBinding
import io.stempedia.pictoblox.util.PictoBloxAnalyticsEventLogger
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import io.stempedia.pictoblox.web.PictoBloxWebActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale


class ExamplesActivity : AppCompatActivity() {

    private val adapter = ExampleAdapters()
    private val vm = ExamplesActivityVM(this)
    private lateinit var mBinding: ActivityExamplesBinding
    private var commManagerService: CommManagerServiceImpl? = null
    private val TAG = "ExamplesActivity"
    private lateinit var spManager: SPManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UPDATE LOCAL OF APP
        spManager = SPManager(this)
        fetchLocal()
        Log.d(TAG, "${resources.configuration.locale}")


        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_examples)
        mBinding.data = vm


        Log.d(TAG, "${resources.configuration.locale}")
        setSupportActionBar(mBinding.tbExampleList)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.tbExampleList.setNavigationOnClickListener { finish() }

        val spaceCount = if (resources.configuration.smallestScreenWidthDp >= 720) {
            5
        } else {
            4
        }

        mBinding.recyclerView2.layoutManager = GridLayoutManager(this, spaceCount, RecyclerView.VERTICAL, false)
        mBinding.recyclerView2.setHasFixedSize(true)
        mBinding.recyclerView2.adapter = adapter

        bindService(
            Intent(this, CommManagerServiceImpl::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
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

    override fun onDestroy() {
        super.onDestroy()
        vm.onServiceDisconnected()
        commManagerService?.also {
            unbindService(serviceConnection)
        }
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
    override fun onSaveInstanceState(outState: Bundle) {
        val bundle = Bundle()
        Log.e("Save","Saveinstance called")
        bundle.putString("lang","hi")
        super.onSaveInstanceState(bundle)
    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            (service as CommManagerServiceImpl.LocalBinder).getService().apply {

                commManagerService = this
                vm.onServiceConnected(this)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            commManagerService = null
        }
    }


    fun startPictoBloxWebActivity() {
        val intent = Intent(this, PictoBloxWebActivity::class.java)
        startActivity(intent)
    }

    fun setExamples(it: List<ExamplesItemVM>) {
        adapter.data.clear()
        adapter.data.addAll(it)
        adapter.notifyDataSetChanged()
    }

    inner class ExampleAdapters : RecyclerView.Adapter<ExampleViewHolder>() {
        val data = mutableListOf<ExamplesItemVM>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
            val v = layoutInflater.inflate(R.layout.row_example, parent, false)

            return ExampleViewHolder(v)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
            holder.setData(data[position])
        }

    }

    class ExampleViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val mBinding = DataBindingUtil.bind<RowExampleBinding>(v)

        fun setData(examplesItemVM: ExamplesItemVM) {
            mBinding?.data = examplesItemVM
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        vm.vmOnActivityResult(requestCode, resultCode, data)

    }
}


class ExamplesActivityVM(val activity: ExamplesActivity) {

    val isLoadingExamples = ObservableBoolean()
    val isErrorInLoadingExamples = ObservableBoolean()
    val errorMessage = ObservableField<String>()
    var langcode = ""
    val isDownloadingExamples = ObservableBoolean()
    val downloadProgress = ObservableInt()
    private var registration: ListenerRegistration? = null
    private lateinit var spManager: SPManager
    private lateinit var commManagerServiceImpl: CommManagerServiceImpl
    private val compositeDisposable = CompositeDisposable()
    private var downloadDisposable: Disposable? = null

    //For Description of examples
    val SHOW_DESCRIPTION_CODE = 1100
    var shouldProceed = false


    fun onDestroy() {
        registration?.remove()
    }

    fun onServiceConnected(commManagerServiceImpl: CommManagerServiceImpl) {
        this.commManagerServiceImpl = commManagerServiceImpl
        spManager = SPManager(activity)
        compositeDisposable.clear()

        if (!isLoadingExamples.get()) {
            loadExamples()
        }
    }

    fun onServiceDisconnected() {
        compositeDisposable.dispose()
    }

    fun onIgnoreClick() {

    }

    fun onCancelDownloadClicked() {
        downloadDisposable?.also {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
    }
    fun InternetIsConnected(): Boolean {
        (activity.getSystemService(Context.CONNECTIVITY_SERVICE)as ConnectivityManager).let { connection  ->
            connection.activeNetwork?.let {
                return true
            }?:run {
                return false
            }
        }
    }

    private fun loadExamples() {
         CoroutineScope(Dispatchers.IO).launch{
            if (!InternetIsConnected()){
                withContext(Dispatchers.Main){
                    Toast.makeText(activity,activity.getString(R.string.no_internet),Toast.LENGTH_SHORT).show()
                }
                isLoadingExamples.set(false)
            }
        }

        isLoadingExamples.set(true)

        registration = FirebaseFirestore.getInstance().collection("examples").orderBy("index")
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                override fun onEvent(p0: QuerySnapshot?, p1: FirebaseFirestoreException?) {
                    isLoadingExamples.set(false)

                    if (p1 != null) {
                        isErrorInLoadingExamples.set(true)
                        errorMessage.set(p1.message)
                        return
                    }

                    isErrorInLoadingExamples.set(false)

                    p0?.documents?.map {
                        Log.d("ere","${it.get("translations")}")
                        val translations = it.get("translations") as Map<String,String>?
                        var cloudVersion :Int = 0
                        var localVersion :Int = 0
                        try{
                            cloudVersion = (it.get("version") as Long).toInt()
                            localVersion = spManager.getVersionOfExampleFile(it.id)

                        }catch (e: Exception){
                            e.localizedMessage?.let { it1 -> Log.e("error", it1.toString()) }
                        }
                        val lang = spManager.pictobloxLocale.let {
                            if (it.length > 2)  it.substring(it.length-2) else it
                        }
                        ExamplesItemVM(
                            translations?.get(lang) ?: it.getString("name")!! ,
                            it.id,
                            cloudVersion > localVersion,
                            cloudVersion,
                            this@ExamplesActivityVM
                        )

                    }?.also {
                        activity.setExamples(it)
                    }

                }
            })
    }

    fun downloadAndRun(id: String, latestVersion: Int, fileName: String) {
        isDownloadingExamples.set(true)

        downloadDisposable = commManagerServiceImpl.communicationHandler.storageHandler.createExampleFile(id)
            .flatMapObservable { file -> downloadTaskWrapper(file, id) }
            .subscribeWith(object : DisposableObserver<Int>() {
                override fun onComplete() {
                    loadFromLocalAndRun(id, fileName)
                    isDownloadingExamples.set(false)
                    spManager.setVersionOfExampleFile(id, latestVersion)
                }

                override fun onNext(t: Int) {
                    downloadProgress.set(t)
                }

                override fun onError(e: Throwable) {
                    isDownloadingExamples.set(false)
                }

            })

        compositeDisposable.add(downloadDisposable!!)


    }

    fun loadFromLocalAndRun(id: String, fileName: String) {
        compositeDisposable.add(
            commManagerServiceImpl.communicationHandler.loadExample(id, fileName)
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        activity.startPictoBloxWebActivity()
                    }

                    override fun onError(e: Throwable) {
                        PictobloxLogger.getInstance().logException(e)
                    }

                })
        )
    }

    private fun downloadTaskWrapper(file: File, id: String) = Observable.create<Int> { emitter ->

        val ref = FirebaseStorage.getInstance().getReference("example_assets").child(id).child("code.sb3")
            .getFile(file)
            .addOnSuccessListener {
                emitter.onComplete()
            }
            .addOnFailureListener {
                emitter.onError(it)
            }
            .addOnProgressListener {
                val div = (it.bytesTransferred * 100.0) / it.totalByteCount
                PictobloxLogger.getInstance().logd("transfered ${it.bytesTransferred} : Total ${it.totalByteCount}  : percentage $div")
                emitter.onNext(div.toInt())
            }


        emitter.setCancellable {
            ref.cancel()
        }
    }

    fun vmOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        if(requestCode == SHOW_DESCRIPTION_CODE && data!=null){

            shouldProceed = resultCode == RESULT_OK
            if(shouldProceed){
                val shouldDownloadLatestSb3 = data.getBooleanExtra("shouldDownloadLatestSb3",false)
                val id = data.getStringExtra("id")
                val name = data.getStringExtra("name")
                val latestVersion = data.getIntExtra("id",0)
                if(shouldDownloadLatestSb3){
                    downloadAndRun(id!!, latestVersion, name!!)
                }
                else{
                    loadFromLocalAndRun(id!!, name!!)
                }
            }
        }
    }
}

class ExamplesItemVM(
    val name: String,
    val id: String,
    private val shouldDownloadLatestSb3: Boolean,
    val latestVersion: Int,
    private val activityVM: ExamplesActivityVM
) {
    val thumbRef: StorageReference = FirebaseStorage.getInstance().getReference("example_assets").child(id).child("cover.png")

    fun onItemClick() {

        /*val intentShowDescription = Intent(activityVM.activity,ExampleDescriptionActivity::class.java)
        intentShowDescription.putExtra("shouldDownloadLatestSb3",shouldDownloadLatestSb3)
        intentShowDescription.putExtra("id",id)
        intentShowDescription.putExtra("latestVersion",latestVersion)
        intentShowDescription.putExtra("name",name)

        activityVM.activity.startActivityForResult(intentShowDescription, activityVM.SHOW_DESCRIPTION_CODE)

        PictoBloxAnalyticsEventLogger.getInstance().setExampleOpened(name)*/

        if(shouldDownloadLatestSb3){
            activityVM.downloadAndRun(id, latestVersion, name)
        }
        else{
            activityVM.loadFromLocalAndRun(id, name)
        }
    }

}

