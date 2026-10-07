# Totem SGA - Documentação

**Versão atual:** 1.1.7 (7 de outubro de 2026)

O **Totem SGA** é um aplicativo Android para terminais de autoatendimento. O cliente escolhe o atendimento na tela, o app gera a senha no **NovoSGA** (sistema de filas) e imprime o comprovante na impressora térmica do próprio totem.

Este documento tem duas partes:

- **Parte 1 - Operação**: para quem instala, configura e dá manutenção no totem.
- **Parte 2 - Técnica**: para quem altera o código.

---

# Parte 1 - Operação

## 1. O que o totem faz

| Função | Descrição |
|---|---|
| Emissão de senha | O cliente escolhe o departamento e o serviço; o app gera a senha e imprime. |
| Triagem | Opcional por serviço. Pede documento (CPF, CNPJ ou telefone), nome e prioridade antes de gerar a senha. O nome é preenchido sozinho se o cliente já estiver cadastrado no NovoSGA. |
| Já tenho agendamento | O cliente digita o documento; o app procura agendamentos de hoje e confirma a presença, gerando a senha. |
| Reimpressão (2ª via) | Busca por número da senha ou documento e reimprime uma senha emitida hoje. |
| Publicidade | Mostra vídeos e imagens quando o totem fica parado. |
| Modo Kiosk | Trava o aparelho no aplicativo, sem acesso ao Android. |

## 2. Equipamento e rede

- **Aparelho homologado:** Sunmi K2, Android 7.1.2, com impressora térmica embutida.
- **Servidor NovoSGA:** `http://10.7.0.89` (versão 2.2.5).
- **Pasta de atualizações:** `\\192.168.1.227\Programas TI\TOTEM_SGA`.
- O totem precisa enxergar pela rede o servidor NovoSGA e, para atualizar, o servidor da pasta.

O totem de homologação (IP `192.168.168.139`) está com a porta USB quebrada. A tela dele é acessada por **RealVNC** e os arquivos por **AirDroid Web** (`http://192.168.168.139:8888`).

## 3. Primeira configuração

1. Instale o APK e abra o aplicativo. Na primeira vez ele abre a tela **Configuração Inicial do Servidor**.
2. Preencha:
   - **URL da API**: endereço do NovoSGA.
   - **Client ID** e **Client Secret**: do cliente OAuth cadastrado no NovoSGA.
   - **Usuário** e **Senha**: de um usuário do NovoSGA com permissão de triagem.
3. Toque em **Salvar**. Deve aparecer "Autenticado com sucesso".
4. No **Admin**, escolha a **Unidade** e toque em **Salvar e Sair**.

## 4. Tela de Admin

Para entrar: toque no botão de configurações da tela inicial (ou segure o dedo na área de acesso) e digite a senha do Admin.

A senha de fábrica é `admin`. **Troque no primeiro acesso**; enquanto ela não for trocada, o Admin mostra um aviso toda vez que é aberto.

| Opção | Para que serve |
|---|---|
| Unidade | Unidade do NovoSGA atendida por este totem. Obrigatória. |
| Habilitar Impressão | Liga a impressão do comprovante. Com ela ligada, o totem não emite senha se a impressora estiver com problema (ver seção 6). |
| Habilitar Triagem | Liga a tela de dados do cliente. |
| Timeout Triagem | Segundos sem toque até o totem voltar à tela inicial. |
| Nova Senha Admin | Troca a senha de acesso ao Admin. |
| Ativar Modo Kiosk | Trava o aparelho no aplicativo. |
| Ativar Administrador do Dispositivo | Permissão do Android para o Kiosk funcionar sem avisos. |
| Configuração de Layout | Cores, logotipo, tamanho dos botões, número de colunas. Em **Ajustes dos Botões de Seleção**, a altura vale para os cards de departamento e serviço e para os botões laranja; o tamanho da fonte vale para o nome nos cards e para o texto dos botões laranja. |
| Configuração Layout de Impressão | Tamanho das letras, alinhamento e o que aparece no comprovante. |
| Configuração de Publicidade | Vídeos e imagens exibidos com o totem parado. |
| Configuração Inicial Servidor | Reabre a tela de URL, Client ID, usuário e senha. |
| Diagnóstico (vX.Y.Z) | Testes de rede e de impressora e o registro de erros. O botão mostra a **versão instalada**. |
| Atualizar aplicativo | Busca e instala uma versão nova pela pasta de rede (ver seção 7). |

