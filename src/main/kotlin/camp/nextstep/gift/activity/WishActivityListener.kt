package camp.nextstep.gift.activity

import camp.nextstep.gift.wish.WishAdded
import camp.nextstep.gift.wish.WishQuantityChanged
import camp.nextstep.gift.wish.WishRemoved
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WishActivityListener(
    private val repository: WishActivityRepository,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onAdded(event: WishAdded) {
        repository.save(WishActivity.added(event.memberId, event.productId, event.quantity, event.occurredAt))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onQuantityChanged(event: WishQuantityChanged) {
        repository.save(
            WishActivity.quantityChanged(event.memberId, event.productId, event.newQuantity, event.occurredAt),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onRemoved(event: WishRemoved) {
        repository.save(WishActivity.removed(event.memberId, event.productId, event.occurredAt))
    }
}
