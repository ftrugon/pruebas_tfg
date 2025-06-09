package com.pruebas.model

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document

@Serializable
@Document(collection = "coll_Tables")
data class Table(
    @BsonId
    val _id:String?,
    val title: String,
    val desc: String,
    var numPlayers:Int,
    val bigBlind: Int,
) {

}