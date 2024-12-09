# neo4jingest

- This Repository Consists of a workable code which fetches a list of users from the Google Workspace using the Admin SDK and ingest it into neo4j database.

- To run the code make sure to place your `credentials.txt` file in the `/src/main/resources` folder and change the neo4j credentials in the source code `/src/main/java/AdminSDKandNeo4jIntegration.java`.Also change the Domain name and ViewType in the Api call in source code accordingly to fetch the usernames. Finally `gradle run` in the source directory to run the program.

- Kindly refer the documentation for further queries.
- [Neo4j Documentation](https://neo4j.com/docs/getting-started/languages-guides/java/java-intro/), 
[Admin-SDK Documentation](https://developers.google.com/admin-sdk/directory/reference/rest/v1/users/list)