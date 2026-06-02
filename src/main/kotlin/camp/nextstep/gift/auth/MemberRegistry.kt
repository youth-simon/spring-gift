package camp.nextstep.gift.auth

import camp.nextstep.gift.member.Member
import camp.nextstep.gift.member.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 카카오 외부 호출과 분리된, 회원 조회/등록만 담당하는 좁은 트랜잭션 경계.
 * 외부 API 응답 지연이 DB 커넥션 점유로 번지지 않도록 AuthService 의 트랜잭션 밖에서 호출된다.
 */
@Service
class MemberRegistry(
    private val memberRepository: MemberRepository,
) {
    @Transactional
    fun findOrRegister(kakaoUser: KakaoUserResponse): Member =
        memberRepository.findByKakaoId(kakaoUser.id)
            ?: memberRepository.save(
                Member.fromKakao(
                    kakaoId = kakaoUser.id,
                    email = kakaoUser.email(),
                    name = kakaoUser.nickname(),
                ),
            )
}
