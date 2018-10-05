import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.auth.principal
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.*

open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

class User(val name: String, val password: String)

class InvalidCredentialsException(message: String) : RuntimeException(message)

val users = Collections.synchronizedMap(
    listOf(
        User("test", "test"),
        User("jack", "jack")
    )
    .associateBy { it.name }
    .toMutableMap()
)

class LoginRegister(val user: String, val password: String)
data class Snippet(val id: Long, val user: String, val text: String)
data class PostSnippet(val snippet: PostSnippet.Text) {
    data class Text(val text: String)
}


val snippets = Collections.synchronizedList(
    mutableListOf(
        Snippet(id = 100, user = "test", text = "hello"),
        Snippet(id = 200, user = "test", text = "world")
    )
)

fun Application.snippet() {
    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT) // Pretty Prints the JSON
        }
    }
    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }

        }
    }
    install(StatusPages) {
        exception<InvalidCredentialsException> { exception ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
    }
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost()
    }
    routing {
        route("/snippets") {
            get {
                call.respond(mapOf("snippets" to synchronized(snippets) { snippets.toList() }))
            }
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    val user = call.principal<UserIdPrincipal>() ?: error("No login")
                    val nextId = ((snippets.maxBy { s -> s.id })?.id ?: 0) + 1
                    snippets += Snippet(nextId, user.name, post.snippet.text)
                    call.respond(mapOf("OK" to true, "id" to nextId))
                }
                delete {
                    val postId = call.receive<Long>()
                    val user = call.principal<UserIdPrincipal>() ?: error("No login")
                    snippets.removeIf { s -> s.id == postId && user.name == s.user }
                    call.respond(mapOf("OK" to true))
                }
            }
        }
        post("/register") {
            val post = call.receive<LoginRegister>()
            val user = users.getOrPut(post.user) { User(post.user, post.password) }
            if (user.password != post.password) throw InvalidCredentialsException("Invalid credentials")
            call.respond(mapOf("token" to simpleJwt.sign(user.name)))
        }

    }
}

fun main(args: Array<String>) {
    val env = applicationEngineEnvironment {
        module {
            snippet()
        }
        // Public API
        connector {
            host = "0.0.0.0"
            port = 8080
        }
    }
    val server = embeddedServer(Netty, env) { }
    server.start(true)
}
