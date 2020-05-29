package yt.javi.twitterbot.domain.tweet

import java.math.BigInteger
import java.net.URL

data class Tweets(val id: BigInteger, val text: String, val urls: List<URL>)