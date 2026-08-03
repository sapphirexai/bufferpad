package cn.tpl.opc.infrastructure.event;

import org.greenrobot.eventbus.EventBus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EventBusDomainEventPublisher implements DomainEventPublisher {
    @Override
    public void publish(Object event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    EventBus.getDefault().post(event);
                }
            });
            return;
        }
        EventBus.getDefault().post(event);
    }
}
