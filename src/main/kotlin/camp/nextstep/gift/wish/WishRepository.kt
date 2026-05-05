package camp.nextstep.gift.wish

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface WishRepository : JpaRepository<Wish, Long> {
    @EntityGraph(attributePaths = ["product"])
    fun findAllByMemberId(
        memberId: Long,
        pageable: Pageable,
    ): Page<Wish>

    @EntityGraph(attributePaths = ["product"])
    fun findByMemberIdAndProductId(
        memberId: Long,
        productId: Long,
    ): Wish?

    fun deleteByMemberIdAndProductId(
        memberId: Long,
        productId: Long,
    ): Long
}
