package com.memorycard.service;

import com.memorycard.entity.SubscriptionStatus;
import com.memorycard.entity.User;
import com.memorycard.exception.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Ponto de extensão para bloqueio por assinatura (Stripe / Mercado Pago).
 * Hoje todos os usuários FREE têm acesso completo ao MVP.
 */
@Service
public class SubscriptionService {

    public void ensureActiveSubscription(User user) {
        if (user.getSubscriptionStatus() == SubscriptionStatus.EXPIRED
                || user.getSubscriptionStatus() == SubscriptionStatus.CANCELLED) {
            throw new AccessDeniedException(
                    "Assinatura inativa. Renove seu plano para continuar usando o MemoryCard.");
        }
    }

    public boolean hasPremiumAccess(User user) {
        return user.getSubscriptionStatus() == SubscriptionStatus.ACTIVE;
    }
}
