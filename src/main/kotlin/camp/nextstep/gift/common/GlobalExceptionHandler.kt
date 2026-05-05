package camp.nextstep.gift.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException) = ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message))

    @ExceptionHandler(UnauthorizedException::class)
    fun unauthorized(ex: UnauthorizedException) = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(ex.message))

    @ExceptionHandler(InvalidStateException::class)
    fun invalidState(ex: InvalidStateException) = ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun illegalArgument(ex: IllegalArgumentException) = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message =
            ex.bindingResult.fieldErrors
                .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ErrorResponse(message))
    }
}

data class ErrorResponse(
    val message: String?,
)
