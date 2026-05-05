package camp.nextstep.gift.common

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import java.time.Clock

@Configuration
@EnableJpaAuditing
class JpaConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
