package dev.adastratech.mercuryshop.user.application.command;

public record ChangePasswordCommand(String currentPassword, String newPassword) {
}
