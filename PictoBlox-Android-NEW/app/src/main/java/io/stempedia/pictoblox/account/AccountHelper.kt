package io.stempedia.pictoblox.account

import android.net.Uri
import android.text.TextUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AccountHelper {
    private val auth = FirebaseAuth.getInstance(FirebaseApp.getInstance())
    private val db = FirebaseFirestore.getInstance()
    private val users = db.collection("users")
    private val usersProgress = db.collection("user_progress")
    private val storageReference = FirebaseStorage.getInstance().reference
    private val userAssets = storageReference.child("user_assets")
    private val courseAssets = storageReference.child("course_assets")

    fun getCourseIntroAsset(courseId: String, key: String) = courseAssets.child(courseId).child("intro_assets").child(key)

    fun getCourseAssets(courseId: String): StorageReference = courseAssets.child(courseId).child("assets.zip")


    fun getCourseThumb(courseId: String, key: String) = storageReference.child("course_assets").child(courseId).child(key)

    /*fun getAllCourses(): Query {
        return db.collectionGroup("courses").whereIn(FieldPath.of("version", "status"), listOf("BETA", "WIP"))
            .orderBy(FieldPath.of("version", "date"), Query.Direction.ASCENDING)
    }*/
    fun getAllCourses(): Query {

        return db.collection("courses")
            .whereIn("version.status", listOf("BETA", "WIP"))
            .orderBy("version.date", Query.Direction.ASCENDING)
    }

    fun getAllCourses2(): Query {
        return db.collectionGroup("courses").whereIn("version.status", listOf("BETA", "WIP")).orderBy("version.date", Query.Direction.ASCENDING)
    }

    fun getCourse(courseDocumentId: String) = db.collection("courses").document(courseDocumentId)

    fun getLessons(documentId: String) = db.collection("courses").document(documentId).collection("lessons")

    /**
     * Tried to get as suitable name, if displayName is empty and phone number is present it will be returned.
     */
    fun getUserName(): String {

        return auth.currentUser?.let {
            if (!TextUtils.isEmpty(it.displayName)) {
                it.displayName

            } else if (it.providerId == PhoneAuthProvider.PROVIDER_ID) {
                it.phoneNumber

            } else {
                ""
            }

        } ?: ""

    }

    //TODO in case of no profile pic set, we'll send a default url from here
    fun getUserPic(): Uri? {
        return auth.currentUser?.photoUrl
    }

    fun getUserPic2(userId: String): StorageReference {
        return FirebaseStorage.getInstance().getReference("user_assets").child(userId).child("profile_images").child("thumb.png")
    }


    fun getUserId(): String {
        return auth.currentUser?.uid ?: ""
    }


    fun getUserPhone(): String? {
        return auth.currentUser?.phoneNumber ?: ""
    }

    fun getAuth() = auth

    fun getUserBirthday(): Task<String> {
        val id = auth.currentUser?.uid ?: ""

        return users.document(id).get()
            .continueWith {
                it.result!!.get("birthday").toString()
            }
    }

    fun x() {
        auth.currentUser?.providerId
    }

    fun getAccountDetail(): Task<DocumentSnapshot> {
        val id = auth.currentUser?.uid ?: ""

        /*return users.document(id).get().continueWith {
            val documentSnapshot = it.result ?: throw Exception("User document not found")
            return@continueWith documentSnapshot.toObject(PictobloxUser::class.java)
        }*/

        return users.document(id).get()
    }


    fun signOut() {
        auth.signOut()
    }

    /*fun onAccountClicked() {

        if (isLoggedIn()) {
            FirebaseAuth.getInstance().signOut()
            //callbacks.showAccountDetail()

        } else {

            // Choose authentication providers
            val providers = arrayListOf(
                AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
            )

            val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.ic_dummy_login_icon)
                .build()

            callbacks.promptLoginOption(intent)

        }

        FirebaseAuth.getInstance().currentUser?.also { it ->

            val userDetail = hashMapOf("name" to "Ankit2", "birthdate" to Timestamp(619781369, 0))


            val db = FirebaseFirestore.getInstance()
            db.collection("users").document("Test_ID_${Random.nextLong()}")
                .set(userDetail, SetOptions.merge())
                .addOnSuccessListener {
                    PictobloxLogger.getInstance().logd("User created")
                }
                .addOnFailureListener {
                    it.printStackTrace()
                }

        }
    }*/

    fun isLoggedIn() = auth.currentUser != null

    fun getUserProgress(courseDocumentId: String): DocumentReference {
        val id = auth.currentUser?.uid ?: ""


        //usersProgress.document(id).

        return usersProgress.document(id).collection("courses").document(courseDocumentId)
    }


    fun getUserProgress2(): DocumentReference {
        val id = auth.currentUser?.uid ?: ""
        return usersProgress.document(id)

    }

    fun getUserProgressOfCourse(documentId: String): DocumentReference {
        return getUserProgress2().collection("courses").document(documentId)
    }


}
