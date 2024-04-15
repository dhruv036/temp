package io.stempedia.pictoblox.projectListing

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.stempedia.pictoblox.BuildConfig
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.databinding.RowProjectItem2Binding
import io.stempedia.pictoblox.settings.MIUI_SHARE_ARRAY
import io.stempedia.pictoblox.settings.ShareForMIUIActivity
import java.io.File


abstract class AbsProjectListFragment : Fragment() {
    protected val adapter = ProjectListAdapter()
    protected var commManagerService: CommManagerServiceImpl? = null
    private val compositeDisposable = CompositeDisposable()

    /*protected abstract fun onPBServiceConnected(commManagerService: CommManagerServiceImpl)
    protected abstract fun onBeforeServiceGetsDisconnected(commManagerService: CommManagerServiceImpl)*/
    abstract fun getViewModel(): AbsProjectListFragmentVM

    abstract fun showError(message: String?)

    fun clearSelection() {
        adapter.clearSelection()
    }

    fun addItemInList(t: ProjectListItemVM) {
        adapter.add(t)
    }

    fun clearList() {
        adapter.clearList()
    }

    fun refreshItem(position: Int) {
        adapter.notifyItemChanged(position)
    }

    fun add(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    fun shareMultipleFiles2(activity: ProjectListActivity, list: List<File>) {
        val intent = Intent(activity, ShareForMIUIActivity::class.java)
            .apply {
                putStringArrayListExtra(MIUI_SHARE_ARRAY, list.map { it.absolutePath } as java.util.ArrayList<String>)
            }

        activity.startActivity(intent)
    }

    fun shareMultipleFiles(activity: ProjectListActivity, list: List<File>) {

        val uris = ArrayList<Uri>()
        list.forEach {
            val uri = FileProvider.getUriForFile(activity.applicationContext, BuildConfig.APPLICATION_ID , it)
            uris.add(uri)
            activity.grantUriPermission("android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        Intent().apply {
            setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            action = Intent.ACTION_SEND_MULTIPLE
            putExtra(Intent.EXTRA_SUBJECT, "PictoBlox Project(s)")
            type = "application/octet-stream"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }.also {
            it.resolveActivityInfo(activity.packageManager, 0)?.apply {
                startActivity(Intent.createChooser(it, "Share ${list.size} PictoBlox project(s)"))
            }
        }


//        val shareIntent = Intent(Intent.ACTION_SEND)
//        shareIntent.type = "image/jpg"
//        val imageUri = FileProvider.getUriForFile(activity.applicationContext, BuildConfig.APPLICATION_ID , qrfile)
//        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
//        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Image")
//        shareIntent.putExtra(Intent.EXTRA_TEXT, "Share QR")
//        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        activity.grantUriPermission("android", imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        activity.startActivity(Intent.createChooser(shareIntent, "Share QR"))

    }


    fun askForConfirmation(size: Int) {
        AlertDialog.Builder(activity)
            .setTitle(activity?.getString(R.string.deleting_files))
            .setMessage(activity?.getString(R.string.deleting_desc)?.let { String.format(it,size) })
            .setPositiveButton(activity?.getString(R.string.yes)?:"Yes") { _, _ -> run { getViewModel().onDeleteConfirmed() } }
            .setNegativeButton(activity?.getString(R.string.no)?: "No", null)
            .show()
    }

    fun showNoFilesToDeleteMessage() {
        Toast.makeText(activity, activity?.getString(R.string.no_file_selected), Toast.LENGTH_LONG).show()
    }

    fun getSelectedFiles(): List<File> {
        return adapter.list.filter { it.isSelected.get() }.map { it.file }
    }

    fun showDeleteErrorMessage(size: Int) {
        Toast.makeText(activity, "Error in deleting $size file(s)", Toast.LENGTH_LONG).show()
    }

    fun showDeleteSuccessMessage(size: Int) {
        Toast.makeText(activity, "SuccessFully deleted $size file(s)", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        commManagerService?.apply {
            getViewModel().refreshData()
            //
            performCheckSequence()
        }
    }

    override fun getUserVisibleHint(): Boolean {
        return super.getUserVisibleHint()
    }

    /*override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            commManagerService?.apply {
                getViewModel().refreshData()
            }
        }
    }*/

/*    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable.clear()
        context?.apply {
            bindService(
                Intent(this, CommManagerServiceImpl::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        commManagerService?.apply {
            //onBeforeServiceGetsDisconnected(this)
            activity?.unbindService(serviceConnection)
        }
    }*/

    override fun onAttach(context: Context) {
        super.onAttach(context)
        compositeDisposable.clear()
        context?.apply {
            bindService(
                Intent(this, CommManagerServiceImpl::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }


    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()
        commManagerService?.apply {
            //onBeforeServiceGetsDisconnected(this)
            activity?.unbindService(serviceConnection)
        }
    }

    fun performCheckSequence() {
        if (adapter.list.find { it.isSelected.get() } == null) {
            getViewModel().disableItemMSelection()
        }

        activity?.also {
            val act = it as ProjectListActivity
            act.getVM().enableLinkCreation(getSelectedFiles().size == 1)
        }

    }

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            (service as CommManagerServiceImpl.LocalBinder).getService().apply {

                //onPBServiceConnected(this)
                commManagerService = this
                activity?.also {
                    getViewModel().onAttached(it as ProjectListActivity, this)
                    getViewModel().refreshData()

                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            commManagerService = null
            getViewModel().resetSelectors()
        }
    }

    inner class ProjectListAdapter : RecyclerView.Adapter<ProjectListVH>() {
        val list = mutableListOf<ProjectListItemVM>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectListVH {
            val v = layoutInflater.inflate(R.layout.row_project_item2, parent, false)

            return ProjectListVH(v)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: ProjectListVH, position: Int) {
            holder.setData(list[position])
        }

        fun clearList() {
            list.clear()
            notifyDataSetChanged()
        }

        fun add(t: ProjectListItemVM) {
            list.add(t)
            adapter.notifyItemInserted(list.size - 1)
        }

        fun clearSelection() {
            list.forEachIndexed { index, vm ->
                run {
                    if (vm.isSelected.get()) {
                        vm.isSelected.set(false)
                        adapter.notifyItemChanged(index)
                    }
                }
            }
        }
    }

    inner class ProjectListVH(v: View) : RecyclerView.ViewHolder(v) {
        private val binding: RowProjectItem2Binding =
            DataBindingUtil.bind<RowProjectItem2Binding>(v)!!
        var item: ProjectListItemVM? = null

        fun setData(vm: ProjectListItemVM) {
            vm.viewHolder = this
            this.item = vm
            binding.data = vm
        }
    }
}