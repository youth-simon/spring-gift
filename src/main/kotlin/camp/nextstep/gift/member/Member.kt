package camp.nextstep.gift.member

import camp.nextstep.gift.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "members",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_members_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_members_kakao_id", columnNames = ["kakao_id"]),
    ],
    indexes = [Index(name = "idx_members_created_at", columnList = "created_at DESC")],
)
class Member protected constructor(
    email: String,
    name: String,
    kakaoId: Long?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false, length = 100)
    var email: String = email
        protected set

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "kakao_id")
    var kakaoId: Long? = kakaoId
        protected set

    fun renameTo(newName: String) {
        require(newName.isNotBlank()) { "name must not be blank" }
        this.name = newName
    }

    companion object {
        fun fromKakao(
            kakaoId: Long,
            email: String,
            name: String,
        ): Member {
            require(email.isNotBlank()) { "email must not be blank" }
            require(name.isNotBlank()) { "name must not be blank" }
            return Member(email = email, name = name, kakaoId = kakaoId)
        }
    }
}
