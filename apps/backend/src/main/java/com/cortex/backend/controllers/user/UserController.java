package com.cortex.backend.controllers.user;

import com.cortex.backend.controllers.user.dto.ChangePasswordRequest;
import com.cortex.backend.controllers.user.dto.UserResponse;
import com.cortex.backend.entities.user.User;
import com.cortex.backend.services.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final IUserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getUserInfo(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    return ResponseEntity.ok(userService.getUserById(user.getId()).orElseThrow());
  }
  
  
  @PatchMapping("/change-password")
  public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest, Authentication authentication) {
    userService.changePassword(changePasswordRequest, authentication);
    return ResponseEntity.ok().build();
  }


}
