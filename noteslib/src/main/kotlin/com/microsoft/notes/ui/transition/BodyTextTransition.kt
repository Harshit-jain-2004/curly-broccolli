package com.microsoft.notes.ui.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.transition.Transition
import android.transition.TransitionValues
import android.view.ViewGroup
import android.widget.TextView

class BodyTextTransition(
    private val params: DynamicTransitionParams,
    val toDetails: Boolean
) : Transition() {
    companion object {
        private const val PROPNAME_SCROLLY = "com.microsoft.notes:BodyTextTransition:scrollY"
    }

    override fun getDuration(): Long = params.duration

    override fun captureStartValues(transitionValues: TransitionValues?) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues?) {
        captureValues(transitionValues)
    }

    override fun createAnimator(
        sceneRoot: ViewGroup?,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val textView = endValues.view as TextView
        val scrollYAnimator = ObjectAnimator.ofInt(
            textView, "scrollY",
            startValues.values[PROPNAME_SCROLLY] as Int, endValues.values[PROPNAME_SCROLLY] as Int
        )

        val animator = AnimatorSet()
        animator.playTogether(scrollYAnimator)
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                val transitionValues = if (toDetails) endValues else startValues
                setValues(transitionValues)
            }

            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                setValues(endValues)
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            private fun setValues(@Suppress("UNUSED_PARAMETER") transitionValues: TransitionValues) {
            }
        })
        return animator
    }

    private fun captureValues(transitionValues: TransitionValues?) {
        if (transitionValues == null) return
        val view = transitionValues.view as TextView
        transitionValues.values[PROPNAME_SCROLLY] = view.scrollY
    }
}
