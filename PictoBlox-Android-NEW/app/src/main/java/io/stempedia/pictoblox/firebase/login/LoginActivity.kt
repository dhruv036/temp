package io.stempedia.pictoblox.firebase.login

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindToLifecycle
import com.trello.rxlifecycle4.android.lifecycle.kotlin.bindUntilEvent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.databinding.ActivityLoginBinding
import io.stempedia.pictoblox.util.PictobloxLogger
import io.stempedia.pictoblox.util.SPManager
import io.stempedia.pictoblox.util.createNewAccountEntry
import io.stempedia.pictoblox.util.firebaseUserDetail
import java.util.Locale

class LoginActivity : AppCompatActivity(), LoginFragmentCallbacks {
    private lateinit var mBinding: ActivityLoginBinding
    lateinit var spManager :SPManager

    override fun onCreate(savedInstanceState: Bundle?) {
        spManager = SPManager(this)
        fetchLocal()
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        val viewModel by viewModels<LoginActivityViewModel>()
        mBinding.data = viewModel

        setSupportActionBar(mBinding.tbLogin)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mBinding.tbLogin.setNavigationOnClickListener { finish() }


/*        if (FirebaseAuth.getInstance().currentUser != null) {
            if (spManager.isMinorForLogin) {
                switchToMinorDetailScreen(spManager.emailToBeVerified)

            } else {
                switchToAdultDetailScreen(spManager.emailToBeVerified)

            }

        } else if (!TextUtils.isEmpty(spManager.guardianVerificationId)) {
            val fragment = EmailConsentFragment()

            fragment.arguments = Bundle().apply {
                putBoolean(IS_MINOR, spManager.isMinorForLogin)
            }


            supportFragmentManager.beginTransaction()
                .replace(R.id.login_fragment_container, fragment)
                .commit()

        } else {
            val fragment = LoginWithEmailPasswordFragment()
            fragment.arguments = Bundle().apply {
                putString(FUNCTION, FUNCTION_INTERNAL_SIGN_IN)
            }


            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(
                    R.id.login_fragment_container,
                    fragment
                )
                .commit()
        }*/

        setResult(Activity.RESULT_CANCELED)

        mBinding.loginClickContainer.setOnClickListener {
            val imm: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        viewModel.outputSwitchToAgeQuery
            .bindToLifecycle(this)
            .subscribe {
                switchToAgeQueryFragment()
            }

        viewModel.outputOpenAccountTypeChooser
            .bindToLifecycle(this)
            .subscribe {
                switchToAccountTypeSelect(it)
            }

        viewModel.outputIsMinorSelected
            .bindToLifecycle(this)
            .subscribe {
                switchToStage1(it.second, it.first)
            }

        viewModel.outputSwitchToLogin
            .bindUntilEvent(this, Lifecycle.Event.ON_STOP)
            .subscribe {
                switchToLogin(it)
            }

        viewModel.outputSwitchToMinorSecondStage
            .bindToLifecycle(this)
            .subscribe {
                switchToMinorDetailScreen()
            }

        viewModel.outputSwitchToAdultSecondStage
            .bindToLifecycle(this)
            .subscribe {
                switchToAdultDetailScreen()
            }

        viewModel.outputSwitchToTeacherSecondStage
            .bindToLifecycle(this)
            .subscribe {
                switchToTeacherDetailScreen()
            }

        viewModel.outputSwitchToValidationRemainingScreen
            .bindToLifecycle(this)
            .subscribe {
                switchToEmailValidationFragment(it)
            }

        viewModel.outputShowConnectivityError
            .bindToLifecycle(this)
            .subscribe {
                AlertDialog.Builder(this)
                    .setTitle("No connectivity")
                    .setMessage("Please check your network connection")
                    .setPositiveButton("Go back") { d, _ ->
                        d.dismiss()
                        finish()
                    }
                    .setNegativeButton("Retry") { d, _ ->
                        viewModel.inputOnRetryClicked.onNext(Unit)
                        d.dismiss()

                    }
                    .create()
                    .show()
            }

        viewModel.outputStage2Completed
            .bindToLifecycle(this)
            .subscribe {
                switchToSignUpCompleteScreen(it)
            }

        viewModel.wrapUp
            .bindToLifecycle(this)
            .subscribe {
                finish()
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
    fun fetchLocal() {
        var code = spManager.pictobloxLocale
        var lang = code
//        Log.d(TAG, "fetchLocal: $code")
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


    private fun switchToLogin(bundle: Bundle) {
        val fragment = LoginWithEmailPasswordFragment()
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(
                R.id.login_fragment_container,
                fragment
            )
            .commit()
    }

    private fun switchToStage1(accountType: String, isMinor: Boolean) {
        val fragment = SignUpStage1Fragment()
        fragment.arguments = Bundle().apply {
            putString(ACCOUNT_TYPE, accountType)
            putBoolean(IS_MINOR, isMinor)
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            .commit()
    }

    private fun switchToAccountTypeSelect(accountType: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(
                R.id.login_fragment_container,
                SelectStudentTeacherFragment()
            )
            .commit()
    }

    private fun switchToMailVerificationFragment(isMinor: Boolean) {
        val fragment = EmailConsentFragment()

        fragment.arguments = Bundle().apply {
            putBoolean(IS_MINOR, isMinor)
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            //.addToBackStack(null)
            .commit()
    }


    private fun switchToEmailValidationFragment(bundle: Bundle) {
        val fragment = EmailValidationRemainingFragment()
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            //.addToBackStack(null)
            .commit()
    }

    /*fun switchToAdultSignUpFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.login_fragment_container, AdultSignUpFragment())
            .commit()
    }*/

    override fun switchToAgeQueryFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, AgeQueryFragment())
            //.addToBackStack(null)
            .commit()
    }

    fun switchToMinorDetailScreen() {
        val fragment = MinorPasswordAndAgeAfterConsentFragment()

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            .commit()
    }

    private fun switchToSignUpCompleteScreen(bundle: Bundle) {
        val fragment = SignUpCompletionFragment()
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            .commit()
    }

    fun onSignUpComplete() {
        Toast.makeText(this, "SuccessFully signed up", Toast.LENGTH_LONG).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    fun switchToAdultDetailScreen() {
        val fragment = AdultDetailFragment()

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            .commit()
    }

    private fun switchToTeacherDetailScreen() {
        val fragment = TeacherDetailFragment()

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            .commit()
    }

    override fun onSignInComplete() {
        Toast.makeText(this, "Sign In Successful", Toast.LENGTH_LONG).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    fun switchLoginFragment() {
        val fragment = LoginWithEmailPasswordFragment()
        fragment.arguments = Bundle().apply {
            putString(FUNCTION, FUNCTION_INTERNAL_SIGN_IN)
        }


        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, fragment)
            .commit()
    }

    override fun switchToForgotPwdFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, ForgotPasswordFragment())
            .commit()
    }

    override fun switchToForgotUsernameFragment() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.enter_from_left, R.anim.exit_to_right)
            .replace(R.id.login_fragment_container, ForgotUsernameFragment())
            .commit()
    }
}

@SuppressLint("CheckResult")
class LoginActivityViewModel(application: Application) : AndroidViewModel(application) {

