# v64 Notes UX — implementation and acceptance ledger

Branch: fix/v64-notes-rtl-navigation-20261118
Baseline: 62560e33a45d6b3b8ff7cd8822427c26bd063af3
Do not modify main. Never claim completion without successful build and a device check.

## P0 — data and navigation
- [ ] Trash only shows deleted notes/folders, not live folders. Verify folder status and item filters independently.
- [ ] Trash has its own unmistakable screen header, back affordance, restore and irreversible-delete confirmation.
- [ ] Back from trash returns to notes; back from nested folder goes to parent; back from root folder returns to notes.
- [ ] Preserve saved notes, attachments, folders, encryption, history and drafts across upgrade.
- [ ] Autosave, save-check and force-close recovery verified on physical device.

## P1 — notes appearance and interactions
- [ ] Reduce folder tab/icon size and improve card composition; keep nested folders and their color/lock.
- [ ] Make ordinary note look like a refined paper surface with readable margins.
- [ ] Title field full width, center-aligned and bold; derive editable fallback title when empty.
- [ ] Default internal note page color matches card color until explicitly customized independently.
- [ ] Fix ruled-line baseline alignment for Arabic and English in reading and editing.
- [ ] Checklist checkbox appears at paragraph start (right in Arabic RTL, left in English LTR).
- [ ] Improve new-document dialog: normal note vs experimental A4 with icon and concise description.
- [ ] All Arabic menus and dialogs use RTL direction; icon is at far right followed immediately by right-aligned label.
- [ ] Standardize icon semantics across menus, selection bars, trash, search, history, sharing and attachment UI.
- [ ] Rework note options as organized sheet with icons and meaningful switches for pin, archive, lock, preview.
- [ ] Remove redundant move-to-trash and nonfunctional text-border control from in-editor options.
- [ ] Rename task/calendar link entry to Note options if it opens that panel; group and order task/checklist/calendar controls.
- [ ] Remove optional reminder and comma-separated tags fields from that panel WITHOUT deleting stored data.
- [ ] Retain preview-hiding option only if its behavior is explicit: hide card body excerpt, not the note itself.

## P2 — A4 editor (larger engineering effort)
- [ ] One shared A4 page geometry (210x297mm), margins, font metrics, pagination between preview and PDF/print.
- [ ] Zoom in/out without modifying document text or printed font size.
- [ ] Page editing, caret/selection, table/image placement and wrapping; no loss when switching modes.
- [ ] Multi-page tests with Arabic RTL, images, tables and varied font sizes.
- [ ] Do not claim Word/Google Docs feature parity until implemented and tested.

## Delivery gates
- [ ] Kotlin compilation and unit tests pass.
- [ ] Signed APK produced with increased versionCode, signature checked and release asset downloaded/verified.
- [ ] Test on Android 35 and 36; record failures, including keyboard/IME limitations, without marking as passes.
- [ ] Provide exact APK link, commit, completed vs deferred items, and a short on-device acceptance checklist.

## Implemented so far
- [x] Editor overflow menu has matching leading icons and an explicit Arabic RTL layout direction (commit 6b32e4c); pending compilation/device verification.
