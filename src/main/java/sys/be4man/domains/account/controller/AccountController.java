package sys.be4man.domains.account.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sys.be4man.domains.account.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "계정 API", description = "마이페이지 등 계정 정보를 조회하고 관리하는 API입니다.")
public class AccountController {

    private final AccountService accountService;

}
