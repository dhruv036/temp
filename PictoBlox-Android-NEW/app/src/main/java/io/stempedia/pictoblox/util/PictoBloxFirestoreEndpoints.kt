package io.stempedia.pictoblox.util

import com.google.firebase.firestore.FirebaseFirestore

fun firebaseUserDetail(uid: String) = FirebaseFirestore.getInstance().collection("users").document(uid)

fun firebaseUserCredits(uid: String) = FirebaseFirestore.getInstance().collection("user_credits").document(uid)

fun firebaseNewUserDevice() = FirebaseFirestore.getInstance().collection("user_devices").document()

fun firebaseCurrentUserDevice(deviceId: String) = FirebaseFirestore.getInstance().collection("user_devices").document(deviceId)

fun firebaseGetDefaultCoupon() = FirebaseFirestore.getInstance()
    .collection("static_docs")
    .document("credit_constants")

fun firebaseCreateNewLinkStorageEntry() = FirebaseFirestore.getInstance()
    .collection("user_links")
    .document()

fun firebaseLinkStorageEntry(docId: String) = FirebaseFirestore.getInstance()
    .collection("user_links")
    .document(docId)