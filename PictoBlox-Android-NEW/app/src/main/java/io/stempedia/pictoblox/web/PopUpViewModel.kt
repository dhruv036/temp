package io.stempedia.pictoblox.web

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.databinding.ObservableField
import io.stempedia.pictoblox.connectivity.StorageHandler
import io.stempedia.pictoblox.util.SPManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder


class PopUpViewModel(val activityViewModel: PictoBloxWebViewModelM2 ,val popUpData: PictoBloxWebViewModelM2.PopUpData) {

    val title = ObservableField<String>()
    val btTitle = ObservableField<String>()
    val img = ObservableField<Bitmap>()

    fun clickPopUpButton(){
        Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(popUpData.link)
            activityViewModel.activity.startActivity(this)
        }
    }
    fun closeDialog(){
        activityViewModel.dismissPopUpDialog()
    }

//    fun validateLink(link : String): String{
//      return Uri.parse(link).let {
//           if (it.queryParameterNames.contains()){
//               replaceQueryParam(it.toString(),"utm_medium","Android")
//           }else{
//               it.toString()
//           }
//       }
//    }



    fun ignoreClick() = null

    init {
        title.set(popUpData.title)
        btTitle.set(popUpData.buttonText)

        CoroutineScope(Dispatchers.IO).launch {
            val path = StorageHandler(activityViewModel.activity,
                SPManager(activityViewModel.activity)
            ).popUpsFilesDir().canonicalPath+"/${popUpData.popUpId}.jpeg"
            Log.e("path", " $path ", )
            withContext(Dispatchers.Main){
                BitmapFactory.decodeFile(path,BitmapFactory.Options()).let {
                    Log.e("bitmap", ": $it ", )
                    img.set(it)
                }
            }

        }
    }
}