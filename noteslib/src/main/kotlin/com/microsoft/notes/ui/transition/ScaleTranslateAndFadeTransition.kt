package com.microsoft.notes.ui.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Rect
import android.transition.TransitionValues
import android.transition.Visibility
import android.view.View
import android.view.ViewGroup
import com.microsoft.notes.ui.transition.extensions.minus

class ScaleTranslateAndFadeTransition(
    private val params: DynamicTransitionParams? = null,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scale: Float = 1f,
    val clipBounds: Rect? = null
) :
    Visibility() {

    companion object {
        private const val PROPNAME_SCREEN_POSITION = "com.microsoft.notes:ScaleTranslateAndFadeTransition:screenPosition"
    }

    override fun getDuration(): Long = params?.duration ?: super.getDuration()

    override fun onAppear(
        sceneRoot: ViewGroup?,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        val endX = view.translationX
        val endY = view.translationY
        val startX = view.translationX + dx
        val startY = view.translationY + dy
        val endAlpha = view.alpha
        val endScaleX = view.scaleX
        val endScaleY = view.scaleY
        view.translationX = startX
        view.translationY = startY
        view.alpha = 0f
        view.scaleX = scale * endScaleX
        view.scaleY = scale * endScaleY
        if (clipBounds != null && endValues != null) {
            val viewPosition = endValues.values[PROPNAME_SCREEN_POSITION] as IntArray
            view.clipBounds = clipBounds - viewPosition
        }

        val anim = AnimatorSet()
        anim.playTogether(
            ObjectAnimator.ofFloat(view, View.TRANSLATION_X, startX, endX),
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY),
            ObjectAnimator.ofFloat(view, View.ALPHA, 0f, endAlpha),
            ObjectAnimator.ofFloat(view, View.SCALE_X, view.scaleX, endScaleX),
            ObjectAnimator.ofFloat(view, View.SCALE_Y, view.scaleY, endScaleY)
        )
        return anim
    }

    override fun onDisappear(
        sceneRoot: ViewGroup?,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        val startX = view.translationX
        val startY = view.translationY
        val endX = view.translationX + dx
        val endY = view.translationY + dy
        val endScaleX = scale * view.scaleX
        val endScaleY = scale * view.scaleY
        if (clipBounds != null && startValues != null) {
            val viewPosition = startValues.values[PROPNAME_SCREEN_POSITION] as IntArray
            view.clipBounds = clipBounds - viewPosition
        }

        val anim = AnimatorSet()
        anim.playTogether(
            ObjectAnimator.ofFloat(view, View.TRANSLATION_X, startX, endX),
            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY),
            ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f),
            ObjectAnimator.ofFloat(view, View.SCALE_X, view.scaleX, endScaleX),
            ObjectAnimator.ofFloat(view, View.SCALE_Y, view.scaleY, endScaleY)
        )
        return anim
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        super.captureStartValues(transitionValues)
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        super.captureEndValues(transitionValues)
        captureValues(transitionValues)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        val position = IntArray(2)
        view.getLocationOnScreen(position)
        transitionValues.values[PROPNAME_SCREEN_POSITION] = position
    }
}
