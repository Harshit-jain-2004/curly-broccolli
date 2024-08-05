package com.microsoft.notes.ui.transition.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

fun Animator.onEnd(listener: (Animator) -> Unit): Animator {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            listener(this@onEnd)
        }
    })
    return this
}

fun Animator.accelerateAndDecelerate(): Animator {
    interpolator = FastOutSlowInInterpolator()
    return this
}
