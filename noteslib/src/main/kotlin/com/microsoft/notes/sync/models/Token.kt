package com.microsoft.notes.sync.models

import android.net.Uri
import com.microsoft.notes.sync.JSON
import java.io.Serializable

sealed class Token : Serializable {
    data class Delta(val token: String) : Token(), Serializable {
        companion object {
            fun fromMap(map: Map<String, Any>): Delta? {
                val token = map.get("token") as? String ?: return null
                return Delta(token)
            }
        }
    }

    data class Skip(val token: String) : Token(), Serializable {
        companion object {
            private const val WSET_SKIP_TOKEN_ARG = "\$skiptoken"
            fun fromMap(map: Map<String, Any>): Skip? {
                val token = map.get("token") as? String ?: return null
                return Skip(token)
            }

            fun fromOdataLink(odataNextLink: String): Skip? {
                val uri: Uri = Uri.parse(odataNextLink)
                val skipToken = uri.getQueryParameter(WSET_SKIP_TOKEN_ARG)
                if (skipToken != null) {
                    return Skip(skipToken)
                }
                return null
            }
        }
    }

    companion object {
        fun fromJSON(json: JSON): Token? {
            val obj = json as? JSON.JObject
            val deltaToken = obj?.get<JSON.JString>("deltaToken")?.let { Delta(it.string) }
            val skipToken = obj?.get<JSON.JString>("skipToken")?.let { Skip(it.string) }
            return deltaToken ?: skipToken
        }

        fun fromWsetResponseJSON(json: JSON): Token? {
            val obj = json as? JSON.JObject
            /*
            Though working set doesn't have delta token and we will never call deltaSync() for meeting notes,
               in order to end the skip token loop and detect we have reached last set of result, returning Delta("")
               so that we can end the skip token loop in OutboundQueueApiRequestHandler.fullSync()
             */
            val oDataNextLink: JSON.JString? = obj?.get<JSON.JString>("@odata.nextLink")
            return if (oDataNextLink == null) Delta("")
            else
                Skip.fromOdataLink(oDataNextLink.string)
        }
    }
}
