package io.stempedia.pictoblox.help

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityHelpBinding
    private val vm = HelpVM(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_help)
        mBinding.data = vm
        setSupportActionBar(mBinding.tbHelp)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        mBinding.tbHelp.setNavigationOnClickListener { finish() }
    }
}
