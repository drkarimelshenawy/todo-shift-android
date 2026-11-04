# Google Cloud Setup Guide — ToDo Shift Neo Luxury 4.0

أُعد في 4 نوفمبر 2026 بعد مراجعة وثائق Google وAndroid الرسمية. هذا دليل تفعيل لمرحلة لاحقة، وليس إعلانًا بأن ربط Google يعمل في السورس الحالي.

## الحدود الحالية أولًا

| داخل السورس الآن | يحتاج تنفيذًا أو إعدادًا لاحقًا |
|---|---|
| واجهات Authentication / Authorization / Drive Backup / Scheduling / Sync Future | موصلات SDK الفعلية واختبارها |
| `DisabledCloud` يعيد Unavailable وأزرار سحابية معطلة | تهيئة OAuth وتوصيل موصلات حقيقية إلى الواجهة |
| تصدير واستعادة محليان بصيغة V2، وManifest وChecksums ومعاينة وتأكيد وRollback Journal | اختبار بناء Android، وفشل الكتابة، وانقطاع العملية على الهاتف |
| تفضيلات يومي/أسبوعي/شهري/Wi-Fi محفوظة، ومخطط مواعيد خالص | WorkManager Worker حقيقي؛ لا توجد جدولة سحابية نشطة حاليًا |
| صفحات سياسات عربية وإنجليزية للمراجعة | هوية المطور وبريد الدعم وروابط HTTPS عامة ومراجعة السياسة |

**إدخال Client IDs وحده لن يفعّل Drive. موصلات Credential Manager وAuthorizationClient وDrive وWorkManager لم تُنفّذ أو تُضف Dependencies لها في هذه المرحلة.**

## 1. جهّز بيانات التطبيق

- الاسم: **ToDo Shift**.
- Package Name: **`com.todoshift.app`**.
- أبقِ شهادة التوقيع الأصلية. لا تنشئ مفتاحًا بديلًا لتحديث نسخة المستخدم الحالية.
- للحصول على بصمتي SHA-1 وSHA-256 من المفتاح الموجود، شغّل في بيئة المطور الأمر التالي؛ يطلب كلمة المرور تفاعليًا ولا يحفظها في الملف:

```bash
keytool -list -v -keystore "$TODOSHIFT_SIGNING_FILE"
```

- عند استخدام Play App Signing، استخدم شهادة App Signing المناسبة، لا تفترض أنها شهادة Upload Key.
- تحقق من بصمة كل بناء فعلي. توجد في workflow الأصلي بصمة SHA-256 متوقعة `aecf41e372d7204dacf6ebb93f02414b265c8d97fd5508023287aa2fc6da6ea9`؛ هذه قيمة مقارنة من السورس، وليست نتيجة استخراج شهادة APK بُني هنا.
- يتطلب Android OAuth Client عادة SHA-1؛ احتفظ أيضًا بـSHA-256 للتوثيق وفحص البناء والخدمات التي تحتاجه. لا تخترع قيمة SHA-1 أو تستنتجها من SHA-256.

