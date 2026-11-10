# Visual evidence scope

The optional probe is `tools/visual-tests/NeoVisualProbeTest.kt`, enabled only by `tools/visual_capture.init.gradle`. Ordinary unit tests do not include this source set. It uses Robolectric SDK28 NATIVE graphics and synthetic fixtures at the current checkpoint. It is not an Android emulator, actual phone, Honor or user-review acceptance.

The workflow images launch actual MainActivity and traverse Calendar, painter/type strip, Schedules/Summary, Tasks, Finance, Settings, five setting subpages and contextual options, with English light and Arabic AMOLED samples plus a narrow/large-text empty calendar. Exact final files/results are listed in the external verification manifest and QA.md. They are diagnostic samples of the existing UI, not proof that P02–P05's rejected UX has been rebuilt.

`component-p01-classic-backup.png` replaces Activity content with the production Classic backup component and an explicitly authorized synthetic host. `component-p01-task-editor-error-model.png` replaces content with the production task editor and a seeded rejection state. Neither reproduces Root disk failure or an original device incident. The seeded error image can verify visible draft/error layout only.

The probe rejects blank captures and documents missing modal windows rather than fabricating them. A prior idling failure is retained separately; final successful results, if present, are distinct. Physical keyboard/IME, real document providers, Launcher, Doze, notification permissions, TalkBack, OS lock-screen redaction, rotation/process death, all independent RTL axes, large-font combinations and full contrast measurements remain unverified.

Inspected images and observed limitations are recorded in QA.md after viewing the final captures. No screenshot is labelled physical or user-approved.
