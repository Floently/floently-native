# Native Read API Contract

## Goal

Native Read must become a premium document-to-audio app with no dead spinner.

## Product areas

Native Read must support:

- Read-specific login/access
- document import
- library
- reader
- audio player
- upload/extraction/audio progress
- retry/cancel/failure state

## Existing frontend/backend clues

Existing frontend had Read routes:

- /read/auth
- /read/app
- /read/import
- /read/library
- /read/reader
- /read/settings
- /read/subscribe
- /read/analytics

Native Read should not copy the Expo UI, but it should preserve product flow.

## Required backend API surface

Native Read needs:

### Document upload

- create upload session
- upload file/chunks
- complete upload
- cancel upload
- retry failed upload

### Processing

- get document status
- first-pages-first extraction status
- first readable text segment
- full text segment list
- failure reason
- retry extraction

### Audio

- prepare first audio segment
- get audio segment URL/file
- get audio queue state
- retry audio preparation

### Library

- list documents
- get document detail
- delete document
- rename document
- update progress

### Access

- get Read entitlement
- get subscription state
- handle internal test access

## Native implementation expectations

iOS:

- UIDocumentPicker / Files integration
- URLSession upload
- background-capable upload where useful
- FileManager cache
- AVFoundation audio playback

Android:

- Android file picker / SAF
- OkHttp upload
- WorkManager for long jobs
- Media3/ExoPlayer playback
- local cache

## UX requirement

Never show a forever spinner.

Always show one of:

- added locally
- uploading
- extracting first pages
- preparing first audio
- ready to play
- failed with retry
- offline with retry
