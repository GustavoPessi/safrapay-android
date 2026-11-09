# LEIAME: SafraPay, aplicativo Android

Instruções para instalar, acessar e testar o aplicativo SafraPay (TCC de Ciência da Computação, UNESC, 2026).
O aplicativo conversa com o servidor que já está em produção (`https://api.safra-pay.com`), então **não é preciso rodar nenhum servidor** para avaliar. O código do servidor e o LEIAME dele estão no repositório [safrapay-backend](https://github.com/GustavoPessi/safrapay-backend).

## 1. Requisitos

- Celular Android **7.0 ou mais novo** (Android 7.0 = API 24). Também roda no emulador do Android Studio.
- Internet para entrar na conta, sincronizar e usar o assistente de IA. O restante funciona sem internet.
- Permissões pedidas durante o uso (todas opcionais): **localização** (marcar o ponto da propriedade/talhão), **microfone** (mensagem de voz no assistente) e **câmera/galeria** (foto do comprovante).

## 2. Instalar o APK (forma recomendada)

1. Baixe o arquivo `SafraPay.apk` pelo link informado na entrega da pré-banca (AVA).
2. Abra o arquivo no celular. Se o Android avisar sobre "fontes desconhecidas", permita a instalação para o app usado para abrir o arquivo (navegador ou gerenciador de arquivos).
3. Toque em **Instalar** e depois em **Abrir**.

> Se o celular já tiver uma versão anterior do SafraPay instalada com outra assinatura, desinstale-a antes.

## 3. Entrar no aplicativo

Na primeira abertura aparecem telas de apresentação (toque em **Pular** ou **Próximo**) e depois a tela de login. Há duas formas de entrar:

- **Conta de demonstração (recomendada):** já tem propriedades, talhões, safras, lançamentos, metas e máquinas com **dados fictícios**. O e-mail e a senha são enviados aos avaliadores junto com a entrega, fora do repositório (não ficam no código por segurança). Informe esses dados e toque em **Entrar**.
- **Conta nova:** toque em **Não tem conta? Cadastre-se**, preencha os dados e confirme o cadastro pelo link enviado ao e-mail informado. A conta começa vazia.

Depois do login, o app baixa os dados da conta do servidor. Se algo não aparecer, use **Perfil → Sincronizar dados**.

## 4. Roteiro de teste

A navegação principal fica na barra inferior: **Início**, **Finanças**, **Metas** e **Perfil**. O ícone de voltar fica no canto superior esquerdo.

### 4.1 Cadastros (propriedade → talhão → safra)

1. **Perfil → Cadastros → Minhas propriedades**: abra uma propriedade ou crie uma nova (nome, área, município; o ponto no mapa usa a localização do celular).
2. Dentro da propriedade, abra ou crie um **talhão** (área em hectares e cultura, por exemplo soja).
3. Dentro do talhão, crie uma **safra**. A data de início e o ano já vêm preenchidos com a data de hoje. Se a data de fim cair no ano seguinte, a safra aparece como "2026/2027".
4. Atalho: no **Início**, o botão **+ Nova safra** faz o mesmo caminho de forma guiada.

### 4.2 Finanças

1. No **Início**, toque em **+ Despesa**: escolha a safra, a categoria (Combustível, Insumos, Máquinas etc.), o valor e a data. Opcional: anexar foto do comprovante.
2. Toque em **+ Receita** e registre uma venda.
3. Abra **Finanças** na barra inferior: veja saldo, receitas e despesas. Teste os filtros de **safra** e de **período** (Tudo, Dia, Mês, Ano ou Intervalo; as setas ‹ › passam para o período anterior ou o próximo). Ao rolar a lista, os filtros sobem para dar espaço.
4. Toque em um lançamento para ver o detalhe, editar ou excluir.

### 4.3 Metas

1. **Metas → +**: crie uma meta de resultado para uma safra (valor e prazo).
2. Abra a meta para ver o progresso, calculado como receitas menos despesas da safra.

### 4.4 Indicadores, relatório e DRE

1. No **Início**, toque em **Ver indicadores**: receitas, despesas e saldo por mês, margem e valores por hectare.
2. **Ver relatório por período**: escolha o período e toque em **Exportar** (PDF, Excel ou CSV) para gerar e compartilhar o arquivo.
3. **Ver DRE**: demonstração do resultado do ano.

### 4.5 Máquinas e tarefas

1. **Perfil → Cadastros → Minhas máquinas**: cadastre uma máquina (marca e modelo vêm de um catálogo).
2. No detalhe da máquina, crie uma anotação/tarefa (por exemplo "Trocar óleo") com prazo. Tarefas que vencem em até uma semana aparecem em **Perfil → Notificações**.
3. Uma despesa da categoria **Máquinas** pode ser ligada a uma máquina; o total aparece no detalhe dela.

### 4.6 Assistente de IA (precisa de internet)

No **Início**, toque em **Falar com o assistente (IA)** e teste, por exemplo:

- Texto: "gastei 350 reais de diesel hoje na safra de soja". O assistente propõe o lançamento e só grava depois que você confirma.
- Pergunta: "quanto gastei este mês?" ou "quais safras estão ativas?". Os valores são calculados no próprio celular.
- Pelo clipe: foto de uma nota fiscal ou planilha (.xlsx/.csv). Pelo microfone: mensagem de voz.

### 4.7 Funcionamento sem internet

1. Ative o modo avião.
2. Cadastre uma despesa: ela é salva normalmente no celular.
3. Desative o modo avião: o app sincroniza sozinho em alguns segundos (ou use **Perfil → Sincronizar dados**).

### 4.8 Uso em equipe (opcional, precisa de uma segunda conta)

1. Com a conta principal: **Perfil → Minha equipe → Convidar pessoa**, informe o e-mail da segunda conta e o papel (**gerente** usa o app completo com os dados da fazenda; **colaborador** vê só máquinas e tarefas).
2. Com a segunda conta (em outro celular, ou saindo e entrando de novo): **Perfil → Convites recebidos**, abra o convite e aceite.
3. Como colaborador, abra uma tarefa e marque como realizada; o dono vê quem concluiu.

### 4.9 Outros

- **Perfil → Configurações → Exportar meus dados**: planilha com todos os dados da conta.
- **Perfil → Configurações → Sobre e ajuda**: manual de uso do aplicativo.

## 5. Compilar pelo código-fonte (opcional)

1. Instale o **Android Studio** (versão recente, com JDK 17, que já vem junto).
2. Clone o repositório: `git clone https://github.com/GustavoPessi/safrapay-android.git`
3. No Android Studio: **File → Open** e escolha a pasta clonada. Aguarde o Gradle sincronizar (baixa as dependências na primeira vez).
4. Conecte um celular com a depuração USB ativa (ou crie um emulador com API 24 ou mais) e clique em **Run ▶**.
5. Para gerar o APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk`.

Não é preciso configurar chave nem variável de ambiente: o endereço do servidor (`https://api.safra-pay.com/`) está em `app/src/main/java/com/tcc/gestaorural/data/remote/RetrofitClient.java`. As chaves das APIs de IA ficam só no servidor.

## 6. Tecnologias e organização do código

- Java 17, Views em XML, Material 3; arquitetura MVVM (Activity → ViewModel → Repository → DAO) com LiveData.
- Banco local Room (SQLite); rede com Retrofit/OkHttp e renovação automática do token (JWT).
- `minSdk 24`, `targetSdk 34`, Android Gradle Plugin 8.13.

```
app/src/main/java/com/tcc/gestaorural/
  data/     → Room (entidades, DAOs), API (Retrofit), repositórios e motor de sincronização
  ui/       → telas, por funcionalidade
  export/   → exportação de relatório e de dados (PDF, Excel, CSV)
  util/     → sessão, datas, máscaras e utilitários
```

Mais detalhes de arquitetura e decisões técnicas estão no `README.md`.

## 7. Problemas comuns

- **"Sem conexão" ao entrar:** confira a internet do celular; o servidor `https://api.safra-pay.com` precisa estar acessível.
- **Dados não aparecem depois do login:** **Perfil → Sincronizar dados**.
- **Não chegou o e-mail de confirmação da conta nova:** verifique a caixa de spam ou use a conta de demonstração.
- **Assistente não responde:** ele precisa de internet; sem conexão, o restante do app continua funcionando.

## Autor

Gustavo Borges Pessi, Ciência da Computação, UNESC, 2026.
