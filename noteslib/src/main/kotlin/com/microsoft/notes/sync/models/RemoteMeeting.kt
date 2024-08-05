package com.microsoft.notes.sync.models

import com.microsoft.notes.sync.JSON
import com.microsoft.notes.sync.filterOutNulls
import com.microsoft.notes.utils.utils.parseISO8601StringToMillis
import java.io.Serializable

data class RemoteMeeting(
    val id: String,
    val subject: String,
    val iCalUId: String,
    val startTime: Long,
    val endTime: Long,
    val organizer: Organizer,
    val type: String,
    val seriesMasterId: String
) : Serializable {

    companion object {
        private const val ID = "Id"
        private const val SUBJECT = "Subject"
        private const val I_CAL_UID = "iCalUId"
        private const val START_TIME = "Start"
        private const val END_TIME = "End"
        private const val ORGANIZER = "Organizer"
        private const val EMAIL_ADDRESS = "EmailAddress"
        private const val DATE_TIME = "DateTime"
        private const val TYPE = "Type"
        private const val SERIES_MASTER_ID = "SeriesMasterId"

        fun fromJSON(json: JSON): RemoteMeeting? {
            val obj = json as? JSON.JObject ?: return null
            val id = obj.get<JSON.JString>(ID)?.string ?: return null
            val subject = obj.get<JSON.JString>(SUBJECT)?.string ?: return null
            val iCalUId = obj.get<JSON.JString>(I_CAL_UID)?.string ?: return null
            val seriesMasterId = obj.get<JSON.JString>(SERIES_MASTER_ID)?.string ?: return null
            val type = obj.get<JSON.JString>(TYPE)?.string ?: return null

            val meetingStartData = obj.get<JSON.JObject>(START_TIME) ?: return null
            val meetingStartTime =
                meetingStartData.get<JSON.JString>(DATE_TIME)?.string ?: return null
            val meetingStartIso8601Time =
                meetingStartTime.substring(0, meetingStartTime.length - 1).plus("Z")
            val meetingStartTimeLong: Long = parseISO8601StringToMillis(meetingStartIso8601Time)

            val meetingEndData = obj.get<JSON.JObject>(END_TIME) ?: return null
            val meetingEndTime = meetingEndData.get<JSON.JString>(DATE_TIME)?.string ?: return null
            val meetingEndIso8601Time =
                meetingEndTime.substring(0, meetingEndTime.length - 1).plus("Z")
            val meetingEndTimeLong: Long = parseISO8601StringToMillis(meetingEndIso8601Time)

            val organizerData = obj.get<JSON.JObject>(ORGANIZER) ?: return null
            val organizer = Organizer.fromJSON(
                organizerData.get<JSON.JObject>(EMAIL_ADDRESS) ?: return null
            ) ?: return null

            return RemoteMeeting(
                id = id,
                subject = subject,
                iCalUId = iCalUId,
                startTime = meetingStartTimeLong,
                endTime = meetingEndTimeLong,
                organizer = organizer,
                type = type,
                seriesMasterId = seriesMasterId
            )
        }
    }
}

data class Organizer(
    val email: String,
    val name: String
) : Serializable {

    companion object {
        private const val NAME = "Name"
        private const val EMAIL = "Address"

        fun fromJSON(json: JSON.JObject): Organizer? {
            val email = json.get<JSON.JString>(EMAIL)?.string ?: return null
            val name = json.get<JSON.JString>(NAME)?.string ?: return null

            return Organizer(
                email,
                name
            )
        }
    }
}

data class MeetingsResponse<out RemoteMeeting>(val value: List<RemoteMeeting>) {
    companion object {
        fun fromJSON(json: JSON): MeetingsResponse<RemoteMeeting>? {
            val obj = json as? JSON.JObject
            val valueParser = (RemoteMeeting)::fromJSON
            val value = obj?.get<JSON.JArray>("value")?.toList()?.map { valueParser(it) }?.filterOutNulls()

            return if (value != null) {
                MeetingsResponse(value = value)
            } else {
                null
            }
        }
    }
}
