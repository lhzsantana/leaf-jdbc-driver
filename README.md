# leaf-jdbc-driver
A lightweight JDBC driver that connects Leaf Agriculture’s APIs to standard SQL-based tools. Enables seamless integration of geospatial and agricultural data with BI platforms, data warehouses, and ETL pipelines. Built for scalability, security, and compatibility with existing JDBC frameworks.
Leaf JDBC Driver (Apache Calcite)
=================================

Lightweight JDBC driver to query Leaf Agriculture's API.

Features
- **SQL validation**: syntax validation via Apache Calcite
- **JWT authentication**: Bearer token support
- **JSON→Rows**: converts API JSON response into a JDBC `ResultSet`

Install (Gradle)
```kotlin
repositories { mavenCentral() }
dependencies { implementation("com.leaf:leaf-jdbc-driver:0.1.0-SNAPSHOT") }
```

Usage
```java
Properties props = new Properties();
props.setProperty("apiPrefix", "https://api.withleaf.io/api/v1");
props.setProperty("token", "<JWT>");

Connection conn = DriverManager.getConnection(
    "jdbc:leaf:", props
);

Statement st = conn.createStatement();
ResultSet rs = st.executeQuery("select * from my_table limit 10");
while (rs.next()) {
  System.out.println(rs.getString(1));
}
```

JDBC URL
- Prefix: `jdbc:leaf:`
- Parameters via connection properties or URL:
  - `apiPrefix`: e.g. `https://api.withleaf.io/api/v1`
  - `token`: JWT Bearer token

Expected API shape
- Endpoint: `{apiPrefix}/pointlake/query?sql=<SQL>`
- Authorization: header `Authorization: Bearer <token>`
- Accepted JSON responses:
  - `{ "columns": ["col1", ...], "rows": [[...], ...] }`, or
  - `{ "data": [ {"col": value, ...}, ... ] }`, or
  - `[ {"col": value, ...}, ... ]`

Limitations
- Basic `Statement` only (no `PreparedStatement`)
- Read-only, forward-only

Easy distribution
- GitHub Packages (Gradle): set `GITHUB_ACTOR`/`GITHUB_TOKEN` and use `com.leaf:leaf-jdbc-driver:<version>`
- Releases: download the `*-all.jar` attached to GitHub Releases (bundles all dependencies)
- JitPack: add repository `https://jitpack.io` and use `com.github.Leaf-Agriculture:leaf-jdbc-driver:<tag>`

Maven Central (OSSRH) publication
- Required repo secrets: `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `SIGNING_KEY` (armored PGP), `SIGNING_PASSWORD`.
- Create a tag `vX.Y.Z` and push. The Release workflow will:
  - Publish to GitHub Packages
  - Publish to Sonatype (staging) and close+release
  - Attach the `*-all.jar` to the GitHub Release