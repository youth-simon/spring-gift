package camp.nextstep.gift.common

sealed class DomainException(
    message: String,
) : RuntimeException(message)

class NotFoundException(
    message: String,
) : DomainException(message)

class InvalidStateException(
    message: String,
) : DomainException(message)

class UnauthorizedException(
    message: String,
) : DomainException(message)
