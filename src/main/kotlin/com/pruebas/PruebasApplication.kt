package com.pruebas

import com.pruebas.security.RSAKeysProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(RSAKeysProperties::class)
class PruebasApplication

fun main(args: Array<String>) {
	runApplication<PruebasApplication>(*args)
}
