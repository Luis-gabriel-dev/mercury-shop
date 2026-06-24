package dev.adastratech.mercuryshop.product.application.command;

/** Campos nulos = sem alteração (PATCH parcial). */
public record UpdateCategoryCommand(String name, String description) {
}
