package com.opencircle.passwordreset;

public class PasswordResetAttemptsExceededException extends RuntimeException {
  public PasswordResetAttemptsExceededException(String message) {
    super(message);
  }
}
