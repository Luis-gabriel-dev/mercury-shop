package dev.adastratech.mercuryshop.user.application.command;

public record RegisterCommand(String email, String password, String fullName, String phone) {
}