    val isCreatingAccount = ObservableBoolean(false)
    private var accountType: String = ""
    private var isMinor: Boolean = false
    private var email: String = ""

    val inputSwitchToSignUp: PublishSubject<Unit> = PublishSubject.create<Unit>()

    //Only pass account type here if we decide to implement back flow
    val outputOpenAccountTypeChooser: Observable<String> = inputSwitchToSignUp.map { accountType }

    val inputAccountTypeSelected: PublishSubject<String> = PublishSubject.create<String>()

    val outputSwitchToAgeQuery: Observable<String> = inputAccountTypeSelected.filter { it == "TYPE_STUDENT" }

    val inputIsMinor: PublishSubject<Boolean> = PublishSubject.create<Boolean>()

    //val outputIsMinorSelected: Observable<Pair<Boolean, String>> = inputIsMinor.map { isMinor -> Pair(isMinor, accountType) }

    val outputIsMinorSelected: Observable<Pair<Boolean, String>> =
        PublishSubject.merge(
            inputAccountTypeSelected.filter { it == "TYPE_TEACHER" }.map { false },
            inputIsMinor
        )
            .map { isMinor -> Pair(isMinor, accountType) }

    val outputSwitchToLogin: PublishSubject<Bundle> = PublishSubject.create<Bundle>()

    val outputSwitchToMinorSecondStage: PublishSubject<Unit> = PublishSubject.create<Unit>()

    val inputOnStage1Completed: PublishSubject<String> = PublishSubject.create<String>()

    val outputSwitchToValidationRemainingScreen: PublishSubject<Bundle> = PublishSubject.create<Bundle>()

    val inputVerificationComplete: PublishSubject<Unit> = PublishSubject.create<Unit>()

    val outputSwitchToAdultSecondStage: PublishSubject<Unit> = PublishSubject.create<Unit>()

