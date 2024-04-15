package io.stempedia.pictoblox.projectListing


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.connectivity.CommManagerServiceImpl
import io.stempedia.pictoblox.databinding.FragmentLocalProjectBinding
import io.stempedia.pictoblox.util.PictobloxLogger


class LocalProjectFragment : AbsProjectListFragment() {
    private val viewModel = LocalProjectListFragVM(this)
    private lateinit var binding:FragmentLocalProjectBinding

    override fun getViewModel(): AbsProjectListFragmentVM {
        return viewModel
    }

    override fun showError(message: String?) {
        Toast.makeText(activity, "Error : $message", Toast.LENGTH_LONG).show()
        PictobloxLogger.getInstance().logd("$message")
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_local_project, container, false)


        val spaceCount = if (requireActivity().resources.configuration.smallestScreenWidthDp >= 720) {
            5
        } else {
            4
        }

        binding.also {
            it.data = viewModel
            it.rvRecentProjects.layoutManager = GridLayoutManager(activity, spaceCount, RecyclerView.VERTICAL, false)
            it.rvRecentProjects.setHasFixedSize(true)
            it.rvRecentProjects.adapter = adapter

        }

        return binding?.root
    }


}
