# Leaf JDBC Driver

A lightweight JDBC driver for querying the Leaf Agriculture API using standard SQL.

## Features

- **SQL Validation**: Syntactic validation via Apache Calcite
- **JWT Authentication**: Connection with Bearer token
- **JSON→Rows Transformation**: Converts API JSON responses into `ResultSet`

## Quick Start

### Step 1: Add Dependency

**Gradle:**
```kotlin
repositories { 
    mavenCentral() 
}
dependencies { 
    implementation("com.leaf:leaf-jdbc-driver:0.2.0") 
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.leaf</groupId>
    <artifactId>leaf-jdbc-driver</artifactId>
    <version>0.2.0</version>
</dependency>
```

### Step 2: Configure Connection

Create a connection using JDBC URL and properties:

```java
import java.sql.*;
import java.util.Properties;

// Set connection properties
Properties props = new Properties();
props.setProperty("apiPrefix", "https://api.withleaf.io/api/v1");
props.setProperty("token", "YOUR_JWT_TOKEN_HERE");

// Create connection
Connection conn = DriverManager.getConnection("jdbc:leaf:", props);
```

### Step 3: Execute Queries

```java
// Create statement
Statement stmt = conn.createStatement();

// Execute query
ResultSet rs = stmt.executeQuery(
    "SELECT geometry FROM leaf.pointlake.points TABLESAMPLE(0.3 PERCENT)"
);

// Process results
while (rs.next()) {
    String geometry = rs.getString("geometry");
    System.out.println(geometry);
}

// Clean up
rs.close();
stmt.close();
conn.close();
```

## Complete Example

```java
import java.sql.*;
import java.util.Properties;

public class LeafExample {
    public static void main(String[] args) throws SQLException {
        // 1. Configure connection
        Properties props = new Properties();
        props.setProperty("apiPrefix", "https://api.withleaf.io/api/v1");
        props.setProperty("token", "your-jwt-token-here");
        
        // 2. Connect
        try (Connection conn = DriverManager.getConnection("jdbc:leaf:", props);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT geometry FROM leaf.pointlake.points LIMIT 10"
             )) {
            
            // 3. Process results
            while (rs.next()) {
                System.out.println(rs.getString("geometry"));
            }
        }
    }
}
```

## JDBC URL Format

**Prefix:** `jdbc:leaf:`

**Connection Properties:**
- `apiPrefix` (required): Base API URL, e.g., `https://api.withleaf.io/api/v1`
- `token` (required): JWT Bearer token for authentication

**URL Examples:**
```java
// Using Properties (recommended)
Properties props = new Properties();
props.setProperty("apiPrefix", "https://api.withleaf.io/api/v1");
props.setProperty("token", "your-token");
Connection conn = DriverManager.getConnection("jdbc:leaf:", props);

// Using URL query parameters
Connection conn = DriverManager.getConnection(
    "jdbc:leaf:?apiPrefix=https://api.withleaf.io/api/v1&token=your-token"
);

// Using URL semicolon format
Connection conn = DriverManager.getConnection(
    "jdbc:leaf:apiPrefix=https://api.withleaf.io/api/v1;token=your-token"
);
```

## API Format

**Endpoint:** `{apiPrefix}/services/pointlake/api/v2/query?sqlEngine=SPARK_SQL`

**Method:** `POST`

**Request:**
- Body: SQL query as plain text (Content-Type: `text/plain; charset=utf-8`)
- Authorization: Header `Authorization: Bearer <token>`

**Response Formats Supported:**
- `[ {"col": value, ...}, ... ]` (direct array of objects) - **Primary format**
- `{ "data": [ {"col": value, ...}, ... ] }` (wrapped array)
- `{ "columns": ["col1",...], "rows": [[...], ...] }` (legacy format)

## Limitations

- Only simple `Statement` (no `PreparedStatement` support)
- Read-only, forward-only mode
- No transactions
- No batch operations
- Not JDBC compliant (limited feature set)

## Distribution

### GitHub Packages
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/OWNER/REPO")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### JitPack
```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}
dependencies {
    implementation("com.github.Leaf-Agriculture:leaf-jdbc-driver:TAG")
}
```

### Releases
Download the `*-all.jar` file from releases (contains all dependencies).

## License

[Add your license here]