مرجع: [Client authentication](https://developers.google.com/android/guides/client-auth).

## 2. أنشئ مشروع Google Cloud

1. افتح [Google Cloud Console](https://console.cloud.google.com/) بحساب المطور المسؤول.
2. أنشئ مشروعًا مخصصًا للتطبيق أو اختر مشروعه المعتمد، وسجل Project ID الحقيقي.
3. من مكتبة APIs & Services فعّل **Google Drive API**.
4. لا تنشئ Service Account لتسجيل دخول مستخدمي الهاتف أو تضف مفتاح Service Account داخل APK. المسار المطلوب هو موافقة المستخدم على حسابه.

## 3. جهّز Google Auth Platform / OAuth Consent Screen

1. استكمل Branding: اسم التطبيق وبيانات الدعم والمطور الحقيقية.
2. اختر Audience المناسب لحسابات المستخدمين المقصودة؛ أضف حسابات Test Users أثناء الاختبار عندما يلزم.
3. انشر ملفات `legal/*.html` على HTTPS، بعد استكمال البيانات الناقصة وإزالة علامة المسودة عقب المراجعة.
4. أضف روابط الخصوصية والاستخدام والدعم الموثقة إلى إعدادات التطبيق.
5. في Data Access، اطلب أقل الصلاحيات اللازمة، وافصل إذن الهوية عن إذن Drive.
6. لا تصف التطبيق بأنه Verified حتى تنتهي إجراءات Google المطلوبة فعليًا.

مرجع: [About Sign in with Google](https://developer.android.com/identity/sign-in/credential-manager-siwg).

## 4. أنشئ OAuth Clients الصحيحة

1. أنشئ **Android Client** باسم الحزمة `com.todoshift.app` وبصمة SHA-1 الصحيحة لشهادة البناء الذي ستختبره.
2. أنشئ عملاء Android إضافيين للشهادات المختلفة التي يلزم دعمها، مثل اختبار ومفتاح Play، داخل المشروع المناسب.
3. أنشئ **Web Client** عند استخدامه كـServer Client ID / audience ضمن مسار Credential Manager الموصى به.
4. لا تضع Web Client Secret داخل Android، ولا ترفع مفاتيح التوقيع أو ملفات أسرار إلى السورس.
5. أدخل معرفاتك الحقيقية من إعداد بناء محلي أو إعداد CI آمن بعد تنفيذ الموصلات. لا توجد معرفات OAuth تجريبية في التسليم.

مرجع: [Google sign-in Android codelab](https://codelabs.developers.google.com/sign-in-with-google-android).

## 5. نفّذ موصل تسجيل الهوية

- استخدم **Android Credential Manager** وGoogle ID وفق الإرشادات الحالية، وليس Google Sign-In القديم أو GoogleAuthUtil أو One Tap القديم.
- أضف Dependencies الضرورية فقط وقت التنفيذ: `androidx.credentials:credentials`، وموصل Play Services الخاص به، و`googleid` حسب الوثائق المعتمدة.
- لم يتم اختيار أو تثبيت إصدارات لهذه المكتبات في هذا السورس. راجع [التنفيذ الرسمي](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation) و[إصدارات Google ID](https://developers.google.com/identity/android-credential-manager/releases) و[إصدارات Credentials](https://developer.android.com/jetpack/androidx/releases/credentials)، ثم ثبّت نسخ Stable متوافقة مع إعدادات المشروع في مراجعة منفصلة.
- اربط الموصل بواجهة `GoogleAuthentication`، ومرر مضيف Activity / ActivityResult من طبقة Android، لا من محركات الحسابات أو الشيفتات.
- عالج إلغاء المستخدم وغياب الحساب وفشل الخدمة دون حذف بيانات أو حجب الوظائف المحلية.
- لو أُضيف خادم لاحقًا، يجب أن يتحقق الخادم من ID Tokens بالشكل الصحيح؛ لا تعتبر فك JWT محليًا بديلًا عن التحقق المطلوب للخادم.

## 6. نفّذ تفويض Drive بصورة مستقلة

- استخدم **AuthorizationClient** لطلب الصلاحيات عند الحاجة للنسخ.
- نطاق المسار المقترح هو `https://www.googleapis.com/auth/drive.appdata` عندما تكون نسخ التطبيق الخاصة هي المطلوب.
- نفّذ مسار PendingIntent / ActivityResult عندما يحتاج المستخدم للموافقة، وعالج الرفض وسحب الإذن.
- اربط `DriveAuthorization.accountSubject` بالحساب المعروض والمقصود. لا ترفع نسخة في حساب آخر بالاعتماد على حساب افتراضي غير متحقق منه.
- لا تستخدم ID Token مكان Drive Access Token، ولا تحفظ Tokens في نسخ JSON أو ملفات UI.
- أضف صلاحية الإنترنت إلى Manifest فقط عند توصيل التنفيذ الشبكي الحقيقي. هذه المرحلة لا تضيفها.

مرجع: [Authorize access to Google user data](https://developer.android.com/identity/authorization).

## 7. نفّذ Drive Backup Adapter

- نفّذ `DriveBackup` باستخدام Drive API v3 وواجهات IO قابلة للإلغاء، خارج Main Thread.
- في app data space، أنشئ الملفات تحت `appDataFolder` واستعلم عن النسخ داخل `spaces=appDataFolder`.
- ارفع أرشيف V2 الذي ينتجه `BackupExport` بدل إنشاء صيغة بيانات ثانية.
- لا تسجّل نجاح النسخ إلا بعد اكتمال الرفع والتحقق من المعرف والحجم والبصمة والبيانات الوصفية المطلوبة.
- اعرض النسخ المتاحة ثم نزّل الملف المرغوب ومرره إلى نفس Validation / Preview / Confirmation / Restore Coordinator المحلي.
- حذف نسخة وإلغاء تفويض الحساب عمليتان مختلفتان. صمم واجهة الحذف وتأكيدها قبل تفعيلها؛ لا تفترض دعم Trash داخل appDataFolder.
- قرر سياسة الاحتفاظ وعدد النسخ صراحة، وحدث السياسة؛ لم يُختر رقم احتفاظ تلقائي في هذا التسليم.

مرجع: [Store application-specific data](https://developers.google.com/workspace/drive/api/guides/appdata).

## 8. نفّذ الجدولة دون خدمة دائمة

- نفّذ `BackupScheduling` بعمل فريد في WorkManager، دون Foreground Service مستمرة.
- اربط العامل بحالة حساب وتفويض صالحين، وألغِه عند تسجيل الخروج أو تعطيل الجدولة.
- طبّق قيد الشبكة المناسب. `UNMETERED` لا يعني Wi-Fi حصريًا في كل جهاز؛ إذا كانت السياسة Wi-Fi فقط فافحص Transport المناسب أيضًا.
- وقت التنفيذ ليس موعدًا دقيقًا بسبب قيود Android والطاقة. للشهري التقويمي يمكن إعادة جدولة OneTimeWorkRequest من الموعد المخطط؛ لا تساوِ شهرًا دائمًا بـ30 يومًا.
- في الخلفية، لا تحاول فتح شاشة موافقة تلقائيًا؛ أعد حالة تحتاج تفاعل المستخدم عند فقد الإذن.
- راجع [تعريف Work Requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) و[إصدارات WorkManager](https://developer.android.com/jetpack/androidx/releases/work) قبل اختيار الإصدار.

## 9. اختبار الربط والاستعادة قبل التفعيل

- حساب اختبار واحد، ثم حساب آخر مع التأكد من عدم اختلاط النسخ.
- رفض/إلغاء تسجيل الدخول، وإلغاء التفويض، وانقطاع الإنترنت، وانتهاء Token.
- رفع نسخة، ظهورها في القائمة، تنزيلها، والتحقق من البصمة والمعاينة قبل الاستعادة.
- استعادة على هاتف ثانٍ مع الحفاظ على بيانات الشيفتات والمهام والماليات، وإعداد حماية جديدة مناسبة للجهاز.
- ملف ناقص، مسار غير مسموح، checksum خاطئ، إصدار غير مدعوم، وفشل كتابة أو قتل العملية أثناء المعاملة.
- إيقاف الجدولة وتسجيل الخروج دون حذف قاعدة البيانات المحلية.
- حذف نسخة مختارة ثم تأكيد عدم حذف غيرها.
- فحص Local First دون أي حساب أو شبكة.

## 10. الخصوصية والتشفير والمزامنة

راجع [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy) و[متطلبات بيانات المستخدم في Google Play](https://support.google.com/googleplay/android-developer/answer/10144311) وفق الخدمات التي تُفعَّل فعلًا.

الأرشيف الحالي **غير مشفر**، وSHA-256 يكشف التلف ولا يثبت موثوقية المرسل. لم تُنفذ إدارة مفاتيح متنقلة بين الأجهزة؛ لا تضف تشفيرًا يعتمد على Keystore الهاتف القديم وحده ثم تدّعي قابلية الاستعادة على هاتف جديد.

واجهة `CloudSyncFuture` لا تنفذ Sync. يلزم تصميم مستقل لإصدارات السجلات والتعارض والحذف قبل أي مزامنة بين الأجهزة. لا تستخدم استعادة Snapshot تلقائية كبديل عن ذلك.
