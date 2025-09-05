Leaf JDBC Driver (Apache Calcite)
=================================

Driver JDBC leve para consultar a API da Leaf Agriculture.

Funcionalidades
- **Validação de SQL**: validação sintática via Apache Calcite
- **Autenticação JWT**: conexão com Bearer token
- **Transformação JSON→Rows**: converte a resposta JSON da API em `ResultSet`

Instalação (Gradle)
```kotlin
repositories { mavenCentral() }
dependencies { implementation("com.leaf:leaf-jdbc-driver:0.1.0-SNAPSHOT") }
```

Uso
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

URL JDBC
- Prefixo: `jdbc:leaf:`
- Parâmetros via propriedades ou URL:
  - `apiPrefix`: ex. `https://api.withleaf.io/api/v1`
  - `token`: JWT Bearer

Formato esperado da API
- Endpoint: `{apiPrefix}/pointlake/query?sql=<SQL>`
- Autorização: header `Authorization: Bearer <token>`
- Resposta JSON aceita:
  - `{ "columns": ["col1",...], "rows": [[...], ...] }` ou
  - `{ "data": [ {"col": value, ...}, ... ] }` ou
  - `[ {"col": value, ...}, ... ]`

Limitações
- Apenas `Statement` simples (sem `PreparedStatement`)
- Modo read-only, forward-only

Distribuição fácil
- GitHub Packages (Gradle): configure `GITHUB_ACTOR`/`GITHUB_TOKEN` e use `com.leaf:leaf-jdbc-driver:<versao>`
- Releases: faça download do JAR `*-all.jar` anexado no release (contém todas dependências)
- JitPack: adicione repositório `https://jitpack.io` e use `com.github.Leaf-Agriculture:leaf-jdbc-driver:<tag>`
