package com.tcc.gestaorural.ui.sobre;

/** Texto do Manual de ajuda: {título, explicação}. Linguagem simples, pensando no produtor. */
final class ConteudoAjuda {

    private ConteudoAjuda() {}

    static final String[][] SECOES = {
        { "🚜 Primeiros passos",
          "O app organiza tudo nesta ordem: Propriedade → Talhão → Safra → Lançamentos (receitas e despesas).\n\n"
        + "1. Cadastre a propriedade (Perfil → Cadastros → Minhas propriedades).\n"
        + "2. Dentro dela, cadastre os talhões com a cultura de cada um.\n"
        + "3. Abra uma safra no talhão.\n"
        + "4. Lance os gastos e as receitas sempre ligados a uma safra.\n\n"
        + "Atalho: no Início, \"+ Nova safra\" faz propriedade, talhão, safra e meta numa tela só." },

        { "🏡 Propriedade — o que é cada campo",
          "• Localidade: cidade, distrito ou comunidade.\n"
        + "• Área total (ha): tamanho da propriedade em hectares.\n"
        + "• Tipo de solo: opcional, só pra sua referência.\n"
        + "• Matrícula: número da matrícula do imóvel no Cartório de Registro de Imóveis (está na escritura/certidão).\n"
        + "• CIB: número do imóvel rural no cadastro da Receita Federal (antigo NIRF); aparece na declaração do ITR.\n"
        + "• Valor: quanto vale a propriedade, se quiser acompanhar o patrimônio. Opcional.\n"
        + "• Usar minha localização: marca no mapa onde a propriedade fica, usando o GPS do celular." },

        { "🌱 Talhão e safra",
          "• Talhão é cada área da propriedade onde se planta (ex.: \"Talhão da baixada\"). Informe a área em hectares e a cultura (soja, milho...).\n"
        + "• Safra é um ciclo de plantio num talhão: ano (ex.: 2026/27), variedade da semente, data de início e de fim.\n"
        + "• Safra ativa = safra sem data de fim. Quando colher e fechar a safra, preencha a data de fim.\n"
        + "• Todo gasto e toda receita ficam ligados a uma safra — é assim que o app mostra quanto cada lavoura deu de lucro ou prejuízo." },

        { "💰 Receitas e despesas",
          "• Receita: o que entrou (venda de grão, por exemplo). Despesa: o que saiu.\n"
        + "• Despesas têm categoria: Máquinas, Combustível, Insumos, Sementes, Impostos, Documentos ou Outras despesas operacionais.\n"
        + "• Na categoria Máquinas dá pra dizer qual máquina gerou o gasto — o total aparece no detalhe da máquina.\n"
        + "• Comprovante: anexe a foto da nota ou do recibo (câmera ou galeria). Por enquanto a foto fica só neste celular." },

        { "🎯 Metas",
          "A meta é por safra: um valor que você quer alcançar até um prazo.\n\n"
        + "O progresso é o resultado da safra: receitas menos despesas dela. Assim você vê se a lavoura está no caminho do lucro esperado." },

        { "🔧 Máquinas e tarefas",
          "• Cadastre as máquinas (Perfil → Cadastros → Minhas máquinas); marca e modelo vêm de um catálogo.\n"
        + "• No detalhe da máquina ficam os gastos dela e as anotações/tarefas (ex.: \"Trocar óleo\").\n"
        + "• Cada tarefa pode ter prazo e responsável (quando você tem equipe).\n"
        + "• Situação: Pendente, Atrasada (pendente com prazo vencido) ou Realizada (mostra quem fez).\n"
        + "• As tarefas que vencem em até uma semana aparecem em Notificações." },

        { "📊 Relatórios",
          "• Indicadores: receitas, despesas e saldo por mês.\n"
        + "• Relatório por período: escolha as datas e exporte em PDF, Excel ou CSV pra mandar ao contador, banco ou cooperativa.\n"
        + "• DRE: resultado do ano organizado como demonstração de resultado.\n\n"
        + "Os relatórios são gerenciais: servem pra acompanhar a propriedade e não substituem a contabilidade formal." },

        { "💬 Assistente de IA",
          "Converse do seu jeito e ele lança pra você — sempre mostra o que entendeu e só grava quando você toca em \"Confirmar e lançar\".\n\n"
        + "• Texto: \"gastei 300 de diesel ontem na soja\".\n"
        + "• Foto: toque no clipe e mande até 5 fotos de notas de uma vez. A foto vira o comprovante do lançamento.\n"
        + "• Planilha ou bloco de notas (.xlsx, .csv, .txt) com vários lançamentos.\n"
        + "• Voz: segure o microfone, fale e solte (deslize pro lado pra cancelar).\n"
        + "• Perguntas: \"quanto gastei este mês?\", \"quais safras estão ativas?\", \"me manda a nota do diesel\".\n\n"
        + "Se tiver mais de uma safra ativa e você não disser qual, ele pergunta." },

        { "👥 Equipe (gerente e colaborador)",
          "O dono da conta pode convidar pessoas em Perfil → Minha equipe:\n\n"
        + "• Gerente: usa o app completo com os dados da sua fazenda (finanças, safras, máquinas). Não mexe na equipe.\n"
        + "• Colaborador: vê só as máquinas e as tarefas dele (e as sem responsável), marca como feitas e reporta problemas. Precisa de internet.\n\n"
        + "Quem é convidado aceita em Perfil → Convites recebidos. Enquanto estiver numa equipe, o app mostra os dados da fazenda do dono; os seus dados ficam guardados e voltam se você sair da equipe." },

        { "🔄 Sincronização e internet",
          "O app funciona sem internet: tudo é salvo no celular. Com internet, ele envia e recebe os dados sozinho — ao abrir o app, quando a internet volta e alguns segundos depois de cada alteração.\n\n"
        + "Também dá pra forçar em Perfil → Sincronizar dados.\n\n"
        + "Assim você pode usar em mais de um celular e trocar de aparelho sem perder os lançamentos (as fotos dos comprovantes ainda ficam só no celular onde foram tiradas)." },

        { "🔒 Seus dados",
          "• Configurações → Exportar meus dados gera uma planilha com tudo da sua conta (propriedades, safras, lançamentos, metas, máquinas, tarefas).\n"
        + "• Configurações → Excluir conta apaga sua conta.\n"
        + "• O assistente de IA usa suas mensagens, fotos e áudios só pra entender o lançamento; o áudio não é guardado." },
    };
}
