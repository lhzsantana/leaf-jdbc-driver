# Leaf JDBC Driver

A lightweight JDBC driver for querying the Leaf Agriculture API using standard SQL.

## Features

- **Automatic Authentication**: Login with username and password - no need to manage tokens
- **SQL Validation**: Syntactic validation via Apache Calcite
- **JSON→Rows Transformation**: Converts API JSON responses into `ResultSet`

## Quick Start

### Step 1: Add Dependency

**Gradle:**
```kotlin
repositories { 
    mavenCentral() 
}
dependencies { 
    implementation("com.leaf:leaf-jdbc-driver:0.2.2") 
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.leaf</groupId>
    <artifactId>leaf-jdbc-driver</artifactId>
    <version>0.2.2</version>
</dependency>
```

### Step 2: Configure Connection

Create a connection using JDBC URL and credentials:

```java
import java.sql.*;
import java.util.Properties;

// Set connection properties
Properties props = new Properties();
props.setProperty("user", "your-username");
props.setProperty("password", "your-password");

// Create connection (authentication happens automatically)
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
        // 1. Configure connection with username and password
        Properties props = new Properties();
        props.setProperty("user", "your-username");
        props.setProperty("password", "your-password");
        
        // 2. Connect (authentication happens automatically)
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
- `user` or `username` (required): Your Leaf API username
- `password` (required): Your Leaf API password

**URL Examples:**
```java
// Using Properties (recommended)
Properties props = new Properties();
props.setProperty("user", "your-username");
props.setProperty("password", "your-password");
Connection conn = DriverManager.getConnection("jdbc:leaf:", props);

// Using URL query parameters
Connection conn = DriverManager.getConnection(
    "jdbc:leaf:?user=your-username&password=your-password"
);

// Using URL semicolon format
Connection conn = DriverManager.getConnection(
    "jdbc:leaf:user=your-username;password=your-password"
);
```

## Authentication

The driver automatically authenticates with the Leaf API using your username and password. No need to manage tokens manually!

- **Endpoint**: `https://api.withleaf.io/api/authenticate`
- **Method**: `POST`
- **Token Duration**: 30 days (with `rememberMe: true`)

The authentication happens automatically when you create a connection. The driver handles token management internally.

## API Format

**Endpoint:** `https://api.withleaf.io/services/pointlake/api/v2/query?sqlEngine=SPARK_SQL`

**Method:** `POST`

**Request:**
- Body: SQL query as plain text (Content-Type: `text/plain; charset=utf-8`)
- Authorization: Header `Authorization: Bearer <token>` (automatically added)

**Response Formats Supported:**
- `[ {"col": value, ...}, ... ]` (direct array of objects) - **Primary format**
- `{ "data": [ {"col": value, ...}, ... ] }` (wrapped array)
- `{ "columns": ["col1",...], "rows": [[...], ...] }` (legacy format)

## Using with DBeaver

The driver is fully compatible with DBeaver! Here's a quick setup guide:

### Quick Setup

1. **Download the driver**: Get `leaf-jdbc-driver-*-all.jar` from [releases](https://github.com/lhzsantana/leaf-jdbc-driver/releases)

2. **Install driver in DBeaver**:
   - Go to **Database** → **Driver Manager** → **New**
   - **Driver Name**: `Leaf JDBC Driver`
   - **Driver Type**: `Generic`
   - **Class Name**: `com.leaf.jdbc.LeafDriver`
   - **URL Template**: `jdbc:leaf:`
   - **Libraries**: Add the downloaded `*-all.jar` file
   - Click **OK**

3. **Create connection**:
   - Go to **Database** → **New Database Connection**
   - Select **Leaf JDBC Driver**
   - **JDBC URL**: `jdbc:leaf:`
   - **Username**: Your Leaf API username
   - **Password**: Your Leaf API password
   - Click **Test Connection** (driver authenticates automatically)
   - Click **Finish**

4. **Execute queries**:
   ```sql
   SELECT geometry FROM leaf.pointlake.points TABLESAMPLE(0.3 PERCENT) LIMIT 10
   ```

📖 **Detailed DBeaver setup guide**: See [DBEAVER_SETUP.md](DBEAVER_SETUP.md) for step-by-step instructions with screenshots and troubleshooting.

**Note**: The driver automatically handles authentication when you enter username/password in DBeaver's connection dialog. No need to manage tokens manually!

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
        url = uri("https://maven.pkg.github.com/lhzsantana/leaf-jdbc-driver")
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
