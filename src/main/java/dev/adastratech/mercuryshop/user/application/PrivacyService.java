package dev.adastratech.mercuryshop.user.application;

import dev.adastratech.mercuryshop.order.domain.OrderRepository;
import dev.adastratech.mercuryshop.shared.application.PageQuery;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import dev.adastratech.mercuryshop.shared.exception.UnauthorizedException;
import dev.adastratech.mercuryshop.shared.security.AuditLogger;
import dev.adastratech.mercuryshop.user.domain.OneTimeTokenRepository;
import dev.adastratech.mercuryshop.user.domain.PasswordHasher;
import dev.adastratech.mercuryshop.user.domain.RefreshTokenStore;
import dev.adastratech.mercuryshop.user.domain.TokenPurpose;
import dev.adastratech.mercuryshop.user.domain.User;
import dev.adastratech.mercuryshop.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Casos de uso de privacidade (LGPD): exportação dos dados pessoais (portabilidade) e exclusão da
 * conta por ANONIMIZAÇÃO — os pedidos (registros financeiros) são mantidos, mas desvinculados de PII.
 */
@Service
public class PrivacyService {

    private static final int EXPORT_ORDER_LIMIT = 1000;

    private final UserRepository users;
    private final OrderRepository orders; // leitura cross-feature apenas para a portabilidade
    private final PasswordHasher passwordHasher;
    private final RefreshTokenStore refreshTokens;
    private final OneTimeTokenRepository tokens;
    private final AuditLogger audit;

    public PrivacyService(UserRepository users, OrderRepository orders, PasswordHasher passwordHasher,
                          RefreshTokenStore refreshTokens, OneTimeTokenRepository tokens, AuditLogger audit) {
        this.users = users;
        this.orders = orders;
        this.passwordHasher = passwordHasher;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public DataExport export(UUID userId) {
        User user = load(userId);
        DataExport.Profile profile = new DataExport.Profile(
                user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.getStatus().name(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                user.isEmailVerified(), user.isMfaEnabled(), user.getCreatedAt());
        PageQuery page = new PageQuery(0, EXPORT_ORDER_LIMIT, "createdAt", PageQuery.Direction.DESC);
        var exportedOrders = orders.findByUserId(userId, page).content().stream()
                .map(order -> new DataExport.Order(
                        order.getId(), order.getStatus().name(), order.getTotal(), order.getCreatedAt(),
                        order.getItems().stream()
                                .map(i -> new DataExport.Item(i.productName(), i.unitPrice(), i.quantity(), i.lineTotal()))
                                .toList()))
                .toList();
        return new DataExport(profile, exportedOrders);
    }

    @Transactional
    public void deleteAccount(UUID userId, String currentPassword) {
        User user = load(userId);
        if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Senha atual inválida");
        }
        String placeholderEmail = "deleted-" + UUID.randomUUID() + "@anonymized.invalid";
        user.anonymize(placeholderEmail, passwordHasher.hash(UUID.randomUUID().toString()));
        users.save(user);
        refreshTokens.revokeAllForUser(userId); // encerra sessões
        for (TokenPurpose purpose : TokenPurpose.values()) {
            tokens.invalidateAll(userId, purpose); // purga tokens de uso único pendentes
        }
        audit.accountDeleted(userId);
    }

    private User load(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
    }
}
