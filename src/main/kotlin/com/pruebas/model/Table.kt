package com.pruebas.model

import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document


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