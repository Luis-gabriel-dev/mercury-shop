package dev.adastratech.mercuryshop.shared.messaging;

/** Mensagem de e-mail publicada na fila. {@code payload} carrega o token ou o id do pedido. */
public record EmailMessage(String to, String type, String payload) {

    public static final String TYPE_EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String TYPE_ORDER_CONFIRMATION = "ORDER_CONFIRMATION";
    public static final String TYPE_EMAIL_CHANGE = "EMAIL_CHANGE";
}
