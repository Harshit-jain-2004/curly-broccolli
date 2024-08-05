package com.microsoft.notes.utils.logging

data class TelemetryEventHeaders(
    @JvmField val eventName: String,
    @JvmField val samplingPolicy: SamplingPolicy,
    @JvmField val expirationDate: ExpirationDate,
    @JvmField val severityLevel: SeverityLevel,
    @JvmField val costPriority: CostPriority,
    @JvmField val persistencePriority: PersistencePriority,
    @JvmField val diagnosticLevel: DiagnosticLevel,
    @JvmField val dataCategory: DataCategory
)