## 5. O que aparece ou não no totem

O que aparece é definido **no NovoSGA**, no campo **Descrição** de cada departamento e de cada serviço. Escreva as palavras-chave separadas por ponto e vírgula, por exemplo: `VISIVEL;TRIAGEM;PRIORIDADE`.

| Palavra-chave | Efeito |
|---|---|
| `VISIVEL` | O item aparece no totem. **Sem ela, o item não aparece.** |
| `TRIAGEM` | Abre a tela de dados do cliente antes de gerar a senha. |
| `TRIAGEM_OBRIGATORIA` | Exige o preenchimento de documento e nome. |
| `PRIORIDADE` | Deixa o cliente escolher entre atendimento normal e prioritário. |
| `NOME` | Imprime o nome do cliente no comprovante. |
| `FACIAL` | Imprime o texto de rodapé configurado no layout de impressão. |
| `SEGUNDAVIA` | Liga o botão de reimpressão na tela inicial (basta um serviço ter). |
| `TOLERANCIA30` | Minutos de tolerância após o horário do agendamento (padrão: 15). `TOLERANCIA0` desliga a verificação. |

Regras:

- O que está no **serviço** vale mais do que o que está no **departamento**. Se o serviço não disser nada, vale o do departamento.
- Para desligar em um serviço algo ligado no departamento, escreva `PALAVRA=false` (exemplo: `TRIAGEM=false`).
- O item também precisa estar **ativo** no NovoSGA.

## 6. Impressora

O tipo de impressora é escolhido na tela de Diagnóstico: **AUTO** (padrão), **SUNMI** ou **ALLPOS**.

Com a impressão ligada, o totem **não emite senha** nestas situações, e mostra "Impressora indisponível. Avise um atendente":

| Situação | O que fazer |
|---|---|
| Sem papel | Repor a bobina. |
| Tampa aberta | Fechar a tampa. |
| Travada (papel preso no corte) | Abrir a tampa, retirar o papel preso e fechar. |

O bloqueio vale para a emissão de senha e para a confirmação de agendamento. Assim que a impressora volta ao normal, o totem volta a emitir sozinho.

Se o problema acontecer **durante** a impressão (a senha já foi gerada), a tela da senha mostra em vermelho um aviso para o cliente anotar o número e chamar um atendente. Depois de resolvido, o cliente pode usar a **Reimpressão**.

> **Não confirmado:** a impressora não tem um aviso específico de "papel engasgado". O app trata como travamento o estado "erro de guilhotina". Se o papel engasgar e o totem não bloquear, abra **Admin → Diagnóstico** antes de destravar e anote o estado da impressora que aparece no registro.

## 7. Atualizar o aplicativo

1. Abra o **Admin** e toque em **Atualizar aplicativo**.
2. Confira a pasta de rede (já vem preenchida) e digite **seu usuário e senha de rede**, no formato `ALVORADA\seu.usuario`.
3. Toque em **Verificar**. A janela mostra a versão instalada e a disponível.
4. Se houver versão mais nova, toque em **Baixar e instalar** e confirme na tela do Android.
5. Abra o Admin de novo e confira a versão no botão **Diagnóstico**.

Observações:

- **O usuário e a senha de rede não ficam guardados no totem.** Quem faz a manutenção digita o próprio acesso a cada atualização.
- A pasta não aceita acesso sem login.
- A atualização mantém todas as configurações do totem.
- A atualização **não é automática**: alguém precisa abrir o Admin e tocar no botão.
- O app só instala arquivos que sejam o Totem SGA, com a mesma assinatura e de versão igual ou maior. Qualquer outro arquivo é recusado com a explicação na tela.

