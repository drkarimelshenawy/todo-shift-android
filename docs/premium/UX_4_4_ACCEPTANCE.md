# Premium UX 4.4.0 — device acceptance

Candidate: 4.4.0-premium-ux-test / versionCode 38. Apply as an update; retain app data. Cash Book is separate.

## Delivered scope and traceability

| Approved group | Implementation | Automated coverage |
|---|---|---|
| Stable calendar cells, independent workplace names, no shift-count badges or creation banner | NeoCalendar, NeoModernPreferences, Neo4AppearanceCodec, V11CalendarUi | PremiumUxUiTest, CalendarUiRegressionTest |
| Select on first tap, details on second, long press for quick summary; painting keeps priority | NeoCalendar | PremiumUxUiTest, PremiumPaintingActivityTest |
| Weighted calendar widget, one continuous paint surface, 4/5/6-week geometry and adjustable panel opacity | NeoCalendarWidgetRenderer, Neo4WidgetPainter, Neo4WidgetStudio, widget_calendar_v2 | CalendarWidgetLayoutTest, NeoPaintEditTest |
| Completed tasks: checkbox, strike-through, quiet colors, reversible state in app and widget | TaskCardV11, TasksWidgetFactory, TasksWidgetProvider | PremiumUxUiTest, CompletedTaskWidgetTest, NeoWidgetIntegrationTest |
| Compact task list, daily progress, independent date/status filters, search, sorting and persisted density/manual order | NeoPremiumTasks, NeoTaskQueries | PremiumUxQueryTest, ScreenUiSmokeTest, PremiumWorkflowUiTest |
| Shared new/edit form, basic date/priority/reminder and collapsible advanced fields | FullTaskEditorDialogV11 via TaskEditorDialogV11 | PremiumUxUiTest, PremiumP01UiTest, PremiumActivitySaveTest |
| Separate template management and use; duplicate, edit, delete, date and links; safe create-on-date replay | NeoTaskTemplatesScreen, NeoSmartStore | TaskTemplateWorkflowUiTest, existing SmartStore tests |

Task ordering is changed with Move up / Move down in each task's menu after selecting Manual sort. This change does not introduce drag gestures. New task attachments are available after the task is saved; existing attachments remain linked by the same task ID. Undo is offered after supported data changes and only while the current data matches that action. Widget labels wrap or truncate within their cells according to available height; full labels remain in accessibility descriptions. Very short widgets omit detail rows instead of changing row sizes.

## Phone checklist — قائمة الاختبار

- [ ] التحديث والتشغيل: الإصدار 38، فتح وإغلاق عدة مرات، بقاء البيانات، عدم ظهور شاشة انتظار مستمرة أو ANR.
- [ ] التقويم: إظهار وإخفاء التقاويم لا يغير ارتفاع الخلايا، واسم مكان العمل مستقل عن مدة الشيفت، ولا يظهر x1/x2 أو سطر New entries.
- [ ] التفاعل: ضغطة تحدد اليوم، الثانية تفتح التفاصيل، والمطولة تفتح المختصر. الانتقال لشهر آخر لا يفتح يومًا قديمًا. الرسم يبقى مستقلًا عن هذا التفاعل.
- [ ] الويدجت: تكبير وتصغير وتغيير الشهر والاتجاه؛ الصفوف متساوية وتمتد لأسفل الشبكة، والحدود متصلة، والشفافية تؤثر في الخلفية. اختبر أسماء طويلة وملاحظة مع الشيفت.
- [ ] المهام المكتملة: علامة صح وخط ولون أهدأ؛ إعادة فتح المهمة ترجع الشكل الطبيعي. اختبر الحالة نفسها من ويدجت المهام.
- [ ] البحث والفلاتر: Today / Upcoming / Overdue / All dates مع All / Active / Done، ثم الأولوية والتقويم وReset filters.
- [ ] كثافة المهام والترتيب: Compact/Comfortable وإخفاء المكتمل؛ ترتيب التاريخ والأولوية؛ Manual ثم Move up/down، وإغلاق التطبيق وفتحه للتأكد من بقاء الترتيب.
- [ ] المحرر: إنشاء وتعديل المهمة من نفس النافذة؛ حفظ الملاحظات والوقت والتكرار والتذكير وروابط التقويم ومكان العمل والشيفت. اختبر لوحة المفاتيح وشاشة صغيرة وعدم تغطية الحفظ.
- [ ] القوالب: إنشاء وتعديل ونسخ وحذف قالب، واستخدامه بتاريخ وروابط مختلفة؛ تكرار الإنشاء لنفس القالب والتاريخ لا يكرر المهام. حذف القالب لا يحذف المهام المنشأة منه.
- [ ] التراجع: أكمل مهمة أو احذفها ثم تراجع، وتأكد من عودة نفس المهمة وتفاصيلها.
- [ ] الحماية والتوافق: PIN وتصوير الشاشة والبيانات المالية محفوظة؛ التذكير يعمل في موعده بعد إذن التنبيهات، وكذلك بعد تعديل المهمة أو إعادة فتحها.
- [ ] العربية والإنجليزية: مواضع أزرار الإنجاز والحفظ والإلغاء ثابتة، والنصوص مقروءة ولا تخرج من حدودها.

Automated tests and native RemoteViews renders do not replace launcher/device timing, notification/Doze, keyboard, backup/restore and non-destructive installer-update acceptance. Protected branches remain unchanged; no merge or release publication is included.

## Current automated delivery gate

The push workflow runs the complete unit/Compose/Robolectric suite plus lint, builds with the existing persistent CI key, verifies APK package/version/source/certificate/alignment, then runs `tools/premium_device_smoke.py` on Android 35. The smoke exercises a vc37-to-vc38 in-place update, UI-created task preservation, editing, completion/reopen and three cold launches. Use the matching run's job conclusions and attached evidence for results, rather than treating this checklist as a pass certificate.
