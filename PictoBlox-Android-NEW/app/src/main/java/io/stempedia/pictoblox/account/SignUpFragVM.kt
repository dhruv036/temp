package io.stempedia.pictoblox.account

import android.net.Uri
import androidx.databinding.ObservableField

class SignUpFragVM(val fragment: SignUpDetailFragment) {

    private var accountHelper = AccountHelper()
    private var isActive = false
    var isNewUser = false
    val name = ObservableField<String>()
    val birthdate = ObservableField<String>()
    val profilePic = ObservableField<Uri>()

    fun onAttach(isNewUser: Boolean) {
        this.isNewUser = isNewUser
        isActive = true
        showUserDetail()
    }

    fun onDetach() {
        isActive = false
    }

    private fun showUserDetail() {

        profilePic.set(accountHelper.getUserPic())
        name.set(accountHelper.getUserName())

        accountHelper.getUserBirthday()
            .addOnSuccessListener {
                birthdate.set(it)
            }
    }

    fun onLogOutClicked(){
        accountHelper.signOut()
        fragment.dismiss()
    }
}