**Sem a pasta de rede:** também é possível instalar pelo AirDroid Web (aba **App**, arrastando o APK) ou por pendrive. Instale sempre **por cima**, sem desinstalar.

> **Atenção:** desinstalar o aplicativo apaga todas as configurações do totem. Antes de qualquer desinstalação, anote os dados da tela Configuração Inicial do Servidor.

## 8. Problemas comuns

| O que aparece | Causa provável | O que fazer |
|---|---|---|
| "Totem temporariamente indisponível. Procure um atendente." | O totem não conseguiu carregar departamentos ou serviços: servidor fora, rede fora ou autenticação recusada. O app tenta de novo sozinho a cada 30 segundos. O motivo exato (por exemplo "HTTP 401") fica no **Diagnóstico**. | Se persistir por mais de um minuto, verificar a rede e o servidor; se o Diagnóstico mostrar 401, conferir usuário, senha e Client ID/Secret em **Configuração Inicial Servidor** e salvar. |
| "Unidade não configurada" | Nenhuma unidade salva no Admin. | Abrir o Admin, escolher a Unidade e salvar. |
| Indisponível só ao abrir um departamento, com "HTTP 404" no Diagnóstico | A unidade salva não existe mais no NovoSGA. | Escolher a unidade de novo no Admin. |
| Sem rede | O totem não alcança o servidor. | Verificar o Wi-Fi e o servidor. O app religa o Wi-Fi sozinho após cerca de 2 minutos sem rede. |
| Botão ou ícone com cor diferente da escolhida no Admin | Proteção de contraste: a cor de tema escolhida era parecida demais com a cor de fundo, e o app trocou pelo laranja da marca para o elemento não sumir. | Escolher uma cor de tema que se destaque do fundo. |
| Departamento ou serviço não aparece | Falta `VISIVEL` na Descrição, ou o item está inativo. | Corrigir no NovoSGA. |
| Botão de reimpressão não aparece | Nenhum serviço tem `SEGUNDAVIA`. | Acrescentar na Descrição do serviço. |
| "Impressora indisponível" | Sem papel, tampa aberta ou travada. | Ver seção 6. |
| "Não foi possível consultar os agendamentos agora" | A consulta ao servidor falhou; não significa que não existe agendamento. | Tentar de novo; se persistir, verificar o servidor. |
| "Acesso negado à pasta" (atualização) | Usuário ou senha de rede incorretos. | Conferir e digitar de novo. |
| "O totem não conseguiu chegar ao servidor" (atualização) | Sem rota de rede até o servidor da pasta. | Verificar com a equipe de rede. |

A tela **Diagnóstico** guarda o registro dos últimos erros (autenticação, ping, impressora e atualização). Esse registro fica só na memória e **se perde quando o aplicativo fecha**; tire um print antes de reiniciar.

## 9. Voltar a uma versão anterior

O Android não deixa instalar uma versão mais antiga por cima de uma mais nova. Para voltar é preciso **desinstalar** (o que apaga as configurações), instalar o APK antigo e configurar tudo de novo.

Os APKs de cada versão ficam guardados nas pastas `BACKUP_V*` do projeto, no computador de desenvolvimento. O estado anterior às correções de 07/10/2026 é o `BACKUP_V4_PONTO_DE_PARTIDA_2026-10-07`.

---

# Parte 2 - Técnica

## 10. Tecnologias

- **Linguagem:** Java 11, Android nativo com layouts XML.
- **Versão do Android:** mínima 7.1 (API 25), alvo 14 (API 34).
- **Rede:** Retrofit 2 + OkHttp, JSON com Gson.
- **Imagens e vídeo:** Glide, ExoPlayer e YouTube Player.
- **Interface:** FlexboxLayout para as grades de botões.
- **Impressão:** biblioteca IT4R/TecToy (`app/libs/it4r_06.12.04.aar`), comandos ESC/POS.
- **Pasta de rede:** biblioteca `smbj` (protocolo SMB do Windows).

## 11. Estrutura

Uma única tela principal (`MainActivity`) troca os fragmentos do fluxo do cliente:

