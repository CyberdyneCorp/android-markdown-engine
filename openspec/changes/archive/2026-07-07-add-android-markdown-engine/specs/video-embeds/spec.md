# video-embeds

Mirrors iOS capability `video-embeds`.

## ADDED Requirements

### Requirement: Video URL classification
The library SHALL provide a pure, deterministic classifier that maps a URL string
to one of `VideoKind.DirectFile`, `VideoKind.Provider`, or `VideoKind.NotVideo`.

#### Scenario: Direct file by extension
- **WHEN** a URL path ends in `.mp4`, `.mov`, `.m4v`, or `.m3u8` (case-insensitive, ignoring query strings)
- **THEN** the classifier SHALL return `DirectFile`

#### Scenario: Provider by host
- **WHEN** a URL host is a known provider (`youtube.com`, `youtu.be`, `vimeo.com`, including `www.` and `m.` subdomains)
- **THEN** the classifier SHALL return `Provider`

#### Scenario: Not a video
- **WHEN** a URL is neither a direct file nor a known provider (e.g. a `.png` or article link)
- **THEN** the classifier SHALL return `NotVideo`

### Requirement: Linked-thumbnail video embed
A Markdown linked image whose link destination is a video URL SHALL render as a
tappable thumbnail with a play overlay; the thumbnail SHALL load like any Markdown
image and its `alt` text SHALL be used as the accessibility label.

#### Scenario: Provider thumbnail opens externally
- **WHEN** the user taps a provider-video thumbnail and no embedder is configured
- **THEN** the URL SHALL open externally via `Intent.ACTION_VIEW`

#### Scenario: Direct-file thumbnail begins playback
- **WHEN** the user taps a direct-file thumbnail
- **THEN** inline playback SHALL begin

### Requirement: Direct video file inline playback
An image node whose source is a direct video file SHALL render as an inline native
player (Media3/ExoPlayer-backed) with a sensible default aspect ratio. It MUST NOT
instantiate a `WebView`.

#### Scenario: Inline player for direct file
- **WHEN** a block is an image whose source is a `.mp4` file
- **THEN** it SHALL render an inline Media3 player and SHALL NOT use a WebView

### Requirement: Injectable provider embedder
The library SHALL allow a host app to supply inline players for provider videos
via an injectable `VideoEmbedder` in `MarkdownServices`. When supplied, the
embedder's view SHALL render inline in place of the thumbnail; when absent, the
provider URL SHALL open externally. The core module MUST NOT import `WebView`; any
WebView-backed embedder lives in the host app.

#### Scenario: Embedder renders inline
- **WHEN** a `VideoEmbedder` is configured and a provider video is present
- **THEN** the embedder's inline view SHALL replace the thumbnail

#### Scenario: No embedder opens externally
- **WHEN** no embedder is configured and a provider thumbnail is tapped
- **THEN** the URL SHALL open externally

#### Scenario: Core imports no WebView
- **WHEN** the core module is built
- **THEN** it SHALL NOT reference `android.webkit.WebView`

### Requirement: Graceful fallback
The library SHALL fall back to a tappable thumbnail/placeholder that opens the URL
externally when inline playback is unavailable (platform limitation or media load
failure).

#### Scenario: Playback failure falls back
- **WHEN** inline playback fails to load the media
- **THEN** a tappable placeholder SHALL be shown that opens the URL externally
