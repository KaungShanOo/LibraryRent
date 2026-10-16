package org.kaungshan.libraryrent.member;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.kaungshan.libraryrent.common.ConflictException;
import org.kaungshan.libraryrent.common.NotFoundException;
import org.kaungshan.libraryrent.rental.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final RentalRepository rentalRepository;
    private final Clock clock;

    public List<MemberResponse> findAll() {
        return memberRepository.findAll().stream().map(MemberResponse::from).toList();
    }

    public MemberResponse findById(Long id) {
        return MemberResponse.from(getMember(id));
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        if (memberRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A member with email " + request.email() + " already exists");
        }
        Member member = new Member();
        apply(member, request);
        member.setJoinedOn(LocalDate.now(clock));
        return MemberResponse.from(memberRepository.save(member));
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        Member member = getMember(id);
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflictException("A member with email " + request.email() + " already exists");
        }
        apply(member, request);
        return MemberResponse.from(member);
    }

    @Transactional
    public void delete(Long id) {
        Member member = getMember(id);
        if (rentalRepository.existsByMemberId(id)) {
            throw new ConflictException("Member has rental history and cannot be deleted");
        }
        memberRepository.delete(member);
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new NotFoundException("Member", id));
    }

    private static void apply(Member member, MemberRequest request) {
        member.setName(request.name());
        member.setEmail(request.email());
        member.setPhone(request.phone());
    }
}
