package com.deathchat.app.data.api.models

import com.google.gson.annotations.SerializedName

data class CreateReportRequest(
    @SerializedName("reportedSessionId") val reportedSessionId: String,
    @SerializedName("roomId") val roomId: String?,
    @SerializedName("reason") val reason: String
)

data class ReportResponseData(
    @SerializedName("reportId") val reportId: String
)
