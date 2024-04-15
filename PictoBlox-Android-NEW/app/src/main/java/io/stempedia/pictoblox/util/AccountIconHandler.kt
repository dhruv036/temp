package io.stempedia.pictoblox.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import io.stempedia.pictoblox.R

interface AccountHandlerCallback {
    fun showSignUpIncompleteAnimation()
    fun dismissSignUpIncompleteAnimationIfRequired()
    fun redirectToSignInProcess()
    fun redirectToProfile()
    fun setAccountImage(bitmap: Bitmap)
}

class AccountIconHandler(val context: Context, val callback: AccountHandlerCallback) {
    //private lateinit var spManager: SPManager
    private var stage1Completed = false
    private var stage2Completed = false

    fun onCreate() {
        //spManager = SPManager(context)
        /*if (FirebaseAuth.getInstance().currentUser != null) {

            firebaseUserDetail(FirebaseAuth.getInstance().currentUser!!.uid)
                .get(Source.CACHE)
                .addOnSuccessListener { documentSnapshot ->

                    stage1Completed = documentSnapshot.getString("email") != null
                    stage2Completed = documentSnapshot.getBoolean("is_secondary_detail_filled") ?: false

                }
        }*/
    }


    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            Handler().postDelayed({
                if (stage1Completed && !stage2Completed) {
                    callback.showSignUpIncompleteAnimation()
                } else {
                    callback.dismissSignUpIncompleteAnimationIfRequired()
                }
            }, 300)
        }
    }

    fun onResume() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            setUserProfileIcon(FirebaseAuth.getInstance().currentUser!!.uid)

            firebaseUserDetail(FirebaseAuth.getInstance().currentUser!!.uid)
                .get(Source.CACHE)
                .addOnSuccessListener { documentSnapshot ->

                    stage1Completed = documentSnapshot.getString("email") != null
                    stage2Completed = documentSnapshot.getBoolean("is_secondary_detail_filled") ?: false

                }


        } else {
            stage1Completed = false
            stage2Completed = false
            setAccountIcon()

        }

    }

    fun onPause() {

    }

    fun onAccountClicked() {
        if (stage1Completed/* && stage2Completed*/) {
            callback.redirectToProfile()

        } else {
            //The Login screen knows what to do
            callback.redirectToSignInProcess()
        }
        /*if (FirebaseAuth.getInstance().currentUser != null) {

        } else {
            //The Login screen knows what to do
            callback.redirectToSignInProcess()
        }*/
    }

    private fun setAccountIcon() {
        Glide.with(context)
            .asBitmap()
            .apply(RequestOptions().circleCrop())
            .load(R.drawable.ic_account3)
            .placeholder(R.drawable.ic_account3)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback.setAccountImage(resource)
                }
            })
    }

    private fun setUserProfileIcon(uid: String) {
        val thumbRef = FirebaseStorage.getInstance().getReference("user_assets").child(uid).child("profile_images").child("thumb.png")
        Glide.with(context)
            .asBitmap()
            .apply(RequestOptions().circleCrop())
            .load(thumbRef)
            .placeholder(R.drawable.ic_account3)
            .into(object : CustomTarget<Bitmap>() {

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    setNoUserThumbIcon()

                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback.setAccountImage(resource)
                }
            })


    }

    private fun setNoUserThumbIcon() {
        Glide.with(context)
            .asBitmap()
            .apply(RequestOptions().circleCrop())
            .load(R.drawable.ic_account3)
            .placeholder(R.drawable.ic_account3)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback.setAccountImage(resource)
                }
            })
    }
}