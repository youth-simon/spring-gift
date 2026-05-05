package camp.nextstep.gift

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SpringGiftApplication

fun main(args: Array<String>) {
    runApplication<SpringGiftApplication>(*args)
}
