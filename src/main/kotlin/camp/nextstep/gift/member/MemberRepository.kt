package camp.nextstep.gift.member

import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByKakaoId(kakaoId: Long): Member?
}