1. `SelectionFragment`: departamentos e serviços.
2. `ScreeningFragment`: triagem (opcional).
3. `ConfirmSchedulingFragment`: confirmação de agendamento.
4. `ReprintFragment`: reimpressão.
5. `SuccessFragment`: mostra a senha e imprime.

Todos herdam de `BaseKioskFragment`, que concentra o tempo de inatividade e os utilitários comuns:

- `runOnUi()` e `safeToast()`: descartam respostas de rede que chegam depois de o cliente sair da tela. **Use sempre estes**, nunca `requireActivity().runOnUiThread()` nem `Toast.makeText(getContext(), ...)` em retorno de rede.
- `printerBlockMessage()`: diz se a impressora impede a emissão.

Pacotes em `br.com.jefferson.totemsga`:

| Pacote | Conteúdo |
|---|---|
| `api` | `ApiService` (endpoints) e `RetrofitClient` (cliente HTTP e autenticação). |
| `model` | Classes de dados. |
| `adapter` | Lista genérica de cards (`GenericItemAdapter`). |
| `util` | `SessionManager` (configurações), `ClienteAuthManager` (sessão por cookie), `SunmiPrinterHelper`, `AppUpdater`, `WifiWatchdog`, `Logger`, `FeatureParser`. |
| `ads` | Publicidade. |
| `receiver` | Início automático no boot e administrador do dispositivo. |

Telas de configuração: `ConfigActivity`, `AdminActivity`, `LayoutConfigActivity`, `PrintLayoutActivity`, `AdConfigActivity`, `DiagnosticActivity`.

## 12. Autenticação com o NovoSGA

O NovoSGA tem duas superfícies com autenticações diferentes. O detalhamento dos endpoints está em `CLAUDE_1.md`.

**API OAuth (`/api/*`)** - unidades, departamentos, serviços, prioridades, emissão de senha.

- Token de acesso válido por 1 hora, enviado no cabeçalho `Authorization: Bearer`.
- Ao receber 401, `RetrofitClient.renewToken()` tenta renovar com o `refresh_token`. Se o servidor recusar, refaz o login completo (`grant_type=password`) com o usuário e a senha salvos.
- O endpoint `/api/token` nunca recebe o cabeçalho `Authorization`.
- No máximo 2 tentativas de re-autenticação por requisição.
- `MainActivity` faz um ping a cada 30 minutos para manter o token vivo.

**Sessão por cookie (`/novosga.triage/*`, `/novosga.monitor/*`)** - cadastro de clientes, agendamentos, fila para reimpressão.

- Login pelo formulário web (`/login`), com token CSRF extraído do HTML.
- Toda chamada passa por `ClienteAuthManager.executeWithSession()`, que detecta a sessão expirada (redirecionamento, 401/403, `sessionStatus = expired` ou HTML de login no lugar do JSON), refaz o login e repete a chamada uma vez.
- Um contador de geração evita vários logins simultâneos quando há consultas em paralelo.

**Recuperação na tela:** `SelectionFragment` recarrega sozinho a cada 30 segundos enquanto a mensagem de erro estiver visível.

## 13. Impressão

`SunmiPrinterHelper.resolveRoute()` decide o caminho a partir do tipo configurado:

| Tipo | Comportamento |
|---|---|
| `SUNMI` | Imprime direto na impressora embutida. |
| `AUTO` | Direto na embutida se ela estiver OK; bloqueia se estiver sem papel, com tampa aberta ou travada; nos demais casos tenta AllPos e, por último, a impressão padrão do Android. |
| `ALLPOS` | Envia para o serviço AllPos. |

A impressão direta usa só dados que já estão no app; não depende de consulta ao servidor.

Estados da biblioteca (`StatusImpressora`) e como o app trata:

| Estado | Código interno | Bloqueia emissão |
|---|---|---|
| `OK` | 1 | Não |
| `SEM_PAPEL` | 4 | Sim |
| `TAMPA_ABERTA` | 6 | Sim |
| `ERRO_GUILHOTINA` | 7 | Sim |
| Demais (`ERRO_COMUNICACAO`, `ERRO_DESCONHECIDO`, etc.) | 0 | Não, só aviso após imprimir |

