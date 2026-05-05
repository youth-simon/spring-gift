package camp.nextstep.gift.wish

import java.time.Instant

sealed interface WishEvent {
    val memberId: Long
    val productId: Long
    val occurredAt: Instant
}

data class WishAdded(
    override val memberId: Long,
    override val productId: Long,
    val quantity: Int,
    val isNew: Boolean,
    override val occurredAt: Instant = Instant.now(),
) : WishEvent

data class WishQuantityChanged(
    override val memberId: Long,
    override val productId: Long,
    val newQuantity: Int,
    override val occurredAt: Instant = Instant.now(),
) : WishEvent

data class WishRemoved(
    override val memberId: Long,
    override val productId: Long,
    override val occurredAt: Instant = Instant.now(),
) : WishEvent
