package com.microsoft.notes.ui.notereference

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.microsoft.notes.models.NoteReference
import com.microsoft.notes.models.extensions.getHostUrl
import com.microsoft.notes.models.extensions.getResourceUrlForSPOItem
import com.microsoft.notes.models.extensions.isSPOItem
import com.microsoft.notes.noteslib.NotesLibrary
import com.microsoft.notes.noteslib.R
import com.microsoft.notes.ui.extensions.hide
import com.microsoft.notes.ui.extensions.show
import kotlinx.android.synthetic.main.onenotepagefragment_layout.*

open class OneNotePageFragment : Fragment() {

    private var currentNoteRefLocalId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.onenotepagefragment_layout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeWebView()
    }

    private fun getCurrentNoteRef() = NotesLibrary.getInstance().getNoteReferenceById(currentNoteRefLocalId)

    fun setCurrentNoteRefId(id: String) {
        currentNoteRefLocalId = id
        view?.let {
            val currentNote = getCurrentNoteRef() ?: return
            setNoteReference(currentNote)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        getCurrentNoteRef()?.let { setNoteReference(it) }
    }

    private fun initializeWebView() {
        pageWebView.isFocusableInTouchMode = true
        pageWebView.isFocusable = true

        // WebView settings
        val webViewSettings = pageWebView.settings
        webViewSettings.javaScriptEnabled = true
        webViewSettings.domStorageEnabled = true
        webViewSettings.databaseEnabled = true
        webViewSettings.setAppCacheEnabled(true)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(pageWebView, true)

        pageWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                pageWebView.show()
                fishbowl.hide()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
            }

            override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
                super.onFormResubmission(view, dontResend, resend)
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            @TargetApi(21)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // For SPO pages, we will get a 302 redirect response for OneNote.aspx urls
                // do some safety checks and open these redirects in the same web view
                if (getCurrentNoteRef()?.isSPOItem() == true &&
                    getCurrentNoteRef()?.getHostUrl()?.equals(request?.url?.host, true) == true &&
                    ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && request?.isRedirect == true) || Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                ) {
                    return false
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, request?.url)
                    context?.startActivity(intent)
                    return true
                }
            }
        }
    }

    // Add extra params to open the page in mobile viewer mode
    private fun getUpdatedUrl(page: NoteReference): String? {
        val webUrl: String = page.webUrl ?: return ""
        return "$webUrl&wdOneNoteTeamsEmbedMobileViewer=1"
    }

    private fun onAuthSuccess(token: String, page: NoteReference) {
        val headers = mutableMapOf<String, String>()
        headers["Authorization"] = "Bearer $token"
        getUpdatedUrl(page)?.let { pageWebView?.loadUrl(it, headers) }
    }

    private fun onAuthFailure() {
        fishbowl.text = context?.getString(R.string.sn_generic_error)
    }

    private fun loadPageWithoutAuth(page: NoteReference) {
        getUpdatedUrl(page)?.let { pageWebView.loadUrl(it) }
    }

    private fun loadPageWithAuth(page: NoteReference) {
        NotesLibrary.getInstance().feedAuthProvider?.getAuthTokenForResource(
            NotesLibrary.getInstance().getUserIDForNoteReferenceLocalID(page.localId),
            page.getResourceUrlForSPOItem(),
            { token: String -> onAuthSuccess(token, page) },
            ::onAuthFailure
        ) ?: loadPageWithoutAuth(page)
    }

    private fun setNoteReference(page: NoteReference) {
        fishbowl.text = context?.getString(R.string.sn_loading)
        fishbowl.show()
        pageWebView.hide()

        if (page.isSPOItem()) {
            loadPageWithAuth(page)
        } else {
            loadPageWithoutAuth(page)
        }
    }
}
