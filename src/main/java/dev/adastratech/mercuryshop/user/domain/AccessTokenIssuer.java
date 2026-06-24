package dev.adastratech.mercuryshop.user.domain;

/** Porta para emissão do access token (JWT assinado). */
public interface AccessTokenIssuer {

    AccessToken issue(User user);
}
