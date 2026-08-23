package com.zedge.automation.data

import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Stable Audio account automation — Android equivalent of the Chrome extension's
 * background.js + content.js. Uses a WebView with JavaScript Interface to:
 *
 * 1. Detect existing session (token in localStorage)
 * 2. If logged in → logout + wipe session data
 * 3. Click "Sign up" → fill Auth0 form with random credentials
 * 4. Wait for Auth0 JWT token to appear in localStorage
 * 5. Extract access_token and save to SettingsStore
 *
 * Token lives in localStorage under key pattern: @@auth0spajs@@::...::@@user@@
 * Value structure: { body: { access_token: "eyJ..." } }
 */
class StableAudioAuth(private val settings: SettingsStore) {

    companion object {
        const val STABLE_AUDIO_URL = "https://stableaudio.com"
        const val AUTH0_LOGOUT_URL = "https://login.stableaudio.com/v2/logout"

        /** How long to wait for token extraction (ms) */
        private const val TOKEN_WAIT_TIMEOUT = 45_000L
        /** How long to wait for page load (ms) */
        private const val PAGE_LOAD_TIMEOUT = 30_000L
        /** Poll interval for token check (ms) */
        private const val POLL_INTERVAL = 500L
    }

    // ── Credential generation (same logic as extension) ──

    fun generateRandomEmail(): String {
        val timestamp = System.currentTimeMillis()
        val random = Random.nextBytes(8).joinToString("") { "%02x".format(it) }.take(10)
        val domains = listOf("gmail.com", "yahoo.com", "outlook.com", "hotmail.com")
        val domain = domains.random()
        return "stableaudio_${random}_$timestamp@$domain"
    }

    fun generateStrongPassword(): String {
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val digits = "0123456789"
        val special = "!@#\$%^&*"
        val all = lower + upper + digits + special

        val password = buildString {
            append(lower.random())
            append(lower.random())
            append(upper.random())
            append(upper.random())
            append(digits.random())
            append(digits.random())
            append(special.random())
            append(special.random())
            repeat(4) { append(all.random()) }
        }
        return password.toCharArray().also { it.shuffle() }.joinToString("")
    }

    // ── JavaScript snippets (equivalent to extension's content.js + background.js) ──

    /**
     * Check localStorage for Auth0 token.
     * Returns the access_token string or null.
     */
    private val JS_CHECK_TOKEN = """
        (function() {
            try {
                for (var i = 0; i < localStorage.length; i++) {
                    var key = localStorage.key(i);
                    if (key && (key.indexOf('auth0') !== -1 || key.indexOf('@@auth0spajs@@') !== -1)) {
                        try {
                            var data = JSON.parse(localStorage.getItem(key));
                            if (data && data.body && data.body.access_token) {
                                return data.body.access_token;
                            } else if (data && data.access_token) {
                                return data.access_token;
                            } else if (data && data.idToken) {
                                return data.idToken;
                            }
                        } catch (e) {}
                    }
                }
            } catch (e) {}
            return null;
        })();
    """.trimIndent()

    /**
     * Check if user is logged in (has "Upgrade" link or token).
     * Returns: "TOKEN_FOUND", "LOGGED_IN", "SIGNUP_FOUND", or "NOTHING"
     */
    private val JS_CHECK_STATE = """
        (function() {
            // Check for token
            try {
                for (var i = 0; i < localStorage.length; i++) {
                    var key = localStorage.key(i);
                    if (key && (key.indexOf('auth0') !== -1 || key.indexOf('@@auth0spajs@@') !== -1)) {
                        try {
                            var data = JSON.parse(localStorage.getItem(key));
                            if (data && data.body && data.body.access_token) return 'TOKEN_FOUND';
                            if (data && data.access_token) return 'TOKEN_FOUND';
                            if (data && data.idToken) return 'TOKEN_FOUND';
                        } catch (e) {}
                    }
                }
            } catch (e) {}

            // Check for "Upgrade" link (logged in indicator)
            var links = Array.from(document.querySelectorAll('a'));
            var upgradeLink = links.find(function(l) {
                return l.textContent.trim().toLowerCase() === 'upgrade' && l.getAttribute('href') === '/pricing';
            });
            if (upgradeLink) return 'LOGGED_IN';

            // Check for "Sign up" button
            var buttons = Array.from(document.querySelectorAll('button'));
            var signupBtn = buttons.find(function(b) {
                var t = b.textContent.trim().toLowerCase();
                return t === 'sign up' || t === 'sign-up' || t === 'get started';
            });
            if (signupBtn) return 'SIGNUP_FOUND';

            return 'NOTHING';
        })();
    """.trimIndent()

