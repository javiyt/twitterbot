package yt.javi.twitterbot.infrastructure.http

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.apache.http.HttpStatus
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.apache.http.HttpStatus.SC_OK
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import yt.javi.twitterbot.WireMockExtension
import yt.javi.twitterbot.domain.tweet.Tweets
import java.math.BigInteger
import java.net.URL

internal class TweetRepositoryTest {
    @JvmField
    @RegisterExtension
    var mockServer = WireMockExtension(8080)

    lateinit var repository: TweetRepository

    @BeforeEach
    internal fun setUp() {
        val fuelManager = FuelManager()
        fuelManager.addRequestInterceptor(LogRequestInterceptor)
        fuelManager.addResponseInterceptor(LogResponseInterceptor)

        repository = TweetRepository("http://localhost:8080", "123", "secret", fuelManager)
        mockServer.resetAll()
    }

    @Nested
    inner class ItShouldFailGettingTweetsWhen {
        @Test
        fun `it fails getting oauth token`() {
            mockServer.stubFor(
                post(urlEqualTo("/oauth2/token"))
                    .withHeader("Authorization", equalTo("Basic 123 secret"))
                    .willReturn(forbidden())
            )

            assertThatExceptionOfType(TweetRepositoryException::class.java)
                .isThrownBy {
                    repository.getTweets(1)
                }
                .withMessage("Forbidden")
            mockServer.verify(1, postRequestedFor(urlEqualTo("/oauth2/token"))
                .withHeader("Authorization", equalTo("Basic 123 secret")))
        }

        @Test
        fun `it fails getting tweets`() {
            mockServer.stubFor(
                post(urlEqualTo("/oauth2/token"))
                    .withHeader("Authorization", equalTo("Basic 123 secret"))
                    .willReturn(okJson("{\"token_type\":\"bearer\",\"access_token\":\"$accessToken\"}"))
            )

            mockServer.stubFor(
                get(urlEqualTo("/1.1/lists/statuses.json?list_id=1"))
                    .withHeader("Authorization", equalTo("Bearer $accessToken"))
                    .willReturn(forbidden())
            )

            assertThatExceptionOfType(TweetRepositoryException::class.java)
                .isThrownBy {
                    repository.getTweets(1)
                }
                .withMessage("Forbidden")
        }
    }

    @Nested
    inner class ItShouldGetTweetsWhen {
        @Test
        fun `get a response from twitter`() {
            mockServer.stubFor(
                post(urlEqualTo("/oauth2/token"))
                    .withHeader("Authorization", equalTo("Basic 123 secret"))
                    .willReturn(
                        okJson("{\"token_type\":\"bearer\",\"access_token\":\"$accessToken\"}")
                    )
            )

            mockServer.stubFor(
                get(urlEqualTo("/1.1/lists/statuses.json?list_id=1"))
                    .withHeader("Authorization", equalTo("Bearer $accessToken"))
                    .willReturn(
                        aResponse()
                            .withStatus(SC_OK)
                            .withHeader("Content-Type", "application/json")
                            .withBodyFile("list_of_tweets.json")
                    )
            )

            val tweets = repository.getTweets(1)

            assertThat(tweets)
                .hasSize(1)
                .element(0)
                .isEqualTo(
                    Tweets(
                        BigInteger.valueOf(1128358947772145672),
                        "Through the Twitter Developer Labs program, we'll soon preview new versions of GET /tweets and GET /users, followed… https://t.co/9i4c5bUUCu",
                        listOf(URL("https://twitter.com/i/web/status/1128358947772145672"))
                    )
                )
        }
    }

    companion object {
        private const val accessToken = "Ns7UHzc3j1lY7PDSWMaSltxL84d0eyZa"
    }
}