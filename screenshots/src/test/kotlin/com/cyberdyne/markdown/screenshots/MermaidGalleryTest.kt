package com.cyberdyne.markdown.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.cyberdyne.markdown.engine.config.DiagramSizing
import com.cyberdyne.markdown.engine.config.MarkdownConfiguration
import com.cyberdyne.markdown.engine.rendering.MarkdownView
import com.cyberdyne.markdown.engine.theming.MarkdownTheme
import org.junit.Rule
import org.junit.Test

class MermaidGalleryTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 1500),
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )

    private val fit = MarkdownConfiguration(diagramSizing = DiagramSizing.FIT_TO_WIDTH)

    private fun shot(name: String, mermaid: String) {
        paparazzi.snapshot(name) {
            GalleryFrame(MarkdownTheme.Light) {
                MarkdownView(
                    markdown = "```mermaid\n$mermaid\n```",
                    configuration = fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }
        }
    }

    @Test fun pie() = shot("mermaid-pie", "pie title Languages\n    \"Kotlin\" : 55\n    \"Java\" : 30\n    \"Other\" : 15")

    @Test fun sequence() = shot(
        "mermaid-sequence",
        "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice->>Bob: Hello\n    Bob-->>Alice: Hi there",
    )

    @Test fun classDiagram() = shot(
        "mermaid-class",
        "classDiagram\n    class Animal {\n        +String name\n        +int age\n        +speak()\n    }\n    class Dog\n    Animal <|-- Dog",
    )

    @Test fun stateDiagram() = shot(
        "mermaid-state",
        "stateDiagram-v2\n    [*] --> Idle\n    Idle --> Running : start\n    Running --> Idle : stop\n    Running --> [*]",
    )

    @Test fun erDiagram() = shot(
        "mermaid-er",
        "erDiagram\n    CUSTOMER ||--o{ ORDER : places\n    ORDER ||--|{ LINE_ITEM : contains",
    )

    @Test fun gitGraph() = shot(
        "mermaid-gitgraph",
        "gitGraph\n    commit\n    branch develop\n    checkout develop\n    commit\n    checkout main\n    merge develop",
    )

    @Test fun journey() = shot(
        "mermaid-journey",
        "journey\n    title My Day\n    section Morning\n      Wake up: 3: Me\n      Coffee: 5: Me\n    section Work\n      Code: 4: Me",
    )

    @Test fun timeline() = shot(
        "mermaid-timeline",
        "timeline\n    title History\n    2019 : Idea\n    2021 : Prototype\n    2024 : Launch : v1.0",
    )

    @Test fun mindmap() = shot(
        "mermaid-mindmap",
        "mindmap\n  root((Engine))\n    Parser\n      CommonMark\n      GFM\n    Renderer\n      Compose",
    )

    @Test fun gantt() = shot(
        "mermaid-gantt",
        "gantt\n    title Plan\n    section Build\n      Design : d1, 2024-01-01, 3d\n      Code : d2, 2024-01-04, 5d\n    section Ship\n      Test : d3, 2024-01-09, 2d",
    )
}