    /**
     * Click the Sign Up button.
     */
    private val JS_CLICK_SIGNUP = """
        (function() {
            var buttons = Array.from(document.querySelectorAll('button'));
            var btn = buttons.find(function(b) {
                var t = b.textContent.trim().toLowerCase();
                return t === 'sign up' || t === 'sign-up' || t === 'get started';
            });
            if (btn) { btn.click(); return true; }
            return false;
        })();
    """.trimIndent()

    /**
     * Check if Auth0 signup form is loaded (email + password inputs present).
     */
    private val JS_CHECK_AUTH0_FORM = """
        (function() {
            var email = document.querySelector('input[name="email"]');
            var pass = document.querySelector('input[name="password"]');
            return (email !== null && pass !== null);
        })();
    """.trimIndent()

    /**
     * Fill Auth0 signup form with credentials using React-compatible triggers.
     */
    private fun jsFillForm(email: String, password: String): String = """
        (function() {
            var emailInput = document.querySelector('input[name="email"]');
            var passInput = document.querySelector('input[name="password"]');
            if (!emailInput || !passInput) return false;

            var setter = Object.getOwnPropertyDescriptor(
                window.HTMLInputElement.prototype, 'value').set;

            setter.call(emailInput, '$email');
            emailInput.dispatchEvent(new Event('input', {bubbles: true}));
            emailInput.dispatchEvent(new Event('change', {bubbles: true}));

            setter.call(passInput, '$password');
            passInput.dispatchEvent(new Event('input', {bubbles: true}));
            passInput.dispatchEvent(new Event('change', {bubbles: true}));

            return true;
        })();
    """.trimIndent()

    /**
     * Click the Auth0 submit button.
     */
    private val JS_CLICK_SUBMIT = """
        (function() {
            var btn = document.querySelector('button[type="submit"]');
            if (btn && !btn.disabled) { btn.click(); return true; }
            return false;
        })();
    """.trimIndent()

    /**
     * Clear all localStorage and sessionStorage.
     */
    private val JS_CLEAR_STORAGE = """
        (function() {
            try { localStorage.clear(); } catch(e) {}
            try { sessionStorage.clear(); } catch(e) {}
        })();
    """.trimIndent()

    /**
     * Check if page has Sign Up or Login buttons (logged out state).
     */
    private val JS_CHECK_LOGGED_OUT = """
        (function() {
            var btns = Array.from(document.querySelectorAll('button'));
            var hasAuth = btns.some(function(b) {
                var t = b.textContent.trim().toLowerCase();
                return t === 'sign up' || t === 'sign-up' || t === 'log in' || t === 'login';
            });
            return hasAuth;
        })();
    """.trimIndent()

    // ── WebView execution helpers ──

    private suspend fun evaluateJs(webView: WebView, script: String): String? =
        withContext(Dispatchers.Main) {
            val deferred = CompletableDeferred<String?>()
            webView.evaluateJavascript(script) { result ->
                // Remove surrounding quotes from string results
                val cleaned = result?.trim('"')?.let { if (it == "null") null else it }
                deferred.complete(cleaned)
            }
            deferred.await()
        }

