package org.jetbrains.ktor.tests.application

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.tests.*
import org.jetbrains.spek.api.*
import org.junit.*

class ApplicationRequestHeaderSpek  {

    Test fun `an application that handles requests to foo`() {
        val testHost = createTestHost()
        on("making an unauthenticated request to /foo") {
            testHost.application.routing {
                get("/foo") {
                    handle {
                        it("should map uri to /foo") {
                            shouldEqual("/foo", uri)
                        }
                        it("should map authorization to empty string") {
                            shouldEqual("", authorization())
                        }
                        it("should return empty string as queryString") {
                            shouldEqual("", queryString())
                        }
                        respond {
                            status(HttpStatusCode.OK)
                            send()
                        }
                    }
                }
            }

            val status = testHost.handleRequest {
                uri = "/foo"
                httpMethod = HttpMethod.Get
                headers.put("Authorization", "")
            }.response?.status

            it("should handle request") {
                shouldEqual(HttpStatusCode.OK.value, status)
            }
        }
    }

    Test fun `an application that handles requests to foo with parameters`() {
        val testHost = createTestHost()
        on("making a request to /foo?key1=value1&key2=value2") {
            testHost.application.routing {
                get("/foo") {
                    handle {
                        it("shoud map uri to /foo?key1=value1&key2=value2") {
                            shouldEqual("/foo?key1=value1&key2=value2", uri)
                        }
                        it("shoud map two parameters key1=value1 and key2=value2") {
                            val params = queryParameters()
                            shouldEqual("value1", params["key1"]?.single())
                            shouldEqual("value2", params["key2"]?.single())
                        }
                        it("should map queryString to key1=value1&key2=value2") {
                            shouldEqual("key1=value1&key2=value2", queryString())
                        }
                        it("should map document to foo") {
                            shouldEqual("foo", document())
                        }
                        it("should map path to /foo") {
                            shouldEqual("/foo", path())
                        }
                        it("should map host to host.name.com") {
                            shouldEqual("host.name.com", host())
                        }
                        it("should map port to 8888") {
                            shouldEqual(8888, port())
                        }
                        respond {
                            status(HttpStatusCode.OK)
                            send()
                        }
                    }
                }
            }

            val status = testHost.handleRequest {
                uri = "/foo?key1=value1&key2=value2"
                httpMethod = HttpMethod.Get
                headers.put("Host", "host.name.com:8888")
            }.response?.status

            it("should handle request") {
                shouldEqual(HttpStatusCode.OK.value, status)
            }
        }
    }

    Test fun `an application that handles requests to root with parameters`() {
        val testHost = createTestHost()
        on("making a request to /?key1=value1&key2=value2") {
            testHost.application.routing {
                get("/") {
                    handle {
                        it("shoud map uri to /?key1=value1&key2=value2") {
                            shouldEqual("/?key1=value1&key2=value2", uri)
                        }
                        it("shoud map two parameters key1=value1 and key2=value2") {
                            val params = queryParameters()
                            shouldEqual("value1", params["key1"]?.single())
                            shouldEqual("value2", params["key2"]?.single())
                        }
                        it("should map queryString to key1=value1&key2=value2") {
                            shouldEqual("key1=value1&key2=value2", queryString())
                        }
                        it("should map document to empty") {
                            shouldEqual("", document())
                        }
                        it("should map path to empty") {
                            shouldEqual("/", path())
                        }
                        respond {
                            status(HttpStatusCode.OK)
                            send()
                        }
                    }
                }
            }

            val status = testHost.handleRequest {
                uri = "/?key1=value1&key2=value2"
                httpMethod = HttpMethod.Get
            }.response?.status

            it("should handle request") {
                shouldEqual(HttpStatusCode.OK.value, status)
            }
        }
    }

}

