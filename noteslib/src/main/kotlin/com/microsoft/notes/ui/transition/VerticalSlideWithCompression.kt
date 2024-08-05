package com.microsoft.notes.ui.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.transition.TransitionValues
import android.transition.Visibility
import android.view.Gravity
import android.view.View
import android.view.ViewGroup

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class VerticalSlideWithCompression(
    private val slideEdge: Int,
    private val params: DynamicTransitionParams
) :
    Visibility() {

    companion object {
        private const val PROPNAME_SCREEN_POSITION = "com.microsoft.notes:VerticalSlideWithCompression:screenPosition"
        private const val PROPNAME_HEIGHT = "com.microsoft.notes:VerticalSlideWithCompression:height"
    }

    private val maxTravelDistance: Int
        get() = if (slideEdge == Gravity.TOP) params.slideUpDistance else params.slideDownDistance

    private val containerBounds: Rect get() = params.containerBounds

    override fun getDuration(): Long = params.duration

    override fun onAppear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (endValues == null) {
            return null
        }
        val position = endValues.values[PROPNAME_SCREEN_POSITION] as IntArray
        val height = endValues.values[PROPNAME_HEIGHT] as Int
        val endY = view.translationY
        val startY = getOffscreenY(view, position, height)
        view.translationY = startY

        val anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
        anim.addUpdateListener {
            view.clipBounds = Rect(
                containerBounds.left - position[0],
                containerBounds.top - position[1] - (it.animatedValue as Float).toInt(),
                containerBounds.right - position[0],
                containerBounds.bottom - position[1] - (it.animatedValue as Float).toInt()
            )
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                view.clipBounds = null
            }
        })
        return anim
    }

    override fun onDisappear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null) {
            return null
        }
        val position = startValues.values[PROPNAME_SCREEN_POSITION] as IntArray
        val height = startValues.values[PROPNAME_HEIGHT] as Int
        val startY = view.translationY
        val endY = getOffscreenY(view, position, height)

        val anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
        anim.addUpdateListener {
            view.clipBounds = Rect(
                containerBounds.left - position[0],
                containerBounds.top - position[1] - (it.animatedValue as Float).toInt(),
                containerBounds.right - position[0],
                containerBounds.bottom - position[1] - (it.animatedValue as Float).toInt()
            )
        }

        return anim
    }

    private fun getOffscreenY(view: View, position: IntArray, height: Int): Float {
        return when (slideEdge) {
            Gravity.TOP -> {
                val travelDistance = -position[1] - height + containerBounds.top
                view.translationY + (travelDistance + minOf(maxTravelDistance, travelDistance)) / 2
            }
            Gravity.BOTTOM -> {
                val travelDistance = -position[1] + containerBounds.bottom + params.slideDownOffset
                view.translationY + (travelDistance + maxOf(maxTravelDistance, travelDistance)) / 2
            }
            else -> view.translationY
        }
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
        transitionValues.values[PROPNAME_HEIGHT] = view.height
    }
}
