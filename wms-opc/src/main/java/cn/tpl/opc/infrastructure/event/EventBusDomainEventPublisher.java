package cn.tpl.opc.infrastructure.event;

import org.greenrobot.eventbus.EventBus;
import org.springframework.stereotype.Component;

@Component
public class EventBusDomainEventPublisher implements DomainEventPublisher {
    @Override
    public void publish(Object event) {
        EventBus.getDefault().post(event);
    }
}
