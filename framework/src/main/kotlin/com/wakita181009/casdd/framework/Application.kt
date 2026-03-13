package com.wakita181009.casdd.framework

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(
    basePackages = [
        "com.wakita181009.casdd.infrastructure",
        "com.wakita181009.casdd.presentation",
        "com.wakita181009.casdd.framework",
    ],
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
