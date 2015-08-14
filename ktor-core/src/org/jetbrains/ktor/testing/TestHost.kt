package org.jetbrains.ktor.testing

import com.typesafe.config.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.interception.*
import java.io.*
import kotlin.reflect.*

inline fun withApplication<reified T : Application>(noinline test: TestApplicationHost.() -> Unit) {
    withApplication(T::class, test)
}

fun withApplication(applicationClass: KClass<*>, test: TestApplicationHost.() -> Unit) {
    val testConfig = ConfigFactory.parseMap(
            mapOf(
                    "ktor.deployment.environment" to "test",
                    "ktor.application.class" to applicationClass.qualifiedName
                 ))
    val config = ApplicationConfig(testConfig, SL4JApplicationLog("<Test>"))
    val host = TestApplicationHost(config)
    host.test()
}

data class RequestResult(val requestResult: ApplicationRequestStatus, val response: TestApplicationResponse?)

class TestApplicationHost(val applicationConfig: ApplicationConfig) {
    val application: Application = ApplicationLoader(applicationConfig).application

    fun handleRequest(setup: TestApplicationRequest.() -> Unit): RequestResult {
        val request = TestApplicationRequest(application)
        request.setup()
        val status = application.handle(request)
        return RequestResult(status, request.response)
    }
}

fun TestApplicationHost.handleRequest(method: HttpMethod, uri: String, setup: TestApplicationRequest.() -> Unit = {}): RequestResult {
    return handleRequest {
        this.uri = uri
        this.method = method
        setup()
    }
}


class TestApplicationRequest(override val application: Application) : ApplicationRequest {
    override var requestLine: HttpRequestLine = HttpRequestLine(HttpMethod.Get, "/", "HTTP/1.1")

    var uri: String
        get() = requestLine.uri
        set(value) {
            requestLine = requestLine.copy(uri = value)
        }

    var method: HttpMethod
        get() = requestLine.method
        set(value) {
            requestLine = requestLine.copy(method = value)
        }

    override var body: String = ""

    override val parameters: Map<String, List<String>> get() {
        return queryParameters()
    }

    override val headers = hashMapOf<String, String>()

    var response: TestApplicationResponse? = null
    override val createResponse = Interceptable0<ApplicationResponse> {
        if (response != null)
            throw IllegalStateException("There should be only one response for a single request. Make sure you haven't called response more than once.")
        response = TestApplicationResponse()
        response!!
    }
}

class TestApplicationResponse : ApplicationResponse {
    override val send = Interceptable1<Any, ApplicationRequestStatus> { value ->
        throw UnsupportedOperationException("No known way to stream value $value")
    }

    val headers = hashMapOf<String, String>()
    override val header = Interceptable2<String, String, ApplicationResponse> { name, value ->
        headers.put(name, value)
        this
    }

    public var code: Int = 501
    override val status = Interceptable1<Int, ApplicationResponse> { code ->
        this.code = code
        this
    }

    public var content: String? = null
    override val stream = Interceptable1<OutputStream.() -> Unit, ApplicationRequestStatus> { body ->
        val stream = ByteArrayOutputStream()
        stream.body()
        content = stream.toString()
        ApplicationRequestStatus.Handled
    }
}

class TestApplication(config: ApplicationConfig) : Application(config)