`getPostPrintProblem()` é chamado 3,5 segundos depois de imprimir. Se a impressora estiver em qualquer estado de erro, a tela mostra o aviso e o nome do estado é gravado no `Logger`.

## 13.1 Aparência e cores

- **Tema:** `Theme.TOTEMSGA` (em `res/values/themes.xml`) define o laranja da marca como cor primária, no lugar do roxo padrão do Material.
- **Tamanho dos cards:** `GenericItemAdapter.setSizing()` recebe a altura e a fonte de "Ajustes dos Botões de Seleção". O ícone acompanha a altura do card.
- **Texto dos botões principais:** `BaseKioskFragment.sizePrimaryButtonText()`.
- **Proteção de contraste:** `util/ColorGuard.java`. `visibleOn()` troca a cor de um botão ou ícone pelo laranja da marca se o contraste com o fundo for menor que 1,3; `readableOn()` troca a cor de um texto por branco ou cinza-escuro se o contraste for menor que 2,0. Os limites foram escolhidos para manter as combinações da marca (branco sobre laranja = 2,7; amarelo sobre branco = 1,5) e barrar tons claros sobre o fundo (cerca de 1,2). **Ao aplicar uma cor configurável a um elemento, passe-a por `ColorGuard`.**
- **Mensagem de erro ao cliente:** a tela de seleção mostra sempre o mesmo texto e grava o detalhe técnico no `Logger` (`SelectionFragment.showUnavailable()`).

## 14. Atualização pela pasta de rede

`util/AppUpdater.java`:

1. `findLatest()`: lista os `.apk` da pasta e escolhe o de **maior versão no nome do arquivo**.
2. `download()`: baixa para `Android/data/<pacote>/files/updates/update.apk`.
3. `validate()`: confere pacote, assinatura e `versionCode` contra o app instalado.
4. `AdminActivity.launchInstaller()`: sai da tela fixada do Kiosk e abre o instalador do Android via `FileProvider`.

O caminho padrão é `SessionManager.DEFAULT_UPDATE_PATH`. Usuário e senha não são persistidos.

## 15. Compilar e publicar uma versão

1. Altere `versionCode` (+1) e `versionName` em `app/build.gradle.kts`.
2. Compile:
   ```
   gradlew.bat assembleDebug
   ```
   O APK sai em `app\build\outputs\apk\debug\TOTEM_SGA.apk`.
3. Confira a assinatura (precisa ser igual à das versões anteriores, senão não instala por cima):
   ```
   apksigner verify --print-certs TOTEM_SGA.apk
   ```
   Digest SHA-256 esperado: `c83b01ef02fcddf585e581ccd1d52db70a82b22b4254508435e720e0feb001d4`.
4. Guarde uma cópia em uma pasta `BACKUP_V<n>_v<versão>` (ignorada pelo Git).
5. Copie para `\\192.168.1.227\Programas TI\TOTEM_SGA` com o nome `TOTEM_SGA_v<versão>.apk`. **O número no nome do arquivo precisa ser igual ao `versionName`**, pois é por ele que o totem escolhe a versão.
6. Atualize o Histórico de Versões abaixo e faça o commit.

O APK é assinado com a chave de depuração do computador de desenvolvimento. **Compilar em outro computador gera outra assinatura**, e o totem recusa a atualização. Para mudar de computador, copie o arquivo `debug.keystore` da pasta `.android` do usuário.

Se a compilação falhar com erro de `ANDROID_PREFS_ROOT`, remova essa variável de ambiente e tente de novo.

## 16. Controle de versões (Git)

- Repositório: `https://github.com/Jeffersont51/Totem_SGA`
- `main`: versões testadas no totem.
- `correcoes-v1.1.0`: correções aguardando teste.
- Marcas: `ponto-de-partida-2026-10-07` (estado antes das correções) e `v1.1.5`.

## 17. Pendências conhecidas

