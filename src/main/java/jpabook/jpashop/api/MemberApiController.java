package jpabook.jpashop.api;


import java.util.List;
import jakarta.validation.Valid;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;


    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);

        return new CreateMemberResponse(id);
    }


    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);

        return new CreateMemberResponse(id);
    }


    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
            @RequestBody @Valid UpdateMemberRequest request) {

        memberService.update(id, request.getName());

        Member member = memberService.findMember(id);

        return new UpdateMemberResponse(id, member.getName());
    }


    @GetMapping("/api/v1/members")
    public List<Member> getMembersV1() {
        return memberService.findMembers();
    }


    @GetMapping("/api/v2/members")
    public Result<List<MemberDto>> getMembersV2() {

        List<MemberDto> members =
                memberService.findMembers().stream().map(member -> new MemberDto(member.getName())).toList();

        return new Result<>(members.size(), members);
    }


    @Data
    @AllArgsConstructor
    static class Result<T> {

        private int count;
        private T data;
    }


    @Data
    @AllArgsConstructor
    static class MemberDto {

        private String name;
    }


    @Data
    static class CreateMemberResponse {

        private Long id;


        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }


    @Data
    static class CreateMemberRequest {

        private String name;
    }


    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {

        private Long id;
        private String name;
    }


    @Data
    static class UpdateMemberRequest {

        private String name;
    }
}
