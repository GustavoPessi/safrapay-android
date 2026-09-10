# Gestão Rural — App Android (TCC)

Sistema Mobile para Auxílio à Gestão e à Contabilidade Rural.
Autor: Gustavo Borges Pessi.

## Stack
- **Linguagem:** Java
- **UI:** XML (Views) + Material 3
- **Persistência:** Room (sobre SQLite) — espelha o `schema_sistema_rural.sql`
- **Arquitetura:** MVVM (Activity → ViewModel → Repository → DAO), pensada para
  migração futura a Kotlin.

## Como abrir
1. Abra a pasta `GestaoRural/` no **Android Studio** (Hedgehog ou mais novo).
2. Aguarde o Gradle sincronizar (baixa dependências automaticamente).
3. Rode em um emulador ou dispositivo (minSdk 24 / Android 7.0+).

## O que já está implementado nesta etapa
- **Camada de dados Room completa** — 8 entidades espelhando o schema do TCC
  (usuário, propriedade, talhão, safra, categoria_despesa, receita, despesa,
  meta_financeira), com FKs, índices e campos de sync (`uuid`, `criado_em`,
  `atualizado_em`, `sincronizado`). Seed automático das 7 categorias de despesa.
- **Fluxo de entrada:** Splash → Onboarding (3 telas) → Login / Cadastro /
  Recuperar senha. Cadastro e login são **reais** (gravam/consultam no Room).
- **Design System:** paleta verde agro, tipografia, botões, inputs e temas em `res/values`.

## Estrutura
```
app/src/main/java/com/tcc/gestaorural/
  data/local/entity/   → 8 entidades Room
  data/local/dao/      → DAOs
  data/local/AppDatabase.java
  data/repository/     → AuthRepository
  ui/splash | onboarding | auth   → telas
  util/                → SecurityUtils, SessionManager, DateUtils
```

## Pendências conhecidas (próximas etapas)
- Telas: Perfil, Propriedades (lista/nova/detalhe), Talhões, Safras, Financeiro,
  Metas, Dashboard (seguindo o Notion — "Fonte da verdade do projeto").
- Recuperação de senha é hoje um stub de UI (envio real depende de backend).
- **Segurança:** `SecurityUtils` usa SHA-256 + salt fixo por simplicidade de TCC.
  Para produção, trocar por bcrypt/argon2 com salt por usuário.
