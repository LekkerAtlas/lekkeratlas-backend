package nl.lekkeratlas.backendapi.web.video;

import static io.github.david.auk.fluid.jdbc.components.daos.querying.operator.SingleValueOperator.EQUALS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.components.daos.QueryBuilder;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.backendapi.web.video.dto.CreatorInfo;
import nl.lekkeratlas.backendapi.web.video.dto.SourcePlatform;
import nl.lekkeratlas.backendapi.web.video.dto.VideoPreview;
import nl.lekkeratlas.backendapi.web.video.dto.VideoSource;
import nl.lekkeratlas.shared.model.content.Content;
import nl.lekkeratlas.shared.model.content.hostedcontent.HostedContent;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

        @GetMapping
        public ResponseEntity<List<VideoPreview>> getVideos()
                        throws SQLException, NoSuchFieldException {

                List<VideoPreview> videoPreviews = new ArrayList<>();

                try (Connection connection = Database.getConnection();
                                Dao<Content, UUID> contentDao = DAOFactory.createDAO(
                                                connection,
                                                Content.class)) {

                        List<Content> sortedContent = new QueryBuilder<>(contentDao)
                                        .orderBy(Content.class.getDeclaredField("publishedAt"))
                                        .desc()
                                        .get();

                        for (Content content : sortedContent) {
                                videoPreviews.add(convert(connection, content));
                        }
                }

                return ResponseEntity.ok(videoPreviews);
        }

        private VideoPreview convert(
                        Connection connection,
                        Content content)
                        throws NoSuchFieldException {

                try (Dao<HostedContent, UUID> hostedContentDao = DAOFactory.createDAO(
                                connection,
                                HostedContent.class)) {

                        List<HostedContent> hostedContents = new QueryBuilder<>(hostedContentDao)
                                        .where(
                                                        HostedContent.class.getDeclaredField("content"),
                                                        EQUALS,
                                                        content.id())
                                        .get();

                        return new VideoPreview(
                                        content.id(),
                                        content.title(),
                                        new CreatorInfo(content.creator().id(),
                                                        content.creator().getDisplayName()),
                                        content.type(),
                                        getAllVideoSources(hostedContents));
                }
        }

        private List<VideoSource> getAllVideoSources(
                        List<HostedContent> hostedContents) {

                return hostedContents.stream()
                                .map(hostedContent -> new VideoSource(
                                                hostedContent.externalContentId(),
                                                SourcePlatform.fromCreatorAccountKind(
                                                                hostedContent
                                                                                .creatorAccount()
                                                                                .getAccountKind())))
                                .toList();
        }
}