    private suspend fun waitAndEvaluate(
        webView: WebView,
        script: String,
        checkInterval: Long = POLL_INTERVAL,
        timeout: Long = PAGE_LOAD_TIMEOUT
    ): String? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val result = evaluateJs(webView, script)
            if (!result.isNullOrEmpty() && result != "null" && result != "false" && result != "NONE") {
                return result
            }
            delay(checkInterval)
        }
        return null
    }

    // ── Core automation flow ──

    /**
     * Main entry point. Call this after WebView has loaded stableaudio.com.
     * Progress updates are sent via [onStatus].
     *
     * Returns AuthResult with token + credentials, or error.
     */
    suspend fun performAutoAccountCreation(
        webView: WebView,
        onStatus: (String) -> Unit
    ): AuthResult = withContext(Dispatchers.Main) {
        try {
            onStatus("Checking session state...")

            // Step 1: Wait for page to be interactive
            delay(2000)

            // Step 2: Check current state
            val state = waitAndEvaluate(webView, JS_CHECK_STATE, timeout = 20_000L)

            when (state) {
                "TOKEN_FOUND", "LOGGED_IN" -> {
                    onStatus("Existing session found. Logging out...")
                    performLogout(webView, onStatus)
                    delay(2000)
                    clickSignupAfterLogout(webView, onStatus)
                }
                "SIGNUP_FOUND" -> {
                    onStatus("Sign up button found. Clicking...")
                    evaluateJs(webView, JS_CLICK_SIGNUP)
                    delay(2000)
                }
                else -> {
                    onStatus("Waiting for page to load...")
                    // Try again with longer wait
                    val retryState = waitAndEvaluate(webView, JS_CHECK_STATE, timeout = 15_000L)
                    if (retryState == "SIGNUP_FOUND") {
                        evaluateJs(webView, JS_CLICK_SIGNUP)
                        delay(2000)
                    } else {
                        throw Exception("Could not find Sign Up button on Stable Audio.")
                    }
                }
            }

            // Step 3: Wait for Auth0 form
            onStatus("Waiting for registration form...")
            val formReady = waitAndEvaluate(webView, JS_CHECK_AUTH0_FORM, timeout = 20_000L)
            if (formReady != "true") {
                throw Exception("Auth0 registration form did not load within 20 seconds.")
            }

            // Step 4: Generate credentials and fill form
            val email = generateRandomEmail()
            val password = generateStrongPassword()
            onStatus("Filling registration form...")

            val filled = evaluateJs(webView, jsFillForm(email, password))
            if (filled != "true") {
                throw Exception("Could not fill registration form fields.")
            }

            delay(500)

            // Step 5: Submit form
            onStatus("Submitting registration...")
            val submitted = evaluateJs(webView, JS_CLICK_SUBMIT)
            if (submitted != "true") {
                throw Exception("Submit button not ready. Please solve CAPTCHA if prompted in the WebView.")
            }

            // Step 6: Wait for token
            onStatus("Waiting for authentication token (solve CAPTCHA in WebView if prompted)...")
            val token = waitAndEvaluate(webView, JS_CHECK_TOKEN, timeout = TOKEN_WAIT_TIMEOUT)

            if (token.isNullOrEmpty() || token == "null") {
                throw Exception("Token was not generated within timeout. If CAPTCHA appeared, solve it in the WebView.")
            }

            // Step 7: Save to SettingsStore
            onStatus("Token extracted! Saving...")
            settings.stableAudioToken = token
            settings.stableAccountEmail = email
            settings.stableAccountPassword = password

            onStatus("Done! Account created and token synced.")
            AuthResult(
                success = true,
                token = token,
                email = email,
                password = password
            )

        } catch (e: Exception) {
            AuthResult(
                success = false,
                error = e.message ?: "Unknown error during account creation."
            )
        }
    }

    /**
     * Nuclear logout — same as extension's performLogoutAndWipeSiteData().
     */
    private suspend fun performLogout(webView: WebView, onStatus: (String) -> Unit) {
        onStatus("Killing session...")

        // Navigate to Auth0 logout endpoint
        webView.loadUrl(AUTH0_LOGOUT_URL)
        delay(3000)

        // Clear all storage
        evaluateJs(webView, JS_CLEAR_STORAGE)

        // Navigate back to Stable Audio
        webView.loadUrl(STABLE_AUDIO_URL)
        delay(3000)

        // Clear again after fresh load
        evaluateJs(webView, JS_CLEAR_STORAGE)

        onStatus("Session wiped. Ready for new account.")
    }

    /**
     * After logout, find and click Sign Up again.
     */
    private suspend fun clickSignupAfterLogout(webView: WebView, onStatus: (String) -> Unit) {
        onStatus("Looking for Sign Up button...")
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 15_000) {
            val clicked = evaluateJs(webView, JS_CLICK_SIGNUP)
            if (clicked == "true") {
                onStatus("Sign Up clicked after logout.")
                delay(2000)
                return
            }
            delay(500)
        }
        throw Exception("Could not find Sign Up button after logout.")
    }

    /**
     * Manual token extraction from an already-logged-in WebView.
     * Use this when the user logs in manually in the WebView.
     */
    suspend fun extractTokenFromWebView(webView: WebView): AuthResult =
        withContext(Dispatchers.Main) {
            val token = evaluateJs(webView, JS_CHECK_TOKEN)
            if (!token.isNullOrEmpty() && token != "null") {
                settings.stableAudioToken = token
                AuthResult(success = true, token = token)
            } else {
                AuthResult(success = false, error = "No token found. Please log in to Stable Audio.")
            }
        }

    /**
     * Logout and clear all saved credentials.
     */
    suspend fun performFullLogout(webView: WebView? = null): AuthResult =
        withContext(Dispatchers.Main) {
            try {
                webView?.let {
                    performLogout(it) { }
                }
                settings.clearStableAudioAccount()
                AuthResult(success = true)
            } catch (e: Exception) {
                settings.clearStableAudioAccount()
                AuthResult(success = true) // Clear local even if WebView logout fails
            }
        }

    /**
     * Auto-recovery: called by StableAudioClient when 401/429 is received.
     * Creates a new account and returns the fresh token.
     */
    suspend fun autoRecoverToken(webView: WebView?, onStatus: (String) -> Unit = {}): String? {
        webView ?: return null
        val result = performAutoAccountCreation(webView, onStatus)
        return if (result.success) result.token else null
    }

    data class AuthResult(
        val success: Boolean,
        val token: String? = null,
        val email: String? = null,
        val password: String? = null,
        val error: String? = null
    )
}
