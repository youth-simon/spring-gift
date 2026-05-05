package camp.nextstep.gift.auth

import camp.nextstep.gift.common.UnauthorizedException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class LoginMemberArgumentResolver(
    private val jwtProvider: JwtProvider,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        if (!parameter.hasParameterAnnotation(LoginMember::class.java)) return false
        val type = parameter.parameterType
        require(type == Long::class.java || type == java.lang.Long::class.java) {
            "@LoginMember can only be applied to Long parameters, but got ${type.name}"
        }
        return true
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val header =
            webRequest.getHeader("Authorization")
                ?: throw UnauthorizedException("missing Authorization header")
        if (!header.startsWith(BEARER_PREFIX)) {
            throw UnauthorizedException("malformed Authorization header")
        }
        val token = header.substring(BEARER_PREFIX.length).trim()
        return jwtProvider.parse(token)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
