package nl.lekkeratlas.shared.model.user;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;
import nl.lekkeratlas.backendapi.web.authentik.dto.AuthentikUser;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@TableName("app_user")
public class User implements TableEntity {

        @PrimaryKey
        @TableColumn
        private final UUID id;

        @TableColumn
        private final String username;

        @TableColumn(columnName = "display_name")
        private final String displayName;

        @TableColumn
        private final String email;

        @TableColumn(columnName = "is_verified")
        private final Boolean isVerified;

        @TableColumn(columnName = "date_joined")
        private final Instant dateJoined;

        @TableColumn(columnName = "last_updated")
        private final Instant lastUpdated;

        @TableColumn(columnName = "last_login")
        private final Instant lastLogin;

        @TableConstructor
        public User(UUID id, String username, String displayName, String email, Boolean isVerified, Instant dateJoined, Instant lastUpdated, Instant lastLogin) {
                this.id = id;
                this.username = username;
                this.displayName = displayName;
                this.email = email;
                this.isVerified = isVerified;
                this.dateJoined = dateJoined;
                this.lastUpdated = lastUpdated;
                this.lastLogin = lastLogin;
        }

        public User(AuthentikUser authentikUser) throws IOException {
                this(
                        authentikUser.formatUuid(),
                        authentikUser.username(),
                        authentikUser.name(),
                        authentikUser.email(),
                        false,
                        authentikUser.date_joined(),
                        authentikUser.last_updated(),
                        authentikUser.last_login()
                );
        }

        public UUID getId() {
                return id;
        }

        public String getUsername() {
                return username;
        }

        public String getDisplayName() {
                return displayName;
        }

        public String getEmail() {
                return email;
        }

        public Boolean getVerified() {
                return isVerified;
        }

        public Instant getDateJoined() {
                return dateJoined;
        }

        public Instant getLastUpdated() {
                return lastUpdated;
        }

        public Instant getLastLogin() {
                return lastLogin;
        }

        /**
         * This class is used to create a User object with a UUID but without any other fields filled in.
         * This is used to PUT a user into the database without having to fill in all the fields.
         */
        public static User getDummyUser(UUID id) {
                return new User(id,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        }
}
