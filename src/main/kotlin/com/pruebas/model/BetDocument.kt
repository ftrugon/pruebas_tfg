package com.pruebas.model

import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "coll_Bets")
data class BetDocument(

    @BsonId
    val _id: String?,

    val tableName: String?,
    val tableId: String?,
    val userId: String,
    val amount : Int,
    val type : String,
) {
}