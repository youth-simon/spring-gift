package camp.nextstep.gift.activity

import org.springframework.data.jpa.repository.JpaRepository

interface WishActivityRepository : JpaRepository<WishActivity, Long> {
    fun findAllByMemberIdOrderByOccurredAtDesc(memberId: Long): List<WishActivity>
}
