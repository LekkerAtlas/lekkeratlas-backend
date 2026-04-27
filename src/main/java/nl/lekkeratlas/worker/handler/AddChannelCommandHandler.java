package nl.lekkeratlas.worker.handler;

import nl.lekkeratlas.shared.command.AddChannelCommand;
import nl.lekkeratlas.shared.command.AddVideoCommand;
import nl.lekkeratlas.shared.command.AddVideoSource;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.command.WorkCommandType;
import nl.lekkeratlas.shared.rabbit.WorkCommandProducer;
import nl.lekkeratlas.worker.scraper.ChannelScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Handles channel imports.
 *
 * This handler should only discover videos and enqueue AddVideoCommand messages.
 * The actual metadata scraping belongs in AddVideoCommandHandler.
 */
@Component
public class AddChannelCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(AddChannelCommandHandler.class);

    private final ChannelScraper channelScraper;
    private final WorkCommandProducer workCommandProducer;

    public AddChannelCommandHandler(
            ChannelScraper channelScraper,
            WorkCommandProducer workCommandProducer
    ) {
        this.channelScraper = channelScraper;
        this.workCommandProducer = workCommandProducer;
    }

    public void handle(
            WorkCommandEnvelope envelope,
            AddChannelCommand command
    ) {
        String channelId = command.channelId();

        if (channelId.isEmpty()) {
            throw new AmqpRejectAndDontRequeueException("Channel ID cannot be empty");
        }

        List<String> videoUrls;

        try {
            videoUrls = channelScraper.findVideoUrls(channelId);
        } catch (IOException e) {
            logger.error(
                    "Failed to scrape channel. Marking command as unrecoverable. commandId={}, parentCommandId={}, channelId={}, requestedByUserId={}",
                    envelope.commandId(),
                    envelope.parentCommandId(),
                    channelId,
                    command.requestedByUserId()
            );

            throw new AmqpRejectAndDontRequeueException(
                    "Failed to scrape channel " + channelId,
                    e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            logger.error(
                    "Channel scraping was interrupted. Marking command as unrecoverable. commandId={}, parentCommandId={}, channelId={}, requestedByUserId={}",
                    envelope.commandId(),
                    envelope.parentCommandId(),
                    channelId,
                    command.requestedByUserId()
            );

            throw new AmqpRejectAndDontRequeueException(
                    "Channel scraping was interrupted for channel " + channelId,
                    e
            );
        }

        for (String videoUrl : videoUrls) {
            workCommandProducer.publish(
                    WorkCommandType.ADD_VIDEO,
                    new AddVideoCommand(
                            videoUrl,
                            command.requestedByUserId(),
                            AddVideoSource.DISCOVERED_FROM_CHANNEL
                    ),
                    envelope.commandId()
            );
        }
    }
}