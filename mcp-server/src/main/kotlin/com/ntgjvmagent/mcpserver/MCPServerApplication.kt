package com.ntgjvmagent.mcpserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MCPServerApplication

fun main(args: Array<String>) {
    runApplication<MCPServerApplication>(*args)
}
