import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.Users;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import static org.neo4j.driver.Values.parameters;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class AdminSDKAndNeo4jIntegration {

    // Google Admin SDK Constants
    private static final String APPLICATION_NAME = "Google Admin SDK Directory API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES =
            Collections.singletonList(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    // Neo4j Constants , Change Here
    private static final String NEO4J_URI = "bolt://localhost:7687";
    private static final String NEO4J_USER = "neo4j";
    private static final String NEO4J_PASSWORD = "password";

    // Credential and Directory Service
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = AdminSDKAndNeo4jIntegration.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static void storeUserInNeo4j(String username) {
        try (var neo4jHandler = new Neo4jDatabaseHandler(NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)) {
            neo4jHandler.storeUsernameInNeo4j(username);
        }
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Directory service =
                new Directory.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                        .setApplicationName(APPLICATION_NAME)
                        .build();

        // Fetch users from Google Admin SDK
        Users result = service.users().list()
                .setDomain("srmist.edu.in") // Change Domain Accordingly
                .setViewType("domain_public") // Change view Type Accordingly
                .setMaxResults(50)
                .execute();
        List<User> users = result.getUsers();

        if (users == null || users.size() == 0) {
            System.out.println("No users found.");
        } else {
            System.out.println("Users:");
            for (User user : users) {
                String username = user.getName().getGivenName();
                System.out.println(username);
                storeUserInNeo4j(username);
            }
        }
    }


    static class Neo4jDatabaseHandler implements AutoCloseable {
        private final Driver driver;

        public Neo4jDatabaseHandler(String uri, String user, String password) {
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        }

        @Override
        public void close() throws RuntimeException {
            driver.close();
        }

        public void storeUsernameInNeo4j(final String username) {
            try (var session = driver.session()) {
                var storedUsername = session.executeWrite(tx -> {
                    var query = "CREATE (u:User {username: $username}) RETURN u";
                    var queryResult = tx.run(query, parameters("username", username));
                    return queryResult.single().get("u").asNode().get("username").asString();
                });
                System.out.println("Stored user: " + storedUsername);
            }
        }

    }
}