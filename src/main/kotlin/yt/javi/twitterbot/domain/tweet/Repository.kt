package yt.javi.twitterbot.domain.tweet

interface Repository {
    fun getTweets(listId: Int): List<Tweets>
}