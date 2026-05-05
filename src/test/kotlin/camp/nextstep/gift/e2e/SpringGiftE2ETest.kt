package camp.nextstep.gift.e2e

import camp.nextstep.gift.auth.KakaoAccount
import camp.nextstep.gift.auth.KakaoOAuthClient
import camp.nextstep.gift.auth.KakaoProfile
import camp.nextstep.gift.auth.KakaoTokenResponse
import camp.nextstep.gift.auth.KakaoUserResponse
import camp.nextstep.gift.member.MemberRepository
import camp.nextstep.gift.product.ProductRepository
import camp.nextstep.gift.support.MySqlIntegrationTest
import camp.nextstep.gift.wish.WishRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Pageable
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("e2e")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SpringGiftE2ETest : MySqlIntegrationTest() {
    @TestConfiguration
    class StubConfig {
        @Bean
        @Primary
        fun stubKakaoClient(): KakaoOAuthClient = StubKakaoClient()
    }

    @LocalServerPort private var port: Int = 0

    @Autowired private lateinit var kakao: KakaoOAuthClient

    @Autowired private lateinit var wishRepository: WishRepository

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var productRepository: ProductRepository

    private val http: HttpClient = HttpClient.newHttpClient()
    private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()

    @BeforeEach
    fun cleanDb() {
        wishRepository.deleteAll()
        memberRepository.deleteAll()
        productRepository.deleteAll()
        (kakao as StubKakaoClient).reset(userId = 9001, email = "neo@kakao.com", nickname = "neo")
    }

    @Test
    fun `happy path - kakao login through wish lifecycle`() {
        val login = call("GET", "/api/auth/kakao/callback?code=fake-auth-code")
        assertEquals(200, login.statusCode())
        val loginBody: Map<String, Any> = mapper.readValue(login.body())
        val accessToken = loginBody["accessToken"] as String
        assertTrue(accessToken.split(".").size == 3)
        val memberId = (loginBody["memberId"] as Number).toLong()

        val productCreated =
            call(
                "POST",
                "/api/products",
                body = mapOf("name" to "americano", "price" to 4500, "imageUrl" to "https://x/y.png", "stock" to 10),
            )
        assertEquals(201, productCreated.statusCode())
        assertNotNull(productCreated.headers().firstValue("Location").orElse(null))
        val productBody: Map<String, Any> = mapper.readValue(productCreated.body())
        val productId = (productBody["id"] as Number).toLong()

        val wishUnauth =
            call(
                "POST",
                "/api/wishes",
                body = mapOf("productId" to productId, "quantity" to 1),
            )
        assertEquals(401, wishUnauth.statusCode())

        val wishCreated =
            call(
                "POST",
                "/api/wishes",
                token = accessToken,
                body = mapOf("productId" to productId, "quantity" to 1),
            )
        assertEquals(201, wishCreated.statusCode())
        val wishBody: Map<String, Any> = mapper.readValue(wishCreated.body())
        assertEquals(productId, (wishBody["productId"] as Number).toLong())
        assertEquals(1, (wishBody["quantity"] as Number).toInt())
        assertEquals("americano", wishBody["productName"])

        val wishUpsert =
            call(
                "POST",
                "/api/wishes",
                token = accessToken,
                body = mapOf("productId" to productId, "quantity" to 2),
            )
        assertEquals(201, wishUpsert.statusCode())

        val wishes = wishRepository.findAllByMemberId(memberId, Pageable.unpaged()).content
        assertEquals(1, wishes.size)
        assertEquals(3, wishes.first().quantity)
        val wishId = wishes.first().id!!

        val list = call("GET", "/api/wishes", token = accessToken)
        assertEquals(200, list.statusCode())
        val listBody: List<Map<String, Any>> = mapper.readValue(list.body())
        assertEquals(1, listBody.size)
        assertEquals("americano", listBody[0]["productName"])

        val patch =
            call(
                "PATCH",
                "/api/wishes/$wishId",
                token = accessToken,
                body = mapOf("quantity" to 7),
            )
        assertEquals(204, patch.statusCode())
        assertEquals(7, wishRepository.findById(wishId).get().quantity)

        val delete = call("DELETE", "/api/wishes/products/$productId", token = accessToken)
        assertEquals(204, delete.statusCode())
        assertNull(wishRepository.findByMemberIdAndProductId(memberId, productId))
    }

    @Test
    fun `wish quantity change is denied for another member`() {
        val tokenA = login("aaa", userId = 1111, email = "a@k.com", nickname = "alpha")
        val productId = createProduct(name = "latte", price = 5000, stock = 5)
        val wishId = addWish(tokenA, productId, quantity = 1)

        val tokenB = login("bbb", userId = 2222, email = "b@k.com", nickname = "beta")
        val patch =
            call(
                "PATCH",
                "/api/wishes/$wishId",
                token = tokenB,
                body = mapOf("quantity" to 99),
            )
        assertEquals(401, patch.statusCode())
        assertEquals(1, wishRepository.findById(wishId).get().quantity)
    }

    @Test
    fun `product validation rejects negative price`() {
        val response =
            call(
                "POST",
                "/api/products",
                body = mapOf("name" to "x", "price" to -1, "imageUrl" to "http://x", "stock" to 1),
            )
        assertEquals(400, response.statusCode())
    }

    @Test
    fun `malformed token is rejected with 401`() {
        val response = call("GET", "/api/wishes", token = "not-a-real-token")
        assertEquals(401, response.statusCode())
    }

    @Test
    fun `not found product returns 404`() {
        val response = call("GET", "/api/products/999999")
        assertEquals(404, response.statusCode())
    }

    private fun login(
        code: String,
        userId: Long,
        email: String,
        nickname: String,
    ): String {
        (kakao as StubKakaoClient).reset(userId, email, nickname)
        val response = call("GET", "/api/auth/kakao/callback?code=$code")
        val body: Map<String, Any> = mapper.readValue(response.body())
        return body["accessToken"] as String
    }

    private fun createProduct(
        name: String,
        price: Long,
        stock: Int,
    ): Long {
        val response =
            call(
                "POST",
                "/api/products",
                body = mapOf("name" to name, "price" to price, "imageUrl" to "http://x", "stock" to stock),
            )
        val body: Map<String, Any> = mapper.readValue(response.body())
        return (body["id"] as Number).toLong()
    }

    private fun addWish(
        token: String,
        productId: Long,
        quantity: Int,
    ): Long {
        val response =
            call(
                "POST",
                "/api/wishes",
                token = token,
                body = mapOf("productId" to productId, "quantity" to quantity),
            )
        val body: Map<String, Any> = mapper.readValue(response.body())
        return (body["id"] as Number).toLong()
    }

    private fun call(
        method: String,
        path: String,
        token: String? = null,
        body: Any? = null,
    ): HttpResponse<String> {
        val builder =
            HttpRequest
                .newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        val publisher =
            if (body == null) {
                BodyPublishers.noBody()
            } else {
                BodyPublishers.ofString(mapper.writeValueAsString(body))
            }
        builder.method(method, publisher)
        return http.send(builder.build(), BodyHandlers.ofString())
    }
}

class StubKakaoClient : KakaoOAuthClient {
    private var userId: Long = 0
    private var email: String = ""
    private var nickname: String = ""

    fun reset(
        userId: Long,
        email: String,
        nickname: String,
    ) {
        this.userId = userId
        this.email = email
        this.nickname = nickname
    }

    override fun exchangeToken(authorizationCode: String): KakaoTokenResponse =
        KakaoTokenResponse(accessToken = "stub-access-$userId", tokenType = "bearer")

    override fun loadUser(accessToken: String): KakaoUserResponse =
        KakaoUserResponse(
            id = userId,
            kakaoAccount = KakaoAccount(email = email, profile = KakaoProfile(nickname = nickname)),
        )
}
