package io.stempedia.pictoblox.firebase.login

import android.animation.*
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import io.stempedia.pictoblox.R
import io.stempedia.pictoblox.util.PictobloxLogger

class PictoBloxActionButtonAnimHelper(val lifecycle: Lifecycle) {
    private var isAnimationRunning = false
    private var isTransactionPending = false
    private var callback: (() -> Unit)? = null

    fun buttonToProgress(view: View, onSafeCompletionCallback: (() -> Unit)?) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {

            val startColor = ContextCompat.getColor(view.context, R.color.colorPrimary)
            val endColor = ContextCompat.getColor(view.context, R.color.colorAccent)

            val valueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)

            valueAnimator.addUpdateListener {
                view.background.setTint(it.animatedValue as Int)
            }

            val animator = ViewAnimationUtils.createCircularReveal(
                view,
                view.width / 2,
                view.height / 2,
                view.width.toFloat(),
                0f
            )

            val set = AnimatorSet().apply {

                playTogether(valueAnimator, animator)
                interpolator = DecelerateInterpolator()
                duration = 350

            }

            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimationRunning = false

                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        view.visibility = View.INVISIBLE
                        view.background.setTint(startColor)
                        onSafeCompletionCallback?.invoke()
                        if (isTransactionPending) {
                            isTransactionPending = false
                            progressToButton(view, this@PictoBloxActionButtonAnimHelper.callback)
                        }
                    }
                }
            })

            isAnimationRunning = true
            set.start()
        }
    }

    fun progressToButton(view: View, onSafeCompletionCallback: (() -> Unit)?) {
        if (isAnimationRunning) {
            this.callback = onSafeCompletionCallback
            isTransactionPending = true
            return
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {

            val startColor = ContextCompat.getColor(view.context, R.color.colorAccent)
            val endColor = ContextCompat.getColor(view.context, R.color.colorPrimary)

            val valueAnimator = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)

            valueAnimator.addUpdateListener {
                view.background.setTint(it.animatedValue as Int)
            }

            val animator = ViewAnimationUtils.createCircularReveal(
                view,
                view.width / 2,
                view.height / 2,
                0f,
                view.width.toFloat()
            )

            val set = AnimatorSet().apply {

                playTogether(valueAnimator, animator)
                interpolator = AccelerateInterpolator()
                duration = 350

            }

            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        onSafeCompletionCallback?.invoke()
                        view.visibility = View.VISIBLE
                    }
                }
            })

            set.start()
        }
    }
}
