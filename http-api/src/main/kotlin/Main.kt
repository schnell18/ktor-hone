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
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
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

val users = Collections.synchronizedMap(
    listOf(
        User("test", "test"),
        User("jack", "jack")
    )
    .associateBy { it.name }
    .toMutableMap()
)

class LoginRegister(val user: String, val password: String)
data class Snippet(val user: String, val text: String)
data class PostSnippet(val snippet: PostSnippet.Text) {
    data class Text(val text: String)
}


val snippets = Collections.synchronizedList(
    mutableListOf(
        Snippet(user = "test", text = "hello"),
        Snippet(user = "test", text = "world")
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
    routing {
        route("/snippets") {
            get {
                call.respond(mapOf("snippets" to synchronized(snippets) { snippets.toList() }))
            }
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    val user = call.principal<UserIdPrincipal>() ?: error("No login")
                    snippets += Snippet(user.name, post.snippet.text)
                    call.respond(mapOf("OK" to true))
                }
            }
        }
        post("/register") {
            val post = call.receive<LoginRegister>()
            val user = users.getOrPut(post.user) { User(post.user, post.password) }
            if (user.password != post.password) error("Invalid credentials")
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