- **Erro 401:** a correção foi feita na v1.1.0, mas só se confirma com o totem ligado de um dia para o outro.
- **Papel engasgado:** falta confirmar qual estado a impressora informa (ver seção 6).
- **Senha do NovoSGA** salva sem criptografia nas preferências do app.
- **Registro de erros** só em memória; perde-se ao fechar o app.
- **Telas de teste** (`TesteClienteActivity`, `TestAutocompleteActivity`) continuam no APK.
- **APK de depuração:** ainda não existe versão de produção assinada com chave própria.
- **Campo Unidade do Admin** mostra o primeiro item da lista quando não há unidade salva, dando a impressão de que já está configurado.

## 18. Histórico de versões

### v1.1.7 (07/10/2026) - instalada e aprovada no totem

Revisão de aparência para uso em totem. Ponto de retorno: marca `ponto-de-retorno-v1.1.6`.

- Cards de departamento e serviço obedecem à altura e à fonte do Admin (antes eram fixos e pequenos); ícone proporcional; mensagem do serviço em até duas linhas.
- Texto dos botões laranja obedece à fonte do Admin.
- Proteção de contraste: botões, ícones e títulos não somem mais quando a cor de tema é parecida com o fundo ("Tentar novamente", "Salvar e Sair", "Próximo", título do Admin, ícones dos serviços).
- Laranja da marca no lugar do roxo nas opções, chaves e botões de contorno.
- "Tempo restante", opções de documento e campos de digitação maiores.
- Indicador de carregamento na tela de seleção.
- Mensagem de erro em linguagem de cliente; o código técnico vai para o Diagnóstico.

### v1.1.6 (07/10/2026) - instalada; papel preso ainda sem confirmação

- Papel preso: `ERRO_GUILHOTINA` passa a bloquear a emissão.
- Conferência da impressora 3,5 segundos após imprimir, com aviso na tela e registro do estado.

### v1.1.5 (07/10/2026)

- O atualizador não guarda mais usuário e senha de rede; o que as versões 1.1.3 e 1.1.4 salvaram é apagado ao abrir o Admin.

### v1.1.3 e v1.1.4 (07/10/2026)

- Botão **Atualizar aplicativo** no Admin, lendo a pasta de rede.
- A v1.1.4 é idêntica à v1.1.3, só com o número maior, usada para testar a atualização.

### v1.1.2 (07/10/2026)

- Impressora sem papel ou com a tampa aberta bloqueia a emissão de senha e a confirmação de agendamento.

### v1.1.1 (07/10/2026)

- Salvar o Admin com a lista de unidades vazia não apaga mais a unidade.
- A tela de serviços avisa "Unidade não configurada" em vez de mostrar erro 404.

### v1.1.0 (07/10/2026)

- **Erro 401:** o app refaz o login sozinho quando a renovação do token é recusada.
- **Senha duplicada:** toque duplo em um serviço, em "Gerar senha" ou em "Confirmar presença" não emite mais duas senhas.
- **Impressão:** direta na Sunmi, sem depender do servidor; aviso de sem papel ou tampa aberta na tela da senha.
- **Agendamento:** falha de consulta não aparece mais como "nenhum agendamento"; sessão expirada se recupera sozinha.
- **Fechamentos inesperados:** respostas de rede tardias são descartadas.
- **Reimpressão:** removido o texto técnico "DADOS RECEBIDOS".
- **Ping de 30 minutos:** registra falha quando o servidor recusa.
- **Rede:** um único cliente HTTP reaproveitado.
- **Segurança:** registro de rede sem senha e tokens; backup do app desligado; aviso de senha padrão no Admin; versão no botão de Diagnóstico.

### v1.0 (até 30/07/2026)

- Emissão de senha, triagem com preenchimento automático, confirmação de agendamento, reimpressão, publicidade, modo Kiosk, layout configurável e reconexão automática do Wi-Fi.

**Testado e aprovado no totem em 07/10/2026:** carregamento de departamentos e serviços, bloqueio sem papel e com tampa aberta, toque duplo, agendamento, reimpressão e atualização pela pasta de rede.

---
*Documentação atualizada em 7 de outubro de 2026.*
