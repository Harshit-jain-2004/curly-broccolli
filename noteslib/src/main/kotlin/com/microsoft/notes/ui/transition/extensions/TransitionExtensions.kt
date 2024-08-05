package com.microsoft.notes.ui.transition.extensions

import android.annotation.TargetApi
import android.os.Build
import android.transition.SidePropagation
import android.transition.Transition
import android.transition.TransitionSet
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Transition.addTargets(vararg ids: Int): Transition {
    ids.forEach { addTarget(it) }
    return this
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Transition.addTargets(vararg names: String): Transition {
    names.forEach { addTarget(it) }
    return this
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Transition.accelerate(): Transition {
    interpolator = FastOutLinearInInterpolator()
    return this
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Transition.accelerateOrDecelerate(accelerate: Boolean): Transition {
    if (accelerate) accelerate() else decelerate()
    return this
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Transition.accelerateAndDecelerate(): Transition {
    interpolator = FastOutSlowInInterpolator()
    return this
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Transition.decelerate(): Transition {
    interpolator = LinearOutSlowInInterpolator()
    return this
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun Transition.withSidePropagation(side: Int, speed: Float): Transition {
    val propagation = SidePropagation()
    propagation.setSide(side)
    propagation.setPropagationSpeed(speed)
    this.propagation = propagation
    return this
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun TransitionSet.addTransitions(vararg transitions: Transition): TransitionSet {
    transitions.forEach { addTransition(it) }
    return this
}
