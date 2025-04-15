package com.pruebas.model

import org.bson.codecs.pojo.annotations.BsonId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "coll_Usuarios")
data class Usuario(
    @BsonId
    val _id : String?,
    @Indexed(unique = true)
    val username: String,
    var password: String,
    @Indexed(unique = true)
    val email: String,
    val roles: String? = "USER"
) {
}