package com.microsoft.notes.ui.transition

import android.animation.TimeInterpolator
import android.annotation.TargetApi
import android.os.Build
import android.transition.Transition
import android.transition.TransitionSet
import com.microsoft.notes.ui.transition.extensions.accelerateAndDecelerate

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
open class DynamicDurationTransitionSet(private val params: DynamicTransitionParams) : TransitionSet() {
    init {
        accelerateAndDecelerate()
        ordering = ORDERING_TOGETHER
    }

    override fun getDuration(): Long = params.duration

    override fun addTransition(transition: Transition): TransitionSet {
        if (interpolator != null) {
            transition.interpolator = interpolator
        }
        return super.addTransition(transition)
    }

    override fun setInterpolator(interpolator: TimeInterpolator?): TransitionSet {
        super.setInterpolator(interpolator)

        if (interpolator != null) {
            val numTransitions = transitionCount
            for (i in 0 until numTransitions) {
                getTransitionAt(i).interpolator = interpolator
            }
        }

        return this
    }
}
