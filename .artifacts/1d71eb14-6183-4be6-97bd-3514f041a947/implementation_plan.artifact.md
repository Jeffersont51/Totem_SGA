# Centralização do Tema do Aplicativo

Este plano visa unificar a aplicação da identidade visual do aplicativo (cor primária definida no Admin) através de uma arquitetura centralizada, garantindo que a Status Bar, Toolbar e botões reflitam as escolhas do usuário de forma consistente em todas as telas.

## User Review Required

> [!IMPORTANT]
> A implementação de uma Toolbar padrão em todas as telas mudará o layout atual das atividades de configuração e administração. Isso é necessário para que a cor do tema seja exibida de forma proeminente no topo da tela, conforme solicitado.

## Proposed Changes

### [Component Name] Core Architecture

#### [NEW] [BaseActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/BaseActivity.java)
- Criar classe base para todas as Activities.
- Implementar `applyTheme()` centralizado.
- Gerenciar a `Toolbar` e aplicar a cor primária automaticamente se presente no layout.
- Aplicar cor de fundo da atividade conforme configurado no `SessionManager`.

#### [NEW] [toolbar_layout.xml](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/res/layout/toolbar_layout.xml)
- Layout reutilizável contendo uma `Material Toolbar`.

---

### [Component Name] Activities & Layouts

#### [MODIFY] [MainActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/MainActivity.java)
- Herdar de `BaseActivity`.
- Remover código duplicado de `applyTheme()`.
- *Nota:* Manterá lógica específica de Kiosk Mode.

#### [MODIFY] [AdminActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/AdminActivity.java)
- Herdar de `BaseActivity`.
- Remover código duplicado de `applyTheme()`.

#### [MODIFY] [ConfigActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/ConfigActivity.java)
- Herdar de `BaseActivity`.
- Remover código duplicado de `applyTheme()`.

#### [MODIFY] [DiagnosticActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/DiagnosticActivity.java)
- Herdar de `BaseActivity`.
- Remover código duplicado de `applyTheme()`.

#### [MODIFY] [AdConfigActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/AdConfigActivity.java)
- Herdar de `BaseActivity`.
- Remover código duplicado de `applyTheme()`.

#### [MODIFY] [LayoutConfigActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/LayoutConfigActivity.java)
- Herdar de `BaseActivity`.
- Remover código duplicado de `applyTheme()`.

#### [MODIFY] [PrintLayoutActivity.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/PrintLayoutActivity.java)
- Herdar de `BaseActivity`.
- Remover código duplicado de `applyTheme()`.

#### [MODIFY] Activity Layouts (Multiple Files)
- Incluir `toolbar_layout.xml` no topo de cada arquivo XML de atividade.
- Ajustar restrições de layout para que o conteúdo fique abaixo da Toolbar.
- Arquivos afetados:
    - `activity_admin.xml`
    - `activity_config.xml`
    - `activity_diagnostic.xml`
    - `activity_ad_config.xml`
    - `activity_layout_config.xml`
    - `activity_print_layout.xml`

---

### [Component Name] Adapters & Fragments

#### [MODIFY] [GenericItemAdapter.java](file:///C:/Users/jefferson.caetano/AndroidStudioProjects/TOTEMSGA2/TOTEMSGA2/app/src/main/java/br/com/jefferson/totemsga/adapter/GenericItemAdapter.java)
- Garantir que a cor padrão (`primaryColor`) seja aplicada corretamente se a cor individual não estiver definida.

## Verification Plan

### Manual Verification
1.  **Troca de Cor:** No Admin -> Configuração de Layout, alterar a "Cor do Tema do Aplicativo" para uma cor distinta (ex: Verde).
2.  **Verificação Global:** Navegar por todas as telas de configuração e administração para confirmar que a Status Bar e a nova Toolbar exibem o Verde.
3.  **MainActivity:** Confirmar que no Totem (MainActivity) a Status Bar também reflete a cor, mesmo no modo Kiosk (quando visível).
4.  **Contraste:** Testar uma cor clara (ex: Branco) para garantir que os ícones da Status Bar fiquem escuros (lógica de luminância).
