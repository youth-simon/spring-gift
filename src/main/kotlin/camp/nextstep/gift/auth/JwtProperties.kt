package camp.nextstep.gift.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "auth.jwt")
data class JwtProperties(
    val secret: String,
    val expiry: Duration = Duration.ofHours(2),
)
