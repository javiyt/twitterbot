package yt.javi.twitterbot.infrastructure.http

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result
import yt.javi.twitterbot.domain.tweet.Tweets
import yt.javi.twitterbot.domain.tweet.Repository
import java.math.BigInteger
import java.net.URL

class TweetRepositoryException(message: String?) : Exception(message)

class TweetRepository(private val baseUrl: String, private val key: String, private val secret: String, private val fuelManager: FuelManager? = null) : Repository {
    private val accessToken: String by lazy { initializeAccessToken() }
    private val manager: FuelManager by lazy { initializeFuelManager() }
    private val mapper: ObjectMapper by lazy { initializeObjectMapper() }

    override fun getTweets(listId: Int): List<Tweets> {
        val response = manager
            .get(listURL, listOf("list_id" to listId))
            .authentication()
            .bearer(accessToken)
            .responseObject<List<Tweet>>(mapper)

        return when (response.third) {
            is Result.Success<*> -> response.third.get().map { Tweets(it.id, it.text, it.entities.urls.map { tweetURL -> URL(tweetURL.expanded_url) }) }
            else -> throw TweetRepositoryException(response.second.responseMessage)
        }
    }

    private fun initializeFuelManager(): FuelManager {
        val manager = fuelManager ?: FuelManager()

        manager.basePath = baseUrl

        return manager
    }

    private fun initializeAccessToken(): String {
        val responseObject = manager
            .post(authURL)
            .header("Authorization", "Basic $key $secret")
            .responseObject<AuthResponse>(mapper)

        return when (responseObject.third) {
            is Result.Success<*> -> responseObject.third.get().accessToken
            else -> throw TweetRepositoryException(responseObject.second.responseMessage)
        }
    }

    private fun initializeObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper().registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE

        return mapper
    }

    data class AuthResponse(var tokenType: String, var accessToken: String)
    data class Tweet(var id: BigInteger, var text: String, var entities: Entities)
    data class Entities(var urls: List<TweetURL>)
    data class TweetURL(var expanded_url: String)

    companion object {
        private const val listURL = "/1.1/lists/statuses.json"
        private const val authURL = "/oauth2/token"
    }
}