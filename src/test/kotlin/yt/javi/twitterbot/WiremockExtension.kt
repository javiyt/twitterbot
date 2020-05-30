package yt.javi.twitterbot

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext


internal class WireMockExtension(port: Int) : WireMockServer(port), BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        start()
    }

    override fun afterEach(context: ExtensionContext) {
        stop()
        resetAll()
    }
}