package cn.tpl.opc.infrastructure.event;

public interface DomainEventPublisher {
    void publish(Object event);
}