    val outputSwitchToTeacherSecondStage: PublishSubject<Unit> = PublishSubject.create<Unit>()

    val outputShowConnectivityError: BehaviorSubject<Unit> = BehaviorSubject.create<Unit>()

    val inputOnRetryClicked: PublishSubject<Unit> = PublishSubject.create<Unit>()

    private val inputAccountCreated: Single<Boolean> =
        Single.create<Boolean> { emitter -> emitter.onSuccess(FirebaseAuth.getInstance().currentUser != null) }

    val inputStage2Completed: PublishSubject<SignUpCompleteResponse> = PublishSubject.create<SignUpCompleteResponse>()

    val outputStage2Completed: Observable<Bundle> = inputStage2Completed
        .map { res ->
            Bundle().apply {
                putLong("credit", res.creditsAdded ?: 0)
            }
        }

    val wrapUp: PublishSubject<Unit> = PublishSubject.create<Unit>()

    init {
        inputAccountTypeSelected
            .subscribe {
                this.accountType = it
            }

        inputIsMinor
            .subscribe {
                this.isMinor = it
            }

        inputOnStage1Completed
            .subscribe {
                this.email = it
                val bundle = Bundle().apply {
                    putString(EMAIL_TO_VERIFY, email)
                    putParcelable(LAST_EMAIL_TIMESTAMP, Timestamp.now())//in this case we just take current time as precision is not a necessity.
                }
                outputSwitchToValidationRemainingScreen.onNext(bundle)
            }

        inputOnRetryClicked
            .subscribe {
                isCreatingAccount.set(true)
                createNewAccountEntry()
                    .addOnSuccessListener {
                        isCreatingAccount.set(true)
                        SPManager(application).firebaseUserDeviceId = it
                        navigateToStep()
                    }
                    .addOnFailureListener {
                        isCreatingAccount.set(true)
                        outputShowConnectivityError.onNext(Unit)
                    }
            }

        inputAccountCreated
            .subscribe { isAccountCreated ->
                if (isAccountCreated) {
                    navigateToStep()
                } else {

                    //we ignite account creation automatically first time.
                    inputOnRetryClicked.onNext(Unit)
                }
            }

        inputVerificationComplete
            .subscribe {
                when (accountType) {
                    "TYPE_STUDENT" -> {
                        if (isMinor) {
                            outputSwitchToMinorSecondStage.onNext(Unit)

                        } else {
                            outputSwitchToAdultSecondStage.onNext(Unit)
                        }
                    }
                    "TYPE_TEACHER" -> {
                        outputSwitchToTeacherSecondStage.onNext(Unit)
                    }
                }
            }
    }


    private fun navigateToStep() {
        firebaseUserDetail(FirebaseAuth.getInstance().currentUser!!.uid)
            .get(Source.CACHE)
            .addOnSuccessListener { documentSnapshot ->

                email = documentSnapshot.getString("email") ?: ""
                val stage1Completed = !TextUtils.isEmpty(email)
                val isEmailVerified = documentSnapshot.getBoolean("is_verified") ?: false
                val stage2Completed = documentSnapshot.getBoolean("is_secondary_detail_filled") ?: false
                accountType = documentSnapshot.getString("account_type") ?: "TYPE_STUDENT"
                isMinor = documentSnapshot.getBoolean("is_minor") ?: false
                val lastEmailTriggeredTime = documentSnapshot.getTimestamp("last_verification_email_triggered_on") ?: Timestamp.now()

                if (!stage1Completed) {
                    val bundle = Bundle().apply {
                        putString(FUNCTION, FUNCTION_INTERNAL_SIGN_IN)
                    }
                    outputSwitchToLogin.onNext(bundle)
                }

                if (stage1Completed && !isEmailVerified) {
                    val bundle = Bundle().apply {
                        putString(EMAIL_TO_VERIFY, email)
                        putParcelable(LAST_EMAIL_TIMESTAMP, lastEmailTriggeredTime)
                    }
                    outputSwitchToValidationRemainingScreen.onNext(bundle)

                }

                if (stage1Completed && isEmailVerified && !stage2Completed) {

                    when (accountType) {
                        "TYPE_STUDENT" -> {
                            if (isMinor) {
                                outputSwitchToMinorSecondStage.onNext(Unit)

                            } else {
                                outputSwitchToAdultSecondStage.onNext(Unit)
                            }
                        }
                        "TYPE_TEACHER" -> {
                            outputSwitchToTeacherSecondStage.onNext(Unit)
                        }
                    }
                }

            }
            .addOnFailureListener {
                PictobloxLogger.getInstance().logException(it)
            }

    }

}
