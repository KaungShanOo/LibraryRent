package org.kaungshan.libraryrent.member;

import java.time.LocalDate;

public record MemberResponse(Long id, String name, String email, String phone, LocalDate joinedOn) {

    static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getPhone(),
                member.getJoinedOn());
    }
}
