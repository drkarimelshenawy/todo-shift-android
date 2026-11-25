package com.exmore.app.v11.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.exmore.app.v11.V11SecurityStore

@Composable
internal fun NeoRecoverySetup(store:V11SecurityStore) {
    val verifier=rememberNeoSecurityVerifier()
    var code by remember {mutableStateOf<String?>(null)}
    var error by remember {mutableStateOf<String?>(null)}
    TextButton(enabled=!verifier.busy&&(store.hasPin()||store.hasPattern()||store.load().biometricEnabled),onClick={verifier.execute({store.createRecoveryCode()}) {it.onSuccess {value->code=value}.onFailure {error=it.message}}}) {Text(if(store.hasRecoveryCode()) neoLabel("تجديد رمز الاسترداد","Replace recovery code") else neoLabel("إنشاء رمز استرداد آمن","Create recovery code"))}
    error?.let {Text(it)}
    code?.let {value->AlertDialog(onDismissRequest={code=null},properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn),title={Text(neoLabel("احفظ رمز الاسترداد خارج الهاتف","Keep the recovery code outside this device"))},text={Column {
        Text(neoLabel("يظهر مرة واحدة. يمنح تغيير PIN دون حذف البيانات. لا يفتح نسخة احتياطية، وأي رمز سابق أصبح غير صالح.","Shown once. Resets the app PIN without deleting records. It does not decrypt backups; any previous recovery code is replaced."))
        SelectionContainer {Text(value)}
    }},confirmButton={TextButton(onClick={code=null}){Text(neoLabel("حفظته في مكان آمن","Stored safely"))}})}
}

@Composable
internal fun NeoRecoveryUnlock(store:V11SecurityStore,dismiss:()->Unit,unlocked:()->Unit) {
    val verifier=rememberNeoSecurityVerifier()
    var code by remember {mutableStateOf("")};var pin by remember {mutableStateOf("")};var confirmation by remember {mutableStateOf("")};var error by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=dismiss,properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn),title={Text(neoLabel("استرداد الوصول","Recover access"))},text={Column {
        OutlinedTextField(code,{code=it},label={Text(neoLabel("رمز الاسترداد المحفوظ","Saved recovery code"))},visualTransformation=PasswordVisualTransformation())
        OutlinedTextField(pin,{pin=it.filter(Char::isDigit).take(12)},label={Text(neoLabel("PIN جديد: ٦–١٢ رقمًا","New PIN: 6–12 digits"))},visualTransformation=PasswordVisualTransformation())
        OutlinedTextField(confirmation,{confirmation=it.filter(Char::isDigit).take(12)},label={Text(neoLabel("تأكيد PIN","Confirm PIN"))},visualTransformation=PasswordVisualTransformation())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=!verifier.busy&&pin.length in 6..12&&pin==confirmation&&code.isNotBlank(),onClick={verifier.execute({store.recoverPin(code,pin)}) {result->if(result.getOrDefault(false)){dismiss();unlocked()} else error=neoSecurityFailure(store,neoLiteral("رمز غير صحيح أو محاولات مؤقتًا محظورة","Incorrect code or attempts temporarily blocked"))}}){Text(neoLabel("تغيير PIN وفتح","Reset PIN and unlock"))}},dismissButton={TextButton(onClick=dismiss){Text(neoLabel("إلغاء","Cancel"))}})
}
