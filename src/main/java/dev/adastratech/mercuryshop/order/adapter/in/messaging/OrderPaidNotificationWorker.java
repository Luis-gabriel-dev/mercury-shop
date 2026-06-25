package dev.adastratech.mercuryshop.order.adapter.in.messaging;

import dev.adastratech.mercuryshop.shared.messaging.EmailMessage;
import dev.adastratech.mercuryshop.shared.messaging.OrderPaidEvent;
import dev.adastratech.mercuryshop.shared.messaging.RabbitConfig;
import dev.adastratech.mercuryshop.user.domain.UserRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Worker que, ao receber OrderPaid, publica o e-mail de confirmação do pedido. */
@Component
class OrderPaidNotificationWorker {

    private final UserRepository users;
    private final RabbitTemplate rabbitTemplate;

    OrderPaidNotificationWorker(UserRepository users, RabbitTemplate rabbitTemplate) {
        this.users = users;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.Q_ORDER_PAID_NOTIFICATION)
    void onOrderPaid(OrderPaidEvent event) {
        users.findById(event.userId()).ifPresent(user ->
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_EMAIL,
                        new EmailMessage(user.getEmail(), EmailMessage.TYPE_ORDER_CONFIRMATION,
                                event.orderId().toString())));
    }
}
