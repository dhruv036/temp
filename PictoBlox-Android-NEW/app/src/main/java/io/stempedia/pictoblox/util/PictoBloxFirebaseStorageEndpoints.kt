package io.stempedia.pictoblox.util

import com.google.firebase.storage.FirebaseStorage

fun fireStorageUserThumb(uid: String) = FirebaseStorage.getInstance()
    .getReference("user_assets")
    .child(uid)
    .child("profile_images")
    .child("thumb.png")


fun fireStorageLinkFile(id: String) = FirebaseStorage.getInstance()
    .getReference("common_link_storage")
    .child("${id}.sb3")
