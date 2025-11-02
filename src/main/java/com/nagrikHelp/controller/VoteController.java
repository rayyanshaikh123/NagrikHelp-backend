package com.nagrikHelp.controller;

import com.nagrikHelp.dto.IssueVoteSummaryDto;
import com.nagrikHelp.dto.VoteRequestDto;
import com.nagrikHelp.model.VoteValue;
import com.nagrikHelp.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/issues/{issueId}/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<IssueVoteSummaryDto> vote(@PathVariable String issueId,
                                                    @Valid @RequestBody VoteRequestDto req,
                                                    @AuthenticationPrincipal UserDetails user) {
        VoteValue value = VoteValue.valueOf(req.getValue().toUpperCase());
        IssueVoteSummaryDto summary = voteService.castVote(issueId, user.getUsername(), value);
        return ResponseEntity.ok(summary);
    }

    @GetMapping
    public ResponseEntity<IssueVoteSummaryDto> summary(@PathVariable String issueId,
                                                       @AuthenticationPrincipal UserDetails user) {
        String uid = user != null ? user.getUsername() : "__anon__";
        return ResponseEntity.ok(voteService.summarize(issueId, uid));
    }
}
