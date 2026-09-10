# AgroConta — Guia inicial: servidor AWS + backend

Este guia é o **passo a passo do que você precisa fazer** para ter o backend do AgroConta rodando na AWS. Ele é voltado para TCC (free tier, custo baixo, sem exageros de infraestrutura).

## 1. Antes de tudo: entenda a ordem

O AWS é **onde o backend roda** — mas o **código do backend (Spring Boot) ainda não existe**; a gente vai escrevê-lo depois. Ou seja:

1. **Agora:** você pode ir preparando a **infraestrutura** (conta, banco RDS) em paralelo.
2. **Depois:** a gente escreve a API Spring Boot (autenticação, gerente/colaborador, sincronização) e só então faz o **deploy** dela na AWS.

O app Android continua **offline-first** (Room/SQLite). O backend serve só para **multiusuário e sincronização** entre os aparelhos da equipe (gerente ↔ colaborador).

```
[App Android - Room/SQLite]  <--- sincroniza --->  [API Spring Boot na AWS]  <--->  [PostgreSQL - RDS]
```

## 2. O que você precisa fazer agora (infra)

### 2.1 Criar a conta e proteger o acesso
- [ ] Criar conta na AWS (https://aws.amazon.com) — pede cartão, mas o free tier cobre o uso do TCC.
- [ ] Ativar **MFA** (autenticação em 2 fatores) na conta raiz.
- [ ] Criar um **usuário IAM** com permissão de administrador para o uso do dia a dia (não use a conta raiz para tudo).

### 2.2 Evitar susto na fatura (importante!)
- [ ] Em **Billing → Budgets**, criar um orçamento de, por exemplo, **US$ 1** com alerta por e-mail. Assim você é avisado se algo sair do free tier.
- [ ] Anotar quais recursos ligou, para **desligar/derrubar** quando não estiver usando.

### 2.3 Escolher a região
- [ ] Usar **sa-east-1 (São Paulo)** — menor latência no Brasil. Crie todos os recursos na mesma região.

### 2.4 Banco de dados — RDS PostgreSQL
- [ ] Criar uma instância **Amazon RDS → PostgreSQL**.
- [ ] Tipo **db.t3.micro** (ou t4g.micro) — elegível ao free tier (750 h/mês por 12 meses, 20 GB).
- [ ] Definir usuário e senha do banco e **anotar**: endpoint (host), porta (5432), usuário, senha, nome do banco.
- [ ] Deixar o acesso **restrito** (só o servidor do app acessa) — configurado no security group (passo 2.6).

### 2.5 Onde o Spring Boot vai rodar (escolha uma opção)
- **Opção A — Elastic Beanstalk (mais fácil, recomendado):** plataforma **Java (Corretto 17)**. Você faz o build do projeto num `.jar` e sobe pelo painel; a AWS cuida de servidor, deploy e reinício.
- **Opção B — EC2 t3.micro:** uma máquina Linux onde você instala o **JDK 17** e roda o `.jar` manualmente (mais controle, mais trabalho).

Para TCC, **comece pela Opção A**.

### 2.6 Rede e segurança (security groups)
- [ ] Liberar a porta do app (8080, ou 80/443 se usar HTTPS) para a internet.
- [ ] Liberar a porta do Postgres (5432) **somente** para o servidor do app (não para o mundo).

## 3. Quando o backend estiver pronto (deploy)
- [ ] Fazer o build do projeto Spring Boot como `.jar`.
- [ ] Subir no Beanstalk (ou copiar para a EC2 e rodar).
- [ ] Configurar a conexão com o banco por **variáveis de ambiente** (host/usuário/senha do RDS) — **nunca** deixar senha no código/repositório.
- [ ] Apontar o app Android para a URL do servidor.

## 4. Boas práticas de segurança (para o TCC e além)
- Não versionar segredos (senhas, chaves) — usar variáveis de ambiente.
- Usar **HTTPS** na API (no Beanstalk dá para configurar via load balancer).
- Autenticação por **token (JWT)** entre app e API.
- Hash de senha com **bcrypt/argon2** no backend (o app hoje usa SHA-256, nível TCC — para produção, migrar).

## 5. Alternativa mais simples: Render
Se o trabalho permitir, o **Render** (https://render.com) é bem mais simples para TCC: Postgres gerenciado + web service com deploy direto do Git, free tier generoso e menos passos de rede/segurança. Dá para começar por ele e migrar para AWS depois, se necessário. (Consigo ajudar a provisionar o Render direto, se você preferir esse caminho.)

## 6. Resumo — o que fazer primeiro
1. Criar conta AWS + MFA + usuário IAM.
2. Criar o alerta de orçamento (Budgets).
3. Criar o RDS PostgreSQL (free tier) e guardar as credenciais.
4. Alinhar com o orientador se o backend entra no escopo/prazo do TCC.
5. Me avisar — aí a gente começa a escrever a API Spring Boot para subir nessa infra.
