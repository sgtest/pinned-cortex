package com.cortex.backend.handler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessErrorCodes {
  NO_CODE(0, NOT_IMPLEMENTED, "No code"),
  INCORRECT_CURRENT_PASSWORD(300, BAD_REQUEST, "Current password is incorrect"),
  NEW_PASSWORD_DOES_NOT_MATCH(301, BAD_REQUEST, "The new password does not match"),
  ACCOUNT_LOCKED(302, FORBIDDEN, "User account is locked"),
  ACCOUNT_DISABLED(303, FORBIDDEN, "User account is disabled"),
  BAD_CREDENTIALS(304, FORBIDDEN, "Login and / or Password is incorrect"),

  USER_ALREADY_EXISTS(305, BAD_REQUEST, "User already exists"),

  INVALID_TOKEN(306, BAD_REQUEST, "Invalid token"),

  EMAIL_SENDING_FAILED(307, INTERNAL_SERVER_ERROR, "Failed to send email");

  private final int code;
  private final String description;
  private final HttpStatus httpStatus;

  BusinessErrorCodes(int code, HttpStatus status, String description) {
    this.code = code;
    this.description = description;
    this.httpStatus = status;
  }
}
