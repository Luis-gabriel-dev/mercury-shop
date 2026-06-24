package dev.adastratech.mercuryshop.user.application.command;

/** Campos nulos = sem alteração (PATCH parcial). */
public record UpdateProfileCommand(String fullName, String phone) {
}
