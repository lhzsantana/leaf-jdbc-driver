# Guia de Configuração do DBeaver com Leaf JDBC Driver

Este guia mostra passo a passo como configurar o Leaf JDBC Driver no DBeaver.

## Pré-requisitos

1. DBeaver instalado (versão Community ou Enterprise)
2. JAR do driver baixado (`leaf-jdbc-driver-*-all.jar`)
   - Baixe de: https://github.com/lhzsantana/leaf-jdbc-driver/releases
   - Use o arquivo que termina com `-all.jar` (contém todas as dependências)

## Passo 1: Instalar o Driver no DBeaver

### 1.1 Abrir o Gerenciador de Drivers

1. Abra o DBeaver
2. Vá em **Database** → **Driver Manager** (ou pressione `Ctrl+Shift+D`)
3. Clique em **New** (botão no canto superior esquerdo)

### 1.2 Configurar o Driver

Na janela "Create new driver", preencha:

**Aba "Settings":**
- **Driver name**: `Leaf JDBC Driver`
- **Driver type**: Selecione `Generic`
- **Class name**: `com.leaf.jdbc.LeafDriver`
- **URL Template**: `jdbc:leaf:`
- **Default Port**: (deixe vazio)
- **Default Database**: (deixe vazio)
- **Default User**: (deixe vazio)
- **Default Password**: (deixe vazio)

**Aba "Libraries":**
1. Clique em **Add File**
2. Navegue até o arquivo `leaf-jdbc-driver-*-all.jar` que você baixou
3. Selecione o arquivo e clique em **Open**
4. O JAR deve aparecer na lista de libraries

**Aba "Driver Properties":**
- Não é necessário configurar nada aqui

5. Clique em **OK** para salvar o driver

## Passo 2: Criar uma Nova Conexão

### 2.1 Iniciar Criação de Conexão

1. Vá em **Database** → **New Database Connection** (ou pressione `Ctrl+Shift+N`)
2. Na lista de drivers, procure por **Leaf JDBC Driver**
3. Selecione **Leaf JDBC Driver**
4. Clique em **Next**

### 2.2 Configurar Credenciais

Na tela de configuração da conexão:

**Aba "Main":**
- **JDBC URL**: `jdbc:leaf:`
  - Mantenha apenas o prefixo, não adicione parâmetros aqui
- **Username**: Seu username da API Leaf
- **Password**: Sua senha da API Leaf
  - ⚠️ **Importante**: O driver fará autenticação automática com essas credenciais

**Outras abas:**
- Não é necessário configurar nada nas outras abas

### 2.3 Testar a Conexão

1. Clique em **Test Connection** (botão no canto inferior)
2. O DBeaver tentará conectar usando o driver
3. O driver fará automaticamente:
   - Autenticação na API Leaf (`https://api.withleaf.io/api/authenticate`)
   - Obtenção do token JWT
   - Validação da conexão
4. Se tudo estiver correto, você verá: **"Connected"** ✅
5. Se houver erro, verifique:
   - Username e password estão corretos?
   - Você tem acesso à API Leaf?
   - O JAR do driver está correto?

### 2.4 Finalizar

1. Clique em **Finish**
2. A conexão será salva e aparecerá no painel de conexões à esquerda

## Passo 3: Executar Queries

### 3.1 Abrir Editor SQL

1. Clique com o botão direito na conexão criada
2. Selecione **SQL Editor** → **New SQL Script**
3. Ou pressione `Ctrl+\` com a conexão selecionada

### 3.2 Executar Query

Digite sua query SQL:

```sql
SELECT geometry 
FROM leaf.pointlake.points 
TABLESAMPLE(0.3 PERCENT) 
LIMIT 10
```

1. Clique em **Execute SQL Statement** (botão ▶️ ou `Ctrl+Enter`)
2. Os resultados aparecerão na aba **Data** abaixo

### 3.3 Exemplos de Queries

```sql
-- Buscar pontos de geometria
SELECT geometry 
FROM leaf.pointlake.points 
LIMIT 100

-- Consultar dados específicos
SELECT * 
FROM leaf.pointlake.points 
WHERE geometry IS NOT NULL
LIMIT 50
```

## Solução de Problemas

### Erro: "Driver not found"

**Solução:**
- Verifique se o JAR foi adicionado corretamente nas Libraries do driver
- Certifique-se de usar o arquivo `*-all.jar` (não o JAR sem dependências)
- Tente remover e recriar o driver

### Erro: "Authentication failed"

**Solução:**
- Verifique se username e password estão corretos
- Teste suas credenciais diretamente na API Leaf
- Verifique sua conexão com a internet

### Erro: "Invalid JDBC URL"

**Solução:**
- Certifique-se de que a URL está exatamente: `jdbc:leaf:`
- Não adicione parâmetros na URL
- Use os campos Username e Password separados

### Erro: "ClassNotFoundException"

**Solução:**
- Certifique-se de usar o arquivo `*-all.jar` que contém todas as dependências
- Verifique se o JAR não está corrompido
- Tente baixar novamente do GitHub Releases

## Configurações Avançadas

### Usar URL com Parâmetros (Alternativa)

Se preferir passar credenciais na URL:

**JDBC URL:**
```
jdbc:leaf:?user=seu-usuario&password=sua-senha
```

**Ou formato com ponto e vírgula:**
```
jdbc:leaf:user=seu-usuario;password=sua-senha
```

⚠️ **Nota**: É mais seguro usar os campos Username/Password separados, pois o DBeaver pode mascarar a senha.

## Recursos Adicionais

- **Documentação da API Leaf**: https://learn.withleaf.io/docs/authentication
- **Repositório do Driver**: https://github.com/lhzsantana/leaf-jdbc-driver
- **Releases**: https://github.com/lhzsantana/leaf-jdbc-driver/releases

## Suporte

Se encontrar problemas:
1. Verifique os logs do DBeaver: **Window** → **Show View** → **Error Log**
2. Abra uma issue no GitHub: https://github.com/lhzsantana/leaf-jdbc-driver/issues
