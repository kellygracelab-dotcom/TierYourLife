package com.artiuillab.tieryourlife.core.usage

class RecordingUsage : Usage {
    val steps = mutableListOf<UsageStep>()
    val counted = mutableListOf<String>()

    override fun step(step: UsageStep) {
        steps += step
    }

    override fun count(feature: UsageFeature) {
        counted += feature.wire
    }
}
