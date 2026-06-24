package dev.adastratech.mercuryshop.user.application.command;

public record ResetPasswordCommand(String token, String newPassword) {
}
