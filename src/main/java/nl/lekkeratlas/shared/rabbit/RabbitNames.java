package nl.lekkeratlas.shared.rabbit;

public final class RabbitNames {

    private RabbitNames() {
    }

    public static final String WORK_EXCHANGE = "lekkeratlas.work";
    public static final String WORK_QUEUE = "lekkeratlas.work.queue";
    public static final String WORK_ROUTING_KEY = "work";
}